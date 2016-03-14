package com.httpdowefeelthebern.cs275lab4webservices;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    TextView textViewResponse;
    EditText urlText;
    EditText userIDText;
    EditText userPasswordText;
    EditText CRNsText;
    static Calendar cal;
    static Button timeTicketButton;
    Button buttonAutoRegister;
    String defaultDBTableName;
    SQLiteDatabase db;

    private AlarmManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewResponse = (TextView)findViewById(R.id.textViewResponse);
        urlText = (EditText)findViewById(R.id.editTextURL);
        userIDText = (EditText)findViewById(R.id.editTextUserID);
        userPasswordText = (EditText)findViewById(R.id.editTextPassword);
        CRNsText = (EditText)findViewById(R.id.editTextCRNs);
        timeTicketButton = (Button)findViewById(R.id.buttonTimeTicket);
        buttonAutoRegister = (Button)findViewById(R.id.autoRegister);
        manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(0, 0, 0, 0, 0, 0);
        defaultDBTableName = "UserData";
        db = openOrCreateDatabase("appdata", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+defaultDBTableName+"(urlText TEXT, userIDText TEXT, userPasswordText TEXT, UtcTime INTEGER, CRNs TEXT);");
        LoadPreviousUIUserData();
        CSVtoJSON("11111");
    }

    private void LoadPreviousUIUserData()
    {
        try{
            boolean alarmUp = (PendingIntent.getBroadcast(this, 0, new Intent("com.httpdowefeelthebern.register"),PendingIntent.FLAG_NO_CREATE) != null);
            if(alarmUp)
            {
                buttonAutoRegister.setText("ALARM IS ON");
            }
            else
            {
                buttonAutoRegister.setText("ALARM IS OFF");
            }

            Cursor resultSet = db.rawQuery("Select * from UserData",null);
            print("There are " + Integer.toString(resultSet.getCount()) + " rows in the db");
            if(resultSet.getCount() == 0){

            }
            else
            {
                resultSet.moveToLast();
                String url = resultSet.getString(0);
                String userID = resultSet.getString(1);
                String userPassword = resultSet.getString(2);
                long time = resultSet.getLong(3);
                String CRNs = resultSet.getString(4);
                urlText.setText(url);
                userIDText.setText(userID);
                userPasswordText.setText(userPassword);
                cal.setTime(new Date(time));
                CRNsText.setText(JSONtoCSV(CRNs));
                updateTimeTicketButtonUI();
            }
        }catch(Exception ex){print(ex.getLocalizedMessage());}
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), hourOfDay, minute);
            updateTimeTicketButtonUI();
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            cal.set(year, month, day, cal.get(Calendar.DATE), cal.get(Calendar.MINUTE));
            FragmentManager manager = getFragmentManager();
            DialogFragment newFragment1 = new TimePickerFragment();
            newFragment1.show(manager, "timePicker");
        }
    }

    public void click_set_time_date(View view)
    {
        FragmentManager manager = getFragmentManager();
        DialogFragment newFragment2 = new DatePickerFragment();
        newFragment2.show(manager, "datePicker");
    }

    static public void updateTimeTicketButtonUI()
    {
        Calendar t = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        t.set(0, 0, 0, 0, 0, 0);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if(cal == t)
        {
            timeTicketButton.setText("SET TIME TICKET TIME AND DATE");
        }
        else
        {
            timeTicketButton.setText(simpleDateFormat.format(cal.getTime()));
        }
    }

    public void startAlarm(View view)
    {
        boolean alarmUp = (PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class),PendingIntent.FLAG_NO_CREATE) != null);
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,  alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pendingIntent);
        alarmUp = (PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class),PendingIntent.FLAG_NO_CREATE) != null);
        print(Boolean.toString(alarmUp));
    }

    public void ToggleAlarmServiceToTrigger(View view)
    {
        boolean alarmUp = (PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class),PendingIntent.FLAG_NO_CREATE) != null);
        //Toast.makeText(this, Boolean.toString(alarmUp), Toast.LENGTH_SHORT).show();
        if(!alarmUp)
        {
            Intent alarmIntent = new Intent(this, AlarmReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,  alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            long curTime = System.currentTimeMillis();
            long doneTime = cal.getTimeInMillis();
            if(curTime < doneTime)
            {
                print(Long.toString(curTime));
                print(Long.toString(doneTime));

                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, doneTime, pendingIntent);
                Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
                buttonAutoRegister.setText("ALARM IS ON");
            }
            else
            {
                Toast.makeText(this, "Please set a time ticket in the future", Toast.LENGTH_SHORT);
            }
        }
        else
        {
            Intent alarmIntent = new Intent(this, AlarmReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,  alarmIntent, PendingIntent.FLAG_NO_CREATE);

            manager.cancel(pendingIntent);
            pendingIntent.cancel();

            Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
            buttonAutoRegister.setText("ALARM IS OFF");
        }
    }

    public void click_send(View view)
    {
        try{
            sendData();
        }catch(Exception e){print(e.getLocalizedMessage());}
    }

    private void sendData() throws Exception
    {
        MakeAddUserTask SendDataTask = new MakeAddUserTask();
        String url = urlText.getText().toString();
        String email = userIDText.getText().toString();
        String pass = userPasswordText.getText().toString();
        String crns = CSVtoJSON(CRNsText.getText().toString());
        print(crns);
        Data d = new Data(url, email, pass, crns);
        SendDataTask.execute(d);
    }

    // (urlText TEXT, userIDText TEXT, userPasswordText TEXT, UtcTime INTEGER)
    protected boolean writeToDB(String url, String userID, String userPassword, long UtcTime, String crns)
    {
        ContentValues values = new ContentValues();
        values.put("urlText",url);
        values.put("userIDText", userID);
        values.put("userPasswordText", userPassword);
        values.put("UtcTime", UtcTime);
        values.put("CRNs", crns);
        boolean success = db.insert(defaultDBTableName, null, values) > 0;
        return success;
    }

    class MakeAddUserTask extends AsyncTask<Data, String, String> {
        public MakeAddUserTask()
        {
        }

        @Override
        protected String doInBackground(Data... datas) {
            String responseString = "";
            try {
                for(final Data data : datas)
                {
                    String urlFinal = data.url + "/add_user";
                    print("Trying URL: " + urlFinal);
                    String urlParameters  = "id=" + data.email + "&email=" + data.email + "&crns=" + data.crns;
                    byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
                    int    postDataLength = postData.length;

                    URL url = new URL(urlFinal);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("charset", "utf-8");
                    urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                    urlConnection.setUseCaches( false );

                    try( DataOutputStream wr = new DataOutputStream( urlConnection.getOutputStream())) {
                        wr.write( postData );
                    }

                    String line;
                    BufferedReader reader = null;
                    try{
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        writeToDB(data.url, data.email, data.pass, cal.getTime().getTime(), data.crns);
                    }catch(Exception e){}
                    if(reader == null){
                        try{
                            reader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                        }catch(Exception e){}
                    }
                    while ((line = reader.readLine()) != null) {
                        responseString += line;
                        System.out.println(line);
                    }
                    reader.close();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            textViewResponse.setText(result);
        }
    }

    public String CSVtoJSON(String csv)
    {
        CSVParser parser = new CSVParser();
        JSONArray array = null;
        try{
            String[] res;
            csv = csv.replace(" ", "");
            res = parser.parseLine(csv);
            array = new JSONArray(Arrays.asList(res));
        }catch(Exception ex){ex.printStackTrace();}
        return array.toString();
    }

    public String JSONtoCSV(String json)
    {
        String result = "";
        JSONArray array = null;
        try{
            array = new JSONArray(json);
            for(int i = 0; i < array.length(); i++)
            {
                result += array.getString(i);
                if(i != array.length()-1)
                {
                    result += ", ";
                }
            }
        }catch(Exception ex){ex.printStackTrace();}
        return result;
    }

    private void print(String str)
    {
        System.out.println(str);
    }
}

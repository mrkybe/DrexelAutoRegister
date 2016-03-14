package com.httpdowefeelthebern.cs275lab4webservices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AlarmReceiver extends BroadcastReceiver {
    SQLiteDatabase db;

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        print("SDJHJKGSFHGJDKSFGH\nJKSFGHJKFSDHGJ\nKHDFSJKGHJK\nDFGHJKFGHJ\n");

        PowerManager pm = (PowerManager) arg0.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();
        SQLiteDatabase db = arg0.openOrCreateDatabase("appdata", arg0.MODE_PRIVATE, null);
        loadUserData(arg0);
        db.close();
        Toast.makeText(arg0, "Registering you for classes!", Toast.LENGTH_SHORT).show();
        wl.release();
        /*AlarmManager manager = (AlarmManager)arg0.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(arg0, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(arg0, 0,  alarmIntent, PendingIntent.FLAG_NO_CREATE);
        manager.cancel(pendingIntent);
        pendingIntent.cancel();*/
    }

    private void quickLine()
    {

    }

    private void loadUserData(Context context)
    {
        try{
            if(db == null)
                db = context.openOrCreateDatabase("appdata", context.MODE_PRIVATE, null);
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
                String CRNs = resultSet.getString(4);
                Data d = new Data(url, userID, userPassword, CRNs);
                registerUser(d);
            }
        }catch(Exception ex){ex.printStackTrace();}
    }

    private void registerUser(Data d) throws Exception
    {
        MakeRegisterUserTask SendDataTask = new MakeRegisterUserTask();
        SendDataTask.execute(d);
    }

    class MakeRegisterUserTask extends AsyncTask<Data, String, String> {
        @Override
        protected String doInBackground(Data... datas) {
            String responseString = "";
            try {
                for(Data data : datas)
                {
                    print("Trying URL: " + data.url + "/register_user");
                    data.url = data.url + "/register_user";

                    String urlParameters  = "id=" + data.email + "&email=" + data.email + "&password=" + data.pass;
                    byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
                    int    postDataLength = postData.length;

                    URL url = new URL(data.url);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("charset", "utf-8");
                    urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                    responseString = urlConnection.toString() + "\n\n";
                    //responseString = urlConnection.getErrorStream()
                    print("test");

                    urlConnection.setUseCaches( false );
                    try( DataOutputStream wr = new DataOutputStream( urlConnection.getOutputStream())) {
                        wr.write( postData );
                    }

                    String line;
                    BufferedReader reader = null;
                    try{
                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    }catch(Exception e){e.printStackTrace();}
                    if(reader == null){
                        try{
                            reader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                        }catch(Exception e){e.printStackTrace();}
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
        }
    }

    private void print(String str)
    {
        System.out.println(str);
    }
}
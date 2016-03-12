package com.httpdowefeelthebern.cs275lab4webservices;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.net.HttpURLConnection;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public enum URLTarget {GEOIP, WEATHER}
    // Get GPS location

    // invoke Weather underground hourly weather service to get the current forecast in Async task

    // parse the forecast to get the Date/Time, weather description, temp, relative humidity, for each hour

    // use link to img in result to set the graphic and display all of those things

    // Store the current date and time in a sqlite database on the device indicating the time that
    // you last queried the forecast, along with the current forecast data (you can store the
    // entire json document, if that is easier for you). Next time you launch the program,
    // if the last time you ran the program was within the past hour, display a toast on
    // the screen to this effect and load the current forecast from the sqlite database
    // instead of making the network call. It should run a little faster thanks to this cache!
    TextView locationText;
    ListView listView;
    SQLiteDatabase db;

    LocationManager locationManager;
    LocationListener listener;

    private Boolean gpsStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView)findViewById(R.id.listView);
        locationText = (TextView)findViewById(R.id.locationText);
        db = openOrCreateDatabase("lab4db",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS GeoCache(UtcTime INTEGER,Result TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS WeatherCache(UtcTime INTEGER,Result TEXT);");
        try{
            // Check if the database has data thats from within the last hour... if not then do this
            dataDB();
        }catch(Exception ex){print(ex.getLocalizedMessage());}
    }

    private void dataDB()
    {
        try{
            Cursor resultSet = db.rawQuery("Select * from GeoCache",null);
            print("There are " + Integer.toString(resultSet.getCount()) + " rows in the db");
            if(resultSet.getCount() == 0){
                callWeatherService();
            }
            else
            {
                updateUI();
                updateLocationUI();
                resultSet.moveToLast();
                Long UtcTime = resultSet.getLong(0);
                String Result = resultSet.getString(1);
                Long diff = (System.currentTimeMillis() - UtcTime) / (1000);
                print("Last time was " + UtcTime.toString());
                print("Current time is " + System.currentTimeMillis());
                print("Difference in time is " + diff + " seconds.");
                if(diff >= 3)
                {
                    print("Data is old!  Refreshing!");
                    callWeatherService();
                }
            }
        }catch(Exception ex){print(ex.getLocalizedMessage());}
    }

    private void updateLocationUI() throws Exception
    {
        try{
            Cursor resultSet = db.rawQuery("Select * from GeoCache",null);
            print("There are " + Integer.toString(resultSet.getCount()) + " rows in the db");
            resultSet.moveToLast();
            String Result = resultSet.getString(1);

            JSONObject res = new JSONObject(Result).getJSONObject("location");
            String city = res.getString("city");
            String state  = res.getString("state");
            String zip = res.getString("zip");
            String answer = "Weather for " + city + ", " + state + " " + zip;
            locationText.setText(answer);
        }catch(Exception ex){print(ex.getLocalizedMessage());}
    }

    private void updateUI() throws Exception
    {
        try{
            Cursor resultSetWeather = db.rawQuery("Select * from WeatherCache",null);
            print("There are " + Integer.toString(resultSetWeather.getCount()) + " rows in the db");
            resultSetWeather.moveToLast();
            String Result = resultSetWeather.getString(1);

            JSONArray res = new JSONObject(Result).getJSONArray("hourly_forecast");
            ArrayList<WeatherData> weather = new ArrayList<>();
            for(int i = 0; i < res.length(); i++)
            {
                WeatherData forecast = new WeatherData();
                JSONObject fc = res.getJSONObject(i);
                forecast.epoch = fc.getJSONObject("FCTTIME").getInt("epoch");
                forecast.month = fc.getJSONObject("FCTTIME").getString("month_name");
                forecast.day = fc.getJSONObject("FCTTIME").getString("weekday_name");
                forecast.date = fc.getJSONObject("FCTTIME").getString("mday");
                forecast.hour = fc.getJSONObject("FCTTIME").getString("civil");
                forecast.weatherDescription = fc.getString("condition");
                forecast.temperature = fc.getJSONObject("temp").getString("english") + "F";
                forecast.humidity = fc.getString("humidity");
                forecast.imgURL = fc.getString("icon_url");

                print(forecast.hour);
                weather.add(forecast);
            }
            Collections.sort(weather, new WeatherComparator());
            CustomWeatherAdapter adapter = new CustomWeatherAdapter(this, weather);
            listView.setAdapter(adapter);
        }catch(Exception ex){print(ex.getLocalizedMessage());}
    }


    private void callWeatherService() throws Exception
    {
        MakeRequestTask GeoLookupTask = new MakeRequestTask(System.currentTimeMillis(), URLTarget.GEOIP);
        GeoLookupTask.execute();
        MakeRequestTask HourlyLookupTask = new MakeRequestTask(System.currentTimeMillis(), URLTarget.WEATHER);
        HourlyLookupTask.execute();
    }

    class MakeRequestTask extends AsyncTask<String, String, String> {
        String GeoLookupURL = "http://api.wunderground.com/api/073d911d85e3dc47/geolookup/q/autoip.json";
        String HourlyLookupURL = "http://api.wunderground.com/api/073d911d85e3dc47/hourly/q/autoip.json";

        Long createdUTC;
        URLTarget target;
        public MakeRequestTask(Long createdUTC_, URLTarget target_)
        {
            createdUTC = createdUTC_;
            target = target_;
        }

        @Override
        protected String doInBackground(String... urls) {
            String responseString = "";
            String urlString;
            if(target  == URLTarget.GEOIP)
            {
                urlString = GeoLookupURL;
                urls = new String[]{GeoLookupURL};
            }
            else {
                urlString = HourlyLookupURL;
                urls = new String[]{HourlyLookupURL};
            }
            try {
                for(String URLin : urls)
                {
                    URL url = new URL(URLin);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("Accept-Encoding", "identity");
                    urlConnection.setRequestProperty("Connection","Close");
                    try {
                        InputStream in = urlConnection.getInputStream();
                        InputStreamReader isw = new InputStreamReader(in);

                        int data = isw.read();
                        while (data != -1) {
                            char current = (char) data;
                            responseString += current;
                            data = isw.read();
                            //System.out.print(current);
                        }

                        //responseString += readStream(in);
                    }catch(Exception ex)
                    {
                        print("SOMETHING HAPPEN "+ ex.getLocalizedMessage());
                    }
                    finally{
                        print(urlConnection.getHeaderFields().get("Content-Length").get(0));
                        urlConnection.disconnect();
                    }
                }
            }
            catch (Exception e) {
                print("SOMETHING BAD HAPPEN "+ e.getLocalizedMessage());
                //TODO Handle problems..
            }
            //print("This is the ANSWER: " + responseString);
            //print("This \n is \n sparta");
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            print("Got Results! " + result);
            if(target == URLTarget.GEOIP)
            {
                print("Put Results in GeoCache!");
                writeToDB("GeoCache",createdUTC,result);
                try {
                    updateLocationUI();
                }catch(Exception ex)
                {
                    print("SOMETHING HAPPEN "+ ex.getLocalizedMessage());
                }
            }
            else if(target == URLTarget.WEATHER)
            {
                print("Put Results in Weather Cache!");
                writeToDB("WeatherCache",createdUTC,result);
                try {
                    updateUI();
                }catch(Exception ex)
                {
                    print("SOMETHING HAPPEN "+ ex.getLocalizedMessage());
                }
            }
            else
            {
                assert false;
            }
        }
    }

    protected boolean writeToDB(String targetTable, Long time, String data)
    {
        ContentValues values = new ContentValues();
        values.put("UtcTime",time);
        values.put("Result",data);
        print(data);
        boolean success = db.insert(targetTable, null, values) > 0;
        return success;
    }

    private void print(String str)
    {
        System.out.println(str);
    }
}

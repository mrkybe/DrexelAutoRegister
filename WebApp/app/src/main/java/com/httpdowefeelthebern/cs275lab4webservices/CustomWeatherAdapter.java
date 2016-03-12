package com.httpdowefeelthebern.cs275lab4webservices;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ky on 3/5/2016.
 */
public class CustomWeatherAdapter extends ArrayAdapter<WeatherData> {
    static Map images = new HashMap();

    private static class ViewHolder{
        ImageView image;
        TextView text;
    }

    public CustomWeatherAdapter(Context context, ArrayList<WeatherData> weather)
    {
        super(context, R.layout.simple_weather_item, weather);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final WeatherData data = getItem(position);

        ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.simple_weather_item, parent, false);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            viewHolder.text = (TextView) convertView.findViewById(R.id.text1);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        final ImageView imgMain = (ImageView) convertView.findViewById(R.id.image);
        TextView tvMain = (TextView)convertView.findViewById(R.id.text1);

        if(!images.containsKey(data.imgURL))
        {
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        Bitmap bimage = getBitmapFromURL(data.imgURL);
                        images.put(data.imgURL, bimage);
                        imgMain.setImageBitmap((Bitmap) images.get(data.imgURL));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
        else
        {
            imgMain.setImageBitmap((Bitmap)images.get(data.imgURL));
        }
        String res = data.hour + " " + data.day + " " + data.month + " " + data.date + "\n" + data.temperature + " Humidity: " + data.humidity + " " + data.weatherDescription;
        tvMain.setText(res);
        return convertView;
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            Log.e("src", src);
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            Log.e("Bitmap","returned");
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Exception", e.getMessage());
            return null;
        }
    }
}

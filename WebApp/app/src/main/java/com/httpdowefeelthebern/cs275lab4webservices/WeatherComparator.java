package com.httpdowefeelthebern.cs275lab4webservices;

import java.util.Comparator;

/**
 * Created by Ky on 3/5/2016.
 */
public class WeatherComparator implements Comparator<WeatherData> {
    @Override
    public int compare(WeatherData o1, WeatherData o2) {
        return Integer.compare(o1.epoch, o2.epoch);
    }
}
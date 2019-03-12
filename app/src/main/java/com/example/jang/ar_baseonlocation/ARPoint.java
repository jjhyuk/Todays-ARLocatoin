package com.example.jang.ar_baseonlocation;

import android.location.Location;
import android.util.Log;

import static com.example.jang.ar_baseonlocation.ARActivity.TAG;

/**
 * Created by ntdat on 1/16/17.
 */

public class ARPoint {
    Location location;
    String name;

    public ARPoint(String name, double lat, double lon, double altitude) {
        this.name = name;
        location = new Location("ARPoint");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAltitude(location.getAltitude());
        Log.d(TAG, "ARPoint: "+name);
        Log.d(TAG, "ARPoint: "+lat);
        Log.d(TAG, "ARPoint: "+lon);
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}

package com.example.jang.ar_baseonlocation;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;
import com.google.maps.android.data.geojson.GeoJsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class GeoJsonDemoActivity extends BaseDemoActivity implements SensorEventListener{

    Boolean check = false;
    private final static String mLogTag = "GeoJsonDemo";
    String TAG = "GeoJsonDemo";

    HashMap<String, String> hashMap = new HashMap<String, String>();
    ArrayList<HashMap<String,String>> arrayList = new ArrayList<>();
    /**
     * Assigns a color based on the given magnitude
     */

    private SurfaceView surfaceView;
    private FrameLayout cameraContainerLayout;
    private AROverlayView arOverlayView;
    private Camera camera;
    private ARCamera arCamera;
    private TextView tvCurrentLocation;
    private  TextView tvTime;
    private  TextView tvDistance;

    private SensorManager sensorManager;
    private final static int REQUEST_CAMERA_PERMISSIONS_CODE = 11;
    public static final int REQUEST_LOCATION_PERMISSIONS_CODE = 0;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 0;//1000 * 60 * 1; // 1 minute

    private LocationManager locationManager;
    public Location location;
    Double lng ;
    Double lat;
    boolean isGPSEnabled;
    boolean isNetworkEnabled;
    boolean locationServiceAvailable;
    Thread t;
    String URL ;
    GoogleMap googleMap;



    private static float magnitudeToColor(double magnitude) {
        if (magnitude < 1.0) {
            return BitmapDescriptorFactory.HUE_CYAN;
        } else if (magnitude < 2.5) {
            return BitmapDescriptorFactory.HUE_GREEN;
        } else if (magnitude < 4.5) {
            return BitmapDescriptorFactory.HUE_YELLOW;
        } else {
            return BitmapDescriptorFactory.HUE_RED;
        }
    }

    protected int getLayoutId() {
        return R.layout.geojson_demo;
    }

    @Override
    protected void startDemo() {
        // Download the GeoJSON file.
        //retrieveFileFromUrl();
        // Alternate approach of loading a local GeoJSON file.
        //retrieveFileFromResource();

        initLocationService();

    }

    private void retrieveFileFromUrl() {
        Intent get = getIntent();
        String endx = get.getStringExtra("lat");
        String endy = get.getStringExtra("lng");
        //endy = "37.46650915033226";
        //endy = "126.88890422571144";
        Log.d("Extra",String.valueOf(endx) +":"+String.valueOf(endy));

        URL = getString(R.string.geojson_url) + "?";
        URL += "startx=" +location.getLongitude()+"&starty="+location.getLatitude()+"&endx="+endy+"&endy="+endx;
        lng = location.getLongitude();
        lat = location.getLatitude();
        Log.d("URL",URL);

        new DownloadGeoJsonFile().execute(URL);
    }

    private void retrieveFileFromResource() {
        try {
            GeoJsonLayer layer = new GeoJsonLayer(getMap(), R.raw.earthquakes_with_usa, this);
            addGeoJsonLayerToMap(layer);
        } catch (IOException e) {
            Log.e(mLogTag, "GeoJSON file could not be read");
        } catch (JSONException e) {
            Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
        }
    }

    /**
     * Adds a point style to all features to change the color of the marker based on its magnitude
     * property
     */
    private void addColorsToMarkers(GeoJsonLayer layer) {
        // Iterate over all the features stored in the layer
        for (GeoJsonFeature feature : layer.getFeatures()) {
            // Check if the magnitude property exists
            if (feature.getProperty("mag") != null && feature.hasProperty("place")) {
                double magnitude = Double.parseDouble(feature.getProperty("mag"));

                // Get the icon for the feature
                BitmapDescriptor pointIcon = BitmapDescriptorFactory
                        .defaultMarker(magnitudeToColor(magnitude));

                // Create a new point style
                GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();

                // Set options for the point style
                pointStyle.setIcon(pointIcon);
                pointStyle.setTitle("Magnitude of " + magnitude);
                pointStyle.setSnippet("Earthquake occured " + feature.getProperty("place"));

                // Assign the point style to the feature
                feature.setPointStyle(pointStyle);
            }
        }
    }

    private class DownloadGeoJsonFile extends AsyncTask<String, Void, GeoJsonLayer> {

        @Override
        protected GeoJsonLayer doInBackground(String... params) {
            try {
                // Open a stream from the URL
                InputStream stream = new URL(params[0]).openStream();

                String line;
                StringBuilder result = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                while ((line = reader.readLine()) != null) {
                    // Read and save each line of the stream
                    result.append(line);
                }

                // Close the stream
                reader.close();
                stream.close();
                GeoJsonParser geoJsonParser = new GeoJsonParser(new JSONObject(result.toString()));
                Log.d(TAG, "doInBackground result: "+result);
                Log.d(TAG, "doInBackground result: "+geoJsonParser);
                ArrayList<GeoJsonFeature> features = new ArrayList<GeoJsonFeature>();

                JSONObject jsonObject = new JSONObject(result.toString());
                features = geoJsonParser.parseFeatureCollection(jsonObject);
                Log.d(TAG, "doInBackground: "+features.size());
                hashMap = new HashMap<String, String>();
                arrayList= new ArrayList<>();
                for(int i = 0 ; i < features.size() ; i++)
                {
                    if(features.get(i).getProperty("pointIndex") != null)
                    {


                        Log.d(TAG, "*****Point*****");
                        Log.d(TAG, "index: "+features.get(i).getProperty("index"));
                        Log.d(TAG, "totalDistance: "+features.get(i).getProperty("totalDistance"));
                        Log.d(TAG, "totalTime: "+features.get(i).getProperty("totalTime"));
                        if(i == 0)
                        {
                            final String totalTime = features.get(0).getProperty("totalTime");
                            final String totalDistance = features.get(0).getProperty("totalDistance");
                            final String firstDescription = features.get(0).getProperty("description");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvCurrentLocation.setText("(1)"+"방향으로 "+firstDescription);
                                    tvDistance.setText("거리:"+Double.parseDouble(totalDistance)/1000+"km");
                                    tvTime.setText("예상소요시간:"+Integer.parseInt(totalTime)/60+"분");
                                }
                            });
                        }
                        Log.d(TAG, "description: "+features.get(i).getProperty("description"));
                        Log.d(TAG, "pointType: "+features.get(i).getProperty("pointType"));
                        Log.d(TAG, "pointIndex: "+features.get(i).getProperty("pointIndex"));
                        Log.d(TAG, "getGeometryObject: "+features.get(i).getGeometry().getGeometryObject());

                        String tmp  = String.valueOf(features.get(i).getGeometry().getGeometryObject());
                        String[] latlng = tmp.split(",");
                        String lat = latlng[0].substring(10,latlng[0].length());
                        String lng = latlng[1].substring(0,latlng[1].length()-1);
                        Log.d(TAG, "lating 1: "+lat);
                        Log.d(TAG, "lating 2: "+lng);

                        hashMap = new HashMap<>();

                        hashMap.put("pointIndex",features.get(i).getProperty("pointIndex"));
                        hashMap.put("description",features.get(i).getProperty("description"));
                        hashMap.put("lat",lat);
                        hashMap.put("lng",lng);
                        arrayList.add(hashMap);
                    }
                    else
                    {
                        Log.d(TAG, "*****NotPoint*****");
                        Log.d(TAG, "index: "+features.get(i).getProperty("index"));
                        Log.d(TAG, "description: "+features.get(i).getProperty("description"));
                        Log.d(TAG, "getGeometryObject: "+features.get(i).getGeometry().getGeometryObject());
                    }

                }

                return new GeoJsonLayer(getMap(), new JSONObject(result.toString()));
            } catch (IOException e) {
                Log.e(mLogTag, "GeoJSON file could not be read");
            } catch (JSONException e) {
                Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
            }
            return null;
        }

        @Override
        protected void onPostExecute(GeoJsonLayer layer) {
            for(int i = 0 ; i < arrayList.size();i++)
            {
                Log.d("ArrayListSet",arrayList.get(i).get("pointIndex"));
                Log.d("ArrayListSet",arrayList.get(i).get("description"));
                Log.d("ArrayListSet",arrayList.get(i).get("lat"));
                Log.d("ArrayListSet",arrayList.get(i).get("lng"));

            }

            if (layer != null) {

                Log.d("arraySize onPost ", String.valueOf(arrayList.size()));
                arOverlayView.arPoints = new ArrayList<ARPoint>() {{
                   /* arOverlayView.select = 0;
                    add(new ARPoint("출발", Double.parseDouble(arrayList.get(arOverlayView.select).get("lat")), Double.parseDouble(arrayList.get(arOverlayView.select).get("lng")), 30));*/
                    for(int i = 0 ; i < arrayList.size();i++)
                    {
                        Log.d("거리",i+"번째 거리"+String.valueOf(calDistance(lat, lng, Double.parseDouble(arrayList.get(i).get("lat")), Double.parseDouble(arrayList.get(i).get("lng")))));
                        Log.d("IN FOR", String.valueOf(i));
                        if(i == 0)
                        {
                            add(new ARPoint("출발",Double.parseDouble(arrayList.get(i).get("lat")),Double.parseDouble(arrayList.get(i).get("lng")),0));
                        }
                        else if(i == arrayList.size()-1)
                        {
                            add(new ARPoint("도착",Double.parseDouble(arrayList.get(i).get("lat")),Double.parseDouble(arrayList.get(i).get("lng")),1));
                        }
                        else {
                            add(new ARPoint(arrayList.get(i).get("pointIndex"), Double.parseDouble(arrayList.get(i).get("lat")), Double.parseDouble(arrayList.get(i).get("lng")), 1));
                        }
                    }
                    initAROverlayView();
                }};
                addGeoJsonLayerToMap(layer);
            }



            Thread tmp = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("arraySize onTmp ", String.valueOf(arrayList.size()));
                    int i = 0;
                    while(location == null)
                    {

                    }

                    Log.d("Location", String.valueOf(location.getLatitude()));
                    Log.d("Location", String.valueOf(location.getLongitude()));
                    while(calDistance(location.getLatitude(), location.getLongitude(), Double.parseDouble(arrayList.get(arrayList.size()-1).get("lat")), Double.parseDouble(arrayList.get(arrayList.size()-1).get("lng")))>10)
                    {
                        //Log.d("현재 위치", String.valueOf(location.getLatitude())+","+String.valueOf(location.getLongitude()));
                        //Log.d("위치",String.valueOf(calDistance(location.getLatitude(), location.getLongitude(), Double.parseDouble(arrayList.get(arrayList.size()-1).get("lat")), Double.parseDouble(arrayList.get(arrayList.size()-1).get("lng")))));
                        //Log.d("각 거리",i+"번째와의 현재위치 거리"+String.valueOf(calDistance(location.getLatitude(), location.getLongitude(), Double.parseDouble(arrayList.get(i).get("lat")), Double.parseDouble(arrayList.get(i).get("lng")))));

                        if(calDistance(location.getLatitude(), location.getLongitude(), Double.parseDouble(arrayList.get(i).get("lat")), Double.parseDouble(arrayList.get(i).get("lng")))<5)
                        {
                            final int tmp = i;
                            if(tmp == arrayList.size())
                            {
                                Toast.makeText(getApplicationContext(), "도착하셨습니다", Toast.LENGTH_SHORT).show();
                                break;
                            }

                            Log.d(TAG, "다음순서로 넘어갑니다");
                            Log.d("tmp", String.valueOf(tmp));
                            Log.d("array",arrayList.get(tmp).get("pointIndex"));
                            Log.d("boolean", String.valueOf(arrayList.get(tmp).get("pointIndex").equals(String.valueOf(arOverlayView.select))));

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(arrayList.get(tmp).get("pointIndex").equals(String.valueOf(arOverlayView.select)))
                                    {
                                        arOverlayView.arPoints.add(new ARPoint(arrayList.get(tmp + 1).get("pointIndex"), Double.parseDouble(arrayList.get(tmp + 1).get("lat")), Double.parseDouble(arrayList.get(tmp + 1).get("lng")), 1));
                                        arOverlayView.select = tmp + 1;
                                        initAROverlayView();
                                    }
                                }
                            });

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int num = Integer.parseInt(arrayList.get(tmp).get("pointIndex"))+1;
                                    tvCurrentLocation.setText("("+num+")"+"방향으로 "+arrayList.get(tmp).get("description"));
                                }
                            });
                        }
                        i++;
                        if(i >= arrayList.size())
                        {
                            i = 0;
                        }

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    };


                }
            });


            tmp.start();

        }

    }

    private void addGeoJsonLayerToMap(final GeoJsonLayer layer) {

    Log.d("layer", String.valueOf(layer));
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(5);
        ArrayList<LatLng> arrayPonints = new ArrayList();



        // for loop를 통한 n개의 마커 생성
        for (int  i = 0; i < arrayList.size(); i++) {
            // 1. 마커 옵션 설정 (만드는 과정)
            MarkerOptions makerOptions = new MarkerOptions();
            if( i == 0)
            {

                makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                        .position(new LatLng(Double.parseDouble(arrayList.get(i).get("lat")) ,Double.parseDouble( arrayList.get(i).get("lng"))))
                        .title("출발") // 타이틀.
                        .snippet(arrayList.get(i).get("description"));

                arrayPonints.add(new LatLng(Double.parseDouble(arrayList.get(i).get("lat")) ,Double.parseDouble( arrayList.get(i).get("lng"))));


            }
            else if(i == arrayList.size()-1)
            {
                makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                        .position(new LatLng(Double.parseDouble(arrayList.get(i).get("lat")) ,Double.parseDouble( arrayList.get(i).get("lng"))))
                        .title("도착") // 타이틀.
                        .snippet(arrayList.get(i).get("description"));

                arrayPonints.add(new LatLng(Double.parseDouble(arrayList.get(i).get("lat")) ,Double.parseDouble( arrayList.get(i).get("lng"))));
            }
            else
            {
                makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                        .position(new LatLng(Double.parseDouble(arrayList.get(i).get("lat")) ,Double.parseDouble( arrayList.get(i).get("lng"))))
                        .title("번호:" + arrayList.get(i).get("pointIndex")) // 타이틀.
                        .snippet(arrayList.get(i).get("description"));

                arrayPonints.add(new LatLng(Double.parseDouble(arrayList.get(i).get("lat")) ,Double.parseDouble( arrayList.get(i).get("lng"))));

            }


            // 2. 마커 생성 (마커를 나타냄)
            Marker marker = mMap.addMarker(makerOptions);





        }




        polylineOptions.addAll(arrayPonints);
        mMap.addPolyline(polylineOptions);





        addColorsToMarkers(layer);
     /*   while(true)
        {
            if(Map_check == true)
                break;
        }*/

        //layer.addLayerToMap();
        getMap().moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lng, lat)));
        LatLng point = new LatLng(lat,lng);
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(point,15));

        // Demonstrate receiving features via GeoJsonLayer clicks.
        /*layer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
            @Override
            public void onFeatureClick(Feature feature) {


            }


        });*/

    }


    private void setUpMap() {
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }


    @Override
    public void onResume() {
        super.onResume();

        setUpMap();

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        cameraContainerLayout = (FrameLayout) findViewById(R.id.camera_container_layout);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        tvCurrentLocation = (TextView) findViewById(R.id.tv_current_location);
        arOverlayView = new AROverlayView(this);
        tvDistance = findViewById(R.id.tv_distance);
        tvTime = findViewById(R.id.tv_time);

        requestLocationPermission();
        requestCameraPermission();
        registerSensors();

    }


    @Override
    public void onPause() {
        releaseCamera();
        super.onPause();
    }

    public void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSIONS_CODE);
        } else {
            initARCameraView();
        }
    }

    public void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSIONS_CODE);
        } else {
            initLocationService();
        }
    }

    public void initAROverlayView() {
        if (arOverlayView.getParent() != null) {
            ((ViewGroup) arOverlayView.getParent()).removeView(arOverlayView);
        }
        cameraContainerLayout.addView(arOverlayView);
    }

    public void initARCameraView() {
        reloadSurfaceView();

        if (arCamera == null) {
            arCamera = new ARCamera(this, surfaceView);
        }
        if (arCamera.getParent() != null) {
            ((ViewGroup) arCamera.getParent()).removeView(arCamera);
        }
        cameraContainerLayout.addView(arCamera);
        arCamera.setKeepScreenOn(true);
        initCamera();
    }

    private void initCamera() {
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open();
                camera.startPreview();
                arCamera.setCamera(camera);
            } catch (RuntimeException ex){
                Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void reloadSurfaceView() {
        if (surfaceView.getParent() != null) {
            ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
        }

        cameraContainerLayout.addView(surfaceView);
    }

    private void releaseCamera() {
        if(camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            arCamera.setCamera(null);
            camera.release();
            camera = null;
        }
    }

    private void registerSensors() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrixFromVector = new float[16];
            float[] projectionMatrix = new float[16];
            float[] rotatedProjectionMatrix = new float[16];

            SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, sensorEvent.values);

            if (arCamera != null) {
                projectionMatrix = arCamera.getProjectionMatrix();
            }

            Matrix.multiplyMM(rotatedProjectionMatrix, 0, projectionMatrix, 0, rotationMatrixFromVector, 0);
            this.arOverlayView.updateRotatedProjectionMatrix(rotatedProjectionMatrix);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //do nothing
    }

    private void initLocationService() {

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }

        try   {
            this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

            // Get GPS and network status
            this.isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            this.isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isNetworkEnabled && !isGPSEnabled)    {
                // cannot get location
                this.locationServiceAvailable = false;
            }

            this.locationServiceAvailable = true;

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);

                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                updateLatestLocation();


            }

            if (isGPSEnabled)  {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);


                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateLatestLocation();

            }


        } catch (Exception ex)  {
            Log.e(TAG, ex.getMessage());

        }
    }

    private void updateLatestLocation() {
        if (arOverlayView !=null && location != null) {
            if(check == false) {
                retrieveFileFromUrl();
                check = true;
                Log.d("check", String.valueOf(check));
            }
            arOverlayView.updateCurrentLocation(location);





            //Toast.makeText(getApplicationContext(),String.valueOf(Math.round(calDistance(location.getLatitude(),location.getLongitude(),37.461701, 126.880365))),Toast.LENGTH_LONG).show();
            Log.d(TAG, "updateLatesdtLocation: "+location.getLatitude()+","+location.getLongitude()+","+location.getAltitude());
            Handler handler =new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
//                    Toast.makeText(getApplicationContext(),location.getLatitude()+","+location.getLongitude(),Toast.LENGTH_SHORT).show();
                }
            });

        }
    }


    public double calDistance(double lat1, double lon1, double lat2, double lon2){

        double theta, dist;
        theta = lon1 - lon2;
        dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);

        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344;    // 단위 mile 에서 km 변환.
        dist = dist * 1000.0;      // 단위  km 에서 m 로 변환

        return dist;
    }

    // 주어진 도(degree) 값을 라디언으로 변환
    private double deg2rad(double deg){
        return (double)(deg * Math.PI / (double)180d);
    }

    // 주어진 라디언(radian) 값을 도(degree) 값으로 변환
    private double rad2deg(double rad){
        return (double)(rad * (double)180d / Math.PI);
    }




    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location2) {
            location = location2;
            updateLatestLocation();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

}


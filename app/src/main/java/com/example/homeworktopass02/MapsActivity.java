package com.example.homeworktopass02;

import androidx.core.app.ActivityCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener, SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    List<Marker> markersList;
    Marker gpsMarker = null;
    List<Double> savedMarkers;
    private final String jsonFile = "markers.json";
    private TextView textView; // :)
    static private SensorManager mSensorManager;
    private Sensor sensor;
    boolean hideBool = false;
    boolean hideButtons = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markersList = new ArrayList<>();
        savedMarkers = new ArrayList<>();
        FloatingActionButton hide = findViewById(R.id.floatingActionButton);
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
        FloatingActionButton show = findViewById(R.id.floatingActionButton2);
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideButtonsFunction();
            }
        });
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        textView.setVisibility(View.INVISIBLE);
    }

    public void hide(){
        hideBool = !hideBool; //every click changes the value
        Log.i("TAG - Bool", ""+hideBool);
        if(hideBool){
            textView.setVisibility(View.VISIBLE);
        } else textView.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJson();

    }
    @Override
    protected void onDestroy(){
        try{
            saveToJson();
        } catch(JSONException e){
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(sensor != null){
            mSensorManager.registerListener(this, sensor,10000);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(sensor != null){
            mSensorManager.unregisterListener(this, sensor);
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        textView.setText(String.format("Acceleration: \n \t x: %.3f\t\t\t y: %.3f\t", x,y));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); // in ms
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    if (gpsMarker != null)
                        gpsMarker.remove();

                    Location location = locationResult.getLastLocation();

                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    @Override
    public void onMapLoaded() {
        //from lab 07
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        createLocationRequest();
        createLocationCallBack();
        startLocationUpdates();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker longClickMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                .alpha(0.7f).title(String.format("Position(%.2f, %.2f) ", latLng.latitude, latLng.longitude)));
        markersList.add(longClickMarker);
        savedMarkers.add(latLng.latitude);
        savedMarkers.add(latLng.longitude);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.getUiSettings().setMapToolbarEnabled(false);
        showButtonsFunction();
        return false;
    }

    public void clearMemoryOnClick(View view) throws JSONException {
        markersList.clear();
        savedMarkers.clear();
        mMap.clear();
        saveToJson();
        if(hideBool){
            textView.setVisibility(View.INVISIBLE);
            hideBool = false;
        } //else textView.setVisibility(View.VISIBLE);

        hideButtonsFunction();
    }

    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    //Gson bliblioteka w javie do obługi plikow JSON :)
    //proszę wybaczyć nazwy zmiennych ale nie zrozumiałam tego na tyle, żeby nazywać je mądrze
    private void saveToJson()throws JSONException {
        Gson gson = new Gson();
        String list = gson.toJson(savedMarkers);
        FileOutputStream daddy;
        try{
            daddy = openFileOutput(jsonFile,MODE_PRIVATE);
            FileWriter mommy = new FileWriter(daddy.getFD());
            mommy.write(list);
            mommy.close();
        } catch(IOException e){
            e.printStackTrace();
        }

    }

    public void restoreFromJson() {
        FileInputStream inputStream;
        int size = 1000;
        Gson gson = new Gson();
        String readJson;

        try {
            //wczytywanie linijka po linijce
            inputStream = openFileInput(jsonFile);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[size];
            int n;
            StringBuilder builder = new StringBuilder();

            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < size) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<Double>>() {
            }.getType();
            List<Double> o = gson.fromJson(readJson, collectionType);
            savedMarkers.clear();
            if (o != null) {
                savedMarkers.addAll(o);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        markersList.clear();
        try{
            for(int i = 0; i < savedMarkers.size(); i +=2){
                Marker onSavedMarked = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(savedMarkers.get(i),savedMarkers.get(i+1)))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                        .alpha(0.7f).title(String.format("Position(%.2f, %.2f) ", savedMarkers.get(i),savedMarkers.get(i+1))));
                markersList.add(onSavedMarked);
            }
        } catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    public void hideButtonsFunction(){
        if(hideButtons == true){
            FlingAnimation cross = new FlingAnimation(findViewById(R.id.animationButtons), DynamicAnimation.SCROLL_X);
            cross.setStartVelocity(-2000).setMinValue(0).setFriction(1.1f).start();
            cross.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
            hideButtons = false;
            if(hideBool){
                textView.setVisibility(View.INVISIBLE);
                hideBool = false;
            }
        }
    }

    public void showButtonsFunction(){
        if(hideButtons == false){
            FlingAnimation cross = new FlingAnimation(findViewById(R.id.animationButtons), DynamicAnimation.SCROLL_X);
            cross.setStartVelocity(2000).setMinValue(-300).setMaxValue(380).setFriction(1.1f).start();
            cross.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
            hideButtons = true;
        }
    }

}

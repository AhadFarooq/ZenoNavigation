package com.example.zenonavigation;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.zenonavigation.maps.GetDirectionsData;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class Main2Activity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng origin;
    private LatLng destination;
    private Marker marker;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private Button buttonDetectionMode;
    private Button buttonStartNavigation;
    private Button buttonCancelNavigation;
    private Button buttonExpand;
    private FrameLayout arLayout;
    private ImageView imageView;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        origin = new LatLng(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        arLayout = findViewById(R.id.arLayout);

        buttonDetectionMode = findViewById(R.id.buttonDetectionMode);
        buttonDetectionMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        buttonStartNavigation = findViewById(R.id.buttonStartNavigation);
        buttonStartNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDirections(origin, destination);
                buttonCancelNavigation.setVisibility(View.VISIBLE);
                buttonStartNavigation.setVisibility(View.INVISIBLE);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 17));
            }
        });

        buttonCancelNavigation = findViewById(R.id.buttonCancelNavigation);
        buttonCancelNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                destination = null;
                buttonCancelNavigation.setVisibility(View.INVISIBLE);
                imageView.setImageResource(android.R.color.transparent);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 17));
            }
        });

        buttonExpand = findViewById(R.id.buttonExpand);
        buttonExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonExpand.getText().equals("Expand"))
                {
                    LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        0
                );
                    arLayout.setLayoutParams(param);
                    buttonExpand.setText("Contract");
                }
                else
                {
                    LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0,
                            3
                    );
                    arLayout.setLayoutParams(param);
                    buttonExpand.setText("Expand");
                }

            }
        });

        imageView = findViewById(R.id.imageView);
        GetDirectionsData getDirectionsData = new GetDirectionsData(imageView);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        getCurrentLocation();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng point) {

                destination = point;
                mMap.clear();
                marker = mMap.addMarker(new MarkerOptions().position(point));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17));

                buttonStartNavigation.setVisibility(View.VISIBLE);
                buttonCancelNavigation.setVisibility(View.INVISIBLE);
                imageView.setImageResource(android.R.color.transparent);

            }

        });

    }

    public void getCurrentLocation() {
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            origin = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 17));
                        }
                    }
                });
    }

    public void requestDirections(LatLng origin, LatLng destination)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("https://maps.googleapis.com/maps/api/directions/json?");
        sb.append("origin="+origin.latitude+","+origin.longitude);
        sb.append("&destination="+destination.latitude+","+destination.longitude);
        sb.append("&key=AIzaSyBo6CVkO8YOfq3eqRgaLSrQ5PEARPKBtyA");

        Object[] dataTransfer = new Object[2];
        dataTransfer[0] = mMap;
        dataTransfer[1] = sb.toString();

        GetDirectionsData getDirectionsData = new GetDirectionsData(getApplicationContext());
        getDirectionsData.execute(dataTransfer);
    }

}

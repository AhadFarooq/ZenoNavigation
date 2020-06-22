package com.example.zenonavigation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.zenonavigation.maps.Directions;
import com.example.zenonavigation.maps.GetDirectionsData;
import com.example.zenonavigation.maps.GetPlaceData;
import com.example.zenonavigation.maps.Place;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {


    private static DecimalFormat df2 = new DecimalFormat("#.##");
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1000;
    private static boolean mLocationPermissionGranted;
    private boolean gps_enabled = false;
    private boolean network_enabled = false;
    private static boolean navigating = false;
    private boolean expanded = false;

    private FrameLayout arLayout;
    private ArFragment arFragment;

    private static GoogleMap mMap;
    private static LatLng origin;
    private static LatLng destination;
    private Marker marker;

    private ImageButton buttonMyLocation;
    private ImageButton buttonExpand;
    private FloatingActionButton fab;
    private FloatingActionButton fab2;
    private SearchView searchView;

    private ImageButton buttonDetect;

    private TextView textSpeed;
    private TextView textInstructions;


    private Session session;
    private Anchor anchor;
    private AnchorNode anchorNode = null;
    private Pose cameraPose;
    private Pose objectPose;
    private Pose currentCameraPose;

    private ViewRenderable left;
    private ViewRenderable right;
    private ViewRenderable slight_left;
    private ViewRenderable slight_right;
    private ViewRenderable straight;
    private ViewRenderable uturn_left;
    private ViewRenderable uturn_right;

    private String[] maneuver;
    private String[] instructions;
    private LatLng[] target_location;
    private double[] distance;

    private int i=0;


    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);





        /**
         *
         *
         *
         AR */

        arLayout = findViewById(R.id.arLayout);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);








        //VIEW RENDERABLES
        //----------------


        ViewRenderable.builder()
                .setView(this, R.layout.left)
                .build()
                .thenAccept(viewRenderable -> left = viewRenderable);


        ViewRenderable.builder()
                .setView(this, R.layout.right)
                .build()
                .thenAccept(viewRenderable -> right = viewRenderable);


        ViewRenderable.builder()
                .setView(this, R.layout.slight_left)
                .build()
                .thenAccept(viewRenderable -> slight_left = viewRenderable);


        ViewRenderable.builder()
                .setView(this, R.layout.slight_right)
                .build()
                .thenAccept(viewRenderable -> slight_right = viewRenderable);


        ViewRenderable.builder()
                .setView(this, R.layout.straight)
                .build()
                .thenAccept(viewRenderable -> straight = viewRenderable);


        ViewRenderable.builder()
                .setView(this, R.layout.uturn_left)
                .build()
                .thenAccept(viewRenderable -> uturn_left = viewRenderable);


        ViewRenderable.builder()
                .setView(this, R.layout.uturn_right)
                .build()
                .thenAccept(viewRenderable -> uturn_right = viewRenderable);









        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {

                hasLocationPermission();
                if (!mLocationPermissionGranted)
                {
                    requestLocationPermission();
                }

                Frame frame = arFragment.getArSceneView().getArFrame();
                Camera camera = frame.getCamera();
                cameraPose = camera.getDisplayOrientedPose().compose(Pose.makeTranslation(0, 0, -5f));

                try
                {
                    if (anchorNode != null)
                    {
                        objectPose = anchor.getPose();
                        currentCameraPose = camera.getDisplayOrientedPose();

                        float dx = objectPose.tx() - currentCameraPose.tx();
                        float dy = objectPose.ty() - currentCameraPose.ty();
                        float dz = objectPose.tz() - currentCameraPose.tz();

                        float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }



            }
        });










        /**
         *
         *
         *
         Location */

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(0);
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);
        locationRequest.setSmallestDisplacement(0);

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {

                        origin = new LatLng(location.getLatitude(), location.getLongitude());





                        if (navigating)
                        {

                            float speed = location.getSpeed();
                            textSpeed.setText(df2.format(speed)+"\nm/s");


                            CameraPosition oldPos = mMap.getCameraPosition();
                            CameraPosition newPos = CameraPosition.builder(oldPos)
                                    .target(origin)
                                    .bearing(location.getBearing())
                                    .tilt(55)
                                    .build();
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newPos));









                            maneuver = Directions.getManeuver();
                            instructions = Directions.getInstructions();
                            target_location = Directions.getStartLocation();



                            if (target_location!=null)
                            {


                                distance = new double[target_location.length];


                                for (int i=0; i<target_location.length; i++)
                                {



                                    double totalDistance = SphericalUtil.computeDistanceBetween(origin, destination);
                                    if (totalDistance < 5)
                                    {
                                        stopNavigation();
                                        break;
                                    }



                                    distance[i] = SphericalUtil.computeDistanceBetween(origin, target_location[i]);

                                    if (distance[i] <= 10)
                                    {

                                        if (maneuver[i]==null) {
                                            removeRenderable();
                                            addRenderable(straight);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                        else if (maneuver[i].equals("turn-slight-left")) {
                                            removeRenderable();
                                            addRenderable(slight_left);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                        else if (maneuver[i].equals("uturn-left")) {
                                            removeRenderable();
                                            addRenderable(uturn_left);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                        else if (maneuver[i].equals("turn-left")) {
                                            removeRenderable();
                                            addRenderable(left);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                        else if (maneuver[i].equals("turn-slight-right")) {
                                            removeRenderable();
                                            addRenderable(slight_right);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                        else if (maneuver[i].equals("uturn-right")) {
                                            removeRenderable();
                                            addRenderable(uturn_right);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                        else if (maneuver[i].equals("turn-right")) {
                                            removeRenderable();
                                            addRenderable(right);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                        else if (maneuver[i].equals("straight")) {
                                            removeRenderable();
                                            addRenderable(straight);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                    }


                                    else
                                    {

                                        removeRenderable();
                                        addRenderable(straight);
                                        textInstructions.setText("Head straight");

                                    }
                                }
                            }
                        }
                    }
                }

            }

        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());




























        /**
         *
         *
         *
         Controls */


        textSpeed = findViewById(R.id.textSpeed);
        textInstructions = findViewById(R.id.textInstructions);


        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navigating)
                {
                    //end navigation
                    stopNavigation();

                }
                else
                {
                    //start navigation
                    contractMap();
                    fab.setImageResource(R.drawable.round_close_white_36);
                    requestDirections(origin, destination);
                    getDeviceLocation();
                    navigating = true;
                }
            }
        });





        fab2 = findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navigating)
                {
                    //end navigation
                    stopNavigation();

                }
                else
                {
                    //start navigation
                    Place p = new Place();
                    destination = p.getPlace();

                    contractMap();
                    fab2.setImageResource(R.drawable.round_close_white_36);
                    requestDirections(origin, destination);
                    getDeviceLocation();
                    navigating = true;
                }
            }
        });





        buttonExpand = findViewById(R.id.buttonExpand);
        buttonExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expanded)
                {
                    contractMap();
                }
                else
                {
                    expandMap();
                }
            }
        });





        buttonMyLocation = findViewById(R.id.buttonMyLocation);
        buttonMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkGpsPermission();
                getGpsPermission();
                hasLocationPermission();
                if (!mLocationPermissionGranted)
                {
                    requestLocationPermission();
                }
                getDeviceLocation();
            }
        });





        buttonDetect = findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocationPermissionGranted)
                {
                    Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                    startActivity(intent);
                }
            }
        });





        //SEARCH VIEW

        searchView = findViewById(R.id.searchView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                checkGpsPermission();
                if (mLocationPermissionGranted && (gps_enabled || network_enabled))
                {
                    requestPlace(query);

                    mMap.clear();
                    navigating = false;
                    fab.setVisibility(View.INVISIBLE);
                    fab.setImageResource(R.drawable.round_navigation_white_36);
                    fab2.setImageResource(R.drawable.round_navigation_white_36);
                }
                searchView.setIconified(true);
                searchView.clearFocus();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
            }
        });

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    expandMap();
                    buttonExpand.setVisibility(View.INVISIBLE);
                    buttonMyLocation.setVisibility(View.INVISIBLE);

                } else {

                    searchView.setIconified(true);
                    searchView.clearFocus();
                    buttonExpand.setVisibility(View.VISIBLE);
                    buttonMyLocation.setVisibility(View.VISIBLE);

                }
            }
        });









    }

    @Override
    public void onResume() {
        super.onResume();

        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                }
            }
        });

        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);

        hasLocationPermission();

    }












    public void removeRenderable()
    {
        try {
            if (anchorNode != null)
            {
                arFragment.getArSceneView().getScene().removeChild(anchorNode);
                anchorNode.getAnchor().detach();
                anchorNode.setParent(null);
                anchor.detach();
                anchorNode = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public void addRenderable(ViewRenderable viewRenderable)
    {
        try {
            session = arFragment.getArSceneView().getSession();
            anchor =  session.createAnchor(cameraPose);
            anchorNode = new AnchorNode(anchor);
            anchorNode.setRenderable(viewRenderable);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }















    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);

        checkGpsPermission();
        updateLocationUI();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng point) {
                checkGpsPermission();
                if (mLocationPermissionGranted && (!navigating) && (gps_enabled || network_enabled))
                {
                    destination = point;
                    mMap.clear();
                    marker = mMap.addMarker(new MarkerOptions().position(point));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17));
                    fab2.setVisibility(View.INVISIBLE);
                    fab2.setImageResource(R.drawable.round_navigation_white_36);
                    fab.setVisibility(View.VISIBLE);
                }
            }

        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!navigating)
                {
                    fab.setVisibility(View.INVISIBLE);
                    fab2.setVisibility(View.INVISIBLE);
                    marker.remove();
                }
                return false;
            }
        });
    }

    private boolean hasLocationPermission() {
        return mLocationPermissionGranted = (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) + ContextCompat
                .checkSelfPermission(this.getApplicationContext(),
                        Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    updateLocationUI();
                }
                else {
                    requestLocationPermission();
                }
            }
        }

    }








    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                getDeviceLocation();
                mMap.setTrafficEnabled(true);

            } else {
                mMap.setMyLocationEnabled(false);
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted && (gps_enabled || network_enabled)) {
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    origin = new LatLng(location.getLatitude(), location.getLongitude());

                                    if (navigating)
                                    {
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(origin)      // Sets the center of the map
                                                .zoom(17)                   // Sets the zoom
                                                .bearing(location.getBearing())                // Sets the orientation of the camera
                                                .tilt(55)                   // Sets the tilt of the camera
                                                .build();                   // Creates a CameraPosition from the builder
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                    }
                                    else
                                    {
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(origin)      // Sets the center of the map
                                                .zoom(17)                   // Sets the zoom
                                                .bearing(0)                // Sets the orientation of the camera
                                                .tilt(0)                   // Sets the tilt of the camera
                                                .build();                   // Creates a CameraPosition from the builder
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                    }

                                }
                            }
                        });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void checkGpsPermission()
    {
        LocationManager lm = (LocationManager)
                getSystemService(Context. LOCATION_SERVICE ) ;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
    }

    private void getGpsPermission () {
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(MainActivity. this )
                    .setMessage( "Turn on device location in settings" )
                    .setPositiveButton( "GO TO SETTINGS" , new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                    startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                }
                            })
                    .setNegativeButton("NO", null)
                    .show() ;
        }
    }

    public void expandMap()
    {
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0
        );
        arLayout.setLayoutParams(param);
        expanded = true;
        buttonExpand.setImageResource(R.drawable.round_expand_more_black_24);
    }

    public void contractMap()
    {
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                3
        );
        arLayout.setLayoutParams(param);
        expanded = false;
        buttonExpand.setImageResource(R.drawable.round_expand_less_black_24);
    }

    public void requestDirections(LatLng origin, LatLng destination)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("https://maps.googleapis.com/maps/api/directions/json?");
        sb.append("origin="+origin.latitude+","+origin.longitude);
        sb.append("&destination="+destination.latitude+","+destination.longitude);
        sb.append("&departure_time=now");
        sb.append("&mode=driving");
        sb.append("&key="+getString(R.string.google_maps_key)); //AIzaSyBo6CVkO8YOfq3eqRgaLSrQ5PEARPKBtyA

        Object[] dataTransfer = new Object[2];
        dataTransfer[0] = mMap;
        dataTransfer[1] = sb.toString();

        GetDirectionsData getDirectionsData = new GetDirectionsData(getApplicationContext());
        getDirectionsData.execute(dataTransfer);


    }

    public void requestPlace(String location)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("https://maps.google.com/maps/api/geocode/json?");
        sb.append("address="+location);
        sb.append("&key="+getString(R.string.google_maps_key)); //AIzaSyBo6CVkO8YOfq3eqRgaLSrQ5PEARPKBtyA

        Object[] dataTransfer = new Object[3];
        dataTransfer[0] = mMap;
        dataTransfer[1] = sb.toString();
        dataTransfer[2] = fab2;


        GetPlaceData getPlaceData = new GetPlaceData(getApplicationContext());
        getPlaceData.execute(dataTransfer);

    }

    public void stopNavigation()
    {

        navigating = false;
        fab.setImageResource(R.drawable.round_navigation_white_36);
        fab2.setImageResource(R.drawable.round_navigation_white_36);
        fab.setVisibility(View.INVISIBLE);
        fab2.setVisibility(View.INVISIBLE);
        textInstructions.setText("");
        textSpeed.setText("");
        mMap.clear();
        destination = null;
        getDeviceLocation();
        removeRenderable();

    }

    public void startNavigation()
    {

    }


    public static boolean getNavigating()
    {
        return navigating;
    }

    public static LatLng getOrigin() { return origin; }

    public static LatLng getDestination()
    {
        return destination;
    }

    public static CameraPosition getCurrentMapPos()
    {
        return mMap.getCameraPosition();
    }

}

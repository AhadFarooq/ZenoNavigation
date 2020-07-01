package com.example.zenonavigation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {


    private static DecimalFormat df2 = new DecimalFormat("#.##");
    private static DecimalFormat df6 = new DecimalFormat("#.######");
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1000;
    private static boolean mLocationPermissionGranted;
    private boolean gps_enabled = false;
    private boolean network_enabled = false;
    private static boolean navigating = false;
    private boolean expanded = false;
    private boolean nightVisionOn = false;
    private static boolean hybridMap = false;

    private FrameLayout arLayout;
    private ArFragment arFragment;

    private FrameLayout popupLayout;

    private FrameLayout nightVision;

    private static GoogleMap mMap;
    private static LatLng origin;
    private static LatLng destination;
    private Marker marker;

    private static String travelMode = "driving";

    private FloatingActionButton buttonMyLocation;
    private FloatingActionButton buttonMapLayer;
    private ImageButton buttonExpand;
    private FloatingActionButton fab;
    private FloatingActionButton fab2;
    private FloatingActionButton fabCancel;
    private FloatingActionButton fabTravelMode;
    private SearchView searchView;

    private ImageButton buttonDetect;
    private ImageButton buttonNightVision;
    private TextView textNightVision;

    private TextView textSpeed;
    private TextView textUnit;
    private TextView textInstructions;

    private TextView textCoordinates;
    private TextView textAddress;


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

    private int i = 0;


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
         Layouts */


        arLayout = findViewById(R.id.arLayout);

        nightVision = findViewById(R.id.nightVision);

        popupLayout = findViewById(R.id.popupLayout);


        /**
         *
         *
         *
         AR */


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


        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {

                Frame frame = arFragment.getArSceneView().getArFrame();
                Camera camera = frame.getCamera();
                cameraPose = camera.getDisplayOrientedPose().compose(Pose.makeTranslation(0, -1, -5f));

                try {
                    if (anchorNode != null) {
                        objectPose = anchor.getPose();
                        currentCameraPose = camera.getDisplayOrientedPose();

                        float dx = objectPose.tx() - currentCameraPose.tx();
                        float dy = objectPose.ty() - currentCameraPose.ty();
                        float dz = objectPose.tz() - currentCameraPose.tz();

                        float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    }
                } catch (Exception e) {
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

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {

                        origin = new LatLng(location.getLatitude(), location.getLongitude());

                        if (navigating) {

                            float speed = location.getSpeed();
                            textSpeed.setText(df2.format(speed));
                            textUnit.setText("m/s");


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


                            if (target_location != null) {


                                distance = new double[target_location.length];


                                for (int i = 0; i < target_location.length; i++) {


                                    double totalDistance = SphericalUtil.computeDistanceBetween(origin, destination);
                                    if (totalDistance < 5) {
                                        stopNavigation();
                                        break;
                                    }


                                    distance[i] = SphericalUtil.computeDistanceBetween(origin, target_location[i]);

                                    if (distance[i] <= 10) {

                                        if (maneuver[i] == null) {
                                            removeRenderable();
                                            addRenderable(straight);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        } else if (maneuver[i].equals("turn-slight-left")) {
                                            removeRenderable();
                                            addRenderable(slight_left);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        } else if (maneuver[i].equals("uturn-left")) {
                                            removeRenderable();
                                            addRenderable(uturn_left);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        } else if (maneuver[i].equals("turn-left")) {
                                            removeRenderable();
                                            addRenderable(left);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        } else if (maneuver[i].equals("turn-slight-right")) {
                                            removeRenderable();
                                            addRenderable(slight_right);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        } else if (maneuver[i].equals("uturn-right")) {
                                            removeRenderable();
                                            addRenderable(uturn_right);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        } else if (maneuver[i].equals("turn-right")) {
                                            removeRenderable();
                                            addRenderable(right);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        } else if (maneuver[i].equals("straight")) {
                                            removeRenderable();
                                            addRenderable(straight);
                                            textInstructions.setText(Html.fromHtml(instructions[i], Html.FROM_HTML_MODE_COMPACT));
                                            break;
                                        }

                                    } else {

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
        textUnit = findViewById(R.id.textUnit);
        textInstructions = findViewById(R.id.textInstructions);
        textNightVision = findViewById(R.id.textNightVision);
        textCoordinates = findViewById(R.id.textCoordinates);
        textAddress = findViewById(R.id.textAddress);


        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navigating) {
                    //end navigation
                    stopNavigation();

                } else {
                    //start navigation
                    contractMap();
                    popupLayout.setBackgroundColor(getResources().getColor(R.color.full_transparent, null));
                    textCoordinates.setVisibility(View.INVISIBLE);
                    textAddress.setVisibility(View.INVISIBLE);
                    fabCancel.setVisibility(View.INVISIBLE);
                    fabTravelMode.setVisibility(View.VISIBLE);
                    fab.setImageResource(R.drawable.round_close_white_36);
                    searchView.setVisibility(View.INVISIBLE);
                    requestDirections(origin, destination, travelMode);
                    getDeviceLocation();
                    navigating = true;
                }
            }
        });


        fab2 = findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navigating) {
                    //end navigation
                    stopNavigation();

                } else {
                    //start navigation
                    Place p = new Place();
                    destination = Place.getPlace();

                    contractMap();
                    popupLayout.setBackgroundColor(getResources().getColor(R.color.full_transparent, null));
                    textCoordinates.setVisibility(View.INVISIBLE);
                    textAddress.setVisibility(View.INVISIBLE);
                    fabCancel.setVisibility(View.INVISIBLE);
                    fabTravelMode.setVisibility(View.VISIBLE);
                    fab2.setImageResource(R.drawable.round_close_white_36);
                    searchView.setVisibility(View.INVISIBLE);
                    requestDirections(origin, destination, travelMode);
                    getDeviceLocation();
                    navigating = true;
                }
            }
        });


        fabCancel = findViewById(R.id.fabCancel);
        fabCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupLayout.setVisibility(View.INVISIBLE);
                fab.setVisibility(View.INVISIBLE);
                fab2.setVisibility(View.INVISIBLE);
                mMap.clear();
            }
        });


        fabTravelMode = findViewById(R.id.fabTravelMode);
        fabTravelMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                marker = mMap.addMarker(new MarkerOptions().position(destination));

                if (travelMode.equals("driving")) {
                    travelMode = "walking";
                    fabTravelMode.setImageResource(R.drawable.round_directions_walk_black_36);
                } else {
                    travelMode = "driving";
                    fabTravelMode.setImageResource(R.drawable.round_directions_car_black_36);
                }


                requestDirections(origin, destination, travelMode);
            }
        });


        buttonExpand = findViewById(R.id.buttonExpand);
        buttonExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expanded) {
                    contractMap();
                } else {
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
                enableMyLocation();
            }
        });


        buttonMapLayer = findViewById(R.id.buttonMapLayer);
        buttonMapLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hybridMap) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    hybridMap = true;
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    hybridMap = false;
                }
            }
        });


        buttonDetect = findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocationPermissionGranted) {
                    Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                    startActivity(intent);
                }
            }
        });


        buttonNightVision = findViewById(R.id.buttonNightVision);
        buttonNightVision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!nightVisionOn) {
                    nightVision.setBackgroundColor(getResources().getColor(R.color.nightVision_transparent, null));
                    textNightVision.setTextColor(getResources().getColor(R.color.colorAccent, null));
                    nightVisionOn = true;
                } else {
                    nightVision.setBackgroundColor(getResources().getColor(R.color.full_transparent, null));
                    textNightVision.setTextColor(getResources().getColor(R.color.colorWhite, null));
                    nightVisionOn = false;
                }
            }
        });


        //SEARCH VIEW

        searchView = findViewById(R.id.searchView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                checkGpsPermission();
                if (mLocationPermissionGranted && (gps_enabled || network_enabled)) {
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            fullScreen();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        fullScreen();

        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);

    }

    public void fullScreen()
    {
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
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
        mMap.setBuildingsEnabled(true);

        checkGpsPermission();
        enableMyLocation();

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

                    try {
                        String address = getAddress(point.latitude, point.longitude);
                        textAddress.setText(address.substring(0, 21)+"...");
                        textCoordinates.setText(df6.format(point.latitude)+" , "+df6.format(point.longitude));
                    } catch (Exception e){}

                    popupLayout.setVisibility(View.VISIBLE);
                    fabCancel.setVisibility(View.VISIBLE);
                    textCoordinates.setVisibility(View.VISIBLE);
                    textAddress.setVisibility(View.VISIBLE);
                }
            }

        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                return false;
            }
        });
    }



    private void enableMyLocation() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mLocationPermissionGranted = true;
                mMap.setMyLocationEnabled(true);
                mMap.setTrafficEnabled(true);
                getDeviceLocation();
            }
        } else {
            mLocationPermissionGranted = false;
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode != PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            return;
        }

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            enableMyLocation();

            Intent intent = new Intent(this, MainActivity.class);
            this.startActivity(intent);
            this.finishAffinity();

        }
        else {
            mLocationPermissionGranted = false;
            enableMyLocation();
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

    public void requestDirections(LatLng origin, LatLng destination, String travelMode)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("https://maps.googleapis.com/maps/api/directions/json?");
        sb.append("origin="+origin.latitude+","+origin.longitude);
        sb.append("&destination="+destination.latitude+","+destination.longitude);
        sb.append("&departure_time=now");
        sb.append("&mode="+travelMode);
        sb.append("&key="+getString(R.string.google_maps_key));

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
        sb.append("&key="+getString(R.string.google_maps_key));

        Object[] dataTransfer = new Object[7];
        dataTransfer[0] = mMap;
        dataTransfer[1] = sb.toString();
        dataTransfer[2] = fab2;
        dataTransfer[3] = popupLayout;
        dataTransfer[4] = fabCancel;
        dataTransfer[5] = textCoordinates;
        dataTransfer[6] = textAddress;


        GetPlaceData getPlaceData = new GetPlaceData(getApplicationContext());
        getPlaceData.execute(dataTransfer);

    }

    public void stopNavigation()
    {

        navigating = false;
        mMap.clear();

        popupLayout.setVisibility(View.INVISIBLE);
        popupLayout.setBackgroundColor(getResources().getColor(R.color.colorBlack, null));
        fabTravelMode.setVisibility(View.INVISIBLE);

        fab.setImageResource(R.drawable.round_navigation_white_36);
        fab2.setImageResource(R.drawable.round_navigation_white_36);
        fab.setVisibility(View.INVISIBLE);
        fab2.setVisibility(View.INVISIBLE);
        searchView.setVisibility(View.VISIBLE);
        textInstructions.setText("");
        textSpeed.setText("");
        textUnit.setText("");
        destination = null;
        getDeviceLocation();
        removeRenderable();

    }

    public String getAddress(double lat, double lng) {

        String addStr = "";

        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(lat, lng, 1);
            Address address = addressList.get(0);
            addStr += address.getAddressLine(0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return addStr;
    }

    public static String getTravelMode()
    {
        return travelMode;
    }

    public static boolean gethybridMap()
    {
        return hybridMap;
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

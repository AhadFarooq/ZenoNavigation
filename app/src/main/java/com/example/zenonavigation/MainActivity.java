package com.example.zenonavigation;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.zenonavigation.customview.OverlayView;
import com.example.zenonavigation.env.BorderedText;
import com.example.zenonavigation.env.ImageUtils;
import com.example.zenonavigation.env.Logger;
import com.example.zenonavigation.maps.GetDirectionsData;
import com.example.zenonavigation.maps.GetPlaceData;
import com.example.zenonavigation.maps.Place;
import com.example.zenonavigation.tflite.Classifier;
import com.example.zenonavigation.tflite.TFLiteObjectDetectionAPIModel;
import com.example.zenonavigation.tracking.MultiBoxTracker;
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
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        OnImageAvailableListener
        , Camera.PreviewCallback
{



  private static final String TAG = "";
  private static androidx.fragment.app.Fragment fragment2;
  private static Fragment fragment;

  private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 10;
  private static boolean mLocationPermissionGranted;
  private boolean gps_enabled = false;
  private boolean network_enabled = false;
  private boolean navigating = false;
  private boolean expanded = false;

  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private boolean debug = false;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(800, 600);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;


  private LocationRequest locationRequest;
  private LocationCallback locationCallback;
  private FusedLocationProviderClient fusedLocationProviderClient;


  private GoogleMap mMap;
  private LatLng origin;
  private LatLng destination;

  private Marker marker;

  private FrameLayout arLayout;

  private Button buttonRender;

  private ImageButton buttonMyLocation;
  private ImageButton buttonExpand;

  private SearchView searchView;

  private FloatingActionButton fab;
  private FloatingActionButton fab2;

  private Switch switchDetection;

  private ArFragment arFragment;

  private ModelRenderable arrowRenderable;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tfe_od_activity_camera);

    /**
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
                          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
      }
    });
     **/

    arLayout = findViewById(R.id.arLayout);

    fragment2 = new ArFragment();

    arFragment = (ArFragment) fragment2;










    /**
     *
     *
     *
     Switch */

    switchDetection = findViewById(R.id.switchDetection);
    switchDetection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mLocationPermissionGranted)
        {
          if (isChecked) {
            // toggle enabled
            getSupportFragmentManager().beginTransaction().remove(fragment2).commit();
            if (hasPermission()) {
              setFragment();
              switchDetection.setTextColor(getResources().getColor(R.color.colorAccent, null));
            }
            else {
              requestPermission();
              switchDetection.setChecked(false);
              switchDetection.setTextColor(getResources().getColor(R.color.colorWhite, null));
            }
          }
          else {
            // toggle disabled
            getFragmentManager().beginTransaction().remove(fragment).commit();
            getSupportFragmentManager().beginTransaction().replace(R.id.arLayout, fragment2).commit();
            switchDetection.setTextColor(getResources().getColor(R.color.colorWhite, null));
          }
        }
      }
    });









    /**
     *
     *
     *
     searchView */

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












    /**
     *
     *
     *
     Buttons */

    fab = findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (navigating)
        {
          //end navigation
          navigating = false;
          fab.setImageResource(R.drawable.round_navigation_white_36);
          fab.setVisibility(View.INVISIBLE);

          mMap.clear();
          destination = null;
          getDeviceLocation();
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
          navigating = false;
          fab2.setImageResource(R.drawable.round_navigation_white_36);
          fab2.setVisibility(View.INVISIBLE);

          mMap.clear();
          destination = null;
          getDeviceLocation();
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
        getDeviceLocation();
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

            //location.getSpeed();

            origin = new LatLng(location.getLatitude(), location.getLongitude());

            if (navigating)
            {
              CameraPosition oldPos = mMap.getCameraPosition();
              CameraPosition newPos = CameraPosition.builder(oldPos)
                      .bearing(location.getBearing())
                      .tilt(55)
                      .build();
              mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newPos));
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
     AR */

    ModelRenderable.builder()
            .setSource(this, R.raw.arrow)
            .build()
            .thenAccept(modelRenderable -> arrowRenderable = modelRenderable)
            .exceptionally(
                    throwable -> {
                      Log.e(TAG, "Unable to load Renderable.", throwable);
                      return null;
                    });

    buttonRender = findViewById(R.id.buttonRender);
    buttonRender.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Session session = arFragment.getArSceneView().getSession();
        float[] pos = { 0, 0, -1 };
        float[] rotation = { 0, 0, 0, 1 };
        Anchor anchor =  session.createAnchor(new Pose(pos, rotation));
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setRenderable(arrowRenderable);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
      }
    });





  }









  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    if(hasFocus) {

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

    }
  }

















  //Maps and Navigation
  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    mMap.getUiSettings().setMyLocationButtonEnabled(false);
    mMap.getUiSettings().setMapToolbarEnabled(false);
    mMap.getUiSettings().setCompassEnabled(false);

    checkGpsPermission();
    getLocationPermission();


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

  private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION) + ContextCompat
            .checkSelfPermission(this.getApplicationContext(),
                    Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
      mLocationPermissionGranted = true;
      updateLocationUI();
    } else {
      ActivityCompat.requestPermissions(this,
              new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
              PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }
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
        }
      }
    }
    updateLocationUI();
  }

  private void updateLocationUI() {
    if (mMap == null) {
      return;
    }
    try {
      if (mLocationPermissionGranted) {
        mMap.setMyLocationEnabled(true);
        getSupportFragmentManager().beginTransaction().replace(R.id.arLayout, fragment2).commit();
        getDeviceLocation();
      } else {
        mMap.setMyLocationEnabled(false);
        getLocationPermission();
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
    switchDetection.setVisibility(View.INVISIBLE);
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
    switchDetection.setVisibility(View.VISIBLE);
    buttonExpand.setImageResource(R.drawable.round_expand_less_black_24);
  }

  public void requestDirections(LatLng origin, LatLng destination)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("https://maps.googleapis.com/maps/api/directions/json?");
    sb.append("origin="+origin.latitude+","+origin.longitude);
    sb.append("&destination="+destination.latitude+","+destination.longitude);
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




























//Camera and Detection

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  /**
  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }
   **/

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermission() {
    if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
      Toast.makeText(
              MainActivity.this,
              "Camera permission is required",
              Toast.LENGTH_LONG)
          .show();
    }
    requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    //Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
              new CameraConnectionFragment.ConnectionCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                  previewHeight = size.getHeight();
                  previewWidth = size.getWidth();
                  MainActivity.this.onPreviewSizeChosen(size, rotation);
                }
              },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;

    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }


  //detector

  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
              TFLiteObjectDetectionAPIModel.create(
                      getAssets(),
                      TF_OD_API_MODEL_FILE,
                      TF_OD_API_LABELS_FILE,
                      TF_OD_API_INPUT_SIZE,
                      TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing classifier!");
      Toast toast =
              Toast.makeText(
                      getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

    frameToCropTransform =
            ImageUtils.getTransformationMatrix(
                    previewWidth, previewHeight,
                    cropSize, cropSize,
                    sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
            new OverlayView.DrawCallback() {
              @Override
              public void drawCallback(final Canvas canvas) {
                tracker.draw(canvas);
                if (isDebug()) {
                  tracker.drawDebug(canvas);
                }
              }
            });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }


  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
            new Runnable() {
              @Override
              public void run() {
                LOGGER.i("Running detection on image " + currTimestamp);
                final long startTime = SystemClock.uptimeMillis();
                final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final Canvas canvas = new Canvas(cropCopyBitmap);
                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);

                float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                switch (MODE) {
                  case TF_OD_API:
                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    break;
                }

                final List<Classifier.Recognition> mappedRecognitions =
                        new LinkedList<Classifier.Recognition>();

                for (final Classifier.Recognition result : results) {
                  final RectF location = result.getLocation();
                  if (location != null && result.getConfidence() >= minimumConfidence) {
                    canvas.drawRect(location, paint);

                    cropToFrameTransform.mapRect(location);

                    result.setLocation(location);
                    mappedRecognitions.add(result);
                  }
                }

                tracker.trackResults(mappedRecognitions, currTimestamp);
                trackingOverlay.postInvalidate();

                computingDetection = false;

                runOnUiThread(
                        new Runnable() {
                          @Override
                          public void run() {
                            //showFrameInfo(previewWidth + "x" + previewHeight);
                            //showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                            //showInference(lastProcessingTimeMs + "ms");
                          }
                        });
              }
            });
  }


  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }


  protected Size getDesiredPreviewFrameSize() {
    //View container = findViewById(R.id.container);
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }





}



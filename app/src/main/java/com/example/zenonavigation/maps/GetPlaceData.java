package com.example.zenonavigation.maps;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class GetPlaceData extends AsyncTask<Object, String ,String> {


    private static DecimalFormat df6 = new DecimalFormat("#.######");

    GoogleMap mMap;
    String url;
    FloatingActionButton fab2;
    FrameLayout popupLayout;
    FloatingActionButton fabCancel;
    TextView textCoordinates;
    TextView textAddress;

    double lat;
    double lng;
    public LatLng location;

    HttpURLConnection httpURLConnection = null;
    String data = "";
    InputStream inputStream = null;
    Context c;

    public GetPlaceData(){}

    public GetPlaceData(Context c)
    {
        this.c = c;
    }

    @Override
    protected String doInBackground(Object... params) {
        mMap = (GoogleMap) params[0];
        url = (String) params[1];
        fab2 = (FloatingActionButton) params[2];
        popupLayout = (FrameLayout) params[3];
        fabCancel = (FloatingActionButton) params[4];
        textCoordinates = (TextView) params[5];
        textAddress = (TextView) params[6];

        try {
            URL myUrl = new URL(url);
            httpURLConnection = (HttpURLConnection) myUrl.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            bufferedReader.close();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    @Override
    protected void onPostExecute(String s) {

        try {
            JSONObject jsonObject = new JSONObject(s);

            JSONArray results = jsonObject.getJSONArray("results");

            if (results.length()>0)
            {
                lat = jsonObject.getJSONArray("results").getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location").getDouble("lat");

                lng = jsonObject.getJSONArray("results").getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location").getDouble("lng");


                location = new LatLng(lat, lng);

                Place.setPlace(location);

                mMap.addMarker(new MarkerOptions().position(location));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 17));

                try {
                    String address = getAddress(location.latitude, location.longitude);
                    textAddress.setText(address.substring(0, 21)+"...");
                    textCoordinates.setText(df6.format(location.latitude)+" , "+df6.format(location.longitude));
                } catch (Exception e){}

                fab2.setVisibility(View.VISIBLE);
                popupLayout.setVisibility(View.VISIBLE);
                fabCancel.setVisibility(View.VISIBLE);
                textCoordinates.setVisibility(View.VISIBLE);
                textAddress.setVisibility(View.VISIBLE);

            }
            else
            {
                Place.setPlace(null);
                fab2.setVisibility(View.INVISIBLE);
                Toast.makeText(c, "No Result", Toast.LENGTH_SHORT).show();
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getAddress(double lat, double lng) {

        String addStr = "";

        Geocoder geocoder = new Geocoder(c, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(lat, lng, 1);
            Address address = addressList.get(0);
            addStr += address.getAddressLine(0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return addStr;
    }

}

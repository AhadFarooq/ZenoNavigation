package com.example.zenonavigation.maps;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.zenonavigation.MainActivity;
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

public class GetPlaceData extends AsyncTask<Object, String ,String> {

    private static final String TAG = "myPlace";
    GoogleMap mMap;
    String url;
    FloatingActionButton fab2;

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

        Place p = new Place();

        try {
            JSONObject jsonObject = new JSONObject(s);

            JSONArray results = jsonObject.getJSONArray("results");

            Log.d(TAG, "place count: "+results.length());

            if (results.length()>0)
            {
                lat = jsonObject.getJSONArray("results").getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location").getDouble("lat");

                lng = jsonObject.getJSONArray("results").getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location").getDouble("lng");


                location = new LatLng(lat, lng);

                p.setPlace(location);

                mMap.addMarker(new MarkerOptions().position(location));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 17));

                fab2.setVisibility(View.VISIBLE);

                //Log.d(TAG, "GetPlaceData: "+location);

            }
            else
            {
                p.setPlace(null);
                fab2.setVisibility(View.INVISIBLE);
                Toast.makeText(c, "No Result", Toast.LENGTH_SHORT).show();
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

}

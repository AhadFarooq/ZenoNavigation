package com.example.zenonavigation.maps;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

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
import java.util.Arrays;
import java.util.List;

public class GetDirectionsData extends AsyncTask<Object, String ,String> {

    static GoogleMap mMap;
    String url;

    HttpURLConnection httpURLConnection = null;
    String data = "";
    InputStream inputStream = null;
    Context c;

    static String[] polylines;
    static String[] instructions;
    static String[] maneuver;
    static LatLng[] start_location;
    static Double[] start_lat;
    static Double[] start_lng;

    static int stepcount;

    public GetDirectionsData(Context c)
    {
        this.c = c;
    }

    @Override
    protected String doInBackground(Object... params) {
        mMap = (GoogleMap) params[0];
        url = (String) params[1];

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

            JSONArray steps = jsonObject.getJSONArray("routes").getJSONObject(0)
                    .getJSONArray("legs").getJSONObject(0).getJSONArray("steps");

            stepcount = steps.length();

            polylines = new String[stepcount];
            start_lat = new Double[stepcount];
            start_lng = new Double[stepcount];
            start_location = new LatLng[stepcount];
            instructions = new String[stepcount];
            maneuver = new String[stepcount];

            for (int i=0; i<stepcount; i++) {

                polylines[i] = steps.getJSONObject(i).getJSONObject("polyline").getString("points");


                start_lat[i] = steps.getJSONObject(i).getJSONObject("start_location").getDouble("lat");
                start_lng[i] = steps.getJSONObject(i).getJSONObject("start_location").getDouble("lng");

                start_location[i] = new LatLng(start_lat[i], start_lng[i]);


                if (steps.getJSONObject(i).has("html_instructions")) {
                    instructions[i] = steps.getJSONObject(i).getString("html_instructions");
                }

                if (steps.getJSONObject(i).has("maneuver")) {
                    maneuver[i] = steps.getJSONObject(i).getString("maneuver");
                }

            }



            //Send to Directions Class
            //------------------------

            Directions.setStartLocation(start_location);
            Directions.setManeuver(maneuver);
            Directions.setInstructions(instructions);

            //------------------------



            int polylinecount = polylines.length;

            PatternItem DOT = new Dot();
            PatternItem GAP = new Gap(10);
            List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);

            for (int i=0; i<polylinecount; i++) {
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.pattern(PATTERN_POLYLINE_DOTTED);
                polylineOptions.color(Color.MAGENTA);
                polylineOptions.width(30);
                polylineOptions.addAll(PolyUtil.decode(polylines[i]));

                mMap.addPolyline(polylineOptions);
            }

        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

}

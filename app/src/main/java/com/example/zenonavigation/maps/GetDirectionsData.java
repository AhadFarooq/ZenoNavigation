package com.example.zenonavigation.maps;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.zenonavigation.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.ButtCap;
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
import java.security.spec.ECField;
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

    static int stepcount;
    static int count;

    static ImageView imageView;

    public GetDirectionsData (final ImageView imageView) {
        this.imageView = imageView;
        imageView.setImageResource(android.R.color.transparent);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDirections();
            }
        });
    }

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
            instructions = new String[stepcount];
            maneuver = new String[stepcount];

            for (int i=0; i<stepcount; i++) {

                polylines[i] = steps.getJSONObject(i).getJSONObject("polyline").getString("points");

                if (steps.getJSONObject(i).has("html_instructions")) {
                    instructions[i] = steps.getJSONObject(i).getString("html_instructions");
                }

                if (steps.getJSONObject(i).has("maneuver")) {
                    maneuver[i] = steps.getJSONObject(i).getString("maneuver");
                }

                Log.d("owman", i+", "+instructions[i]);
                Log.d("owman", i+", "+maneuver[i]);

            }


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

            count = 1;
            getDirections();

        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getDirections() {

        try{
            if (count<stepcount) {

                if (maneuver[count].equals("turn-slight-left")) {
                    imageView.setImageResource(R.drawable.slight_left);
                }
                else if (maneuver[count].equals("uturn-left")) {
                    imageView.setImageResource(R.drawable.uturn_left);
                }
                else if (maneuver[count].equals("turn-left")) {
                    imageView.setImageResource(R.drawable.left);
                }
                else if (maneuver[count].equals("turn-slight-right")) {
                    imageView.setImageResource(R.drawable.slight_right);
                }
                else if (maneuver[count].equals("uturn-right")) {
                    imageView.setImageResource(R.drawable.uturn_right);
                }
                else if (maneuver[count].equals("turn-right")) {
                    imageView.setImageResource(R.drawable.right);
                }
                else if (maneuver[count].equals("straight")) {
                    imageView.setImageResource(R.drawable.straight);
                }
                else if (maneuver[count].equals("null")) {
                    imageView.setImageResource(android.R.color.transparent);
                }
            }
            else{
                mMap.clear();
                imageView.setImageResource(android.R.color.transparent);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        count++;

    }

}

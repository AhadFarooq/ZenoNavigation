package com.example.zenonavigation.maps;

import com.google.android.gms.maps.model.LatLng;

public class Place {

    public static LatLng place;

    public Place(){}

    public void setPlace(LatLng place) {
        Place.place = place;
    }

    public LatLng getPlace() {
        return place;
    }

}
package com.example.zenonavigation.maps;

import com.google.android.gms.maps.model.LatLng;

public class Directions {

    static String[] instructions;
    static String[] maneuver;
    static LatLng[] start_location;

    public Directions(){}

    public static void setStartLocation(LatLng[] start_location) {
        Directions.start_location = null;
        Directions.start_location = start_location;
    }

    public static void setManeuver(String[] maneuver) {
        Directions.maneuver = null;
        Directions.maneuver = maneuver;
    }

    public static void setInstructions(String[] instructions) {
        Directions.instructions = null;
        Directions.instructions = instructions;
    }

    public static LatLng[] getStartLocation() {
        return start_location;
    }

    public static String[] getManeuver() {
        return maneuver;
    }

    public static String[] getInstructions() {
        return instructions;
    }

}

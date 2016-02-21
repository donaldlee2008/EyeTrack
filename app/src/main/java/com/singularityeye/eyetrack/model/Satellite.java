package com.singularityeye.eyetrack.model;

import java.io.Serializable;

/**
 * Satellite wrapper.
 * This class includes last satellite position
 * @author Manuel Martin-Gonzalez
 * @version 1.0.0-alpha
 */
public class Satellite implements Serializable{

    private String NORAD_ID; // id
    private String shortname; // platform name
    private double latitude; // decimal degrees
    private double longitude; // decimal degrees
    private double altitude; // kilometers

    /**
     * Constructor method for a satellite model
     */
    public Satellite() {}

    /**
     * Get the unique identifier for the satellite
     * @return string that holds the unique identifier for the satellite
     */
    public String getNORAD_ID() {
        return NORAD_ID;
    }

    /**
     * Set the unique identifier for the satellite
     * @param NORAD_ID string that holds the unique identifier for the satellite
     */
    public void setNORAD_ID(String NORAD_ID) {
        this.NORAD_ID = NORAD_ID;
    }

    /**
     * Get the name of the satellite
     * @return string that holds the name of the satellite
     */
    public String getShortname() {
        return shortname;
    }

    /**
     * Set the name of the satellite
     * @param shortname string that holds the name of the satellite
     */
    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    /**
     * Get latitude degrees for the position of the satellite
     * @return decimal number that represents latitude degrees for the position of the satellite
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Set latitude degrees for the position of the satellite
     * @param latitude decimal number that represents latitude degrees for the position of the satellite
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Get longitude degrees for the position of the satellite
     * @return decimal number that represents longitude degrees for the position of the satellite
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Set longitude degrees for the position of the satellite
     * @param longitude decimal number that represents longitude degrees for the position of the satellite
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Get kilometers for the altitude of the satellite
     * @return decimal number that represents the altitude of the satellite
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * Set kilometers for the altitude of the satellite
     * @param altitude decimal number that represents the altitude of the satellite
     */
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

}

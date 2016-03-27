package com.d4dl.mesh;


import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 8/11/14
 * Time: 7:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class LatLng implements java.lang.Comparable {
    private double lat;
    private double lng;
    private int hashCode;
    private LatLng derivedFrom;



    public LatLng(double lat, double lng, LatLng derivedFrom) {
        this(lat, lng);
        this.derivedFrom = derivedFrom;
    }

    public LatLng(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
        generatedHashCode();
    }

    private void generatedHashCode() {
        long bits = 1L;
        bits = 31L * bits + (long)VecMathUtil.floatToIntBits((float)lat);
        bits = 31L * bits + (long)VecMathUtil.floatToIntBits((float)lng);
        this.hashCode = (int) (bits ^ (bits >> 32));
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }


    public void setLng(double lng) {
        this.lng = lng;
    }


    public boolean equals(Object otherObject) {
        if(otherObject == null) {
            return false;
        }
        return otherObject.getClass() == LatLng.class &&
               ((LatLng)otherObject).getLat() == this.getLat() &&
               ((LatLng)otherObject).getLng() == this.getLng();
    }

    public int hashCode() {
        return hashCode;
    }

    public int compareTo(Object o) {
        if(!o.getClass().isAssignableFrom(LatLng.class)) {
            return 0;
        }
        LatLng other = (LatLng)o;
        if(this.lat == other.getLat() && this.lng == other.getLng()) {
            return 0;
        } else if(this.lat > other.getLat()) {
            return 1;
        } else if(this.lng > other.getLng()) {
            return 1;
        } else {
            return -1;
        }
    }

    public Coordinate getCoordinate() {
        return new Coordinate(this.getLat(), this.getLng(), 0);
    }

    public String toString() {
        return "(" + lat + ", " + lng + ")";
    }

    public LatLng getDerivedFrom() {
        return derivedFrom;
    }

    public void setDerivedFrom(LatLng derivedFrom) {
        this.derivedFrom = derivedFrom;
    }
}

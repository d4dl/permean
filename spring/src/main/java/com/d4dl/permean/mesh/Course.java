package com.d4dl.permean.mesh;

/**
 * Created by joshuadeford on 5/30/17.
 */
public class Course {

    //course.a
    //the heading at pos1 in radians clockwise from the local meridian
    private final double heading;
    //course.d
    //the distance traveled
    private final double distance;

    public Course(double distance, double heading) {
        this.distance = distance;
        this.heading = heading;
    }

    public double getDistance() {
        return distance;
    }

    public double getHeading() {
        return heading;
    }
}

package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Vertex;
import com.d4dl.permean.mesh.positiontree.LatComparable;

import java.io.Serializable;

import static java.lang.StrictMath.*;

/**
 * Created by joshuadeford on 5/30/17.
 */
public class Position implements Serializable, LatComparable {
    public static final double π = PI;
    public static final double LAT_CONVERT = 90 * 2/π;
    public static final double LNG_CONVERT = 180/PI;
    private Vertex vertex;
    private int vertexIndex;

    //Latitude (elevation angle) the angle in radians between the equatorial plane and the straight line that passes
    //through that point and through (or close to) the center of the Earth
    private final double φ;

    //Longitude (azimuthal angle) the angle in radians east or west of a reference meridian to another meridian that
    //passes through that point
    private final double λ;

    public Position(double φ, double λ) {
        this.φ = φ;
        this.λ =  λ;
    }

    /**
     * Returns the position halfway between this position and another.
     * Chiefly used to test `interpolate`.
     */
    public Position midpoint(Position pos2) {
        double Bx = Math.cos(pos2.φ) * Math.cos(pos2.λ - this.λ);
        double By = Math.cos(pos2.φ) * Math.sin(pos2.λ - this.λ);

        return new Position(
                        atan2(sin(this.φ) + sin(pos2.φ), sqrt((cos(this.φ) + Bx) * (cos(this.φ) + Bx) + By * By)),
                        this.λ + atan2(By, cos(this.φ) + Bx));
    }

    /**
     * Returns the arc length between this and another position.
     *
     * @return {number} arc length
     */
    public double distance(Position other) {
        return 2 * asin(sqrt(
                pow(sin((this.φ - other.φ) / 2), 2) +
                cos(this.φ) * cos(other.φ) * pow(sin((this.λ - other.λ) / 2), 2)
        ));
    }

    /**
     * Returns the course between this position and another.
     *
     * @returns Course course
     */
    public Course course(Position other) {
        double distance = this.distance(other);
        double heading;

        if (sin(other.λ - this.λ) < 0) {
            heading = acos((sin(other.φ) - sin(this.φ) * cos(distance)) / (sin(distance) * cos(this.φ)));
        } else {
            heading = 2 * π - acos((sin(other.φ) - sin(this.φ) * cos(distance)) / (sin(distance) * cos(this.φ)));
        }

        return new Course(distance, heading);
    }


    /**
     * Populates buffer `buf` with `d - 1` evenly-spaced positions between two points.
     *
     * @param other second position
     * @param {number} d - number of times to divide the segment between the positions
     * @param {ArrayBuffer} buf - buffer in which to store the result
     */
    public void interpolate(Position other, double d, double[] buf) {
        double Δ = this.distance(other);
        for (int i = 1; i < d; i += 1) {
            double f = i / d;

            double A = sin((1 - f) * Δ) / sin(Δ);
            double B = sin(f * Δ) / sin(Δ);

            double x = A * cos(this.φ) * cos(this.λ) + B * cos(other.φ) * cos(other.λ);
            double z = A * cos(this.φ) * sin(this.λ) + B * cos(other.φ) * sin(other.λ);
            double y = A * sin(this.φ) + B * sin(other.φ);

            double φ = atan2(y, sqrt(pow(x, 2) + pow(z, 2)));
            double λ = atan2(z, x);

            buf[2 * (i - 1) + 0] = φ;
            buf[2 * (i - 1) + 1] = λ;
        }
    }

    /**
     * Returns the center of the triangle formed by this point and the other 2
     */
    public Position centroid(int index, Position other1, Position other2) {
        Position[] triangle = new Position[]{this, other1, other2};
        int n = triangle.length;
        double sum_x = 0;
        double sum_z = 0;
        double sum_y = 0;
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < n; i += 1) {
            Position current = triangle[i];
            double i_φ = current.getφ();
            double i_λ = current.getλ();

            buffer.append("[").append(i_φ).append(", ");
            buffer.append(i_λ).append("]");

            sum_x += cos(i_φ) * cos(i_λ);
            sum_z += cos(i_φ) * sin(i_λ);
            sum_y += sin(i_φ);
        }

        double x = sum_x / n;
        double z = sum_z / n;
        double y = sum_y / n;

        double r = sqrt(x * x + z * z + y * y);

        double φ = asin(y / r);
        double λ = atan2(z, x);

        //System.out.println("Creating centroid at " + index + " " + φ + ", " + λ + " from " + buffer);
        return new Position(φ, λ);
    }

    public double getλ() {
        return λ;
    }

    public double getφ() {
        return φ;
    }

    public double getLat() {
        //return 90 * (cos(φ) / π) * (φ < 0 ? -1 : 1);
        //return φ * UNIT;
        //return sin(φ) * 90;
        return 90 - (90 - (φ * LAT_CONVERT));
    }

    public double getLng() {
        return 180 * (λ / PI);
    }


    public Vertex getVertex() {
        return vertex;
    }

    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }

    /**
     * Either compareTo using lat or lng if alt is true.
     * @param o
     * @return
     */
    @Override
    public int compareLatTo(Object o) {
        int result = 0;
        Position other = (Position)o;
        double comparee = this.φ;
        double comparor = other.φ;
        if (comparee > comparor) {
            result = 1;
        } else if (comparee < comparor) {
            result = -1;
        }
        return result;
    }

    @Override
    public String toString() {
        return "Position{" +
                "φ=" + φ +
                ", λ=" + λ +
                "}\n";
    }
}

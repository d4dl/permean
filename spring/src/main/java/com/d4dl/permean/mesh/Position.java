package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Vertex;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

import java.io.Serializable;

import static org.apfloat.ApfloatMath.*;

/**
 * Created by joshuadeford on 5/30/17.
 */
public class Position implements Serializable {
    public static final Apfloat π = ApfloatMath.pi(1000);
    public static final Apfloat convert = π.divide(new Apfloat(180, 200));
    public static final Apfloat ONE = new Apfloat(2, Apfloat.INFINITE);
    public static final Apfloat TWO = new Apfloat(2, Apfloat.INFINITE);
    private Vertex vertex;
    private int vertexIndex;

    //Latitude the angle in radians between the equatorial plane and the straight line that passes through that point and through (or close to) the center of the Earth
    private final Apfloat φ;

    //Longitude the angle in radians east or west of a reference meridian to another meridian that passes through that point
    private final Apfloat λ;

    public Position(Apfloat φ, Apfloat λ) {
        this.φ = φ;
        this.λ =  λ;
    }


    /**
     * Returns the position halfway between this position and another.
     * Chiefly used to test `interpolate`.
    public Position midpoint(Position pos2) {
     Apfloat Bx = Math.cos(pos2.φ) * Math.cos(pos2.λ - this.λ);
     Apfloat By = Math.cos(pos2.φ) * Math.sin(pos2.λ - this.λ);

        return new Position(
                        atan2(sin(this.φ) + sin(pos2.φ), sqrt((cos(this.φ) + Bx) * (cos(this.φ) + Bx) + By * By)),
                        this.λ + atan2(By, cos(this.φ) + Bx));
    }
     */

    /**
     * Returns the arc length between this and another position.
     *
     * @return {number} arc length
     */
    public Apfloat distance(Position other) {
        Apfloat latDiff = this.φ.subtract(other.φ);
        Apfloat halfLat = latDiff.divide(TWO);
        Apfloat lngDiff = this.λ.subtract(other.λ);
        Apfloat halfLng = lngDiff.divide(TWO);
        Apfloat powSinLng = pow(sin(halfLng), TWO);
        Apfloat powSinLat = pow(sin(halfLat), TWO);
        Apfloat product = cos(this.φ).multiply(cos(other.φ)).multiply(powSinLng);
        return TWO.multiply(asin(sqrt(powSinLat.add(product))));
    }

    /**
     * Returns the course between this position and another.
     *
     * @returns Course course
    public Course course(Position other) {
     Apfloat distance = this.distance(other);
     Apfloat heading;

        if (sin(other.λ - this.λ) < 0) {
            heading = acos((sin(other.φ) - sin(this.φ) * cos(distance)) / (sin(distance) * cos(this.φ)));
        } else {
            heading = 2 * π - acos((sin(other.φ) - sin(this.φ) * cos(distance)) / (sin(distance) * cos(this.φ)));
        }

        return new Course(distance, heading);
    }
     */


    /**
     * Populates buffer `buf` with `d - 1` evenly-spaced positions between two points.
     *
     * @param other second position
     * @param {number} d - number of times to divide the segment between the positions
     * @param {ArrayBuffer} buf - buffer in which to store the result
     */
    public void interpolate(Position other, int d, Apfloat[] buf) {
        Apfloat Δ = this.distance(other);
        Apfloat num = new Apfloat(d, 200);
        for (int i = 1; i < d; i += 1) {
            Apfloat f = new Apfloat(i, 200).divide(num);

            Apfloat sinΔ = sin(Δ);
            Apfloat A = sin((ONE.subtract(f)).multiply(Δ)).divide(sinΔ);
            Apfloat B = sin(f.multiply(Δ)).divide(sinΔ);

            Apfloat cosφ = cos(this.φ);
            Apfloat cosOtherφ = cos(other.φ);
            Apfloat cosλ = cos(this.λ);
            Apfloat cosOtherλ = cos(other.λ);
            Apfloat x = A.multiply(cosφ).multiply(cosλ).add(B.multiply(cosOtherφ.multiply(cosOtherλ)));
            Apfloat z = A.multiply(cosφ).multiply(sin(this.λ)).add((B.multiply(cosOtherφ).multiply(sin(other.λ))));
            Apfloat y = A.multiply(sin(this.φ)).add(B.multiply(sin(other.φ)));

            Apfloat φ = atan2(y, sqrt(pow(x, TWO).add(pow(z, TWO))));
            Apfloat λ = atan2(z, x);

            buf[2 * (i - 1) + 0] = φ;
            buf[2 * (i - 1) + 1] = λ;
        }
    }

    /**
     * Returns the center of the triangle formed by this point and the other 2
     */
    public Position centroid(Position other1, Position other2) {
        Position[] triangle = new Position[]{this, other1, other2};
        int n = triangle.length;
        Apfloat len = new Apfloat(triangle.length, 200);
        Apfloat sum_x = new Apfloat(0, 200);
        Apfloat sum_z = new Apfloat(0, 200);
        Apfloat sum_y = new Apfloat(0, 200);

        for (int i = 0; i < n; i += 1) {
            Position current = triangle[i];
            Apfloat i_φ = current.getφ();
            Apfloat i_λ = current.getλ();

            Apfloat cosi_φ = cos(i_φ);
            sum_x = sum_x.add(cosi_φ.multiply(cos(i_λ)));
            sum_z = sum_z.add(cosi_φ.multiply(sin(i_λ)));
            sum_y = sum_y.add(sin(i_φ));
        }

        Apfloat x = sum_x.divide(len);
        Apfloat z = sum_z.divide(len);
        Apfloat y = sum_y.divide(len);

        Apfloat r = sqrt((x.multiply(x)).add(z.multiply(z)).add(y.multiply(y)));

        Apfloat φ = asin(y.divide(r));
        Apfloat λ = atan2(z, x);
        return new Position(φ, λ);
    }

    public Apfloat getλ() {
        return λ;
    }

    public Apfloat getφ() {
        return φ;
    }

    public Apfloat getLat() {
        return φ.divide(convert);
    }

    public Apfloat getLng() {
        return λ.divide(convert);
    }
    public String toString() {
        return getLat() + ", " + getLng() + ",0";
        //return "φ: " + φ + ", λ: " + λ;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }
}

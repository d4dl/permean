package com.d4dl.permean.mesh;

import static java.lang.StrictMath.PI;

import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Data
public class DefaultVertex implements MeshVertex {

    public static final MathContext CONTEXT = new MathContext(10, RoundingMode.HALF_EVEN);
    public static float tiny = 0.0000001f;
    private short accessCount;
    private String stableKey;
    private float latitude;
    private float longitude;
    int index;
    private UUID id;
    private Set<MeshCell> cells;

    public DefaultVertex() {

    }

    public DefaultVertex(String stableKey, int index, float latitude, float longitude) {
        this((UUID)null, index, latitude, longitude);
        this.stableKey = stableKey;
    }

    public DefaultVertex(UUID stableUUID, int index, float latitude, float longitude) {
        this(stableUUID, latitude, longitude);
        this.index = index;
    }

    public DefaultVertex(UUID uuid, float latitude, float longitude) {
        this.setId(uuid);
        this.latitude = Math.abs(latitude) < tiny ? 0 : latitude;
        this.longitude = Math.abs(longitude) < tiny ? 0 : longitude;
    }

    public DefaultVertex(int index, float lat, float lng) {
        this(null, lat, lng);
        this.index = index;
    }

    public String kmlString(int height) {
        return "              " + longitude + "," +  latitude + "," + height;
        //return "φ: " + φ + ", λ: " + λ;
    }

    public boolean getShouldPersist() {
      return accessCount == 0;
    }

    public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append(this.index).append(" [").append(this.latitude).append(",").append(this.longitude).append("]");
      return buffer.toString();
    }

    public short access() {
        accessCount++;
        return accessCount;
    }


    public double getφ() {
        return latitude / Position.LAT_CONVERT;
    }

    public double getλ() {
        return (longitude / 180) * PI;
    }

    public Position getPosition() {
        return new Position(getφ(), getλ());
    }

}

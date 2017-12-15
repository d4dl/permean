package com.d4dl.permean.data;

import lombok.Data;

import javax.persistence.*;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Data
@Table(uniqueConstraints={
        @UniqueConstraint(columnNames = {"latitude", "longitude"})
})
@Entity
public class Vertex extends BaseEntity {
    double latitude;
    double longitude;

    public Vertex(String uuid, double latitude, double longitude) {
        this.setId(uuid);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String kmlString(int height) {
        return "              " + longitude + "," +  latitude + "," + height;
        //return "φ: " + φ + ", λ: " + λ;
    }

    public String toString() {
        return id;
    }
}

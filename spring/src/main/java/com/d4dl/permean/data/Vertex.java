package com.d4dl.permean.data;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Data
@Table(uniqueConstraints={
        @UniqueConstraint(columnNames = {"index"}),
        @UniqueConstraint(columnNames = {"latitude", "longitude"})
})
@Entity
public class Vertex extends BaseEntity {
    double latitude;
    double longitude;
    int index;


    public Vertex(String uuid, int index, double latitude, double longitude) {
        this.setId(uuid);
        this.index = index;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String toString() {
        return id;
    }
}

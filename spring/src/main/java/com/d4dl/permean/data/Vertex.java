package com.d4dl.permean.data;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Data
@Table(uniqueConstraints={
        @UniqueConstraint(columnNames = {"latitude", "longitude"})
})
@Entity
public class Vertex extends BaseEntity {
    BigDecimal latitude;
    BigDecimal longitude;

    public Vertex(String uuid, double latitude, double longitude) {
        this.setId(uuid);
        this.latitude = new BigDecimal(latitude < Math.abs(0.00000000000001) ? 0 : latitude, MathContext.DECIMAL64);
        this.longitude = new BigDecimal(longitude < Math.abs(0.00000000000001) ? 0 : longitude, MathContext.DECIMAL64);
    }

    public String kmlString(int height) {
        return "              " + longitude + "," +  latitude + "," + height;
        //return "φ: " + φ + ", λ: " + λ;
    }

    public String toString() {
        return id;
    }
}

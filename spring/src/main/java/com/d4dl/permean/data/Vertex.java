package com.d4dl.permean.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Data
@Table(name = "vertex", uniqueConstraints={
        @UniqueConstraint(columnNames = {"latitude", "longitude"})
})
@Entity
public class Vertex extends BasicEntity {

    public static final MathContext CONTEXT = new MathContext(10, RoundingMode.HALF_EVEN);
    public static BigDecimal tiny = new BigDecimal(0.00000000000001, CONTEXT);
    BigDecimal latitude;
    BigDecimal longitude;

    @ManyToMany(mappedBy = "vertices")
    @JsonIgnore
    Set<Cell> cells;

    public Vertex() {

    }

    public Vertex(String uuid, BigDecimal latitude, BigDecimal longitude) {
        this.setId(uuid);
        this.latitude = latitude.abs().compareTo(tiny) <= 0 ? new BigDecimal(0, CONTEXT) : latitude;
        this.longitude = longitude.abs().compareTo(tiny) <= 0 ? new BigDecimal(0, CONTEXT) : longitude;
        //this.latitude = latitude;
        //this.longitude = longitude;
    }

    public String kmlString(int height) {
        return "              " + longitude + "," +  latitude + "," + height;
        //return "φ: " + φ + ", λ: " + λ;
    }

    public String toString() {
        return id;
    }
}

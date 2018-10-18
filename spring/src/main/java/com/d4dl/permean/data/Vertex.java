package com.d4dl.permean.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
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
@EqualsAndHashCode(exclude={"cells"})
public class Vertex extends BasicEntity {

    public static final MathContext CONTEXT = new MathContext(10, RoundingMode.HALF_EVEN);
    public static BigDecimal tiny = new BigDecimal(0.00000000000001, CONTEXT);
    BigDecimal latitude;
    BigDecimal longitude;

    @ManyToMany(mappedBy = "vertices", fetch = FetchType.LAZY)
    @JsonIgnore
    Set<Cell> cells;

    public Vertex() {

    }

    public Vertex(UUID uuid, BigDecimal latitude, BigDecimal longitude) {
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
}

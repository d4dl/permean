package com.d4dl.permean.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
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
    public static float tiny = 0.0000001f;

    @Transient
    @JsonIgnore
    private short accessCount;

    float latitude;
    float longitude;
    int index;

    @ManyToMany(mappedBy = "vertices", fetch = FetchType.LAZY)
    @JsonIgnore
    Set<Cell> cells;

    public Vertex() {

    }

    public Vertex(UUID stableUUID, int index, float latitude, float longitude) {
        this(stableUUID, latitude, longitude);
        this.index = index;
    }

    public Vertex(UUID uuid, float latitude, float longitude) {
        this.setId(uuid);
        this.latitude = Math.abs(latitude) < tiny ? 0 : latitude;
        this.longitude = Math.abs(longitude) < tiny ? 0 : longitude;
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
      buffer.append("[").append(this.latitude).append(",").append(this.longitude).append("]");
      return buffer.toString();
    }

    public short access() {
        accessCount++;
        return accessCount;
    }
}

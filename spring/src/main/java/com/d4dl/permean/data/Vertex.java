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
    private boolean shouldPersist;

    float latitude;
    float longitude;

    @ManyToMany(mappedBy = "vertices", fetch = FetchType.LAZY)
    @JsonIgnore
    Set<Cell> cells;

    public Vertex() {

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

    /**
     * To avoid persisting vertices more than once some cells "own" a vertex so it can be
     * determined when the vertex, which is shared with other cells, should be persisted.
     */
    public void setShouldPersist() {
        this.shouldPersist = true;
    }

    public boolean getShouldPersist() {
      return shouldPersist;
    }
}

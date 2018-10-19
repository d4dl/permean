package com.d4dl.permean.data;

import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.List;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Data
@Entity
@NamedEntityGraph(name = "Cell.vertices",
        attributeNodes = @NamedAttributeNode("vertices"))
public class Cell extends BasicEntity {

    private double area;
    float centerLatitude;
    float centerLongitude;

    @ManyToMany(fetch = FetchType.EAGER,
                cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Fetch(FetchMode.SUBSELECT)
    @JoinTable(name="cell_vertices",
            joinColumns=
            @JoinColumn(name="cell_id", referencedColumnName="id"),
            inverseJoinColumns=
            @JoinColumn(name="vertices_id", referencedColumnName="id")
    )
    @OrderColumn(name="sequence")
    private List<Vertex> vertices;


    public Cell() {

    }

    public Cell(List<Vertex> vertices, double area, float centerLatitude, float centerLongitude) {
        this(UUID.randomUUID(), vertices, area, centerLatitude, centerLongitude);
    }

    public Cell(UUID id, List<Vertex> vertices, double area, float centerLatitude, float centerLongitude) {
        setId(id);
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.vertices = vertices;
        this.area = area;
    }
}

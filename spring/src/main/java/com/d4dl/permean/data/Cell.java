package com.d4dl.permean.data;

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

    private int parentSize;
    private double area;

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

    public Cell(String id, List<Vertex> vertices, int parentSize, double area) {
        setId(id);
        this.vertices = vertices;
        this.area = area;
        this.parentSize = parentSize;
    }

}

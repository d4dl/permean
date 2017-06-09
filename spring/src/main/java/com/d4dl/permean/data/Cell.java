package com.d4dl.permean.data;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import java.util.List;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Data
@Entity
public class Cell extends BaseEntity {


    private final int parentSize;
    @ManyToMany
    @OrderColumn(name="sequence")
    private List<Vertex> vertices;

    double area;

    public Cell(String id, List<Vertex> vertices, int parentSize, double area) {
        setId(id);
        this.vertices = vertices;
        this.area = area;
        this.parentSize = parentSize;
    }

}

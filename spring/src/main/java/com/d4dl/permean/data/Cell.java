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
    @ManyToMany
    @OrderColumn(name="sequence")
    private List<Vertex> vertices;

    double area;

    public Cell(List<Vertex> vertices, double area) {
        this.vertices = vertices;
        this.area = area;
    }

}

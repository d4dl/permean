package com.d4dl.permean.data;

import org.springframework.data.rest.core.config.Projection;

import java.util.List;

/**
 * Created by joshuadeford on 1/17/18.
 */
@Projection(name = "vertices" , types = Cell.class)
public interface CellVerticesProjection {
    String getId();
    List<Vertex> getVertices();
}

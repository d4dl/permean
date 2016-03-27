package com.d4dl.mesh;

import com.d4dl.mesh.geom.LatLngPolygon;

/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 8/11/14
 * Time: 7:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class LatLngTriangle extends LatLngPolygon {
    private int hashCode;

    /**
     * Create a new triangle. This does not copy the vertices.  It sorts them.
     * If you need to reuse the vertices intact make a copy first.
     * @param vertices
     */
    public LatLngTriangle(LatLng[] vertices) {
        super("", vertices);

        if(vertices.length != 3) {
            throw new IllegalArgumentException("There must be three and only three vertices.");
        }
    }

    public LatLngTriangle() {
    }

    public LatLngTriangle(LatLng v1, LatLng v2, LatLng v3) {
        this(new LatLng[]{v1, v2, v3});
    }




    public void addVertex(LatLng latLng) {
        LatLng[] vertices = this.getVertexArray();
        if(vertices[0] == null) {
            vertices[0] = latLng;
        } else if(vertices[1] == null) {
            vertices[1] = latLng;
        } else if(vertices[2] == null) {
            vertices[2] = latLng;
        } else {
            throw new IllegalStateException("this triangle is full.  No more vertices can be added");
        }
    }
}

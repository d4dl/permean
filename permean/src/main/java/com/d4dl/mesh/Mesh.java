package com.d4dl.mesh;

import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 8/11/14
 * Time: 7:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class Mesh {
    private HashSet<LatLngTriangle> triangles = new HashSet<LatLngTriangle>();

    public HashSet<LatLngTriangle> getTriangles() {
        return triangles;
    }

    public void setTriangles(HashSet<LatLngTriangle> triangles) {
        this.triangles = triangles;
    }

    /**
     * Adds a triangle to the mesh.
     * @param triangle
     * @return 1 if there is a duplicate 0 otherwise.
     */
    public int addTriangle(LatLngTriangle triangle) {
        int duplicate = 0;
        if(this.triangles.contains(triangle)) {
            //throw new IllegalStateException("The mesh already contains the triangle " + triangle);
            duplicate = 1;
        }
        this.triangles.add(triangle);
        System.out.println("Added " + triangle);
        return duplicate;
    }
}

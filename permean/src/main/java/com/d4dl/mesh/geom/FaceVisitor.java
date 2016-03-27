package com.d4dl.mesh.geom;

/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 3/28/15
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FaceVisitor {
    public void visit(LatLngPolygon face, boolean replace);

    public boolean isReady();
}

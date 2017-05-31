package com.d4dl.mesh;

import com.d4dl.mesh.geom.LatLngPolygon;
import com.d4dl.mesh.geom.LatLngPolyhedron;
import mesh.HexField;
import mesh.Sphere;

/**
 * Created with IntelliJ IDEA.
 * User: joshuadeford
 * Date: 3/26/15
 * Time: 4:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class BetterIcosahedronTesselator extends IcosahedronTesselator {

    public LatLngPolyhedron createIcosohedron() {
        LatLngPolyhedron polyhedron = new LatLngPolyhedron();
        Sphere sphere = new Sphere(1);
        for(HexField field : sphere.getFields()) {
            LatLngPolygon latLngPolygon = new LatLngPolygon("1", field.getLatLon());
            polyhedron.addFace(latLngPolygon);
        }
        return polyhedron;
    }
}
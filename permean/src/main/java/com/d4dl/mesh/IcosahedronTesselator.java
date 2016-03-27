package com.d4dl.mesh;

import com.d4dl.mesh.geom.LatLngPolygon;
import com.d4dl.mesh.geom.LatLngPolyhedron;

/**
 * Created with IntelliJ IDEA.
 * User: joshuadeford
 * Date: 3/26/15
 * Time: 4:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class IcosahedronTesselator {
    public static double PHI = (1 + Math.sqrt(5)) / 2;
    public static final LatLng northPole = new LatLng(90, 0);
    public static final LatLng southPole = new LatLng(-90, 0);

    public static double[] longitudes = {-144, -108, -72, -36, 0, 36, 72, 108, 144, 180};

    public static void main(String args[]) {
        IcosahedronTesselator tesselator = new IcosahedronTesselator();
        LatLngPolyhedron polyhedron = tesselator.createIcosohedron();
        polyhedron.report();
        polyhedron.truncateVertices(0);
        polyhedron.truncateVertices(1);

        polyhedron.truncateVertices(2);
        polyhedron.truncateVertices(3);
        polyhedron.truncateVertices(4);
        polyhedron.truncateVertices(5);
        polyhedron.truncateVertices(6);
        polyhedron.truncateVertices(7);
        polyhedron.truncateVertices(8);
//        tesselator.truncateVertices(9);
//        tesselator.truncateVertices(10);
        System.out.println("Tessallated " + polyhedron.getVertices().size() + " vertices.");
    }


    public LatLngPolyhedron createIcosohedron() {
        LatLngPolyhedron polyhedron = new LatLngPolyhedron();

        LatLng first = new LatLng(30 , longitudes[0]);
        LatLng second = new LatLng(-30 , longitudes[1]);

        LatLng oneAgo = second;
        LatLng twoAgo = first;
        for (int j = 2; j < longitudes.length; j++) {
            boolean isTop = (j % 2 == 0);
            LatLng current = new LatLng(isTop ? 30 : -30, longitudes[j]);
            int indexName = j / 2;
            LatLngPolygon p1 = newFace(polyhedron, current, oneAgo, twoAgo, (isTop ? "UpF" : "DownF") + indexName);
            LatLngPolygon p2 = newFace(polyhedron, current, isTop ? northPole : southPole, twoAgo, (isTop ? "Top" : "Bottom") + indexName);
            twoAgo = oneAgo;
            oneAgo = current;
        }
        LatLngPolygon l1 = newFace(polyhedron, twoAgo, northPole, first, "TopWrap5", true);
        LatLngPolygon l2 = newFace(polyhedron, oneAgo, second, southPole, "BottomWrap5", true);
        LatLngPolygon l3 = newFace(polyhedron, twoAgo, first, oneAgo, "UpFWrap5", true);
        LatLngPolygon l4 = newFace(polyhedron, oneAgo, first, second, "DownFWrap5", true);
        return polyhedron;
    }

    private LatLngPolygon newFace(LatLngPolyhedron polyhedron, LatLng l1, LatLng l2, LatLng l3, String name) {
        return this.newFace(polyhedron, l1, l2, l3, name, false);
    }

    private LatLngPolygon newFace(LatLngPolyhedron polyhedron, LatLng l1, LatLng l2, LatLng l3, String name, boolean forceVertexOrder) {
        LatLng[] vertices = {l1, l2, l3};
        LatLngPolygon latLngPolygon = new LatLngPolygon(name, (forceVertexOrder || LatLngPolygon.isCCW(vertices)) ? vertices : new LatLng[]{l1, l3, l2});
        polyhedron.addFace(latLngPolygon);
        return latLngPolygon;
    }
}
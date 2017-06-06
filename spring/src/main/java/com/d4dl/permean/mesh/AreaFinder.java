package com.d4dl.permean.mesh;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * Created by joshuadeford on 5/31/17.
 */
public class AreaFinder {

    GeometryFactory geomFactory = new GeometryFactory();
    CoordinateReferenceSystem calculationCRS;
    MathTransform transform;

    public AreaFinder() {
        try {
            calculationCRS = CRS.decode("EPSG:2163", true);
            transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, calculationCRS, true);
        } catch (FactoryException e) {
            throw new RuntimeException("Couldn't get CRS", e);
        }
    }

    public static void main(String[] args) throws Exception {
        // Example data: vertex coords of a unit rectangle
        //double[] x = {-43, -43, -44, -43}; // longitudes
        //double[] y = {-23, -22, -22, -23}; // latitudes
        /**
        Position[] coords = new Position[]{
                new Position(33.79787108857934, -99.28590692589428),
                new Position(33.40020055341449, -99.18281160052523),
                new Position(33.44000622594606, -98.78925663799728),
                new Position(33.79433622199097, -98.53781907559026)};
         **/


        // calculate area
        //double area = new AreaFinder().getArea(coords);
        //System.out.println("Polygon area: " + area);
    }

    public double getArea(Position[] latLngs)  {
        try {
            final int N = latLngs.length;

            // Create the polygon
            Coordinate[] coords = new Coordinate[N + 1];

            for (int i = 0; i < N; i++) {
                // remember X = longitude, Y = latitude !
                coords[i] = new Coordinate(latLngs[i].getLng().doubleValue(), latLngs[i].getLat().doubleValue());
            }
            // closing coordinate (same as first coord
            coords[N] = new Coordinate(coords[0]);

            LinearRing polygonBoundary = geomFactory.createLinearRing(coords);
            LinearRing[] polygonHoles = null;
            Geometry polygon = geomFactory.createPolygon(polygonBoundary, polygonHoles);
            //if(latLngs[0].getLat() > 30 && latLngs[0].getLat() < 40 && latLngs[0].getLng() > 30 && latLngs[0].getLng() < 40) {
                //System.out.println("Found good lat long");
            //}


            // Reproject the polygon and return its area
            Geometry transformedPolygon = JTS.transform(polygon, transform);
            //Geometry transformedPolygon = JTS.transform(densePolygon, transform);
            double area = transformedPolygon.getArea();
            return area;
        } catch (TransformException e) {
            throw new RuntimeException("Couldn't get area", e);
        }
    }

}

package com.d4dl.mesh.geom;

import com.d4dl.mesh.LatLng;
import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 3/28/15
 * Time: 9:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class Edge implements Iterable<LatLng> {
    private LatLng start;
    private LatLng end;
    int ROUND_AMOUNT = 100000000;
    public static CoordinateReferenceSystem CRS = new DefaultGeographicCRS(DefaultGeographicCRS.WGS84);

    public Edge(LatLng start, LatLng end) {
        if(start == end) {
            throw new IllegalStateException("An edge cannot have the same start and end: " + start);
        }
        this.start = start;
        this.end = end;
    }

    public LatLng getStart() {
        return start;
    }

    public LatLng getEnd() {
        return end;
    }

    /**
     * Gets a coordinate one third of the way beteen this and the specified destination.
     * If the specified destination is not either the start or end an illegal argument exception is thrown
     * @param destination
     * @return
     */
    public LatLng shorten(LatLng destination, double amount) {

        LatLng start = this.getOther(destination);
        LatLng target = destination;
        if(destination.getDerivedFrom() != null) {
           target = destination.getDerivedFrom();
        }
        if(start.getDerivedFrom() != null) {
            start = start.getDerivedFrom();
        }
        GeodeticCalculator distanceCalculator = new GeodeticCalculator();
        try {
            distanceCalculator.setStartingPosition(JTS.toDirectPosition(new Coordinate(start.getLng(), start.getLat()), CRS));
            distanceCalculator.setDestinationPosition(JTS.toDirectPosition(new Coordinate(target.getLng(), target.getLat()), CRS));
        } catch (TransformException e) {
            e.printStackTrace();
        }

        double distance = distanceCalculator.getOrthodromicDistance();
        double angle = distanceCalculator.getAzimuth();
        angle = new BigDecimal(angle, new MathContext(12, RoundingMode.HALF_UP)).doubleValue();

        GeodeticCalculator newPositionCalculator = new GeodeticCalculator();
        try {
            newPositionCalculator.setStartingPosition(JTS.toDirectPosition(new Coordinate(start.getLng(), start.getLat()), CRS));
            newPositionCalculator.setDirection(angle, (distance * amount));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Point2D dest = newPositionCalculator.getDestinationGeographicPoint();
        double y = new BigDecimal(dest.getY(), new MathContext(10, RoundingMode.HALF_UP)).doubleValue();
        double x = new BigDecimal(dest.getX(), new MathContext(10, RoundingMode.HALF_UP)).doubleValue();
        LatLng result = new LatLng(y, x);
        if(this.start == destination) {
            this.end = result;
        } else {
            this.start = result;
        }
        return result;
    }

    public LatLng getOther(LatLng destination) {
        if(destination != this.start && destination != this.end) {
            throw new IllegalArgumentException(destination + " is not the start ("+ this.start +") or the end("+ this.end +")");
        }
        return destination == this.start ? this.end : this.start;
    }

    public String toString() {
        return this.start + "->" +  this.end;
    }

    @Override
    public Iterator<LatLng> iterator() {
        Iterator<LatLng> itor = new Iterator() {
            private int index = 0;
            @Override
            public boolean hasNext() {
                return index < 2;
            }

            @Override
            public Object next() {
                index++;
                switch (index) {
                    case 1: return getStart();
                    case 2: return getEnd();
                }
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is invalid");
            }
        };

        return itor;
    }
}

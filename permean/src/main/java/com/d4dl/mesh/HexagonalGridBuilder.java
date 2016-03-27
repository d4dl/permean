package com.d4dl.mesh;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.GridElement;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.PolygonElement;
import org.geotools.grid.hexagon.HexagonOrientation;
import org.geotools.grid.hexagon.Hexagons;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: joshuadeford
 * Date: 3/26/15
 * Time: 2:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class HexagonalGridBuilder {
    public static void main(String args[]) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("hextype");
        typeBuilder.add("hexagon", Polygon.class, (CoordinateReferenceSystem)null);
        typeBuilder.add("color", Color.class);
        SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

        final ReferencedEnvelope bounds = new ReferencedEnvelope(0, 100, 0, 100, null);

        GridFeatureBuilder builder = new GridFeatureBuilder(TYPE) {
            @Override
            public void setAttributes(GridElement element, Map<String, Object> attributes) {
                double area = element.getBounds().getArea();
                System.out.println("area of grid element " + area);
                PolygonElement polyEl = (PolygonElement) element;
                int g = (int) (255 * polyEl.getCenter().x / bounds.getWidth());
                int b = (int) (255 * polyEl.getCenter().y / bounds.getHeight());
                attributes.put("color", new Color(0, g, b));
            }
        };

        // Pass the GridFeatureBuilder object to the createHexagonalGrid method
        // (the -1 value here indicates that we don't need densified polygons)
        final double sideLen = 5.0;
        SimpleFeatureSource grid = Hexagons.createGrid(bounds, sideLen, HexagonOrientation.ANGLED, builder);
    }
}

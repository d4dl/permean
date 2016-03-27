package com.d4dl.mesh;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.geotools.factory.BasicFactories;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.DirectPosition3D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.grid.GridElement;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.TriangleStripArray;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class GeoMeshBuilder  {
    final FilterFactory2 ff2 = CommonFactoryFinder.getFilterFactory2();
    final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
    public static final String VERTEX_INSERT = "INSERT INTO mesh_vertex (id, lat, lng) VALUES\n(\n";
    public static final String TRIANGLE_INSERT = "INSERT INTO mesh_triangle (uri, v0, v1, v2) VALUES\n(\n";

    int id = 0;

    public GeoMeshBuilder() {
    }

    public void setAttributes(GridElement el, Map<String, Object> attributes) {
        attributes.put("id", ++id);
    }



    public static void main(String arg[]) throws Exception {

        try {
            System.out.println("J3D is coming from: " + Class.forName("javax.media.j3d.Pipeline").getProtectionDomain().getCodeSource().getLocation());
            //TriangleStripArray indexedTriangles = new GeoMeshBuilder().getGeoTriangles();
            //new GeoMeshBuilder().createGeoMesh(indexedTriangles);
            IndexedTriangleArray indexedTriangles = new GeoMeshBuilder().getIndexedGeoTriangles();
            new GeoMeshBuilder().createIndexedGeoMesh(indexedTriangles);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    private LatLngTriangle getTriangle(double[] xyzCoordinates, CoordinateOperation operation, int offset) throws FactoryException, TransformException {

        LatLngTriangle triangle = new LatLngTriangle();
        for(int triangleVertexCount = 0; triangleVertexCount < 9;) {//For each point in the triangle
            double coordinate0X = xyzCoordinates[offset + triangleVertexCount];
            double coordinate0Y = xyzCoordinates[offset + triangleVertexCount + 1];
            double coordinate0Z = xyzCoordinates[offset + triangleVertexCount + 2];

            DirectPosition3D srcPosition0 = new DirectPosition3D(coordinate0X, coordinate0Y, coordinate0Z);

            DirectPosition2D destPosition = new DirectPosition2D();
            MathTransform transform = operation.getMathTransform();
            transform.transform(srcPosition0, destPosition);
            triangle.addVertex(new LatLng(destPosition.getOrdinate(0), destPosition.getOrdinate(1)));
            triangleVertexCount += 3;
        }//triangle
        return triangle;
    }

    private IndexedLatLngTriangle getIndexedTriangle(int[] triangleIndices, double[] xyzCoordinates, LatLng[] latLngCoordinates, CoordinateOperation operation) throws FactoryException, TransformException {

        IndexedLatLngTriangle triangle = new IndexedLatLngTriangle(triangleIndices, latLngCoordinates);

        for(int triangleVertexCount = 0; triangleVertexCount < triangleIndices.length;) {//For each point in the triangle
            int coordinateIndex  = triangleIndices[triangleVertexCount];

            DirectPosition3D srcPosition0 = new DirectPosition3D(xyzCoordinates[coordinateIndex], xyzCoordinates[coordinateIndex + 1], xyzCoordinates[coordinateIndex + 2]);

            DirectPosition2D destPosition = new DirectPosition2D();
            MathTransform transform = operation.getMathTransform();
            transform.transform(srcPosition0, destPosition);
            latLngCoordinates[(coordinateIndex) /2] = new LatLng(destPosition.getOrdinate(0), destPosition.getOrdinate(1));
            triangleVertexCount++;
        }
        return triangle;
    }

    private void createIndexedGeoMesh(IndexedTriangleArray indexedTriangles) throws FactoryException, IOException, TransformException {
        CoordinateOperation operation = buildTransformOperation();
        int indexCount = indexedTriangles.getIndexCount();
        System.out.println("Creating a mesh with " + indexCount/3 + " triangles.");
        int[] indices = new int[indexCount];
        indexedTriangles.getCoordinateIndices(indexedTriangles.getInitialIndexIndex(), indices);

        int vertexCount = indexedTriangles.getVertexCount();
        //3 entries for each vertex,
        int coordinateCount = vertexCount * 3;
        double[] xyzCoordinates = new double[coordinateCount];
        indexedTriangles.getCoordinates(0, xyzCoordinates);
        List<IndexedLatLngTriangle> triangleList = new ArrayList<IndexedLatLngTriangle>();
        File meshSQLVerticesOut = new File("/Users/jdeford/dev/D4DL/plmbackend/src/java/com/d4dl/mesh/meshOutVertices.sql");
        BufferedWriter meshSqlVerticesWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(meshSQLVerticesOut)));
        File meshSQLTrianglesOut = new File("/Users/jdeford/dev/D4DL/plmbackend/src/java/com/d4dl/mesh/meshOutTriangles.sql");
        BufferedWriter meshSqlTrianglesWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(meshSQLTrianglesOut)));

        File meshFile = new File("/Users/jdeford/dev/D4DL/plmbackend/src/java/com/d4dl/mesh/mesh.json");
        BufferedWriter meshJSONWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(meshFile)));
        try {
            meshJSONWriter.write("{\n\"  triangleIndices\":\n [\n");

            LatLng[] latLngCoordinates = new LatLng[vertexCount];

            meshSqlVerticesWriter.write(VERTEX_INSERT);
            meshSqlTrianglesWriter.write(TRIANGLE_INSERT);

            for(int i = 0; i < indexCount;) {
                int[] triangleIndices = {indices[i], indices[i+1], indices[i+2]};
                IndexedLatLngTriangle triangle = this.getIndexedTriangle(triangleIndices, xyzCoordinates, latLngCoordinates, operation);
                meshJSONWriter.write("    \""+triangle.getURI()+"\"");
                meshSqlTrianglesWriter.write("(" + triangle.toString() +")");
                i += 3;
                if(i < indexCount) {
                    meshSqlTrianglesWriter.write(",\n");
                    meshJSONWriter.write(",\n");
                }
            }

            meshJSONWriter.write("\n  ],\n  \"vertices\":\n [");


            for(int i=0; i < latLngCoordinates.length; i++) {
                LatLng coordinate = latLngCoordinates[i];
                meshSqlVerticesWriter.write("(" + i + ", " + coordinate.getLat() + ", " + coordinate.getLng() + ")");
                meshJSONWriter.write("    [" + coordinate.getLat() + ", " + coordinate.getLng() + "]");
                if(i < latLngCoordinates.length) {
                    meshSqlVerticesWriter.write(",\n");
                }
            }

            meshSqlTrianglesWriter.write("\n);\n\n");
            meshSqlVerticesWriter.write("\n);\n\n");
            meshJSONWriter.write("\n  ]\n}");
        } finally {
            meshJSONWriter.close();
            meshSqlTrianglesWriter.close();
            meshSqlVerticesWriter.close();
        }
    }

    private CoordinateOperation buildTransformOperation() throws FactoryException {
        CoordinateReferenceSystem sourceCRS = new DefaultGeocentricCRS(DefaultGeocentricCRS.CARTESIAN);
        CoordinateReferenceSystem destCRS = new DefaultGeographicCRS(DefaultGeographicCRS.WGS84);

        BasicFactories basicFactories = BasicFactories.getDefault();

        CoordinateOperationFactory operationFactory = basicFactories.getCoordinateOperationFactory();
        return operationFactory.createOperation(sourceCRS, destCRS);
    }


    public IndexedTriangleArray getIndexedGeoTriangles() {

        SimpleUniverse universe = new SimpleUniverse();

        universe.getViewingPlatform().setNominalViewingTransform();
        BranchGroup contentRoot = new BranchGroup();
        //Sphere earth = new Sphere(6378000f, Sphere.ALLOW_CHILDREN_READ, 7800);
        Sphere earth = new Sphere(6378000f, Sphere.ALLOW_CHILDREN_READ, 5);

        universe.addBranchGraph(contentRoot);
        TriangleStripArray geometry = (TriangleStripArray)earth.getShape().getGeometry();
        GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);
        gi.reset(geometry);
        gi.convertToIndexedTriangles();
        IndexedTriangleArray indexedTriangles = (IndexedTriangleArray)gi.getIndexedGeometryArray();

        return indexedTriangles;
    }
}
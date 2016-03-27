package com.d4dl.mesh.geom;

import com.d4dl.mesh.LatLng;
import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.operation.TransformException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 8/11/14
 * Time: 7:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class LatLngPolygon {
    private LinkedList<LatLng> vertices = new LinkedList<LatLng>();
    private static int FACE_COUNT = 1;
    private LatLng centroid;
    private Boolean wraps = null;
    private int id;
    public static final int xA=0;
    public static final int yA=1;
    public static final int xB=2;
    public static final int yB=3;
    public static final int xC=4;
    public static final int yC=5;

    private String name;

    public LatLngPolygon() {
        FACE_COUNT++;
        this.id = FACE_COUNT;
    }


    public LatLng getVertex(int i) {
        return this.vertices.get(i);
    }

    /**
     * Vertices should be CCW
    * @param vertices
    */
    public LatLngPolygon(String name, LatLng[] vertices) {
        this();
        this.name = name;
        for(int i=0; i < vertices.length; i++) {
            this.addVertex(vertices[i]);
        }

    }


    private Edge[] getEdgesForVertex(LatLng vertex) {
        int vertexIndex = this.vertices.indexOf(vertex);
        Edge[] edges = new Edge[2];
        int lastIndex = this.vertices.size() - 1;
        LatLng previousVertex;
        LatLng nextVertex;
        if(vertexIndex == 0) {
            previousVertex = this.vertices.get(lastIndex);
            nextVertex = this.vertices.get(vertexIndex + 1);
        } else if(vertexIndex == lastIndex) {
            previousVertex = this.vertices.get(vertexIndex - 1);
            nextVertex = this.vertices.get(0);
        } else {
            nextVertex = this.vertices.get(vertexIndex + 1);
            previousVertex = this.vertices.get(vertexIndex - 1);
        }
        edges[0] = new Edge(previousVertex, vertex);
        edges[1] = new Edge(vertex, nextVertex);
//        if(!this.isCCW(edges[0])) {
//            throw new IllegalStateException("This is a pointless check but the edge is not adjacent");
//        }
//        if(!this.isCCW(edges[1])) {
//            throw new IllegalStateException("This is a pointless check but the edge is not adjacent");
//        }
        return edges;
    }

    public List<LatLng> getVertices() {
        return vertices;
    }

    public LatLng[] getVertexArray() {
        return vertices.toArray(new LatLng[vertices.size()]);
    }

    public void addVertex(LatLng newVertex) {
        this.vertices.add(newVertex);
    }


    /**
     * Looks up the edges using the old vertex.
     * There should only be two.
     * Create a new edge
     * @param oldVertex
     * @param newEdge
     */
    private void replaceVertexWithEdge(LatLng oldVertex, Edge newEdge) {
        int oldVertexIndex = this.vertices.indexOf(oldVertex);
        this.vertices.set(oldVertexIndex, newEdge.getStart());//Replace old vertex
        this.vertices.add(oldVertexIndex + 1, newEdge.getEnd());//add the next end of the edge right after it
    }

    /**
     * Reteruns a new edge that runs CCW
     * @param vertex
     * @return
     */
    public Edge truncateToNewEdge(LatLng vertex, ArrayList<LatLng> reusableVertices) {
        //rebuilding it is cheaper than examining it to clean it up.
        //Should be only two for a polygon
        Edge[] edges = this.getEdgesForVertex(vertex);

        Edge edge1 = edges[0];
        Edge edge2 = edges[1];

        LatLng v1 = edge1.shorten(edge1.getOther(vertex),  1d / 3d);
        LatLng v2 = edge2.shorten(edge2.getOther(vertex), 1d / 3d);
        v1.setDerivedFrom(vertex);
        v2.setDerivedFrom(vertex);
        int reusableIndex1 = reusableVertices.indexOf(v1);
        int reusableIndex2 = reusableVertices.indexOf(v2);
        if(reusableIndex1 >= 0) {
            v1 = reusableVertices.get(reusableIndex1);
        } else {
            reusableVertices.add(v1);
        }
        if(reusableIndex2 >= 0) {
            v2 = reusableVertices.get(reusableIndex2);
        } else {
            reusableVertices.add(v2);
        }
        Edge newEdge = new Edge(v1, v2);

        //To keep the winding correct, replacing one vertex
        //with two, the endpoints must be in reverse winding.
        this.replaceVertexWithEdge(vertex, newEdge);
        return newEdge;
    }


    /**
     * Returns true if the specified specified points run counter clockwise in this polygon.
     * @return
     */
    public boolean isCCW(LatLng point1, LatLng point2) {
        int index0 = this.vertices.indexOf(point1);
        int index1 = this.vertices.indexOf(point2);
        int vertexCount = this.vertices.size();

        int lastIndex = vertexCount - 1;
        int indexDiff = Math.abs(index0 - index1);
        if(indexDiff > 1 && indexDiff != lastIndex) {
            throw new IllegalStateException("The indexes are not adjacent");
        }
        if(index0 < 0 || index1 < 0) {
            throw new IllegalStateException("Edge wasn't found.");
        }
        if(index0 == index1) {
            throw new IllegalStateException("Edges are identical");
        }
        return (index1 == 0 && index0 == lastIndex) ||
               (index0 + 1) == index1;
    }

    public boolean isCCW(Edge edge) {
        return this.isCCW(edge.getStart(), edge.getEnd());
    }

    /**
     * This only works for triangles that don't have vertexes that loop around the earth's prime meridian
     * @return
     */
    public static boolean isCCW(LatLng[] vertices) {
        double[] testValues = new double[vertices.length * 2];
        for(int i=0; i < vertices.length; i++) {
            LatLng vertex = vertices[i];

            double testValueLat = vertex.getLat();
            double testValueLng = vertex.getLng();

            testValues[i * 2] = testValueLng;
            testValues[(i * 2) + 1] = testValueLat;
        }

        double determinant =  testValues[xA]*testValues[yB] +
                              testValues[xB]*testValues[yC] +
                              testValues[xC]*testValues[yA] -
                              testValues[yB]*testValues[xC] -
                              testValues[yC]*testValues[xA] -
                              testValues[yA]*testValues[xB];
        return determinant <= 0;
    }


    public double calculateArea() {
        double area = 0;         // Accumulates area in the loop
        int vertexCount = this.vertices.size();
        int lastVertexIndex = vertexCount - 1;  // The last vertex is the 'previous' one to the first

        LatLng reference = this.vertices.get(lastVertexIndex);
        Coordinate referenceCoordinate = new Coordinate(reference.getLng(), reference.getLat());
        double lastDistance = this.calculateDistance(lastVertexIndex);

        for (int i=0; i < vertexCount; i++) {
            LatLng vertex = this.getVertices().get(i);
            Coordinate currentCoordinate = new Coordinate(reference.getLng(), vertex.getLat());
            double distance = this.calculateDistance(currentCoordinate, referenceCoordinate);

            area = area + (distance * lastDistance);
            lastDistance = distance;
        }
        area = area / 2;
        return area;
    }

    public double calculatePerimiter() {
        int perimeter = 0;
        int vertexCount = this.getVertices().size();
        Coordinate[] coordinates = new Coordinate[vertexCount];
        for(int i=0; i < vertexCount; i++) {
            LatLng vertex = this.getVertices().get(i);
            Coordinate point = new Coordinate(vertex.getLng(), vertex.getLat());
            JTS.toDirectPosition(point, Edge.CRS);
            coordinates[(vertexCount - 1) - 1] = point;
            if(i > 0) {
                double distance = this.calculateDistance(i);
                perimeter += distance;//Not real perimiter. It misses a side.
            }
        }
        return perimeter;
    }

    /**
     * Gets the distance of the edge starting at the specified index.
     * @param fromIndex
     * @return
     */
    public double calculateDistance(int fromIndex) {
        int vertexCount = this.vertices.size();
        int lastVertexIndex = vertexCount - 1;  // The last vertex is the 'previous' one to the first
        int nextIndex = (lastVertexIndex == fromIndex) ? 0 : fromIndex + 1;
        LatLng fromVertex = this.getVertices().get(fromIndex);
        LatLng toVertex = this.getVertices().get(nextIndex);
        return calculateDistance(fromVertex, toVertex);
    }

    private double calculateDistance(int fromIndex, LatLng toVertex) {
        LatLng fromVertex = this.getVertices().get(fromIndex);
        return calculateDistance(fromVertex, toVertex);
    }

    private double calculateDistance(LatLng fromVertex, int toIndex) {
        LatLng toVertex = this.getVertices().get(toIndex);
        return calculateDistance(fromVertex, toVertex);
    }

    private double calculateDistance(LatLng from, LatLng to) {
        Coordinate fromCoordinate = new Coordinate(from.getLng(), from.getLat());
        Coordinate toCoordinate = new Coordinate(to.getLng(), to.getLat());
        return calculateDistance(fromCoordinate, toCoordinate);
    }

    private double calculateDistance(Coordinate fromCoordinate, Coordinate toCoordinate) {
        GeodeticCalculator distanceCalculator = new GeodeticCalculator();
        try {
            distanceCalculator.setStartingPosition(JTS.toDirectPosition(fromCoordinate, Edge.CRS));
            distanceCalculator.setDestinationPosition(JTS.toDirectPosition(toCoordinate, Edge.CRS));
        } catch (TransformException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return distanceCalculator.getOrthodromicDistance();
    }

    public String toString() {
        List<LatLng> vertices = this.getVertices();
        StringBuilder builder = new StringBuilder("[");
        for(int i = 0; i < vertices.size(); i++) {
            LatLng vertex = vertices.get(i);
            builder.append(vertex.toString());
        }
        builder.append("]");
        return builder.toString();

    }


    public String getName() {
        return name;
    }

    public boolean edgeWraps(LatLng vertex1, LatLng vertex2) {
        if(wraps == null) {
            if(this.isCCW(vertex1, vertex2)) {
                this.wraps = true;
            }
            if(!this.isHexagon() && !this.isPentagon()) {
                this.wraps = this.doesWrap();
            } else {
                LatLng centroid = this.getCentroid();
                if(Math.abs(vertex1.getLat() - centroid.getLat()) > 180 / 4) {
                    this.wraps = true;
                } else {
                    if(Math.abs(vertex1.getLat() - vertex2.getLat()) > Math.abs((vertex1.getLat() - centroid.getLat()) * 2)) {
                        this.wraps = true;
                    } else if(Math.abs(vertex1.getLng() - vertex2.getLng()) > Math.abs((vertex1.getLng() - centroid.getLng()) * 2)) {
                        this.wraps = true;
                    } else {
                        this.wraps = false;
                    }
                }
            }
        }
        return this.wraps;
    }

    public boolean doesWrap() {
        int vertexCount = this.getVertices().size();
        LatLng previousVertex = this.getVertices().get(vertexCount - 1);
        for(int i=0; i < vertexCount; i++) {
            LatLng vertex = this.getVertices().get(i);
            if(Math.abs(vertex.getLat() - previousVertex.getLat()) >= 90) {
                return true;
            }
            if(Math.abs(vertex.getLng() - previousVertex.getLng()) >= 180) {
                return true;
            }
        }
        return false;
    }

    private LatLng calculateCentroid() {
        LatLng firstVertex = this.vertices.get(0);
        LatLng oppositeVertex = this.vertices.get(3);

        if(isHexagon()) {
            return getMidpoint(firstVertex, oppositeVertex);
        } else if (isPentagon()) {
            LatLng secondVertex = this.vertices.get(1);
            LatLng edgeMidpoint =  this.getMidpoint(firstVertex, secondVertex);
            return this.getMidpoint(edgeMidpoint, oppositeVertex);
        } else {
            throw new IllegalStateException("No calculating algorithm for the centroid is specified for faces other than pentagons and hexagons. EdgeCount: " + this.vertices.size());
        }

    }

    private boolean isPentagon() {
        return this.vertices.size() == 5;
    }

    private boolean isHexagon() {
        return this.vertices.size() == 6;
    }

    private LatLng getMidpoint(LatLng firstVertex, LatLng otherVertex) {
        double lng0 = firstVertex.getLng();
        double lat0 = firstVertex.getLat();
        double lng2 = otherVertex.getLng();
        double lat2 = otherVertex.getLat();
        return getMidpoint(lng0, lat0, lng2, lat2);
    }

    private LatLng getMidpoint(double lng0, double lat0, double lng1, double lat1) {
        double newLat = (lat0 + lat1) / 2d;
        double newLng = (lng0 + lng1) / 2d;
        return new LatLng(newLat, newLng);
    }

    public LatLng getCentroid() {
        if(centroid == null) {
            this.centroid = this.calculateCentroid();
        }
        return centroid;
    }

    public void setCentroid(LatLng centroid) {
        this.centroid = centroid;
    }
}

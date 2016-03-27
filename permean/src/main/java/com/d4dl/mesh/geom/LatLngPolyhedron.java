package com.d4dl.mesh.geom;

import com.d4dl.mesh.LatLng;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: joshuadeford
 * Date: 3/26/15
 * Time: 5:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class LatLngPolyhedron {
    private HashMap<LatLng, Set<LatLngPolygon>> vertexMap = new HashMap<LatLng, Set<LatLngPolygon>>();
    private List<LatLngPolygon> faces = new ArrayList<LatLngPolygon>();
    FaceVisitor visitor;
    private boolean needsRefresh;
    private int truncationCount;


    public LatLngPolyhedron(FaceVisitor visitor) {
        this.visitor = visitor;
    }

    public LatLngPolyhedron() {

    }
    private HashMap<LatLng, LatLng> vertices = new HashMap<LatLng, LatLng>();


    public void addFace(LatLngPolygon face) {
        //System.out.println("Adding face " + face);
        faces.add(face);
        if (visitor != null) {
            visit(face);
        }
        addFaceVerticesToVertexMap(face);

        this.setNeedsRefresh(true);
    }

    private void addFaceVerticesToVertexMap(LatLngPolygon face) {
        List<LatLng> faceVertices = face.getVertices();
        for (int i = 0; i < faceVertices.size(); i++) {
            Set facesForVertex = vertexMap.get(faceVertices.get(i));
            if (facesForVertex == null) {
                facesForVertex = new HashSet();
                vertexMap.put(faceVertices.get(i), facesForVertex);
            }
            facesForVertex.add(face);
        }
    }

    /**
     * For the specified vertex get all the faces that
     * share it.  Create a new face that has points along all the edges by 1/3
     * remove the specified face replacing them with the new edges.
     * <p/>
     * See truncated Icosahedron for more understanding.
     *
     * @param vertex
     */
    public void truncateToNewFace(LatLng vertex, int depth) {
        Set<LatLngPolygon> facesForVertex = vertexMap.remove(vertex);
        //A new list for holding references to reusable vertices.
        //this should be enough to ensure no vertices that can be reused aren't.
        ArrayList<LatLng> reusableVertices = new ArrayList<LatLng>();
        int faceCount = facesForVertex.size();
        if (faceCount < 3) {
            throw new IllegalStateException("A vertex with less than three faces was found.  Can't truncate.");
        }
        List<Edge> debugNewEdgeList = new ArrayList<Edge>();

        LinkedFaceEdgeMap faceEdgeMap = new LinkedFaceEdgeMap(faceCount);
        for (LatLngPolygon face : facesForVertex) {
            visit(face);
            Edge newEdge = face.truncateToNewEdge(vertex, reusableVertices);
            visit(face);
            boolean ccw = face.isCCW(newEdge);
            //System.out.println("Adding face to map ccw: " + ccw + " edge: " + newEdge);
            faceEdgeMap.addEdgeForFace(newEdge, !ccw);
            debugNewEdgeList.add(newEdge);
            //The vertices of the face have changed.
            //map the vertices.
            this.addFaceVerticesToVertexMap(face);
        }
        LatLngPolygon newFace = new LatLngPolygon("d " + depth + "truncated" + vertex, faceEdgeMap.getOrderedVertices());
        if(vertex.getLng() == 0.0) {
            newFace.setCentroid(vertex);
        }
        visit(newFace);
        this.addFace(newFace);
    }


    public void truncateVertices(int depth) {
        this.truncationCount++;
        if(truncationCount > 1) {
            createInnerLattice();
        }
        LatLng[] vertices = this.getVertices().toArray(new LatLng[0]);
        long start = System.currentTimeMillis();
        System.out.println("Tessallating " + vertices.length + " vertices.");
        for (LatLng vertex : vertices) {
            this.truncateToNewFace(vertex, depth);
        }
        long finish = System.currentTimeMillis();


        this.report();
        this.cleanFaces();
        System.out.println("Finished depth " + depth + " in " + (int) (finish / 60000l) + " seconds.");
    }


    private void createInnerLattice() {
        List<LatLngPolygon> originalFaces = this.faces;
        reset();

        //This is leaving the containing face intact.
        int faceCount = originalFaces.size();
        for (int i = 0; i < faceCount; i++) {
            LatLngPolygon face = originalFaces.get(i);
            int vertexCount = face.getVertices().size();
            if (vertexCount < 3) {
                System.out.println("Weird vertex count");
            }
            LatLng previousVertex = face.getVertex(vertexCount - 1);
            LatLng centroid = face.getCentroid();
            for (int j = 0; j < vertexCount; j++) {
                LatLng vertex = face.getVertex(j);
                LatLng[] latLngs = {getVertex(centroid), getVertex(previousVertex), getVertex(vertex)};
                this.addFace(new LatLngPolygon("centroidFace " + j + " from " + face.getName(), latLngs));
                previousVertex = vertex;
            }
        }
    }

    private void reset() {
        this.vertexMap = new HashMap<LatLng, Set<LatLngPolygon>>();
        this.faces = new ArrayList<LatLngPolygon>();
        this.vertices = new HashMap<LatLng, LatLng>();
    }

    private LatLng getVertex(LatLng vertex) {
        LatLng existingVertex = this.vertices.get(vertex);
        if(existingVertex == null) {
            existingVertex = vertex;
        }
        this.vertices.put(existingVertex, existingVertex);
        return existingVertex;
    }

    private void cleanFaces() {
        for (LatLngPolygon face : this.getFaces()) {
            for (LatLng vertex : face.getVertices()) {
                vertex.setDerivedFrom(null);
            }
        }
    }

    private void visit(LatLngPolygon face) {
        if (visitor != null) {
            visitor.visit(face, true);
        }

    }


    public Set<LatLng> getVertices() {
        return vertexMap.keySet();
    }

    public List<LatLngPolygon> getFaces() {
        return this.faces;
    }

    public void setVisitor(GraphicalFaceVisitor visitor) {
        this.visitor = visitor;
    }

    public void report() {
        ArrayList<Double> areaBins = new ArrayList<Double>();
        double totalArea = 0;
        double minArea = Double.MAX_VALUE;
        double maxArea = 0;
        for (LatLngPolygon face : this.getFaces()) {
            double perimiter = face.calculatePerimiter();
            double area = face.calculateArea();
            minArea = Math.min(minArea, area);
            maxArea = Math.max(maxArea, area);
            areaBins.add(area);
            totalArea += area;
            int vertexCount = this.faces.size();
            try {
//                GeometryFactory fact = new GeometryFactory();
//                LinearRing linear = new GeometryFactory().createLinearRing(coordinates);
//                Polygon poly = new Polygon(linear, null, fact);
            } catch (Exception e) {
                //System.out.print("Got an error reporting. " + e.getMessage());
            }
        }
        Collections.sort(areaBins);

        double areaThresholds = (maxArea - minArea) / 5;
        int thresholdMultiplier = 1;

        double threshold = areaThresholds;
        int counter = 0;
        for (double area : areaBins) {
            counter++;
            if (area > threshold) {
                System.out.println(counter + " faces under " + threshold + " square meters");
                counter = 0;
                threshold = areaThresholds * thresholdMultiplier++;
            }
        }

        System.out.println(" " + counter + " faces under " + threshold + " square meters. Area range " + minArea + " to " + maxArea + " square meters.");
        System.out.println("Polyhedron with total area of " + totalArea);
        System.out.println("Counted " + this.faces.size() + " faces.\n");
    }


    public boolean isNeedsRefresh() {
        return needsRefresh;
    }

    public void setNeedsRefresh(boolean needsRefresh) {
        this.needsRefresh = needsRefresh;
    }
}

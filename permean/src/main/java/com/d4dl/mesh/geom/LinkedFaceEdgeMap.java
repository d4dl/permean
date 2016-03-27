package com.d4dl.mesh.geom;

import com.d4dl.mesh.LatLng;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: joshuadeford
 * Date: 3/27/15
 * Time: 10:19 AM
 * A Utility to help generate new faces from several existing faace.
 */
public class LinkedFaceEdgeMap {
    private Map<LatLng, LatLng> linkMap = new HashMap<LatLng, LatLng>();
    private LatLng firstVertex;
    private int size;
    private LatLng[] orderedVertices;

    public LinkedFaceEdgeMap(int size) {
        this.size = size;
    }

    /**
     * The edge should be provided CCW for the face that created it.
     * For the new face CCW is the other direction.
     * @param edge
     */
    public void addEdgeForFace(Edge edge, boolean ccw) {
        LatLng key = ccw ? edge.getStart() : edge.getEnd();
        LatLng value = ccw ? edge.getEnd() : edge.getStart();

        //System.out.println("Adding " + key + " ");
        if(linkMap.get(key) != null) {
            throw new IllegalArgumentException("A key for this edge already exists. " + key);
        }
        if(firstVertex == null) {
            firstVertex = key;
        }
        linkMap.put(key, value);
    }

    public LatLng[] getOrderedVertices() {
        LatLng[] orderedVertices = new LatLng[this.size];
        LatLng lastVertex = firstVertex;
        for(int i=0; i < this.size; i++) {
            orderedVertices[i] = lastVertex;
            if(i <= this.size) {
                LatLng nextVertex = linkMap.get(lastVertex);
                if(nextVertex == lastVertex) {
                    throw new IllegalStateException("Two consecutive vertices are identical " + nextVertex);
                }
                if(nextVertex == null) {
                    throw new IllegalStateException("The next vertex is null");
                }
                lastVertex = nextVertex;
            }
        }
        return orderedVertices;
    }
}

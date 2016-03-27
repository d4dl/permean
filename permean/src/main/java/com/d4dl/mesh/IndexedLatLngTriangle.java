package com.d4dl.mesh;

import com.google.gwt.user.server.Base64Utils;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 8/11/14
 * Time: 7:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class IndexedLatLngTriangle extends LatLngTriangle {
    private int[] vertexIndices = null;
    private int hashCode;
    private LatLng[] latLngData;
    //GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    public IndexedLatLngTriangle(int[] vertexIndices, LatLng[] meshCoordinates) {
        this.latLngData = meshCoordinates;
        this.vertexIndices = vertexIndices;
    }

    public LatLng[] getVerticexArray() {
        return new LatLng[]{this.latLngData[vertexIndices[0]],
                            this.latLngData[vertexIndices[1]],
                            this.latLngData[vertexIndices[2]]};
    }

    public int hashCode() {
        LatLng[] vertices = getVerticexArray();
        return (int)vertices[0].getLat() ^ (int)vertices[1].getLat() ^ (int)vertices[2].getLat() ^
                (int)vertices[0].getLng() ^ (int)vertices[1].getLng() ^ (int)vertices[2].getLng();
    }

    public String getURI() {
        return "http://d4dl.com/plm/gemoesh/IndexTriangle#" + Base64Utils.toBase64(vertexIndices[0]) + "-" + Base64Utils.toBase64(vertexIndices[1]) + "-" + Base64Utils.toBase64((vertexIndices[2]));
    }


    public void addVertex(LatLng latLng) {
        throw new UnsupportedOperationException("Use addVertexIndex()");
    }

    public int[] getVertexIndices() {
        return vertexIndices;
    }

    public void setVertexIndices(int[] vertexIndices) {
        this.vertexIndices = vertexIndices;
    }

    public String toString() {
        return this.getURI() + ", " + this.vertexIndices[0] + ", " + this.vertexIndices[1] + ", " + this.vertexIndices[2];
    }
}

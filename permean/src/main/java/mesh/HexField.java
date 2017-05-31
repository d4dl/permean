package mesh;

import com.d4dl.mesh.LatLng;

import java.util.HashMap;

/**
 * Created by joshuadeford on 5/29/17.
 */
public class HexField {

    private Sphere parent;
    private int index;
    private final boolean isPentagon;
    private HexField[] adjacentFields;
    //Arbitrary data allowed. Its stored in a list so the correct data can be provided for the current iteration
    private HashMap currentData;
    private HashMap newData;
    private double area;

    HexField(Sphere parent, int index, HashMap data) {
        this.parent         = parent;
        this.index          = index;
        this.currentData    = data;
        isPentagon = ((index < 2) || (getSxy()[2] == 0 && ((getSxy()[1] + 1) % parent.getDivisions()) == 0));
    }

    public int[] getSxy() {
        if(index < 2) {
            return null;
        } else {
            int l = index - 2;
            int x_lim = parent.getDivisions() * 2;
            int y_lim = parent.getDivisions();

            int s = (int) Math.floor(l / (x_lim * y_lim));
            int x = (int) Math.floor((l - s * x_lim * y_lim) / y_lim);
            int y = (l - s * x_lim * y_lim - x * y_lim);
            return new int[]{s, x, y};
        }
    }

    public HashMap getData() {
        return this.currentData;
    }

    public void setData(HashMap newData) {
        this.newData = newData;
    }

    public HexField[] getAdjacentFields() {
        return adjacentFields;
    }

    public void setAdjacentFields(HexField[] adjacentFields) {
        this.adjacentFields = adjacentFields;
    }

    void finishIteration() {
        if(this.parent.isIterating() == true) {
            throw new IllegalStateException("Iteration's cannot be finished while the parent is still iterating.  This should be called by the parent after iterating is complete.");
        }
    }

    public int getIndex() {
        return index;
    }


    public HexField getAdjacent(int p) {
        return this.adjacentFields[p];
    }

    public HexField[] getAdjacents() {
        return this.adjacentFields;
    }

    public void link() {
        {
            int d = parent.getDivisions();
            int[] sxy = getSxy();;
            int max_x = d * 2 - 1;
            int max_y = d - 1;

            // Link polar pentagons to the adjacent fields
            if (this.index == 0) {
                this.adjacentFields = new HexField[]{
                        parent.get(0, 0, 0),
                        parent.get(1, 0, 0),
                        parent.get(2, 0, 0),
                        parent.get(3, 0, 0),
                        parent.get(4, 0, 0)
                };
            } else if (this.index == 1) {
                this.adjacentFields = new HexField[]{
                        parent.get(0, max_x, max_y),
                        parent.get(1, max_x, max_y),
                        parent.get(2, max_x, max_y),
                        parent.get(3, max_x, max_y),
                        parent.get(4, max_x, max_y)
                };
            } else {
                int next = (sxy[0] + 1 + Sphere.PEELS) % Sphere.PEELS;
                int prev = (sxy[0] - 1 + Sphere.PEELS) % Sphere.PEELS;

                int s = sxy[0];
                int x = sxy[1];
                int y = sxy[2];

                this.adjacentFields = new HexField[isPentagon ? 5 : 6];

                // 0: northwestern adjacent (x--)
                if (x > 0) {
                    this.adjacentFields[0] = parent.get(s, x - 1, y);
                } else {
                    if (y == 0) {
                        this.adjacentFields[0] = parent.getNorth();
                    } else {
                        this.adjacentFields[0] = parent.get(prev, y - 1, 0);
                    }
                }

                // 1: western adjacent (x--, y++)
                if (x == 0) {
                    // attach northwestern edge to previous north-northeastern edge
                    this.adjacentFields[1] = parent.get(prev, y, 0);
                } else {
                    if (y == max_y) {
                        // attach southwestern edge...
                        if (x > d) {
                            // ...to previous southeastern edge
                            this.adjacentFields[1] = parent.get(prev, max_x, x - d);
                        } else {
                            // ...to previous east-northeastern edge
                            this.adjacentFields[1] = parent.get(prev, x + d - 1, 0);
                        }
                    } else {
                        this.adjacentFields[1] = parent.get(s, x - 1, y + 1);
                    }
                }

                // 2: southwestern adjacent (y++)
                if (y < max_y) {
                    this.adjacentFields[2] = parent.get(s, x, y + 1);
                } else {
                    if (x == max_x && y == max_y) {
                        this.adjacentFields[2] = parent.getSouth();
                    } else {
                        // attach southwestern edge...
                        if (x >= d) {
                            // ...to previous southeastern edge
                            this.adjacentFields[2] = parent.get(prev, max_x, x - d + 1);
                        } else {
                            // ...to previous east-northeastern edge
                            this.adjacentFields[2] = parent.get(prev, x + d, 0);
                        }
                    }
                }

                if (isPentagon) {
                    // the last two aren't the same for pentagons
                    if (x == d - 1) {
                        // this is the northern tropical pentagon
                        this.adjacentFields[3] = parent.get(s, x + 1, 0);
                        this.adjacentFields[4] = parent.get(next, 0, max_y);
                    } else if (x == max_x) {
                        // this is the southern tropical pentagon
                        this.adjacentFields[3] = parent.get(next, d, max_y);
                        this.adjacentFields[4] = parent.get(next, d - 1, max_y);
                    }
                } else {
                    // 3: southeastern adjacent (x++)
                    if (x == max_x) {
                        this.adjacentFields[3] = parent.get(next, y + d, max_y);
                    } else {
                        this.adjacentFields[3] = parent.get(s, x + 1, y);
                    }

                    // 4: eastern adjacent (x++, y--)
                    if (x == max_x) {
                        this.adjacentFields[4] = parent.get(next, y + d - 1, max_y);
                    } else {
                        if (y == 0) {
                            // attach northeastern side to...
                            if (x < d) {
                                // ...to next northwestern edge
                                this.adjacentFields[4] = parent.get(next, 0, x + 1);
                            } else {
                                // ...to next west-southwestern edge
                                this.adjacentFields[4] = parent.get(next, x - d + 1, max_y);
                            }
                        } else {
                            this.adjacentFields[4] = parent.get(s, x + 1, y - 1);
                        }
                    }

                    // 5: northeastern adjacent (y--)
                    if (y > 0) {
                        this.adjacentFields[5] = parent.get(s, x, y - 1);
                    } else {
                        if (y == 0) {
                            // attach northeastern side to...
                            if (x < d) {
                                // ...to next northwestern edge
                                this.adjacentFields[5] = parent.get(next, 0, x);
                            } else {
                                // ...to next west-southwestern edge
                                this.adjacentFields[5] = parent.get(next, x - d, max_y);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isPentagon() {
        return isPentagon;
    }

    /**
     * Returns a field's edge vertices and its bounds. Latitudinal coordinates may be
     * greater than π if the field straddles the meridian across from 0.
     */
   public Position[] getVertices() {
       Position[] vertices = new Position[adjacentFields.length];
        Position[] ifc = this.parent.getInterfieldCentroids();
        int[]    ifi = this.parent.getInterfieldIndices();

        for (int v = 0; v < this.adjacentFields.length; v++) {
            vertices[v] = ifc[ifi[6 * index + v]];
        }

        return vertices;
    }

    public LatLng[] getLatLon() {
        LatLng[] vertices = new LatLng[adjacentFields.length];

        Position[] ifc = this.parent.getInterfieldCentroids();
        int[]    ifi = this.parent.getInterfieldIndices();

        for (int v = 0; v < this.adjacentFields.length; v++) {
            Position position = ifc[ifi[6 * index + v]];
            vertices[v] = position.getLatLng();
        }

        return vertices;
    }

    public double[] getLngs() {
        double[] lngs = new double[adjacentFields.length];

        Position[] ifc = this.parent.getInterfieldCentroids();
        int[]    ifi = this.parent.getInterfieldIndices();

        for (int v = this.adjacentFields.length - 1; v >= 0 ; v--) {
            Position position = ifc[ifi[6 * index + v]];
            lngs[v] = position.getLng();
        }

        return lngs;
    }

    public void getInterfieldTriangles(int[] interfieldTriangles) {
        if (index > 1) { // not North or South
            int n1i = this.getAdjacent(0).getIndex();
            int n2i = this.getAdjacent(1).getIndex();
            int n3i = this.getAdjacent(2).getIndex();
            int f1 = index * 2 - 4;
            int f2 = index * 2 - 3;

            interfieldTriangles[f1 * 3 + 0] = n2i;
            interfieldTriangles[f1 * 3 + 1] = n1i;
            interfieldTriangles[f1 * 3 + 2] = index;

            interfieldTriangles[f2 * 3 + 0] = n3i;
            interfieldTriangles[f2 * 3 + 1] = n2i;
            interfieldTriangles[f2 * 3 + 2] = index;
        }
    }

    public void getInterfieldCentroids(int[] triangles, Position[] interfieldCentroids, Position[] positions, HexField[] fields) {
        Position firstPos = positions[fields[triangles[3 * index]].getIndex()];
        Position centroid = firstPos.centroid(
                positions[fields[triangles[3 * index + 1]].getIndex()],
                positions[fields[triangles[3 * index + 2]].getIndex()]
        );
        interfieldCentroids[index] = centroid;
    }


    public void getInterfieldCentroids(int[] triangles, int centroidIndex, Position[] interfieldCentroids) {
        int fieldIndex = 3 * centroidIndex;
        HexField secondField = parent.getFields()[triangles[fieldIndex + 1]];
        HexField thirdField = parent.getFields()[triangles[fieldIndex + 2]];

        System.out.println("Getting centroid: " + centroidIndex + " and fp index " + this.getIndex());
        Position firstPos = parent.getPositions()[this.getIndex()];
        Position secondPos = parent.getPositions()[secondField.getIndex()];
        Position thirdPos = parent.getPositions()[thirdField.getIndex()];
        Position centroid = firstPos.centroid(secondPos, thirdPos);
        interfieldCentroids[centroidIndex] = centroid;
    }

    public void getInterfieldIndices(int[] interfieldIndices, int[] interfieldTriangles) {
        int sides = getAdjacentFields().length;

        for (int s = 0; s < sides; s += 1) {
            interfieldIndices[6 * index + s] = getTriangleIndex(interfieldTriangles, s);
        }
    }

    public int getTriangleIndex(int[] triangles, int side) {

        int fi1 = index;

        int sides = getAdjacentFields().length;
        int fi2 = getAdjacent(side).getIndex();
        int fi3 = getAdjacent((side + 1) % sides).getIndex();

        int c = faceIndex(fi1, fi2, fi3, triangles);
        if (c >= 0) return c;

        c = faceIndex(fi2, fi1, fi3, triangles);
        if (c >= 0) return c;

        c = faceIndex(fi3, fi1, fi2, triangles);
        if (c >= 0) return c;

        throw new Error("`Could not find triangle index for faces: " + fi1 + ", " + fi2 + ", " + fi3);

    }



    public int faceIndex(int i, int a1, int a2, int[] ts) {
        int f1 = i * 2 - 4;
        int f2 = i * 2 - 3;
        int index = -1;

        if (f1 >= 0 && ((ts[f1 * 3 + 1] == a1 || ts[f1 * 3 + 1] == a2) &&
                (ts[f1 * 3 + 0] == a1 || ts[f1 * 3 + 0] == a2))) {
            index = f1;
        }

        if (f2 >= 0 && ((ts[f2 * 3 + 1] == a1 || ts[f2 * 3 + 1] == a2) &&
                (ts[f2 * 3 + 0] == a1 || ts[f2 * 3 + 0] == a2))) {
            index = f2;
        }

        return index;
    }

    /**
     */
    public double[] getLats() {
        double[] lats = new double[adjacentFields.length];

        Position[] ifc = this.parent.getInterfieldCentroids();
        int[]    ifi = this.parent.getInterfieldIndices();

        for (int v = this.adjacentFields.length - 1; v >= 0; v--) {
          Position position = ifc[ifi[6 * index + v]];
          lats[v] = position.getLat();
        }

        return lats;
    }

    public String toString() {
        Position[] vertices = getVertices();
        StringBuffer buff = new StringBuffer();
        for(int i=0; i < vertices.length; i++) {
            buff.append(vertices[i]).append("\n");
        }
        return buff.toString();
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }
}

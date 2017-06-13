package com.d4dl.permean.mesh;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by joshuadeford on 5/29/17.
 */
public class CellProxy implements Serializable {

    private Sphere parent;
    private int index;
    private final boolean isPentagon;
    private CellProxy[] adjacentCells;
    private Position position;
    private double area;

    CellProxy(Sphere parent, int index) {
        this.parent = parent;
        this.index = index;
        isPentagon = ((index < 2) || (getSxy()[2] == 0 && ((getSxy()[1] + 1) % parent.getDivisions()) == 0));
    }

    public int[] getSxy() {
        if (index < 2) {
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

    public CellProxy[] getAdjacentCells() {
        return adjacentCells;
    }

    public void setAdjacentCells(CellProxy[] adjacentCells) {
        this.adjacentCells = adjacentCells;
    }

    public int getIndex() {
        return index;
    }


    public CellProxy getAdjacent(int p) {
        return this.adjacentCells[p];
    }

    public CellProxy[] getAdjacents() {
        return this.adjacentCells;
    }

    public void link() {
        {
            int d = parent.getDivisions();
            int[] sxy = getSxy();
            ;
            int max_x = d * 2 - 1;
            int max_y = d - 1;

            // Link polar pentagons to the adjacent cells
            if (this.index == 0) {
                this.adjacentCells = new CellProxy[]{
                        parent.get(0, 0, 0),
                        parent.get(1, 0, 0),
                        parent.get(2, 0, 0),
                        parent.get(3, 0, 0),
                        parent.get(4, 0, 0)
                };
            } else if (this.index == 1) {
                this.adjacentCells = new CellProxy[]{
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

                this.adjacentCells = new CellProxy[isPentagon ? 5 : 6];

                // 0: northwestern adjacent (x--)
                if (x > 0) {
                    this.adjacentCells[0] = parent.get(s, x - 1, y);
                } else {
                    if (y == 0) {
                        this.adjacentCells[0] = parent.getNorth();
                    } else {
                        this.adjacentCells[0] = parent.get(prev, y - 1, 0);
                    }
                }

                // 1: western adjacent (x--, y++)
                if (x == 0) {
                    // attach northwestern edge to previous north-northeastern edge
                    this.adjacentCells[1] = parent.get(prev, y, 0);
                } else {
                    if (y == max_y) {
                        // attach southwestern edge...
                        if (x > d) {
                            // ...to previous southeastern edge
                            this.adjacentCells[1] = parent.get(prev, max_x, x - d);
                        } else {
                            // ...to previous east-northeastern edge
                            this.adjacentCells[1] = parent.get(prev, x + d - 1, 0);
                        }
                    } else {
                        this.adjacentCells[1] = parent.get(s, x - 1, y + 1);
                    }
                }

                // 2: southwestern adjacent (y++)
                if (y < max_y) {
                    this.adjacentCells[2] = parent.get(s, x, y + 1);
                } else {
                    if (x == max_x && y == max_y) {
                        this.adjacentCells[2] = parent.getSouth();
                    } else {
                        // attach southwestern edge...
                        if (x >= d) {
                            // ...to previous southeastern edge
                            this.adjacentCells[2] = parent.get(prev, max_x, x - d + 1);
                        } else {
                            // ...to previous east-northeastern edge
                            this.adjacentCells[2] = parent.get(prev, x + d, 0);
                        }
                    }
                }

                if (isPentagon) {
                    // the last two aren't the same for pentagons
                    if (x == d - 1) {
                        // this is the northern tropical pentagon
                        this.adjacentCells[3] = parent.get(s, x + 1, 0);
                        this.adjacentCells[4] = parent.get(next, 0, max_y);
                    } else if (x == max_x) {
                        // this is the southern tropical pentagon
                        this.adjacentCells[3] = parent.get(next, d, max_y);
                        this.adjacentCells[4] = parent.get(next, d - 1, max_y);
                    }
                } else {
                    // 3: southeastern adjacent (x++)
                    if (x == max_x) {
                        this.adjacentCells[3] = parent.get(next, y + d, max_y);
                    } else {
                        this.adjacentCells[3] = parent.get(s, x + 1, y);
                    }

                    // 4: eastern adjacent (x++, y--)
                    if (x == max_x) {
                        this.adjacentCells[4] = parent.get(next, y + d - 1, max_y);
                    } else {
                        if (y == 0) {
                            // attach northeastern side to...
                            if (x < d) {
                                // ...to next northwestern edge
                                this.adjacentCells[4] = parent.get(next, 0, x + 1);
                            } else {
                                // ...to next west-southwestern edge
                                this.adjacentCells[4] = parent.get(next, x - d + 1, max_y);
                            }
                        } else {
                            this.adjacentCells[4] = parent.get(s, x + 1, y - 1);
                        }
                    }

                    // 5: northeastern adjacent (y--)
                    if (y > 0) {
                        this.adjacentCells[5] = parent.get(s, x, y - 1);
                    } else {
                        if (y == 0) {
                            // attach northeastern side to...
                            if (x < d) {
                                // ...to next northwestern edge
                                this.adjacentCells[5] = parent.get(next, 0, x);
                            } else {
                                // ...to next west-southwestern edge
                                this.adjacentCells[5] = parent.get(next, x - d, max_y);
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
     * Returns a cell's edge vertices and its bounds. Latitudinal coordinates may be
     * greater than π if the cell straddles the meridian across from 0.
     */
    public Position[] getVertices() {
        Position[] vertices = new Position[adjacentCells.length];
        Map<Integer, Position> ifc = this.parent.getIntercellCentroids();
        Map<Integer, Integer> ifi = this.parent.getIntercellIndices();

        for (int v = 0; v < this.adjacentCells.length; v++) {
            vertices[v] = ifc.get(ifi.get(6 * index + v));
        }

        return vertices;
    }


    public int[] getVertexIndexes() {
        int[] vertices = new int[adjacentCells.length];
        Map<Integer, Integer> ifi = this.parent.getIntercellIndices();

        for (int v = 0; v < this.adjacentCells.length; v++) {
            vertices[v] = ifi.get(6 * index + v);
        }

        return vertices;
    }

    public Position[] getLatLon() {
        Position[] vertices = new Position[adjacentCells.length];

        Map<Integer, Position> ifc = this.parent.getIntercellCentroids();
        Map<Integer, Integer> ifi = this.parent.getIntercellIndices();

        for (int v = 0; v < this.adjacentCells.length; v++) {
            Position position = ifc.get(ifi.get(6 * index + v));
            vertices[v] = position;
        }

        return vertices;
    }


    public String toString() {
        Position[] vertices = getVertices();
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < vertices.length; i++) {
            if(i > 0) {
                buff.append(",");
            }
            buff.append(vertices[i]);
        }
        return buff.toString();
    }


    /**
    public String toString() {
        Position[] vertices = getVertices();
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < vertices.length; i++) {
            buff.append("\t\t<Placemark>\n" +
                    "\t\t\t<name>Cell " + index + " - " + i + "</name>\n" +
                    "\t\t\t<styleUrl>#msn_bus</styleUrl>\n" +
                    "\t\t\t<Point>\n" +
                    "\t\t\t\t<gx:drawOrder>1</gx:drawOrder>\n" +
                    "\t\t\t\t<coordinates>");
            buff.append(vertices[i]);
            buff.append("\t\t\t</coordinates></Point>\n" +
                    "\t\t</Placemark>\n");
        }
        //buff.append(vertices[0]);
        return buff.toString();
    }
    */

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public void getIntercellTriangles(Map<Integer, Integer> intercellTriangles) {
        if (index > 1) { // not North or South
            int n1i = this.getAdjacent(0).getIndex();
            int n2i = this.getAdjacent(1).getIndex();
            int n3i = this.getAdjacent(2).getIndex();
            int f1 = index * 2 - 4;
            int f2 = index * 2 - 3;

            intercellTriangles.put(f1 * 3 + 0, n2i);
            intercellTriangles.put(f1 * 3 + 1, n1i);
            intercellTriangles.put(f1 * 3 + 2, index);

            intercellTriangles.put(f2 * 3 + 0, n3i);
            intercellTriangles.put(f2 * 3 + 1, n2i);
            intercellTriangles.put(f2 * 3 + 2, index);
        }

    }

    public Position getIntercellCentroids(Map<Integer, Integer> triangles, int centroidIndex) {
        int cellIndex = 3 * centroidIndex;
        CellProxy secondCell = parent.getCellProxies()[triangles.get(cellIndex + 1)];
        CellProxy thirdCell = parent.getCellProxies()[triangles.get(cellIndex + 2)];

        //System.out.println("Getting centroid: " + centroidIndex + " and fp index " + this.getIndex());
        Position firstPos = parent.getCellProxies()[this.getIndex()].getPosition();
        Position secondPos = parent.getCellProxies()[secondCell.getIndex()].getPosition();
        Position thirdPos = parent.getCellProxies()[thirdCell.getIndex()].getPosition();
        Position centroid = firstPos.centroid(secondPos, thirdPos);
        //System.out.println(centroid);

        return centroid;
    }

    /**
     * Populates intercell indexes
     * @param intercellIndices
     * @param intercellTriangles
     * @return
     */
    public void getIntercellIndices(Map<Integer, Integer> intercellIndices, Map<Integer, Integer> intercellTriangles) {
        int sides = getAdjacentCells().length;

        for (int s = 0; s < sides; s += 1) {
            int index = 6 * this.index + s;
            intercellIndices.put(index, getTriangleIndex(intercellTriangles, s));
        }
    }

    public int[] getIntercellIndices(Map<Integer, Integer> intercellTriangles) {
        int sides = getAdjacentCells().length;
        int[] indexes = new int[sides];

        for (int s = 0; s < sides; s += 1) {
            int index = 6 * this.index + s;
            indexes[s] = index;
        }
        return indexes;
    }

    public int getTriangleIndex(Map<Integer, Integer> triangles, int side) {

        int fi1 = index;

        int sides = getAdjacentCells().length;
        int fi2 = getAdjacent(side).getIndex();
        int fi3 = getAdjacent((side + 1) % sides).getIndex();

        int c = faceIndex(fi1, fi2, fi3, triangles);
        if (c >= 0) return c;

        c = faceIndex(fi2, fi1, fi3, triangles);
        if (c >= 0) return c;

        c = faceIndex(fi3, fi1, fi2, triangles);
        if (c >= 0) return c;

        throw new Error("Could not find triangle index for faces: " + fi1 + ", " + fi2 + ", " + fi3);

    }


    public int faceIndex(int i, int a1, int a2, Map<Integer, Integer> ts) {
        int f1 = i * 2 - 4;
        int f2 = i * 2 - 3;
        int index = -1;

        if (f1 >= 0 && ((ts.get(f1 * 3 + 1) == a1 || ts.get(f1 * 3 + 1) == a2) &&
                (ts.get(f1 * 3 + 0) == a1 || ts.get(f1 * 3 + 0) == a2))) {
            index = f1;
        }

        if (f2 >= 0 && ((ts.get(f2 * 3 + 1) == a1 || ts.get(f2 * 3 + 1) == a2) &&
                (ts.get(f2 * 3 + 0) == a1 || ts.get(f2 * 3 + 0) == a2))) {
            index = f2;
        }

        return index;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setPosition(double φ, double λ) {
        this.position = new Position(φ, λ);
    }
}

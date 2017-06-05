package mesh;

import com.d4dl.mesh.LatLng;

import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is not a thread safe class. In fact, no two threads should be operating on two different
 * cells at the same time.  To take advantage of multi-threading have a working thread processing a queue
 * of cells to populate their triangles.
 */
public class Cell {
    private int index;

    private Sphere parent;
    private Cell[] adjacentCells;


    private int[] vertexIndices;
    private static final int SIX_CELLS = (1 << 6) ^ (1 << 5) ^ (1 << 4) ^ (1 << 3) ^ (1 << 2) ^ (1 << 1);
    private static final int FIVE_CELLS = (1 << 5) ^ (1 << 4) ^ (1 << 3) ^ (1 << 2) ^ (1 << 1);
    //Used to determine when all vertices are set
    //private int vertexMask;
    //Used to determine when all adjacent cells have positions
    private int adjacentCellsWithPositions;
    //When all neighbors are complete a lot of stuff can be cleaned up to conserve memory
    private AtomicInteger completeNeighborCount = new AtomicInteger(0);
    private AtomicInteger readyNeighborCount = new AtomicInteger(0);
    private Map<Cell, Map<Cell, Integer>> commonVertices;
    private Position position;

    private final boolean isPentagon;
    //Arbitrary data allowed. Its stored in a list so the correct data can be provided for the current iteration
    private HashMap currentData;
    private HashMap newData;
    private String name;
    private double area;
    private boolean latLngIsPopulated;

    Cell(Sphere parent, int index, HashMap data) {
        this.parent = parent;
        this.index = index;
        this.currentData = data;
        isPentagon = ((index < 2) || (getSxy()[2] == 0 && ((getSxy()[1] + 1) % parent.getDivisions()) == 0));
    }

    private void initVertexCalculationObjects() {
        if(vertexIndices == null) {
            commonVertices = new HashMap();
            for(Cell neighbor : adjacentCells) {
                commonVertices.put(neighbor, new HashMap());
            }
            if (isPentagon) {
                vertexIndices = new int[]{-1, -1, -1, -1, -1};
            } else {
                vertexIndices = new int[]{-1, -1, -1, -1, -1, -1};
            }
        }
    }

    public void clean() {
        //Clean up memory
        position = null;
        parent = null;
        adjacentCells = null;
        completeNeighborCount = null;
        commonVertices = null;
    }

    public boolean canBeCleaned() {
        return completeNeighborCount.get() == adjacentCells.length;
    }

    public boolean canTakeCoordinates() {
        return readyNeighborCount.get() == adjacentCells.length;
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

    public int[] getVertexIndices() {
        return vertexIndices;
    }
    public HashMap getData() {
        return this.currentData;
    }

    public void setData(HashMap newData) {
        this.newData = newData;
    }

    public Cell[] getAdjacentCells() {
        return adjacentCells;
    }

    public void setAdjacentCells(Cell[] adjacentCells) {
        this.adjacentCells = adjacentCells;
    }

    void finishIteration() {
        if (this.parent.isIterating() == true) {
            throw new IllegalStateException("Iteration's cannot be finished while the parent is still iterating.  This should be called by the parent after iterating is complete.");
        }
    }

    public int getIndex() {
        return index;
    }


    public Cell getAdjacent(int p) {
        return this.adjacentCells[p];
    }

    public Cell[] getAdjacents() {
        return this.adjacentCells;
    }

    /**
     * Sets the
     * @param vertices
     * @return
     */
    public void setLatLngIndices(AtomicInteger nextIndex, LatLng[] vertices) {
        initVertexCalculationObjects();
        int sides = adjacentCells.length;
        for(int i = 0; i < sides; i++) {
            Cell neighbor1 = adjacentCells[i];
            Cell neighbor2 = adjacentCells[(1+i)%sides];
            Integer sharedVertexIndex = vertexIndices[i] >= 0 ? vertexIndices[i] : null;
            if(sharedVertexIndex == null) {
                sharedVertexIndex = getSharedVertexIndex(neighbor1, neighbor2);
            }
            if (sharedVertexIndex == null) {
                Position newVertex = getPosition().centroid(neighbor1.getPosition(), neighbor2.getPosition());
                sharedVertexIndex = nextIndex.getAndIncrement();
                vertices[sharedVertexIndex] = newVertex.getLatLng();
            }
            setSharedVertexIndex(neighbor1, neighbor2, i, sharedVertexIndex);
            setVertex(i, sharedVertexIndex);
            neighbor1.completeNeighborCount.incrementAndGet();
        }
    }

    private Integer getSharedVertexIndex(Cell neighbor1, Cell neighbor2) {
        initVertexCalculationObjects();
        Integer shared = commonVertices.get(neighbor2).get(neighbor1);
        if(shared == null) {
            shared = commonVertices.get(neighbor1).get(neighbor2);
        }
        return shared;
    }

    private void setSharedVertexIndex(Cell neighbor1, Cell neighbor2, int neighborIndex, Integer index) {
        setSharedVertexIndex(neighbor1, neighbor2, neighborIndex, index, true);
    }


    private void setSharedVertexIndex(Cell neighbor1, Cell neighbor2, int index, Integer vertex, boolean deep) {
        initVertexCalculationObjects();
        commonVertices.get(neighbor1).put(neighbor2, vertex);
        setVertex(index, vertex);
        if(deep) {
            for(int i=0; i < neighbor1.adjacentCells.length; i++) {
                if(neighbor1.adjacentCells[i] == this) {
                    neighbor1.setSharedVertexIndex(this, neighbor2, i, vertex, false);
                }
            }
            for(int i=0; i < neighbor2.adjacentCells.length; i++) {
                if(neighbor2.adjacentCells[i] == this) {
                    neighbor2.setSharedVertexIndex(this, neighbor1, i, vertex, false);
                }
            }
        }
    }

    private void setVertex(int index, int vertex) {
        //vertexMask = vertexMask ^ (index + 1);
        this.vertexIndices[index] = vertex;
    }

    public void linkNeighboringCells() {
        int d = parent.getDivisions();
        int[] sxy = getSxy();
        int max_x = d * 2 - 1;
        int max_y = d - 1;

        // Link polar pentagons to the adjacent cells
        if (this.index == 0) {
            this.adjacentCells = new Cell[]{
                    parent.getCellBySXY(0, 0, 0),
                    parent.getCellBySXY(1, 0, 0),
                    parent.getCellBySXY(2, 0, 0),
                    parent.getCellBySXY(3, 0, 0),
                    parent.getCellBySXY(4, 0, 0)
            };
        } else if (this.index == 1) {
            this.adjacentCells = new Cell[]{
                    parent.getCellBySXY(0, max_x, max_y),
                    parent.getCellBySXY(1, max_x, max_y),
                    parent.getCellBySXY(2, max_x, max_y),
                    parent.getCellBySXY(3, max_x, max_y),
                    parent.getCellBySXY(4, max_x, max_y)
            };
        } else {
            int next = (sxy[0] + 1 + Sphere.PEELS) % Sphere.PEELS;
            int prev = (sxy[0] - 1 + Sphere.PEELS) % Sphere.PEELS;

            int s = sxy[0];
            int x = sxy[1];
            int y = sxy[2];

            this.adjacentCells = new Cell[isPentagon ? 5 : 6];

            // 0: northwestern adjacent (x--)
            if (x > 0) {
                this.adjacentCells[0] = parent.getCellBySXY(s, x - 1, y);
            } else {
                if (y == 0) {
                    this.adjacentCells[0] = parent.getNorth();
                } else {
                    this.adjacentCells[0] = parent.getCellBySXY(prev, y - 1, 0);
                }
            }

            // 1: western adjacent (x--, y++)
            if (x == 0) {
                // attach northwestern edge to previous north-northeastern edge
                this.adjacentCells[1] = parent.getCellBySXY(prev, y, 0);
            } else {
                if (y == max_y) {
                    // attach southwestern edge...
                    if (x > d) {
                        // ...to previous southeastern edge
                        this.adjacentCells[1] = parent.getCellBySXY(prev, max_x, x - d);
                    } else {
                        // ...to previous east-northeastern edge
                        this.adjacentCells[1] = parent.getCellBySXY(prev, x + d - 1, 0);
                    }
                } else {
                    this.adjacentCells[1] = parent.getCellBySXY(s, x - 1, y + 1);
                }
            }

            // 2: southwestern adjacent (y++)
            if (y < max_y) {
                this.adjacentCells[2] = parent.getCellBySXY(s, x, y + 1);
            } else {
                if (x == max_x && y == max_y) {
                    this.adjacentCells[2] = parent.getSouth();
                } else {
                    // attach southwestern edge...
                    if (x >= d) {
                        // ...to previous southeastern edge
                        this.adjacentCells[2] = parent.getCellBySXY(prev, max_x, x - d + 1);
                    } else {
                        // ...to previous east-northeastern edge
                        this.adjacentCells[2] = parent.getCellBySXY(prev, x + d, 0);
                    }
                }
            }

            if (isPentagon) {
                // the last two aren't the same for pentagons
                if (x == d - 1) {
                    // this is the northern tropical pentagon
                    this.adjacentCells[3] = parent.getCellBySXY(s, x + 1, 0);
                    this.adjacentCells[4] = parent.getCellBySXY(next, 0, max_y);
                } else if (x == max_x) {
                    // this is the southern tropical pentagon
                    this.adjacentCells[3] = parent.getCellBySXY(next, d, max_y);
                    this.adjacentCells[4] = parent.getCellBySXY(next, d - 1, max_y);
                }
            } else {
                // 3: southeastern adjacent (x++)
                if (x == max_x) {
                    this.adjacentCells[3] = parent.getCellBySXY(next, y + d, max_y);
                } else {
                    this.adjacentCells[3] = parent.getCellBySXY(s, x + 1, y);
                }

                // 4: eastern adjacent (x++, y--)
                if (x == max_x) {
                    this.adjacentCells[4] = parent.getCellBySXY(next, y + d - 1, max_y);
                } else {
                    if (y == 0) {
                        // attach northeastern side to...
                        if (x < d) {
                            // ...to next northwestern edge
                            this.adjacentCells[4] = parent.getCellBySXY(next, 0, x + 1);
                        } else {
                            // ...to next west-southwestern edge
                            this.adjacentCells[4] = parent.getCellBySXY(next, x - d + 1, max_y);
                        }
                    } else {
                        this.adjacentCells[4] = parent.getCellBySXY(s, x + 1, y - 1);
                    }
                }

                // 5: northeastern adjacent (y--)
                if (y > 0) {
                    this.adjacentCells[5] = parent.getCellBySXY(s, x, y - 1);
                } else {
                    if (y == 0) {
                        // attach northeastern side to...
                        if (x < d) {
                            // ...to next northwestern edge
                            this.adjacentCells[5] = parent.getCellBySXY(next, 0, x);
                        } else {
                            // ...to next west-southwestern edge
                            this.adjacentCells[5] = parent.getCellBySXY(next, x - d, max_y);
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
    public LatLng[] getVertices(LatLng[] latLng) {
        LatLng[] vertices = new LatLng[vertexIndices.length];
        for (int v = 0; v < this.vertexIndices.length; v++) {
            vertices[v] = latLng[vertexIndices[v]];
        }

        return vertices;
    }

    //public String toString() {
        //LatLng[] vertices = getVertices();
        //StringBuffer buff = new StringBuffer((name != null ? name : "Intercell")).append("\n");
        //for (int i = 0; i < vertices.length; i++) {
            //buff.append(vertices[i]).append("\n");
        //}
        //return buff.toString();
    //}

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position, AbstractQueue<Cell> coordinatesQueue) {
        coordinatesQueue.add(this);
        for(Cell adjacent : adjacentCells) {
            adjacent.readyNeighborCount.incrementAndGet();
        }
        this.position = position;
    }

    public void setPosition(double φ, double λ, AbstractQueue<Cell> coordinatesQueue) {
        setPosition(new Position(φ, λ), coordinatesQueue);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

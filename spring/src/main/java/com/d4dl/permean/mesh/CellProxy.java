package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

/**
 * Provides functionality to create and track data about a cell that will be used to create a cell later.
 */
public class CellProxy implements Serializable {

    private Sphere parentSphere;
    private int index;
    private final boolean isPentagon;
    private CellProxy[] adjacentCells;
    private double area;
    //Barycenter position
    private Position barycenter;
    private String name;
    private Cell cell;
    private short ownedVertexCount;

    CellProxy(Sphere parentSphere, int index) {
        this.parentSphere = parentSphere;
        this.index = index;
        isPentagon = ((index < 2) || (getSxy()[2] == 0 && ((getSxy()[1] + 1) % parentSphere.getDivisions()) == 0));
    }

    public int[] getSxy() {
        if (index < 2) {
            return null;
        } else {
            int l = index - 2;
            int x_lim = parentSphere.getDivisions() * 2;
            int y_lim = parentSphere.getDivisions();

            int s = (int) Math.floor(l / (x_lim * y_lim));
            int x = (int) Math.floor((l - s * x_lim * y_lim) / y_lim);
            int y = (l - s * x_lim * y_lim - x * y_lim);
            return new int[]{s, x, y};
        }
    }

    public int getIndex() {
        return index;
    }


    public CellProxy getAdjacent(int p) {
        return this.adjacentCells[p == adjacentCells.length ? 0 : p];
    }

    public void link() {
        {
            int d = parentSphere.getDivisions();
            int[] sxy = getSxy();
            ;
            int max_x = d * 2 - 1;
            int max_y = d - 1;

            // Link polar pentagons to the adjacent cells
            if (this.index == 0) {
                this.adjacentCells = new CellProxy[]{
                        parentSphere.get(0, 0, 0),
                        parentSphere.get(1, 0, 0),
                        parentSphere.get(2, 0, 0),
                        parentSphere.get(3, 0, 0),
                        parentSphere.get(4, 0, 0)
                };
            } else if (this.index == 1) {
                this.adjacentCells = new CellProxy[]{
                        parentSphere.get(0, max_x, max_y),
                        parentSphere.get(1, max_x, max_y),
                        parentSphere.get(2, max_x, max_y),
                        parentSphere.get(3, max_x, max_y),
                        parentSphere.get(4, max_x, max_y)
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
                    this.adjacentCells[0] = parentSphere.get(s, x - 1, y);
                } else {
                    if (y == 0) {
                        this.adjacentCells[0] = parentSphere.getNorth();
                    } else {
                        this.adjacentCells[0] = parentSphere.get(prev, y - 1, 0);
                    }
                }

                // 1: western adjacent (x--, y++)
                if (x == 0) {
                    // attach northwestern edge to previous north-northeastern edge
                    this.adjacentCells[1] = parentSphere.get(prev, y, 0);
                } else {
                    if (y == max_y) {
                        // attach southwestern edge...
                        if (x > d) {
                            // ...to previous southeastern edge
                            this.adjacentCells[1] = parentSphere.get(prev, max_x, x - d);
                        } else {
                            // ...to previous east-northeastern edge
                            this.adjacentCells[1] = parentSphere.get(prev, x + d - 1, 0);
                        }
                    } else {
                        this.adjacentCells[1] = parentSphere.get(s, x - 1, y + 1);
                    }
                }

                // 2: southwestern adjacent (y++)
                if (y < max_y) {
                    this.adjacentCells[2] = parentSphere.get(s, x, y + 1);
                } else {
                    if (x == max_x && y == max_y) {
                        this.adjacentCells[2] = parentSphere.getSouth();
                    } else {
                        // attach southwestern edge...
                        if (x >= d) {
                            // ...to previous southeastern edge
                            this.adjacentCells[2] = parentSphere.get(prev, max_x, x - d + 1);
                        } else {
                            // ...to previous east-northeastern edge
                            this.adjacentCells[2] = parentSphere.get(prev, x + d, 0);
                        }
                    }
                }

                if (isPentagon) {
                    // the last two aren't the same for pentagons
                    if (x == d - 1) {
                        // this is the northern tropical pentagon
                        this.adjacentCells[3] = parentSphere.get(s, x + 1, 0);
                        this.adjacentCells[4] = parentSphere.get(next, 0, max_y);
                    } else if (x == max_x) {
                        // this is the southern tropical pentagon
                        this.adjacentCells[3] = parentSphere.get(next, d, max_y);
                        this.adjacentCells[4] = parentSphere.get(next, d - 1, max_y);
                    }
                } else {
                    // 3: southeastern adjacent (x++)
                    if (x == max_x) {
                        this.adjacentCells[3] = parentSphere.get(next, y + d, max_y);
                    } else {
                        this.adjacentCells[3] = parentSphere.get(s, x + 1, y);
                    }

                    // 4: eastern adjacent (x++, y--)
                    if (x == max_x) {
                        this.adjacentCells[4] = parentSphere.get(next, y + d - 1, max_y);
                    } else {
                        if (y == 0) {
                            // attach northeastern side to...
                            if (x < d) {
                                // ...to next northwestern edge
                                this.adjacentCells[4] = parentSphere.get(next, 0, x + 1);
                            } else {
                                // ...to next west-southwestern edge
                                this.adjacentCells[4] = parentSphere.get(next, x - d + 1, max_y);
                            }
                        } else {
                            this.adjacentCells[4] = parentSphere.get(s, x + 1, y - 1);
                        }
                    }

                    // 5: northeastern adjacent (y--)
                    if (y > 0) {
                        this.adjacentCells[5] = parentSphere.get(s, x, y - 1);
                    } else {
                        if (y == 0) {
                            // attach northeastern side to...
                            if (x < d) {
                                // ...to next northwestern edge
                                this.adjacentCells[5] = parentSphere.get(next, 0, x);
                            } else {
                                // ...to next west-southwestern edge
                                this.adjacentCells[5] = parentSphere.get(next, x - d, max_y);
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

    /**
     * Each triangle's vertices are three neighboring cell's barycenters.
     * They are used to calculate the vertex.
     * @return vertexes that should be saved. This relies on the principle that vertex indexes
     * share vertices with two others.  if only the vertex with the smallest index of the three
     * is returned, all vertices will be guaranteed to be returned only once.
     * adding a vertex more than once (when another neighboring cell is processed) can be avoided.
     */
    public Cell populateCell() {
      if (this.cell == null) {
          UUID id = UUID.randomUUID();
          this.cell = new Cell(id, populateVertices(id), parentSphere.getDivisions(), 0, (float)getBarycenter().getLat(), (float)getBarycenter().getLng());
      }
      return this.cell;
    }

    public List<Vertex> populateVertices(UUID owningCellId) {

        List<Vertex> addedVertices = new ArrayList();
        ownedVertexCount = 0;
        for (int i = 0; i < adjacentCells.length; i++) {
            CellProxy firstAdjacent = getAdjacent(i);
            CellProxy secondAdjacent = getAdjacent(i == adjacentCells.length ? 0 : i + 1);
            //The key of the vertex is the sorted index array of the three cells that share the
            //vertex.  It should be calculated only once then placed in the map for retrieval by
            //the other two cells that share the vertex.
            int[] sharedVertexKey = new int[]{getIndex(), firstAdjacent.getIndex(), secondAdjacent.getIndex()};
            Arrays.sort(sharedVertexKey);
            UUID stableUUID = createStableUUID(sharedVertexKey);
            //These three positions represent the triangle whose vertices are the three barycenters
            //that can be used to calculate the centroid of said triangle which is the vertex that
            //the three cells share.
            Position firstPos = parentSphere.getCellProxies()[this.getIndex()].getBarycenter();
            Position secondPos = parentSphere.getCellProxies()[firstAdjacent.getIndex()].getBarycenter();
            Position thirdPos = parentSphere.getCellProxies()[secondAdjacent.getIndex()].getBarycenter();
            Position centroid = firstPos.centroid(index, secondPos, thirdPos);
            // A vertex with the same id may be created more than once.  But it shouldn't be persisted more than once.
            Vertex sharedVertex = new Vertex(stableUUID, (float)centroid.getLat(), (float)centroid.getLng());
            boolean isPersistenceOwner = this.getIndex() < firstAdjacent.getIndex() && this.getIndex() < secondAdjacent .getIndex();
            if (isPersistenceOwner) {
                ownedVertexCount++;
                sharedVertex.setShouldPersist();
            }
            addedVertices.add(sharedVertex);
        }

        return addedVertices;
    }


    @NotNull
    private UUID createStableUUID(int[] sharedVertexKey) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(32 * 3);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(sharedVertexKey);

        return UUID.nameUUIDFromBytes(byteBuffer.array());
    }

    public Position getBarycenter() {
        return barycenter;
    }

    public void setBarycenter(double φ, double λ) {
        this.barycenter = new Position(φ, λ);
        parentSphere.incrementBarycenterCount();
    }

    public void setName(String name) {
        if(this.name != null) {
            throw new IllegalStateException("The cell " + this.name + " cannot be renamed to " + name);
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public double getArea() {
        return area;
    }

    public Cell getCell() {
      if (this.cell == null) {
          throw new IllegalStateException("Populate Cell must be called before trying to retrieve the cell");
      }
      return cell;
    }

    public short getOwnedVertexCount() {
        return ownedVertexCount;
    }
}

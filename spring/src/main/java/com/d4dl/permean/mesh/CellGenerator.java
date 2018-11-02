package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import com.d4dl.permean.io.SizeManager;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class CellGenerator {

  public static final int EUCLID_DETERMINED_PENTAGONS_IN_A_TRUNCATED_ICOSOHEDRON = 12;
  private final Position[] barycenters;
  private final ProgressReporter reporter;
  private final int sphereDivisions;

  private final int maxX;
  private final int maxY;

  public CellGenerator(int cellCount, int sphereDivisions, ProgressReporter reporter) {
    this.barycenters = new Position[cellCount];
    this.reporter = reporter;
    this.sphereDivisions = sphereDivisions;
    maxX = sphereDivisions * 2 - 1;
    maxY = sphereDivisions - 1;
  }


  public Position getBarycenter(int cellIndex) {
    return barycenters[cellIndex];
  }

  public void addBarycenter(int cellIndex, double φ, double λ) {
    this.barycenters[cellIndex] = new Position(φ, λ);
    reporter.incrementBarycenterCount();
  }


  /**
   * Each triangle's vertices are three neighboring cell's barycenters.
   * They are used to calculate the vertex.
   * @return vertexes that should be saved. This relies on the principle that vertex indexes
   * share vertices with two others.  if only the vertex with the smallest index of the three
   * is returned, all vertices will be guaranteed to be returned only once.
   * adding a vertex more than once (when another neighboring cell is processed) can be avoided.
   */
  public Cell populateCell(int cellIndex, String initiator, Map<String, Vertex> seenVertexMap, AtomicInteger currentVertexIndex) {
    UUID id = UUID.randomUUID();
    return new Cell(initiator, id, populateVertices(cellIndex, seenVertexMap, currentVertexIndex), 0, (float)barycenters[cellIndex].getLat(), (float)barycenters[cellIndex].getLng());
  }

  public Vertex[] populateVertices(int cellIndex, Map<String, Vertex> seenVertexMap, AtomicInteger currentVertexIndex) {

    boolean isPentagon = isPentagon(cellIndex);
    Vertex[] vertices = new Vertex[isPentagon ? 5 : 6];

    int[] adjacentCellIndices = getAdjacentCellIndices(cellIndex);
    for (int i = 0; i < adjacentCellIndices.length; i++) {
      int firstAdjacentIndex = adjacentCellIndices[i];
      int neighborIndex = (i == (adjacentCellIndices.length - 1) ? 0 : i + 1);
      int secondAdjacentIndex = adjacentCellIndices[neighborIndex];
      //The key of the vertex is the sorted index array of the three cells that share the
      //vertex.  It should be calculated only once then placed in the map for retrieval by
      //the other two cells that share the vertex.
      int[] sharedVertexKey = new int[]{cellIndex, firstAdjacentIndex, secondAdjacentIndex};
      Arrays.sort(sharedVertexKey);
      UUID stableUUID = createStableUUID(sharedVertexKey);
      Vertex sharedVertex = seenVertexMap != null ? seenVertexMap.get(stableUUID.toString()) : null;
      if (sharedVertex == null) {
        //These three positions represent the triangle whose vertices are the three barycenters
        //that can be used to calculate the centroid of said triangle which is the vertex that
        //the three cells share.
        Position firstPos = barycenters[cellIndex];
        Position secondPos = barycenters[firstAdjacentIndex];
        Position thirdPos = barycenters[secondAdjacentIndex];
        Position centroid = firstPos.centroid(cellIndex, secondPos, thirdPos);
        if (currentVertexIndex != null) {
          int vertexIndex = currentVertexIndex.getAndIncrement();
          // A vertex with the same id may be created more than once.  But it shouldn't be persisted more than once.
          sharedVertex = new Vertex(stableUUID, vertexIndex, (float) centroid.getLat(), (float) centroid.getLng());
        } else {
          sharedVertex = new Vertex(stableUUID, (float) centroid.getLat(), (float) centroid.getLng());
        }
        if(seenVertexMap != null) {
          seenVertexMap.put(stableUUID.toString(), sharedVertex);
        }
      } else if (seenVertexMap != null) {
        sharedVertex.access();
        if (sharedVertex.getAccessCount() == 2) {
          seenVertexMap.remove(sharedVertex.getId().toString());
        }
      }
      vertices[i] = sharedVertex;
    }

    return vertices;
  }


  private UUID createStableUUID(int[] sharedVertexKey) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(32 * 3);
    IntBuffer intBuffer = byteBuffer.asIntBuffer();
    intBuffer.put(sharedVertexKey);

    return UUID.nameUUIDFromBytes(byteBuffer.array());
  }


  public int[] getAdjacentCellIndices(int cellIndex) {
    boolean isPentagon = isPentagon(cellIndex);
    int[] adjacentCellIndices = null;
    // Don't forget to prevent recalculation of the adjacent cells from ocurring over and over
    int[] sxy = getSxy(cellIndex);


    // Link polar pentagons to the adjacent cells
    if (cellIndex == 0) {
      adjacentCellIndices = new int[]{
          getCellIndex(0, 0, 0),
          getCellIndex(1, 0, 0),
          getCellIndex(2, 0, 0),
          getCellIndex(3, 0, 0),
          getCellIndex(4, 0, 0)
      };
    } else if (cellIndex == 1) {
      adjacentCellIndices = new int[]{
          getCellIndex(0, maxX, maxY),
          getCellIndex(1, maxX, maxY),
          getCellIndex(2, maxX, maxY),
          getCellIndex(3, maxX, maxY),
          getCellIndex(4, maxX, maxY)
      };
    } else {
      int next = (sxy[0] + 1 + Sphere.PEELS) % Sphere.PEELS;
      int prev = (sxy[0] - 1 + Sphere.PEELS) % Sphere.PEELS;

      int s = sxy[0];
      int x = sxy[1];
      int y = sxy[2];

      adjacentCellIndices = new int[isPentagon ? 5 : 6];

      // 0: northwestern adjacent (x--)
      if (x > 0) {
        adjacentCellIndices[0] = getCellIndex(s, x - 1, y);
      } else {
        if (y == 0) {
          adjacentCellIndices[0] = getNorthPentagonIndex();
        } else {
          adjacentCellIndices[0] = getCellIndex(prev, y - 1, 0);
        }
      }

      // 1: western adjacent (x--, y++)
      if (x == 0) {
        // attach northwestern edge to previous north-northeastern edge
        adjacentCellIndices[1] = getCellIndex(prev, y, 0);
      } else {
        if (y == maxY) {
          // attach southwestern edge...
          if (x > sphereDivisions) {
            // ...to previous southeastern edge
            adjacentCellIndices[1] = getCellIndex(prev, maxX, x - sphereDivisions);
          } else {
            // ...to previous east-northeastern edge
            adjacentCellIndices[1] = getCellIndex(prev, x + sphereDivisions - 1, 0);
          }
        } else {
          adjacentCellIndices[1] = getCellIndex(s, x - 1, y + 1);
        }
      }
      adjacentCellIndices[2] = getThirdNeighbor(prev, s, x, y);

      if (isPentagon) {
        // the last two aren't the same for pentagons
        if (x == sphereDivisions - 1) {
          // this is the northern tropical pentagon
          adjacentCellIndices[3] = getCellIndex(s, x + 1, 0);
          adjacentCellIndices[4] = getCellIndex(next, 0, maxY);
        } else if (x == maxX) {
          // this is the southern tropical pentagon
          adjacentCellIndices[3] = getCellIndex(next, sphereDivisions, maxY);
          adjacentCellIndices[4] = getCellIndex(next, sphereDivisions - 1, maxY);
        }
      } else {
        // 3: southeastern adjacent (x++)
        if (x == maxX) {
          adjacentCellIndices[3] = getCellIndex(next, y + sphereDivisions, maxY);
        } else {
          adjacentCellIndices[3] = getCellIndex(s, x + 1, y);
        }

        // 4: eastern adjacent (x++, y--)
        if (x == maxX) {
          adjacentCellIndices[4] = getCellIndex(next, y + sphereDivisions - 1, maxY);
        } else {
          if (y == 0) {
            // attach northeastern side to...
            if (x < sphereDivisions) {
              // ...to next northwestern edge
              adjacentCellIndices[4] = getCellIndex(next, 0, x + 1);
            } else {
              // ...to next west-southwestern edge
              adjacentCellIndices[4] = getCellIndex(next, x - sphereDivisions + 1, maxY);
            }
          } else {
            adjacentCellIndices[4] = getCellIndex(s, x + 1, y - 1);
          }
        }

        // 5: northeastern adjacent (y--)
        if (y > 0) {
          adjacentCellIndices[5] = getCellIndex(s, x, y - 1);
        } else {
          if (y == 0) {
            // attach northeastern side to...
            if (x < sphereDivisions) {
              // ...to next northwestern edge
              adjacentCellIndices[5] = getCellIndex(next, 0, x);
            } else {
              // ...to next west-southwestern edge
              adjacentCellIndices[5] = getCellIndex(next, x - sphereDivisions, maxY);
            }
          }
        }
      }
    }
    return adjacentCellIndices;
  }

  public static void main(String args[]) {
    int divisions = 3333;
    SizeManager sizeManager = new SizeManager();
    int cellCount = sizeManager.calculateCellCount(divisions);
    int vertexCount = sizeManager.calculateVertexCount(divisions);
    AtomicInteger cellCounter = new AtomicInteger();

    CellGenerator cg = new CellGenerator(cellCount, divisions, null);
    System.out.println("Beginning the pentagon hunt");
    IntStream parallel = IntStream.range(0, cellCount).parallel();
    parallel.forEach(f -> {
      if(cg.isPentagon(f)) {
        System.out.println(cellCounter.incrementAndGet() + ") Found a pentagon at " + f);
      }
    });
    System.out.println("Finished hunting for pentagons");
  }

  /**
   * Allows for the retreival of pentagons first so they can be persisted first
   * @return
   */
  public Cell[] getPentagons(int cellCount, Map<String, Vertex> vertexMap, AtomicInteger currentVertexIndex) {
    Cell[] pentagons = new Cell[EUCLID_DETERMINED_PENTAGONS_IN_A_TRUNCATED_ICOSOHEDRON];
    AtomicInteger cellCounter = new AtomicInteger();
    IntStream parallel = IntStream.range(0, cellCount).parallel();
    parallel.forEach(f -> {
      if(isPentagon(f)) {
        System.out.println("Found Pentagon " + f);
        pentagons[cellCounter.getAndIncrement()]  = populateCell(f, f % 4 == 0 ? Sphere.initiatorKey18Percent : Sphere.initiatorKey82Percent, vertexMap, currentVertexIndex);
      }
    });

    return pentagons;
  }

  public boolean isPentagon(int cellIndex) {
    return (cellIndex < 2) || (getSxy(cellIndex)[2] == 0 && ((getSxy(cellIndex)[1] + 1) % sphereDivisions) == 0);
  }

  public int getThirdNeighbor(int cellIndex) {
    int[] sxy = getSxy(cellIndex);
    int prev = (sxy[0] - 1 + Sphere.PEELS) % Sphere.PEELS;

    int s = sxy[0];
    int x = sxy[1];
    int y = sxy[2];

    return getThirdNeighbor(prev, s, x, y);
  }

  private int getThirdNeighbor(int prev, int s, int x, int y) {
    int thirdNeighbor;
    // 2: southwestern adjacent (y++)
    if (y < maxY) {
      thirdNeighbor = getCellIndex(s, x, y + 1);
    } else {
      if (x == maxX && y == maxY) {
        thirdNeighbor = getSouthPentagonIndex();
      } else {
        // attach southwestern edge...
        if (x >= sphereDivisions) {
          // ...to previous southeastern edge
          thirdNeighbor = getCellIndex(prev, maxX, x - sphereDivisions + 1);
        } else {
          // ...to previous east-northeastern edge
          thirdNeighbor = getCellIndex(prev, x + sphereDivisions, 0);
        }
      }
    }
    return thirdNeighbor;
  }

  public int[] getSxy(int cellIndex) {
    if (cellIndex < 2) {
      return null;
    } else {
      int l = cellIndex - 2;
      int x_lim = sphereDivisions * 2;
      int y_lim = sphereDivisions;

      int s = (int) Math.floor(l / (x_lim * y_lim));
      int x = (int) Math.floor((l - s * x_lim * y_lim) / y_lim);
      int y = (l - s * x_lim * y_lim - x * y_lim);
      return new int[]{s, x, y};
    }
  }

  public int getNorthPentagonIndex() {
    return 0;
  }

  public int getSouthPentagonIndex() {
    return 1;
  }

  public int getCellIndex(int s, int x, int y) {
    return s * this.sphereDivisions * this.sphereDivisions * 2 + x * this.sphereDivisions + y + 2;
  }
}

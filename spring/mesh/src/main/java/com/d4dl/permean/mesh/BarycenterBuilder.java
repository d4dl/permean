package com.d4dl.permean.mesh;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.acos;
import static java.lang.StrictMath.asin;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.sqrt;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class BarycenterBuilder {

  private final CellGenerator cellGenerator;
  private int divisions;

  public static final double L = acos(sqrt(5) / 5); // the spherical arclength of the icosahedron's edges.

  public BarycenterBuilder(CellGenerator cellGenerator, int divisions) {
    this.cellGenerator = cellGenerator;
    this.divisions = divisions;
  }
  /**
   * Sets the barycenter position of every cell on a Sphere.
   *
   * @this {Sphere}
   */
  public void populateBarycenters() {
    int max_x = 2 * divisions - 1;
    double[] buf = new double[((divisions - 1) * 2)];


    // Determine position for polar and tropical pentagon cells using only arithmetic.
    cellGenerator.addBarycenter(cellGenerator.getNorthPentagonIndex(), PI / 2, 0);
    cellGenerator.addBarycenter(cellGenerator.getSouthPentagonIndex(), PI / -2, 0);

    //Set the other 10 pentagon's barycenters
    for (int s = 0; s < Sphere.PEELS; s += 1) {
      double λNorth = ((double) s) * 2 / 5 * PI;
      double λSouth = ((double) s) * 2 / 5 * PI + PI / 5;

      cellGenerator.addBarycenter(cellGenerator.getCellIndex(s, divisions - 1, 0), PI / 2 - L, λNorth);
      cellGenerator.addBarycenter(cellGenerator.getCellIndex(s, max_x, 0), PI / -2 + L, λSouth);
    }

    // Determine positions for the cells along the edges using arc interpolation.

    if ((divisions - 1) > 0) { // divisions must be at least 2 for there to be cells between vertices.

      //PEELS is 5 so loop 5 times.
      for (final AtomicInteger s = new AtomicInteger(0); s.get() < Sphere.PEELS; s.incrementAndGet()) {
        int p = (s.get() + 4) % Sphere.PEELS;
        int northPole = 0;
        int southPole = 1;
        int currentNorthTropicalPentagon = cellGenerator.getCellIndex(s.get(), divisions - 1, 0);
        int previousNorthTropicalPentagon = cellGenerator.getCellIndex(p, divisions - 1, 0);
        int currentSouthTropicalPentagon = cellGenerator.getCellIndex(s.get(), max_x, 0);
        int previousSoutTropicalPentagon = cellGenerator.getCellIndex(p, max_x, 0);

        // north pole to current north tropical pentagon
        cellGenerator.getBarycenter(northPole).interpolate(cellGenerator.getBarycenter(currentNorthTropicalPentagon), divisions, buf);
        IntStream.range(1, divisions).parallel().forEach(i -> {
          //for(int i=1; i < divisions; i++) {
          int cellIndex = cellGenerator.getCellIndex(s.get(), i - 1, 0);
          cellGenerator.addBarycenter(cellIndex, buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
        });

        // current north tropical pentagon to previous north tropical pentagon
        cellGenerator.getBarycenter(currentNorthTropicalPentagon).interpolate(cellGenerator.getBarycenter(previousNorthTropicalPentagon), divisions, buf);
        IntStream.range(1, divisions).parallel().forEach(i -> {
          //for(int i=1; i < divisions; i++) {
          int cellIndex = cellGenerator.getCellIndex(s.get(), divisions - 1 - i, i);
          cellGenerator.addBarycenter(cellIndex, buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
        });

        // current north tropical pentagon to previous south tropical pentagon
        cellGenerator.getBarycenter(currentNorthTropicalPentagon).interpolate(cellGenerator.getBarycenter(previousSoutTropicalPentagon), divisions, buf);
        IntStream.range(1, divisions).parallel().forEach(i -> {
          //for(int i=1; i < divisions; i++) {
          int cellIndex = cellGenerator.getCellIndex(s.get(), divisions - 1, i);
          cellGenerator.addBarycenter(cellIndex, buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
          //}
        });

        // current north tropical pentagon to current south tropical pentagon
        cellGenerator.getBarycenter(currentNorthTropicalPentagon).interpolate(cellGenerator.getBarycenter(currentSouthTropicalPentagon), divisions, buf);
        IntStream.range(1, divisions).parallel().forEach(i -> {
          //for(int i=1; i < divisions; i++) {
          int cellIndex = cellGenerator.getCellIndex(s.get(), divisions - 1 + i, 0);
          cellGenerator.addBarycenter(cellIndex, buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
          //}
        });

        // current south tropical pentagon to previous south tropical pentagon
        cellGenerator.getBarycenter(currentSouthTropicalPentagon).interpolate(cellGenerator.getBarycenter(previousSoutTropicalPentagon), divisions, buf);
        //for(int i=1; i < divisions; i++) {
        IntStream.range(1, divisions).parallel().forEach(i -> {
          int cellIndex = cellGenerator.getCellIndex(s.get(), max_x - i, i);
          cellGenerator.addBarycenter(cellIndex, buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
          //}
        });

        // current south tropical pentagon to south pole
        cellGenerator.getBarycenter(currentSouthTropicalPentagon).interpolate(cellGenerator.getBarycenter(southPole), divisions, buf);
        //for(int i=1; i < divisions; i++) {
        IntStream.range(1, divisions).parallel().forEach(i -> {
          int cellIndex = cellGenerator.getCellIndex(s.get(), max_x, i);
          cellGenerator.addBarycenter(cellIndex, buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
          //}
        });
      }
    }

    // Determine positions for cells between edges using interpolation.

    if ((divisions - 2) > 0) { // divisions must be at least 3 for there to be cells not along edges.
      for (int k = 0; k < Sphere.PEELS; k++) {
        final int s = k;

        for (int y = 0; y < (divisions * 2); y++) {
          final int x = y;
          // for each column, fill in values for cells between edge cells,
          // whose positions were defined in the previous block.
          if ((x + 1) % divisions > 0) { // ignore the columns that are edges.

            int j = divisions - ((x + 1) % divisions); // the y index of the cell in this column that is along a diagonal edge
            int n1 = j - 1; // the number of unpositioned cells before j
            int n2 = divisions - 1 - j; // the number of unpositioned cells after j
            int f1 = cellGenerator.getCellIndex(s, x, 0); // the cell along the early edge
            int f2 = cellGenerator.getCellIndex(s, x, j); // the cell along the diagonal edge
            // the cell along the later edge, which will necessarily belong to another section.
            int laterEdgeCellIndex = cellGenerator.getCellIndex(s, x, divisions - 1);
            //This should be temporary.  The cell indexes should be generated just to get the third index
            int thirdNeihborIndex = cellGenerator.getThirdNeighbor(laterEdgeCellIndex);
            int f3 = thirdNeihborIndex;

            cellGenerator.getBarycenter(f1).interpolate(cellGenerator.getBarycenter(f2), n1 + 1, buf);
            IntStream.range(1, j).parallel().forEach(i -> {
              int b1 = 2 * (i - 1) + 0;
              int b2 = 2 * (i - 1) + 1;
              int cellIndex = cellGenerator.getCellIndex(s, x, i);
              cellGenerator.addBarycenter(cellIndex, buf[b1], buf[b2]);
            });

            cellGenerator.getBarycenter(f2).interpolate(cellGenerator.getBarycenter(f3), n2 + 1, buf);
            IntStream.range(j + 1, divisions).parallel().forEach(i -> {
              int b1 = 2 * (i - j - 1) + 0;
              int b2 = 2 * (i - j - 1) + 1;
              int cellIndex = cellGenerator.getCellIndex(s, x, i);
              cellGenerator.addBarycenter(cellIndex,buf[b1], buf[b2]);
            });
          }
        }
      }
    }
  }


  public static Position calculateBaryCenter(MeshVertex[] polygon) {
    Position[] positions = new Position[polygon.length];
    for (int i=0; i < positions.length; i++) {
      positions[i] = polygon[i].getPosition();
    }
    return calculateBaryCenter(positions);
  }

  public static Position calculateBaryCenter(Position[] polygon) {
    int n=polygon.length;
    double sum_x = 0;
    double sum_z = 0;
    double sum_y = 0;
    //StringBuffer buffer = new StringBuffer();

    for (int i = 0; i < n; i += 1) {
      Position current = polygon[i];
      double i_φ = current.getφ();
      double i_λ = current.getλ();

      //buffer.append("[").append(i_φ).append(", ");
      //buffer.append(i_λ).append("]");

      sum_x += cos(i_φ) * cos(i_λ);
      sum_z += cos(i_φ) * sin(i_λ);
      sum_y += sin(i_φ);
    }

    double x = sum_x / n;
    double z = sum_z / n;
    double y = sum_y / n;

    double r = sqrt(x * x + z * z + y * y);

    double φ = asin(y / r);
    double λ = atan2(z, x);

    //System.out.println("Creating centroid at " + index + " " + φ + ", " + λ + " from " + buffer);
    return new Position(φ, λ);
  }
}

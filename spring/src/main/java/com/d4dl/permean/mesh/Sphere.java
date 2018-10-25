package com.d4dl.permean.mesh;

import com.d4dl.permean.ProgressReporter;
import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.DatabaseLoader;
import com.d4dl.permean.data.Vertex;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.lang.StrictMath.*;

/**
 * Represents a sphere of cells arranged in an interpolated icosahedral geodesic pattern.
 *
 * @param {object} options
 * @param {number} [options.divisions = 8] - Integer greater than 0:
 *                 the number of cells per edge of the icosahedron including
 *                 the origin of the edge (but not the endpoint). Determines the
 *                 angular resolution of the sphere. Directly & exponentially related
 *                 to the memory consumption of a Sphere.
 * @param {object} [options.data] - JSON representation of a sphere.
 *                 Should follow the same schema as output by Sphere.serialize. Constructor
 *                 will throw an error if it doesn't.
 * @constructor
 */
public class Sphere {

    public static int PEELS = 5;
    private final int divisions;
    private final DatabaseLoader databaseLoader;
    ProgressReporter reporter;

    private NumberFormat formatter = NumberFormat.getInstance();

    private Stack<Cell> cellStack = new Stack();
    private boolean stackIsDone = false;

    public final static String initiatorKey18Percent = "1F2F34D186D89AA0C8806F9EA9E51F8CB2274D5947118C67FBB0B0887EAF8734";
    public final static String initiatorKey82Percent = "9EAC9F9894BC86E1932019AF3B1F3C376C7BBC799F6555B8B623C7ED80E3DD66";

    private int cellCount = -1;
    private int vertexCount = -1;

    public static final double L = acos(sqrt(5) / 5); // the spherical arclength of the icosahedron's edges.
    private NumberFormat percentInstance = NumberFormat.getPercentInstance();

    double minLng = Integer.MAX_VALUE;
    double maxLng = Integer.MIN_VALUE;
    double minLat = Integer.MAX_VALUE;
    double maxLat = Integer.MIN_VALUE;
    double minArea = Integer.MAX_VALUE;
    double maxArea = Integer.MIN_VALUE;
    private static double AVG_EARTH_RADIUS_MI = 3959;
    int goodDivisionsValue = 6833;
    // division=2555 results in: 65,280,252 cells.  Each one will average 3.017164889884918 square miles.  130,560,500  Vertices
    AtomicReference<Float> vertexWriteRate = new AtomicReference();
    AtomicReference<Float> cellWriteRate = new AtomicReference();
    private String fileOut;


    //The laptop can do this easily.  It produces 25 million regions of 7.7 square miles each
    int anotherGoodDivisionsValue = 1600;
    private CellGenerator cellGenerator;

    public Sphere(String fileOut, int divisions, DatabaseLoader loader) {
        this.divisions = divisions;
        this.databaseLoader = loader;
        this.fileOut = fileOut;
    }

    public static void main(String[] args) throws Exception {
        new Sphere(args[0], Integer.parseInt(System.getProperty("sphere.divisions")), null).buildCells();
    }

    public void buildCells() throws IOException {

        percentInstance.setMaximumFractionDigits(2);

        //You want 100,000,000 cells so they will be around 2 square miles each.
        cellCount = PEELS * 2 * this.divisions * this.divisions + 2;
        vertexCount = divisions * divisions * 20;
        reporter = new ProgressReporter("Sphere", cellCount, vertexCount, cellStack);
        cellGenerator = new CellGenerator(cellCount, this.divisions, reporter);
        Object averageValue = new Integer[]{34, 93, 90, 45, 83, 94};
        double sphereRadius = Math.pow(AVG_EARTH_RADIUS_MI, 2) * 4 * PI;
        double cellArea = sphereRadius / cellCount;
        System.out.println("Initialized cells to: " + formatter.format(cellCount) + " cells.  Each one will average " + cellArea + " square miles.");
        reporter.start();


        try {
            populateBarycenters();//For all the ells, determine and set their barycenters
            System.out.println("Finished populating");

            String outputKMLSampleSizeProperty = System.getProperty("outputKMLSampleSize");
            String latLngBoundsProperty = System.getProperty("latLngBounds");
            //Just output some cells to see what the kml looks like
            if (latLngBoundsProperty != null) {
                String[] latLngBounds = latLngBoundsProperty.split(",");
                float lat1 = Float.parseFloat(latLngBounds[0]);
                float lng1 = Float.parseFloat(latLngBounds[1]);
                float lat2 = Float.parseFloat(latLngBounds[2]);
                float lng2 = Float.parseFloat(latLngBounds[3]);
                if(lat2 < lat1) {
                    float tmp = lat1;
                    lat1 = lat2;
                    lat2 = tmp;
                }
                if(lng2 < lng1) {
                    float tmp = lng1;
                    lng1 = lng2;
                    lng2 = tmp;
                }

                List<Cell> cellList = new ArrayList();
                for(int i=0; i < cellCount; i++) {
                  Position barycenter = cellGenerator.getBarycenter(i);
                  double barycenterLat = barycenter.getLat();
                  double barycenterLng = barycenter.getLng();
                  if (lat1 < barycenterLat && barycenterLat < lat2 &&
                      lng1 < barycenterLng && barycenterLng < lng2) {
                      cellList.add(cellGenerator.populateCell(i, "nobody"));
                  }
                }
                CellSerializer nerializer = new CellSerializer(fileOut, fileOut, reporter, true);
                new KMLWriter().outputKML(nerializer, cellList.toArray(new Cell[0]));
            } else if (outputKMLSampleSizeProperty != null) {
              int outKMLSampleSize = Integer.parseInt(outputKMLSampleSizeProperty);
              Cell[] cells = new Cell[outKMLSampleSize];
              IntStream parallel = IntStream.range(0, cells.length).parallel();
              parallel.forEach(f -> {
                  reporter.incrementBuiltCellCount();
                  cells[f] = cellGenerator.populateCell(f, "nobody");
              });
              CellSerializer nerializer = new CellSerializer(fileOut, fileOut, reporter, true);
              new KMLWriter().outputKML(nerializer, cells);
            } else {
                createCellStackWriter(reporter, cellCount, vertexCount);
                buildCellStack(cellCount);
                stackIsDone = true;
            }
        } finally {
            if (databaseLoader != null) {
                databaseLoader.stop();
            }
        }
        //System.out.println("Min was: " + minArea + " max was " + maxArea);
        System.out.println("Created and saved " + cellCount + " cells.\nNow go run constraints.sql");
        final boolean outputKML = Boolean.parseBoolean(System.getProperty("outputKML"));
        CellSerializer deSerializer = new CellSerializer(fileOut, fileOut, reporter, true);

        if (outputKML) {
            while (!stackIsDone || !cellStack.empty()) {
                try {
                    Thread.sleep(1000);//Wait for the stack to finish processing before trying to read the file
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            reporter.reset();
            new KMLWriter().outputKML(deSerializer, deSerializer.readCells());
        }
        reporter.stop();
    }


    private void createCellStackWriter(ProgressReporter progressReporter, final int cellCount, final int vertexCount) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            System.out.println("Thread running " + Thread.currentThread().getName());
            CellSerializer serializer = new CellSerializer(null, fileOut, reporter);
            serializer.setCountsAndStartWriting(cellCount, vertexCount);
            try {
                while (!stackIsDone || !cellStack.empty()) {
                    if (!cellStack.empty()) {
                        serializer.writeCell(cellStack.pop(), true);
                    } else {//If there's nothing in the queue don't keep looping more then once per second
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                progressReporter.stop();
            } finally {
                serializer.close();
            }
        });
        executor.shutdown();
    }


    public void buildCellStack(int cellCount) {
        System.out.println("Building cells.");
        IntStream parallel = IntStream.range(0, cellCount).parallel();
        parallel.forEach(f -> {
            double random = Math.random() * 100;
            reporter.incrementBuiltCellCount();
            String initiator = random > 82 ? initiatorKey18Percent : Sphere.initiatorKey82Percent;
            cellStack.push(cellGenerator.populateCell(f, initiator));
        });
        System.out.println("Finished building cells.");
    }

    public void saveCells(Cell[] cells, double eightyTwoPercent) {
        for (int f = 0; f < cells.length; f++) {
            Cell cell = cells[f];
            try {
                if (databaseLoader != null) {
                    databaseLoader.add(cell);
                }
            } catch (Exception e) {
                throw new RuntimeException("Can't add", e);
            }
        }
        //databaseLoader.completeVertices();
        System.out.println("Finished saving cells.  MaxLat = " + maxLat + " MinLat = " + minLat + " MaxLng = " + maxLng + " MinLng " + minLng);
    }



    /**
     * Sets the barycenter position of every cell on a Sphere.
     *
     * @this {Sphere}
     */
    public void populateBarycenters() {
        int max_x = 2 * divisions - 1;
        double[] buf = new double[((divisions - 1) * 2)];


        // Determine position for polar and tropical cells using only arithmetic.

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


    private IntStream saveVertices(Cell[] cells) {
        IntStream parallel = IntStream.range(0, cells.length).parallel();
        //AreaFinder areaFinder = new AreaFinder();
        //areaFinder.getArea(cells[0].getVertices(sharedVertexMap));
        parallel.forEach(f -> {
            Cell cell = cells[f];
            Vertex[] verticesToSave = cell.getVertices();
            for (int i = 0; i < verticesToSave.length; i++) {
                Vertex vertex = verticesToSave[i];
                if (vertex.getShouldPersist()) {
                    minLat = min(minLat, vertex.getLatitude());
                    maxLat = max(maxLat, vertex.getLatitude());
                    minLng = min(minLng, vertex.getLongitude());
                    maxLng = max(maxLng, vertex.getLongitude());
                    databaseLoader.add(vertex);
                    reporter.incrementVerticesWritten();
                }
            }
        });
        databaseLoader.completeVertices();
        return parallel;
    }

    //public String toString() {
    //String out = "";
    //for(int i=0; i < cells; i++) {
    //out += "Cell " + i + ":\n" + cells[i] + "\n";
    //}
    //return out;
    //}
}

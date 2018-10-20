package com.d4dl.permean.mesh;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.acos;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.sqrt;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.DatabaseLoader;
import com.d4dl.permean.data.Vertex;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

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

    private boolean iterating;
    private boolean reportingPaused = false;
    private NumberFormat formatter = NumberFormat.getInstance();

    private AtomicInteger populatedBaryCenterCount = new AtomicInteger(0);
    private AtomicInteger builtCellCount = new AtomicInteger(0);
    private AtomicInteger savedCellCount = new AtomicInteger(0);
    private AtomicInteger savedVertexCount = new AtomicInteger(0);
    private float cellWriteRate = 0;
    private float vertexWriteRate = 0;
    private Stack<Cell> cellStack = new Stack();
    private boolean stackIsDone = false;

    public final static String initiatorKey18Percent = "1F2F34D186D89AA0C8806F9EA9E51F8CB2274D5947118C67FBB0B0887EAF8734";
    public final static String initiatorKey82Percent = "9EAC9F9894BC86E1932019AF3B1F3C376C7BBC799F6555B8B623C7ED80E3DD66";

    private int cellCount = -1;
    private int vertexCount = -1;

    public static final double L = acos(sqrt(5) / 5); // the spherical arclength of the icosahedron's edges.
    private NumberFormat percentInstance = NumberFormat.getPercentInstance();

    Timer reportTimer = new Timer();
    double minLng = Integer.MAX_VALUE;
    double maxLng = Integer.MIN_VALUE;
    double minLat = Integer.MAX_VALUE;
    double maxLat = Integer.MIN_VALUE;
    double minArea = Integer.MAX_VALUE;
    double maxArea = Integer.MIN_VALUE;
    private static double AVG_EARTH_RADIUS_MI = 3959;
    int goodDivisionsValue = 6833;

    //The laptop can do this easily.  It produces 25 million regions of 7.7 square miles each
    int anotherGoodDivisionsValue = 1600;
    private CellGenerator cellGenerator;

    public Sphere(int divisions, DatabaseLoader loader) {
        this.divisions = divisions;
        this.databaseLoader = loader;
    }

    public static void main(String[] args) throws Exception {
        new Sphere(Integer.parseInt(System.getProperty("sphere.divisions")), null).buildCells();
    }

    public void buildCells() throws IOException {

        percentInstance.setMaximumFractionDigits(2);

        //You want 100,000,000 cells so they will be around 2 square miles each.
        cellCount = PEELS * 2 * this.divisions * this.divisions + 2;
        vertexCount = divisions * divisions * 20;
        cellGenerator = new CellGenerator(cellCount, this.divisions, populatedBaryCenterCount);
        Object averageValue = new Integer[]{34, 93, 90, 45, 83, 94};
        double sphereRadius = Math.pow(AVG_EARTH_RADIUS_MI, 2) * 4 * PI;
        double cellArea = sphereRadius / cellCount;
        System.out.println("Initialized cells to: " + formatter.format(cellCount) + " cells.  Each one will average " + cellArea + " square miles.");
        Timer rateTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                percentInstance = NumberFormat.getPercentInstance();
                report();
            }
        };
        //  task will be scheduled after 5 sec delay
        reportTimer.schedule(task, 1000, 1000);

        try {
            populateBarycenters();//For all the ells, determine and set their barycenters
            System.out.println("Finished populating");
            report();
            createRateWriteTracker(rateTimer);
            createCellStackWriter();
            buildCellStack(cellCount);
            stackIsDone = true;
        } finally {
            if (databaseLoader != null) {
                databaseLoader.stop();
            }
            reportTimer.cancel();
            rateTimer.cancel();
            task.cancel();
        }
        //System.out.println("Min was: " + minArea + " max was " + maxArea);
        System.out.println("Created and saved " + cellCount + " cells.\nNow go run constraints.sql");
        final boolean outputKML = Boolean.parseBoolean(System.getProperty("outputKML"));
        CellSerializer deSerializer = new CellSerializer(true);

        if (outputKML) {
            while (!stackIsDone || !cellStack.empty()) {
                try {
                    Thread.sleep(1000);//Wait for the stack to finish processing before trying to read the file
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            deSerializer.outputKML(deSerializer, deSerializer.readCells().entrySet().iterator().next().getValue());
        }
    }

    private void createRateWriteTracker(Timer rateTimer) {
        final float[] lastWriteCounts = new float[2];
        TimerTask writeRateTracker = new TimerTask() {
            @Override
            public void run() {
                int savedCells = savedCellCount.get();
                float cellsSavedSinceLast = savedCells - lastWriteCounts[0];
                cellWriteRate = cellsSavedSinceLast / 1000;
                lastWriteCounts[0] = savedCells;

                int savedVertexes = savedVertexCount.get();
                float vertexesSavedSinceLast = savedVertexes - lastWriteCounts[1];
                lastWriteCounts[1] = savedVertexes;
                vertexWriteRate = vertexesSavedSinceLast / 1000;
            }
        };

        //  count cells written every 10 seconds
        rateTimer.schedule(writeRateTracker, new Date(), 1000);
    }

    private void createCellStackWriter() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            System.out.println("Thread running " + Thread.currentThread().getName());
            CellSerializer serializer = new CellSerializer(cellCount, vertexCount, savedCellCount, savedVertexCount, builtCellCount);
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
            builtCellCount.incrementAndGet();
            cellStack.push(cellGenerator.populateCell(f));
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
                savedCellCount.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException("Can't add", e);
            }
        }
        //databaseLoader.completeVertices();
        report();
        System.out.println("Finished saving cells.  MaxLat = " + maxLat + " MinLat = " + minLat + " MaxLng = " + maxLng + " MinLng " + minLng);
    }

    private void report() {
        if(!reportingPaused) {
            report("Populated", cellCount, populatedBaryCenterCount.get(), "Barycenters");
            report("Built", cellCount, builtCellCount.get(), "Cells");
            report("Saved", vertexCount, savedVertexCount.get(), "Vertexes");
            report("Saved", cellCount, savedCellCount.get(), "Cells");
            System.out.print(" Writing " + vertexWriteRate + " vertexes per ms.");
            System.out.print(" Writing " + cellWriteRate + " cells per ms.");
            System.out.print(cellStack.size() + " cells in the cell stack.");
            System.out.print("\n");
        }
    }

    private void report(String verb, int total, int count, String type) {
        System.out.print(" " + verb + " " + formatter.format(count) +
                " of " + formatter.format(total) +
                " " + type + " (" + percentInstance.format((double) count / (double) total) +
                ")");
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


    @NotNull
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
                    savedVertexCount.incrementAndGet();
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

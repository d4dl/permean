package mesh;

import com.d4dl.mesh.LatLng;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.lang.Math.*;

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
    private final AtomicInteger pentagonCount = new AtomicInteger(0);

    AtomicInteger barycenterCount = new AtomicInteger(0);
    private Position[] intercellCentroids = null;
    private final Cell[] cells;

    private boolean iterating;
    private AtomicInteger createdCellCount = new AtomicInteger(0);
    private AtomicInteger linkedCellCount = new AtomicInteger(0);
    private AtomicInteger indexCount = new AtomicInteger(0);

    public static final double L = acos(sqrt(5) / 5); // the spherical arclength of the icosahedron's edges.
    private NumberFormat percentInstance = NumberFormat.getPercentInstance();
    private int[] intercellTriangles = null;
    private int[] intercellIndices = null;
    //One thread monitors this queue and puts Cells in the toPopulate queue once they can be
    private ConcurrentLinkedQueue<Cell> needCoordinatesQueue = new ConcurrentLinkedQueue();
    private ConcurrentLinkedQueue<Cell> needCleaningQueue = new ConcurrentLinkedQueue();
    private LatLng[] latLngCoords;
    private boolean processCoordinatesQueue = true;
    private boolean queueIsEmpty = false;
    TimerTask timer;
    AtomicInteger vertexIndex = new AtomicInteger(0);

    public static void main(String args[]) {
        new Sphere(1);
    }

    private int MAX_DIRTY = 500;

    private Thread coordinateSettingThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(processCoordinatesQueue || needCoordinatesQueue.size() > 0) {
                Iterator<Cell> coordinateIterator = needCoordinatesQueue.iterator();
                List<Cell> canTakeCoordinatesList = new LinkedList();
                System.out.println("Processing " + needCoordinatesQueue.size() + " cells.");
                while (coordinateIterator.hasNext()) {
                    Cell cell = coordinateIterator.next();
                    if (cell.canTakeCoordinates()) {
                        coordinateIterator.remove();
                        canTakeCoordinatesList.add(cell);
                    }
                }

                IntStream.range(0, canTakeCoordinatesList.size()).parallel().forEach(i -> {
                    Cell cell = canTakeCoordinatesList.get(i);
                    cell.setLatLngIndices(vertexIndex, latLngCoords);
                    needCleaningQueue.add(cell);
                });

                //}
                //if(needCleaningQueue.size() > MAX_DIRTY) {
                    //System.out.println("Too many dirty cells. Switching to clean only");
                    //cleanOnly = true;
                //}
                //if(cleanOnly == true && needCleaningQueue.size() < 1) {
                    //System.out.println("Finished clean only.  Switching back to clean and process.");
                    //cleanOnly = false;
                //}
                Iterator<Cell> cleaningIterator = needCleaningQueue.iterator();
                //System.out.println("Cleaning " + needCleaningQueue.size() + " cells.");
                while (cleaningIterator.hasNext()) {
                    Cell cell = cleaningIterator.next();
                    if (cell.canBeCleaned()) {
                        //cell.clean();
                        cleaningIterator.remove();
                    }
                }
                //cleanOnly = false;
                if(!processCoordinatesQueue && needCoordinatesQueue.size() == 0) {
                    queueIsEmpty = true;
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Finished processing coordinates");
        }
    }, "Coordinate setting thread");

    public Sphere(int divisions) {
        long start = System.currentTimeMillis();
        percentInstance.setMaximumFractionDigits(2);
        this.divisions = divisions;

        cells = new Cell[(PEELS * 2 * this.divisions * this.divisions + 2)];
        System.out.println("Initialized hex cells to: " + cells.length + " cells.");

        //List<HexCell> cellList = Arrays.asList(cells);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                percentInstance = NumberFormat.getPercentInstance();
                report();
            }
        };
        Timer timer = new Timer();
        //  task will be scheduled after 5 sec delay
        timer.schedule(task, 1000, 1000);

        Arrays.parallelSetAll(cells, i -> {
            createdCellCount.incrementAndGet();
            Cell cell = new Cell(this, i, null);
            if (cell.isPentagon()) {
                pentagonCount.incrementAndGet();
            }
            return cell;
        });

        System.out.println("Finished creating the cells. There are " + pentagonCount + " pentagons and " + (cells.length - pentagonCount.get()) + " hexagons.");
        IntStream.range(0, cells.length).parallel().forEach(i -> {
            linkedCellCount.incrementAndGet();
            cells[i].linkNeighboringCells();
        });
        System.out.println("Finished linking the hex cells");

        latLngCoords = new LatLng[(cells.length * 2) - 4];
        //coordinateSettingThread.start();
        populate();
        processCoordinatesQueue = false;
        //while(!queueIsEmpty) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        //}
        //IntStream.range(0, cells.length).parallel().forEach(i -> {
        for(int i=0; i < cells.length; i++) {
            cells[i].setLatLngIndices(vertexIndex, latLngCoords);
            indexCount.incrementAndGet();
        }
        System.out.println("Finished populating");
        long s = System.currentTimeMillis();
        //System.out.println("Created polygon in " + String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60)));
        report();
        timer.cancel();
        populateAreas();
        System.out.println("Finished.  Cell count: and Index count \t" + cells.length + "\t" + vertexIndex);

    }

    private void report() {
        int indexCount = intercellIndices != null ? intercellIndices.length : -1;
        System.out.println("Need Coordinates: " + needCoordinatesQueue.size() + ". Need Cleaning " + needCleaningQueue.size() + "\nCreated " + createdCellCount +
                " of " + cells.length +
                " cells (" + percentInstance.format((double)createdCellCount.get()/(double)cells.length) +
                "). Linked " + linkedCellCount +
                " of " + cells.length +
                " cells (" + percentInstance.format((double)linkedCellCount.get()/(double)cells.length) +
                ") Created " + this.indexCount +
                " of " + cells.length +
                " indexes (" + percentInstance.format((double) this.indexCount.get()/(double) cells.length) +
                ") Populate " + this.barycenterCount +
                " of " + cells.length +
                " barycenters (" + percentInstance.format((double) this.barycenterCount.get()/(double) cells.length) +
                ")\n");
    }

    public Cell getNorth() {
        return this.cells[0];
    }

    public Cell getSouth() {
        return this.cells[1];
    }

    public int getDivisions() {
        return divisions;
    }

    public boolean isIterating() {
        return iterating;
    }

    public void setIterating(boolean iterating) {
        this.iterating = iterating;
    }

    /**
     * Sets the barycenter position of every cell on a Sphere.
     *
     * @this {Sphere}
     */
    public void populate() {
        timer = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Sphere is populating: " + cells.length + " cells");
            }
        };
        int max_x = 2 * divisions - 1;
        double[] buf = new double[((divisions - 1) * 2)];
        timer.cancel();


        // Determine position for polar and tropical cells using only arithmetic.

        cells[0].setPosition(PI / 2, 0, needCoordinatesQueue);
        cells[0].setName("North");
        cells[1].setPosition(PI / -2, 0, needCoordinatesQueue);
        cells[1].setName("South");
        barycenterCount.getAndAdd(2);

        for (int s = 0; s < Sphere.PEELS; s += 1) {
            double λNorth = ((double)s) * 2 / 5 * PI;
            double λSouth = ((double)s) * 2 / 5 * PI + PI / 5;

            Cell peelUp = this.getCellBySXY(s, divisions - 1, 0);
            peelUp.setName("North Tropical " + s);
            peelUp.setPosition(PI / 2 - L, λNorth, needCoordinatesQueue);
            Cell peelDown = this.getCellBySXY(s, max_x, 0);
            peelDown.setName("South Tropical " + s);
            peelDown.setPosition(PI / -2 + L, λSouth, needCoordinatesQueue);
            barycenterCount.getAndAdd(2);
        }

        // Determine positions for the cells along the edges using arc interpolation.

        if ((divisions - 1) > 0) { // divisions must be at least 2 for there to be cells between vertices.

            for (final AtomicInteger s = new AtomicInteger(0); s.get() < Sphere.PEELS; s.incrementAndGet()) {
                int p = (s.get() + 4) % Sphere.PEELS;
                int northPole = 0;
                int southPole = 1;
                int currentNorthTropicalPentagon = this.getCellBySXY(s.get(), divisions - 1, 0).getIndex();
                int previousNorthTropicalPentagon = this.getCellBySXY(p, divisions - 1, 0).getIndex();
                int currentSouthTropicalPentagon = this.getCellBySXY(s.get(), max_x, 0).getIndex();
                int previousSoutTropicalPentagon = this.getCellBySXY(p, max_x, 0).getIndex();

                // north pole to current north tropical pentagon
                this.cells[northPole].getPosition().interpolate(this.cells[currentNorthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.getCellBySXY(s.get(), i - 1, 0).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], needCoordinatesQueue);
                    barycenterCount.getAndIncrement();
                });

                // current north tropical pentagon to previous north tropical pentagon
                this.cells[currentNorthTropicalPentagon].getPosition().interpolate(this.cells[previousNorthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.getCellBySXY(s.get(), divisions - 1 - i, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], needCoordinatesQueue);
                    barycenterCount.getAndIncrement();
                });

                // current north tropical pentagon to previous south tropical pentagon
                this.cells[currentNorthTropicalPentagon].getPosition().interpolate(this.cells[previousSoutTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.getCellBySXY(s.get(), divisions - 1, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], needCoordinatesQueue);
                    barycenterCount.getAndIncrement();
                });

                // current north tropical pentagon to current south tropical pentagon
                this.cells[currentNorthTropicalPentagon].getPosition().interpolate(this.cells[currentSouthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.getCellBySXY(s.get(), divisions - 1 + i, 0).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], needCoordinatesQueue);
                    barycenterCount.getAndIncrement();
                });

                // current south tropical pentagon to previous south tropical pentagon
                this.cells[currentSouthTropicalPentagon].getPosition().interpolate(this.cells[previousSoutTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.getCellBySXY(s.get(), max_x - i, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], needCoordinatesQueue);
                    barycenterCount.getAndIncrement();
                });

                // current south tropical pentagon to south pole
                this.cells[currentSouthTropicalPentagon].getPosition().interpolate(this.cells[southPole].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.getCellBySXY(s.get(), max_x, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], needCoordinatesQueue);
                    barycenterCount.getAndIncrement();
                });
            }
        }

        //try {
            //while(needCleaningQueue.size() > 12) {
                //Thread.sleep(100);
                //System.out.println("Waiting for " + needCleaningQueue.size() + " Cells.");
            //}
            //System.out.println("Finished waiting");
        //} catch (InterruptedException e) {
            //e.printStackTrace();
        //}

        // Determine positions for cells between edges using interpolation.


        if ((divisions - 2) > 0) { // divisions must be at least 3 for there to be cells not along edges.
            for (final AtomicInteger s = new AtomicInteger(0); s.get() < Sphere.PEELS; s.incrementAndGet()) {
                IntStream.range(0, divisions * 2).parallel().forEach(x -> {
                    // for each column, fill in values for cells between edge cells,
                    // whose positions were defined in the previous block.
                    if ((x + 1) % divisions > 0) { // ignore the columns that are edges.

                        int j = divisions - ((x + 1) % divisions); // the y index of the cell in this column that is along a diagonal edge
                        int n1 = j - 1; // the number of unpositioned cells before j
                        int n2 = divisions - 1 - j; // the number of unpositioned cells after j
                        int f1 = this.getCellBySXY(s.get(), x, 0).getIndex(); // the cell along the early edge
                        int f2 = this.getCellBySXY(s.get(), x, j).getIndex(); // the cell along the diagonal edge
                        int f3 = this.getCellBySXY(s.get(), x, divisions - 1).getAdjacent(2).getIndex(); // the cell along the later edge,
                        // which will necessarily belong to
                        // another section.

                        this.cells[f1].getPosition().interpolate(this.cells[f2].getPosition(), n1 + 1, buf);
                        for (int i = 1; i < j; i += 1) {
                            this.getCellBySXY(s.get(), x, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], needCoordinatesQueue);
                            barycenterCount.getAndIncrement();
                        }

                        this.cells[f2].getPosition().interpolate(this.cells[f3].getPosition(), n2 + 1, buf);
                        for (int i = j + 1; i < divisions; i += 1) {
                            this.getCellBySXY(s.get(), x, i).setPosition(buf[2 * (i - j - 1) + 0], buf[2 * (i - j - 1) + 1], needCoordinatesQueue);
                            barycenterCount.getAndIncrement();
                        }
                    }
                });
            }
        }
        System.out.println("Populated " + barycenterCount + " barycenters");
    }

    public int getPentagonCount() {
        return pentagonCount.get();
    }

    public Cell[] getCells() {
        return cells;
    }

    public Cell getCell(int i) {
        return cells[i];
    }

    public Cell getCellBySXY(int s, int x, int y) {
        return this.cells[s * this.divisions * this.divisions * 2 + x * this.divisions + y + 2];
    }

    public LatLng[] getLatLngCoords() {
        return latLngCoords;
    }

    public void setLatLngCoords(LatLng[] latLngCoords) {
        this.latLngCoords = latLngCoords;
    }

    public void populateAreas() {
        AreaFinder areaFinder = new AreaFinder();
        //Prime the thing
        areaFinder.getArea(cells[0].getVertices(latLngCoords));
        IntStream.range(0, cells.length).parallel().forEach(f -> {
            double area = areaFinder.getArea(cells[f].getVertices(latLngCoords));
            cells[f].setArea(area);
        });
        for(int i=0; i < cells.length; i++) {
            Cell cell = cells[i];
            double area = areaFinder.getArea(cell.getVertices(latLngCoords));
            //System.out.println("Cell " + i + " area: " + area + " first area: " + cells[i].getArea());
            for(int x=0; x < cell.getVertexIndices().length; x++) {
                int vertex = cell.getVertexIndices()[x];
                System.out.println("Cell\t" + i + "\t" + vertex);
            }
        }
    }

    public String toString() {
        String out = "";
        for(int i=0; i < cells.length; i++) {
            out += "Cell " + i + ":\n" + cells[i] + "\n";
        }
        return out;
    }
}

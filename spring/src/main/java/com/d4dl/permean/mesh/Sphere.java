package com.d4dl.permean.mesh;

import com.d4dl.permean.DatabaseLoader;
import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import net.openhft.chronicle.map.ChronicleMap;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.lang.StrictMath.*;

/**
 * Represents a sphere of proxies arranged in an interpolated icosahedral geodesic pattern.
 *
 * @param {object} options
 * @param {number} [options.divisions = 8] - Integer greater than 0:
 *                 the number of proxies per edge of the icosahedron including
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
    private final DatabaseLoader databaseLoader;
    private Map<Integer, CellProxy> proxies;
    private boolean iterating;

    private AtomicInteger createdCellCount = new AtomicInteger(0);
    private AtomicInteger savedCellCount = new AtomicInteger(0);
    private AtomicInteger savedVertexCount = new AtomicInteger(0);
    private AtomicInteger linkedCellCount = new AtomicInteger(0);


    private int indicesLength = -1;
    private int cellProxiesLength = -1;
    private int centroidsLength = -1;
    private int trianglesLength = -1;

    private AtomicInteger centroidCount = new AtomicInteger(0);
    private AtomicInteger indexCount = new AtomicInteger(0);
    private AtomicInteger trianglesCount = new AtomicInteger(0);

    public static final double L = acos(sqrt(5) / 5); // the spherical arclength of the icosahedron's edges.
    private NumberFormat percentInstance = NumberFormat.getPercentInstance();

    private Map<Integer, Integer> intercellTriangles = null;
    private Map<Integer, Position> intercellCentroids = null;
    private Map<Integer, Integer> intercellIndices = null;

    Timer timer = new Timer();
    double minArea = Integer.MAX_VALUE;
    double maxArea = Integer.MIN_VALUE;
    //private static double AVG_EARTH_RADIUS_MI = 3959;

    public Sphere(int divisions, DatabaseLoader loader) {
        this.divisions = divisions;
        this.databaseLoader = loader;
    }

    public void buildCells() {
        percentInstance.setMaximumFractionDigits(2);

        cellProxiesLength = PEELS * 2 * this.divisions * this.divisions + 2;
        //proxies = new CellProxy[cellProxiesLength];
        proxies = ChronicleMap.of(Integer.class, CellProxy.class)
                .name("cellProxies")
                .averageValue(new CellProxy(1, new int[]{1,1,1,1,1,1}))
                .entries(cellProxiesLength)
                .create();
        System.out.println("Initialized hex proxies to: " + cellProxiesLength + " proxies.");

        //List<HexCell> cellList = Arrays.asList(proxies);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                percentInstance = NumberFormat.getPercentInstance();
                report();
            }
        };
        //  task will be scheduled after 5 sec delay
        timer.schedule(task, 1000, 1000);

        IntStream.range(0, cellProxiesLength).parallel().forEach(i -> {

        //Arrays.parallelSetAll(proxies, i -> {
            createdCellCount.incrementAndGet();
            CellProxy hexCell = new CellProxy(this, i);
            if (hexCell.isPentagon()) {
                pentagonCount.incrementAndGet();
            }
            proxies.put(new Integer(i), hexCell);
            //return hexCell;
        //});

        });
        
        System.out.println("Finished creating the hex proxies");
        IntStream.range(0, cellProxiesLength).parallel().forEach(i -> {
            //for(int i=0; i < cellProxiesLength; i++) {
            linkedCellCount.incrementAndGet();
            CellProxy cellProxy = proxies.get(i);
            cellProxy.link(this);
            proxies.put(new Integer(i), cellProxy);
            //}
        });
        System.out.println("Finished linking the cell proxies. Populating.");

        //for (int i = 0; i < cellProxiesLength; i++) {
        //this.proxies[i].link();
        //}
        populate();
        System.out.println("Finished populating");
        getIntercellCentroids();
        System.out.println("Finished getting centroids");
        getIntercellIndices();
        System.out.println("Finished getting indexes");
        report();
        timer.cancel();
        if(databaseLoader != null) {
            saveCells();
        }
        //populateAreas();
        System.out.println("Min was: " + minArea + " max was " + maxArea);
        System.out.println("Created and saved " + cellProxiesLength + " proxies and " + savedVertexCount.get() + " vertices.");
    }

    private void report() {
        report("Created", cellProxiesLength, createdCellCount.get(), "CellProxies");
        report("Linked", cellProxiesLength, linkedCellCount.get(), "CellProxies");
        report("Created", trianglesLength, trianglesCount.get(), "Triangles");
        report("Created", centroidsLength, centroidCount.get(), "Centroid");
        report("Created", indicesLength, indexCount.get(), "Indices");
        report("Saved", cellProxiesLength, savedCellCount.get(), "Cells");
        report("Saved", centroidsLength, savedVertexCount.get(), "Vertices");
        System.out.print("\n");
    }

    private void report(String verb, int total, int count, String type) {
        System.out.print(" " + verb + " " + count +
                " of " + total +
                " " + type + " (" + percentInstance.format((double) count / (double) total) +
                ")");
    }

    public CellProxy getNorth() {
        return this.proxies.get(0);
    }

    public CellProxy getSouth() {
        return this.proxies.get(1);
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
        int max_x = 2 * divisions - 1;
        double[] buf = new double[((divisions - 1) * 2)];


        // Determine position for polar and tropical proxies using only arithmetic.

        proxies.get(0).setPosition(PI / 2, 0, proxies);
        proxies.get(1).setPosition(PI / -2, 0, proxies);

        for (int s = 0; s < Sphere.PEELS; s += 1) {
            double λNorth = ((double) s) * 2 / 5 * PI;
            double λSouth = ((double) s) * 2 / 5 * PI + PI / 5;

            this.get(s, divisions - 1, 0).setPosition(PI / 2 - L, λNorth, proxies);
            this.get(s, max_x, 0).setPosition(PI / -2 + L, λSouth, proxies);
        }

        // Determine positions for the proxies along the edges using arc interpolation.

        if ((divisions - 1) > 0) { // divisions must be at least 2 for there to be proxies between vertices.

            for (final AtomicInteger s = new AtomicInteger(0); s.get() < Sphere.PEELS; s.incrementAndGet()) {
                int p = (s.get() + 4) % Sphere.PEELS;
                int northPole = 0;
                int southPole = 1;
                int currentNorthTropicalPentagon = this.get(s.get(), divisions - 1, 0).getIndex();
                int previousNorthTropicalPentagon = this.get(p, divisions - 1, 0).getIndex();
                int currentSouthTropicalPentagon = this.get(s.get(), max_x, 0).getIndex();
                int previousSoutTropicalPentagon = this.get(p, max_x, 0).getIndex();

                // north pole to current north tropical pentagon
                this.proxies.get(northPole).getPosition().interpolate(this.proxies.get(currentNorthTropicalPentagon).getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    this.get(s.get(), i - 1, 0).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], proxies);
                });

                // current north tropical pentagon to previous north tropical pentagon
                this.proxies.get(currentNorthTropicalPentagon).getPosition().interpolate(this.proxies.get(previousNorthTropicalPentagon).getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    this.get(s.get(), divisions - 1 - i, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], proxies);
                });

                // current north tropical pentagon to previous south tropical pentagon
                this.proxies.get(currentNorthTropicalPentagon).getPosition().interpolate(this.proxies.get(previousSoutTropicalPentagon).getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    this.get(s.get(), divisions - 1, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], proxies);
                    //}
                });

                // current north tropical pentagon to current south tropical pentagon
                this.proxies.get(currentNorthTropicalPentagon).getPosition().interpolate(this.proxies.get(currentSouthTropicalPentagon).getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    this.get(s.get(), divisions - 1 + i, 0).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], proxies);
                    //}
                });

                // current south tropical pentagon to previous south tropical pentagon
                this.proxies.get(currentSouthTropicalPentagon).getPosition().interpolate(this.proxies.get(previousSoutTropicalPentagon).getPosition(), divisions, buf);
                //for(int i=1; i < divisions; i++) {
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), max_x - i, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], proxies);
                    //}
                });

                // current south tropical pentagon to south pole
                this.proxies.get(currentSouthTropicalPentagon).getPosition().interpolate(this.proxies.get(southPole).getPosition(), divisions, buf);
                //for(int i=1; i < divisions; i++) {
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), max_x, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1], proxies);
                    //}
                });
            }
        }

        // Determine positions for proxies between edges using interpolation.

        if ((divisions - 2) > 0) { // divisions must be at least 3 for there to be proxies not along edges.
            for (int k = 0; k < Sphere.PEELS; k++) {
                final int s = k;

                for (int y = 0; y < (divisions * 2); y++) {
                    final int x = y;
                    // for each column, fill in values for proxies between edge proxies,
                    // whose positions were defined in the previous block.
                    if ((x + 1) % divisions > 0) { // ignore the columns that are edges.

                        int j = divisions - ((x + 1) % divisions); // the y index of the cell in this column that is along a diagonal edge
                        int n1 = j - 1; // the number of unpositioned proxies before j
                        int n2 = divisions - 1 - j; // the number of unpositioned proxies after j
                        int f1 = this.get(s, x, 0).getIndex(); // the cell along the early edge
                        int f2 = this.get(s, x, j).getIndex(); // the cell along the diagonal edge
                        int f3 = this.get(s, x, divisions - 1).getAdjacent(2); // the cell along the later edge,
                        // which will necessarily belong to
                        // another section.

                        this.proxies.get(f1).getPosition().interpolate(this.proxies.get(f2).getPosition(), n1 + 1, buf);
                        IntStream.range(1, j).parallel().forEach(i -> {
                            int b1 = 2 * (i - 1) + 0;
                            int b2 = 2 * (i - 1) + 1;
                            this.get(s, x, i).setPosition(buf[b1], buf[b2], proxies);
                        });

                        this.proxies.get(f2).getPosition().interpolate(this.proxies.get(f3).getPosition(), n2 + 1, buf);
                        IntStream.range(j + 1, divisions).parallel().forEach(i -> {
                            int b1 = 2 * (i - j - 1) + 0;
                            int b2 = 2 * (i - j - 1) + 1;
                            this.get(s, x, i).setPosition(buf[b1], buf[b2], proxies);
                        });
                    }
                }
            }
        }
    }

    public int getPentagonCount() {
        return pentagonCount.get();
    }

    public Map<Integer, CellProxy> getCellProxies() {
        return proxies;
    }

    public CellProxy getCell(int i) {
        return proxies.get(i);
    }

    public CellProxy get(int s, int x, int y) {
        return this.proxies.get(s * this.divisions * this.divisions * 2 + x * this.divisions + y + 2);
    }


    public void populateAreas() {
        AreaFinder areaFinder = new AreaFinder();
        //Prime the thing
        areaFinder.getArea(proxies.get(0).getVertices(this));

        //IntStream.range(0, proxies).parallel().forEach(f -> {
        //double area = areaFinder.getArea(proxies[f].getLats(), proxies[f].getLngs());
        //proxies[f].setArea(area);
        ////System.out.println("Cell " + i + " area: " + area + " first area: " + proxies[i].getArea());
        //});
        for (int i = 0; i < cellProxiesLength; i++) {
            double areaAngle = areaFinder.getArea(proxies.get(i).getVertices(this));
            double areaSide = Math.sqrt(areaAngle);
            double areaSideRadians = areaSide * (180 / PI);
            //double sideLengthMI = areaSideRadians * AVG_EARTH_RADIUS_MI;
            //double areaMiles = sideLengthMI * sideLengthMI;
            //System.out.println("Cell " + i + " area: " + areaMiles + " sq. mi.");
            minArea = min(minArea, areaAngle);
            maxArea = max(maxArea, areaAngle);
            System.out.println("Cell " + i + " area: " + areaAngle);
            //System.out.println("\t<Placemark>\n" +
            //"\t\t<name>Cell " + i + "</name>\n" +
            //"\t\t<styleUrl>#m_ylw-pushpin0</styleUrl>\n" +
            //"\t\t<Polygon>\n" +
            //"\t\t\t<tessellate>1</tessellate>\n" +
            //"\t\t\t<outerBoundaryIs>\n" +
            //"\t\t\t\t<LinearRing>\n" +
            //"\t\t\t\t\t<coordinates>\n" +
            //"\t\t\t\t\t\t" + proxies[i]+ "\n" +
            //"\t\t\t\t\t</coordinates>\n" +
            //"\t\t\t\t</LinearRing>\n" +
            //"\t\t\t</outerBoundaryIs>\n" +
            //"\t\t</Polygon>\n" +
            //"\t</Placemark>");
            if (i % 400 == 0) {
                System.out.println(proxies.get(i));
            }
        }
    }

    public Map<Integer, Integer> getIntercellTriangles() {
        if (intercellTriangles == null) {
            trianglesLength = ((2 * cellProxiesLength - 4) * 3);
            intercellTriangles = ChronicleMap
                    .of(Integer.class, Integer.class)
                    .name("intercellTriangles")
                    .entries(trianglesLength)
                    .create();

            System.out.println("Initialized intercell triangles to: " + intercellTriangles.size() + " triangles.");
            IntStream.range(0, cellProxiesLength).parallel().forEach(f -> {
                //for(int f=0; f < length; f++) {
                proxies.get(f).getIntercellTriangles(intercellTriangles, this);
                trianglesCount.addAndGet(6);
                //}
            });

            System.out.println("Finished creating triangles");
        }

        return intercellTriangles;
    }


    public Map<Integer, Position> getIntercellCentroids() {
        Map<Integer, Integer> triangles = getIntercellTriangles();
        if (intercellCentroids == null) {
            centroidsLength = triangles.size() / 3;
            intercellCentroids = ChronicleMap
                    .of(Integer.class, Position.class)
                    .name("intercellCentroids")
                    .entries(centroidsLength)
                    .averageValue(new Position(1d, 1d, new Vertex(UUID.randomUUID().toString(), 1, 1, 1)))
                    .create();
            System.out.println("Initialized intercell centroids to: " + intercellCentroids.size() + " centroids.");

            IntStream.range(0, centroidsLength).parallel().forEach(centroidIndex -> {
                //for(int centroidIndex=0; centroidIndex < length; centroidIndex++) {
                int triangleIndex = 3 * centroidIndex;
                int cellIndex = triangles.get(triangleIndex);
                Position centroid = proxies.get(cellIndex).getIntercellCentroids(this, triangles, centroidIndex);
                if (databaseLoader != null) {
                    Vertex vertex = new Vertex(UUID.randomUUID().toString(), centroidIndex, centroid.getLat(), centroid.getLng());
                    databaseLoader.add(vertex);
                    savedVertexCount.incrementAndGet();
                    centroid.setVertex(vertex);
                }
                intercellCentroids.put(centroidIndex, centroid);
                centroidCount.getAndIncrement();
                //}
            });
            System.out.println("Finished creating centroids");
        }

        return intercellCentroids;
    }


    public void saveCells() {
        int n = this.cellProxiesLength;
        IntStream parallel = IntStream.range(0, n).parallel();
        parallel.forEach(f -> {
            CellProxy proxy = this.proxies.get(f);
            proxy.getIntercellIndices(intercellIndices, intercellTriangles);
            Position[] positions = proxies.get(f).getVertices(this);
            List<Vertex> vertices = new ArrayList();
            for (int i = 0; i < positions.length; i++) {
                vertices.add(positions[i].getVertex());
            }
            Cell cell = new Cell(UUID.randomUUID().toString(), vertices, divisions, proxy.getArea());
            databaseLoader.add(cell);
            savedCellCount.incrementAndGet();
        });
        databaseLoader.stop();
        System.out.println("Finished saving proxies");
    }

    public Map<Integer, Integer> getIntercellIndices() {
        if (intercellIndices == null) {
            indicesLength = 6 * cellProxiesLength;
            intercellIndices = ChronicleMap.of(Integer.class, Integer.class)
                    .name("intercellIndices")
                    .entries(indicesLength)
                    .create();
            System.out.println("Initialized intercell indices to: " + intercellTriangles.size() + " indices.");

            IntStream.range(0, cellProxiesLength).parallel().forEach(f -> {
                //for(int f=0; f < n; f++) {
                CellProxy cell = this.proxies.get(f);
                cell.getIntercellIndices(intercellIndices, intercellTriangles);
                indexCount.getAndAdd(cell.getAdjacentCells(this).length);
                //}
            });
            System.out.println("Finished creating indices");
        }
        return intercellIndices;
    }


    //public String toString() {
    //String out = "";
    //for(int i=0; i < proxies; i++) {
    //out += "Cell " + i + ":\n" + proxies[i] + "\n";
    //}
    //return out;
    //}
}

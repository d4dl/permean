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
    private CellProxy[] proxies;
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
        proxies = new CellProxy[cellProxiesLength];
        System.out.println("Initialized hex proxies to: " + proxies.length + " proxies.");

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

        Arrays.parallelSetAll(proxies, i -> {
            createdCellCount.incrementAndGet();
            CellProxy hexCell = new CellProxy(this, i);
            if (hexCell.isPentagon()) {
                pentagonCount.incrementAndGet();
            }
            return hexCell;
        });

        System.out.println("Finished creating the hex proxies");
        IntStream.range(0, proxies.length).parallel().forEach(i -> {
            //for(int i=0; i < proxies.length; i++) {
            linkedCellCount.incrementAndGet();
            proxies[i].link();
            //}
        });
        System.out.println("Finished linking the cell proxies. Populating.");

        //for (int i = 0; i < proxies.length; i++) {
        //this.proxies[i].link();
        //}
        populate();
        System.out.println("Finished populating");
        getIntercellCentroids();
        System.out.println("Finished getting centroids");
        getIntercellIndices();
        System.out.println("Finished getting indexes");
        report();
        populateAreas();
        timer.cancel();
        if(databaseLoader != null) {
            saveCells();
        }
        System.out.println("Min was: " + minArea + " max was " + maxArea);
        System.out.println("Created and saved " + proxies.length + " proxies and " + savedVertexCount.get() + " vertices.");
    }

    private void report() {
        report("Created", proxies.length, createdCellCount.get(), "CellProxies");
        report("Linked", proxies.length, linkedCellCount.get(), "CellProxies");
        report("Created", trianglesLength, trianglesCount.get(), "Triangles");
        report("Created", centroidsLength, centroidCount.get(), "Centroid");
        report("Created", indicesLength, indexCount.get(), "Indices");
        report("Saved", proxies.length, savedCellCount.get(), "Cells");
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
        return this.proxies[0];
    }

    public CellProxy getSouth() {
        return this.proxies[1];
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

        proxies[0].setPosition(PI / 2, 0);
        proxies[1].setPosition(PI / -2, 0);

        for (int s = 0; s < Sphere.PEELS; s += 1) {
            double λNorth = ((double) s) * 2 / 5 * PI;
            double λSouth = ((double) s) * 2 / 5 * PI + PI / 5;

            this.get(s, divisions - 1, 0).setPosition(PI / 2 - L, λNorth);
            this.get(s, max_x, 0).setPosition(PI / -2 + L, λSouth);
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
                this.proxies[northPole].getPosition().interpolate(this.proxies[currentNorthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    this.get(s.get(), i - 1, 0).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current north tropical pentagon to previous north tropical pentagon
                this.proxies[currentNorthTropicalPentagon].getPosition().interpolate(this.proxies[previousNorthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    this.get(s.get(), divisions - 1 - i, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current north tropical pentagon to previous south tropical pentagon
                this.proxies[currentNorthTropicalPentagon].getPosition().interpolate(this.proxies[previousSoutTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    this.get(s.get(), divisions - 1, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                    //}
                });

                // current north tropical pentagon to current south tropical pentagon
                this.proxies[currentNorthTropicalPentagon].getPosition().interpolate(this.proxies[currentSouthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    this.get(s.get(), divisions - 1 + i, 0).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                    //}
                });

                // current south tropical pentagon to previous south tropical pentagon
                this.proxies[currentSouthTropicalPentagon].getPosition().interpolate(this.proxies[previousSoutTropicalPentagon].getPosition(), divisions, buf);
                //for(int i=1; i < divisions; i++) {
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), max_x - i, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                    //}
                });

                // current south tropical pentagon to south pole
                this.proxies[currentSouthTropicalPentagon].getPosition().interpolate(this.proxies[southPole].getPosition(), divisions, buf);
                //for(int i=1; i < divisions; i++) {
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), max_x, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
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
                        int f3 = this.get(s, x, divisions - 1).getAdjacent(2).getIndex(); // the cell along the later edge,
                        // which will necessarily belong to
                        // another section.

                        this.proxies[f1].getPosition().interpolate(this.proxies[f2].getPosition(), n1 + 1, buf);
                        IntStream.range(1, j).parallel().forEach(i -> {
                            int b1 = 2 * (i - 1) + 0;
                            int b2 = 2 * (i - 1) + 1;
                            this.get(s, x, i).setPosition(buf[b1], buf[b2]);
                        });

                        this.proxies[f2].getPosition().interpolate(this.proxies[f3].getPosition(), n2 + 1, buf);
                        IntStream.range(j + 1, divisions).parallel().forEach(i -> {
                            int b1 = 2 * (i - j - 1) + 0;
                            int b2 = 2 * (i - j - 1) + 1;
                            this.get(s, x, i).setPosition(buf[b1], buf[b2]);
                        });
                    }
                }
            }
        }
    }

    public int getPentagonCount() {
        return pentagonCount.get();
    }

    public CellProxy[] getCellProxies() {
        return proxies;
    }

    public CellProxy getCell(int i) {
        return proxies[i];
    }

    public CellProxy get(int s, int x, int y) {
        return this.proxies[s * this.divisions * this.divisions * 2 + x * this.divisions + y + 2];
    }


    public void populateAreas() {
        AreaFinder areaFinder = new AreaFinder();
        //Prime the thing
        areaFinder.getArea(proxies[0].getVertices());

        IntStream.range(0, cellProxiesLength).parallel().forEach(i -> {
            //double area = areaFinder.getArea(proxies[f].getLats(), proxies[f].getLngs());
            //proxies[f].setArea(area);
            ////System.out.println("Cell " + i + " area: " + area + " first area: " + proxies[i].getArea());
            //});
            //for (int i = 0; i < proxies.length; i++) {
            double areaAngle = areaFinder.getArea(proxies[i].getVertices());
            getCell(i).setArea(areaAngle);
            double areaSide = Math.sqrt(areaAngle);
            double areaSideRadians = areaSide * (180 / PI);
            //double sideLengthMI = areaSideRadians * AVG_EARTH_RADIUS_MI;
            //double areaMiles = sideLengthMI * sideLengthMI;
            //System.out.println("Cell " + i + " area: " + areaMiles + " sq. mi.");
            minArea = min(minArea, areaAngle);
            maxArea = max(maxArea, areaAngle);
            //System.out.println("Cell " + i + " area: " + areaAngle);
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
            //}
        });
    }

    public Map<Integer, Integer> getIntercellTriangles() {
        if (intercellTriangles == null) {
            trianglesLength = ((2 * proxies.length - 4) * 3);
            intercellTriangles = ChronicleMap
                    .of(Integer.class, Integer.class)
                    .name("intercellTriangles")
                    .entries(trianglesLength)
                    .create();

            System.out.println("Initialized intercell triangles to: " + intercellTriangles.size() + " triangles.");
            IntStream.range(0, proxies.length).parallel().forEach(f -> {
                //for(int f=0; f < length; f++) {
                proxies[f].getIntercellTriangles(intercellTriangles);
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
                Position centroid = proxies[cellIndex].getIntercellCentroids(triangles, centroidIndex);
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
        int n = this.proxies.length;
        IntStream parallel = IntStream.range(0, n).parallel();
        parallel.forEach(f -> {
            CellProxy proxy = this.proxies[f];
            proxy.getIntercellIndices(intercellIndices, intercellTriangles);
            Position[] positions = proxies[f].getVertices();
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
            indicesLength = 6 * proxies.length;
            intercellIndices = ChronicleMap.of(Integer.class, Integer.class)
                    .name("intercellIndices")
                    .entries(indicesLength)
                    .create();
            System.out.println("Initialized intercell indices to: " + intercellTriangles.size() + " indices.");

            IntStream.range(0, proxies.length).parallel().forEach(f -> {
                //for(int f=0; f < n; f++) {
                CellProxy cell = this.proxies[f];
                cell.getIntercellIndices(intercellIndices, intercellTriangles);
                indexCount.getAndAdd(cell.getAdjacentCells().length);
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

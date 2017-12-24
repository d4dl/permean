package com.d4dl.permean.mesh;

import com.d4dl.permean.DatabaseLoader;
import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import net.openhft.chronicle.map.ChronicleMap;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
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
    private boolean reportingPaused = false;

    private AtomicInteger createdCellCount = new AtomicInteger(0);
    private AtomicInteger savedCellCount = new AtomicInteger(0);
    private AtomicInteger savedVertexCount = new AtomicInteger(0);
    private AtomicInteger linkedCellCount = new AtomicInteger(0);


    private int cellProxiesLength = -1;
    private int sharedVertexMapLength = -1;

    private AtomicInteger sharedVertexMapCount = new AtomicInteger(0);

    public static final double L = acos(sqrt(5) / 5); // the spherical arclength of the icosahedron's edges.
    private NumberFormat percentInstance = NumberFormat.getPercentInstance();

    private Map<int[], Vertex> sharedVertexMap = null;

    Timer timer = new Timer();
    double minLng = Integer.MAX_VALUE;
    double maxLng = Integer.MIN_VALUE;
    double minLat = Integer.MAX_VALUE;
    double maxLat = Integer.MIN_VALUE;
    double minArea = Integer.MAX_VALUE;
    double maxArea = Integer.MIN_VALUE;
    private static double AVG_EARTH_RADIUS_MI = 3959;
    int goodDivisionsValue = 6833;

    public Sphere(int divisions, DatabaseLoader loader) {
        this.divisions = divisions;
        this.databaseLoader = loader;
    }

    public void buildCells() {
        percentInstance.setMaximumFractionDigits(2);

        //You want 100,000,000 cells so they will be around 2 square miles each.
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

        System.out.println("Finished creating the cell proxies");
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
        populateBarycenters();//For all the proxies, determine and set their barycenters
        System.out.println("Finished populating");
        createSharedVertexMap();
        //System.out.println("Finished getting indexes");
        report();
        //outputKML();
        if(databaseLoader != null) {
            saveCells();
        }
        timer.cancel();
        task.cancel();
        System.out.println("Min was: " + minArea + " max was " + maxArea);
        System.out.println("Created and saved " + proxies.length + " proxies and " + savedVertexCount.get() + " vertices.");
    }

    private void report() {
        if(!reportingPaused) {
            report("Created", proxies.length, createdCellCount.get(), "CellProxies");
            report("Linked", proxies.length, linkedCellCount.get(), "CellProxies");
            report("Created", sharedVertexMapLength, sharedVertexMapCount.get(), "Shared Vertices");
            report("Saved", proxies.length, savedCellCount.get(), "Cells");
            System.out.print("\n");
        }
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
    public void populateBarycenters() {
        int max_x = 2 * divisions - 1;
        double[] buf = new double[((divisions - 1) * 2)];


        // Determine position for polar and tropical proxies using only arithmetic.

        proxies[0].setBarycenter(PI / 2, 0);
        proxies[0].setName("Pentagon 0 (North)");
        proxies[1].setBarycenter(PI / -2, 0);
        proxies[1].setName("Pentagon 1 (South)");

        //Set the other 10 pentagon's barycenters
        for (int s = 0; s < Sphere.PEELS; s += 1) {
            double λNorth = ((double) s) * 2 / 5 * PI;
            double λSouth = ((double) s) * 2 / 5 * PI + PI / 5;

            CellProxy northernPentagon = this.get(s, divisions - 1, 0);
            northernPentagon.setBarycenter(PI / 2 - L, λNorth);
            northernPentagon.setName("Pentagon " + ((s * 2) + 2) + " (northern)");
            CellProxy southernPentagon = this.get(s, max_x, 0);
            southernPentagon.setBarycenter(PI / -2 + L, λSouth);
            southernPentagon.setName("Pentagon " + ((s * 2) + 3) + " (southern)");
        }

        // Determine positions for the proxies along the edges using arc interpolation.

        if ((divisions - 1) > 0) { // divisions must be at least 2 for there to be proxies between vertices.

            //PEELS is 5 so loop 5 times.
            for (final AtomicInteger s = new AtomicInteger(0); s.get() < Sphere.PEELS; s.incrementAndGet()) {
                int p = (s.get() + 4) % Sphere.PEELS;
                int northPole = 0;
                int southPole = 1;
                int currentNorthTropicalPentagon = this.get(s.get(), divisions - 1, 0).getIndex();
                int previousNorthTropicalPentagon = this.get(p, divisions - 1, 0).getIndex();
                int currentSouthTropicalPentagon = this.get(s.get(), max_x, 0).getIndex();
                int previousSoutTropicalPentagon = this.get(p, max_x, 0).getIndex();

                // north pole to current north tropical pentagon
                this.proxies[northPole].getBarycenter().interpolate(this.proxies[currentNorthTropicalPentagon].getBarycenter(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    CellProxy cellProxy = this.get(s.get(), i - 1, 0);
                    cellProxy.setName("North to North Tropical " + s.get());
                    cellProxy.setBarycenter(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current north tropical pentagon to previous north tropical pentagon
                this.proxies[currentNorthTropicalPentagon].getBarycenter().interpolate(this.proxies[previousNorthTropicalPentagon].getBarycenter(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    CellProxy cellProxy = this.get(s.get(), divisions - 1 - i, i);
                    cellProxy.setName("North Tropical to North Tropical " + s.get());
                    cellProxy.setBarycenter(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current north tropical pentagon to previous south tropical pentagon
                this.proxies[currentNorthTropicalPentagon].getBarycenter().interpolate(this.proxies[previousSoutTropicalPentagon].getBarycenter(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    CellProxy cellProxy = this.get(s.get(), divisions - 1, i);
                    cellProxy.setName("North Tropical to South Tropical " + s.get());
                    cellProxy.setBarycenter(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                    //}
                });

                // current north tropical pentagon to current south tropical pentagon
                this.proxies[currentNorthTropicalPentagon].getBarycenter().interpolate(this.proxies[currentSouthTropicalPentagon].getBarycenter(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    //for(int i=1; i < divisions; i++) {
                    CellProxy cellProxy = this.get(s.get(), divisions - 1 + i, 0);
                    cellProxy.setName("North Tropical to South Tropical " + s.get());
                    cellProxy.setBarycenter(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                    //}
                });

                // current south tropical pentagon to previous south tropical pentagon
                this.proxies[currentSouthTropicalPentagon].getBarycenter().interpolate(this.proxies[previousSoutTropicalPentagon].getBarycenter(), divisions, buf);
                //for(int i=1; i < divisions; i++) {
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    CellProxy cellProxy = this.get(s.get(), max_x - i, i);
                    cellProxy.setName("South Tropical to South Tropical " + s.get());
                    cellProxy.setBarycenter(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                    //}
                });

                // current south tropical pentagon to south pole
                this.proxies[currentSouthTropicalPentagon].getBarycenter().interpolate(this.proxies[southPole].getBarycenter(), divisions, buf);
                //for(int i=1; i < divisions; i++) {
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    CellProxy cellProxy = this.get(s.get(), max_x, i);
                    cellProxy.setName("South Tropical to South " + s.get());
                    cellProxy.setBarycenter(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
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

                        this.proxies[f1].getBarycenter().interpolate(this.proxies[f2].getBarycenter(), n1 + 1, buf);
                        IntStream.range(1, j).parallel().forEach(i -> {
                            int b1 = 2 * (i - 1) + 0;
                            int b2 = 2 * (i - 1) + 1;
                            this.get(s, x, i).setBarycenter(buf[b1], buf[b2]);
                        });

                        this.proxies[f2].getBarycenter().interpolate(this.proxies[f3].getBarycenter(), n2 + 1, buf);
                        IntStream.range(j + 1, divisions).parallel().forEach(i -> {
                            int b1 = 2 * (i - j - 1) + 0;
                            int b2 = 2 * (i - j - 1) + 1;
                            this.get(s, x, i).setBarycenter(buf[b1], buf[b2]);
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



    private void outputKML() {
        StringBuffer buffer = new StringBuffer();
        String[] styles = new String[]{"transBluePoly", "transRedPoly", "transGreenPoly", "transYellowPoly"};
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "  <Document>\n" +
                "    <Style id=\"transRedPoly\">\n" +
                "      <LineStyle>\n" +
                "        <color>ff0000ff</color>\n" +
                "      </LineStyle>\n" +
                "      <PolyStyle>\n" +
                "        <color>55ff0000</color>\n" +
                "      </PolyStyle>\n" +
                "    </Style>\n" +
                "    <Style id=\"transYellowPoly\">\n" +
                "      <LineStyle>\n" +
                "        <color>7f00ffff</color>\n" +
                "      </LineStyle>\n" +
                "      <PolyStyle>\n" +
                "        <color>5500ff00</color>\n" +
                "      </PolyStyle>\n" +
                "    </Style>\n" +
                "    <Style id=\"transBluePoly\">\n" +
                "      <LineStyle>\n" +
                "        <color>7dffbb00</color>\n" +
                "      </LineStyle>\n" +
                "      <PolyStyle>\n" +
                "        <color>550000ff</color>\n" +
                "      </PolyStyle>\n" +
                "    </Style>\n" +
                "    <Style id=\"transGreenPoly\">\n" +
                "      <LineStyle>\n" +
                "        <color>7f00ff00</color>\n" +
                "      </LineStyle>\n" +
                "      <PolyStyle>\n" +
                "        <color>5500ffff</color>\n" +
                "      </PolyStyle>\n" +
                "    </Style>\n"
        );
        int limit = proxies.length;//20
        for(int i=0; i < limit; i++) {
            buffer.append("    <Placemark>\n" +
                    "      <name>" + proxies[i].getName() + " " + proxies[i].getArea() + "</name>\n" +
                    //"      <styleUrl>#" + styles[i % styles.length] + "</styleUrl>\n" +
                    "      <styleUrl>#" + styles[0] + "</styleUrl>\n" +
                    getLineString(i) +
                    "    </Placemark>\n");
        }
        buffer.append("  </Document>\n" +
                "</kml>");
        String fileName = "/Users/joshuadeford/rings.kml";
        File file = new File(fileName);
        try {
            System.out.println("Writing to file: " + fileName);
            FileUtils.writeStringToFile(file, buffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLineString(int i) {
                return "      <LineString>\n" +
                "        <tesselate>1</tesselate>\n" +
                "        <altitudeMode>relativeToGround</altitudeMode>\n" +
                "        <coordinates>\n" +
                proxies[i].kmlString(2000, sharedVertexMap) + "\n" +
                "        </coordinates>\n" +
                "      </LineString>\n";
    }
    private String getPolygon(int i) {
        return "      <Polygon>\n" +
                "      <outerBoundaryIs>\n" +
                "      <LinearRing>\n" +
                "        <tesselate>1</tesselate>\n" +
                "        <altitudeMode>relativeToGround</altitudeMode>\n" +
                "        <coordinates>\n" +
                proxies[i].kmlString(2000, sharedVertexMap) + "\n" +
                "        </coordinates>\n" +
                "      </LinearRing>\n" +
                "      </outerBoundaryIs>\n" +
                "      </Polygon>\n";
    }

    public Map<int[], Vertex> createSharedVertexMap() {
        sharedVertexMapLength = 12 * (divisions) + 3;
        int[] ints = new int[6];
        System.out.println("Initializing shared vertex map ");
        reportingPaused = true;
        try {
            sharedVertexMap = (Map<int[], Vertex>) ChronicleMap
                    .of(ints.getClass(), Vertex.class)
                    .averageKeySize(6 * 32)
                    .averageValue(new Vertex("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", 0, 0))
                    .name("sharedVertexMap")
                    .entries(sharedVertexMapLength)
                    .createPersistedTo(new File("./sharedVertexMap_divisions_" + divisions + ".dat"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Initialized shared vertex map " + sharedVertexMapLength + " vertices.");
        reportingPaused = false;
        IntStream.range(0, proxies.length).parallel().forEach(f -> {
            //for(int f=0; f < proxies.length; f++) {
            List<Vertex> verticesAdded = proxies[f].populateSharedVertices(sharedVertexMap);
            for (Vertex vertex : verticesAdded) {
                databaseLoader.add(vertex);
            }
            sharedVertexMapCount.addAndGet(verticesAdded.size());
            //    }

        });
        databaseLoader.completeVertices();

        System.out.println("Finished creating shared vertex map");
        return sharedVertexMap;
    }


    public void saveCells() {
        int n = this.proxies.length;
        IntStream parallel = IntStream.range(0, n).parallel();
        //AreaFinder areaFinder = new AreaFinder();
        //areaFinder.getArea(proxies[0].getVertices(sharedVertexMap));
        //parallel.forEach(f -> {
        for(int f=0; f < this.proxies.length; f++) {
            CellProxy proxy = this.proxies[f];
            Vertex[] vertices = proxy.getVertices(sharedVertexMap);
            //double areaAngle = areaFinder.getArea(vertices);
            //Cell cell = new Cell(UUID.randomUUID().toString(), Arrays.asList(), divisions, areaAngle);
            Cell cell = new Cell(UUID.randomUUID().toString(), Arrays.asList(vertices), divisions, 0);
            //double areaSide = Math.sqrt(areaAngle);
            //double areaSideRadians = areaSide * (180 / PI);
            //double sideLengthMI = areaSideRadians * AVG_EARTH_RADIUS_MI;
            //double areaMiles = sideLengthMI * sideLengthMI;
            //System.out.println("Cell " + proxy.getName() + " area: " + areaMiles + " sq. mi.");
            //proxy.setArea(areaMiles);
            //minArea = min(minArea, areaAngle);
            //maxArea = max(maxArea, areaAngle);
            for (int i = 0; i < vertices.length; i++) {
                minLat = min(minLat, vertices[i].getLatitude().doubleValue());
                maxLat = max(maxLat, vertices[i].getLatitude().doubleValue());
                minLng = min(minLng, vertices[i].getLongitude().doubleValue());
                maxLng = max(maxLng, vertices[i].getLongitude().doubleValue());
            }
            try {
                databaseLoader.add(cell);
            } catch (Exception e) {
                throw new RuntimeException("Can't add", e);
            }
            savedCellCount.incrementAndGet();
        }
        //});
        databaseLoader.stop();
        System.out.println("Finished saving cells.  MaxLat = " + maxLat + " MinLat = " + minLat + " MaxLng = " + maxLng + " MinLng " + minLng);
    }



    //public String toString() {
    //String out = "";
    //for(int i=0; i < proxies; i++) {
    //out += "Cell " + i + ":\n" + proxies[i] + "\n";
    //}
    //return out;
    //}
}

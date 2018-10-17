package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.DatabaseLoader;
import com.d4dl.permean.data.Vertex;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.BufferedOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

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
    public static final File CELLS_DIR = new File("/tmp/cells");
    // public static final String shmDir = "/Volumes/RAMDisk";
    // public static final File CELL_PROXY_INDICES = new File(shmDir, "cellProxyIndices");
    public static int PEELS = 5;
    private final int divisions;
    private final AtomicInteger pentagonCount = new AtomicInteger(0);
    private final DatabaseLoader databaseLoader;
    private CellProxy[] proxies;
    private boolean iterating;
    private boolean reportingPaused = false;
    private NumberFormat formatter = NumberFormat.getInstance();
    private Map<Integer, Object> cellProxyIndexMap;

    private AtomicInteger createdProxyCount = new AtomicInteger(0);
    private AtomicInteger populatedBaryCenterCount = new AtomicInteger(0);
    private AtomicInteger savedCellCount = new AtomicInteger(0);
    private AtomicInteger savedVertexCount = new AtomicInteger(0);
    private AtomicInteger linkedCellCount = new AtomicInteger(0);
    private float cellWriteRate = 0;

    private final static String initiatorKey18Percent = "1F2F34D186D89AA0C8806F9EA9E51F8CB2274D5947118C67FBB0B0887EAF8734";
    private final static String initiatorKey82Percent = "9EAC9F9894BC86E1932019AF3B1F3C376C7BBC799F6555B8B623C7ED80E3DD66";


    private int cellProxiesLength = -1;
    private int vertexCount = -1;

    public static final double L = acos(sqrt(5) / 5); // the spherical arclength of the icosahedron's edges.
    private NumberFormat percentInstance = NumberFormat.getPercentInstance();

    Timer timer = new Timer();
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

    public Sphere(int divisions, DatabaseLoader loader) {
        this.divisions = divisions;
        this.databaseLoader = loader;
    }

    public static void main(String[] args) throws Exception {
        new Sphere(Integer.parseInt(System.getProperty("sphere.divisions")), null).buildCells();
    }

    public void buildCells() throws IOException {
        try {
            percentInstance.setMaximumFractionDigits(2);

            //You want 100,000,000 cells so they will be around 2 square miles each.
            cellProxiesLength = PEELS * 2 * this.divisions * this.divisions + 2;
            vertexCount = divisions * divisions * 20;
            proxies = new CellProxy[cellProxiesLength];
            Object averageValue = new Integer[]{34, 93, 90, 45, 83, 94};
            //new File(shmDir).mkdirs();
            // cellProxyIndices = new int[cellProxiesLength * 6];
            //createOffHeapProxyMap(averageValue);
            createOnHeapProxyMap();
            double sphereRadius = Math.pow(AVG_EARTH_RADIUS_MI, 2) * 4 * PI;
            double cellArea = sphereRadius / cellProxiesLength;
            System.out.println("Initialized hex proxies to: " + formatter.format(proxies.length) + " proxies.  Each one will average " + cellArea + " square miles.");

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
                createdProxyCount.incrementAndGet();
                CellProxy hexCell = new CellProxy(this, i);
                if (hexCell.isPentagon()) {
                    pentagonCount.incrementAndGet();
                }
                return hexCell;
            });

            System.out.println("Finished creating the cell proxies. " + pentagonCount + " of them are pentagons.");
            IntStream.range(0, proxies.length).parallel().forEach(i -> {
                //for(int i=0; i < proxies.length; i++) {
                linkedCellCount.incrementAndGet();
                proxies[i].link();
                //}
            });
            System.out.println("Finished linking the cell proxies. Populating Barycenters. There's not much of an update during this point.");

            //for (int i = 0; i < proxies.length; i++) {
            //this.proxies[i].link();
            //}
            populateBarycenters();//For all the proxies, determine and set their barycenters
            System.out.println("Finished populating");
            //System.out.println("Finished getting indexes");
            report();
            final boolean outputKML = Boolean.parseBoolean(System.getProperty("outputKML"));
            if(outputKML) {
                outputKML();
            }

            final float[] lastCellCount = new float[1];
            Timer rateTimer = new Timer();
            TimerTask cellWriteRateTracker = new TimerTask() {
                @Override
                public void run() {
                  int savedCells = savedCellCount.get();
                  float cellsSavedSinceLast = savedCells - lastCellCount[0];
                  lastCellCount[0] = savedCells;
                  cellWriteRate = cellsSavedSinceLast / 1000;
                }
            };

            //  count cells written every 10 seconds
            rateTimer.schedule(cellWriteRateTracker, new Date(), 1000);

            JsonGenerator vertexGenerator = initializeGenerator("vertices", "vertices.json.gz");
            JsonGenerator cellsGenerator = initializeGenerator("cellMap", "cellMap.json.gz");
            double eightyTwoPercent = cellProxiesLength * .82;
            saveCells(vertexGenerator, cellsGenerator, eightyTwoPercent);
            if(databaseLoader != null) {
                databaseLoader.stop();
            }
            try {
                vertexGenerator.writeEndObject();
                cellsGenerator.writeEndObject();
                vertexGenerator.writeEndObject();
                cellsGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeGenerator(vertexGenerator);
                closeGenerator(cellsGenerator);
            }
            rateTimer.cancel();
            timer.cancel();
            task.cancel();
            //System.out.println("Min was: " + minArea + " max was " + maxArea);
            System.out.println("Created and saved " + proxies.length + " cells.\nNow go run constraints.sql");
        } finally {
        }
    }

    private void createOnHeapProxyMap() {
        cellProxyIndexMap = new HashMap();
    }


    private JsonGenerator initializeGenerator(String objectName, String fileName) {

        JsonGenerator generator = null;

        try {
           new File(this.CELLS_DIR.getAbsolutePath()).mkdirs();
           RandomAccessFile raf = new RandomAccessFile(new File(this.CELLS_DIR, fileName), "rw");
           OutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(raf.getFD()), 217447));

           JsonFactory jfactory = new JsonFactory();
           generator = jfactory.createGenerator(out, JsonEncoding.UTF8);
           generator.useDefaultPrettyPrinter();
           generator.writeStartObject();
           generator.writeObjectFieldStart(objectName);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return generator;
    }

    private void writeCells(JsonGenerator cellsGenerator, String initiator, Cell cell) throws IOException {
        cellsGenerator.writeObjectFieldStart(cell.getId());
        cellsGenerator.writeObjectField("initiator", initiator);
        cellsGenerator.writeFieldName("vertices");

        List<Vertex> vertices = cell.getVertices();
        cellsGenerator.writeStartArray();
        for (Vertex vertex : vertices) {
            cellsGenerator.writeString(vertex.getId());
        }
        cellsGenerator.writeEndArray();
        cellsGenerator.writeEndObject();
    }

    private void closeGenerator(JsonGenerator cellsGenerator) {
        if (cellsGenerator != null) {
            try {
                cellsGenerator.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeVertices(Cell cell, JsonGenerator generator)
        throws IOException {
        for (Vertex vertex : cell.getVertices()) {
            generator.writeFieldName(vertex.getId());
            generator.writeArray(new double[]{vertex.getLatitude().doubleValue(), vertex.getLongitude().doubleValue()}, 0, 2);
        }
    }

    private void report() {
        if(!reportingPaused) {
            report("Created", proxies.length, createdProxyCount.get(), "CellProxies");
            report("Linked", proxies.length, linkedCellCount.get(), "CellProxies");
            report("Populated", proxies.length, populatedBaryCenterCount.get(), "Barycenters");
            //report("Saved", vertexCount, savedVertexCount.get(), "Vertexes");
            report("Saved", proxies.length, savedCellCount.get(), "Cells");
            System.out.print(" Writing " + cellWriteRate + " cells per ms");
            System.out.print("\n");
        }
    }

    private void report(String verb, int total, int count, String type) {
        System.out.print(" " + verb + " " + formatter.format(count) +
                " of " + formatter.format(total) +
                " " + type + " (" + percentInstance.format((double) count / (double) total) +
                ")");
    }

    public int getNorthCellIndex() {
        return 0;
    }

    public int getSouthCellIndex() {
        return 1;
    }

    public CellProxy getNorth() {
        return this.proxies[getNorthCellIndex()];
    }

    public CellProxy getSouth() {
        return this.proxies[getSouthCellIndex()];
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

    public int getCellIndex(int s, int x, int y) {
        return s * this.divisions * this.divisions * 2 + x * this.divisions + y + 2;
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
        String fileName = "~/rings.kml";
        File file = new File(fileName);
        try {
            System.out.println("Writing to file: " + file.getAbsolutePath());
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
                proxies[i].kmlString(2000) + "\n" +
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
                proxies[i].kmlString(2000) + "\n" +
                "        </coordinates>\n" +
                "      </LinearRing>\n" +
                "      </outerBoundaryIs>\n" +
                "      </Polygon>\n";
    }




    public void buildCellsFromProxies() {
        int n = this.proxies.length;
        IntStream parallel = IntStream.range(0, n).parallel();
        parallel.forEach(f -> {
            CellProxy proxy = this.proxies[f];
            proxy.populateCell();
        });
        System.out.println("Finished saving cells.  MaxLat = " + maxLat + " MinLat = " + minLat + " MaxLng = " + maxLng + " MinLng " + minLng);

    }


    public void saveCells(JsonGenerator vertexGenerator, JsonGenerator cellsGenerator, double eightyTwoPercent) {
        buildCellsFromProxies();
        for (int f = 0; f < this.proxies.length; f++) {
            String initiator = f > eightyTwoPercent ? initiatorKey18Percent : initiatorKey82Percent;
            CellProxy cellProxy = this.proxies[f];
            try {
                writeVertices(cellProxy.getCell(), vertexGenerator);
                writeCells(cellsGenerator, initiator, cellProxy.getCell());

                if (databaseLoader != null) {
                    databaseLoader.add(cellProxy.getCell());
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

    @NotNull
    private IntStream saveVertices() {
        int n = this.proxies.length;
        IntStream parallel = IntStream.range(0, n).parallel();
        //AreaFinder areaFinder = new AreaFinder();
        //areaFinder.getArea(proxies[0].getVertices(sharedVertexMap));
        parallel.forEach(f -> {
            CellProxy proxy = this.proxies[f];
            List<Vertex> verticesToSave = proxy.populateCell().getVertices();
            for (int i = 0; i < verticesToSave.size(); i++) {
                minLat = min(minLat, verticesToSave.get(i).getLatitude().doubleValue());
                maxLat = max(maxLat, verticesToSave.get(i).getLatitude().doubleValue());
                minLng = min(minLng, verticesToSave.get(i).getLongitude().doubleValue());
                maxLng = max(maxLng, verticesToSave.get(i).getLongitude().doubleValue());
                databaseLoader.add(verticesToSave.get(i));
                savedVertexCount.incrementAndGet();
            }
        });
        databaseLoader.completeVertices();
        return parallel;
    }

    public void incrementBarycenterCount() {
        populatedBaryCenterCount.getAndIncrement();
    }

    public void setAdjacentCellIndices(Integer index, Integer[] adjacentCells) {
        cellProxyIndexMap.put(index, adjacentCells);
    }

    public Integer[] getAdjacentCellIndices(Integer index) {
      return (Integer[]) cellProxyIndexMap.get(index);
    }

    //public String toString() {
    //String out = "";
    //for(int i=0; i < proxies; i++) {
    //out += "Cell " + i + ":\n" + proxies[i] + "\n";
    //}
    //return out;
    //}
}

package com.d4dl.permean.mesh;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

import com.d4dl.permean.io.CellReader;
import com.d4dl.permean.io.CellWriter;
import com.d4dl.permean.io.KMLWriter;
import com.d4dl.permean.io.KinesisWriter;
import com.d4dl.permean.io.NoOpWriter;
import com.d4dl.permean.io.ShortFormatCellReader;
import com.d4dl.permean.io.ShortFormatCellWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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
    private final MeshLoader databaseLoader;
    ProgressReporter reporter;

    private NumberFormat formatter = NumberFormat.getInstance();

    private boolean stackIsDone = false;

    public final static String initiatorKey18Percent = "1F2F34D186D89AA0C8806F9EA9E51F8CB2274D5947118C67FBB0B0887EAF8734";
    public final static String initiatorKey82Percent = "9EAC9F9894BC86E1932019AF3B1F3C376C7BBC799F6555B8B623C7ED80E3DD66";

    private int cellCount = -1;
    private int vertexCount = -1;
    // Track the vertices which have already been seen so the can be used again by
    // cells that share them.  The cell generator will remove vertices from this map when they're no
    // longer needed.
    private Map<String, MeshVertex> vertexCache = new HashMap();
    private AtomicInteger currentVertexIndex = new AtomicInteger(0);

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
    private String fileOut;


    //The laptop can do this easily.  It produces 25 million regions of 7.7 square miles each
    int anotherGoodDivisionsValue = 1600;
    private CellGenerator cellGenerator;

    public Sphere(String fileOut, int divisions, MeshLoader loader) {
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
        reporter = new ProgressReporter("CellGenerator", cellCount, vertexCount, vertexCache);
        reporter.setCellCount(cellCount);
        reporter.setVertexCount(vertexCount);
        cellGenerator = new CellGenerator(cellCount, this.divisions, reporter);
        Object averageValue = new Integer[]{34, 93, 90, 45, 83, 94};
        double sphereRadius = Math.pow(AVG_EARTH_RADIUS_MI, 2) * 4 * PI;
        double cellArea = sphereRadius / cellCount;
        System.out.println("Initialized cells to: " + formatter.format(cellCount) + " cells.  Each one will average " + cellArea + " square miles.");
        reporter.start();


        try {
            final String kmlOutFile = System.getProperty("kmlOutFile");

            BarycenterBuilder barycenterBuilder = new BarycenterBuilder(cellGenerator, divisions);
            barycenterBuilder.populateBarycenters();//For all the ells, determine and set their barycenters
            System.out.println("Finished populating");

            String outputKMLSampleSizeProperty = System.getProperty("outputKMLSampleSize");
            String latLngBoundsProperty = System.getProperty("latLngBounds");
            //Just output some cells to see what the kml looks like
            if (latLngBoundsProperty != null) {
                if (kmlOutFile == null) {
                    throw new IllegalArgumentException("-DkmlOutFile must be specified");
                }
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

                List<MeshCell> cellList = new ArrayList();
                for(int i=0; i < cellCount; i++) {
                  Position barycenter = cellGenerator.getBarycenter(i);
                  double barycenterLat = barycenter.getLat();
                  double barycenterLng = barycenter.getLng();
                  if (lat1 < barycenterLat && barycenterLat < lat2 &&
                      lng1 < barycenterLng && barycenterLng < lng2) {
                      cellList.add(cellGenerator.populateCell(i, "nobody", null, null));
                  }
                }
                CellReader reader = new ShortFormatCellReader("KMLReader", fileOut);
                new KMLWriter(kmlOutFile).outputKML(cellList.toArray(new MeshCell[0]));
            } else if (outputKMLSampleSizeProperty != null) {
              if (kmlOutFile == null) {
                  throw new IllegalArgumentException("-DkmlOutFile must be specified");
              }
              int outKMLSampleSize = Integer.parseInt(outputKMLSampleSizeProperty);
              MeshCell[] cells = new MeshCell[outKMLSampleSize];
              IntStream parallel = IntStream.range(0, outKMLSampleSize).parallel();
              parallel.forEach(f -> {
                  reporter.incrementBuiltCellCount();
                  cells[f] = cellGenerator.populateCell(f, "nobody", null, null);
              });
              new KMLWriter(kmlOutFile).outputKML(cells);
            } else {
                writeCells(cellCount, reporter);
                stackIsDone = true;
            }
        } finally {
            if (databaseLoader != null) {
                databaseLoader.stop();
            }
        }
        reporter.stop();
    }


    public void writeCells(int cellCount, ProgressReporter progressReporter) {
        System.out.println("Building cells.");
        CellWriter writer;
        if(Boolean.parseBoolean(System.getProperty("kinesis"))) {
            writer = new KinesisWriter(progressReporter, null, 20);
        }
        else if(Boolean.parseBoolean(System.getProperty("noop"))) {
            writer = new NoOpWriter(progressReporter);
        } else {
            writer = new ShortFormatCellWriter(progressReporter, fileOut);
        }
        writer.setCountsAndStartWriting(cellCount, vertexCount);

        writeCells(cellCount, writer, true);
        System.out.println("\n\nWrote the pentagons");
        writeCells(cellCount, writer, false);
        System.out.println("Finished building cells.");
        reporter.report();
    }

    private void writeCells(int cellCount, CellWriter writer, boolean pentagons) {
        IntStream parallel = IntStream.range(0, cellCount).parallel();
        //parallel.forEach(f -> {
        for (int f=0; f < cellCount;) {
            if(pentagons && cellGenerator.isPentagon(f) || !pentagons && !cellGenerator.isPentagon(f)) {
                double random = Math.random() * 100;
                reporter.incrementBuiltCellCount();
                String initiator = random > 82 ? initiatorKey18Percent : Sphere.initiatorKey82Percent;
                MeshCell cell = cellGenerator.populateCell(f, initiator, vertexCache, currentVertexIndex);
                //System.out.println("Writing a cell with " + cell.getVertices().length + " vertices.");
                writer.writeCell(cell, true);
            }
            f++;
        }
        //);
    }

    public void saveCells(MeshCell[] cells, double eightyTwoPercent) {
        for (int f = 0; f < cells.length; f++) {
            MeshCell cell = cells[f];
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





    private IntStream saveVertices(MeshCell[] cells) {
        IntStream parallel = IntStream.range(0, cells.length).parallel();
        //AreaFinder areaFinder = new AreaFinder();
        //areaFinder.getArea(cells[0].getVertices(sharedVertexMap));
        parallel.forEach(f -> {
            MeshCell cell = cells[f];
            MeshVertex[] verticesToSave = cell.getVertices();
            for (int i = 0; i < verticesToSave.length; i++) {
                MeshVertex vertex = verticesToSave[i];
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
        return parallel;
    }

    //public String toString() {
    //String out = "";
    //for(int i=0; i < cells; i++) {
    //out += "MeshCell " + i + ":\n" + cells[i] + "\n";
    //}
    //return out;
    //}
}

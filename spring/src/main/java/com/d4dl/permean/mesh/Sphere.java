package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.CellRepository;
import com.d4dl.permean.data.Vertex;
import com.d4dl.permean.data.VertexRepository;
import net.openhft.chronicle.map.ChronicleMap;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.apfloat.ApfloatMath.acos;
import static org.apfloat.ApfloatMath.sqrt;

/**
 * Represents a sphere of fields arranged in an interpolated icosahedral geodesic pattern.
 *
 * @param {object} options
 * @param {number} [options.divisions = 8] - Integer greater than 0:
 *                 the number of fields per edge of the icosahedron including
 *                 the origin of the edge (but not the endpoint). Determines the
 *                 angular resolution of the sphere. Directly & exponentially related
 *                 to the memory consumption of a Sphere.
 * @param {object} [options.data] - JSON representation of a sphere.
 *                 Should follow the same schema as output by Sphere.serialize. Constructor
 *                 will throw an error if it doesn't.
 * @constructor
 */
public class Sphere {
    private Apfloat pi = ApfloatMath.pi(1000);

    public static int PEELS = 5;
    private final int divisions;
    private final AtomicInteger pentagonCount = new AtomicInteger(0);
    private HexField[] fields;
    private boolean iterating;
    private AtomicInteger createdFieldCount = new AtomicInteger(0);
    private AtomicInteger linkedFieldCount = new AtomicInteger(0);
    private AtomicInteger populatedFieldCount = new AtomicInteger(0);

    private AtomicInteger triangleCount = new AtomicInteger(0);
    private AtomicInteger centroidCount = new AtomicInteger(0);
    private AtomicInteger indexCount = new AtomicInteger(0);

    public static final Apfloat L = acos(sqrt(new Apfloat(2, 200)).divide(new Apfloat(5, 200))); // the spherical arclength of the icosahedron's edges.
    private NumberFormat percentInstance = NumberFormat.getPercentInstance();

    private ChronicleMap<Integer, Integer> interfieldTriangles = null;
    private ChronicleMap<Integer, Position> interfieldCentroids = null;
    private ChronicleMap<Integer, Integer> interfieldIndices = null;
    private int indicesSize = -1;
    private int centroidsSize = -1;
    private int trianglesSize = -1;
    Timer timer=new Timer();
    double minArea = Integer.MAX_VALUE;
    double maxArea = Integer.MIN_VALUE;
    //private static double AVG_EARTH_RADIUS_MI = 3959;

    private VertexRepository vertexRepository;
    private CellRepository cellRepository;

    public static void main(String args[]) {
        new Sphere(1);
    }

    public Sphere(int divisions, VertexRepository vertexRepository, CellRepository cellRepository) {
        this(divisions);
        this.vertexRepository = vertexRepository;
        this.cellRepository = cellRepository;
    }

    public Sphere(int divisions) {
        this.divisions = divisions;
    }

    public void buildCells() {
        percentInstance.setMaximumFractionDigits(2);

        fields = new HexField[(PEELS * 2 * this.divisions * this.divisions + 2)];
        System.out.println("Initialized hex fields to: " + fields.length + " fields.");

        //List<HexField> fieldList = Arrays.asList(fields);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                percentInstance = NumberFormat.getPercentInstance();
                report();
            }
        };
        //  task will be scheduled after 5 sec delay
        timer.schedule(task, 1000, 1000);

        Arrays.parallelSetAll(fields, i -> {
            createdFieldCount.incrementAndGet();
            HexField hexField = new HexField(this, i, null);
            if(hexField.isPentagon()) {
                pentagonCount.incrementAndGet();
            }
            return hexField;
        });

        System.out.println("Finished creating the hex fields");
        IntStream.range(0, fields.length).parallel().forEach(i -> {
            linkedFieldCount.incrementAndGet();
            fields[i].link();
        });
        System.out.println("Finished linking the hex fields");

        //for (int i = 0; i < fields.length; i++) {
        //this.fields[i].link();
        //}
        populate();
        System.out.println("Finished populating");
        getInterfieldCentroids();
        System.out.println("Finished getting centroids");
        getInterfieldIndices();
        System.out.println("Finished getting indexes");
        report();
        timer.cancel();
        populateAreas();
        saveCells();
        if(vertexRepository != null) {
            System.out.println("Saved " + cellRepository.count() + " cells.");
            System.out.println("Saved " + vertexRepository.count() + " vertexes.");
        }
        System.out.println("Min was: " + minArea + " max was " + maxArea);
    }

    private void report() {
        System.out.println("Created " + createdFieldCount +
                " of " + fields.length +
                " fields (" + percentInstance.format((double)createdFieldCount.get()/(double)fields.length) +
                "). Linked " + linkedFieldCount +
                " of " + fields.length +
                " fields (" + percentInstance.format((double)linkedFieldCount.get()/(double)fields.length) +
                ") Created " + this.triangleCount +
                " of " + trianglesSize +
                " triangles (" + percentInstance.format((double) this.triangleCount.get()/(double) trianglesSize) +
                ") Created " + this.centroidCount +
                " of " + centroidsSize +
                " centroids (" + percentInstance.format((double) this.centroidCount.get()/(double) centroidsSize) +
                ") Created " + this.indexCount +
                " of " + indicesSize +
                " indexes (" + percentInstance.format((double) this.indexCount.get()/(double) indicesSize) +
                ")\r");
    }

    public HexField getNorth() {
        return this.fields[0];
    }

    public HexField getSouth() {
        return this.fields[1];
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
     * Sets the barycenter position of every field on a Sphere.
     *
     * @this {Sphere}
     */
    public void populate() {
        int max_x = 2 * divisions - 1;
        Apfloat[] buf = new Apfloat[((divisions - 1) * 2)];


        // Determine position for polar and tropical fields using only arithmetic.

        fields[0].setPosition(pi.divide(new Apfloat(2, 200)), new Apfloat(0, 200));
        fields[1].setPosition(pi.divide(new Apfloat(-2, 200)), new Apfloat(0));

        for (int s = 0; s < Sphere.PEELS; s += 1) {
            Apfloat λNorth = new Apfloat(s, 200).multiply(new Apfloat(2, 200)).divide(new Apfloat(5, 200)).multiply(pi);
            Apfloat λSouth = λNorth.add(pi.divide(new Apfloat(5)));

            this.get(s, divisions - 1, 0).setPosition(pi.divide(new Apfloat(2, 200)).subtract(L), λNorth);
            this.get(s, max_x, 0).setPosition(pi.divide(new Apfloat(-2, 200)).add(L), λSouth);
        }

        // Determine positions for the fields along the edges using arc interpolation.

        if ((divisions - 1) > 0) { // divisions must be at least 2 for there to be fields between vertices.

            for (final AtomicInteger s = new AtomicInteger(0); s.get() < Sphere.PEELS; s.incrementAndGet()) {
                int p = (s.get() + 4) % Sphere.PEELS;
                int northPole = 0;
                int southPole = 1;
                int currentNorthTropicalPentagon = this.get(s.get(), divisions - 1, 0).getIndex();
                int previousNorthTropicalPentagon = this.get(p, divisions - 1, 0).getIndex();
                int currentSouthTropicalPentagon = this.get(s.get(), max_x, 0).getIndex();
                int previousSoutTropicalPentagon = this.get(p, max_x, 0).getIndex();

                // north pole to current north tropical pentagon
                this.fields[northPole].getPosition().interpolate(this.fields[currentNorthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), i - 1, 0).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current north tropical pentagon to previous north tropical pentagon
                this.fields[currentNorthTropicalPentagon].getPosition().interpolate(this.fields[previousNorthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), divisions - 1 - i, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current north tropical pentagon to previous south tropical pentagon
                this.fields[currentNorthTropicalPentagon].getPosition().interpolate(this.fields[previousSoutTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), divisions - 1, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current north tropical pentagon to current south tropical pentagon
                this.fields[currentNorthTropicalPentagon].getPosition().interpolate(this.fields[currentSouthTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), divisions - 1 + i, 0).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current south tropical pentagon to previous south tropical pentagon
                this.fields[currentSouthTropicalPentagon].getPosition().interpolate(this.fields[previousSoutTropicalPentagon].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), max_x - i, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });

                // current south tropical pentagon to south pole
                this.fields[currentSouthTropicalPentagon].getPosition().interpolate(this.fields[southPole].getPosition(), divisions, buf);
                IntStream.range(1, divisions).parallel().forEach(i -> {
                    this.get(s.get(), max_x, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                });
            }
        }

        // Determine positions for fields between edges using interpolation.

        if ((divisions - 2) > 0) { // divisions must be at least 3 for there to be fields not along edges.
            for (final AtomicInteger s = new AtomicInteger(0); s.get() < Sphere.PEELS; s.incrementAndGet()) {
                IntStream.range(0, divisions * 2).parallel().forEach(x -> {
                    // for each column, fill in values for fields between edge fields,
                    // whose positions were defined in the previous block.
                    if ((x + 1) % divisions > 0) { // ignore the columns that are edges.

                        int j = divisions - ((x + 1) % divisions); // the y index of the field in this column that is along a diagonal edge
                        int n1 = j - 1; // the number of unpositioned fields before j
                        int n2 = divisions - 1 - j; // the number of unpositioned fields after j
                        int f1 = this.get(s.get(), x, 0).getIndex(); // the field along the early edge
                        int f2 = this.get(s.get(), x, j).getIndex(); // the field along the diagonal edge
                        int f3 = this.get(s.get(), x, divisions - 1).getAdjacent(2).getIndex(); // the field along the later edge,
                        // which will necessarily belong to
                        // another section.

                        this.fields[f1].getPosition().interpolate(this.fields[f2].getPosition(), n1 + 1, buf);
                        for (int i = 1; i < j; i += 1) {
                            this.get(s.get(), x, i).setPosition(buf[2 * (i - 1) + 0], buf[2 * (i - 1) + 1]);
                        }

                        this.fields[f2].getPosition().interpolate(this.fields[f3].getPosition(), n2 + 1, buf);
                        for (int i = j + 1; i < divisions; i += 1) {
                            this.get(s.get(), x, i).setPosition(buf[2 * (i - j - 1) + 0], buf[2 * (i - j - 1) + 1]);
                        }
                    }
                });
            }
        }
    }

    public int getPentagonCount() {
        return pentagonCount.get();
    }

    public HexField[] getFields() {
        return fields;
    }

    public HexField getField(int i) {
        return fields[i];
    }

    public HexField get(int s, int x, int y) {
        return this.fields[s * this.divisions * this.divisions * 2 + x * this.divisions + y + 2];
    }


    public void populateAreas() {
        AreaFinder areaFinder = new AreaFinder();
        //Prime the thing
        areaFinder.getArea(fields[0].getVertices());

        //IntStream.range(0, fieldsSize).parallel().forEach(f -> {
            //double area = areaFinder.getArea(fields[f].getLats(), fields[f].getLngs());
            //fields[f].setArea(area);
            ////System.out.println("Field " + i + " area: " + area + " first area: " + fields[i].getArea());
        //});
        for(int i=0; i < fields.length; i++) {
            double areaAngle = areaFinder.getArea(fields[i].getVertices());
            //double areaSide = Math.sqrt(areaAngle);
            //double areaSideRadians = areaSide * (180/PI);
            //double sideLengthMI = areaSideRadians * AVG_EARTH_RADIUS_MI;
            //double areaMiles = sideLengthMI * sideLengthMI;
            //System.out.println("Field " + i + " area: " + areaMiles + " sq. mi.");
            minArea = java.lang.Math.min(minArea, areaAngle);
            maxArea = java.lang.Math.max(maxArea, areaAngle);
            System.out.println("Field " + i + " area: " + areaAngle);
            //System.out.println("\t<Placemark>\n" +
                    //"\t\t<name>Field " + i + "</name>\n" +
                    //"\t\t<styleUrl>#m_ylw-pushpin0</styleUrl>\n" +
                    //"\t\t<Polygon>\n" +
                    //"\t\t\t<tessellate>1</tessellate>\n" +
                    //"\t\t\t<outerBoundaryIs>\n" +
                    //"\t\t\t\t<LinearRing>\n" +
                    //"\t\t\t\t\t<coordinates>\n" +
                    //"\t\t\t\t\t\t" + fields[i]+ "\n" +
                    //"\t\t\t\t\t</coordinates>\n" +
                    //"\t\t\t\t</LinearRing>\n" +
                    //"\t\t\t</outerBoundaryIs>\n" +
                    //"\t\t</Polygon>\n" +
                    //"\t</Placemark>");
            //if(i % 400 == 0)  {
                //System.out.println(fields[i]);
            //}
        }
    }

    public ChronicleMap<Integer, Integer> getInterfieldTriangles() {
        if(interfieldTriangles == null) {
            int length = this.fields.length;
            trianglesSize = ((2 * length - 4) * 3);
            interfieldTriangles = ChronicleMap
                    .of(Integer.class, Integer.class)
                    .name("interfieldTriangles")
                    .entries(trianglesSize)
                    .create();

            System.out.println("Initialized interfield triangles to: " + interfieldTriangles.size() + " triangles.");
            IntStream.range(0, length).parallel().forEach(f -> {
                fields[f].getInterfieldTriangles(interfieldTriangles);
                triangleCount.addAndGet(6);
            });

            System.out.println("Finished creating triangles");
        }

        return interfieldTriangles;
    }


    public ChronicleMap<Integer, Position> getInterfieldCentroids() {
        ChronicleMap<Integer, Integer> triangles = getInterfieldTriangles();
        if(interfieldCentroids == null) {
            centroidsSize = triangles.size() / 3;
            interfieldCentroids =ChronicleMap
                    .of(Integer.class, Position.class)
                    .name("interfieldCentroids")
                    .entries(centroidsSize)
                    .averageValue(new Position(new Apfloat(1), new Apfloat(1)))
                    .create();
            int length = triangles.size() / 3;
            System.out.println("Initialized interfield centroids to: " + interfieldCentroids.size() + " centroids.");

            IntStream.range(0, length).parallel().forEach(centroidIndex -> {
            //for(int centroidIndex=0; centroidIndex < length; centroidIndex++) {
                int triangleIndex = 3 * centroidIndex;
                int fieldIndex = triangles.get(triangleIndex);
                Position centroid = fields[fieldIndex].getInterfieldCentroids(triangles, centroidIndex, interfieldCentroids);
                if(vertexRepository != null){
                    Vertex vertex = new Vertex(centroidIndex, centroid.getLat().doubleValue(), centroid.getLng().doubleValue());
                    centroid.setVertex(vertexRepository.save(vertex));
                }
                //interfieldCentroids.put(centroidIndex, centroid);
                centroidCount.getAndIncrement();
            ////}
            });
            System.out.println("Finished creating centroids");
        }
        return interfieldCentroids;
    }


    public void saveCells() {
        int n = this.fields.length;
        IntStream.range(0, n).parallel().forEach(f -> {
            HexField field = this.fields[f];
            field.getInterfieldIndices(interfieldIndices, interfieldTriangles);
            Position[] positions = fields[f].getVertices();
            if(cellRepository != null) {
                List<Vertex> vertices = new ArrayList();
                for (int i = 0; i < positions.length; i++) {
                    vertices.add(positions[i].getVertex());
                }
                Cell cell = new Cell(vertices, field.getArea());
                cellRepository.save(cell);
            }
            indexCount.getAndAdd(field.getAdjacentFields().length);
        });
        System.out.println("Finished saving cells");
    }

    public ChronicleMap<Integer, Integer>  getInterfieldIndices() {
        if(interfieldIndices == null) {
            int n = this.fields.length;
            indicesSize = 6 * n;
            interfieldIndices = ChronicleMap.of(Integer.class, Integer.class)
                    .name("interfieldIndices")
                    .entries(indicesSize)
                    .create();
            System.out.println("Initialized interfield indices to: " + interfieldTriangles.size() + " indices.");

            IntStream.range(0, n).parallel().forEach(f -> {
                HexField field = this.fields[f];
                field.getInterfieldIndices(interfieldIndices, interfieldTriangles);
                indexCount.getAndAdd(field.getAdjacentFields().length);
            });
            System.out.println("Finished creating indices");
        }

        return interfieldIndices;
    }



    //public String toString() {
        //String out = "";
        //for(int i=0; i < fieldsSize; i++) {
            //out += "Field " + i + ":\n" + fields[i] + "\n";
        //}
        //return out;
    //}
}

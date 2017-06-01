package mesh;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.lang.Math.*;

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
    public static int PEELS = 5;
    private final int divisions;
    private final AtomicInteger pentagonCount = new AtomicInteger(0);
    private final HexField[] fields;
    private boolean iterating;
    private AtomicInteger createdFieldCount = new AtomicInteger(0);
    private AtomicInteger linkedFieldCount = new AtomicInteger(0);
    private AtomicInteger populatedFieldCount = new AtomicInteger(0);

    private AtomicInteger triangleCount = new AtomicInteger(0);
    private AtomicInteger centroidCount = new AtomicInteger(0);
    private AtomicInteger indexCount = new AtomicInteger(0);

    public static final double L = acos(sqrt(5) / 5); // the spherical arclength of the icosahedron's edges.
    private NumberFormat percentInstance = NumberFormat.getPercentInstance();
    private int[] interfieldTriangles = null;
    private Position[] interfieldCentroids = null;
    private int[] interfieldIndices = null;

    public static void main(String args[]) {
        new Sphere(1);
    }

    public Sphere(int divisions) {
        percentInstance.setMaximumFractionDigits(2);
        this.divisions = divisions;

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
        Timer timer=new Timer();
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
        populateAreas();

    }

    private void report() {
        int triangleCount = interfieldTriangles != null ? interfieldTriangles.length : -1;
        int centroidCount = interfieldCentroids != null ? interfieldCentroids.length : -1;
        int indexCount = interfieldIndices != null ? interfieldIndices.length : -1;
        System.out.println("Created " + createdFieldCount +
                " of " + fields.length +
                " fields (" + percentInstance.format((double)createdFieldCount.get()/(double)fields.length) +
                "). Linked " + linkedFieldCount +
                " of " + fields.length +
                " fields (" + percentInstance.format((double)linkedFieldCount.get()/(double)fields.length) +
                ") Created " + this.triangleCount +
                " of " + triangleCount +
                " triangles (" + percentInstance.format((double) this.triangleCount.get()/(double) triangleCount) +
                ") Created " + this.centroidCount +
                " of " + centroidCount +
                " indexes (" + percentInstance.format((double) this.centroidCount.get()/(double) centroidCount) +
                ") Created " + this.indexCount +
                " of " + indexCount +
                " centroids (" + percentInstance.format((double) this.indexCount.get()/(double) indexCount) +
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
        TimerTask timer = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Sphere is populating: " + fields.length + " fields");
            }
        };
        int max_x = 2 * divisions - 1;
        double[] buf = new double[((divisions - 1) * 2)];


        // Determine position for polar and tropical fields using only arithmetic.

        fields[0].setPosition(PI / 2, 0);
        fields[1].setPosition(PI / -2, 0);

        for (int s = 0; s < Sphere.PEELS; s += 1) {
            double λNorth = ((double)s) * 2 / 5 * PI;
            double λSouth = ((double)s) * 2 / 5 * PI + PI / 5;

            this.get(s, divisions - 1, 0).setPosition(PI / 2 - L, λNorth);
            this.get(s, max_x, 0).setPosition(PI / -2 + L, λSouth);
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
        areaFinder.getArea(fields[0].getLats(), fields[1].getLngs());
        IntStream.range(0, fields.length).parallel().forEach(f -> {
            double area = areaFinder.getArea(fields[f].getLats(), fields[f].getLngs());
            fields[f].setArea(area);
        });
        for(int i=0; i < fields.length; i++) {
            double area = areaFinder.getArea(fields[i].getLats(), fields[i].getLngs());
            //System.out.println("Field " + i + " area: " + area + " first area: " + fields[i].getArea());
        }
    }

    public int[] getInterfieldTriangles() {
        if(interfieldTriangles == null) {
            int length = this.fields.length;
            interfieldTriangles = new int[((2 * length - 4) * 3)];

            System.out.println("Initialized interfield triangles to: " + interfieldTriangles.length + " triangles.");
            IntStream.range(0, length).parallel().forEach(f -> {
                fields[f].getInterfieldTriangles(interfieldTriangles);
                triangleCount.getAndAdd(6);
            });

        }

        return interfieldTriangles;
    }


    public Position[] getInterfieldCentroids() {
        int[] triangles = getInterfieldTriangles();
        if(interfieldCentroids == null) {
            interfieldCentroids = new Position[triangles.length / 3];
            int length = interfieldCentroids.length;
            System.out.println("Initialized interfield centroids to: " + interfieldCentroids.length + " centroids.");

            IntStream.range(0, length).parallel().forEach(centroidIndex -> {
                int triangleIndex = 3 * centroidIndex;
                int fieldIndex = triangles[triangleIndex];
                fields[fieldIndex].getInterfieldCentroids(triangles, centroidIndex, interfieldCentroids);
                centroidCount.getAndIncrement();
            });
        }
        return interfieldCentroids;
    }


    public int[] getInterfieldIndices() {
        if(interfieldIndices == null) {
            int n = this.fields.length;
            interfieldIndices = new int[6 * n];
            System.out.println("Initialized interfield indices to: " + interfieldTriangles.length + " indices.");

            IntStream.range(0, n).parallel().forEach(f -> {
                HexField field = this.fields[f];
                field.getInterfieldIndices(interfieldIndices, interfieldTriangles);
                indexCount.getAndAdd(field.getAdjacentFields().length);
            });
        }

        return interfieldIndices;
    }



    public String toString() {
        String out = "";
        for(int i=0; i < fields.length; i++) {
            out += "Field " + i + ":\n" + fields[i] + "\n";
        }
        return out;
    }
}

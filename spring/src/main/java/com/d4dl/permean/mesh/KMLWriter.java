package com.d4dl.permean.mesh;

import com.d4dl.permean.ProgressReporter;
import com.d4dl.permean.data.Cell;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;

/**
 * Created by joshuadeford on 10/23/18.
 */

public class KMLWriter {

    public void outputKML(CellSerializer serializer, Cell[] cells) throws IOException {
        outputKML(serializer, cells, -1);
    }
    public void outputKML(CellSerializer serializer, Cell[] cells, int cellLimit) throws IOException {
        String fileName = "~/rings.kml";
        File file = new File(fileName);
        System.out.println("Writing to file: " + file.getAbsolutePath());
        BufferedWriter writer = null;
        int writtenCellCount = 0;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            String[] styles = new String[]{"transBluePoly", "transRedPoly", "transGreenPoly", "transYellowPoly"};
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            int limit = cells.length;//20
            for (int i = 0; i < limit; i++) {
                if (cellLimit < 0 || writtenCellCount++ < cellLimit) {
                    int styleIndex = initiatorKey18Percent.equals(cells[i].getInitiator()) ? 0 : 2;
                    if (initiatorKey18Percent.equals(cells[i].getInitiator()))
                        writer.write("    <Placemark>\n" +
                                "      <name>" + cells[i] + " " + cells[i].getArea() + "</name>\n" +
                                "      <styleUrl>#" + styles[styleIndex] + "</styleUrl>\n" +
                                //"      <styleUrl>#" + styles[0] + "</styleUrl>\n" +
                                getLineString(serializer, cells[i]) +
                                //getPolygon(serializer, cells[i]) +
                                "    </Placemark>\n");
                } else {
                    break;
                }
            }
            writer.write("  </Document>\n" +
                    "</kml>");
            System.out.println("Wrote to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }


    private String getLineString(CellSerializer serializer, Cell cell) {
        return
                "      <LineString>\n" +
                        "        <tesselate>1</tesselate>\n" +
                        "        <altitudeMode>relativeToGround</altitudeMode>\n" +
                        "        <coordinates>\n" +
                        serializer.kmlString(2000, cell) + "\n" +
                        "        </coordinates>\n" +
                        "      </LineString>\n";
    }
    private String getPolygon(CellSerializer serializer, Cell cell) {
        return
                "      <Polygon>\n" +
                        "      <outerBoundaryIs>\n" +
                        "      <LinearRing>\n" +
                        "        <tesselate>1</tesselate>\n" +
                        "        <altitudeMode>relativeToGround</altitudeMode>\n" +
                        "        <coordinates>\n" +
                        serializer.kmlString(2000, cell) + "\n" +
                        "        </coordinates>\n" +
                        "      </LinearRing>\n" +
                        "      </outerBoundaryIs>\n" +
                        "      </Polygon>\n";
    }

    public static void main(String[] args) throws IOException {
        ProgressReporter readReporter = new ProgressReporter("ReadLong" + 0, 0, 0, null);
        CellSerializer deSerializer = new CellSerializer(args[0], args[1], readReporter, false);
        new KMLWriter().outputKML(deSerializer, deSerializer.readCells(), 1000);
        deSerializer.close();
    }
}

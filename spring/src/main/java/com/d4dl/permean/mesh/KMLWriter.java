package com.d4dl.permean.mesh;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;

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
   outputKML(serializer, cells, false, -1);
  }

 public void outputKML(CellSerializer serializer, Cell[] cells, boolean initiator18Only, int limit) throws IOException {
    String fileName = "~/rings.kml";
    File file = new File(fileName);
    System.out.println("Writing to file: " + file.getAbsolutePath());
    BufferedWriter writer = null;
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
      int length = limit < 0 ? cells.length : limit;
      for (int i = 0; i < length; i++) {
        int styleIndex = initiatorKey18Percent.equals(cells[i].getInitiator()) ? 0 : 2;
        if (!initiator18Only || initiatorKey18Percent.equals(cells[i].getInitiator()))
          writer.write("    <Placemark>\n" +
              "      <name>" + cells[i] + " " + cells[i].getArea() + "</name>\n" +
              "      <styleUrl>#" + styles[styleIndex] + "</styleUrl>\n" +
              //"      <styleUrl>#" + styles[0] + "</styleUrl>\n" +
              getLineString(serializer, cells[i]) +
              //getPolygon(serializer, cells[i]) +
              "    </Placemark>\n");
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
    ProgressReporter readReporter = null;
    CellSerializer deSerializer = null;
    try {
      readReporter = new ProgressReporter("Read For KML " + 0, 0, 0, null);
      readReporter.start();
      //Read from short file and create KML
      readReporter.reset();
      deSerializer = new CellSerializer(args[0], args[1], readReporter, false);
      new KMLWriter().outputKML(deSerializer, deSerializer.readCells());
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      readReporter.stop();
      if (deSerializer != null) {
        deSerializer.close();
      }
    }
  }
}

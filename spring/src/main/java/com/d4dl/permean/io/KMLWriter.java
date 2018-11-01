package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;

import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.ProgressReporter;
import com.d4dl.permean.data.Cell;

import com.d4dl.permean.data.Vertex;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by joshuadeford on 10/23/18.
 */

public class KMLWriter {

  private final String fileName;

  public KMLWriter(String kmlOutFile) {
    this.fileName = kmlOutFile;
  }

  public void outputKML(MeshCell[] cells) throws IOException {
   outputKML(cells, false, -1);
  }

 public void outputKML(MeshCell[] cells, boolean initiator18Only, int limit) throws IOException {
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
      int length = limit < 0 ? cells.length : Math.min(cells.length, limit);
      for (int i = 0; i < length; i++) {
        int styleIndex = initiatorKey18Percent.equals(cells[i].getInitiator()) ? 0 : 2;
        if (!initiator18Only || initiatorKey18Percent.equals(cells[i].getInitiator()))
          writer.write("    <Placemark>\n" +
              "      <name>" + cells[i] + " " + cells[i].getArea() + "</name>\n" +
              "      <styleUrl>#" + styles[styleIndex] + "</styleUrl>\n" +
              //"      <styleUrl>#" + styles[0] + "</styleUrl>\n" +
              getLineString(cells[i]) +
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

  private String getLineString(MeshCell cell) {
    return
        "      <LineString>\n" +
            "        <tesselate>1</tesselate>\n" +
            "        <altitudeMode>relativeToGround</altitudeMode>\n" +
            "        <coordinates>\n" +
            kmlString(2000, cell) + "\n" +
            "        </coordinates>\n" +
            "      </LineString>\n";
  }

  private String getPolygon(Cell cell) {
    return
        "      <Polygon>\n" +
            "      <outerBoundaryIs>\n" +
            "      <LinearRing>\n" +
            "        <tesselate>1</tesselate>\n" +
            "        <altitudeMode>relativeToGround</altitudeMode>\n" +
            "        <coordinates>\n" +
            kmlString(2000, cell) + "\n" +
            "        </coordinates>\n" +
            "      </LinearRing>\n" +
            "      </outerBoundaryIs>\n" +
            "      </Polygon>\n";
  }




  public String kmlString(int height, MeshCell cell) {
    Vertex[] vertices = cell.getVertices();
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < vertices.length; i++) {
      if(i > 0) {
        buff.append("\n");
      }
      buff.append(vertices[i].kmlString(height));
    }
    buff.append("\n");
    buff.append(vertices[0].kmlString(height));//Make the first one also last.
    return buff.toString();
  }


  /**
   * Write a kml file from a short format cell file
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    ProgressReporter readReporter = null;
    CellReader cellReader = new ShortFormatMappedCellReader("KMLReader", args[0]);
    try {
      final boolean initiator18Only = Boolean.parseBoolean(System.getProperty("initiator18Only"));
      readReporter = new ProgressReporter("Read For KML " + 0, 0, 0);
      readReporter.start();
      //Read from short file and create KML
      readReporter.reset();
      new KMLWriter(args[1]).outputKML(cellReader.readCells(null), initiator18Only, -1);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      readReporter.stop();
      cellReader.close();
    }
  }
}

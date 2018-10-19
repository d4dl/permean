package com.d4dl.permean.mesh;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class CellSerializer {

  private static final byte FIVE_VERTEX_CELL = 5;
  private static final byte SIX_VERTEX_CELL = 6;
  private static final String FILE_NAME = "cellMap.json";
  public static final int VERTEX_BYTE_SIZE = (Long.BYTES + Long.BYTES + (2 * Float.BYTES));
  private static final int VERTEX_AND_CELL_COUNT_SIZE = 8;

  private static ByteBuffer SIX_VERTEX_CELL_BUFFER = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          6 * (Long.BYTES + Long.BYTES)
  );

  private static ByteBuffer FIVE_VERTEX_CELL_BUFFER = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          5 * (Long.BYTES + Long.BYTES)
  );

  public static final File CELLS_DIR = new File("/tmp/cells");
  private int vertexFileOffset;
  private AtomicInteger savedVertexCount;
  private AtomicInteger savedCellCount;
  private AtomicInteger builtProxyCount;
  FileChannel cellWriter = null;
  FileChannel vertexWriter = null;
  double eightyTwoPercent;

  public CellSerializer(int totalCellCount, int totalVertexCount, AtomicInteger savedCellCount, AtomicInteger savedVertexCount, AtomicInteger builtProxyCount) {
    this.savedCellCount = savedCellCount;
    this.savedVertexCount = savedVertexCount;
    this.builtProxyCount = builtProxyCount;
    this.vertexFileOffset = totalVertexCount * VERTEX_BYTE_SIZE + VERTEX_AND_CELL_COUNT_SIZE;
    this.eightyTwoPercent = totalCellCount * .82;
    initializeWriters();
    writeCounts(totalCellCount, totalVertexCount);

  }

  public CellSerializer() {

  }

  private void writeCounts(int cellCount, int vertexCount) {
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(
          Integer.BYTES + Integer.BYTES
      );
      // Start with the number of cells
      buffer.putInt(cellCount);
      // Then the number of vertices
      buffer.putInt(vertexCount);

      buffer.flip();
      vertexWriter.write(buffer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void writeCell(Cell cell) {
    try {
      writeVertices(cell, savedVertexCount);
      String initiator = savedCellCount.get() > eightyTwoPercent ? initiatorKey18Percent : Sphere.initiatorKey82Percent;
      savedCellCount.incrementAndGet();
      writeCell(initiator, cell);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeCell(String initiator, Cell cell) throws IOException {
    long uuidMSB = cell.getId().getMostSignificantBits();
    long uuidLSB = cell.getId().getLeastSignificantBits();
    int vertexCount = cell.getVertices().size();
    ByteBuffer buffer = vertexCount == 5 ? FIVE_VERTEX_CELL_BUFFER : SIX_VERTEX_CELL_BUFFER;

    buffer.putLong(uuidMSB);
    buffer.putLong(uuidLSB);
    buffer.put((byte) (initiator.equals(initiatorKey18Percent) ? 1 : 0));//Then a byte 0 or 1 depending on which initiator it is
    // A byte here less than 6 indicates its a list of vertices, more than six its a cell
    buffer.put(vertexCount == 5 ? FIVE_VERTEX_CELL : SIX_VERTEX_CELL); //Then a byte indicating how many vertices the cell has

    //Then the vertices a byte for the integer part before the decimal and 4 bytes for the fractional part
    List<Vertex> vertices = cell.getVertices();
    for (Vertex vertex : vertices) {
      buffer.putLong(vertex.getId().getMostSignificantBits());
      buffer.putLong(vertex.getId().getLeastSignificantBits());
    }
    buffer.flip();
    cellWriter.write(buffer);                                 //Each cell starts with a 128 bit uuid
    buffer.flip();
  }

  private void writeVertices(Cell cell, AtomicInteger savedVertexCount) throws IOException {
    List<Vertex> vertices = cell.getVertices();
    int persistentVertexCount = 0;
    for (Vertex vertex : vertices) {
      if (vertex.getShouldPersist()) {
        persistentVertexCount++;
      }
    }
    ByteBuffer buffer = ByteBuffer.allocateDirect(
        (persistentVertexCount * VERTEX_BYTE_SIZE) // The lat longs
    );

    for (Vertex vertex : vertices) {
      //Don't write duplicates
      if(vertex.getShouldPersist()) {
        buffer.putLong(vertex.getId().getMostSignificantBits());
        buffer.putLong(vertex.getId().getLeastSignificantBits());

        float latitude = vertex.getLatitude();
        buffer.putFloat(latitude);
        float longitude = vertex.getLongitude();
        buffer.putFloat(longitude);
        savedVertexCount.incrementAndGet();
      }
    }
    buffer.flip();
    vertexWriter.write(buffer);
    buffer.flip();
  }


  public String kmlString(int height, Cell cell) {
    List<Vertex> vertices = cell.getVertices();
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < vertices.size(); i++) {
      if(i > 0) {
        buff.append("\n");
      }
      buff.append(vertices.get(i).kmlString(height));
    }
    buff.append(vertices.get(0).kmlString(height));//Make the first one also last.
    return buff.toString();
  }



  private DataInputStream initializeReader() {
    try {
      return new DataInputStream(new BufferedInputStream(new FileInputStream(new File(this.CELLS_DIR, FILE_NAME))));
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void initializeWriters() {
    try {
      new File(this.CELLS_DIR.getAbsolutePath()).mkdirs();
      //RandomAccessFile raf = new RandomAccessFile(new File(this.CELLS_DIR, fileName), "rw");
      // OutputStream writer = new BufferedOutputStream());
      // Writer writer = new UTF8OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(raf.getFD()), 217447)));
      cellWriter = new RandomAccessFile(new File(this.CELLS_DIR, FILE_NAME), "rw").getChannel();
      vertexWriter = new RandomAccessFile(new File(this.CELLS_DIR, FILE_NAME), "rw").getChannel();
      cellWriter.position(this.vertexFileOffset);
      //return new FileOutputStream(new File(this.CELLS_DIR, fileName));
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void close() {
      try {
        cellWriter.close();
        vertexWriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
  }


  public Cell[] readCells() {
    DataInputStream in = initializeReader();
    int cellCount = 0;
    int totalVertexCount = 0;
    Cell[] cells = null;
    Map<UUID, Vertex> vertexMap = new HashMap();
    try {
      cellCount = in.readInt();
      totalVertexCount = in.readInt();
      cells = new Cell[cellCount];
      // Read all the vertexes
      for (int i = 0; i < totalVertexCount; i++) {
        long vertexUuidMSB = in.readLong();
        long vertexUuidLSB = in.readLong();
        UUID vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
        float latitude = in.readFloat();
        float longitude = in.readFloat();
        vertexMap.put(vertexId, new Vertex(vertexId, latitude, longitude));
      }

      for (int c=0; c < cellCount; c++) {
        long uuidMSB = in.readLong();
        long uuidLSB = in.readLong();
        UUID cellId = new UUID(uuidMSB, uuidLSB);
        int initiator = in.readByte();// 18% or 72%
        int vertexCount = in.readByte();
        Vertex[] vertices = new Vertex[vertexCount];
        //Read the vertex ids for the cell
        for (int i = 0; i < vertexCount; i++) {
          long vertexUuidMSB = in.readLong();
          long vertexUuidLSB = in.readLong();
          UUID vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
          vertices[i] = vertexMap.get(vertexId);
        }
        cells[c] = new Cell(cellId, Arrays.asList(vertices),  0, 0, 0);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return cells;
  }


  public void outputKML(CellSerializer serializer, Cell[] cells) throws IOException {
    String fileName = "~/rings.kml";
    File file = new File(fileName);
    System.out.println("Writing to file: " + file.getAbsolutePath());
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(file));
      String[] styles = new String[]{"transBluePoly", "transRedPoly", "transGreenPoly",
          "transYellowPoly"};
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
        writer.write("    <Placemark>\n" +
            "      <name>" + cells[i] + " " + cells[i].getArea() + "</name>\n" +
            //"      <styleUrl>#" + styles[i % styles.length] + "</styleUrl>\n" +
            "      <styleUrl>#" + styles[0] + "</styleUrl>\n" +
            getLineString(serializer, cells[i]) +
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
    return "      <LineString>\n" +
        "        <tesselate>1</tesselate>\n" +
        "        <altitudeMode>relativeToGround</altitudeMode>\n" +
        "        <coordinates>\n" +
        serializer.kmlString(2000, cell) + "\n" +
        "        </coordinates>\n" +
        "      </LineString>\n";
  }
  private String getPolygon(CellSerializer serializer, Cell cell) {
    return "      <Polygon>\n" +
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


}

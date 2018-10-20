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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class CellSerializer {

  private static final byte FIVE_VERTEX_CELL = 5;
  private static final byte SIX_VERTEX_CELL = 6;
  private static final String FILE_NAME_LONG = "cellMap.json";
  private static final String FILE_NAME_SHORT = "cells.json";
  public static final int VERTEX_BYTE_SIZE_LONG = (Long.BYTES + Long.BYTES + (2 * Float.BYTES));
  public static final int VERTEX_BYTE_SIZE_SHORT = 2 * Float.BYTES;//Short format is just the vertices in order
  private static final int VERTEX_AND_CELL_COUNT_SIZE = 8;

  private static ByteBuffer SIX_VERTEX_CELL_BUFFER_LONG = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          6 * (Long.BYTES + Long.BYTES)
  );

  private static ByteBuffer FIVE_VERTEX_CELL_BUFFER_LONG = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          5 * (Long.BYTES + Long.BYTES)
  );

  private static ByteBuffer SIX_VERTEX_CELL_BUFFER_SHORT = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          6 * (Integer.BYTES)
  );

  private static ByteBuffer FIVE_VERTEX_CELL_BUFFER_SHORT = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          5 * (Integer.BYTES)
  );

  public static final File CELLS_DIR = new File("/tmp/cells");
  private int vertexFileOffset;
  private AtomicInteger savedVertexCount;
  private AtomicInteger savedCellCount;
  private AtomicInteger builtProxyCount;
  FileChannel cellWriter = null;
  FileChannel vertexWriter = null;
  double eightyTwoPercent;
  //Long format uses vertex uuids short format uses vertex indexes
  boolean longFormat = false;

  public CellSerializer(int totalCellCount, int totalVertexCount, AtomicInteger savedCellCount, AtomicInteger savedVertexCount, AtomicInteger builtProxyCount) {
   this(totalCellCount, totalVertexCount, savedCellCount, savedVertexCount, builtProxyCount, true);
  }
  public CellSerializer(int totalCellCount, int totalVertexCount, AtomicInteger savedCellCount, AtomicInteger savedVertexCount, AtomicInteger builtProxyCount, boolean longFormat) {
    this.savedCellCount = savedCellCount;
    this.savedVertexCount = savedVertexCount;
    this.builtProxyCount = builtProxyCount;
    if (longFormat) {
      this.vertexFileOffset = totalVertexCount * VERTEX_BYTE_SIZE_LONG + VERTEX_AND_CELL_COUNT_SIZE;
    } else {
      this.vertexFileOffset = totalVertexCount * VERTEX_BYTE_SIZE_SHORT + VERTEX_AND_CELL_COUNT_SIZE;
    }
    this.longFormat = longFormat;
    this.eightyTwoPercent = totalCellCount * .82;
    initializeWriters();
    writeCounts(totalCellCount, totalVertexCount);

  }

  public CellSerializer(boolean longFormat) {
    this.longFormat = longFormat;
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

  public void writeCell(Cell cell, boolean writeVertices) {
    try {
      if (writeVertices) {
        writeVertices(cell);
      }
      String initiator = savedCellCount.get() > eightyTwoPercent ? initiatorKey18Percent : Sphere.initiatorKey82Percent;
      savedCellCount.incrementAndGet();
      writeCell(initiator, cell);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param initiator
   * @param cell
   * @throws IOException
   */
  private void writeCell(String initiator, Cell cell) throws IOException {
    long uuidMSB = cell.getId().getMostSignificantBits();
    long uuidLSB = cell.getId().getLeastSignificantBits();
    int vertexCount = cell.getVertices().length;
    ByteBuffer buffer;

    if(longFormat) {
      buffer = vertexCount == 5 ? FIVE_VERTEX_CELL_BUFFER_LONG : SIX_VERTEX_CELL_BUFFER_LONG;
    } else {
      buffer = vertexCount == 5 ? FIVE_VERTEX_CELL_BUFFER_SHORT : SIX_VERTEX_CELL_BUFFER_SHORT;
    }

    buffer.putLong(uuidMSB);
    buffer.putLong(uuidLSB);
    buffer.put((byte) (initiator.equals(initiatorKey18Percent) ? 1 : 0));//Then a byte 0 or 1 depending on which initiator it is
    // A byte here less than 6 indicates its a list of vertices, more than six its a cell
    buffer.put(vertexCount == 5 ? FIVE_VERTEX_CELL : SIX_VERTEX_CELL); //Then a byte indicating how many vertices the cell has

    //Then the vertices a byte for the integer part before the decimal and 4 bytes for the fractional part
    Vertex[] vertices = cell.getVertices();
    for (Vertex vertex : vertices) {
      if (longFormat) {
        buffer.putLong(vertex.getId().getMostSignificantBits());
        buffer.putLong(vertex.getId().getLeastSignificantBits());
      } else {
        //System.out.println("Putting int " + vertex.getIndex());
        buffer.putInt(vertex.getIndex());
      }
    }
    buffer.flip();
    cellWriter.write(buffer);                                 //Each cell starts with a 128 bit uuid
    buffer.flip();
  }

  private void writeVertices(Cell cell) throws IOException {
    Vertex[] vertices = cell.getVertices();
    List<Vertex> verticesToPersist = new ArrayList(vertices.length);
    for (Vertex vertex : vertices) {
      if (vertex.getShouldPersist()) {
        verticesToPersist.add(vertex);
      }
    }
    writeVertices(verticesToPersist);
  }

  private void writeVertices(List<Vertex> vertices) throws IOException {
    if (vertices.size() == 0) {
      return;
    }
    ByteBuffer buffer;
    if (longFormat) {
      buffer = ByteBuffer.allocateDirect(
          (vertices.size() * VERTEX_BYTE_SIZE_LONG) // The lat longs
      );
    } else {
      buffer = ByteBuffer.allocateDirect(
          (vertices.size() * VERTEX_BYTE_SIZE_SHORT) // The lat longs
      );
    }

    for (Vertex vertex : vertices) {
      //Don't write duplicates
      if(longFormat) {
        buffer.putLong(vertex.getId().getMostSignificantBits());
        buffer.putLong(vertex.getId().getLeastSignificantBits());
      }

      //System.out.println(savedVertexCount + " Putting vertex " + vertex);
      float latitude = vertex.getLatitude();
      buffer.putFloat(latitude);
      float longitude = vertex.getLongitude();
      buffer.putFloat(longitude);
      savedVertexCount.incrementAndGet();
    }
    buffer.flip();
    vertexWriter.write(buffer);
    buffer.flip();
  }


  public String kmlString(int height, Cell cell) {
    Vertex[] vertices = cell.getVertices();
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < vertices.length; i++) {
      if(i > 0) {
        buff.append("\n");
      }
      buff.append(vertices[i].kmlString(height));
    }
    buff.append(vertices[0].kmlString(height));//Make the first one also last.
    return buff.toString();
  }



  private DataInputStream initializeReader() {
    try {
      File file = new File(this.CELLS_DIR, longFormat ? FILE_NAME_LONG : FILE_NAME_SHORT);
      System.out.println("Reading cells from " + file.getAbsolutePath());
      return new DataInputStream(new BufferedInputStream(new FileInputStream( file)));
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
      File file1 = new File(this.CELLS_DIR, longFormat ? FILE_NAME_LONG : FILE_NAME_SHORT);
      System.out.println("Writing file to " + file1.getAbsolutePath());
      cellWriter = new RandomAccessFile(file1, "rw").getChannel();
      vertexWriter = new RandomAccessFile(new File(this.CELLS_DIR, longFormat ? FILE_NAME_LONG : FILE_NAME_SHORT), "rw").getChannel();
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

  public Map<Integer, Cell[]> readCells() {
    DataInputStream in = initializeReader();
    int cellCount = 0;
    int totalVertexCount = 0;
    Cell[] cells = null;
    Map cellsByVertexCount = new HashMap();
    // Preserve the order the vertices are read in so the indexes are correct
    Map<UUID, Vertex> vertexMap = new LinkedHashMap();// For the long format
    try {
      cellCount = in.readInt();
      totalVertexCount = in.readInt();
      Vertex[] orderedVertices = new Vertex[totalVertexCount];//For the short format
      cells = new Cell[cellCount];
      cellsByVertexCount.put(totalVertexCount, cells);
      // Read all the vertexes
      for (int i = 0; i < totalVertexCount; i++) {
        UUID vertexId = null;
        if (longFormat) {
          long vertexUuidMSB = in.readLong();
          long vertexUuidLSB = in.readLong();
          vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
        }
        float latitude = in.readFloat();
        float longitude = in.readFloat();
        if (longFormat) {
          // The uuids can actually just be generated.  See stableUUID
          vertexMap.put(vertexId, new Vertex(i, latitude, longitude));
        } else {
          orderedVertices[i] = new Vertex(i, latitude, longitude);
        }
        //System.out.println(i + " Getting vertex " + orderedVertices[i]);
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
          Vertex vertex;
          if (longFormat) {
            long vertexUuidMSB = in.readLong();
            long vertexUuidLSB = in.readLong();
            UUID vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
            vertex = vertexMap.get(vertexId);
          } else {
            int vertexIndex = in.readInt();
            //System.out.println("Reading int " + vertexIndex);
            vertex = orderedVertices[vertexIndex];
          }
          vertices[i] = vertex;
        }
        cells[c] = new Cell(cellId, vertices,  0, 0, 0);

        //System.out.println("R: " + cells[c]);
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

    return cellsByVertexCount;
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

  public static void main(String[] args) throws IOException {
    // Read cells from a long format file and write a short format file
    //Read from long file
    CellSerializer selfReader = new CellSerializer(true);
    Map<Integer, Cell[]> cellsByVertexCount = selfReader.readCells();
    Integer totalVertexCount = cellsByVertexCount.entrySet().iterator().next().getKey();
    Cell[] longFormatCells = cellsByVertexCount.entrySet().iterator().next().getValue();
    AtomicInteger savedVertexCount = new AtomicInteger();

    //Write to short file
    CellSerializer selfWriter = new CellSerializer(longFormatCells.length, totalVertexCount, new AtomicInteger(), savedVertexCount, new AtomicInteger(), false);
    int currentPersistentVertexIndex = 0;

    //System.out.println("\n");
    for (Cell longFormatCell : longFormatCells) {
      selfWriter.writeCell(longFormatCell, false);
      //The consecutive vertices should be persisted.  The non-consecutive ones were either already
      //persisted or will be later
      Vertex[] vertices = longFormatCell.getVertices();
      List<Vertex> verticesToPersist = new ArrayList(vertices.length);
      for(Vertex vertex : vertices) {
        int vertexIndex = vertex.getIndex();
        if (vertexIndex == currentPersistentVertexIndex) {
          currentPersistentVertexIndex++;
          verticesToPersist.add(vertex);
        }
      }
      //System.out.println("W: " + longFormatCell);
      selfWriter.writeVertices(verticesToPersist);
    }

    //Read from short file and create KML
    final boolean outputKML = Boolean.parseBoolean(System.getProperty("outputKML"));
    if (outputKML) {
      CellSerializer deSerializer = new CellSerializer(false);
      deSerializer.outputKML(deSerializer, deSerializer.readCells().entrySet().iterator().next().getValue());
    }
  }


}

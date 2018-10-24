package com.d4dl.permean.mesh;

import com.d4dl.permean.ProgressReporter;
import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

public class CellSerializer {

  private final String fileIn;
  private final String fileOut;
  public static final int VERTEX_BYTE_SIZE_LONG = (Long.BYTES + Long.BYTES + (2 * Float.BYTES));
  public static final int VERTEX_BYTE_SIZE_SHORT = 2 * Float.BYTES;//Short format is just the vertices in order
  private static final int VERTEX_AND_CELL_COUNT_SIZE = 8;
  private ProgressReporter reporter;

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

  private long vertexFileOffset;
  FileChannel cellWriter = null;
  FileChannel vertexWriter = null;
  //static double eightyTwoPercent;
  //Long format uses vertex uuids short format uses vertex indexes
  boolean longFormat = false;

  public CellSerializer(String fileIn, String fileOut, ProgressReporter reporter) {
   this(fileIn, fileOut, reporter, true);
  }
  public CellSerializer(String fileIn, String fileOut, ProgressReporter reporter, boolean longFormat) {
    this.fileIn = fileIn;
    this.fileOut = fileOut;
    this.reporter = reporter;
    this.longFormat = longFormat;

    //this.eightyTwoPercent = totalCellCount * .82;
  }

  public void setCountsAndStartWriting(int cellCount, int vertexCount) {
    if (longFormat) {
      this.vertexFileOffset = (long) vertexCount * (long) VERTEX_BYTE_SIZE_LONG + (long) VERTEX_AND_CELL_COUNT_SIZE;
    } else {
      this.vertexFileOffset = (long) vertexCount * (long) VERTEX_BYTE_SIZE_SHORT + (long) VERTEX_AND_CELL_COUNT_SIZE;
    }
    reporter.setCellCount(cellCount);
    reporter.setVertexCount(vertexCount);
    initializeWriters();
    writeCounts(cellCount, vertexCount);
  }

  private void writeCounts(int cellCount, int vertexCount) {
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(
          Integer.BYTES + Integer.BYTES
      );
      // Start with the number of cells
      putInt(cellCount, buffer);
      // Then the number of vertices
      putInt(vertexCount, buffer);

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
      reporter.incrementCellsWritten();
      writeCell(cell.getInitiator(), cell);
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

    putLong(uuidMSB, buffer);
    putLong(uuidLSB, buffer);
    byte initiatorByte = (byte) (initiator.equals(initiatorKey18Percent) ? 1 : 0);
    putByte(initiatorByte, buffer);//Then a byte 0 or 1 depending on which initiator it is
    // A byte here less than 6 indicates its a list of vertices, more than six its a cell
    putByte((byte) vertexCount, buffer); //Then a byte indicating how many vertices the cell has

    //Then the vertices a byte for the integer part before the decimal and 4 bytes for the fractional part
    Vertex[] vertices = cell.getVertices();
    for (Vertex vertex : vertices) {
      if (longFormat) {
        putLong(vertex.getId().getMostSignificantBits(), buffer);
        putLong(vertex.getId().getLeastSignificantBits(), buffer);
      } else {
        //System.out.println("Putting int " + vertex.getIndex());
        putInt(vertex.getIndex(), buffer);
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
        putLong(vertex.getId().getMostSignificantBits(), buffer);
        putLong(vertex.getId().getLeastSignificantBits(), buffer);
      }

      //System.out.println(savedVertexCount + " Putting vertex " + vertex);
      float latitude = vertex.getLatitude();
      putFloat(buffer, latitude);
      float longitude = vertex.getLongitude();
      putFloat(buffer, longitude);
      reporter.incrementVerticesWritten();
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
    buff.append("\n");
    buff.append(vertices[0].kmlString(height));//Make the first one also last.
    return buff.toString();
  }



  private DataInputStream initializeReader() {
    try {
      File file = new File(fileIn);
      System.out.println("Reading cells from " + file.getAbsolutePath());
      if(file.getName().endsWith(".gz")) {
        return new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))));
      } else {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void initializeWriters() {
    try {
      //RandomAccessFile raf = new RandomAccessFile(new File(this.CELLS_DIR, fileName), "rw");
      // OutputStream writer = new BufferedOutputStream());
      // Writer writer = new UTF8OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(raf.getFD()), 217447)));

      System.out.println("Writing file to " + new File(fileOut).getAbsolutePath());
      cellWriter = new RandomAccessFile(new File(fileOut), "rw").getChannel();
      vertexWriter = new RandomAccessFile(new File(fileOut), "rw").getChannel();
      cellWriter.position(this.vertexFileOffset);
      //return new FileOutputStream(new File(this.CELLS_DIR, fileName));
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void close() {
    try {
      if (cellWriter != null) {
        cellWriter.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (vertexWriter != null) {
        vertexWriter.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public Cell[] readCells() {
    return readCells(null);
  }
  /**
   *
   * @param writer write the cells out, otherwise return them
   * @return
   */
  public Cell[] readCells(CellSerializer writer) {
    int initiator82Count = 0;
    int initiator18Count = 0;
    int currentPersistentVertexIndex = 0;
    DataInputStream in = initializeReader();
    Cell[] cells = null;
    // Preserve the order the vertices are read in so the indexes are correct
    try {
      int cellCount = readInt(in);
      int totalVertexCount = readInt(in);
      if (writer != null) {
        writer.setCountsAndStartWriting(cellCount, totalVertexCount);
      }
      Map<UUID, Vertex> vertexMap = new HashMap(totalVertexCount);// For the long format
      Vertex[] orderedVertices = new Vertex[totalVertexCount];//For the short format
      cells = new Cell[cellCount];
      // Read all the vertexes
      for (int i = 0; i < totalVertexCount; i++) {
        UUID vertexId = null;
        if (longFormat) {
          long vertexUuidMSB = readLong(in);
          long vertexUuidLSB = readLong(in);
          vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
        }
        float latitude = readFloat(in);
        float longitude = readFloat(in);
        if (longFormat) {
          // The uuids can actually just be generated.  See stableUUID
          vertexMap.put(vertexId, new Vertex(i, latitude, longitude));
        } else {
          orderedVertices[i] = new Vertex(i, latitude, longitude);
        }
        //System.out.println(i + " Getting vertex " + orderedVertices[i]);
        reporter.incrementVerticesWritten();
      }
      System.out.println("Finished reading in vertices.  Now reading and populating cells.");
      if(writer != null && reporter != null) {
        reporter.reset();
      }

      for (int c=0; c < cellCount; c++) {
        long uuidMSB = readLong(in);
        long uuidLSB = readLong(in);
        UUID cellId = new UUID(uuidMSB, uuidLSB);
        int initiator = readByte(in);
        int vertexCount = readByte(in);
        Vertex[] vertices = new Vertex[vertexCount];
        //Read the vertex ids for the cell
        for (int i = 0; i < vertexCount; i++) {
          Vertex vertex;
          if (longFormat) {
            long vertexUuidMSB = readLong(in);
            long vertexUuidLSB = readLong(in);
            UUID vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
            vertex = vertexMap.get(vertexId);
            short accessCount = vertex.access();
            if(accessCount == 3) {
              vertexMap.remove(vertexId);
            }
          } else {
            int vertexIndex = readInt(in);
            //System.out.println("Reading int " + vertexIndex);
            vertex = orderedVertices[vertexIndex];
          }
          vertices[i] = vertex;
        }
        if (initiator == 0) {
          initiator82Count++;
        } else {
          initiator18Count++;
        }
        Cell cell = new Cell(initiator == 0 ? initiatorKey82Percent : initiatorKey18Percent, cellId, vertices, 0, 0, 0);
        if (writer != null) {
          currentPersistentVertexIndex = writer.writeCell(currentPersistentVertexIndex, cell);
        } else {
          cells[c] = cell;
        }
        reporter.incrementCellsWritten();

        //System.out.println("R: " + cells[c]);
      }
      System.out.println("18% " + initiator18Count + " 82% " + initiator82Count + " " + (initiator18Count / cellCount) + "%");
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


  public static void main(String[] args) throws IOException {
    ProgressReporter readReporter = null;
    CellSerializer selfWriter = null;
    CellSerializer selfReader = null;
    CellSerializer deSerializer = null;
    try {
      // Read cells from a long format file and write a short format file
      //Read from long file
      readReporter = new ProgressReporter("ReadLong" + 0, 0, 0, null);
      readReporter.start();
      selfReader = new CellSerializer(args[0], args[1], readReporter, true);

      //Write to short file
      readReporter.reset();
      selfWriter = new CellSerializer(args[0], args[1], readReporter, false);
      Cell[] longFormatCells = selfReader.readCells(selfWriter);
      int currentPersistentVertexIndex = 0;

      //System.out.println("\n");
      //for (Cell longFormatCell : longFormatCells) {
        //currentPersistentVertexIndex = selfWriter.writeCell(currentPersistentVertexIndex, longFormatCell);
      //}

      //Read from short file and create KML
      final boolean outputKML = Boolean.parseBoolean(System.getProperty("outputKML"));
      if (outputKML) {
        readReporter.reset();
        deSerializer = new CellSerializer(args[0], args[1], readReporter, false);
        new KMLWriter().outputKML(deSerializer, deSerializer.readCells());
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      readReporter.stop();
      if (selfReader != null) {
        selfReader.close();
      }
      if (selfWriter != null) {
        selfWriter.close();
      }
      if (deSerializer != null) {
        deSerializer.close();
      }
    }
  }

  private int writeCell(int currentPersistentVertexIndex, Cell cell) throws IOException {
    writeCell(cell, false);
    //The consecutive vertices should be persisted.  The non-consecutive ones were either already
    //persisted or will be later
    Vertex[] vertices = cell.getVertices();
    List<Vertex> verticesToPersist = new ArrayList(vertices.length);
    for(Vertex vertex : vertices) {
      int vertexIndex = vertex.getIndex();
      if (vertexIndex == currentPersistentVertexIndex) {
        currentPersistentVertexIndex++;
        verticesToPersist.add(vertex);
      }
    }
    //System.out.println("W: " + longFormatCell);
    writeVertices(verticesToPersist);
    return currentPersistentVertexIndex;
  }



  private ByteBuffer putByte(byte value, ByteBuffer buffer) {
    //System.out.println("OUT 8 " + value);
    return buffer.put(value);
  }

  private void putInt(int value, ByteBuffer buffer) {
    //System.out.println("OUT 32F  " + value);
    buffer.putInt(value);
  }

  private void putLong(long value, ByteBuffer buffer) {
    //System.out.println("OUT 64 " + value);
    buffer.putLong(value);
  }

  private void putFloat(ByteBuffer buffer, float value) {
    //System.out.println("OUT 32 " + value);
    buffer.putFloat(value);
  }


  private int readByte(DataInputStream in) throws IOException {
    int value = in.readByte();
    //System.out.println("IN 8 " + value);
    return value;
  }

  private int readInt(DataInputStream in) throws IOException {
    int value = in.readInt();
    //System.out.println("IN 32 " + value);
    return value;
  }

  private long readLong(DataInputStream in) throws IOException {
    long value = in.readLong();
    //System.out.println("IN 64 " + value);
    return value;
  }

  private float readFloat(DataInputStream in) throws IOException {
    float value = in.readFloat();
    //System.out.println("IN 32F " + value);
    return value;
  }

}

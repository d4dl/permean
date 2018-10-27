package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public abstract class CellWriter extends DataIO {

  protected String fileOut;
  protected FileChannel cellFileChannel = null;
  protected FileChannel vertexFileChannel = null;

  protected CellWriter(String reporterName, String fileOut) {
    super(reporterName);
    this.fileOut = fileOut;
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
  public void writeCell(String initiator, Cell cell) throws IOException {
    long uuidMSB = cell.getId().getMostSignificantBits();
    long uuidLSB = cell.getId().getLeastSignificantBits();
    int vertexCount = cell.getVertices().length;

    ByteBuffer buffer = getCellBuffer(vertexCount);

    putLong(uuidMSB, buffer);
    putLong(uuidLSB, buffer);
    byte initiatorByte = (byte) (initiator.equals(initiatorKey18Percent) ? 1 : 0);
    putByte(initiatorByte, buffer);//Then a byte 0 or 1 depending on which initiator it is
    // A byte here less than 6 indicates its a list of vertices, more than six its a cell
    putByte((byte) vertexCount, buffer); //Then a byte indicating how many vertices the cell has

    //Then the vertices a byte for the integer part before the decimal and 4 bytes for the fractional part
    Vertex[] vertices = cell.getVertices();
    for (Vertex vertex : vertices) {
      writeVertexRef(buffer, vertex);
    }
    buffer.flip();
    cellFileChannel.write(buffer);                                 //Each cell starts with a 128 bit uuid
    buffer.flip();
  }

  protected abstract ByteBuffer getCellBuffer(int vertexCount);

  protected abstract void writeVertexRef(ByteBuffer buffer, Vertex vertex);


  public void setCountsAndStartWriting(int cellCount, int vertexCount) {
    long vertexFileOffset = getVertexFileOffset(vertexCount);

    reporter.setCellCount(cellCount);
    reporter.setVertexCount(vertexCount);
    initializeWriters(vertexFileOffset);
    writeCounts(cellCount, vertexCount);
  }

  protected abstract long getVertexFileOffset(long vertexCount);


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
      vertexFileChannel.write(buffer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
    ByteBuffer buffer = getVertexBuffer(vertices.size());

    for (Vertex vertex : vertices) {
      writeVertex(buffer, vertex);
      reporter.incrementVerticesWritten();
    }
    buffer.flip();
    vertexFileChannel.write(buffer);
    buffer.flip();
  }

  protected abstract void writeVertex(ByteBuffer buffer, Vertex vertex);

  protected abstract ByteBuffer getVertexBuffer(int vertexCount);


  private void initializeWriters(long vertexFileOffset) {
    try {
      //RandomAccessFile raf = new RandomAccessFile(new File(this.CELLS_DIR, fileName), "rw");
      // OutputStream writer = new BufferedOutputStream());
      // Writer writer = new UTF8OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(raf.getFD()), 217447)));

      System.out.println("Writing file to " + new File(fileOut).getAbsolutePath());
      cellFileChannel = new RandomAccessFile(new File(fileOut), "rw").getChannel();
      vertexFileChannel = new RandomAccessFile(new File(fileOut), "rw").getChannel();
      cellFileChannel.position(vertexFileOffset);
      //return new FileOutputStream(new File(this.CELLS_DIR, fileName));
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }


  public int writeCell(int currentPersistentVertexIndex, Cell cell) throws IOException {
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

  protected ByteBuffer putByte(byte value, ByteBuffer buffer) {
    //System.out.println("OUT 8 " + value);
    return buffer.put(value);
  }

  protected void putInt(int value, ByteBuffer buffer) {
    //System.out.println("OUT 32F  " + value);
    buffer.putInt(value);
  }

  protected void putLong(long value, ByteBuffer buffer) {
    //System.out.println("OUT 64 " + value);
    buffer.putLong(value);
  }

  protected void putFloat(ByteBuffer buffer, float value) {
    //System.out.println("OUT 32 " + value);
    buffer.putFloat(value);
  }

  public void close() {
    try {
      if (cellFileChannel != null) {
        cellFileChannel.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (vertexFileChannel != null) {
        vertexFileChannel.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    super.close();
  }
}

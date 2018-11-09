package com.d4dl.permean.io;

import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import com.d4dl.permean.mesh.ProgressReporter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCellWriter extends DataIO implements CellWriter {

  protected String fileOut;
  protected FileChannel cellFileChannel = null;
  protected FileChannel vertexFileChannel = null;
  private final SizeManager sizeManager = new SizeManager();
  private final CellBufferBuilder cellBufferBuilder = new CellBufferBuilder();

  protected AbstractCellWriter(String reporterName, String fileOut) {
    super(reporterName);
    this.fileOut = fileOut;
  }

  public AbstractCellWriter(ProgressReporter reporter, String fileName) {
    super(reporter);
    this.fileOut = fileName;
  }

  public void writeCell(MeshCell cell, boolean writeVertices) {
    try {
      if (writeVertices) {
        writeVertices(cell);
      }
      incrementCellsWritten();
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
  public void writeCell(String initiator, MeshCell cell) throws IOException {
    ByteBuffer buffer = cellBufferBuilder.fillCellBuffer(cell);
    buffer.flip();
    cellFileChannel.write(buffer);                                 //Each cell starts with a 128 bit uuid
    buffer.flip();
  }


  public void setCountsAndStartWriting(int cellCount, int vertexCount) {
    long vertexFileOffset = sizeManager.getVertexFileOffset(vertexCount);

    setCellCount(cellCount);
    setVertexCount(vertexCount);
    initializeWriters(vertexFileOffset);
    writeCounts(cellCount, vertexCount);
  }



  private void writeCounts(int cellCount, int vertexCount) {
    try {
      ByteBuffer buffer = ByteBuffer.allocateDirect(
          Integer.BYTES + Integer.BYTES
      );
      // Start with the number of cells
      cellBufferBuilder.putInt(cellCount, buffer);
      // Then the number of vertices
      cellBufferBuilder.putInt(vertexCount, buffer);

      buffer.flip();
      vertexFileChannel.write(buffer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void writeVertices(MeshCell cell) {
    MeshVertex[] vertices = cell.getVertices();
    List<MeshVertex> verticesToPersist = new ArrayList(vertices.length);
    for (MeshVertex vertex : vertices) {
      if (vertex.getShouldPersist()) {
        verticesToPersist.add(vertex);
      }
    }
    writeVertices(verticesToPersist);
  }

  private void writeVertices(List<MeshVertex> vertices) {
    if (vertices.size() == 0) {
      return;
    }
    ByteBuffer buffer = cellBufferBuilder.fillVertexBuffer(vertices, false);

    incrementVerticesWritten(vertices.size());
    buffer.flip();
    try {
      vertexFileChannel.write(buffer);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    buffer.flip();
  }


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


  public int writeCell(int currentPersistentVertexIndex, MeshCell cell) {
    writeCell(cell, false);
    //The consecutive vertices should be persisted.  The non-consecutive ones were either already
    //persisted or will be later
    MeshVertex[] vertices = cell.getVertices();
    List<MeshVertex> verticesToPersist = new ArrayList(vertices.length);
    for(MeshVertex vertex : vertices) {
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

package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class ShortFormatMappedCellReader extends CellReader {

  public static final int LAT_LNG_SIZE = 2 * Float.BYTES;
  protected FileChannel cellFileChannel = null;
  protected FileChannel vertexFileChannel = null;
  protected long vertexFileOffset;
  private ByteBuffer cellBuffer = ByteBuffer.allocateDirect(2 * Long.BYTES + 2 * Byte.BYTES);
  private ByteBuffer latLngBuffer = ByteBuffer.allocateDirect(LAT_LNG_SIZE);
  private ByteBuffer indexBuffer = ByteBuffer.allocateDirect(Integer.BYTES);
  private ByteBuffer countBuffer = ByteBuffer.allocateDirect(Integer.BYTES);

  public ShortFormatMappedCellReader(String reporterName, String fileName) {
    super(reporterName, fileName);
    initializeChannels(fileName);
  }

  @Override
  protected Cell nextCell() throws IOException {
    cellFileChannel.read(cellBuffer);
    long uuidMSB = cellBuffer.getLong(0);
    long uuidLSB = cellBuffer.getLong(1);
    UUID cellId = new UUID(uuidMSB, uuidLSB);
    int initiator = cellBuffer.get(16);
    int vertexCount = cellBuffer.get(17);
    cellBuffer.flip();
    Vertex[] vertices = new Vertex[vertexCount];
    //Read the vertex ids for the cell
    for (int i = 0; i < vertexCount; i++) {
      vertices[i] = nextVertex();
    }

    if (initiator == 0) {
      initiator82Count++;
    } else {
      initiator18Count++;
    }
    return new Cell(initiator == 0 ? initiatorKey82Percent : initiatorKey18Percent, cellId, vertices, 0, 0, 0);
  }

  @Override
  protected int readCellCount() throws IOException {
    return readCount();
  }

  private int readCount() throws IOException {
    vertexFileChannel.read(countBuffer);
    int count = countBuffer.getInt(0);
    countBuffer.flip();
    return count;
  }

  @Override
  protected int readVertexCount() throws IOException {
    return readCount();
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

  @Override
  protected Vertex nextVertex() throws IOException {
    cellFileChannel.read(indexBuffer);
    int vertexIndex = indexBuffer.getInt(0);
    indexBuffer.flip();
    vertexFileChannel.position(vertexIndex * LAT_LNG_SIZE + VERTEX_AND_CELL_COUNT_SIZE);
    vertexFileChannel.read(latLngBuffer);
    float latitude = latLngBuffer.getFloat(0);
    float longitude = latLngBuffer.getFloat(4);
    Vertex vertex = new Vertex(null, vertexIndex, latitude, longitude);
    latLngBuffer.flip();
    return vertex;
  }


  private void initializeChannels(String fileOut) {
    try {
      System.out.println("Writing file to " + new File(fileOut).getAbsolutePath());
      cellFileChannel = new RandomAccessFile(new File(fileOut), "rw").getChannel();
      vertexFileChannel = new RandomAccessFile(new File(fileOut), "rw").getChannel();
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void initializeVertices(int vertexCount) throws IOException {
    //Do nothing as this will just read vertexes from the file at the specified index
    vertexFileOffset = vertexCount * (long) VERTEX_BYTE_SIZE_SHORT + (long) VERTEX_AND_CELL_COUNT_SIZE;
    cellFileChannel.position(vertexFileOffset);
  }
}

package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

import com.d4dl.permean.data.Vertex;
import com.d4dl.permean.mesh.MeshCell;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class ShortFormatMappedCellReader extends CellReader {

  //Used to determine which buffer to look cells up from.
  private static final int CELL_BUFFER_LIMIT = 2261;

  public static final int LAT_LNG_SIZE = 2 * Float.BYTES;
  // protected FileChannel cellFileChannel = null;
  protected FileChannel allCellFileChannel = null;
  protected FileChannel vertexFileChannel = null;
  protected long vertexFileOffset;
  private ByteBuffer[] cellData;
  // private ByteBuffer cellBuffer = ByteBuffer.allocateDirect(2 * Long.BYTES + 2 * Byte.BYTES);
  private ByteBuffer latLngBuffer = ByteBuffer.allocateDirect(LAT_LNG_SIZE);
  private ByteBuffer indexBuffer6 = ByteBuffer.allocateDirect(6 * Integer.BYTES);
  private ByteBuffer indexBuffer5 = ByteBuffer.allocateDirect(5 * Integer.BYTES);
  private ByteBuffer countBuffer = ByteBuffer.allocateDirect(Integer.BYTES);
  private int cellBufferOffset = 0;
  private ByteBuffer lastBuffer = null;

  public ShortFormatMappedCellReader(String reporterName, String fileName) {
    super(reporterName, fileName);
    initializeChannels(fileName);
  }

  @Override
  protected MeshCell nextCell(int cellIndex) throws IOException {
    int bufferIndex = cellIndex / CELL_BUFFER_LIMIT;
    ByteBuffer cellBuffer = cellData[bufferIndex];
    if (cellBuffer != lastBuffer) {
      cellBufferOffset = 0;
      lastBuffer = cellBuffer;
    }
    cellBuffer.position(cellBufferOffset);
    //cellFileChannel.read(cellBuffer);
    long uuidMSB = cellBuffer.getLong(cellBufferOffset);
    long uuidLSB = cellBuffer.getLong(cellBufferOffset + 1);
    UUID cellId = new UUID(uuidMSB, uuidLSB);
    int initiator = cellBuffer.get(16);
    int vertexCount = cellBuffer.get(17);
    int[] vertices = getVertexIndices(cellBuffer, cellBufferOffset + 18, vertexCount);
    cellBufferOffset += getCellFileSize(vertexCount);

    if (initiator == 0) {
      initiator82Count++;
    } else {
      initiator18Count++;
    }
    return new FileMappedCell(cellId, initiator == 0 ? initiatorKey82Percent : initiatorKey18Percent, vertices, vertexFileChannel);
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
      if (allCellFileChannel != null) {
        allCellFileChannel.close();
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

  protected int[] getVertexIndices(ByteBuffer byteBuffer, int offset, int vertexCount) throws IOException {
    int vertexIndexes[] = new int[vertexCount];
    for (int i=0; i < vertexCount; i++) {
      vertexIndexes[i] = byteBuffer.getInt(offset + i * 4);
    }
    return vertexIndexes;
  }

  /**
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
    return null;
  }
   **/


  private void initializeChannels(String fileOut) {
    try {
      System.out.println("Writing file to " + new File(fileOut).getAbsolutePath());
      allCellFileChannel = new RandomAccessFile(new File(fileOut), "rw").getChannel();
      vertexFileChannel = new RandomAccessFile(new File(fileOut), "rw").getChannel();
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void initializeVertices(int cellCount, int vertexCount) throws IOException {
    //Do nothing as this will just read vertexes from the file at the specified index
    vertexFileOffset = vertexCount * (long) VERTEX_BYTE_SIZE_SHORT + (long) VERTEX_AND_CELL_COUNT_SIZE;
    allCellFileChannel.position(vertexFileOffset);
    int bufferCount = cellCount / CELL_BUFFER_LIMIT + 1;
    cellData = new ByteBuffer[bufferCount];
    for (int i=0; i < bufferCount; i++) {
      int bufferCellCount;
      if (i < bufferCount - 1) {
        bufferCellCount = CELL_BUFFER_LIMIT;
      } else {
        bufferCellCount = cellCount % CELL_BUFFER_LIMIT;
      }
      int cellsFileSize = getCellsFileSize(bufferCellCount);
      cellData[i] = ByteBuffer.allocateDirect(cellsFileSize);
    }
    allCellFileChannel.read(cellData);

    for (int i=0; i < cellData.length; i++) {
      cellData[i].flip();
    }

    /**
    if(cellCount > CELL_BUFFER_LIMIT) {
      int buffer1Size =   getCellsFileSize(CELL_BUFFER_LIMIT);
      cellData1 = ByteBuffer.allocateDirect(buffer1Size);
      int buffer2Size =   getCellsFileSize(cellCount - CELL_BUFFER_LIMIT);
      cellData2 = ByteBuffer.allocateDirect(buffer2Size);
      allCellFileChannel.read(cellData1);
      allCellFileChannel.read(cellData2);
    } else {
      int bufferSize =   getCellsFileSize(cellCount);
      cellData1 = ByteBuffer.allocateDirect(bufferSize);
      allCellFileChannel.read(cellData1);
    }
     **/
  }

  public int getCellFileSize(int vertexCount) {
    int idSize = Long.BYTES + Long.BYTES;
    int initiatorAndCellCountSize = Byte.BYTES + Byte.BYTES;
    int cellSize = idSize + getVertexRefFileSize(vertexCount) + initiatorAndCellCountSize;

    return cellSize;
  }

  public int getCellsFileSize(int cellCount) {
    int pentagonsSize = 12 * getCellFileSize(5);
    int hexagonsSize = (cellCount - 12) * getCellFileSize(6);
    return pentagonsSize + hexagonsSize;
  }

  private int getVertexRefFileSize(int vertexCount) {
    return Integer.BYTES * vertexCount;
  }
}

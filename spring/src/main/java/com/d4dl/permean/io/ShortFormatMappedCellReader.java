package com.d4dl.permean.io;

import static com.d4dl.permean.io.SizeManager.LAT_LNG_SIZE;
import static com.d4dl.permean.mesh.CellGenerator.EUCLID_DETERMINED_PENTAGONS_IN_A_TRUNCATED_ICOSOHEDRON;
import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

import com.d4dl.permean.mesh.MeshCell;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

/**
 * Using this class requires that the first 12 pentagons are the first 12 cells in the cell section of the file.
 */
public class ShortFormatMappedCellReader extends CellReader {

  //Used to determine which buffer to look cells up from.
  private static final int CELL_BUFFER_LIMIT = 51121212;
  //private static final int CELL_BUFFER_LIMIT = 2000;
  private final SizeManager sizeManager = new SizeManager();


  // protected FileChannel cellFileChannel = null;
  protected FileChannel allCellFileChannel = null;
  protected FileChannel vertexFileChannel = null;
  protected long vertexFileOffset;
  private ByteBuffer[] cellData;
  // private ByteBuffer cellBuffer = ByteBuffer.allocateDirect(2 * Long.BYTES + 2 * Byte.BYTES);
  private ByteBuffer latLngBuffer = ByteBuffer.allocateDirect(LAT_LNG_SIZE);
  private ByteBuffer indexBuffer6 = ByteBuffer.allocateDirect(6 * Integer.BYTES);
  private ByteBuffer indexBuffer5 = ByteBuffer.allocateDirect(5 * Integer.BYTES);
  //Pentagons are first so they get their own buffer.
  private ByteBuffer pentagonBuffer = ByteBuffer.allocateDirect(sizeManager.getCellFileSize(5) * EUCLID_DETERMINED_PENTAGONS_IN_A_TRUNCATED_ICOSOHEDRON);
  private ByteBuffer countBuffer = ByteBuffer.allocateDirect(Integer.BYTES);
  private int cellBufferOffset = 0;
  private int currentBufferIndex;

  public ShortFormatMappedCellReader(String reporterName, String fileName) {
    super(reporterName, fileName);
    initializeChannels(fileName);
  }

  @Override
  protected MeshCell nextCell(int cellIndex) throws IOException {
    ByteBuffer cellBuffer = cellData[currentBufferIndex];
    if (cellBufferOffset == cellBuffer.limit()) {
      cellBufferOffset = 0;
      try {
          currentBufferIndex++;
          cellBuffer = cellData[currentBufferIndex];
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    long uuidMSB = 0;
    try {
      uuidMSB = cellBuffer.getLong(cellBufferOffset);
    } catch (Exception e) {
      e.printStackTrace();
    }
    long uuidLSB = cellBuffer.getLong(cellBufferOffset + 1);
    UUID cellId = new UUID(uuidMSB, uuidLSB);
    int initiator = cellBuffer.get(16);
    int vertexCount = cellBuffer.get(17);
    int[] vertices = getVertexIndices(cellBuffer, cellBufferOffset + 18, vertexCount);
    cellBufferOffset += sizeManager.getCellFileSize(vertexCount);

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
    int hexagonCount = cellCount - EUCLID_DETERMINED_PENTAGONS_IN_A_TRUNCATED_ICOSOHEDRON;
    int hexBufferCount = hexagonCount / CELL_BUFFER_LIMIT + 1;
    cellData = new ByteBuffer[hexBufferCount + 1];
    cellData[0] = pentagonBuffer;//Pentagons are first
    for (int i=1; i <= hexBufferCount; i++) {
      int hexBufferCellCount;
      if (i < hexBufferCount) {
        hexBufferCellCount = CELL_BUFFER_LIMIT;
      } else {
        hexBufferCellCount = hexagonCount % CELL_BUFFER_LIMIT;
      }
      int cellsFileSize = sizeManager.getHexagoncellsFileSize(hexBufferCellCount);
      cellData[i] = ByteBuffer.allocateDirect(cellsFileSize);
    }
    allCellFileChannel.read(cellData);

    for (int i=0; i < cellData.length; i++) {
      if(cellData[i].position() != 0) {
        cellData[i].flip();
      }
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

}

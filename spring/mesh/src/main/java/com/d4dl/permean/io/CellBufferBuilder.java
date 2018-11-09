package com.d4dl.permean.io;

import static com.d4dl.permean.io.DataIO.FIVE_VERTEX_CELL_BUFFER_SHORT;
import static com.d4dl.permean.io.DataIO.FIVE_VERTEX_CELL_BUFFER_SHORT_WITH_CENTER;
import static com.d4dl.permean.io.DataIO.SIX_VERTEX_CELL_BUFFER_SHORT;
import static com.d4dl.permean.io.DataIO.SIX_VERTEX_CELL_BUFFER_SHORT_WITH_CENTER;
import static com.d4dl.permean.io.DataIO.VERTEX_BYTE_SIZE_SHORT;
import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;

import com.d4dl.permean.mesh.DefaultCell;
import com.d4dl.permean.mesh.DefaultVertex;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class CellBufferBuilder {

  public ByteBuffer fillCellBuffer(MeshCell cell) {
    return fillCellBuffer(cell, false);
  }

  public ByteBuffer fillCellBuffer(MeshCell cell, boolean writeCenter) {
    String initiator = cell.getInitiator();
    long uuidMSB = cell.getId().getMostSignificantBits();
    long uuidLSB = cell.getId().getLeastSignificantBits();
    int vertexCount = cell.getVertices().length;

    ByteBuffer buffer = getCellBuffer(vertexCount, writeCenter);

    putLong(uuidMSB, buffer);
    putLong(uuidLSB, buffer);
    if(writeCenter) {
      putFloat(buffer, cell.getCenterLatitude());
      putFloat(buffer, cell.getCenterLongitude());
    }
    byte initiatorByte = (byte) (initiator.equals(initiatorKey18Percent) ? 1 : 0);
    putByte(initiatorByte, buffer);//Then a byte 0 or 1 depending on which initiator it is
    // A byte here less than 6 indicates its a list of vertices, more than six its a cell
    putByte((byte) vertexCount, buffer); //Then a byte indicating how many vertices the cell has

    //Then the vertices a byte for the integer part before the decimal and 4 bytes for the fractional part
    MeshVertex[] vertices = cell.getVertices();
    for (MeshVertex vertex : vertices) {
      writeVertexRef(buffer, vertex);
    }
    buffer.flip();
    return buffer;
  }

  public static void main (String[] args) {
    DefaultVertex vertex = new DefaultVertex(1, 1.1f, 23.0f);
    DefaultVertex[] vertices = {
        vertex
    };
    MeshCell cell = new DefaultCell("blah", UUID.fromString("16aad503-2406-4297-aad5-0324064297bb"), vertices, 2, 88, 99);
    ByteBuffer cellBuffer = new CellBufferBuilder().fillCellBuffer(cell, true);
    byte[] cells = new byte[cellBuffer.limit()];
    cellBuffer.get(cells);
    System.out.println("cell\n[" + Base64.getEncoder().encodeToString(cells) + "]");

    ByteBuffer vertexBuffer = new CellBufferBuilder().fillVertex(vertex, true);
    byte[] vertexBytes = new byte[vertexBuffer.limit()];
    vertexBuffer.get(vertexBytes);
    String encodedVertex = Base64.getEncoder().encodeToString(vertexBytes);
    System.out.println("vertex\n[" + encodedVertex + "]");
  }

  public ByteBuffer fillVertex(MeshVertex vertex, boolean withIndex) {
    ByteBuffer vertexBuffer = getVertexBuffer(1, withIndex);
    writeVertex(vertexBuffer, vertex, withIndex);//8 bytes
    vertexBuffer.flip();
    return vertexBuffer;
  }

  public ByteBuffer fillVertexBuffer(List<MeshVertex> vertices, boolean withIndex) {
    ByteBuffer buffer = getVertexBuffer(vertices.size(), withIndex);

    for (MeshVertex vertex : vertices) {
      writeVertex(buffer, vertex, withIndex);
    }
    return buffer;
  }

  protected void writeVertex(ByteBuffer buffer, MeshVertex vertex, boolean withIndex) {
    if(withIndex) {
      buffer.putInt(vertex.getIndex());
    }
    float latitude = vertex.getLatitude();
    putFloat(buffer, latitude);
    float longitude = vertex.getLongitude();
    putFloat(buffer, longitude);
  }

  protected void writeVertexRef(ByteBuffer buffer, MeshVertex vertex) {
    putInt(vertex.getIndex(), buffer);
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


  protected ByteBuffer getCellBuffer(int vertexCount) {
    return getCellBuffer(vertexCount, false);
  }

  protected ByteBuffer getCellBuffer(int vertexCount, boolean withCenter) {
    int bufferSize;
    if (withCenter) {
      bufferSize = vertexCount == 5 ? FIVE_VERTEX_CELL_BUFFER_SHORT_WITH_CENTER : SIX_VERTEX_CELL_BUFFER_SHORT_WITH_CENTER;
    } else {
      bufferSize = vertexCount == 5 ? FIVE_VERTEX_CELL_BUFFER_SHORT : SIX_VERTEX_CELL_BUFFER_SHORT;
    }
    return ByteBuffer.allocateDirect(bufferSize);
  }


  protected ByteBuffer getVertexBuffer(int vertexCount, boolean withIndex) {
    int vertexSize = VERTEX_BYTE_SIZE_SHORT;
    if (withIndex) {
      vertexSize += Integer.BYTES;
    }
    return ByteBuffer.allocateDirect((vertexCount * vertexSize) // The lat longs
    );
  }


  public static class GetCellData {

    private final int cellBufferOffset;
    private ByteBuffer cellBuffer;
    private float centerLatitude;
    private float centerLongitude;
    private UUID cellId;
    private int initiator;
    private int[] vertexIndices;

    public GetCellData(ByteBuffer cellBuffer, int bufferOffset) {
      this.cellBuffer = cellBuffer;
      this.cellBufferOffset = bufferOffset;
    }

    public UUID getCellId() {
      return cellId;
    }

    public int getInitiator() {
      return initiator;
    }

    public int[] getVertexIndices() {
      return vertexIndices;
    }

    public GetCellData invoke() throws IOException {
      return invoke(false);
    }

    public GetCellData invoke(boolean withCenter) {
      long uuidMSB = cellBuffer.getLong(cellBufferOffset);
      long uuidLSB = cellBuffer.getLong(cellBufferOffset + 1);
      cellId = new UUID(uuidMSB, uuidLSB);

      if (withCenter) {
        centerLatitude = cellBuffer.getFloat(16);
        centerLongitude = cellBuffer.getFloat(20);
        initiator = cellBuffer.get(24);
        int vertexCount = cellBuffer.get(25);
        vertexIndices = getVertexIndices(cellBuffer, cellBufferOffset + 26, vertexCount);
      } else {
        initiator = cellBuffer.get(16);
        int vertexCount = cellBuffer.get(17);
        vertexIndices = getVertexIndices(cellBuffer, cellBufferOffset + 18, vertexCount);
      }
      return this;
    }


    public int[] getVertexIndices(ByteBuffer byteBuffer, int offset, int vertexCount) {
      int vertexIndexes[] = new int[vertexCount];
      for (int i=0; i < vertexCount; i++) {
        vertexIndexes[i] = byteBuffer.getInt(offset + i * 4);
      }
      return vertexIndexes;
    }

    public float getCenterLatitude() {
      return centerLatitude;
    }
    public float getCenterLongitude() {
      return centerLongitude;
    }
  }
}

package com.d4dl.permean.io;

import com.d4dl.permean.data.Vertex;
import java.nio.ByteBuffer;

public class LongFormatCellFileWriter extends CellWriter {

  public LongFormatCellFileWriter(String reporterName, String fileOut) {
    super(reporterName, fileOut);
  }

  protected void writeVertex(ByteBuffer buffer, Vertex vertex) {
    writeVertexRef(buffer, vertex);
    //System.out.println(savedVertexCount + " Putting vertex " + vertex);
    float latitude = vertex.getLatitude();
    putFloat(buffer, latitude);
    float longitude = vertex.getLongitude();
    putFloat(buffer, longitude);
  }

  @Override
  protected void writeVertexRef(ByteBuffer buffer, Vertex vertex) {
    putLong(vertex.getId().getMostSignificantBits(), buffer);
    putLong(vertex.getId().getLeastSignificantBits(), buffer);
  }

  protected ByteBuffer getCellBuffer(int vertexCount) {
    ByteBuffer buffer;
    buffer = vertexCount == 5 ? FIVE_VERTEX_CELL_BUFFER_LONG : SIX_VERTEX_CELL_BUFFER_LONG;
    return buffer;
  }

  protected long getVertexFileOffset(long vertexCount) {
    long vertexFileOffset;
    vertexFileOffset = vertexCount * (long) VERTEX_BYTE_SIZE_LONG + (long) VERTEX_AND_CELL_COUNT_SIZE;
    return vertexFileOffset;
  }

  @Override
  protected ByteBuffer getVertexBuffer(int size) {
    return ByteBuffer.allocateDirect((size * VERTEX_BYTE_SIZE_LONG) // The lat longs
    );
  }
}

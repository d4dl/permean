package com.d4dl.permean.io;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.mesh.ProgressReporter;
import com.d4dl.permean.data.Vertex;
import java.nio.ByteBuffer;

public class ShortFormatCellWriter extends AbstractCellWriter {

  public ShortFormatCellWriter(ProgressReporter reporter, String fileName) {
    super(reporter, fileName);
  }

  @Override
  protected void writeVertex(ByteBuffer buffer, Vertex vertex) {
    float latitude = vertex.getLatitude();
    putFloat(buffer, latitude);
    float longitude = vertex.getLongitude();
    putFloat(buffer, longitude);
  }

  @Override
  protected void writeVertexRef(ByteBuffer buffer, Vertex vertex) {
    putInt(vertex.getIndex(), buffer);
  }

  protected ByteBuffer getCellBuffer(int vertexCount) {
    ByteBuffer buffer;
    buffer = vertexCount == 5 ? FIVE_VERTEX_CELL_BUFFER_SHORT : SIX_VERTEX_CELL_BUFFER_SHORT;
    return buffer;
  }


  @Override
  protected ByteBuffer getVertexBuffer(int vertexCount) {
    return ByteBuffer.allocateDirect((vertexCount * VERTEX_BYTE_SIZE_SHORT) // The lat longs
    );
  }

}

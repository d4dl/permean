package com.d4dl.permean.io;

import com.d4dl.permean.data.Vertex;
import java.io.IOException;
import java.util.UUID;

public class ShortFormatCellReader extends CellReader {
  Vertex[] orderedVertices;

  public ShortFormatCellReader(String reporterName, String fileName) {
    super(reporterName, fileName);
  }

  @Override
  protected Vertex nextVertex() throws IOException {
    int vertexIndex = readInt();
    //System.out.println("Reading int " + vertexIndex);
    return orderedVertices[vertexIndex];
  }

  @Override
  protected void initializeVertices(int vertexCount) throws IOException {
    orderedVertices = new Vertex[vertexCount];//For the short format

    // Read all the vertexes
    for (int i = 0; i < vertexCount; i++) {
      UUID vertexId = null;
      float latitude = readFloat();
      float longitude = readFloat();
      orderedVertices[i] = new Vertex(i, latitude, longitude);
      //System.out.println(i + " Getting vertex " + orderedVertices[i]);
      reporter.incrementVerticesWritten();
    }
  }
}

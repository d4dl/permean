package com.d4dl.permean.io;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class LongFormatCellFileReader extends CellReader {
  Map<UUID, Vertex> vertexMap;

  public LongFormatCellFileReader(String reporterName, String fileName) {
    super(reporterName, fileName);
  }

  @Override
  protected void initializeVertices(int totalVertexCount) throws IOException {
    vertexMap = new HashMap(totalVertexCount);// For the long format
    // Read all the vertexes
    for (int i = 0; i < totalVertexCount; i++) {
      UUID vertexId = null;
      long vertexUuidMSB = readLong();
      long vertexUuidLSB = readLong();
      vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
      float latitude = readFloat();
      float longitude = readFloat();
      // The uuids can actually just be generated.  See stableUUID
      vertexMap.put(vertexId, new Vertex(i, latitude, longitude));
      //System.out.println(i + " Getting vertex " + orderedVertices[i]);
      reporter.incrementVerticesWritten();
    }
  }


  @NotNull
  protected Vertex nextVertex() throws IOException {
    Vertex vertex;
    long vertexUuidMSB = readLong();
    long vertexUuidLSB = readLong();
    UUID vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
    vertex = vertexMap.get(vertexId);
    short accessCount = vertex.access();
    if (accessCount == 3) {
      vertexMap.remove(vertexId);
    }
    return vertex;
  }
}

package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.jetbrains.annotations.NotNull;

public class LongFormatCellFileReader extends DataInputCellReader {
  Map<UUID, Vertex> vertexMap;

  public LongFormatCellFileReader(String reporterName, String fileName) {
    super(reporterName, fileName);
  }

  @Override
  protected void initializeVertices(int totalVertexCount) throws IOException {
    vertexMap = ChronicleMapBuilder.of(UUID.class, Vertex.class)
        .entries(totalVertexCount) //the maximum number of entries for the map
        .averageValue(new Vertex(99, 99, 99f))
        .averageKey(UUID.randomUUID())
        .createPersistedTo(new File("/tmp/cellsTmp.map"));

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
      incrementVerticesWritten();
    }
  }

  @Override
  protected Cell nextCell() throws IOException {
    long uuidMSB = readLong();
    long uuidLSB = readLong();
    UUID cellId = new UUID(uuidMSB, uuidLSB);
    int initiator = readByte();
    int vertexCount = readByte();
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
    Cell cell = new Cell(initiator == 0 ? initiatorKey82Percent : initiatorKey18Percent, cellId, vertices, 0, 0, 0);
    return cell;
  }

  @Override
  protected int readCellCount() throws IOException {
    return readInt();
  }

  @Override
  protected int readVertexCount() throws IOException {
    return readInt();
  }

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

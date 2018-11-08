package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

import com.d4dl.permean.mesh.DefaultCell;
import com.d4dl.permean.mesh.DefaultVertex;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import java.io.IOException;
import java.util.UUID;

public class ShortFormatCellReader extends DataInputCellReader {
  MeshVertex[] orderedVertices;

  public ShortFormatCellReader(String reporterName, String fileName) {
    super(reporterName, fileName);
  }

  //@Override
  protected MeshVertex nextVertex() throws IOException {
    int vertexIndex = readInt();
    //System.out.println("Reading int " + vertexIndex);
    return orderedVertices[vertexIndex];
  }

  @Override
  protected void initializeVertices(int cellCount, int vertexCount) throws IOException {
    orderedVertices = new MeshVertex[vertexCount];//For the short format

    // Read all the vertexes
    for (int i = 0; i < vertexCount; i++) {
      float latitude = readFloat();
      float longitude = readFloat();
      orderedVertices[i] = new DefaultVertex((UUID)null, i, latitude, longitude);
      //System.out.println(i + " Getting vertex " + orderedVertices[i]);
      incrementVerticesWritten();
    }
  }


  @Override
  protected MeshCell nextCell(int cellIndex) throws IOException {
    long uuidMSB = readLong();
    long uuidLSB = readLong();
    UUID cellId = new UUID(uuidMSB, uuidLSB);
    int initiator = readByte();
    int vertexCount = readByte();
    MeshVertex[] vertices = new MeshVertex[vertexCount];
    //Read the vertex ids for the cell
    for (int i = 0; i < vertexCount; i++) {
      vertices[i] = nextVertex();
    }

    if (initiator == 0) {
      initiator82Count++;
    } else {
      initiator18Count++;
    }
    return new DefaultCell(initiator == 0 ? initiatorKey82Percent : initiatorKey18Percent, cellId, vertices, 0, 0, 0);
  }

  @Override
  protected int readCellCount() throws IOException {
    return readInt();
  }

  @Override
  protected int readVertexCount() throws IOException {
    return readInt();
  }
}

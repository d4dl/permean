package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.jetbrains.annotations.NotNull;

public class ShortFormatCellReader extends DataInputCellReader {
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
      float latitude = readFloat();
      float longitude = readFloat();
      orderedVertices[i] = new Vertex(i, latitude, longitude);
      //System.out.println(i + " Getting vertex " + orderedVertices[i]);
      reporter.incrementVerticesWritten();
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
    return new Cell(initiator == 0 ? initiatorKey82Percent : initiatorKey18Percent, cellId, vertices, 0, 0, 0);
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

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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class CellReader extends DataIO {

  private DataInputStream in;
  private final boolean longFormat;

  protected CellReader(String reporterName, String fileIn, boolean longFormat) {
    super(reporterName);
    in = initializeReader(fileIn);
    this.longFormat = longFormat;
  }

  private DataInputStream initializeReader(String fileIn) {
    try {
      File file = new File(fileIn);
      System.out.println("Reading cells from " + file.getAbsolutePath());
      if(file.getName().endsWith(".gz")) {
        return new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))));
      } else {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  protected int readByte() throws IOException {
    int value = in.readByte();
    //System.out.println("IN 8 " + value);
    return value;
  }

  protected int readInt() throws IOException {
    int value = in.readInt();
    //System.out.println("IN 32 " + value);
    return value;
  }

  protected long readLong() throws IOException {
    long value = in.readLong();
    //System.out.println("IN 64 " + value);
    return value;
  }

  protected float readFloat() throws IOException {
    float value = in.readFloat();
    //System.out.println("IN 32F " + value);
    return value;
  }


  /**
   *
   * @param writer write the cells out, otherwise return them
   * @param validateOnly if true returns and empty cell array just to make sure the file can be read
   * @return
   */
  public Cell[] readCells(CellWriter writer, boolean validateOnly) {
    int initiator82Count = 0;
    int initiator18Count = 0;
    int currentPersistentVertexIndex = 0;
    Cell[] cells = null;
    // Preserve the order the vertices are read in so the indexes are correct
    try {
      int cellCount = readInt();
      int totalVertexCount = readInt();
      if (reporter != null) {
        reporter.setVertexCount(totalVertexCount);
        reporter.setCellCount(cellCount);
      }
      if (writer != null) {
        writer.setCountsAndStartWriting(cellCount, totalVertexCount);
      }
      Map<UUID, Vertex> vertexMap = new HashMap(totalVertexCount);// For the long format
      Vertex[] orderedVertices = new Vertex[totalVertexCount];//For the short format
      cells = new Cell[cellCount];
      // Read all the vertexes
      for (int i = 0; i < totalVertexCount; i++) {
        UUID vertexId = null;
        if (longFormat) {
          long vertexUuidMSB = readLong();
          long vertexUuidLSB = readLong();
          vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
        }
        float latitude = readFloat();
        float longitude = readFloat();
        if (longFormat) {
          // The uuids can actually just be generated.  See stableUUID
          vertexMap.put(vertexId, new Vertex(i, latitude, longitude));
        } else {
          orderedVertices[i] = new Vertex(i, latitude, longitude);
        }
        //System.out.println(i + " Getting vertex " + orderedVertices[i]);
        reporter.incrementVerticesWritten();
      }
      System.out.println("Finished reading in vertices.  Now reading and populating cells.");
      if(writer != null && reporter != null) {
        reporter.reset();
      }

      for (int c=0; c < cellCount; c++) {
        long uuidMSB = readLong();
        long uuidLSB = readLong();
        UUID cellId = new UUID(uuidMSB, uuidLSB);
        int initiator = readByte();
        int vertexCount = readByte();
        Vertex[] vertices = new Vertex[vertexCount];
        //Read the vertex ids for the cell
        for (int i = 0; i < vertexCount; i++) {
          Vertex vertex;
          if (longFormat) {
            long vertexUuidMSB = readLong();
            long vertexUuidLSB = readLong();
            UUID vertexId = new UUID(vertexUuidMSB, vertexUuidLSB);
            vertex = vertexMap.get(vertexId);
            short accessCount = vertex.access();
            if (accessCount == 3) {
              vertexMap.remove(vertexId);
            }
          } else {
            int vertexIndex = readInt();
            //System.out.println("Reading int " + vertexIndex);
            vertex = orderedVertices[vertexIndex];
          }
          vertices[i] = vertex;
        }
        if (initiator == 0) {
          initiator82Count++;
        } else {
          initiator18Count++;
        }
        Cell cell = new Cell(initiator == 0 ? initiatorKey82Percent : initiatorKey18Percent, cellId, vertices, 0, 0, 0);
        if (writer != null) {
          currentPersistentVertexIndex = writer.writeCell(currentPersistentVertexIndex, cell);
        } else if (!validateOnly) {
          cells[c] = cell;
        }
        reporter.incrementCellsWritten();

        //System.out.println("R: " + cells[c]);
      }
      System.out.println("18% " + initiator18Count + " 82% " + initiator82Count + " " + (initiator18Count / cellCount) + "%");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        in.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return cells;
  }

  public void close() {
    try {
      if (in != null) {
        in.close();
      }
      super.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Cell[] readCells(CellWriter writer) {
    return readCells(writer, false);
  }
}

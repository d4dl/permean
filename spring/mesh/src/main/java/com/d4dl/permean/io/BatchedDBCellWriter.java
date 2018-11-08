package com.d4dl.permean.io;

import com.d4dl.permean.mesh.BarycenterBuilder;
import com.d4dl.permean.mesh.DefaultCell;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import com.d4dl.permean.mesh.Position;
import com.d4dl.permean.mesh.StatementWriter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class BatchedDBCellWriter implements CellWriter {

  private final StatementWriter cellWriter;
  private int currentPersistentVertexIndex;

  public BatchedDBCellWriter(StatementWriter cellWriter) {
   this.cellWriter = cellWriter;
  }

  Set seenIds = new HashSet();
  @Override
  public void writeCell(MeshCell cell, boolean b) {
    MeshVertex[] vertices = cell.getVertices();
    for(MeshVertex vertex : vertices) {
      if (vertex.getIndex() == currentPersistentVertexIndex) {
        currentPersistentVertexIndex++;
        cellWriter.add(vertex);
      }
    }
    Position center = BarycenterBuilder.calculateBaryCenter(vertices);
    MeshVertex centerVertex = center.getVertex();
    if (seenIds.contains(cell.getId().toString())){
      System.out.println("I see the same cell");
    }
    seenIds.add(cell.getId().toString());
    String initiator = cell.getInitiator();
    cellWriter.add(new DefaultCell(initiator, cell.getId(), vertices, -1, centerVertex.getLatitude(), centerVertex.getLongitude()));
  }

  @Override
  public void setCountsAndStartWriting(int cellCount, int vertexCount) {
  }

  @Override
  public int writeCell(int currentPersistentVertexIndex, MeshCell cell) {
    writeCell(cell, false);
    return 0;
  }

  public static void main(String args[]) throws SQLException {
    if(args.length != 7) {
      System.err.println("Usage java -cp <cp> StatementWriter dbHost dbPort db username password meshFile dbBatchSize");
      System.exit(4);
    }
    int batchSize = Integer.parseInt(args[6]);
    StatementWriter cellWriter = new StatementWriter(-1, false, false, args[0], args[1], args[2], args[3], args[4], batchSize);


    CellReader reader = null;
    try {
      reader = new ShortFormatMappedCellReader("DbLoader", args[5]);
      BatchedDBCellWriter writer = new BatchedDBCellWriter(cellWriter);
      reader.readCells(writer);
    } finally {
      reader.close();
      if(cellWriter != null) {
        try {
          cellWriter.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

}


package com.d4dl.permean.io;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import com.d4dl.permean.mesh.BarycenterBuilder;
import com.d4dl.permean.mesh.Position;
import com.d4dl.permean.mesh.StatementWriter;
import com.d4dl.permean.mesh.MeshCell;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    Vertex[] vertices = cell.getVertices();
    for(Vertex vertex : vertices) {
      if (vertex.getIndex() == currentPersistentVertexIndex) {
        currentPersistentVertexIndex++;
        cellWriter.add(vertex);
      }
    }
    Position center = BarycenterBuilder.calculateBaryCenter(vertices);
    Vertex centerVertex = center.getVertex();
    if (seenIds.contains(cell.getId().toString())){
      System.out.println("I see the same cell");
    }
    seenIds.add(cell.getId().toString());
    String initiator = cell.getInitiator();
    cellWriter.add(new Cell(initiator, cell.getId(), vertices, -1, centerVertex.getLatitude(), centerVertex.getLongitude()));
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
    if(args.length != 5) {
      System.err.println("Usage java -cp <cp> com.d4dl.permean.mesh.StatementWriter dbHost db username password meshFile");
      System.exit(4);
    }
    StatementWriter cellWriter = new StatementWriter(-1, false, false, args[0], args[1], args[2], args[3]);


    CellReader reader = null;
    try {
      reader = new ShortFormatMappedCellReader("DbLoader", args[4]);
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


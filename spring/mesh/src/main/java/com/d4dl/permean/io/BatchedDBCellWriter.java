package com.d4dl.permean.io;

import com.d4dl.permean.mesh.BarycenterBuilder;
import com.d4dl.permean.mesh.DefaultCell;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import com.d4dl.permean.mesh.Position;
import com.d4dl.permean.mesh.ProgressReporter;
import com.d4dl.permean.mesh.StatementWriter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BatchedDBCellWriter extends DataIO implements CellWriter {

  private final ThreadLocal<StatementWriter> cellWriter = new ThreadLocal();
  private int currentPersistentVertexIndex;
  private int batchSize;
  private ExecutorService workPool = Executors.newWorkStealingPool();
  private Set<StatementWriter> allWriters = new HashSet();

  public BatchedDBCellWriter(ProgressReporter progressReporter, int batchSize) {
    super(progressReporter);
    this.batchSize = batchSize;
  }

  Set seenIds = new HashSet();
  @Override
  public void writeCell(MeshCell cell, boolean b) {
    MeshVertex[] vertices = cell.getVertices();
    for(MeshVertex vertex : vertices) {
      if (vertex.getIndex() == currentPersistentVertexIndex) {
        currentPersistentVertexIndex++;
        workPool.submit(() -> {
          getCellWriter().add(vertex);
          incrementVerticesWritten();
        });
      }
    }
    Position center = BarycenterBuilder.calculateBaryCenter(vertices);
    MeshVertex centerVertex = center.getVertex();
    if (seenIds.contains(cell.getId().toString())){
      System.out.println("I see the same cell");
    }
    seenIds.add(cell.getId().toString());
    String initiator = cell.getInitiator();
    workPool.submit(() -> {
      incrementBuiltCellCount();
      DefaultCell defaultCell = new DefaultCell(initiator, cell.getId(), vertices, -1, centerVertex.getLatitude(), centerVertex.getLongitude());
      int written = getCellWriter().add(defaultCell);
      incrementCellsWritten(written);
    });
  }

  @Override
  public void setCountsAndStartWriting(int cellCount, int vertexCount) {
    setCellCount(cellCount);
    setVertexCount(vertexCount);
  }

  @Override
  public int writeCell(int currentPersistentVertexIndex, MeshCell cell) {
    writeCell(cell, false);
    return 0;
  }


  private StatementWriter getCellWriter() {
    StatementWriter writer = cellWriter.get();
    if(writer == null) {
      writer = new StatementWriter(-1, false, false, batchSize);
      this.cellWriter.set(writer);
      this.allWriters.add(writer);
    }
    return writer;
  }

  public void close() {
    super.close();
    workPool.shutdown();
    try {
      workPool.awaitTermination(10, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      for (StatementWriter writer : allWriters) {
        writer.close();
      }
    }
  }

  public static void main(String args[]) throws SQLException {
    if(args.length != 2) {
      System.err.println("Usage java -cp <cp> com.d4dl.permean.io.BatchedDBCellWriter meshFile dbBatchSize");
      System.exit(4);
    }
    String argMeshFile = args[0];
    String argBatchSize = args[1];
    int batchSize = Integer.parseInt(argBatchSize);

    CellReader reader = null;
    ProgressReporter reporter = new ProgressReporter("DB Writer");
    BatchedDBCellWriter writer = null;
    try {
      reporter.start();
      reader = new ShortFormatMappedCellReader(null, argMeshFile);
      writer = new BatchedDBCellWriter(reporter, batchSize);
      reader.readCells(writer);
    } finally {
      reader.close();
      reporter.stop();

      if(writer != null) {
        try {
          writer.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

}


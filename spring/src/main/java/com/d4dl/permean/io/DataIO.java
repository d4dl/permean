package com.d4dl.permean.io;

import com.d4dl.permean.mesh.ProgressReporter;
import java.nio.ByteBuffer;

public class DataIO {

  private ProgressReporter reporter;


  public static final int VERTEX_BYTE_SIZE_SHORT = 2 * Float.BYTES;//Short format is just the vertices in order
  public static final int VERTEX_AND_CELL_COUNT_SIZE = 8;

  public DataIO(String reporterName) {
    if (reporterName != null) {
      reporter = new ProgressReporter(reporterName, 0, 0);
      reporter.start();
    }
  }

  // 8 + 8 + 1 + 1 + (5 * 4) = 38
  public static ByteBuffer FIVE_VERTEX_CELL_BUFFER_SHORT = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          5 * (Integer.BYTES)
  );

  // 8 + 8 + 1 + 1 + (6 * 4) = 42
  public static ByteBuffer SIX_VERTEX_CELL_BUFFER_SHORT = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          6 * (Integer.BYTES)
  );


  public DataIO(ProgressReporter reporter) {
    this.reporter = reporter;
  }


  protected void incrementVerticesWritten() {
    if (reporter != null) {
      reporter.incrementVerticesWritten();
    }
  }

  protected void incrementCellsWritten() {
    if (reporter != null) {
      reporter.incrementCellsWritten();
    }
  }

  protected void reset() {
    if (reporter != null) {
      reporter.reset();
    }
  }

  protected void setCellCount(int cellCount) {
    if (reporter != null) {
      reporter.setCellCount(cellCount);
    }
  }

  protected void setVertexCount(int totalVertexCount) {
    if (reporter != null) {
      reporter.setVertexCount(totalVertexCount);
    }
  }

  public void close() {
    if (reporter != null) {
      reporter.report();
      reporter.stop();
    }
  }


}

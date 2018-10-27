package com.d4dl.permean.io;

import com.d4dl.permean.ProgressReporter;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DataIO {

  protected final ProgressReporter reporter;


  public static final int VERTEX_BYTE_SIZE_LONG = (Long.BYTES + Long.BYTES + (2 * Float.BYTES));
  public static final int VERTEX_BYTE_SIZE_SHORT = 2 * Float.BYTES;//Short format is just the vertices in order
  public static final int VERTEX_AND_CELL_COUNT_SIZE = 8;

  public DataIO(String reporterName) {
    reporter = new ProgressReporter(reporterName, 0, 0, null);
    reporter.start();
  }

  public static ByteBuffer SIX_VERTEX_CELL_BUFFER_LONG = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          6 * (Long.BYTES + Long.BYTES)
  );

  public static ByteBuffer FIVE_VERTEX_CELL_BUFFER_LONG = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          5 * (Long.BYTES + Long.BYTES)
  );

  public static ByteBuffer SIX_VERTEX_CELL_BUFFER_SHORT = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          6 * (Integer.BYTES)
  );

  public static ByteBuffer FIVE_VERTEX_CELL_BUFFER_SHORT = ByteBuffer.allocateDirect(
      Long.BYTES + Long.BYTES +// The cell uuid
          Byte.BYTES +             // The initiator flag
          Byte.BYTES +             // The vertex count
          5 * (Integer.BYTES)
  );


  public void close() {
    reporter.report();
    reporter.stop();
  }


}

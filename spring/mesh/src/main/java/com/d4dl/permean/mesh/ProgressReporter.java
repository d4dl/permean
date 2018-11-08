package com.d4dl.permean.mesh;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressReporter {


  private final NumberFormat percentInstance;
  private boolean reportingPaused = false;
  private TimerTask reportTask;
  private TimerTask writeRateTracker;
  private Timer reportTimer = new Timer();
  private Timer rateTimer = new Timer();


  private NumberFormat formatter = NumberFormat.getInstance();
  private final String name;
  private int cellCount;
  private int vertexCount;

  private final AtomicInteger populatedBaryCenterCount = new AtomicInteger();
  private final AtomicInteger builtCellCount = new AtomicInteger();
  private final AtomicInteger savedVertexCount = new AtomicInteger();
  private final AtomicInteger savedCellCount = new AtomicInteger();
  private float vertexIORate;//Rate since last report
  private float cellIORate;//Rate since last report
  private float overallCellIORate;//Rate since the beginning
  private long start;
  private Map<String, MeshVertex> cachedVertices;



  public ProgressReporter(String name, int cellCount, int vertexCount, Map<String, MeshVertex> cachedVertices) {
    this(name, cellCount, vertexCount);
    this.cachedVertices = cachedVertices;
  }

  public ProgressReporter(String name, int cellCount, int vertexCount) {
    this.name = name;
    this.cellCount = cellCount;
    this.vertexCount = vertexCount;
    percentInstance = NumberFormat.getPercentInstance();
  }

  public void start() {
    report();
    createRateWriteTracker(rateTimer);
    reportTask = new TimerTask() {
      @Override
      public void run() {
        report();
      }
    };
    reportTimer.schedule(reportTask, 1000, 1000);
  }

  public void stop() {
    reportTask.cancel();
    reportTimer.cancel();
    rateTimer.cancel();
    writeRateTracker.cancel();
    report();
  }

  public void report() {
    if(!reportingPaused) {
      System.out.print(name + " ");
      if (populatedBaryCenterCount != null && populatedBaryCenterCount.get() > 0) {
        report("Populated", cellCount, populatedBaryCenterCount.get(), "Barycenters");
      }
      if (builtCellCount != null && builtCellCount.get() > 0) {
        report("Built", cellCount, builtCellCount.get(), "Cells");
      }
      if (savedVertexCount != null && savedVertexCount.get() > 0) {
        report("Saved", vertexCount, savedVertexCount.get(), "Vertexes");
      }
      if (savedCellCount != null && savedCellCount.get() > 0) {
        report("Saved", cellCount, savedCellCount.get(), "Cells");
      }
      if (cachedVertices != null && cachedVertices.size() > 0) {
        report("Caching", vertexCount, cachedVertices.size(), "vertices");
      }
      if (vertexIORate > 0) {
        System.out.print(" IO " + vertexIORate + " vertexes per ms.");
      }
      if (cellIORate > 0) {
        System.out.print(" IO " + cellIORate + " cells per ms.");
      }
      if (overallCellIORate > 0) {
        System.out.print(" :: Overall " + overallCellIORate + " cells per ms.");
      }
      //System.out.print(" " + cellStack.size() + " cells in the cell stack (" + percentInstance.format(((double)cellStack.size()) / ((double)cellCount) + ")."));

      System.out.print("\n");
    }
  }



  private void report(String verb, int total, int count, String type) {
    if (total > 0) {
      double completion = (double) count / (double) total;
      if(completion < 1) {
        System.out.print(" " + verb + " " + formatter.format(count) +
            " of " + formatter.format(total) +
            " " + type + " (" + percentInstance.format(completion) +
            ")");

      }
    }
  }


  private void createRateWriteTracker(Timer rateTimer) {
    final float[] lastWriteCounts = new float[2];
    writeRateTracker = new TimerTask() {
      @Override
      public void run() {
        int savedCells = savedCellCount.get();
        float cellsSavedSinceLast = savedCells - lastWriteCounts[0];
        cellIORate = cellsSavedSinceLast / 1000f;
        lastWriteCounts[0] = savedCells;

        int savedVertexes = savedVertexCount.get();
        float vertexesSavedSinceLast = savedVertexes - lastWriteCounts[1];
        lastWriteCounts[1] = savedVertexes;
        vertexIORate = vertexesSavedSinceLast / 1000f;

        float totalSavedCells = savedCellCount.get();
        float duration = System.currentTimeMillis() - start;
        overallCellIORate = totalSavedCells / duration;
      }
    };

    //  count cells written every 10 seconds
    rateTimer.schedule(writeRateTracker, new Date(), 1000);
  }

  public void reset() {
    this.cellIORate = 0;
    this.vertexIORate = 0;
    populatedBaryCenterCount.set(0);
    builtCellCount.set(0);
    savedVertexCount.set(0);
    savedCellCount.set(0);
  }

  public void incrementBarycenterCount() {
    this.populatedBaryCenterCount.incrementAndGet();
  }

  public void incrementBuiltCellCount() {
    this.builtCellCount.incrementAndGet();
  }

  public void incrementVerticesWritten(int count) {
    this.savedVertexCount.addAndGet(count);
  }
  public void incrementVerticesWritten() {
    this.savedVertexCount.incrementAndGet();
  }
  public void incrementCellsWritten() {
    if (start == 0) {
      start = System.currentTimeMillis();
    }
    this.savedCellCount.incrementAndGet();
  }

  public void setCellCount(int cellCount) {
    this.cellCount = cellCount;
  }

  public void setVertexCount(int vertexCount) {
    this.vertexCount = vertexCount;
  }
}

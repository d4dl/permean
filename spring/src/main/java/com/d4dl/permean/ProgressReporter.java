package com.d4dl.permean;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Data;

@Data
public class ProgressReporter {


  private final NumberFormat percentInstance;
  private boolean iterating;
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
  private final Stack cellStack;
  private float vertexIORate;
  private float cellIORate;



  public ProgressReporter(String name, int cellCount, int vertexCount, Stack cellStack) {
    this.name = name;
    this.cellCount = cellCount;
    this.vertexCount = vertexCount;
    this.cellStack = cellStack;
    percentInstance = NumberFormat.getPercentInstance();
    rateTimer = new Timer();
    createRateWriteTracker(rateTimer);

  }

  public void start() {
    report();
    reportTask = new TimerTask() {
      @Override
      public void run() {
        report();
      }
    };

    //  reportTask will be scheduled after 5 sec delay
    reportTimer.schedule(reportTask, 1000, 1000);
  }

  public void stop() {
    reportTask.cancel();
    reportTimer.cancel();
    rateTimer.cancel();
    writeRateTracker.cancel();
    report();
  }

  private void report() {
    if(!reportingPaused) {
      System.out.print(name + " ");
      if (populatedBaryCenterCount != null && populatedBaryCenterCount.get() > 0) {
        report("Populated", cellCount, populatedBaryCenterCount.get(), "Barycenters");
      }
      if (builtCellCount != null && builtCellCount.get() > 0) {
        report("Built", cellCount, builtCellCount.get(), "Cells");
      }
      if (savedVertexCount != null && savedVertexCount.get() > 0) {
        report("Got", vertexCount, savedVertexCount.get(), "Vertexes");
      }
      if (savedCellCount != null && savedCellCount.get() > 0) {
        report("Got", cellCount, savedCellCount.get(), "Cells");
      }
      if (vertexIORate > 0) {
        System.out.print(" IO " + vertexIORate + " vertexes per ms.");
      }
      if (cellIORate > 0) {
        System.out.print(" IO " + cellIORate + " cells per ms.");
      }
      //System.out.print(" " + cellStack.size() + " cells in the cell stack (" + percentInstance.format(((double)cellStack.size()) / ((double)cellCount) + ")."));

      if (cellStack != null && cellStack.size() > 0) {
        System.out.print(" " + cellStack.size() + " cells in the cell stack .");
      }
      System.out.print("\n");
    }
  }



  private void report(String verb, int total, int count, String type) {
    if (total > 0) {
      System.out.print(" " + verb + " " + formatter.format(count) +
          " of " + formatter.format(total) +
          " " + type + " (" + percentInstance.format((double) count / (double) total) +
          ")");
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

  public void incrementVerticesWritten() {
    this.savedVertexCount.incrementAndGet();
  }
  public void incrementCellsWritten() {
    this.savedCellCount.incrementAndGet();
  }
}

package com.d4dl.permean.io;

import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import com.d4dl.permean.mesh.ProgressReporter;

public class NoOpWriter extends DataIO implements CellWriter {

  public NoOpWriter(ProgressReporter progressReporter) {
    super(progressReporter);
  }

  @Override
  public void writeCell(MeshCell cell, boolean b) {
    writeCell(0, cell);
  }

  @Override
  public void setCountsAndStartWriting(int cellCount, int vertexCount) {
    super.setCellCount(cellCount);
    super.setVertexCount(vertexCount);
  }

  public int writeCell(int currentPersistentVertexIndex, MeshCell cell) {
    super.incrementCellsWritten();
    int writtenVertices = 0;
    for (MeshVertex vertex : cell.getVertices()) {
      if (vertex.getShouldPersist()) {
        super.incrementVerticesWritten();
        writtenVertices++;
      }
    }
    return currentPersistentVertexIndex + writtenVertices;
  }
}

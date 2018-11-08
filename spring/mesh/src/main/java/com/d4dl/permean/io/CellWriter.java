package com.d4dl.permean.io;

import com.d4dl.permean.mesh.MeshCell;

public interface CellWriter {

  void writeCell(MeshCell cell, boolean b);

  void setCountsAndStartWriting(int cellCount, int vertexCount);

  int writeCell(int currentPersistentVertexIndex, MeshCell cell);
}

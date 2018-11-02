package com.d4dl.permean.io;

import com.d4dl.permean.mesh.MeshCell;

public interface CellReader {

  MeshCell[] readCells(CellWriter w);

  void close();
}

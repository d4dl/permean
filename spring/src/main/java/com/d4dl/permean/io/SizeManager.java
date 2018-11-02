package com.d4dl.permean.io;

import static com.d4dl.permean.io.DataIO.VERTEX_AND_CELL_COUNT_SIZE;
import static com.d4dl.permean.io.DataIO.VERTEX_BYTE_SIZE_SHORT;
import static com.d4dl.permean.mesh.Sphere.PEELS;

public class SizeManager {

  public static final int LAT_LNG_SIZE = 2 * Float.BYTES;

  public int calculateVertexCount(int divisions) {
    return divisions * divisions * 20;
  }

  public int calculateCellCount(int divisions) {
    return PEELS * 2 * divisions * divisions + 2;
  }


  public int getCellFileSize(int vertexCount) {
    int idSize = Long.BYTES + Long.BYTES;
    int initiatorAndCellCountSize = Byte.BYTES + Byte.BYTES;
    int cellSize = idSize + getVertexRefFileSize(vertexCount) + initiatorAndCellCountSize;

    return cellSize;
  }

  public int getHexagoncellsFileSize(int cellCount) {
    int hexagonsSize = getCellFileSize(6) * cellCount;
    return hexagonsSize;
  }

  private int getVertexRefFileSize(int vertexCount) {
    return Integer.BYTES * vertexCount;
  }

  public long getVertexFileOffset(long vertexCount) {
    return vertexCount * (long) VERTEX_BYTE_SIZE_SHORT + (long) VERTEX_AND_CELL_COUNT_SIZE;
  }

}

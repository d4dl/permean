package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.PEELS;

public class SizeManager {

  public int calculateVertexCount(int divisions) {
    return divisions * divisions * 20;
  }

  public int calculateCellCount(int divisions) {
    return PEELS * 2 * divisions * divisions + 2;
  }

}

package com.d4dl.permean.mesh;

import java.util.UUID;

public interface MeshCell {

  public MeshVertex[] getVertices();

  String getInitiator();

  UUID getId();

  float getArea();

  float getCenterLongitude();

  float getCenterLatitude();
}

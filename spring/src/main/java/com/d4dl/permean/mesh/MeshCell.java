package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Vertex;
import java.util.UUID;

public interface MeshCell {

  public Vertex[] getVertices();

  String getInitiator();

  UUID getId();

  float getArea();
}

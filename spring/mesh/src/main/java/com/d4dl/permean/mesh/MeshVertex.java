package com.d4dl.permean.mesh;

public interface MeshVertex {

  boolean getShouldPersist();

  int getIndex();

  float getLatitude();

  float getLongitude();

  short access();

  short getAccessCount();

  String getStableKey();

  String kmlString(int height);

  Position getPosition();
}

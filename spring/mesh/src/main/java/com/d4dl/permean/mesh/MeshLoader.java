package com.d4dl.permean.mesh;

public interface MeshLoader {

  void add(MeshVertex vertex);

  void stop();

  void add(MeshCell cell);
}

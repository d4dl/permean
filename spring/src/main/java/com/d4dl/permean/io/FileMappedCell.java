package com.d4dl.permean.io;

import com.d4dl.permean.data.Vertex;
import com.d4dl.permean.mesh.MeshCell;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class FileMappedCell implements MeshCell {

  private final FileChannel channel;
  private final String initiator;
  private final int[] vertexIndices;
  private final UUID id;

  public FileMappedCell(UUID id, String initiator, int[] vertexIndices, FileChannel channel) {
    this.id = id;
    this.initiator = initiator;
    this.vertexIndices = vertexIndices;
    this.channel = channel;
  }

  @Override
  public Vertex[] getVertices() {
    return new Vertex[0];
  }

  @Override
  public String getInitiator() {
    return initiator;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public float getArea() {
    return 0;
  }
}

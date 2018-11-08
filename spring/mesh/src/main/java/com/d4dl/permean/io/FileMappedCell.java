package com.d4dl.permean.io;

import static com.d4dl.permean.io.SizeManager.LAT_LNG_SIZE;

import com.d4dl.permean.mesh.DefaultVertex;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class FileMappedCell implements MeshCell {

  private final FileChannel channel;
  private final String initiator;
  private final int[] vertexIndices;
  private final UUID id;
  private final SizeManager sizeManager = new SizeManager();
  ByteBuffer positionBuffer = ByteBuffer.allocateDirect(LAT_LNG_SIZE);

  public FileMappedCell(UUID id, String initiator, int[] vertexIndices, FileChannel channel) {
    this.id = id;
    this.initiator = initiator;
    this.vertexIndices = vertexIndices;
    this.channel = channel;
  }

  @Override
  public MeshVertex[] getVertices() {
    MeshVertex[] vertices = new MeshVertex[vertexIndices.length];
    for(int i=0; i < vertices.length; i++) {
      int index = vertexIndices[i];
      try {
        long position = sizeManager.getVertexFileOffset(index);
        channel.position(position);
        channel.read(positionBuffer, position);
        float lat = positionBuffer.getFloat(0);
        float lng = positionBuffer.getFloat(4);
        vertices[i] = new DefaultVertex((UUID)null, index, lat, lng);
        positionBuffer.flip();
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    return vertices;
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

  @Override
  public float getCenterLongitude() {
    return 0;
  }

  @Override
  public float getCenterLatitude() {
    return 0;
  }
}

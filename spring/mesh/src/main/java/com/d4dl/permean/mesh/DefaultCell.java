package com.d4dl.permean.mesh;

import java.util.Arrays;
import java.util.UUID;
import lombok.Data;

@Data
public class DefaultCell implements MeshCell {

  private UUID id;
  private String initiator;
  private float area;
  private float centerLatitude;
  private float centerLongitude;
  private MeshVertex[] vertices;


  public DefaultCell(MeshVertex[] vertices, float area, float centerLatitude, float centerLongitude) {
    this(null, UUID.randomUUID(), vertices, area, centerLatitude, centerLongitude);
  }

  public DefaultCell(String initiator, UUID id, MeshVertex[] vertices, float area, float centerLatitude, float centerLongitude) {
    this.id = id;
    this.initiator = initiator;
    this.centerLatitude = centerLatitude;
    this.centerLongitude = centerLongitude;
    this.vertices = vertices;
    this.area = area;
  }
}

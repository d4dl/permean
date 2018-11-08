package com.d4dl.permean.data;

import com.d4dl.permean.mesh.MeshCell;
import java.util.UUID;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Entity
@NamedEntityGraph(name = "Cell.vertices",
        attributeNodes = @NamedAttributeNode("vertices"))
public class Cell extends BasicEntity implements MeshCell {

  private String initiator;
  private float area;
  private float centerLatitude;
  private float centerLongitude;

    @ManyToMany(fetch = FetchType.EAGER,
                cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Fetch(FetchMode.SUBSELECT)
    @JoinTable(name="cell_vertices",
            joinColumns=
            @JoinColumn(name="cell_id", referencedColumnName="id"),
            inverseJoinColumns=
            @JoinColumn(name="vertices_id", referencedColumnName="id")
    )
    @OrderColumn(name="sequence")
    private Vertex[] vertices;


    public Cell() {

    }

    public Cell(Vertex[] vertices, float area, float centerLatitude, float centerLongitude) {
        this(null, UUID.randomUUID(), vertices, area, centerLatitude, centerLongitude);
    }

    public Cell(String initiator, UUID id, Vertex[] vertices, float area, float centerLatitude, float centerLongitude) {
        setId(id);
        this.initiator = initiator;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.vertices = vertices;
        this.area = area;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

      buffer.append(getId()).append(" = {");
      for(Vertex vertex : vertices) {
          buffer.append(vertex.toString()).append(",");
      }
      buffer.append("}");
      return buffer.toString();
    }


  public String getInitiator() {
    return initiator;
  }

  public void setInitiator(String initiator) {
    this.initiator = initiator;
  }

  public float getArea() {
    return area;
  }

  public void setArea(float area) {
    this.area = area;
  }

  public float getCenterLatitude() {
    return centerLatitude;
  }

  public void setCenterLatitude(float centerLatitude) {
    this.centerLatitude = centerLatitude;
  }

  public float getCenterLongitude() {
    return centerLongitude;
  }

  public void setCenterLongitude(float centerLongitude) {
    this.centerLongitude = centerLongitude;
  }

  public Vertex[] getVertices() {
    return vertices;
  }

  public void setVertices(Vertex[] vertices) {
    this.vertices = vertices;
  }
}

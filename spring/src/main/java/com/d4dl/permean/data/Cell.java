package com.d4dl.permean.data;

import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.List;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Data
@Entity
@NamedEntityGraph(name = "Cell.vertices",
        attributeNodes = @NamedAttributeNode("vertices"))
public class Cell extends BasicEntity {

  private String initiator;
  private double area;
    float centerLatitude;
    float centerLongitude;

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

    public Cell(Vertex[] vertices, double area, float centerLatitude, float centerLongitude) {
        this(null, UUID.randomUUID(), vertices, area, centerLatitude, centerLongitude);
    }

    public Cell(String initiator, UUID id, Vertex[] vertices, double area, float centerLatitude, float centerLongitude) {
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
}

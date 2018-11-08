package com.d4dl.handler;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent.Record;
import com.d4dl.permean.io.CellBufferBuilder;
import com.d4dl.permean.io.CellBufferBuilder.GetCellData;
import com.d4dl.permean.mesh.BarycenterBuilder;
import com.d4dl.permean.mesh.DefaultCell;
import com.d4dl.permean.mesh.DefaultVertex;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import com.d4dl.permean.mesh.Position;
import com.d4dl.permean.mesh.Sphere;
import com.d4dl.permean.mesh.StatementWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class CellTransformer {

  static final Logger logger = LogManager.getLogger(CellTransformer.class);
  static final StatementWriter statementWriter = new StatementWriter();


  public String handleCellRequest(KinesisFirehoseEvent event) throws IOException {
    System.out.println("Record Size - " + event.getRecords().size());
    List<MeshCell> cells = new ArrayList();
    Map<UUID, int[]> vertexMap = new HashMap();
    for(Record rec : event.getRecords()) {
      ByteBuffer cellBuffer = rec.getData();
      GetCellData getCellData = new CellBufferBuilder.GetCellData(cellBuffer).invoke(true);
      String initiator = getCellData.getInitiator() == 0 ? Sphere.initiatorKey82Percent : Sphere.initiatorKey18Percent;
      cells.add(new DefaultCell(initiator, getCellData.getCellId(), new MeshVertex[]{}, 0, getCellData.getCenterLatitude(), getCellData.getCenterLongitude()));
      vertexMap.put(getCellData.getCellId(), getCellData.getVertexIndices());
    }
    SessionFactory sessionFactory = RDSUtil.getSessionFactory();
    try (Session session = sessionFactory.openSession()) {
      session.beginTransaction();
      session.doWork(connection -> statementWriter.persistCells(connection, cells, vertexMap));
      session.getTransaction().commit();
    }
    return "success";
  }

  public String handleVertexRequest(KinesisFirehoseEvent event) throws IOException {
      System.out.println("Record Size - " + event.getRecords().size());
      List<MeshVertex> vertices = new ArrayList();
      for(Record rec : event.getRecords()) {
        ByteBuffer vertexBuffer = rec.getData();
        int index = vertexBuffer.getInt();
        float lat = vertexBuffer.getFloat();
        float lng = vertexBuffer.getFloat();
        vertices.add(new DefaultVertex((UUID)null, index, lat, lng));
      }
      SessionFactory sessionFactory = RDSUtil.getSessionFactory();
      try (Session session = sessionFactory.openSession()) {
        session.beginTransaction();
        session.doWork(connection -> statementWriter.persistVertices(connection, vertices.toArray(new MeshVertex[0])));
        session.getTransaction().commit();
      }
      return "success";
  }
}

package com.d4dl.handler;

import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent.Record;
import com.d4dl.permean.io.CellBufferBuilder;
import com.d4dl.permean.io.CellBufferBuilder.GetCellData;
import com.d4dl.permean.mesh.DefaultCell;
import com.d4dl.permean.mesh.DefaultVertex;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import com.d4dl.permean.mesh.Sphere;
import com.d4dl.permean.mesh.StatementWriter;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CellTransformer {

  static final StatementWriter statementWriter = new StatementWriter(10);


  public String handleCellRequest(KinesisFirehoseEvent event) {
    StringBuffer buffer = new StringBuffer("\n");
    //System.out.println("Record Size:  " + event.getRecords().size());
    List<MeshCell> cells = new ArrayList();
    Map<UUID, int[]> vertexMap = new HashMap();
    for(Record rec : event.getRecords()) {
      ByteBuffer cellBuffer = rec.getData();
      logDataIn(cellBuffer);
      GetCellData getCellData = new CellBufferBuilder.GetCellData(cellBuffer, 0).invoke(true);
      String initiator = getCellData.getInitiator() == 0 ? Sphere.initiatorKey82Percent : Sphere.initiatorKey18Percent;
      cells.add(new DefaultCell(initiator, getCellData.getCellId(), new MeshVertex[]{}, 0, getCellData.getCenterLatitude(), getCellData.getCenterLongitude()));
      vertexMap.put(getCellData.getCellId(), getCellData.getVertexIndices());
      buffer.append(getCellData.getCellId()).append(": ").append(Arrays.asList(getCellData.getVertexIndices()));
    }

    try (Connection connection = RDSUtil.getConnection()) {
      statementWriter.persistCells(connection, cells, vertexMap);
      if (connection.getAutoCommit() == false) {
        connection.commit();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return buffer.toString();
  }

  public String handleVertexRequest(KinesisFirehoseEvent event) {
      StringBuffer buffer = new StringBuffer("\n");
      //System.out.println("Record Size " + event.getRecords().size());
      List<MeshVertex> vertices = new ArrayList();
      for(Record rec : event.getRecords()) {
        ByteBuffer vertexBuffer = rec.getData();
        logDataIn(vertexBuffer);
        int index = vertexBuffer.getInt();
        float lat = vertexBuffer.getFloat();
        float lng = vertexBuffer.getFloat();
        DefaultVertex vertex = new DefaultVertex((UUID) null, index, lat, lng);
        vertices.add(vertex);
        buffer.append(vertex.toString());
      }
    try (Connection connection = RDSUtil.getConnection()) {
      statementWriter.persistVertices(connection, vertices.toArray(new MeshVertex[0]));

      if (connection.getAutoCommit() == false) {
        connection.commit();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return buffer.toString();
  }

  public static void main(String args[]) {

    KinesisFirehoseEvent vertexEvent = new KinesisFirehoseEvent();
    Record vertexRecord = new Record();
    byte[] vertexData = Base64.getDecoder().decode("AAAAAT+MzM1BuAAA");
    ByteBuffer vertexBuffer = ByteBuffer.wrap(vertexData);
    vertexRecord.setData(vertexBuffer);
    vertexEvent.setRecords(Arrays.asList(new KinesisFirehoseEvent.Record[]{vertexRecord}));

    KinesisFirehoseEvent cellEvent = new KinesisFirehoseEvent();
    Record cellRecord = new Record();
    byte[] cellData = Base64.getDecoder().decode("FqrVAyQGQpeq1QMkBkKXu0KwAABCxgAAAAEAAAAB");
    cellRecord.setData(ByteBuffer.wrap(cellData));
    cellEvent.setRecords(Arrays.asList(new KinesisFirehoseEvent.Record[]{cellRecord}));

    new CellTransformer().handleVertexRequest(vertexEvent);
    new CellTransformer().handleCellRequest(cellEvent);
  }


  public void logDataIn(ByteBuffer buffer) {
    //System.out.println("Data in: pos" + buffer.position() + " limit: " + buffer.limit() + " capacity " + buffer.capacity());
    byte[] cells = new byte[buffer.limit()];
    buffer.get(cells);
    //System.out.println("[" + Base64.getEncoder().encodeToString(cells) + "]");
    buffer.flip();
  }
}

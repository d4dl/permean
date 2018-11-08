package com.d4dl.permean.io;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import com.d4dl.permean.mesh.ProgressReporter;
import java.nio.ByteBuffer;

public class KinesisWriter extends DataIO implements CellWriter {

  private int currentPersistentVertexIndex;
  private KinesisProducer kinesisCellProducer;
  private KinesisProducer kinesisVertexProducer;
  private final CellBufferBuilder cellBufferBuilder = new CellBufferBuilder();

  public KinesisWriter(ProgressReporter reporter, String endpoint, int maxConnections) {
    super(reporter);
    KinesisProducerConfiguration cellConfig = new KinesisProducerConfiguration()
        .setRecordMaxBufferedTime(3000)
        .setMetricsNamespace("cellStream")
        .setMaxConnections(maxConnections)
        .setRequestTimeout(60000)
        .setAggregationEnabled(true)
        .setCollectionMaxCount(12)
        .setKinesisEndpoint(endpoint)
        .setMetricsGranularity("stream")
        .setRegion("us-east-1");

    KinesisProducerConfiguration vertexConfig = new KinesisProducerConfiguration()
        .setRecordMaxBufferedTime(3000)
        .setMetricsNamespace("vertexStream")
        .setMaxConnections(maxConnections)
        .setRequestTimeout(60000)
        .setAggregationEnabled(true)
        .setCollectionMaxCount(12)
        .setKinesisEndpoint(endpoint)
        .setMetricsGranularity("stream")
        .setRegion("us-east-1");

    kinesisVertexProducer = new KinesisProducer(vertexConfig);
    kinesisCellProducer = new KinesisProducer(cellConfig);
  }

  @Override
  public int writeCell(int index, MeshCell cell) {
    ByteBuffer cellBuffer = cellBufferBuilder.fillCellBuffer(cell, true);//46 bytes
    addRecord(cellBuffer, kinesisCellProducer, "cellStream", cell.getId().toString());
    //The consecutive vertices should be persisted.  The non-consecutive ones were either already
    //persisted or will be later
    MeshVertex[] vertices = cell.getVertices();
    int persistedVertexCount = 0;
    for(MeshVertex vertex : vertices) {
      int vertexIndex = vertex.getIndex();
      if (vertexIndex == currentPersistentVertexIndex) {
        currentPersistentVertexIndex++;
        ByteBuffer vertexBuffer = cellBufferBuilder.getVertexBuffer(1);
        cellBufferBuilder.writeVertex(vertexBuffer, vertex);//8 bytes
        addRecord(vertexBuffer, kinesisVertexProducer, "vertexStream", vertex.getStableKey());
        persistedVertexCount++;
      }
    }
    incrementVerticesWritten(persistedVertexCount);
    incrementCellsWritten();
    return currentPersistentVertexIndex;
  }

  private void addRecord(ByteBuffer vertexBuffer, KinesisProducer producer, String vertexStream, String partitionKey) {
    producer.addUserRecord(vertexStream, partitionKey, vertexBuffer);
  }

  @Override
  public void setCountsAndStartWriting(int cellCount, int vertexCount) {
    super.setCellCount(cellCount);
    super.setVertexCount(vertexCount);
  }

  @Override
  public void writeCell(MeshCell cell, boolean writeVertices) {
    writeCell(0, cell);
  }
}

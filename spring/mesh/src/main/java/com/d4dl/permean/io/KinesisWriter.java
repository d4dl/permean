package com.d4dl.permean.io;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.d4dl.permean.io.CellBufferBuilder;
import com.d4dl.permean.io.CellWriter;
import com.d4dl.permean.io.DataIO;
import com.d4dl.permean.mesh.MeshCell;
import com.d4dl.permean.mesh.MeshVertex;
import com.d4dl.permean.mesh.ProgressReporter;
import java.nio.ByteBuffer;

public class KinesisWriter extends DataIO implements CellWriter {

  private final String cellStream;
  private final String vertexStream;
  private int currentPersistentVertexIndex;
  private KinesisProducer kinesisCellProducer;
  private KinesisProducer kinesisVertexProducer;
  private final CellBufferBuilder cellBufferBuilder = new CellBufferBuilder();

  public KinesisWriter(ProgressReporter reporter, String cellStream, String vertexStream, int cellCount, int vertexCount) {
    super(reporter);
    this.cellStream = cellStream;
    this.vertexStream = vertexStream;
    String cellNamespace = "JED" + this.cellStream + "Metrics" + cellCount;
    KinesisProducerConfiguration cellConfig = new KinesisProducerConfiguration()
        .setRequestTimeout(60000)
        .setMetricsNamespace(cellNamespace)
        .setAggregationEnabled(true)
        .setMetricsGranularity("stream")
        .setRegion("us-east-1");

    String vertexNamespace = "JED" + this.vertexStream + "Metrics" + vertexCount;
    KinesisProducerConfiguration vertexConfig = new KinesisProducerConfiguration()
        .setMetricsNamespace(vertexNamespace)
        .setRequestTimeout(60000)
        .setAggregationEnabled(true)
        .setMetricsGranularity("stream")
        .setRegion("us-east-1");

    System.out.println("Initialized kinesis writers to '"
        + this.cellStream
        + "' and '"
        + this.vertexStream
        + "'. Logs are in the namespaces: '"
        + cellNamespace
        + "' and '" + vertexNamespace + "'");
    kinesisVertexProducer = new KinesisProducer(vertexConfig);
    kinesisCellProducer = new KinesisProducer(cellConfig);
  }

  @Override
  public int writeCell(int index, MeshCell cell) {
    ByteBuffer cellBuffer = cellBufferBuilder.fillCellBuffer(cell, true);//46 bytes
    addRecord(cellBuffer, kinesisCellProducer, this.cellStream, cell.getId().toString());
    //The consecutive vertices should be persisted.  The non-consecutive ones were either already
    //persisted or will be later
    MeshVertex[] vertices = cell.getVertices();
    int persistedVertexCount = 0;
    for(MeshVertex vertex : vertices) {
      int vertexIndex = vertex.getIndex();
      if (vertexIndex == currentPersistentVertexIndex) {
        currentPersistentVertexIndex++;
        ByteBuffer vertexBuffer = cellBufferBuilder.fillVertex(vertex, true);
        addRecord(vertexBuffer, kinesisVertexProducer, this.vertexStream, vertex.getStableKey());
        persistedVertexCount++;
      }
    }
    incrementVerticesWritten(persistedVertexCount);
    incrementCellsWritten();
    return currentPersistentVertexIndex;
  }


  private void addRecord(ByteBuffer buffer, KinesisProducer producer, String stream, String partitionKey) {
  //  producer.addUserRecord(stream, partitionKey, buffer);
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

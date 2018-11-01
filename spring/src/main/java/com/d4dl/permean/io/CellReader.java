package com.d4dl.permean.io;

import static com.d4dl.permean.mesh.Sphere.initiatorKey18Percent;
import static com.d4dl.permean.mesh.Sphere.initiatorKey82Percent;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.jetbrains.annotations.NotNull;

public abstract class CellReader extends DataIO {

  protected int initiator82Count = 0;
  protected int initiator18Count = 0;

  protected CellReader(String reporterName, String fileIn) {
    super(reporterName);
  }


  /**
   *
   * @param writer write the cells out, otherwise return them
   * @param validateOnly if true returns and empty cell array just to make sure the file can be read
   * @return
   */
  public Cell[] readCells(CellWriter writer, boolean validateOnly) {
    int currentPersistentVertexIndex = 0;
    Cell[] cells = null;
    // Preserve the order the vertices are read in so the indexes are correct
    try {
      int cellCount = readCellCount();
      int totalVertexCount = readVertexCount();
      setVertexCount(totalVertexCount);
      setCellCount(cellCount);
      cells = new Cell[cellCount];
      if (writer != null) {
        writer.setCountsAndStartWriting(cellCount, totalVertexCount);
      }
      initializeVertices(totalVertexCount);

      System.out.println("Finished reading in vertices.  Now reading and populating cells.");
      if(writer != null) {
        reset();
      }

      for (int c=0; c < cellCount; c++) {
        Cell cell = nextCell();
        if (writer != null) {
          currentPersistentVertexIndex = writer.writeCell(currentPersistentVertexIndex, cell);
        } else if (!validateOnly) {
          cells[c] = cell;
        }
        incrementCellsWritten();

        //System.out.println("R: " + cells[c]);
      }
      System.out.println("18% " + initiator18Count + " 82% " + initiator82Count + " " + (initiator18Count / cellCount) + "%");
    } catch (IOException e) {
      e.printStackTrace();
    }

    return cells;
  }


  protected abstract Cell nextCell() throws IOException;

  protected abstract int readCellCount() throws IOException;
  protected abstract int readVertexCount() throws IOException;

  protected abstract Vertex nextVertex() throws IOException;


  protected abstract void initializeVertices(int vertexCount) throws IOException;

  public void close() {
    super.close();
  }

  public Cell[] readCells(CellWriter writer) {
    return readCells(writer, false);
  }
}

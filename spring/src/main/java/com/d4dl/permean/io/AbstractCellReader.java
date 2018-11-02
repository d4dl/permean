package com.d4dl.permean.io;

import com.d4dl.permean.mesh.MeshCell;
import java.io.IOException;

public abstract class AbstractCellReader extends DataIO implements CellReader {

  protected int initiator82Count = 0;
  protected int initiator18Count = 0;

  protected AbstractCellReader(String reporterName, String fileIn) {
    super(reporterName);
  }

  public MeshCell[] readCells() {
    return readCells(null, false);
  }

  /**
   *
   * @param writer write the cells out, otherwise return them
   * @param validateOnly if true returns and empty cell array just to make sure the file can be read
   * @return
   */
  public MeshCell[] readCells(CellWriter writer, boolean validateOnly) {
    int currentPersistentVertexIndex = 0;
    MeshCell[] cells = null;
    // Preserve the order the vertices are read in so the indexes are correct
    try {
      int cellCount = readCellCount();
      int totalVertexCount = readVertexCount();
      setVertexCount(totalVertexCount);
      setCellCount(cellCount);
      cells = new MeshCell[cellCount];
      if (writer != null) {
        writer.setCountsAndStartWriting(cellCount, totalVertexCount);
      }
      initializeVertices(cellCount, totalVertexCount);

      System.out.println("Finished reading in vertices.  Now reading and populating cells.");
      if(writer != null) {
        reset();
      }

      for (int c=0; c < cellCount; c++) {
        MeshCell cell = nextCell(c);
        if (writer != null) {
          currentPersistentVertexIndex = writer.writeCell(currentPersistentVertexIndex, cell);
        } else if (!validateOnly) {
          cells[c] = cell;
        }

        if (validateOnly) {
          // Validate that getting vertices doesn't tank everything.
          cell.getVertices();
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


  protected abstract MeshCell nextCell(int cellIndex) throws IOException;

  protected abstract int readCellCount() throws IOException;
  protected abstract int readVertexCount() throws IOException;

  protected abstract void initializeVertices(int cellCount, int vertexCount) throws IOException;

  public void close() {
    super.close();
  }

  public MeshCell[] readCells(CellWriter writer) {
    return readCells(writer, false);
  }
}

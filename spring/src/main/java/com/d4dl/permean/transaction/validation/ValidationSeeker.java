package com.d4dl.permean.transaction.validation;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.io.CellReader;
import com.d4dl.permean.io.ShortFormatCellReader;
import com.d4dl.permean.io.ShortFormatMappedCellReader;
import com.d4dl.permean.mesh.MeshCell;

public class ValidationSeeker {

  public static void main(String[] args) {
    CellReader cellReader = new ShortFormatMappedCellReader("Seeker", args[0]);
    MeshCell[] cells = cellReader.readCells(null, true);
    System.out.println("Finished reading " + cells.length + " cells");
    cellReader.close();
  }

}

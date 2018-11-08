package com.d4dl.permean.validation;

import com.d4dl.permean.io.AbstractCellReader;
import com.d4dl.permean.io.ShortFormatMappedCellReader;
import com.d4dl.permean.mesh.MeshCell;

public class ValidationSeeker {

  public static void main(String[] args) {
    AbstractCellReader cellReader = new ShortFormatMappedCellReader("Seeker", args[0]);
    MeshCell[] cells = cellReader.readCells(null, true);
    System.out.println("Finished reading " + cells.length + " cells");
    cellReader.close();
  }

}

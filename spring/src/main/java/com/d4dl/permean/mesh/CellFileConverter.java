package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Cell;

import com.d4dl.permean.io.CellReader;
import com.d4dl.permean.io.CellWriter;
import com.d4dl.permean.io.LongFormatCellFileReader;
import com.d4dl.permean.io.ShortFormatCellReader;
import com.d4dl.permean.io.ShortFormatCellWriter;
import java.io.*;

/**
 * Reads a cell file in long format and converts it to short format
 */
public class CellFileConverter {

  //static double eightyTwoPercent;
  //Long format uses vertex uuids short format uses vertex indexes
  boolean longFormat = false;

  public CellFileConverter(String fileIn, String fileOut, String reporterName) {
   this(fileIn, fileOut, reporterName, true);
  }
  public CellFileConverter(String fileIn, String fileOut, String reporterName, boolean longFormat) {
    this.longFormat = longFormat;

    //this.eightyTwoPercent = totalCellCount * .82;
  }






  public static void main(String[] args) throws IOException {
    // Just validate that the input file is correct.
    final boolean validate = Boolean.parseBoolean(System.getProperty("validate"));
    if (validate) {
      CellReader reader = new LongFormatCellFileReader("ValidatingReader", args[0]);
      reader.readCells(null, true);
      reader.close();
    } else {
      CellWriter selfWriter = null;
      CellReader selfReader = null;
      try {
        // Read cells from a long format file and write a short format file
        //Read from long file
        selfReader = new LongFormatCellFileReader("LongReader", args[0]);

        //Write to short file
        selfWriter = new ShortFormatCellWriter("ShortWriter", args[1]);
        Cell[] longFormatCells = selfReader.readCells(selfWriter);
        int currentPersistentVertexIndex = 0;

        //System.out.println("\n");
        //for (Cell longFormatCell : longFormatCells) {
        //currentPersistentVertexIndex = selfWriter.writeCell(currentPersistentVertexIndex, longFormatCell);
        //}

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (selfReader != null) {
          selfReader.close();
        }
        if (selfWriter != null) {
          selfWriter.close();
        }
      }
    }
  }



}

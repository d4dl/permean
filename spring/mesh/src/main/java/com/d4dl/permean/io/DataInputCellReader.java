package com.d4dl.permean.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public abstract class DataInputCellReader extends AbstractCellReader {

  private DataInputStream in;

  public DataInputCellReader(String reporterName, String fileName) {
    super(reporterName, fileName);
    in = initializeReader(fileName);
  }


  private DataInputStream initializeReader(String fileIn) {
    try {
      File file = new File(fileIn);
      System.out.println("Reading cells from " + file.getAbsolutePath());
      if(file.getName().endsWith(".gz")) {
        return new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))));
      } else {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void close() {
    try {
      if (in != null) {
        in.close();
      }
      super.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected int readByte() throws IOException {
    int value = in.readByte();
    //System.out.println("IN 8 " + value);
    return value;
  }

  protected int readInt() throws IOException {
    int value = in.readInt();
    //System.out.println("IN 32 " + value);
    return value;
  }

  protected long readLong() throws IOException {
    long value = in.readLong();
    //System.out.println("IN 64 " + value);
    return value;
  }

  protected float readFloat() throws IOException {
    float value = in.readFloat();
    //System.out.println("IN 32F " + value);
    return value;
  }

  protected float skipFloat() throws IOException {
    float value = in.skipBytes(8);
    //System.out.println("IN 32F " + value);
    return value;
  }
}

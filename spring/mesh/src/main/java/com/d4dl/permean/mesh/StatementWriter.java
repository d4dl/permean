package com.d4dl.permean.mesh;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by joshuadeford on 6/6/17.
 */
public class StatementWriter {
    private int parentSize;
    private Writer joinWriter;
    private Writer cellFileChannel;
    private Writer vertexFileChannel;
    private boolean writeFiles;
    List<MeshCell> cells = new ArrayList();
    List<MeshVertex> vertices = new ArrayList();
    public static final String CELL_INSERT = "INSERT INTO cell (id, area, parent_size, center_latitude, center_longitude) VALUES(?, ?, ?, ?, ?)";
    public static final String JOIN_INSERT = "INSERT INTO cell_vertices (cell_id, vertices_id, sequence) VALUES(?, ?, ?)";
    public static final String VERTEX_INSERT = "INSERT INTO vertex (id, latitude, longitude) VALUES(?, ?, ?)";
    //Connection joinConnection;
    Connection cellConnection;
    //Connection vertexConnection;
    public int batchSize;
    //public static int BATCH_SIZE = 1;
    private int wroteCellCount;
    private int wroteVerticesCount;
    private boolean offlineMode = false;

    public StatementWriter(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     *
     * @param parentSize
     * @param offlineMode don't really write anything.
     */
    public StatementWriter(int parentSize, boolean offlineMode, boolean writeFiles, int batchSize) {
        this(batchSize);
        this.parentSize = parentSize;
        this.offlineMode = offlineMode;
        this.writeFiles = writeFiles;
        String threadName = Thread.currentThread().getName();
        joinWriter = getFileWriter(threadName, "cell_vertices");
        cellFileChannel = getFileWriter(threadName, "cells");
        vertexFileChannel = getFileWriter(threadName, "vertices");
        //joinConnection = getConnection(threadName, "cell_vertices");
        //cellConnection = getConnection(dbHost, dbPort, db, userName, password);
        //vertexConnection = getConnection(threadName, "vertices");
    }

    public void close() {
        try {
            if(joinWriter != null) {
                joinWriter.close();
                cellFileChannel.close();
                vertexFileChannel.close();
            }
        } catch (IOException e) {
            System.out.println("Closing a writer that's already closed.");
        }
        try {
            //closeConnection(joinConnection);
            closeConnection(getConnection());
            //closeConnection(vertexConnection);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Closing a connection that's already closed.");
        }
    }

    private void closeConnection(Connection conn) throws SQLException {
        completeCells();
        if (conn.getAutoCommit() == false) {
            conn.commit();
        }
        conn.close();
    }

    public void completeCells() {
        doCells(true);
        try {
            if (getConnection().getAutoCommit() == false) {
                getConnection().commit();
            }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
    }

    private int doCells() {
        return doCells(false);
    }

    private int doCells(boolean force) {
        int wroteCellCount = 0;
        //For hexagons there will be 7 inserts for each cell
        if (!offlineMode && ((force && cells.size() > 0) || cells.size() > batchSize )) {
          wroteCellCount = persistCells(getConnection(), parentSize, cells, null);
          cells = new ArrayList();
        }
        return wroteCellCount;
    }

  public int persistCells(Connection conn, List<MeshCell> cells, Map<UUID, int[]> vertexIndices) {
      return persistCells(conn, 0, cells, vertexIndices);
  }

  public int persistCells(Connection conn, int parentSize, List<MeshCell> cells, Map<UUID, int[]> vertexIndices) {
    int wroteCellCount = 0;
    try (PreparedStatement cellStmt = conn.prepareStatement(CELL_INSERT); PreparedStatement joinStmt = conn.prepareStatement(JOIN_INSERT)) {
        doVertices();
        for (MeshCell cell : cells) {
            if(vertexIndices != null) {
                addVertexIndices(joinStmt, cell, vertexIndices.get(cell.getId()));
            } else {
                addVertexIndices(joinStmt, cell);
            }
            cellStmt.setString(1, cell.getId().toString());
            cellStmt.setDouble(2, cell.getArea());
            cellStmt.setInt(3, parentSize);
            cellStmt.setFloat(4, cell.getCenterLatitude());
            cellStmt.setFloat(5, cell.getCenterLongitude());
            cellStmt.addBatch();
        }
        cellStmt.executeLargeBatch();
        joinStmt.executeLargeBatch();
        wroteCellCount += cells.size();
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Caught an exception writing cells: " + e.getMessage() + " The query will be retried.");
        throw new RuntimeException(e);
    }
    return wroteCellCount;
  }

  private void doVertices() {
        try {
            if (!offlineMode && vertices.size() > 0) {
                try {
                    persistVertices(getConnection(), vertices.toArray(new MeshVertex[0]));
                    wroteVerticesCount += vertices.size();
                    vertices = new ArrayList();
                } catch (Exception e) {
                    System.out.println("Caught an exception writing vertexes: " + e.getMessage() + " The query will be retried.");
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void persistVertices(Connection conn, MeshVertex[] vertices) {
        try (PreparedStatement stmt = conn.prepareStatement(VERTEX_INSERT)) {
            for (MeshVertex vertex : vertices) {
                stmt.setInt(1, vertex.getIndex());
                stmt.setFloat(2, vertex.getLatitude());
                stmt.setFloat(3, vertex.getLongitude());
                stmt.addBatch();
            }
            stmt.executeLargeBatch();
            if (conn.getAutoCommit() == false) {
                conn.commit();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addVertexIndices(PreparedStatement joinStmt, MeshCell cell, int[] vertexIndices) {
        try {
            for (int i = 0; i < vertexIndices.length; i++) {
                joinStmt.setString(1, cell.getId().toString());
                joinStmt.setInt(2, vertexIndices[i]);
                joinStmt.setInt(3, i);
                joinStmt.addBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addVertexIndices(PreparedStatement joinStmt, MeshCell cell) {
        try {
            for (int i = 0; i < cell.getVertices().length; i++) {
                MeshVertex vertex = cell.getVertices()[i];
                joinStmt.setString(1, cell.getId().toString());
                joinStmt.setInt(2, vertex.getIndex());
                joinStmt.setInt(3, i);
                joinStmt.addBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int add(MeshCell cell) {
        cells.add(cell);
        return doCells();
    }

    public void add(MeshVertex vertex) {
        vertices.add(vertex);
    }

    private Writer getFileWriter(String threadName, String typeName) {
        try {
            if(writeFiles) {
                File dir = new File("." + File.separator + "sql" + File.separator + typeName);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String fileName = dir + File.separator + threadName + ".sql";
                Writer writer = new BufferedWriter(new java.io.FileWriter(fileName));
                return writer;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    //export RDS_DB_NAME=mesh
    //export RDS_HOSTNAME=mesh-2768.cluster-c1gmxay4krmu.us-east-1.rds.amazonaws.com
    //export RDS_PASSWORD=OpenSesame1
    //export RDS_USERNAME=finley

    private Connection getConnection() {
        if (cellConnection != null) {
            return cellConnection;
        }

        //(String dbHost, String port, String db, String username, String password)
        String rdsPort = System.getenv("RDS_PORT");
        if (rdsPort == null) {
            rdsPort = "3306";
        }
        this.cellConnection = getConnection(System.getenv("RDS_HOSTNAME"),
            rdsPort,
            System.getenv("RDS_DB_NAME"),
            System.getenv("RDS_USERNAME"),
            System.getenv("RDS_PASSWORD"));
        return cellConnection;
    }

    public Connection getConnection(String dbHost, String port, String db, String username, String password) {
        try {
            //String connectionURL = "jdbc:mysql://52.204.194.246:3306/plm2";
            String connectionURL = "jdbc:mysql://" + dbHost + ":" + port + "/" + db+ "?serverTimezone=UTC";
            //System.out.println("Getting connection for " + connectionURL);
            Connection con = DriverManager.getConnection(connectionURL, username, password);
            con.setAutoCommit(false);
            try (Statement stmt = con.createStatement()) {
                //System.out.println(sql);
                 stmt.execute("SET unique_checks=0");
                 stmt.execute("SET foreign_key_checks=0");
                 con.commit();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error getting connection");
            }
            //System.out.println("Created a connection to " + connectionURL);
            return con;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getWroteCellCount() {
        return wroteCellCount;
    }

    public int getWroteVerticesCount() {
        return wroteVerticesCount;
    }

}

package com.d4dl.permean.mesh;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by joshuadeford on 6/6/17.
 */
public class StatementWriter {
    private final int parentSize;
    private final Writer joinWriter;
    private final Writer cellFileChannel;
    private final Writer vertexFileChannel;
    private final boolean writeFiles;
    List<Cell> cells = new ArrayList();
    List<Vertex> vertices = new ArrayList();
    public static final String CELL_INSERT = "INSERT INTO cell VALUES(?, ?, ?, ?, ?)";
    public static final String JOIN_INSERT = "INSERT INTO cell_vertices VALUES(?, ?, ?)";
    public static final String VERTEX_INSERT = "INSERT INTO vertex VALUES(?, ?, ?)";
    //Connection joinConnection;
    Connection cellConnection;
    //Connection vertexConnection;
    public static int BATCH_SIZE = 1000;
    //public static int BATCH_SIZE = 1;
    private int wroteCellCount;
    private int wroteVerticesCount;
    private boolean offlineMode = false;

    /**
     *
     * @param parentSize
     * @param offlineMode don't really write anything.
     */
    public StatementWriter(int parentSize, boolean offlineMode, boolean writeFiles, String dbHost, String db, String userName, String password) {
        this.parentSize = parentSize;
        this.offlineMode = offlineMode;
        this.writeFiles = writeFiles;
        String threadName = Thread.currentThread().getName();
        joinWriter = getFileWriter(threadName, "cell_vertices");
        cellFileChannel = getFileWriter(threadName, "cells");
        vertexFileChannel = getFileWriter(threadName, "vertices");
        //joinConnection = getConnection(threadName, "cell_vertices");
        cellConnection = getConnection(dbHost, db, userName, password);
        //vertexConnection = getConnection(threadName, "vertices");
    }

    public void close() throws Exception {
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
            closeConnection(cellConnection);
            //closeConnection(vertexConnection);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Closing a connection that's already closed.");
        }
    }

    private void closeConnection(Connection conn) throws SQLException {
        completeVertices();
        completeCells();
        conn.commit();
        conn.close();
    }

    public void completeVertices() {
        doVertices(true);
        try {
            cellConnection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void completeCells() {
        doCells(true);
        try {
            cellConnection.commit();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
    }

    private void doVertices() {
        doVertices(false);
    }

    private void doCells() {
        doCells(false);
    }

    private void doCells(boolean force) {
        //For hexagons there will be 7 inserts for each cell
        if (!offlineMode && ((force && cells.size() > 0) || cells.size() > BATCH_SIZE / 7 )) {
            doVertices(true);
            try (PreparedStatement cellStmt = cellConnection.prepareStatement(CELL_INSERT); PreparedStatement joinStmt = cellConnection.prepareStatement(JOIN_INSERT)) {
                for (Cell cell : cells) {
                    addVertexValues(joinStmt, cell);
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
                cells = new ArrayList();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Caught an exception writing cells: " + e.getMessage() + " The query will be retried.");
                throw new RuntimeException(e);
            }
        }
    }

    private void doVertices(boolean force) {
        try {
            if (!offlineMode && ((force && vertices.size() > 0) || vertices.size() > BATCH_SIZE)) {
                try (PreparedStatement stmt = cellConnection.prepareStatement(VERTEX_INSERT)) {
                    for (Vertex vertex : vertices) {
                        stmt.setInt(1, vertex.getIndex());
                        stmt.setFloat(2, vertex.getLatitude());
                        stmt.setFloat(3, vertex.getLongitude());
                        stmt.addBatch();
                    }
                    stmt.executeLargeBatch();
                    cellConnection.commit();
                    wroteVerticesCount += vertices.size();
                    vertices = new ArrayList();
                } catch (Exception e) {
                    System.out.println("Caught an exception writing vertexes: " + e.getMessage() + " The query will be retried.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addVertexValues(PreparedStatement joinStmt, Cell cell) {
        try {
            for (int i = 0; i < cell.getVertices().length; i++) {
                Vertex vertex = cell.getVertices()[i];
                joinStmt.setString(1, cell.getId().toString());
                joinStmt.setInt(2, vertex.getIndex());
                joinStmt.setInt(3, i);
                joinStmt.addBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(Cell cell) {
        cells.add(cell);
        doCells();
    }

    public void add(Vertex vertex) {
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


    private static Connection getConnection(String dbHost, String db, String username, String password) {
        try {
            //String connectionURL = "jdbc:mysql://52.204.194.246:3306/plm2";
            String connectionURL = "jdbc:mysql://" + dbHost + ":3307/" + db+ "?dontCheckOnDuplicateKeyUpdateInSQL=TRUE&serverTimezone=UTC";
            System.out.println("Getting connection for " + connectionURL);
            Connection con = DriverManager.getConnection(connectionURL, username, password);
            con.setAutoCommit(false);
            try (Statement stmt = con.createStatement()) {
                //System.out.println(sql);
                // stmt.execute("SET unique_checks=0");
                // stmt.execute("SET foreign_key_checks=0");
                // con.commit();
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

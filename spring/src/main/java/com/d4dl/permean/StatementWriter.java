package com.d4dl.permean;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joshuadeford on 6/6/17.
 */
public class StatementWriter {
    private final int parentSize;
    List<Cell> cells = new ArrayList();
    List<Vertex> vertices = new ArrayList();
    public static final String CELL_INSERT = "INSERT INTO cell (id, area, parent_size) ";
    public static final String JOIN_INSERT = "INSERT INTO cell_vertices (cell_id, vertices_id, sequence) ";
    public static final String VERTEX_INSERT = "INSERT INTO vertex (id, `index`, latitude, longitude) ";
    Connection joinConnection;
    Connection cellConnection;
    Connection vertexConnection;
    public static int BUFFER_SIZE = 200;
    private int wroteCellCount;
    private int wroteVerticesCount;

    public StatementWriter(int parentSize) {
        this.parentSize = parentSize;
        String threadName = Thread.currentThread().getName();
        joinConnection = getConnection(threadName, "cell_vertices");
        cellConnection = getConnection(threadName, "cells");
        vertexConnection = getConnection(threadName, "vertices");
    }

    public void close() throws Exception {
        try {
            joinConnection.close();
            cellConnection.close();
            vertexConnection.close();
        } catch (Exception e) {
            System.out.println("Closing a stream that's already closed.");
        }
    }

    public void completeVertices() {
        doVertices(true);
    }

    public void completeCells() throws Exception {
        doCells(true);
    }

    private void doVertices() {
        doVertices(false);
    }

    private void doCells() throws Exception {
        doCells(false);
    }

    private void doCells(boolean force) throws Exception {
        if (force || cells.size() > BUFFER_SIZE && cells.size() > 0) {
                StringBuffer joinBuffer = new StringBuffer();
                StringBuffer cellBuffer = new StringBuffer();
                boolean needComma = false;
                joinBuffer.append(JOIN_INSERT).append("VALUES");
                cellBuffer.append(CELL_INSERT).append("VALUES\n(");
                for (Cell cell : cells) {
                    addVertexValues(joinBuffer, cell, needComma);
                    if (needComma) {
                        cellBuffer.append(",\n(");
                    } else {
                        needComma = true;
                    }
                    cellBuffer.append("'").append(cell.getId()).append("'").append(",").append("" + cell.getArea()).append(",").append("" + parentSize).append(")");
                }
                writeStatement(cellBuffer, cellConnection);
                writeStatement(joinBuffer, joinConnection);
                wroteCellCount += cells.size();
                cells = new ArrayList();
        }
    }

    private void doVertices(boolean force) {
        try {
            if ((force && vertices.size() > 0) || vertices.size() > BUFFER_SIZE) {
                StringBuffer vertexBuffer = new StringBuffer();
                vertexBuffer.append(VERTEX_INSERT).append("VALUES\n(");
                boolean needComma = false;
                for (Vertex vertex : vertices) {
                    if (needComma) {
                        vertexBuffer.append(",\n(");
                    }
                    vertexBuffer.append("'").append(vertex.getId()).append("',").append("" + vertex.getIndex()).append(",").append("" + vertex.getLatitude()).append(",").append("" + vertex.getLongitude());
                    needComma = true;
                    vertexBuffer.append(")");
                }
                writeStatement(vertexBuffer, vertexConnection);
                wroteVerticesCount += vertices.size();
                vertices = new ArrayList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeStatement(StringBuffer queryBuffer, Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            boolean result = stmt.execute(queryBuffer.toString());
        } catch (Exception e) {
            throw new RuntimeException("Error executing: " + queryBuffer);
        }
    }


    private void addVertexValues(StringBuffer joinBuffer, Cell cell, boolean needComma) {
        try {
            for (int i = 0; i < cell.getVertices().size(); i++) {
                if (i != 0 || needComma) {
                    joinBuffer.append(",");
                }
                Vertex vertex = cell.getVertices().get(i);
                joinBuffer.append("\n('").append(cell.getId()).append("','").append(vertex.getId()).append("',").append("" + i).append(")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(Cell cell) throws Exception {
        cells.add(cell);
        doCells();
    }

    public void add(Vertex vertex) {
        vertices.add(vertex);
        doVertices();
    }

    private Connection getConnection(String threadName, String typeName) {
        try {
            File dir = new File("." + File.separator + "sql" + File.separator + typeName);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = dir + File.separator + threadName + ".sql";
            Writer writer = new BufferedWriter(new java.io.FileWriter(fileName));
            String connectionURL = "jdbc:mysql://52.204.194.246:3306/plm";
            Connection con= DriverManager.getConnection(connectionURL,"finley","some_pass");
            System.out.println("Created a connection to " + connectionURL);
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

package com.d4dl.permean;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.Vertex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
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
    Writer joinWriter;
    Writer cellWriter;
    Writer vertexWriter;
    public static int BUFFER_SIZE = 20;
    private int wroteCellCount;
    private int wroteVerticesCount;

    public StatementWriter(int parentSize) {
        this.parentSize = parentSize;
        String threadName = Thread.currentThread().getName();
        joinWriter = getWriter(threadName, "cell_vertices");
        cellWriter = getWriter(threadName, "cells");
        vertexWriter = getWriter(threadName, "vertices");
    }

    public void close() throws IOException {
        try {
            joinWriter.close();
            cellWriter.close();
            vertexWriter.close();
        } catch (IOException e) {
            System.out.println("Closing a stream that's already closed.");
        }
    }

    public void complete() throws IOException {
        doCells(true);
        doVertices(true);
    }

    private void doVertices() {
        doVertices(false);
    }

    private void doCells() {
        doCells(false);
    }

    private void doCells(boolean force) {
        if (force || cells.size() > BUFFER_SIZE && cells.size() > 0) {
            try {
                boolean needComma = false;
                joinWriter.append(JOIN_INSERT).append("VALUES");
                cellWriter.append(CELL_INSERT).append("VALUES\n(");
                for (Cell cell : cells) {
                    addVertexValues(joinWriter, cell, needComma);
                    if (needComma) {
                        cellWriter.append(",\n(");
                    } else {
                        needComma = true;
                    }
                    cellWriter.append("'").append(cell.getId()).append("'").append(",").append("" + cell.getArea()).append(",").append("" + parentSize).append(")");
                }
                cellWriter.append(";\n");
                joinWriter.append(";\n");
                wroteCellCount += cells.size();
                cells = new ArrayList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doVertices(boolean force) {
        try {
            if ((force && vertices.size() > 0) || vertices.size() > BUFFER_SIZE) {
                vertexWriter.append(VERTEX_INSERT).append("VALUES\n(");
                boolean needComma = false;
                for (Vertex vertex : vertices) {
                    if (needComma) {
                        vertexWriter.append(",\n(");
                    }
                    vertexWriter.append("'").append(vertex.getId()).append("',").append("" + vertex.getIndex()).append(",").append("" + vertex.getLatitude()).append(",").append("" + vertex.getLongitude());
                    needComma = true;
                    vertexWriter.append(")");
                }
                vertexWriter.append(";\n");
                wroteVerticesCount += vertices.size();
                vertices = new ArrayList();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void addVertexValues(Writer joinBuffer, Cell cell, boolean needComma) {
        try {
            for (int i = 0; i < cell.getVertices().size(); i++) {
                if (i != 0 || needComma) {
                    joinBuffer.append(",");
                }
                Vertex vertex = cell.getVertices().get(i);
                joinBuffer.append("\n('").append(cell.getId()).append("','").append(vertex.getId()).append("',").append("" + i).append(")");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void add(Cell cell) {
        cells.add(cell);
        doCells();
    }

    public void add(Vertex vertex) {
        vertices.add(vertex);
        doVertices();
    }

    private Writer getWriter(String threadName, String typeName) {
        try {
            File dir = new File("." + File.separator + "sql" + File.separator + typeName);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = dir + File.separator + threadName + ".sql";
            Writer writer = new BufferedWriter(new java.io.FileWriter(fileName));
            return writer;
        } catch (IOException e) {
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

package com.d4dl.permean;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.CellRepository;
import com.d4dl.permean.data.Vertex;
import com.d4dl.permean.data.VertexRepository;
import com.d4dl.permean.mesh.Sphere;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Component
public class DatabaseLoader implements CommandLineRunner {
    boolean running = false;

    ConcurrentHashMap<String, StatementWriter> writers = new ConcurrentHashMap();
    int parentSize = 1756;
    //int parentSize = 1756;
    //int parentSize = 2153;

    public static final String CELL_INSERT = "INSERT INTO cell (id, area, parent_size) ";
    public static final String JOIN_INSERT = "INSERT INTO cell_vertices (cell_id, vertices_id, sequence) ";
    public static final String VERTEX_INSERT = "INSERT INTO vertex (id, `index`, latitude, longitude) ";

    @Autowired
    CellRepository cellRepository;

    @Autowired
    VertexRepository vertexRepository;

    public static void main(String args) throws Exception {
        new DatabaseLoader().run(args);
    }

    @Override
    public void run(String... strings) throws Exception {
        String parentSize = System.getProperty("sphere.divisions");
        if(parentSize != null) {
            this.parentSize = Integer.parseInt(parentSize);
        }

        System.out.println("Creating a sphere with " + this.parentSize + " divisions");
        Sphere sphere = new Sphere(this.parentSize, this);
        sphere.buildCells();
    }


    public void completeVertices() {
        for(StatementWriter writer : writers.values()) {
            try {
                writer.completeVertices();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        this.running = false;
        int wroteCells = 0;
        int wroteVertices = 0;
        for(StatementWriter writer : writers.values()) {
            try {
                writer.completeCells();
                wroteCells += writer.getWroteCellCount();
                wroteVertices += writer.getWroteVerticesCount();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for(StatementWriter writer : writers.values()) {
            try {
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Closed writers. Wrote " + wroteCells + " cells and " + wroteVertices + " vertices");
    }

    public void add(Cell cell) throws Exception {
        StatementWriter writer = getFileWriter();
        writer.add(cell);
    }

    @NotNull
    private StatementWriter getFileWriter() {
        String name = Thread.currentThread().getName();
        StatementWriter writer = writers.get(name);
        if(writer == null) {
            writer = new StatementWriter(this.parentSize);
            writers.put(name, writer);
        }
        return writer;
    }

    public void add(Vertex vertex) {
        StatementWriter writer = getFileWriter();
        writer.add(vertex);
    }
}

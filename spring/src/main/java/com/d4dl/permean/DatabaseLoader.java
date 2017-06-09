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
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Component
public class DatabaseLoader implements CommandLineRunner {
    boolean running = false;
    ThreadLocal<StatementWriter> allWriters = new ThreadLocal();
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


    @Override
    public void run(String... strings) throws Exception {
        Sphere sphere = new Sphere(parentSize, null);
        sphere.buildCells();
    }

    public void stop() {
        this.running = false;
        int wroteCells = 0;
        int wroteVertices = 0;
        StatementWriter.closeAll();
        System.out.println("Closed writers. Wrote " + wroteCells + " cells and " + wroteVertices + " vertices");
    }

    public void add(Cell cell) {
        StatementWriter writer = getFileWriter();
        writer.add(cell);
    }

    @NotNull
    private StatementWriter getFileWriter() {
        StatementWriter writer = allWriters.get();
        if(writer == null) {
            writer = new StatementWriter(this.parentSize);
            allWriters.set(writer);
        }
        return writer;
    }

    public void add(Vertex vertex) {
        StatementWriter writer = getFileWriter();
        writer.add(vertex);
    }
}

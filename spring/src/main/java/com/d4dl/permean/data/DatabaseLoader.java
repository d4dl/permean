package com.d4dl.permean.data;

import com.d4dl.permean.StatementWriter;
import com.d4dl.permean.mesh.Sphere;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

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
        System.out.println("Database loader is not creating Earth segments");
        segmentTheEarth();
    }

    private void segmentTheEarth() {
        String parentSize = System.getProperty("sphere.divisions");
        if(parentSize == null) {
            System.out.println("Integer property sphere.divisions is required");
        }
        if (System.getProperty("offline") == null) {
            System.out.println("Boolean property offline is required. If true no database updates will be made.");
        }
        if (System.getProperty("writeFiles") == null) {
            System.out.println("Property writeFiles is required. If true sql update files will be produced.");
        }
        if (System.getProperty("outputKML") == null) {
            System.out.println("Property outputKML is required.  If true a kml file will be created.");
        }

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
                writer.completeVertices();
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
        final boolean OFFLINE = Boolean.parseBoolean(System.getProperty("offline"));
        final boolean WRITE_FILES = Boolean.parseBoolean(System.getProperty("writeFiles"));
        if(writer == null) {
            writer = new StatementWriter(this.parentSize, OFFLINE, WRITE_FILES);
            writers.put(name, writer);
        }
        return writer;
    }

    public void add(Vertex vertex) {
        StatementWriter writer = getFileWriter();
        writer.add(vertex);
    }
}

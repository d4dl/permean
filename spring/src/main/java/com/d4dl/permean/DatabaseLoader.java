package com.d4dl.permean;

import com.d4dl.permean.data.CellRepository;
import com.d4dl.permean.data.VertexRepository;
import com.d4dl.permean.mesh.Sphere;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Created by joshuadeford on 6/6/17.
 */
@Component
public class DatabaseLoader implements CommandLineRunner {

    @Autowired
    CellRepository cellRepository;

    @Autowired
    VertexRepository vertexRepository;


    @Override
    public void run(String... strings) throws Exception {
        //Sphere sphere = new Sphere(2532);
        //Sphere sphere = new Sphere(800, vertexRepository, cellRepository);
        //Sphere sphere = new Sphere(800);
        Sphere sphere = new Sphere(3);
         sphere.buildCells();
    }
}

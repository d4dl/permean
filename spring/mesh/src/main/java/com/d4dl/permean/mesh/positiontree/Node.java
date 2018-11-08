package com.d4dl.permean.mesh.positiontree;

import com.d4dl.permean.mesh.Position;

/**
 * Created by joshuadeford on 4/11/18.
 */
public class Node {
    Position payload;
    int height;
    String name;
    Node left, right;

    Node(Position position, String name) {
        this.payload = position;
        this.name = name;
        this.height = 1;

    }
}

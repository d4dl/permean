package com.d4dl.permean.mesh.positiontree;

import com.d4dl.permean.mesh.Position;

import java.util.Random;

/**
 * Created by joshuadeford on 4/11/18.
 */
public class PositionTree {


    public static void main(String[] args) {
        AVLTreeRecursive latTree = new AVLTreeRecursive();
        Random rand = new Random(System.currentTimeMillis());


        for (int i=0; i < 65_280_252; i++) {
            Position position = new Position(rand.nextDouble() * 2*StrictMath.PI, rand.nextDouble() * 2*StrictMath.PI);
            latTree.insert(position);
        }

        /* The constructed AVL Tree would be
             30
            /  \
          20   40
         /  \     \
        10  25    50
        */
        System.out.println("Inorder traversal of constructed latTree is : ");
        latTree.inOrder(latTree.root);
    }
// This code has been contributed by Mayank Jaiswal
}

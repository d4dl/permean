package com.d4dl.permean.mesh.positiontree;

import com.d4dl.permean.mesh.Position;
import org.junit.Test;

import java.util.Random;

/**
 */
public class AVLTreeTests {
    @Test
    public void whenPositionsAreAddedRangeSearchWorks() {
        AVLTreeRecursive latTree = new AVLTreeRecursive();
        Random rand = new Random(System.currentTimeMillis());


        for (int i=0; i < 900; i++) {
            Position position = new Position(rand.nextDouble() * 2*StrictMath.PI, rand.nextDouble() * 2*StrictMath.PI);
            latTree.insert(position);
        }

        double leftLngRandom   = rand.nextDouble() * 2*StrictMath.PI;
        double rightLngRandom  = rand.nextDouble() * 2*StrictMath.PI;
        double topLatRandom    = rand.nextDouble() * 2*StrictMath.PI;
        double bottomLatRandom = rand.nextDouble() * 2*StrictMath.PI;

        double leftLng = Math.min(leftLngRandom, rightLngRandom);
        double rightLng = Math.max(leftLngRandom, rightLngRandom);
        double topLat = Math.max(topLatRandom, bottomLatRandom);
        double bottomLat = Math.min(topLatRandom, bottomLatRandom);

        /**
        Set<Position> results = latTree.findInRectangle(bottomLat, topLat, leftLng, rightLng);

        for (Position result : results) {
            assertThat(result.getλ()).isLessThanOrEqualTo(rightLng);
            assertThat(result.getλ()).isGreaterThanOrEqualTo(leftLng);
            assertThat(result.getφ()).isLessThanOrEqualTo(topLat);
            assertThat(result.getφ()).isGreaterThanOrEqualTo(bottomLat);
        }
         **/
    }
}

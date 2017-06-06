package com.d4dl.mesh.geom;

import com.d4dl.mesh.BetterIcosahedronTesselator;


/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 3/28/15
 * Time: 12:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class BetterTessalatorPainter extends TessalatorPainter {

    public static void main(String[] args) {
        final BetterTessalatorPainter self = new BetterTessalatorPainter();
        self.init(self, new BetterIcosahedronTesselator());
    }
}

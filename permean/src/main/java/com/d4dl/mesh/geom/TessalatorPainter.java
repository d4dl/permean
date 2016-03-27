package com.d4dl.mesh.geom;

import com.d4dl.mesh.IcosahedronTesselator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 3/28/15
 * Time: 12:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class TessalatorPainter extends Canvas {

    private static LatLngPolyhedron polyhedron;
    IcosahedronTesselator tesselator = new IcosahedronTesselator();
    private static int indexon = 0;
    private static GraphicalFaceVisitor visitor;

    public static void main(String[] args) {
        final TessalatorPainter self = new TessalatorPainter();
        self.tesselator = new IcosahedronTesselator();
        self.polyhedron = self.tesselator.createIcosohedron();
        visitor = new GraphicalFaceVisitor(polyhedron, self);
        polyhedron.setVisitor(visitor);

        self.setSize(800, 800);
        JFrame frame = new JFrame("FrameDemo");
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(self, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        self.createBufferStrategy(2);
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    if(polyhedron.isNeedsRefresh()) {
                        self.repaint();
                    }
                }

            }
        }).start();
        self.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                indexon++;
                if(e.getClickCount() > 1) {
                    //Click count is always 1.
                } else {
                    try {
                        new Thread(new Runnable() {
                            public void run() {
                                //if (visitor != null && indexon == 1) {
                                polyhedron.truncateVertices(indexon);
//                                for(LatLngPolygon face : polyhedron.getFaces()) {
//                                    visitor.visit(face, true);
//                                }
                                //polyhedron.truncateToNewFace(IcosahedronTesselator.northPole);
                                //polyhedron.truncateToNewFace(IcosahedronTesselator.southPole);
                                //}
                                //if(visitor != null) {
                                //    visitor.setReady();
                                //}
                                //self.repaint();
                            }
                        }).start();

                    } catch (Exception e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void mouseReleased(MouseEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void mouseEntered(MouseEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void mouseExited(MouseEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

    }

    public TessalatorPainter() {

    }

    public void paint(final Graphics g) {
        if (visitor != null) {
            visitor.repaint((Graphics2D)g);
        }
    }
}

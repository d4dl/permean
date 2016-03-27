package com.d4dl.mesh.geom;

import com.d4dl.mesh.IcosahedronTesselator;
import com.d4dl.mesh.LatLng;

import java.awt.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jdeford
 * Date: 3/28/15
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class GraphicalFaceVisitor implements FaceVisitor {
    final Color[] colors = {Color.red, Color.green, Color.blue};
    private LatLngPolyhedron polyhedron;
    private boolean isReady;
    private boolean isPainting;
    private Canvas canvas;
    private int frameTime = 300;
    private double scale = 1;
    private Image image;

    public GraphicalFaceVisitor(LatLngPolyhedron polyhedron, Canvas canvas) {
        this.polyhedron = polyhedron;
        this.canvas = canvas;
    }

    public void visit(LatLngPolygon face, boolean replace) {
//        isReady = false;
//        if (!isPainting) {
//            synchronized (faces) {
//                if (replace) {
//                    faces = new ArrayList<LatLngPolygon>();
//                }
//                this.faces.add(face);
//            }
//            waitForReady();
//
//        }
    }

    private void waitForReady() {
        while (!this.isReady()) {
            try {
                Thread.sleep(frameTime);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void repaint(Graphics2D graphics) {
        if (image == null) {
            image = canvas.getToolkit().getImage("/Users/joshuadeford/dev/plm/plmbackend/src/java/com/d4dl/mesh/geom/garnet.png");
        }

        // if (polyhedron.isNeedsRefresh()) {
        long start = System.currentTimeMillis();
        if (!isPainting) {
            graphics.setColor(new Color(161, 198, 158));
            graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            polyhedron.setNeedsRefresh(false);
            // graphics.clearRect(0, 0, 1000, 1000);
            isPainting = true;

            graphics.setColor(new Color(0, 0, 200));
            graphics.setFont(new Font("arial", Font.BOLD, 14));
            for (int j = 0; j < IcosahedronTesselator.longitudes.length; j++) {
                double lng = IcosahedronTesselator.longitudes[j];
                graphics.drawLine(this.getXLng(lng), 0, this.getXLng(lng), 1000);
            }

            graphics.drawLine(0, this.getYLat(-90), 1000, this.getYLat(-90));
            graphics.drawLine(0, this.getYLat(30), 1000, this.getYLat(30));
            graphics.drawLine(0, this.getYLat(-30), 1000, this.getYLat(-30));
            graphics.drawLine(0, this.getYLat(90), 1000, this.getYLat(90));

            graphics.setStroke(new BasicStroke(1));
            // java.util.List<LatLngPolygon> faces = this.faces == null || this.faces.size() == 0 ? polyhedron.getFaces() : this.faces;
            int faceCount = 1;
            int lastFaceCount = 0;
            int iterations = 0;
            while (faceCount != lastFaceCount) {
                List<LatLngPolygon> faces = polyhedron.getFaces();
                faceCount = faces.size();
                if (iterations % 2 == 0) {
                    for (int faceIndex = 0; faceIndex < faceCount; faceIndex++) {
                        paintFace(graphics, faces, faceIndex);
                    }

                } else {
                    for (int faceIndex = faceCount - 1; faceIndex >= 0; faceIndex--) {
                        paintFace(graphics, faces, faceIndex);
                    }
                }
                faces = polyhedron.getFaces();
                lastFaceCount = faces.size();
                iterations++;
            }
        }
        isPainting = false;
        //}
    }

    private void paintFace(Graphics2D graphics, List<LatLngPolygon> faces, int faceIndex) {
        LatLngPolygon face = faces.get(faceIndex);

        graphics.setColor(new Color(200, 0, 0));
        //graphics.drawString(face.getName(), 15, 15);
        List<LatLng> vertices = face.getVertices();
        LatLng lastVertex = vertices.get(vertices.size() - 1);
        for (int i = 0; i < vertices.size(); i++) {
            LatLng vertex = vertices.get(i);
            if (!face.edgeWraps(lastVertex, vertex)) {
                int x = getX(vertex);
                int y = getY(vertex);
                //graphics.setColor(Color.black);
                // graphics.drawString("" + (i + 1));
                //graphics.drawString("" + (i + 1) + " " + vertex + "", (float) (x + 15), (float) (y + 15));

                if (lastVertex != null) {
                    //graphics.setColor(colors[(i - 1) % colors.length]);
                    graphics.drawLine(getX(lastVertex), getY(lastVertex), x, y);
                    //drawGarnet(graphics, x, y);
                }
            }
            lastVertex = vertex;
        }
        //drawGarnet(graphics, getX(lastVertex), getY(lastVertex));
        //LatLng centroid = face.getCentroid();
        //graphics.fillOval(this.getX(centroid), this.getY(centroid), 3, 3);
    }

    private void drawGarnet(Graphics2D graphics, int x, int y) {
        //graphics.scale(1/this.scale, 1/this.scale);
        graphics.drawImage(image, x - image.getWidth(null) / 2, y - image.getHeight(null) / 2, null);
        //graphics.scale(1, 1);
    }

    public boolean isReady() {
        //return this.isReady;
        return true;
    }

    public void setReady() {
        this.isReady = true;
    }

    public int getY(LatLng vertex) {
        double lat = vertex.getLat();
        return getYLat(lat);
    }

    public int getYLat(double lat) {
        //return ((int) lat + 90) * 2;
        return ((int) lat + 90) * canvas.getHeight() / 180;
    }

    public int getX(LatLng vertex) {
        double lng = vertex.getLng();
        return getXLng(lng);
    }

    public int getXLng(double lng) {
        // return ((int) lng + 180)  * (90 / canvas.getHeight());
        return ((int) lng + 180) * (canvas.getWidth() / 360);
    }
}

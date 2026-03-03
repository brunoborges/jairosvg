package com.jairosvg;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import static com.jairosvg.Helpers.*;

/**
 * SVG shape drawers. Port of CairoSVG shapes.py
 */
public final class ShapeDrawer {

    private ShapeDrawer() {
    }

    /** Draw a circle node. */
    public static void circle(Surface surface, Node node) {
        double r = size(surface, node.get("r"));
        if (r == 0)
            return;
        double cx = size(surface, node.get("cx"), "x");
        double cy = size(surface, node.get("cy"), "y");

        surface.path.append(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r), false);
    }

    /** Draw an ellipse node. */
    public static void ellipse(Surface surface, Node node) {
        double rx = size(surface, node.get("rx"), "x");
        double ry = size(surface, node.get("ry"), "y");
        if (rx == 0 || ry == 0)
            return;
        double cx = size(surface, node.get("cx"), "x");
        double cy = size(surface, node.get("cy"), "y");

        surface.path.append(new Ellipse2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry), false);
    }

    /** Draw a line node. */
    public static void line(Surface surface, Node node) {
        double x1 = size(surface, node.get("x1"), "x");
        double y1 = size(surface, node.get("y1"), "y");
        double x2 = size(surface, node.get("x2"), "x");
        double y2 = size(surface, node.get("y2"), "y");

        surface.path.moveTo(x1, y1);
        surface.path.lineTo(x2, y2);

        double angle = pointAngle(x1, y1, x2, y2);
        node.vertices = new java.util.ArrayList<>();
        node.vertices.add(new double[]{x1, y1});
        node.vertices.add(new double[]{Math.PI - angle, angle});
        node.vertices.add(new double[]{x2, y2});
    }

    /** Draw a polygon node. */
    public static void polygon(Surface surface, Node node) {
        polyline(surface, node);
        surface.path.closePath();
    }

    /** Draw a polyline node. */
    public static void polyline(Surface surface, Node node) {
        String points = normalize(node.get("points", ""));
        if (points.isEmpty())
            return;

        node.vertices = new java.util.ArrayList<>();
        boolean first = true;
        while (!points.isBlank()) {
            ParsedPoint pt = pointWithRemainder(surface, points);
            double x = pt.x(), y = pt.y();
            points = pt.remainder();

            if (first) {
                surface.path.moveTo(x, y);
                node.vertices.add(new double[]{x, y});
                first = false;
            } else {
                double[] prev = getPrevPoint(node);
                double angle = pointAngle(prev[0], prev[1], x, y);
                node.vertices.add(new double[]{Math.PI - angle, angle});
                surface.path.lineTo(x, y);
                node.vertices.add(new double[]{x, y});
            }
        }
    }

    /** Draw a rect node. */
    public static void rect(Surface surface, Node node) {
        double x = size(surface, node.get("x"), "x");
        double y = size(surface, node.get("y"), "y");
        double width = size(surface, node.get("width"), "x");
        double height = size(surface, node.get("height"), "y");

        String rxStr = node.get("rx");
        String ryStr = node.get("ry");
        if (rxStr != null && ryStr == null)
            ryStr = rxStr;
        if (ryStr != null && rxStr == null)
            rxStr = ryStr;

        double rx = size(surface, rxStr, "x");
        double ry = size(surface, ryStr, "y");

        if (rx == 0 || ry == 0) {
            surface.path.append(new Rectangle2D.Double(x, y, width, height), false);
        } else {
            rx = Math.min(rx, width / 2);
            ry = Math.min(ry, height / 2);
            surface.path.append(new RoundRectangle2D.Double(x, y, width, height, rx * 2, ry * 2), false);
        }
    }

    private static double[] getPrevPoint(Node node) {
        for (int i = node.vertices.size() - 1; i >= 0; i--) {
            Object v = node.vertices.get(i);
            if (v instanceof double[] arr && arr.length == 2) {
                return arr;
            }
        }
        return new double[]{0, 0};
    }
}

package io.brunoborges.jairosvg.draw;

import static io.brunoborges.jairosvg.util.Helpers.clipMarkerBox;
import static io.brunoborges.jairosvg.util.Helpers.preserveRatio;
import static io.brunoborges.jairosvg.util.Helpers.size;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.surface.Surface;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * Marker rendering for SVG marker elements.
 */
public final class MarkerDrawer {

    private MarkerDrawer() {
    }

    public static void drawMarkers(Surface surface, Node node) {
        if (node.vertices == null || node.vertices.isEmpty()) {
            return;
        }

        List<MarkerVertex> markerVertices = collectMarkerVertices(node.vertices);
        if (markerVertices.isEmpty()) {
            return;
        }

        String startId = markerId(node.get("marker-start"));
        String midId = markerId(node.get("marker-mid"));
        String endId = markerId(node.get("marker-end"));

        drawMarkerAtVertex(surface, node, surface.markers.get(startId), markerVertices.getFirst(), true,
                markerVertices.size() == 1);
        if (markerVertices.size() == 1) {
            drawMarkerAtVertex(surface, node, surface.markers.get(endId), markerVertices.getFirst(), false, true);
            return;
        }
        for (int i = 1; i < markerVertices.size() - 1; i++) {
            drawMarkerAtVertex(surface, node, surface.markers.get(midId), markerVertices.get(i), false, false);
        }
        drawMarkerAtVertex(surface, node, surface.markers.get(endId), markerVertices.getLast(), false, true);
    }

    private static List<MarkerVertex> collectMarkerVertices(List<Object> vertices) {
        List<MarkerVertex> markerVertices = new ArrayList<>();
        boolean expectsPoint = true;
        MarkerVertex previous = null;
        double pendingInAngle = Double.NaN;
        for (Object vertex : vertices) {
            if (vertex == null) {
                expectsPoint = true;
                previous = null;
                pendingInAngle = Double.NaN;
                continue;
            }
            if (!(vertex instanceof double[] values) || values.length != 2) {
                continue;
            }
            if (expectsPoint) {
                MarkerVertex current = new MarkerVertex(values[0], values[1]);
                markerVertices.add(current);
                if (!Double.isNaN(pendingInAngle)) {
                    current.inAngle = pendingInAngle;
                    pendingInAngle = Double.NaN;
                }
                previous = current;
                expectsPoint = false;
            } else {
                // Angle entry: [0]=outgoing tangent at segment start, [1]=incoming tangent at
                // segment end
                if (previous != null) {
                    previous.outAngle = values[0];
                }
                pendingInAngle = values[1];
                expectsPoint = true;
            }
        }
        return markerVertices;
    }

    private static String markerId(String markerValue) {
        if (markerValue == null || markerValue.isEmpty()) {
            return null;
        }
        return UrlHelper.parseUrl(markerValue).fragment();
    }

    private static void drawMarkerAtVertex(Surface surface, Node node, Node markerNode, MarkerVertex vertex,
            boolean isStart, boolean isEnd) {
        if (markerNode == null || vertex == null) {
            return;
        }

        double angle = markerAngle(markerNode, vertex, isStart, isEnd);
        AffineTransform savedTransform = surface.context.getTransform();
        Shape savedClip = surface.context.getClip();

        surface.context.translate(vertex.x, vertex.y);
        surface.context.rotate(angle);

        if (!"userSpaceOnUse".equals(markerNode.get("markerUnits"))) {
            double strokeWidth = size(surface, node.get("stroke-width", "1"));
            surface.context.scale(strokeWidth, strokeWidth);
        }

        double[] ratio = preserveRatio(surface, markerNode);
        surface.context.scale(ratio[0], ratio[1]);
        surface.context.translate(ratio[2], ratio[3]);

        if (!"visible".equals(markerNode.get("overflow", "hidden"))) {
            double[] clipBox = clipMarkerBox(surface, markerNode, ratio[0], ratio[1]);
            surface.context.clip(new Rectangle2D.Double(clipBox[0], clipBox[1], clipBox[2], clipBox[3]));
        }

        for (Node child : markerNode.children) {
            surface.draw(child);
        }

        surface.context.setTransform(savedTransform);
        surface.context.setClip(savedClip);
    }

    private static double markerAngle(Node markerNode, MarkerVertex vertex, boolean isStart, boolean isEnd) {
        String orient = markerNode.get("orient", "0");
        if ("auto".equals(orient) || "auto-start-reverse".equals(orient)) {
            double angle;
            if (isEnd) {
                angle = vertex.inAngle;
                if (Double.isNaN(angle)) {
                    angle = vertex.outAngle;
                }
            } else if (isStart) {
                angle = vertex.outAngle;
                if (Double.isNaN(angle)) {
                    angle = vertex.inAngle;
                }
            } else {
                // Mid marker: bisector of incoming and outgoing tangent angles
                if (!Double.isNaN(vertex.inAngle) && !Double.isNaN(vertex.outAngle)) {
                    double dx = Math.cos(vertex.inAngle) + Math.cos(vertex.outAngle);
                    double dy = Math.sin(vertex.inAngle) + Math.sin(vertex.outAngle);
                    angle = Math.atan2(dy, dx);
                } else if (!Double.isNaN(vertex.outAngle)) {
                    angle = vertex.outAngle;
                } else if (!Double.isNaN(vertex.inAngle)) {
                    angle = vertex.inAngle;
                } else {
                    angle = 0;
                }
            }
            if (Double.isNaN(angle)) {
                angle = 0;
            }
            if (isStart && "auto-start-reverse".equals(orient)) {
                angle += Math.PI;
            }
            return angle;
        }
        try {
            return Math.toRadians(Double.parseDouble(orient));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final class MarkerVertex {
        final double x;
        final double y;
        double inAngle = Double.NaN;
        double outAngle = Double.NaN;

        MarkerVertex(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}

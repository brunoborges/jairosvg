package com.jairosvg;

import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import static com.jairosvg.Helpers.*;

/**
 * SVG path command parser and drawer.
 * Port of CairoSVG path.py
 */
public final class PathDrawer {

    private PathDrawer() {}

    /** Draw a path node. */
    public static void path(Surface surface, Node node) {
        String d = node.get("d", "");
        node.vertices = new ArrayList<>();

        for (char c : PATH_LETTERS.toCharArray()) {
            d = d.replace(String.valueOf(c), " " + c + " ");
        }

        String lastLetter = null;
        d = normalize(d);

        double cx = 0, cy = 0; // current point
        double x1 = 0, y1 = 0, x2 = 0, y2 = 0, x3 = 0, y3 = 0;
        double[] firstPathPoint = null;
        String letter = null;

        while (!d.isBlank()) {
            d = d.strip();
            String first = d.split("\\s+", 2)[0];

            if (first.length() == 1 && PATH_LETTERS.indexOf(first.charAt(0)) >= 0) {
                letter = first;
                d = d.length() > 1 ? d.substring(2).strip() : "";
                if ((lastLetter == null || "z".equals(lastLetter) || "Z".equals(lastLetter))
                    && !"m".equals(letter) && !"M".equals(letter)) {
                    node.vertices.add(new double[]{cx, cy});
                    firstPathPoint = new double[]{cx, cy};
                }
            } else if ("M".equals(letter)) {
                letter = "L";
            } else if ("m".equals(letter)) {
                letter = "l";
            }

            if (lastLetter == null || "m".equals(lastLetter) || "M".equals(lastLetter)
                || "z".equals(lastLetter) || "Z".equals(lastLetter)) {
                firstPathPoint = null;
            }
            if (letter != null && !"m".equals(letter) && !"M".equals(letter)
                && !"z".equals(letter) && !"Z".equals(letter) && firstPathPoint == null) {
                firstPathPoint = new double[]{cx, cy};
            }

            try {
                switch (letter) {
                    case "M" -> {
                        Object[] pt = pointWithRemainder(surface, d);
                        double x = (double) pt[0], y = (double) pt[1];
                        d = (String) pt[2];
                        if (lastLetter != null && !"z".equals(lastLetter) && !"Z".equals(lastLetter)) {
                            node.vertices.add(null);
                        }
                        surface.path.moveTo(x, y);
                        cx = x; cy = y;
                    }
                    case "m" -> {
                        Object[] pt = pointWithRemainder(surface, d);
                        double dx = (double) pt[0], dy = (double) pt[1];
                        d = (String) pt[2];
                        if (lastLetter != null && !"z".equals(lastLetter) && !"Z".equals(lastLetter)) {
                            node.vertices.add(null);
                        }
                        cx += dx; cy += dy;
                        surface.path.moveTo(cx, cy);
                    }
                    case "L" -> {
                        Object[] pt = pointWithRemainder(surface, d);
                        double x = (double) pt[0], y = (double) pt[1];
                        d = (String) pt[2];
                        double angle = pointAngle(cx, cy, x, y);
                        node.vertices.add(new double[]{Math.PI - angle, angle});
                        surface.path.lineTo(x, y);
                        cx = x; cy = y;
                    }
                    case "l" -> {
                        Object[] pt = pointWithRemainder(surface, d);
                        double dx = (double) pt[0], dy = (double) pt[1];
                        d = (String) pt[2];
                        double angle = pointAngle(0, 0, dx, dy);
                        node.vertices.add(new double[]{Math.PI - angle, angle});
                        cx += dx; cy += dy;
                        surface.path.lineTo(cx, cy);
                    }
                    case "H" -> {
                        String[] sp = (d + " ").split("\\s+", 2);
                        double x = size(surface, sp[0], "x");
                        d = sp[1];
                        double angle = x > cx ? 0 : Math.PI;
                        node.vertices.add(new double[]{Math.PI - angle, angle});
                        surface.path.lineTo(x, cy);
                        cx = x;
                    }
                    case "h" -> {
                        String[] sp = (d + " ").split("\\s+", 2);
                        double dx = size(surface, sp[0], "x");
                        d = sp[1];
                        double angle = dx > 0 ? 0 : Math.PI;
                        node.vertices.add(new double[]{Math.PI - angle, angle});
                        cx += dx;
                        surface.path.lineTo(cx, cy);
                    }
                    case "V" -> {
                        String[] sp = (d + " ").split("\\s+", 2);
                        double y = size(surface, sp[0], "y");
                        d = sp[1];
                        double angle = Math.copySign(Math.PI / 2, y - cy);
                        node.vertices.add(new double[]{-angle, angle});
                        surface.path.lineTo(cx, y);
                        cy = y;
                    }
                    case "v" -> {
                        String[] sp = (d + " ").split("\\s+", 2);
                        double dy = size(surface, sp[0], "y");
                        d = sp[1];
                        double angle = Math.copySign(Math.PI / 2, dy);
                        node.vertices.add(new double[]{-angle, angle});
                        cy += dy;
                        surface.path.lineTo(cx, cy);
                    }
                    case "C" -> {
                        Object[] p1 = pointWithRemainder(surface, d);
                        Object[] p2 = pointWithRemainder(surface, (String) p1[2]);
                        Object[] p3 = pointWithRemainder(surface, (String) p2[2]);
                        x1 = (double) p1[0]; y1 = (double) p1[1];
                        x2 = (double) p2[0]; y2 = (double) p2[1];
                        x3 = (double) p3[0]; y3 = (double) p3[1];
                        d = (String) p3[2];
                        node.vertices.add(new double[]{
                            pointAngle(x2, y2, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3; cy = y3;
                    }
                    case "c" -> {
                        double ox = cx, oy = cy;
                        Object[] p1 = pointWithRemainder(surface, d);
                        Object[] p2 = pointWithRemainder(surface, (String) p1[2]);
                        Object[] p3 = pointWithRemainder(surface, (String) p2[2]);
                        double dx1 = (double) p1[0], dy1 = (double) p1[1];
                        double dx2 = (double) p2[0], dy2 = (double) p2[1];
                        double dx3 = (double) p3[0], dy3 = (double) p3[1];
                        d = (String) p3[2];
                        x1 = ox + dx1; y1 = oy + dy1;
                        x2 = ox + dx2; y2 = oy + dy2;
                        x3 = ox + dx3; y3 = oy + dy3;
                        node.vertices.add(new double[]{
                            pointAngle(x2, y2, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3; cy = y3;
                    }
                    case "S" -> {
                        double sx1 = cx, sy1 = cy;
                        if (lastLetter != null && "csCS".contains(lastLetter)) {
                            sx1 = x3 + (x3 - x2);
                            sy1 = y3 + (y3 - y2);
                        }
                        Object[] p2 = pointWithRemainder(surface, d);
                        Object[] p3 = pointWithRemainder(surface, (String) p2[2]);
                        x2 = (double) p2[0]; y2 = (double) p2[1];
                        x3 = (double) p3[0]; y3 = (double) p3[1];
                        d = (String) p3[2];
                        x1 = sx1; y1 = sy1;
                        node.vertices.add(new double[]{
                            pointAngle(x2, y2, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3; cy = y3;
                    }
                    case "s" -> {
                        double ox = cx, oy = cy;
                        double sx1 = 0, sy1 = 0;
                        if (lastLetter != null && "csCS".contains(lastLetter)) {
                            sx1 = x3 - x2;
                            sy1 = y3 - y2;
                        }
                        Object[] p2 = pointWithRemainder(surface, d);
                        Object[] p3 = pointWithRemainder(surface, (String) p2[2]);
                        double dx2 = (double) p2[0], dy2 = (double) p2[1];
                        double dx3 = (double) p3[0], dy3 = (double) p3[1];
                        d = (String) p3[2];
                        x1 = ox + sx1; y1 = oy + sy1;
                        x2 = ox + dx2; y2 = oy + dy2;
                        x3 = ox + dx3; y3 = oy + dy3;
                        node.vertices.add(new double[]{
                            pointAngle(x2, y2, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3; cy = y3;
                    }
                    case "Q" -> {
                        Object[] p2 = pointWithRemainder(surface, d);
                        Object[] p3 = pointWithRemainder(surface, (String) p2[2]);
                        x2 = (double) p2[0]; y2 = (double) p2[1];
                        x3 = (double) p3[0]; y3 = (double) p3[1];
                        d = (String) p3[2];
                        double[] qp = quadraticPoints(cx, cy, x2, y2, x3, y3);
                        surface.path.curveTo(qp[0], qp[1], qp[2], qp[3], qp[4], qp[5]);
                        node.vertices.add(new double[]{0, 0});
                        cx = x3; cy = y3;
                    }
                    case "q" -> {
                        Object[] p2 = pointWithRemainder(surface, d);
                        Object[] p3 = pointWithRemainder(surface, (String) p2[2]);
                        double dx2 = (double) p2[0], dy2 = (double) p2[1];
                        double dx3 = (double) p3[0], dy3 = (double) p3[1];
                        d = (String) p3[2];
                        x2 = cx + dx2; y2 = cy + dy2;
                        x3 = cx + dx3; y3 = cy + dy3;
                        double[] qp = quadraticPoints(0, 0, dx2, dy2, dx3, dy3);
                        surface.path.curveTo(cx + qp[0], cy + qp[1],
                                             cx + qp[2], cy + qp[3],
                                             cx + qp[4], cy + qp[5]);
                        node.vertices.add(new double[]{0, 0});
                        cx = x3; cy = y3;
                    }
                    case "A", "a" -> {
                        Object[] rxy = pointWithRemainder(surface, d);
                        double rx = (double) rxy[0], ry = (double) rxy[1];
                        d = ((String) rxy[2]).strip();

                        String[] sp = (d + " ").split("\\s+", 2);
                        double rotation = Math.toRadians(Double.parseDouble(sp[0]));
                        d = sp[1].strip();

                        char large = d.charAt(0); d = d.substring(1).strip();
                        char sweep = d.charAt(0); d = d.substring(1).strip();

                        int largeArc = large - '0';
                        int sweepFlag = sweep - '0';
                        if ((largeArc != 0 && largeArc != 1) || (sweepFlag != 0 && sweepFlag != 1)) {
                            continue;
                        }

                        Object[] ep = pointWithRemainder(surface, d);
                        double ex = (double) ep[0], ey = (double) ep[1];
                        d = (String) ep[2];

                        if ("a".equals(letter)) {
                            ex += cx; ey += cy;
                        }

                        // Draw arc using approximation
                        drawArc(surface, cx, cy, rx, ry, rotation, largeArc != 0, sweepFlag != 0, ex, ey);
                        cx = ex; cy = ey;
                    }
                    case "Z", "z" -> {
                        node.vertices.add(null);
                        surface.path.closePath();
                        if (firstPathPoint != null) {
                            cx = firstPathPoint[0];
                            cy = firstPathPoint[1];
                        }
                    }
                }
            } catch (Helpers.PointError e) {
                break;
            } catch (Exception e) {
                // Skip malformed data
                if (!d.isEmpty()) {
                    String[] sp = d.split("\\s+", 2);
                    d = sp.length > 1 ? sp[1] : "";
                }
                continue;
            }

            if (letter != null && !"Z".equals(letter) && !"z".equals(letter)) {
                node.vertices.add(new double[]{cx, cy});
            }

            d = d.strip();
            lastLetter = letter;
        }
    }

    /** Draw an elliptical arc approximation using cubic Bézier curves. */
    private static void drawArc(Surface surface, double x1, double y1,
                                double rx, double ry, double rotation,
                                boolean largeArc, boolean sweep,
                                double x2, double y2) {
        if (rx == 0 || ry == 0) {
            surface.path.lineTo(x2, y2);
            return;
        }

        rx = Math.abs(rx);
        ry = Math.abs(ry);

        double cosR = Math.cos(rotation);
        double sinR = Math.sin(rotation);

        double dx = (x1 - x2) / 2;
        double dy = (y1 - y2) / 2;
        double x1p = cosR * dx + sinR * dy;
        double y1p = -sinR * dx + cosR * dy;

        double x1p2 = x1p * x1p;
        double y1p2 = y1p * y1p;
        double rx2 = rx * rx;
        double ry2 = ry * ry;

        // Scale radii if necessary
        double lambda = x1p2 / rx2 + y1p2 / ry2;
        if (lambda > 1) {
            double sqrtLambda = Math.sqrt(lambda);
            rx *= sqrtLambda;
            ry *= sqrtLambda;
            rx2 = rx * rx;
            ry2 = ry * ry;
        }

        double num = rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2;
        double den = rx2 * y1p2 + ry2 * x1p2;
        double sq = Math.max(0, num / den);
        sq = Math.sqrt(sq) * (largeArc == sweep ? -1 : 1);

        double cxp = sq * rx * y1p / ry;
        double cyp = -sq * ry * x1p / rx;

        double cxr = cosR * cxp - sinR * cyp + (x1 + x2) / 2;
        double cyr = sinR * cxp + cosR * cyp + (y1 + y2) / 2;

        double theta1 = angle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry);
        double dtheta = angle((x1p - cxp) / rx, (y1p - cyp) / ry,
                              (-x1p - cxp) / rx, (-y1p - cyp) / ry);

        if (!sweep && dtheta > 0) dtheta -= 2 * Math.PI;
        if (sweep && dtheta < 0) dtheta += 2 * Math.PI;

        // Approximate with cubic Bézier curves
        int segments = (int) Math.ceil(Math.abs(dtheta) / (Math.PI / 2));
        double segAngle = dtheta / segments;

        for (int i = 0; i < segments; i++) {
            double t1 = theta1 + i * segAngle;
            double t2 = theta1 + (i + 1) * segAngle;
            arcToCubic(surface, cxr, cyr, rx, ry, rotation, t1, t2);
        }
    }

    private static void arcToCubic(Surface surface, double cx, double cy,
                                   double rx, double ry, double phi,
                                   double theta1, double theta2) {
        double alpha = Math.sin(theta2 - theta1) *
            (Math.sqrt(4 + 3 * Math.pow(Math.tan((theta2 - theta1) / 2), 2)) - 1) / 3;

        double cosT1 = Math.cos(theta1), sinT1 = Math.sin(theta1);
        double cosT2 = Math.cos(theta2), sinT2 = Math.sin(theta2);
        double cosPhi = Math.cos(phi), sinPhi = Math.sin(phi);

        double px1 = rx * cosT1;
        double py1 = ry * sinT1;
        double dx1 = -rx * sinT1;
        double dy1 = ry * cosT1;

        double px2 = rx * cosT2;
        double py2 = ry * sinT2;
        double dx2 = -rx * sinT2;
        double dy2 = ry * cosT2;

        double cp1x = cx + cosPhi * (px1 + alpha * dx1) - sinPhi * (py1 + alpha * dy1);
        double cp1y = cy + sinPhi * (px1 + alpha * dx1) + cosPhi * (py1 + alpha * dy1);
        double cp2x = cx + cosPhi * (px2 - alpha * dx2) - sinPhi * (py2 - alpha * dy2);
        double cp2y = cy + sinPhi * (px2 - alpha * dx2) + cosPhi * (py2 - alpha * dy2);
        double ex = cx + cosPhi * px2 - sinPhi * py2;
        double ey = cy + sinPhi * px2 + cosPhi * py2;

        surface.path.curveTo(cp1x, cp1y, cp2x, cp2y, ex, ey);
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt(ux * ux + uy * uy) * Math.sqrt(vx * vx + vy * vy);
        double cos = Math.max(-1, Math.min(1, dot / len));
        double angle = Math.acos(cos);
        if (ux * vy - uy * vx < 0) angle = -angle;
        return angle;
    }
}

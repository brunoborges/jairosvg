package com.jairosvg.draw;

import java.util.ArrayList;

import com.jairosvg.dom.Node;
import com.jairosvg.surface.Surface;
import com.jairosvg.util.Helpers;
import com.jairosvg.util.ParsedPoint;

import static com.jairosvg.util.Helpers.*;

/**
 * SVG path command parser and drawer. Port of CairoSVG path.py
 */
public final class PathDrawer {

    private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");
    private static final java.util.regex.Pattern PATH_LETTER_PATTERN = java.util.regex.Pattern
            .compile("([achlmqstvzACHLMQSTVZ])");

    private PathDrawer() {
    }

    /** Draw a path node. */
    public static void path(Surface surface, Node node) {
        String d = node.get("d", "");
        node.vertices = new ArrayList<>();

        d = PATH_LETTER_PATTERN.matcher(d).replaceAll(" $1 ");

        String lastLetter = null;
        d = normalize(d);

        double cx = 0, cy = 0; // current point
        double x1 = 0, y1 = 0, x2 = 0, y2 = 0, x3 = 0, y3 = 0;
        double[] firstPathPoint = null;
        String letter = null;

        while (!d.isBlank()) {
            d = d.strip();
            int spaceIdx = d.indexOf(' ');
            String first = spaceIdx > 0 ? d.substring(0, spaceIdx) : d;

            if (first.length() == 1 && PATH_LETTERS.indexOf(first.charAt(0)) >= 0) {
                letter = first;
                d = d.length() > 1 ? d.substring(2).strip() : "";
                if ((lastLetter == null || lastLetter.charAt(0) == 'z' || lastLetter.charAt(0) == 'Z')
                        && (letter == null || (letter.charAt(0) != 'm' && letter.charAt(0) != 'M'))) {
                    node.vertices.add(new double[]{cx, cy});
                    firstPathPoint = new double[]{cx, cy};
                }
            } else if (letter != null && letter.charAt(0) == 'M') {
                letter = "L";
            } else if (letter != null && letter.charAt(0) == 'm') {
                letter = "l";
            }

            if (lastLetter == null || lastLetter.charAt(0) == 'm' || lastLetter.charAt(0) == 'M'
                    || lastLetter.charAt(0) == 'z' || lastLetter.charAt(0) == 'Z') {
                firstPathPoint = null;
            }
            if (letter != null && letter.charAt(0) != 'm' && letter.charAt(0) != 'M' && letter.charAt(0) != 'z'
                    && letter.charAt(0) != 'Z' && firstPathPoint == null) {
                firstPathPoint = new double[]{cx, cy};
            }

            try {
                switch (letter) {
                    case "M" -> {
                        ParsedPoint pt = pointWithRemainder(surface, d);
                        double x = pt.x(), y = pt.y();
                        d = pt.remainder();
                        if (lastLetter != null && lastLetter.charAt(0) != 'z' && lastLetter.charAt(0) != 'Z') {
                            node.vertices.add(null);
                        }
                        surface.path.moveTo(x, y);
                        cx = x;
                        cy = y;
                    }
                    case "m" -> {
                        ParsedPoint pt = pointWithRemainder(surface, d);
                        double dx = pt.x(), dy = pt.y();
                        d = pt.remainder();
                        if (lastLetter != null && lastLetter.charAt(0) != 'z' && lastLetter.charAt(0) != 'Z') {
                            node.vertices.add(null);
                        }
                        cx += dx;
                        cy += dy;
                        surface.path.moveTo(cx, cy);
                    }
                    case "L" -> {
                        ParsedPoint pt = pointWithRemainder(surface, d);
                        double x = pt.x(), y = pt.y();
                        d = pt.remainder();
                        double angle = pointAngle(cx, cy, x, y);
                        node.vertices.add(new double[]{Math.PI - angle, angle});
                        surface.path.lineTo(x, y);
                        cx = x;
                        cy = y;
                    }
                    case "l" -> {
                        ParsedPoint pt = pointWithRemainder(surface, d);
                        double dx = pt.x(), dy = pt.y();
                        d = pt.remainder();
                        double angle = pointAngle(0, 0, dx, dy);
                        node.vertices.add(new double[]{Math.PI - angle, angle});
                        cx += dx;
                        cy += dy;
                        surface.path.lineTo(cx, cy);
                    }
                    case "H" -> {
                        String[] sp = WHITESPACE.split(d + " ", 2);
                        double x = size(surface, sp[0], "x");
                        d = sp[1];
                        double angle = x > cx ? 0 : Math.PI;
                        node.vertices.add(new double[]{Math.PI - angle, angle});
                        surface.path.lineTo(x, cy);
                        cx = x;
                    }
                    case "h" -> {
                        String[] sp = WHITESPACE.split(d + " ", 2);
                        double dx = size(surface, sp[0], "x");
                        d = sp[1];
                        double angle = dx > 0 ? 0 : Math.PI;
                        node.vertices.add(new double[]{Math.PI - angle, angle});
                        cx += dx;
                        surface.path.lineTo(cx, cy);
                    }
                    case "V" -> {
                        String[] sp = WHITESPACE.split(d + " ", 2);
                        double y = size(surface, sp[0], "y");
                        d = sp[1];
                        double angle = Math.copySign(Math.PI / 2, y - cy);
                        node.vertices.add(new double[]{-angle, angle});
                        surface.path.lineTo(cx, y);
                        cy = y;
                    }
                    case "v" -> {
                        String[] sp = WHITESPACE.split(d + " ", 2);
                        double dy = size(surface, sp[0], "y");
                        d = sp[1];
                        double angle = Math.copySign(Math.PI / 2, dy);
                        node.vertices.add(new double[]{-angle, angle});
                        cy += dy;
                        surface.path.lineTo(cx, cy);
                    }
                    case "C" -> {
                        ParsedPoint p1 = pointWithRemainder(surface, d);
                        ParsedPoint p2 = pointWithRemainder(surface, p1.remainder());
                        ParsedPoint p3 = pointWithRemainder(surface, p2.remainder());
                        x1 = p1.x();
                        y1 = p1.y();
                        x2 = p2.x();
                        y2 = p2.y();
                        x3 = p3.x();
                        y3 = p3.y();
                        d = p3.remainder();
                        node.vertices.add(new double[]{pointAngle(x2, y2, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3;
                        cy = y3;
                    }
                    case "c" -> {
                        double ox = cx, oy = cy;
                        ParsedPoint p1 = pointWithRemainder(surface, d);
                        ParsedPoint p2 = pointWithRemainder(surface, p1.remainder());
                        ParsedPoint p3 = pointWithRemainder(surface, p2.remainder());
                        double dx1 = p1.x(), dy1 = p1.y();
                        double dx2 = p2.x(), dy2 = p2.y();
                        double dx3 = p3.x(), dy3 = p3.y();
                        d = p3.remainder();
                        x1 = ox + dx1;
                        y1 = oy + dy1;
                        x2 = ox + dx2;
                        y2 = oy + dy2;
                        x3 = ox + dx3;
                        y3 = oy + dy3;
                        node.vertices.add(new double[]{pointAngle(x2, y2, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3;
                        cy = y3;
                    }
                    case "S" -> {
                        double sx1 = cx, sy1 = cy;
                        if (lastLetter != null && "csCS".indexOf(lastLetter.charAt(0)) >= 0) {
                            sx1 = x3 + (x3 - x2);
                            sy1 = y3 + (y3 - y2);
                        }
                        ParsedPoint p2 = pointWithRemainder(surface, d);
                        ParsedPoint p3 = pointWithRemainder(surface, p2.remainder());
                        x2 = p2.x();
                        y2 = p2.y();
                        x3 = p3.x();
                        y3 = p3.y();
                        d = p3.remainder();
                        x1 = sx1;
                        y1 = sy1;
                        node.vertices.add(new double[]{pointAngle(x2, y2, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3;
                        cy = y3;
                    }
                    case "s" -> {
                        double ox = cx, oy = cy;
                        double sx1 = 0, sy1 = 0;
                        if (lastLetter != null && "csCS".indexOf(lastLetter.charAt(0)) >= 0) {
                            sx1 = x3 - x2;
                            sy1 = y3 - y2;
                        }
                        ParsedPoint p2 = pointWithRemainder(surface, d);
                        ParsedPoint p3 = pointWithRemainder(surface, p2.remainder());
                        double dx2 = p2.x(), dy2 = p2.y();
                        double dx3 = p3.x(), dy3 = p3.y();
                        d = p3.remainder();
                        x1 = ox + sx1;
                        y1 = oy + sy1;
                        x2 = ox + dx2;
                        y2 = oy + dy2;
                        x3 = ox + dx3;
                        y3 = oy + dy3;
                        node.vertices.add(new double[]{pointAngle(x2, y2, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3;
                        cy = y3;
                    }
                    case "Q" -> {
                        ParsedPoint p2 = pointWithRemainder(surface, d);
                        ParsedPoint p3 = pointWithRemainder(surface, p2.remainder());
                        x2 = p2.x();
                        y2 = p2.y();
                        x3 = p3.x();
                        y3 = p3.y();
                        d = p3.remainder();
                        double[] qp = quadraticPoints(cx, cy, x2, y2, x3, y3);
                        surface.path.curveTo(qp[0], qp[1], qp[2], qp[3], qp[4], qp[5]);
                        node.vertices.add(new double[]{0, 0});
                        cx = x3;
                        cy = y3;
                    }
                    case "q" -> {
                        ParsedPoint p2 = pointWithRemainder(surface, d);
                        ParsedPoint p3 = pointWithRemainder(surface, p2.remainder());
                        double dx2 = p2.x(), dy2 = p2.y();
                        double dx3 = p3.x(), dy3 = p3.y();
                        d = p3.remainder();
                        x2 = cx + dx2;
                        y2 = cy + dy2;
                        x3 = cx + dx3;
                        y3 = cy + dy3;
                        double[] qp = quadraticPoints(0, 0, dx2, dy2, dx3, dy3);
                        surface.path.curveTo(cx + qp[0], cy + qp[1], cx + qp[2], cy + qp[3], cx + qp[4], cy + qp[5]);
                        node.vertices.add(new double[]{0, 0});
                        cx = x3;
                        cy = y3;
                    }
                    case "T" -> {
                        if (lastLetter != null && "qtQT".indexOf(lastLetter.charAt(0)) >= 0) {
                            x2 = 2 * cx - x2;
                            y2 = 2 * cy - y2;
                        } else {
                            x2 = cx;
                            y2 = cy;
                        }
                        ParsedPoint p3 = pointWithRemainder(surface, d);
                        x3 = p3.x();
                        y3 = p3.y();
                        d = p3.remainder();
                        double[] qp = quadraticPoints(cx, cy, x2, y2, x3, y3);
                        surface.path.curveTo(qp[0], qp[1], qp[2], qp[3], qp[4], qp[5]);
                        node.vertices.add(new double[]{0, 0});
                        cx = x3;
                        cy = y3;
                    }
                    case "t" -> {
                        if (lastLetter != null && "qtQT".indexOf(lastLetter.charAt(0)) >= 0) {
                            x2 = 2 * cx - x2;
                            y2 = 2 * cy - y2;
                        } else {
                            x2 = cx;
                            y2 = cy;
                        }
                        ParsedPoint p3 = pointWithRemainder(surface, d);
                        double dx3 = p3.x(), dy3 = p3.y();
                        d = p3.remainder();
                        x3 = cx + dx3;
                        y3 = cy + dy3;
                        double[] qp = quadraticPoints(cx, cy, x2, y2, x3, y3);
                        surface.path.curveTo(qp[0], qp[1], qp[2], qp[3], qp[4], qp[5]);
                        node.vertices.add(new double[]{0, 0});
                        cx = x3;
                        cy = y3;
                    }
                    case "A", "a" -> {
                        ParsedPoint rxy = pointWithRemainder(surface, d);
                        double rx = rxy.x(), ry = rxy.y();
                        d = rxy.remainder().strip();

                        String[] sp = WHITESPACE.split(d + " ", 2);
                        double rotation = Math.toRadians(Double.parseDouble(sp[0]));
                        d = sp[1].strip();

                        char large = d.charAt(0);
                        d = d.substring(1).strip();
                        char sweep = d.charAt(0);
                        d = d.substring(1).strip();

                        int largeArc = large - '0';
                        int sweepFlag = sweep - '0';
                        if ((largeArc != 0 && largeArc != 1) || (sweepFlag != 0 && sweepFlag != 1)) {
                            continue;
                        }

                        ParsedPoint ep = pointWithRemainder(surface, d);
                        double ex = ep.x(), ey = ep.y();
                        d = ep.remainder();

                        if (letter.charAt(0) == 'a') {
                            ex += cx;
                            ey += cy;
                        }

                        // Draw arc using approximation
                        drawArc(surface, cx, cy, rx, ry, rotation, largeArc != 0, sweepFlag != 0, ex, ey);
                        cx = ex;
                        cy = ey;
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
                    String[] sp = WHITESPACE.split(d, 2);
                    d = sp.length > 1 ? sp[1] : "";
                }
                continue;
            }

            if (letter != null && letter.charAt(0) != 'Z' && letter.charAt(0) != 'z') {
                node.vertices.add(new double[]{cx, cy});
            }

            d = d.strip();
            lastLetter = letter;
        }
    }

    /** Draw an elliptical arc approximation using cubic Bézier curves. */
    private static void drawArc(Surface surface, double x1, double y1, double rx, double ry, double rotation,
            boolean largeArc, boolean sweep, double x2, double y2) {
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
        double dtheta = angle((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry);

        if (!sweep && dtheta > 0)
            dtheta -= 2 * Math.PI;
        if (sweep && dtheta < 0)
            dtheta += 2 * Math.PI;

        // Approximate with cubic Bézier curves
        int segments = (int) Math.ceil(Math.abs(dtheta) / (Math.PI / 2));
        double segAngle = dtheta / segments;

        for (int i = 0; i < segments; i++) {
            double t1 = theta1 + i * segAngle;
            double t2 = theta1 + (i + 1) * segAngle;
            arcToCubic(surface, cxr, cyr, rx, ry, rotation, t1, t2);
        }
    }

    private static void arcToCubic(Surface surface, double cx, double cy, double rx, double ry, double phi,
            double theta1, double theta2) {
        double dTheta = theta2 - theta1;
        double alpha = Math.sin(dTheta) * (Math.sqrt(4 + 3 * Math.pow(Math.tan(dTheta / 2), 2)) - 1) / 3;

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
        if (ux * vy - uy * vx < 0)
            angle = -angle;
        return angle;
    }
}

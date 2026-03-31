package io.brunoborges.jairosvg.draw;

import java.util.ArrayList;

import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.surface.Surface;

import java.awt.geom.GeneralPath;

import static io.brunoborges.jairosvg.util.Helpers.*;

/**
 * SVG path command parser and drawer. Port of CairoSVG path.py
 */
public final class PathDrawer {

    private static final System.Logger LOG = System.getLogger(PathDrawer.class.getName());

    private PathDrawer() {
    }

    /** Draw a path node. */
    public static void path(Surface surface, Node node) {
        String d = node.get("d", "");

        // Use cached path if available (helps when the same node is drawn multiple
        // times via <use> or markers)
        if (node.cachedPath != null) {
            surface.path.append(node.cachedPath.getPathIterator(null), false);
            node.vertices = node.cachedVertices;
            return;
        }

        node.vertices = new ArrayList<>();

        var sc = new PathScanner(d);

        char letter = 0, lastLetter = 0;
        double cx = 0, cy = 0;
        double x1 = 0, y1 = 0, x2 = 0, y2 = 0, x3 = 0, y3 = 0;
        double[] firstPathPoint = null;

        while (sc.hasMore()) {
            char cmd = sc.peekCmd();
            if (cmd != 0) {
                letter = cmd;
                if ((lastLetter == 0 || lastLetter == 'z' || lastLetter == 'Z') && letter != 'm' && letter != 'M') {
                    node.vertices.add(new double[]{cx, cy});
                    firstPathPoint = new double[]{cx, cy};
                }
            } else if (letter == 'M') {
                letter = 'L';
            } else if (letter == 'm') {
                letter = 'l';
            }

            if (lastLetter == 0 || lastLetter == 'm' || lastLetter == 'M' || lastLetter == 'z' || lastLetter == 'Z') {
                firstPathPoint = null;
            }
            if (letter != 'm' && letter != 'M' && letter != 'z' && letter != 'Z' && firstPathPoint == null) {
                firstPathPoint = new double[]{cx, cy};
            }

            try {
                switch (letter) {
                    case 'M' -> {
                        double x = sc.nextDouble(), y = sc.nextDouble();
                        if (lastLetter != 0 && lastLetter != 'z' && lastLetter != 'Z') {
                            node.vertices.add(null);
                        }
                        surface.path.moveTo(x, y);
                        cx = x;
                        cy = y;
                    }
                    case 'm' -> {
                        double dx = sc.nextDouble(), dy = sc.nextDouble();
                        if (lastLetter != 0 && lastLetter != 'z' && lastLetter != 'Z') {
                            node.vertices.add(null);
                        }
                        cx += dx;
                        cy += dy;
                        surface.path.moveTo(cx, cy);
                    }
                    case 'L' -> {
                        double x = sc.nextDouble(), y = sc.nextDouble();
                        double angle = pointAngle(cx, cy, x, y);
                        node.vertices.add(new double[]{angle, angle});
                        surface.path.lineTo(x, y);
                        cx = x;
                        cy = y;
                    }
                    case 'l' -> {
                        double dx = sc.nextDouble(), dy = sc.nextDouble();
                        double angle = pointAngle(0, 0, dx, dy);
                        node.vertices.add(new double[]{angle, angle});
                        cx += dx;
                        cy += dy;
                        surface.path.lineTo(cx, cy);
                    }
                    case 'H' -> {
                        double x = sc.nextDouble();
                        double angle = x > cx ? 0 : Math.PI;
                        node.vertices.add(new double[]{angle, angle});
                        surface.path.lineTo(x, cy);
                        cx = x;
                    }
                    case 'h' -> {
                        double dx = sc.nextDouble();
                        double angle = dx > 0 ? 0 : Math.PI;
                        node.vertices.add(new double[]{angle, angle});
                        cx += dx;
                        surface.path.lineTo(cx, cy);
                    }
                    case 'V' -> {
                        double y = sc.nextDouble();
                        double angle = Math.copySign(Math.PI / 2, y - cy);
                        node.vertices.add(new double[]{angle, angle});
                        surface.path.lineTo(cx, y);
                        cy = y;
                    }
                    case 'v' -> {
                        double dy = sc.nextDouble();
                        double angle = Math.copySign(Math.PI / 2, dy);
                        node.vertices.add(new double[]{angle, angle});
                        cy += dy;
                        surface.path.lineTo(cx, cy);
                    }
                    case 'C' -> {
                        x1 = sc.nextDouble();
                        y1 = sc.nextDouble();
                        x2 = sc.nextDouble();
                        y2 = sc.nextDouble();
                        x3 = sc.nextDouble();
                        y3 = sc.nextDouble();
                        node.vertices.add(new double[]{pointAngle(cx, cy, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3;
                        cy = y3;
                    }
                    case 'c' -> {
                        double dx1 = sc.nextDouble(), dy1 = sc.nextDouble();
                        double dx2 = sc.nextDouble(), dy2 = sc.nextDouble();
                        double dx3 = sc.nextDouble(), dy3 = sc.nextDouble();
                        x1 = cx + dx1;
                        y1 = cy + dy1;
                        x2 = cx + dx2;
                        y2 = cy + dy2;
                        x3 = cx + dx3;
                        y3 = cy + dy3;
                        node.vertices.add(new double[]{pointAngle(cx, cy, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3;
                        cy = y3;
                    }
                    case 'S' -> {
                        double sx1 = cx, sy1 = cy;
                        if ("csCS".indexOf(lastLetter) >= 0) {
                            sx1 = x3 + (x3 - x2);
                            sy1 = y3 + (y3 - y2);
                        }
                        x2 = sc.nextDouble();
                        y2 = sc.nextDouble();
                        x3 = sc.nextDouble();
                        y3 = sc.nextDouble();
                        x1 = sx1;
                        y1 = sy1;
                        node.vertices.add(new double[]{pointAngle(cx, cy, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3;
                        cy = y3;
                    }
                    case 's' -> {
                        double sx1 = 0, sy1 = 0;
                        if ("csCS".indexOf(lastLetter) >= 0) {
                            sx1 = x3 - x2;
                            sy1 = y3 - y2;
                        }
                        double dx2 = sc.nextDouble(), dy2 = sc.nextDouble();
                        double dx3 = sc.nextDouble(), dy3 = sc.nextDouble();
                        x1 = cx + sx1;
                        y1 = cy + sy1;
                        x2 = cx + dx2;
                        y2 = cy + dy2;
                        x3 = cx + dx3;
                        y3 = cy + dy3;
                        node.vertices.add(new double[]{pointAngle(cx, cy, x1, y1), pointAngle(x2, y2, x3, y3)});
                        surface.path.curveTo(x1, y1, x2, y2, x3, y3);
                        cx = x3;
                        cy = y3;
                    }
                    case 'Q' -> {
                        x2 = sc.nextDouble();
                        y2 = sc.nextDouble();
                        x3 = sc.nextDouble();
                        y3 = sc.nextDouble();
                        double[] qp = quadraticPoints(cx, cy, x2, y2, x3, y3);
                        surface.path.curveTo(qp[0], qp[1], qp[2], qp[3], qp[4], qp[5]);
                        node.vertices.add(new double[]{pointAngle(cx, cy, x2, y2), pointAngle(x2, y2, x3, y3)});
                        cx = x3;
                        cy = y3;
                    }
                    case 'q' -> {
                        double dx2 = sc.nextDouble(), dy2 = sc.nextDouble();
                        double dx3 = sc.nextDouble(), dy3 = sc.nextDouble();
                        x2 = cx + dx2;
                        y2 = cy + dy2;
                        x3 = cx + dx3;
                        y3 = cy + dy3;
                        double[] qp = quadraticPoints(0, 0, dx2, dy2, dx3, dy3);
                        surface.path.curveTo(cx + qp[0], cy + qp[1], cx + qp[2], cy + qp[3], cx + qp[4], cy + qp[5]);
                        node.vertices.add(new double[]{pointAngle(cx, cy, x2, y2), pointAngle(x2, y2, x3, y3)});
                        cx = x3;
                        cy = y3;
                    }
                    case 'T' -> {
                        if ("qtQT".indexOf(lastLetter) >= 0) {
                            x2 = 2 * cx - x2;
                            y2 = 2 * cy - y2;
                        } else {
                            x2 = cx;
                            y2 = cy;
                        }
                        x3 = sc.nextDouble();
                        y3 = sc.nextDouble();
                        double[] qp = quadraticPoints(cx, cy, x2, y2, x3, y3);
                        surface.path.curveTo(qp[0], qp[1], qp[2], qp[3], qp[4], qp[5]);
                        node.vertices.add(new double[]{pointAngle(cx, cy, x2, y2), pointAngle(x2, y2, x3, y3)});
                        cx = x3;
                        cy = y3;
                    }
                    case 't' -> {
                        if ("qtQT".indexOf(lastLetter) >= 0) {
                            x2 = 2 * cx - x2;
                            y2 = 2 * cy - y2;
                        } else {
                            x2 = cx;
                            y2 = cy;
                        }
                        double dx3 = sc.nextDouble(), dy3 = sc.nextDouble();
                        x3 = cx + dx3;
                        y3 = cy + dy3;
                        double[] qp = quadraticPoints(cx, cy, x2, y2, x3, y3);
                        surface.path.curveTo(qp[0], qp[1], qp[2], qp[3], qp[4], qp[5]);
                        node.vertices.add(new double[]{pointAngle(cx, cy, x2, y2), pointAngle(x2, y2, x3, y3)});
                        cx = x3;
                        cy = y3;
                    }
                    case 'A', 'a' -> {
                        double rx = sc.nextDouble(), ry = sc.nextDouble();
                        double rotation = Math.toRadians(sc.nextDouble());
                        int largeArc = sc.nextFlag();
                        int sweepFlag = sc.nextFlag();
                        if ((largeArc != 0 && largeArc != 1) || (sweepFlag != 0 && sweepFlag != 1)) {
                            continue;
                        }
                        double ex = sc.nextDouble(), ey = sc.nextDouble();
                        if (letter == 'a') {
                            ex += cx;
                            ey += cy;
                        }
                        double arcAngle = pointAngle(cx, cy, ex, ey);
                        node.vertices.add(new double[]{arcAngle, arcAngle});
                        drawArc(surface, cx, cy, rx, ry, rotation, largeArc != 0, sweepFlag != 0, ex, ey);
                        cx = ex;
                        cy = ey;
                    }
                    case 'Z', 'z' -> {
                        node.vertices.add(null);
                        surface.path.closePath();
                        if (firstPathPoint != null) {
                            cx = firstPathPoint[0];
                            cy = firstPathPoint[1];
                        }
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                LOG.log(System.Logger.Level.DEBUG, "Skipping invalid path token", e);
                sc.skipToken();
                continue;
            }

            if (letter != 'Z' && letter != 'z') {
                node.vertices.add(new double[]{cx, cy});
            }
            lastLetter = letter;
        }

        // Cache the result for subsequent renders of this node
        node.cachedPath = new GeneralPath(surface.path);
        node.cachedVertices = node.vertices;
    }

    /**
     * Zero-allocation, single-pass tokenizer for SVG path data. Handles implicit
     * sign separators (50-30 → 50, -30), consecutive decimals (0.5.3 → 0.5, 0.3),
     * comma/whitespace separators, and scientific notation. Replaces 4 regex passes
     * + N split() calls.
     */
    private static final class PathScanner {
        private final String d;
        private final int len;
        private int pos;

        PathScanner(String d) {
            this.d = d;
            this.len = d.length();
        }

        boolean hasMore() {
            int p = pos;
            while (p < len && isWsOrComma(d.charAt(p)))
                p++;
            return p < len;
        }

        /**
         * If the next non-ws char is a path command letter, consume and return it; else
         * return 0.
         */
        char peekCmd() {
            skipWs();
            if (pos < len) {
                char c = d.charAt(pos);
                if (PATH_LETTERS.indexOf(c) >= 0) {
                    pos++;
                    return c;
                }
            }
            return 0;
        }

        /** Parse the next floating-point number (handles implicit separators). */
        double nextDouble() {
            skipWs();
            int start = pos;
            if (pos < len && (d.charAt(pos) == '+' || d.charAt(pos) == '-'))
                pos++;
            boolean hasDot = false;
            while (pos < len) {
                char c = d.charAt(pos);
                if (c >= '0' && c <= '9') {
                    pos++;
                } else if (c == '.' && !hasDot) {
                    hasDot = true;
                    pos++;
                } else if ((c == 'e' || c == 'E') && pos > start) {
                    pos++;
                    if (pos < len && (d.charAt(pos) == '+' || d.charAt(pos) == '-'))
                        pos++;
                } else {
                    break;
                }
            }
            return Double.parseDouble(d.substring(start, pos));
        }

        /** Parse a single arc flag (0 or 1), skipping leading whitespace. */
        int nextFlag() {
            skipWs();
            int f = d.charAt(pos++) - '0';
            skipOptionalComma();
            return f;
        }

        /** Skip one token (for error recovery). */
        void skipToken() {
            skipWs();
            while (pos < len && !isWsOrComma(d.charAt(pos)))
                pos++;
        }

        private void skipWs() {
            while (pos < len && isWsOrComma(d.charAt(pos)))
                pos++;
        }

        private void skipOptionalComma() {
            while (pos < len && d.charAt(pos) == ' ')
                pos++;
            if (pos < len && d.charAt(pos) == ',')
                pos++;
        }

        private static boolean isWsOrComma(char c) {
            return c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r';
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

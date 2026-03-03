package io.brunoborges.jairosvg.dom;

import io.brunoborges.jairosvg.surface.Surface;
import io.brunoborges.jairosvg.util.ParsedPoint;

import static io.brunoborges.jairosvg.util.Helpers.*;

/**
 * Bounding box calculations for SVG shapes and paths. Port of CairoSVG
 * bounding_box.py
 */
public final class BoundingBox {

    /** Bounding box as (minX, minY, width, height). */
    public record Box(double minX, double minY, double width, double height) {
    }

    public static final Box EMPTY = new Box(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 0);

    private BoundingBox() {
    }

    public static Box calculate(Surface surface, Node node) {
        return switch (node.tag) {
            case "rect" -> rect(surface, node);
            case "circle" -> circle(surface, node);
            case "ellipse" -> ellipse(surface, node);
            case "line" -> line(surface, node);
            case "polyline", "polygon" -> polyline(surface, node);
            case "path" -> path(surface, node);
            case "g", "marker" -> group(surface, node);
            default -> null;
        };
    }

    public static Box rect(Surface surface, Node node) {
        double x = size(surface, node.get("x"), "x");
        double y = size(surface, node.get("y"), "y");
        double w = size(surface, node.get("width"), "x");
        double h = size(surface, node.get("height"), "y");
        return new Box(x, y, w, h);
    }

    public static Box circle(Surface surface, Node node) {
        double cx = size(surface, node.get("cx"), "x");
        double cy = size(surface, node.get("cy"), "y");
        double r = size(surface, node.get("r"));
        return new Box(cx - r, cy - r, 2 * r, 2 * r);
    }

    public static Box ellipse(Surface surface, Node node) {
        double rx = size(surface, node.get("rx"), "x");
        double ry = size(surface, node.get("ry"), "y");
        double cx = size(surface, node.get("cx"), "x");
        double cy = size(surface, node.get("cy"), "y");
        return new Box(cx - rx, cy - ry, 2 * rx, 2 * ry);
    }

    public static Box line(Surface surface, Node node) {
        double x1 = size(surface, node.get("x1"), "x");
        double y1 = size(surface, node.get("y1"), "y");
        double x2 = size(surface, node.get("x2"), "x");
        double y2 = size(surface, node.get("y2"), "y");
        double x = Math.min(x1, x2), y = Math.min(y1, y2);
        return new Box(x, y, Math.max(x1, x2) - x, Math.max(y1, y2) - y);
    }

    public static Box polyline(Surface surface, Node node) {
        String points = normalize(node.get("points", ""));
        Box box = EMPTY;
        while (!points.isBlank()) {
            ParsedPoint pt = pointWithRemainder(null, points);
            box = extendBox(box, pt.x(), pt.y());
            points = pt.remainder();
        }
        return box;
    }

    public static Box path(Surface surface, Node node) {
        String d = node.get("d", "");
        for (char c : PATH_LETTERS.toCharArray()) {
            d = d.replace(String.valueOf(c), " " + c + " ");
        }
        d = normalize(d);

        Box box = EMPTY;
        double px = 0, py = 0;
        String letter = "M";

        while (!d.isBlank()) {
            d = d.strip();
            String first = d.split("\\s+", 2)[0];
            if (first.length() == 1 && PATH_LETTERS.indexOf(first.charAt(0)) >= 0) {
                letter = first;
                d = d.substring(1).strip();
            }

            try {
                switch (letter) {
                    case "M", "L", "T", "m", "l", "t" -> {
                        ParsedPoint pt = pointWithRemainder(null, d);
                        double x = pt.x(), y = pt.y();
                        d = pt.remainder();
                        if ("m".equals(letter) || "l".equals(letter) || "t".equals(letter)) {
                            x += px;
                            y += py;
                        }
                        box = extendBox(box, x, y);
                        px = x;
                        py = y;
                    }
                    case "H", "h" -> {
                        String[] sp = (d + " ").split("\\s+", 2);
                        double x = Double.parseDouble(sp[0]);
                        d = sp[1];
                        if ("h".equals(letter))
                            x += px;
                        box = extendBox(box, x, py);
                        px = x;
                    }
                    case "V", "v" -> {
                        String[] sp = (d + " ").split("\\s+", 2);
                        double y = Double.parseDouble(sp[0]);
                        d = sp[1];
                        if ("v".equals(letter))
                            y += py;
                        box = extendBox(box, px, y);
                        py = y;
                    }
                    case "C", "c" -> {
                        ParsedPoint p1 = pointWithRemainder(null, d);
                        ParsedPoint p2 = pointWithRemainder(null, p1.remainder());
                        ParsedPoint p3 = pointWithRemainder(null, p2.remainder());
                        d = p3.remainder();
                        double x1 = p1.x(), y1 = p1.y();
                        double x2 = p2.x(), y2 = p2.y();
                        double x = p3.x(), y = p3.y();
                        if ("c".equals(letter)) {
                            x1 += px;
                            y1 += py;
                            x2 += px;
                            y2 += py;
                            x += px;
                            y += py;
                        }
                        box = extendBox(box, x1, y1);
                        box = extendBox(box, x2, y2);
                        box = extendBox(box, x, y);
                        px = x;
                        py = y;
                    }
                    case "S", "s", "Q", "q" -> {
                        ParsedPoint p1 = pointWithRemainder(null, d);
                        ParsedPoint p2 = pointWithRemainder(null, p1.remainder());
                        d = p2.remainder();
                        double x1 = p1.x(), y1 = p1.y();
                        double x = p2.x(), y = p2.y();
                        if ("s".equals(letter) || "q".equals(letter)) {
                            x1 += px;
                            y1 += py;
                            x += px;
                            y += py;
                        }
                        box = extendBox(box, x1, y1);
                        box = extendBox(box, x, y);
                        px = x;
                        py = y;
                    }
                    case "A", "a" -> {
                        ParsedPoint rxy = pointWithRemainder(null, d);
                        d = rxy.remainder();
                        // Skip rotation, large-arc, sweep
                        String[] parts = (d + " ").split("\\s+", 4);
                        d = parts.length > 3 ? parts[3] : "";
                        ParsedPoint ep = pointWithRemainder(null, d);
                        double x = ep.x(), y = ep.y();
                        d = ep.remainder();
                        if ("a".equals(letter)) {
                            x += px;
                            y += py;
                        }
                        box = extendBox(box, x, y);
                        px = x;
                        py = y;
                    }
                    case "Z", "z" -> {
                        // close path, nothing to extend
                    }
                    default -> {
                        // Unknown command, try to skip
                        if (!d.isEmpty()) {
                            String[] sp = d.split("\\s+", 2);
                            d = sp.length > 1 ? sp[1] : "";
                        }
                    }
                }
            } catch (Exception e) {
                break;
            }
            d = d.strip();
        }
        return box;
    }

    public static Box group(Surface surface, Node node) {
        Box box = EMPTY;
        for (Node child : node.children) {
            box = combine(box, calculate(surface, child));
        }
        return box;
    }

    public static Box extendBox(Box box, double x, double y) {
        double minX = Math.min(box.minX, x);
        double minY = Math.min(box.minY, y);
        double maxX = Double.isInfinite(box.minX) ? x : Math.max(box.minX + box.width, x);
        double maxY = Double.isInfinite(box.minY) ? y : Math.max(box.minY + box.height, y);
        return new Box(minX, minY, maxX - minX, maxY - minY);
    }

    public static Box combine(Box a, Box b) {
        if (b == null || !isValid(b))
            return a;
        Box result = extendBox(a, b.minX, b.minY);
        return extendBox(result, b.minX + b.width, b.minY + b.height);
    }

    public static boolean isValid(Box box) {
        return box != null && !Double.isInfinite(box.minX) && !Double.isInfinite(box.minY);
    }

    public static boolean isNonEmpty(Box box) {
        return isValid(box) && box.width != 0 && box.height != 0;
    }
}

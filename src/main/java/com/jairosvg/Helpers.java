package com.jairosvg;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Surface helpers: size parsing, transforms, normalize, point, etc.
 * Port of CairoSVG helpers.py
 */
public final class Helpers {

    public static final Map<String, Double> UNITS = Map.of(
        "mm", 1.0 / 25.4,
        "cm", 1.0 / 2.54,
        "in", 1.0,
        "pt", 1.0 / 72.0,
        "pc", 1.0 / 6.0
    );

    public static final Pattern PAINT_URL = Pattern.compile("(url\\(.+\\))\\s*(.*)");
    public static final String PATH_LETTERS = "achlmqstvzACHLMQSTVZ";
    private static final Pattern RECT_PATTERN = Pattern.compile("rect\\(\\s*(.+?)\\s*\\)");
    private static final Pattern NEGATIVE_SIGN = Pattern.compile("(?<!e)-");
    private static final Pattern WHITESPACE_COMMA = Pattern.compile("[\\s,]+");
    private static final Pattern DECIMAL_SPLIT = Pattern.compile("(\\.[0-9-]+)(?=\\.)");
    private static final Pattern POINT_PATTERN = Pattern.compile("^(\\S+?)\\s+(\\S+?)(?:\\s+|$)");
    private static final Pattern TRANSFORM_PATTERN = Pattern.compile("(\\w+)\\s*\\(\\s*(.*?)\\s*\\)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private Helpers() {}

    public static class PointError extends RuntimeException {
        public PointError() { super(); }
        public PointError(String msg) { super(msg); }
    }

    /** Distance between two points. */
    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    /** Extract URI and color from a paint value. Returns [source, color]. */
    public static String[] paint(String value) {
        if (value == null || value.isBlank()) {
            return new String[]{null, null};
        }
        value = value.strip();
        Matcher m = PAINT_URL.matcher(value);
        if (m.find()) {
            String source = UrlHelper.parseUrl(m.group(1)).fragment();
            String color = m.group(2).isEmpty() ? null : m.group(2);
            return new String[]{source, color};
        }
        return new String[]{null, value.isEmpty() ? null : value};
    }

    /** Return (width, height, viewbox) of a node. */
    public static double[] nodeFormat(Surface surface, Node node, boolean reference) {
        String refSize = reference ? "xy" : null;
        double width = size(surface, node.get("width", "100%"), refSize != null ? "x" : null);
        double height = size(surface, node.get("height", "100%"), refSize != null ? "y" : null);
        String viewboxStr = node.get("viewBox");
        double[] viewbox = null;
        if (viewboxStr != null && !viewboxStr.isEmpty()) {
            viewboxStr = WHITESPACE_COMMA.matcher(viewboxStr).replaceAll(" ").strip();
            String[] parts = viewboxStr.split(" ");
            if (parts.length == 4) {
                viewbox = new double[]{
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
                };
                if (width == 0) width = viewbox[2];
                if (height == 0) height = viewbox[3];
            }
        }
        return new double[]{width, height,
            viewbox != null ? viewbox[0] : Double.NaN,
            viewbox != null ? viewbox[1] : Double.NaN,
            viewbox != null ? viewbox[2] : Double.NaN,
            viewbox != null ? viewbox[3] : Double.NaN};
    }

    public static double[] nodeFormat(Surface surface, Node node) {
        return nodeFormat(surface, node, true);
    }

    /** Check if viewbox is present (not NaN). */
    public static boolean hasViewbox(double[] nodeFormat) {
        return !Double.isNaN(nodeFormat[2]);
    }

    /** Get viewbox part from nodeFormat result. Returns null if no viewbox. */
    public static double[] getViewbox(double[] nodeFormat) {
        if (!hasViewbox(nodeFormat)) return null;
        return new double[]{nodeFormat[2], nodeFormat[3], nodeFormat[4], nodeFormat[5]};
    }

    /** Normalize a string corresponding to an array of various values. */
    public static String normalize(String string) {
        if (string == null || string.isEmpty()) return "";
        string = string.replace('E', 'e');
        string = NEGATIVE_SIGN.matcher(string).replaceAll(" -");
        string = WHITESPACE_COMMA.matcher(string).replaceAll(" ");
        string = DECIMAL_SPLIT.matcher(string).replaceAll("$1 ");
        return string.strip();
    }

    /** Return (x, y, trailing_text) from string. */
    public static double[] point(Surface surface, String string) {
        string = string.strip();
        Matcher m = POINT_PATTERN.matcher(string);
        if (m.find()) {
            double x = size(surface, m.group(1), "x");
            double y = size(surface, m.group(2), "y");
            return new double[]{x, y};
        }
        throw new PointError("Cannot parse point from: " + string);
    }

    /** Return (x, y, remaining_string). */
    public static Object[] pointWithRemainder(Surface surface, String string) {
        string = string.strip();
        Matcher m = POINT_PATTERN.matcher(string);
        if (m.find()) {
            double x = size(surface, m.group(1), "x");
            double y = size(surface, m.group(2), "y");
            String remainder = string.substring(m.end()).strip();
            return new Object[]{x, y, remainder};
        }
        throw new PointError("Cannot parse point from: " + string);
    }

    /** Return angle between x axis and point knowing given center. */
    public static double pointAngle(double cx, double cy, double px, double py) {
        return Math.atan2(py - cy, px - cx);
    }

    /** Manage the ratio preservation. Returns [scaleX, scaleY, translateX, translateY]. */
    public static double[] preserveRatio(Surface surface, Node node,
                                         double width, double height) {
        double viewboxWidth, viewboxHeight;

        if ("marker".equals(node.tag)) {
            if (width == 0) width = size(surface, node.get("markerWidth", "3"), "x");
            if (height == 0) height = size(surface, node.get("markerHeight", "3"), "y");
            double[] nf = nodeFormat(surface, node);
            double[] vb = getViewbox(nf);
            if (vb == null) return new double[]{1, 1, 0, 0};
            viewboxWidth = vb[2];
            viewboxHeight = vb[3];
        } else if ("svg".equals(node.tag) || "image".equals(node.tag) || "g".equals(node.tag)) {
            double[] nf = nodeFormat(surface, node);
            if (width == 0) width = nf[0];
            if (height == 0) height = nf[1];
            viewboxWidth = node.imageWidth;
            viewboxHeight = node.imageHeight;
        } else {
            throw new IllegalArgumentException("Root node is " + node.tag +
                ". Should be one of marker, svg, image, or g.");
        }

        double translateX = 0, translateY = 0;
        double scaleX = viewboxWidth > 0 ? width / viewboxWidth : 1;
        double scaleY = viewboxHeight > 0 ? height / viewboxHeight : 1;

        String par = node.get("preserveAspectRatio", "xMidYMid");
        String[] parts = WHITESPACE.split(par);
        String align = parts[0];

        String xPosition, yPosition;
        if ("none".equals(align)) {
            xPosition = "min";
            yPosition = "min";
        } else {
            String meetOrSlice = parts.length > 1 ? parts[1] : null;
            double scaleValue;
            if ("slice".equals(meetOrSlice)) {
                scaleValue = Math.max(scaleX, scaleY);
            } else {
                scaleValue = Math.min(scaleX, scaleY);
            }
            scaleX = scaleY = scaleValue;
            xPosition = align.substring(1, 4).toLowerCase();
            yPosition = align.substring(5).toLowerCase();
        }

        if ("marker".equals(node.tag)) {
            translateX = -size(surface, node.get("refX", "0"), "x");
            translateY = -size(surface, node.get("refY", "0"), "y");
        } else {
            if ("mid".equals(xPosition)) {
                translateX = (width / scaleX - viewboxWidth) / 2;
            } else if ("max".equals(xPosition)) {
                translateX = width / scaleX - viewboxWidth;
            }
            if ("mid".equals(yPosition)) {
                translateY = (height / scaleY - viewboxHeight) / 2;
            } else if ("max".equals(yPosition)) {
                translateY = height / scaleY - viewboxHeight;
            }
        }

        return new double[]{scaleX, scaleY, translateX, translateY};
    }

    public static double[] preserveRatio(Surface surface, Node node) {
        return preserveRatio(surface, node, 0, 0);
    }

    /** Get clip (x, y, width, height) of marker box. */
    public static double[] clipMarkerBox(Surface surface, Node node,
                                         double scaleX, double scaleY) {
        double mw = size(surface, node.get("markerWidth", "3"), "x");
        double mh = size(surface, node.get("markerHeight", "3"), "y");
        double[] nf = nodeFormat(surface, node);
        double[] vb = getViewbox(nf);
        if (vb == null) return new double[]{0, 0, mw, mh};
        double vbW = vb[2], vbH = vb[3];

        String align = WHITESPACE.split(node.get("preserveAspectRatio", "xMidYMid"))[0];
        String xPos = "none".equals(align) ? "min" : align.substring(1, 4).toLowerCase();
        String yPos = "none".equals(align) ? "min" : align.substring(5).toLowerCase();

        double clipX = vb[0];
        if ("mid".equals(xPos)) clipX += (vbW - mw / scaleX) / 2.0;
        else if ("max".equals(xPos)) clipX += vbW - mw / scaleX;

        double clipY = vb[1];
        if ("mid".equals(yPos)) clipY += (vbH - mh / scaleY) / 2.0;
        else if ("max".equals(yPos)) clipY += vbH - mh / scaleY;

        return new double[]{clipX, clipY, mw / scaleX, mh / scaleY};
    }

    /** Return quadratic points for cubic curve approximation. */
    public static double[] quadraticPoints(double x1, double y1,
                                           double x2, double y2,
                                           double x3, double y3) {
        double xq1 = x2 * 2.0 / 3 + x1 / 3.0;
        double yq1 = y2 * 2.0 / 3 + y1 / 3.0;
        double xq2 = x2 * 2.0 / 3 + x3 / 3.0;
        double yq2 = y2 * 2.0 / 3 + y3 / 3.0;
        return new double[]{xq1, yq1, xq2, yq2, x3, y3};
    }

    /** Rotate a point by angle around origin. */
    public static double[] rotate(double x, double y, double angle) {
        return new double[]{
            x * Math.cos(angle) - y * Math.sin(angle),
            y * Math.cos(angle) + x * Math.sin(angle)
        };
    }

    /** Apply SVG transform string to the surface Graphics2D. */
    public static void transform(Surface surface, String transformString,
                                 AffineTransform gradient,
                                 String transformOrigin) {
        if (transformString == null || transformString.isEmpty()) return;

        String normalized = normalize(transformString);
        Matcher tm = TRANSFORM_PATTERN.matcher(normalized);

        AffineTransform matrix = new AffineTransform();

        // Handle transform-origin
        double originX = 0, originY = 0;
        boolean hasOrigin = false;
        if (transformOrigin != null && !transformOrigin.isEmpty()) {
            String[] origin = WHITESPACE.split(transformOrigin.strip());
            hasOrigin = true;
            originX = parseOriginComponent(surface, origin[0], true);
            originY = origin.length > 1
                ? parseOriginComponent(surface, origin[1], false)
                : surface.contextHeight / 2;
            matrix.translate(originX, originY);
        }

        while (tm.find()) {
            String type = tm.group(1);
            String[] valStrs = WHITESPACE.split(tm.group(2).strip());
            double[] values = new double[valStrs.length];
            for (int i = 0; i < valStrs.length; i++) {
                values[i] = size(surface, valStrs[i]);
            }

            switch (type) {
                case "matrix" -> {
                    if (values.length >= 6) {
                        AffineTransform m = new AffineTransform(
                            values[0], values[1], values[2],
                            values[3], values[4], values[5]);
                        matrix.concatenate(m);
                    }
                }
                case "rotate" -> {
                    double angle = Math.toRadians(values[0]);
                    double cx = values.length > 1 ? values[1] : 0;
                    double cy = values.length > 2 ? values[2] : 0;
                    matrix.translate(cx, cy);
                    matrix.rotate(angle);
                    matrix.translate(-cx, -cy);
                }
                case "skewX" -> {
                    double tangent = Math.tan(Math.toRadians(values[0]));
                    matrix.concatenate(new AffineTransform(1, 0, tangent, 1, 0, 0));
                }
                case "skewY" -> {
                    double tangent = Math.tan(Math.toRadians(values[0]));
                    matrix.concatenate(new AffineTransform(1, tangent, 0, 1, 0, 0));
                }
                case "translate" -> {
                    double tx = values[0];
                    double ty = values.length > 1 ? values[1] : 0;
                    matrix.translate(tx, ty);
                }
                case "scale" -> {
                    double sx = values[0];
                    double sy = values.length > 1 ? values[1] : sx;
                    matrix.scale(sx, sy);
                }
            }
        }

        if (hasOrigin) {
            matrix.translate(-originX, -originY);
        }

        if (gradient != null) {
            try {
                AffineTransform inv = matrix.createInverse();
                gradient.concatenate(inv);
            } catch (NoninvertibleTransformException e) {
                // Non-invertible, skip
            }
        } else {
            surface.context.transform(matrix);
        }
    }

    public static void transform(Surface surface, String transformString) {
        transform(surface, transformString, null, null);
    }

    /** Parse clip rect values. */
    public static String[] clipRect(String string) {
        if (string == null || string.isEmpty()) return new String[0];
        Matcher m = RECT_PATTERN.matcher(normalize(string));
        if (m.find()) {
            return WHITESPACE.split(m.group(1));
        }
        return new String[0];
    }

    /** Get original rotation values from a node. */
    public static List<Double> rotations(Node node) {
        String rotate = node.get("rotate");
        if (rotate != null && !rotate.isEmpty()) {
            List<Double> result = new ArrayList<>();
            for (String s : WHITESPACE.split(normalize(rotate).strip())) {
                result.add(Double.parseDouble(s));
            }
            return result;
        }
        return new ArrayList<>();
    }

    /** Pop rotation values already used. */
    public static void popRotation(Node node, List<Double> originalRotate, List<Double> rotate) {
        String text = node.text;
        if (text == null) text = "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) sb.append(' ');
            double r = !rotate.isEmpty() ? rotate.remove(0) : originalRotate.get(originalRotate.size() - 1);
            sb.append(r);
        }
        node.set("rotate", sb.toString());
    }

    /** Flatten text of a node and its children. */
    public static String flatten(org.w3c.dom.Element node) {
        StringBuilder sb = new StringBuilder();
        if (node.getTextContent() != null) {
            sb.append(node.getTextContent());
        }
        return sb.toString();
    }

    /**
     * Replace a string with units by a float value.
     * Reference: 'x' = viewport width, 'y' = viewport height, 'xy' = diagonal
     */
    public static double size(Surface surface, String string, String reference) {
        if (string == null || string.isEmpty()) return 0;

        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            // Not a plain number
        }

        if (surface == null) return 0;

        String normalized = normalize(string);
        int spaceIdx = normalized.indexOf(' ');
        string = spaceIdx > 0 ? normalized.substring(0, spaceIdx) : normalized;

        if (string.endsWith("%")) {
            double ref;
            if ("x".equals(reference)) {
                ref = surface.contextWidth;
            } else if ("y".equals(reference)) {
                ref = surface.contextHeight;
            } else {
                ref = Math.hypot(surface.contextWidth, surface.contextHeight) / Math.sqrt(2);
            }
            return Double.parseDouble(string.substring(0, string.length() - 1)) * ref / 100;
        } else if (string.endsWith("em")) {
            return surface.fontSize * Double.parseDouble(string.substring(0, string.length() - 2));
        } else if (string.endsWith("ex")) {
            return surface.fontSize * Double.parseDouble(string.substring(0, string.length() - 2)) / 2;
        } else if (string.endsWith("ch")) {
            return surface.fontSize * Double.parseDouble(string.substring(0, string.length() - 2)) / 2;
        }

        for (var entry : UNITS.entrySet()) {
            if (string.endsWith(entry.getKey())) {
                String numStr = string.substring(0, string.length() - entry.getKey().length());
                double num = Double.parseDouble(numStr);
                return num * surface.dpi * entry.getValue();
            }
        }

        // px or unknown
        if (string.endsWith("px")) {
            return Double.parseDouble(string.substring(0, string.length() - 2));
        }

        return 0;
    }

    /** Size with no reference. */
    public static double size(Surface surface, String string) {
        return size(surface, string, "xy");
    }

    /** Parse font shorthand property. */
    public static Map<String, String> parseFont(String value) {
        var result = new java.util.HashMap<>(Map.of(
            "font-family", "",
            "font-size", "",
            "font-style", "normal",
            "font-variant", "normal",
            "font-weight", "normal",
            "line-height", "normal"
        ));

        var fontStyles = List.of("italic", "oblique");
        var fontVariants = List.of("small-caps");
        var fontWeights = List.of("bold", "bolder", "lighter",
            "100", "200", "300", "400", "500", "600", "700", "800", "900");

        for (String element : WHITESPACE.split(value)) {
            if ("normal".equals(element)) continue;
            if (!result.get("font-family").isEmpty()) {
                result.put("font-family", result.get("font-family") + " " + element);
            } else if (fontStyles.contains(element)) {
                result.put("font-style", element);
            } else if (fontVariants.contains(element)) {
                result.put("font-variant", element);
            } else if (fontWeights.contains(element)) {
                result.put("font-weight", element);
            } else {
                if (result.get("font-size").isEmpty()) {
                    String[] fontParts = element.split("/");
                    result.put("font-size", fontParts[0]);
                    if (fontParts.length > 1) {
                        result.put("line-height", fontParts[1]);
                    }
                } else {
                    result.put("font-family", element);
                }
            }
        }
        return result;
    }

    private static double parseOriginComponent(Surface surface, String value, boolean isX) {
        return switch (value) {
            case "center" -> isX ? surface.contextWidth / 2 : surface.contextHeight / 2;
            case "left" -> 0;
            case "right" -> surface.contextWidth;
            case "top" -> 0;
            case "bottom" -> surface.contextHeight;
            default -> size(surface, value, isX ? "x" : "y");
        };
    }
}

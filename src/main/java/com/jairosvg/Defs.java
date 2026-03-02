package com.jairosvg;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import static com.jairosvg.Helpers.*;

/**
 * SVG definitions: gradients, patterns, clips, masks, filters, markers, use.
 * Port of CairoSVG defs.py
 */
public final class Defs {

    private Defs() {}

    /** Recursively parse all definition elements. */
    public static void parseAllDefs(Surface surface, Node node) {
        parseDef(surface, node);
        for (Node child : node.children) {
            parseAllDefs(surface, child);
        }
    }

    /** Parse a single definition element. */
    public static void parseDef(Surface surface, Node node) {
        String tag = node.tag.toLowerCase();

        // SVG fonts are keyed by font-family, not by id, so handle before the id check
        if (tag.equals("font")) {
            SvgFont svgFont = SvgFont.parse(node);
            if (svgFont != null) {
                surface.fonts.put(svgFont.family, svgFont);
            }
        }

        String id = node.get("id");
        if (id == null) return;

        if (tag.contains("marker")) surface.markers.put(id, node);
        if (tag.contains("gradient")) surface.gradients.put(id, node);
        if (tag.contains("pattern")) surface.patterns.put(id, node);
        if (tag.contains("mask")) surface.masks.put(id, node);
        if (tag.contains("filter")) surface.filters.put(id, node);
        if (tag.contains("image")) surface.images.put(id, node);
        if (tag.equals("clippath")) surface.paths.put(id, node);
    }

    /** Apply gradient or pattern color. Returns true if a gradient/pattern was applied. */
    public static boolean gradientOrPattern(Surface surface, Node node,
                                            String name, double opacity) {
        if (name == null) return false;
        if (surface.gradients.containsKey(name)) {
            return drawGradient(surface, node, name, opacity);
        }
        if (surface.patterns.containsKey(name)) {
            return drawPattern(surface, node, name, opacity);
        }
        return false;
    }

    /** Draw a gradient. */
    public static boolean drawGradient(Surface surface, Node node,
                                       String name, double opacity) {
        Node gradientNode = surface.gradients.get(name);
        if (gradientNode == null) return false;

        // Follow href chain
        String href = gradientNode.getHref();
        if (href != null && !href.isEmpty()) {
            String refId = UrlHelper.parseUrl(href).fragment();
            if (refId != null && surface.gradients.containsKey(refId)) {
                Node refNode = surface.gradients.get(refId);
                // Inherit stops if this gradient has none
                if (gradientNode.children.isEmpty()) {
                    gradientNode.children = new ArrayList<>(refNode.children);
                }
                // Inherit attributes
                for (var entry : refNode.entries()) {
                    if (!gradientNode.has(entry.getKey())) {
                        gradientNode.set(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        boolean userSpace = "userSpaceOnUse".equals(gradientNode.get("gradientUnits"));

        // Collect stops
        java.util.List<float[]> stops = new ArrayList<>();
        java.util.List<Color> colors = new ArrayList<>();
        float lastOffset = 0;

        for (Node child : gradientNode.children) {
            if (!"stop".equals(child.tag)) continue;
            float offset = (float) size(surface, child.get("offset"), null);
            if (child.get("offset", "").contains("%")) {
                offset = offset; // already parsed as fraction by size()
            }
            offset = Math.max(lastOffset, Math.min(1, offset));
            lastOffset = offset;

            Colors.RGBA rgba = surface.mapColor(
                child.get("stop-color", "black"),
                parseDouble(child.get("stop-opacity", "1")) * opacity);
            stops.add(new float[]{offset});
            colors.add(new Color((float) rgba.r(), (float) rgba.g(),
                                 (float) rgba.b(), (float) rgba.a()));
        }

        if (stops.isEmpty()) return false;

        // Ensure we have at least 2 stops
        if (stops.size() == 1) {
            stops.add(new float[]{1.0f});
            colors.add(colors.get(0));
        }

        Paint paint;

        if ("linearGradient".equals(gradientNode.tag)) {
            String ref = userSpace ? "x" : null;
            float x1, y1, x2, y2;

            if (userSpace) {
                x1 = (float) size(surface, gradientNode.get("x1", "0%"), "x");
                y1 = (float) size(surface, gradientNode.get("y1", "0%"), "y");
                x2 = (float) size(surface, gradientNode.get("x2", "100%"), "x");
                y2 = (float) size(surface, gradientNode.get("y2", "0%"), "y");
            } else {
                BoundingBox.Box bb = BoundingBox.calculate(surface, node);
                if (bb == null || !BoundingBox.isNonEmpty(bb)) return false;

                float pctX1 = parsePercent(gradientNode.get("x1", "0%"));
                float pctY1 = parsePercent(gradientNode.get("y1", "0%"));
                float pctX2 = parsePercent(gradientNode.get("x2", "100%"));
                float pctY2 = parsePercent(gradientNode.get("y2", "0%"));

                x1 = (float) (bb.minX() + pctX1 * bb.width());
                y1 = (float) (bb.minY() + pctY1 * bb.height());
                x2 = (float) (bb.minX() + pctX2 * bb.width());
                y2 = (float) (bb.minY() + pctY2 * bb.height());
            }

            if (x1 == x2 && y1 == y2) {
                // Degenerate gradient - use last color
                surface.context.setColor(colors.get(colors.size() - 1));
                return true;
            }

            float[] fractions = new float[stops.size()];
            Color[] colorArr = colors.toArray(new Color[0]);
            for (int i = 0; i < stops.size(); i++) fractions[i] = stops.get(i)[0];

            // Fix duplicate fractions, clamp to [0,1], ensure strictly increasing
            for (int i = 1; i < fractions.length; i++) {
                if (fractions[i] <= fractions[i - 1]) {
                    fractions[i] = fractions[i - 1] + 0.0001f;
                }
            }
            for (int i = 0; i < fractions.length; i++) {
                fractions[i] = Math.min(1.0f, Math.max(0.0f, fractions[i]));
            }
            for (int i = fractions.length - 2; i >= 0; i--) {
                if (fractions[i] >= fractions[i + 1]) {
                    fractions[i] = Math.nextDown(fractions[i + 1]);
                }
            }

            paint = new LinearGradientPaint(x1, y1, x2, y2, fractions, colorArr,
                getSpreadMethod(gradientNode.get("spreadMethod", "pad")));

        } else if ("radialGradient".equals(gradientNode.tag)) {
            float cx, cy, r, fx, fy;

            if (userSpace) {
                r = (float) size(surface, gradientNode.get("r", "50%"), "xy");
                cx = (float) size(surface, gradientNode.get("cx", "50%"), "x");
                cy = (float) size(surface, gradientNode.get("cy", "50%"), "y");
                fx = (float) size(surface, gradientNode.get("fx", String.valueOf(cx)), "x");
                fy = (float) size(surface, gradientNode.get("fy", String.valueOf(cy)), "y");
            } else {
                BoundingBox.Box bb = BoundingBox.calculate(surface, node);
                if (bb == null || !BoundingBox.isNonEmpty(bb)) return false;

                float pctR = parsePercent(gradientNode.get("r", "50%"));
                float pctCx = parsePercent(gradientNode.get("cx", "50%"));
                float pctCy = parsePercent(gradientNode.get("cy", "50%"));

                cx = (float) (bb.minX() + pctCx * bb.width());
                cy = (float) (bb.minY() + pctCy * bb.height());
                r = (float) (pctR * Math.max(bb.width(), bb.height()));

                String fxStr = gradientNode.get("fx");
                String fyStr = gradientNode.get("fy");
                fx = fxStr != null ? (float) (bb.minX() + parsePercent(fxStr) * bb.width()) : cx;
                fy = fyStr != null ? (float) (bb.minY() + parsePercent(fyStr) * bb.height()) : cy;
            }

            if (r <= 0) return false;

            // Ensure focus is within radius
            double dist = Math.hypot(fx - cx, fy - cy);
            if (dist >= r) {
                double scale = (r * 0.99) / dist;
                fx = (float) (cx + (fx - cx) * scale);
                fy = (float) (cy + (fy - cy) * scale);
            }

            float[] fractions = new float[stops.size()];
            Color[] colorArr = colors.toArray(new Color[0]);
            for (int i = 0; i < stops.size(); i++) fractions[i] = stops.get(i)[0];

            for (int i = 1; i < fractions.length; i++) {
                if (fractions[i] <= fractions[i - 1]) {
                    fractions[i] = fractions[i - 1] + 0.0001f;
                }
            }
            for (int i = 0; i < fractions.length; i++) {
                fractions[i] = Math.min(1.0f, Math.max(0.0f, fractions[i]));
            }
            for (int i = fractions.length - 2; i >= 0; i--) {
                if (fractions[i] >= fractions[i + 1]) {
                    fractions[i] = Math.nextDown(fractions[i + 1]);
                }
            }

            paint = new RadialGradientPaint(
                new Point2D.Float(cx, cy), r,
                new Point2D.Float(fx, fy),
                fractions, colorArr,
                getSpreadMethod(gradientNode.get("spreadMethod", "pad")));
        } else {
            return false;
        }

        surface.context.setPaint(paint);
        return true;
    }

    /** Draw a pattern (simplified). */
    public static boolean drawPattern(Surface surface, Node node,
                                      String name, double opacity) {
        // Pattern rendering is complex - provide basic support
        Node patternNode = surface.patterns.get(name);
        if (patternNode == null) return false;

        // For now, try to render the first child's fill color
        if (!patternNode.children.isEmpty()) {
            Node firstChild = patternNode.children.get(0);
            String fill = firstChild.get("fill");
            if (fill != null) {
                Colors.RGBA rgba = Colors.color(fill, opacity);
                surface.context.setColor(new Color(
                    (float) rgba.r(), (float) rgba.g(),
                    (float) rgba.b(), (float) rgba.a()));
                return true;
            }
        }
        return false;
    }

    /** Handle clip-path. */
    public static void clipPath(Surface surface, Node node) {
        String id = node.get("id");
        if (id != null) {
            surface.paths.put(id, node);
        }
    }

    /** Handle use element. */
    public static void use(Surface surface, Node node) {
        double x = size(surface, node.get("x"), "x");
        double y = size(surface, node.get("y"), "y");

        String href = node.getHref();
        if (href == null) return;

        String fragment = UrlHelper.parseUrl(href).fragment();
        if (fragment == null) return;

        // Find referenced element by ID in the tree
        Node refNode = findNodeById(surface.rootNode, fragment);
        if (refNode == null) return;

        var savedTransform = surface.context.getTransform();
        surface.context.translate(x, y);

        // If it's an svg or symbol, treat as svg
        if ("svg".equals(refNode.tag) || "symbol".equals(refNode.tag)) {
            String origTag = refNode.tag;
            refNode.tag = "svg";
            if (node.has("width") && node.has("height")) {
                refNode.set("width", node.get("width"));
                refNode.set("height", node.get("height"));
            }
            surface.draw(refNode);
            refNode.tag = origTag;
        } else {
            surface.draw(refNode);
        }

        surface.context.setTransform(savedTransform);
    }

    /** Handle filter (simplified - mainly feOffset). */
    public static void prepareFilter(Surface surface, Node node, String name) {
        Node filterNode = surface.filters.get(name);
        if (filterNode == null) return;

        for (Node child : filterNode.children) {
            if ("feOffset".equals(child.tag)) {
                double dx = size(surface, child.get("dx", "0"));
                double dy = size(surface, child.get("dy", "0"));
                surface.context.translate(dx, dy);
            }
        }
    }

    /** Handle mask (simplified). */
    public static void paintMask(Surface surface, Node node, String name, double opacity) {
        // Simplified mask - just apply opacity
        if (opacity < 1) {
            surface.context.setComposite(
                java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) opacity));
        }
    }

    /** Handle markers. */
    public static void marker(Surface surface, Node node) {
        parseDef(surface, node);
    }

    /** Handle mask definition. */
    public static void mask(Surface surface, Node node) {
        parseDef(surface, node);
    }

    /** Handle filter definition. */
    public static void filter(Surface surface, Node node) {
        parseDef(surface, node);
    }

    /** Handle gradient definitions. */
    public static void linearGradient(Surface surface, Node node) {
        parseDef(surface, node);
    }

    public static void radialGradient(Surface surface, Node node) {
        parseDef(surface, node);
    }

    /** Handle pattern definition. */
    public static void pattern(Surface surface, Node node) {
        parseDef(surface, node);
    }

    static Node findNodeById(Node root, String id) {
        if (root == null || id == null) return null;
        if (id.equals(root.get("id"))) return root;
        for (Node child : root.children) {
            Node found = findNodeById(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private static MultipleGradientPaint.CycleMethod getSpreadMethod(String method) {
        return switch (method) {
            case "reflect" -> MultipleGradientPaint.CycleMethod.REFLECT;
            case "repeat" -> MultipleGradientPaint.CycleMethod.REPEAT;
            default -> MultipleGradientPaint.CycleMethod.NO_CYCLE;
        };
    }

    private static float parsePercent(String s) {
        if (s == null) return 0;
        s = s.strip();
        if (s.endsWith("%")) {
            return Float.parseFloat(s.substring(0, s.length() - 1)) / 100f;
        }
        return Float.parseFloat(s);
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }
}

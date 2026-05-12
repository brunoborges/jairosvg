package io.brunoborges.jairosvg.draw;

import static io.brunoborges.jairosvg.util.Helpers.parseDouble;
import static io.brunoborges.jairosvg.util.Helpers.parsePercent;
import static io.brunoborges.jairosvg.util.Helpers.parseDouble;
import static io.brunoborges.jairosvg.util.Helpers.parsePercent;
import static io.brunoborges.jairosvg.util.Helpers.size;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import io.brunoborges.jairosvg.dom.BoundingBox;
import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.surface.Surface;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * Gradient rendering for linearGradient and radialGradient elements.
 */
public final class GradientPainter {

    private static final System.Logger LOG = System.getLogger(GradientPainter.class.getName());

    private GradientPainter() {
    }

    /** Draw a gradient. */
    public static boolean drawGradient(Surface surface, Node node, String name, double opacity) {
        Node gradientNode = surface.gradients.get(name);
        if (gradientNode == null)
            return false;

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

        // Check gradient stop cache
        Surface.GradientStops cached = surface.gradientStopCache.get(gradientNode);
        float[] fractions;
        Color[] colorArr;
        if (cached != null && cached.opacity() == opacity) {
            fractions = cached.fractions();
            colorArr = cached.colors();
        } else {
            // Collect stops
            java.util.List<float[]> stops = new ArrayList<>();
            java.util.List<Color> stopColors = new ArrayList<>();
            float lastOffset = 0;

            for (Node child : gradientNode.children) {
                if (!"stop".equals(child.tag))
                    continue;
                float offset = parsePercent(child.get("offset", "0"));
                offset = Math.max(lastOffset, Math.min(1, offset));
                lastOffset = offset;

                in.virit.color.Color stopColor = surface.mapColor(child.get("stop-color", "black"),
                        parseDouble(child.get("stop-opacity", "1")) * opacity);
                stops.add(new float[]{offset});
                stopColors.add(io.brunoborges.jairosvg.surface.Surface.toAwtColor(stopColor));
            }

            if (stops.isEmpty())
                return false;

            // Ensure we have at least 2 stops
            if (stops.size() == 1) {
                stops.add(new float[]{1.0f});
                stopColors.add(stopColors.get(0));
            }

            // Build fractions array (shared for both gradient types)
            fractions = new float[stops.size()];
            colorArr = stopColors.toArray(new Color[0]);
            for (int i = 0; i < stops.size(); i++)
                fractions[i] = stops.get(i)[0];

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

            surface.gradientStopCache.put(gradientNode, new Surface.GradientStops(fractions, colorArr, opacity));
        }

        Paint paint;

        if ("linearGradient".equals(gradientNode.tag)) {
            float x1, y1, x2, y2;

            if (userSpace) {
                x1 = (float) size(surface, gradientNode.get("x1", "0%"), "x");
                y1 = (float) size(surface, gradientNode.get("y1", "0%"), "y");
                x2 = (float) size(surface, gradientNode.get("x2", "100%"), "x");
                y2 = (float) size(surface, gradientNode.get("y2", "0%"), "y");
            } else {
                BoundingBox.Box bb = BoundingBox.calculate(surface, node);
                if (bb == null || !BoundingBox.isNonEmpty(bb))
                    return false;

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
                surface.context.setColor(colorArr[colorArr.length - 1]);
                return true;
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
                if (bb == null || !BoundingBox.isNonEmpty(bb))
                    return false;

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

            if (r <= 0)
                return false;

            // Ensure focus is within radius
            double dist = Math.hypot(fx - cx, fy - cy);
            if (dist >= r) {
                double scale = (r * 0.99) / dist;
                fx = (float) (cx + (fx - cx) * scale);
                fy = (float) (cy + (fy - cy) * scale);
            }

            paint = new RadialGradientPaint(new Point2D.Float(cx, cy), r, new Point2D.Float(fx, fy), fractions,
                    colorArr, getSpreadMethod(gradientNode.get("spreadMethod", "pad")));
        } else {
            return false;
        }

        surface.context.setPaint(paint);
        return true;
    }

    private static MultipleGradientPaint.CycleMethod getSpreadMethod(String method) {
        return switch (method) {
            case "reflect" -> MultipleGradientPaint.CycleMethod.REFLECT;
            case "repeat" -> MultipleGradientPaint.CycleMethod.REPEAT;
            default -> MultipleGradientPaint.CycleMethod.NO_CYCLE;
        };
    }

}

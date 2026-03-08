package io.brunoborges.jairosvg.draw;

import static io.brunoborges.jairosvg.util.Helpers.clipMarkerBox;
import static io.brunoborges.jairosvg.util.Helpers.pointAngle;
import static io.brunoborges.jairosvg.util.Helpers.preserveRatio;
import static io.brunoborges.jairosvg.util.Helpers.size;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

import io.brunoborges.jairosvg.css.Colors;
import io.brunoborges.jairosvg.dom.BoundingBox;
import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.dom.SvgFont;
import io.brunoborges.jairosvg.surface.Surface;
import io.brunoborges.jairosvg.util.Helpers;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * SVG definitions: gradients, patterns, clips, masks, filters, markers, use.
 * Port of CairoSVG defs.py
 */
public final class Defs {

    private static final int LUMINANCE_RED_COEFF_256 = 54;
    private static final int LUMINANCE_GREEN_COEFF_256 = 183;
    private static final int LUMINANCE_BLUE_COEFF_256 = 19;
    private static final int ALPHA_MAX = 255;
    private static final int MIN_IMAGE_BYTES = 5;

    private Defs() {
    }

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
        if (id == null)
            return;

        if (tag.contains("marker"))
            surface.markers.put(id, node);
        if (tag.contains("gradient"))
            surface.gradients.put(id, node);
        if (tag.contains("pattern"))
            surface.patterns.put(id, node);
        if (tag.contains("mask"))
            surface.masks.put(id, node);
        if (tag.contains("filter"))
            surface.filters.put(id, node);
        if (tag.contains("image"))
            surface.images.put(id, node);
        if (tag.equals("clippath"))
            surface.paths.put(id, node);
    }

    /**
     * Apply gradient or pattern color. Returns true if a gradient/pattern was
     * applied.
     */
    public static boolean gradientOrPattern(Surface surface, Node node, String name, double opacity) {
        if (name == null)
            return false;
        if (surface.gradients.containsKey(name)) {
            return drawGradient(surface, node, name, opacity);
        }
        if (surface.patterns.containsKey(name)) {
            return drawPattern(surface, node, name, opacity);
        }
        return false;
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

        // Collect stops
        java.util.List<float[]> stops = new ArrayList<>();
        java.util.List<Color> colors = new ArrayList<>();
        float lastOffset = 0;

        for (Node child : gradientNode.children) {
            if (!"stop".equals(child.tag))
                continue;
            float offset = parsePercent(child.get("offset", "0"));
            offset = Math.max(lastOffset, Math.min(1, offset));
            lastOffset = offset;

            Colors.RGBA rgba = surface.mapColor(child.get("stop-color", "black"),
                    parseDouble(child.get("stop-opacity", "1")) * opacity);
            stops.add(new float[]{offset});
            colors.add(new Color((float) rgba.r(), (float) rgba.g(), (float) rgba.b(), (float) rgba.a()));
        }

        if (stops.isEmpty())
            return false;

        // Ensure we have at least 2 stops
        if (stops.size() == 1) {
            stops.add(new float[]{1.0f});
            colors.add(colors.get(0));
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
                surface.context.setColor(colors.get(colors.size() - 1));
                return true;
            }

            float[] fractions = new float[stops.size()];
            Color[] colorArr = colors.toArray(new Color[0]);
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

            float[] fractions = new float[stops.size()];
            Color[] colorArr = colors.toArray(new Color[0]);
            for (int i = 0; i < stops.size(); i++)
                fractions[i] = stops.get(i)[0];

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

            paint = new RadialGradientPaint(new Point2D.Float(cx, cy), r, new Point2D.Float(fx, fy), fractions,
                    colorArr, getSpreadMethod(gradientNode.get("spreadMethod", "pad")));
        } else {
            return false;
        }

        surface.context.setPaint(paint);
        return true;
    }

    /** Draw a pattern. */
    public static boolean drawPattern(Surface surface, Node node, String name, double opacity) {
        Node patternNode = surface.patterns.get(name);
        if (patternNode == null)
            return false;

        // Follow href chain
        String href = patternNode.getHref();
        if (href != null && !href.isEmpty()) {
            String refId = UrlHelper.parseUrl(href).fragment();
            if (refId != null && surface.patterns.containsKey(refId)) {
                Node refNode = surface.patterns.get(refId);
                if (patternNode.children.isEmpty()) {
                    patternNode.children = new ArrayList<>(refNode.children);
                }
                for (var entry : refNode.entries()) {
                    if (!patternNode.has(entry.getKey())) {
                        patternNode.set(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        if (patternNode.children.isEmpty())
            return false;

        boolean userSpace = "userSpaceOnUse".equals(patternNode.get("patternUnits"));

        double patX, patY, patW, patH;
        if (userSpace) {
            patX = size(surface, patternNode.get("x", "0"), "x");
            patY = size(surface, patternNode.get("y", "0"), "y");
            patW = size(surface, patternNode.get("width", "0"), "x");
            patH = size(surface, patternNode.get("height", "0"), "y");
        } else {
            BoundingBox.Box bb = BoundingBox.calculate(surface, node);
            if (bb == null || !BoundingBox.isNonEmpty(bb))
                return false;

            double pctX = parsePercent(patternNode.get("x", "0"));
            double pctY = parsePercent(patternNode.get("y", "0"));
            double pctW = parsePercent(patternNode.get("width", "0"));
            double pctH = parsePercent(patternNode.get("height", "0"));

            patX = bb.minX() + pctX * bb.width();
            patY = bb.minY() + pctY * bb.height();
            patW = pctW * bb.width();
            patH = pctH * bb.height();
        }

        if (patW <= 0 || patH <= 0)
            return false;

        // Render pattern content into a BufferedImage
        int imgW = Math.min(4096, Math.max(1, (int) Math.ceil(patW)));
        int imgH = Math.min(4096, Math.max(1, (int) Math.ceil(patH)));
        BufferedImage patImage = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D patG2d = patImage.createGraphics();
        patG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        patG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        // Apply overall opacity to all pattern content
        patG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));

        // Save surface state
        Graphics2D savedContext = surface.context;
        GeneralPath savedPath = surface.path;
        double savedWidth = surface.contextWidth;
        double savedHeight = surface.contextHeight;

        surface.context = patG2d;
        surface.path = new GeneralPath();
        surface.contextWidth = patW;
        surface.contextHeight = patH;

        // Translate so pattern children (in user space) are drawn relative to the tile
        // origin
        if (patX != 0 || patY != 0) {
            patG2d.translate(-patX, -patY);
        }

        // Draw pattern children
        for (Node child : patternNode.children) {
            surface.draw(child);
        }

        patG2d.dispose();

        // Restore surface state
        surface.context = savedContext;
        surface.path = savedPath;
        surface.contextWidth = savedWidth;
        surface.contextHeight = savedHeight;

        // Create anchor rectangle (untransformed pattern region)
        Rectangle2D anchor = new Rectangle2D.Double(patX, patY, patW, patH);

        // Apply patternTransform by pre-transforming the pattern image and anchor,
        // so that rotation/skew are preserved in the tile pixels.
        BufferedImage paintImage = patImage;
        String ptStr = patternNode.get("patternTransform");
        if (ptStr != null && !ptStr.isEmpty()) {
            AffineTransform pt = Helpers.parseTransform(surface, ptStr);
            if (pt != null && !pt.isIdentity()) {
                // Compute transformed anchor bounds in user space
                Rectangle2D dstRect = pt.createTransformedShape(anchor).getBounds2D();

                int dstW = Math.max(1, (int) Math.ceil(dstRect.getWidth()));
                int dstH = Math.max(1, (int) Math.ceil(dstRect.getHeight()));

                BufferedImage transformedImage = new BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = transformedImage.createGraphics();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    // Map: input pixel → pattern space → user space → output pixel
                    AffineTransform at = new AffineTransform();
                    at.translate(-dstRect.getX(), -dstRect.getY());
                    at.concatenate(pt);
                    at.translate(patX, patY);
                    g2.drawImage(patImage, at, null);
                } finally {
                    g2.dispose();
                }
                paintImage = transformedImage;
                anchor = dstRect;
            }
        }

        // Create TexturePaint
        TexturePaint texturePaint = new TexturePaint(paintImage, anchor);
        surface.context.setPaint(texturePaint);
        return true;
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
        if (href == null)
            return;

        String fragment = UrlHelper.parseUrl(href).fragment();
        if (fragment == null)
            return;

        // Find referenced element by ID in the tree
        Node refNode = findNodeById(surface.rootNode, fragment);
        if (refNode == null)
            return;

        // Propagate inheritable presentation attributes from <use> to the referenced
        // node (SVG spec: <use> acts as parent for inheritance). Save originals to
        // restore after drawing.
        var notInherited = Node.notInheritedAttributes();
        Map<String, String> saved = new HashMap<>();
        for (var entry : node.entries()) {
            String key = entry.getKey();
            if (!notInherited.contains(key) && !refNode.has(key)) {
                saved.put(key, null);
                refNode.set(key, entry.getValue());
            }
        }

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

        // Restore: remove attributes that were injected
        for (var key : saved.keySet()) {
            refNode.remove(key);
        }
    }

    /** Handle filter preparation. */
    public static void prepareFilter(Surface surface, Node node, String name) {
        // Filter processing is handled after node rendering.
    }

    /** Apply filter primitives to an image. */
    public static BufferedImage applyFilter(Surface surface, String name, BufferedImage sourceGraphic) {
        Node filterNode = surface.filters.get(name);
        if (filterNode == null) {
            return sourceGraphic;
        }

        int fullW = sourceGraphic.getWidth();
        int fullH = sourceGraphic.getHeight();

        // Compute filter region once for primitives that need it (e.g. feTile)
        java.awt.Rectangle filterRegion = computeFilterRegion(sourceGraphic, filterNode);

        // Compute expanded processing region (includes blur/offset padding)
        java.awt.Rectangle subRegion = computeProcessingRegion(filterRegion, filterNode, fullW, fullH);

        // Use sub-region processing if it's significantly smaller than full canvas
        boolean usingSubRegion = subRegion != null
                && (long) subRegion.width * subRegion.height < (long) fullW * fullH * 4 / 5;
        int w, h;
        int offsetX = 0, offsetY = 0;
        BufferedImage workSource;

        if (usingSubRegion) {
            offsetX = subRegion.x;
            offsetY = subRegion.y;
            w = subRegion.width;
            h = subRegion.height;
            workSource = extractSubRegion(sourceGraphic, subRegion);
            // Adjust filter region to sub-region coordinates for feTile
            if (filterRegion != null) {
                filterRegion = new java.awt.Rectangle(filterRegion.x - offsetX, filterRegion.y - offsetY,
                        filterRegion.width, filterRegion.height);
            }
        } else {
            w = fullW;
            h = fullH;
            workSource = sourceGraphic;
        }

        BufferedImage buf1 = null;
        BufferedImage buf2 = null;
        BufferedImage buf3 = null;

        Map<String, BufferedImage> results = new HashMap<>();
        results.put("SourceGraphic", workSource);
        BufferedImage last = workSource;

        for (Node child : filterNode.children) {
            BufferedImage input = resolveInput(results, child.get("in"), last, workSource);
            BufferedImage output = switch (child.tag) {
                case "feGaussianBlur" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage temp = pickBuffer(input, null, buf1, buf2, buf3);
                    BufferedImage out = pickBuffer(input, temp, buf1, buf2, buf3);
                    yield gaussianBlur(input, parseDoubleOr(child.get("stdDeviation"), 0), temp, out);
                }
                case "feOffset" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield offset(input, size(surface, child.get("dx", "0")), size(surface, child.get("dy", "0")), out);
                }
                case "feFlood" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield flood(w, h, child.get("flood-color", "black"), parseDoubleOr(child.get("flood-opacity"), 1),
                            out);
                }
                case "feBlend" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage in2 = resolveInput(results, child.get("in2"), last, workSource);
                    BufferedImage out = pickBuffer(input, in2, buf1, buf2, buf3);
                    yield blend(input, in2, child.get("mode", "normal"), out);
                }
                case "feMerge" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield merge(results, child, w, h, last, out);
                }
                case "feDropShadow" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    boolean inputIsManaged = input == buf1 || input == buf2 || input == buf3;
                    if (inputIsManaged) {
                        yield dropShadow(surface, input, child);
                    }
                    yield dropShadowBuffered(surface, input, child, buf1, buf2, buf3);
                }
                case "feImage" -> feImage(surface, child, w, h);
                case "feTile" -> tile(input, filterRegion);
                default -> input;
            };
            String resultName = child.get("result");
            if (resultName != null && !resultName.isEmpty()) {
                results.put(resultName, cloneImage(output));
            }
            last = output;
            results.put("last", last);
        }

        if (usingSubRegion) {
            placeSubRegion(sourceGraphic, last, subRegion);
            return sourceGraphic;
        }
        return last;
    }

    /**
     * Compute the filter region from the sourceGraphic's non-transparent bounds,
     * extended by 10% on each side (SVG default filter region). If the filter node
     * has explicit x/y/width/height attributes, those override the default. Returns
     * a Rectangle, or null if the image is fully transparent.
     */
    public static java.awt.Rectangle computeFilterRegion(BufferedImage sourceGraphic, Node filterNode) {
        int w = sourceGraphic.getWidth();
        int h = sourceGraphic.getHeight();
        int[] pixels = ((DataBufferInt) sourceGraphic.getRaster().getDataBuffer()).getData();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            int off = y * w;
            for (int x = 0; x < w; x++) {
                if ((pixels[off + x] >>> 24) != 0) {
                    if (x < minX)
                        minX = x;
                    if (x > maxX)
                        maxX = x;
                    if (y < minY)
                        minY = y;
                    if (y > maxY)
                        maxY = y;
                }
            }
        }
        if (maxX < minX) {
            return null;
        }
        int bw = maxX - minX + 1;
        int bh = maxY - minY + 1;

        // Check for explicit filter region attributes on the <filter> element
        if (filterNode != null && filterNode.has("width") && filterNode.has("height")) {
            boolean userSpace = "userSpaceOnUse".equals(filterNode.get("filterUnits"));
            if (userSpace) {
                int fx = (int) parseDoubleOr(filterNode.get("x"), 0);
                int fy = (int) parseDoubleOr(filterNode.get("y"), 0);
                int fw = (int) parseDoubleOr(filterNode.get("width"), w);
                int fh = (int) parseDoubleOr(filterNode.get("height"), h);
                return new java.awt.Rectangle(fx, fy, fw, fh);
            }
            double pctX = parsePercentOrFraction(filterNode.get("x", "-10%"), -0.1);
            double pctY = parsePercentOrFraction(filterNode.get("y", "-10%"), -0.1);
            double pctW = parsePercentOrFraction(filterNode.get("width", "120%"), 1.2);
            double pctH = parsePercentOrFraction(filterNode.get("height", "120%"), 1.2);
            int fx = Math.max(0, (int) (minX + pctX * bw));
            int fy = Math.max(0, (int) (minY + pctY * bh));
            int fw = Math.min(w - fx, (int) (pctW * bw));
            int fh = Math.min(h - fy, (int) (pctH * bh));
            return new java.awt.Rectangle(fx, fy, fw, fh);
        }

        // Default: bbox + 10% padding
        int padX = Math.max(1, (int) Math.ceil(bw * 0.1));
        int padY = Math.max(1, (int) Math.ceil(bh * 0.1));
        return new java.awt.Rectangle(Math.max(0, minX - padX), Math.max(0, minY - padY), Math.min(w, bw + 2 * padX),
                Math.min(h, bh + 2 * padY));
    }

    private static double parsePercentOrFraction(String value, double defaultVal) {
        if (value == null || value.isEmpty()) {
            return defaultVal;
        }
        try {
            if (value.endsWith("%")) {
                return Double.parseDouble(value.substring(0, value.length() - 1)) / 100.0;
            }
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static BufferedImage pickBuffer(BufferedImage avoid1, BufferedImage avoid2, BufferedImage buf1,
            BufferedImage buf2, BufferedImage buf3) {
        if (buf1 != avoid1 && buf1 != avoid2) {
            return buf1;
        }
        if (buf2 != avoid1 && buf2 != avoid2) {
            return buf2;
        }
        return buf3;
    }

    private static void clearBuffer(BufferedImage buf) {
        java.util.Arrays.fill(((DataBufferInt) buf.getRaster().getDataBuffer()).getData(), 0);
    }

    private static BufferedImage cloneImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] srcData = ((DataBufferInt) source.getRaster().getDataBuffer()).getData();
        int[] dstData = ((DataBufferInt) copy.getRaster().getDataBuffer()).getData();
        System.arraycopy(srcData, 0, dstData, 0, srcData.length);
        return copy;
    }

    /**
     * Extract a sub-region from a full-canvas image into a smaller buffer.
     */
    private static BufferedImage extractSubRegion(BufferedImage src, java.awt.Rectangle region) {
        int fullW = src.getWidth();
        int w = region.width, h = region.height;
        BufferedImage sub = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] srcPixels = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        int[] subPixels = ((DataBufferInt) sub.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < h; y++) {
            System.arraycopy(srcPixels, (region.y + y) * fullW + region.x, subPixels, y * w, w);
        }
        return sub;
    }

    /**
     * Place a sub-region result back into a full-canvas image at the given offset.
     */
    private static void placeSubRegion(BufferedImage dst, BufferedImage sub, java.awt.Rectangle region) {
        int dstW = dst.getWidth();
        int subW = sub.getWidth(), subH = sub.getHeight();
        int[] dstPixels = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        int[] subPixels = ((DataBufferInt) sub.getRaster().getDataBuffer()).getData();
        // Clear the destination region
        for (int y = 0; y < region.height; y++) {
            java.util.Arrays.fill(dstPixels, (region.y + y) * dstW + region.x,
                    (region.y + y) * dstW + region.x + region.width, 0);
        }
        // Copy sub-region result back
        int copyW = Math.min(subW, region.width);
        int copyH = Math.min(subH, region.height);
        for (int y = 0; y < copyH; y++) {
            System.arraycopy(subPixels, y * subW, dstPixels, (region.y + y) * dstW + region.x, copyW);
        }
    }

    /**
     * Expand the filter region to include space for blur kernels and offset/shadow
     * effects. This ensures the sub-region is large enough to contain all filter
     * output without clipping.
     */
    private static java.awt.Rectangle computeProcessingRegion(java.awt.Rectangle filterRegion, Node filterNode,
            int fullW, int fullH) {
        if (filterRegion == null) {
            return null;
        }
        int blurPad = 0, dxPad = 0, dyPad = 0;
        if (filterNode != null) {
            for (Node child : filterNode.children) {
                switch (child.tag) {
                    case "feGaussianBlur" -> {
                        double sigma = parseDoubleOr(child.get("stdDeviation"), 0);
                        blurPad = Math.max(blurPad, (int) Math.ceil(sigma * 3));
                    }
                    case "feDropShadow" -> {
                        double sigma = parseDoubleOr(child.get("stdDeviation"), 0);
                        blurPad = Math.max(blurPad, (int) Math.ceil(sigma * 3));
                        dxPad = Math.max(dxPad, (int) Math.ceil(Math.abs(parseDoubleOr(child.get("dx"), 0))));
                        dyPad = Math.max(dyPad, (int) Math.ceil(Math.abs(parseDoubleOr(child.get("dy"), 0))));
                    }
                    case "feOffset" -> {
                        dxPad = Math.max(dxPad, (int) Math.ceil(Math.abs(parseDoubleOr(child.get("dx"), 0))));
                        dyPad = Math.max(dyPad, (int) Math.ceil(Math.abs(parseDoubleOr(child.get("dy"), 0))));
                    }
                    default -> {
                    }
                }
            }
        }
        int totalPadX = blurPad + dxPad;
        int totalPadY = blurPad + dyPad;
        int x = Math.max(0, filterRegion.x - totalPadX);
        int y = Math.max(0, filterRegion.y - totalPadY);
        int w = Math.min(fullW - x, filterRegion.width + 2 * totalPadX);
        int h = Math.min(fullH - y, filterRegion.height + 2 * totalPadY);
        return new java.awt.Rectangle(x, y, w, h);
    }

    /** Render and apply luminance mask to an off-screen source image. */
    public static BufferedImage paintMask(Surface surface, Node node, String name, BufferedImage sourceImage) {
        Node maskNode = surface.masks.get(name);
        if (maskNode == null) {
            return sourceImage;
        }

        int w = sourceImage.getWidth();
        int h = sourceImage.getHeight();

        // Reuse mask buffer from surface to avoid per-mask allocation
        BufferedImage maskImage = surface.maskBuffer;
        if (maskImage == null || maskImage.getWidth() != w || maskImage.getHeight() != h) {
            maskImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            surface.maskBuffer = maskImage;
        } else {
            java.util.Arrays.fill(((DataBufferInt) maskImage.getRaster().getDataBuffer()).getData(), 0);
        }
        Graphics2D maskG2d = maskImage.createGraphics();
        maskG2d.setRenderingHints(surface.context.getRenderingHints());

        Graphics2D savedContext = surface.context;
        GeneralPath savedPath = surface.path;
        double savedWidth = surface.contextWidth;
        double savedHeight = surface.contextHeight;

        surface.context = maskG2d;
        surface.path = new GeneralPath();
        surface.contextWidth = savedWidth;
        surface.contextHeight = savedHeight;

        for (Node child : maskNode.children) {
            surface.draw(child);
        }

        surface.context = savedContext;
        surface.path = savedPath;
        surface.contextWidth = savedWidth;
        surface.contextHeight = savedHeight;
        maskG2d.dispose();

        // Apply mask luminance to source alpha in-place
        int[] sourcePixels = ((DataBufferInt) sourceImage.getRaster().getDataBuffer()).getData();
        int[] maskPixels = ((DataBufferInt) maskImage.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < sourcePixels.length; i++) {
            int src = sourcePixels[i];
            int srcA = src >>> 24;
            if (srcA == 0) {
                continue;
            }
            int m = maskPixels[i];
            int ma = m >>> 24;
            if (ma == 0) {
                sourcePixels[i] = 0;
                continue;
            }
            int mr = (m >> 16) & 0xFF;
            int mg = (m >> 8) & 0xFF;
            int mb = m & 0xFF;
            int luminance256 = LUMINANCE_RED_COEFF_256 * mr + LUMINANCE_GREEN_COEFF_256 * mg
                    + LUMINANCE_BLUE_COEFF_256 * mb;
            if (luminance256 == 0) {
                sourcePixels[i] = 0;
                continue;
            }
            // Fast /255: two byte-scale multiplies instead of long division
            int luminance = (luminance256 + 128) >> 8;
            int maskAlpha = ma * luminance;
            maskAlpha = (maskAlpha + 1 + (maskAlpha >> 8)) >> 8;
            int combined = srcA * maskAlpha;
            int outA = (combined + 1 + (combined >> 8)) >> 8;
            sourcePixels[i] = (outA << 24) | (src & 0x00FFFFFF);
        }
        return sourceImage;
    }

    /** Handle markers. */
    public static void marker(Surface surface, Node node) {
        parseDef(surface, node);
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
        if (root == null || id == null)
            return null;
        if (id.equals(root.get("id")))
            return root;
        for (Node child : root.children) {
            Node found = findNodeById(child, id);
            if (found != null)
                return found;
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
        if (s == null)
            return 0;
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

    private static List<MarkerVertex> collectMarkerVertices(List<Object> vertices) {
        List<MarkerVertex> markerVertices = new ArrayList<>();
        boolean expectsPoint = true;
        MarkerVertex previous = null;
        for (Object vertex : vertices) {
            if (vertex == null) {
                expectsPoint = true;
                previous = null;
                continue;
            }
            if (!(vertex instanceof double[] values) || values.length != 2) {
                continue;
            }
            if (expectsPoint) {
                MarkerVertex current = new MarkerVertex(values[0], values[1]);
                markerVertices.add(current);
                if (previous != null) {
                    previous.outAngle = pointAngle(previous.x, previous.y, current.x, current.y);
                    current.inAngle = previous.outAngle;
                }
                previous = current;
                expectsPoint = false;
            } else {
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
            double angle = isEnd ? vertex.inAngle : vertex.outAngle;
            if (Double.isNaN(angle)) {
                angle = isEnd ? vertex.outAngle : vertex.inAngle;
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
    private static double parseDoubleOr(String s, double def) {
        if (s == null) {
            return def;
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static BufferedImage resolveInput(Map<String, BufferedImage> results, String in, BufferedImage last,
            BufferedImage sourceGraphic) {
        if (in == null || in.isEmpty()) {
            return last;
        }
        return switch (in) {
            case "SourceGraphic" -> sourceGraphic;
            default -> results.getOrDefault(in, last);
        };
    }

    private static BufferedImage offset(BufferedImage input, double dx, double dy, BufferedImage output) {
        clearBuffer(output);
        Graphics2D g = output.createGraphics();
        g.drawImage(input, (int) Math.round(dx), (int) Math.round(dy), null);
        g.dispose();
        return output;
    }

    private static BufferedImage flood(int width, int height, String color, double opacity, BufferedImage output) {
        Colors.RGBA floodColor = Colors.color(color, opacity);
        int a = Math.clamp((int) (floodColor.a() * 255 + 0.5), 0, 255);
        int r = Math.clamp((int) (floodColor.r() * 255 + 0.5), 0, 255);
        int g = Math.clamp((int) (floodColor.g() * 255 + 0.5), 0, 255);
        int b = Math.clamp((int) (floodColor.b() * 255 + 0.5), 0, 255);
        int pixel = (a << 24) | (r << 16) | (g << 8) | b;
        java.util.Arrays.fill(((DataBufferInt) output.getRaster().getDataBuffer()).getData(), pixel);
        return output;
    }

    private static final int BLEND_NORMAL = 0;
    private static final int BLEND_MULTIPLY = 1;
    private static final int BLEND_SCREEN = 2;
    private static final int BLEND_DARKEN = 3;
    private static final int BLEND_LIGHTEN = 4;

    // Precomputed reciprocals for alpha un-premultiply: ALPHA_RECIP[a] ≈
    // (1<<32)/(a*255).
    // Replaces per-channel integer division with multiply+shift in the blend loop.
    private static final long[] ALPHA_RECIP = new long[256];
    static {
        for (int a = 1; a <= 255; a++) {
            long divisor = a * 255L;
            ALPHA_RECIP[a] = ((1L << 32) + divisor - 1) / divisor;
        }
    }

    private static int blendModeId(String mode) {
        return switch (mode) {
            case "multiply" -> BLEND_MULTIPLY;
            case "screen" -> BLEND_SCREEN;
            case "darken" -> BLEND_DARKEN;
            case "lighten" -> BLEND_LIGHTEN;
            default -> BLEND_NORMAL;
        };
    }

    private static BufferedImage blend(BufferedImage input, BufferedImage destination, String mode,
            BufferedImage output) {
        int width = Math.max(input.getWidth(), destination.getWidth());
        int height = Math.max(input.getHeight(), destination.getHeight());
        boolean sameDimensions = input.getWidth() == width && input.getHeight() == height
                && destination.getWidth() == width && destination.getHeight() == height;

        if (sameDimensions) {
            return blendDirect(input, destination, blendModeId(mode), width, height, output);
        }

        if (output == null || output.getWidth() != width || output.getHeight() != height) {
            output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            clearBuffer(output);
        }
        int modeId = blendModeId(mode);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int src = x < input.getWidth() && y < input.getHeight() ? input.getRGB(x, y) : 0;
                int dst = x < destination.getWidth() && y < destination.getHeight() ? destination.getRGB(x, y) : 0;
                output.setRGB(x, y, blendPixel(src, dst, modeId));
            }
        }
        return output;
    }

    private static BufferedImage blendDirect(BufferedImage input, BufferedImage destination, int modeId, int width,
            int height, BufferedImage output) {
        int[] srcPixels = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] dstPixels = ((DataBufferInt) destination.getRaster().getDataBuffer()).getData();
        if (output == null || output.getWidth() != width || output.getHeight() != height) {
            output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        int[] outPixels = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        int len = srcPixels.length;

        switch (modeId) {
            case BLEND_NORMAL -> blendLoopNormal(srcPixels, dstPixels, outPixels, len);
            case BLEND_MULTIPLY -> blendLoopMultiply(srcPixels, dstPixels, outPixels, len);
            case BLEND_SCREEN -> blendLoopScreen(srcPixels, dstPixels, outPixels, len);
            case BLEND_DARKEN -> blendLoopDarken(srcPixels, dstPixels, outPixels, len);
            case BLEND_LIGHTEN -> blendLoopLighten(srcPixels, dstPixels, outPixels, len);
            default -> blendLoopNormal(srcPixels, dstPixels, outPixels, len);
        }
        return output;
    }

    private static void blendLoopNormal(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;
            if (outA > 255)
                outA = 255;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            // NORMAL: blended = src, so premul = f1*dst + 255*srcA*src
            int f1 = (255 - srcA) * dstA;
            int srcA255 = srcA * 255;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + srcA255 * srcR) * recip >>> 32);
            int outG = (int) ((f1 * dstG + srcA255 * srcG) * recip >>> 32);
            int outB = (int) ((f1 * dstB + srcA255 * srcB) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static void blendLoopMultiply(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;
            if (outA > 255)
                outA = 255;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int f2 = (255 - dstA) * srcA;
            int f3 = srcA * dstA;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + f2 * srcR + f3 * ((dstR * srcR + 127) / 255)) * recip >>> 32);
            int outG = (int) ((f1 * dstG + f2 * srcG + f3 * ((dstG * srcG + 127) / 255)) * recip >>> 32);
            int outB = (int) ((f1 * dstB + f2 * srcB + f3 * ((dstB * srcB + 127) / 255)) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static void blendLoopScreen(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;
            if (outA > 255)
                outA = 255;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int f2 = (255 - dstA) * srcA;
            int f3 = srcA * dstA;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + f2 * srcR + f3 * (dstR + srcR - (dstR * srcR + 127) / 255)) * recip >>> 32);
            int outG = (int) ((f1 * dstG + f2 * srcG + f3 * (dstG + srcG - (dstG * srcG + 127) / 255)) * recip >>> 32);
            int outB = (int) ((f1 * dstB + f2 * srcB + f3 * (dstB + srcB - (dstB * srcB + 127) / 255)) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static void blendLoopDarken(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;
            if (outA > 255)
                outA = 255;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int f2 = (255 - dstA) * srcA;
            int f3 = srcA * dstA;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + f2 * srcR + f3 * Math.min(dstR, srcR)) * recip >>> 32);
            int outG = (int) ((f1 * dstG + f2 * srcG + f3 * Math.min(dstG, srcG)) * recip >>> 32);
            int outB = (int) ((f1 * dstB + f2 * srcB + f3 * Math.min(dstB, srcB)) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static void blendLoopLighten(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;
            if (outA > 255)
                outA = 255;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int f2 = (255 - dstA) * srcA;
            int f3 = srcA * dstA;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + f2 * srcR + f3 * Math.max(dstR, srcR)) * recip >>> 32);
            int outG = (int) ((f1 * dstG + f2 * srcG + f3 * Math.max(dstG, srcG)) * recip >>> 32);
            int outB = (int) ((f1 * dstB + f2 * srcB + f3 * Math.max(dstB, srcB)) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static int blendPixel(int src, int dst, int modeId) {
        int srcA = (src >>> 24) & 0xFF;
        int dstA = (dst >>> 24) & 0xFF;
        if ((srcA | dstA) == 0) {
            return 0;
        }

        int srcR = (src >>> 16) & 0xFF;
        int srcG = (src >>> 8) & 0xFF;
        int srcB = src & 0xFF;
        int dstR = (dst >>> 16) & 0xFF;
        int dstG = (dst >>> 8) & 0xFF;
        int dstB = dst & 0xFF;

        // outA = srcA + dstA - srcA*dstA/255, scaled to 0-255
        int outA = srcA + dstA - (srcA * dstA + 127) / 255;
        if (outA <= 0) {
            return 0;
        }
        if (outA > 255) {
            outA = 255;
        }

        int outR = blendComposite(srcR, dstR, srcA, dstA, outA, modeId);
        int outG = blendComposite(srcG, dstG, srcA, dstA, outA, modeId);
        int outB = blendComposite(srcB, dstB, srcA, dstA, outA, modeId);

        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    private static int blendComposite(int src, int dst, int srcA, int dstA, int outA, int modeId) {
        int blended = switch (modeId) {
            case BLEND_MULTIPLY -> (dst * src + 127) / 255;
            case BLEND_SCREEN -> dst + src - (dst * src + 127) / 255;
            case BLEND_DARKEN -> Math.min(dst, src);
            case BLEND_LIGHTEN -> Math.max(dst, src);
            default -> src;
        };
        // SVG feBlend: (1 - srcA) * dstA * dst + (1 - dstA) * srcA * src + srcA * dstA
        // * blended
        // All in 0-255 space, divide by 255 for each alpha multiply, then by outA to
        // un-premultiply.
        int premul = (255 - srcA) * dstA * dst + (255 - dstA) * srcA * src + srcA * dstA * blended;
        int result = (premul + outA * 255 / 2) / (outA * 255);
        return Math.clamp(result, 0, 255);
    }

    private static BufferedImage merge(Map<String, BufferedImage> results, Node mergeNode, int width, int height,
            BufferedImage last, BufferedImage output) {
        clearBuffer(output);
        Graphics2D g = output.createGraphics();
        for (Node mergeChild : mergeNode.children) {
            if (!"feMergeNode".equals(mergeChild.tag)) {
                continue;
            }
            BufferedImage input = resolveInput(results, mergeChild.get("in"), last, results.get("SourceGraphic"));
            g.drawImage(input, 0, 0, null);
        }
        g.dispose();
        return output;
    }

    private static BufferedImage dropShadow(Surface surface, BufferedImage input, Node node) {
        int w = input.getWidth();
        int h = input.getHeight();
        BufferedImage buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        BufferedImage buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        BufferedImage buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        return dropShadowBuffered(surface, input, node, buf1, buf2, buf3);
    }

    private static BufferedImage dropShadowBuffered(Surface surface, BufferedImage input, Node node, BufferedImage buf1,
            BufferedImage buf2, BufferedImage buf3) {
        double stdDeviation = parseDouble(node.get("stdDeviation"));
        double dx = size(surface, node.get("dx", "0"));
        double dy = size(surface, node.get("dy", "0"));
        String floodColor = node.get("flood-color", "black");
        double floodOpacity = parseDoubleOr(node.get("flood-opacity"), 1);

        // Step 1: shadow colorization → buf1 (use pixel operations instead of
        // Graphics2D)
        Colors.RGBA rgba = Colors.color(floodColor, floodOpacity);
        int colorRGB = ((int) (rgba.r() * 255) << 16) | ((int) (rgba.g() * 255) << 8) | (int) (rgba.b() * 255);
        int[] inputPixels = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] shadowPixels = ((DataBufferInt) buf1.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < inputPixels.length; i++) {
            int alpha = inputPixels[i] >>> 24;
            shadowPixels[i] = (((int) (alpha * rgba.a()) << 24) | colorRGB);
        }

        // Step 2: blur shadow (buf1) → temp=buf2, output=buf3
        BufferedImage blurredShadow = gaussianBlur(buf1, stdDeviation, buf2, buf3);

        // Step 3: offset blurred shadow (buf3) → buf1 (shadow no longer needed)
        BufferedImage offsetShadow = offset(blurredShadow, dx, dy, buf1);

        // Step 4: composite offsetShadow + original input → buf2 (direct pixel copy)
        int[] shadowPixels2 = ((DataBufferInt) offsetShadow.getRaster().getDataBuffer()).getData();
        int[] inputPixels2 = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] outPixels = ((DataBufferInt) buf2.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < outPixels.length; i++) {
            int shadow = shadowPixels2[i];
            int orig = inputPixels2[i];
            int origAlpha = orig >>> 24;
            if (origAlpha == 255) {
                outPixels[i] = orig;
            } else if (origAlpha == 0) {
                outPixels[i] = shadow;
            } else {
                int shadowAlpha = shadow >>> 24;
                int outAlpha = origAlpha + (shadowAlpha * (255 - origAlpha) / 255);
                outPixels[i] = (outAlpha << 24) | (orig & 0x00FFFFFF);
            }
        }
        return buf2;
    }

    private static BufferedImage feImage(Surface surface, Node node, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        String href = node.getHref();
        if (href == null || href.isEmpty()) {
            return output;
        }

        UrlHelper.ParsedUrl parsedUrl = UrlHelper.parseUrl(href, resolveBaseUrl(node));
        String refId = parsedUrl.fragment();
        if (refId != null && !parsedUrl.hasNonFragmentParts()) {
            Node imageNode = surface.images.get(refId);
            if (imageNode != null) {
                return renderNode(surface, imageNode, output);
            }
            return output;
        }

        try {
            byte[] imageBytes = node.fetchUrl(parsedUrl, "image/*");
            if (imageBytes == null || imageBytes.length < MIN_IMAGE_BYTES) {
                return output;
            }
            var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(imageBytes));
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                return output;
            }
            Graphics2D g = output.createGraphics();
            g.setRenderingHints(surface.context.getRenderingHints());
            g.drawImage(image, 0, 0, width, height, null);
            g.dispose();
            return output;
        } catch (IOException e) {
            return output;
        }
    }

    private static String resolveBaseUrl(Node node) {
        String baseUrl = node.get("{http://www.w3.org/XML/1998/namespace}base");
        if (baseUrl == null && node.url != null) {
            int lastSlash = node.url.lastIndexOf('/');
            baseUrl = lastSlash >= 0 ? node.url.substring(0, lastSlash + 1) : null;
        }
        return baseUrl;
    }

    private static BufferedImage renderNode(Surface surface, Node node, BufferedImage output) {
        Graphics2D imageContext = output.createGraphics();
        imageContext.setRenderingHints(surface.context.getRenderingHints());

        Graphics2D savedContext = surface.context;
        GeneralPath savedPath = surface.path;
        double savedWidth = surface.contextWidth;
        double savedHeight = surface.contextHeight;

        surface.context = imageContext;
        surface.path = new GeneralPath();
        surface.contextWidth = output.getWidth();
        surface.contextHeight = output.getHeight();

        surface.draw(node);

        surface.context = savedContext;
        surface.path = savedPath;
        surface.contextWidth = savedWidth;
        surface.contextHeight = savedHeight;
        imageContext.dispose();
        return output;
    }

    private static BufferedImage tile(BufferedImage input, java.awt.Rectangle filterRegion) {
        int width = input.getWidth();
        int height = input.getHeight();
        int[] pixels = ((java.awt.image.DataBufferInt) input.getRaster().getDataBuffer()).getData();

        // Per SVG spec, the tile region is the input's filter primitive subregion.
        // Without per-primitive subregion tracking, we use the filter region as the
        // best approximation. When the filter region matches the canvas, this makes
        // feTile a pass-through (one tile fills everything), which is correct for
        // the common case of feTile on SourceGraphic with default subregions.
        int tileX, tileY, tileW, tileH;
        if (filterRegion != null) {
            tileX = Math.max(0, filterRegion.x);
            tileY = Math.max(0, filterRegion.y);
            tileW = Math.min(filterRegion.width, width - tileX);
            tileH = Math.min(filterRegion.height, height - tileY);
        } else {
            // Fallback: find non-transparent bounds
            int minX = width, minY = height, maxX = -1, maxY = -1;
            for (int y = 0; y < height; y++) {
                int rowOffset = y * width;
                for (int x = 0; x < width; x++) {
                    if ((pixels[rowOffset + x] >>> 24) != 0) {
                        if (x < minX)
                            minX = x;
                        if (x > maxX)
                            maxX = x;
                        if (y < minY)
                            minY = y;
                        if (y > maxY)
                            maxY = y;
                    }
                }
            }
            if (maxX < minX)
                return input;
            tileX = minX;
            tileY = minY;
            tileW = maxX - minX + 1;
            tileH = maxY - minY + 1;
        }

        if (tileW <= 0 || tileH <= 0) {
            return input;
        }

        // Extract tile pixels from the tile region
        int[] tilePixels = new int[tileW * tileH];
        for (int y = 0; y < tileH; y++) {
            System.arraycopy(pixels, (tileY + y) * width + tileX, tilePixels, y * tileW, tileW);
        }

        // Tile across the full canvas with origin at (tileX, tileY) so that the
        // original content stays at its original position
        int[] outPixels = new int[width * height];
        int txStart = ((-tileX % tileW) + tileW) % tileW;
        for (int y = 0; y < height; y++) {
            int ty = ((y - tileY) % tileH + tileH) % tileH;
            int tileRowOff = ty * tileW;
            int outRowOff = y * width;

            int x = 0;
            if (txStart != 0) {
                int firstLen = Math.min(tileW - txStart, width);
                System.arraycopy(tilePixels, tileRowOff + txStart, outPixels, outRowOff, firstLen);
                x = firstLen;
            }
            while (x + tileW <= width) {
                System.arraycopy(tilePixels, tileRowOff, outPixels, outRowOff + x, tileW);
                x += tileW;
            }
            if (x < width) {
                System.arraycopy(tilePixels, tileRowOff, outPixels, outRowOff + x, width - x);
            }
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        output.getRaster().setDataElements(0, 0, width, height, outPixels);
        return output;
    }

    private static BufferedImage gaussianBlur(BufferedImage input, double stdDeviation, BufferedImage temp,
            BufferedImage output) {
        if (stdDeviation <= 0) {
            return input;
        }
        int w = input.getWidth();
        int h = input.getHeight();
        int[] radii = boxRadii(stdDeviation);
        int[] src = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] tmp = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();
        int[] dst = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        // 3-pass box blur approximation of Gaussian (W3C recommended)
        boxBlurH(src, tmp, w, h, radii[0]);
        boxBlurV(tmp, dst, w, h, radii[0]);
        boxBlurH(dst, tmp, w, h, radii[1]);
        boxBlurV(tmp, dst, w, h, radii[1]);
        boxBlurH(dst, tmp, w, h, radii[2]);
        boxBlurV(tmp, dst, w, h, radii[2]);
        return output;
    }

    // Compute 3 box-blur radii that approximate a Gaussian with the given sigma.
    // See http://blog.ivank.net/fastest-gaussian-blur.html
    private static int[] boxRadii(double sigma) {
        double ideal = Math.sqrt((12 * sigma * sigma / 3) + 1);
        int wl = (int) Math.floor(ideal);
        if (wl % 2 == 0) {
            wl--;
        }
        int wu = wl + 2;
        int m = (int) Math.round((12 * sigma * sigma - 3.0 * wl * wl - 12.0 * wl - 9) / (-4.0 * wl - 4));
        return new int[]{(m > 0 ? wl : wu) / 2, (m > 1 ? wl : wu) / 2, (m > 2 ? wl : wu) / 2};
    }

    private static void boxBlurH(int[] src, int[] dst, int w, int h, int r) {
        if (r <= 0) {
            System.arraycopy(src, 0, dst, 0, src.length);
            return;
        }
        int boxSize = r + r + 1;
        int scale = (1 << 24) / boxSize;
        for (int y = 0; y < h; y++) {
            int off = y * w;
            int sa = 0, sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int px = src[off + Math.min(Math.max(i, 0), w - 1)];
                sa += (px >>> 24);
                sr += (px >> 16) & 0xFF;
                sg += (px >> 8) & 0xFF;
                sb += px & 0xFF;
            }
            int prev = dst[off] = ((sa * scale) & 0xFF000000) | (((sr * scale) & 0xFF000000) >>> 8)
                    | (((sg * scale) & 0xFF000000) >>> 16) | (((sb * scale) & 0xFF000000) >>> 24);
            for (int x = 1; x < w; x++) {
                int addPx = src[off + Math.min(x + r, w - 1)];
                int remPx = src[off + Math.max(x - r - 1, 0)];
                if (addPx == remPx) {
                    dst[off + x] = prev;
                } else {
                    sa += (addPx >>> 24) - (remPx >>> 24);
                    sr += ((addPx >> 16) & 0xFF) - ((remPx >> 16) & 0xFF);
                    sg += ((addPx >> 8) & 0xFF) - ((remPx >> 8) & 0xFF);
                    sb += (addPx & 0xFF) - (remPx & 0xFF);
                    prev = dst[off + x] = ((sa * scale) & 0xFF000000) | (((sr * scale) & 0xFF000000) >>> 8)
                            | (((sg * scale) & 0xFF000000) >>> 16) | (((sb * scale) & 0xFF000000) >>> 24);
                }
            }
        }
    }

    private static void boxBlurV(int[] src, int[] dst, int w, int h, int r) {
        if (r <= 0) {
            System.arraycopy(src, 0, dst, 0, src.length);
            return;
        }
        int boxSize = r + r + 1;
        int scale = (1 << 24) / boxSize;
        for (int x = 0; x < w; x++) {
            int sa = 0, sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int px = src[Math.min(Math.max(i, 0), h - 1) * w + x];
                sa += (px >>> 24);
                sr += (px >> 16) & 0xFF;
                sg += (px >> 8) & 0xFF;
                sb += px & 0xFF;
            }
            int prev = dst[x] = ((sa * scale) & 0xFF000000) | (((sr * scale) & 0xFF000000) >>> 8)
                    | (((sg * scale) & 0xFF000000) >>> 16) | (((sb * scale) & 0xFF000000) >>> 24);
            for (int y = 1; y < h; y++) {
                int addPx = src[Math.min(y + r, h - 1) * w + x];
                int remPx = src[Math.max(y - r - 1, 0) * w + x];
                if (addPx == remPx) {
                    dst[y * w + x] = prev;
                } else {
                    sa += (addPx >>> 24) - (remPx >>> 24);
                    sr += ((addPx >> 16) & 0xFF) - ((remPx >> 16) & 0xFF);
                    sg += ((addPx >> 8) & 0xFF) - ((remPx >> 8) & 0xFF);
                    sb += (addPx & 0xFF) - (remPx & 0xFF);
                    prev = dst[y * w + x] = ((sa * scale) & 0xFF000000) | (((sr * scale) & 0xFF000000) >>> 8)
                            | (((sg * scale) & 0xFF000000) >>> 16) | (((sb * scale) & 0xFF000000) >>> 24);
                }
            }
        }
    }
}

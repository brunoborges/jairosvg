package io.brunoborges.jairosvg.draw;

import static io.brunoborges.jairosvg.util.Helpers.parseDouble;
import static io.brunoborges.jairosvg.util.Helpers.parseDoubleOr;
import static io.brunoborges.jairosvg.util.Helpers.parseDouble;
import static io.brunoborges.jairosvg.util.Helpers.parseDoubleOr;
import static io.brunoborges.jairosvg.util.Helpers.size;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

import io.brunoborges.jairosvg.css.Colors;
import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.surface.Surface;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * SVG filter pipeline and filter primitive implementations.
 */
public final class FilterRenderer {

    private static final System.Logger LOG = System.getLogger(FilterRenderer.class.getName());
    private static final int MIN_IMAGE_BYTES = 5;

    private FilterRenderer() {
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

        // Pre-scan which named results are actually referenced by later primitives
        Set<String> referencedResults = new java.util.HashSet<>();
        for (Node child : filterNode.children) {
            String in = child.get("in");
            if (in != null && !in.isEmpty())
                referencedResults.add(in);
            String in2 = child.get("in2");
            if (in2 != null && !in2.isEmpty())
                referencedResults.add(in2);
            for (Node grandchild : child.children) {
                String mergeIn = grandchild.get("in");
                if (mergeIn != null && !mergeIn.isEmpty())
                    referencedResults.add(mergeIn);
            }
        }

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
                    yield GaussianBlur.apply(input, parseDoubleOr(child.get("stdDeviation"), 0), temp, out);
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
                    yield BlendCompositor.blend(input, in2, child.get("mode", "normal"), out);
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
                case "feImage" -> feImage(surface, child, w, h, offsetX, offsetY);
                case "feTile" -> tile(input, filterRegion);
                default -> input;
            };
            String resultName = child.get("result");
            if (resultName != null && !resultName.isEmpty() && referencedResults.contains(resultName)) {
                // Only clone if the output is a managed buffer that may be overwritten
                boolean isManaged = output == buf1 || output == buf2 || output == buf3;
                results.put(resultName, isManaged ? cloneImage(output) : output);
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

        // Short-circuit: if filter has explicit userSpaceOnUse region, skip pixel scan
        if (filterNode != null && filterNode.has("width") && filterNode.has("height")) {
            boolean userSpace = "userSpaceOnUse".equals(filterNode.get("filterUnits"));
            if (userSpace) {
                int fx = (int) parseDoubleOr(filterNode.get("x"), 0);
                int fy = (int) parseDoubleOr(filterNode.get("y"), 0);
                int fw = (int) parseDoubleOr(filterNode.get("width"), w);
                int fh = (int) parseDoubleOr(filterNode.get("height"), h);
                return new java.awt.Rectangle(fx, fy, fw, fh);
            }
        }

        // Scan for opaque bounds
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

        // Check for explicit percentage-based filter region
        if (filterNode != null && filterNode.has("width") && filterNode.has("height")) {
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

    // ── Private helpers ──────────────────────────────────────────────────

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

    // ── Filter primitives ────────────────────────────────────────────────

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

    // ── Merge / Drop Shadow / feImage / Tile ─────────────────────────────

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
        BufferedImage blurredShadow = GaussianBlur.apply(buf1, stdDeviation, buf2, buf3);

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

    private static BufferedImage feImage(Surface surface, Node node, int width, int height, int subRegionOffsetX,
            int subRegionOffsetY) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        String href = node.getHref();
        if (href == null || href.isEmpty()) {
            return output;
        }

        UrlHelper.ParsedUrl parsedUrl = UrlHelper.parseUrl(href, UrlHelper.resolveBaseUrl(node));
        String refId = parsedUrl.fragment();
        if (refId != null && !parsedUrl.hasNonFragmentParts()) {
            Node imageNode = surface.images.get(refId);
            if (imageNode != null) {
                return renderNode(surface, imageNode, output, subRegionOffsetX, subRegionOffsetY);
            }
            return output;
        }

        try {
            byte[] imageBytes = node.fetchUrl(parsedUrl, "image/*");
            if (imageBytes == null || imageBytes.length < MIN_IMAGE_BYTES) {
                return output;
            }
            var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(imageBytes));
            java.awt.image.BufferedImage image = ImageIO.read(input);
            if (image == null) {
                return output;
            }
            Graphics2D g = output.createGraphics();
            g.setRenderingHints(surface.context.getRenderingHints());
            g.drawImage(image, 0, 0, width, height, null);
            g.dispose();
            return output;
        } catch (IOException e) {
            LOG.log(System.Logger.Level.DEBUG, "Failed to load feImage URL: {0}", e.getMessage());
            return output;
        }
    }

    private static BufferedImage renderNode(Surface surface, Node node, BufferedImage output, int offsetX,
            int offsetY) {
        Graphics2D imageContext = output.createGraphics();
        imageContext.setRenderingHints(surface.context.getRenderingHints());
        if (offsetX != 0 || offsetY != 0) {
            imageContext.translate(-offsetX, -offsetY);
        }

        Graphics2D savedContext = surface.context;
        GeneralPath savedPath = surface.path;
        double savedWidth = surface.contextWidth;
        double savedHeight = surface.contextHeight;

        surface.context = imageContext;
        surface.path = new GeneralPath();
        surface.contextWidth = output.getWidth();
        surface.contextHeight = output.getHeight();

        try {
            surface.draw(node);
        } finally {
            surface.context = savedContext;
            surface.path = savedPath;
            surface.contextWidth = savedWidth;
            surface.contextHeight = savedHeight;
            imageContext.dispose();
        }
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
}

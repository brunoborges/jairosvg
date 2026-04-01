package io.brunoborges.jairosvg.draw;

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

    /** Apply filter primitives to an image with a pre-computed filter region. */
    public static BufferedImage applyFilter(Surface surface, String name, BufferedImage sourceGraphic,
            java.awt.Rectangle precomputedRegion) {
        Node filterNode = surface.filters.get(name);
        if (filterNode == null) {
            return sourceGraphic;
        }

        int fullW = sourceGraphic.getWidth();
        int fullH = sourceGraphic.getHeight();

        // Use pre-computed filter region if available, otherwise compute
        java.awt.Rectangle filterRegion = precomputedRegion != null
                ? precomputedRegion
                : computeFilterRegion(sourceGraphic, filterNode);

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

        // Reuse or allocate filter primitive buffers from the Surface cache
        BufferedImage buf1 = reuseOrNull(surface.filterBuf1, w, h);
        BufferedImage buf2 = reuseOrNull(surface.filterBuf2, w, h);
        BufferedImage buf3 = reuseOrNull(surface.filterBuf3, w, h);

        Map<String, BufferedImage> results = new HashMap<>();
        results.put("SourceGraphic", workSource);
        BufferedImage last = workSource;

        // Lazily create SourceAlpha if referenced
        BufferedImage sourceAlpha = null;

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
            // Resolve "SourceAlpha" lazily
            String inRef = child.get("in");
            if ("SourceAlpha".equals(inRef) && sourceAlpha == null) {
                sourceAlpha = createSourceAlpha(workSource);
                results.put("SourceAlpha", sourceAlpha);
            }
            String in2Ref = child.get("in2");
            if ("SourceAlpha".equals(in2Ref) && sourceAlpha == null) {
                sourceAlpha = createSourceAlpha(workSource);
                results.put("SourceAlpha", sourceAlpha);
            }
            BufferedImage input = resolveInput(results, inRef, last, workSource);
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
                case "feComposite" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage in2 = resolveInput(results, child.get("in2"), last, workSource);
                    BufferedImage out = pickBuffer(input, in2, buf1, buf2, buf3);
                    yield composite(input, in2, child.get("operator", "over"), parseDoubleOr(child.get("k1"), 0),
                            parseDoubleOr(child.get("k2"), 0), parseDoubleOr(child.get("k3"), 0),
                            parseDoubleOr(child.get("k4"), 0), out);
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
                case "feColorMatrix" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield colorMatrix(input, child.get("type", "matrix"), child.get("values", ""), out);
                }
                case "feComponentTransfer" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield componentTransfer(input, child, out);
                }
                case "feMorphology" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    String radiusStr = child.get("radius", "0");
                    String[] parts = radiusStr.strip().split("[\\s,]+");
                    double rx = parseDoubleOr(parts[0], 0);
                    double ry = parts.length > 1 ? parseDoubleOr(parts[1], 0) : rx;
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield morphology(input, child.get("operator", "erode"), rx, ry, out);
                }
                case "feConvolveMatrix" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield convolveMatrix(input, child, out);
                }
                case "feDisplacementMap" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage in2 = resolveInput(results, child.get("in2"), last, workSource);
                    BufferedImage out = pickBuffer(input, in2, buf1, buf2, buf3);
                    yield displacementMap(input, in2, parseDoubleOr(child.get("scale"), 0),
                            child.get("xChannelSelector", "A"), child.get("yChannelSelector", "A"), out);
                }
                case "feTurbulence" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    String freqStr = child.get("baseFrequency", "0");
                    String[] freqParts = freqStr.strip().split("[\\s,]+");
                    double freqX = parseDoubleOr(freqParts[0], 0);
                    double freqY = freqParts.length > 1 ? parseDoubleOr(freqParts[1], 0) : freqX;
                    yield turbulence(w, h, child.get("type", "turbulence"), freqX, freqY,
                            (int) parseDoubleOr(child.get("numOctaves"), 1), (int) parseDoubleOr(child.get("seed"), 0),
                            out);
                }
                case "feDiffuseLighting" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield diffuseLighting(input, child, out, offsetX, offsetY);
                }
                case "feSpecularLighting" -> {
                    if (buf1 == null)
                        buf1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf2 == null)
                        buf2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    if (buf3 == null)
                        buf3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage out = pickBuffer(input, null, buf1, buf2, buf3);
                    yield specularLighting(input, child, out, offsetX, offsetY);
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

        // Cache buffers back to Surface for reuse across filter invocations
        if (buf1 != null)
            surface.filterBuf1 = buf1;
        if (buf2 != null)
            surface.filterBuf2 = buf2;
        if (buf3 != null)
            surface.filterBuf3 = buf3;

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

        // Scan for opaque bounds — early-exit on row boundaries
        int[] pixels = ((DataBufferInt) sourceGraphic.getRaster().getDataBuffer()).getData();

        // Find minY: scan rows top-down, break at first non-transparent row
        int minY = -1;
        for (int y = 0; y < h; y++) {
            int off = y * w;
            for (int x = 0; x < w; x++) {
                if ((pixels[off + x] >>> 24) != 0) {
                    minY = y;
                    break;
                }
            }
            if (minY >= 0)
                break;
        }
        if (minY < 0) {
            return null; // fully transparent
        }

        // Find maxY: scan rows bottom-up
        int maxY = minY;
        for (int y = h - 1; y > minY; y--) {
            int off = y * w;
            for (int x = 0; x < w; x++) {
                if ((pixels[off + x] >>> 24) != 0) {
                    maxY = y;
                    break;
                }
            }
            if (maxY > minY)
                break;
        }

        // Find minX/maxX only within active rows
        int minX = w, maxX = -1;
        for (int y = minY; y <= maxY; y++) {
            int off = y * w;
            for (int x = 0; x < minX; x++) {
                if ((pixels[off + x] >>> 24) != 0) {
                    minX = x;
                    break;
                }
            }
            for (int x = w - 1; x > maxX; x--) {
                if ((pixels[off + x] >>> 24) != 0) {
                    maxX = x;
                    break;
                }
            }
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

    /**
     * Return cached buffer if dimensions match (cleared), else null for lazy init.
     */
    private static BufferedImage reuseOrNull(BufferedImage cached, int w, int h) {
        if (cached != null && cached.getWidth() == w && cached.getHeight() == h) {
            clearBuffer(cached);
            return cached;
        }
        return null;
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
            case "SourceAlpha" -> results.getOrDefault("SourceAlpha", sourceGraphic);
            default -> results.getOrDefault(in, last);
        };
    }

    /** Create a SourceAlpha image: same alpha channel, RGB set to 0. */
    private static BufferedImage createSourceAlpha(BufferedImage source) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage alpha = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] srcData = ((DataBufferInt) source.getRaster().getDataBuffer()).getData();
        int[] dstData = ((DataBufferInt) alpha.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < srcData.length; i++) {
            dstData[i] = srcData[i] & 0xFF000000; // keep only alpha
        }
        return alpha;
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

    private static BufferedImage composite(BufferedImage in1, BufferedImage in2, String operator, double k1, double k2,
            double k3, double k4, BufferedImage output) {
        int[] in1Data = ((DataBufferInt) in1.getRaster().getDataBuffer()).getData();
        int[] in2Data = ((DataBufferInt) in2.getRaster().getDataBuffer()).getData();
        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        int len = outData.length;

        for (int i = 0; i < len; i++) {
            int p1 = in1Data[i];
            int p2 = in2Data[i];

            int a1 = p1 >>> 24;
            int r1 = (p1 >> 16) & 0xFF;
            int g1 = (p1 >> 8) & 0xFF;
            int b1 = p1 & 0xFF;

            int a2 = p2 >>> 24;
            int r2 = (p2 >> 16) & 0xFF;
            int g2 = (p2 >> 8) & 0xFF;
            int b2 = p2 & 0xFF;

            // Convert to premultiplied alpha
            int pr1 = r1 * a1 / 255;
            int pg1 = g1 * a1 / 255;
            int pb1 = b1 * a1 / 255;
            int pr2 = r2 * a2 / 255;
            int pg2 = g2 * a2 / 255;
            int pb2 = b2 * a2 / 255;

            int oa, or, og, ob; // premultiplied output
            switch (operator) {
                case "in" -> {
                    oa = a1 * a2 / 255;
                    or = pr1 * a2 / 255;
                    og = pg1 * a2 / 255;
                    ob = pb1 * a2 / 255;
                }
                case "out" -> {
                    oa = a1 * (255 - a2) / 255;
                    or = pr1 * (255 - a2) / 255;
                    og = pg1 * (255 - a2) / 255;
                    ob = pb1 * (255 - a2) / 255;
                }
                case "atop" -> {
                    oa = a2;
                    or = (pr1 * a2 + pr2 * (255 - a1)) / 255;
                    og = (pg1 * a2 + pg2 * (255 - a1)) / 255;
                    ob = (pb1 * a2 + pb2 * (255 - a1)) / 255;
                }
                case "xor" -> {
                    oa = (a1 * (255 - a2) + a2 * (255 - a1)) / 255;
                    or = (pr1 * (255 - a2) + pr2 * (255 - a1)) / 255;
                    og = (pg1 * (255 - a2) + pg2 * (255 - a1)) / 255;
                    ob = (pb1 * (255 - a2) + pb2 * (255 - a1)) / 255;
                }
                case "arithmetic" -> {
                    oa = Math.clamp((int) (k1 * a1 * a2 / 255 + k2 * a1 + k3 * a2 + k4 * 255), 0, 255);
                    or = Math.clamp((int) (k1 * pr1 * pr2 / 255 + k2 * pr1 + k3 * pr2 + k4 * 255), 0, 255);
                    og = Math.clamp((int) (k1 * pg1 * pg2 / 255 + k2 * pg1 + k3 * pg2 + k4 * 255), 0, 255);
                    ob = Math.clamp((int) (k1 * pb1 * pb2 / 255 + k2 * pb1 + k3 * pb2 + k4 * 255), 0, 255);
                }
                default -> { // "over"
                    oa = a1 + a2 * (255 - a1) / 255;
                    or = pr1 + pr2 * (255 - a1) / 255;
                    og = pg1 + pg2 * (255 - a1) / 255;
                    ob = pb1 + pb2 * (255 - a1) / 255;
                }
            }

            // Convert back from premultiplied to non-premultiplied
            oa = Math.clamp(oa, 0, 255);
            if (oa == 0) {
                outData[i] = 0;
            } else {
                or = Math.clamp(or * 255 / oa, 0, 255);
                og = Math.clamp(og * 255 / oa, 0, 255);
                ob = Math.clamp(ob * 255 / oa, 0, 255);
                outData[i] = (oa << 24) | (or << 16) | (og << 8) | ob;
            }
        }
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

    private static BufferedImage colorMatrix(BufferedImage input, String type, String values, BufferedImage output) {
        int w = input.getWidth(), h = input.getHeight();
        double[] m = new double[20];
        switch (type) {
            case "saturate" -> {
                double s = parseDoubleOr(values.strip(), 1);
                m[0] = 0.2126 + 0.7874 * s;
                m[1] = 0.7152 - 0.7152 * s;
                m[2] = 0.0722 - 0.0722 * s;
                m[5] = 0.2126 - 0.2126 * s;
                m[6] = 0.7152 + 0.2848 * s;
                m[7] = 0.0722 - 0.0722 * s;
                m[10] = 0.2126 - 0.2126 * s;
                m[11] = 0.7152 - 0.7152 * s;
                m[12] = 0.0722 + 0.9278 * s;
                m[18] = 1;
            }
            case "hueRotate" -> {
                double deg = parseDoubleOr(values.strip(), 0);
                double rad = Math.toRadians(deg);
                double cos = Math.cos(rad), sin = Math.sin(rad);
                m[0] = 0.2126 + 0.7874 * cos + -0.2126 * sin;
                m[1] = 0.7152 - 0.7152 * cos + -0.7152 * sin;
                m[2] = 0.0722 - 0.0722 * cos + 0.9278 * sin;
                m[5] = 0.2126 - 0.2126 * cos + 0.1430 * sin;
                m[6] = 0.7152 + 0.2848 * cos + 0.1400 * sin;
                m[7] = 0.0722 - 0.0722 * cos + -0.2830 * sin;
                m[10] = 0.2126 - 0.2126 * cos + -0.7874 * sin;
                m[11] = 0.7152 - 0.7152 * cos + 0.7152 * sin;
                m[12] = 0.0722 + 0.9278 * cos + 0.0722 * sin;
                m[18] = 1;
            }
            case "luminanceToAlpha" -> {
                m[15] = 0.2126;
                m[16] = 0.7152;
                m[17] = 0.0722;
            }
            default -> { // "matrix"
                String[] parts = values.strip().split("[\\s,]+");
                for (int i = 0; i < Math.min(parts.length, 20); i++) {
                    m[i] = parseDoubleOr(parts[i], 0);
                }
            }
        }

        // SVG spec: feColorMatrix operates on non-premultiplied color values.
        // TYPE_INT_ARGB buffers already store non-premultiplied data.
        int[] inData = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < w * h; i++) {
            int p = inData[i];
            int a = (p >> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            int nr = clamp((int) Math.round(m[0] * r + m[1] * g + m[2] * b + m[3] * a + m[4] * 255));
            int ng = clamp((int) Math.round(m[5] * r + m[6] * g + m[7] * b + m[8] * a + m[9] * 255));
            int nb = clamp((int) Math.round(m[10] * r + m[11] * g + m[12] * b + m[13] * a + m[14] * 255));
            int na = clamp((int) Math.round(m[15] * r + m[16] * g + m[17] * b + m[18] * a + m[19] * 255));
            if (na == 0) {
                nr = ng = nb = 0;
            }
            outData[i] = (na << 24) | (nr << 16) | (ng << 8) | nb;
        }
        return output;
    }

    private static BufferedImage componentTransfer(BufferedImage input, Node node, BufferedImage output) {
        int w = input.getWidth(), h = input.getHeight();
        int[] lutR = identityLut(), lutG = identityLut(), lutB = identityLut(), lutA = identityLut();
        for (Node child : node.children) {
            switch (child.tag) {
                case "feFuncR" -> lutR = buildTransferLut(child);
                case "feFuncG" -> lutG = buildTransferLut(child);
                case "feFuncB" -> lutB = buildTransferLut(child);
                case "feFuncA" -> lutA = buildTransferLut(child);
                default -> {
                }
            }
        }

        int[] inData = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        // SVG spec: feComponentTransfer operates on non-premultiplied color values.
        // TYPE_INT_ARGB buffers already store non-premultiplied data.
        for (int i = 0; i < w * h; i++) {
            int p = inData[i];
            int a = (p >> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            int nr = lutR[r], ng = lutG[g], nb = lutB[b], na = lutA[a];
            if (na == 0) {
                nr = ng = nb = 0;
            }
            outData[i] = (na << 24) | (nr << 16) | (ng << 8) | nb;
        }
        return output;
    }

    private static int[] identityLut() {
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++)
            lut[i] = i;
        return lut;
    }

    private static int[] buildTransferLut(Node func) {
        String type = func.get("type", "identity");
        int[] lut = new int[256];
        switch (type) {
            case "linear" -> {
                double slope = parseDoubleOr(func.get("slope"), 1);
                double intercept = parseDoubleOr(func.get("intercept"), 0);
                for (int i = 0; i < 256; i++) {
                    lut[i] = clamp((int) Math.round((slope * (i / 255.0) + intercept) * 255));
                }
            }
            case "gamma" -> {
                double amplitude = parseDoubleOr(func.get("amplitude"), 1);
                double exponent = parseDoubleOr(func.get("exponent"), 1);
                double offset = parseDoubleOr(func.get("offset"), 0);
                for (int i = 0; i < 256; i++) {
                    double c = i / 255.0;
                    lut[i] = clamp((int) Math.round((amplitude * Math.pow(c, exponent) + offset) * 255));
                }
            }
            case "table" -> {
                String vals = func.get("tableValues", "");
                double[] table = parseDoubleArray(vals);
                if (table.length < 2) {
                    return identityLut();
                }
                int n = table.length - 1;
                for (int i = 0; i < 256; i++) {
                    double c = i / 255.0;
                    double pos = c * n;
                    int k = Math.min((int) pos, n - 1);
                    double frac = pos - k;
                    lut[i] = clamp((int) Math.round((table[k] + frac * (table[k + 1] - table[k])) * 255));
                }
            }
            case "discrete" -> {
                String vals = func.get("tableValues", "");
                double[] table = parseDoubleArray(vals);
                if (table.length == 0) {
                    return identityLut();
                }
                int n = table.length;
                for (int i = 0; i < 256; i++) {
                    double c = i / 255.0;
                    int k = Math.min((int) (c * n), n - 1);
                    lut[i] = clamp((int) Math.round(table[k] * 255));
                }
            }
            default -> { // "identity"
                return identityLut();
            }
        }
        return lut;
    }

    private static double[] parseDoubleArray(String s) {
        if (s == null || s.isBlank())
            return new double[0];
        String[] parts = s.strip().split("[\\s,]+");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parseDoubleOr(parts[i], 0);
        }
        return result;
    }

    private static BufferedImage morphology(BufferedImage input, String operator, double radiusX, double radiusY,
            BufferedImage output) {
        int w = input.getWidth(), h = input.getHeight();
        int[] inData = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        int rx = (int) Math.round(radiusX), ry = (int) Math.round(radiusY);
        boolean erode = !"dilate".equals(operator);

        if (rx <= 0 && ry <= 0) {
            System.arraycopy(inData, 0, outData, 0, w * h);
            return output;
        }

        for (int y = 0; y < h; y++) {
            int ky0 = Math.max(0, y - ry);
            int ky1 = Math.min(h - 1, y + ry);
            for (int x = 0; x < w; x++) {
                int kx0 = Math.max(0, x - rx);
                int kx1 = Math.min(w - 1, x + rx);
                int minR = 255, minG = 255, minB = 255, minA = 255;
                int maxR = 0, maxG = 0, maxB = 0, maxA = 0;
                for (int ky = ky0; ky <= ky1; ky++) {
                    int rowOff = ky * w;
                    for (int kx = kx0; kx <= kx1; kx++) {
                        int p = inData[rowOff + kx];
                        int pa = (p >> 24) & 0xFF;
                        int pr = (p >> 16) & 0xFF;
                        int pg = (p >> 8) & 0xFF;
                        int pb = p & 0xFF;
                        if (pa < minA)
                            minA = pa;
                        if (pr < minR)
                            minR = pr;
                        if (pg < minG)
                            minG = pg;
                        if (pb < minB)
                            minB = pb;
                        if (pa > maxA)
                            maxA = pa;
                        if (pr > maxR)
                            maxR = pr;
                        if (pg > maxG)
                            maxG = pg;
                        if (pb > maxB)
                            maxB = pb;
                    }
                }
                if (erode) {
                    outData[y * w + x] = (minA << 24) | (minR << 16) | (minG << 8) | minB;
                } else {
                    outData[y * w + x] = (maxA << 24) | (maxR << 16) | (maxG << 8) | maxB;
                }
            }
        }
        return output;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // ── feConvolveMatrix ─────────────────────────────────────────────────

    private static BufferedImage convolveMatrix(BufferedImage input, Node child, BufferedImage output) {
        int w = input.getWidth(), h = input.getHeight();
        String orderStr = child.get("order", "3");
        String[] orderParts = orderStr.strip().split("[\\s,]+");
        int orderX = (int) parseDoubleOr(orderParts[0], 3);
        int orderY = orderParts.length > 1 ? (int) parseDoubleOr(orderParts[1], 3) : orderX;

        double[] kernel = parseDoubleArray(child.get("kernelMatrix", ""));
        if (kernel.length < orderX * orderY) {
            // Invalid kernel — copy input to output
            int[] inData = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
            int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
            System.arraycopy(inData, 0, outData, 0, w * h);
            return output;
        }

        double kernelSum = 0;
        for (double v : kernel)
            kernelSum += v;
        double divisor = parseDoubleOr(child.get("divisor"), kernelSum == 0 ? 1 : kernelSum);
        if (divisor == 0)
            divisor = 1;
        double bias = parseDoubleOr(child.get("bias"), 0);
        int targetX = (int) parseDoubleOr(child.get("targetX"), Math.floor(orderX / 2.0));
        int targetY = (int) parseDoubleOr(child.get("targetY"), Math.floor(orderY / 2.0));
        String edgeMode = child.get("edgeMode", "duplicate");
        boolean preserveAlpha = "true".equals(child.get("preserveAlpha"));

        int[] inData = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                for (int ky = 0; ky < orderY; ky++) {
                    for (int kx = 0; kx < orderX; kx++) {
                        int sx = x + kx - targetX;
                        int sy = y + ky - targetY;
                        int pixel = sampleEdge(inData, w, h, sx, sy, edgeMode);
                        double kVal = kernel[ky * orderX + kx];
                        int pa = (pixel >> 24) & 0xFF;
                        int pr = (pixel >> 16) & 0xFF;
                        int pg = (pixel >> 8) & 0xFF;
                        int pb = pixel & 0xFF;
                        if (!preserveAlpha) {
                            // SVG spec: default — kernel operates on premultiplied values
                            if (pa > 0 && pa < 255) {
                                pr = pr * pa / 255;
                                pg = pg * pa / 255;
                                pb = pb * pa / 255;
                            } else if (pa == 0) {
                                pr = pg = pb = 0;
                            }
                        }
                        sumA += kVal * pa;
                        sumR += kVal * pr;
                        sumG += kVal * pg;
                        sumB += kVal * pb;
                    }
                }
                int a, r, g, b;
                int pR = clamp((int) Math.round(sumR / divisor + bias * 255));
                int pG = clamp((int) Math.round(sumG / divisor + bias * 255));
                int pB = clamp((int) Math.round(sumB / divisor + bias * 255));
                if (preserveAlpha) {
                    a = (inData[y * w + x] >> 24) & 0xFF;
                    // preserveAlpha: no premultiply was applied, store directly
                    r = pR;
                    g = pG;
                    b = pB;
                } else {
                    a = clamp((int) Math.round(sumA / divisor + bias * 255));
                    // Un-premultiply for storage in TYPE_INT_ARGB
                    if (a > 0 && a < 255) {
                        r = Math.min(255, pR * 255 / a);
                        g = Math.min(255, pG * 255 / a);
                        b = Math.min(255, pB * 255 / a);
                    } else if (a == 0) {
                        r = g = b = 0;
                    } else {
                        r = pR;
                        g = pG;
                        b = pB;
                    }
                }
                outData[y * w + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return output;
    }

    private static int sampleEdge(int[] data, int w, int h, int x, int y, String edgeMode) {
        if (x >= 0 && x < w && y >= 0 && y < h)
            return data[y * w + x];
        return switch (edgeMode) {
            case "wrap" -> {
                int wx = ((x % w) + w) % w;
                int wy = ((y % h) + h) % h;
                yield data[wy * w + wx];
            }
            case "none" -> 0; // transparent black
            default -> { // "duplicate"
                int cx = Math.max(0, Math.min(w - 1, x));
                int cy = Math.max(0, Math.min(h - 1, y));
                yield data[cy * w + cx];
            }
        };
    }

    // ── feDisplacementMap ────────────────────────────────────────────────

    private static BufferedImage displacementMap(BufferedImage input, BufferedImage in2, double scale,
            String xChannelSelector, String yChannelSelector, BufferedImage output) {
        int w = input.getWidth(), h = input.getHeight();
        int[] inData = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();

        if (in2 == null) {
            System.arraycopy(inData, 0, outData, 0, w * h);
            return output;
        }
        int w2 = in2.getWidth(), h2 = in2.getHeight();
        int[] mapData = ((DataBufferInt) in2.getRaster().getDataBuffer()).getData();

        int xShift = channelShift(xChannelSelector);
        int yShift = channelShift(yChannelSelector);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int mapPixel;
                if (x < w2 && y < h2)
                    mapPixel = mapData[y * w2 + x];
                else
                    mapPixel = 0;

                int xChannel = (mapPixel >> xShift) & 0xFF;
                int yChannel = (mapPixel >> yShift) & 0xFF;
                double dx = scale * (xChannel / 255.0 - 0.5);
                double dy = scale * (yChannel / 255.0 - 0.5);
                int srcX = (int) Math.round(x + dx);
                int srcY = (int) Math.round(y + dy);
                if (srcX >= 0 && srcX < w && srcY >= 0 && srcY < h) {
                    outData[y * w + x] = inData[srcY * w + srcX];
                } else {
                    outData[y * w + x] = 0; // transparent
                }
            }
        }
        return output;
    }

    private static int channelShift(String selector) {
        return switch (selector) {
            case "R" -> 16;
            case "G" -> 8;
            case "B" -> 0;
            default -> 24; // "A"
        };
    }

    // ── feTurbulence ─────────────────────────────────────────────────────

    private static final int PERLIN_B = 0x100;
    private static final int PERLIN_BM = 0xFF;

    private static BufferedImage turbulence(int width, int height, String type, double baseFreqX, double baseFreqY,
            int numOctaves, int seed, BufferedImage output) {
        clearBuffer(output);
        if (baseFreqX == 0 && baseFreqY == 0) {
            // Zero frequency: fractalNoise produces mid-gray, turbulence produces black
            if ("fractalNoise".equals(type)) {
                int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
                int gray = (128 << 24) | (128 << 16) | (128 << 8) | 128;
                java.util.Arrays.fill(outData, gray);
            }
            return output;
        }

        boolean fractalNoise = "fractalNoise".equals(type);
        if (numOctaves < 1)
            numOctaves = 1;

        // Initialize permutation and gradient tables
        int[] latticeSelector = new int[PERLIN_B + PERLIN_B + 2];
        double[][] gradient = new double[4][(PERLIN_B + PERLIN_B + 2) * 2];
        initTurbulence(seed, latticeSelector, gradient);

        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double[] rgba = computeTurbulence(x, y, baseFreqX, baseFreqY, numOctaves, fractalNoise, latticeSelector,
                        gradient);
                int r = clamp((int) Math.round(rgba[0] * 255));
                int g = clamp((int) Math.round(rgba[1] * 255));
                int b = clamp((int) Math.round(rgba[2] * 255));
                int a = clamp((int) Math.round(rgba[3] * 255));
                outData[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return output;
    }

    private static void initTurbulence(int seed, int[] latticeSelector, double[][] gradient) {
        java.util.Random rng = new java.util.Random(seed);
        for (int k = 0; k < PERLIN_B; k++) {
            latticeSelector[k] = k;
            for (int ch = 0; ch < 4; ch++) {
                gradient[ch][k * 2] = (rng.nextInt(PERLIN_B + PERLIN_B) - PERLIN_B) / (double) PERLIN_B;
                gradient[ch][k * 2 + 1] = (rng.nextInt(PERLIN_B + PERLIN_B) - PERLIN_B) / (double) PERLIN_B;
                double len = Math.sqrt(
                        gradient[ch][k * 2] * gradient[ch][k * 2] + gradient[ch][k * 2 + 1] * gradient[ch][k * 2 + 1]);
                if (len > 0) {
                    gradient[ch][k * 2] /= len;
                    gradient[ch][k * 2 + 1] /= len;
                }
            }
        }
        // Shuffle permutation table
        for (int i = PERLIN_B - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = latticeSelector[i];
            latticeSelector[i] = latticeSelector[j];
            latticeSelector[j] = tmp;
        }
        // Duplicate for wrapping
        for (int i = 0; i < PERLIN_B + 2; i++) {
            latticeSelector[PERLIN_B + i] = latticeSelector[i];
            for (int ch = 0; ch < 4; ch++) {
                gradient[ch][(PERLIN_B + i) * 2] = gradient[ch][i * 2];
                gradient[ch][(PERLIN_B + i) * 2 + 1] = gradient[ch][i * 2 + 1];
            }
        }
    }

    private static double[] computeTurbulence(double x, double y, double baseFreqX, double baseFreqY, int numOctaves,
            boolean fractalNoise, int[] latticeSelector, double[][] gradient) {
        double[] result = new double[4];
        for (int ch = 0; ch < 4; ch++) {
            double freq_x = baseFreqX;
            double freq_y = baseFreqY;
            double amplitude = 1.0;
            double total = 0;
            for (int oct = 0; oct < numOctaves; oct++) {
                double n = noise2(x * freq_x, y * freq_y, ch, latticeSelector, gradient);
                if (fractalNoise) {
                    total += n * amplitude;
                } else {
                    total += Math.abs(n) * amplitude;
                }
                freq_x *= 2;
                freq_y *= 2;
                amplitude *= 0.5;
            }
            if (fractalNoise) {
                result[ch] = (total + 1) / 2.0;
            } else {
                result[ch] = total;
            }
        }
        return result;
    }

    private static double noise2(double x, double y, int channel, int[] latticeSelector, double[][] grad) {
        double t = x + 10000; // offset to avoid negative issues
        int bx0 = ((int) Math.floor(t)) & PERLIN_BM;
        int bx1 = (bx0 + 1) & PERLIN_BM;
        double rx0 = t - Math.floor(t);
        double rx1 = rx0 - 1.0;

        t = y + 10000;
        int by0 = ((int) Math.floor(t)) & PERLIN_BM;
        int by1 = (by0 + 1) & PERLIN_BM;
        double ry0 = t - Math.floor(t);
        double ry1 = ry0 - 1.0;

        int i = latticeSelector[bx0];
        int j = latticeSelector[bx1];

        // Fade curves
        double sx = rx0 * rx0 * (3.0 - 2.0 * rx0);
        double sy = ry0 * ry0 * (3.0 - 2.0 * ry0);

        // Corners
        int idx;
        idx = (latticeSelector[i + by0]) * 2;
        double u = rx0 * grad[channel][idx] + ry0 * grad[channel][idx + 1];
        idx = (latticeSelector[j + by0]) * 2;
        double v = rx1 * grad[channel][idx] + ry0 * grad[channel][idx + 1];
        double a = u + sx * (v - u);

        idx = (latticeSelector[i + by1]) * 2;
        u = rx0 * grad[channel][idx] + ry1 * grad[channel][idx + 1];
        idx = (latticeSelector[j + by1]) * 2;
        v = rx1 * grad[channel][idx] + ry1 * grad[channel][idx + 1];
        double b = u + sx * (v - u);

        return a + sy * (b - a);
    }

    // ── Lighting infrastructure ──────────────────────────────────────────

    private record LightSource(String type, double azimuth, double elevation, double x, double y, double z,
            double pointsAtX, double pointsAtY, double pointsAtZ, double spotExponent, double limitingConeAngle) {
    }

    private static LightSource parseLightSource(Node lightingNode) {
        for (Node child : lightingNode.children) {
            switch (child.tag) {
                case "feDistantLight" -> {
                    return new LightSource("distant", parseDoubleOr(child.get("azimuth"), 0),
                            parseDoubleOr(child.get("elevation"), 0), 0, 0, 0, 0, 0, 0, 1, -1);
                }
                case "fePointLight" -> {
                    return new LightSource("point", 0, 0, parseDoubleOr(child.get("x"), 0),
                            parseDoubleOr(child.get("y"), 0), parseDoubleOr(child.get("z"), 0), 0, 0, 0, 1, -1);
                }
                case "feSpotLight" -> {
                    return new LightSource("spot", 0, 0, parseDoubleOr(child.get("x"), 0),
                            parseDoubleOr(child.get("y"), 0), parseDoubleOr(child.get("z"), 0),
                            parseDoubleOr(child.get("pointsAtX"), 0), parseDoubleOr(child.get("pointsAtY"), 0),
                            parseDoubleOr(child.get("pointsAtZ"), 0), parseDoubleOr(child.get("specularExponent"), 1),
                            parseDoubleOr(child.get("limitingConeAngle"), -1));
                }
                default -> {
                }
            }
        }
        return null;
    }

    private static double[] computeLightVector(LightSource light, int px, int py, double surfaceZ, int offsetX,
            int offsetY) {
        return switch (light.type) {
            case "distant" -> {
                double az = Math.toRadians(light.azimuth);
                double el = Math.toRadians(light.elevation);
                yield new double[]{Math.cos(el) * Math.cos(az), Math.cos(el) * Math.sin(az), Math.sin(el)};
            }
            case "point", "spot" -> {
                // Light coordinates are in SVG user space; px/py are in the
                // working buffer which may be offset by (offsetX, offsetY).
                double dx = light.x - (px + offsetX);
                double dy = light.y - (py + offsetY);
                double dz = light.z - surfaceZ;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 1e-10)
                    yield new double[]{0, 0, 1};
                yield new double[]{dx / len, dy / len, dz / len};
            }
            default -> new double[]{0, 0, 1};
        };
    }

    private static double computeSpotIntensity(LightSource light, double[] L) {
        if (!"spot".equals(light.type))
            return 1.0;
        double sdx = light.pointsAtX - light.x;
        double sdy = light.pointsAtY - light.y;
        double sdz = light.pointsAtZ - light.z;
        double slen = Math.sqrt(sdx * sdx + sdy * sdy + sdz * sdz);
        if (slen < 1e-10)
            return 1.0;
        sdx /= slen;
        sdy /= slen;
        sdz /= slen;
        // dot(-L, spotDirection)
        double cosAngle = -L[0] * sdx + -L[1] * sdy + -L[2] * sdz;
        if (light.limitingConeAngle >= 0) {
            double limitCos = Math.cos(Math.toRadians(light.limitingConeAngle));
            if (cosAngle < limitCos)
                return 0.0;
        }
        if (cosAngle <= 0)
            return 0.0;
        return Math.pow(cosAngle, light.spotExponent);
    }

    private static double[] surfaceNormal(int[] alphaData, int x, int y, int w, int h, double surfaceScale) {
        // Sobel-like kernel using clamped coordinates for edges
        int x0 = Math.max(x - 1, 0);
        int x1 = Math.min(x + 1, w - 1);
        int y0 = Math.max(y - 1, 0);
        int y1 = Math.min(y + 1, h - 1);

        // SVG spec: I(x,y) is alpha in [0,1], so divide by 255.0
        double a00 = (alphaData[y0 * w + x0] >>> 24) / 255.0;
        double a10 = (alphaData[y0 * w + x] >>> 24) / 255.0;
        double a20 = (alphaData[y0 * w + x1] >>> 24) / 255.0;
        double a01 = (alphaData[y * w + x0] >>> 24) / 255.0;
        double a21 = (alphaData[y * w + x1] >>> 24) / 255.0;
        double a02 = (alphaData[y1 * w + x0] >>> 24) / 255.0;
        double a12 = (alphaData[y1 * w + x] >>> 24) / 255.0;
        double a22 = (alphaData[y1 * w + x1] >>> 24) / 255.0;

        double factorX = x > 0 && x < w - 1 ? 0.25 : 0.5;
        double factorY = y > 0 && y < h - 1 ? 0.25 : 0.5;

        double nx = -surfaceScale * factorX * ((a20 - a00) + 2.0 * (a21 - a01) + (a22 - a02));
        double ny = -surfaceScale * factorY * ((a02 - a00) + 2.0 * (a12 - a10) + (a22 - a20));
        double nz = 1.0;

        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-10)
            return new double[]{0, 0, 1};
        return new double[]{nx / len, ny / len, nz / len};
    }

    // ── feDiffuseLighting ────────────────────────────────────────────────

    private static BufferedImage diffuseLighting(BufferedImage input, Node node, BufferedImage output, int offsetX,
            int offsetY) {
        int w = input.getWidth();
        int h = input.getHeight();
        double surfaceScale = parseDoubleOr(node.get("surfaceScale"), 1);
        double kd = parseDoubleOr(node.get("diffuseConstant"), 1);
        Colors.RGBA lightColor = Colors.color(node.get("lighting-color", "white"), 1.0);
        if (lightColor == null)
            lightColor = new Colors.RGBA(1, 1, 1, 1);

        LightSource light = parseLightSource(node);
        if (light == null) {
            clearBuffer(output);
            return output;
        }

        int[] inData = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();

        double lr = lightColor.r();
        double lg = lightColor.g();
        double lb = lightColor.b();

        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                double[] N = surfaceNormal(inData, px, py, w, h, surfaceScale);
                double alpha = (inData[py * w + px] >>> 24);
                double sz = surfaceScale * alpha / 255.0;
                double[] L = computeLightVector(light, px, py, sz, offsetX, offsetY);
                double spotIntensity = computeSpotIntensity(light, L);
                double nDotL = Math.max(0, N[0] * L[0] + N[1] * L[1] + N[2] * L[2]);
                double factor = kd * nDotL * spotIntensity;

                int r = clamp255(factor * lr);
                int g = clamp255(factor * lg);
                int b = clamp255(factor * lb);
                outData[py * w + px] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return output;
    }

    // ── feSpecularLighting ───────────────────────────────────────────────

    private static BufferedImage specularLighting(BufferedImage input, Node node, BufferedImage output, int offsetX,
            int offsetY) {
        int w = input.getWidth();
        int h = input.getHeight();
        double surfaceScale = parseDoubleOr(node.get("surfaceScale"), 1);
        double ks = parseDoubleOr(node.get("specularConstant"), 1);
        double specExp = parseDoubleOr(node.get("specularExponent"), 1);
        Colors.RGBA lightColor = Colors.color(node.get("lighting-color", "white"), 1.0);
        if (lightColor == null)
            lightColor = new Colors.RGBA(1, 1, 1, 1);

        LightSource light = parseLightSource(node);
        if (light == null) {
            clearBuffer(output);
            return output;
        }

        int[] inData = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] outData = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();

        double lr = lightColor.r();
        double lg = lightColor.g();
        double lb = lightColor.b();

        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                double[] N = surfaceNormal(inData, px, py, w, h, surfaceScale);
                double alpha = (inData[py * w + px] >>> 24);
                double sz = surfaceScale * alpha / 255.0;
                double[] L = computeLightVector(light, px, py, sz, offsetX, offsetY);
                double spotIntensity = computeSpotIntensity(light, L);

                // Half-angle vector H = normalize(L + E), E = (0, 0, 1)
                double hx = L[0];
                double hy = L[1];
                double hz = L[2] + 1.0;
                double hlen = Math.sqrt(hx * hx + hy * hy + hz * hz);
                if (hlen > 1e-10) {
                    hx /= hlen;
                    hy /= hlen;
                    hz /= hlen;
                }
                double nDotH = Math.max(0, N[0] * hx + N[1] * hy + N[2] * hz);
                double specular = ks * Math.pow(nDotH, specExp) * spotIntensity;

                int r = clamp255(specular * lr);
                int g = clamp255(specular * lg);
                int b = clamp255(specular * lb);
                int a = Math.max(r, Math.max(g, b));
                outData[py * w + px] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return output;
    }

    private static int clamp255(double v) {
        return Math.clamp((int) (v * 255 + 0.5), 0, 255);
    }
}

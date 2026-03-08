package io.brunoborges.jairosvg.surface;

import static io.brunoborges.jairosvg.util.Helpers.getViewbox;
import static io.brunoborges.jairosvg.util.Helpers.nodeFormat;
import static io.brunoborges.jairosvg.util.Helpers.normalize;
import static io.brunoborges.jairosvg.util.Helpers.preserveRatio;
import static io.brunoborges.jairosvg.util.Helpers.size;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import io.brunoborges.jairosvg.css.Colors;
import io.brunoborges.jairosvg.dom.BoundingBox;
import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.dom.SvgFont;
import io.brunoborges.jairosvg.draw.Defs;
import io.brunoborges.jairosvg.draw.ImageHandler;
import io.brunoborges.jairosvg.draw.PathDrawer;
import io.brunoborges.jairosvg.draw.ShapeDrawer;
import io.brunoborges.jairosvg.draw.SvgDrawer;
import io.brunoborges.jairosvg.draw.TextDrawer;
import io.brunoborges.jairosvg.util.Helpers;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * Abstract base surface for SVG rendering using Java2D. Port of CairoSVG
 * surface.py
 */
public sealed class Surface permits PngSurface, JpegSurface, TiffSurface, PdfSurface, PsSurface {

    private static final Set<String> INVISIBLE_TAGS = Set.of("clipPath", "desc", "filter", "font", "font-face",
            "foreignObject", "glyph", "linearGradient", "marker", "mask", "metadata", "missing-glyph", "pattern",
            "radialGradient", "symbol", "title");

    private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");
    private static final float[] NO_DASH = new float[0];

    // Rendering state
    public Graphics2D context;
    public GeneralPath path = new GeneralPath();
    public double contextWidth;
    public double contextHeight;
    public double[] cursorPosition = {0, 0};
    public double[] cursorDPosition = {0, 0};
    public double textPathWidth = 0;
    public double dpi;
    public double fontSize;
    public boolean strokeAndFill = true;
    public Node parentNode;
    public Node rootNode;

    // Definition stores
    public Map<String, Node> markers = new LinkedHashMap<>();
    public Map<String, Node> gradients = new LinkedHashMap<>();
    public Map<String, Node> patterns = new LinkedHashMap<>();
    public Map<String, Node> masks = new LinkedHashMap<>();
    public Map<String, Node> paths = new LinkedHashMap<>();
    public Map<String, Node> filters = new LinkedHashMap<>();
    public Map<String, Node> images = new LinkedHashMap<>();
    public Map<String, SvgFont> fonts = new LinkedHashMap<>();

    // Reusable mask buffer (lazily allocated, avoids per-mask allocation)
    public BufferedImage maskBuffer;

    // Reusable off-screen effect buffer for filters/masks/opacity
    private BufferedImage effectBuffer;
    private boolean effectBufferInUse;

    // Surface dimensions
    protected BufferedImage image;
    protected double width;
    protected double height;
    protected OutputStream output;

    // Color mapping
    private UnaryOperator<Colors.RGBA> mapRgba;

    // Stroke caches
    private final Map<String, float[]> dashArrayCache = new HashMap<>();
    private final Map<String, BasicStroke> strokeCache = new HashMap<>();

    // Raster image cache (keyed by data: URI or resolved URL)
    public Map<String, BufferedImage> rasterImageCache = new HashMap<>();

    public Surface() {
    }

    /** Initialize the surface. */
    public void init(Node tree, OutputStream output, double dpi, Surface parentSurface, Double parentWidth,
            Double parentHeight, double scale, Double outputWidth, Double outputHeight, String backgroundColor,
            UnaryOperator<Colors.RGBA> mapRgba) {
        init(tree, output, dpi, parentSurface, parentWidth, parentHeight, scale, outputWidth, outputHeight,
                backgroundColor, mapRgba, null);
    }

    /** Initialize the surface with optional rendering hint overrides. */
    public void init(Node tree, OutputStream output, double dpi, Surface parentSurface, Double parentWidth,
            Double parentHeight, double scale, Double outputWidth, Double outputHeight, String backgroundColor,
            UnaryOperator<Colors.RGBA> mapRgba, Map<RenderingHints.Key, Object> renderingHintOverrides) {

        this.output = output;
        this.dpi = dpi;
        this.mapRgba = mapRgba;
        this.contextWidth = parentWidth != null ? parentWidth : 0;
        this.contextHeight = parentHeight != null ? parentHeight : 0;
        this.rootNode = tree;

        if (parentSurface != null) {
            this.markers = parentSurface.markers;
            this.gradients = parentSurface.gradients;
            this.patterns = parentSurface.patterns;
            this.masks = parentSurface.masks;
            this.paths = parentSurface.paths;
            this.filters = parentSurface.filters;
            this.images = parentSurface.images;
            this.fonts = parentSurface.fonts;
            this.rasterImageCache = parentSurface.rasterImageCache;
        }

        this.fontSize = size(this, "12pt");

        double[] nf = nodeFormat(this, tree);
        double w = nf[0], h = nf[1];
        double[] viewbox = getViewbox(nf);
        if (viewbox == null) {
            viewbox = new double[]{0, 0, w, h};
        }

        if (outputWidth != null && outputHeight != null) {
            w = outputWidth;
            h = outputHeight;
        } else if (outputWidth != null) {
            if (w > 0)
                h *= outputWidth / w;
            w = outputWidth;
        } else if (outputHeight != null) {
            if (h > 0)
                w *= outputHeight / h;
            h = outputHeight;
        } else {
            w *= scale;
            h *= scale;
        }

        createSurface(w, h);

        if (width == 0 || height == 0) {
            throw new IllegalArgumentException("The SVG size is undefined");
        }

        this.context = image.createGraphics();
        setupRenderingHints(renderingHintOverrides);

        setContextSize(w, h, viewbox, tree);
        context.translate(0, 0);

        if (backgroundColor != null) {
            Colors.RGBA bg = Colors.color(backgroundColor);
            context.setColor(new Color((float) bg.r(), (float) bg.g(), (float) bg.b(), (float) bg.a()));
            context.fillRect(0, 0, image.getWidth(), image.getHeight());
        }

        draw(tree);
    }

    protected void createSurface(double w, double h) {
        int iw = Math.max(1, (int) Math.round(w));
        int ih = Math.max(1, (int) Math.round(h));
        this.image = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        this.width = iw;
        this.height = ih;
    }

    private void setupRenderingHints(Map<RenderingHints.Key, Object> overrides) {
        context.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        context.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (overrides != null) {
            overrides.forEach(context::setRenderingHint);
        }
    }

    /** Set the context size and apply viewport transformations. */
    public void setContextSize(double w, double h, double[] viewbox, Node tree) {
        double rectX, rectY;
        if (viewbox != null) {
            rectX = viewbox[0];
            rectY = viewbox[1];
            tree.imageWidth = viewbox[2];
            tree.imageHeight = viewbox[3];
        } else {
            rectX = 0;
            rectY = 0;
            tree.imageWidth = w;
            tree.imageHeight = h;
        }

        double[] ratio = preserveRatio(this, tree, w, h);
        double scaleX = ratio[0], scaleY = ratio[1];
        double translateX = ratio[2], translateY = ratio[3];

        // Clip to viewport
        if (!"visible".equals(tree.get("overflow", "hidden"))) {
            context.setClip(new Rectangle2D.Double(0, 0, w, h));
        }

        context.scale(scaleX, scaleY);
        context.translate(translateX - rectX, translateY - rectY);
        this.contextWidth = w / scaleX;
        this.contextHeight = h / scaleY;
    }

    /** Map a color string through the color mapper. */
    public Colors.RGBA mapColor(String string, double opacity) {
        Colors.RGBA rgba = Colors.color(string, opacity);
        return mapRgba != null ? mapRgba.apply(rgba) : rgba;
    }

    public Colors.RGBA mapColor(String string) {
        return mapColor(string, 1.0);
    }

    /** Main draw method - renders a node and its children. */
    public void draw(Node node) {
        if ("svg".equals(node.tag)) {
            Defs.parseAllDefs(this, node);
        }

        if ("defs".equals(node.tag))
            return;

        if ((node.has("width") && size(this, node.get("width")) == 0)
                || (node.has("height") && size(this, node.get("height")) == 0)) {
            return;
        }

        // Save state
        Node oldParentNode = this.parentNode;
        double oldFontSize = this.fontSize;
        double oldContextWidth = this.contextWidth;
        double oldContextHeight = this.contextHeight;
        this.parentNode = node;

        // Handle font shorthand
        if (node.has("font")) {
            var font = Helpers.parseFont(node.get("font"));
            for (var entry : font.entrySet()) {
                if (!node.has(entry.getKey()) && !entry.getValue().isEmpty()) {
                    node.set(entry.getKey(), entry.getValue());
                }
            }
        }

        this.fontSize = size(this, node.get("font-size", "12pt"));

        AffineTransform savedTransform = context.getTransform();
        Shape savedClip = context.getClip();
        Composite savedComposite = context.getComposite();
        Stroke savedStroke = context.getStroke();

        // Apply transformations
        Helpers.transform(this, node.get("transform"), null, node.get("transform-origin"));

        // Filter and opacity
        String filterName = null;
        String filterStr = node.get("filter");
        if (filterStr != null) {
            filterName = UrlHelper.parseUrl(filterStr).fragment();
        }
        String maskName = null;
        String maskStr = node.get("mask");
        if (maskStr != null) {
            maskName = UrlHelper.parseUrl(maskStr).fragment();
        }

        double opacity = parseDoubleOr(node.get("opacity"), 1);
        boolean groupOpacity = opacity < 1 && !node.children.isEmpty();

        if (filterName != null) {
            Defs.prepareFilter(this, node, filterName);
        }

        Graphics2D effectBaseContext = null;
        Graphics2D effectContext = null;
        BufferedImage effectSourceImage = null;
        boolean ownEffectBuffer = false;
        int effectOffsetX = 0;
        int effectOffsetY = 0;
        if (filterName != null || maskName != null || groupOpacity) {
            effectBaseContext = context;
            int iw = image.getWidth();
            int ih = image.getHeight();

            // Attempt sub-region allocation using geometric bounding box.
            // Falls back to full-canvas for unsupported tags (text, image, use, svg).
            BoundingBox.Box bbox = BoundingBox.calculate(this, node);
            if (BoundingBox.isNonEmpty(bbox)) {
                // Map the four bbox corners to device (pixel) space via the current transform.
                AffineTransform tx = context.getTransform();
                double bx = bbox.minX(), by = bbox.minY();
                double bxw = bx + bbox.width(), byh = by + bbox.height();
                double[] corners = {bx, by, bxw, by, bx, byh, bxw, byh};
                tx.transform(corners, 0, corners, 0, 4);
                double dMinX = Math.min(Math.min(corners[0], corners[2]), Math.min(corners[4], corners[6]));
                double dMinY = Math.min(Math.min(corners[1], corners[3]), Math.min(corners[5], corners[7]));
                double dMaxX = Math.max(Math.max(corners[0], corners[2]), Math.max(corners[4], corners[6]));
                double dMaxY = Math.max(Math.max(corners[1], corners[3]), Math.max(corners[5], corners[7]));

                // Padding for filters: blur/shadow primitives extend well beyond the
                // geometric shape; use 20% of the largest bbox dimension with a minimum
                // of 10 px to avoid clipping the effect at the buffer edge.
                // Padding for mask/opacity: only 2 px to absorb sub-pixel anti-aliasing.
                int padding = filterName != null
                        ? Math.max(10, (int) Math.ceil(Math.max(dMaxX - dMinX, dMaxY - dMinY) * 0.2))
                        : 2;
                int px = Math.max(0, (int) Math.floor(dMinX) - padding);
                int py = Math.max(0, (int) Math.floor(dMinY) - padding);
                int px2 = Math.min(iw, (int) Math.ceil(dMaxX) + padding);
                int py2 = Math.min(ih, (int) Math.ceil(dMaxY) + padding);
                int pw = px2 - px;
                int ph = py2 - py;

                if (pw > 0 && ph > 0 && (long) pw * ph < (long) iw * ih) {
                    effectOffsetX = px;
                    effectOffsetY = py;
                    effectSourceImage = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
                    effectBufferInUse = true;
                    ownEffectBuffer = true;
                    effectContext = effectSourceImage.createGraphics();
                    effectContext.setRenderingHints(effectBaseContext.getRenderingHints());
                    // Apply -offset translation so the element renders at (0,0) within the buffer.
                    AffineTransform bufferTransform = new AffineTransform(effectBaseContext.getTransform());
                    bufferTransform
                            .preConcatenate(AffineTransform.getTranslateInstance(-effectOffsetX, -effectOffsetY));
                    effectContext.setTransform(bufferTransform);
                    effectContext.setClip(effectBaseContext.getClip());
                    effectContext.setComposite(effectBaseContext.getComposite());
                    effectContext.setStroke(effectBaseContext.getStroke());
                    context = effectContext;
                }
            }

            // Full-canvas fallback when sub-region could not be computed.
            if (effectContext == null) {
                if (!effectBufferInUse && effectBuffer != null && effectBuffer.getWidth() == iw
                        && effectBuffer.getHeight() == ih) {
                    effectSourceImage = effectBuffer;
                    java.util.Arrays.fill(
                            ((java.awt.image.DataBufferInt) effectSourceImage.getRaster().getDataBuffer()).getData(),
                            0);
                } else {
                    effectSourceImage = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
                    if (!effectBufferInUse) {
                        effectBuffer = effectSourceImage;
                    }
                }
                effectBufferInUse = true;
                ownEffectBuffer = true;
                effectContext = effectSourceImage.createGraphics();
                effectContext.setRenderingHints(effectBaseContext.getRenderingHints());
                effectContext.setTransform(effectBaseContext.getTransform());
                effectContext.setClip(effectBaseContext.getClip());
                effectContext.setComposite(effectBaseContext.getComposite());
                effectContext.setStroke(effectBaseContext.getStroke());
                context = effectContext;
            }
        }

        // Set stroke properties
        configureStroke(node);

        // Clip
        applyClip(node);

        // Reset path for this node
        path.reset();

        // Draw the tag
        String tag = node.tag;
        boolean drawn = true;
        try {
            switch (tag) {
                case "a", "text", "textPath", "tspan" -> TextDrawer.text(this, node);
                case "circle" -> ShapeDrawer.circle(this, node);
                case "clipPath" -> Defs.clipPath(this, node);
                case "ellipse" -> ShapeDrawer.ellipse(this, node);
                case "filter" -> Defs.filter(this, node);
                case "image" -> ImageHandler.image(this, node);
                case "line" -> ShapeDrawer.line(this, node);
                case "linearGradient" -> Defs.linearGradient(this, node);
                case "marker" -> Defs.marker(this, node);
                case "mask" -> Defs.mask(this, node);
                case "path" -> PathDrawer.path(this, node);
                case "pattern" -> Defs.pattern(this, node);
                case "polyline" -> ShapeDrawer.polyline(this, node);
                case "polygon" -> ShapeDrawer.polygon(this, node);
                case "radialGradient" -> Defs.radialGradient(this, node);
                case "rect" -> ShapeDrawer.rect(this, node);
                case "svg" -> SvgDrawer.svg(this, node);
                case "use" -> Defs.use(this, node);
                default -> drawn = false;
            }
        } catch (Helpers.PointError e) {
            // Ignore point parsing errors
        }

        // Get stroke and fill opacity
        double strokeOpacity = parseDoubleOr(node.get("stroke-opacity"), 1);
        double fillOpacity = parseDoubleOr(node.get("fill-opacity"), 1);
        if (opacity < 1 && node.children.isEmpty()) {
            strokeOpacity *= opacity;
            fillOpacity *= opacity;
        }

        // Manage display and visibility
        boolean display = !"none".equals(node.get("display", "inline"));
        boolean visible = display && !"hidden".equals(node.get("visibility", "visible"));

        // Fill and stroke
        if (strokeAndFill && visible && drawn && !path.getPathIterator(null).isDone()) {
            // Fill
            String[] paintValue = Helpers.paint(node.get("fill", "black"));
            if (!Defs.gradientOrPattern(this, node, paintValue[0], fillOpacity)) {
                Colors.RGBA fillColor = mapColor(paintValue[1], fillOpacity);
                context.setColor(toAwtColor(fillColor));
            }

            // Set fill rule
            if ("evenodd".equals(node.get("fill-rule"))) {
                path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
            } else {
                path.setWindingRule(GeneralPath.WIND_NON_ZERO);
            }

            context.fill(path);

            // Stroke
            String[] strokePaint = Helpers.paint(node.get("stroke"));
            if (strokePaint[1] != null && !"none".equals(strokePaint[1])) {
                if (!Defs.gradientOrPattern(this, node, strokePaint[0], strokeOpacity)) {
                    Colors.RGBA strokeColor = mapColor(strokePaint[1], strokeOpacity);
                    context.setColor(toAwtColor(strokeColor));
                }

                float strokeWidth = (float) size(this, node.get("stroke-width", "1"));
                int cap = getLineCap(node.get("stroke-linecap"));
                int join = getLineJoin(node.get("stroke-linejoin"));
                float miterLimit = (float) parseDoubleOr(node.get("stroke-miterlimit"), 4);

                // Dash array
                String dashStr = node.get("stroke-dasharray", "").strip();
                float[] dashArray = null;
                if (!dashStr.isEmpty() && !"none".equals(dashStr)) {
                    float[] cached = dashArrayCache.computeIfAbsent(dashStr, k -> {
                        String[] parts = WHITESPACE.split(normalize(k));
                        float[] arr = new float[parts.length];
                        float sum = 0;
                        for (int i = 0; i < parts.length; i++) {
                            arr[i] = (float) size(Surface.this, parts[i]);
                            sum += arr[i];
                        }
                        return sum == 0 ? NO_DASH : arr;
                    });
                    if (cached != NO_DASH) {
                        dashArray = cached;
                    }
                }

                float dashOffset = (float) size(this, node.get("stroke-dashoffset"));

                String strokeKey = strokeWidth + "|" + cap + "|" + join + "|" + miterLimit + "|" + dashStr + "|"
                        + dashOffset;
                final float[] dashForStroke = dashArray;
                BasicStroke stroke = strokeCache.computeIfAbsent(strokeKey,
                        k -> dashForStroke != null
                                ? new BasicStroke(strokeWidth, cap, join, miterLimit, dashForStroke, dashOffset)
                                : new BasicStroke(strokeWidth, cap, join, miterLimit));

                context.setStroke(stroke);
                context.draw(path);
            }

            Defs.drawMarkers(this, node);
        } else if (!visible) {
            // Reset path
            path.reset();
        }

        // Draw children
        if (display && !INVISIBLE_TAGS.contains(node.tag)) {
            for (Node child : node.children) {
                draw(child);
            }
        }

        // Restore state
        context.setTransform(savedTransform);
        context.setClip(savedClip);
        context.setComposite(savedComposite);
        context.setStroke(savedStroke);

        if (effectContext != null) {
            BufferedImage renderedImage = effectSourceImage;
            java.awt.Rectangle filterClip = null;
            if (filterName != null) {
                // Compute the filter region BEFORE filtering so that primitives like
                // feFlood don't bleed into other elements' areas when composited back.
                Node filterNode = this.filters.get(filterName);
                filterClip = Defs.computeFilterRegion(effectSourceImage, filterNode);
                renderedImage = Defs.applyFilter(this, filterName, renderedImage);
            }
            if (maskName != null) {
                // The mask content must be rendered in the parent's coordinate system
                // (before the masked element's own transform), offset for sub-regions.
                AffineTransform maskTransform = new AffineTransform(savedTransform);
                if (effectOffsetX != 0 || effectOffsetY != 0) {
                    maskTransform.preConcatenate(AffineTransform.getTranslateInstance(-effectOffsetX, -effectOffsetY));
                }
                renderedImage = Defs.paintMask(this, node, maskName, renderedImage, maskTransform);
            }
            context = effectBaseContext;
            if (groupOpacity) {
                effectBaseContext.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
            }
            if (filterClip != null) {
                Shape prevClip = effectBaseContext.getClip();
                effectBaseContext.clip(new java.awt.Rectangle(effectOffsetX + filterClip.x,
                        effectOffsetY + filterClip.y, filterClip.width, filterClip.height));
                effectBaseContext.drawImage(renderedImage, effectOffsetX, effectOffsetY, null);
                effectBaseContext.setClip(prevClip);
            } else {
                effectBaseContext.drawImage(renderedImage, effectOffsetX, effectOffsetY, null);
            }
            if (groupOpacity) {
                effectBaseContext.setComposite(savedComposite);
            }
            effectContext.dispose();
            if (ownEffectBuffer) {
                effectBufferInUse = false;
            }
        }

        this.parentNode = oldParentNode;
        this.fontSize = oldFontSize;
        this.contextWidth = oldContextWidth;
        this.contextHeight = oldContextHeight;
    }

    private void configureStroke(Node node) {
        // These are set during draw for each node's stroke
    }

    private void applyClip(Node node) {
        // Clip rect
        String[] rectValues = Helpers.clipRect(node.get("clip"));
        if (rectValues.length == 4) {
            double top = size(this, rectValues[0], "y");
            double right = size(this, rectValues[1], "x");
            double bottom = size(this, rectValues[2], "y");
            double left = size(this, rectValues[3], "x");
            double x = size(this, node.get("x"), "x");
            double y = size(this, node.get("y"), "y");
            double w = size(this, node.get("width"), "x");
            double h = size(this, node.get("height"), "y");
            context.clip(new Rectangle2D.Double(x + left, y + top, w - left - right, h - top - bottom));
        }

        // Clip path
        String clipPathStr = node.get("clip-path");
        if (clipPathStr != null) {
            String clipId = UrlHelper.parseUrl(clipPathStr).fragment();
            if (clipId != null) {
                Node clipNode = this.paths.get(clipId);
                if (clipNode != null) {
                    // Render clip path
                    GeneralPath savedPath = this.path;
                    boolean savedStrokeAndFill = this.strokeAndFill;
                    this.path = new GeneralPath();
                    this.strokeAndFill = false;

                    for (Node child : clipNode.children) {
                        draw(child);
                    }

                    if (!this.path.getPathIterator(null).isDone()) {
                        context.clip(this.path);
                    }

                    this.path = savedPath;
                    this.strokeAndFill = savedStrokeAndFill;
                }
            }
        }
    }

    /** Write output. Override in subclasses. */
    public void finish() throws IOException {
        if (context != null) {
            context.dispose();
        }
    }

    /** Get the rendered image. */
    public BufferedImage getImage() {
        return image;
    }

    private static int getLineCap(String cap) {
        if (cap == null)
            return BasicStroke.CAP_BUTT;
        return switch (cap) {
            case "round" -> BasicStroke.CAP_ROUND;
            case "square" -> BasicStroke.CAP_SQUARE;
            default -> BasicStroke.CAP_BUTT;
        };
    }

    private static int getLineJoin(String join) {
        if (join == null)
            return BasicStroke.JOIN_MITER;
        return switch (join) {
            case "round" -> BasicStroke.JOIN_ROUND;
            case "bevel" -> BasicStroke.JOIN_BEVEL;
            default -> BasicStroke.JOIN_MITER;
        };
    }

    /** Convert RGBA to a clamped AWT Color. */
    static Color toAwtColor(Colors.RGBA c) {
        return new Color((float) Math.max(0, Math.min(1, c.r())), (float) Math.max(0, Math.min(1, c.g())),
                (float) Math.max(0, Math.min(1, c.b())), (float) Math.max(0, Math.min(1, c.a())));
    }

    private static double parseDoubleOr(String s, double def) {
        if (s == null)
            return def;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

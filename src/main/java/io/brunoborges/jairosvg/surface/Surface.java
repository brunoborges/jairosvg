package io.brunoborges.jairosvg.surface;

import static io.brunoborges.jairosvg.util.Helpers.getViewbox;
import static io.brunoborges.jairosvg.util.Helpers.nodeFormat;
import static io.brunoborges.jairosvg.util.Helpers.normalize;
import static io.brunoborges.jairosvg.util.Helpers.parseDoubleOr;
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
import java.awt.font.FontRenderContext;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import io.brunoborges.jairosvg.css.Colors;
import io.brunoborges.jairosvg.dom.BoundingBox;
import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.dom.SvgFont;
import io.brunoborges.jairosvg.draw.Defs;
import io.brunoborges.jairosvg.draw.FilterRenderer;
import io.brunoborges.jairosvg.draw.ImageHandler;
import io.brunoborges.jairosvg.draw.MarkerDrawer;
import io.brunoborges.jairosvg.draw.MaskPainter;
import io.brunoborges.jairosvg.draw.PathDrawer;
import io.brunoborges.jairosvg.draw.ShapeDrawer;
import io.brunoborges.jairosvg.draw.SvgDrawer;
import io.brunoborges.jairosvg.draw.TextDrawer;
import io.brunoborges.jairosvg.util.Helpers;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * Abstract base surface for SVG rendering using Java2D.
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

    // Reusable filter primitive buffers (lazily allocated, shared across filter
    // invocations)
    public BufferedImage filterBuf1, filterBuf2, filterBuf3;

    // Reusable off-screen effect buffer for filters/masks/opacity
    private BufferedImage effectBuffer;
    private boolean effectBufferInUse;

    // Pattern transform (set by PatternPainter, consumed by fill/stroke)
    public AffineTransform paintTransform;

    // Surface dimensions
    protected BufferedImage image;
    protected double width;
    protected double height;
    protected OutputStream output;

    // Color mapping
    private UnaryOperator<Colors.RGBA> mapRgba;

    // Dash array cache (keyed by raw stroke-dasharray string)
    private final Map<String, float[]> dashArrayCache = new HashMap<>();

    // Solid color cache: maps color string → AWT Color for opacity=1.0 (avoids
    // re-parsing hex/named colors and re-creating Color objects on every render)
    private final Map<String, Color> solidColorCache = new HashMap<>();

    // Raster image cache (keyed by data: URI or resolved URL)
    public Map<String, BufferedImage> rasterImageCache = new HashMap<>();

    // Pre-allocated transform stack to avoid AffineTransform allocations per node
    private final AffineTransform[] transformStack = buildTransformStack();
    private int transformDepth = 0;
    private static final AffineTransform IDENTITY_TRANSFORM = new AffineTransform();

    private static AffineTransform[] buildTransformStack() {
        AffineTransform[] s = new AffineTransform[64];
        for (int i = 0; i < s.length; i++)
            s[i] = new AffineTransform();
        return s;
    }

    // Cached FontRenderContext — valid as long as rendering hints don't change
    public FontRenderContext cachedFRC;

    // Gradient stop cache to avoid re-parsing stops on every gradient application
    public record GradientStops(float[] fractions, Color[] colors, double opacity) {
    }
    public final IdentityHashMap<Node, GradientStops> gradientStopCache = new IdentityHashMap<>();

    // ID index for O(1) element lookup (used by <use> and others)
    public final Map<String, Node> nodeById = new HashMap<>();

    public Surface() {
    }

    /** Initialize the surface with optional rendering hint overrides. */
    public void init(Node tree, OutputStream output, double dpi, Double parentWidth, Double parentHeight, double scale,
            Double outputWidth, Double outputHeight, String backgroundColor, UnaryOperator<Colors.RGBA> mapRgba,
            Map<RenderingHints.Key, Object> renderingHintOverrides) {

        this.output = output;
        this.dpi = dpi;
        this.mapRgba = mapRgba;
        this.contextWidth = parentWidth != null ? parentWidth : 0;
        this.contextHeight = parentHeight != null ? parentHeight : 0;
        this.rootNode = tree;

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
        this.cachedFRC = context.getFontRenderContext();
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

        this.fontSize = size(this, node.get("font-size", "12pt"));

        int savedDepth = transformDepth;
        transformStack[transformDepth++].setTransform(context.getTransform());
        Shape savedClip = context.getClip();
        Composite savedComposite = context.getComposite();
        Stroke savedStroke = context.getStroke();

        // Apply transformations
        Helpers.transform(this, node, node.get("transform"), node.get("transform-origin"));

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
            FilterRenderer.prepareFilter(this, node, filterName);
        }

        Graphics2D effectBaseContext = null;
        Graphics2D effectContext = null;
        BufferedImage effectSourceImage = null;
        boolean subRegionEffect = false;
        int ebX = 0, ebY = 0;
        AffineTransform subRegionXform = null;
        if (filterName != null || maskName != null || groupOpacity) {
            effectBaseContext = context;
            int fullW = image.getWidth();
            int fullH = image.getHeight();
            int iw = fullW, ih = fullH;

            // Sub-region effect buffers for mask/opacity-only elements (no filter).
            // Filters are excluded because primitives like feFlood fill the entire
            // buffer, making the result dependent on buffer dimensions. Filter
            // sub-region optimization is handled inside applyFilter() instead.
            if (filterName == null) {
                BoundingBox.Box bbox = BoundingBox.calculate(this, node);
                if (bbox != null && BoundingBox.isNonEmpty(bbox)) {
                    AffineTransform xf = context.getTransform();
                    double[] pts = {bbox.minX(), bbox.minY(), bbox.minX() + bbox.width(), bbox.minY(), bbox.minX(),
                            bbox.minY() + bbox.height(), bbox.minX() + bbox.width(), bbox.minY() + bbox.height()};
                    double[] dst = new double[8];
                    xf.transform(pts, 0, dst, 0, 4);

                    double dMinX = Math.min(Math.min(dst[0], dst[2]), Math.min(dst[4], dst[6]));
                    double dMinY = Math.min(Math.min(dst[1], dst[3]), Math.min(dst[5], dst[7]));
                    double dMaxX = Math.max(Math.max(dst[0], dst[2]), Math.max(dst[4], dst[6]));
                    double dMaxY = Math.max(Math.max(dst[1], dst[3]), Math.max(dst[5], dst[7]));

                    int pad = computeEffectPadding(node);
                    ebX = Math.max(0, (int) Math.floor(dMinX) - pad);
                    ebY = Math.max(0, (int) Math.floor(dMinY) - pad);
                    int ebW = Math.min(fullW, (int) Math.ceil(dMaxX) + pad) - ebX;
                    int ebH = Math.min(fullH, (int) Math.ceil(dMaxY) + pad) - ebY;

                    if (ebW > 0 && ebH > 0 && (long) ebW * ebH < (long) fullW * fullH * 4 / 5) {
                        iw = ebW;
                        ih = ebH;
                        subRegionEffect = true;
                    }
                }
            }

            if (!subRegionEffect && !effectBufferInUse && effectBuffer != null && effectBuffer.getWidth() == iw
                    && effectBuffer.getHeight() == ih) {
                effectSourceImage = effectBuffer;
                java.util.Arrays.fill(
                        ((java.awt.image.DataBufferInt) effectSourceImage.getRaster().getDataBuffer()).getData(), 0);
            } else {
                effectSourceImage = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
                if (!effectBufferInUse && !subRegionEffect) {
                    effectBuffer = effectSourceImage;
                }
            }
            effectBufferInUse = true;
            effectContext = effectSourceImage.createGraphics();
            effectContext.setRenderingHints(effectBaseContext.getRenderingHints());
            if (subRegionEffect) {
                subRegionXform = new AffineTransform(effectBaseContext.getTransform());
                subRegionXform.preConcatenate(AffineTransform.getTranslateInstance(-ebX, -ebY));
                effectContext.setTransform(subRegionXform);
            } else {
                effectContext.setTransform(effectBaseContext.getTransform());
            }
            effectContext.setClip(effectBaseContext.getClip());
            effectContext.setComposite(effectBaseContext.getComposite());
            effectContext.setStroke(effectBaseContext.getStroke());
            context = effectContext;
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

        // Manage display
        boolean display = !"none".equals(node.get("display", "inline"));

        // Fill and stroke — defer opacity/visibility computation until needed
        if (strokeAndFill && drawn && !path.getPathIterator(null).isDone()) {
            boolean visible = display && !"hidden".equals(node.get("visibility", "visible"));
            if (visible) {
                double strokeOpacity = parseDoubleOr(node.get("stroke-opacity"), 1);
                double fillOpacity = parseDoubleOr(node.get("fill-opacity"), 1);
                if (opacity < 1 && node.children.isEmpty()) {
                    strokeOpacity *= opacity;
                    fillOpacity *= opacity;
                }

                // Fill — skip entirely when fill is "none" or fully transparent
                String fillStr = node.get("fill", "black");
                boolean doFill = !"none".equals(fillStr);
                if (doFill) {
                    boolean gradientFill = false;
                    String fillColorStr = fillStr;
                    if (fillStr.charAt(0) == 'u' && fillStr.startsWith("url")) {
                        String[] paintValue = Helpers.paint(fillStr);
                        gradientFill = Defs.gradientOrPattern(this, node, paintValue[0], fillOpacity);
                        if (!gradientFill) {
                            fillColorStr = paintValue[1];
                        }
                    }
                    if (!gradientFill) {
                        if (fillOpacity >= 1.0 && mapRgba == null) {
                            Color cached = solidColorCache.get(fillColorStr);
                            if (cached != null) {
                                context.setColor(cached);
                            } else {
                                Colors.RGBA fillColor = Colors.color(fillColorStr, 1.0);
                                doFill = fillColor.a() > 0;
                                if (doFill) {
                                    cached = toAwtColor(fillColor);
                                    solidColorCache.put(fillColorStr, cached);
                                    context.setColor(cached);
                                }
                            }
                        } else {
                            Colors.RGBA fillColor = mapColor(fillColorStr, fillOpacity);
                            doFill = fillColor.a() > 0;
                            if (doFill)
                                context.setColor(toAwtColor(fillColor));
                        }
                    }
                    if (doFill || gradientFill) {
                        // Set fill rule
                        if ("evenodd".equals(node.get("fill-rule"))) {
                            path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
                        } else {
                            path.setWindingRule(GeneralPath.WIND_NON_ZERO);
                        }
                        paintWithTransform(path, true);
                    }
                }

                // Stroke
                String[] strokePaint = Helpers.paint(node.get("stroke"));
                if (strokePaint[1] != null && !"none".equals(strokePaint[1])) {
                    if (!Defs.gradientOrPattern(this, node, strokePaint[0], strokeOpacity)) {
                        Colors.RGBA strokeColor = mapColor(strokePaint[1], strokeOpacity);
                        if (strokeColor.a() > 0) {
                            context.setColor(toAwtColor(strokeColor));
                        } else {
                            strokePaint[1] = null; // suppress stroke
                        }
                    }

                    if (strokePaint[1] != null) {
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

                        BasicStroke stroke = dashArray != null
                                ? new BasicStroke(strokeWidth, cap, join, miterLimit, dashArray, dashOffset)
                                : new BasicStroke(strokeWidth, cap, join, miterLimit);

                        context.setStroke(stroke);
                        paintWithTransform(path, false);
                    }
                }

                MarkerDrawer.drawMarkers(this, node);
            } else {
                path.reset();
            }
        }

        // Draw children
        if (display && !INVISIBLE_TAGS.contains(node.tag)) {
            for (Node child : node.children) {
                draw(child);
            }
        }

        // Restore state
        transformDepth = savedDepth;
        context.setTransform(transformStack[savedDepth]);
        context.setClip(savedClip);
        context.setComposite(savedComposite);
        context.setStroke(savedStroke);

        if (effectContext != null) {
            // Save pre-node transform in a local variable before mask/filter
            // processing can clobber transformStack[savedDepth] via recursive
            // draw() calls (mask children, feImage, etc.).
            AffineTransform preNodeTransform = new AffineTransform(transformStack[savedDepth]);

            BufferedImage renderedImage = effectSourceImage;
            java.awt.Rectangle filterClip = null;
            if (filterName != null) {
                // Compute the filter region BEFORE filtering so that primitives like
                // feFlood don't bleed into other elements' areas when composited back.
                Node filterNode = this.filters.get(filterName);
                filterClip = FilterRenderer.computeFilterRegion(effectSourceImage, filterNode);
                renderedImage = FilterRenderer.applyFilter(this, filterName, renderedImage, filterClip);
            }
            if (maskName != null) {
                renderedImage = MaskPainter.paintMask(this, node, maskName, renderedImage,
                        subRegionEffect ? subRegionXform : null);
            }
            context = effectBaseContext;
            if (groupOpacity) {
                effectBaseContext.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
            }
            // Full-size effect buffers already contain content at device
            // coordinates (the node's transform was applied during rendering).
            // Composite with identity transform to avoid double-applying it.
            effectBaseContext.setTransform(IDENTITY_TRANSFORM);
            if (subRegionEffect) {
                effectBaseContext.drawImage(renderedImage, ebX, ebY, null);
            } else {
                if (filterClip != null) {
                    Shape prevClip = effectBaseContext.getClip();
                    effectBaseContext.clip(filterClip);
                    effectBaseContext.drawImage(renderedImage, 0, 0, null);
                    effectBaseContext.setClip(prevClip);
                } else {
                    effectBaseContext.drawImage(renderedImage, 0, 0, null);
                }
            }
            // Restore to pre-node transform (not the node-transformed state),
            // preventing the node's transform from leaking to subsequent siblings.
            effectBaseContext.setTransform(preNodeTransform);
            if (groupOpacity) {
                effectBaseContext.setComposite(savedComposite);
            }
            effectContext.dispose();
            effectBufferInUse = false;
        }

        this.parentNode = oldParentNode;
        this.fontSize = oldFontSize;
        this.contextWidth = oldContextWidth;
        this.contextHeight = oldContextHeight;
    }

    private void configureStroke(Node node) {
        // These are set during draw for each node's stroke
    }

    /**
     * Compute device-pixel padding for the sub-region effect buffer. Accounts for
     * stroke width extent.
     */
    private int computeEffectPadding(Node node) {
        int pad = 4; // base padding for anti-aliasing and rounding

        // Stroke extent
        String stroke = node.get("stroke", "none");
        if (!"none".equals(stroke)) {
            double sw = size(this, node.get("stroke-width", "1"));
            AffineTransform xf = context.getTransform();
            double sx = Math.sqrt(xf.getScaleX() * xf.getScaleX() + xf.getShearX() * xf.getShearX());
            double sy = Math.sqrt(xf.getScaleY() * xf.getScaleY() + xf.getShearY() * xf.getShearY());
            pad += (int) Math.ceil(sw * Math.max(sx, sy) / 2) + 1;
        }

        return pad;
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

    /**
     * Paint a shape (fill or stroke) while honouring a pending
     * {@link #paintTransform}. When a pattern's {@code patternTransform} is active
     * the Graphics2D coordinate system is temporarily transformed so that the
     * TexturePaint tiling grid is correctly scaled/rotated/skewed.
     */
    private void paintWithTransform(Shape shape, boolean fill) {
        if (paintTransform != null) {
            AffineTransform saved = context.getTransform();
            context.transform(paintTransform);
            try {
                Shape mapped = paintTransform.createInverse().createTransformedShape(shape);
                if (fill)
                    context.fill(mapped);
                else
                    context.draw(mapped);
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                if (fill)
                    context.fill(shape);
                else
                    context.draw(shape);
            }
            context.setTransform(saved);
            paintTransform = null;
        } else {
            if (fill)
                context.fill(shape);
            else
                context.draw(shape);
        }
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
}

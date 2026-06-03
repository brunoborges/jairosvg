package io.brunoborges.jairosvg;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import io.brunoborges.jairosvg.css.Colors;
import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.surface.*;

/**
 * JairoSVG — SVG 1.1 to PNG, PDF, and PS converter powered by Java2D.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>{@code
 * // Convert SVG to PNG bytes
 * byte[] png = JairoSVG.svg2png(svgBytes);
 *
 * // Convert SVG file to PDF file
 * JairoSVG.svg2pdf(Path.of("input.svg"), Path.of("output.pdf"));
 *
 * // Convert with options
 * byte[] png = JairoSVG.builder().fromBytes(svgBytes).dpi(150).scale(2).backgroundColor("#ffffff").toPng();
 * }</pre>
 */
public final class JairoSVG {

    public static final String VERSION = loadVersion();

    private static String loadVersion() {
        try (var is = JairoSVG.class.getResourceAsStream("/jairosvg.properties")) {
            if (is != null) {
                var props = new java.util.Properties();
                props.load(is);
                return props.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            // fall through
        }
        return "unknown";
    }

    private JairoSVG() {
    }

    // ---- Simple API ----

    /** Convert SVG bytes to PNG bytes. */
    public static byte[] svg2png(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toPng();
    }

    /** Convert SVG bytes to JPEG bytes. */
    public static byte[] svg2jpeg(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toJpeg();
    }

    /** Convert SVG bytes to TIFF bytes. */
    public static byte[] svg2tiff(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toTiff();
    }

    /** Convert SVG bytes to PDF bytes. */
    public static byte[] svg2pdf(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toPdf();
    }

    /** Convert SVG bytes to PostScript bytes. */
    public static byte[] svg2ps(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toPs();
    }

    /** Convert SVG bytes to Encapsulated PostScript bytes. */
    public static byte[] svg2eps(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toEps();
    }

    /** Convert SVG file to PNG file. */
    public static void svg2png(Path input, Path output) throws Exception {
        byte[] result = builder().fromFile(input).toPng();
        Files.write(output, result);
    }

    /** Convert SVG file to JPEG file. */
    public static void svg2jpeg(Path input, Path output) throws Exception {
        byte[] result = builder().fromFile(input).toJpeg();
        Files.write(output, result);
    }

    /** Convert SVG file to TIFF file. */
    public static void svg2tiff(Path input, Path output) throws Exception {
        byte[] result = builder().fromFile(input).toTiff();
        Files.write(output, result);
    }

    /** Convert SVG file to PDF file. */
    public static void svg2pdf(Path input, Path output) throws Exception {
        byte[] result = builder().fromFile(input).toPdf();
        Files.write(output, result);
    }

    /** Convert SVG file to PostScript file. */
    public static void svg2ps(Path input, Path output) throws Exception {
        byte[] result = builder().fromFile(input).toPs();
        Files.write(output, result);
    }

    /** Convert SVG file to Encapsulated PostScript file. */
    public static void svg2eps(Path input, Path output) throws Exception {
        byte[] result = builder().fromFile(input).toEps();
        Files.write(output, result);
    }

    /** Convert SVG URL to PNG bytes. */
    public static byte[] svg2png(String url) throws Exception {
        return builder().fromUrl(url).toPng();
    }

    /** Convert SVG URL to JPEG bytes. */
    public static byte[] svg2jpeg(String url) throws Exception {
        return builder().fromUrl(url).toJpeg();
    }

    /** Convert SVG URL to TIFF bytes. */
    public static byte[] svg2tiff(String url) throws Exception {
        return builder().fromUrl(url).toTiff();
    }

    /** Convert SVG URL to PDF bytes. */
    public static byte[] svg2pdf(String url) throws Exception {
        return builder().fromUrl(url).toPdf();
    }

    /** Convert SVG URL to PostScript bytes. */
    public static byte[] svg2ps(String url) throws Exception {
        return builder().fromUrl(url).toPs();
    }

    /** Convert SVG URL to Encapsulated PostScript bytes. */
    public static byte[] svg2eps(String url) throws Exception {
        return builder().fromUrl(url).toEps();
    }

    // ---- Builder API ----

    /** Create a new conversion builder. */
    public static ConversionBuilder builder() {
        return new ConversionBuilder();
    }

    /**
     * Fluent builder for SVG conversion.
     */
    public static final class ConversionBuilder {

        private byte[] bytestring;
        private String url;
        private double dpi = 96;
        private Double parentWidth;
        private Double parentHeight;
        private double scale = 1;
        private boolean unsafe = false;
        private String backgroundColor;
        private boolean negateColors = false;
        private Double outputWidth;
        private Double outputHeight;
        private int pngCompressionLevel = -1;
        private float jpegQuality = -1f;
        private String tiffCompressionType;
        private Map<RenderingHints.Key, Object> renderingHints;

        ConversionBuilder() {
        }

        /** Set SVG input from a byte array. */
        public ConversionBuilder fromBytes(byte[] svgBytes) {
            this.bytestring = svgBytes;
            return this;
        }

        /** Set SVG input from a string. */
        public ConversionBuilder fromString(String svgString) {
            this.bytestring = svgString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return this;
        }

        /** Set SVG input from a file path. */
        public ConversionBuilder fromFile(Path path) throws IOException {
            this.bytestring = Files.readAllBytes(path);
            this.url = path.toUri().toString();
            return this;
        }

        /** Set SVG input from an input stream. */
        public ConversionBuilder fromStream(InputStream stream) throws IOException {
            this.bytestring = stream.readAllBytes();
            return this;
        }

        /** Set SVG input from a URL string. */
        public ConversionBuilder fromUrl(String url) {
            this.url = url;
            return this;
        }

        /** Set the DPI for conversion (default: 96). */
        public ConversionBuilder dpi(double dpi) {
            this.dpi = dpi;
            return this;
        }

        /** Set the parent container width for percentage-based sizing. */
        public ConversionBuilder parentWidth(double width) {
            this.parentWidth = width;
            return this;
        }

        /** Set the parent container height for percentage-based sizing. */
        public ConversionBuilder parentHeight(double height) {
            this.parentHeight = height;
            return this;
        }

        /** Set the output scaling factor (default: 1). */
        public ConversionBuilder scale(double scale) {
            this.scale = scale;
            return this;
        }

        /** Allow external file access and XML entities when true. */
        public ConversionBuilder unsafe(boolean unsafe) {
            this.unsafe = unsafe;
            return this;
        }

        /** Set the output background color (e.g. "#ffffff"). */
        public ConversionBuilder backgroundColor(String color) {
            this.backgroundColor = color;
            return this;
        }

        /** Negate vector colors when true. */
        public ConversionBuilder negateColors(boolean negate) {
            this.negateColors = negate;
            return this;
        }

        /** Set the desired output width in pixels. */
        public ConversionBuilder outputWidth(double width) {
            this.outputWidth = width;
            return this;
        }

        /** Set the desired output height in pixels. */
        public ConversionBuilder outputHeight(double height) {
            this.outputHeight = height;
            return this;
        }

        /**
         * Set PNG compression level (0-9). 0 = no compression (fastest), 9 = max
         * compression (smallest). Default uses the JDK default (~6).
         */
        public ConversionBuilder pngCompressionLevel(int level) {
            this.pngCompressionLevel = level;
            return this;
        }

        /**
         * Set JPEG quality (0.0-1.0). 0.0 = lowest quality (smallest file), 1.0 =
         * highest quality (largest file). Default uses the JDK default (~0.75).
         */
        public ConversionBuilder jpegQuality(float quality) {
            this.jpegQuality = quality;
            return this;
        }

        /**
         * Set TIFF compression type. Common values: "Deflate", "LZW", "JPEG", "ZLib",
         * "PackBits", "Uncompressed". Default uses the writer's default.
         */
        public ConversionBuilder tiffCompressionType(String type) {
            this.tiffCompressionType = type;
            return this;
        }

        /**
         * Set a Java2D rendering hint. Overrides the default value for the given key.
         * Can be called multiple times for different keys.
         *
         * <p>
         * Defaults (applied if not overridden, matching JSVG):
         * </p>
         * <ul>
         * <li>{@code KEY_ANTIALIASING} → {@code VALUE_ANTIALIAS_ON}</li>
         * <li>{@code KEY_STROKE_CONTROL} → {@code VALUE_STROKE_PURE}</li>
         * </ul>
         *
         * <p>
         * Example — enable quality rendering for higher fidelity:
         * </p>
         *
         * <pre>{@code
         * JairoSVG.builder().fromBytes(svg).renderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
         * 		.toPng();
         * }</pre>
         *
         * @param key
         *            the rendering hint key (e.g.,
         *            {@code RenderingHints.KEY_RENDERING})
         * @param value
         *            the rendering hint value (e.g.,
         *            {@code RenderingHints.VALUE_RENDER_QUALITY})
         * @return this builder
         * @see java.awt.RenderingHints
         */
        public ConversionBuilder renderingHint(RenderingHints.Key key, Object value) {
            if (this.renderingHints == null) {
                this.renderingHints = new HashMap<>();
            }
            this.renderingHints.put(key, value);
            return this;
        }

        /** Convert to PNG bytes. */
        public byte[] toPng() throws Exception {
            var surface = new PngSurface();
            if (pngCompressionLevel >= 0)
                surface.setCompressionLevel(pngCompressionLevel);
            return convert(surface);
        }

        /** Convert to PNG and write to output stream. */
        public void toPng(OutputStream out) throws Exception {
            var surface = new PngSurface();
            if (pngCompressionLevel >= 0)
                surface.setCompressionLevel(pngCompressionLevel);
            convert(surface, out);
        }

        /** Convert to JPEG bytes. */
        public byte[] toJpeg() throws Exception {
            var surface = new JpegSurface();
            if (jpegQuality >= 0f)
                surface.setQuality(jpegQuality);
            return convert(surface);
        }

        /** Convert to JPEG and write to output stream. */
        public void toJpeg(OutputStream out) throws Exception {
            var surface = new JpegSurface();
            if (jpegQuality >= 0f)
                surface.setQuality(jpegQuality);
            convert(surface, out);
        }

        /** Convert to TIFF bytes. */
        public byte[] toTiff() throws Exception {
            var surface = new TiffSurface();
            if (tiffCompressionType != null)
                surface.setCompressionType(tiffCompressionType);
            return convert(surface);
        }

        /** Convert to TIFF and write to output stream. */
        public void toTiff(OutputStream out) throws Exception {
            var surface = new TiffSurface();
            if (tiffCompressionType != null)
                surface.setCompressionType(tiffCompressionType);
            convert(surface, out);
        }

        /** Convert to PDF bytes. */
        public byte[] toPdf() throws Exception {
            checkPdfBoxAvailable();
            return convert(new PdfSurface());
        }

        /** Convert to PDF and write to output stream. */
        public void toPdf(OutputStream out) throws Exception {
            checkPdfBoxAvailable();
            convert(new PdfSurface(), out);
        }

        /** Convert to PS bytes. */
        public byte[] toPs() throws Exception {
            return convert(new PsSurface());
        }

        /** Convert to PS and write to output stream. */
        public void toPs(OutputStream out) throws Exception {
            convert(new PsSurface(), out);
        }

        /** Convert to EPS bytes. */
        public byte[] toEps() throws Exception {
            return convert(new PsSurface(true));
        }

        /** Convert to EPS and write to output stream. */
        public void toEps(OutputStream out) throws Exception {
            convert(new PsSurface(true), out);
        }

        /** Convert and return the rendered BufferedImage (useful for in-memory use). */
        public BufferedImage toImage() throws Exception {
            Node tree = parseInput();
            var surface = new PngSurface();
            initSurface(surface, tree, new ByteArrayOutputStream());
            return surface.getImage();
        }

        private byte[] convert(Surface surface) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            convert(surface, baos);
            return baos.toByteArray();
        }

        private void convert(Surface surface, OutputStream out) throws Exception {
            Node tree = parseInput();
            initSurface(surface, tree, out);
            try {
                surface.finish();
            } catch (Exception e) {
                surface.context.dispose();
                throw e;
            }
        }

        private void initSurface(Surface surface, Node tree, OutputStream out) {
            UnaryOperator<in.virit.color.Color> colorMapper = negateColors ? in.virit.color.Color::negate : null;

            surface.init(tree, out, dpi, parentWidth, parentHeight, scale, outputWidth, outputHeight, backgroundColor,
                    colorMapper, renderingHints);
        }

        private Node parseInput() throws Exception {
            byte[] data = this.bytestring;
            if (data == null && this.url != null) {
                return Node.parseTree(null, this.url, unsafe);
            }
            if (data == null) {
                throw new IllegalArgumentException(
                        "No input. Use fromBytes(), fromFile(), fromUrl(), or fromStream().");
            }
            return Node.parseTree(data, this.url, unsafe);
        }

        private static void checkPdfBoxAvailable() {
            checkClassAvailable("org.apache.pdfbox.pdmodel.PDDocument",
                    "PDF output requires Apache PDFBox. Add org.apache.pdfbox:pdfbox to your classpath.");
        }

        static void checkClassAvailable(String className, String errorMessage) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException(errorMessage, e);
            }
        }
    }
}

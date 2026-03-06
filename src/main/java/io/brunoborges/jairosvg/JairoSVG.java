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
 * JairoSVG - A Java port of CairoSVG. SVG 1.1 to PNG, PDF, and PS converter.
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

    public static final String VERSION = "1.0.2";

    private JairoSVG() {
    }

    // ---- Simple API ----

    /** Convert SVG bytes to PNG bytes. */
    public static byte[] svg2png(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toPng();
    }

    /** Convert SVG bytes to PDF bytes. */
    public static byte[] svg2pdf(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toPdf();
    }

    /** Convert SVG file to PNG file. */
    public static void svg2png(Path input, Path output) throws Exception {
        byte[] result = builder().fromFile(input).toPng();
        Files.write(output, result);
    }

    /** Convert SVG file to PDF file. */
    public static void svg2pdf(Path input, Path output) throws Exception {
        byte[] result = builder().fromFile(input).toPdf();
        Files.write(output, result);
    }

    /** Convert SVG URL to PNG bytes. */
    public static byte[] svg2png(String url) throws Exception {
        return builder().fromUrl(url).toPng();
    }

    /** Convert SVG URL to PDF bytes. */
    public static byte[] svg2pdf(String url) throws Exception {
        return builder().fromUrl(url).toPdf();
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
        private InputStream fileObj;
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

        public ConversionBuilder fromBytes(byte[] svgBytes) {
            this.bytestring = svgBytes;
            return this;
        }

        public ConversionBuilder fromString(String svgString) {
            this.bytestring = svgString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return this;
        }

        public ConversionBuilder fromFile(Path path) throws IOException {
            this.bytestring = Files.readAllBytes(path);
            this.url = path.toUri().toString();
            return this;
        }

        public ConversionBuilder fromStream(InputStream stream) throws IOException {
            this.bytestring = stream.readAllBytes();
            return this;
        }

        public ConversionBuilder fromUrl(String url) {
            this.url = url;
            return this;
        }

        public ConversionBuilder dpi(double dpi) {
            this.dpi = dpi;
            return this;
        }

        public ConversionBuilder parentWidth(double width) {
            this.parentWidth = width;
            return this;
        }

        public ConversionBuilder parentHeight(double height) {
            this.parentHeight = height;
            return this;
        }

        public ConversionBuilder scale(double scale) {
            this.scale = scale;
            return this;
        }

        public ConversionBuilder unsafe(boolean unsafe) {
            this.unsafe = unsafe;
            return this;
        }

        public ConversionBuilder backgroundColor(String color) {
            this.backgroundColor = color;
            return this;
        }

        public ConversionBuilder negateColors(boolean negate) {
            this.negateColors = negate;
            return this;
        }

        public ConversionBuilder outputWidth(double width) {
            this.outputWidth = width;
            return this;
        }

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
         * Defaults (applied if not overridden):
         * </p>
         * <ul>
         * <li>{@code KEY_ANTIALIASING} → {@code VALUE_ANTIALIAS_ON}</li>
         * <li>{@code KEY_TEXT_ANTIALIASING} → {@code VALUE_TEXT_ANTIALIAS_ON}</li>
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

        /** Convert to EPS bytes. */
        public byte[] toEps() throws Exception {
            return convert(new PsSurface(true));
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
            surface.finish();
        }

        private void initSurface(Surface surface, Node tree, OutputStream out) {
            UnaryOperator<Colors.RGBA> colorMapper = negateColors ? Colors::negateColor : null;
            // invertImages handling would require a BufferedImage mapper

            surface.init(tree, out, dpi, null, parentWidth, parentHeight, scale, outputWidth, outputHeight,
                    backgroundColor, colorMapper, renderingHints);
        }

        private Node parseInput() throws Exception {
            byte[] data = this.bytestring;
            if (data == null && this.fileObj != null) {
                data = this.fileObj.readAllBytes();
            }
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
            try {
                Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException(
                        "PDF output requires Apache PDFBox. Add org.apache.pdfbox:pdfbox to your classpath.", e);
            }
        }
    }
}

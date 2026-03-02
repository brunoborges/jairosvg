package com.jairosvg;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

/**
 * JairoSVG - A Java port of CairoSVG.
 * SVG 1.1 to PNG, PDF, PS and SVG converter.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Convert SVG to PNG bytes
 * byte[] png = JairoSVG.svg2png(svgBytes);
 *
 * // Convert SVG file to PDF file
 * JairoSVG.svg2pdf(Path.of("input.svg"), Path.of("output.pdf"));
 *
 * // Convert with options
 * byte[] png = JairoSVG.builder()
 *     .fromBytes(svgBytes)
 *     .dpi(150)
 *     .scale(2)
 *     .backgroundColor("#ffffff")
 *     .toPng();
 * }</pre>
 */
public final class JairoSVG {

    public static final String VERSION = "1.0.0";

    private JairoSVG() {}

    // ---- Simple API ----

    /** Convert SVG bytes to PNG bytes. */
    public static byte[] svg2png(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toPng();
    }

    /** Convert SVG bytes to PDF bytes. */
    public static byte[] svg2pdf(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toPdf();
    }

    /** Convert SVG bytes to SVG bytes (re-render). */
    public static byte[] svg2svg(byte[] svgBytes) throws Exception {
        return builder().fromBytes(svgBytes).toSvg();
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
        private boolean invertImages = false;
        private Double outputWidth;
        private Double outputHeight;

        ConversionBuilder() {}

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

        public ConversionBuilder invertImages(boolean invert) {
            this.invertImages = invert;
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

        /** Convert to PNG bytes. */
        public byte[] toPng() throws Exception {
            return convert(new PngSurface());
        }

        /** Convert to PNG and write to output stream. */
        public void toPng(OutputStream out) throws Exception {
            convert(new PngSurface(), out);
        }

        /** Convert to PDF bytes. */
        public byte[] toPdf() throws Exception {
            return convert(new PdfSurface());
        }

        /** Convert to PDF and write to output stream. */
        public void toPdf(OutputStream out) throws Exception {
            convert(new PdfSurface(), out);
        }

        /** Convert to SVG bytes. */
        public byte[] toSvg() throws Exception {
            return convert(new SvgSurface());
        }

        /** Convert to SVG and write to output stream. */
        public void toSvg(OutputStream out) throws Exception {
            convert(new SvgSurface(), out);
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

            surface.init(tree, out, dpi, null,
                parentWidth, parentHeight, scale,
                outputWidth, outputHeight,
                backgroundColor, colorMapper, null);
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
    }
}

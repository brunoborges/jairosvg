package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConversionBuilderTest {

    private static final String SIMPLE_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
              <rect width="50" height="50" fill="red"/>
            </svg>
            """;

    // ---- Static convenience methods ----

    @Test
    void testSvg2pdfPathToPath(@TempDir Path tempDir) throws Exception {
        Path svgFile = tempDir.resolve("test.svg");
        Path pdfFile = tempDir.resolve("test.pdf");
        Files.writeString(svgFile, SIMPLE_SVG);

        JairoSVG.svg2pdf(svgFile, pdfFile);

        assertTrue(Files.exists(pdfFile));
        byte[] pdf = Files.readAllBytes(pdfFile);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void testSvg2pngFromUrl(@TempDir Path tempDir) throws Exception {
        Path svgFile = tempDir.resolve("test.svg");
        Files.writeString(svgFile, SIMPLE_SVG);

        byte[] png = JairoSVG.svg2png(svgFile.toUri().toString());

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(50, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @Test
    void testSvg2pdfFromUrl(@TempDir Path tempDir) throws Exception {
        Path svgFile = tempDir.resolve("test.svg");
        Files.writeString(svgFile, SIMPLE_SVG);

        byte[] pdf = JairoSVG.svg2pdf(svgFile.toUri().toString());

        assertNotNull(pdf);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
    }

    // ---- Builder input methods ----

    @Test
    void testFromStream() throws Exception {
        InputStream stream = new ByteArrayInputStream(SIMPLE_SVG.getBytes(StandardCharsets.UTF_8));

        byte[] png = JairoSVG.builder().fromStream(stream).toPng();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(50, img.getWidth());
    }

    @Test
    void testParentWidth() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50%" height="40">
                  <rect width="100%" height="100%" fill="blue"/>
                </svg>
                """;

        byte[] png = JairoSVG.builder().fromString(svg).parentWidth(200).toPng();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(100, img.getWidth());
    }

    @Test
    void testParentHeight() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="40" height="50%">
                  <rect width="100%" height="100%" fill="blue"/>
                </svg>
                """;

        byte[] png = JairoSVG.builder().fromString(svg).parentHeight(200).toPng();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(100, img.getHeight());
    }

    // ---- Output format options ----

    @Test
    void testPngCompressionLevel() throws Exception {
        byte[] uncompressed = JairoSVG.builder().fromString(SIMPLE_SVG).pngCompressionLevel(0).toPng();

        byte[] maxCompressed = JairoSVG.builder().fromString(SIMPLE_SVG).pngCompressionLevel(9).toPng();

        assertNotNull(uncompressed);
        assertNotNull(maxCompressed);
        // Uncompressed should be larger than max compressed
        assertTrue(uncompressed.length >= maxCompressed.length, "Uncompressed (%d) should be >= max compressed (%d)"
                .formatted(uncompressed.length, maxCompressed.length));
    }

    @Test
    void testPngCompressionLevelToStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JairoSVG.builder().fromString(SIMPLE_SVG).pngCompressionLevel(5).toPng(out);

        assertTrue(out.size() > 0);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(50, img.getWidth());
    }

    @Test
    void testJpegQuality() throws Exception {
        byte[] lowQ = JairoSVG.builder().fromString(SIMPLE_SVG).jpegQuality(0.1f).toJpeg();

        byte[] highQ = JairoSVG.builder().fromString(SIMPLE_SVG).jpegQuality(1.0f).toJpeg();

        assertNotNull(lowQ);
        assertNotNull(highQ);
        // Higher quality should generally produce larger file
        assertTrue(highQ.length >= lowQ.length,
                "High quality (%d) should be >= low quality (%d)".formatted(highQ.length, lowQ.length));
    }

    @Test
    void testJpegQualityToStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JairoSVG.builder().fromString(SIMPLE_SVG).jpegQuality(0.8f).toJpeg(out);

        assertTrue(out.size() > 0);
        // JPEG magic bytes
        byte[] data = out.toByteArray();
        assertEquals((byte) 0xFF, data[0]);
        assertEquals((byte) 0xD8, data[1]);
    }

    @Test
    void testTiffCompressionType() throws Exception {
        byte[] tiff = JairoSVG.builder().fromString(SIMPLE_SVG).tiffCompressionType("Deflate").toTiff();

        assertNotNull(tiff);
        assertTrue(tiff.length > 0);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(tiff));
        assertEquals(50, img.getWidth());
    }

    @Test
    void testTiffCompressionTypeToStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JairoSVG.builder().fromString(SIMPLE_SVG).tiffCompressionType("LZW").toTiff(out);

        assertTrue(out.size() > 0);
    }

    @Test
    void testRenderingHint() throws Exception {
        byte[] png = JairoSVG.builder().fromString(SIMPLE_SVG)
                .renderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                .renderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON).toPng();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(50, img.getWidth());
    }

    // ---- PDF OutputStream ----

    @Test
    void testToPdfOutputStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JairoSVG.builder().fromString(SIMPLE_SVG).toPdf(out);

        byte[] pdf = out.toByteArray();
        assertTrue(pdf.length > 0);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
    }

    // ---- parseInput branches ----

    @Test
    void testParseInputUrlOnly(@TempDir Path tempDir) throws Exception {
        Path svgFile = tempDir.resolve("input.svg");
        Files.writeString(svgFile, SIMPLE_SVG);

        byte[] png = JairoSVG.builder().fromUrl(svgFile.toUri().toString()).toPng();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(50, img.getWidth());
    }

    @Test
    void testParseInputNoInput() {
        var builder = JairoSVG.builder();

        var ex = assertThrows(IllegalArgumentException.class, builder::toPng);
        assertTrue(ex.getMessage().contains("No input"));
    }

    // ---- Error handling ----

    @Test
    void testConvertFinishError() {
        OutputStream broken = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Broken stream");
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("Broken stream");
            }
        };

        assertThrows(Exception.class, () -> JairoSVG.builder().fromString(SIMPLE_SVG).toPng(broken));
    }

    @Test
    void testCheckClassAvailableSuccess() {
        // Should not throw — String is always on classpath
        assertDoesNotThrow(() -> JairoSVG.ConversionBuilder.checkClassAvailable("java.lang.String", "should not fail"));
    }

    @Test
    void testCheckClassAvailableMissing() {
        var ex = assertThrows(UnsupportedOperationException.class,
                () -> JairoSVG.ConversionBuilder.checkClassAvailable("com.nonexistent.FakeClass", "Not found"));

        assertTrue(ex.getMessage().contains("Not found"));
        assertInstanceOf(ClassNotFoundException.class, ex.getCause());
    }
}

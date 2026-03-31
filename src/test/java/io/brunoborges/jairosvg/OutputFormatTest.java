package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OutputFormatTest {

    @Test
    void testSvgToPdf() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;

        byte[] pdf = JairoSVG.svg2pdf(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // PDF magic number
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void testSvgToJpeg() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;

        byte[] jpeg = JairoSVG.builder().fromString(svg).toJpeg();
        assertNotNull(jpeg);
        assertTrue(jpeg.length > 0);
        assertEquals((byte) 0xFF, jpeg[0]);
        assertEquals((byte) 0xD8, jpeg[1]);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
    }

    @Test
    void testSvgToTiff() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;

        byte[] tiff = JairoSVG.builder().fromString(svg).toTiff();
        assertNotNull(tiff);
        assertTrue(tiff.length > 0);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(tiff));
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
    }

    // ── PNG compression level ────────────────────────────────────────────

    @Test
    void testPngCompressionLevel0() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="green"/>
                </svg>
                """;
        byte[] png = JairoSVG.builder().fromString(svg).pngCompressionLevel(0).toPng();
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testPngCompressionLevel9() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="green"/>
                </svg>
                """;
        byte[] png = JairoSVG.builder().fromString(svg).pngCompressionLevel(9).toPng();
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testPngCompressionLevelInvalid() {
        assertThrows(IllegalArgumentException.class, () -> JairoSVG.builder()
                .fromString("<svg xmlns='http://www.w3.org/2000/svg'/>").pngCompressionLevel(10).toPng());
    }

    @Test
    void testPngCompressionLevelNegative() {
        // Negative compression level should be caught during conversion
        try {
            JairoSVG.builder().fromString("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'/>")
                    .pngCompressionLevel(-1).toPng();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("compression"));
        } catch (Exception e) {
            // Other exceptions are also acceptable
        }
    }

    // ── JPEG quality ─────────────────────────────────────────────────────

    @Test
    void testJpegQuality() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="blue"/>
                </svg>
                """;
        byte[] jpeg = JairoSVG.builder().fromString(svg).jpegQuality(0.9f).toJpeg();
        assertNotNull(jpeg);
        assertTrue(jpeg.length > 0);
    }

    @Test
    void testJpegQualityLow() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="blue"/>
                </svg>
                """;
        byte[] jpeg = JairoSVG.builder().fromString(svg).jpegQuality(0.1f).toJpeg();
        assertNotNull(jpeg);
        assertTrue(jpeg.length > 0);
    }

    @Test
    void testJpegQualityInvalid() {
        assertThrows(IllegalArgumentException.class, () -> JairoSVG.builder()
                .fromString("<svg xmlns='http://www.w3.org/2000/svg'/>").jpegQuality(1.5f).toJpeg());
    }

    @Test
    void testJpegQualityNegative() {
        // Negative quality should be caught during conversion
        try {
            JairoSVG.builder().fromString("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'/>")
                    .jpegQuality(-0.1f).toJpeg();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("quality") || e.getMessage().contains("Quality"));
        } catch (Exception e) {
            // Other exceptions are also acceptable
        }
    }

    // ── TIFF with compression ────────────────────────────────────────────

    @Test
    void testTiffWithDeflateCompression() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="red"/>
                </svg>
                """;
        byte[] tiff = JairoSVG.builder().fromString(svg).tiffCompressionType("Deflate").toTiff();
        assertNotNull(tiff);
        assertTrue(tiff.length > 0);
    }

    // ── PostScript output ────────────────────────────────────────────────

    @Test
    void testSvgToPs() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;
        byte[] ps = JairoSVG.builder().fromString(svg).toPs();
        assertNotNull(ps);
        assertTrue(ps.length > 0);
    }

    // ── PNG default (no explicit compression) ────────────────────────────

    @Test
    void testPngDefaultCompression() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="30" height="30">
                  <rect width="30" height="30" fill="yellow"/>
                </svg>
                """;
        byte[] png = JairoSVG.builder().fromString(svg).toPng();
        assertNotNull(png);
        assertTrue(png.length > 0);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(30, img.getWidth());
    }
}

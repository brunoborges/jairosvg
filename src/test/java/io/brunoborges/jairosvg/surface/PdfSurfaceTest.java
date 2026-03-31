package io.brunoborges.jairosvg.surface;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.brunoborges.jairosvg.JairoSVG;

class PdfSurfaceTest {

    private static final String SIMPLE_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
              <rect width="50" height="50" fill="blue"/>
            </svg>
            """;

    @Test
    void testFinishWithNullOutput() throws Exception {
        // PdfSurface.finish() should return early when output is null
        var surface = new PdfSurface();
        surface.image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        // output is null — finish should be a no-op
        assertDoesNotThrow(() -> surface.finish());
    }

    @Test
    void testFinishWithNullImage() throws Exception {
        // PdfSurface.finish() should return early when image is null
        var surface = new PdfSurface();
        surface.output = new ByteArrayOutputStream();
        // image is null — finish should be a no-op
        assertDoesNotThrow(() -> surface.finish());
    }

    @Test
    void testFinishWithBothNull() throws Exception {
        // PdfSurface.finish() should return early when both are null
        var surface = new PdfSurface();
        assertDoesNotThrow(() -> surface.finish());
    }

    @Test
    void testPdfContainsValidContent() throws Exception {
        byte[] pdf = JairoSVG.builder().fromString(SIMPLE_SVG).toPdf();

        // Verify PDF structure
        String pdfStr = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(pdfStr.startsWith("%PDF-"));
        assertTrue(pdfStr.contains("%%EOF"));
    }

    @Test
    void testPdfOutputStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JairoSVG.builder().fromString(SIMPLE_SVG).toPdf(out);

        byte[] pdf = out.toByteArray();
        assertTrue(pdf.length > 100);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void testPdfWithScaleAndBackground() throws Exception {
        byte[] pdf = JairoSVG.builder().fromString(SIMPLE_SVG).scale(2).backgroundColor("white").toPdf();

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String pdfStr = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(pdfStr.startsWith("%PDF-"));
    }
}

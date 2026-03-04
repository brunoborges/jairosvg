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

}

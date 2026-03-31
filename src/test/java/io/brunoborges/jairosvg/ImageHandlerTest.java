package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ImageHandlerTest {

    private static String redPngBase64;
    private static String blueSvgBase64;

    @BeforeAll
    static void createTestImages() throws Exception {
        redPngBase64 = createBase64Png(255, 0, 0);
        String blueSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
                  <rect width="10" height="10" fill="blue"/>
                </svg>""";
        blueSvgBase64 = Base64.getEncoder().encodeToString(blueSvg.getBytes(StandardCharsets.UTF_8));
    }

    private static String createBase64Png(int r, int g, int b) throws Exception {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(r, g, b));
        g2.fillRect(0, 0, 4, 4);
        g2.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Test
    void testEmbeddedRasterImageDataUri() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="white"/>
                  <image href="data:image/png;base64,%s" x="10" y="10" width="30" height="30"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        // Center of the image area should be red
        int[] c = rgba(img, 25, 25);
        assertTrue(c[0] > 200, "Red channel should be high, got " + c[0]);
        assertTrue(c[1] < 50, "Green channel should be low, got " + c[1]);
        assertTrue(c[2] < 50, "Blue channel should be low, got " + c[2]);
    }

    @Test
    void testEmbeddedSvgImage() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/svg+xml;base64,%s" x="10" y="10" width="50" height="50"/>
                </svg>""".formatted(blueSvgBase64);
        BufferedImage img = render(svg);
        // Should have blue pixels in the image area
        boolean hasBlue = false;
        for (int y = 15; y < 55 && !hasBlue; y++) {
            for (int x = 15; x < 55 && !hasBlue; x++) {
                int[] c = rgba(img, x, y);
                if (c[3] > 200 && c[2] > 200 && c[0] < 50 && c[1] < 50) {
                    hasBlue = true;
                }
            }
        }
        assertTrue(hasBlue, "Embedded SVG image should produce blue pixels");
    }

    @Test
    void testImageWithOpacity() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="white"/>
                  <image href="data:image/png;base64,%s" x="0" y="0" width="50" height="50" opacity="0.5"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        // With 50% opacity on a white background, red should be blended
        int[] c = rgba(img, 25, 25);
        // The red channel should be dimmed (not full 255) because of blending with
        // white
        assertTrue(c[0] > 100 && c[0] <= 255, "Red with opacity should be blended, got " + c[0]);
        // Green should be higher than 0 due to white background showing through
        assertTrue(c[1] > 50, "Green should show white blending through, got " + c[1]);
    }

    @Test
    void testImageWithNoHref() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image x="0" y="0" width="50" height="50"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error when image has no href");
    }

    @Test
    void testImageWithEmptyHref() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="" x="0" y="0" width="50" height="50"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error when image has empty href");
    }

    @Test
    void testImageNaturalSizeNoDimensions() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/png;base64,%s"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render using natural image dimensions");
    }

    @Test
    void testImageRenderingOptimizeSpeed() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/png;base64,%s" width="100" height="100"
                         image-rendering="optimizeSpeed"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        // Should render with nearest-neighbor interpolation (pixelated)
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 200, "Pixelated red image should still be red, got " + c[0]);
    }
}

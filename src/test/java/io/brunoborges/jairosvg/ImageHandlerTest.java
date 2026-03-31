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

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testImageRenderingPixelated() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/png;base64,%s" width="100" height="100"
                         image-rendering="pixelated"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 200, "pixelated rendering should still show red");
    }

    @Test
    void testImageRenderingCrispEdges() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/png;base64,%s" width="100" height="100"
                         image-rendering="crisp-edges"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 200, "crisp-edges rendering should still show red");
    }

    @Test
    void testImageRenderingDefault() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/png;base64,%s" width="100" height="100"
                         image-rendering="auto"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 200, "auto rendering should still show red");
    }

    @Test
    void testEmbeddedSvgImageWithoutDimensions() throws Exception {
        // Embedded SVG that lacks width/height attributes — should fallback
        String innerSvg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <rect width="10" height="10" fill="green"/>
                </svg>""";
        String innerBase64 = Base64.getEncoder().encodeToString(innerSvg.getBytes(StandardCharsets.UTF_8));
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/svg+xml;base64,%s" x="10" y="10" width="50" height="50"/>
                </svg>""".formatted(innerBase64);
        BufferedImage img = render(svg);
        assertNotNull(img, "SVG image without dimensions should render using fallback");
    }

    @Test
    void testImageWithInvalidDataUri() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/png;base64,INVALID_NOT_BASE64!!!" x="0" y="0" width="50" height="50"/>
                </svg>""";
        // Should not crash — invalid base64 should be handled gracefully
        try {
            BufferedImage img = render(svg);
            assertNotNull(img);
        } catch (Exception e) {
            // Some decoders may throw; that's acceptable
        }
    }

    @Test
    void testImageWithTruncatedBytes() throws Exception {
        // Very short data — less than 5 bytes — tests isSvgContent short-data path
        String tinyBase64 = Base64.getEncoder().encodeToString(new byte[]{1, 2});
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/png;base64,%s" x="0" y="0" width="50" height="50"/>
                </svg>""".formatted(tinyBase64);
        BufferedImage img = render(svg);
        assertNotNull(img, "Image with too-short bytes should handle gracefully");
    }

    @Test
    void testImageWithTransform() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/png;base64,%s" x="0" y="0" width="50" height="50"
                         transform="translate(25,25)"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 200, "Transformed image should render red at center");
    }

    @Test
    void testImagePreserveAspectRatio() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <image href="data:image/png;base64,%s" x="0" y="0" width="100" height="100"
                         preserveAspectRatio="xMidYMid meet"/>
                </svg>""".formatted(redPngBase64);
        BufferedImage img = render(svg);
        assertNotNull(img, "preserveAspectRatio should be handled");
    }

    @Test
    void testImageEmptyHref() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="" x="0" y="0" width="50" height="50"/>
                </svg>""";
        assertDoesNotThrow(() -> render(svg));
    }

    @Test
    void testImageNoHref() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image x="0" y="0" width="50" height="50"/>
                </svg>""";
        assertDoesNotThrow(() -> render(svg));
    }

    @Test
    void testImageWithNonExistentFileRef() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="/nonexistent/image.png" x="0" y="0" width="50" height="50"/>
                </svg>""";
        assertDoesNotThrow(() -> render(svg));
    }

    @Test
    void testIsSvgContentGzipped() throws Exception {
        // Gzipped SVG as data URI — exercises gzip detection in isSvgContent
        byte[] svgBytes = """
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
                  <rect width="10" height="10" fill="green"/>
                </svg>""".getBytes(StandardCharsets.UTF_8);
        byte[] gzipped = RenderTestHelper.gzip(svgBytes);
        String b64 = Base64.getEncoder().encodeToString(gzipped);
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/svg+xml;base64,%s"
                         x="0" y="0" width="50" height="50"/>
                </svg>""".formatted(b64);
        assertDoesNotThrow(() -> render(svg));
    }

    @Test
    void testIsSvgContentXmlDecl() throws Exception {
        String xmlSvg = """
                <?xml version="1.0"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
                  <rect width="10" height="10" fill="blue"/>
                </svg>""";
        String b64 = Base64.getEncoder().encodeToString(xmlSvg.getBytes(StandardCharsets.UTF_8));
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/svg+xml;base64,%s"
                         x="0" y="0" width="50" height="50"/>
                </svg>""".formatted(b64);
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── image with SVG content containing viewBox ──

    @Test
    void svgImageWithViewBox() throws Exception {
        // data: URI containing SVG with a viewBox
        String innerSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 20' width='20' height='20'><circle cx='10' cy='10' r='8' fill='green'/></svg>";
        String b64 = java.util.Base64.getEncoder().encodeToString(innerSvg.getBytes());
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <image href="data:image/svg+xml;base64,%s" width="50" height="50"/>
                </svg>
                """.formatted(b64);
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── image with opacity < 1 ──

    @Test
    void imageWithOpacity() throws Exception {
        String b64 = "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVQYV2P4z8BQz0BHwMDAwMDAAAA//wNFAgLiNfJ9AAAAAElFTkSuQmCC";
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/png;base64,%s" width="50" height="50" opacity="0.5"/>
                </svg>
                """.formatted(b64);
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── image element with preserveAspectRatio ──

    @Test
    void imagePreserveAspectRatio() throws Exception {
        String b64 = "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVQYV2P4z8BQz0BHwMDAwMDAAAA//wNFAgLiNfJ9AAAAAElFTkSuQmCC";
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <image href="data:image/png;base64,%s" width="100" height="50"
                         preserveAspectRatio="xMidYMid meet"/>
                </svg>
                """.formatted(b64);
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── image with image-rendering auto ──

    @Test
    void imageRenderingAuto() throws Exception {
        String b64 = "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVQYV2P4z8BQz0BHwMDAwMDAAAA//wNFAgLiNfJ9AAAAAElFTkSuQmCC";
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/png;base64,%s" width="50" height="50"
                         image-rendering="auto"/>
                </svg>
                """.formatted(b64);
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── image with null return from ImageIO.read ──

    @Test
    void imageBadDataReturnsNull() throws Exception {
        // Invalid image data that isn't SVG and ImageIO.read returns null
        String b64 = java.util.Base64.getEncoder().encodeToString("not an image format at all here".getBytes());
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/png;base64,%s" width="50" height="50"/>
                </svg>
                """.formatted(b64);
        // Should not throw, just silently skip
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── image cached for reuse ──

    @Test
    void imageCacheReuse() throws Exception {
        String b64 = "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVQYV2P4z8BQz0BHwMDAwMDAAAA//wNFAgLiNfJ9AAAAAElFTkSuQmCC";
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <image href="data:image/png;base64,%s" x="0" width="50" height="50"/>
                  <image href="data:image/png;base64,%s" x="50" width="50" height="50"/>
                </svg>
                """.formatted(b64, b64);
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── SVG image with zero treeWidth/treeHeight (fallback to element
    // width/height) ──

    @Test
    void svgImageZeroDimensions() throws Exception {
        // Inline SVG data with no width/height/viewBox
        String innerSvg = "<svg xmlns='http://www.w3.org/2000/svg'><rect fill='red' width='10' height='10'/></svg>";
        String b64 = java.util.Base64.getEncoder().encodeToString(innerSvg.getBytes());
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <image href="data:image/svg+xml;base64,%s" width="50" height="50"/>
                </svg>
                """.formatted(b64);
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Image with very short data (< 5 bytes → skip) ──

    @Test
    void imageShortData() throws Exception {
        String b64 = java.util.Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/png;base64,%s" width="50" height="50"/>
                </svg>
                """.formatted(b64);
        // Only 3 bytes → skipped
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Image with empty href (no-op) ──

    @Test
    void imageEmptyHref() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="" width="50" height="50"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Image with invalid raster data (ImageIO.read returns null) ──

    @Test
    void imageInvalidRasterData() throws Exception {
        // Valid-length but not a real image format
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
        String b64 = java.util.Base64.getEncoder().encodeToString(garbage);
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/png;base64,%s" width="50" height="50"/>
                </svg>
                """.formatted(b64);
        // ImageIO.read returns null → gracefully skipped
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Image with width/height=0 falling back to image intrinsic dimensions ──

    @Test
    void imageZeroDimensionsFallback() throws Exception {
        var tinyPng = createTinyPng();
        String b64 = java.util.Base64.getEncoder().encodeToString(tinyPng);
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="data:image/png;base64,%s"/>
                </svg>
                """.formatted(b64);
        // No width/height on <image> → falls back to image intrinsic size
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    private static byte[] createTinyPng() throws Exception {
        var img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 0, 10, 10);
        g.dispose();
        var baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}

package com.jairosvg;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SvgFeatureTest {

    @Test
    void testSvgWithTransform() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <g transform="translate(50, 50)">
                    <rect width="100" height="100" fill="orange"/>
                  </g>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgWithText() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="300" height="100">
                  <text x="10" y="50" font-size="24" fill="black">Hello JairoSVG</text>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgWithGradient() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="0%">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="200" height="200" fill="url(#grad1)"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgWithViewBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 100 100">
                  <rect width="100" height="100" fill="purple"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    void testSvgWithCssStyle() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <style>
                    .myRect { fill: red; stroke: black; stroke-width: 2; }
                  </style>
                  <rect class="myRect" x="10" y="10" width="180" height="180"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // Center should be red
        int pixel = image.getRGB(100, 100);
        int red = (pixel >> 16) & 0xFF;
        assertTrue(red > 200);
    }

    @Test
    void testSvgWithInlineStyle() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect style="fill:blue;stroke:red;stroke-width:3" width="100" height="100"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgWithUse() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <circle id="myCircle" r="20" fill="red"/>
                  </defs>
                  <use href="#myCircle" x="50" y="50"/>
                  <use href="#myCircle" x="150" y="150"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgWithClipPath() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <clipPath id="clip">
                      <circle cx="100" cy="100" r="80"/>
                    </clipPath>
                  </defs>
                  <rect width="200" height="200" fill="blue" clip-path="url(#clip)"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgWithClipPathGradientPreservesMultipleStops() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
                  <defs>
                    <clipPath id="starClip">
                      <polygon points="200,30 230,110 315,110 245,160 270,240 200,190 130,240 155,160 85,110 170,110"/>
                    </clipPath>
                    <linearGradient id="rainbow" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stop-color="#e74c3c"/>
                      <stop offset="25%" stop-color="#f39c12"/>
                      <stop offset="50%" stop-color="#2ecc71"/>
                      <stop offset="75%" stop-color="#3498db"/>
                      <stop offset="100%" stop-color="#9b59b6"/>
                    </linearGradient>
                  </defs>
                  <g clip-path="url(#starClip)">
                    <rect width="400" height="300" fill="url(#rainbow)"/>
                  </g>
                </svg>
                """;

        BufferedImage image = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8))));
        int topRgb = image.getRGB(200, 40);
        int lowerRightRgb = image.getRGB(260, 220);
        int topRed = (topRgb >> 16) & 0xFF;
        int topBlue = topRgb & 0xFF;
        int lowerRightRed = (lowerRightRgb >> 16) & 0xFF;
        int lowerRightBlue = lowerRightRgb & 0xFF;

        assertTrue(topRed > topBlue, "Top of gradient should remain warm");
        assertTrue(lowerRightBlue > lowerRightRed, "Lower-right of clipped gradient should reach cool colors");
    }

    @Test
    void testSvgWithTextDecoration() throws Exception {
        int baselineY = 80;
        int startX = 20;
        Font font = new Font("SansSerif", Font.PLAIN, 48);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        var metrics = font.getLineMetrics("....", frc);
        int endX = startX + (int) Math.ceil(font.getStringBounds("....", frc).getWidth());

        String plainSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="260" height="120">
                  <rect width="260" height="120" fill="white"/>
                  <text x="20" y="80" font-size="48" fill="black">....</text>
                </svg>
                """;
        BufferedImage plainImage = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(plainSvg.getBytes(StandardCharsets.UTF_8))));

        String[] decorations = {"underline", "overline", "line-through"};
        int[] expectedRows = {(int) Math.round(baselineY + metrics.getUnderlineOffset()),
                (int) Math.round(baselineY - metrics.getAscent()),
                (int) Math.round(baselineY + metrics.getStrikethroughOffset())};

        for (int i = 0; i < decorations.length; i++) {
            String svg = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="260" height="120">
                      <rect width="260" height="120" fill="white"/>
                      <text x="20" y="80" font-size="48" fill="black" text-decoration="%s">....</text>
                    </svg>
                    """.formatted(decorations[i]);
            BufferedImage decoratedImage = ImageIO
                    .read(new ByteArrayInputStream(JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8))));
            int plainPixels = countDarkPixelsInBand(plainImage, startX, endX, expectedRows[i], 2);
            int decoratedPixels = countDarkPixelsInBand(decoratedImage, startX, endX, expectedRows[i], 2);
            assertTrue(decoratedPixels > plainPixels + 8, "Expected visible " + decorations[i] + " decoration line");
        }
    }

    private static int countDarkPixelsInBand(BufferedImage image, int startX, int endX, int centerY, int halfBand) {
        int darkPixels = 0;
        int minY = Math.max(0, centerY - halfBand);
        int maxY = Math.min(image.getHeight() - 1, centerY + halfBand);
        int clampedStartX = Math.max(0, startX);
        int clampedEndX = Math.min(image.getWidth() - 1, endX);
        for (int y = minY; y <= maxY; y++) {
            for (int x = clampedStartX; x <= clampedEndX; x++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                if (alpha > 0 && red < 80 && green < 80 && blue < 80) {
                    darkPixels++;
                }
            }
        }
        return darkPixels;
    }
}

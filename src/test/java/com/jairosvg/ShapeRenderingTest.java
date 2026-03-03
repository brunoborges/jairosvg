package com.jairosvg;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ShapeRenderingTest {
    private static final int MIN_COLOR_CHANNEL_THRESHOLD = 120;

    @Test
    void testSimpleRectSvgToPng() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="10" y="10" width="80" height="80" fill="red"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);

        // Verify it's a valid PNG
        assertTrue(png[0] == (byte) 0x89);
        assertTrue(png[1] == (byte) 0x50); // 'P'
        assertTrue(png[2] == (byte) 0x4E); // 'N'
        assertTrue(png[3] == (byte) 0x47); // 'G'

        // Verify dimensions
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());

        // Verify red pixel at center
        int pixel = image.getRGB(50, 50);
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        assertEquals(255, red);
        assertEquals(0, green);
        assertEquals(0, blue);
    }

    @Test
    void testCircleSvgToPng() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <circle cx="100" cy="100" r="80" fill="blue" stroke="black" stroke-width="2"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());

        // Center should be blue
        int pixel = image.getRGB(100, 100);
        int blue = pixel & 0xFF;
        assertTrue(blue > 200);
    }

    @Test
    void testPathSvgToPng() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M 10 10 L 90 10 L 90 90 L 10 90 Z" fill="green"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(100, image.getWidth());
    }

    @Test
    void testComplexSvg() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400">
                  <defs>
                    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stop-color="#ff6b6b"/>
                      <stop offset="100%" stop-color="#4ecdc4"/>
                    </linearGradient>
                  </defs>
                  <rect width="400" height="400" fill="url(#bg)"/>
                  <circle cx="200" cy="200" r="150" fill="none" stroke="white" stroke-width="4"/>
                  <ellipse cx="200" cy="200" rx="100" ry="50" fill="rgba(255,255,255,0.3)"/>
                  <line x1="50" y1="50" x2="350" y2="350" stroke="white" stroke-width="2" stroke-dasharray="10 5"/>
                  <polygon points="200,50 250,150 350,150 270,210 300,320 200,260 100,320 130,210 50,150 150,150"
                           fill="gold" stroke="white" stroke-width="2"/>
                  <text x="200" y="380" text-anchor="middle" font-size="20" fill="white">JairoSVG</text>
                  <path d="M 50 350 Q 200 300 350 350" fill="none" stroke="white" stroke-width="2"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(400, image.getWidth());
        assertEquals(400, image.getHeight());
    }

    @Test
    void testCssCustomPropertiesVarSupport() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    svg { --main-fill: rgb(0, 128, 0); }
                    rect { fill: var(--main-fill); }
                  </style>
                  <rect x="10" y="10" width="80" height="80"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int center = image.getRGB(50, 50);
        int centerRed = (center >> 16) & 0xFF;
        int centerGreen = (center >> 8) & 0xFF;
        int centerBlue = center & 0xFF;
        assertTrue(centerGreen > 100);
        assertTrue(centerRed < 20);
        assertTrue(centerBlue < 20);
    }

    @Test
    void testGaussianBlurFilterRenders() throws Exception {
        int shapeX = 30;
        int shapeY = 30;
        int shapeSize = 40;

        String filteredSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="blur">
                      <feGaussianBlur stdDeviation="4"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect x="30" y="30" width="40" height="40" fill="red" filter="url(#blur)"/>
                </svg>
                """;
        String plainSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="%d" y="%d" width="%d" height="%d" fill="red"/>
                </svg>
                """.formatted(shapeX, shapeY, shapeSize, shapeSize);

        BufferedImage filteredImage = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(filteredSvg.getBytes(StandardCharsets.UTF_8))));
        BufferedImage plainImage = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(plainSvg.getBytes(StandardCharsets.UTF_8))));

        int probeY = shapeY + shapeSize / 2;
        boolean blurFound = false;
        for (int x = shapeX - 8; x < shapeX; x++) {
            if (plainImage.getRGB(x, probeY) != filteredImage.getRGB(x, probeY)) {
                blurFound = true;
                break;
            }
        }
        assertTrue(blurFound);
    }

    @Test
    void testDropShadowFilterRenders() throws Exception {
        String filteredSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="shadow">
                      <feDropShadow dx="6" dy="6" stdDeviation="2" flood-color="black" flood-opacity="0.7"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect x="20" y="20" width="30" height="30" fill="#3498db" filter="url(#shadow)"/>
                </svg>
                """;
        String plainSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="20" y="20" width="30" height="30" fill="#3498db"/>
                </svg>
                """;

        BufferedImage filteredImage = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(filteredSvg.getBytes(StandardCharsets.UTF_8))));
        BufferedImage plainImage = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(plainSvg.getBytes(StandardCharsets.UTF_8))));

        int shapeX = 20;
        int shapeY = 20;
        int shapeSize = 30;
        int dx = 6;
        int dy = 6;
        int searchStartX = shapeX + shapeSize + 1;
        int searchStartY = shapeY + shapeSize + 1;
        int searchEndX = searchStartX + dx + 14;
        int searchEndY = searchStartY + dy + 14;

        boolean shadowFound = false;
        for (int y = searchStartY; y <= searchEndY && !shadowFound; y++) {
            for (int x = searchStartX; x <= searchEndX; x++) {
                if (plainImage.getRGB(x, y) != filteredImage.getRGB(x, y)) {
                    shadowFound = true;
                    break;
                }
            }
        }
        assertTrue(shadowFound);
    }
  
    @Test
    void testMarkersRenderOnLinePolylineAndPath() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
                  <defs>
                    <marker id="dot" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="10" markerHeight="10">
                      <circle cx="5" cy="5" r="4" fill="blue"/>
                    </marker>
                    <marker id="square" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="10" markerHeight="10">
                      <rect width="10" height="10" fill="lime"/>
                    </marker>
                    <marker id="triangle" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="10" markerHeight="10">
                      <path d="M0,10 L5,0 L10,10 Z" fill="red"/>
                    </marker>
                  </defs>
                  <line x1="10" y1="20" x2="110" y2="20" stroke="black" marker-end="url(#dot)"/>
                  <polyline points="10,50 60,50 110,50" fill="none" stroke="black" marker-mid="url(#square)"/>
                  <path d="M10,90 L110,90" fill="none" stroke="black" marker-end="url(#triangle)"/>
                </svg>
            """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int lineEnd = image.getRGB(110, 20);
        int polyMid = image.getRGB(60, 50);
        int pathEnd = image.getRGB(110, 90);

        assertTrue((lineEnd & 0xFF) > MIN_COLOR_CHANNEL_THRESHOLD);
        assertTrue(((polyMid >> 8) & 0xFF) > MIN_COLOR_CHANNEL_THRESHOLD);
        assertTrue(((pathEnd >> 16) & 0xFF) > MIN_COLOR_CHANNEL_THRESHOLD);
    }

    @Test
    void testTextPathFollowsCurve() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
                  <defs>
                    <path id="curve" d="M30,200 C100,100 300,100 370,200" fill="none"/>
                  </defs>
                  <text font-size="18" fill="#9b59b6">
                    <textPath href="#curve">Text following a curved path element</textPath>
                  </text>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int curvedBandPixels = 0;
        // This band covers the upper-middle section of curve M30,200 C100,100 300,100
        // 370,200 within the 400x300 image.
        // Straight-line rendering at the baseline does not produce pixels in this
        // region.
        for (int y = 110; y <= 150; y++) {
            for (int x = 150; x <= 250; x++) {
                int alpha = (image.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 0) {
                    curvedBandPixels++;
                }
            }
        }
        assertTrue(curvedBandPixels > 20, "textPath should render visible pixels along the curved path");
    }
}

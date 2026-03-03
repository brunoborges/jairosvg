package com.jairosvg;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ShapeRenderingTest {

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
    void testEmbeddedSvgImageOpacity() throws Exception {
        String embeddedSvg = """
                <svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>
                  <rect width='10' height='10' fill='black'/>
                </svg>
                """;
        String imageHref = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(embeddedSvg.getBytes(StandardCharsets.UTF_8));
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
                  <rect width="10" height="10" fill="white"/>
                  <image x="0" y="0" width="10" height="10" opacity="0.5" href="%s"/>
                </svg>
                """.formatted(imageHref);
        String opaqueSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
                  <rect width="10" height="10" fill="white"/>
                  <image x="0" y="0" width="10" height="10" opacity="1" href="%s"/>
                </svg>
                """.formatted(imageHref);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8))));
        BufferedImage opaqueImage = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(opaqueSvg.getBytes(StandardCharsets.UTF_8))));
        int pixel = image.getRGB(5, 5);
        int opaquePixel = opaqueImage.getRGB(5, 5);
        int red = (pixel >> 16) & 0xFF;
        int opaqueRed = (opaquePixel >> 16) & 0xFF;
        assertTrue(red > opaqueRed, "50% opacity embedded image should blend lighter than opaque image");
    }
}

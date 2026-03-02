package com.jairosvg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JairoSVG.
 */
class JairoSVGTest {

    @Test
    void testColorParsing() {
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("red"));
        assertEquals(new Colors.RGBA(0, 0, 0, 1), Colors.color("black"));
        assertEquals(new Colors.RGBA(1, 1, 1, 1), Colors.color("white"));
        assertEquals(new Colors.RGBA(0, 0, 0, 0), Colors.color("transparent"));
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("#ff0000"));
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("#f00"));
        assertNotNull(Colors.color("rgb(255, 0, 0)"));
        assertNotNull(Colors.color("rgba(255, 0, 0, 1)"));
    }

    @Test
    void testColorNegate() {
        Colors.RGBA red = new Colors.RGBA(1, 0, 0, 1);
        Colors.RGBA negated = Colors.negateColor(red);
        assertEquals(0, negated.r(), 0.001);
        assertEquals(1, negated.g(), 0.001);
        assertEquals(1, negated.b(), 0.001);
        assertEquals(1, negated.a(), 0.001);
    }

    @Test
    void testNormalize() {
        assertEquals("10 20 30 40", Helpers.normalize("10,20,30,40"));
        assertEquals("10 -20 30", Helpers.normalize("10-20 30"));
    }

    @Test
    void testUrlParsing() {
        var url = UrlHelper.parseUrl("#myId");
        assertEquals("myId", url.fragment());

        var url2 = UrlHelper.parseUrl("url(#grad1)");
        assertEquals("grad1", url2.fragment());
    }

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
    void testSvgToSvg() throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect width="100" height="100" fill="red"/>
            </svg>
            """;

        byte[] result = JairoSVG.svg2svg(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(result);
        String resultStr = new String(result, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<svg"));
    }

    @Test
    void testBuilderApi() throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect width="100" height="100" fill="green"/>
            </svg>
            """;

        byte[] png = JairoSVG.builder()
            .fromString(svg)
            .scale(2)
            .backgroundColor("white")
            .toPng();

        assertNotNull(png);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    void testToImage() throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
              <circle cx="25" cy="25" r="20" fill="yellow"/>
            </svg>
            """;

        BufferedImage image = JairoSVG.builder()
            .fromString(svg)
            .toImage();

        assertNotNull(image);
        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    @Test
    void testFileOutput(@TempDir Path tempDir) throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect width="100" height="100" fill="blue"/>
            </svg>
            """;

        Path svgFile = tempDir.resolve("test.svg");
        Path pngFile = tempDir.resolve("test.png");
        Files.writeString(svgFile, svg);

        JairoSVG.svg2png(svgFile, pngFile);

        assertTrue(Files.exists(pngFile));
        assertTrue(Files.size(pngFile) > 0);
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
    void testOutputWidthHeight() throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect width="100" height="100" fill="green"/>
            </svg>
            """;

        byte[] png = JairoSVG.builder()
            .fromString(svg)
            .outputWidth(300)
            .outputHeight(200)
            .toPng();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(300, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    void testNegateColors() throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect width="100" height="100" fill="red"/>
            </svg>
            """;

        byte[] png = JairoSVG.builder()
            .fromString(svg)
            .negateColors(true)
            .toPng();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int pixel = image.getRGB(50, 50);
        // Red negated should be cyan (0, 255, 255)
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        assertEquals(0, red);
        assertEquals(255, green);
        assertEquals(255, blue);
    }

    @Test
    void testSvgWithSvgFont() throws Exception {
        // SVG font with a simple square glyph for 'A' and a triangle for 'B'
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
              <defs>
                <font id="TestFont" horiz-adv-x="1000">
                  <font-face font-family="TestFont" units-per-em="1000" ascent="800" descent="-200"/>
                  <glyph unicode="A" horiz-adv-x="1000" d="M 100 0 L 100 800 L 900 800 L 900 0 Z"/>
                  <glyph unicode="B" horiz-adv-x="1000" d="M 100 0 L 500 800 L 900 0 Z"/>
                  <missing-glyph horiz-adv-x="500" d="M 0 0 L 0 800 L 500 800 L 500 0 Z"/>
                </font>
              </defs>
              <text x="10" y="60" font-family="TestFont" font-size="48" fill="red">AB</text>
            </svg>
            """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(200, image.getWidth());
        assertEquals(100, image.getHeight());

        // The glyph paths should produce non-transparent red pixels in the text area
        int pixel = image.getRGB(30, 40);
        int alpha = (pixel >> 24) & 0xFF;
        int red = (pixel >> 16) & 0xFF;
        assertTrue(alpha > 0, "SVG font glyph should render visible pixels");
        assertTrue(red > 200, "SVG font glyph should be red (fill='red')");
    }

    @Test
    void testSvgFontWithMissingGlyph() throws Exception {
        // SVG font that only defines glyph for 'X', so other chars use missing-glyph
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
              <defs>
                <font id="MissingTest" horiz-adv-x="1000">
                  <font-face font-family="MissingTest" units-per-em="1000" ascent="800" descent="-200"/>
                  <glyph unicode="X" horiz-adv-x="1000" d="M 0 0 L 0 800 L 1000 800 L 1000 0 Z"/>
                  <missing-glyph horiz-adv-x="500" d="M 100 100 L 100 700 L 400 700 L 400 100 Z"/>
                </font>
              </defs>
              <text x="10" y="60" font-family="MissingTest" font-size="48" fill="blue">XY</text>
            </svg>
            """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(200, image.getWidth());
        // Should render without errors (Y uses missing-glyph)
    }

    @Test
    void testSvgFontFallbackToAwt() throws Exception {
        // When font-family doesn't match any SVG font, should fallback to AWT
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
              <defs>
                <font id="OnlyThisFont" horiz-adv-x="1000">
                  <font-face font-family="OnlyThisFont" units-per-em="1000" ascent="800" descent="-200"/>
                  <glyph unicode="A" d="M 0 0 L 0 800 L 1000 800 L 1000 0 Z"/>
                </font>
              </defs>
              <text x="10" y="50" font-family="SansSerif" font-size="24" fill="black">Hello</text>
            </svg>
            """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgFontWithTextAnchor() throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
              <defs>
                <font id="AnchorFont" horiz-adv-x="500">
                  <font-face font-family="AnchorFont" units-per-em="1000" ascent="800" descent="-200"/>
                  <glyph unicode="A" horiz-adv-x="500" d="M 0 0 L 0 800 L 500 800 L 500 0 Z"/>
                </font>
              </defs>
              <text x="100" y="50" font-family="AnchorFont" font-size="24"
                    text-anchor="middle" fill="green">AA</text>
            </svg>
            """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgFontWithoutId() throws Exception {
        // <font> without id — should still be registered via <font-face font-family="...">
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
              <defs>
                <font horiz-adv-x="1000">
                  <font-face font-family="NoIdFont" units-per-em="1000" ascent="800" descent="-200"/>
                  <glyph unicode="A" horiz-adv-x="1000" d="M 100 0 L 100 800 L 900 800 L 900 0 Z"/>
                  <missing-glyph horiz-adv-x="500" d="M 0 0 L 0 800 L 500 800 L 500 0 Z"/>
                </font>
              </defs>
              <text x="10" y="60" font-family="NoIdFont" font-size="48" fill="red">A</text>
            </svg>
            """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // The glyph path should produce non-transparent red pixels
        int pixel = image.getRGB(30, 40);
        int alpha = (pixel >> 24) & 0xFF;
        int red = (pixel >> 16) & 0xFF;
        assertTrue(alpha > 0, "SVG font without id should still render glyphs");
        assertTrue(red > 200, "SVG font glyph should be red (fill='red')");
    }
}

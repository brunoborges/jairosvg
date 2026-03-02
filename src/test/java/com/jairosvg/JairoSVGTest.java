package com.jairosvg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.font.FontRenderContext;
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
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("rgb(255, 0, 0)"));
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("rgba(255, 0, 0, 1)"));

        // HSL: red = hsl(0, 100%, 50%)
        Colors.RGBA hslRed = Colors.color("hsl(0, 100%, 50%)");
        assertEquals(1.0, hslRed.r(), 0.01);
        assertEquals(0.0, hslRed.g(), 0.01);
        assertEquals(0.0, hslRed.b(), 0.01);
        assertEquals(1.0, hslRed.a(), 0.01);

        // HSL: green = hsl(120, 100%, 50%)
        Colors.RGBA hslGreen = Colors.color("hsl(120, 100%, 50%)");
        assertEquals(0.0, hslGreen.r(), 0.01);
        assertEquals(1.0, hslGreen.g(), 0.01);
        assertEquals(0.0, hslGreen.b(), 0.01);

        // HSL: blue = hsl(240, 100%, 50%)
        Colors.RGBA hslBlue = Colors.color("hsl(240, 100%, 50%)");
        assertEquals(0.0, hslBlue.r(), 0.01);
        assertEquals(0.0, hslBlue.g(), 0.01);
        assertEquals(1.0, hslBlue.b(), 0.01);

        // HSL: gray = hsl(0, 0%, 50%)
        Colors.RGBA hslGray = Colors.color("hsl(0, 0%, 50%)");
        assertEquals(0.5, hslGray.r(), 0.01);
        assertEquals(0.5, hslGray.g(), 0.01);
        assertEquals(0.5, hslGray.b(), 0.01);

        // HSLA with alpha
        Colors.RGBA hslaRed = Colors.color("hsla(0, 100%, 50%, 0.5)");
        assertEquals(1.0, hslaRed.r(), 0.01);
        assertEquals(0.0, hslaRed.g(), 0.01);
        assertEquals(0.0, hslaRed.b(), 0.01);
        assertEquals(0.5, hslaRed.a(), 0.01);

        // Verify opacity parameter interaction for HSL/HSLA
        Colors.RGBA hslRedWithOpacity = Colors.color("hsl(0, 100%, 50%)", 0.5);
        assertEquals(1.0, hslRedWithOpacity.r(), 0.01);
        assertEquals(0.0, hslRedWithOpacity.g(), 0.01);
        assertEquals(0.0, hslRedWithOpacity.b(), 0.01);
        assertEquals(0.5, hslRedWithOpacity.a(), 0.01);

        Colors.RGBA hslaRedWithOpacity = Colors.color("hsla(0, 100%, 50%, 0.5)", 0.5);
        assertEquals(1.0, hslaRedWithOpacity.r(), 0.01);
        assertEquals(0.0, hslaRedWithOpacity.g(), 0.01);
        assertEquals(0.0, hslaRedWithOpacity.b(), 0.01);
        // hsla alpha (0.5) multiplied by opacity (0.5) => 0.25
        assertEquals(0.25, hslaRedWithOpacity.a(), 0.01);
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
        BufferedImage plainImage = ImageIO.read(new ByteArrayInputStream(
            JairoSVG.svg2png(plainSvg.getBytes(StandardCharsets.UTF_8))));

        String[] decorations = {"underline", "overline", "line-through"};
        int[] expectedRows = {
            (int) Math.round(baselineY + metrics.getUnderlineOffset()),
            (int) Math.round(baselineY - metrics.getAscent()),
            (int) Math.round(baselineY + metrics.getStrikethroughOffset())
        };

        for (int i = 0; i < decorations.length; i++) {
            String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="260" height="120">
                  <rect width="260" height="120" fill="white"/>
                  <text x="20" y="80" font-size="48" fill="black" text-decoration="%s">....</text>
                </svg>
                """.formatted(decorations[i]);
            BufferedImage decoratedImage = ImageIO.read(new ByteArrayInputStream(
                JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8))));
            int plainPixels = countDarkPixelsInBand(plainImage, startX, endX, expectedRows[i], 2);
            int decoratedPixels = countDarkPixelsInBand(decoratedImage, startX, endX, expectedRows[i], 2);
            assertTrue(decoratedPixels > plainPixels + 8,
                "Expected visible " + decorations[i] + " decoration line");
        }
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
    void testExternalCssStylesheetWithUnsafe(@TempDir Path tempDir) throws Exception {
        // Create external CSS file
        String css = ".myRect { fill: red; }";
        Path cssFile = tempDir.resolve("style.css");
        Files.writeString(cssFile, css);

        // Create SVG that references external CSS
        String svg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet type="text/css" href="style.css"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect class="myRect" x="0" y="0" width="100" height="100"/>
            </svg>
            """;
        Path svgFile = tempDir.resolve("test.svg");
        Files.writeString(svgFile, svg);

        // With unsafe=true, external CSS should be loaded
        byte[] png = JairoSVG.builder()
            .fromFile(svgFile)
            .unsafe(true)
            .toPng();

        assertNotNull(png);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // Center should be red from external CSS
        int pixel = image.getRGB(50, 50);
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        assertEquals(255, red);
        assertEquals(0, green);
        assertEquals(0, blue);
    }

    @Test
    void testExternalCssStylesheetBlockedWithoutUnsafe(@TempDir Path tempDir) throws Exception {
        // Create external CSS file
        String css = ".myRect { fill: red; }";
        Path cssFile = tempDir.resolve("style.css");
        Files.writeString(cssFile, css);

        // Create SVG that references external CSS
        String svg = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet type="text/css" href="style.css"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect class="myRect" x="0" y="0" width="100" height="100" fill="blue"/>
            </svg>
            """;
        Path svgFile = tempDir.resolve("test.svg");
        Files.writeString(svgFile, svg);

        // With unsafe=false (default), external CSS should NOT be loaded
        byte[] png = JairoSVG.builder()
            .fromFile(svgFile)
            .toPng();

        assertNotNull(png);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // Center should be blue (inline attribute), not red (blocked external CSS)
        int pixel = image.getRGB(50, 50);
        int red = (pixel >> 16) & 0xFF;
        int blue = pixel & 0xFF;
        assertTrue(blue > 200, "Should be blue since external CSS is blocked");
        assertTrue(red < 50, "Should not be red since external CSS is blocked");
    }

    @Test
    void testParsePseudoAttributes() {
        var attrs = CssProcessor.parsePseudoAttributes("type=\"text/css\" href=\"style.css\"");
        assertEquals("text/css", attrs.get("type"));
        assertEquals("style.css", attrs.get("href"));

        // Single quotes
        var attrs2 = CssProcessor.parsePseudoAttributes("type='text/css' href='other.css'");
        assertEquals("text/css", attrs2.get("type"));
        assertEquals("other.css", attrs2.get("href"));

        // Empty/null
        assertTrue(CssProcessor.parsePseudoAttributes("").isEmpty());
        assertTrue(CssProcessor.parsePseudoAttributes(null).isEmpty());
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

    @Test
    void testSvgWithPatternFill() throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
              <defs>
                <pattern id="pat1" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
                  <rect width="20" height="20" fill="red"/>
                </pattern>
              </defs>
              <rect width="200" height="200" fill="url(#pat1)"/>
            </svg>
            """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(200, image.getWidth());
        // Center pixel should be filled with the pattern (red)
        int pixel = image.getRGB(100, 100);
        int red = (pixel >> 16) & 0xFF;
        assertTrue(red > 200, "Pattern fill should produce red pixels");
    }

    @Test
    void testSvgWithPatternTransformScale() throws Exception {
        // Pattern: 20×20 tile, left half blue, right half transparent.
        // Without scale, blue repeats every 20px; with scale(2), the tile doubles to 40px.
        // So at x=15 the unscaled pattern would show blue (within first 10px tile repeat),
        // but at x=25 in a scaled pattern the blue half extends to 20px wide.
        String svgNoScale = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
              <defs>
                <pattern id="patNoScale" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
                  <rect width="10" height="20" fill="blue"/>
                </pattern>
              </defs>
              <rect width="200" height="200" fill="url(#patNoScale)"/>
            </svg>
            """;
        String svgScaled = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
              <defs>
                <pattern id="patScaled" x="0" y="0" width="20" height="20"
                         patternUnits="userSpaceOnUse"
                         patternTransform="scale(2)">
                  <rect width="10" height="20" fill="blue"/>
                </pattern>
              </defs>
              <rect width="200" height="200" fill="url(#patScaled)"/>
            </svg>
            """;

        BufferedImage imgNoScale = ImageIO.read(new ByteArrayInputStream(
            JairoSVG.svg2png(svgNoScale.getBytes(StandardCharsets.UTF_8))));
        BufferedImage imgScaled = ImageIO.read(new ByteArrayInputStream(
            JairoSVG.svg2png(svgScaled.getBytes(StandardCharsets.UTF_8))));

        // Both images should have visible content
        assertEquals(200, imgNoScale.getWidth());
        assertEquals(200, imgScaled.getWidth());

        // At x=5, y=5: both should be blue (within the blue half of the tile)
        int noScaleBlue = imgNoScale.getRGB(5, 5) & 0xFF;
        int scaledBlue = imgScaled.getRGB(5, 5) & 0xFF;
        assertTrue(noScaleBlue > 200, "Unscaled pattern should be blue at (5,5)");
        assertTrue(scaledBlue > 200, "Scaled pattern should be blue at (5,5)");

        // At x=15, y=5: unscaled is in the transparent half (x=15 % 20 = 15 > 10),
        // but scaled tile stretches to 40px, so x=15 is still in the blue half.
        int noScaleAt15 = imgNoScale.getRGB(15, 5) & 0xFF;
        int scaledAt15 = imgScaled.getRGB(15, 5) & 0xFF;
        assertTrue(noScaleAt15 < 50, "Unscaled pattern should NOT be blue at (15,5)");
        assertTrue(scaledAt15 > 200, "Scaled pattern should still be blue at (15,5)");
    }

    @Test
    void testSvgWithPatternTransformRotate() throws Exception {
        // Pattern: 20×20 tile, left half green, right half transparent.
        // Without rotation, pixel at (15,10) is in the transparent half.
        // With rotate(90), the pattern is rotated so the green/transparent boundary
        // changes orientation, altering which pixels are filled.
        String svgNoRotate = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
              <defs>
                <pattern id="patNoRot" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
                  <rect width="10" height="20" fill="green"/>
                </pattern>
              </defs>
              <rect width="200" height="200" fill="url(#patNoRot)"/>
            </svg>
            """;
        String svgRotated = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
              <defs>
                <pattern id="patRot" x="0" y="0" width="20" height="20"
                         patternUnits="userSpaceOnUse"
                         patternTransform="rotate(90)">
                  <rect width="10" height="20" fill="green"/>
                </pattern>
              </defs>
              <rect width="200" height="200" fill="url(#patRot)"/>
            </svg>
            """;

        BufferedImage imgNoRot = ImageIO.read(new ByteArrayInputStream(
            JairoSVG.svg2png(svgNoRotate.getBytes(StandardCharsets.UTF_8))));
        BufferedImage imgRotated = ImageIO.read(new ByteArrayInputStream(
            JairoSVG.svg2png(svgRotated.getBytes(StandardCharsets.UTF_8))));

        assertEquals(200, imgNoRot.getWidth());
        assertEquals(200, imgRotated.getWidth());

        // The two renderings should differ — rotation changes the pattern layout
        // Compare a sampling of pixels; at least some must differ
        int differences = 0;
        for (int y = 0; y < 200; y += 10) {
            for (int x = 0; x < 200; x += 10) {
                if (imgNoRot.getRGB(x, y) != imgRotated.getRGB(x, y)) {
                    differences++;
                }
            }
        }
        assertTrue(differences > 0, "Rotated pattern should differ from unrotated pattern");
    }

    @Test
    void testSvgWithPatternTransformTranslate() throws Exception {
        String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
              <defs>
                <pattern id="pat4" x="0" y="0" width="20" height="20"
                         patternUnits="userSpaceOnUse"
                         patternTransform="translate(5, 5)">
                  <rect width="20" height="20" fill="red"/>
                </pattern>
              </defs>
              <rect width="200" height="200" fill="url(#pat4)"/>
            </svg>
            """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // Center should be filled with the pattern (red)
        int pixel = image.getRGB(100, 100);
        int red = (pixel >> 16) & 0xFF;
        assertTrue(red > 200, "Pattern with translate transform should still produce red pixels");
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

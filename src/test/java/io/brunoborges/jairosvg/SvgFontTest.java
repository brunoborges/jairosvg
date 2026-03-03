package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SvgFontTest {

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
        // <font> without id — should still be registered via <font-face
        // font-family="...">
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

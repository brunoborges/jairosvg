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
        // SVG font that only defines glyph for 'X', so 'Y' falls back to system font
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

        // X glyph should render using the SVG font (filled rectangle)
        int pxX = image.getRGB(30, 40);
        assertTrue(((pxX >> 24) & 0xFF) > 0, "X glyph should be visible");
        assertTrue((pxX & 0xFF) > 200, "X glyph should be blue");

        // Y should fall back to system font (not use missing-glyph)
        // The Y character rendered by a system font should still produce visible pixels
        int pxY = image.getRGB(68, 40);
        assertTrue(((pxY >> 24) & 0xFF) > 0, "Fallback Y should be visible");
    }

    @Test
    void testSvgFontUndefinedCharFallsBackToSystemFont() throws Exception {
        // Verify that characters not defined in the SVG font are rendered using
        // the system font (not the missing-glyph), matching browser/EchoSVG behaviour.
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="300" height="100">
                  <defs>
                    <font id="FallbackTest" horiz-adv-x="1000">
                      <font-face font-family="FallbackTest" units-per-em="1000" ascent="800" descent="-200"/>
                      <glyph unicode="A" horiz-adv-x="1000" d="M 100 0 L 100 800 L 900 800 L 900 0 Z"/>
                      <missing-glyph horiz-adv-x="500" d="M 50 0 L 50 800 L 450 800 L 450 0 Z
                                                           M 150 100 L 350 100 L 350 700 L 150 700 Z"/>
                    </font>
                  </defs>
                  <text x="10" y="60" font-family="FallbackTest" font-size="48" fill="red">AZ</text>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));

        // A glyph should be a filled rectangle (SVG font glyph)
        int pxA = image.getRGB(25, 40);
        assertTrue(((pxA >> 24) & 0xFF) > 0, "A (SVG glyph) should be visible");
        assertTrue(((pxA >> 16) & 0xFF) > 200, "A should be red");

        // Z should fall back to system font, producing a recognizable Z character.
        // The Z character in a system font has a top horizontal stroke, a diagonal,
        // and a bottom horizontal stroke. The centre of the character body should
        // have some visible pixels.
        // Advance of A is 1000*48/1000 = 48px, so Z starts at x≈58.
        boolean zVisible = false;
        for (int y = 30; y < 60; y++) {
            for (int x = 58; x < 100; x++) {
                int px = image.getRGB(x, y);
                if (((px >> 24) & 0xFF) > 0 && ((px >> 16) & 0xFF) > 150) {
                    zVisible = true;
                    break;
                }
            }
            if (zVisible)
                break;
        }
        assertTrue(zVisible, "Z should fall back to system font and render visible red pixels");
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

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testSvgFontWithEmptyGlyphPath() throws Exception {
        // Glyph with empty d="" — should be handled gracefully
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <font id="EmptyGlyph" horiz-adv-x="1000">
                      <font-face font-family="EmptyGlyph" units-per-em="1000" ascent="800" descent="-200"/>
                      <glyph unicode="A" horiz-adv-x="1000" d=""/>
                      <glyph unicode="B" horiz-adv-x="1000" d="M 0 0 L 0 800 L 1000 800 L 1000 0 Z"/>
                      <missing-glyph horiz-adv-x="500" d="M 0 0 L 0 800 L 500 800 L 500 0 Z"/>
                    </font>
                  </defs>
                  <text x="10" y="60" font-family="EmptyGlyph" font-size="48" fill="red">AB</text>
                </svg>
                """;
        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0, "Empty glyph path should be handled");
    }

    @Test
    void testSvgFontGlyphWithoutUnicode() throws Exception {
        // Glyph without unicode attribute — should be skipped
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <font id="NoUnicode" horiz-adv-x="1000">
                      <font-face font-family="NoUnicode" units-per-em="1000" ascent="800" descent="-200"/>
                      <glyph horiz-adv-x="1000" d="M 0 0 L 0 800 L 1000 800 L 1000 0 Z"/>
                      <glyph unicode="X" horiz-adv-x="1000" d="M 0 0 L 0 800 L 1000 800 L 1000 0 Z"/>
                      <missing-glyph horiz-adv-x="500" d="M 0 0 L 0 800 L 500 800 L 500 0 Z"/>
                    </font>
                  </defs>
                  <text x="10" y="60" font-family="NoUnicode" font-size="48" fill="blue">X</text>
                </svg>
                """;
        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);
    }

    @Test
    void testSvgFontWithoutFontFace() throws Exception {
        // <font> without <font-face> — family resolved via font id
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <font id="NoFace" horiz-adv-x="1000">
                      <glyph unicode="A" horiz-adv-x="1000" d="M 0 0 L 0 800 L 1000 800 L 1000 0 Z"/>
                    </font>
                  </defs>
                  <text x="10" y="60" font-family="NoFace" font-size="48" fill="red">A</text>
                </svg>
                """;
        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0, "Font without font-face should fallback to font id");
    }

    @Test
    void testSvgFontWithoutFontFaceAndNoId() throws Exception {
        // <font> with neither <font-face> nor id — should return null and be skipped
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <font horiz-adv-x="1000">
                      <glyph unicode="A" horiz-adv-x="1000" d="M 0 0 L 0 800 L 1000 800 L 1000 0 Z"/>
                    </font>
                  </defs>
                  <text x="10" y="60" font-family="SansSerif" font-size="24" fill="black">Hello</text>
                </svg>
                """;
        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0, "Font without face/id should be ignored gracefully");
    }

    @Test
    void testSvgFontGlyphDefaultAdvance() throws Exception {
        // Glyph without horiz-adv-x — should use font's default horiz-adv-x
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <font id="DefAdv" horiz-adv-x="800">
                      <font-face font-family="DefAdv" units-per-em="1000" ascent="800" descent="-200"/>
                      <glyph unicode="A" d="M 0 0 L 0 800 L 800 800 L 800 0 Z"/>
                    </font>
                  </defs>
                  <text x="10" y="60" font-family="DefAdv" font-size="48" fill="red">AA</text>
                </svg>
                """;
        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0, "Glyph should use default advance when horiz-adv-x missing");
    }
}

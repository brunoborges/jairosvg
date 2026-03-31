package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class DefsTest {

    @Test
    void testUseSymbolWithViewBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
                  <rect width="120" height="120" fill="white"/>
                  <defs>
                    <symbol id="sym" viewBox="0 0 10 10">
                      <rect width="10" height="10" fill="red"/>
                    </symbol>
                  </defs>
                  <use href="#sym" x="10" y="10" width="50" height="50"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Red pixels in the (10,10)-(60,60) region
        int[] center = rgba(img, 35, 35);
        assertTrue(center[0] > 200, "Center of use region should be red, R=" + center[0]);
        assertTrue(center[1] < 50, "Center of use region should be red, G=" + center[1]);
        assertTrue(center[2] < 50, "Center of use region should be red, B=" + center[2]);

        // Outside the use region should be white (from the background rect)
        assertPixelColor(img, 70, 70, 255, 255, 255);
        assertPixelColor(img, 115, 115, 255, 255, 255);
    }

    @Test
    void testUseReferencingGroup() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <defs>
                    <g id="shapes">
                      <rect x="0" y="0" width="20" height="20" fill="red"/>
                      <circle cx="10" cy="10" r="5" fill="blue"/>
                    </g>
                  </defs>
                  <use href="#shapes" x="40" y="40"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Colored pixels should appear around (40-60, 40-60)
        boolean hasColor = false;
        for (int y = 40; y <= 60; y++) {
            for (int x = 40; x <= 60; x++) {
                int[] c = rgba(img, x, y);
                if (c[3] > 0 && !(c[0] > 250 && c[1] > 250 && c[2] > 250)) {
                    hasColor = true;
                    break;
                }
            }
            if (hasColor)
                break;
        }
        assertTrue(hasColor, "Referenced group should render colored pixels around (40-60,40-60)");

        // Far corner should be white
        assertPixelColor(img, 2, 2, 255, 255, 255);
    }

    @Test
    void testUseMissingHref() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <use x="10" y="10"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void testUseReferencingMissingElement() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <use href="#nonexistent" x="10" y="10"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void testClipPath() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <clipPath id="clip">
                      <circle cx="50" cy="50" r="30"/>
                    </clipPath>
                  </defs>
                  <rect width="100" height="100" fill="red" clip-path="url(#clip)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Center (50,50) should be red
        int[] center = rgba(img, 50, 50);
        assertTrue(center[0] > 200, "Center should be red, R=" + center[0]);

        // Corner (5,5) should be transparent (alpha = 0)
        int[] corner = rgba(img, 5, 5);
        assertTrue(corner[3] < 10, "Corner should be transparent, A=" + corner[3]);
    }

    @Test
    void testMultipleUseSameDef() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <rect width="100" height="50" fill="white"/>
                  <defs>
                    <rect id="r" width="20" height="20" fill="blue"/>
                  </defs>
                  <use href="#r" x="5" y="15"/>
                  <use href="#r" x="35" y="15"/>
                  <use href="#r" x="65" y="15"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Check blue pixels in three distinct regions
        boolean region1 = hasBluePixel(img, 5, 15, 24, 34);
        boolean region2 = hasBluePixel(img, 35, 15, 54, 34);
        boolean region3 = hasBluePixel(img, 65, 15, 84, 34);

        assertTrue(region1, "First use should have blue pixels");
        assertTrue(region2, "Second use should have blue pixels");
        assertTrue(region3, "Third use should have blue pixels");
    }

    @Test
    void testUseAttributeInheritance() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <defs>
                    <rect id="box" width="40" height="40"/>
                  </defs>
                  <use href="#box" x="5" y="5" fill="red"/>
                  <use href="#box" x="55" y="5" fill="blue"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Red pixels in top-left region
        boolean hasRed = false;
        for (int y = 5; y <= 44; y++) {
            for (int x = 5; x <= 44; x++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    hasRed = true;
                    break;
                }
            }
            if (hasRed)
                break;
        }
        assertTrue(hasRed, "First use with fill=red should render red pixels");

        // Blue pixels in top-right region
        boolean hasBlue = false;
        for (int y = 5; y <= 44; y++) {
            for (int x = 55; x <= 94; x++) {
                int[] c = rgba(img, x, y);
                if (c[2] > 200 && c[0] < 50 && c[1] < 50) {
                    hasBlue = true;
                    break;
                }
            }
            if (hasBlue)
                break;
        }
        assertTrue(hasBlue, "Second use with fill=blue should render blue pixels");
    }

    @Test
    void testClipPathWithRectangle() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <clipPath id="c">
                      <rect x="20" y="20" width="60" height="60"/>
                    </clipPath>
                  </defs>
                  <rect width="100" height="100" fill="red" clip-path="url(#c)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Center (50,50) should be red
        int[] center = rgba(img, 50, 50);
        assertTrue(center[0] > 200, "Center should be red, R=" + center[0]);

        // Corner (5,5) should be transparent
        int[] corner = rgba(img, 5, 5);
        assertTrue(corner[3] < 10, "Corner should be transparent, A=" + corner[3]);
    }

    @Test
    void testClipPathWithoutId() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <clipPath><rect width="50" height="50"/></clipPath>
                  </defs>
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error when clipPath has no id");
    }

    @Test
    void testGradientOrPatternWithNullName() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render with default fill when no gradient/pattern");
    }

    @Test
    void testUseWithNestedSvg() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <svg id="nested" width="30" height="30" viewBox="0 0 10 10">
                      <rect width="10" height="10" fill="green"/>
                    </svg>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <use href="#nested" x="10" y="10" width="50" height="50"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Should have green pixels in the use region
        boolean hasGreen = false;
        for (int y = 12; y < 55; y++) {
            for (int x = 12; x < 55; x++) {
                int[] c = rgba(img, x, y);
                if (c[1] > 100 && c[0] < 50 && c[2] < 50) {
                    hasGreen = true;
                    break;
                }
            }
            if (hasGreen)
                break;
        }
        assertTrue(hasGreen, "Expected green pixels from nested svg via use");
    }

    @Test
    void testUseWithWidthHeightOnSymbol() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <symbol id="s" viewBox="0 0 20 20">
                      <rect width="20" height="20" fill="blue"/>
                    </symbol>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <use href="#s" x="10" y="10" width="80" height="80"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Blue pixels should fill the 80x80 area
        int[] center = rgba(img, 50, 50);
        assertTrue(center[2] > 200 && center[0] < 50 && center[1] < 50,
                "Center of symbol should be blue, got rgb(%d,%d,%d)".formatted(center[0], center[1], center[2]));
    }

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testUseWithSymbolNoWidthHeight() throws Exception {
        // Symbol without explicit width/height on <use>: SVG 2 auto defaults to
        // viewBox dimensions, so the green rect should be 20x20 at (10,10).
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <symbol id="s2" viewBox="0 0 20 20">
                      <rect width="20" height="20" fill="green"/>
                    </symbol>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <use href="#s2" x="10" y="10"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Inside the 20x20 green area at (10,10)
        int[] inside = rgba(img, 20, 20);
        assertTrue(inside[1] > 100 && inside[0] < 50,
                "Inside should be green, got rgb(%d,%d,%d)".formatted(inside[0], inside[1], inside[2]));
        // Outside the 20x20 area: (35,35) should be white
        int[] outside = rgba(img, 35, 35);
        assertTrue(outside[0] > 200 && outside[1] > 200 && outside[2] > 200,
                "Outside should be white, got rgb(%d,%d,%d)".formatted(outside[0], outside[1], outside[2]));
    }

    @Test
    void testMultipleUseSymbolDifferentSizes() throws Exception {
        // Two <use> elements reference the same symbol at different sizes.
        // Width/height must NOT leak from the first use to the second.
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <defs>
                    <symbol id="s3" viewBox="0 0 10 10">
                      <rect width="10" height="10" fill="red"/>
                    </symbol>
                  </defs>
                  <use href="#s3" x="0" y="0" width="40" height="40"/>
                  <use href="#s3" x="100" y="0" width="80" height="80"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // First use: 40x40 red region at (0,0) — pixel (20,20) should be red
        int[] first = rgba(img, 20, 20);
        assertTrue(first[0] > 200 && first[1] < 50, "First use should be red at (20,20)");
        // First use edge: pixel (45,20) should be white (outside 40px width)
        int[] gap = rgba(img, 45, 20);
        assertTrue(gap[0] > 200 && gap[1] > 200 && gap[2] > 200, "Gap should be white at (45,20)");
        // Second use: 80x80 red region at (100,0) — pixel (140,40) should be red
        int[] second = rgba(img, 140, 40);
        assertTrue(second[0] > 200 && second[1] < 50, "Second use should be red at (140,40)");
    }

    @Test
    void testUseWithHrefWithoutHash() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <use href="externalWithoutHash" x="10" y="10"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "use without fragment should not crash");
    }

    @Test
    void testDefWithoutId() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <linearGradient>
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="50" height="50" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Def without id should be skipped gracefully");
    }

    @Test
    void testFontDefRegistration() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <font id="TestFont2" horiz-adv-x="1000">
                      <font-face font-family="TestFont2" units-per-em="1000" ascent="800" descent="-200"/>
                      <glyph unicode="X" horiz-adv-x="1000" d="M 0 0 L 0 800 L 1000 800 L 1000 0 Z"/>
                    </font>
                  </defs>
                  <text x="10" y="60" font-family="TestFont2" font-size="48" fill="red">X</text>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Font def should be registered and used");
    }

    @Test
    void testClipPathNullId() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <clipPath>
                      <rect width="50" height="50"/>
                    </clipPath>
                  </defs>
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // No clipPath effect, full rect should render
        int[] center = rgba(img, 50, 50);
        assertTrue(center[0] > 200, "Without clipPath id, full rect should render red");
    }

    /** Check if a region contains any blue pixel. */
    private static boolean hasBluePixel(BufferedImage img, int x1, int y1, int x2, int y2) {
        for (int y = y1; y <= Math.min(y2, img.getHeight() - 1); y++) {
            for (int x = x1; x <= Math.min(x2, img.getWidth() - 1); x++) {
                int[] c = rgba(img, x, y);
                if (c[3] > 0 && c[2] > 200 && c[0] < 50 && c[1] < 50) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── findNodeById with null root → returns null ───────────────────────

    @Test
    void findNodeByIdNullRoot() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <text font-size="12" fill="black">
                    <textPath href="#nonexistent">Text</textPath>
                  </text>
                </svg>
                """;
        assertDoesNotThrow(() -> render(svg));
    }

    // ── findNodeById deep nesting ────────────────────────────────────────

    @Test
    void findNodeByIdDeep() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <g>
                      <g>
                        <linearGradient id="deep">
                          <stop offset="0" stop-color="red"/>
                          <stop offset="1" stop-color="blue"/>
                        </linearGradient>
                      </g>
                    </g>
                  </defs>
                  <rect width="100" height="100" fill="url(#deep)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }
}

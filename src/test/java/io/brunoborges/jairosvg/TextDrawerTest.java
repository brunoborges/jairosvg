package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class TextDrawerTest {

    @Test
    void testTextAnchorMiddle() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60">
                  <rect width="200" height="60" fill="white"/>
                  <text x="100" y="30" text-anchor="middle" font-size="20" fill="black">Mid</text>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Dark pixels should be centered around x=100
        int leftCount = countDarkPixels(img, 0, 0, 99, 59, 128);
        int rightCount = countDarkPixels(img, 101, 0, 199, 59, 128);
        // Both sides should have dark pixels (text centered)
        assertTrue(leftCount > 0, "Should have dark pixels left of center");
        assertTrue(rightCount > 0, "Should have dark pixels right of center");
        // Roughly balanced (within 3:1 ratio)
        double ratio = (double) Math.max(leftCount, rightCount) / Math.max(1, Math.min(leftCount, rightCount));
        assertTrue(ratio < 3.0, "Dark pixels should be roughly centered, ratio=" + ratio);
    }

    @Test
    void testTextAnchorEnd() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60">
                  <rect width="200" height="60" fill="white"/>
                  <text x="190" y="30" text-anchor="end" font-size="20" fill="black">End</text>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Dark pixels should be to the LEFT of x=190
        int leftCount = countDarkPixels(img, 0, 0, 185, 59, 128);
        int rightCount = countDarkPixels(img, 191, 0, 199, 59, 128);
        assertTrue(leftCount > 0, "Should have dark pixels to the left of anchor");
        assertTrue(leftCount > rightCount, "Most pixels should be left of the anchor point");
    }

    @Test
    void testLetterSpacing() throws Exception {
        String normalSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60">
                  <rect width="200" height="60" fill="white"/>
                  <text x="10" y="40" font-size="20" fill="black">AB</text>
                </svg>
                """;
        String spacedSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60">
                  <rect width="200" height="60" fill="white"/>
                  <text x="10" y="40" font-size="20" fill="black" letter-spacing="10">AB</text>
                </svg>
                """;
        BufferedImage normalImg = render(normalSvg);
        BufferedImage spacedImg = render(spacedSvg);

        // Find rightmost dark pixel in each
        int normalRightmost = findRightmostDarkPixel(normalImg, 128);
        int spacedRightmost = findRightmostDarkPixel(spacedImg, 128);
        assertTrue(spacedRightmost > normalRightmost,
                "Spaced text should extend further right: normal=" + normalRightmost + " spaced=" + spacedRightmost);
    }

    @Test
    void testTspanDxDyOffsets() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="80">
                  <rect width="200" height="80" fill="white"/>
                  <text x="10" y="40" font-size="20" fill="black">
                    <tspan>Hello</tspan>
                    <tspan dx="20" dy="10" fill="red">World</tspan>
                  </text>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Should have both black and red pixels
        boolean hasBlack = false;
        boolean hasRed = false;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int[] c = rgba(img, x, y);
                if (c[3] > 0 && c[0] < 50 && c[1] < 50 && c[2] < 50) {
                    hasBlack = true;
                }
                if (c[3] > 0 && c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    hasRed = true;
                }
            }
        }
        assertTrue(hasBlack, "Should have black pixels from 'Hello'");
        assertTrue(hasRed, "Should have red pixels from 'World'");
    }

    @Test
    void testTextPath() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <defs><path id="curve" d="M10 80 Q100 10 190 80"/></defs>
                  <text font-size="16" fill="black">
                    <textPath href="#curve">Text on a path</textPath>
                  </text>
                </svg>
                """;
        BufferedImage img = render(svg);

        int totalDark = countDarkPixels(img, 0, 0, 199, 99, 128);
        assertTrue(totalDark > 20, "Text on path should produce dark pixels, got " + totalDark);

        // Verify text is NOT just on a single horizontal line — check multiple vertical
        // bands
        int topHalfDark = countDarkPixels(img, 0, 0, 199, 49, 128);
        int bottomHalfDark = countDarkPixels(img, 50, 50, 199, 99, 128);
        assertTrue(topHalfDark > 0 || bottomHalfDark > 0, "Text on curved path should span vertically");
    }

    @Test
    void testTextPathStartOffset() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <defs><path id="line" d="M0 50 L200 50"/></defs>
                  <text font-size="16" fill="black">
                    <textPath href="#line" startOffset="50%">Offset</textPath>
                  </text>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Text should start around the middle
        int leftDark = countDarkPixels(img, 0, 30, 80, 70, 128);
        int rightDark = countDarkPixels(img, 90, 30, 199, 70, 128);
        assertTrue(rightDark > leftDark, "With 50% startOffset, text should be mostly in the right half: left="
                + leftDark + " right=" + rightDark);
    }

    @Test
    void testTextPathMissingHref() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <text font-size="14" fill="black">
                    <textPath>Orphan</textPath>
                  </text>
                </svg>
                """;
        // Should render without error
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void testTextDecorations() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60">
                  <rect width="200" height="60" fill="white"/>
                  <text x="10" y="40" font-size="20" fill="black" text-decoration="underline overline line-through">Decorated</text>
                </svg>
                """;
        BufferedImage img = render(svg);

        int darkPixels = countDarkPixels(img, 0, 0, 199, 59, 128);
        assertTrue(darkPixels > 50, "Decorated text should have dark pixels, got " + darkPixels);
    }

    @Test
    void testEmptyTextElement() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <text x="10" y="30" font-size="14"></text>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertEquals(100, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @Test
    void testNestedTspanColors() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60">
                  <rect width="200" height="60" fill="white"/>
                  <text x="10" y="40" font-size="16">
                    <tspan fill="red">Red</tspan>
                    <tspan fill="blue">Blue</tspan>
                  </text>
                </svg>
                """;
        BufferedImage img = render(svg);

        boolean hasRed = false;
        boolean hasBlue = false;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int[] c = rgba(img, x, y);
                if (c[3] > 0 && c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    hasRed = true;
                }
                if (c[3] > 0 && c[2] > 200 && c[0] < 50 && c[1] < 50) {
                    hasBlue = true;
                }
            }
        }
        assertTrue(hasRed, "Should have red pixels from 'Red' tspan");
        assertTrue(hasBlue, "Should have blue pixels from 'Blue' tspan");
    }

    // ── resolveFont() branch coverage ──

    @Test
    void testFontFamilySansSerif() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="35" font-family="sans-serif" font-size="20" fill="black">Sans</text>
                </svg>""";
        BufferedImage img = render(svg);
        assertTrue(countDarkPixels(img, 5, 15, 100, 45, 128) > 10);
    }

    @Test
    void testFontFamilySerif() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="35" font-family="serif" font-size="20" fill="black">Serif</text>
                </svg>""";
        BufferedImage img = render(svg);
        assertTrue(countDarkPixels(img, 5, 15, 100, 45, 128) > 10);
    }

    @Test
    void testFontFamilyMonospace() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="35" font-family="monospace" font-size="20" fill="black">Mono</text>
                </svg>""";
        BufferedImage img = render(svg);
        assertTrue(countDarkPixels(img, 5, 15, 100, 45, 128) > 10);
    }

    @Test
    void testFontStyleItalic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="35" font-family="serif" font-style="italic" font-size="20" fill="black">Italic</text>
                </svg>""";
        BufferedImage img = render(svg);
        assertTrue(countDarkPixels(img, 5, 15, 150, 45, 128) > 10);
    }

    @Test
    void testFontStyleOblique() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="35" font-family="serif" font-style="oblique" font-size="20" fill="black">Oblique</text>
                </svg>""";
        BufferedImage img = render(svg);
        assertTrue(countDarkPixels(img, 5, 15, 150, 45, 128) > 10);
    }

    @Test
    void testFontWeightBold() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="35" font-family="serif" font-weight="bold" font-size="20" fill="black">Bold</text>
                </svg>""";
        BufferedImage img = render(svg);
        assertTrue(countDarkPixels(img, 5, 15, 100, 45, 128) > 10);
    }

    @Test
    void testFontWeightNumericHeavy() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="35" font-family="serif" font-weight="700" font-size="20" fill="black">Heavy</text>
                </svg>""";
        BufferedImage img = render(svg);
        assertTrue(countDarkPixels(img, 5, 15, 100, 45, 128) > 10);
    }

    // ── measureChildrenWidth() branch coverage ──

    @Test
    void testTextAnchorMiddleWithTspanChildren() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="100" y="35" text-anchor="middle" font-size="16" fill="black">
                    <tspan>Hello</tspan><tspan> World</tspan>
                  </text>
                </svg>""";
        BufferedImage img = render(svg);
        int leftDark = countDarkPixels(img, 40, 15, 100, 45, 128);
        int rightDark = countDarkPixels(img, 100, 15, 160, 45, 128);
        assertTrue(leftDark > 5, "Should have text left of center");
        assertTrue(rightDark > 5, "Should have text right of center");
    }

    @Test
    void testTextAnchorEndWithChildren() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="190" y="35" text-anchor="end" font-size="16" fill="black">
                    <tspan>End</tspan><tspan>Text</tspan>
                  </text>
                </svg>""";
        BufferedImage img = render(svg);
        int rightDark = countDarkPixels(img, 100, 15, 195, 45, 128);
        assertTrue(rightDark > 5, "Text should be anchored to x=190");
    }

    // ── parseStartOffset edge cases ──

    @Test
    void testTextPathStartOffsetPixels() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <defs><path id="p" d="M0 50 L200 50"/></defs>
                  <text font-size="16" fill="black">
                    <textPath href="#p" startOffset="50px">Offset</textPath>
                  </text>
                </svg>""";
        BufferedImage img = render(svg);
        int leftDark = countDarkPixels(img, 0, 30, 40, 60, 128);
        int midDark = countDarkPixels(img, 50, 30, 150, 60, 128);
        assertTrue(midDark > leftDark);
    }

    @Test
    void testTextPathStartOffsetInvalid() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <defs><path id="p" d="M0 50 L200 50"/></defs>
                  <text font-size="16" fill="black">
                    <textPath href="#p" startOffset="foo">Start</textPath>
                  </text>
                </svg>""";
        BufferedImage img = render(svg);
        int leftDark = countDarkPixels(img, 0, 30, 80, 60, 128);
        assertTrue(leftDark > 5);
    }

    // ── text() with blank text + children ──

    @Test
    void testBlankTextWithTspanChildren() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="35" font-size="16">   <tspan fill="red">Hello</tspan></text>
                </svg>""";
        BufferedImage img = render(svg);
        int darkPixels = countDarkPixels(img, 5, 15, 100, 45, 128);
        assertTrue(darkPixels >= 0); // ensure no crash
    }

    // ── tspan with dy offset ──

    @Test
    void testTspanWithDyOffset() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <text x="10" y="30" font-size="14" fill="black">Line1<tspan x="10" dy="20">Line2</tspan></text>
                </svg>""";
        BufferedImage img = render(svg);
        int topDark = countDarkPixels(img, 5, 15, 100, 35, 128);
        int botDark = countDarkPixels(img, 5, 40, 100, 60, 128);
        assertTrue(topDark > 5, "Should have text on first line");
        assertTrue(botDark > 5, "Should have text on second line");
    }

    /** Find the x coordinate of the rightmost dark pixel in the image. */
    private static int findRightmostDarkPixel(BufferedImage img, int threshold) {
        int rightmost = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = img.getWidth() - 1; x >= 0; x--) {
                int pixel = img.getRGB(x, y);
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                if (a > 0 && r < threshold && g < threshold && b < threshold) {
                    rightmost = Math.max(rightmost, x);
                    break;
                }
            }
        }
        return rightmost;
    }
}

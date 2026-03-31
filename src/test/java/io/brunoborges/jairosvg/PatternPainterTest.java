package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class PatternPainterTest {

    @Test
    void testPatternUserSpaceOnUse() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="p" patternUnits="userSpaceOnUse" x="0" y="0" width="20" height="20">
                      <rect width="10" height="10" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="100" height="100" fill="url(#p)"/>
                </svg>""";
        BufferedImage img = render(svg);
        // (5,5) should be red (inside the 10x10 red rect of the 20x20 tile)
        assertPixelColor(img, 5, 5, 255, 0, 0, 10);
        // (15,15) should be transparent/non-red (outside the 10x10 red rect)
        int[] px = rgba(img, 15, 15);
        assertTrue(px[0] < 50 || px[3] < 50, "Pixel at (15,15) should not be red");
    }

    @Test
    void testPatternHrefInheritance() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="base" width="20" height="20">
                      <rect width="10" height="10" fill="blue"/>
                    </pattern>
                    <pattern id="derived" href="#base" patternUnits="userSpaceOnUse"
                             x="0" y="0" width="20" height="20"/>
                  </defs>
                  <rect width="100" height="100" fill="url(#derived)"/>
                </svg>""";
        BufferedImage img = render(svg);
        // Should have blue pixels from inherited children
        boolean hasBlue = false;
        for (int y = 0; y < 20 && !hasBlue; y++) {
            for (int x = 0; x < 20 && !hasBlue; x++) {
                int[] c = rgba(img, x, y);
                if (c[3] > 200 && c[2] > 200 && c[0] < 50 && c[1] < 50) {
                    hasBlue = true;
                }
            }
        }
        assertTrue(hasBlue, "Derived pattern should render blue from base pattern children");
    }

    @Test
    void testPatternWithPatternTransform() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="p" patternUnits="userSpaceOnUse" width="20" height="20"
                             patternTransform="rotate(45)">
                      <rect width="10" height="10" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="100" height="100" fill="url(#p)"/>
                </svg>""";
        BufferedImage img = render(svg);
        // Should render without error and have some red pixels
        boolean hasRed = false;
        for (int y = 0; y < img.getHeight() && !hasRed; y++) {
            for (int x = 0; x < img.getWidth() && !hasRed; x++) {
                int[] c = rgba(img, x, y);
                if (c[3] > 200 && c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    hasRed = true;
                }
            }
        }
        assertTrue(hasRed, "Rotated pattern should still contain red pixels");
    }

    @Test
    void testMissingPatternReference() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="url(#nonexistent)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error when pattern is missing");
    }

    @Test
    void testEmptyPatternNoChildren() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <pattern id="empty" width="10" height="10"/>
                  </defs>
                  <rect width="50" height="50" fill="url(#empty)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error with empty pattern");
    }

    @Test
    void testPatternZeroWidth() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <pattern id="zero" patternUnits="userSpaceOnUse" width="0" height="10">
                      <rect width="5" height="5" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="50" height="50" fill="url(#zero)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error when pattern has zero width");
    }

    @Test
    void testPatternObjectBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="p" width="0.2" height="0.2">
                      <rect width="100%" height="100%" fill="red"/>
                    </pattern>
                  </defs>
                  <rect x="10" y="10" width="80" height="80" fill="url(#p)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Should have some red pixels in the rect area
        boolean hasRed = false;
        for (int y = 10; y < 90 && !hasRed; y++) {
            for (int x = 10; x < 90 && !hasRed; x++) {
                int[] c = rgba(img, x, y);
                if (c[3] > 200 && c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    hasRed = true;
                }
            }
        }
        assertTrue(hasRed, "objectBoundingBox pattern should render red tiles");
    }

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testPatternWithXYOffset() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="pxy" patternUnits="userSpaceOnUse" x="5" y="5" width="20" height="20">
                      <rect width="10" height="10" fill="green"/>
                    </pattern>
                  </defs>
                  <rect width="100" height="100" fill="url(#pxy)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Pattern with x/y offset should render");
        // Somewhere should be green
        boolean hasGreen = false;
        for (int y = 0; y < 100 && !hasGreen; y++) {
            for (int x = 0; x < 100 && !hasGreen; x++) {
                int[] c = rgba(img, x, y);
                if (c[1] > 100 && c[0] < 50 && c[2] < 50) {
                    hasGreen = true;
                }
            }
        }
        assertTrue(hasGreen, "Pattern with x/y offset should produce green pixels");
    }

    @Test
    void testPatternZeroHeight() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <pattern id="zh" patternUnits="userSpaceOnUse" width="10" height="0">
                      <rect width="5" height="5" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="50" height="50" fill="url(#zh)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error when pattern has zero height");
    }

    @Test
    void testPatternOnZeroSizeElement() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="pz" width="0.2" height="0.2">
                      <rect width="100%" height="100%" fill="red"/>
                    </pattern>
                  </defs>
                  <rect x="50" y="50" width="0" height="0" fill="url(#pz)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Pattern on zero-size element should not crash");
    }

    @Test
    void testPatternHrefInheritAttributes() throws Exception {
        // Child pattern inherits width/height/patternUnits from base
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="pbase" patternUnits="userSpaceOnUse" width="20" height="20">
                      <rect width="10" height="10" fill="blue"/>
                    </pattern>
                    <pattern id="pchild" href="#pbase">
                      <rect width="5" height="5" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="100" height="100" fill="url(#pchild)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img, "Pattern with href inheriting attrs should render");
    }

    // ── Missing pattern ref → drawPattern returns false ──────────────────

    @Test
    void missingPatternRef() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="url(#nopattern) green"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = RenderTestHelper.rgba(img, 25, 25);
        assertTrue(px[1] > 100, "Fallback green should be used");
    }

    // ── Pattern with no children → renders but no content ────────────────

    @Test
    void patternNoChildren() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <pattern id="p" width="10" height="10" patternUnits="userSpaceOnUse"/>
                  </defs>
                  <rect width="50" height="50" fill="url(#p)"/>
                </svg>""";
        assertDoesNotThrow(() -> render(svg));
    }

    // ── Pattern with zero-size tile ──────────────────────────────────────

    @Test
    void patternZeroSizeTile() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <pattern id="p" width="0" height="0" patternUnits="userSpaceOnUse">
                      <rect width="10" height="10" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="50" height="50" fill="url(#p)"/>
                </svg>""";
        assertDoesNotThrow(() -> render(svg));
    }

    // ── Pattern with patternTransform ────────────────────────────────────

    @Test
    void patternWithTransform() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="p" width="20" height="20" patternUnits="userSpaceOnUse"
                             patternTransform="rotate(45)">
                      <rect width="20" height="20" fill="yellow"/>
                      <rect x="5" y="5" width="10" height="10" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="100" height="100" fill="url(#p)"/>
                </svg>""";
        BufferedImage img = render(svg);
        assertNotNull(img);
    }
}

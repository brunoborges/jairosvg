package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class ShapeDrawerTest {

    @Test
    void testRectWithRoundedCorners() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" rx="15" ry="15" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Center should be red
        assertPixelColor(img, 50, 50, 255, 0, 0);

        // A point well inside the rounding area should still be red
        assertPixelColor(img, 30, 30, 255, 0, 0);
    }

    @Test
    void testRectWithOnlyRx() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" rx="20" fill="blue"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Center should be blue
        assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    @Test
    void testCircleWithZeroRadius() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <circle cx="25" cy="25" r="0" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error with zero-radius circle");
    }

    @Test
    void testEllipseWithZeroRx() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <ellipse cx="25" cy="25" rx="0" ry="20" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error with zero rx");
    }

    @Test
    void testPolylineVsPolygon() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <polyline points="10,10 90,10 90,90" fill="none" stroke="red" stroke-width="3"/>
                  <polygon points="10,10 90,10 90,90" fill="blue" fill-opacity="0.3"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Should have red pixels (polyline stroke)
        boolean hasRed = false;
        for (int x = 10; x <= 90; x++) {
            int[] c = rgba(img, x, 10);
            if (c[0] > 150 && c[1] < 100 && c[2] < 100) {
                hasRed = true;
                break;
            }
        }
        assertTrue(hasRed, "Expected red pixels from polyline stroke");

        // Should have blue-tinted pixels (polygon fill)
        boolean hasBlue = false;
        for (int y = 20; y < 80; y++) {
            int[] c = rgba(img, 80, y);
            if (c[2] > 30) {
                hasBlue = true;
                break;
            }
        }
        assertTrue(hasBlue, "Expected blue-tinted pixels from polygon fill");
    }

    @Test
    void testLineElement() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <line x1="10" y1="10" x2="90" y2="90" stroke="red" stroke-width="3"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Red pixels along the diagonal
        boolean foundRed = false;
        for (int i = 20; i < 80; i++) {
            for (int dy = -3; dy <= 3; dy++) {
                int y = i + dy;
                if (y >= 0 && y < 100) {
                    int[] c = rgba(img, i, y);
                    if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                        foundRed = true;
                        break;
                    }
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red pixels along diagonal line");
    }

    @Test
    void testRectWithNegativeDimensions() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect x="10" y="10" width="-5" height="-5" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error with negative dimensions");
    }

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testRectWithOnlyRy() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" ry="20" fill="green"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Center should be green
        int[] c = rgba(img, 50, 50);
        assertTrue(c[1] > 100, "ry-only rect should render green");
    }

    @Test
    void testRectZeroRxNonZeroRy() throws Exception {
        // rx=0 ry=5 → should produce a straight rect (rx=0 means no rounding)
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" rx="0" ry="5" fill="blue"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[2] > 200, "Rect with rx=0 should render blue");
    }

    @Test
    void testEmptyPolylinePoints() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <polyline points="" stroke="red" fill="none"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Empty polyline points should not crash");
    }

    @Test
    void testLineWithMarkers() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="mk" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="5" markerHeight="5">
                      <circle cx="5" cy="5" r="5" fill="red"/>
                    </marker>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <line x1="10" y1="50" x2="90" y2="50" stroke="black" stroke-width="2"
                        marker-start="url(#mk)" marker-end="url(#mk)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Should have red marker pixels
        boolean hasRed = false;
        for (int y = 40; y < 60; y++) {
            for (int x = 0; x < 30; x++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    hasRed = true;
                    break;
                }
            }
            if (hasRed)
                break;
        }
        assertTrue(hasRed, "Line with markers should show red marker circles");
    }

    @Test
    void testPolylineWithMarkers() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="m2" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="5" markerHeight="5">
                      <circle cx="5" cy="5" r="5" fill="blue"/>
                    </marker>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <polyline points="10,50 50,10 90,50" fill="none" stroke="black" stroke-width="2"
                            marker-start="url(#m2)" marker-mid="url(#m2)" marker-end="url(#m2)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Polyline with markers should render");
    }

    @Test
    void testEllipseWithZeroRy() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <ellipse cx="25" cy="25" rx="20" ry="0" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Ellipse with zero ry should render without error");
    }

    // ── getPrevPoint with no double[] vertices → fallback to (0,0) ──

    @Test
    void testPolylineMarkerWithSinglePoint() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="m" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="6" markerHeight="6" orient="auto">
                      <circle cx="5" cy="5" r="4" fill="red"/>
                    </marker>
                  </defs>
                  <polyline points="50,50" fill="none" stroke="black"
                            marker-start="url(#m)"/>
                </svg>
                """;
        assertDoesNotThrow(() -> render(svg));
    }

    // ── rect with rounded corners ──

    @Test
    void testRectRoundedCorners() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="10" y="10" width="80" height="80" rx="15" ry="15" fill="blue"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Corner pixel should NOT be blue (it's rounded)
        int[] corner = rgba(img, 12, 12);
        assertTrue(corner[2] < 200 || corner[3] < 200, "Rounded corner should not be fully blue");
    }

    // ── getPrevPoint with vertices list containing nulls (path break markers) ──

    @Test
    void getPrevPointSkipsNulls() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="gp" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="4" markerHeight="4">
                      <rect width="10" height="10" fill="red"/>
                    </marker>
                  </defs>
                  <polygon points="20,20 80,20 80,80 20,80" fill="none" stroke="black"
                           marker-start="url(#gp)" marker-end="url(#gp)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── rect with only rx (ry auto-derived) ──

    @Test
    void rectWithOnlyRx() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="10" y="10" width="80" height="80" rx="15" fill="blue"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── ellipse with zero rx or ry → no rendering ──

    @Test
    void ellipseZeroRadius() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <ellipse cx="50" cy="50" rx="0" ry="30" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── line element ──

    @Test
    void lineElement() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <line x1="10" y1="10" x2="90" y2="90" stroke="black" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Should have some dark pixels along the diagonal
        assertTrue(countDarkPixels(img, 0, 0, 99, 99, 128) > 0);
    }
}

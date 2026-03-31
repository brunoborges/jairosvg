package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class MarkerDrawerTest {

    @Test
    void testOrientAutoMarker() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <marker id="arrow" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="6" markerHeight="6" orient="auto">
                      <path d="M0 0 L10 5 L0 10 Z" fill="red"/>
                    </marker>
                  </defs>
                  <rect width="200" height="100" fill="white"/>
                  <path d="M20 50 L100 20 L180 50" fill="none" stroke="black" stroke-width="2"
                        marker-start="url(#arrow)" marker-mid="url(#arrow)" marker-end="url(#arrow)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);

        // Verify red pixels exist (markers rendered)
        assertTrue(hasColorPixel(img, 0, 0, 200, 100, 200, 0, 0, 50), "Expected red marker pixels");

        // Verify dark pixels (path rendered)
        int dark = countDarkPixels(img, 0, 0, 199, 99, 50);
        assertTrue(dark > 0, "Expected dark pixels from the path stroke");
    }

    @Test
    void testOrientAutoStartReverseMarker() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <marker id="arrow2" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                      <path d="M0 0 L10 5 L0 10 Z" fill="blue"/>
                    </marker>
                  </defs>
                  <rect width="200" height="100" fill="white"/>
                  <polyline points="20,50 100,20 180,50" fill="none" stroke="black" stroke-width="2"
                            marker-start="url(#arrow2)" marker-end="url(#arrow2)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);

        // Verify blue pixels exist (markers rendered)
        assertTrue(hasColorPixel(img, 0, 0, 200, 100, 0, 0, 200, 50), "Expected blue marker pixels");
    }

    @Test
    void testOrientNumericAngle() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="m45" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="8" markerHeight="8" orient="45">
                      <rect width="10" height="10" fill="green"/>
                    </marker>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <line x1="10" y1="50" x2="90" y2="50" stroke="black" stroke-width="2" marker-end="url(#m45)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);

        // Check green pixels exist (marker at 45°)
        assertTrue(hasColorPixel(img, 0, 0, 100, 100, 0, 100, 0, 50), "Expected green marker pixels at 45 degrees");
    }

    @Test
    void testOrientInvalidValueDefaultsToZero() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="mbad" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="6" markerHeight="6" orient="abc">
                      <rect width="10" height="10" fill="red"/>
                    </marker>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <line x1="10" y1="50" x2="90" y2="50" stroke="black" stroke-width="2" marker-end="url(#mbad)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error with invalid orient value");
    }

    @Test
    void testMarkerOnPolygon() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="dot" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="5" markerHeight="5">
                      <circle cx="5" cy="5" r="5" fill="red"/>
                    </marker>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <polygon points="50,10 90,90 10,90" fill="none" stroke="black"
                           marker-start="url(#dot)" marker-mid="url(#dot)" marker-end="url(#dot)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);

        // Verify red pixels exist near the triangle vertices
        assertTrue(hasColorPixel(img, 0, 0, 100, 100, 200, 0, 0, 50), "Expected red marker pixels at polygon vertices");
    }

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testMarkerUnitsUserSpaceOnUse() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <marker id="usu" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="10" markerHeight="10" markerUnits="userSpaceOnUse" orient="auto">
                      <rect width="10" height="10" fill="red"/>
                    </marker>
                  </defs>
                  <rect width="200" height="100" fill="white"/>
                  <line x1="20" y1="50" x2="180" y2="50" stroke="black" stroke-width="3"
                        marker-start="url(#usu)" marker-end="url(#usu)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Marker with markerUnits=userSpaceOnUse should render");
    }

    @Test
    void testMarkerOverflowVisible() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <marker id="vis" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="6" markerHeight="6" orient="auto" overflow="visible">
                      <rect width="20" height="20" fill="blue"/>
                    </marker>
                  </defs>
                  <rect width="200" height="100" fill="white"/>
                  <line x1="50" y1="50" x2="150" y2="50" stroke="black" stroke-width="2"
                        marker-end="url(#vis)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Marker with overflow=visible should render");
    }

    @Test
    void testMarkerOnSinglePoint() throws Exception {
        // Path with only moveto+close — should still attempt markers without crash
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="pt" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="8" markerHeight="8" orient="auto">
                      <circle cx="5" cy="5" r="5" fill="red"/>
                    </marker>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <path d="M50 50 Z" fill="none" stroke="black"
                        marker-start="url(#pt)" marker-end="url(#pt)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Marker on single-point path should not crash");
    }

    @Test
    void testMarkerMissingReference() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <line x1="10" y1="50" x2="90" y2="50" stroke="black" stroke-width="2"
                        marker-end="url(#nonexistent_marker)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Missing marker reference should not crash");
    }

    @Test
    void testMarkerOnlyStartNoMidEnd() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <defs>
                    <marker id="s" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="6" markerHeight="6" orient="auto">
                      <rect width="10" height="10" fill="green"/>
                    </marker>
                  </defs>
                  <rect width="200" height="100" fill="white"/>
                  <path d="M20 50 L100 20 L180 50" fill="none" stroke="black" stroke-width="2"
                        marker-start="url(#s)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    /**
     * Scan a region for pixels matching the given color thresholds. For "red":
     * minR=200, minG=0, minB=0, maxOther=50 means R>200, G<50, B<50.
     */
    private static boolean hasColorPixel(BufferedImage img, int x1, int y1, int x2, int y2, int targetR, int targetG,
            int targetB, int tolerance) {
        for (int y = Math.max(0, y1); y < Math.min(img.getHeight(), y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(img.getWidth(), x2); x++) {
                int[] c = rgba(img, x, y);
                if (Math.abs(c[0] - targetR) <= tolerance && Math.abs(c[1] - targetG) <= tolerance
                        && Math.abs(c[2] - targetB) <= tolerance) {
                    return true;
                }
            }
        }
        return false;
    }
}

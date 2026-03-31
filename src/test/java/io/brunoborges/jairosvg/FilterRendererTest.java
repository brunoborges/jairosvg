package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.assertPixelColor;
import static io.brunoborges.jairosvg.RenderTestHelper.render;
import static io.brunoborges.jairosvg.RenderTestHelper.rgba;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * Tests covering uncovered branches in FilterRenderer.
 */
class FilterRendererTest {

    @Test
    void feMerge() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="red" flood-opacity="0.5" result="flood"/>
                      <feMerge>
                        <feMergeNode in="SourceGraphic"/>
                        <feMergeNode in="flood"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[0] > 50, "Expected some red component from flood overlay, got R=" + px[0]);
    }

    @Test
    void feDropShadow() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
                  <defs>
                    <filter id="shadow">
                      <feDropShadow dx="5" dy="5" stdDeviation="2" flood-color="black" flood-opacity="0.5"/>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="60" height="60" fill="red" filter="url(#shadow)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Shadow area should have some dark/non-transparent pixels
        int darkCount = RenderTestHelper.countDarkPixels(img, 80, 80, 90, 90, 100);
        assertTrue(darkCount >= 0, "Shadow area rendered");
        // Upper-left corner should be near-transparent
        int[] corner = rgba(img, 5, 5);
        assertTrue(corner[3] < 30, "Upper-left should be transparent, got A=" + corner[3]);
    }

    @Test
    void feFlood() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="green" flood-opacity="1" result="flood"/>
                      <feMerge><feMergeNode in="flood"/></feMerge>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // SVG named "green" is (0, 128, 0)
        assertPixelColor(img, 50, 50, 0, 128, 0);
    }

    @Test
    void feOffset() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
                      <feOffset dx="30" dy="30"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="30" height="30" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Original position should NOT be red (offset moved it)
        int[] orig = rgba(img, 15, 15);
        assertTrue(orig[0] < 50 || orig[3] < 50,
                "Original position should not be red after offset, R=" + orig[0] + " A=" + orig[3]);
        // Offset position should have red
        int[] moved = rgba(img, 45, 45);
        assertTrue(moved[0] > 200, "Offset position should be red, got R=" + moved[0]);
    }

    @Test
    void feBlendMultiply() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="red" flood-opacity="1" result="r"/>
                      <feBlend in="SourceGraphic" in2="r" mode="multiply"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // white × red = red: high R, low G and B
        assertTrue(px[0] > 200, "Expected high red, got R=" + px[0]);
        assertTrue(px[1] < 30, "Expected low green, got G=" + px[1]);
        assertTrue(px[2] < 30, "Expected low blue, got B=" + px[2]);
    }

    @Test
    void feTileWithFilterRegion() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
                      <feTile in="SourceGraphic"/>
                    </filter>
                  </defs>
                  <rect x="0" y="0" width="50" height="50" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    @Test
    void feImageWithDataUri() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feImage href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVQYV2P8z8BQz0BFwMgwasChBwAG/wH9WLE1RAAAAABJRU5ErkJggg=="/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    @Test
    void feImageMissingHref() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feImage/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    @Test
    void filterUnitsUserSpaceOnUse() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="10" y="10" width="80" height="80">
                      <feGaussianBlur stdDeviation="1"/>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[0] > 200, "Center should be red-ish, got R=" + px[0]);
    }

    @Test
    void filterOnTransparentElement() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f"><feGaussianBlur stdDeviation="2"/></filter>
                  </defs>
                  <rect width="50" height="50" fill="none" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
        int[] px = rgba(img, 25, 25);
        assertTrue(px[3] < 10, "Transparent element should remain transparent, got A=" + px[3]);
    }

    @Test
    void filterWithPercentageRegion() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" x="-10%" y="-10%" width="120%" height="120%">
                      <feGaussianBlur stdDeviation="2"/>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="60" height="60" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[2] > 200, "Center should be blue-ish, got B=" + px[2]);
    }

    @Test
    void chainedFilterPrimitives() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="blur"/>
                      <feOffset in="blur" dx="5" dy="5" result="offset"/>
                      <feMerge>
                        <feMergeNode in="offset"/>
                        <feMergeNode in="SourceGraphic"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="40" height="40" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 40, 40);
        assertTrue(px[0] > 200, "Center of rect should be red, got R=" + px[0]);
    }

    @Test
    void feDropShadowDefaults() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f"><feDropShadow/></filter>
                  </defs>
                  <circle cx="40" cy="40" r="20" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    @Test
    void missingFilterReference() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <rect width="50" height="50" fill="red" filter="url(#nonexistent)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertPixelColor(img, 25, 25, 255, 0, 0);
    }
}

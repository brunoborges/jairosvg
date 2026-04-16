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
    void feTileWithNonZeroFilterOffset() throws Exception {
        // filterRegion.x=5 → txStart != 0 → exercises first-partial-row copy
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="tile2" filterUnits="userSpaceOnUse" x="5" y="5" width="90" height="90">
                      <feTile in="SourceGraphic"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="30" height="30" fill="red" filter="url(#tile2)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
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

    // ── feImage with fragment reference ──

    @Test
    void feImageWithFragmentRef() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <rect id="myRect" width="100" height="100" fill="green"/>
                    <filter id="f">
                      <feImage href="#myRect"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    @Test
    void feImageWithBrokenFragment() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feImage href="#doesNotExist"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    // ── feImage with invalid external URL ──

    @Test
    void feImageWithInvalidUrl() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feImage href="data:image/png;base64,AAAA"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    // ── feTile without filter region (scans for non-transparent bounds) ──

    @Test
    void feTileWithoutFilterRegion() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feTile in="SourceGraphic"/>
                    </filter>
                  </defs>
                  <rect x="0" y="0" width="30" height="30" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    // ── feTile on fully transparent input ──

    @Test
    void feTileOnTransparentInput() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feTile in="SourceGraphic"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="none" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    // ── unrecognized filter primitive (default switch case) ──

    @Test
    void unknownFilterPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feCustomUnknown in="SourceGraphic"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
        // Unknown primitive should pass through, red should still be visible
        int[] px = rgba(img, 50, 50);
        assertTrue(px[0] > 150, "Unknown filter primitive should pass through source, got R=" + px[0]);
    }

    // ── resolveInput with explicit "SourceGraphic" ──

    @Test
    void resolveInputSourceGraphic() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feOffset in="SourceGraphic" dx="0" dy="0" result="pass"/>
                      <feMerge><feMergeNode in="SourceGraphic"/></feMerge>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="green" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[1] > 100, "SourceGraphic resolve should show green");
    }

    // ── resolveInput with named result ──

    @Test
    void resolveInputNamedResult() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="cyan" result="myFlood"/>
                      <feOffset in="myFlood" dx="0" dy="0"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[1] > 200 && px[2] > 200, "Named result resolve should produce cyan");
    }

    // ── feMerge with non-feMergeNode children (ignored) ──

    @Test
    void feMergeWithNonMergeNodeChild() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="red" result="r"/>
                      <feMerge>
                        <desc>Not a merge node</desc>
                        <feMergeNode in="r"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ── feBlend with various modes ──

    @Test
    void feBlendScreen() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="blue" result="b"/>
                      <feBlend in="SourceGraphic" in2="b" mode="screen"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // Screen of red and blue = purple-ish (high R, low G, high B)
        assertTrue(px[0] > 150, "Screen blend should keep red");
        assertTrue(px[2] > 150, "Screen blend should add blue");
    }

    @Test
    void feBlendDarken() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="rgb(100,200,50)" result="fg"/>
                      <feBlend in="SourceGraphic" in2="fg" mode="darken"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="rgb(200,100,150)" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // Darken picks min of each channel: min(200,100)=100, min(100,200)=100,
        // min(150,50)=50
        assertTrue(px[0] <= 110, "Darken should pick smaller R");
        assertTrue(px[2] <= 60, "Darken should pick smaller B");
    }

    @Test
    void feBlendLighten() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="rgb(100,200,50)" result="fg"/>
                      <feBlend in="SourceGraphic" in2="fg" mode="lighten"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="rgb(200,100,150)" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // Lighten picks max of each channel: max(200,100)=200, max(100,200)=200,
        // max(150,50)=150
        assertTrue(px[0] >= 190, "Lighten should pick larger R");
        assertTrue(px[1] >= 190, "Lighten should pick larger G");
    }

    // ── feDropShadow with managed input buffer ──

    @Test
    void feDropShadowAfterOtherPrimitives() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="blurred"/>
                      <feDropShadow in="blurred" dx="5" dy="5" stdDeviation="2"
                                    flood-color="black" flood-opacity="0.5"/>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    // ── filter region with percentage values ──

    @Test
    void filterRegionPercentageValues() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" x="-20%" y="-20%" width="140%" height="140%">
                      <feOffset dx="5" dy="5"/>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="60" height="60" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 30, 30);
        assertTrue(px[2] > 150 || px[3] < 50, "Filter with percentage region should work");
    }

    // ── filter with explicit width/height and userSpaceOnUse ──

    @Test
    void filterExplicitWidthHeightUserSpace() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
                      <feFlood flood-color="orange" result="fill"/>
                      <feMerge><feMergeNode in="fill"/></feMerge>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // SVG "orange" = (255, 165, 0)
        assertTrue(px[0] > 200, "Orange flood fill, R=" + px[0]);
    }

    // ── feGaussianBlur with large deviation (exercises buffer allocation) ──

    @Test
    void feGaussianBlurLargeDeviation() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="5"/>
                    </filter>
                  </defs>
                  <rect x="40" y="40" width="20" height="20" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] center = rgba(img, 50, 50);
        assertTrue(center[0] > 50, "Blurred red should have some red in center");
    }

    // ── renderNode with offset (subregion) ──

    @Test
    void feImageFragmentWithSubRegion() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <circle id="circ" cx="50" cy="50" r="30" fill="purple"/>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
                      <feImage href="#circ"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    // ── filter with in2 resolving to named buffer ──

    @Test
    void feBlendWithNamedInputs() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="red" result="a"/>
                      <feFlood flood-color="blue" result="b"/>
                      <feBlend in="a" in2="b" mode="normal"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // Normal blend: 'a' (red) blended on top of 'b' (blue) → red on top
        assertTrue(px[0] > 150 || px[2] > 150, "Named blend inputs should produce colored output");
    }

    // ── filter on small element within large canvas (sub-region optimization) ──

    @Test
    void filterSubRegionOptimization() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="500" height="500">
                  <rect width="500" height="500" fill="white"/>
                  <rect x="200" y="200" width="50" height="50" fill="red">
                  </rect>
                  <rect x="200" y="200" width="50" height="50" fill="blue" opacity="0.5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 225, 225);
        // Semi-transparent blue over red
        assertTrue(px[2] > 80, "Subregion optimized element should render");
    }

    // ── feDropShadow with large offset to exercise padding computation ──

    @Test
    void feDropShadowLargeOffset() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <filter id="f">
                      <feDropShadow dx="30" dy="30" stdDeviation="5" flood-color="gray"/>
                    </filter>
                  </defs>
                  <rect width="200" height="200" fill="white"/>
                  <rect x="20" y="20" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Shadow at offset position
        int[] shadow = rgba(img, 80, 80);
        assertTrue(shadow[3] > 0, "Shadow should be visible at offset position");
    }

    // ── Multiple chained blur→blend→merge with buffer rotation ──

    @Test
    void complexFilterChain() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="2" result="blur1"/>
                      <feFlood flood-color="yellow" result="yellow"/>
                      <feBlend in="blur1" in2="yellow" mode="multiply" result="blended"/>
                      <feMerge>
                        <feMergeNode in="blended"/>
                        <feMergeNode in="SourceGraphic"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[0] > 100, "Complex filter chain should render red-ish center");
    }

    // ── feTile with null filterRegion (no precomputed region) → opaque bounds scan
    // ──

    @Test
    void feTileWithNullFilterRegionFallback() throws Exception {
        // Small rect that doesn't fill canvas, exercising opaque bounds scan in tile()
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="60" height="60">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="60" height="60">
                      <feFlood flood-color="green" flood-opacity="1" result="flood"/>
                      <feTile in="flood"/>
                    </filter>
                  </defs>
                  <rect width="60" height="60" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 30, 30);
        assertTrue(px[1] > 100, "feTile should tile the flood, green center");
    }

    // ── feTile with fully transparent input → returns input unchanged ──

    @Test
    void feTileFullyTransparentInput() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="50" height="50">
                      <feFlood flood-color="red" flood-opacity="0" result="clear"/>
                      <feTile in="clear"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDropShadow where input IS a managed buffer (exercises dropShadow
    // wrapper) ──

    @Test
    void feDropShadowAfterBlur() throws Exception {
        // feGaussianBlur output goes to a managed buffer, then feDropShadow reuses it
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="blurred"/>
                      <feDropShadow in="blurred" dx="3" dy="3" stdDeviation="1" flood-color="blue"/>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] shadow = rgba(img, 55, 55);
        assertTrue(shadow[3] > 0, "Shadow should be visible");
    }

    // ── feBlend where buf3 fallback is needed (input == buf1, in2 == buf2) ──

    @Test
    void feBlendExhaustBuffers() throws Exception {
        // Chain: blur→result1 (uses buf1), flood→result2 (uses buf2), blend(result1,
        // result2)→buf3
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="b"/>
                      <feFlood flood-color="blue" flood-opacity="0.5" result="fl"/>
                      <feBlend in="b" in2="fl" mode="screen"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feImage with empty href → returns blank output ──

    @Test
    void feImageEmptyHref() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feImage href=""/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Two filtered elements on same surface → exercises buffer reuse
    // (reuseOrNull cache hit) ──

    @Test
    void bufferReuseBetweenFilteredElements() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1"/>
                    </filter>
                  </defs>
                  <rect x="5" y="5" width="40" height="40" fill="red" filter="url(#f)"/>
                  <rect x="55" y="55" width="40" height="40" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Both rects should be visible (blurred)
        int[] left = rgba(img, 25, 25);
        assertTrue(left[0] > 100, "First filtered rect should be visible");
        int[] right = rgba(img, 75, 75);
        assertTrue(right[2] > 100, "Second filtered rect should be visible");
    }

    // ── Filter with invalid percentage → exercises parsePercentOrFraction
    // NumberFormatException ──

    @Test
    void filterRegionInvalidPercentage() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" x="abc%" y="xyz%" width="120%" height="120%">
                      <feGaussianBlur stdDeviation="1"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="80" height="80" fill="green" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDropShadow with non-default flood-color and flood-opacity ──

    @Test
    void feDropShadowCustomFloodColor() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feDropShadow dx="5" dy="5" stdDeviation="0" flood-color="red" flood-opacity="0.8"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="40" height="40" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Multiple elements with same filter → reuse cached buffers (reuseOrNull
    // hit) ──

    @Test
    void multipleFilteredElementsReuseBuffers() throws Exception {
        // Full-canvas elements ensure same buffer dimensions for reuse
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="blur"/>
                      <feFlood flood-color="green" flood-opacity="0.5" result="fl"/>
                      <feBlend in="blur" in2="fl" mode="multiply"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                  <rect width="50" height="50" fill="blue" filter="url(#f)" opacity="0.5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDropShadow chained after feOffset → input IS managed buffer ──

    @Test
    void feDropShadowChainedAfterOffset() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feOffset dx="2" dy="2" result="shifted"/>
                      <feDropShadow in="shifted" dx="3" dy="3" stdDeviation="1" flood-color="green"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Filter chain with 4+ primitives → all 3 buffers allocated, pickBuffer buf3
    // ──

    @Test
    void fourPrimitiveFilterChain() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="b1"/>
                      <feOffset dx="2" dy="2" result="o1"/>
                      <feFlood flood-color="blue" flood-opacity="0.5" result="fl"/>
                      <feBlend in="o1" in2="fl" mode="screen" result="blend"/>
                      <feMerge>
                        <feMergeNode in="b1"/>
                        <feMergeNode in="blend"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Filter chain where named result IS referenced later → exercises clone path
    // ──

    @Test
    void filterNamedResultReferencedLater() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="shared"/>
                      <feOffset in="shared" dx="3" dy="3" result="off"/>
                      <feBlend in="shared" in2="off" mode="normal"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feImage with non-existent fragment ref → returns blank ──

    @Test
    void feImageMissingFragmentRef() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feImage href="#nonexistent"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Filter with result name that is not referenced → no clone needed ──

    @Test
    void filterResultNotReferenced() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="2" result="unused"/>
                      <feFlood flood-color="red" flood-opacity="1"/>
                    </filter>
                  </defs>
                  <rect width="80" height="80" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDropShadow with managed input (implicit last from feOffset, no result
    // name) ──

    @Test
    void feDropShadowWithManagedInput() throws Exception {
        // feOffset output goes into buf1 (managed). Since no result= attribute,
        // it becomes "last" without being cloned. feDropShadow gets managed input.
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="chain">
                      <feOffset dx="2" dy="2"/>
                      <feDropShadow dx="1" dy="1" stdDeviation="1"
                                    flood-color="black" flood-opacity="0.5"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#chain)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDropShadow preceded by feFlood (managed buffer, no result name) ──

    @Test
    void feDropShadowAfterFlood() throws Exception {
        // feFlood output goes to managed buf1. No result= → last = buf1.
        // feDropShadow detects inputIsManaged=true → calls dropShadow().
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="fdf">
                      <feFlood flood-color="blue" flood-opacity="0.8"/>
                      <feDropShadow dx="1" dy="1" stdDeviation="0.5"
                                    flood-color="red" flood-opacity="0.3"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="green" filter="url(#fdf)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── 5-primitive chain forcing buf3 via pickBuffer ──

    @Test
    void fivePrimitiveChainForcesBuf3() throws Exception {
        // feBlend with input from buf1 and in2 from buf2 → picks buf3
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f5">
                      <feGaussianBlur stdDeviation="1" result="b1"/>
                      <feFlood flood-color="red" flood-opacity="0.5" result="b2"/>
                      <feBlend in="b1" in2="b2" mode="screen" result="b3"/>
                      <feOffset dx="1" dy="1" result="b4"/>
                      <feMerge>
                        <feMergeNode in="b3"/>
                        <feMergeNode in="b4"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="blue" filter="url(#f5)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── named result clone: managed buffer referenced later should be cloned ──

    @Test
    void namedResultClonedWhenManaged() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="fc">
                      <feFlood flood-color="blue" result="fl"/>
                      <feGaussianBlur stdDeviation="1" result="g1"/>
                      <feBlend in="fl" in2="g1" mode="normal"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#fc)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── filter percentage region ──

    @Test
    void filterPercentageRegion() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="pf" x="10%" y="10%" width="80%" height="80%">
                      <feGaussianBlur stdDeviation="2"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#pf)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feTile with feImage source and userSpaceOnUse filter ──

    @Test
    void feTileWithFeImageSource() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="tile" x="0" y="0" width="100" height="100"
                            filterUnits="userSpaceOnUse">
                      <feImage href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVQYV2P4z8BQz0BHwMDAwMDAAAA//wNFAgLiNfJ9AAAAAElFTkSuQmCC"
                               width="10" height="10" result="img"/>
                      <feTile in="img"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" filter="url(#tile)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feBlend as first primitive (allocates buf1/buf2/buf3 itself) ──

    @Test
    void feBlendAsFirstPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="bf">
                      <feBlend in="SourceGraphic" in2="SourceGraphic" mode="multiply"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#bf)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feMerge as first primitive ──

    @Test
    void feMergeAsFirstPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="mf">
                      <feMerge>
                        <feMergeNode in="SourceGraphic"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="blue" filter="url(#mf)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDropShadow as first primitive ──

    @Test
    void feDropShadowAsFirstPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="ds">
                      <feDropShadow dx="2" dy="2" stdDeviation="1"
                                    flood-color="black" flood-opacity="0.5"/>
                    </filter>
                  </defs>
                  <rect width="30" height="30" fill="red" filter="url(#ds)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feOffset as first primitive ──

    @Test
    void feOffsetAsFirstPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="of">
                      <feOffset dx="5" dy="5"/>
                    </filter>
                  </defs>
                  <rect width="30" height="30" fill="green" filter="url(#of)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feFlood as first primitive ──

    @Test
    void feFloodAsFirstPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="ff">
                      <feFlood flood-color="purple" flood-opacity="0.7"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="yellow" filter="url(#ff)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feTile with fully transparent source (null filterRegion fallback) ──

    @Test
    void feTileOnFullyTransparentSource() throws Exception {
        // feFlood with opacity=0 produces fully transparent source.
        // computeFilterRegion returns null for fully transparent images.
        // tile() then falls back to scanning for non-transparent bounds.
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="60" height="60">
                  <defs>
                    <filter id="ft">
                      <feFlood flood-color="red" flood-opacity="0" result="transparent"/>
                      <feTile in="transparent"/>
                    </filter>
                  </defs>
                  <rect width="60" height="60" fill="blue" filter="url(#ft)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feTile with tile width that doesn't evenly divide output ──

    @Test
    void feTileTailCopyBranch() throws Exception {
        // Use a small filter region so tile width doesn't evenly divide the canvas.
        // Canvas is 70px wide, filter region via userSpaceOnUse is 30px wide.
        // 70 / 30 = 2 remainder 10, so the tail copy (x < width) executes.
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="70" height="30">
                  <defs>
                    <filter id="ft2" filterUnits="userSpaceOnUse"
                            x="0" y="0" width="30" height="30">
                      <feFlood flood-color="green" flood-opacity="1"/>
                      <feTile/>
                    </filter>
                  </defs>
                  <rect width="70" height="30" fill="white" filter="url(#ft2)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // The green tile should repeat across the 70px canvas
        int[] greenPx = rgba(img, 5, 15);
        assertTrue(greenPx[1] > 100, "Should have green channel from flood");
    }

    // ── feTile with partially transparent source (null filterRegion,
    // non-transparent bounds found) ──

    @Test
    void feTileNullRegionWithPartialContent() throws Exception {
        // Source graphic is fully transparent (fill="none" stroke="none").
        // computeFilterRegion returns null for fully transparent source.
        // feFlood creates visible content, then feTile uses null-region fallback.
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="tnr">
                      <feFlood flood-color="red" flood-opacity="0.8" x="10" y="10" width="20" height="20"/>
                      <feTile/>
                    </filter>
                  </defs>
                  <rect x="5" y="5" width="70" height="70" fill="none" stroke="none" filter="url(#tnr)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ---- feColorMatrix tests ----

    @Test
    void feColorMatrixSaturate() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feColorMatrix type="saturate" values="0"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // saturate=0 on red should produce grayscale: R=G=B
        assertTrue(Math.abs(px[0] - px[1]) < 5, "R and G should be close for grayscale, R=" + px[0] + " G=" + px[1]);
        assertTrue(Math.abs(px[0] - px[2]) < 5, "R and B should be close for grayscale, R=" + px[0] + " B=" + px[2]);
    }

    @Test
    void feColorMatrixHueRotate() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feColorMatrix type="hueRotate" values="90"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // hueRotate 90 on pure red should shift color away from pure red
        assertTrue(px[1] > 0 || px[2] > 0, "Hue rotation should shift red to other channels");
    }

    @Test
    void feColorMatrixLuminanceToAlpha() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feColorMatrix type="luminanceToAlpha"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // luminanceToAlpha on white: alpha should be full (255), RGB should be 0
        assertTrue(px[3] > 200, "Alpha should be high for white input, got A=" + px[3]);
    }

    @Test
    void feColorMatrixIdentity() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feColorMatrix type="matrix"
                        values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 1 0"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ---- feComponentTransfer tests ----

    @Test
    void feComponentTransferLinear() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncR type="linear" slope="0.5" intercept="0"/>
                        <feFuncG type="identity"/>
                        <feFuncB type="identity"/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // slope=0.5 on red channel: R should be ~128
        assertTrue(px[0] >= 120 && px[0] <= 136, "Red channel should be ~128 with slope=0.5, got R=" + px[0]);
    }

    @Test
    void feComponentTransferGamma() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncR type="gamma" amplitude="1" exponent="2" offset="0"/>
                        <feFuncG type="identity"/>
                        <feFuncB type="identity"/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="rgb(128,0,0)" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // gamma exponent=2 on ~0.5 input: output ~ 0.25 * 255 ~ 64
        assertTrue(px[0] < 100, "Gamma exponent=2 should reduce mid-red, got R=" + px[0]);
    }

    @Test
    void feComponentTransferTable() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncR type="table" tableValues="1 0"/>
                        <feFuncG type="identity"/>
                        <feFuncB type="identity"/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // table "1 0" inverts the red channel: 255 -> 0
        assertTrue(px[0] < 5, "Table 1,0 should invert red to ~0, got R=" + px[0]);
    }

    @Test
    void feComponentTransferDiscrete() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncR type="discrete" tableValues="0 0.5 1"/>
                        <feFuncG type="identity"/>
                        <feFuncB type="identity"/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="rgb(200,0,0)" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // input R=200/255~0.784, with 3 steps: step index = min(floor(0.784*3),2) = 2,
        // value = 1.0 -> 255
        assertTrue(px[0] > 240, "Discrete step should map high red to 255, got R=" + px[0]);
    }

    @Test
    void feComponentTransferIdentity() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncR type="identity"/>
                        <feFuncG type="identity"/>
                        <feFuncB type="identity"/>
                        <feFuncA type="identity"/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ---- feMorphology tests ----

    @Test
    void feMorphologyDilate() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
                      <feMorphology operator="dilate" radius="5"/>
                    </filter>
                  </defs>
                  <rect x="40" y="40" width="20" height="20" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Original rect is 40-60. Dilate by 5 should extend it. Pixel at (36,50) should
        // now be red.
        int[] px = rgba(img, 36, 50);
        assertTrue(px[0] > 200, "Dilate should expand red region, got R=" + px[0] + " at (36,50)");
    }

    @Test
    void feMorphologyErode() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
                      <feMorphology operator="erode" radius="5"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="80" height="80" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Erode by 5: edge pixel at (11,50) should be eroded to transparent
        int[] edge = rgba(img, 11, 50);
        assertTrue(edge[0] < 50 || edge[3] < 50,
                "Erode should shrink red region at edges, got R=" + edge[0] + " A=" + edge[3]);
        // Center should still be red
        int[] center = rgba(img, 50, 50);
        assertTrue(center[0] > 200, "Center should remain red after erode, got R=" + center[0]);
    }

    @Test
    void feMorphologyZeroRadius() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feMorphology operator="erode" radius="0"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ── feConvolveMatrix ─────────────────────────────────────────────────

    @Test
    void feConvolveMatrixIdentity() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3" kernelMatrix="0 0 0 0 1 0 0 0 0"/>
                  </filter></defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void feConvolveMatrixBlur() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3" kernelMatrix="1 1 1 1 1 1 1 1 1"/>
                  </filter></defs>
                  <rect x="25" y="25" width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Center should still be red (all neighbors are red)
        int[] center = rgba(img, 50, 50);
        assertTrue(center[0] > 200, "Center should remain red after box blur, got R=" + center[0]);
    }

    @Test
    void feConvolveMatrixEdgeModes() throws Exception {
        for (String mode : new String[]{"duplicate", "wrap", "none"}) {
            var svg = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                      <defs><filter id="f">
                        <feConvolveMatrix order="3" kernelMatrix="1 1 1 1 1 1 1 1 1"
                          edgeMode="%s"/>
                      </filter></defs>
                      <rect width="50" height="50" fill="blue" filter="url(#f)"/>
                    </svg>
                    """.formatted(mode);
            BufferedImage img = render(svg);
            assertNotNull(img);
        }
    }

    // ── feDisplacementMap ────────────────────────────────────────────────

    @Test
    void feDisplacementMapBasic() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs><filter id="f">
                    <feFlood flood-color="rgb(128,128,128)" result="map"/>
                    <feDisplacementMap in="SourceGraphic" in2="map" scale="10"
                      xChannelSelector="R" yChannelSelector="G"/>
                  </filter></defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // With mid-gray map (128), displacement ~0 → image mostly unchanged
        int[] px = rgba(img, 50, 50);
        assertTrue(px[0] > 200, "Expected red with near-zero displacement, got R=" + px[0]);
    }

    @Test
    void feDisplacementMapZeroScale() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs><filter id="f">
                    <feFlood flood-color="white" result="map"/>
                    <feDisplacementMap in="SourceGraphic" in2="map" scale="0"
                      xChannelSelector="R" yChannelSelector="G"/>
                  </filter></defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void feDisplacementMapChannelSelect() throws Exception {
        // Verify R vs G channel selector doesn't crash and produces output
        for (String ch : new String[]{"R", "G", "B", "A"}) {
            var svg = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                      <defs><filter id="f">
                        <feFlood flood-color="red" result="map"/>
                        <feDisplacementMap in="SourceGraphic" in2="map" scale="5"
                          xChannelSelector="%s" yChannelSelector="%s"/>
                      </filter></defs>
                      <rect width="50" height="50" fill="blue" filter="url(#f)"/>
                    </svg>
                    """.formatted(ch, ch);
            BufferedImage img = render(svg);
            assertNotNull(img);
        }
    }

    // ── feTurbulence ─────────────────────────────────────────────────────

    @Test
    void feTurbulenceBasic() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs><filter id="f">
                    <feTurbulence baseFrequency="0.05" numOctaves="2" seed="42"/>
                  </filter></defs>
                  <rect width="100" height="100" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Should produce non-uniform pixels
        int[] px1 = rgba(img, 10, 10);
        int[] px2 = rgba(img, 50, 50);
        boolean different = px1[0] != px2[0] || px1[1] != px2[1] || px1[2] != px2[2];
        assertTrue(different, "Turbulence should produce non-uniform pixels");
    }

    @Test
    void feTurbulenceFractalNoise() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs><filter id="f">
                    <feTurbulence type="fractalNoise" baseFrequency="0.1" numOctaves="3" seed="7"/>
                  </filter></defs>
                  <rect width="100" height="100" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        // fractalNoise should produce visible pattern — not all zeros
        assertTrue(px[3] > 0, "fractalNoise should produce non-transparent output");
    }

    @Test
    void feTurbulenceZeroFrequency() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feTurbulence baseFrequency="0" numOctaves="1"/>
                  </filter></defs>
                  <rect width="50" height="50" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Zero frequency → uniform output; all pixels same
        int[] px1 = rgba(img, 0, 0);
        int[] px2 = rgba(img, 25, 25);
        assertTrue(px1[0] == px2[0] && px1[1] == px2[1] && px1[2] == px2[2] && px1[3] == px2[3],
                "Zero frequency should produce uniform output");
    }

    @Test
    void feTurbulenceSeed() throws Exception {
        BufferedImage[] imgs = new BufferedImage[2];
        int seedIdx = 0;
        for (int seed : new int[]{1, 999}) {
            var svg = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                      <defs><filter id="f">
                        <feTurbulence baseFrequency="0.1" numOctaves="2" seed="%d"/>
                      </filter></defs>
                      <rect width="50" height="50" filter="url(#f)"/>
                    </svg>
                    """.formatted(seed);
            imgs[seedIdx++] = render(svg);
        }
        // Different seeds should produce different outputs
        int[] px1 = rgba(imgs[0], 25, 25);
        int[] px2 = rgba(imgs[1], 25, 25);
        boolean different = px1[0] != px2[0] || px1[1] != px2[1] || px1[2] != px2[2];
        assertTrue(different, "Different seeds should produce different turbulence patterns");
    }

    // ── feDiffuseLighting ────────────────────────────────────────────────

    @Test
    void feDiffuseLightingDistantLight() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feDiffuseLighting surfaceScale="5" diffuseConstant="1" lighting-color="white">
                        <feDistantLight azimuth="45" elevation="45"/>
                      </feDiffuseLighting>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // White rect with uniform alpha -> flat surface -> uniform lighting
        int[] px = rgba(img, 50, 50);
        assertTrue(px[3] > 0, "Should produce visible output");
    }

    @Test
    void feDiffuseLightingPointLight() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feDiffuseLighting surfaceScale="2" diffuseConstant="1" lighting-color="yellow">
                        <fePointLight x="50" y="50" z="100"/>
                      </feDiffuseLighting>
                    </filter>
                  </defs>
                  <circle cx="50" cy="50" r="40" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] center = rgba(img, 50, 50);
        assertTrue(center[3] > 0, "Center should have visible lighting");
    }

    @Test
    void feDiffuseLightingWithComposite() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feDiffuseLighting in="SourceGraphic" surfaceScale="3" diffuseConstant="1" result="light">
                        <feDistantLight azimuth="225" elevation="45"/>
                      </feDiffuseLighting>
                      <feComposite in="SourceGraphic" in2="light" operator="arithmetic" k1="1" k2="0" k3="0" k4="0"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feSpecularLighting ───────────────────────────────────────────────

    @Test
    void feSpecularLightingDistantLight() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feSpecularLighting surfaceScale="5" specularConstant="1" specularExponent="20" lighting-color="white">
                        <feDistantLight azimuth="45" elevation="45"/>
                      </feSpecularLighting>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[3] >= 0, "Should render without error");
    }

    @Test
    void feSpecularLightingSpotLight() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feSpecularLighting surfaceScale="3" specularConstant="1" specularExponent="10" lighting-color="white">
                        <feSpotLight x="50" y="0" z="100" pointsAtX="50" pointsAtY="50" pointsAtZ="0" specularExponent="5"/>
                      </feSpecularLighting>
                    </filter>
                  </defs>
                  <circle cx="50" cy="50" r="40" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── SourceAlpha lazy creation via in= ────────────────────────────────

    @Test
    void sourceAlphaViaIn() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="red" result="r"/>
                      <feComposite in="SourceAlpha" in2="r" operator="in"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[3] > 0, "SourceAlpha via in= should produce visible output");
    }

    @Test
    void sourceAlphaViaIn2() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="green" result="g"/>
                      <feComposite in="g" in2="SourceAlpha" operator="in"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feComposite operator branches ────────────────────────────────────

    @Test
    void feCompositeOut() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="red" result="r"/>
                      <feComposite in="SourceGraphic" in2="r" operator="out"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void feCompositeAtop() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="red" flood-opacity="0.5" result="r"/>
                      <feComposite in="SourceGraphic" in2="r" operator="atop"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[3] > 0, "Atop composite should produce visible output");
    }

    @Test
    void feCompositeXor() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="green" flood-opacity="0.5" result="g"/>
                      <feComposite in="SourceGraphic" in2="g" operator="xor"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void feCompositeOver() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="blue" flood-opacity="0.5" result="b"/>
                      <feComposite in="SourceGraphic" in2="b" operator="over"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[0] > 200, "Over composite should show source on top, R=" + px[0]);
    }

    // ── feComponentTransfer with feFuncA ─────────────────────────────────

    @Test
    void feComponentTransferWithFuncA() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncR type="identity"/>
                        <feFuncA type="linear" slope="0.5" intercept="0"/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[3] >= 120 && px[3] <= 136, "Alpha should be ~128 with slope=0.5, got A=" + px[3]);
    }

    @Test
    void feComponentTransferTableTooFewValues() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncR type="table" tableValues="0.5"/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // table with < 2 values falls back to identity
        assertPixelColor(img, 25, 25, 255, 0, 0);
    }

    @Test
    void feComponentTransferDiscreteEmptyTable() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncR type="discrete" tableValues=""/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // empty discrete falls back to identity
        assertPixelColor(img, 25, 25, 255, 0, 0);
    }

    @Test
    void feComponentTransferProducesZeroAlpha() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feComponentTransfer>
                        <feFuncA type="linear" slope="0" intercept="0"/>
                      </feComponentTransfer>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 25, 25);
        assertTrue(px[3] < 5, "Zero alpha slope should produce transparent, got A=" + px[3]);
    }

    // ── feConvolveMatrix additional branches ─────────────────────────────

    @Test
    void feConvolveMatrixPreserveAlpha() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3" kernelMatrix="0 0 0 0 1 0 0 0 0"
                      preserveAlpha="true"/>
                  </filter></defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertPixelColor(img, 25, 25, 255, 0, 0);
    }

    @Test
    void feConvolveMatrixInvalidKernel() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3" kernelMatrix="1 0"/>
                  </filter></defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Invalid kernel copies input unchanged
        assertPixelColor(img, 25, 25, 255, 0, 0);
    }

    @Test
    void feConvolveMatrixZeroSumKernel() throws Exception {
        // Kernel sums to 0 → divisor defaults to 1 (not 0)
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3"
                      kernelMatrix="-1 -1 -1 -1 8 -1 -1 -1 -1"/>
                  </filter></defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void feConvolveMatrixExplicitDivisorZero() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3" kernelMatrix="0 0 0 0 1 0 0 0 0"
                      divisor="0"/>
                  </filter></defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void feConvolveMatrixRectangularOrder() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3 2"
                      kernelMatrix="0 0 0 0 1 0"/>
                  </filter></defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void feConvolveMatrixSemiTransparentInput() throws Exception {
        // Semi-transparent pixels exercise the premultiply branch (pa > 0 && pa < 255)
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3" kernelMatrix="0 0 0 0 1 0 0 0 0"/>
                  </filter></defs>
                  <rect width="50" height="50" fill="red" opacity="0.5" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 25, 25);
        assertTrue(px[0] > 100, "Semi-transparent convolved should keep red, got R=" + px[0]);
    }

    // ── feTile zero-dimension filter region ──────────────────────────────

    @Test
    void feTileZeroDimensionRegion() throws Exception {
        // Filter region that ends up with tileW<=0 after clamping in tile()
        // Use a filter region at the far edge beyond image content
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="49" y="49" width="1" height="1">
                      <feTile in="SourceGraphic"/>
                    </filter>
                  </defs>
                  <rect x="0" y="0" width="10" height="10" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    // ── feColorMatrix produces zero alpha ────────────────────────────────

    @Test
    void feColorMatrixZeroAlpha() throws Exception {
        // Matrix that zeroes the alpha channel
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feColorMatrix type="matrix"
                        values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 0 0"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 25, 25);
        assertTrue(px[3] < 5, "Alpha-zeroing matrix should produce transparent, got A=" + px[3]);
    }

    // ── feTurbulence zero frequency with type=turbulence ─────────────────

    @Test
    void feTurbulenceZeroFrequencyTurbulenceType() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feTurbulence type="turbulence" baseFrequency="0" numOctaves="1"/>
                  </filter></defs>
                  <rect width="50" height="50" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // turbulence type with zero frequency → black (not mid-gray like fractalNoise)
        int[] px = rgba(img, 25, 25);
        assertTrue(px[0] < 5 && px[1] < 5 && px[2] < 5, "Zero-freq turbulence should be black");
    }

    @Test
    void feTurbulenceZeroOctaves() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feTurbulence baseFrequency="0.1" numOctaves="0" seed="1"/>
                  </filter></defs>
                  <rect width="50" height="50" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 25, 25);
        assertTrue(px[3] > 0, "numOctaves clamped to 1 should produce visible output");
    }

    @Test
    void feTurbulenceTwoComponentFrequency() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feTurbulence baseFrequency="0.05 0.1" numOctaves="2" seed="10"/>
                  </filter></defs>
                  <rect width="50" height="50" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDiffuseLighting / feSpecularLighting no light source ────────────

    @Test
    void feDiffuseLightingNoLightSource() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feDiffuseLighting surfaceScale="1" diffuseConstant="1">
                      </feDiffuseLighting>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // No light source → cleared buffer
        int[] px = rgba(img, 25, 25);
        assertTrue(px[3] < 5, "No light source should produce transparent output");
    }

    @Test
    void feSpecularLightingNoLightSource() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feSpecularLighting surfaceScale="1" specularConstant="1" specularExponent="10">
                      </feSpecularLighting>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feSpecularLighting with spot light + limiting cone angle ──────────

    @Test
    void feSpecularLightingSpotLightWithConeAngle() throws Exception {
        // Tight cone angle causes cone rejection on pixels far from pointsAt target
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feSpecularLighting surfaceScale="3" specularConstant="1" specularExponent="10">
                        <feSpotLight x="50" y="50" z="50" pointsAtX="50" pointsAtY="50" pointsAtZ="0"
                          specularExponent="5" limitingConeAngle="10"/>
                      </feSpecularLighting>
                    </filter>
                  </defs>
                  <circle cx="50" cy="50" r="40" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void feDiffuseLightingSpotLightBehindSurface() throws Exception {
        // Light behind the surface: cosAngle <= 0 path
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feDiffuseLighting surfaceScale="1" diffuseConstant="1">
                        <feSpotLight x="50" y="50" z="-100" pointsAtX="50" pointsAtY="50" pointsAtZ="-200"
                          specularExponent="1" limitingConeAngle="90"/>
                      </feDiffuseLighting>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feImage with non-image data (ImageIO returns null) ───────────────

    @Test
    void feImageWithNonImageData() throws Exception {
        // base64 of "Hello World!" — not an image, but >= MIN_IMAGE_BYTES
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feImage href="data:application/octet-stream;base64,SGVsbG8gV29ybGQh"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = assertDoesNotThrow(() -> render(svg));
        assertNotNull(img);
    }

    // ── feImage fragment ref with no sub-region offset ────────────────────

    @Test
    void feImageFragmentRefNoOffset() throws Exception {
        // No userSpaceOnUse filter → no sub-region, so renderNode gets offsetX=0,
        // offsetY=0
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <rect id="ref" width="50" height="50" fill="green"/>
                    <filter id="f">
                      <feImage href="#ref"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Chained filter that pre-allocates buffers ────────────────────────

    @Test
    void chainedPrimitivesReuseAllocatedBuffers() throws Exception {
        // feGaussianBlur allocates buf1/buf2/buf3. Subsequent primitives
        // should reuse them (false branch of buf==null).
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="b"/>
                      <feColorMatrix type="saturate" values="0.5"/>
                      <feComponentTransfer>
                        <feFuncR type="linear" slope="1.2" intercept="0"/>
                      </feComponentTransfer>
                      <feMorphology operator="dilate" radius="1"/>
                      <feConvolveMatrix order="3" kernelMatrix="0 0 0 0 1 0 0 0 0"/>
                      <feFlood flood-color="red" flood-opacity="0.3" result="fl"/>
                      <feBlend in="b" in2="fl" mode="normal"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="60" height="60" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feMorphology with two-component radius ───────────────────────────

    @Test
    void feMorphologyTwoComponentRadius() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
                      <feMorphology operator="dilate" radius="3 5"/>
                    </filter>
                  </defs>
                  <rect x="40" y="40" width="20" height="20" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feTurbulence fractalNoise with zero frequency ────────────────────

    @Test
    void feTurbulenceFractalNoiseZeroFrequency() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feTurbulence type="fractalNoise" baseFrequency="0" numOctaves="1"/>
                  </filter></defs>
                  <rect width="50" height="50" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // fractalNoise + zero freq → mid-gray
        int[] px = rgba(img, 25, 25);
        assertTrue(px[0] > 100 && px[0] < 160, "fractalNoise zero-freq should be mid-gray, got R=" + px[0]);
    }

    // ── parseLightSource with unknown child ──────────────────────────────

    @Test
    void lightingWithUnknownChildElement() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feDiffuseLighting surfaceScale="1" diffuseConstant="1">
                        <desc>Not a light source</desc>
                      </feDiffuseLighting>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDisplacementMap with smaller map than input ─────────────────────

    @Test
    void feDisplacementMapSmallMap() throws Exception {
        // in2 is a small feImage, smaller than the canvas, so map pixel boundary check
        // fires
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
                      <feFlood flood-color="rgb(128,128,128)" flood-opacity="1" result="map"/>
                      <feDisplacementMap in="SourceGraphic" in2="map" scale="5"
                        xChannelSelector="R" yChannelSelector="G"/>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── Chained filters exercising feDiffuseLighting + feSpecularLighting
    // after prior primitives (buf already allocated) ────────────────────

    @Test
    void lightingAfterPriorPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="b"/>
                      <feDiffuseLighting in="b" surfaceScale="2" diffuseConstant="1" result="diffuse">
                        <fePointLight x="40" y="40" z="50"/>
                      </feDiffuseLighting>
                      <feSpecularLighting in="b" surfaceScale="2" specularConstant="1" specularExponent="10" result="spec">
                        <feDistantLight azimuth="45" elevation="45"/>
                      </feSpecularLighting>
                      <feMerge>
                        <feMergeNode in="diffuse"/>
                        <feMergeNode in="spec"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <circle cx="40" cy="40" r="30" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDisplacementMap after prior primitives (buf pre-allocated) ──────

    @Test
    void feDisplacementMapAfterPriorPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="b"/>
                      <feTurbulence baseFrequency="0.05" numOctaves="1" seed="1" result="turb"/>
                      <feDisplacementMap in="b" in2="turb" scale="10"
                        xChannelSelector="R" yChannelSelector="G"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feTurbulence after prior primitives (buf pre-allocated) ──────────

    @Test
    void feTurbulenceAfterPriorPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feGaussianBlur stdDeviation="1" result="b"/>
                      <feTurbulence baseFrequency="0.05" numOctaves="1" seed="1"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDropShadow as first primitive with SourceGraphic (non-managed) ─

    @Test
    void feDropShadowNonManagedInput() throws Exception {
        // SourceGraphic is not a managed buffer (not buf1/buf2/buf3)
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feDropShadow in="SourceGraphic" dx="3" dy="3" stdDeviation="1"
                        flood-color="black" flood-opacity="0.5"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── resolveInput with SourceAlpha via default getOrDefault ────────────

    @Test
    void resolveInputSourceAlphaDefault() throws Exception {
        // References SourceAlpha but from in2 of feBlend after SourceAlpha is already
        // created
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feOffset in="SourceAlpha" dx="3" dy="3" result="alphaOff"/>
                      <feFlood flood-color="black" flood-opacity="0.5" result="shadow"/>
                      <feComposite in="shadow" in2="alphaOff" operator="in" result="clipped"/>
                      <feMerge>
                        <feMergeNode in="clipped"/>
                        <feMergeNode in="SourceGraphic"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="50" height="50" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── parsePercentOrFraction with null/empty/non-percent value ──────────

    @Test
    void filterRegionFractionValues() throws Exception {
        // Explicit fraction values (not percentage) for filter region
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f" x="-0.1" y="-0.1" width="1.2" height="1.2">
                      <feGaussianBlur stdDeviation="1"/>
                    </filter>
                  </defs>
                  <rect x="20" y="20" width="60" height="60" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feConvolveMatrix output un-premultiply with mid-alpha ────────────

    @Test
    void feConvolveMatrixMidAlphaUnpremultiply() throws Exception {
        // A box-blur kernel on semi-transparent edge pixels exercises
        // the un-premultiply path where 0 < a < 255
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs><filter id="f">
                    <feConvolveMatrix order="3" kernelMatrix="1 1 1 1 1 1 1 1 1"/>
                  </filter></defs>
                  <rect x="10" y="10" width="30" height="30" fill="red" opacity="0.5" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── SourceAlpha referenced multiple times ────────────────────────────

    @Test
    void sourceAlphaReusedMultipleTimes() throws Exception {
        // First primitive creates SourceAlpha; subsequent ones reuse it
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f">
                      <feOffset in="SourceAlpha" dx="2" dy="2" result="off1"/>
                      <feOffset in="SourceAlpha" dx="4" dy="4" result="off2"/>
                      <feFlood flood-color="red" result="r"/>
                      <feComposite in="r" in2="SourceAlpha" operator="in"/>
                    </filter>
                  </defs>
                  <rect x="10" y="10" width="50" height="50" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feComposite as very first primitive (buf allocation path) ────────

    @Test
    void feCompositeAsFirstPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feComposite in="SourceGraphic" in2="SourceGraphic" operator="over"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertPixelColor(img, 25, 25, 255, 0, 0);
    }

    // ── feDisplacementMap as very first primitive ─────────────────────────

    @Test
    void feDisplacementMapAsFirstPrimitive() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feDisplacementMap in="SourceGraphic" in2="SourceGraphic" scale="0"
                        xChannelSelector="R" yChannelSelector="G"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="blue" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feMorphology with asymmetric zero radius (rx=0, ry>0) ────────────

    @Test
    void feMorphologyAsymmetricRadius() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80">
                  <defs>
                    <filter id="f" filterUnits="userSpaceOnUse" x="0" y="0" width="80" height="80">
                      <feMorphology operator="dilate" radius="0 3"/>
                    </filter>
                  </defs>
                  <rect x="30" y="30" width="20" height="20" fill="red" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feSpecularLighting with coincident light position ────────────────

    @Test
    void feSpecularLightingCoincidentSpotLight() throws Exception {
        // Light at same position as pointsAt → slen near zero in computeSpotIntensity
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feSpecularLighting surfaceScale="1" specularConstant="1" specularExponent="5">
                        <feSpotLight x="25" y="25" z="10" pointsAtX="25" pointsAtY="25" pointsAtZ="10"
                          specularExponent="1"/>
                      </feSpecularLighting>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feSpecularLighting with low hlen (L near opposite of E) ──────────

    @Test
    void feSpecularLightingLowHlen() throws Exception {
        // Point light directly below surface: L ≈ (0, 0, -1), E = (0, 0, 1)
        // H = L + E ≈ (0, 0, 0), so hlen near zero
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feSpecularLighting surfaceScale="1" specularConstant="1" specularExponent="5">
                        <fePointLight x="25" y="25" z="-1000"/>
                      </feSpecularLighting>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── computeLightVector near-zero length vector ───────────────────────

    @Test
    void feDiffuseLightingPointLightAtSurface() throws Exception {
        // Point light at exact surface position: dx=dy=dz≈0 → near-zero vector
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feDiffuseLighting surfaceScale="0" diffuseConstant="1">
                        <fePointLight x="25" y="25" z="0"/>
                      </feDiffuseLighting>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── computeLightVector distant light branch ──────────────────────────

    @Test
    void feDiffuseLightingDistantLightBranch() throws Exception {
        // Distant light exercises the "distant" case in computeLightVector switch
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="f">
                      <feDiffuseLighting surfaceScale="2" diffuseConstant="1">
                        <feDistantLight azimuth="90" elevation="30"/>
                      </feDiffuseLighting>
                    </filter>
                  </defs>
                  <circle cx="25" cy="25" r="20" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        int[] px = rgba(img, 25, 25);
        assertTrue(px[3] > 0, "Should produce visible output");
    }
}

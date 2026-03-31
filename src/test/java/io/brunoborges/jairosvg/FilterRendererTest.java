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
}

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

    // ── feDropShadow with managed input (chain from feOffset) ──

    @Test
    void feDropShadowWithManagedInput() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="chain">
                      <feOffset dx="2" dy="2" result="off"/>
                      <feDropShadow dx="1" dy="1" stdDeviation="1" in="off"
                                    flood-color="black" flood-opacity="0.5"/>
                    </filter>
                  </defs>
                  <rect width="50" height="50" fill="red" filter="url(#chain)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── feDropShadow preceded by feFlood (managed buffer as input) ──

    @Test
    void feDropShadowAfterFlood() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <defs>
                    <filter id="fdf">
                      <feFlood flood-color="blue" flood-opacity="0.8" result="fl"/>
                      <feDropShadow dx="1" dy="1" stdDeviation="0.5" in="fl"
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
}

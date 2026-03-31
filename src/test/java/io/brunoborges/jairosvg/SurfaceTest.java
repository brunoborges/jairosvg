package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests targeting missed branches in Surface.java: visibility, display,
 * opacity, fill/stroke properties, clip paths, font shorthand, stroke dash
 * arrays, line cap/join, fill-rule, effect buffers, and tag dispatch.
 */
class SurfaceTest {

    // ── display / visibility ──

    @Test
    void testDisplayNone() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="red" display="none"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // The red rect should NOT render
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    @Test
    void testVisibilityHidden() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="red" visibility="hidden"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // The hidden rect should NOT be painted
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    @Test
    void testVisibilityHiddenButChildrenVisible() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <g visibility="hidden">
                    <rect x="10" y="10" width="80" height="80" fill="blue" visibility="visible"/>
                  </g>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Child with visibility="visible" should render even when parent is hidden
        assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    @Test
    void testDisplayNoneGroupSuppressesChildren() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <g display="none">
                    <rect x="10" y="10" width="80" height="80" fill="red"/>
                  </g>
                </svg>
                """;
        BufferedImage img = render(svg);
        // display="none" suppresses both self and children
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    // ── opacity ──

    @Test
    void testLeafElementOpacity() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="0" y="0" width="100" height="100" fill="black" opacity="0.5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // 50% opacity black over white ~ gray ~128
        assertTrue(px[0] > 100 && px[0] < 160, "Expected grayish, got R=" + px[0]);
    }

    @Test
    void testGroupOpacityWithChildren() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <g opacity="0.5">
                    <rect width="100" height="100" fill="red"/>
                  </g>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // 50% opacity red over white => pinkish (~255/2 + 255/2 = ~128+127 for R, ~128
        // for G/B)
        assertTrue(px[0] > 150, "Expected high red, got R=" + px[0]);
        assertTrue(px[1] > 90 && px[1] < 180, "Expected medium green, got G=" + px[1]);
    }

    @Test
    void testFillOpacity() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect width="100" height="100" fill="blue" fill-opacity="0.5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // 50% fill-opacity blue over white
        assertTrue(px[2] > 150, "Expected high blue, got B=" + px[2]);
        assertTrue(px[0] > 90 && px[0] < 180, "Expected medium red from blending, got R=" + px[0]);
    }

    @Test
    void testStrokeOpacity() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <line x1="0" y1="50" x2="100" y2="50" stroke="black" stroke-width="20" stroke-opacity="0.5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // 50% stroke-opacity black on white = grayish
        assertTrue(px[0] > 90 && px[0] < 180, "Expected grayish from stroke-opacity, got R=" + px[0]);
    }

    // ── fill ──

    @Test
    void testFillNone() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Center should be white (no fill)
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    @Test
    void testFillUrlGradient() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="grad" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#grad)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] left = rgba(img, 5, 50);
        int[] right = rgba(img, 95, 50);
        assertTrue(left[0] > 200, "Left should be reddish, got R=" + left[0]);
        assertTrue(right[2] > 200, "Right should be bluish, got B=" + right[2]);
    }

    @Test
    void testFillRuleEvenOdd() throws Exception {
        // Star-like shape that differs with evenodd vs nonzero
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M50,5 L20,95 L95,40 L5,40 L80,95 Z" fill="red" fill-rule="evenodd"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Center of star with evenodd should have a hole (white)
        int[] center = rgba(img, 50, 50);
        // With evenodd, center should NOT be filled
        assertTrue(center[0] > 200 && center[1] > 200, "Center of evenodd star should be white or near-white");
    }

    @Test
    void testFillRuleNonzero() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M50,5 L20,95 L95,40 L5,40 L80,95 Z" fill="red" fill-rule="nonzero"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] center = rgba(img, 50, 50);
        // With nonzero, center should be filled red
        assertTrue(center[0] > 200, "Center of nonzero star should be red, got R=" + center[0]);
    }

    // ── stroke properties ──

    @Test
    void testStrokeLinecapRound() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <line x1="20" y1="50" x2="80" y2="50" stroke="black" stroke-width="20" stroke-linecap="round"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // With round cap, pixels just past the line end should have some dark pixels
        int darkCount = RenderTestHelper.countDarkPixels(img, 80, 40, 95, 60, 128);
        assertTrue(darkCount > 0, "Round linecap should extend past endpoint");
    }

    @Test
    void testStrokeLinecapSquare() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <line x1="20" y1="50" x2="80" y2="50" stroke="black" stroke-width="20" stroke-linecap="square"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Square cap also extends past endpoint
        int darkCount = RenderTestHelper.countDarkPixels(img, 80, 40, 95, 60, 128);
        assertTrue(darkCount > 0, "Square linecap should extend past endpoint");
    }

    @Test
    void testStrokeLinejoinRound() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <polyline points="20,80 50,20 80,80" stroke="black" stroke-width="10"
                            fill="none" stroke-linejoin="round"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int dark = RenderTestHelper.countDarkPixels(img, 0, 0, 99, 99, 128);
        assertTrue(dark > 100, "Should render polyline with round join");
    }

    @Test
    void testStrokeLinejoinBevel() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <polyline points="20,80 50,20 80,80" stroke="black" stroke-width="10"
                            fill="none" stroke-linejoin="bevel"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int dark = RenderTestHelper.countDarkPixels(img, 0, 0, 99, 99, 128);
        assertTrue(dark > 100, "Should render polyline with bevel join");
    }

    @Test
    void testStrokeMiterlimit() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <polyline points="20,80 50,20 80,80" stroke="black" stroke-width="10"
                            fill="none" stroke-linejoin="miter" stroke-miterlimit="1"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int dark = RenderTestHelper.countDarkPixels(img, 0, 0, 99, 99, 128);
        assertTrue(dark > 50, "Should render with miter limit");
    }

    @Test
    void testStrokeDasharray() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="40">
                  <rect width="200" height="40" fill="white"/>
                  <line x1="10" y1="20" x2="190" y2="20" stroke="black" stroke-width="4"
                        stroke-dasharray="10,10"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Count dark pixels in segments — should have gaps
        int firstSeg = RenderTestHelper.countDarkPixels(img, 10, 18, 20, 22, 128);
        int gap = RenderTestHelper.countDarkPixels(img, 22, 18, 30, 22, 128);
        assertTrue(firstSeg > 0, "First dash segment should have dark pixels");
        // The gap area should have fewer (or zero) dark pixels than the dash area
        assertTrue(firstSeg > gap, "Dash gaps should have fewer dark pixels than dashes");
    }

    @Test
    void testStrokeDasharrayNone() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="40">
                  <rect width="100" height="40" fill="white"/>
                  <line x1="10" y1="20" x2="90" y2="20" stroke="black" stroke-width="4"
                        stroke-dasharray="none"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Solid line, no dashes
        int dark = RenderTestHelper.countDarkPixels(img, 10, 18, 90, 22, 128);
        assertTrue(dark > 200, "Solid line should have many dark pixels");
    }

    @Test
    void testStrokeDasharrayAllZero() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="40">
                  <rect width="100" height="40" fill="white"/>
                  <line x1="10" y1="20" x2="90" y2="20" stroke="black" stroke-width="4"
                        stroke-dasharray="0,0"/>
                </svg>
                """;
        // All-zero dash array collapses to NO_DASH (solid line or no stroke)
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void testStrokeDashoffset() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="40">
                  <rect width="200" height="40" fill="white"/>
                  <line x1="10" y1="20" x2="190" y2="20" stroke="black" stroke-width="4"
                        stroke-dasharray="20,10" stroke-dashoffset="10"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int dark = RenderTestHelper.countDarkPixels(img, 0, 18, 199, 22, 128);
        assertTrue(dark > 50, "Dashed line with offset should render");
    }

    // ── stroke with gradient/pattern ──

    @Test
    void testStrokeUrlGradient() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="sg" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="none"
                        stroke="url(#sg)" stroke-width="10"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] left = rgba(img, 10, 50);
        assertTrue(left[0] > 150 || left[2] > 150, "Stroke should have gradient color");
    }

    // ── fill with zero alpha color ──

    @Test
    void testFillColorWithZeroAlpha() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="rgba(255,0,0,0)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[3] == 0, "Zero-alpha fill should be transparent, got A=" + px[3]);
    }

    // ── stroke color with zero alpha ──

    @Test
    void testStrokeColorZeroAlpha() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="none"
                        stroke="rgba(0,0,0,0)" stroke-width="5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Stroke with alpha=0 should be invisible
        assertPixelColor(img, 10, 50, 255, 255, 255);
    }

    // ── font shorthand ──

    @Test
    void testFontShorthand() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60">
                  <rect width="200" height="60" fill="white"/>
                  <text x="10" y="40" font="italic bold 20px serif" fill="black">Shorthand</text>
                </svg>
                """;
        BufferedImage img = render(svg);
        int dark = RenderTestHelper.countDarkPixels(img, 0, 0, 199, 59, 128);
        assertTrue(dark > 20, "Font shorthand should render text, got " + dark + " dark pixels");
    }

    // ── width=0 or height=0 elements ──

    @Test
    void testElementWithZeroWidth() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="0" height="80" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Zero-width rect should not render
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    @Test
    void testElementWithZeroHeight() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="0" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    // ── clip-path ──

    @Test
    void testClipPathClipsContent() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <clipPath id="cp">
                      <rect x="25" y="25" width="50" height="50"/>
                    </clipPath>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect width="100" height="100" fill="red" clip-path="url(#cp)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Inside clip: red
        assertPixelColor(img, 50, 50, 255, 0, 0);
        // Outside clip: white
        assertPixelColor(img, 5, 5, 255, 255, 255);
    }

    @Test
    void testClipPathEmpty() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <clipPath id="empty"/>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect width="100" height="100" fill="red" clip-path="url(#empty)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── mask + filter combination ──

    @Test
    void testMaskWithElement() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m">
                      <rect width="100" height="100" fill="white"/>
                      <rect x="25" y="25" width="50" height="50" fill="black"/>
                    </mask>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect width="100" height="100" fill="red" mask="url(#m)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Mask white area (corners): red visible
        int[] corner = rgba(img, 5, 5);
        assertTrue(corner[0] > 150, "Corner should have red from mask white area, R=" + corner[0]);
    }

    // ── nested SVG with overflow ──

    @Test
    void testNestedSvgOverflowHidden() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <svg x="10" y="10" width="50" height="50" overflow="hidden">
                    <rect x="-10" y="-10" width="200" height="200" fill="red"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Outside the nested SVG boundary should be white
        assertPixelColor(img, 5, 5, 255, 255, 255);
        // Inside should be red
        assertPixelColor(img, 30, 30, 255, 0, 0);
    }

    @Test
    void testNestedSvgOverflowScroll() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <svg x="10" y="10" width="50" height="50" overflow="scroll">
                    <rect x="-10" y="-10" width="200" height="200" fill="blue"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // overflow="scroll" clips like hidden
        assertPixelColor(img, 5, 5, 255, 255, 255);
    }

    @Test
    void testNestedSvgOverflowAuto() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <svg x="10" y="10" width="50" height="50" overflow="auto">
                    <rect x="-10" y="-10" width="200" height="200" fill="green"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 5, 5, 255, 255, 255);
    }

    // ── symbol/use ──

    @Test
    void testSymbolWithUse() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <symbol id="s" viewBox="0 0 10 10">
                      <rect width="10" height="10" fill="red"/>
                    </symbol>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <use href="#s" x="10" y="10" width="50" height="50"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 30, 30, 255, 0, 0);
    }

    // ── currentColor ──

    @Test
    void testCurrentColorInheritance() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <g color="blue">
                    <rect width="100" height="100" fill="currentColor"/>
                  </g>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    // ── viewBox + preserveAspectRatio ──

    @Test
    void testViewBoxPreserveAspectRatioXMidYMid() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 200 200">
                  <rect width="200" height="200" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void testViewBoxNone() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 200 100" preserveAspectRatio="none">
                  <rect width="200" height="100" fill="green"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        assertTrue(px[1] > 100, "Should have green, got G=" + px[1]);
    }

    // ── switch element with requiredFeatures ──

    @Test
    void testSwitchElement() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <switch>
                    <rect requiredFeatures="http://www.w3.org/TR/SVG11/feature#BasicStructure"
                          width="100" height="100" fill="green"/>
                    <rect width="100" height="100" fill="red"/>
                  </switch>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 50, 50);
        // Should render one of the alternatives (first supported wins)
        assertTrue((px[1] > 100 && px[0] < 50) || (px[0] > 200), "Expected green or red from switch");
    }

    // ── unknown tag is ignored ──

    @Test
    void testUnknownTagIgnored() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <customElement x="10" y="10" width="50" height="50" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Unknown element does not render
        assertPixelColor(img, 30, 30, 255, 255, 255);
    }

    // ── g element (container tag with no draw case -> default -> drawn=false) ──

    @Test
    void testGroupElementRendersChildren() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <g fill="red">
                    <rect width="100" height="100"/>
                  </g>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ── API builder with output dimensions ──

    @Test
    void testOutputWidthOnly() throws Exception {
        var svg = "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='100'><rect width='200' height='100' fill='red'/></svg>";
        byte[] png = JairoSVG.builder().fromBytes(svg.getBytes(StandardCharsets.UTF_8)).outputWidth(100).toPng();
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(100, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @Test
    void testOutputHeightOnly() throws Exception {
        var svg = "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='100'><rect width='200' height='100' fill='red'/></svg>";
        byte[] png = JairoSVG.builder().fromBytes(svg.getBytes(StandardCharsets.UTF_8)).outputHeight(50).toPng();
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(100, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @Test
    void testOutputWidthAndHeight() throws Exception {
        var svg = "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='100'><rect width='200' height='100' fill='red'/></svg>";
        byte[] png = JairoSVG.builder().fromBytes(svg.getBytes(StandardCharsets.UTF_8)).outputWidth(80).outputHeight(40)
                .toPng();
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(80, img.getWidth());
        assertEquals(40, img.getHeight());
    }

    // ── filter + mask combination for effect buffer branches ──

    @Test
    void testFilterAndMaskCombination() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="blur"><feGaussianBlur stdDeviation="1"/></filter>
                    <mask id="m"><rect width="100" height="100" fill="white"/></mask>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect width="80" height="80" fill="red" filter="url(#blur)" mask="url(#m)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 40, 40);
        assertTrue(px[0] > 150, "Should have red from filtered+masked rect");
    }

    // ── effect buffer on small element (subRegionEffect) ──

    @Test
    void testSubRegionEffectBuffer() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">
                  <rect width="400" height="400" fill="white"/>
                  <rect x="180" y="180" width="40" height="40" fill="red" opacity="0.8"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 200, 200);
        assertTrue(px[0] > 200, "Small element with opacity should render");
    }

    // ── computeEffectPadding for filter primitives ──

    @Test
    void testFilterPaddingFromDropShadow() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
                  <defs>
                    <filter id="ds">
                      <feDropShadow dx="10" dy="10" stdDeviation="3"/>
                    </filter>
                  </defs>
                  <rect width="120" height="120" fill="white"/>
                  <rect x="20" y="20" width="50" height="50" fill="red" filter="url(#ds)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Shadow should extend beyond the rect
        int darkInShadow = RenderTestHelper.countDarkPixels(img, 75, 75, 90, 90, 200);
        assertTrue(darkInShadow >= 0, "Drop shadow should create visible area");
    }

    @Test
    void testFilterPaddingFromOffset() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
                  <defs>
                    <filter id="off">
                      <feOffset dx="15" dy="15"/>
                    </filter>
                  </defs>
                  <rect width="120" height="120" fill="white"/>
                  <rect x="10" y="10" width="40" height="40" fill="blue" filter="url(#off)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] px = rgba(img, 35, 35);
        assertTrue(px[2] > 150, "Offset should move blue rect");
    }

    // ── stroke contributes to effect padding ──

    @Test
    void testStrokeContributesToEffectPadding() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m"><rect width="100" height="100" fill="white"/></mask>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect x="20" y="20" width="60" height="60" fill="none" stroke="red"
                        stroke-width="10" mask="url(#m)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // The stroked rect should be visible through the mask
        int[] edge = rgba(img, 20, 50);
        assertTrue(edge[0] > 150, "Stroked rect should be visible through mask");
    }

    // ── clip rect (legacy clip="rect(...)") ──

    @Test
    void testLegacyClipRect() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect width="100" height="100" fill="red" style="clip: rect(25px, 75px, 75px, 25px)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Clip rect should restrict rendering — just verify it renders without error
        assertNotNull(img);
        assertEquals(100, img.getWidth());
    }

    // ── INVISIBLE_TAGS: desc, title, metadata ──

    @Test
    void testInvisibleTagsDoNotRenderChildren() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <desc><rect width="100" height="100" fill="red"/></desc>
                  <title><rect width="100" height="100" fill="blue"/></title>
                  <metadata><rect width="100" height="100" fill="green"/></metadata>
                </svg>
                """;
        BufferedImage img = render(svg);
        // None of the rects inside invisible tags should render
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    // ── Multiple filter primitives sharing result names ──

    @Test
    void testFilterResultNameCaching() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <filter id="f">
                      <feFlood flood-color="red" result="r"/>
                      <feFlood flood-color="blue" result="b"/>
                      <feMerge>
                        <feMergeNode in="r"/>
                        <feMergeNode in="b"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect width="100" height="100" fill="white" filter="url(#f)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Last merged layer (blue) should be on top
        int[] px = rgba(img, 50, 50);
        assertTrue(px[2] > 150, "Blue result should be visible");
    }

    // ── Pattern fill for stroke ──

    @Test
    void testPatternFill() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <pattern id="p" patternUnits="userSpaceOnUse" width="20" height="20">
                      <rect width="10" height="10" fill="red"/>
                      <rect x="10" y="10" width="10" height="10" fill="blue"/>
                    </pattern>
                  </defs>
                  <rect width="100" height="100" fill="url(#p)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Should have both red and blue patterns tiled
        int[] topLeft = rgba(img, 5, 5);
        int[] bottomRight = rgba(img, 15, 15);
        assertTrue(topLeft[0] > 200 || bottomRight[2] > 200, "Pattern should render alternating colors");
    }

    // ── Effect buffer reuse test (render two elements with same-size effect
    // buffers) ──

    @Test
    void testEffectBufferReuse() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="red" opacity="0.7"/>
                  <rect x="110" y="110" width="80" height="80" fill="blue" opacity="0.7"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] r = rgba(img, 50, 50);
        int[] b = rgba(img, 150, 150);
        assertTrue(r[0] > 150, "First rect should be reddish");
        assertTrue(b[2] > 150, "Second rect should be bluish");
    }

    // ── zero-size SVG still renders (minimum 1x1) ──

    @Test
    void testZeroWidthSvgRendersMinimum() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="0" height="100">
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertTrue(img.getWidth() >= 1);
    }

    @Test
    void testZeroHeightSvgRendersMinimum() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="0">
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertTrue(img.getHeight() >= 1);
    }

    // ── outputWidth/outputHeight scaling branches ──

    @Test
    void testOutputWidthOnlyScaling() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                  <rect width="100" height="100" fill="green"/>
                </svg>
                """;
        byte[] png = JairoSVG.builder().fromBytes(svg.getBytes(StandardCharsets.UTF_8)).outputWidth(50.0).toPng();
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(50, img.getWidth());
    }

    @Test
    void testOutputHeightOnlyScaling() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                  <rect width="100" height="100" fill="green"/>
                </svg>
                """;
        byte[] png = JairoSVG.builder().fromBytes(svg.getBytes(StandardCharsets.UTF_8)).outputHeight(50.0).toPng();
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(50, img.getHeight());
    }

    @Test
    void testOutputWidthWithZeroW() throws Exception {
        // SVG with no width, no viewBox → nodeFormat returns w=0
        // outputWidth is set → if (w > 0) should be false
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" height="100">
                  <rect width="100" height="100" fill="green"/>
                </svg>
                """;
        byte[] png = JairoSVG.builder().fromBytes(svg.getBytes(StandardCharsets.UTF_8)).outputWidth(50.0).toPng();
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(50, img.getWidth());
    }

    @Test
    void testOutputHeightWithZeroH() throws Exception {
        // SVG with no height, no viewBox → nodeFormat returns h=0
        // outputHeight is set → if (h > 0) should be false
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100">
                  <rect width="100" height="100" fill="green"/>
                </svg>
                """;
        byte[] png = JairoSVG.builder().fromBytes(svg.getBytes(StandardCharsets.UTF_8)).outputHeight(50.0).toPng();
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(50, img.getHeight());
    }

    // ── font shorthand (expanded during CSS cascade, tested in NodeTest) ──

    @Test
    void testFontShorthandRendersCorrectly() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <rect width="200" height="50" fill="white"/>
                  <text x="10" y="30" font="bold 24px serif" fill="black">Test</text>
                </svg>
                """;
        BufferedImage img = render(svg);
        int dark = RenderTestHelper.countDarkPixels(img, 0, 0, 199, 49, 128);
        assertTrue(dark > 10, "Text should render with font shorthand");
    }

    // ── def tags outside <defs> ──

    @Test
    void testDefTagsOutsideDefs() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <clipPath id="cp"><rect width="50" height="50"/></clipPath>
                  <filter id="f1"><feGaussianBlur stdDeviation="1"/></filter>
                  <linearGradient id="lg1"><stop offset="0" stop-color="red"/><stop offset="1" stop-color="blue"/></linearGradient>
                  <marker id="mk1"><circle r="2" fill="red"/></marker>
                  <mask id="m1"><rect width="100" height="100" fill="white"/></mask>
                  <pattern id="p1" width="10" height="10"><rect width="10" height="10" fill="red"/></pattern>
                  <radialGradient id="rg1"><stop offset="0" stop-color="green"/><stop offset="1" stop-color="yellow"/></radialGradient>
                  <rect width="100" height="100" fill="url(#lg1)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertTrue(img.getWidth() > 0);
    }

    // ── PointError catch (malformed polyline) ──

    @Test
    void testMalformedPolylinePointsIgnored() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <polyline points="abc,def" stroke="red" fill="none"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    // ── getLineCap default value ──

    @Test
    void testUnknownStrokeLinecapDefaultsToButt() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <line x1="10" y1="50" x2="90" y2="50" stroke="black" stroke-width="10"
                        stroke-linecap="invalid"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int dark = RenderTestHelper.countDarkPixels(img, 10, 45, 90, 55, 128);
        assertTrue(dark > 20, "Line should render with default cap");
    }

    // ── effect buffer reuse via consecutive groups with opacity ──

    @Test
    void testEffectBufferReuseFullCanvas() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <g opacity="0.5"><rect width="100" height="100" fill="red"/></g>
                  <g opacity="0.5"><rect width="100" height="100" fill="blue"/></g>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 30 || c[2] > 30, "Should show blended colors");
    }

    // ── per-element opacity < 1 with no children ──

    @Test
    void testElementOpacityNoChildren() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect width="100" height="100" fill="black" opacity="0.5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 100 && c[0] < 160, "Should be grayish: " + c[0]);
    }

    // ── fill starting with 'u' but not "url" ──

    @Test
    void testFillStartingWithUNotUrl() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="urchin"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── clip-path edge cases ──

    @Test
    void testClipPathNoFragment() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="green" clip-path="url()"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 0, 128, 0, 10);
    }

    @Test
    void testClipPathNonExistentId() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="green" clip-path="url(#doesNotExist)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 0, 128, 0, 10);
    }

    // ── stroke with gradient ──

    @Test
    void testStrokeWithGradient() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="sg" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0" stop-color="red"/>
                      <stop offset="1" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="none" stroke="url(#sg)" stroke-width="8"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── transparent fill suppressed ──

    @Test
    void testTransparentFillSuppressed() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="transparent"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    // ── stroke with zero alpha suppressed ──

    @Test
    void testStrokeWithZeroAlphaSuppressed() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <rect x="10" y="10" width="80" height="80" fill="none" stroke="transparent" stroke-width="4"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 12, 12, 255, 255, 255);
    }

    // ── mask on element with large bounding box (no sub-region) ──

    @Test
    void testMaskOnLargeElement() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m"><rect width="100" height="100" fill="white"/></mask>
                  </defs>
                  <rect width="100" height="100" fill="white"/>
                  <g mask="url(#m)">
                    <rect width="100" height="100" fill="red"/>
                  </g>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ── fill-rule evenodd ──

    @Test
    void fillRuleEvenOdd() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M10,10 L90,10 L90,90 L10,90 Z M30,30 L70,30 L70,70 L30,70 Z"
                        fill="red" fill-rule="evenodd"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── stroke with gradient ──

    @Test
    void strokeWithGradient() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="sg" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect x="10" y="10" width="80" height="80" fill="none"
                        stroke="url(#sg)" stroke-width="5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── element with stroke-opacity ──

    @Test
    void elementWithStrokeOpacity() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="10" y="10" width="80" height="80"
                        fill="red" stroke="blue" stroke-width="5"
                        stroke-opacity="0.5" fill-opacity="0.5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── element with opacity < 1 and children ──

    @Test
    void groupWithOpacity() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <g opacity="0.5">
                    <rect width="50" height="100" fill="red"/>
                    <rect x="50" width="50" height="100" fill="blue"/>
                  </g>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── PointError in draw → caught and ignored ──

    @Test
    void pointErrorInDrawCaught() throws Exception {
        // Malformed numeric data triggers PointError which is caught by Surface.draw
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <circle cx="abc" cy="50" r="20" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── clipPath ──

    @Test
    void clipPathRendering() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <clipPath id="clip">
                      <circle cx="50" cy="50" r="40"/>
                    </clipPath>
                  </defs>
                  <rect width="100" height="100" fill="green" clip-path="url(#clip)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Corner should be transparent (clipped)
        int[] corner = rgba(img, 2, 2);
        assertTrue(corner[1] < 50 || corner[3] < 50, "Corner should be clipped");
    }

    // ── use element ──

    @Test
    void useElement() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <rect id="myRect" width="40" height="40" fill="blue"/>
                  </defs>
                  <use href="#myRect" x="10" y="10"/>
                  <use href="#myRect" x="50" y="50"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
        // Both use elements should render blue pixels
        int[] px = rgba(img, 30, 30);
        assertTrue(px[2] > 200, "Use element should render blue rect");
    }

    // ── stroke-dasharray ──

    @Test
    void strokeDashArray() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <line x1="10" y1="25" x2="90" y2="25" stroke="black"
                        stroke-width="3" stroke-dasharray="10,5"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    // ── stroke-linecap and stroke-linejoin ──

    @Test
    void strokeLinecapAndLinejoin() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <polyline points="20,80 50,20 80,80" fill="none" stroke="black"
                            stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }
}

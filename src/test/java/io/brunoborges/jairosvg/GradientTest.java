package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class GradientTest {

    @Test
    void testLinearGradientBasic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <defs>
                    <linearGradient id="lg1" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="50" fill="url(#lg1)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Left side should be red-ish
        int[] left = rgba(img, 5, 25);
        assertTrue(left[0] > 200, "Left should have high red, got " + left[0]);
        assertTrue(left[2] < 50, "Left should have low blue, got " + left[2]);

        // Right side should be blue-ish
        int[] right = rgba(img, 95, 25);
        assertTrue(right[2] > 200, "Right should have high blue, got " + right[2]);
        assertTrue(right[0] < 50, "Right should have low red, got " + right[0]);

        // Middle should be a mix (purple-ish)
        int[] mid = rgba(img, 50, 25);
        assertTrue(mid[0] > 50 && mid[2] > 50, "Middle should have both red and blue components");
    }

    @Test
    void testRadialGradientBasic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <radialGradient id="rg1" cx="50%" cy="50%" r="50%">
                      <stop offset="0%" stop-color="white"/>
                      <stop offset="100%" stop-color="black"/>
                    </radialGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#rg1)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Center should be lighter than corners
        int[] center = rgba(img, 50, 50);
        int[] corner = rgba(img, 5, 5);
        int centerBrightness = center[0] + center[1] + center[2];
        int cornerBrightness = corner[0] + corner[1] + corner[2];
        assertTrue(centerBrightness > cornerBrightness,
                "Center (%d) should be brighter than corner (%d)".formatted(centerBrightness, cornerBrightness));
    }

    @Test
    void testGradientHrefInheritance() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <defs>
                    <linearGradient id="base">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                    <linearGradient id="derived" href="#base" x1="0" y1="0" x2="1" y2="0"/>
                  </defs>
                  <rect width="100" height="50" fill="url(#derived)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Should inherit stops from base: left red, right blue
        int[] left = rgba(img, 5, 25);
        assertTrue(left[0] > 200, "Left should be red-ish, got R=" + left[0]);

        int[] right = rgba(img, 95, 25);
        assertTrue(right[2] > 200, "Right should be blue-ish, got B=" + right[2]);
    }

    @Test
    void testUserSpaceOnUse() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="usu" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="100" y2="0">
                      <stop offset="0%" stop-color="yellow"/>
                      <stop offset="100%" stop-color="green"/>
                    </linearGradient>
                  </defs>
                  <rect x="20" y="20" width="60" height="60" fill="url(#usu)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Left side of rect should be yellow-ish (high R, high G)
        int[] left = rgba(img, 25, 50);
        assertTrue(left[0] > 150 && left[1] > 150,
                "Left of rect should be yellow-ish, got rgb(%d,%d,%d)".formatted(left[0], left[1], left[2]));

        // Right side of rect should be green-ish (low R, high G)
        int[] right = rgba(img, 75, 50);
        assertTrue(right[1] > 100,
                "Right of rect should be green-ish, got rgb(%d,%d,%d)".formatted(right[0], right[1], right[2]));
    }

    @Test
    void testSpreadMethodReflect() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <defs>
                    <linearGradient id="ref" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="50" y2="0" spreadMethod="reflect">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="200" height="50" fill="url(#ref)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);

        // Should have both red and blue regions due to reflect
        boolean hasRed = false, hasBlue = false;
        for (int x = 0; x < 200; x += 5) {
            int[] c = rgba(img, x, 25);
            if (c[0] > 200 && c[2] < 80)
                hasRed = true;
            if (c[2] > 200 && c[0] < 80)
                hasBlue = true;
        }
        assertTrue(hasRed, "Reflect gradient should have red regions");
        assertTrue(hasBlue, "Reflect gradient should have blue regions");
    }

    @Test
    void testSpreadMethodRepeat() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <defs>
                    <linearGradient id="rep" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="50" y2="0" spreadMethod="repeat">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="200" height="50" fill="url(#rep)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);

        boolean hasRed = false, hasBlue = false;
        for (int x = 0; x < 200; x += 5) {
            int[] c = rgba(img, x, 25);
            if (c[0] > 200 && c[2] < 80)
                hasRed = true;
            if (c[2] > 200 && c[0] < 80)
                hasBlue = true;
        }
        assertTrue(hasRed, "Repeat gradient should have red regions");
        assertTrue(hasBlue, "Repeat gradient should have blue regions");
    }

    @Test
    void testRadialGradientWithFocusPoint() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <radialGradient id="focus" cx="50%" cy="50%" r="50%" fx="20%" fy="20%">
                      <stop offset="0%" stop-color="white"/>
                      <stop offset="100%" stop-color="red"/>
                    </radialGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#focus)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Near focus point (20,20) should be lighter than far corner (80,80)
        int[] nearFocus = rgba(img, 20, 20);
        int[] farCorner = rgba(img, 80, 80);
        int focusBrightness = nearFocus[0] + nearFocus[1] + nearFocus[2];
        int cornerBrightness = farCorner[0] + farCorner[1] + farCorner[2];
        assertTrue(focusBrightness > cornerBrightness, "Pixel near focus (%d) should be brighter than far corner (%d)"
                .formatted(focusBrightness, cornerBrightness));
    }

    @Test
    void testDegenerateRadialGradient() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <radialGradient id="bad" r="0">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </radialGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#bad)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Degenerate gradient (r=0) should render without error");
        assertEquals(100, img.getWidth());
    }

    @Test
    void testSingleStopGradient() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <defs>
                    <linearGradient id="one">
                      <stop offset="50%" stop-color="red"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="50" fill="url(#one)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Entire rect should be red (single stop duplicated)
        assertPixelColor(img, 10, 25, 255, 0, 0, 5);
        assertPixelColor(img, 90, 25, 255, 0, 0, 5);
    }

    @Test
    void testNoStopsGradient() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <defs>
                    <linearGradient id="empty"/>
                  </defs>
                  <rect width="100" height="50" fill="url(#empty)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Gradient with no stops should render without error");
        assertEquals(100, img.getWidth());
    }

    @Test
    void testGradientTransform() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="rotated" x1="0" y1="0" x2="1" y2="0" gradientTransform="rotate(90 0.5 0.5)">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#rotated)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // With 90° rotation in objectBoundingBox space, the gradient direction changes.
        // The top-left and bottom-right corners should differ in color.
        int[] topLeft = rgba(img, 5, 5);
        int[] bottomRight = rgba(img, 95, 95);
        // They should not be the same color — the gradient is rotated
        boolean colorsDiffer = Math.abs(topLeft[0] - bottomRight[0]) > 20 || Math.abs(topLeft[2] - bottomRight[2]) > 20;
        assertTrue(colorsDiffer,
                "Rotated gradient should produce different colors at opposite corners, got tl=rgb(%d,%d,%d) br=rgb(%d,%d,%d)"
                        .formatted(topLeft[0], topLeft[1], topLeft[2], bottomRight[0], bottomRight[1], bottomRight[2]));
    }

    @Test
    void testStopOpacity() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <defs>
                    <linearGradient id="opacity" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="red" stop-opacity="1"/>
                      <stop offset="100%" stop-color="red" stop-opacity="0"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="50" fill="url(#opacity)"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Left side should be opaque red
        int[] left = rgba(img, 5, 25);
        assertTrue(left[0] > 200, "Left should be red, got R=" + left[0]);
        assertTrue(left[3] > 200, "Left should be opaque, got A=" + left[3]);

        // Right side should have low alpha
        int[] right = rgba(img, 95, 25);
        assertTrue(right[3] < 50, "Right should be nearly transparent, got A=" + right[3]);
    }

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testGradientWithDuplicateStopOffsets() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <defs>
                    <linearGradient id="dup" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="0%" stop-color="green"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="50" fill="url(#dup)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Gradient with duplicate offsets should render without error");
    }

    @Test
    void testDegenerateLinearGradientSameEndpoints() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="degen" x1="0.5" y1="0.5" x2="0.5" y2="0.5">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#degen)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Degenerate linear gradient (same endpoints) should render");
    }

    @Test
    void testMissingGradientReference() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <rect width="100" height="50" fill="url(#nonexistent_gradient)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Missing gradient reference should not crash");
    }

    @Test
    void testRadialGradientUserSpaceOnUse() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <radialGradient id="rgusu" gradientUnits="userSpaceOnUse" cx="50" cy="50" r="50">
                      <stop offset="0%" stop-color="white"/>
                      <stop offset="100%" stop-color="black"/>
                    </radialGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#rgusu)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] center = rgba(img, 50, 50);
        int centerBrightness = center[0] + center[1] + center[2];
        assertTrue(centerBrightness > 300, "Center should be bright (white), got " + centerBrightness);
    }

    @Test
    void testRadialGradientFocusOutsideRadius() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <radialGradient id="rfar" cx="50%" cy="50%" r="10%" fx="0%" fy="0%">
                      <stop offset="0%" stop-color="white"/>
                      <stop offset="100%" stop-color="black"/>
                    </radialGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#rfar)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Radial gradient with focus outside radius should render");
    }

    @Test
    void testGradientHrefInheritMissingAttributes() throws Exception {
        // Child gradient has no x1/y1/x2/y2, inherits from base
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <defs>
                    <linearGradient id="parent" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                    <linearGradient id="child" href="#parent"/>
                  </defs>
                  <rect width="100" height="50" fill="url(#child)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] left = rgba(img, 5, 25);
        assertTrue(left[0] > 150, "Should inherit stops and attributes from parent");
    }

    @Test
    void testGradientWithNonStopChildren() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <defs>
                    <linearGradient id="mixed" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="red"/>
                      <desc>This is a description</desc>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="50" fill="url(#mixed)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] left = rgba(img, 5, 25);
        assertTrue(left[0] > 200, "Should skip non-stop children");
    }

    @Test
    void testLinearGradientUserSpaceOnUse() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="lusu" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="100">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#lusu)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] top = rgba(img, 50, 5);
        int[] bottom = rgba(img, 50, 95);
        assertTrue(top[0] > 200, "Top should be red");
        assertTrue(bottom[2] > 200, "Bottom should be blue");
    }

    @Test
    void testGradientOnZeroSizeElement() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="g">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect x="50" y="50" width="0" height="0" fill="url(#g)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Gradient on zero-size element should not crash");
    }
}

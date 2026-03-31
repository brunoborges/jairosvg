package io.brunoborges.jairosvg.draw;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MaskPainter} — verifies luminance mask rendering.
 */
class MaskPainterTest {

    @Test
    void testMaskWithWhiteFill() throws Exception {
        // White mask = fully opaque → source shows through
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m1">
                      <rect width="100" height="100" fill="white"/>
                    </mask>
                  </defs>
                  <rect width="100" height="100" fill="red" mask="url(#m1)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 200, "White mask should pass through red, R=" + c[0]);
        assertTrue(c[3] > 200, "White mask should keep full alpha, A=" + c[3]);
    }

    @Test
    void testMaskWithBlackFill() throws Exception {
        // Black mask = zero luminance → source is erased
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m2">
                      <rect width="100" height="100" fill="black"/>
                    </mask>
                  </defs>
                  <rect width="100" height="100" fill="red" mask="url(#m2)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[3] < 10, "Black mask should erase content, A=" + c[3]);
    }

    @Test
    void testMaskMissingReference() throws Exception {
        // Missing mask → source unchanged
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="red" mask="url(#nonexistent)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[0] > 200, "Missing mask should render source unchanged, R=" + c[0]);
    }

    @Test
    void testMaskWithTransparentContent() throws Exception {
        // Mask with fully transparent content → erase source
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m3">
                      <rect width="100" height="100" fill="white" fill-opacity="0"/>
                    </mask>
                  </defs>
                  <rect width="100" height="100" fill="blue" mask="url(#m3)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        assertTrue(c[3] < 10, "Transparent mask should erase content, A=" + c[3]);
    }

    @Test
    void testMaskWithGrayFill() throws Exception {
        // 50% gray mask → half luminance → half alpha
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m4">
                      <rect width="100" height="100" fill="gray"/>
                    </mask>
                  </defs>
                  <rect width="100" height="100" fill="red" mask="url(#m4)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 50);
        // Alpha should be roughly half (gray luminance ≈ 0.5)
        assertTrue(c[3] > 30 && c[3] < 200, "Gray mask should produce partial alpha, A=" + c[3]);
    }

    @Test
    void testMaskWithPartialCoverage() throws Exception {
        // Mask covers only left half
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m5">
                      <rect width="50" height="100" fill="white"/>
                    </mask>
                  </defs>
                  <rect width="100" height="100" fill="red" mask="url(#m5)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] left = rgba(img, 25, 50);
        int[] right = rgba(img, 75, 50);
        assertTrue(left[0] > 200, "Left should be red (masked with white)");
        assertTrue(left[3] > 200, "Left should be opaque");
        assertTrue(right[3] < 10, "Right should be transparent (outside mask)");
    }

    @Test
    void testMaskAppliedTwice() throws Exception {
        // Two elements using the same mask — tests mask buffer reuse path
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <mask id="m6">
                      <rect width="100" height="100" fill="white"/>
                    </mask>
                  </defs>
                  <rect x="0" y="0" width="50" height="100" fill="red" mask="url(#m6)"/>
                  <rect x="50" y="0" width="50" height="100" fill="blue" mask="url(#m6)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] left = rgba(img, 25, 50);
        int[] right = rgba(img, 75, 50);
        assertTrue(left[0] > 200, "Left rect should be red");
        assertTrue(right[2] > 200, "Right rect should be blue");
    }
}

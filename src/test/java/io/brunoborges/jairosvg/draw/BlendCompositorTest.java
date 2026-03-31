package io.brunoborges.jairosvg.draw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BlendCompositor} — verifies all 5 SVG feBlend modes.
 */
class BlendCompositorTest {

    private static BufferedImage solidImage(int w, int h, int argb) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }

    private static int alpha(int pixel) {
        return (pixel >> 24) & 0xFF;
    }

    private static int red(int pixel) {
        return (pixel >> 16) & 0xFF;
    }

    private static int green(int pixel) {
        return (pixel >> 8) & 0xFF;
    }

    private static int blue(int pixel) {
        return pixel & 0xFF;
    }

    @Test
    void normalBlendOpaqueOverOpaque() {
        // Opaque red over opaque blue → should yield red
        BufferedImage src = solidImage(2, 2, 0xFFFF0000);
        BufferedImage dst = solidImage(2, 2, 0xFF0000FF);
        BufferedImage result = BlendCompositor.blend(src, dst, "normal", null);
        assertNotNull(result);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, alpha(pixel));
        assertEquals(255, red(pixel));
        assertEquals(0, green(pixel));
        assertEquals(0, blue(pixel));
    }

    @Test
    void normalBlendSemiTransparentOverOpaque() {
        // 50% transparent red over opaque blue
        BufferedImage src = solidImage(2, 2, 0x80FF0000);
        BufferedImage dst = solidImage(2, 2, 0xFF0000FF);
        BufferedImage result = BlendCompositor.blend(src, dst, "normal", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, alpha(pixel));
        // Red should be roughly half (128), blue roughly half (127)
        assertTrue(red(pixel) > 100 && red(pixel) < 160, "Red should be ~128, was " + red(pixel));
        assertTrue(blue(pixel) > 90 && blue(pixel) < 160, "Blue should be ~127, was " + blue(pixel));
    }

    @Test
    void normalBlendTransparentSrc() {
        // Fully transparent over opaque green → should yield green
        BufferedImage src = solidImage(2, 2, 0x00000000);
        BufferedImage dst = solidImage(2, 2, 0xFF00FF00);
        BufferedImage result = BlendCompositor.blend(src, dst, "normal", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, alpha(pixel));
        assertEquals(0, red(pixel));
        assertEquals(255, green(pixel));
        assertEquals(0, blue(pixel));
    }

    @Test
    void normalBlendTransparentDst() {
        // Opaque red over transparent → should yield red
        BufferedImage src = solidImage(2, 2, 0xFFFF0000);
        BufferedImage dst = solidImage(2, 2, 0x00000000);
        BufferedImage result = BlendCompositor.blend(src, dst, "normal", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, alpha(pixel));
        assertEquals(255, red(pixel));
    }

    @Test
    void multiplyBlend() {
        // multiply(white, color) = color; multiply(black, anything) = black
        BufferedImage white = solidImage(2, 2, 0xFFFFFFFF);
        BufferedImage color = solidImage(2, 2, 0xFF804020);
        BufferedImage result = BlendCompositor.blend(white, color, "multiply", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, alpha(pixel));
        // multiply(1.0, 0.5) = 0.5 → ~128
        assertTrue(Math.abs(red(pixel) - 0x80) <= 1, "Red should be ~128, was " + red(pixel));
        assertTrue(Math.abs(green(pixel) - 0x40) <= 1, "Green should be ~64, was " + green(pixel));
        assertTrue(Math.abs(blue(pixel) - 0x20) <= 1, "Blue should be ~32, was " + blue(pixel));
    }

    @Test
    void multiplyBlendBlackYieldsBlack() {
        BufferedImage black = solidImage(2, 2, 0xFF000000);
        BufferedImage color = solidImage(2, 2, 0xFFFF8040);
        BufferedImage result = BlendCompositor.blend(black, color, "multiply", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(0, red(pixel));
        assertEquals(0, green(pixel));
        assertEquals(0, blue(pixel));
    }

    @Test
    void screenBlend() {
        // screen(A, B) = A + B - A*B → screen(black, color) = color
        BufferedImage black = solidImage(2, 2, 0xFF000000);
        BufferedImage color = solidImage(2, 2, 0xFF804020);
        BufferedImage result = BlendCompositor.blend(black, color, "screen", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, alpha(pixel));
        assertTrue(Math.abs(red(pixel) - 0x80) <= 1, "Red should be ~128, was " + red(pixel));
        assertTrue(Math.abs(green(pixel) - 0x40) <= 1, "Green should be ~64, was " + green(pixel));
        assertTrue(Math.abs(blue(pixel) - 0x20) <= 1, "Blue should be ~32, was " + blue(pixel));
    }

    @Test
    void screenBlendWhiteYieldsWhite() {
        // screen(white, anything) = white
        BufferedImage white = solidImage(2, 2, 0xFFFFFFFF);
        BufferedImage color = solidImage(2, 2, 0xFF804020);
        BufferedImage result = BlendCompositor.blend(white, color, "screen", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, red(pixel));
        assertEquals(255, green(pixel));
        assertEquals(255, blue(pixel));
    }

    @Test
    void darkenBlend() {
        // darken picks minimum per channel
        BufferedImage a = solidImage(2, 2, 0xFFFF4080);
        BufferedImage b = solidImage(2, 2, 0xFF408040);
        BufferedImage result = BlendCompositor.blend(a, b, "darken", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, alpha(pixel));
        assertEquals(0x40, red(pixel)); // min(0xFF, 0x40)
        assertEquals(0x40, green(pixel)); // min(0x40, 0x80)
        assertEquals(0x40, blue(pixel)); // min(0x80, 0x40)
    }

    @Test
    void lightenBlend() {
        // lighten picks maximum per channel
        BufferedImage a = solidImage(2, 2, 0xFFFF4080);
        BufferedImage b = solidImage(2, 2, 0xFF408040);
        BufferedImage result = BlendCompositor.blend(a, b, "lighten", null);
        int pixel = result.getRGB(0, 0);
        assertEquals(255, alpha(pixel));
        assertEquals(0xFF, red(pixel)); // max(0xFF, 0x40)
        assertEquals(0x80, green(pixel)); // max(0x40, 0x80)
        assertEquals(0x80, blue(pixel)); // max(0x80, 0x40)
    }

    @Test
    void blendWithOutputBuffer() {
        // Reuse a pre-allocated output buffer
        BufferedImage src = solidImage(4, 4, 0xFFFF0000);
        BufferedImage dst = solidImage(4, 4, 0xFF0000FF);
        BufferedImage output = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        BufferedImage result = BlendCompositor.blend(src, dst, "normal", output);
        assertEquals(output, result, "Should reuse the provided output buffer");
        assertEquals(255, red(result.getRGB(1, 1)));
    }

    @Test
    void blendDifferentDimensions() {
        // src is 4x4, dst is 2x2 — exercises the slow path
        BufferedImage src = solidImage(4, 4, 0xFFFF0000);
        BufferedImage dst = solidImage(2, 2, 0xFF0000FF);
        BufferedImage result = BlendCompositor.blend(src, dst, "normal", null);
        assertNotNull(result);
        assertEquals(4, result.getWidth());
        assertEquals(4, result.getHeight());
        // Top-left (2x2 overlap): red over blue → red
        assertEquals(255, red(result.getRGB(0, 0)));
        // Bottom-right (outside dst): just red src
        assertEquals(255, red(result.getRGB(3, 3)));
    }

    @Test
    void blendBothTransparentYieldsTransparent() {
        BufferedImage src = solidImage(2, 2, 0x00000000);
        BufferedImage dst = solidImage(2, 2, 0x00000000);
        BufferedImage result = BlendCompositor.blend(src, dst, "normal", null);
        assertEquals(0, alpha(result.getRGB(0, 0)));
    }

    @Test
    void unknownModeDefaultsToNormal() {
        BufferedImage src = solidImage(2, 2, 0xFFFF0000);
        BufferedImage dst = solidImage(2, 2, 0xFF0000FF);
        BufferedImage result = BlendCompositor.blend(src, dst, "overlay", null);
        // Should behave like normal — opaque red
        int pixel = result.getRGB(0, 0);
        assertEquals(255, red(pixel));
        assertEquals(0, blue(pixel));
    }
}

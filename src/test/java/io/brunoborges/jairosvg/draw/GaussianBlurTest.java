package io.brunoborges.jairosvg.draw;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GaussianBlur} — 3-pass box blur approximation.
 */
class GaussianBlurTest {

    private static BufferedImage solidImage(int w, int h, int argb) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        java.util.Arrays.fill(data, argb);
        return img;
    }

    private static BufferedImage checkerboard(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, ((x + y) % 2 == 0) ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        return img;
    }

    @Test
    void zeroDeviationReturnsInputUnchanged() {
        BufferedImage input = solidImage(10, 10, 0xFFFF0000);
        BufferedImage temp = solidImage(10, 10, 0);
        BufferedImage output = solidImage(10, 10, 0);
        BufferedImage result = GaussianBlur.apply(input, 0.0, temp, output);
        assertSame(input, result, "Zero stdDeviation should return the input image");
    }

    @Test
    void negativeDeviationReturnsInputUnchanged() {
        BufferedImage input = solidImage(10, 10, 0xFFFF0000);
        BufferedImage temp = solidImage(10, 10, 0);
        BufferedImage output = solidImage(10, 10, 0);
        BufferedImage result = GaussianBlur.apply(input, -1.0, temp, output);
        assertSame(input, result);
    }

    @Test
    void solidImageStaysSolid() {
        // Blurring a uniform image should keep it uniform
        BufferedImage input = solidImage(20, 20, 0xFFFF8040);
        BufferedImage temp = solidImage(20, 20, 0);
        BufferedImage output = solidImage(20, 20, 0);
        BufferedImage result = GaussianBlur.apply(input, 2.0, temp, output);
        // Check center pixel — should still be roughly the same color
        int pixel = result.getRGB(10, 10);
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        // Box blur integer rounding can drift by a few values on uniform input
        assertTrue(Math.abs(r - 0xFF) <= 8, "Red should be ~255, was " + r);
        assertTrue(Math.abs(g - 0x80) <= 8, "Green should be ~128, was " + g);
        assertTrue(Math.abs(b - 0x40) <= 8, "Blue should be ~64, was " + b);
    }

    @Test
    void blurReducesContrastOnCheckerboard() {
        // A checkerboard of black/white should have reduced contrast after blur
        int size = 32;
        BufferedImage input = checkerboard(size, size);
        BufferedImage temp = solidImage(size, size, 0);
        BufferedImage output = solidImage(size, size, 0);

        BufferedImage result = GaussianBlur.apply(input, 3.0, temp, output);

        // Center pixels should no longer be pure black or pure white
        int pixel = result.getRGB(size / 2, size / 2);
        int r = (pixel >> 16) & 0xFF;
        // After blur, checkerboard center should converge toward mid-gray
        assertTrue(r > 50 && r < 205, "Blurred checkerboard center should be mid-gray, red was " + r);
    }

    @Test
    void blurPreservesAlpha() {
        // Fully opaque image should remain fully opaque after blur
        BufferedImage input = solidImage(16, 16, 0xFFFF0000);
        BufferedImage temp = solidImage(16, 16, 0);
        BufferedImage output = solidImage(16, 16, 0);
        BufferedImage result = GaussianBlur.apply(input, 1.0, temp, output);
        int pixel = result.getRGB(8, 8);
        int a = (pixel >> 24) & 0xFF;
        assertTrue(a >= 250, "Alpha should remain ~255 for solid opaque input, was " + a);
    }

    @Test
    void blurUsesOutputBuffer() {
        BufferedImage input = solidImage(10, 10, 0xFFFF0000);
        BufferedImage temp = solidImage(10, 10, 0);
        BufferedImage output = solidImage(10, 10, 0);
        BufferedImage result = GaussianBlur.apply(input, 1.0, temp, output);
        assertSame(output, result, "Should use the provided output buffer");
    }

    @Test
    void largerDeviationProducesMoreBlur() {
        // With a sharp edge, larger sigma should produce smoother gradients
        int size = 32;
        BufferedImage input = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        // Left half white, right half black
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                input.setRGB(x, y, x < size / 2 ? 0xFFFFFFFF : 0xFF000000);
            }
        }

        BufferedImage temp1 = solidImage(size, size, 0);
        BufferedImage out1 = solidImage(size, size, 0);
        BufferedImage result1 = GaussianBlur.apply(input, 1.0, temp1, out1);

        // Re-create input (blur modifies pixel arrays in-place)
        BufferedImage input2 = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                input2.setRGB(x, y, x < size / 2 ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        BufferedImage temp2 = solidImage(size, size, 0);
        BufferedImage out2 = solidImage(size, size, 0);
        BufferedImage result2 = GaussianBlur.apply(input2, 5.0, temp2, out2);

        // At a point well into the "black" half, larger blur should bring more white
        int r1 = (result1.getRGB(size / 2 + 4, size / 2) >> 16) & 0xFF;
        int r2 = (result2.getRGB(size / 2 + 4, size / 2) >> 16) & 0xFF;
        assertTrue(r2 > r1, "Larger sigma should produce more bleed-over. sigma=1 red=" + r1 + ", sigma=5 red=" + r2);
    }
}

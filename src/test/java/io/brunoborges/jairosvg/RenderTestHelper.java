package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

/**
 * Shared test utilities for rendering SVG and asserting pixel colors.
 */
public final class RenderTestHelper {

    private static final int DEFAULT_TOLERANCE = 2;

    private RenderTestHelper() {
    }

    /** Render an SVG string to a BufferedImage via PNG. */
    public static BufferedImage render(String svg) throws Exception {
        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    /** Extract RGBA components from a pixel. */
    public static int[] rgba(BufferedImage img, int x, int y) {
        int p = img.getRGB(x, y);
        return new int[]{(p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF, (p >> 24) & 0xFF};
    }

    /** Assert pixel RGB with default tolerance (2). */
    public static void assertPixelColor(BufferedImage img, int x, int y, int r, int g, int b) {
        assertPixelColor(img, x, y, r, g, b, DEFAULT_TOLERANCE);
    }

    /** Assert pixel RGB with custom tolerance. */
    public static void assertPixelColor(BufferedImage img, int x, int y, int r, int g, int b, int tolerance) {
        int pixel = img.getRGB(x, y);
        int actualR = (pixel >> 16) & 0xFF;
        int actualG = (pixel >> 8) & 0xFF;
        int actualB = pixel & 0xFF;
        String msg = "Pixel(%d,%d) expected rgb(%d,%d,%d) but was rgb(%d,%d,%d)".formatted(x, y, r, g, b, actualR,
                actualG, actualB);
        assertTrue(Math.abs(actualR - r) <= tolerance, msg);
        assertTrue(Math.abs(actualG - g) <= tolerance, msg);
        assertTrue(Math.abs(actualB - b) <= tolerance, msg);
    }

    /** Assert pixel RGBA with default tolerance (2). */
    public static void assertPixelRGBA(BufferedImage img, int x, int y, int r, int g, int b, int a) {
        assertPixelRGBA(img, x, y, r, g, b, a, DEFAULT_TOLERANCE);
    }

    /** Assert pixel RGBA with custom tolerance. */
    public static void assertPixelRGBA(BufferedImage img, int x, int y, int r, int g, int b, int a, int tolerance) {
        int pixel = img.getRGB(x, y);
        int actualA = (pixel >> 24) & 0xFF;
        int actualR = (pixel >> 16) & 0xFF;
        int actualG = (pixel >> 8) & 0xFF;
        int actualB = pixel & 0xFF;
        String msg = "Pixel(%d,%d) expected rgba(%d,%d,%d,%d) but was rgba(%d,%d,%d,%d)".formatted(x, y, r, g, b, a,
                actualR, actualG, actualB, actualA);
        assertTrue(Math.abs(actualR - r) <= tolerance, msg);
        assertTrue(Math.abs(actualG - g) <= tolerance, msg);
        assertTrue(Math.abs(actualB - b) <= tolerance, msg);
        assertTrue(Math.abs(actualA - a) <= tolerance, msg);
    }

    /** Assert that a pixel is fully opaque with given RGB. */
    public static void assertOpaqueColor(int pixel, int r, int g, int b) {
        assertEquals(r, (pixel >> 16) & 0xFF, "Red channel");
        assertEquals(g, (pixel >> 8) & 0xFF, "Green channel");
        assertEquals(b, pixel & 0xFF, "Blue channel");
    }

    /** GZIP-compress bytes. */
    public static byte[] gzip(byte[] bytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(bytes);
        }
        return out.toByteArray();
    }

    /**
     * Count dark pixels (R,G,B all < threshold, alpha > 0) in a rectangular region.
     */
    public static int countDarkPixels(BufferedImage img, int x1, int y1, int x2, int y2, int threshold) {
        int count = 0;
        for (int y = Math.max(0, y1); y <= Math.min(img.getHeight() - 1, y2); y++) {
            for (int x = Math.max(0, x1); x <= Math.min(img.getWidth() - 1, x2); x++) {
                int pixel = img.getRGB(x, y);
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                if (a > 0 && r < threshold && g < threshold && b < threshold) {
                    count++;
                }
            }
        }
        return count;
    }
}

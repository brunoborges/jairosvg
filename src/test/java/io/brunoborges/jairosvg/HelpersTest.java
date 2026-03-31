package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import io.brunoborges.jairosvg.surface.PngSurface;
import io.brunoborges.jairosvg.surface.Surface;
import io.brunoborges.jairosvg.util.Helpers;

class HelpersTest {

    private static final double EPSILON = 0.01;

    /**
     * Create a minimal Surface with standard DPI (96) for unit-conversion tests.
     */
    private static Surface testSurface() {
        Surface s = new PngSurface();
        s.dpi = 96;
        s.fontSize = 16;
        s.contextWidth = 800;
        s.contextHeight = 600;
        return s;
    }

    // ── normalize ──────────────────────────────────────────────────────

    @Test
    void testNormalize() {
        assertEquals("10 20 30 40", Helpers.normalize("10,20,30,40"));
        assertEquals("10 -20 30", Helpers.normalize("10-20 30"));
    }

    // ── parseTransform ─────────────────────────────────────────────────

    @Test
    void testParseTransformTranslateXY() {
        AffineTransform t = Helpers.parseTransform(null, "translate(10, 20)");
        assertEquals(10, t.getTranslateX(), EPSILON);
        assertEquals(20, t.getTranslateY(), EPSILON);
    }

    @Test
    void testParseTransformTranslateXOnly() {
        AffineTransform t = Helpers.parseTransform(null, "translate(10)");
        assertEquals(10, t.getTranslateX(), EPSILON);
        assertEquals(0, t.getTranslateY(), EPSILON);
    }

    @Test
    void testParseTransformScaleUniform() {
        AffineTransform t = Helpers.parseTransform(null, "scale(2)");
        assertEquals(2, t.getScaleX(), EPSILON);
        assertEquals(2, t.getScaleY(), EPSILON);
    }

    @Test
    void testParseTransformScaleNonUniform() {
        AffineTransform t = Helpers.parseTransform(null, "scale(2, 3)");
        assertEquals(2, t.getScaleX(), EPSILON);
        assertEquals(3, t.getScaleY(), EPSILON);
    }

    @Test
    void testParseTransformRotate90() {
        AffineTransform t = Helpers.parseTransform(null, "rotate(90)");
        // 90° rotation: cos90=0, sin90=1
        double[] m = new double[6];
        t.getMatrix(m); // [m00, m10, m01, m11, m02, m12]
        assertEquals(0, m[0], EPSILON); // cos(90)
        assertEquals(1, m[1], EPSILON); // sin(90)
        assertEquals(-1, m[2], EPSILON); // -sin(90)
        assertEquals(0, m[3], EPSILON); // cos(90)
    }

    @Test
    void testParseTransformRotateAroundPoint() {
        AffineTransform t = Helpers.parseTransform(null, "rotate(45, 50, 50)");
        // Should be equivalent to translate(50,50) rotate(45) translate(-50,-50)
        AffineTransform expected = new AffineTransform();
        expected.translate(50, 50);
        expected.rotate(Math.toRadians(45));
        expected.translate(-50, -50);
        assertTransformEquals(expected, t);
    }

    @Test
    void testParseTransformSkewX() {
        AffineTransform t = Helpers.parseTransform(null, "skewX(30)");
        assertEquals(Math.tan(Math.toRadians(30)), t.getShearX(), EPSILON);
        assertEquals(0, t.getShearY(), EPSILON);
    }

    @Test
    void testParseTransformSkewY() {
        AffineTransform t = Helpers.parseTransform(null, "skewY(30)");
        assertEquals(0, t.getShearX(), EPSILON);
        assertEquals(Math.tan(Math.toRadians(30)), t.getShearY(), EPSILON);
    }

    @Test
    void testParseTransformMatrix() {
        AffineTransform t = Helpers.parseTransform(null, "matrix(1, 0, 0, 1, 10, 20)");
        assertEquals(1, t.getScaleX(), EPSILON);
        assertEquals(1, t.getScaleY(), EPSILON);
        assertEquals(10, t.getTranslateX(), EPSILON);
        assertEquals(20, t.getTranslateY(), EPSILON);
    }

    @Test
    void testParseTransformMultiple() {
        AffineTransform t = Helpers.parseTransform(null, "translate(10,20) scale(2)");
        // translate then scale: the resulting matrix is translate(10,20) concatenated
        // with scale(2)
        AffineTransform expected = new AffineTransform();
        expected.translate(10, 20);
        expected.scale(2, 2);
        assertTransformEquals(expected, t);
    }

    @Test
    void testParseTransformEmptyString() {
        AffineTransform t = Helpers.parseTransform(null, "");
        assertTrue(t.isIdentity());
    }

    @Test
    void testParseTransformNull() {
        AffineTransform t = Helpers.parseTransform(null, null);
        assertTrue(t.isIdentity());
    }

    private void assertTransformEquals(AffineTransform expected, AffineTransform actual) {
        double[] e = new double[6];
        double[] a = new double[6];
        expected.getMatrix(e);
        actual.getMatrix(a);
        for (int i = 0; i < 6; i++) {
            assertEquals(e[i], a[i], EPSILON, "matrix element [" + i + "]");
        }
    }

    // ── size / unit parsing ─────────────────────────────────────────────

    @Test
    void testSizePlainNumber() {
        assertEquals(100.0, Helpers.size(null, "100", "x"), EPSILON);
    }

    @Test
    void testSizePixels() {
        assertEquals(50.0, Helpers.size(testSurface(), "50px", "x"), EPSILON);
    }

    @Test
    void testSizeInches() {
        // 1in = 96px at 96 DPI
        assertEquals(96.0, Helpers.size(testSurface(), "1in", "x"), EPSILON);
    }

    @Test
    void testSizeCentimeters() {
        // 2.54cm = 1in = 96px at 96 DPI
        assertEquals(96.0, Helpers.size(testSurface(), "2.54cm", "x"), EPSILON);
    }

    @Test
    void testSizeMillimeters() {
        // 25.4mm = 1in = 96px at 96 DPI
        assertEquals(96.0, Helpers.size(testSurface(), "25.4mm", "x"), EPSILON);
    }

    @Test
    void testSizePoints() {
        // 72pt = 1in = 96px at 96 DPI
        assertEquals(96.0, Helpers.size(testSurface(), "72pt", "x"), EPSILON);
    }

    @Test
    void testSizePicas() {
        // 6pc = 1in = 96px at 96 DPI
        assertEquals(96.0, Helpers.size(testSurface(), "6pc", "x"), EPSILON);
    }

    @Test
    void testSizeZero() {
        assertEquals(0.0, Helpers.size(null, "0", "x"), EPSILON);
    }

    @Test
    void testSizeEmptyString() {
        assertEquals(0.0, Helpers.size(null, "", "x"), EPSILON);
    }

    @Test
    void testSizeNull() {
        assertEquals(0.0, Helpers.size(null, null, "x"), EPSILON);
    }

    @Test
    void testSizeInvalidString() {
        assertEquals(0.0, Helpers.size(testSurface(), "abc", "x"), EPSILON);
    }

    // ── parsePercent ────────────────────────────────────────────────────

    @Test
    void testParsePercentWithPercent() {
        assertEquals(0.5f, Helpers.parsePercent("50%"), 0.001f);
    }

    @Test
    void testParsePercentPlainFloat() {
        assertEquals(0.5f, Helpers.parsePercent("0.5"), 0.001f);
    }

    @Test
    void testParsePercentEmpty() {
        assertEquals(0.0f, Helpers.parsePercent(""), 0.001f);
    }

    @Test
    void testParsePercentNull() {
        assertEquals(0.0f, Helpers.parsePercent(null), 0.001f);
    }

    @Test
    void testParsePercentInvalid() {
        assertEquals(0.0f, Helpers.parsePercent("abc"), 0.001f);
    }

    @Test
    void testParsePercent100() {
        assertEquals(1.0f, Helpers.parsePercent("100%"), 0.001f);
    }

    @Test
    void testParsePercent0() {
        assertEquals(0.0f, Helpers.parsePercent("0%"), 0.001f);
    }

    // ── parseDoubleOr ──────────────────────────────────────────────────

    @Test
    void testParseDoubleOrValid() {
        assertEquals(3.14, Helpers.parseDoubleOr("3.14", 0), EPSILON);
    }

    @Test
    void testParseDoubleOrInvalid() {
        assertEquals(5.0, Helpers.parseDoubleOr("abc", 5.0), EPSILON);
    }

    @Test
    void testParseDoubleOrNull() {
        assertEquals(1.0, Helpers.parseDoubleOr(null, 1.0), EPSILON);
    }

    @Test
    void testParseDoubleOrEmpty() {
        assertEquals(2.0, Helpers.parseDoubleOr("", 2.0), EPSILON);
    }

    // ── parseDouble ────────────────────────────────────────────────────

    @Test
    void testParseDoubleValid() {
        assertEquals(42.0, Helpers.parseDouble("42"), EPSILON);
    }

    @Test
    void testParseDoubleInvalid() {
        assertEquals(0.0, Helpers.parseDouble("abc"), EPSILON);
    }

    @Test
    void testParseDoubleNegative() {
        assertEquals(-7.5, Helpers.parseDouble("-7.5"), EPSILON);
    }

    // ── preserveAspectRatio via SVG rendering ──────────────────────────

    @Test
    void testPreserveAspectRatioNone() throws Exception {
        // "none" stretches: a 200x100 viewBox into 100x100 should stretch
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 200 100" preserveAspectRatio="none">
                  <rect x="190" y="0" width="10" height="10" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
        assertEquals(100, img.getHeight());
        // With "none", the 200-wide viewBox is squeezed into 100px width,
        // so the rect at x=190 maps to x=95. Red should appear near the right edge.
        int[] pixel = RenderTestHelper.rgba(img, 97, 5);
        assertEquals(255, pixel[0], 5); // red channel
    }

    @Test
    void testPreserveAspectRatioMeet() throws Exception {
        // "xMidYMid meet": scale uniformly to fit within 100x100
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 200 100" preserveAspectRatio="xMidYMid meet">
                  <rect x="0" y="0" width="200" height="100" fill="blue"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
        assertEquals(100, img.getHeight());
        // meet: uniform scale = min(100/200, 100/100) = 0.5
        // Content is 100x50 centered → top 25px should be transparent/white
        int[] topPixel = RenderTestHelper.rgba(img, 50, 5);
        // Not blue — the content is centered vertically, so top band is empty
        assertTrue(topPixel[2] < 200 || topPixel[3] < 128, "Top area should not be fully blue under meet");
    }

    @Test
    void testPreserveAspectRatioSlice() throws Exception {
        // "xMinYMin slice": scale uniformly to cover, clipping overflow
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 200 100" preserveAspectRatio="xMinYMin slice">
                  <rect x="0" y="0" width="200" height="100" fill="green"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
        assertEquals(100, img.getHeight());
        // slice: uniform scale = max(100/200, 100/100) = 1.0
        // Content fills entire viewport starting from top-left
        int[] midPixel = RenderTestHelper.rgba(img, 50, 50);
        assertEquals(128, midPixel[1], 10); // green channel (0x80 = 128)
    }
}

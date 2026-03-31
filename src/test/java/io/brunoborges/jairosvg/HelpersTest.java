package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;

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

    // ── distance ───────────────────────────────────────────────────────

    @Test
    void testDistance() {
        assertEquals(5.0, Helpers.distance(0, 0, 3, 4), EPSILON);
        assertEquals(0.0, Helpers.distance(5, 5, 5, 5), EPSILON);
    }

    // ── paint ──────────────────────────────────────────────────────────

    @Test
    void testPaintNull() {
        String[] result = Helpers.paint(null);
        assertNull(result[0]);
        assertNull(result[1]);
    }

    @Test
    void testPaintBlank() {
        String[] result = Helpers.paint("   ");
        assertNull(result[0]);
        assertNull(result[1]);
    }

    @Test
    void testPaintPlainColor() {
        String[] result = Helpers.paint("red");
        assertNull(result[0]);
        assertEquals("red", result[1]);
    }

    @Test
    void testPaintUrlOnly() {
        String[] result = Helpers.paint("url(#gradient1)");
        assertEquals("gradient1", result[0]);
        assertNull(result[1]);
    }

    @Test
    void testPaintUrlWithColor() {
        String[] result = Helpers.paint("url(#grad) red");
        assertEquals("grad", result[0]);
        assertEquals("red", result[1]);
    }

    @Test
    void testPaintEmpty() {
        String[] result = Helpers.paint("");
        assertNull(result[0]);
        assertNull(result[1]);
    }

    // ── pointAngle ────────────────────────────────────────────────────

    @Test
    void testPointAngle() {
        assertEquals(0, Helpers.pointAngle(0, 0, 1, 0), EPSILON);
        assertEquals(Math.PI / 2, Helpers.pointAngle(0, 0, 0, 1), EPSILON);
        assertEquals(Math.PI, Helpers.pointAngle(0, 0, -1, 0), EPSILON);
    }

    // ── quadraticPoints ───────────────────────────────────────────────

    @Test
    void testQuadraticPoints() {
        double[] result = Helpers.quadraticPoints(0, 0, 50, 100, 100, 0);
        assertEquals(6, result.length);
        // xq1 = 50*2/3 + 0/3 = 33.33
        assertEquals(33.33, result[0], EPSILON);
        // yq1 = 100*2/3 + 0/3 = 66.67
        assertEquals(66.67, result[1], EPSILON);
        // endpoint
        assertEquals(100, result[4], EPSILON);
        assertEquals(0, result[5], EPSILON);
    }

    // ── rotate ────────────────────────────────────────────────────────

    @Test
    void testRotate() {
        double[] r = Helpers.rotate(1, 0, Math.PI / 2);
        assertEquals(0, r[0], EPSILON);
        assertEquals(1, r[1], EPSILON);
    }

    // ── hasViewbox / getViewbox ───────────────────────────────────────

    @Test
    void testHasViewboxTrue() {
        double[] nf = {100, 100, 0, 0, 100, 100};
        assertTrue(Helpers.hasViewbox(nf));
        double[] vb = Helpers.getViewbox(nf);
        assertNotNull(vb);
        assertEquals(0, vb[0], EPSILON);
    }

    @Test
    void testHasViewboxFalse() {
        double[] nf = {100, 100, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        assertFalse(Helpers.hasViewbox(nf));
        assertNull(Helpers.getViewbox(nf));
    }

    // ── normalize ─────────────────────────────────────────────────────

    @Test
    void testNormalizeNull() {
        assertEquals("", Helpers.normalize(null));
    }

    @Test
    void testNormalizeEmpty() {
        assertEquals("", Helpers.normalize(""));
    }

    @Test
    void testNormalizeUppercaseE() {
        // 'E' → 'e' replacement
        assertEquals("1e5", Helpers.normalize("1E5"));
    }

    @Test
    void testNormalizeDecimalSplit() {
        // consecutive decimals split: ".5.6" → ".5 .6"
        assertEquals(".5 .6", Helpers.normalize(".5.6"));
    }

    @Test
    void testNormalizeNegativeSigns() {
        // negative sign gets space: "10-20" → "10 -20"
        assertEquals("10 -20 30", Helpers.normalize("10-20 30"));
    }

    @Test
    void testNormalizeExponentNotSplit() {
        // negative after 'e' should NOT be split (lookbehind (?<!e))
        assertEquals("1e-5", Helpers.normalize("1e-5"));
    }

    // ── point ─────────────────────────────────────────────────────────

    @Test
    void testPointValid() {
        double[] p = Helpers.point(null, "10 20");
        assertEquals(10, p[0], EPSILON);
        assertEquals(20, p[1], EPSILON);
    }

    @Test
    void testPointThrowsOnSingleValue() {
        assertThrows(Helpers.PointError.class, () -> Helpers.point(null, "10"));
    }

    // ── parseTransform extras ─────────────────────────────────────────

    @Test
    void testParseTransformUnknownType() {
        // Unknown transform type goes to switch default (no-op)
        AffineTransform t = Helpers.parseTransform(null, "translateX(10)");
        assertTrue(t.isIdentity());
    }

    @Test
    void testParseTransformMatrixTooFewValues() {
        // matrix with < 6 values should be ignored
        AffineTransform t = Helpers.parseTransform(null, "matrix(1, 0, 0)");
        assertTrue(t.isIdentity());
    }

    @Test
    void testParseTransformWhitespaceVariations() {
        AffineTransform t = Helpers.parseTransform(null, "translate( 10 , 20 )");
        assertEquals(10, t.getTranslateX(), EPSILON);
        assertEquals(20, t.getTranslateY(), EPSILON);
    }

    @Test
    void testParseTransformRotateWithCx() {
        // rotate with only 2 values (angle + cx but no cy)
        AffineTransform t = Helpers.parseTransform(null, "rotate(90, 50)");
        assertFalse(t.isIdentity());
    }

    // ── size / unit parsing extras ────────────────────────────────────

    @Test
    void testSizeEm() {
        // 2em = 2 * fontSize(16) = 32
        assertEquals(32.0, Helpers.size(testSurface(), "2em", "x"), EPSILON);
    }

    @Test
    void testSizeEx() {
        // 2ex = 2 * fontSize/2 = 16
        assertEquals(16.0, Helpers.size(testSurface(), "2ex", "x"), EPSILON);
    }

    @Test
    void testSizeCh() {
        // 2ch = 2 * fontSize/2 = 16
        assertEquals(16.0, Helpers.size(testSurface(), "2ch", "x"), EPSILON);
    }

    @Test
    void testSizePercentX() {
        // 50% of contextWidth 800 = 400
        assertEquals(400.0, Helpers.size(testSurface(), "50%", "x"), EPSILON);
    }

    @Test
    void testSizePercentY() {
        // 50% of contextHeight 600 = 300
        assertEquals(300.0, Helpers.size(testSurface(), "50%", "y"), EPSILON);
    }

    @Test
    void testSizePercentDiagonal() {
        // 50% of diagonal reference = hypot(800,600)/sqrt(2) * 0.5
        double expected = Math.hypot(800, 600) / Math.sqrt(2) * 0.5;
        assertEquals(expected, Helpers.size(testSurface(), "50%", null), EPSILON);
    }

    @Test
    void testSizePercentXyReference() {
        // "xy" reference uses diagonal
        double expected = Math.hypot(800, 600) / Math.sqrt(2) * 0.5;
        assertEquals(expected, Helpers.size(testSurface(), "50%", "xy"), EPSILON);
    }

    @Test
    void testSizeNoSurface() {
        // Non-plain-number string with null surface returns 0
        assertEquals(0.0, Helpers.size(null, "50%", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackWithWhitespace() {
        // Whitespace causes fallback path via normalize
        assertEquals(50.0, Helpers.size(testSurface(), "  50px", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackCommaList() {
        // Comma in value triggers normalize; takes first token
        assertEquals(80.0, Helpers.size(testSurface(), "10%,20", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackEmUnit() {
        // Whitespace + em unit forces fallback path
        assertEquals(32.0, Helpers.size(testSurface(), "  2em", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackExUnit() {
        assertEquals(16.0, Helpers.size(testSurface(), "  2ex", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackChUnit() {
        assertEquals(16.0, Helpers.size(testSurface(), "  2ch", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackPtUnit() {
        // 72pt = 1in = 96px at 96 DPI, so in fallback
        assertEquals(96.0, Helpers.size(testSurface(), "  72pt", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackPxUnit() {
        assertEquals(50.0, Helpers.size(testSurface(), "  50px", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackMmUnit() {
        // 25.4mm = 1in = 96px at 96 DPI
        assertEquals(96.0, Helpers.size(testSurface(), "  25.4mm", "x"), EPSILON);
    }

    @Test
    void testSizeFallbackPercentUnit() {
        assertEquals(400.0, Helpers.size(testSurface(), "  50%", "x"), EPSILON);
    }

    @Test
    void testSizeNoReference() {
        // size(Surface, String) calls size(Surface, String, "xy")
        assertEquals(50.0, Helpers.size(testSurface(), "50px"), EPSILON);
    }

    @Test
    void testSizeScientificNotation() {
        assertEquals(100.0, Helpers.size(null, "1e2", "x"), EPSILON);
    }

    // ── calc() expressions ────────────────────────────────────────────

    @Test
    void testCalcAddition() {
        assertEquals(150.0, Helpers.size(testSurface(), "calc(100px + 50px)", "x"), EPSILON);
    }

    @Test
    void testCalcSubtraction() {
        assertEquals(50.0, Helpers.size(testSurface(), "calc(100px - 50px)", "x"), EPSILON);
    }

    @Test
    void testCalcMultiplication() {
        assertEquals(100.0, Helpers.size(testSurface(), "calc(2 * 50px)", "x"), EPSILON);
    }

    @Test
    void testCalcDivision() {
        assertEquals(50.0, Helpers.size(testSurface(), "calc(100px / 2)", "x"), EPSILON);
    }

    @Test
    void testCalcParenthesized() {
        assertEquals(300.0, Helpers.size(testSurface(), "calc((100px + 50px) * 2)", "x"), EPSILON);
    }

    @Test
    void testCalcPercent() {
        // 50% of 800 (x ref) = 400
        assertEquals(400.0, Helpers.size(testSurface(), "calc(50%)", "x"), EPSILON);
    }

    @Test
    void testCalcNestedCalc() {
        assertEquals(100.0, Helpers.size(testSurface(), "calc(calc(50px) + 50px)", "x"), EPSILON);
    }

    @Test
    void testCalcNegativeSign() {
        // -10px → -10
        assertEquals(-10.0, Helpers.size(testSurface(), "calc(-10px)", "x"), EPSILON);
    }

    @Test
    void testCalcPositiveSign() {
        assertEquals(10.0, Helpers.size(testSurface(), "calc(+10px)", "x"), EPSILON);
    }

    @Test
    void testCalcExponent() {
        // 1e2px = 100px
        assertEquals(100.0, Helpers.size(testSurface(), "calc(1e2px)", "x"), EPSILON);
    }

    @Test
    void testCalcExponentWithSign() {
        // 1e+2px = 100px
        assertEquals(100.0, Helpers.size(testSurface(), "calc(1e+2px)", "x"), EPSILON);
    }

    @Test
    void testCalcEmAndPx() {
        // 1em + 2px = 16 + 2 = 18
        assertEquals(18.0, Helpers.size(testSurface(), "calc(1em + 2px)", "x"), EPSILON);
    }

    @Test
    void testCalcEmpty() {
        assertEquals(0.0, Helpers.size(testSurface(), "calc()", "x"), EPSILON);
    }

    @Test
    void testCalcCaseInsensitive() {
        assertEquals(50.0, Helpers.size(testSurface(), "CALC(50px)", "x"), EPSILON);
    }

    @Test
    void testCalcPercentMinusPx() {
        // 100% of 800 (x) - 50px = 750
        assertEquals(750.0, Helpers.size(testSurface(), "calc(100% - 50px)", "x"), EPSILON);
    }

    // ── clipRect ──────────────────────────────────────────────────────

    @Test
    void testClipRectNull() {
        assertEquals(0, Helpers.clipRect(null).length);
    }

    @Test
    void testClipRectEmpty() {
        assertEquals(0, Helpers.clipRect("").length);
    }

    @Test
    void testClipRectNoMatch() {
        assertEquals(0, Helpers.clipRect("something").length);
    }

    @Test
    void testClipRectValid() {
        String[] result = Helpers.clipRect("rect(10 20 30 40)");
        assertEquals(4, result.length);
        assertEquals("10", result[0]);
        assertEquals("20", result[1]);
        assertEquals("30", result[2]);
        assertEquals("40", result[3]);
    }

    // ── parseFont ─────────────────────────────────────────────────────

    @Test
    void testParseFontFull() {
        Map<String, String> f = Helpers.parseFont("italic small-caps bold 12px/14px Arial sans-serif");
        assertEquals("italic", f.get("font-style"));
        assertEquals("small-caps", f.get("font-variant"));
        assertEquals("bold", f.get("font-weight"));
        assertEquals("12px", f.get("font-size"));
        assertEquals("14px", f.get("line-height"));
        assertEquals("Arial sans-serif", f.get("font-family"));
    }

    @Test
    void testParseFontMinimal() {
        Map<String, String> f = Helpers.parseFont("16px Helvetica");
        assertEquals("normal", f.get("font-style"));
        assertEquals("normal", f.get("font-variant"));
        assertEquals("normal", f.get("font-weight"));
        assertEquals("16px", f.get("font-size"));
        assertEquals("Helvetica", f.get("font-family"));
    }

    @Test
    void testParseFontOblique() {
        Map<String, String> f = Helpers.parseFont("oblique 12px Times");
        assertEquals("oblique", f.get("font-style"));
    }

    @Test
    void testParseFontNumericWeight() {
        Map<String, String> f = Helpers.parseFont("700 12px monospace");
        assertEquals("700", f.get("font-weight"));
    }

    @Test
    void testParseFontNormal() {
        Map<String, String> f = Helpers.parseFont("normal 12px sans-serif");
        assertEquals("normal", f.get("font-style"));
        assertEquals("12px", f.get("font-size"));
    }

    @Test
    void testParseFontBolder() {
        Map<String, String> f = Helpers.parseFont("bolder 10px Georgia");
        assertEquals("bolder", f.get("font-weight"));
    }

    @Test
    void testParseFontLighter() {
        Map<String, String> f = Helpers.parseFont("lighter 10px Georgia");
        assertEquals("lighter", f.get("font-weight"));
    }

    @Test
    void testParseFontMultiWordFamily() {
        Map<String, String> f = Helpers.parseFont("16px Times New Roman");
        assertEquals("Times New Roman", f.get("font-family"));
    }

    // ── preserveAspectRatio extras via rendering ──────────────────────

    @Test
    void testPreserveAspectRatioXMaxYMax() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 200 100" preserveAspectRatio="xMaxYMax meet">
                  <rect x="0" y="0" width="200" height="100" fill="blue"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
        assertEquals(100, img.getHeight());
    }

    @Test
    void testPreserveAspectRatioXMinYMax() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 200 100" preserveAspectRatio="xMinYMax meet">
                  <rect x="0" y="0" width="200" height="100" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
    }

    @Test
    void testPreserveAspectRatioXMaxYMin() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 200 100" preserveAspectRatio="xMaxYMin meet">
                  <rect x="0" y="0" width="200" height="100" fill="green"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
    }

    @Test
    void testPreserveAspectRatioXMidYMinSlice() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 100 200" preserveAspectRatio="xMidYMin slice">
                  <rect x="0" y="0" width="100" height="200" fill="blue"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
    }

    @Test
    void testPreserveAspectRatioXMaxYMaxSlice() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"
                     viewBox="0 0 200 100" preserveAspectRatio="xMaxYMax slice">
                  <rect x="0" y="0" width="200" height="100" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
    }

    // ── transform-origin via rendering ────────────────────────────────

    @Test
    void testTransformOriginCenter() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="25" y="25" width="50" height="50" fill="red"
                        transform="rotate(0)" style="transform-origin: center center"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0, 10);
    }

    @Test
    void testTransformOriginLeftTop() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="0" y="0" width="50" height="50" fill="blue"
                        transform="rotate(0)" style="transform-origin: left top"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 25, 25, 0, 0, 255, 10);
    }

    @Test
    void testTransformOriginRightBottom() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="50" y="50" width="50" height="50" fill="green"
                        transform="rotate(0)" style="transform-origin: right bottom"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        int[] pixel = RenderTestHelper.rgba(img, 75, 75);
        assertTrue(pixel[1] > 100, "Should contain green");
    }

    @Test
    void testTransformOriginNumericValue() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="0" y="0" width="50" height="50" fill="red"
                        transform="rotate(0)" style="transform-origin: 50px 50px"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 25, 25, 255, 0, 0, 10);
    }

    @Test
    void testTransformOriginSingleValue() throws Exception {
        // Single value for origin-x; origin-y defaults to contextHeight/2
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="0" y="0" width="100" height="100" fill="red"
                        transform="scale(1)" style="transform-origin: center"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0, 10);
    }

    // ── marker with viewBox via rendering ─────────────────────────────

    @Test
    void testMarkerWithViewBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <marker id="m" markerWidth="10" markerHeight="10"
                            refX="5" refY="5" viewBox="0 0 10 10"
                            preserveAspectRatio="xMidYMid meet">
                      <circle cx="5" cy="5" r="5" fill="red"/>
                    </marker>
                  </defs>
                  <line x1="10" y1="50" x2="90" y2="50" stroke="black"
                        stroke-width="2" marker-end="url(#m)"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertEquals(100, img.getWidth());
    }

    // ── path normalize edge cases via rendering ───────────────────────

    @Test
    void testPathRelativeCommands() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M10,10 l80,0 l0,80 l-80,0 z" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0, 10);
    }

    @Test
    void testPathShorthandCurves() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M10,50 C10,10 40,10 50,50 S90,90 90,50" fill="none"
                        stroke="black" stroke-width="3"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertTrue(RenderTestHelper.countDarkPixels(img, 0, 0, 99, 99, 128) > 0);
    }

    @Test
    void testPathQuadraticCurves() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M10,80 Q50,10 90,80 T10,80" fill="none"
                        stroke="black" stroke-width="3"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertTrue(RenderTestHelper.countDarkPixels(img, 0, 0, 99, 99, 128) > 0);
    }

    @Test
    void testPathArc() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M10,50 A40,40 0 1,1 90,50" fill="none"
                        stroke="black" stroke-width="3"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertTrue(RenderTestHelper.countDarkPixels(img, 0, 0, 99, 99, 128) > 0);
    }

    @Test
    void testPathHorizontalVerticalLines() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M10,10 H90 V90 H10 Z" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0, 10);
    }

    // ── SVG viewBox width/height defaulting ───────────────────────────

    @Test
    void testViewBoxDefaultsWidthHeight() throws Exception {
        // viewBox present, no explicit width/height → defaults from viewBox
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50">
                  <rect x="0" y="0" width="50" height="50" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── gradient with transform (exercises gradient inverse path) ─────

    @Test
    void testGradientWithTransform() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="g1" gradientTransform="rotate(45)">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect x="0" y="0" width="100" height="100" fill="url(#g1)"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── text with rotate attribute (exercises rotations/popRotation) ──

    @Test
    void testTextRotation() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <text x="10" y="50" rotate="0 15 30 45" font-size="20"
                        fill="black">Test</text>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertTrue(RenderTestHelper.countDarkPixels(img, 0, 0, 199, 99, 128) > 0);
    }

    // ── image with preserveAspectRatio ────────────────────────────────

    @Test
    void testSvgNestedViewBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <svg x="10" y="10" width="80" height="80" viewBox="0 0 40 40"
                       preserveAspectRatio="xMaxYMax meet">
                    <rect x="0" y="0" width="40" height="40" fill="red"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── clip-path with rect() ─────────────────────────────────────────

    @Test
    void testClipPathRect() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="0" y="0" width="100" height="100" fill="red"
                        clip="rect(10px 90px 90px 10px)"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }
}

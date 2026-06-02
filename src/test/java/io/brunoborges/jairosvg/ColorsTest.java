package io.brunoborges.jairosvg;

import io.brunoborges.jairosvg.css.Colors;
import in.virit.color.Color;
import in.virit.color.RgbColor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorsTest {

    private static final double EPSILON = 0.01;

    /** Component getters in 0..1 space, mirroring the old RGBA API. */
    private static double r(Color c) {
        return c.toRgbColor().r() / 255.0;
    }
    private static double g(Color c) {
        return c.toRgbColor().g() / 255.0;
    }
    private static double b(Color c) {
        return c.toRgbColor().b() / 255.0;
    }
    private static double a(Color c) {
        return c.toRgbColor().a();
    }

    /**
     * Construct an RgbColor from 0..1 component fractions for equality comparisons.
     */
    private static Color rgba(double r, double g, double b, double a) {
        return new RgbColor((int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255), a);
    }

    private static void assertColor(Color color, double r, double g, double b, double a) {
        assertEquals(r, r(color), EPSILON);
        assertEquals(g, g(color), EPSILON);
        assertEquals(b, b(color), EPSILON);
        assertEquals(a, a(color), EPSILON);
    }

    // ── Basic named colors ────────────────────────────────────────────

    @Test
    void testColorParsing() {
        assertEquals(rgba(1, 0, 0, 1), Colors.color("red"));
        assertEquals(rgba(0, 0, 0, 1), Colors.color("black"));
        assertEquals(rgba(1, 1, 1, 1), Colors.color("white"));
        assertEquals(rgba(0, 0, 0, 0), Colors.color("transparent"));
        assertEquals(rgba(1, 0, 0, 1), Colors.color("#ff0000"));
        assertEquals(rgba(1, 0, 0, 1), Colors.color("#f00"));
        assertEquals(rgba(1, 0, 0, 1), Colors.color("rgb(255, 0, 0)"));
        assertEquals(rgba(1, 0, 0, 1), Colors.color("rgba(255, 0, 0, 1)"));

        Color hslRed = Colors.color("hsl(0, 100%, 50%)");
        assertEquals(1.0, r(hslRed), EPSILON);
        assertEquals(0.0, g(hslRed), EPSILON);
        assertEquals(0.0, b(hslRed), EPSILON);
        assertEquals(1.0, a(hslRed), EPSILON);

        Color hslGreen = Colors.color("hsl(120, 100%, 50%)");
        assertEquals(0.0, r(hslGreen), EPSILON);
        assertEquals(1.0, g(hslGreen), EPSILON);
        assertEquals(0.0, b(hslGreen), EPSILON);

        Color hslBlue = Colors.color("hsl(240, 100%, 50%)");
        assertEquals(0.0, r(hslBlue), EPSILON);
        assertEquals(0.0, g(hslBlue), EPSILON);
        assertEquals(1.0, b(hslBlue), EPSILON);

        Color hslGray = Colors.color("hsl(0, 0%, 50%)");
        assertEquals(0.5, r(hslGray), EPSILON);
        assertEquals(0.5, g(hslGray), EPSILON);
        assertEquals(0.5, b(hslGray), EPSILON);

        Color hslaRed = Colors.color("hsla(0, 100%, 50%, 0.5)");
        assertEquals(1.0, r(hslaRed), EPSILON);
        assertEquals(0.0, g(hslaRed), EPSILON);
        assertEquals(0.0, b(hslaRed), EPSILON);
        assertEquals(0.5, a(hslaRed), EPSILON);

        Color hslRedWithOpacity = Colors.color("hsl(0, 100%, 50%)", 0.5);
        assertEquals(1.0, r(hslRedWithOpacity), EPSILON);
        assertEquals(0.0, g(hslRedWithOpacity), EPSILON);
        assertEquals(0.0, b(hslRedWithOpacity), EPSILON);
        assertEquals(0.5, a(hslRedWithOpacity), EPSILON);

        Color hslaRedWithOpacity = Colors.color("hsla(0, 100%, 50%, 0.5)", 0.5);
        assertEquals(1.0, r(hslaRedWithOpacity), EPSILON);
        assertEquals(0.0, g(hslaRedWithOpacity), EPSILON);
        assertEquals(0.0, b(hslaRedWithOpacity), EPSILON);
        assertEquals(0.25, a(hslaRedWithOpacity), EPSILON);
    }

    @Test
    void testColorNegate() {
        Color red = rgba(1, 0, 0, 1);
        Color negated = red.negate();
        assertEquals(0, r(negated), 0.001);
        assertEquals(1, g(negated), 0.001);
        assertEquals(1, b(negated), 0.001);
        assertEquals(1, a(negated), 0.001);
    }

    // ── null / blank / empty ──────────────────────────────────────────

    @Test
    void testColorNull() {
        assertEquals(Colors.TRANSPARENT, Colors.color(null));
    }

    @Test
    void testColorEmpty() {
        assertEquals(Colors.TRANSPARENT, Colors.color(""));
    }

    @Test
    void testColorBlank() {
        assertEquals(Colors.TRANSPARENT, Colors.color("   "));
    }

    // ── Hex colors ────────────────────────────────────────────────────

    @Test
    void testHex6() {
        Color c = Colors.color("#00ff00");
        assertEquals(0.0, r(c), EPSILON);
        assertEquals(1.0, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
        assertEquals(1.0, a(c), EPSILON);
    }

    @Test
    void testHex3() {
        Color c = Colors.color("#0f0");
        assertEquals(0.0, r(c), EPSILON);
        assertEquals(1.0, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
    }

    @Test
    void testHex8() {
        assertColor(Colors.color("#00ff0080"), 0.0, 1.0, 0.0, 128 / 255.0);
    }

    @Test
    void testHex4() {
        assertColor(Colors.color("#0f08"), 0.0, 1.0, 0.0, 8 / 15.0);
    }

    @Test
    void testHex6WithOpacity() {
        Color c = Colors.color("#ff0000", 0.5);
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.5, a(c), EPSILON);
    }

    @Test
    void testHex3WithOpacity() {
        Color c = Colors.color("#f00", 0.5);
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.5, a(c), EPSILON);
    }

    @Test
    void testHexInvalidLength() {
        Color c = Colors.color("#ff");
        assertEquals(Colors.BLACK, c);
    }

    @Test
    void testHex5Chars() {
        Color c = Colors.color("#ff00f");
        assertEquals(Colors.BLACK, c);
    }

    // Regression: malformed hex digits must not abort rendering.
    // Reported by Bruno on PR #211 — fill="#ggg" used to throw
    // IllegalArgumentException because the hex fast path bypassed the
    // lenient parser and fed -1 (from Character.digit('g', 16)) into
    // the RgbColor range check.
    @Test
    void testHexInvalidDigitsShortFormFallsBackToBlack() {
        Color c = Colors.color("#ggg");
        assertEquals(Colors.BLACK, c);
    }

    @Test
    void testHexInvalidDigitsLongFormFallsBackToBlack() {
        Color c = Colors.color("#gggggg");
        assertEquals(Colors.BLACK, c);
    }

    @Test
    void testHexInvalidDigitsWithAlphaFallsBackToBlack() {
        Color c = Colors.color("#gggggggg");
        assertEquals(Colors.BLACK, c);
    }

    // ── Named colors with opacity ─────────────────────────────────────

    @Test
    void testNamedColorWithOpacity() {
        Color c = Colors.color("red", 0.5);
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.0, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
        assertEquals(0.5, a(c), EPSILON);
    }

    @Test
    void testNamedColorFullOpacity() {
        Color c = Colors.color("blue");
        assertEquals(0.0, r(c), EPSILON);
        assertEquals(0.0, g(c), EPSILON);
        assertEquals(1.0, b(c), EPSILON);
        assertEquals(1.0, a(c), EPSILON);
    }

    @Test
    void testNamedColorCaseInsensitive() {
        Color c = Colors.color("  RED  ");
        assertEquals(1.0, r(c), EPSILON);
    }

    @Test
    void testSvgKeywordNoneCaseInsensitive() {
        assertEquals(Colors.TRANSPARENT, Colors.color("  NoNe  "));
    }

    // ── rgb() ─────────────────────────────────────────────────────────

    @Test
    void testRgbIntegers() {
        Color c = Colors.color("rgb(0, 128, 255)");
        assertEquals(0.0, r(c), EPSILON);
        assertEquals(128 / 255.0, g(c), EPSILON);
        assertEquals(1.0, b(c), EPSILON);
    }

    @Test
    void testRgbPercentages() {
        Color c = Colors.color("rgb(100%, 0%, 50%)");
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.0, g(c), EPSILON);
        assertEquals(0.5, b(c), EPSILON);
    }

    @Test
    void testRgbWrongPartCount() {
        Color c = Colors.color("rgb(255, 0)");
        assertEquals(Colors.BLACK, c);
    }

    @Test
    void testRgbWithOpacity() {
        Color c = Colors.color("rgb(255, 0, 0)", 0.5);
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.5, a(c), EPSILON);
    }

    @Test
    void testRgbSpaceSeparatedWithSlashAlpha() {
        assertColor(Colors.color("rgb(255 0 128 / 50%)"), 1.0, 0.0, 128 / 255.0, 0.5);
    }

    // ── rgba() ────────────────────────────────────────────────────────

    @Test
    void testRgbaValid() {
        Color c = Colors.color("rgba(128, 0, 255, 0.5)");
        assertEquals(128 / 255.0, r(c), EPSILON);
        assertEquals(0.0, g(c), EPSILON);
        assertEquals(1.0, b(c), EPSILON);
        assertEquals(0.5, a(c), EPSILON);
    }

    @Test
    void testRgbaAcceptsThreeArgs() {
        // CSS Color 4: rgba() is an alias of rgb(), so 3 args is valid.
        Color c = Colors.color("rgba(255, 0, 0)");
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.0, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
        assertEquals(1.0, a(c), EPSILON);
    }

    @Test
    void testRgbaWithOpacityMultiplied() {
        Color c = Colors.color("rgba(255, 0, 0, 0.5)", 0.5);
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.25, a(c), EPSILON);
    }

    @Test
    void testRgbaPercentages() {
        Color c = Colors.color("rgba(100%, 50%, 0%, 0.8)");
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.5, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
        assertEquals(0.8, a(c), EPSILON);
    }

    // ── hsl() ─────────────────────────────────────────────────────────

    @Test
    void testHslAchromatic() {
        Color c = Colors.color("hsl(0, 0%, 75%)");
        assertEquals(0.75, r(c), EPSILON);
        assertEquals(0.75, g(c), EPSILON);
        assertEquals(0.75, b(c), EPSILON);
    }

    @Test
    void testHslLowLightness() {
        Color c = Colors.color("hsl(0, 100%, 25%)");
        assertEquals(0.5, r(c), EPSILON);
        assertEquals(0.0, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
    }

    @Test
    void testHslHighLightness() {
        Color c = Colors.color("hsl(0, 50%, 75%)");
        assertTrue(r(c) > 0.5);
        assertTrue(g(c) > 0.3);
    }

    @Test
    void testHslCyan() {
        Color c = Colors.color("hsl(180, 100%, 50%)");
        assertEquals(0.0, r(c), EPSILON);
        assertEquals(1.0, g(c), EPSILON);
        assertEquals(1.0, b(c), EPSILON);
    }

    @Test
    void testHslMagenta() {
        Color c = Colors.color("hsl(300, 100%, 50%)");
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.0, g(c), EPSILON);
        assertEquals(1.0, b(c), EPSILON);
    }

    @Test
    void testHslNegativeHue() {
        Color c = Colors.color("hsl(-60, 100%, 50%)");
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.0, g(c), EPSILON);
        assertEquals(1.0, b(c), EPSILON);
    }

    @Test
    void testHslLargeHue() {
        Color c = Colors.color("hsl(420, 100%, 50%)");
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(1.0, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
    }

    @Test
    void testHslWrongPartCount() {
        Color c = Colors.color("hsl(120, 100%)");
        assertEquals(Colors.BLACK, c);
    }

    @Test
    void testHslAcceptsBareNumbers() {
        // CSS Color 4 permits hsl(h s l) without explicit percent signs;
        // bare 50 is interpreted as 50% for saturation/lightness.
        Color c = Colors.color("hsl(0, 50, 50)");
        assertEquals(0.749, r(c), 0.01);
        assertEquals(0.251, g(c), 0.01);
        assertEquals(0.251, b(c), 0.01);
    }

    @Test
    void testHslSpaceSeparatedWithHueUnitsAndSlashAlpha() {
        assertColor(Colors.color("hsl(0.5turn 100% 50% / 25%)"), 0.0, 1.0, 1.0, 0.25);
        assertColor(Colors.color("hsl(3.141592653589793rad 100% 50%)"), 0.0, 1.0, 1.0, 1.0);
        assertColor(Colors.color("hsl(200grad 100% 50%)"), 0.0, 1.0, 1.0, 1.0);
    }

    // ── hsla() ────────────────────────────────────────────────────────

    @Test
    void testHslaValid() {
        Color c = Colors.color("hsla(120, 100%, 50%, 0.5)");
        assertEquals(0.0, r(c), EPSILON);
        assertEquals(1.0, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
        assertEquals(0.5, a(c), EPSILON);
    }

    @Test
    void testHslaAcceptsThreeArgs() {
        // CSS Color 4: hsla() is an alias of hsl(), so 3 args is valid.
        Color c = Colors.color("hsla(120, 100%, 50%)");
        assertEquals(0.0, r(c), EPSILON);
        assertEquals(1.0, g(c), EPSILON);
        assertEquals(0.0, b(c), EPSILON);
        assertEquals(1.0, a(c), EPSILON);
    }

    @Test
    void testHslaWithOpacity() {
        Color c = Colors.color("hsla(0, 100%, 50%, 0.8)", 0.5);
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(0.4, a(c), EPSILON);
    }

    // ── CSS Color Level 4 ─────────────────────────────────────────────

    @Test
    void testHwb() {
        assertColor(Colors.color("hwb(120 0% 0%)"), 0.0, 1.0, 0.0, 1.0);
        assertColor(Colors.color("hwb(0 20% 30% / 50%)"), 0.7, 0.2, 0.2, 0.5);
        assertColor(Colors.color("hwb(0 100% 100%)"), 0.5, 0.5, 0.5, 1.0);
    }

    @Test
    void testLabAndLch() {
        assertColor(Colors.color("lab(50% 0 0 / 75%)"), 0.47, 0.47, 0.47, 0.75);
        assertColor(Colors.color("lch(50% 0 120)"), 0.47, 0.47, 0.47, 1.0);
        assertColor(Colors.color("lab(54.291% 80.812 69.885)"), 1.0, 0.0, 0.0, 1.0);
        assertColor(Colors.color("lch(54.291% 106.839 40.853)"), 1.0, 0.0, 0.0, 1.0);
    }

    @Test
    void testOklabAndOklch() {
        assertColor(Colors.color("oklab(0.5 0 0 / 75%)"), 0.39, 0.39, 0.39, 0.75);
        assertColor(Colors.color("oklch(0.5 0 120)"), 0.39, 0.39, 0.39, 1.0);
        assertColor(Colors.color("oklab(62.796% 0.22486 0.12585)"), 1.0, 0.0, 0.0, 1.0);
        assertColor(Colors.color("oklch(62.796% 0.25768 29.234deg)"), 1.0, 0.0, 0.0, 1.0);
    }

    @Test
    void testColorFunction() {
        assertColor(Colors.color("color(srgb 25% 50% 75% / 25%)"), 0.25, 0.5, 0.75, 0.25);
        assertColor(Colors.color("color(srgb-linear 0.214041 0.214041 0.214041)"), 0.5, 0.5, 0.5, 1.0);
        assertColor(Colors.color("color(xyz-d65 0.95047 1 1.08883)"), 1.0, 1.0, 1.0, 1.0);
        assertColor(Colors.color("color(xyz-d50 0.96422 1 0.82521)"), 1.0, 1.0, 1.0, 1.0);
    }

    @Test
    void testWideGamutColorFunctionSpaces() {
        assertColor(Colors.color("color(display-p3 1 1 1)"), 1.0, 1.0, 1.0, 1.0);
        assertColor(Colors.color("color(a98-rgb 1 1 1)"), 1.0, 1.0, 1.0, 1.0);
        assertColor(Colors.color("color(prophoto-rgb 1 1 1)"), 1.0, 1.0, 1.0, 1.0);
        assertColor(Colors.color("color(rec2020 1 1 1)"), 1.0, 1.0, 1.0, 1.0);
    }

    @Test
    void testCssColorLevel4ReferenceVectors() {
        assertColor(Colors.color("hwb(210 10% 20% / 0.6)"), 0.100000, 0.450000, 0.800000, 0.600000);
        assertColor(Colors.color("lab(29.2345% 39.3825 20.0664 / 0.75)"), 0.490634, 0.138671, 0.159006, 0.750000);
        assertColor(Colors.color("lch(52.2345% 72.2 56.2 / 0.4)"), 0.776174, 0.363390, 0.024548, 0.400000);
        assertColor(Colors.color("oklab(0.65 0.12 -0.08 / 0.5)"), 0.736052, 0.427375, 0.742813, 0.500000);
        assertColor(Colors.color("oklch(0.72 0.15 145deg / 0.8)"), 0.381141, 0.739946, 0.402384, 0.800000);
        assertColor(Colors.color("color(srgb 0.2 0.4 0.6 / 0.7)"), 0.200000, 0.400000, 0.600000, 0.700000);
        assertColor(Colors.color("color(srgb-linear 0.0331048 0.132868 0.318547 / 0.9)"), 0.200000, 0.400000, 0.600000,
                0.900000);
        assertColor(Colors.color("color(display-p3 0.8 0.2 0.4 / 0.65)"), 0.871509, 0.093899, 0.397991, 0.650000);
        assertColor(Colors.color("color(a98-rgb 0.7 0.3 0.5 / 0.55)"), 0.803625, 0.295040, 0.510684, 0.550000);
        assertColor(Colors.color("color(prophoto-rgb 0.6 0.3 0.1 / 0.45)"), 0.866626, 0.247179, 0.000000, 0.450000);
        assertColor(Colors.color("color(rec2020 0.7 0.4 0.2 / 0.35)"), 0.860803, 0.402047, 0.208319, 0.350000);
        assertColor(Colors.color("color(xyz-d65 0.25 0.4 0.1 / 0.25)"), 0.417450, 0.743356, 0.215123, 0.250000);
        assertColor(Colors.color("color(xyz-d50 0.25 0.4 0.1 / 0.15)"), 0.327340, 0.751683, 0.286937, 0.150000);
    }

    // ── Unknown color → BLACK ─────────────────────────────────────────

    @Test
    void testUnknownColor() {
        Color c = Colors.color("notacolor");
        assertEquals(Colors.BLACK, c);
    }

    // ── negateColor ───────────────────────────────────────────────────

    @Test
    void testNegateColorPartial() {
        Color c = rgba(0.2, 0.3, 0.4, 1.0).negate();
        assertEquals(0.8, r(c), EPSILON);
        assertEquals(0.7, g(c), EPSILON);
        assertEquals(0.6, b(c), EPSILON);
        assertEquals(1.0, a(c), EPSILON);
    }

    @Test
    void testNegateBlack() {
        Color c = Colors.BLACK.negate();
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(1.0, g(c), EPSILON);
        assertEquals(1.0, b(c), EPSILON);
    }

    // ── color(String) overload ────────────────────────────────────────

    @Test
    void testColorDefaultOpacity() {
        Color c = Colors.color("#ff0000");
        assertEquals(1.0, r(c), EPSILON);
        assertEquals(1.0, a(c), EPSILON);
    }

    // ── Rendering-based color tests ───────────────────────────────────

    @Test
    void testHslColorRendering() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="0" y="0" width="100" height="100" fill="hsl(0, 100%, 50%)"/>
                </svg>
                """;
        var img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0, 5);
    }

    @Test
    void testRgbaColorRendering() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="0" y="0" width="100" height="100" fill="rgba(0, 0, 255, 0.5)"/>
                </svg>
                """;
        var img = RenderTestHelper.render(svg);
        int[] pixel = RenderTestHelper.rgba(img, 50, 50);
        assertTrue(pixel[2] > 100, "Should have blue channel");
    }

    @Test
    void testHslaColorRendering() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="0" y="0" width="100" height="100" fill="hsla(120, 100%, 50%, 0.8)"/>
                </svg>
                """;
        var img = RenderTestHelper.render(svg);
        int[] pixel = RenderTestHelper.rgba(img, 50, 50);
        assertTrue(pixel[1] > 150, "Should have green channel");
    }

    @Test
    void testOklchColorRendering() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="0" y="0" width="100" height="100" fill="oklch(62.796% 0.25768 29.234deg)"/>
                </svg>
                """;
        var img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0, 5);
    }
}

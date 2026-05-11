package io.brunoborges.jairosvg;

import io.brunoborges.jairosvg.css.Colors;
import io.brunoborges.jairosvg.css.Colors.RGBA;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorsTest {

    private static final double EPSILON = 0.01;

    private static void assertColor(RGBA color, double r, double g, double b, double a) {
        assertEquals(r, color.r(), EPSILON);
        assertEquals(g, color.g(), EPSILON);
        assertEquals(b, color.b(), EPSILON);
        assertEquals(a, color.a(), EPSILON);
    }

    // ── Basic named colors ────────────────────────────────────────────

    @Test
    void testColorParsing() {
        assertEquals(new RGBA(1, 0, 0, 1), Colors.color("red"));
        assertEquals(new RGBA(0, 0, 0, 1), Colors.color("black"));
        assertEquals(new RGBA(1, 1, 1, 1), Colors.color("white"));
        assertEquals(new RGBA(0, 0, 0, 0), Colors.color("transparent"));
        assertEquals(new RGBA(1, 0, 0, 1), Colors.color("#ff0000"));
        assertEquals(new RGBA(1, 0, 0, 1), Colors.color("#f00"));
        assertEquals(new RGBA(1, 0, 0, 1), Colors.color("rgb(255, 0, 0)"));
        assertEquals(new RGBA(1, 0, 0, 1), Colors.color("rgba(255, 0, 0, 1)"));

        Colors.RGBA hslRed = Colors.color("hsl(0, 100%, 50%)");
        assertEquals(1.0, hslRed.r(), EPSILON);
        assertEquals(0.0, hslRed.g(), EPSILON);
        assertEquals(0.0, hslRed.b(), EPSILON);
        assertEquals(1.0, hslRed.a(), EPSILON);

        Colors.RGBA hslGreen = Colors.color("hsl(120, 100%, 50%)");
        assertEquals(0.0, hslGreen.r(), EPSILON);
        assertEquals(1.0, hslGreen.g(), EPSILON);
        assertEquals(0.0, hslGreen.b(), EPSILON);

        Colors.RGBA hslBlue = Colors.color("hsl(240, 100%, 50%)");
        assertEquals(0.0, hslBlue.r(), EPSILON);
        assertEquals(0.0, hslBlue.g(), EPSILON);
        assertEquals(1.0, hslBlue.b(), EPSILON);

        Colors.RGBA hslGray = Colors.color("hsl(0, 0%, 50%)");
        assertEquals(0.5, hslGray.r(), EPSILON);
        assertEquals(0.5, hslGray.g(), EPSILON);
        assertEquals(0.5, hslGray.b(), EPSILON);

        Colors.RGBA hslaRed = Colors.color("hsla(0, 100%, 50%, 0.5)");
        assertEquals(1.0, hslaRed.r(), EPSILON);
        assertEquals(0.0, hslaRed.g(), EPSILON);
        assertEquals(0.0, hslaRed.b(), EPSILON);
        assertEquals(0.5, hslaRed.a(), EPSILON);

        Colors.RGBA hslRedWithOpacity = Colors.color("hsl(0, 100%, 50%)", 0.5);
        assertEquals(1.0, hslRedWithOpacity.r(), EPSILON);
        assertEquals(0.0, hslRedWithOpacity.g(), EPSILON);
        assertEquals(0.0, hslRedWithOpacity.b(), EPSILON);
        assertEquals(0.5, hslRedWithOpacity.a(), EPSILON);

        Colors.RGBA hslaRedWithOpacity = Colors.color("hsla(0, 100%, 50%, 0.5)", 0.5);
        assertEquals(1.0, hslaRedWithOpacity.r(), EPSILON);
        assertEquals(0.0, hslaRedWithOpacity.g(), EPSILON);
        assertEquals(0.0, hslaRedWithOpacity.b(), EPSILON);
        assertEquals(0.25, hslaRedWithOpacity.a(), EPSILON);
    }

    @Test
    void testColorNegate() {
        RGBA red = new RGBA(1, 0, 0, 1);
        RGBA negated = Colors.negateColor(red);
        assertEquals(0, negated.r(), 0.001);
        assertEquals(1, negated.g(), 0.001);
        assertEquals(1, negated.b(), 0.001);
        assertEquals(1, negated.a(), 0.001);
    }

    // ── null / blank / empty ──────────────────────────────────────────

    @Test
    void testColorNull() {
        assertEquals(RGBA.TRANSPARENT, Colors.color(null));
    }

    @Test
    void testColorEmpty() {
        assertEquals(RGBA.TRANSPARENT, Colors.color(""));
    }

    @Test
    void testColorBlank() {
        assertEquals(RGBA.TRANSPARENT, Colors.color("   "));
    }

    // ── Hex colors ────────────────────────────────────────────────────

    @Test
    void testHex6() {
        RGBA c = Colors.color("#00ff00");
        assertEquals(0.0, c.r(), EPSILON);
        assertEquals(1.0, c.g(), EPSILON);
        assertEquals(0.0, c.b(), EPSILON);
        assertEquals(1.0, c.a(), EPSILON);
    }

    @Test
    void testHex3() {
        RGBA c = Colors.color("#0f0");
        assertEquals(0.0, c.r(), EPSILON);
        assertEquals(1.0, c.g(), EPSILON);
        assertEquals(0.0, c.b(), EPSILON);
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
        RGBA c = Colors.color("#ff0000", 0.5);
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.5, c.a(), EPSILON);
    }

    @Test
    void testHex3WithOpacity() {
        RGBA c = Colors.color("#f00", 0.5);
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.5, c.a(), EPSILON);
    }

    @Test
    void testHexInvalidLength() {
        RGBA c = Colors.color("#ff");
        assertEquals(RGBA.BLACK, c);
    }

    @Test
    void testHex5Chars() {
        RGBA c = Colors.color("#ff00f");
        assertEquals(RGBA.BLACK, c);
    }

    // ── Named colors with opacity ─────────────────────────────────────

    @Test
    void testNamedColorWithOpacity() {
        RGBA c = Colors.color("red", 0.5);
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.0, c.g(), EPSILON);
        assertEquals(0.0, c.b(), EPSILON);
        assertEquals(0.5, c.a(), EPSILON);
    }

    @Test
    void testNamedColorFullOpacity() {
        RGBA c = Colors.color("blue");
        assertEquals(0.0, c.r(), EPSILON);
        assertEquals(0.0, c.g(), EPSILON);
        assertEquals(1.0, c.b(), EPSILON);
        assertEquals(1.0, c.a(), EPSILON);
    }

    @Test
    void testNamedColorCaseInsensitive() {
        RGBA c = Colors.color("  RED  ");
        assertEquals(1.0, c.r(), EPSILON);
    }

    // ── rgb() ─────────────────────────────────────────────────────────

    @Test
    void testRgbIntegers() {
        RGBA c = Colors.color("rgb(0, 128, 255)");
        assertEquals(0.0, c.r(), EPSILON);
        assertEquals(128 / 255.0, c.g(), EPSILON);
        assertEquals(1.0, c.b(), EPSILON);
    }

    @Test
    void testRgbPercentages() {
        RGBA c = Colors.color("rgb(100%, 0%, 50%)");
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.0, c.g(), EPSILON);
        assertEquals(0.5, c.b(), EPSILON);
    }

    @Test
    void testRgbWrongPartCount() {
        RGBA c = Colors.color("rgb(255, 0)");
        assertEquals(RGBA.BLACK, c);
    }

    @Test
    void testRgbWithOpacity() {
        RGBA c = Colors.color("rgb(255, 0, 0)", 0.5);
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.5, c.a(), EPSILON);
    }

    @Test
    void testRgbSpaceSeparatedWithSlashAlpha() {
        assertColor(Colors.color("rgb(255 0 128 / 50%)"), 1.0, 0.0, 128 / 255.0, 0.5);
    }

    // ── rgba() ────────────────────────────────────────────────────────

    @Test
    void testRgbaValid() {
        RGBA c = Colors.color("rgba(128, 0, 255, 0.5)");
        assertEquals(128 / 255.0, c.r(), EPSILON);
        assertEquals(0.0, c.g(), EPSILON);
        assertEquals(1.0, c.b(), EPSILON);
        assertEquals(0.5, c.a(), EPSILON);
    }

    @Test
    void testRgbaWrongPartCount() {
        RGBA c = Colors.color("rgba(255, 0, 0)");
        assertEquals(RGBA.BLACK, c);
    }

    @Test
    void testRgbaWithOpacityMultiplied() {
        RGBA c = Colors.color("rgba(255, 0, 0, 0.5)", 0.5);
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.25, c.a(), EPSILON);
    }

    @Test
    void testRgbaPercentages() {
        RGBA c = Colors.color("rgba(100%, 50%, 0%, 0.8)");
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.5, c.g(), EPSILON);
        assertEquals(0.0, c.b(), EPSILON);
        assertEquals(0.8, c.a(), EPSILON);
    }

    // ── hsl() ─────────────────────────────────────────────────────────

    @Test
    void testHslAchromatic() {
        RGBA c = Colors.color("hsl(0, 0%, 75%)");
        assertEquals(0.75, c.r(), EPSILON);
        assertEquals(0.75, c.g(), EPSILON);
        assertEquals(0.75, c.b(), EPSILON);
    }

    @Test
    void testHslLowLightness() {
        RGBA c = Colors.color("hsl(0, 100%, 25%)");
        assertEquals(0.5, c.r(), EPSILON);
        assertEquals(0.0, c.g(), EPSILON);
        assertEquals(0.0, c.b(), EPSILON);
    }

    @Test
    void testHslHighLightness() {
        RGBA c = Colors.color("hsl(0, 50%, 75%)");
        assertTrue(c.r() > 0.5);
        assertTrue(c.g() > 0.3);
    }

    @Test
    void testHslCyan() {
        RGBA c = Colors.color("hsl(180, 100%, 50%)");
        assertEquals(0.0, c.r(), EPSILON);
        assertEquals(1.0, c.g(), EPSILON);
        assertEquals(1.0, c.b(), EPSILON);
    }

    @Test
    void testHslMagenta() {
        RGBA c = Colors.color("hsl(300, 100%, 50%)");
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.0, c.g(), EPSILON);
        assertEquals(1.0, c.b(), EPSILON);
    }

    @Test
    void testHslNegativeHue() {
        RGBA c = Colors.color("hsl(-60, 100%, 50%)");
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.0, c.g(), EPSILON);
        assertEquals(1.0, c.b(), EPSILON);
    }

    @Test
    void testHslLargeHue() {
        RGBA c = Colors.color("hsl(420, 100%, 50%)");
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(1.0, c.g(), EPSILON);
        assertEquals(0.0, c.b(), EPSILON);
    }

    @Test
    void testHslWrongPartCount() {
        RGBA c = Colors.color("hsl(120, 100%)");
        assertEquals(RGBA.BLACK, c);
    }

    @Test
    void testHslWithoutPercentThrows() {
        assertThrows(IllegalArgumentException.class, () -> Colors.color("hsl(0, 50, 50)"));
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
        RGBA c = Colors.color("hsla(120, 100%, 50%, 0.5)");
        assertEquals(0.0, c.r(), EPSILON);
        assertEquals(1.0, c.g(), EPSILON);
        assertEquals(0.0, c.b(), EPSILON);
        assertEquals(0.5, c.a(), EPSILON);
    }

    @Test
    void testHslaWrongPartCount() {
        RGBA c = Colors.color("hsla(120, 100%, 50%)");
        assertEquals(RGBA.BLACK, c);
    }

    @Test
    void testHslaWithOpacity() {
        RGBA c = Colors.color("hsla(0, 100%, 50%, 0.8)", 0.5);
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(0.4, c.a(), EPSILON);
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

    // ── Unknown color → BLACK ─────────────────────────────────────────

    @Test
    void testUnknownColor() {
        RGBA c = Colors.color("notacolor");
        assertEquals(RGBA.BLACK, c);
    }

    // ── negateColor ───────────────────────────────────────────────────

    @Test
    void testNegateColorPartial() {
        RGBA c = Colors.negateColor(new RGBA(0.2, 0.3, 0.4, 1.0));
        assertEquals(0.8, c.r(), EPSILON);
        assertEquals(0.7, c.g(), EPSILON);
        assertEquals(0.6, c.b(), EPSILON);
        assertEquals(1.0, c.a(), EPSILON);
    }

    @Test
    void testNegateBlack() {
        RGBA c = Colors.negateColor(RGBA.BLACK);
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(1.0, c.g(), EPSILON);
        assertEquals(1.0, c.b(), EPSILON);
    }

    // ── color(String) overload ────────────────────────────────────────

    @Test
    void testColorDefaultOpacity() {
        RGBA c = Colors.color("#ff0000");
        assertEquals(1.0, c.r(), EPSILON);
        assertEquals(1.0, c.a(), EPSILON);
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

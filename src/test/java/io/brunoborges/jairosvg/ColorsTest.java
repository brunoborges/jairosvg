package io.brunoborges.jairosvg;

import io.brunoborges.jairosvg.css.Colors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorsTest {

    @Test
    void testColorParsing() {
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("red"));
        assertEquals(new Colors.RGBA(0, 0, 0, 1), Colors.color("black"));
        assertEquals(new Colors.RGBA(1, 1, 1, 1), Colors.color("white"));
        assertEquals(new Colors.RGBA(0, 0, 0, 0), Colors.color("transparent"));
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("#ff0000"));
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("#f00"));
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("rgb(255, 0, 0)"));
        assertEquals(new Colors.RGBA(1, 0, 0, 1), Colors.color("rgba(255, 0, 0, 1)"));

        // HSL: red = hsl(0, 100%, 50%)
        Colors.RGBA hslRed = Colors.color("hsl(0, 100%, 50%)");
        assertEquals(1.0, hslRed.r(), 0.01);
        assertEquals(0.0, hslRed.g(), 0.01);
        assertEquals(0.0, hslRed.b(), 0.01);
        assertEquals(1.0, hslRed.a(), 0.01);

        // HSL: green = hsl(120, 100%, 50%)
        Colors.RGBA hslGreen = Colors.color("hsl(120, 100%, 50%)");
        assertEquals(0.0, hslGreen.r(), 0.01);
        assertEquals(1.0, hslGreen.g(), 0.01);
        assertEquals(0.0, hslGreen.b(), 0.01);

        // HSL: blue = hsl(240, 100%, 50%)
        Colors.RGBA hslBlue = Colors.color("hsl(240, 100%, 50%)");
        assertEquals(0.0, hslBlue.r(), 0.01);
        assertEquals(0.0, hslBlue.g(), 0.01);
        assertEquals(1.0, hslBlue.b(), 0.01);

        // HSL: gray = hsl(0, 0%, 50%)
        Colors.RGBA hslGray = Colors.color("hsl(0, 0%, 50%)");
        assertEquals(0.5, hslGray.r(), 0.01);
        assertEquals(0.5, hslGray.g(), 0.01);
        assertEquals(0.5, hslGray.b(), 0.01);

        // HSLA with alpha
        Colors.RGBA hslaRed = Colors.color("hsla(0, 100%, 50%, 0.5)");
        assertEquals(1.0, hslaRed.r(), 0.01);
        assertEquals(0.0, hslaRed.g(), 0.01);
        assertEquals(0.0, hslaRed.b(), 0.01);
        assertEquals(0.5, hslaRed.a(), 0.01);

        // Verify opacity parameter interaction for HSL/HSLA
        Colors.RGBA hslRedWithOpacity = Colors.color("hsl(0, 100%, 50%)", 0.5);
        assertEquals(1.0, hslRedWithOpacity.r(), 0.01);
        assertEquals(0.0, hslRedWithOpacity.g(), 0.01);
        assertEquals(0.0, hslRedWithOpacity.b(), 0.01);
        assertEquals(0.5, hslRedWithOpacity.a(), 0.01);

        Colors.RGBA hslaRedWithOpacity = Colors.color("hsla(0, 100%, 50%, 0.5)", 0.5);
        assertEquals(1.0, hslaRedWithOpacity.r(), 0.01);
        assertEquals(0.0, hslaRedWithOpacity.g(), 0.01);
        assertEquals(0.0, hslaRedWithOpacity.b(), 0.01);
        // hsla alpha (0.5) multiplied by opacity (0.5) => 0.25
        assertEquals(0.25, hslaRedWithOpacity.a(), 0.01);
    }

    @Test
    void testColorNegate() {
        Colors.RGBA red = new Colors.RGBA(1, 0, 0, 1);
        Colors.RGBA negated = Colors.negateColor(red);
        assertEquals(0, negated.r(), 0.001);
        assertEquals(1, negated.g(), 0.001);
        assertEquals(1, negated.b(), 0.001);
        assertEquals(1, negated.a(), 0.001);
    }
}

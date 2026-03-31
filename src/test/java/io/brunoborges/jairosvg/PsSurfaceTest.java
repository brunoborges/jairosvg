package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for PostScript / EPS output via {@link JairoSVG}.
 */
class PsSurfaceTest {

    private static final String SIMPLE_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect width="100" height="100" fill="red"/>
            </svg>
            """;

    @Test
    void toPsProducesNonEmptyOutput() throws Exception {
        byte[] ps = JairoSVG.builder().fromString(SIMPLE_SVG).toPs();
        assertNotNull(ps);
        assertTrue(ps.length > 0, "PS output should be non-empty");
    }

    @Test
    void toPsOutputContainsPostScriptOrPngFallback() throws Exception {
        byte[] ps = JairoSVG.builder().fromString(SIMPLE_SVG).toPs();
        assertNotNull(ps);
        // The output should be either PostScript (%!PS header) or PNG fallback
        boolean isPostScript = ps.length > 4 && ps[0] == '%' && ps[1] == '!';
        boolean isPng = ps.length > 8 && (ps[1] & 0xFF) == 'P' && (ps[2] & 0xFF) == 'N' && (ps[3] & 0xFF) == 'G';
        assertTrue(isPostScript || isPng, "Output should be PostScript or PNG fallback");
    }

    @Test
    void toEpsProducesOutput() throws Exception {
        byte[] eps = JairoSVG.builder().fromString(SIMPLE_SVG).toEps();
        assertNotNull(eps);
        assertTrue(eps.length > 0, "EPS output should be non-empty");
    }

    @Test
    void toPsWithGradient() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <linearGradient id="g1"><stop offset="0" stop-color="red"/>
                    <stop offset="1" stop-color="blue"/></linearGradient>
                  </defs>
                  <rect width="100" height="100" fill="url(#g1)"/>
                </svg>
                """;
        byte[] ps = JairoSVG.builder().fromString(svg).toPs();
        assertNotNull(ps);
        assertTrue(ps.length > 0);
    }

    @Test
    void toPsWithText() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="50">
                  <text x="10" y="30" font-size="20" fill="black">Hello</text>
                </svg>
                """;
        byte[] ps = JairoSVG.builder().fromString(svg).toPs();
        assertNotNull(ps);
        assertTrue(ps.length > 0);
    }

    @Test
    void toPsWithCustomDimensions() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="300" height="200">
                  <circle cx="150" cy="100" r="80" fill="green"/>
                </svg>
                """;
        byte[] ps = JairoSVG.builder().fromString(svg).toPs();
        assertNotNull(ps);
        assertTrue(ps.length > 0);
    }
}

package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.brunoborges.jairosvg.util.Features;

/**
 * Unit tests for {@link Features} — SVG 1.1 conditional processing.
 */
class FeaturesTest {

    // ── hasFeatures ──────────────────────────────────────────────────────

    @Test
    void hasFeatures_null_returnsTrue() {
        assertTrue(Features.hasFeatures(null));
    }

    @Test
    void hasFeatures_empty_returnsTrue() {
        assertTrue(Features.hasFeatures(""));
        assertTrue(Features.hasFeatures("   "));
    }

    @Test
    void hasFeatures_singleSupported() {
        assertTrue(Features.hasFeatures("http://www.w3.org/TR/SVG11/feature#SVG"));
        assertTrue(Features.hasFeatures("http://www.w3.org/TR/SVG11/feature#Shape"));
        assertTrue(Features.hasFeatures("http://www.w3.org/TR/SVG11/feature#BasicText"));
        assertTrue(Features.hasFeatures("http://www.w3.org/TR/SVG11/feature#Gradient"));
    }

    @Test
    void hasFeatures_multipleSupported() {
        assertTrue(Features
                .hasFeatures("http://www.w3.org/TR/SVG11/feature#SVG http://www.w3.org/TR/SVG11/feature#Shape"));
    }

    @Test
    void hasFeatures_unsupported() {
        assertFalse(Features.hasFeatures("http://www.w3.org/TR/SVG11/feature#Animation"));
        assertFalse(Features.hasFeatures("http://example.com/nonexistent"));
    }

    @Test
    void hasFeatures_mixedSupportedAndUnsupported() {
        assertFalse(Features
                .hasFeatures("http://www.w3.org/TR/SVG11/feature#SVG http://www.w3.org/TR/SVG11/feature#Animation"));
    }

    // ── supportLanguages ─────────────────────────────────────────────────

    @Test
    void supportLanguages_null_returnsTrue() {
        assertTrue(Features.supportLanguages(null));
    }

    @Test
    void supportLanguages_matchingLocale() {
        // Get current locale and test with its prefix
        String locale = java.util.Locale.getDefault().toString().replace('_', '-');
        String prefix = locale.length() >= 2 ? locale.substring(0, 2) : locale;
        assertTrue(Features.supportLanguages(prefix));
    }

    @Test
    void supportLanguages_noMatch() {
        // Use a very unlikely language tag
        assertFalse(Features.supportLanguages("xx-NONEXISTENT"));
    }

    @Test
    void supportLanguages_commaList_oneMatches() {
        String locale = java.util.Locale.getDefault().toString().replace('_', '-');
        String prefix = locale.length() >= 2 ? locale.substring(0, 2) : locale;
        assertTrue(Features.supportLanguages("xx-FAKE, " + prefix + ", yy-FAKE"));
    }

    // ── matchFeatures ────────────────────────────────────────────────────

    @Test
    void matchFeatures_allNull_returnsTrue() {
        assertTrue(Features.matchFeatures(null, null, null));
    }

    @Test
    void matchFeatures_requiredExtensions_returnsFalse() {
        assertFalse(Features.matchFeatures(null, "http://example.com/ext", null));
    }

    @Test
    void matchFeatures_supportedFeature_returnsTrue() {
        assertTrue(Features.matchFeatures("http://www.w3.org/TR/SVG11/feature#Shape", null, null));
    }

    @Test
    void matchFeatures_unsupportedFeature_returnsFalse() {
        assertFalse(Features.matchFeatures("http://www.w3.org/TR/SVG11/feature#Animation", null, null));
    }

    @Test
    void matchFeatures_matchingLanguage_returnsTrue() {
        String locale = java.util.Locale.getDefault().toString().replace('_', '-');
        String prefix = locale.length() >= 2 ? locale.substring(0, 2) : locale;
        assertTrue(Features.matchFeatures(null, null, prefix));
    }

    @Test
    void matchFeatures_nonMatchingLanguage_returnsFalse() {
        assertFalse(Features.matchFeatures(null, null, "xx-NONEXISTENT"));
    }

    @Test
    void matchFeatures_extensionsTakePrecedence() {
        // Even with valid features and language, extensions cause failure
        String locale = java.util.Locale.getDefault().toString().replace('_', '-');
        String prefix = locale.length() >= 2 ? locale.substring(0, 2) : locale;
        assertFalse(Features.matchFeatures("http://www.w3.org/TR/SVG11/feature#SVG", "http://example.com/ext", prefix));
    }

    // ── Integration: conditional rendering ───────────────────────────────

    @Test
    void conditionalRendering_switchWithFeatures() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <switch>
                    <rect requiredFeatures="http://www.w3.org/TR/SVG11/feature#Shape"
                          width="100" height="100" fill="green"/>
                    <rect width="100" height="100" fill="red"/>
                  </switch>
                </svg>
                """;
        var img = RenderTestHelper.render(svg);
        // Should render green (supported feature), not red
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 128, 0);
    }

    @Test
    void conditionalRendering_switchWithUnsupportedFeature() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <switch>
                    <rect requiredFeatures="http://www.w3.org/TR/SVG11/feature#Animation"
                          width="100" height="100" fill="red"/>
                    <rect width="100" height="100" fill="blue"/>
                  </switch>
                </svg>
                """;
        var img = RenderTestHelper.render(svg);
        // Should render blue (fallback)
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 255);
    }
}

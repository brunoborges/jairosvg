package com.jairosvg;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a parsed SVG font with its glyphs.
 * Supports {@code <font>}, {@code <font-face>}, {@code <glyph>}, and {@code <missing-glyph>} elements.
 *
 * @see <a href="https://www.w3.org/TR/SVG11/fonts.html">SVG 1.1 — Fonts</a>
 */
final class SvgFont {

    /** Font family name from {@code <font-face font-family="...">}. */
    final String family;

    /** Default horizontal advance from {@code <font horiz-adv-x="...">}. */
    final double defaultHorizAdvX;

    /** Units per em from {@code <font-face units-per-em="...">}. Defaults to 1000. */
    final double unitsPerEm;

    /** Ascent from {@code <font-face ascent="...">}. */
    final double ascent;

    /** Descent from {@code <font-face descent="...">}. */
    final double descent;

    /** Glyph map: Unicode character(s) → Glyph data. */
    final Map<String, Glyph> glyphs;

    /** Missing glyph (fallback). */
    final Glyph missingGlyph;

    /** Maximum length of any unicode key in the glyph map (for greedy matching). */
    final int maxUnicodeLen;

    SvgFont(String family, double defaultHorizAdvX, double unitsPerEm,
            double ascent, double descent,
            Map<String, Glyph> glyphs, Glyph missingGlyph) {
        this.family = family;
        this.defaultHorizAdvX = defaultHorizAdvX;
        this.unitsPerEm = unitsPerEm;
        this.ascent = ascent;
        this.descent = descent;
        this.glyphs = glyphs;
        this.missingGlyph = missingGlyph;
        this.maxUnicodeLen = glyphs.keySet().stream()
            .mapToInt(String::length).max().orElse(1);
    }

    /** A single glyph definition with a pre-parsed cached path. */
    record Glyph(GeneralPath cachedPath, double horizAdvX) {}

    /**
     * Parse an SVG font from a {@code <font>} node.
     */
    static SvgFont parse(Node fontNode) {
        double defaultHorizAdvX = parseDouble(fontNode.get("horiz-adv-x"), 1000);

        // Find <font-face>
        String family = null;
        double unitsPerEm = 1000;
        double ascent = 800;
        double descent = -200;

        for (Node child : fontNode.children) {
            if ("font-face".equals(child.tag)) {
                family = child.get("font-family");
                unitsPerEm = parseDouble(child.get("units-per-em"), 1000);
                ascent = parseDouble(child.get("ascent"), unitsPerEm * 0.8);
                descent = parseDouble(child.get("descent"), -unitsPerEm * 0.2);
                break;
            }
        }

        if (family == null) {
            // Try the font node's id as fallback family name
            family = fontNode.get("id");
        }
        if (family == null) return null;

        // Parse glyphs — path data is parsed once and cached in the Glyph record
        Map<String, Glyph> glyphs = new LinkedHashMap<>();
        Glyph missingGlyph = null;

        for (Node child : fontNode.children) {
            if ("glyph".equals(child.tag)) {
                String unicode = child.get("unicode");
                if (unicode == null || unicode.isEmpty()) continue;
                String d = child.get("d", "");
                double advX = parseDouble(child.get("horiz-adv-x"), defaultHorizAdvX);
                glyphs.put(unicode, new Glyph(parsePathData(d), advX));
            } else if ("missing-glyph".equals(child.tag)) {
                String d = child.get("d", "");
                double advX = parseDouble(child.get("horiz-adv-x"), defaultHorizAdvX);
                missingGlyph = new Glyph(parsePathData(d), advX);
            }
        }

        return new SvgFont(family, defaultHorizAdvX, unitsPerEm, ascent, descent,
                           glyphs, missingGlyph);
    }

    /**
     * Build a GeneralPath for a glyph, scaled to the given font size.
     * SVG font glyphs are defined with y-axis pointing up (origin at baseline),
     * so we flip vertically around the baseline.
     * Uses the pre-parsed cached path from the Glyph record.
     */
    GeneralPath buildGlyphPath(Glyph glyph, double fontSize, double xOffset, double yOffset) {
        if (glyph == null || glyph.cachedPath() == null) return null;

        double scale = fontSize / unitsPerEm;

        // SVG fonts: y=0 is baseline, positive y is up.
        // Screen: y increases downward, yOffset is the baseline.
        // Flip y-axis and scale to font size.
        AffineTransform transform = new AffineTransform();
        transform.translate(xOffset, yOffset);
        transform.scale(scale, -scale);

        GeneralPath result = new GeneralPath();
        result.append(glyph.cachedPath().getPathIterator(transform), false);
        return result;
    }

    /** Get the scaled horizontal advance for a glyph. */
    double getAdvance(Glyph glyph, double fontSize) {
        double advX = glyph != null ? glyph.horizAdvX() : defaultHorizAdvX;
        return advX * fontSize / unitsPerEm;
    }

    /** Result of a glyph lookup: the matched glyph and how many chars were consumed. */
    record GlyphMatch(Glyph glyph, int charsConsumed) {}

    /**
     * Look up a glyph starting at {@code offset} in {@code text} using greedy longest-match.
     * Handles multi-character unicode values and supplementary (non-BMP) code points.
     */
    GlyphMatch getGlyph(String text, int offset) {
        // Try longest match first
        int remaining = text.length() - offset;
        for (int len = Math.min(maxUnicodeLen, remaining); len > 0; len--) {
            String candidate = text.substring(offset, offset + len);
            Glyph g = glyphs.get(candidate);
            if (g != null) return new GlyphMatch(g, len);
        }
        // No match — consume one code point and use missing glyph
        int cp = text.codePointAt(offset);
        int cpLen = Character.charCount(cp);
        return new GlyphMatch(missingGlyph, cpLen);
    }

    /** Parse SVG path data string into a GeneralPath. Returns null for empty/blank data. */
    private static GeneralPath parsePathData(String d) {
        if (d == null || d.isBlank()) return null;
        try {
            // Create a minimal node with the path data and use PathDrawer
            Node tempNode = new Node();
            tempNode.tag = "path";
            tempNode.set("d", d);

            Surface tempSurface = new Surface();
            tempSurface.path = new GeneralPath();
            tempSurface.contextWidth = 1000;
            tempSurface.contextHeight = 1000;
            tempSurface.dpi = 96;
            tempSurface.fontSize = 12;

            PathDrawer.path(tempSurface, tempNode);
            return tempSurface.path;
        } catch (Exception e) {
            return null;
        }
    }

    private static double parseDouble(String s, double def) {
        if (s == null) return def;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

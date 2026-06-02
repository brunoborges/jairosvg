package io.brunoborges.jairosvg.css;

import in.virit.color.Color;
import in.virit.color.RgbColor;

import java.util.Locale;
import java.util.Map;

/**
 * SVG color parsing. Returns {@link Color} instances from the
 * {@code in.virit:color} library — typically {@link RgbColor}.
 *
 * <p>
 * All CSS color syntax — {@code #hex} (including the {@code #RGBA} short form),
 * named colors (case-insensitive) and the functional notations
 * ({@code rgb(), hsl(), hwb(), lab(), lch(), oklab(), oklch(), color()}) — is
 * delegated to {@link Color#parseCssColor} via its lenient
 * {@link Color#tryParseCssColor} entry point. The library owns the parsing,
 * the CSS named-color table and CSS Color 4 conversions; this class only adds
 * the handful of SVG-specific keywords that are <em>not</em> CSS colors.
 *
 * <p>
 * The internal representation is {@link RgbColor} (int 0..255 + double alpha)
 * because the SVG rendering target is 8-bit PNG/AWT, so sub-byte precision in
 * intermediate values is irrelevant.
 */
public final class Colors {

    /** Fully transparent black. */
    public static final RgbColor TRANSPARENT = new RgbColor(0, 0, 0, 0);

    /** Fully opaque black. */
    public static final RgbColor BLACK = new RgbColor(0, 0, 0, 1);

    /**
     * Keywords accepted on SVG fill/stroke attributes that are not CSS colors,
     * so the color library does not (and should not) resolve them:
     * <ul>
     * <li>{@code none} — SVG's "no paint" keyword, treated here as fully
     * transparent.</li>
     * <li>the legacy CSS2 system colors — deprecated in CSS Color 4 but still
     * found in old SVGs; resolved to the values browsers historically used.</li>
     * </ul>
     * Looked up in lowercase, mirroring CSS keyword case-insensitivity.
     */
    private static final Map<String, RgbColor> SVG_KEYWORDS = Map.ofEntries(
            Map.entry("none", TRANSPARENT),
            Map.entry("activeborder", new RgbColor(0, 0, 255)),
            Map.entry("activecaption", new RgbColor(0, 0, 255)),
            Map.entry("appworkspace", new RgbColor(255, 255, 255)),
            Map.entry("background", new RgbColor(255, 255, 255)),
            Map.entry("buttonface", new RgbColor(0, 0, 0)),
            Map.entry("buttonhighlight", new RgbColor(204, 204, 204)),
            Map.entry("buttonshadow", new RgbColor(51, 51, 51)),
            Map.entry("buttontext", new RgbColor(0, 0, 0)),
            Map.entry("captiontext", new RgbColor(0, 0, 0)),
            Map.entry("graytext", new RgbColor(51, 51, 51)),
            Map.entry("highlight", new RgbColor(0, 0, 255)),
            Map.entry("highlighttext", new RgbColor(204, 204, 204)),
            Map.entry("inactiveborder", new RgbColor(51, 51, 51)),
            Map.entry("inactivecaption", new RgbColor(204, 204, 204)),
            Map.entry("inactivecaptiontext", new RgbColor(51, 51, 51)),
            Map.entry("infobackground", new RgbColor(204, 204, 204)),
            Map.entry("infotext", new RgbColor(0, 0, 0)),
            Map.entry("menu", new RgbColor(204, 204, 204)),
            Map.entry("menutext", new RgbColor(51, 51, 51)),
            Map.entry("scrollbar", new RgbColor(204, 204, 204)),
            Map.entry("threeddarkshadow", new RgbColor(51, 51, 51)),
            Map.entry("threedface", new RgbColor(204, 204, 204)),
            Map.entry("threedhighlight", new RgbColor(255, 255, 255)),
            Map.entry("threedlightshadow", new RgbColor(51, 51, 51)),
            Map.entry("threedshadow", new RgbColor(51, 51, 51)),
            Map.entry("window", new RgbColor(204, 204, 204)),
            Map.entry("windowframe", new RgbColor(204, 204, 204)),
            Map.entry("windowtext", new RgbColor(0, 0, 0)));

    private Colors() {
    }

    /** Parse a color string and multiply alpha by {@code opacity}. */
    public static Color color(String string, double opacity) {
        if (string == null || string.isBlank()) {
            return TRANSPARENT;
        }
        string = string.strip();

        // The library parses every real CSS color (hex, named, functional) and
        // recovers gracefully from malformed input, so it handles the common
        // case without us re-implementing anything.
        RgbColor rgb = Color.tryParseCssColor(string).map(Color::toRgbColor).orElse(null);
        if (rgb != null) {
            return applyOpacity(rgb, opacity);
        }

        // Not a CSS color — try the SVG-specific keywords ("none", system
        // colors). These never overlap with CSS named colors.
        RgbColor keyword = SVG_KEYWORDS.get(string.toLowerCase(Locale.ROOT));
        if (keyword != null) {
            return applyOpacity(keyword, opacity);
        }

        return BLACK;
    }

    private static RgbColor applyOpacity(RgbColor rgb, double opacity) {
        return opacity >= 1.0 ? rgb : rgb.withAlpha(rgb.a() * opacity);
    }

    /** Parse with default opacity of 1. */
    public static Color color(String string) {
        return color(string, 1.0);
    }
}

package io.brunoborges.jairosvg.css;

import in.virit.color.Color;
import in.virit.color.NamedColor;
import in.virit.color.RgbColor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SVG color parsing. Returns {@link Color} instances from the
 * {@code in.virit:color} library — typically {@link RgbColor} — with hex and
 * named colors taking direct fast paths and functional notation
 * ({@code rgb(), hsl(), hwb(), lab(), lch(),
 * oklab(), oklch(), color()}) delegated to the library's CSS parser.
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

    private static final Map<String, RgbColor> NAMED_COLORS = buildNamedColors();

    private Colors() {
    }

    private static Map<String, RgbColor> buildNamedColors() {
        var m = new HashMap<String, RgbColor>(192);
        // CSS named colors — single source of truth lives in in.virit:color.
        for (NamedColor nc : NamedColor.values()) {
            m.put(nc.name().toLowerCase(Locale.ROOT), nc.toRgbColor());
        }
        // British-spelling aliases that the library also carries already arrive
        // via NamedColor.values() (darkgrey, dimgrey, slategrey, ...).

        // CSS2 system colors — deprecated in CSS Color 4 but still found in
        // legacy SVGs, so resolve them to the colors browsers historically used.
        m.put("activeborder", new RgbColor(0, 0, 255));
        m.put("activecaption", new RgbColor(0, 0, 255));
        m.put("appworkspace", new RgbColor(255, 255, 255));
        m.put("background", new RgbColor(255, 255, 255));
        m.put("buttonface", new RgbColor(0, 0, 0));
        m.put("buttonhighlight", new RgbColor(204, 204, 204));
        m.put("buttonshadow", new RgbColor(51, 51, 51));
        m.put("buttontext", new RgbColor(0, 0, 0));
        m.put("captiontext", new RgbColor(0, 0, 0));
        m.put("graytext", new RgbColor(51, 51, 51));
        m.put("highlight", new RgbColor(0, 0, 255));
        m.put("highlighttext", new RgbColor(204, 204, 204));
        m.put("inactiveborder", new RgbColor(51, 51, 51));
        m.put("inactivecaption", new RgbColor(204, 204, 204));
        m.put("inactivecaptiontext", new RgbColor(51, 51, 51));
        m.put("infobackground", new RgbColor(204, 204, 204));
        m.put("infotext", new RgbColor(0, 0, 0));
        m.put("menu", new RgbColor(204, 204, 204));
        m.put("menutext", new RgbColor(51, 51, 51));
        m.put("scrollbar", new RgbColor(204, 204, 204));
        m.put("threeddarkshadow", new RgbColor(51, 51, 51));
        m.put("threedface", new RgbColor(204, 204, 204));
        m.put("threedhighlight", new RgbColor(255, 255, 255));
        m.put("threedlightshadow", new RgbColor(51, 51, 51));
        m.put("threedshadow", new RgbColor(51, 51, 51));
        m.put("window", new RgbColor(204, 204, 204));
        m.put("windowframe", new RgbColor(204, 204, 204));
        m.put("windowtext", new RgbColor(0, 0, 0));

        // SVG's "no paint" keyword — not a CSS color but routinely set on
        // fill/stroke attributes, so resolve to fully transparent here.
        m.put("none", TRANSPARENT);

        return Map.copyOf(m);
    }

    /** Parse a color string and multiply alpha by {@code opacity}. */
    public static Color color(String string, double opacity) {
        if (string == null || string.isBlank()) {
            return TRANSPARENT;
        }

        string = string.strip();

        // Fast-path: hex color — skip toLowerCase() since parseInt handles case
        if (string.charAt(0) == '#') {
            int len = string.length();
            if (len == 9) {
                int r = Integer.parseInt(string, 1, 3, 16);
                int g = Integer.parseInt(string, 3, 5, 16);
                int b = Integer.parseInt(string, 5, 7, 16);
                double a = Integer.parseInt(string, 7, 9, 16) / 255.0;
                return new RgbColor(r, g, b, a * opacity);
            }
            if (len == 7) {
                int r = Integer.parseInt(string, 1, 3, 16);
                int g = Integer.parseInt(string, 3, 5, 16);
                int b = Integer.parseInt(string, 5, 7, 16);
                return new RgbColor(r, g, b, opacity);
            }
            if (len == 5) {
                int r = Character.digit(string.charAt(1), 16) * 17;
                int g = Character.digit(string.charAt(2), 16) * 17;
                int b = Character.digit(string.charAt(3), 16) * 17;
                double a = Character.digit(string.charAt(4), 16) / 15.0;
                return new RgbColor(r, g, b, a * opacity);
            }
            if (len == 4) {
                int r = Character.digit(string.charAt(1), 16) * 17;
                int g = Character.digit(string.charAt(2), 16) * 17;
                int b = Character.digit(string.charAt(3), 16) * 17;
                return new RgbColor(r, g, b, opacity);
            }
            return BLACK;
        }

        string = string.toLowerCase();

        // Named color lookup
        RgbColor named = NAMED_COLORS.get(string);
        if (named != null) {
            return (opacity >= 1.0 && named.a() >= 1.0)
                    ? named
                    : new RgbColor(named.r(), named.g(), named.b(), named.a() * opacity);
        }

        // Functional notation — delegated to the in.virit:color library.
        // Lenient parse so malformed input falls back to BLACK instead of
        // aborting the render.
        if (isFunctionalNotation(string)) {
            return Color.tryParseCssColor(string).map(Color::toRgbColor)
                    .map(rgb -> opacity >= 1.0 ? rgb : rgb.withAlpha(rgb.a() * opacity)).orElse(BLACK);
        }

        return BLACK;
    }

    private static boolean isFunctionalNotation(String s) {
        return s.startsWith("rgb") || s.startsWith("hsl") || s.startsWith("hwb") || s.startsWith("lab")
                || s.startsWith("lch") || s.startsWith("oklab") || s.startsWith("oklch") || s.startsWith("color(");
    }

    /** Parse with default opacity of 1. */
    public static Color color(String string) {
        return color(string, 1.0);
    }

    /** Negate (complement) a color in sRGB. Preserves alpha. */
    public static Color negateColor(Color c) {
        RgbColor rgb = c.toRgbColor();
        return new RgbColor(255 - rgb.r(), 255 - rgb.g(), 255 - rgb.b(), rgb.a());
    }
}

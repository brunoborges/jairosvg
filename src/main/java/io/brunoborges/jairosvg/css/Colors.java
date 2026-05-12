package io.brunoborges.jairosvg.css;

import in.virit.color.Color;
import in.virit.color.RgbColor;

import java.util.HashMap;
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

    @SuppressWarnings("java:S1192") // color values are intentionally repeated
    private static Map<String, RgbColor> buildNamedColors() {
        var m = new HashMap<String, RgbColor>(256);
        m.put("activeborder", new RgbColor(0, 0, 255));
        m.put("activecaption", new RgbColor(0, 0, 255));
        m.put("aliceblue", new RgbColor(240, 248, 255));
        m.put("antiquewhite", new RgbColor(250, 235, 215));
        m.put("appworkspace", new RgbColor(255, 255, 255));
        m.put("aqua", new RgbColor(0, 255, 255));
        m.put("aquamarine", new RgbColor(127, 255, 212));
        m.put("azure", new RgbColor(240, 255, 255));
        m.put("background", new RgbColor(255, 255, 255));
        m.put("beige", new RgbColor(245, 245, 220));
        m.put("bisque", new RgbColor(255, 228, 196));
        m.put("black", new RgbColor(0, 0, 0));
        m.put("blanchedalmond", new RgbColor(255, 235, 205));
        m.put("blue", new RgbColor(0, 0, 255));
        m.put("blueviolet", new RgbColor(138, 43, 226));
        m.put("brown", new RgbColor(165, 42, 42));
        m.put("burlywood", new RgbColor(222, 184, 135));
        m.put("buttonface", new RgbColor(0, 0, 0));
        m.put("buttonhighlight", new RgbColor(204, 204, 204));
        m.put("buttonshadow", new RgbColor(51, 51, 51));
        m.put("buttontext", new RgbColor(0, 0, 0));
        m.put("cadetblue", new RgbColor(95, 158, 160));
        m.put("captiontext", new RgbColor(0, 0, 0));
        m.put("chartreuse", new RgbColor(127, 255, 0));
        m.put("chocolate", new RgbColor(210, 105, 30));
        m.put("coral", new RgbColor(255, 127, 80));
        m.put("cornflowerblue", new RgbColor(100, 149, 237));
        m.put("cornsilk", new RgbColor(255, 248, 220));
        m.put("crimson", new RgbColor(220, 20, 60));
        m.put("cyan", new RgbColor(0, 255, 255));
        m.put("darkblue", new RgbColor(0, 0, 139));
        m.put("darkcyan", new RgbColor(0, 139, 139));
        m.put("darkgoldenrod", new RgbColor(184, 134, 11));
        m.put("darkgray", new RgbColor(169, 169, 169));
        m.put("darkgreen", new RgbColor(0, 100, 0));
        m.put("darkgrey", new RgbColor(169, 169, 169));
        m.put("darkkhaki", new RgbColor(189, 183, 107));
        m.put("darkmagenta", new RgbColor(139, 0, 139));
        m.put("darkolivegreen", new RgbColor(85, 107, 47));
        m.put("darkorange", new RgbColor(255, 140, 0));
        m.put("darkorchid", new RgbColor(153, 50, 204));
        m.put("darkred", new RgbColor(139, 0, 0));
        m.put("darksalmon", new RgbColor(233, 150, 122));
        m.put("darkseagreen", new RgbColor(143, 188, 143));
        m.put("darkslateblue", new RgbColor(72, 61, 139));
        m.put("darkslategray", new RgbColor(47, 79, 79));
        m.put("darkslategrey", new RgbColor(47, 79, 79));
        m.put("darkturquoise", new RgbColor(0, 206, 209));
        m.put("darkviolet", new RgbColor(148, 0, 211));
        m.put("deeppink", new RgbColor(255, 20, 147));
        m.put("deepskyblue", new RgbColor(0, 191, 255));
        m.put("dimgray", new RgbColor(105, 105, 105));
        m.put("dimgrey", new RgbColor(105, 105, 105));
        m.put("dodgerblue", new RgbColor(30, 144, 255));
        m.put("firebrick", new RgbColor(178, 34, 34));
        m.put("floralwhite", new RgbColor(255, 250, 240));
        m.put("forestgreen", new RgbColor(34, 139, 34));
        m.put("fuchsia", new RgbColor(255, 0, 255));
        m.put("gainsboro", new RgbColor(220, 220, 220));
        m.put("ghostwhite", new RgbColor(248, 248, 255));
        m.put("gold", new RgbColor(255, 215, 0));
        m.put("goldenrod", new RgbColor(218, 165, 32));
        m.put("gray", new RgbColor(128, 128, 128));
        m.put("graytext", new RgbColor(51, 51, 51));
        m.put("green", new RgbColor(0, 128, 0));
        m.put("greenyellow", new RgbColor(173, 255, 47));
        m.put("grey", new RgbColor(128, 128, 128));
        m.put("highlight", new RgbColor(0, 0, 255));
        m.put("highlighttext", new RgbColor(204, 204, 204));
        m.put("honeydew", new RgbColor(240, 255, 240));
        m.put("hotpink", new RgbColor(255, 105, 180));
        m.put("inactiveborder", new RgbColor(51, 51, 51));
        m.put("inactivecaption", new RgbColor(204, 204, 204));
        m.put("inactivecaptiontext", new RgbColor(51, 51, 51));
        m.put("indianred", new RgbColor(205, 92, 92));
        m.put("indigo", new RgbColor(75, 0, 130));
        m.put("infobackground", new RgbColor(204, 204, 204));
        m.put("infotext", new RgbColor(0, 0, 0));
        m.put("ivory", new RgbColor(255, 255, 240));
        m.put("khaki", new RgbColor(240, 230, 140));
        m.put("lavender", new RgbColor(230, 230, 250));
        m.put("lavenderblush", new RgbColor(255, 240, 245));
        m.put("lawngreen", new RgbColor(124, 252, 0));
        m.put("lemonchiffon", new RgbColor(255, 250, 205));
        m.put("lightblue", new RgbColor(173, 216, 230));
        m.put("lightcoral", new RgbColor(240, 128, 128));
        m.put("lightcyan", new RgbColor(224, 255, 255));
        m.put("lightgoldenrodyellow", new RgbColor(250, 250, 210));
        m.put("lightgray", new RgbColor(211, 211, 211));
        m.put("lightgreen", new RgbColor(144, 238, 144));
        m.put("lightgrey", new RgbColor(211, 211, 211));
        m.put("lightpink", new RgbColor(255, 182, 193));
        m.put("lightsalmon", new RgbColor(255, 160, 122));
        m.put("lightseagreen", new RgbColor(32, 178, 170));
        m.put("lightskyblue", new RgbColor(135, 206, 250));
        m.put("lightslategray", new RgbColor(119, 136, 153));
        m.put("lightslategrey", new RgbColor(119, 136, 153));
        m.put("lightsteelblue", new RgbColor(176, 196, 222));
        m.put("lightyellow", new RgbColor(255, 255, 224));
        m.put("lime", new RgbColor(0, 255, 0));
        m.put("limegreen", new RgbColor(50, 205, 50));
        m.put("linen", new RgbColor(250, 240, 230));
        m.put("magenta", new RgbColor(255, 0, 255));
        m.put("maroon", new RgbColor(128, 0, 0));
        m.put("mediumaquamarine", new RgbColor(102, 205, 170));
        m.put("mediumblue", new RgbColor(0, 0, 205));
        m.put("mediumorchid", new RgbColor(186, 85, 211));
        m.put("mediumpurple", new RgbColor(147, 112, 219));
        m.put("mediumseagreen", new RgbColor(60, 179, 113));
        m.put("mediumslateblue", new RgbColor(123, 104, 238));
        m.put("mediumspringgreen", new RgbColor(0, 250, 154));
        m.put("mediumturquoise", new RgbColor(72, 209, 204));
        m.put("mediumvioletred", new RgbColor(199, 21, 133));
        m.put("menu", new RgbColor(204, 204, 204));
        m.put("menutext", new RgbColor(51, 51, 51));
        m.put("midnightblue", new RgbColor(25, 25, 112));
        m.put("mintcream", new RgbColor(245, 255, 250));
        m.put("mistyrose", new RgbColor(255, 228, 225));
        m.put("moccasin", new RgbColor(255, 228, 181));
        m.put("navajowhite", new RgbColor(255, 222, 173));
        m.put("navy", new RgbColor(0, 0, 128));
        m.put("none", TRANSPARENT);
        m.put("oldlace", new RgbColor(253, 245, 230));
        m.put("olive", new RgbColor(128, 128, 0));
        m.put("olivedrab", new RgbColor(107, 142, 35));
        m.put("orange", new RgbColor(255, 165, 0));
        m.put("orangered", new RgbColor(255, 69, 0));
        m.put("orchid", new RgbColor(218, 112, 214));
        m.put("palegoldenrod", new RgbColor(238, 232, 170));
        m.put("palegreen", new RgbColor(152, 251, 152));
        m.put("paleturquoise", new RgbColor(175, 238, 238));
        m.put("palevioletred", new RgbColor(219, 112, 147));
        m.put("papayawhip", new RgbColor(255, 239, 213));
        m.put("peachpuff", new RgbColor(255, 218, 185));
        m.put("peru", new RgbColor(205, 133, 63));
        m.put("pink", new RgbColor(255, 192, 203));
        m.put("plum", new RgbColor(221, 160, 221));
        m.put("powderblue", new RgbColor(176, 224, 230));
        m.put("purple", new RgbColor(128, 0, 128));
        m.put("red", new RgbColor(255, 0, 0));
        m.put("rosybrown", new RgbColor(188, 143, 143));
        m.put("royalblue", new RgbColor(65, 105, 225));
        m.put("saddlebrown", new RgbColor(139, 69, 19));
        m.put("salmon", new RgbColor(250, 128, 114));
        m.put("sandybrown", new RgbColor(244, 164, 96));
        m.put("scrollbar", new RgbColor(204, 204, 204));
        m.put("seagreen", new RgbColor(46, 139, 87));
        m.put("seashell", new RgbColor(255, 245, 238));
        m.put("sienna", new RgbColor(160, 82, 45));
        m.put("silver", new RgbColor(192, 192, 192));
        m.put("skyblue", new RgbColor(135, 206, 235));
        m.put("slateblue", new RgbColor(106, 90, 205));
        m.put("slategray", new RgbColor(112, 128, 144));
        m.put("slategrey", new RgbColor(112, 128, 144));
        m.put("snow", new RgbColor(255, 250, 250));
        m.put("springgreen", new RgbColor(0, 255, 127));
        m.put("steelblue", new RgbColor(70, 130, 180));
        m.put("tan", new RgbColor(210, 180, 140));
        m.put("teal", new RgbColor(0, 128, 128));
        m.put("thistle", new RgbColor(216, 191, 216));
        m.put("threeddarkshadow", new RgbColor(51, 51, 51));
        m.put("threedface", new RgbColor(204, 204, 204));
        m.put("threedhighlight", new RgbColor(255, 255, 255));
        m.put("threedlightshadow", new RgbColor(51, 51, 51));
        m.put("threedshadow", new RgbColor(51, 51, 51));
        m.put("tomato", new RgbColor(255, 99, 71));
        m.put("transparent", TRANSPARENT);
        m.put("turquoise", new RgbColor(64, 224, 208));
        m.put("violet", new RgbColor(238, 130, 238));
        m.put("wheat", new RgbColor(245, 222, 179));
        m.put("white", new RgbColor(255, 255, 255));
        m.put("whitesmoke", new RgbColor(245, 245, 245));
        m.put("window", new RgbColor(204, 204, 204));
        m.put("windowframe", new RgbColor(204, 204, 204));
        m.put("windowtext", new RgbColor(0, 0, 0));
        m.put("yellow", new RgbColor(255, 255, 0));
        m.put("yellowgreen", new RgbColor(154, 205, 50));
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

        // Functional notation — delegated to the in.virit:color library
        if (isFunctionalNotation(string)) {
            try {
                RgbColor rgb = Color.parseCssColor(string).toRgbColor();
                return opacity >= 1.0 ? rgb : rgb.withAlpha(rgb.a() * opacity);
            } catch (IllegalArgumentException e) {
                return BLACK;
            }
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

package com.jairosvg;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SVG color parsing. Converts color strings to RGBA tuples (0.0–1.0).
 * Port of CairoSVG colors.py
 */
public final class Colors {

    /** RGBA color as (r, g, b, a) with each component in [0.0, 1.0]. */
    public record RGBA(double r, double g, double b, double a) {
        public static final RGBA TRANSPARENT = new RGBA(0, 0, 0, 0);
        public static final RGBA BLACK = new RGBA(0, 0, 0, 1);
    }

    private static final Pattern RGBA_PATTERN = Pattern.compile("rgba\\((.+?)\\)");
    private static final Pattern RGB_PATTERN = Pattern.compile("rgb\\((.+?)\\)");
    private static final Pattern HSLA_PATTERN = Pattern.compile("hsla\\((.+?)\\)");
    private static final Pattern HSL_PATTERN = Pattern.compile("hsl\\((.+?)\\)");
    private static final Pattern HEX_RRGGBB = Pattern.compile("#[0-9a-f]{6}");
    private static final Pattern HEX_RGB = Pattern.compile("#[0-9a-f]{3}");

    private static final Map<String, RGBA> NAMED_COLORS = Map.ofEntries(
        Map.entry("aliceblue", rgba(240, 248, 255)),
        Map.entry("antiquewhite", rgba(250, 235, 215)),
        Map.entry("aqua", rgba(0, 255, 255)),
        Map.entry("aquamarine", rgba(127, 255, 212)),
        Map.entry("azure", rgba(240, 255, 255)),
        Map.entry("beige", rgba(245, 245, 220)),
        Map.entry("bisque", rgba(255, 228, 196)),
        Map.entry("black", rgba(0, 0, 0)),
        Map.entry("blanchedalmond", rgba(255, 235, 205)),
        Map.entry("blue", rgba(0, 0, 255)),
        Map.entry("blueviolet", rgba(138, 43, 226)),
        Map.entry("brown", rgba(165, 42, 42)),
        Map.entry("burlywood", rgba(222, 184, 135)),
        Map.entry("cadetblue", rgba(95, 158, 160)),
        Map.entry("chartreuse", rgba(127, 255, 0)),
        Map.entry("chocolate", rgba(210, 105, 30)),
        Map.entry("coral", rgba(255, 127, 80)),
        Map.entry("cornflowerblue", rgba(100, 149, 237)),
        Map.entry("cornsilk", rgba(255, 248, 220)),
        Map.entry("crimson", rgba(220, 20, 60)),
        Map.entry("cyan", rgba(0, 255, 255)),
        Map.entry("darkblue", rgba(0, 0, 139)),
        Map.entry("darkcyan", rgba(0, 139, 139)),
        Map.entry("darkgoldenrod", rgba(184, 134, 11)),
        Map.entry("darkgray", rgba(169, 169, 169)),
        Map.entry("darkgreen", rgba(0, 100, 0)),
        Map.entry("darkgrey", rgba(169, 169, 169)),
        Map.entry("darkkhaki", rgba(189, 183, 107)),
        Map.entry("darkmagenta", rgba(139, 0, 139)),
        Map.entry("darkolivegreen", rgba(85, 107, 47)),
        Map.entry("darkorange", rgba(255, 140, 0)),
        Map.entry("darkorchid", rgba(153, 50, 204)),
        Map.entry("darkred", rgba(139, 0, 0)),
        Map.entry("darksalmon", rgba(233, 150, 122)),
        Map.entry("darkseagreen", rgba(143, 188, 143)),
        Map.entry("darkslateblue", rgba(72, 61, 139)),
        Map.entry("darkslategray", rgba(47, 79, 79)),
        Map.entry("darkslategrey", rgba(47, 79, 79)),
        Map.entry("darkturquoise", rgba(0, 206, 209)),
        Map.entry("darkviolet", rgba(148, 0, 211)),
        Map.entry("deeppink", rgba(255, 20, 147)),
        Map.entry("deepskyblue", rgba(0, 191, 255)),
        Map.entry("dimgray", rgba(105, 105, 105)),
        Map.entry("dimgrey", rgba(105, 105, 105)),
        Map.entry("dodgerblue", rgba(30, 144, 255)),
        Map.entry("firebrick", rgba(178, 34, 34)),
        Map.entry("floralwhite", rgba(255, 250, 240)),
        Map.entry("forestgreen", rgba(34, 139, 34)),
        Map.entry("fuchsia", rgba(255, 0, 255)),
        Map.entry("gainsboro", rgba(220, 220, 220)),
        Map.entry("ghostwhite", rgba(248, 248, 255)),
        Map.entry("gold", rgba(255, 215, 0)),
        Map.entry("goldenrod", rgba(218, 165, 32)),
        Map.entry("gray", rgba(128, 128, 128)),
        Map.entry("grey", rgba(128, 128, 128)),
        Map.entry("green", rgba(0, 128, 0)),
        Map.entry("greenyellow", rgba(173, 255, 47)),
        Map.entry("honeydew", rgba(240, 255, 240)),
        Map.entry("hotpink", rgba(255, 105, 180)),
        Map.entry("indianred", rgba(205, 92, 92)),
        Map.entry("indigo", rgba(75, 0, 130)),
        Map.entry("ivory", rgba(255, 255, 240)),
        Map.entry("khaki", rgba(240, 230, 140)),
        Map.entry("lavender", rgba(230, 230, 250)),
        Map.entry("lavenderblush", rgba(255, 240, 245)),
        Map.entry("lawngreen", rgba(124, 252, 0)),
        Map.entry("lemonchiffon", rgba(255, 250, 205)),
        Map.entry("lightblue", rgba(173, 216, 230)),
        Map.entry("lightcoral", rgba(240, 128, 128)),
        Map.entry("lightcyan", rgba(224, 255, 255)),
        Map.entry("lightgoldenrodyellow", rgba(250, 250, 210)),
        Map.entry("lightgray", rgba(211, 211, 211)),
        Map.entry("lightgreen", rgba(144, 238, 144)),
        Map.entry("lightgrey", rgba(211, 211, 211)),
        Map.entry("lightpink", rgba(255, 182, 193)),
        Map.entry("lightsalmon", rgba(255, 160, 122)),
        Map.entry("lightseagreen", rgba(32, 178, 170)),
        Map.entry("lightskyblue", rgba(135, 206, 250)),
        Map.entry("lightslategray", rgba(119, 136, 153)),
        Map.entry("lightslategrey", rgba(119, 136, 153)),
        Map.entry("lightsteelblue", rgba(176, 196, 222)),
        Map.entry("lightyellow", rgba(255, 255, 224)),
        Map.entry("lime", rgba(0, 255, 0)),
        Map.entry("limegreen", rgba(50, 205, 50)),
        Map.entry("linen", rgba(250, 240, 230)),
        Map.entry("magenta", rgba(255, 0, 255)),
        Map.entry("maroon", rgba(128, 0, 0)),
        Map.entry("mediumaquamarine", rgba(102, 205, 170)),
        Map.entry("mediumblue", rgba(0, 0, 205)),
        Map.entry("mediumorchid", rgba(186, 85, 211)),
        Map.entry("mediumpurple", rgba(147, 112, 219)),
        Map.entry("mediumseagreen", rgba(60, 179, 113)),
        Map.entry("mediumslateblue", rgba(123, 104, 238)),
        Map.entry("mediumspringgreen", rgba(0, 250, 154)),
        Map.entry("mediumturquoise", rgba(72, 209, 204)),
        Map.entry("mediumvioletred", rgba(199, 21, 133)),
        Map.entry("midnightblue", rgba(25, 25, 112)),
        Map.entry("mintcream", rgba(245, 255, 250)),
        Map.entry("mistyrose", rgba(255, 228, 225)),
        Map.entry("moccasin", rgba(255, 228, 181)),
        Map.entry("navajowhite", rgba(255, 222, 173)),
        Map.entry("navy", rgba(0, 0, 128)),
        Map.entry("oldlace", rgba(253, 245, 230)),
        Map.entry("olive", rgba(128, 128, 0)),
        Map.entry("olivedrab", rgba(107, 142, 35)),
        Map.entry("orange", rgba(255, 165, 0)),
        Map.entry("orangered", rgba(255, 69, 0)),
        Map.entry("orchid", rgba(218, 112, 214)),
        Map.entry("palegoldenrod", rgba(238, 232, 170)),
        Map.entry("palegreen", rgba(152, 251, 152)),
        Map.entry("paleturquoise", rgba(175, 238, 238)),
        Map.entry("palevioletred", rgba(219, 112, 147)),
        Map.entry("papayawhip", rgba(255, 239, 213)),
        Map.entry("peachpuff", rgba(255, 218, 185)),
        Map.entry("peru", rgba(205, 133, 63)),
        Map.entry("pink", rgba(255, 192, 203)),
        Map.entry("plum", rgba(221, 160, 221)),
        Map.entry("powderblue", rgba(176, 224, 230)),
        Map.entry("purple", rgba(128, 0, 128)),
        Map.entry("red", rgba(255, 0, 0)),
        Map.entry("rosybrown", rgba(188, 143, 143)),
        Map.entry("royalblue", rgba(65, 105, 225)),
        Map.entry("saddlebrown", rgba(139, 69, 19)),
        Map.entry("salmon", rgba(250, 128, 114)),
        Map.entry("sandybrown", rgba(244, 164, 96)),
        Map.entry("seagreen", rgba(46, 139, 87)),
        Map.entry("seashell", rgba(255, 245, 238)),
        Map.entry("sienna", rgba(160, 82, 45)),
        Map.entry("silver", rgba(192, 192, 192)),
        Map.entry("skyblue", rgba(135, 206, 235)),
        Map.entry("slateblue", rgba(106, 90, 205)),
        Map.entry("slategray", rgba(112, 128, 144)),
        Map.entry("slategrey", rgba(112, 128, 144)),
        Map.entry("snow", rgba(255, 250, 250)),
        Map.entry("springgreen", rgba(0, 255, 127)),
        Map.entry("steelblue", rgba(70, 130, 180)),
        Map.entry("tan", rgba(210, 180, 140)),
        Map.entry("teal", rgba(0, 128, 128)),
        Map.entry("thistle", rgba(216, 191, 216)),
        Map.entry("tomato", rgba(255, 99, 71)),
        Map.entry("turquoise", rgba(64, 224, 208)),
        Map.entry("violet", rgba(238, 130, 238)),
        Map.entry("wheat", rgba(245, 222, 179)),
        Map.entry("white", rgba(255, 255, 255)),
        Map.entry("whitesmoke", rgba(245, 245, 245)),
        Map.entry("yellow", rgba(255, 255, 0)),
        Map.entry("yellowgreen", rgba(154, 205, 50)),
        Map.entry("none", RGBA.TRANSPARENT),
        Map.entry("transparent", RGBA.TRANSPARENT),
        // System colors (minimal defaults)
        Map.entry("activeborder", new RGBA(0, 0, 1, 1)),
        Map.entry("activecaption", new RGBA(0, 0, 1, 1)),
        Map.entry("appworkspace", new RGBA(1, 1, 1, 1)),
        Map.entry("background", new RGBA(1, 1, 1, 1)),
        Map.entry("buttonface", new RGBA(0, 0, 0, 1)),
        Map.entry("buttonhighlight", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("buttonshadow", new RGBA(0.2, 0.2, 0.2, 1)),
        Map.entry("buttontext", new RGBA(0, 0, 0, 1)),
        Map.entry("captiontext", new RGBA(0, 0, 0, 1)),
        Map.entry("graytext", new RGBA(0.2, 0.2, 0.2, 1)),
        Map.entry("highlight", new RGBA(0, 0, 1, 1)),
        Map.entry("highlighttext", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("inactiveborder", new RGBA(0.2, 0.2, 0.2, 1)),
        Map.entry("inactivecaption", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("inactivecaptiontext", new RGBA(0.2, 0.2, 0.2, 1)),
        Map.entry("infobackground", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("infotext", new RGBA(0, 0, 0, 1)),
        Map.entry("menu", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("menutext", new RGBA(0.2, 0.2, 0.2, 1)),
        Map.entry("scrollbar", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("threeddarkshadow", new RGBA(0.2, 0.2, 0.2, 1)),
        Map.entry("threedface", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("threedhighlight", new RGBA(1, 1, 1, 1)),
        Map.entry("threedlightshadow", new RGBA(0.2, 0.2, 0.2, 1)),
        Map.entry("threedshadow", new RGBA(0.2, 0.2, 0.2, 1)),
        Map.entry("window", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("windowframe", new RGBA(0.8, 0.8, 0.8, 1)),
        Map.entry("windowtext", new RGBA(0, 0, 0, 1))
    );

    private Colors() {}

    private static RGBA rgba(int r, int g, int b) {
        return new RGBA(r / 255.0, g / 255.0, b / 255.0, 1.0);
    }

    /** Parse a color string into RGBA. */
    public static RGBA color(String string, double opacity) {
        if (string == null || string.isBlank()) {
            return RGBA.TRANSPARENT;
        }

        string = string.strip().toLowerCase();

        RGBA named = NAMED_COLORS.get(string);
        if (named != null) {
            return new RGBA(named.r(), named.g(), named.b(), named.a() * opacity);
        }

        Matcher m = RGBA_PATTERN.matcher(string);
        if (m.find()) {
            String[] parts = m.group(1).strip().split(",");
            if (parts.length == 4) {
                double r = parseColorComponent(parts[0]);
                double g = parseColorComponent(parts[1]);
                double b = parseColorComponent(parts[2]);
                double a = Double.parseDouble(parts[3].strip());
                return new RGBA(r, g, b, a * opacity);
            }
        }

        m = RGB_PATTERN.matcher(string);
        if (m.find()) {
            String[] parts = m.group(1).strip().split(",");
            if (parts.length == 3) {
                double r = parseColorComponent(parts[0]);
                double g = parseColorComponent(parts[1]);
                double b = parseColorComponent(parts[2]);
                return new RGBA(r, g, b, opacity);
            }
        }

        m = HSLA_PATTERN.matcher(string);
        if (m.find()) {
            String[] parts = m.group(1).strip().split(",");
            if (parts.length == 4) {
                double[] rgb = hslToRgb(parts[0], parts[1], parts[2]);
                double a = Double.parseDouble(parts[3].strip());
                return new RGBA(rgb[0], rgb[1], rgb[2], a * opacity);
            }
        }

        m = HSL_PATTERN.matcher(string);
        if (m.find()) {
            String[] parts = m.group(1).strip().split(",");
            if (parts.length == 3) {
                double[] rgb = hslToRgb(parts[0], parts[1], parts[2]);
                return new RGBA(rgb[0], rgb[1], rgb[2], opacity);
            }
        }

        m = HEX_RRGGBB.matcher(string);
        if (m.find()) {
            double r = Integer.parseInt(string.substring(1, 3), 16) / 255.0;
            double g = Integer.parseInt(string.substring(3, 5), 16) / 255.0;
            double b = Integer.parseInt(string.substring(5, 7), 16) / 255.0;
            return new RGBA(r, g, b, opacity);
        }

        m = HEX_RGB.matcher(string);
        if (m.find()) {
            double r = Integer.parseInt(String.valueOf(string.charAt(1)), 16) / 15.0;
            double g = Integer.parseInt(String.valueOf(string.charAt(2)), 16) / 15.0;
            double b = Integer.parseInt(String.valueOf(string.charAt(3)), 16) / 15.0;
            return new RGBA(r, g, b, opacity);
        }

        return RGBA.BLACK;
    }

    /** Parse with default opacity of 1. */
    public static RGBA color(String string) {
        return color(string, 1.0);
    }

    /** Negate (complement) a color. */
    public static RGBA negateColor(RGBA c) {
        return new RGBA(1 - c.r(), 1 - c.g(), 1 - c.b(), c.a());
    }

    private static double[] hslToRgb(String hPart, String sPart, String lPart) {
        double h = (((Double.parseDouble(hPart.strip()) % 360) + 360) % 360) / 360.0;
        sPart = sPart.strip();
        lPart = lPart.strip();
        if (!sPart.endsWith("%") || !lPart.endsWith("%")) {
            throw new IllegalArgumentException("Saturation and lightness in hsl() must be percentages.");
        }
        double s = Double.parseDouble(sPart.substring(0, sPart.length() - 1)) / 100.0;
        double l = Double.parseDouble(lPart.substring(0, lPart.length() - 1)) / 100.0;
        if (s == 0) {
            return new double[]{l, l, l};
        }
        double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        double p = 2 * l - q;
        return new double[]{hueToRgb(p, q, h + 1.0 / 3), hueToRgb(p, q, h), hueToRgb(p, q, h - 1.0 / 3)};
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2) return q;
        if (t < 2.0 / 3) return p + (q - p) * (2.0 / 3 - t) * 6;
        return p;
    }

    private static double parseColorComponent(String part) {
        part = part.strip();
        if (part.endsWith("%")) {
            return Double.parseDouble(part.substring(0, part.length() - 1)) / 100.0;
        }
        return Double.parseDouble(part) / 255.0;
    }
}

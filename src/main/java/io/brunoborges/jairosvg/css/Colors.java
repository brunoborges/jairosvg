package io.brunoborges.jairosvg.css;

import java.util.HashMap;
import java.util.Map;

/**
 * SVG color parsing. Converts color strings to RGBA tuples (0.0–1.0).
 */
public final class Colors {

    /** RGBA color as (r, g, b, a) with each component in [0.0, 1.0]. */
    public record RGBA(double r, double g, double b, double a) {
        public static final RGBA TRANSPARENT = new RGBA(0, 0, 0, 0);
        public static final RGBA BLACK = new RGBA(0, 0, 0, 1);
    }

    private static final double D50_X = 0.96422;
    private static final double D50_Y = 1.0;
    private static final double D50_Z = 0.82521;
    private static final double LAB_EPSILON = 216.0 / 24389.0;
    private static final double LAB_KAPPA = 24389.0 / 27.0;

    private static final Map<String, RGBA> NAMED_COLORS = buildNamedColors();

    private record ParsedComponents(String[] components, String alpha) {
    }

    private Colors() {
    }

    @SuppressWarnings("java:S1192") // color values are intentionally repeated
    private static Map<String, RGBA> buildNamedColors() {
        var m = new HashMap<String, RGBA>(256);
        m.put("activeborder", new RGBA(0 / 255.0, 0 / 255.0, 255 / 255.0, 1));
        m.put("activecaption", new RGBA(0 / 255.0, 0 / 255.0, 255 / 255.0, 1));
        m.put("aliceblue", new RGBA(240 / 255.0, 248 / 255.0, 255 / 255.0, 1));
        m.put("antiquewhite", new RGBA(250 / 255.0, 235 / 255.0, 215 / 255.0, 1));
        m.put("appworkspace", new RGBA(255 / 255.0, 255 / 255.0, 255 / 255.0, 1));
        m.put("aqua", new RGBA(0 / 255.0, 255 / 255.0, 255 / 255.0, 1));
        m.put("aquamarine", new RGBA(127 / 255.0, 255 / 255.0, 212 / 255.0, 1));
        m.put("azure", new RGBA(240 / 255.0, 255 / 255.0, 255 / 255.0, 1));
        m.put("background", new RGBA(255 / 255.0, 255 / 255.0, 255 / 255.0, 1));
        m.put("beige", new RGBA(245 / 255.0, 245 / 255.0, 220 / 255.0, 1));
        m.put("bisque", new RGBA(255 / 255.0, 228 / 255.0, 196 / 255.0, 1));
        m.put("black", new RGBA(0 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("blanchedalmond", new RGBA(255 / 255.0, 235 / 255.0, 205 / 255.0, 1));
        m.put("blue", new RGBA(0 / 255.0, 0 / 255.0, 255 / 255.0, 1));
        m.put("blueviolet", new RGBA(138 / 255.0, 43 / 255.0, 226 / 255.0, 1));
        m.put("brown", new RGBA(165 / 255.0, 42 / 255.0, 42 / 255.0, 1));
        m.put("burlywood", new RGBA(222 / 255.0, 184 / 255.0, 135 / 255.0, 1));
        m.put("buttonface", new RGBA(0 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("buttonhighlight", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("buttonshadow", new RGBA(51 / 255.0, 51 / 255.0, 51 / 255.0, 1));
        m.put("buttontext", new RGBA(0 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("cadetblue", new RGBA(95 / 255.0, 158 / 255.0, 160 / 255.0, 1));
        m.put("captiontext", new RGBA(0 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("chartreuse", new RGBA(127 / 255.0, 255 / 255.0, 0 / 255.0, 1));
        m.put("chocolate", new RGBA(210 / 255.0, 105 / 255.0, 30 / 255.0, 1));
        m.put("coral", new RGBA(255 / 255.0, 127 / 255.0, 80 / 255.0, 1));
        m.put("cornflowerblue", new RGBA(100 / 255.0, 149 / 255.0, 237 / 255.0, 1));
        m.put("cornsilk", new RGBA(255 / 255.0, 248 / 255.0, 220 / 255.0, 1));
        m.put("crimson", new RGBA(220 / 255.0, 20 / 255.0, 60 / 255.0, 1));
        m.put("cyan", new RGBA(0 / 255.0, 255 / 255.0, 255 / 255.0, 1));
        m.put("darkblue", new RGBA(0 / 255.0, 0 / 255.0, 139 / 255.0, 1));
        m.put("darkcyan", new RGBA(0 / 255.0, 139 / 255.0, 139 / 255.0, 1));
        m.put("darkgoldenrod", new RGBA(184 / 255.0, 134 / 255.0, 11 / 255.0, 1));
        m.put("darkgray", new RGBA(169 / 255.0, 169 / 255.0, 169 / 255.0, 1));
        m.put("darkgreen", new RGBA(0 / 255.0, 100 / 255.0, 0 / 255.0, 1));
        m.put("darkgrey", new RGBA(169 / 255.0, 169 / 255.0, 169 / 255.0, 1));
        m.put("darkkhaki", new RGBA(189 / 255.0, 183 / 255.0, 107 / 255.0, 1));
        m.put("darkmagenta", new RGBA(139 / 255.0, 0 / 255.0, 139 / 255.0, 1));
        m.put("darkolivegreen", new RGBA(85 / 255.0, 107 / 255.0, 47 / 255.0, 1));
        m.put("darkorange", new RGBA(255 / 255.0, 140 / 255.0, 0 / 255.0, 1));
        m.put("darkorchid", new RGBA(153 / 255.0, 50 / 255.0, 204 / 255.0, 1));
        m.put("darkred", new RGBA(139 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("darksalmon", new RGBA(233 / 255.0, 150 / 255.0, 122 / 255.0, 1));
        m.put("darkseagreen", new RGBA(143 / 255.0, 188 / 255.0, 143 / 255.0, 1));
        m.put("darkslateblue", new RGBA(72 / 255.0, 61 / 255.0, 139 / 255.0, 1));
        m.put("darkslategray", new RGBA(47 / 255.0, 79 / 255.0, 79 / 255.0, 1));
        m.put("darkslategrey", new RGBA(47 / 255.0, 79 / 255.0, 79 / 255.0, 1));
        m.put("darkturquoise", new RGBA(0 / 255.0, 206 / 255.0, 209 / 255.0, 1));
        m.put("darkviolet", new RGBA(148 / 255.0, 0 / 255.0, 211 / 255.0, 1));
        m.put("deeppink", new RGBA(255 / 255.0, 20 / 255.0, 147 / 255.0, 1));
        m.put("deepskyblue", new RGBA(0 / 255.0, 191 / 255.0, 255 / 255.0, 1));
        m.put("dimgray", new RGBA(105 / 255.0, 105 / 255.0, 105 / 255.0, 1));
        m.put("dimgrey", new RGBA(105 / 255.0, 105 / 255.0, 105 / 255.0, 1));
        m.put("dodgerblue", new RGBA(30 / 255.0, 144 / 255.0, 255 / 255.0, 1));
        m.put("firebrick", new RGBA(178 / 255.0, 34 / 255.0, 34 / 255.0, 1));
        m.put("floralwhite", new RGBA(255 / 255.0, 250 / 255.0, 240 / 255.0, 1));
        m.put("forestgreen", new RGBA(34 / 255.0, 139 / 255.0, 34 / 255.0, 1));
        m.put("fuchsia", new RGBA(255 / 255.0, 0 / 255.0, 255 / 255.0, 1));
        m.put("gainsboro", new RGBA(220 / 255.0, 220 / 255.0, 220 / 255.0, 1));
        m.put("ghostwhite", new RGBA(248 / 255.0, 248 / 255.0, 255 / 255.0, 1));
        m.put("gold", new RGBA(255 / 255.0, 215 / 255.0, 0 / 255.0, 1));
        m.put("goldenrod", new RGBA(218 / 255.0, 165 / 255.0, 32 / 255.0, 1));
        m.put("gray", new RGBA(128 / 255.0, 128 / 255.0, 128 / 255.0, 1));
        m.put("graytext", new RGBA(51 / 255.0, 51 / 255.0, 51 / 255.0, 1));
        m.put("green", new RGBA(0 / 255.0, 128 / 255.0, 0 / 255.0, 1));
        m.put("greenyellow", new RGBA(173 / 255.0, 255 / 255.0, 47 / 255.0, 1));
        m.put("grey", new RGBA(128 / 255.0, 128 / 255.0, 128 / 255.0, 1));
        m.put("highlight", new RGBA(0 / 255.0, 0 / 255.0, 255 / 255.0, 1));
        m.put("highlighttext", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("honeydew", new RGBA(240 / 255.0, 255 / 255.0, 240 / 255.0, 1));
        m.put("hotpink", new RGBA(255 / 255.0, 105 / 255.0, 180 / 255.0, 1));
        m.put("inactiveborder", new RGBA(51 / 255.0, 51 / 255.0, 51 / 255.0, 1));
        m.put("inactivecaption", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("inactivecaptiontext", new RGBA(51 / 255.0, 51 / 255.0, 51 / 255.0, 1));
        m.put("indianred", new RGBA(205 / 255.0, 92 / 255.0, 92 / 255.0, 1));
        m.put("indigo", new RGBA(75 / 255.0, 0 / 255.0, 130 / 255.0, 1));
        m.put("infobackground", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("infotext", new RGBA(0 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("ivory", new RGBA(255 / 255.0, 255 / 255.0, 240 / 255.0, 1));
        m.put("khaki", new RGBA(240 / 255.0, 230 / 255.0, 140 / 255.0, 1));
        m.put("lavender", new RGBA(230 / 255.0, 230 / 255.0, 250 / 255.0, 1));
        m.put("lavenderblush", new RGBA(255 / 255.0, 240 / 255.0, 245 / 255.0, 1));
        m.put("lawngreen", new RGBA(124 / 255.0, 252 / 255.0, 0 / 255.0, 1));
        m.put("lemonchiffon", new RGBA(255 / 255.0, 250 / 255.0, 205 / 255.0, 1));
        m.put("lightblue", new RGBA(173 / 255.0, 216 / 255.0, 230 / 255.0, 1));
        m.put("lightcoral", new RGBA(240 / 255.0, 128 / 255.0, 128 / 255.0, 1));
        m.put("lightcyan", new RGBA(224 / 255.0, 255 / 255.0, 255 / 255.0, 1));
        m.put("lightgoldenrodyellow", new RGBA(250 / 255.0, 250 / 255.0, 210 / 255.0, 1));
        m.put("lightgray", new RGBA(211 / 255.0, 211 / 255.0, 211 / 255.0, 1));
        m.put("lightgreen", new RGBA(144 / 255.0, 238 / 255.0, 144 / 255.0, 1));
        m.put("lightgrey", new RGBA(211 / 255.0, 211 / 255.0, 211 / 255.0, 1));
        m.put("lightpink", new RGBA(255 / 255.0, 182 / 255.0, 193 / 255.0, 1));
        m.put("lightsalmon", new RGBA(255 / 255.0, 160 / 255.0, 122 / 255.0, 1));
        m.put("lightseagreen", new RGBA(32 / 255.0, 178 / 255.0, 170 / 255.0, 1));
        m.put("lightskyblue", new RGBA(135 / 255.0, 206 / 255.0, 250 / 255.0, 1));
        m.put("lightslategray", new RGBA(119 / 255.0, 136 / 255.0, 153 / 255.0, 1));
        m.put("lightslategrey", new RGBA(119 / 255.0, 136 / 255.0, 153 / 255.0, 1));
        m.put("lightsteelblue", new RGBA(176 / 255.0, 196 / 255.0, 222 / 255.0, 1));
        m.put("lightyellow", new RGBA(255 / 255.0, 255 / 255.0, 224 / 255.0, 1));
        m.put("lime", new RGBA(0 / 255.0, 255 / 255.0, 0 / 255.0, 1));
        m.put("limegreen", new RGBA(50 / 255.0, 205 / 255.0, 50 / 255.0, 1));
        m.put("linen", new RGBA(250 / 255.0, 240 / 255.0, 230 / 255.0, 1));
        m.put("magenta", new RGBA(255 / 255.0, 0 / 255.0, 255 / 255.0, 1));
        m.put("maroon", new RGBA(128 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("mediumaquamarine", new RGBA(102 / 255.0, 205 / 255.0, 170 / 255.0, 1));
        m.put("mediumblue", new RGBA(0 / 255.0, 0 / 255.0, 205 / 255.0, 1));
        m.put("mediumorchid", new RGBA(186 / 255.0, 85 / 255.0, 211 / 255.0, 1));
        m.put("mediumpurple", new RGBA(147 / 255.0, 112 / 255.0, 219 / 255.0, 1));
        m.put("mediumseagreen", new RGBA(60 / 255.0, 179 / 255.0, 113 / 255.0, 1));
        m.put("mediumslateblue", new RGBA(123 / 255.0, 104 / 255.0, 238 / 255.0, 1));
        m.put("mediumspringgreen", new RGBA(0 / 255.0, 250 / 255.0, 154 / 255.0, 1));
        m.put("mediumturquoise", new RGBA(72 / 255.0, 209 / 255.0, 204 / 255.0, 1));
        m.put("mediumvioletred", new RGBA(199 / 255.0, 21 / 255.0, 133 / 255.0, 1));
        m.put("menu", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("menutext", new RGBA(51 / 255.0, 51 / 255.0, 51 / 255.0, 1));
        m.put("midnightblue", new RGBA(25 / 255.0, 25 / 255.0, 112 / 255.0, 1));
        m.put("mintcream", new RGBA(245 / 255.0, 255 / 255.0, 250 / 255.0, 1));
        m.put("mistyrose", new RGBA(255 / 255.0, 228 / 255.0, 225 / 255.0, 1));
        m.put("moccasin", new RGBA(255 / 255.0, 228 / 255.0, 181 / 255.0, 1));
        m.put("navajowhite", new RGBA(255 / 255.0, 222 / 255.0, 173 / 255.0, 1));
        m.put("navy", new RGBA(0 / 255.0, 0 / 255.0, 128 / 255.0, 1));
        m.put("none", RGBA.TRANSPARENT);
        m.put("oldlace", new RGBA(253 / 255.0, 245 / 255.0, 230 / 255.0, 1));
        m.put("olive", new RGBA(128 / 255.0, 128 / 255.0, 0 / 255.0, 1));
        m.put("olivedrab", new RGBA(107 / 255.0, 142 / 255.0, 35 / 255.0, 1));
        m.put("orange", new RGBA(255 / 255.0, 165 / 255.0, 0 / 255.0, 1));
        m.put("orangered", new RGBA(255 / 255.0, 69 / 255.0, 0 / 255.0, 1));
        m.put("orchid", new RGBA(218 / 255.0, 112 / 255.0, 214 / 255.0, 1));
        m.put("palegoldenrod", new RGBA(238 / 255.0, 232 / 255.0, 170 / 255.0, 1));
        m.put("palegreen", new RGBA(152 / 255.0, 251 / 255.0, 152 / 255.0, 1));
        m.put("paleturquoise", new RGBA(175 / 255.0, 238 / 255.0, 238 / 255.0, 1));
        m.put("palevioletred", new RGBA(219 / 255.0, 112 / 255.0, 147 / 255.0, 1));
        m.put("papayawhip", new RGBA(255 / 255.0, 239 / 255.0, 213 / 255.0, 1));
        m.put("peachpuff", new RGBA(255 / 255.0, 218 / 255.0, 185 / 255.0, 1));
        m.put("peru", new RGBA(205 / 255.0, 133 / 255.0, 63 / 255.0, 1));
        m.put("pink", new RGBA(255 / 255.0, 192 / 255.0, 203 / 255.0, 1));
        m.put("plum", new RGBA(221 / 255.0, 160 / 255.0, 221 / 255.0, 1));
        m.put("powderblue", new RGBA(176 / 255.0, 224 / 255.0, 230 / 255.0, 1));
        m.put("purple", new RGBA(128 / 255.0, 0 / 255.0, 128 / 255.0, 1));
        m.put("red", new RGBA(255 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("rosybrown", new RGBA(188 / 255.0, 143 / 255.0, 143 / 255.0, 1));
        m.put("royalblue", new RGBA(65 / 255.0, 105 / 255.0, 225 / 255.0, 1));
        m.put("saddlebrown", new RGBA(139 / 255.0, 69 / 255.0, 19 / 255.0, 1));
        m.put("salmon", new RGBA(250 / 255.0, 128 / 255.0, 114 / 255.0, 1));
        m.put("sandybrown", new RGBA(244 / 255.0, 164 / 255.0, 96 / 255.0, 1));
        m.put("scrollbar", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("seagreen", new RGBA(46 / 255.0, 139 / 255.0, 87 / 255.0, 1));
        m.put("seashell", new RGBA(255 / 255.0, 245 / 255.0, 238 / 255.0, 1));
        m.put("sienna", new RGBA(160 / 255.0, 82 / 255.0, 45 / 255.0, 1));
        m.put("silver", new RGBA(192 / 255.0, 192 / 255.0, 192 / 255.0, 1));
        m.put("skyblue", new RGBA(135 / 255.0, 206 / 255.0, 235 / 255.0, 1));
        m.put("slateblue", new RGBA(106 / 255.0, 90 / 255.0, 205 / 255.0, 1));
        m.put("slategray", new RGBA(112 / 255.0, 128 / 255.0, 144 / 255.0, 1));
        m.put("slategrey", new RGBA(112 / 255.0, 128 / 255.0, 144 / 255.0, 1));
        m.put("snow", new RGBA(255 / 255.0, 250 / 255.0, 250 / 255.0, 1));
        m.put("springgreen", new RGBA(0 / 255.0, 255 / 255.0, 127 / 255.0, 1));
        m.put("steelblue", new RGBA(70 / 255.0, 130 / 255.0, 180 / 255.0, 1));
        m.put("tan", new RGBA(210 / 255.0, 180 / 255.0, 140 / 255.0, 1));
        m.put("teal", new RGBA(0 / 255.0, 128 / 255.0, 128 / 255.0, 1));
        m.put("thistle", new RGBA(216 / 255.0, 191 / 255.0, 216 / 255.0, 1));
        m.put("threeddarkshadow", new RGBA(51 / 255.0, 51 / 255.0, 51 / 255.0, 1));
        m.put("threedface", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("threedhighlight", new RGBA(255 / 255.0, 255 / 255.0, 255 / 255.0, 1));
        m.put("threedlightshadow", new RGBA(51 / 255.0, 51 / 255.0, 51 / 255.0, 1));
        m.put("threedshadow", new RGBA(51 / 255.0, 51 / 255.0, 51 / 255.0, 1));
        m.put("tomato", new RGBA(255 / 255.0, 99 / 255.0, 71 / 255.0, 1));
        m.put("transparent", RGBA.TRANSPARENT);
        m.put("turquoise", new RGBA(64 / 255.0, 224 / 255.0, 208 / 255.0, 1));
        m.put("violet", new RGBA(238 / 255.0, 130 / 255.0, 238 / 255.0, 1));
        m.put("wheat", new RGBA(245 / 255.0, 222 / 255.0, 179 / 255.0, 1));
        m.put("white", new RGBA(255 / 255.0, 255 / 255.0, 255 / 255.0, 1));
        m.put("whitesmoke", new RGBA(245 / 255.0, 245 / 255.0, 245 / 255.0, 1));
        m.put("window", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("windowframe", new RGBA(204 / 255.0, 204 / 255.0, 204 / 255.0, 1));
        m.put("windowtext", new RGBA(0 / 255.0, 0 / 255.0, 0 / 255.0, 1));
        m.put("yellow", new RGBA(255 / 255.0, 255 / 255.0, 0 / 255.0, 1));
        m.put("yellowgreen", new RGBA(154 / 255.0, 205 / 255.0, 50 / 255.0, 1));
        return Map.copyOf(m);
    }

    /** Parse a color string into RGBA. */
    public static RGBA color(String string, double opacity) {
        if (string == null || string.isBlank()) {
            return RGBA.TRANSPARENT;
        }

        string = string.strip();

        // Fast-path: hex color — skip toLowerCase() since parseInt handles case
        if (string.charAt(0) == '#') {
            int len = string.length();
            if (len == 9) {
                double r = Integer.parseInt(string, 1, 3, 16) / 255.0;
                double g = Integer.parseInt(string, 3, 5, 16) / 255.0;
                double b = Integer.parseInt(string, 5, 7, 16) / 255.0;
                double a = Integer.parseInt(string, 7, 9, 16) / 255.0;
                return new RGBA(r, g, b, a * opacity);
            }
            if (len == 7) {
                double r = Integer.parseInt(string, 1, 3, 16) / 255.0;
                double g = Integer.parseInt(string, 3, 5, 16) / 255.0;
                double b = Integer.parseInt(string, 5, 7, 16) / 255.0;
                return new RGBA(r, g, b, opacity);
            }
            if (len == 5) {
                double r = Character.digit(string.charAt(1), 16) / 15.0;
                double g = Character.digit(string.charAt(2), 16) / 15.0;
                double b = Character.digit(string.charAt(3), 16) / 15.0;
                double a = Character.digit(string.charAt(4), 16) / 15.0;
                return new RGBA(r, g, b, a * opacity);
            }
            if (len == 4) {
                double r = Character.digit(string.charAt(1), 16) / 15.0;
                double g = Character.digit(string.charAt(2), 16) / 15.0;
                double b = Character.digit(string.charAt(3), 16) / 15.0;
                return new RGBA(r, g, b, opacity);
            }
            return RGBA.BLACK;
        }

        string = string.toLowerCase();

        // Named color lookup
        RGBA named = NAMED_COLORS.get(string);
        if (named != null) {
            return (opacity >= 1.0 && named.a() >= 1.0)
                    ? named
                    : new RGBA(named.r(), named.g(), named.b(), named.a() * opacity);
        }

        // Functional notation — dispatch by prefix to avoid creating Matchers for wrong
        // types
        if (string.startsWith("rgba(")) {
            return parseRgb(functionBody(string, "rgba"), true, opacity);
        } else if (string.startsWith("rgb(")) {
            return parseRgb(functionBody(string, "rgb"), false, opacity);
        } else if (string.startsWith("hsla(")) {
            return parseHsl(functionBody(string, "hsla"), true, opacity);
        } else if (string.startsWith("hsl(")) {
            return parseHsl(functionBody(string, "hsl"), false, opacity);
        } else if (string.startsWith("hwb(")) {
            return parseHwb(functionBody(string, "hwb"), opacity);
        } else if (string.startsWith("lab(")) {
            return parseLab(functionBody(string, "lab"), opacity);
        } else if (string.startsWith("lch(")) {
            return parseLch(functionBody(string, "lch"), opacity);
        } else if (string.startsWith("oklab(")) {
            return parseOklab(functionBody(string, "oklab"), opacity);
        } else if (string.startsWith("oklch(")) {
            return parseOklch(functionBody(string, "oklch"), opacity);
        } else if (string.startsWith("color(")) {
            return parseColorFunction(functionBody(string, "color"), opacity);
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

    private static String functionBody(String string, String name) {
        if (!string.endsWith(")")) {
            return "";
        }
        return string.substring(name.length() + 1, string.length() - 1);
    }

    private static RGBA parseRgb(String body, boolean alphaAllowedWithCommas, double opacity) {
        if (body.indexOf(',') >= 0) {
            String[] parts = body.strip().split(",");
            if (parts.length != (alphaAllowedWithCommas ? 4 : 3)) {
                return RGBA.BLACK;
            }
            double a = alphaAllowedWithCommas ? parseAlpha(parts[3]) : 1.0;
            return rgba(parseColorComponent(parts[0]), parseColorComponent(parts[1]), parseColorComponent(parts[2]), a,
                    opacity);
        }

        ParsedComponents parsed = parseSpaceSeparated(body, 3);
        if (parsed == null) {
            return RGBA.BLACK;
        }
        double a = parsed.alpha() == null ? 1.0 : parseAlpha(parsed.alpha());
        return rgba(parseColorComponent(parsed.components()[0]), parseColorComponent(parsed.components()[1]),
                parseColorComponent(parsed.components()[2]), a, opacity);
    }

    private static RGBA parseHsl(String body, boolean alphaAllowedWithCommas, double opacity) {
        if (body.indexOf(',') >= 0) {
            String[] parts = body.strip().split(",");
            if (parts.length != (alphaAllowedWithCommas ? 4 : 3)) {
                return RGBA.BLACK;
            }
            double[] rgb = hslToRgb(parts[0], parts[1], parts[2]);
            double a = alphaAllowedWithCommas ? parseAlpha(parts[3]) : 1.0;
            return rgba(rgb[0], rgb[1], rgb[2], a, opacity);
        }

        ParsedComponents parsed = parseSpaceSeparated(body, 3);
        if (parsed == null) {
            return RGBA.BLACK;
        }
        double[] rgb = hslToRgb(parsed.components()[0], parsed.components()[1], parsed.components()[2]);
        double a = parsed.alpha() == null ? 1.0 : parseAlpha(parsed.alpha());
        return rgba(rgb[0], rgb[1], rgb[2], a, opacity);
    }

    private static RGBA parseHwb(String body, double opacity) {
        ParsedComponents parsed = parseSpaceSeparated(body, 3);
        if (parsed == null) {
            return RGBA.BLACK;
        }

        double[] hue = hslToRgb(parsed.components()[0], "100%", "50%");
        double whiteness = parseRequiredPercent(parsed.components()[1]);
        double blackness = parseRequiredPercent(parsed.components()[2]);
        double a = parsed.alpha() == null ? 1.0 : parseAlpha(parsed.alpha());
        if (whiteness + blackness >= 1.0) {
            double gray = whiteness / (whiteness + blackness);
            return rgba(gray, gray, gray, a, opacity);
        }

        double factor = 1.0 - whiteness - blackness;
        return rgba(hue[0] * factor + whiteness, hue[1] * factor + whiteness, hue[2] * factor + whiteness, a, opacity);
    }

    private static RGBA parseLab(String body, double opacity) {
        ParsedComponents parsed = parseSpaceSeparated(body, 3);
        if (parsed == null) {
            return RGBA.BLACK;
        }

        double lightness = parseLightness(parsed.components()[0], 100.0);
        double aAxis = parseSignedPercent(parsed.components()[1], 125.0);
        double bAxis = parseSignedPercent(parsed.components()[2], 125.0);
        double alpha = parsed.alpha() == null ? 1.0 : parseAlpha(parsed.alpha());
        return labToRgb(lightness, aAxis, bAxis, alpha, opacity);
    }

    private static RGBA parseLch(String body, double opacity) {
        ParsedComponents parsed = parseSpaceSeparated(body, 3);
        if (parsed == null) {
            return RGBA.BLACK;
        }

        double lightness = parseLightness(parsed.components()[0], 100.0);
        double chroma = parseSignedPercent(parsed.components()[1], 150.0);
        double hueRadians = Math.toRadians(parseHue(parsed.components()[2]));
        double alpha = parsed.alpha() == null ? 1.0 : parseAlpha(parsed.alpha());
        return labToRgb(lightness, chroma * Math.cos(hueRadians), chroma * Math.sin(hueRadians), alpha, opacity);
    }

    private static RGBA parseOklab(String body, double opacity) {
        ParsedComponents parsed = parseSpaceSeparated(body, 3);
        if (parsed == null) {
            return RGBA.BLACK;
        }

        double lightness = parseLightness(parsed.components()[0], 1.0);
        double aAxis = parseSignedPercent(parsed.components()[1], 0.4);
        double bAxis = parseSignedPercent(parsed.components()[2], 0.4);
        double alpha = parsed.alpha() == null ? 1.0 : parseAlpha(parsed.alpha());
        return oklabToRgb(lightness, aAxis, bAxis, alpha, opacity);
    }

    private static RGBA parseOklch(String body, double opacity) {
        ParsedComponents parsed = parseSpaceSeparated(body, 3);
        if (parsed == null) {
            return RGBA.BLACK;
        }

        double lightness = parseLightness(parsed.components()[0], 1.0);
        double chroma = parseSignedPercent(parsed.components()[1], 0.4);
        double hueRadians = Math.toRadians(parseHue(parsed.components()[2]));
        double alpha = parsed.alpha() == null ? 1.0 : parseAlpha(parsed.alpha());
        return oklabToRgb(lightness, chroma * Math.cos(hueRadians), chroma * Math.sin(hueRadians), alpha, opacity);
    }

    private static RGBA parseColorFunction(String body, double opacity) {
        String normalized = body.replace("/", " / ").strip();
        if (normalized.isEmpty()) {
            return RGBA.BLACK;
        }

        String[] tokens = normalized.split("\\s+");
        if (tokens.length < 4) {
            return RGBA.BLACK;
        }

        String colorSpace = tokens[0];
        StringBuilder components = new StringBuilder();
        for (int i = 1; i < tokens.length; i++) {
            if (components.length() > 0) {
                components.append(' ');
            }
            components.append(tokens[i]);
        }

        ParsedComponents parsed = parseSpaceSeparated(components.toString(), 3);
        if (parsed == null) {
            return RGBA.BLACK;
        }

        double c0 = parseUnitComponent(parsed.components()[0]);
        double c1 = parseUnitComponent(parsed.components()[1]);
        double c2 = parseUnitComponent(parsed.components()[2]);
        double alpha = parsed.alpha() == null ? 1.0 : parseAlpha(parsed.alpha());

        return switch (colorSpace) {
            case "srgb" -> rgba(c0, c1, c2, alpha, opacity);
            case "srgb-linear" -> linearSrgbToRgb(c0, c1, c2, alpha, opacity);
            case "display-p3" -> displayP3ToRgb(c0, c1, c2, alpha, opacity);
            case "a98-rgb" -> a98RgbToRgb(c0, c1, c2, alpha, opacity);
            case "prophoto-rgb" -> prophotoRgbToRgb(c0, c1, c2, alpha, opacity);
            case "rec2020" -> rec2020ToRgb(c0, c1, c2, alpha, opacity);
            case "xyz", "xyz-d65" -> xyzD65ToRgb(c0, c1, c2, alpha, opacity);
            case "xyz-d50" -> {
                double[] xyzD65 = d50ToD65(c0, c1, c2);
                yield xyzD65ToRgb(xyzD65[0], xyzD65[1], xyzD65[2], alpha, opacity);
            }
            default -> RGBA.BLACK;
        };
    }

    private static ParsedComponents parseSpaceSeparated(String body, int expectedComponents) {
        String normalized = body.replace("/", " / ").strip();
        if (normalized.isEmpty()) {
            return null;
        }

        String[] tokens = normalized.split("\\s+");
        String[] components = new String[expectedComponents];
        String alpha = null;
        int componentIndex = 0;
        boolean readingAlpha = false;
        for (String token : tokens) {
            if (token.equals("/")) {
                if (readingAlpha) {
                    return null;
                }
                readingAlpha = true;
                continue;
            }
            if (readingAlpha) {
                if (alpha != null) {
                    return null;
                }
                alpha = token;
            } else {
                if (componentIndex >= expectedComponents) {
                    return null;
                }
                components[componentIndex++] = token;
            }
        }

        if (componentIndex != expectedComponents || readingAlpha && alpha == null) {
            return null;
        }
        return new ParsedComponents(components, alpha);
    }

    private static double[] hslToRgb(String hPart, String sPart, String lPart) {
        double h = (((parseHue(hPart) % 360) + 360) % 360) / 360.0;
        double s = parseRequiredPercent(sPart);
        double l = parseRequiredPercent(lPart);
        if (s == 0) {
            return new double[]{l, l, l};
        }
        double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        double p = 2 * l - q;
        return new double[]{hueToRgb(p, q, h + 1.0 / 3), hueToRgb(p, q, h), hueToRgb(p, q, h - 1.0 / 3)};
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0)
            t += 1;
        if (t > 1)
            t -= 1;
        if (t < 1.0 / 6)
            return p + (q - p) * 6 * t;
        if (t < 1.0 / 2)
            return q;
        if (t < 2.0 / 3)
            return p + (q - p) * (2.0 / 3 - t) * 6;
        return p;
    }

    private static double parseColorComponent(String part) {
        part = part.strip();
        if (part.equals("none")) {
            return 0;
        }
        if (part.endsWith("%")) {
            return Double.parseDouble(part.substring(0, part.length() - 1)) / 100.0;
        }
        return Double.parseDouble(part) / 255.0;
    }

    private static double parseUnitComponent(String part) {
        part = part.strip();
        if (part.equals("none")) {
            return 0;
        }
        if (part.endsWith("%")) {
            return Double.parseDouble(part.substring(0, part.length() - 1)) / 100.0;
        }
        return Double.parseDouble(part);
    }

    private static double parseAlpha(String part) {
        part = part.strip();
        if (part.equals("none")) {
            return 1;
        }
        if (part.endsWith("%")) {
            return Double.parseDouble(part.substring(0, part.length() - 1)) / 100.0;
        }
        return Double.parseDouble(part);
    }

    private static double parseHue(String part) {
        part = part.strip();
        if (part.equals("none")) {
            return 0;
        }
        if (part.endsWith("deg")) {
            return Double.parseDouble(part.substring(0, part.length() - 3));
        }
        if (part.endsWith("grad")) {
            return Double.parseDouble(part.substring(0, part.length() - 4)) * 0.9;
        }
        if (part.endsWith("rad")) {
            return Math.toDegrees(Double.parseDouble(part.substring(0, part.length() - 3)));
        }
        if (part.endsWith("turn")) {
            return Double.parseDouble(part.substring(0, part.length() - 4)) * 360.0;
        }
        return Double.parseDouble(part);
    }

    private static double parseRequiredPercent(String part) {
        part = part.strip();
        if (part.equals("none")) {
            return 0;
        }
        if (!part.endsWith("%")) {
            throw new IllegalArgumentException("Expected percentage color component.");
        }
        return Double.parseDouble(part.substring(0, part.length() - 1)) / 100.0;
    }

    private static double parseLightness(String part, double percentScale) {
        part = part.strip();
        if (part.equals("none")) {
            return 0;
        }
        if (part.endsWith("%")) {
            return Double.parseDouble(part.substring(0, part.length() - 1)) / 100.0 * percentScale;
        }
        return Double.parseDouble(part);
    }

    private static double parseSignedPercent(String part, double percentScale) {
        part = part.strip();
        if (part.equals("none")) {
            return 0;
        }
        if (part.endsWith("%")) {
            return Double.parseDouble(part.substring(0, part.length() - 1)) / 100.0 * percentScale;
        }
        return Double.parseDouble(part);
    }

    private static RGBA labToRgb(double lightness, double aAxis, double bAxis, double alpha, double opacity) {
        double fy = (lightness + 16.0) / 116.0;
        double fx = fy + aAxis / 500.0;
        double fz = fy - bAxis / 200.0;

        double x = D50_X * labInverse(fx);
        double y = D50_Y * labInverse(fy);
        double z = D50_Z * labInverse(fz);
        double[] xyzD65 = d50ToD65(x, y, z);
        return xyzD65ToRgb(xyzD65[0], xyzD65[1], xyzD65[2], alpha, opacity);
    }

    private static double labInverse(double value) {
        double cube = value * value * value;
        return cube > LAB_EPSILON ? cube : (116.0 * value - 16.0) / LAB_KAPPA;
    }

    private static RGBA oklabToRgb(double lightness, double aAxis, double bAxis, double alpha, double opacity) {
        double lPrime = lightness + 0.3963377774 * aAxis + 0.2158037573 * bAxis;
        double mPrime = lightness - 0.1055613458 * aAxis - 0.0638541728 * bAxis;
        double sPrime = lightness - 0.0894841775 * aAxis - 1.2914855480 * bAxis;

        double l = lPrime * lPrime * lPrime;
        double m = mPrime * mPrime * mPrime;
        double s = sPrime * sPrime * sPrime;

        double r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
        double g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
        double b = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;
        return linearSrgbToRgb(r, g, b, alpha, opacity);
    }

    private static RGBA displayP3ToRgb(double r, double g, double b, double alpha, double opacity) {
        double linearR = srgbToLinear(r);
        double linearG = srgbToLinear(g);
        double linearB = srgbToLinear(b);
        double x = 0.4865709486482162 * linearR + 0.26566769316909306 * linearG + 0.1982172852343625 * linearB;
        double y = 0.2289745640697488 * linearR + 0.6917385218365064 * linearG + 0.079286914093745 * linearB;
        double z = 0.04511338185890264 * linearG + 1.043944368900976 * linearB;
        return xyzD65ToRgb(x, y, z, alpha, opacity);
    }

    private static RGBA a98RgbToRgb(double r, double g, double b, double alpha, double opacity) {
        double linearR = Math.copySign(Math.pow(Math.abs(r), 563.0 / 256.0), r);
        double linearG = Math.copySign(Math.pow(Math.abs(g), 563.0 / 256.0), g);
        double linearB = Math.copySign(Math.pow(Math.abs(b), 563.0 / 256.0), b);
        double x = 0.5766690429 * linearR + 0.1855582379 * linearG + 0.1882286462 * linearB;
        double y = 0.2973449753 * linearR + 0.6273635663 * linearG + 0.0752914585 * linearB;
        double z = 0.0270313614 * linearR + 0.0706888525 * linearG + 0.9913375368 * linearB;
        return xyzD65ToRgb(x, y, z, alpha, opacity);
    }

    private static RGBA prophotoRgbToRgb(double r, double g, double b, double alpha, double opacity) {
        double linearR = prophotoToLinear(r);
        double linearG = prophotoToLinear(g);
        double linearB = prophotoToLinear(b);
        double x = 0.7977604897 * linearR + 0.1351858372 * linearG + 0.0313493496 * linearB;
        double y = 0.2880711282 * linearR + 0.7118432178 * linearG + 0.0000856539 * linearB;
        double z = 0.8251046025 * linearB;
        double[] xyzD65 = d50ToD65(x, y, z);
        return xyzD65ToRgb(xyzD65[0], xyzD65[1], xyzD65[2], alpha, opacity);
    }

    private static double prophotoToLinear(double component) {
        double absolute = Math.abs(component);
        double linear = absolute <= 16.0 / 512.0 ? absolute / 16.0 : Math.pow(absolute, 1.8);
        return Math.copySign(linear, component);
    }

    private static RGBA rec2020ToRgb(double r, double g, double b, double alpha, double opacity) {
        double linearR = rec2020ToLinear(r);
        double linearG = rec2020ToLinear(g);
        double linearB = rec2020ToLinear(b);
        double x = 0.6369580483 * linearR + 0.1446169036 * linearG + 0.1688809752 * linearB;
        double y = 0.2627002120 * linearR + 0.6779980715 * linearG + 0.0593017165 * linearB;
        double z = 0.0280726930 * linearG + 1.0609850577 * linearB;
        return xyzD65ToRgb(x, y, z, alpha, opacity);
    }

    private static double rec2020ToLinear(double component) {
        double alpha = 1.09929682680944;
        double beta = 0.018053968510807;
        double absolute = Math.abs(component);
        double linear = absolute < beta * 4.5 ? absolute / 4.5 : Math.pow((absolute + alpha - 1.0) / alpha, 1.0 / 0.45);
        return Math.copySign(linear, component);
    }

    private static RGBA xyzD65ToRgb(double x, double y, double z, double alpha, double opacity) {
        double r = 3.2409699419045226 * x - 1.5373831775700935 * y - 0.4986107602930034 * z;
        double g = -0.9692436362808796 * x + 1.8759675015077202 * y + 0.0415550574071756 * z;
        double b = 0.0556300796969936 * x - 0.2039769588889765 * y + 1.0569715142428786 * z;
        return linearSrgbToRgb(r, g, b, alpha, opacity);
    }

    private static double[] d50ToD65(double x, double y, double z) {
        return new double[]{0.9555766 * x - 0.0230393 * y + 0.0631636 * z,
                -0.0282895 * x + 1.0099416 * y + 0.0210077 * z, 0.0122982 * x - 0.0204830 * y + 1.3299098 * z};
    }

    private static RGBA linearSrgbToRgb(double r, double g, double b, double alpha, double opacity) {
        return rgba(linearToSrgb(r), linearToSrgb(g), linearToSrgb(b), alpha, opacity);
    }

    private static double srgbToLinear(double component) {
        double absolute = Math.abs(component);
        double linear = absolute <= 0.04045 ? absolute / 12.92 : Math.pow((absolute + 0.055) / 1.055, 2.4);
        return Math.copySign(linear, component);
    }

    private static double linearToSrgb(double component) {
        double absolute = Math.abs(component);
        double encoded = absolute <= 0.0031308 ? 12.92 * absolute : 1.055 * Math.pow(absolute, 1.0 / 2.4) - 0.055;
        return Math.copySign(encoded, component);
    }

    private static RGBA rgba(double r, double g, double b, double alpha, double opacity) {
        return new RGBA(clamp01(r), clamp01(g), clamp01(b), clamp01(alpha * opacity));
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}

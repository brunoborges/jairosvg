package io.brunoborges.jairosvg.css;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SVG color parsing. Converts color strings to RGBA tuples (0.0–1.0).
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

    private static final Map<String, RGBA> NAMED_COLORS = buildNamedColors();

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

        // Fast-path: hex color — skip strip()/toLowerCase() since parseInt handles
        // case
        if (string.charAt(0) == '#') {
            int len = string.length();
            if (len == 7) {
                double r = Integer.parseInt(string, 1, 3, 16) / 255.0;
                double g = Integer.parseInt(string, 3, 5, 16) / 255.0;
                double b = Integer.parseInt(string, 5, 7, 16) / 255.0;
                return new RGBA(r, g, b, opacity);
            }
            if (len == 4) {
                double r = Character.digit(string.charAt(1), 16) / 15.0;
                double g = Character.digit(string.charAt(2), 16) / 15.0;
                double b = Character.digit(string.charAt(3), 16) / 15.0;
                return new RGBA(r, g, b, opacity);
            }
            return RGBA.BLACK;
        }

        string = string.strip().toLowerCase();

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
        } else if (string.startsWith("rgb(")) {
            Matcher m = RGB_PATTERN.matcher(string);
            if (m.find()) {
                String[] parts = m.group(1).strip().split(",");
                if (parts.length == 3) {
                    double r = parseColorComponent(parts[0]);
                    double g = parseColorComponent(parts[1]);
                    double b = parseColorComponent(parts[2]);
                    return new RGBA(r, g, b, opacity);
                }
            }
        } else if (string.startsWith("hsla(")) {
            Matcher m = HSLA_PATTERN.matcher(string);
            if (m.find()) {
                String[] parts = m.group(1).strip().split(",");
                if (parts.length == 4) {
                    double[] rgb = hslToRgb(parts[0], parts[1], parts[2]);
                    double a = Double.parseDouble(parts[3].strip());
                    return new RGBA(rgb[0], rgb[1], rgb[2], a * opacity);
                }
            }
        } else if (string.startsWith("hsl(")) {
            Matcher m = HSL_PATTERN.matcher(string);
            if (m.find()) {
                String[] parts = m.group(1).strip().split(",");
                if (parts.length == 3) {
                    double[] rgb = hslToRgb(parts[0], parts[1], parts[2]);
                    return new RGBA(rgb[0], rgb[1], rgb[2], opacity);
                }
            }
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
        if (part.endsWith("%")) {
            return Double.parseDouble(part.substring(0, part.length() - 1)) / 100.0;
        }
        return Double.parseDouble(part) / 255.0;
    }
}

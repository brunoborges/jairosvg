package io.brunoborges.jairosvg.css;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SVG color parsing. Converts color strings to RGBA tuples (0.0–1.0). Port of
 * CairoSVG colors.py
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

    private static final Map<String, RGBA> NAMED_COLORS = loadNamedColors();

    private Colors() {
    }

    private static Map<String, RGBA> loadNamedColors() {
        var props = new Properties();
        try (var in = Colors.class.getResourceAsStream("colors.properties")) {
            if (in == null) {
                throw new ExceptionInInitializerError("colors.properties not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        var map = new HashMap<String, RGBA>(props.size() * 2);
        for (var name : props.stringPropertyNames()) {
            String value = props.getProperty(name);
            if ("transparent".equals(value)) {
                map.put(name, RGBA.TRANSPARENT);
            } else {
                String[] parts = value.split(",");
                double r = Integer.parseInt(parts[0]) / 255.0;
                double g = Integer.parseInt(parts[1]) / 255.0;
                double b = Integer.parseInt(parts[2]) / 255.0;
                double a = parts.length > 3 ? Double.parseDouble(parts[3]) : 1.0;
                map.put(name, new RGBA(r, g, b, a));
            }
        }
        return Map.copyOf(map);
    }

    /** Parse a color string into RGBA. */
    public static RGBA color(String string, double opacity) {
        if (string == null || string.isBlank()) {
            return RGBA.TRANSPARENT;
        }

        string = string.strip().toLowerCase();

        RGBA named = NAMED_COLORS.get(string);
        if (named != null) {
            return (opacity >= 1.0 && named.a() >= 1.0)
                    ? named
                    : new RGBA(named.r(), named.g(), named.b(), named.a() * opacity);
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
            double r = Integer.parseInt(string, 1, 3, 16) / 255.0;
            double g = Integer.parseInt(string, 3, 5, 16) / 255.0;
            double b = Integer.parseInt(string, 5, 7, 16) / 255.0;
            return new RGBA(r, g, b, opacity);
        }

        m = HEX_RGB.matcher(string);
        if (m.find()) {
            double r = Character.digit(string.charAt(1), 16) / 15.0;
            double g = Character.digit(string.charAt(2), 16) / 15.0;
            double b = Character.digit(string.charAt(3), 16) / 15.0;
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

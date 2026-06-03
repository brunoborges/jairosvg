package io.brunoborges.jairosvg;

/**
 * Library-owned wrapper for type-safe CSS color values passed to JairoSVG APIs.
 */
@FunctionalInterface
public interface CssColor {

    /**
     * Return this color serialized as a CSS color string.
     */
    String toCssColorString();

    /**
     * Create a {@link CssColor} from a CSS color string.
     */
    static CssColor of(String color) {
        return () -> color;
    }
}

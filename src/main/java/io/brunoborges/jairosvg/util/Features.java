package io.brunoborges.jairosvg.util;

import java.util.Locale;
import java.util.Set;

/**
 * SVG conditional processing helpers. Port of CairoSVG features.py
 */
public final class Features {

    private static final String ROOT = "http://www.w3.org/TR/SVG11/feature";
    private static final String LOCALE = Locale.getDefault().toString().replace('_', '-');

    private static final Set<String> SUPPORTED_FEATURES = Set.of(ROOT + "#SVG", ROOT + "#SVG-static",
            ROOT + "#CoreAttribute", ROOT + "#Structure", ROOT + "#BasicStructure", ROOT + "#ConditionalProcessing",
            ROOT + "#Image", ROOT + "#Style", ROOT + "#ViewportAttribute", ROOT + "#Shape", ROOT + "#BasicText",
            ROOT + "#BasicPaintAttribute", ROOT + "#OpacityAttribute", ROOT + "#BasicGraphicsAttribute",
            ROOT + "#Marker", ROOT + "#Gradient", ROOT + "#Pattern", ROOT + "#Clip", ROOT + "#BasicClip",
            ROOT + "#Mask");

    private Features() {
    }

    /** Check whether all listed features are supported. */
    public static boolean hasFeatures(String features) {
        if (features == null || features.isBlank())
            return true;
        for (String feature : features.strip().split("\\s+")) {
            if (!SUPPORTED_FEATURES.contains(feature))
                return false;
        }
        return true;
    }

    /** Check whether one of the languages is part of user locales. */
    public static boolean supportLanguages(String languages) {
        if (languages == null)
            return true;
        for (String lang : languages.split(",")) {
            lang = lang.strip();
            if (!lang.isEmpty() && LOCALE.startsWith(lang))
                return true;
        }
        return false;
    }

    /** Check the node matches the conditional processing attributes. */
    public static boolean matchFeatures(String requiredFeatures, String requiredExtensions, String systemLanguage) {
        if (requiredExtensions != null && !requiredExtensions.isEmpty())
            return false;
        if (requiredFeatures != null && !requiredFeatures.isEmpty() && !hasFeatures(requiredFeatures))
            return false;
        if (systemLanguage != null && !systemLanguage.isEmpty() && !supportLanguages(systemLanguage))
            return false;
        return true;
    }
}

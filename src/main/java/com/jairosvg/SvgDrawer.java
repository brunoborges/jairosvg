package com.jairosvg;

import static com.jairosvg.Helpers.*;

/**
 * Root SVG tag drawer.
 * Port of CairoSVG svg.py
 */
public final class SvgDrawer {

    private SvgDrawer() {}

    /** Draw a svg node (nested SVG). */
    public static void svg(Surface surface, Node node) {
        if (node.parent != null) {
            double[] nf = nodeFormat(surface, node);
            double width = nf[0], height = nf[1];
            double[] viewbox = getViewbox(nf);
            surface.setContextSize(width, height, viewbox, node);
        }
    }
}

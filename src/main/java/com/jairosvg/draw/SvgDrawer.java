package com.jairosvg.draw;

import com.jairosvg.dom.Node;
import com.jairosvg.surface.Surface;

import static com.jairosvg.util.Helpers.*;

/**
 * Root SVG tag drawer. Port of CairoSVG svg.py
 */
public final class SvgDrawer {

    private SvgDrawer() {
    }

    /** Draw a svg node (nested SVG). */
    public static void svg(Surface surface, Node node) {
        if (node.parent != null) {
            double x = size(surface, node.get("x", "0"), "x");
            double y = size(surface, node.get("y", "0"), "y");
            surface.context.translate(x, y);
            double[] nf = nodeFormat(surface, node);
            double width = nf[0], height = nf[1];
            double[] viewbox = getViewbox(nf);
            surface.setContextSize(width, height, viewbox, node);
        }
    }
}

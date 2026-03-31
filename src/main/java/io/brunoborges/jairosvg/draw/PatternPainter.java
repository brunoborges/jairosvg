package io.brunoborges.jairosvg.draw;

import static io.brunoborges.jairosvg.util.Helpers.parsePercent;
import static io.brunoborges.jairosvg.util.Helpers.parsePercent;
import static io.brunoborges.jairosvg.util.Helpers.size;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import io.brunoborges.jairosvg.dom.BoundingBox;
import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.surface.Surface;
import io.brunoborges.jairosvg.util.Helpers;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * Pattern rendering for SVG pattern elements.
 */
public final class PatternPainter {

    private PatternPainter() {
    }

    /** Draw a pattern. */
    public static boolean drawPattern(Surface surface, Node node, String name, double opacity) {
        Node patternNode = surface.patterns.get(name);
        if (patternNode == null)
            return false;

        // Follow href chain
        String href = patternNode.getHref();
        if (href != null && !href.isEmpty()) {
            String refId = UrlHelper.parseUrl(href).fragment();
            if (refId != null && surface.patterns.containsKey(refId)) {
                Node refNode = surface.patterns.get(refId);
                if (patternNode.children.isEmpty()) {
                    patternNode.children = new ArrayList<>(refNode.children);
                }
                for (var entry : refNode.entries()) {
                    if (!patternNode.has(entry.getKey())) {
                        patternNode.set(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        if (patternNode.children.isEmpty())
            return false;

        boolean userSpace = "userSpaceOnUse".equals(patternNode.get("patternUnits"));

        double patX, patY, patW, patH;
        if (userSpace) {
            patX = size(surface, patternNode.get("x", "0"), "x");
            patY = size(surface, patternNode.get("y", "0"), "y");
            patW = size(surface, patternNode.get("width", "0"), "x");
            patH = size(surface, patternNode.get("height", "0"), "y");
        } else {
            BoundingBox.Box bb = BoundingBox.calculate(surface, node);
            if (bb == null || !BoundingBox.isNonEmpty(bb))
                return false;

            double pctX = parsePercent(patternNode.get("x", "0"));
            double pctY = parsePercent(patternNode.get("y", "0"));
            double pctW = parsePercent(patternNode.get("width", "0"));
            double pctH = parsePercent(patternNode.get("height", "0"));

            patX = bb.minX() + pctX * bb.width();
            patY = bb.minY() + pctY * bb.height();
            patW = pctW * bb.width();
            patH = pctH * bb.height();
        }

        if (patW <= 0 || patH <= 0)
            return false;

        // Render pattern content into a BufferedImage
        int imgW = Math.min(4096, Math.max(1, (int) Math.ceil(patW)));
        int imgH = Math.min(4096, Math.max(1, (int) Math.ceil(patH)));
        BufferedImage patImage = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D patG2d = patImage.createGraphics();
        patG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        patG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        // Apply overall opacity to all pattern content
        patG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));

        // Save surface state
        Graphics2D savedContext = surface.context;
        GeneralPath savedPath = surface.path;
        double savedWidth = surface.contextWidth;
        double savedHeight = surface.contextHeight;

        surface.context = patG2d;
        surface.path = new GeneralPath();
        surface.contextWidth = patW;
        surface.contextHeight = patH;

        try {
            // Translate so pattern children (in user space) are drawn relative to the tile
            // origin
            if (patX != 0 || patY != 0) {
                patG2d.translate(-patX, -patY);
            }

            // Draw pattern children
            for (Node child : patternNode.children) {
                surface.draw(child);
            }
        } finally {
            // Restore surface state
            surface.context = savedContext;
            surface.path = savedPath;
            surface.contextWidth = savedWidth;
            surface.contextHeight = savedHeight;
            patG2d.dispose();
        }

        // Create anchor rectangle (untransformed pattern region)
        Rectangle2D anchor = new Rectangle2D.Double(patX, patY, patW, patH);

        // Parse patternTransform and store on surface for application during
        // fill/stroke. The transform is applied to the Graphics2D coordinate system
        // so that the entire tiling grid is transformed, rather than transforming
        // individual tile images (which breaks for rotation/skew combinations).
        String ptStr = patternNode.get("patternTransform");
        if (ptStr != null && !ptStr.isEmpty()) {
            AffineTransform pt = Helpers.parseTransform(surface, ptStr);
            if (pt != null && !pt.isIdentity()) {
                surface.paintTransform = pt;
            }
        }

        // Create TexturePaint
        TexturePaint texturePaint = new TexturePaint(patImage, anchor);
        surface.context.setPaint(texturePaint);
        return true;
    }

}

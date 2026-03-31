package io.brunoborges.jairosvg.draw;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.surface.Surface;

/**
 * Mask rendering and luminance-to-alpha compositing.
 */
public final class MaskPainter {

    // SVG mask luminance coefficients (BT.709, scaled to ×256 for integer math)
    private static final int LUMINANCE_RED_COEFF_256 = 54; // 0.2126 × 256
    private static final int LUMINANCE_GREEN_COEFF_256 = 183; // 0.7152 × 256
    private static final int LUMINANCE_BLUE_COEFF_256 = 19; // 0.0722 × 256

    private MaskPainter() {
    }

    /** Render and apply luminance mask to an off-screen source image. */
    public static BufferedImage paintMask(Surface surface, Node node, String name, BufferedImage sourceImage,
            java.awt.geom.AffineTransform subRegionTransform) {
        Node maskNode = surface.masks.get(name);
        if (maskNode == null) {
            return sourceImage;
        }

        int w = sourceImage.getWidth();
        int h = sourceImage.getHeight();

        // TYPE_INT_ARGB mask buffer — reuse full-canvas buffer; sub-region buffers are
        // temporary and not cached to avoid evicting the reusable full-canvas instance.
        boolean isSubRegion = subRegionTransform != null;
        BufferedImage maskImage = surface.maskBuffer;
        if (!isSubRegion && maskImage != null && maskImage.getWidth() == w && maskImage.getHeight() == h) {
            java.util.Arrays.fill(((DataBufferInt) maskImage.getRaster().getDataBuffer()).getData(), 0);
        } else {
            maskImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            if (!isSubRegion) {
                surface.maskBuffer = maskImage;
            }
        }
        Graphics2D maskG2d = maskImage.createGraphics();
        maskG2d.setRenderingHints(surface.context.getRenderingHints());
        if (subRegionTransform != null) {
            maskG2d.setTransform(subRegionTransform);
        }

        Graphics2D savedContext = surface.context;
        GeneralPath savedPath = surface.path;
        double savedWidth = surface.contextWidth;
        double savedHeight = surface.contextHeight;

        surface.context = maskG2d;
        surface.path = new GeneralPath();
        surface.contextWidth = savedWidth;
        surface.contextHeight = savedHeight;

        try {
            for (Node child : maskNode.children) {
                surface.draw(child);
            }
        } finally {
            surface.context = savedContext;
            surface.path = savedPath;
            surface.contextWidth = savedWidth;
            surface.contextHeight = savedHeight;
            maskG2d.dispose();
        }

        // Single combined pass: apply BT.709 luminance of mask pixel to source alpha.
        // Operates on the sub-region buffers — typically far smaller than the full
        // image.
        int[] sourcePixels = ((DataBufferInt) sourceImage.getRaster().getDataBuffer()).getData();
        int[] maskPixels = ((DataBufferInt) maskImage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < sourcePixels.length; i++) {
            int src = sourcePixels[i];
            int srcA = src >>> 24;
            if (srcA == 0)
                continue;
            int m = maskPixels[i];
            int ma = m >>> 24;
            if (ma == 0) {
                sourcePixels[i] = 0;
                continue;
            }
            int mr = (m >> 16) & 0xFF;
            int mg = (m >> 8) & 0xFF;
            int mb = m & 0xFF;
            int luminance256 = LUMINANCE_RED_COEFF_256 * mr + LUMINANCE_GREEN_COEFF_256 * mg
                    + LUMINANCE_BLUE_COEFF_256 * mb;
            if (luminance256 == 0) {
                sourcePixels[i] = 0;
                continue;
            }
            int luminance = (luminance256 + 128) >> 8;
            if (luminance == 255 && ma == 255)
                continue; // fast path: fully opaque white mask
            int maskAlpha = ma * luminance;
            maskAlpha = (maskAlpha + 1 + (maskAlpha >> 8)) >> 8;
            int combined = srcA * maskAlpha;
            int outA = (combined + 1 + (combined >> 8)) >> 8;
            sourcePixels[i] = (outA << 24) | (src & 0x00FFFFFF);
        }
        return sourceImage;
    }
}

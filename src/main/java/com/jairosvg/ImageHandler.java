package com.jairosvg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import static com.jairosvg.Helpers.*;

/**
 * SVG image element handler.
 * Port of CairoSVG image.py
 */
public final class ImageHandler {

    private ImageHandler() {}

    /** Draw an image node. */
    public static void image(Surface surface, Node node) {
        String href = node.getHref();
        if (href == null || href.isEmpty()) return;

        String baseUrl = node.get("{http://www.w3.org/XML/1998/namespace}base");
        if (baseUrl == null && node.url != null) {
            int lastSlash = node.url.lastIndexOf('/');
            baseUrl = lastSlash >= 0 ? node.url.substring(0, lastSlash + 1) : null;
        }

        UrlHelper.ParsedUrl url = UrlHelper.parseUrl(href, baseUrl);
        byte[] imageBytes;
        try {
            imageBytes = node.fetchUrl(url, "image/*");
        } catch (IOException e) {
            return;
        }

        if (imageBytes == null || imageBytes.length < 5) return;

        double x = size(surface, node.get("x"), "x");
        double y = size(surface, node.get("y"), "y");
        double width = size(surface, node.get("width"), "x");
        double height = size(surface, node.get("height"), "y");

        // Check if it's an SVG image
        if (isSvgContent(imageBytes)) {
            try {
                Node tree = Node.parseTree(imageBytes, url.getUrl(), node.urlFetcher, node.unsafe);
                double[] nf = nodeFormat(surface, tree, false);
                double treeWidth = nf[0], treeHeight = nf[1];
                if (treeWidth == 0) treeWidth = width;
                if (treeHeight == 0) treeHeight = height;

                node.imageWidth = treeWidth;
                node.imageHeight = treeHeight;
                double[] ratio = preserveRatio(surface, node, width, height);

                var savedTransform = surface.context.getTransform();
                surface.context.translate(x, y);
                surface.context.scale(ratio[0], ratio[1]);
                surface.context.translate(ratio[2], ratio[3]);
                surface.draw(tree);
                surface.context.setTransform(savedTransform);
            } catch (Exception e) {
                // Skip invalid SVG images
            }
            return;
        }

        // Raster image
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) return;

            node.imageWidth = img.getWidth();
            node.imageHeight = img.getHeight();
            if (width == 0) width = node.imageWidth;
            if (height == 0) height = node.imageHeight;

            double[] ratio = preserveRatio(surface, node, width, height);
            double opacity = 1;
            String opacityStr = node.get("opacity");
            if (opacityStr != null) opacity = Double.parseDouble(opacityStr);

            var savedTransform = surface.context.getTransform();
            var savedComposite = surface.context.getComposite();

            surface.context.translate(x, y);
            surface.context.scale(ratio[0], ratio[1]);
            surface.context.translate(ratio[2], ratio[3]);

            if (opacity < 1) {
                surface.context.setComposite(
                    java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) opacity));
            }

            surface.context.drawImage(img, 0, 0, null);

            surface.context.setComposite(savedComposite);
            surface.context.setTransform(savedTransform);
        } catch (IOException e) {
            // Skip unreadable images
        }
    }

    private static boolean isSvgContent(byte[] data) {
        if (data.length < 5) return false;
        String start = new String(data, 0, Math.min(data.length, 256));
        return start.contains("<svg") || start.startsWith("<?xml") || start.startsWith("<!DOC")
            || (data[0] == 0x1f && data[1] == (byte) 0x8b); // gzip
    }
}

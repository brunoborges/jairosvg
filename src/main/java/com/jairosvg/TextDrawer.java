package com.jairosvg;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static com.jairosvg.Helpers.*;

/**
 * SVG text rendering.
 * Port of CairoSVG text.py
 */
public final class TextDrawer {

    private TextDrawer() {}

    /** Draw a text node. */
    public static void text(Surface surface, Node node) {
        text(surface, node, false);
    }

    /** Draw a text node, optionally as filled text. */
    public static void text(Surface surface, Node node, boolean drawAsText) {
        String fontFamily = node.get("font-family", "SansSerif");
        fontFamily = fontFamily.split(",")[0].strip().replace("'", "").replace("\"", "");

        // Map common generic font families
        fontFamily = switch (fontFamily.toLowerCase()) {
            case "sans-serif" -> "SansSerif";
            case "serif" -> "Serif";
            case "monospace" -> "Monospaced";
            default -> fontFamily;
        };

        int fontStyle = Font.PLAIN;
        String styleStr = node.get("font-style", "normal");
        if ("italic".equals(styleStr) || "oblique".equals(styleStr)) {
            fontStyle |= Font.ITALIC;
        }

        String weightStr = node.get("font-weight", "normal");
        if ("bold".equals(weightStr) || "bolder".equals(weightStr)) {
            fontStyle |= Font.BOLD;
        } else if (weightStr.matches("\\d+") && Integer.parseInt(weightStr) >= 550) {
            fontStyle |= Font.BOLD;
        }

        Font font = new Font(fontFamily, fontStyle, 1);
        font = font.deriveFont((float) surface.fontSize);

        FontRenderContext frc = surface.context.getFontRenderContext();

        String textContent = node.text;
        if (textContent == null || textContent.isEmpty()) {
            // Process children (tspan, etc.)
            for (Node child : node.children) {
                text(surface, child, drawAsText);
            }
            return;
        }

        // Parse position attributes
        double startX = 0, startY = 0;
        String xStr = node.get("x");
        String yStr = node.get("y");
        String dxStr = node.get("dx");
        String dyStr = node.get("dy");

        if (xStr != null) startX = size(surface, xStr.split("\\s+")[0], "x");
        else startX = surface.cursorPosition[0];

        if (yStr != null) startY = size(surface, yStr.split("\\s+")[0], "y");
        else startY = surface.cursorPosition[1];

        if (dxStr != null) startX += size(surface, dxStr.split("\\s+")[0], "x");
        if (dyStr != null) startY += size(surface, dyStr.split("\\s+")[0], "y");

        // Text anchor alignment
        String textAnchor = node.get("text-anchor");
        if (textAnchor != null) {
            Rectangle2D bounds = font.getStringBounds(textContent, frc);
            if ("middle".equals(textAnchor)) {
                startX -= bounds.getWidth() / 2;
            } else if ("end".equals(textAnchor)) {
                startX -= bounds.getWidth();
            }
        }

        // Apply letter spacing
        double letterSpacing = size(surface, node.get("letter-spacing"));

        AffineTransform savedTransform = surface.context.getTransform();

        if (letterSpacing != 0) {
            double curX = startX;
            for (int i = 0; i < textContent.length(); i++) {
                String ch = String.valueOf(textContent.charAt(i));
                if (!ch.isBlank()) {
                    if (drawAsText) {
                        surface.context.setFont(font);
                        surface.context.drawString(ch, (float) curX, (float) startY);
                    } else {
                        GlyphVector gv = font.createGlyphVector(frc, ch);
                        surface.path.append(gv.getOutline((float) curX, (float) startY), false);
                    }
                }
                Rectangle2D charBounds = font.getStringBounds(ch, frc);
                curX += charBounds.getWidth() + letterSpacing;
            }
            surface.cursorPosition[0] = curX;
        } else {
            if (drawAsText) {
                surface.context.setFont(font);
                surface.context.drawString(textContent, (float) startX, (float) startY);
            } else {
                GlyphVector gv = font.createGlyphVector(frc, textContent);
                surface.path.append(gv.getOutline((float) startX, (float) startY), false);
            }
            Rectangle2D bounds = font.getStringBounds(textContent, frc);
            surface.cursorPosition[0] = startX + bounds.getWidth();
        }

        surface.cursorPosition[1] = startY;
    }
}

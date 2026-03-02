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

    /** Extract the primary font family name from a font-family attribute value. */
    private static String parseFontFamily(Node node) {
        return node.get("font-family", "SansSerif")
            .split(",")[0].strip().replace("'", "").replace("\"", "");
    }

    /** Draw a text node. */
    public static void text(Surface surface, Node node) {
        text(surface, node, false);
    }

    /** Draw a text node, optionally as filled text. */
    public static void text(Surface surface, Node node, boolean drawAsText) {
        String fontFamily = parseFontFamily(node);

        // Check for SVG font match before mapping to AWT fonts
        SvgFont svgFont = surface.fonts.get(fontFamily);

        // Map common generic font families (for AWT fallback)
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
            // Set initial cursor position from this node's x/y
            String parentX = node.get("x");
            String parentY = node.get("y");
            String parentDx = node.get("dx");
            String parentDy = node.get("dy");

            if (parentX != null) surface.cursorPosition[0] = size(surface, parentX.split("\\s+")[0], "x");
            if (parentY != null) surface.cursorPosition[1] = size(surface, parentY.split("\\s+")[0], "y");
            if (parentDx != null) surface.cursorPosition[0] += size(surface, parentDx.split("\\s+")[0], "x");
            if (parentDy != null) surface.cursorPosition[1] += size(surface, parentDy.split("\\s+")[0], "y");

            // Adjust cursor for text-anchor using total width of all children
            String textAnchor = node.get("text-anchor");
            if (textAnchor != null && ("middle".equals(textAnchor) || "end".equals(textAnchor))) {
                double totalWidth = measureChildrenWidth(surface, node, frc);
                if ("middle".equals(textAnchor)) {
                    surface.cursorPosition[0] -= totalWidth / 2;
                } else {
                    surface.cursorPosition[0] -= totalWidth;
                }
            }

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

        // Text anchor alignment (only when node has its own x position)
        String textAnchor = node.get("text-anchor");
        if (textAnchor != null && xStr != null) {
            double textWidth = svgFont != null
                ? measureSvgFontWidth(svgFont, textContent, surface.fontSize)
                : font.getStringBounds(textContent, frc).getWidth();
            if ("middle".equals(textAnchor)) {
                startX -= textWidth / 2;
            } else if ("end".equals(textAnchor)) {
                startX -= textWidth;
            }
        }

        // Apply letter spacing
        double letterSpacing = size(surface, node.get("letter-spacing"));

        AffineTransform savedTransform = surface.context.getTransform();

        if (svgFont != null) {
            // Render using SVG font glyphs as paths (greedy longest-match for multi-char unicode)
            double curX = startX;
            int i = 0;
            while (i < textContent.length()) {
                SvgFont.GlyphMatch match = svgFont.getGlyph(textContent, i);
                SvgFont.Glyph glyph = match.glyph();
                if (glyph != null) {
                    java.awt.geom.GeneralPath glyphPath =
                        svgFont.buildGlyphPath(glyph, surface.fontSize, curX, startY);
                    if (glyphPath != null) {
                        surface.path.append(glyphPath, false);
                    }
                }
                curX += svgFont.getAdvance(glyph, surface.fontSize) + letterSpacing;
                i += match.charsConsumed();
            }
            surface.cursorPosition[0] = curX;
        } else if (letterSpacing != 0) {
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

    /** Measure total text width of all children for text-anchor calculation. */
    private static double measureChildrenWidth(Surface surface, Node parent, FontRenderContext frc) {
        double totalWidth = 0;
        for (Node child : parent.children) {
            if (child.text != null && !child.text.isEmpty()) {
                String family = parseFontFamily(child);
                SvgFont svgF = surface.fonts.get(family);
                if (svgF != null) {
                    totalWidth += measureSvgFontWidth(svgF, child.text, surface.fontSize);
                } else {
                    totalWidth += resolveFont(surface, child).getStringBounds(child.text, frc).getWidth();
                }
            }
            if (child.children != null && !child.children.isEmpty()) {
                totalWidth += measureChildrenWidth(surface, child, frc);
            }
        }
        return totalWidth;
    }

    /** Measure the width of text rendered with an SVG font. */
    private static double measureSvgFontWidth(SvgFont svgFont, String text, double fontSize) {
        double width = 0;
        int i = 0;
        while (i < text.length()) {
            SvgFont.GlyphMatch match = svgFont.getGlyph(text, i);
            width += svgFont.getAdvance(match.glyph(), fontSize);
            i += match.charsConsumed();
        }
        return width;
    }

    /** Resolve the Font for a given node based on its font attributes. */
    private static Font resolveFont(Surface surface, Node node) {
        String fontFamily = parseFontFamily(node);
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
        return font.deriveFont((float) surface.fontSize);
    }
}

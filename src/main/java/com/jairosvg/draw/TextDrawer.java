package com.jairosvg.draw;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.jairosvg.dom.Node;
import com.jairosvg.dom.SvgFont;
import com.jairosvg.surface.Surface;
import com.jairosvg.util.UrlHelper;

import static com.jairosvg.util.Helpers.*;

/**
 * SVG text rendering. Port of CairoSVG text.py
 */
public final class TextDrawer {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");
    private static final double FLATTEN_TOLERANCE = 0.5;

    private static final Map<String, Font> FONT_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Font> eldest) {
            return size() > 64;
        }
    };

    private TextDrawer() {
    }

    /** Extract the primary font family name from a font-family attribute value. */
    private static String parseFontFamily(Node node) {
        return node.get("font-family", "SansSerif").split(",")[0].strip().replace("'", "").replace("\"", "");
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
        } else if (NUMERIC_PATTERN.matcher(weightStr).matches() && Integer.parseInt(weightStr) >= 550) {
            fontStyle |= Font.BOLD;
        }

        String fontKey = fontFamily + "|" + fontStyle + "|" + (float) surface.fontSize;
        Font font = FONT_CACHE.get(fontKey);
        if (font == null) {
            font = new Font(fontFamily, fontStyle, 1).deriveFont((float) surface.fontSize);
            FONT_CACHE.put(fontKey, font);
        }

        FontRenderContext frc = surface.context.getFontRenderContext();

        String textContent = node.text;
        // Whitespace-only text between child elements (e.g. tspan) should be ignored
        if (textContent != null && textContent.isBlank() && !node.children.isEmpty()) {
            textContent = null;
        }
        if (textContent == null || textContent.isEmpty()) {
            // Set initial cursor position from this node's x/y
            String parentX = node.get("x");
            String parentY = node.get("y");
            String parentDx = node.get("dx");
            String parentDy = node.get("dy");

            if (parentX != null)
                surface.cursorPosition[0] = size(surface, parentX.split("\\s+")[0], "x");
            if (parentY != null)
                surface.cursorPosition[1] = size(surface, parentY.split("\\s+")[0], "y");
            if (parentDx != null)
                surface.cursorPosition[0] += size(surface, parentDx.split("\\s+")[0], "x");
            if (parentDy != null)
                surface.cursorPosition[1] += size(surface, parentDy.split("\\s+")[0], "y");

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

            return;
        }

        // Parse position attributes
        double startX = 0, startY = 0;
        String xStr = node.get("x");
        String yStr = node.get("y");
        String dxStr = node.get("dx");
        String dyStr = node.get("dy");

        if (xStr != null)
            startX = size(surface, xStr.split("\\s+")[0], "x");
        else
            startX = surface.cursorPosition[0];

        if (yStr != null)
            startY = size(surface, yStr.split("\\s+")[0], "y");
        else
            startY = surface.cursorPosition[1];

        if (dxStr != null)
            startX += size(surface, dxStr.split("\\s+")[0], "x");
        if (dyStr != null)
            startY += size(surface, dyStr.split("\\s+")[0], "y");

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
        double textWidth = 0;

        if (svgFont == null && "textPath".equals(node.tag)
                && drawTextPath(surface, node, font, frc, textContent, letterSpacing)) {
            return;
        }

        if (svgFont != null) {
            // Render using SVG font glyphs as paths (greedy longest-match for multi-char
            // unicode)
            double curX = startX;
            int i = 0;
            while (i < textContent.length()) {
                SvgFont.GlyphMatch match = svgFont.getGlyph(textContent, i);
                SvgFont.Glyph glyph = match.glyph();
                if (glyph != null) {
                    java.awt.geom.GeneralPath glyphPath = svgFont.buildGlyphPath(glyph, surface.fontSize, curX, startY);
                    if (glyphPath != null) {
                        surface.path.append(glyphPath, false);
                    }
                }
                curX += svgFont.getAdvance(glyph, surface.fontSize) + letterSpacing;
                i += match.charsConsumed();
            }
            surface.cursorPosition[0] = curX;
            textWidth = curX - startX;
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
            textWidth = curX - startX - letterSpacing;
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
            textWidth = bounds.getWidth();
        }

        String textDecoration = node.get("text-decoration");
        String normalizedTextDecoration = textDecoration == null ? null : textDecoration.strip();
        if (svgFont == null && textWidth > 0 && normalizedTextDecoration != null
                && !"none".equals(normalizedTextDecoration)) {
            LineMetrics lineMetrics = font.getLineMetrics(textContent, frc);
            double decorationThickness = lineMetrics.getUnderlineThickness();
            for (String decoration : normalizedTextDecoration.split("\\s+")) {
                double decorationY = switch (decoration) {
                    case "underline" -> startY + lineMetrics.getUnderlineOffset();
                    case "overline" -> startY - lineMetrics.getAscent();
                    case "line-through" -> startY + lineMetrics.getStrikethroughOffset();
                    default -> Double.NaN;
                };
                if (!Double.isNaN(decorationY)) {
                    surface.path.append(new Rectangle2D.Double(startX, decorationY - decorationThickness / 2.0,
                            textWidth, decorationThickness), false);
                }
            }
        }

        surface.cursorPosition[1] = startY;
    }

    private static boolean drawTextPath(Surface surface, Node node, Font font, FontRenderContext frc,
            String textContent, double letterSpacing) {
        String href = node.getHref();
        if (href == null || href.isEmpty()) {
            return false;
        }

        String pathId = UrlHelper.parseUrl(href).fragment();
        if (pathId == null || pathId.isEmpty()) {
            return false;
        }

        Node pathNode = Defs.findNodeById(surface.rootNode, pathId);
        if (pathNode == null || !"path".equals(pathNode.tag)) {
            return false;
        }

        GeneralPath savedPath = surface.path;
        GeneralPath textPath = new GeneralPath();
        surface.path = textPath;
        PathDrawer.path(surface, pathNode);
        surface.path = savedPath;

        List<double[]> segments = flattenPathSegments(textPath);
        if (segments.isEmpty()) {
            return false;
        }

        double totalLength = 0;
        for (double[] segment : segments) {
            totalLength += segment[4];
        }

        double distance = parseStartOffset(surface, node.get("startOffset"), totalLength);
        Point2D cursorPoint = pointAtDistance(segments, distance);
        for (int i = 0; i < textContent.length(); i++) {
            String ch = String.valueOf(textContent.charAt(i));
            GlyphVector gv = font.createGlyphVector(frc, ch);
            double advance = gv.getGlyphMetrics(0).getAdvance() + letterSpacing;

            Point2D point = pointAtDistance(segments, distance);
            Point2D tangent = tangentAtDistance(segments, distance);
            if (point == null || tangent == null) {
                break;
            }
            cursorPoint = point;

            if (!ch.isBlank()) {
                double angle = Math.atan2(tangent.getY(), tangent.getX());
                AffineTransform placement = new AffineTransform();
                placement.translate(point.getX(), point.getY());
                placement.rotate(angle);
                surface.path.append(placement.createTransformedShape(gv.getOutline()), false);
            }

            distance += advance;
            if (distance > totalLength) {
                distance = totalLength;
                break;
            }
        }

        if (cursorPoint != null) {
            surface.cursorPosition[0] = cursorPoint.getX();
            surface.cursorPosition[1] = cursorPoint.getY();
        }
        return true;
    }

    private static List<double[]> flattenPathSegments(Shape shape) {
        List<double[]> segments = new ArrayList<>();
        PathIterator iterator = shape.getPathIterator(null, FLATTEN_TOLERANCE);
        double[] coords = new double[6];
        double moveX = 0;
        double moveY = 0;
        double prevX = 0;
        double prevY = 0;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO -> {
                    moveX = coords[0];
                    moveY = coords[1];
                    prevX = coords[0];
                    prevY = coords[1];
                }
                case PathIterator.SEG_LINETO -> {
                    double len = Point2D.distance(prevX, prevY, coords[0], coords[1]);
                    if (len > 0) {
                        segments.add(new double[]{prevX, prevY, coords[0], coords[1], len});
                    }
                    prevX = coords[0];
                    prevY = coords[1];
                }
                case PathIterator.SEG_CLOSE -> {
                    double len = Point2D.distance(prevX, prevY, moveX, moveY);
                    if (len > 0) {
                        segments.add(new double[]{prevX, prevY, moveX, moveY, len});
                    }
                    prevX = moveX;
                    prevY = moveY;
                }
                default -> {
                }
            }
            iterator.next();
        }
        return segments;
    }

    private static Point2D pointAtDistance(List<double[]> segments, double distance) {
        if (segments.isEmpty()) {
            return null;
        }
        double remaining = Math.max(0, distance);
        for (double[] segment : segments) {
            if (remaining <= segment[4]) {
                double t = segment[4] == 0 ? 0 : remaining / segment[4];
                double x = segment[0] + (segment[2] - segment[0]) * t;
                double y = segment[1] + (segment[3] - segment[1]) * t;
                return new Point2D.Double(x, y);
            }
            remaining -= segment[4];
        }
        double[] last = segments.getLast();
        return new Point2D.Double(last[2], last[3]);
    }

    private static Point2D tangentAtDistance(List<double[]> segments, double distance) {
        if (segments.isEmpty()) {
            return null;
        }
        double remaining = Math.max(0, distance);
        for (double[] segment : segments) {
            if (remaining <= segment[4]) {
                return new Point2D.Double(segment[2] - segment[0], segment[3] - segment[1]);
            }
            remaining -= segment[4];
        }
        double[] last = segments.getLast();
        return new Point2D.Double(last[2] - last[0], last[3] - last[1]);
    }

    private static double parseStartOffset(Surface surface, String startOffset, double totalLength) {
        if (startOffset == null || startOffset.isBlank()) {
            return 0;
        }
        String normalized = startOffset.strip();
        if (normalized.endsWith("%")) {
            try {
                return Math.max(0,
                        totalLength * Double.parseDouble(normalized.substring(0, normalized.length() - 1)) / 100.0);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return Math.max(0, size(surface, normalized, "x"));
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
        } else if (NUMERIC_PATTERN.matcher(weightStr).matches() && Integer.parseInt(weightStr) >= 550) {
            fontStyle |= Font.BOLD;
        }
        String fontKey = fontFamily + "|" + fontStyle + "|" + (float) surface.fontSize;
        Font font = FONT_CACHE.get(fontKey);
        if (font == null) {
            font = new Font(fontFamily, fontStyle, 1).deriveFont((float) surface.fontSize);
            FONT_CACHE.put(fontKey, font);
        }
        return font;
    }
}

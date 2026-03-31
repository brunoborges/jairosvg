package io.brunoborges.jairosvg.draw;

import static io.brunoborges.jairosvg.util.Helpers.size;

import java.util.HashMap;
import java.util.Map;

import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.dom.SvgFont;
import io.brunoborges.jairosvg.surface.Surface;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * SVG definitions: registration, use element, clip paths, and dispatch to
 * specialized renderers. Port of CairoSVG defs.py
 */
public final class Defs {

    private Defs() {
    }

    /** Recursively parse all definition elements. */
    public static void parseAllDefs(Surface surface, Node node) {
        parseDef(surface, node);
        for (Node child : node.children) {
            parseAllDefs(surface, child);
        }
    }

    /** Parse a single definition element. */
    public static void parseDef(Surface surface, Node node) {
        String tag = node.tag.toLowerCase();

        // SVG fonts are keyed by font-family, not by id, so handle before the id check
        if (tag.equals("font")) {
            SvgFont svgFont = SvgFont.parse(node);
            if (svgFont != null) {
                surface.fonts.put(svgFont.family, svgFont);
            }
        }

        String id = node.get("id");
        if (id == null)
            return;

        // Build global ID index for O(1) <use> and cross-reference lookups
        surface.nodeById.put(id, node);

        if (tag.contains("marker"))
            surface.markers.put(id, node);
        if (tag.contains("gradient"))
            surface.gradients.put(id, node);
        if (tag.contains("pattern"))
            surface.patterns.put(id, node);
        if (tag.contains("mask"))
            surface.masks.put(id, node);
        if (tag.contains("filter"))
            surface.filters.put(id, node);
        if (tag.contains("image"))
            surface.images.put(id, node);
        if (tag.equals("clippath"))
            surface.paths.put(id, node);
    }

    /**
     * Apply gradient or pattern color. Returns true if a gradient/pattern was
     * applied.
     */
    public static boolean gradientOrPattern(Surface surface, Node node, String name, double opacity) {
        if (name == null)
            return false;
        if (surface.gradients.containsKey(name)) {
            return GradientPainter.drawGradient(surface, node, name, opacity);
        }
        if (surface.patterns.containsKey(name)) {
            return PatternPainter.drawPattern(surface, node, name, opacity);
        }
        return false;
    }

    /** Handle clip-path. */
    public static void clipPath(Surface surface, Node node) {
        String id = node.get("id");
        if (id != null) {
            surface.paths.put(id, node);
        }
    }

    /** Handle use element. */
    public static void use(Surface surface, Node node) {
        double x = size(surface, node.get("x"), "x");
        double y = size(surface, node.get("y"), "y");

        String href = node.getHref();
        if (href == null)
            return;

        String fragment = UrlHelper.parseUrl(href).fragment();
        if (fragment == null)
            return;

        // Find referenced element by ID in the index (O(1) instead of O(n) tree walk)
        Node refNode = surface.nodeById.get(fragment);
        if (refNode == null)
            return;

        // Propagate inheritable presentation attributes from <use> to the referenced
        // node (SVG spec: <use> acts as parent for inheritance). Save originals to
        // restore after drawing.
        var notInherited = Node.notInheritedAttributes();
        Map<String, String> saved = new HashMap<>();
        for (var entry : node.entries()) {
            String key = entry.getKey();
            if (!notInherited.contains(key) && !refNode.has(key)) {
                saved.put(key, null);
                refNode.set(key, entry.getValue());
            }
        }

        var savedTransform = surface.context.getTransform();
        surface.context.translate(x, y);

        // If it's an svg or symbol, treat as svg
        if ("svg".equals(refNode.tag) || "symbol".equals(refNode.tag)) {
            String origTag = refNode.tag;
            String origWidth = refNode.get("width");
            String origHeight = refNode.get("height");
            refNode.tag = "svg";
            if (node.has("width") && node.has("height")) {
                refNode.set("width", node.get("width"));
                refNode.set("height", node.get("height"));
            } else if ("symbol".equals(origTag)) {
                // SVG 2: without explicit width/height on <use>, default to the
                // symbol's viewBox dimensions (browser "auto" behaviour) instead
                // of falling back to 100% of the parent viewport.
                String viewBox = refNode.get("viewBox");
                if (viewBox != null) {
                    String[] vbParts = viewBox.strip().split("[\\s,]+");
                    if (vbParts.length == 4) {
                        refNode.set("width", vbParts[2]);
                        refNode.set("height", vbParts[3]);
                    }
                }
            }
            surface.draw(refNode);
            refNode.tag = origTag;
            // Restore original width/height to prevent pollution across
            // multiple <use> elements referencing the same symbol/svg
            if (origWidth != null)
                refNode.set("width", origWidth);
            else
                refNode.remove("width");
            if (origHeight != null)
                refNode.set("height", origHeight);
            else
                refNode.remove("height");
        } else {
            surface.draw(refNode);
        }

        // Clear any path data left by the referenced element's draw() so
        // the caller's draw() does not re-fill/stroke it with <use>'s own
        // default paint (black).
        surface.path.reset();

        surface.context.setTransform(savedTransform);

        // Restore: remove attributes that were injected
        for (var key : saved.keySet()) {
            refNode.remove(key);
        }
    }

    /** Handle markers. */
    public static void marker(Surface surface, Node node) {
        parseDef(surface, node);
    }

    /** Handle mask definition. */
    public static void mask(Surface surface, Node node) {
        parseDef(surface, node);
    }

    /** Handle filter definition. */
    public static void filter(Surface surface, Node node) {
        parseDef(surface, node);
    }

    /** Handle gradient definitions. */
    public static void linearGradient(Surface surface, Node node) {
        parseDef(surface, node);
    }

    public static void radialGradient(Surface surface, Node node) {
        parseDef(surface, node);
    }

    /** Handle pattern definition. */
    public static void pattern(Surface surface, Node node) {
        parseDef(surface, node);
    }

    static Node findNodeById(Node root, String id) {
        if (root == null || id == null)
            return null;
        if (id.equals(root.get("id")))
            return root;
        for (Node child : root.children) {
            Node found = findNodeById(child, id);
            if (found != null)
                return found;
        }
        return null;
    }
}

package io.brunoborges.jairosvg.dom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import io.brunoborges.jairosvg.css.CssProcessor;
import io.brunoborges.jairosvg.util.Features;
import io.brunoborges.jairosvg.util.Helpers;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * SVG Node with dict-like properties and children. Port of CairoSVG parser.py
 * (Node and Tree classes).
 */
public class Node {

    private static final SAXParserFactory SAFE_SAX_FACTORY;
    private static final SAXParserFactory UNSAFE_SAX_FACTORY;
    static {
        SAFE_SAX_FACTORY = SAXParserFactory.newInstance();
        SAFE_SAX_FACTORY.setNamespaceAware(true);
        try {
            SAFE_SAX_FACTORY.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            SAFE_SAX_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        UNSAFE_SAX_FACTORY = SAXParserFactory.newInstance();
        UNSAFE_SAX_FACTORY.setNamespaceAware(true);
    }

    private static final ThreadLocal<SAXParser> SAFE_PARSER = ThreadLocal.withInitial(() -> {
        try {
            return SAFE_SAX_FACTORY.newSAXParser();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    });
    private static final ThreadLocal<SAXParser> UNSAFE_PARSER = ThreadLocal.withInitial(() -> {
        try {
            return UNSAFE_SAX_FACTORY.newSAXParser();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    });

    private static final Set<String> NOT_INHERITED_ATTRIBUTES = Set.of("clip", "clip-path", "display", "filter",
            "height", "id", "mask", "opacity", "overflow", "rotate", "stop-color", "stop-opacity", "style", "transform",
            "transform-origin", "viewBox", "width", "x", "y", "dx", "dy", "{http://www.w3.org/1999/xlink}href", "href");

    private static final Set<String> COLOR_ATTRIBUTES = Set.of("fill", "flood-color", "lighting-color", "stop-color",
            "stroke");

    public String tag;
    public String text;
    public String url;
    public Node parent;
    public boolean root = false;
    public List<Node> children;
    public double imageWidth;
    public double imageHeight;
    public List<Object> vertices;
    public boolean unsafe = false;
    public UrlHelper.UrlFetcher urlFetcher;

    private final Map<String, String> attributes;
    private Set<String> elementAttrKeys;

    /**
     * Lightweight constructor for programmatic node creation (e.g. SVG font glyph
     * parsing).
     */
    Node() {
        this.attributes = new LinkedHashMap<>();
        this.children = new ArrayList<>();
    }

    /**
     * Create a raw Node from SAX-parsed data. Inherits attributes from parent but
     * does NOT apply CSS. Call {@link #applyCss} after the full tree is built.
     */
    Node(String tag, String text, Map<String, String> elementAttrs, Node parent, UrlHelper.UrlFetcher urlFetcher,
            boolean unsafe) {
        int parentAttrCount = parent != null ? parent.attributes.size() : 0;
        int elementAttrCount = elementAttrs != null ? elementAttrs.size() : 0;
        this.attributes = LinkedHashMap.newLinkedHashMap(parentAttrCount + elementAttrCount + 8);

        this.tag = tag;
        this.text = text;
        this.urlFetcher = urlFetcher;
        this.unsafe = unsafe;
        this.children = new ArrayList<>();

        // Remember element's own attribute keys for re-inheritance in applyCss
        this.elementAttrKeys = elementAttrs != null ? Set.copyOf(elementAttrs.keySet()) : Set.of();

        // Inherit from parent
        if (parent != null) {
            this.parent = parent;
            this.url = parent.url;
            for (var entry : parent.attributes.entrySet()) {
                if (!NOT_INHERITED_ATTRIBUTES.contains(entry.getKey())) {
                    this.attributes.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Copy element attributes (overrides inherited)
        if (elementAttrs != null) {
            this.attributes.putAll(elementAttrs);
        }
    }

    /**
     * Apply CSS rules, inline styles, custom properties, currentColor, and inherit
     * resolution. Must be called after the full tree is built so that sibling-based
     * CSS selectors (:first-child, :last-child, :nth-child) work correctly.
     */
    void applyCss(List<CssProcessor.StyleRule> styleRules) {
        // Re-inherit from parent to pick up CSS-applied attributes (e.g. custom
        // properties set via stylesheet rules on an ancestor)
        if (parent != null) {
            for (var entry : parent.attributes.entrySet()) {
                String key = entry.getKey();
                if (!NOT_INHERITED_ATTRIBUTES.contains(key) && !elementAttrKeys.contains(key)) {
                    this.attributes.put(key, entry.getValue());
                }
            }
        }

        // Apply CSS rules from stylesheets (non-important first, important after inline
        // styles)
        CssProcessor.MatchResult matchResult = null;
        if (styleRules != null && !styleRules.isEmpty()) {
            matchResult = CssProcessor.getAllMatchingDeclarations(this, styleRules);
            for (var decl : matchResult.normal()) {
                this.attributes.put(decl.name(), decl.value());
            }
        }

        // Apply inline style
        String style = this.attributes.get("style");
        if (style != null && !style.isEmpty()) {
            var parsed = CssProcessor.parseDeclarations(style);
            for (var decl : parsed[0]) {
                this.attributes.put(decl.name(), decl.value());
            }
            for (var decl : parsed[1]) {
                this.attributes.put(decl.name(), decl.value());
            }
        }

        // Apply important CSS rules (override inline styles)
        if (matchResult != null) {
            for (var decl : matchResult.important()) {
                this.attributes.put(decl.name(), decl.value());
            }
        }

        for (var entry : new ArrayList<>(this.attributes.entrySet())) {
            this.attributes.put(entry.getKey(),
                    CssProcessor.resolveCustomProperties(entry.getValue(), this.attributes));
        }

        // Replace currentColor (lazy-resolve only if needed)
        String currentColorValue = null;
        for (String attr : COLOR_ATTRIBUTES) {
            if ("currentColor".equals(this.attributes.get(attr))) {
                if (currentColorValue == null)
                    currentColorValue = get("color", "black");
                this.attributes.put(attr, currentColorValue);
            }
        }

        // Replace inherit
        for (var entry : new ArrayList<>(this.attributes.entrySet())) {
            if ("inherit".equals(entry.getValue())) {
                if (parent != null && parent.attributes.containsKey(entry.getKey())) {
                    this.attributes.put(entry.getKey(), parent.get(entry.getKey()));
                } else {
                    this.attributes.remove(entry.getKey());
                }
            }
        }

        // Process font shorthand
        if (this.attributes.containsKey("font")) {
            var fontProps = Helpers.parseFont(this.attributes.get("font"));
            for (var entry : fontProps.entrySet()) {
                if (!this.attributes.containsKey(entry.getKey()) && !entry.getValue().isEmpty()) {
                    this.attributes.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Free element attribute keys (no longer needed after CSS application)
        this.elementAttrKeys = null;

        // Recurse into children
        for (Node child : this.children) {
            child.applyCss(styleRules);
        }
    }

    public String get(String key) {
        return attributes.get(key);
    }

    public String get(String key, String defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    public void set(String key, String value) {
        attributes.put(key, value);
    }

    public void set(String key, double value) {
        attributes.put(key, String.valueOf(value));
    }

    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    public void remove(String key) {
        attributes.remove(key);
    }

    public Set<Map.Entry<String, String>> entries() {
        return attributes.entrySet();
    }

    /** Get href, checking both xlink:href and href. */
    public String getHref() {
        String href = get("{http://www.w3.org/1999/xlink}href");
        if (href == null)
            href = get("xlink:href");
        if (href == null)
            href = get("href");
        return href;
    }

    /** Fetch URL content. */
    public byte[] fetchUrl(UrlHelper.ParsedUrl parsedUrl, String resourceType) throws IOException {
        return UrlHelper.readUrl(parsedUrl, urlFetcher, resourceType);
    }

    // ---------- Tree (static factory) ----------

    /** Parse an SVG from bytes, file, or URL into a Node tree. */
    public static Node parseTree(byte[] bytestring, String url, UrlHelper.UrlFetcher urlFetcher, boolean unsafe)
            throws Exception {
        if (bytestring == null && url != null) {
            UrlHelper.ParsedUrl parsed = UrlHelper.parseUrl(url);
            bytestring = UrlHelper.readUrl(parsed, urlFetcher != null ? urlFetcher : UrlHelper::fetch, "image/svg+xml");
        }
        if (bytestring == null) {
            throw new IllegalArgumentException("No input. Provide bytestring or url.");
        }

        // Handle gzip
        if (bytestring.length > 2 && bytestring[0] == (byte) 0x1f && bytestring[1] == (byte) 0x8b) {
            bytestring = new GZIPInputStream(new ByteArrayInputStream(bytestring)).readAllBytes();
        }

        UrlHelper.UrlFetcher fetcher = urlFetcher;
        if (fetcher == null) {
            fetcher = unsafe ? UrlHelper::fetch : UrlHelper::safeFetch;
        }

        // Parse SVG via SAX directly into Node tree
        SaxTreeBuilder handler = new SaxTreeBuilder(fetcher, unsafe);
        SAXParser parser = unsafe ? UNSAFE_PARSER.get() : SAFE_PARSER.get();
        try {
            parser.parse(new InputSource(new ByteArrayInputStream(bytestring)), handler);
        } finally {
            parser.reset();
        }

        Node tree = handler.getRoot();
        if (tree == null) {
            throw new IllegalArgumentException("Empty SVG document.");
        }
        tree.url = url;
        tree.root = true;

        // Parse CSS stylesheets from <?xml-stylesheet?> processing instructions
        List<CssProcessor.StyleRule> styleRules = new ArrayList<>();
        if (unsafe && !handler.getStylesheetPIs().isEmpty()) {
            styleRules.addAll(CssProcessor.parseExternalStylesheets(handler.getStylesheetPIs(), fetcher, url));
        }

        // Parse CSS stylesheets from <style> elements
        styleRules.addAll(CssProcessor.parseStylesheets(tree));

        // Apply CSS to the full tree
        tree.applyCss(styleRules);

        return tree;
    }

    public static Node parseTree(byte[] bytestring, String url, boolean unsafe) throws Exception {
        return parseTree(bytestring, url, null, unsafe);
    }

    public static Node parseTree(byte[] bytestring) throws Exception {
        return parseTree(bytestring, null, null, false);
    }

    // ---------- SAX Handler ----------

    /** SAX ContentHandler that builds a Node tree directly from SAX events. */
    private static class SaxTreeBuilder extends DefaultHandler {

        private final UrlHelper.UrlFetcher urlFetcher;
        private final boolean unsafe;
        private Node root;
        private final Deque<Node> stack = new ArrayDeque<>();
        private final Deque<StringBuilder> textStack = new ArrayDeque<>();
        private final List<String> stylesheetPIs = new ArrayList<>();
        // Track skipping depth for elements that fail feature matching
        private int skipDepth = 0;

        SaxTreeBuilder(UrlHelper.UrlFetcher urlFetcher, boolean unsafe) {
            this.urlFetcher = urlFetcher;
            this.unsafe = unsafe;
        }

        Node getRoot() {
            return root;
        }

        List<String> getStylesheetPIs() {
            return stylesheetPIs;
        }

        @Override
        public void processingInstruction(String target, String data) {
            if ("xml-stylesheet".equals(target)) {
                stylesheetPIs.add(data);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes saxAttrs) {
            if (skipDepth > 0) {
                skipDepth++;
                return;
            }

            // Check features using raw SAX attributes
            String reqFeatures = saxAttrs.getValue("requiredFeatures");
            String reqExtensions = saxAttrs.getValue("requiredExtensions");
            String sysLanguage = saxAttrs.getValue("systemLanguage");
            if (!Features.matchFeatures(reqFeatures, reqExtensions, sysLanguage)) {
                skipDepth = 1;
                return;
            }

            // Determine tag name (strip SVG namespace, prefix others)
            String tag;
            if (uri == null || "http://www.w3.org/2000/svg".equals(uri) || uri.isEmpty()) {
                tag = localName != null && !localName.isEmpty() ? localName : qName;
            } else {
                String name = localName != null && !localName.isEmpty() ? localName : qName;
                tag = "{" + uri + "}" + name;
            }

            // Extract attributes (skip xmlns declarations)
            Map<String, String> attrs = LinkedHashMap.newLinkedHashMap(saxAttrs.getLength());
            for (int i = 0; i < saxAttrs.getLength(); i++) {
                String name = saxAttrs.getQName(i);
                if (name.startsWith("xmlns"))
                    continue;
                attrs.put(name, saxAttrs.getValue(i));
            }

            Node parent = stack.isEmpty() ? null : stack.peek();
            Node node = new Node(tag, null, attrs, parent, urlFetcher, unsafe);

            if (parent != null) {
                // For <switch> elements, only add the first matching child
                if (!"switch".equals(parent.tag) || parent.children.isEmpty()) {
                    parent.children.add(node);
                } else {
                    // Skip remaining children of <switch> after first match
                    skipDepth = 1;
                    return;
                }
            }

            if (root == null) {
                root = node;
            }

            stack.push(node);
            textStack.push(new StringBuilder());
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (skipDepth > 0 || textStack.isEmpty())
                return;
            textStack.peek().append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (skipDepth > 0) {
                skipDepth--;
                return;
            }

            if (stack.isEmpty())
                return;

            Node node = stack.pop();
            StringBuilder textBuf = textStack.pop();

            // Set direct text content (only text before first child element)
            String text = textBuf.toString();
            node.text = text.isEmpty() ? null : text;
        }
    }
}

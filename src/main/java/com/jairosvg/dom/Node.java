package com.jairosvg.dom;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.jairosvg.css.CssProcessor;
import com.jairosvg.util.Features;
import com.jairosvg.util.Helpers;
import com.jairosvg.util.UrlHelper;

/**
 * SVG Node with dict-like properties and children. Port of CairoSVG parser.py
 * (Node and Tree classes).
 */
public class Node {

    private static final DocumentBuilderFactory SAFE_FACTORY;
    private static final DocumentBuilderFactory UNSAFE_FACTORY;
    static {
        SAFE_FACTORY = DocumentBuilderFactory.newInstance();
        SAFE_FACTORY.setNamespaceAware(true);
        try {
            SAFE_FACTORY.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            SAFE_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        UNSAFE_FACTORY = DocumentBuilderFactory.newInstance();
        UNSAFE_FACTORY.setNamespaceAware(true);
    }

    private static final ThreadLocal<DocumentBuilder> SAFE_BUILDER = ThreadLocal.withInitial(() -> {
        try {
            return SAFE_FACTORY.newDocumentBuilder();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    });
    private static final ThreadLocal<DocumentBuilder> UNSAFE_BUILDER = ThreadLocal.withInitial(() -> {
        try {
            return UNSAFE_FACTORY.newDocumentBuilder();
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
    public Element xmlTree;
    public double imageWidth;
    public double imageHeight;
    public List<Object> vertices;
    public boolean unsafe = false;
    public UrlHelper.UrlFetcher urlFetcher;

    private final Map<String, String> attributes;
    private List<CssProcessor.StyleRule> styleRules;

    /**
     * Lightweight constructor for programmatic node creation (e.g. SVG font glyph
     * parsing).
     */
    Node() {
        this.attributes = new LinkedHashMap<>();
        this.children = new ArrayList<>();
    }

    /** Create a Node from a DOM Element. */
    public Node(Element element, Node parent, List<CssProcessor.StyleRule> styleRules, UrlHelper.UrlFetcher urlFetcher,
            boolean unsafe) {
        int parentAttrCount = parent != null ? parent.attributes.size() : 0;
        int elementAttrCount = element.getAttributes().getLength();
        this.attributes = LinkedHashMap.newLinkedHashMap(parentAttrCount + elementAttrCount + 8);

        this.xmlTree = element;
        this.urlFetcher = urlFetcher;
        this.unsafe = unsafe;
        this.styleRules = styleRules;

        // Determine tag name (strip namespace for SVG elements)
        String nsUri = element.getNamespaceURI();
        if (nsUri == null || "http://www.w3.org/2000/svg".equals(nsUri) || nsUri.isEmpty()) {
            this.tag = element.getLocalName() != null ? element.getLocalName() : element.getTagName();
        } else {
            this.tag = "{" + nsUri + "}"
                    + (element.getLocalName() != null ? element.getLocalName() : element.getTagName());
        }

        this.text = getDirectText(element);

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

        // Copy element attributes
        var attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            var attr = attrs.item(i);
            String name = attr.getNodeName();
            if (name.startsWith("xmlns"))
                continue;
            this.attributes.put(name, attr.getNodeValue());
        }

        // Apply CSS rules from stylesheets (non-important first, important after inline
        // styles)
        CssProcessor.MatchResult matchResult = null;
        if (styleRules != null) {
            matchResult = CssProcessor.getAllMatchingDeclarations(element, styleRules);
            for (var decl : matchResult.normal()) {
                this.attributes.put(decl.name(), decl.value());
            }
        }

        // Apply inline style
        String style = element.getAttribute("style");
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
            this.attributes.put(entry.getKey(), CssProcessor.resolveCustomProperties(entry.getValue(), this.attributes));
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

        // Build children
        var childNodes = element.getChildNodes();
        this.children = new ArrayList<>(childNodes.getLength());
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element childElem) {
                if (Features.matchFeatures(childElem)) {
                    this.children.add(new Node(childElem, this, styleRules, urlFetcher, unsafe));
                    if ("switch".equals(this.tag))
                        break;
                }
            }
        }
    }

    /** Get direct text content of element (not from children). */
    private static String getDirectText(Element element) {
        var childNodes = element.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() == org.w3c.dom.Node.TEXT_NODE
                    || childNodes.item(i).getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                sb.append(childNodes.item(i).getTextContent());
            }
        }
        String result = sb.toString();
        return result.isEmpty() ? null : result;
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

        DocumentBuilder builder = unsafe ? UNSAFE_BUILDER.get() : SAFE_BUILDER.get();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(bytestring)));
        Element root = doc.getDocumentElement();

        UrlHelper.UrlFetcher fetcher = urlFetcher;
        if (fetcher == null) {
            fetcher = unsafe ? UrlHelper::fetch : UrlHelper::safeFetch;
        }

        // Parse external CSS stylesheets from <?xml-stylesheet?> processing
        // instructions
        List<CssProcessor.StyleRule> styleRules = new ArrayList<>();
        if (unsafe) {
            styleRules.addAll(CssProcessor.parseExternalStylesheets(doc, fetcher, url));
        }

        // Parse CSS stylesheets from <style> elements
        styleRules.addAll(CssProcessor.parseStylesheets(root));

        Node tree = new Node(root, null, styleRules, fetcher, unsafe);
        tree.url = url;
        tree.root = true;
        return tree;
    }

    public static Node parseTree(byte[] bytestring, String url, boolean unsafe) throws Exception {
        return parseTree(bytestring, url, null, unsafe);
    }

    public static Node parseTree(byte[] bytestring) throws Exception {
        return parseTree(bytestring, null, null, false);
    }
}

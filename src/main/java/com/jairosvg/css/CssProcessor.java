package com.jairosvg.css;

import java.util.*;
import java.util.regex.*;

import com.jairosvg.util.UrlHelper;

/**
 * Minimal CSS parsing for SVG inline styles and stylesheets. Port of CairoSVG
 * css.py
 */
public final class CssProcessor {

    private static final java.util.regex.Pattern COMMENT_PATTERN = java.util.regex.Pattern.compile("/\\*.*?\\*/",
            java.util.regex.Pattern.DOTALL);
    private static final java.util.regex.Pattern IMPORT_PATTERN = java.util.regex.Pattern.compile("@import[^;]*;");
    private static final java.util.regex.Pattern RULE_PATTERN = java.util.regex.Pattern
            .compile("([^{}]+)\\{([^}]*)\\}");
    private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");
    private static final java.util.regex.Pattern PSEUDO_ELEMENT_PATTERN = java.util.regex.Pattern.compile("::[\\w-]+");
    private static final java.util.regex.Pattern NOT_PATTERN = java.util.regex.Pattern.compile(":not\\(([^()]*)\\)");
    private static final java.util.regex.Pattern FIRST_CHILD_PATTERN = java.util.regex.Pattern
            .compile(":first-child\\b");
    private static final java.util.regex.Pattern LAST_CHILD_PATTERN = java.util.regex.Pattern.compile(":last-child\\b");
    private static final java.util.regex.Pattern NTH_CHILD_PATTERN = java.util.regex.Pattern
            .compile(":nth-child\\(([^)]*)\\)");
    private static final java.util.regex.Pattern NTH_CHILD_AN_B_PATTERN = java.util.regex.Pattern
            .compile("([+-]?\\d*)n([+-]\\d+)?");
    private static final java.util.regex.Pattern PSEUDO_ATTR_PATTERN = java.util.regex.Pattern
            .compile("(\\w[\\w-]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final String VAR_FUNCTION = "var(";

    private CssProcessor() {
    }

    /** A CSS declaration: (property-name, value). */
    public record Declaration(String name, String value) {
    }

    /** Parse inline style declarations into normal and important lists. */
    public static List<Declaration>[] parseDeclarations(String input) {
        List<Declaration> normal = new ArrayList<>();
        List<Declaration> important = new ArrayList<>();

        if (input == null || input.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Declaration>[] result = new List[]{normal, important};
            return result;
        }

        for (String part : input.split(";")) {
            part = part.strip();
            if (part.isEmpty())
                continue;
            int colonIdx = part.indexOf(':');
            if (colonIdx < 0)
                continue;

            String name = part.substring(0, colonIdx).strip().toLowerCase();
            String value = part.substring(colonIdx + 1).strip();

            if (name.startsWith("-") && !name.startsWith("--"))
                continue; // Skip vendor prefixes

            boolean isImportant = false;
            if (value.toLowerCase().endsWith("!important")) {
                value = value.substring(0, value.length() - 10).strip();
                isImportant = true;
            }

            (isImportant ? important : normal).add(new Declaration(name, value));
        }

        @SuppressWarnings("unchecked")
        List<Declaration>[] result = new List[]{normal, important};
        return result;
    }

    /** Extract stylesheets from <style> elements in the SVG tree. */
    public static List<StyleRule> parseStylesheets(org.w3c.dom.Element root) {
        List<StyleRule> rules = new ArrayList<>();
        extractStyleElements(root, rules);
        return rules;
    }

    /**
     * Extract external stylesheets referenced via {@code <?xml-stylesheet?>}
     * processing instructions. Only loads stylesheets with {@code type="text/css"}.
     */
    public static List<StyleRule> parseExternalStylesheets(org.w3c.dom.Document doc, UrlHelper.UrlFetcher fetcher,
            String baseUrl) {
        List<StyleRule> rules = new ArrayList<>();
        var childNodes = doc.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            var node = childNodes.item(i);
            if (node.getNodeType() == org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE
                    && "xml-stylesheet".equals(node.getNodeName())) {
                var pi = (org.w3c.dom.ProcessingInstruction) node;
                var attrs = parsePseudoAttributes(pi.getData());
                String type = attrs.getOrDefault("type", "");
                String normalizedType = "";
                if (type != null) {
                    int semicolonIndex = type.indexOf(';');
                    if (semicolonIndex >= 0) {
                        type = type.substring(0, semicolonIndex);
                    }
                    normalizedType = type.trim().toLowerCase(java.util.Locale.ROOT);
                }
                String href = attrs.get("href");
                if ("text/css".equals(normalizedType) && href != null && !href.isEmpty()) {
                    try {
                        String resolvedUrl = resolveHref(href, baseUrl);
                        byte[] cssBytes = fetcher.fetch(resolvedUrl, "text/css");
                        if (cssBytes != null && cssBytes.length > 0) {
                            parseStylesheet(new String(cssBytes, java.nio.charset.StandardCharsets.UTF_8), rules);
                        }
                    } catch (Exception e) {
                        // Skip stylesheets that cannot be loaded
                    }
                }
            }
        }
        return rules;
    }

    /** Parse pseudo-attributes from a processing instruction data string. */
    public static Map<String, String> parsePseudoAttributes(String data) {
        Map<String, String> attrs = new LinkedHashMap<>();
        if (data == null || data.isEmpty())
            return attrs;
        Matcher m = PSEUDO_ATTR_PATTERN.matcher(data);
        while (m.find()) {
            String name = m.group(1);
            String value = m.group(2) != null ? m.group(2) : m.group(3);
            if (value != null) {
                attrs.put(name, value);
            }
        }
        return attrs;
    }

    private static String resolveHref(String href, String baseUrl) {
        return UrlHelper.parseUrl(href, baseUrl).getUrl();
    }

    /** A CSS rule with selector and declarations. */
    public record StyleRule(String selector, List<Declaration> declarations, boolean important) {
    }

    private static void extractStyleElements(org.w3c.dom.Element element, List<StyleRule> rules) {
        var childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof org.w3c.dom.Element child) {
                String localName = child.getLocalName();
                if (localName == null)
                    localName = child.getTagName();
                if ("style".equals(localName)) {
                    String cssText = child.getTextContent();
                    if (cssText != null) {
                        parseStylesheet(cssText, rules);
                    }
                }
                extractStyleElements(child, rules);
            }
        }
    }

    /** Parse a CSS stylesheet string into rules. */
    public static void parseStylesheet(String cssText, List<StyleRule> rules) {
        // Remove comments
        cssText = COMMENT_PATTERN.matcher(cssText).replaceAll("");
        // Handle @import (basic - skip for now)
        cssText = IMPORT_PATTERN.matcher(cssText).replaceAll("");

        Matcher m = RULE_PATTERN.matcher(cssText);
        while (m.find()) {
            String selectors = m.group(1).strip();
            String declarations = m.group(2).strip();

            List<Declaration>[] parsed = parseDeclarations(declarations);

            for (String selector : selectors.split(",")) {
                selector = selector.strip();
                if (!selector.isEmpty()) {
                    if (!parsed[0].isEmpty()) {
                        rules.add(new StyleRule(selector, parsed[0], false));
                    }
                    if (!parsed[1].isEmpty()) {
                        rules.add(new StyleRule(selector, parsed[1], true));
                    }
                }
            }
        }
    }

    /** Check if an element matches a simple CSS selector. */
    public static boolean matchesSelector(org.w3c.dom.Element element, String selector) {
        selector = selector.strip();
        selector = PSEUDO_ELEMENT_PATTERN.matcher(selector).replaceAll("").strip();

        Matcher notMatcher = NOT_PATTERN.matcher(selector);
        while (notMatcher.find()) {
            String negated = notMatcher.group(1).strip();
            if (negated.isEmpty() || matchesSelector(element, negated))
                return false;
            selector = notMatcher.replaceFirst("").strip();
            notMatcher = NOT_PATTERN.matcher(selector);
        }

        if (FIRST_CHILD_PATTERN.matcher(selector).find()) {
            if (!isFirstChild(element))
                return false;
            selector = FIRST_CHILD_PATTERN.matcher(selector).replaceAll("").strip();
        }

        if (LAST_CHILD_PATTERN.matcher(selector).find()) {
            if (!isLastChild(element))
                return false;
            selector = LAST_CHILD_PATTERN.matcher(selector).replaceAll("").strip();
        }

        Matcher nthMatcher = NTH_CHILD_PATTERN.matcher(selector);
        while (nthMatcher.find()) {
            String nthExpr = nthMatcher.group(1).strip();
            if (!matchesNthChild(element, nthExpr))
                return false;
            selector = nthMatcher.replaceFirst("").strip();
            nthMatcher = NTH_CHILD_PATTERN.matcher(selector);
        }

        if (selector.isEmpty())
            return true;

        // Universal selector
        if ("*".equals(selector))
            return true;

        // Type selector
        String localName = element.getLocalName();
        if (localName == null)
            localName = element.getTagName();

        // ID selector
        if (selector.startsWith("#")) {
            String id = element.getAttribute("id");
            return selector.substring(1).equals(id);
        }

        // Class selector
        if (selector.startsWith(".")) {
            String className = element.getAttribute("class");
            if (className != null) {
                for (String cls : WHITESPACE.split(className)) {
                    if (selector.substring(1).equals(cls))
                        return true;
                }
            }
            return false;
        }

        // Type selector (possibly with class or id)
        if (selector.contains(".")) {
            int dotIdx = selector.indexOf('.');
            String type = selector.substring(0, dotIdx);
            String cls = selector.substring(dotIdx + 1);
            if (!type.isEmpty() && !type.equals(localName))
                return false;
            String className = element.getAttribute("class");
            if (className == null)
                return false;
            for (String c : WHITESPACE.split(className)) {
                if (cls.equals(c))
                    return true;
            }
            return false;
        }

        if (selector.contains("#")) {
            int hashIdx = selector.indexOf('#');
            String type = selector.substring(0, hashIdx);
            String id = selector.substring(hashIdx + 1);
            if (!type.isEmpty() && !type.equals(localName))
                return false;
            return id.equals(element.getAttribute("id"));
        }

        // Simple type selector
        return selector.equals(localName);
    }

    private static boolean isFirstChild(org.w3c.dom.Element element) {
        var parent = element.getParentNode();
        if (parent == null)
            return false;
        var siblings = parent.getChildNodes();
        for (int i = 0; i < siblings.getLength(); i++) {
            if (siblings.item(i) instanceof org.w3c.dom.Element sibling) {
                return sibling == element;
            }
        }
        return false;
    }

    private static boolean isLastChild(org.w3c.dom.Element element) {
        var parent = element.getParentNode();
        if (parent == null)
            return false;
        var siblings = parent.getChildNodes();
        for (int i = siblings.getLength() - 1; i >= 0; i--) {
            if (siblings.item(i) instanceof org.w3c.dom.Element sibling) {
                return sibling == element;
            }
        }
        return false;
    }

    private static boolean matchesNthChild(org.w3c.dom.Element element, String expr) {
        var parent = element.getParentNode();
        if (parent == null)
            return false;

        int index = 0;
        var siblings = parent.getChildNodes();
        for (int i = 0; i < siblings.getLength(); i++) {
            if (siblings.item(i) instanceof org.w3c.dom.Element sibling) {
                index++;
                if (sibling == element)
                    break;
            }
        }
        if (index == 0)
            return false;

        String normalized = WHITESPACE.matcher(expr.toLowerCase(Locale.ROOT)).replaceAll("");
        if ("odd".equals(normalized))
            return index % 2 == 1;
        if ("even".equals(normalized))
            return index % 2 == 0;

        try {
            return index == Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            // Continue with an+b pattern parsing
        }

        Matcher m = NTH_CHILD_AN_B_PATTERN.matcher(normalized);
        if (!m.matches())
            return false;

        String aStr = m.group(1);
        int a;
        if (aStr == null || aStr.isEmpty() || "+".equals(aStr)) {
            a = 1;
        } else if ("-".equals(aStr)) {
            a = -1;
        } else {
            a = Integer.parseInt(aStr);
        }
        int b = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        if (a == 0)
            return index == b;

        int diff = index - b;
        if (diff % a != 0)
            return false;
        return diff / a >= 0;
    }

    /** Both normal and important declarations from a single pass. */
    public record MatchResult(List<Declaration> normal, List<Declaration> important) {
    }

    /** Get all matching CSS declarations in a single pass through the rules. */
    public static MatchResult getAllMatchingDeclarations(org.w3c.dom.Element element, List<StyleRule> rules) {
        List<Declaration> normal = new ArrayList<>();
        List<Declaration> important = new ArrayList<>();
        for (StyleRule rule : rules) {
            if (matchesSelector(element, rule.selector())) {
                (rule.important() ? important : normal).addAll(rule.declarations());
            }
        }
        return new MatchResult(normal, important);
    }

    /** Get CSS declarations that apply to this element from a list of rules. */
    public static List<Declaration> getMatchingDeclarations(org.w3c.dom.Element element, List<StyleRule> rules,
            boolean important) {
        List<Declaration> result = new ArrayList<>();
        for (StyleRule rule : rules) {
            if (rule.important() == important && matchesSelector(element, rule.selector())) {
                result.addAll(rule.declarations());
            }
        }
        return result;
    }

    /** Resolve CSS custom properties in {@code var(--name[, fallback])} expressions. */
    public static String resolveCustomProperties(String value, Map<String, String> attributes) {
        return resolveCustomProperties(value, attributes, new HashSet<>());
    }

    private static String resolveCustomProperties(String value, Map<String, String> attributes, Set<String> resolving) {
        if (value == null || value.isEmpty() || !value.contains(VAR_FUNCTION))
            return value;

        StringBuilder resolved = new StringBuilder();
        int index = 0;
        while (index < value.length()) {
            int varIndex = value.indexOf(VAR_FUNCTION, index);
            if (varIndex < 0) {
                resolved.append(value, index, value.length());
                break;
            }

            resolved.append(value, index, varIndex);
            int endIndex = findClosingParenthesis(value, varIndex + VAR_FUNCTION.length());
            if (endIndex < 0) {
                resolved.append(value, varIndex, value.length());
                break;
            }

            String[] parts = splitVariableArguments(value.substring(varIndex + VAR_FUNCTION.length(), endIndex));
            String name = parts[0].strip();
            String fallback = parts[1];
            String replacement = null;

            String propertyValue = attributes.get(name);
            if (propertyValue == null) {
                propertyValue = attributes.get(name.toLowerCase(Locale.ROOT));
            }

            if (propertyValue != null && !resolving.contains(name)) {
                resolving.add(name);
                replacement = resolveCustomProperties(propertyValue, attributes, resolving);
                resolving.remove(name);
            } else if (fallback != null) {
                replacement = resolveCustomProperties(fallback, attributes, resolving);
            }

            if (replacement != null) {
                resolved.append(replacement);
            }
            index = endIndex + 1;
        }
        return resolved.toString();
    }

    /** Find the matching ')' for a var() expression, accounting for nested parentheses. */
    private static int findClosingParenthesis(String value, int start) {
        int depth = 1;
        for (int i = start; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '(')
                depth++;
            else if (current == ')') {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return -1;
    }

    /** Split var() content into [name, fallback], respecting nested parentheses in fallback. */
    private static String[] splitVariableArguments(String content) {
        int depth = 0;
        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            } else if (current == ',' && depth == 0) {
                return new String[]{content.substring(0, i), content.substring(i + 1).strip()};
            }
        }
        return new String[]{content, null};
    }
}

package io.brunoborges.jairosvg.css;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.brunoborges.jairosvg.dom.Node;
import io.brunoborges.jairosvg.util.UrlHelper;

/**
 * Minimal CSS parsing for SVG inline styles and stylesheets. Port of CairoSVG
 * css.py
 */
public final class CssProcessor {

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("@import[^;]*;");
    private static final Pattern RULE_PATTERN = Pattern.compile("([^{}]+)\\{([^}]*)\\}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PSEUDO_ELEMENT_PATTERN = Pattern.compile("::[\\w-]+");
    private static final Pattern NOT_PATTERN = Pattern.compile(":not\\(([^()]*)\\)");
    private static final Pattern FIRST_CHILD_PATTERN = Pattern.compile(":first-child\\b");
    private static final Pattern LAST_CHILD_PATTERN = Pattern.compile(":last-child\\b");
    private static final Pattern NTH_CHILD_PATTERN = Pattern.compile(":nth-child\\(([^)]*)\\)");
    private static final Pattern NTH_CHILD_AN_B_PATTERN = Pattern.compile("([+-]?\\d*)n([+-]\\d+)?");
    private static final Pattern PSEUDO_ATTR_PATTERN = Pattern
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

    /** Extract stylesheets from &lt;style&gt; elements in the Node tree. */
    public static List<StyleRule> parseStylesheets(Node root) {
        List<StyleRule> rules = new ArrayList<>();
        extractStyleElements(root, rules);
        return rules;
    }

    /**
     * Parse external stylesheet data from captured {@code <?xml-stylesheet?>}
     * processing instruction data strings. Only loads stylesheets with
     * {@code type="text/css"}.
     */
    public static List<StyleRule> parseExternalStylesheets(List<String> piDataList, UrlHelper.UrlFetcher fetcher,
            String baseUrl) {
        List<StyleRule> rules = new ArrayList<>();
        for (String piData : piDataList) {
            var attrs = parsePseudoAttributes(piData);
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

    private static void extractStyleElements(Node node, List<StyleRule> rules) {
        for (Node child : node.children) {
            if ("style".equals(child.tag)) {
                if (child.text != null) {
                    parseStylesheet(child.text, rules);
                }
            }
            extractStyleElements(child, rules);
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

    /** Check if a Node matches a simple CSS selector. */
    public static boolean matchesSelector(Node node, String selector) {
        selector = selector.strip();
        selector = PSEUDO_ELEMENT_PATTERN.matcher(selector).replaceAll("").strip();

        Matcher notMatcher = NOT_PATTERN.matcher(selector);
        while (notMatcher.find()) {
            String negated = notMatcher.group(1).strip();
            if (negated.isEmpty() || matchesSelector(node, negated))
                return false;
            selector = notMatcher.replaceFirst("").strip();
            notMatcher = NOT_PATTERN.matcher(selector);
        }

        if (FIRST_CHILD_PATTERN.matcher(selector).find()) {
            if (!isFirstChild(node))
                return false;
            selector = FIRST_CHILD_PATTERN.matcher(selector).replaceAll("").strip();
        }

        if (LAST_CHILD_PATTERN.matcher(selector).find()) {
            if (!isLastChild(node))
                return false;
            selector = LAST_CHILD_PATTERN.matcher(selector).replaceAll("").strip();
        }

        Matcher nthMatcher = NTH_CHILD_PATTERN.matcher(selector);
        while (nthMatcher.find()) {
            String nthExpr = nthMatcher.group(1).strip();
            if (!matchesNthChild(node, nthExpr))
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
        String localName = node.tag;

        // ID selector
        if (selector.startsWith("#")) {
            String id = node.get("id");
            return selector.substring(1).equals(id);
        }

        // Class selector
        if (selector.startsWith(".")) {
            String className = node.get("class");
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
            String className = node.get("class");
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
            return id.equals(node.get("id"));
        }

        // Simple type selector
        return selector.equals(localName);
    }

    private static boolean isFirstChild(Node node) {
        if (node.parent == null)
            return false;
        for (Node sibling : node.parent.children) {
            return sibling == node;
        }
        return false;
    }

    private static boolean isLastChild(Node node) {
        if (node.parent == null)
            return false;
        var siblings = node.parent.children;
        return !siblings.isEmpty() && siblings.get(siblings.size() - 1) == node;
    }

    private static boolean matchesNthChild(Node node, String expr) {
        if (node.parent == null)
            return false;

        int index = 0;
        for (Node sibling : node.parent.children) {
            index++;
            if (sibling == node)
                break;
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
    public static MatchResult getAllMatchingDeclarations(Node node, List<StyleRule> rules) {
        List<Declaration> normal = new ArrayList<>();
        List<Declaration> important = new ArrayList<>();
        for (StyleRule rule : rules) {
            if (matchesSelector(node, rule.selector())) {
                (rule.important() ? important : normal).addAll(rule.declarations());
            }
        }
        return new MatchResult(normal, important);
    }

    /** Get CSS declarations that apply to this node from a list of rules. */
    public static List<Declaration> getMatchingDeclarations(Node node, List<StyleRule> rules, boolean important) {
        List<Declaration> result = new ArrayList<>();
        for (StyleRule rule : rules) {
            if (rule.important() == important && matchesSelector(node, rule.selector())) {
                result.addAll(rule.declarations());
            }
        }
        return result;
    }

    /**
     * Resolve CSS custom properties in {@code var(--name[, fallback])} expressions.
     */
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

    /**
     * Find the matching ')' for a var() expression, accounting for nested
     * parentheses.
     */
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

    /**
     * Split var() content into [name, fallback], respecting nested parentheses in
     * fallback.
     */
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

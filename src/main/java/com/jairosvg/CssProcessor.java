package com.jairosvg;

import java.util.*;
import java.util.regex.*;

/**
 * Minimal CSS parsing for SVG inline styles and stylesheets.
 * Port of CairoSVG css.py
 */
public final class CssProcessor {

    private CssProcessor() {}

    /** A CSS declaration: (property-name, value). */
    public record Declaration(String name, String value) {}

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
            if (part.isEmpty()) continue;
            int colonIdx = part.indexOf(':');
            if (colonIdx < 0) continue;

            String name = part.substring(0, colonIdx).strip().toLowerCase();
            String value = part.substring(colonIdx + 1).strip();

            if (name.startsWith("-")) continue; // Skip vendor prefixes

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

    /** A CSS rule with selector and declarations. */
    public record StyleRule(String selector, List<Declaration> declarations, boolean important) {}

    private static void extractStyleElements(org.w3c.dom.Element element, List<StyleRule> rules) {
        var childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof org.w3c.dom.Element child) {
                String localName = child.getLocalName();
                if (localName == null) localName = child.getTagName();
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
        cssText = cssText.replaceAll("/\\*.*?\\*/", "");
        // Handle @import (basic - skip for now)
        cssText = cssText.replaceAll("@import[^;]*;", "");

        Pattern rulePattern = Pattern.compile("([^{}]+)\\{([^}]*)\\}");
        Matcher m = rulePattern.matcher(cssText);
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

        // Universal selector
        if ("*".equals(selector)) return true;

        // Type selector
        String localName = element.getLocalName();
        if (localName == null) localName = element.getTagName();

        // ID selector
        if (selector.startsWith("#")) {
            String id = element.getAttribute("id");
            return selector.substring(1).equals(id);
        }

        // Class selector
        if (selector.startsWith(".")) {
            String className = element.getAttribute("class");
            if (className != null) {
                for (String cls : className.split("\\s+")) {
                    if (selector.substring(1).equals(cls)) return true;
                }
            }
            return false;
        }

        // Type selector (possibly with class or id)
        if (selector.contains(".")) {
            int dotIdx = selector.indexOf('.');
            String type = selector.substring(0, dotIdx);
            String cls = selector.substring(dotIdx + 1);
            if (!type.isEmpty() && !type.equals(localName)) return false;
            String className = element.getAttribute("class");
            if (className == null) return false;
            for (String c : className.split("\\s+")) {
                if (cls.equals(c)) return true;
            }
            return false;
        }

        if (selector.contains("#")) {
            int hashIdx = selector.indexOf('#');
            String type = selector.substring(0, hashIdx);
            String id = selector.substring(hashIdx + 1);
            if (!type.isEmpty() && !type.equals(localName)) return false;
            return id.equals(element.getAttribute("id"));
        }

        // Simple type selector
        return selector.equals(localName);
    }

    /** Get CSS declarations that apply to this element from a list of rules. */
    public static List<Declaration> getMatchingDeclarations(
            org.w3c.dom.Element element, List<StyleRule> rules, boolean important) {
        List<Declaration> result = new ArrayList<>();
        for (StyleRule rule : rules) {
            if (rule.important() == important && matchesSelector(element, rule.selector())) {
                result.addAll(rule.declarations());
            }
        }
        return result;
    }
}

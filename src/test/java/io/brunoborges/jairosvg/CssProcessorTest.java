package io.brunoborges.jairosvg;

import io.brunoborges.jairosvg.css.CssProcessor;
import io.brunoborges.jairosvg.dom.Node;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CssProcessorTest {

    @Test
    void testParsePseudoAttributes() {
        var attrs = CssProcessor.parsePseudoAttributes("type=\"text/css\" href=\"style.css\"");
        assertEquals("text/css", attrs.get("type"));
        assertEquals("style.css", attrs.get("href"));

        // Single quotes
        var attrs2 = CssProcessor.parsePseudoAttributes("type='text/css' href='other.css'");
        assertEquals("text/css", attrs2.get("type"));
        assertEquals("other.css", attrs2.get("href"));

        // Empty/null
        assertTrue(CssProcessor.parsePseudoAttributes("").isEmpty());
        assertTrue(CssProcessor.parsePseudoAttributes(null).isEmpty());
    }

    @Test
    void testCssPseudoClassesAndPseudoElementsSelectorMatching() throws Exception {
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g>
                    <rect id="first"/>
                    <rect id="second" class="skip"/>
                    <rect id="third"/>
                  </g>
                </svg>
                """);
        // Navigate: root(svg) -> g -> children
        Node g = root.children.get(0);
        Node first = g.children.get(0);
        Node second = g.children.get(1);
        Node third = g.children.get(2);

        assertTrue(CssProcessor.matchesSelector(first, "rect:first-child"));
        assertFalse(CssProcessor.matchesSelector(second, "rect:first-child"));
        assertTrue(CssProcessor.matchesSelector(third, "rect:last-child"));
        assertTrue(CssProcessor.matchesSelector(second, "rect:nth-child(2)"));
        assertTrue(CssProcessor.matchesSelector(first, "rect:nth-child(odd)"));
        assertTrue(CssProcessor.matchesSelector(second, "rect:nth-child(even)"));
        assertTrue(CssProcessor.matchesSelector(first, "rect:not(.skip)"));
        assertFalse(CssProcessor.matchesSelector(second, "rect:not(.skip)"));
        assertTrue(CssProcessor.matchesSelector(second, "rect:not(:first-child)"));
        assertTrue(CssProcessor.matchesSelector(first, "rect::first-line"));
        assertTrue(CssProcessor.matchesSelector(first, "rect::first-letter"));
        assertFalse(CssProcessor.matchesSelector(first, "circle::first-line"));
    }

    @Test
    void testCssPseudoClassesInStylesheetApplication() throws Exception {
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g>
                    <rect id="first"/>
                    <rect id="second" class="skip"/>
                    <rect id="third"/>
                  </g>
                </svg>
                """);
        Node g = root.children.get(0);
        Node first = g.children.get(0);
        Node second = g.children.get(1);
        Node third = g.children.get(2);

        List<CssProcessor.StyleRule> rules = new ArrayList<>();
        CssProcessor.parseStylesheet("""
                rect:first-child { fill: red; }
                rect:nth-child(2) { stroke: blue; }
                rect:last-child { opacity: 0.5; }
                rect:not(.skip) { stroke-width: 3; }
                rect::first-line { display: inline; }
                """, rules);

        var firstDecls = CssProcessor.getMatchingDeclarations(first, rules, false);
        var secondDecls = CssProcessor.getMatchingDeclarations(second, rules, false);
        var thirdDecls = CssProcessor.getMatchingDeclarations(third, rules, false);

        assertTrue(firstDecls.stream().anyMatch(d -> "fill".equals(d.name()) && "red".equals(d.value())));
        assertTrue(firstDecls.stream().anyMatch(d -> "stroke-width".equals(d.name()) && "3".equals(d.value())));
        assertTrue(firstDecls.stream().anyMatch(d -> "display".equals(d.name()) && "inline".equals(d.value())));
        assertTrue(secondDecls.stream().anyMatch(d -> "stroke".equals(d.name()) && "blue".equals(d.value())));
        assertFalse(secondDecls.stream().anyMatch(d -> "stroke-width".equals(d.name())));
        assertTrue(thirdDecls.stream().anyMatch(d -> "opacity".equals(d.name()) && "0.5".equals(d.value())));
        assertTrue(thirdDecls.stream().anyMatch(d -> "stroke-width".equals(d.name()) && "3".equals(d.value())));
    }

    @Test
    void testParseDeclarationsKeepsCustomProperties() {
        var parsed = CssProcessor.parseDeclarations("--main-color: red; fill: var(--main-color);");
        assertTrue(parsed[0].stream().anyMatch(d -> "--main-color".equals(d.name()) && "red".equals(d.value())));
        assertTrue(parsed[0].stream().anyMatch(d -> "fill".equals(d.name()) && "var(--main-color)".equals(d.value())));
    }

    @Test
    void testResolveCustomPropertiesWithFallback() {
        var attributes = new LinkedHashMap<String, String>();
        attributes.put("--primary", "blue");
        attributes.put("fill", "var(--primary)");
        attributes.put("stroke", "var(--missing, rgb(255, 0, 0))");

        assertEquals("blue", CssProcessor.resolveCustomProperties(attributes.get("fill"), attributes));
        assertEquals("rgb(255, 0, 0)", CssProcessor.resolveCustomProperties(attributes.get("stroke"), attributes));
    }

    /** Parse SVG string into a raw Node tree (without CSS application). */
    private static Node parseToNodeTree(String xml) throws Exception {
        return Node.parseTree(xml.getBytes(StandardCharsets.UTF_8));
    }
}

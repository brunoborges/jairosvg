package io.brunoborges.jairosvg;

import io.brunoborges.jairosvg.css.CssProcessor;
import io.brunoborges.jairosvg.dom.Node;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // ---- parseDeclarations edge cases (L51, L59, L62, L68, L72, L77) ----

    @Test
    void testParseDeclarationsNullAndEmpty() {
        // L51: null input
        var nullResult = CssProcessor.parseDeclarations(null);
        assertTrue(nullResult[0].isEmpty());
        assertTrue(nullResult[1].isEmpty());

        // L51: empty input
        var emptyResult = CssProcessor.parseDeclarations("");
        assertTrue(emptyResult[0].isEmpty());
        assertTrue(emptyResult[1].isEmpty());
    }

    @Test
    void testParseDeclarationsEmptyParts() {
        // L59: empty parts from consecutive semicolons
        var parsed = CssProcessor.parseDeclarations("fill:red;;stroke:blue;");
        assertEquals(2, parsed[0].size());
    }

    @Test
    void testParseDeclarationsNoColon() {
        // L62: malformed declaration without colon
        var parsed = CssProcessor.parseDeclarations("fill red; stroke: blue");
        assertEquals(1, parsed[0].size());
        assertEquals("stroke", parsed[0].get(0).name());
    }

    @Test
    void testParseDeclarationsVendorPrefix() {
        // L68: vendor-prefixed property should be skipped
        var parsed = CssProcessor.parseDeclarations("-webkit-fill: red; fill: blue");
        assertEquals(1, parsed[0].size());
        assertEquals("fill", parsed[0].get(0).name());
        assertEquals("blue", parsed[0].get(0).value());
    }

    @Test
    void testParseDeclarationsImportant() {
        // L72, L77: !important handling
        var parsed = CssProcessor.parseDeclarations("fill: red !important; stroke: blue");
        assertEquals(1, parsed[0].size());
        assertEquals("stroke", parsed[0].get(0).name());
        assertEquals(1, parsed[1].size());
        assertEquals("fill", parsed[1].get(0).name());
        assertEquals("red", parsed[1].get(0).value());
    }

    // ---- External stylesheet PI parsing (L104, L106, L112, L116) ----

    @Test
    void testExternalStylesheetNonCssType() {
        // L112: non-CSS type should be skipped
        var rules = CssProcessor.parseExternalStylesheets(List.of("type=\"text/xsl\" href=\"style.xsl\""),
                (url, type) -> "rect { fill: red; }".getBytes(StandardCharsets.UTF_8), null);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testExternalStylesheetMissingHref() {
        // L112: missing href
        var rules = CssProcessor.parseExternalStylesheets(List.of("type=\"text/css\""),
                (url, type) -> "rect { fill: red; }".getBytes(StandardCharsets.UTF_8), null);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testExternalStylesheetEmptyHref() {
        // L112: empty href
        var rules = CssProcessor.parseExternalStylesheets(List.of("type=\"text/css\" href=\"\""),
                (url, type) -> "rect { fill: red; }".getBytes(StandardCharsets.UTF_8), null);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testExternalStylesheetTypeWithSemicolon() {
        // L106: type with parameter after semicolon
        var rules = CssProcessor.parseExternalStylesheets(
                List.of("type=\"text/css; charset=UTF-8\" href=\"style.css\""),
                (url, type) -> "rect { fill: red; }".getBytes(StandardCharsets.UTF_8), null);
        assertFalse(rules.isEmpty());
    }

    @Test
    void testExternalStylesheetNullBytes() {
        // L116: fetcher returns null
        var rules = CssProcessor.parseExternalStylesheets(List.of("type=\"text/css\" href=\"style.css\""),
                (url, type) -> null, null);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testExternalStylesheetEmptyBytes() {
        // L116: fetcher returns empty bytes
        var rules = CssProcessor.parseExternalStylesheets(List.of("type=\"text/css\" href=\"style.css\""),
                (url, type) -> new byte[0], null);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testExternalStylesheetFetchException() {
        // IOException should be caught and ignored
        var rules = CssProcessor.parseExternalStylesheets(List.of("type=\"text/css\" href=\"style.css\""),
                (url, type) -> {
                    throw new IOException("Network error");
                }, null);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testExternalStylesheetNoTypeAttribute() {
        // L104: PI without type attribute - normalizedType stays ""
        var rules = CssProcessor.parseExternalStylesheets(List.of("href=\"style.css\""),
                (url, type) -> "rect { fill: red; }".getBytes(StandardCharsets.UTF_8), null);
        assertTrue(rules.isEmpty());
    }

    // ---- parseStylesheet edge cases (L154, L178, L179, L182) ----

    @Test
    void testParseStylesheetEmptyStyleElement() throws Exception {
        // L154: <style></style> with null text
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <style></style>
                  <rect fill="blue" width="10" height="10"/>
                </svg>
                """);
        var rules = CssProcessor.parseStylesheets(root);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testParseStylesheetBlankSelector() {
        // L178: blank selector after comma
        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("rect, { fill: red; }", rules);
        // Only the non-blank selector should be recorded
        assertEquals(1, rules.size());
        assertEquals("rect", rules.get(0).selector());
    }

    @Test
    void testParseStylesheetImportantOnly() {
        // L179, L182: rule with only !important declarations
        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("rect { fill: red !important; }", rules);
        // Should have important rule, no normal rule
        assertTrue(rules.stream().anyMatch(CssProcessor.StyleRule::important));
        assertFalse(rules.stream().anyMatch(r -> !r.important()));
    }

    @Test
    void testParseStylesheetMixedImportantAndNormal() {
        // Both normal and important in same rule block
        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("rect { fill: red; stroke: blue !important; }", rules);
        assertEquals(2, rules.size());
        var normalRule = rules.stream().filter(r -> !r.important()).findFirst().orElseThrow();
        var importantRule = rules.stream().filter(CssProcessor.StyleRule::important).findFirst().orElseThrow();
        assertTrue(normalRule.declarations().stream().anyMatch(d -> "fill".equals(d.name())));
        assertTrue(importantRule.declarations().stream().anyMatch(d -> "stroke".equals(d.name())));
    }

    @Test
    void testParseStylesheetCommentsAndImports() {
        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("""
                /* This is a comment */
                @import url("other.css");
                rect { fill: green; }
                """, rules);
        assertEquals(1, rules.size());
        assertEquals("rect", rules.get(0).selector());
    }

    // ---- Selector matching branches (L225, L229, L236, L245, L254, L258, L270,
    // L274) ----

    @Test
    void testSelectorMatchingUniversal() throws Exception {
        // L229: * selector
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>
                """);
        Node rect = root.children.get(0);
        assertTrue(CssProcessor.matchesSelector(rect, "*"));
    }

    @Test
    void testSelectorMatchingPureId() throws Exception {
        // L236: #id selector
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect id="myRect"/></svg>
                """);
        Node rect = root.children.get(0);
        assertTrue(CssProcessor.matchesSelector(rect, "#myRect"));
        assertFalse(CssProcessor.matchesSelector(rect, "#other"));
    }

    @Test
    void testSelectorMatchingPureClassNoMatch() throws Exception {
        // L245, L246: class selector on element without class
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>
                """);
        Node rect = root.children.get(0);
        assertFalse(CssProcessor.matchesSelector(rect, ".missing"));
    }

    @Test
    void testSelectorMatchingPureClassWithMultipleClasses() throws Exception {
        // L245: class selector with space-separated classes
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect class="a b c"/></svg>
                """);
        Node rect = root.children.get(0);
        assertTrue(CssProcessor.matchesSelector(rect, ".b"));
        assertFalse(CssProcessor.matchesSelector(rect, ".d"));
    }

    @Test
    void testSelectorMatchingTypeWithClass() throws Exception {
        // L254-L267: type.class combined selector
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <rect class="highlight"/>
                  <circle class="highlight"/>
                </svg>
                """);
        Node rect = root.children.get(0);
        Node circle = root.children.get(1);
        assertTrue(CssProcessor.matchesSelector(rect, "rect.highlight"));
        assertFalse(CssProcessor.matchesSelector(circle, "rect.highlight"));
        assertTrue(CssProcessor.matchesSelector(circle, "circle.highlight"));
    }

    @Test
    void testSelectorMatchingTypeWithClassNoClassAttr() throws Exception {
        // L261: type.class where element has no class at all
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>
                """);
        Node rect = root.children.get(0);
        assertFalse(CssProcessor.matchesSelector(rect, "rect.highlight"));
    }

    @Test
    void testSelectorMatchingTypeWithClassWrongClass() throws Exception {
        // L264: type.class where class doesn't match
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect class="other"/></svg>
                """);
        Node rect = root.children.get(0);
        assertFalse(CssProcessor.matchesSelector(rect, "rect.highlight"));
    }

    @Test
    void testSelectorMatchingTypeWithId() throws Exception {
        // L270-L276: type#id combined selector
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <rect id="r1"/>
                  <circle id="c1"/>
                </svg>
                """);
        Node rect = root.children.get(0);
        Node circle = root.children.get(1);
        assertTrue(CssProcessor.matchesSelector(rect, "rect#r1"));
        assertFalse(CssProcessor.matchesSelector(circle, "rect#r1"));
        assertFalse(CssProcessor.matchesSelector(rect, "rect#r2"));
    }

    @Test
    void testSelectorMatchingEmptyAfterPseudoStrip() throws Exception {
        // L225: selector becomes empty after pseudo-class stripping
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g><rect/></g>
                </svg>
                """);
        Node rect = root.children.get(0).children.get(0);
        assertTrue(CssProcessor.matchesSelector(rect, ":first-child"));
    }

    @Test
    void testSelectorNotMatchingSelf() throws Exception {
        // L198: :not(rect) on a rect should return false
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>
                """);
        Node rect = root.children.get(0);
        assertFalse(CssProcessor.matchesSelector(rect, ":not(rect)"));
    }

    @Test
    void testSelectorNotEmpty() throws Exception {
        // L198: :not() with empty negated content
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>
                """);
        Node rect = root.children.get(0);
        assertFalse(CssProcessor.matchesSelector(rect, ":not()"));
    }

    // ---- :last-child / :first-child on root/orphan (L284, L286, L293, L296) ----

    @Test
    void testFirstChildOnRoot() throws Exception {
        // L284: isFirstChild on node with no parent
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"></svg>
                """);
        assertFalse(CssProcessor.matchesSelector(root, ":first-child"));
    }

    @Test
    void testLastChildOnRoot() throws Exception {
        // L293: isLastChild on node with no parent
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"></svg>
                """);
        assertFalse(CssProcessor.matchesSelector(root, ":last-child"));
    }

    @Test
    void testLastChildNotLast() throws Exception {
        // L296: node is not the last child
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g><rect/><circle/></g>
                </svg>
                """);
        Node rect = root.children.get(0).children.get(0);
        assertFalse(CssProcessor.matchesSelector(rect, ":last-child"));
    }

    // ---- nth-child: an+b patterns (L300, L309, L314, L316, L325, L330, L332,
    // L337-L344) ----

    @Test
    void testNthChildOnRoot() throws Exception {
        // L300: matchesNthChild on node with no parent
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"></svg>
                """);
        assertFalse(CssProcessor.matchesSelector(root, ":nth-child(1)"));
    }

    @Test
    void testNthChildAnBPattern() throws Exception {
        // L330-L344: various an+b patterns
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g>
                    <rect id="c1"/>
                    <rect id="c2"/>
                    <rect id="c3"/>
                    <rect id="c4"/>
                    <rect id="c5"/>
                    <rect id="c6"/>
                  </g>
                </svg>
                """);
        Node g = root.children.get(0);

        // 2n+1 matches children 1,3,5 (positions 1-based)
        assertTrue(CssProcessor.matchesSelector(g.children.get(0), ":nth-child(2n+1)"));
        assertFalse(CssProcessor.matchesSelector(g.children.get(1), ":nth-child(2n+1)"));
        assertTrue(CssProcessor.matchesSelector(g.children.get(2), ":nth-child(2n+1)"));

        // -n+3 matches children 1,2,3
        assertTrue(CssProcessor.matchesSelector(g.children.get(0), ":nth-child(-n+3)"));
        assertTrue(CssProcessor.matchesSelector(g.children.get(2), ":nth-child(-n+3)"));
        assertFalse(CssProcessor.matchesSelector(g.children.get(3), ":nth-child(-n+3)"));

        // n+3 matches children 3,4,5,6
        assertFalse(CssProcessor.matchesSelector(g.children.get(1), ":nth-child(n+3)"));
        assertTrue(CssProcessor.matchesSelector(g.children.get(2), ":nth-child(n+3)"));
        assertTrue(CssProcessor.matchesSelector(g.children.get(5), ":nth-child(n+3)"));

        // 3n+2 matches children 2,5
        assertFalse(CssProcessor.matchesSelector(g.children.get(0), ":nth-child(3n+2)"));
        assertTrue(CssProcessor.matchesSelector(g.children.get(1), ":nth-child(3n+2)"));
        assertFalse(CssProcessor.matchesSelector(g.children.get(2), ":nth-child(3n+2)"));
        assertTrue(CssProcessor.matchesSelector(g.children.get(4), ":nth-child(3n+2)"));
    }

    @Test
    void testNthChildMalformedExpression() throws Exception {
        // L325: malformed expression that doesn't parse as an+b
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g><rect/></g>
                </svg>
                """);
        Node rect = root.children.get(0).children.get(0);
        assertFalse(CssProcessor.matchesSelector(rect, ":nth-child(foo)"));
    }

    @Test
    void testNthChildZeroCoefficient() throws Exception {
        // L338-L339: 0n+b exact match
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g><rect/><rect/><rect/></g>
                </svg>
                """);
        Node g = root.children.get(0);
        assertFalse(CssProcessor.matchesSelector(g.children.get(0), ":nth-child(0n+3)"));
        assertTrue(CssProcessor.matchesSelector(g.children.get(2), ":nth-child(0n+3)"));
    }

    @Test
    void testNthChildNonMatchingRemainder() throws Exception {
        // L342: diff % a != 0
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g><rect/><rect/><rect/><rect/></g>
                </svg>
                """);
        Node g = root.children.get(0);
        // 3n+1 matches 1,4 but not 2,3
        assertTrue(CssProcessor.matchesSelector(g.children.get(0), ":nth-child(3n+1)"));
        assertFalse(CssProcessor.matchesSelector(g.children.get(1), ":nth-child(3n+1)"));
        assertFalse(CssProcessor.matchesSelector(g.children.get(2), ":nth-child(3n+1)"));
        assertTrue(CssProcessor.matchesSelector(g.children.get(3), ":nth-child(3n+1)"));
    }

    // ---- getAllMatchingDeclarations and getMatchingDeclarations (L357, L367) ----

    @Test
    void testGetAllMatchingDeclarationsWithImportant() throws Exception {
        // L357: important branch in getAllMatchingDeclarations
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>
                """);
        Node rect = root.children.get(0);
        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("rect { fill: red; stroke: blue !important; }", rules);

        var result = CssProcessor.getAllMatchingDeclarations(rect, rules);
        assertTrue(result.normal().stream().anyMatch(d -> "fill".equals(d.name())));
        assertTrue(result.important().stream().anyMatch(d -> "stroke".equals(d.name())));
    }

    @Test
    void testGetMatchingDeclarationsImportantFlag() throws Exception {
        // L367: getMatchingDeclarations with important=true
        Node root = parseToNodeTree("""
                <svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>
                """);
        Node rect = root.children.get(0);
        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("rect { fill: red; stroke: blue !important; }", rules);

        var importantDecls = CssProcessor.getMatchingDeclarations(rect, rules, true);
        assertTrue(importantDecls.stream().anyMatch(d -> "stroke".equals(d.name())));
        assertFalse(importantDecls.stream().anyMatch(d -> "fill".equals(d.name())));
    }

    // ---- Custom property resolution edge cases (L382, L389, L396, L411, L415,
    // L419, L433-L458) ----

    @Test
    void testResolveCustomPropertiesNullAndEmpty() {
        // L382: null, empty, no var()
        assertNull(CssProcessor.resolveCustomProperties(null, Map.of()));
        assertEquals("", CssProcessor.resolveCustomProperties("", Map.of()));
        assertEquals("red", CssProcessor.resolveCustomProperties("red", Map.of()));
    }

    @Test
    void testResolveCustomPropertiesTrailingText() {
        // L389: text after var()
        var attrs = new LinkedHashMap<String, String>();
        attrs.put("--a", "1px");
        assertEquals("1px solid", CssProcessor.resolveCustomProperties("var(--a) solid", attrs));
    }

    @Test
    void testResolveCustomPropertiesUnterminated() {
        // L396: unterminated var(
        var attrs = new LinkedHashMap<String, String>();
        assertEquals("fill: var(--a", CssProcessor.resolveCustomProperties("fill: var(--a", attrs));
    }

    @Test
    void testResolveCustomPropertiesCyclicReference() {
        // L411: cyclic custom property reference
        var attrs = new LinkedHashMap<String, String>();
        attrs.put("--a", "var(--a)");
        // Should not infinitely recurse; cyclic ref means value is unresolved
        String result = CssProcessor.resolveCustomProperties("var(--a)", attrs);
        assertNotNull(result);
    }

    @Test
    void testResolveCustomPropertiesMissingNoFallback() {
        // L415, L419: missing property with no fallback produces empty
        var attrs = new LinkedHashMap<String, String>();
        String result = CssProcessor.resolveCustomProperties("var(--missing)", attrs);
        assertEquals("", result);
    }

    @Test
    void testResolveCustomPropertiesNestedFunctions() {
        // L433, L454, L456, L458: nested parentheses in fallback
        var attrs = new LinkedHashMap<String, String>();
        String result = CssProcessor.resolveCustomProperties("var(--missing, rgb(255, 0, 0))", attrs);
        assertEquals("rgb(255, 0, 0)", result);
    }

    @Test
    void testResolveCustomPropertiesNestedVar() {
        // Nested var() in fallback
        var attrs = new LinkedHashMap<String, String>();
        attrs.put("--fallback", "green");
        String result = CssProcessor.resolveCustomProperties("var(--missing, var(--fallback))", attrs);
        assertEquals("green", result);
    }

    @Test
    void testResolveCustomPropertiesLowercaseLookup() {
        // L411: fallback to lowercase lookup
        var attrs = new LinkedHashMap<String, String>();
        attrs.put("--my-color", "blue");
        assertEquals("blue", CssProcessor.resolveCustomProperties("var(--MY-COLOR)", attrs));
    }

    // ---- Rendering-based CSS selector tests ----

    @Test
    void testCssIdSelectorRendering() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>#myRect { fill: red; }</style>
                  <rect id="myRect" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void testCssUniversalSelectorRendering() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>* { fill: lime; }</style>
                  <rect x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 255, 0);
    }

    @Test
    void testCssTypeClassSelectorRendering() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>rect.highlight { fill: blue; }</style>
                  <rect class="highlight" x="0" y="0" width="50" height="100"/>
                  <rect class="other" x="50" y="0" width="50" height="100" fill="red"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 25, 50, 0, 0, 255);
        RenderTestHelper.assertPixelColor(img, 75, 50, 255, 0, 0);
    }

    @Test
    void testCssTypeIdSelectorRendering() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>rect#special { fill: blue; }</style>
                  <rect id="special" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    @Test
    void testCssImportantOverridesInline() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>rect { fill: blue !important; }</style>
                  <rect style="fill:red" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        // !important CSS should override inline style
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    @Test
    void testCssCustomPropertiesRendering() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    svg { --bg: red; }
                    rect { fill: var(--bg); }
                  </style>
                  <rect x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void testCssCustomPropertyFallbackRendering() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect { fill: var(--undefined, blue); }
                  </style>
                  <rect x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    @Test
    void testCssFirstChildLastChildRendering() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect:first-child { fill: red; }
                    rect:last-child { fill: blue; }
                  </style>
                  <g>
                    <rect x="0" y="0" width="50" height="100"/>
                    <rect x="50" y="0" width="50" height="100"/>
                  </g>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 25, 50, 255, 0, 0);
        RenderTestHelper.assertPixelColor(img, 75, 50, 0, 0, 255);
    }

    @Test
    void testCssNthChildRendering() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect:nth-child(2n+1) { fill: red; }
                    rect:nth-child(2n) { fill: blue; }
                  </style>
                  <g>
                    <rect x="0" y="0" width="25" height="100"/>
                    <rect x="25" y="0" width="25" height="100"/>
                    <rect x="50" y="0" width="25" height="100"/>
                    <rect x="75" y="0" width="25" height="100"/>
                  </g>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 12, 50, 255, 0, 0);
        RenderTestHelper.assertPixelColor(img, 37, 50, 0, 0, 255);
        RenderTestHelper.assertPixelColor(img, 62, 50, 255, 0, 0);
        RenderTestHelper.assertPixelColor(img, 87, 50, 0, 0, 255);
    }

    /** Parse SVG string into a raw Node tree (without CSS application). */
    private static Node parseToNodeTree(String xml) throws Exception {
        return Node.parseTree(xml.getBytes(StandardCharsets.UTF_8));
    }

    // ── :first-child on root element (parent == null) → false ────────────

    @Test
    void firstChildOnRootElement() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <style>
                    svg:first-child { fill: green; }
                  </style>
                  <rect width="50" height="50"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── nth-child with complex formula ───────────────────────────────────

    @Test
    void nthChildFormula() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <style>
                    rect:nth-child(2n+1) { fill: red; }
                    rect:nth-child(2n) { fill: blue; }
                  </style>
                  <rect x="0" y="0" width="25" height="50"/>
                  <rect x="25" y="0" width="25" height="50"/>
                  <rect x="50" y="0" width="25" height="50"/>
                  <rect x="75" y="0" width="25" height="50"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── nth-child odd ──

    @Test
    void nthChildOdd() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect:nth-child(odd) { fill: red; }
                    rect:nth-child(even) { fill: blue; }
                  </style>
                  <g>
                    <rect width="30" height="100"/>
                    <rect x="35" width="30" height="100"/>
                    <rect x="70" width="30" height="100"/>
                  </g>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
        // First rect (child 1=odd) should be red, second (child 2=even) blue
        int[] first = RenderTestHelper.rgba(img, 15, 50);
        int[] second = RenderTestHelper.rgba(img, 50, 50);
        assertTrue(first[0] > 200, "Odd child should be red, got r=" + first[0]);
        assertTrue(second[2] > 200, "Even child should be blue, got b=" + second[2]);
    }

    // ── last-child selector ──

    @Test
    void lastChildSelector() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect:last-child { fill: green; }
                  </style>
                  <g>
                    <rect width="50" height="100" fill="red"/>
                    <rect x="50" width="50" height="100"/>
                  </g>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
        int[] last = RenderTestHelper.rgba(img, 75, 50);
        assertTrue(last[1] > 100, "Last child should be green, got g=" + last[1]);
    }

    // ── :not() pseudo-class ──

    @Test
    void notPseudoClass() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect:not(.special) { fill: gray; }
                    .special { fill: red; }
                  </style>
                  <rect width="50" height="100"/>
                  <rect x="50" width="50" height="100" class="special"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── CSS with !important declaration ──

    @Test
    void cssImportant() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect { fill: blue !important; }
                  </style>
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
        // Important CSS should override attribute → blue
        int[] pixel = RenderTestHelper.rgba(img, 50, 50);
        assertTrue(pixel[2] > 200, "!important should override fill attribute");
    }

    // ── CSS var() with fallback ──

    @Test
    void cssVarWithFallback() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    :root { --mycolor: green; }
                    rect { fill: var(--mycolor, red); }
                  </style>
                  <rect width="100" height="100"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── CSS var() with undefined var (uses fallback) ──

    @Test
    void cssVarUndefinedUsesFallback() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect { fill: var(--undefined, orange); }
                  </style>
                  <rect width="100" height="100"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── CSS with descendant combinator ──

    @Test
    void cssDescendantCombinator() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    g rect { fill: purple; }
                  </style>
                  <g>
                    <rect width="100" height="100"/>
                  </g>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── nth-child with an+b where a < 0 ──

    @Test
    void nthChildNegativeA() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="150" height="100">
                  <style>
                    rect:nth-child(-n+2) { fill: red; }
                  </style>
                  <rect width="50" height="100"/>
                  <rect x="50" width="50" height="100"/>
                  <rect x="100" width="50" height="100"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── nth-child with a==0 (matches exactly b) ──

    @Test
    void nthChildZeroA() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="150" height="100">
                  <style>
                    rect:nth-child(0n+2) { fill: green; }
                  </style>
                  <g>
                    <rect width="50" height="100"/>
                    <rect x="50" width="50" height="100"/>
                    <rect x="100" width="50" height="100"/>
                  </g>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
        // Only second rect should be green
        int[] second = RenderTestHelper.rgba(img, 75, 50);
        assertTrue(second[1] > 100, "Second child should be green, got g=" + second[1]);
    }

    // ── @import URL extraction ────────────────────────────────────────────

    @Test
    void importUrlExtractionUrlDoubleQuotes() {
        assertEquals("path/to/style.css", CssProcessor.extractImportUrl("@import url(\"path/to/style.css\");"));
    }

    @Test
    void importUrlExtractionUrlSingleQuotes() {
        assertEquals("path/to/style.css", CssProcessor.extractImportUrl("@import url('path/to/style.css');"));
    }

    @Test
    void importUrlExtractionUrlNoQuotes() {
        assertEquals("path/to/style.css", CssProcessor.extractImportUrl("@import url(path/to/style.css);"));
    }

    @Test
    void importUrlExtractionBareDoubleQuotes() {
        assertEquals("path/to/style.css", CssProcessor.extractImportUrl("@import \"path/to/style.css\";"));
    }

    @Test
    void importUrlExtractionBareSingleQuotes() {
        assertEquals("path/to/style.css", CssProcessor.extractImportUrl("@import 'path/to/style.css';"));
    }

    @Test
    void importUrlExtractionHttpUrl() {
        assertEquals("https://example.com/style.css",
                CssProcessor.extractImportUrl("@import url(\"https://example.com/style.css\");"));
    }

    @Test
    void importUrlExtractionReturnsNullForInvalid() {
        assertNull(CssProcessor.extractImportUrl("@import ;"));
        assertNull(CssProcessor.extractImportUrl("not an import"));
    }

    // ── @import resolution with mock fetcher ─────────────────────────────

    @Test
    void importResolvedWithFetcher() {
        String importedCss = "rect { fill: blue; }";
        var fetcher = mockFetcher(Map.of("http://example.com/imported.css", importedCss));

        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("""
                @import url("http://example.com/imported.css");
                circle { stroke: red; }
                """, rules, fetcher, "http://example.com/base.css", new java.util.HashSet<>());

        // Imported rule should come first (processed before local rules)
        assertTrue(rules.stream().anyMatch(r -> "rect".equals(r.selector())
                && r.declarations().stream().anyMatch(d -> "fill".equals(d.name()) && "blue".equals(d.value()))));
        assertTrue(rules.stream().anyMatch(r -> "circle".equals(r.selector())
                && r.declarations().stream().anyMatch(d -> "stroke".equals(d.name()) && "red".equals(d.value()))));
    }

    @Test
    void nestedImportsResolved() {
        String innerCss = "ellipse { opacity: 0.5; }";
        String outerCss = """
                @import url("http://example.com/inner.css");
                rect { fill: green; }
                """;
        var fetcher = mockFetcher(
                Map.of("http://example.com/outer.css", outerCss, "http://example.com/inner.css", innerCss));

        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("""
                @import url("http://example.com/outer.css");
                circle { stroke: red; }
                """, rules, fetcher, "http://example.com/base.css", new java.util.HashSet<>());

        assertTrue(rules.stream().anyMatch(r -> "ellipse".equals(r.selector())));
        assertTrue(rules.stream().anyMatch(r -> "rect".equals(r.selector())));
        assertTrue(rules.stream().anyMatch(r -> "circle".equals(r.selector())));
    }

    @Test
    void circularImportDetection() {
        // a.css imports b.css which imports a.css — should not loop
        String aCss = """
                @import url("http://example.com/b.css");
                rect { fill: red; }
                """;
        String bCss = """
                @import url("http://example.com/a.css");
                circle { fill: blue; }
                """;
        var fetcher = mockFetcher(Map.of("http://example.com/a.css", aCss, "http://example.com/b.css", bCss));

        var rules = new ArrayList<CssProcessor.StyleRule>();
        var visited = new java.util.HashSet<String>();
        visited.add("http://example.com/a.css"); // mark current as visited
        CssProcessor.parseStylesheet(aCss, rules, fetcher, "http://example.com/a.css", visited);

        // Both should be parsed, no infinite loop
        assertTrue(rules.stream().anyMatch(r -> "rect".equals(r.selector())));
        assertTrue(rules.stream().anyMatch(r -> "circle".equals(r.selector())));
    }

    @Test
    void importSkippedGracefullyOnFetchError() {
        // Fetcher always throws — import should be silently skipped
        io.brunoborges.jairosvg.util.UrlHelper.UrlFetcher fetcher = (url, type) -> {
            throw new IOException("Network error");
        };

        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("""
                @import url("http://unreachable.invalid/style.css");
                rect { fill: green; }
                """, rules, fetcher, "http://example.com/base.css", new java.util.HashSet<>());

        assertEquals(1, rules.size());
        assertEquals("rect", rules.get(0).selector());
    }

    @Test
    void importSkippedGracefullyInRendering() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    @import url("http://nonexistent.invalid/style.css");
                    rect { fill: green; }
                  </style>
                  <rect width="100" height="100"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 128, 0);
    }

    @Test
    void twoArgParseStylesheetStillStripsImports() {
        var rules = new ArrayList<CssProcessor.StyleRule>();
        CssProcessor.parseStylesheet("""
                @import url("anything.css");
                rect { fill: red; }
                """, rules);
        assertEquals(1, rules.size());
        assertEquals("rect", rules.get(0).selector());
    }

    private static io.brunoborges.jairosvg.util.UrlHelper.UrlFetcher mockFetcher(Map<String, String> responses) {
        return (url, type) -> {
            String content = responses.get(url);
            if (content == null) {
                throw new IOException("Not found: " + url);
            }
            return content.getBytes(StandardCharsets.UTF_8);
        };
    }
}

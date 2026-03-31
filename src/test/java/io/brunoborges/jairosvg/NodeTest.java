package io.brunoborges.jairosvg;

import io.brunoborges.jairosvg.dom.Node;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    // ---- currentColor resolution (L212-L215) ----

    @Test
    void testCurrentColorMultipleAttributes() throws Exception {
        // L212-L213: multiple color attributes set to currentColor
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    rect { color: red; fill: currentColor; stroke: currentColor; stroke-width: 20; }
                  </style>
                  <rect x="10" y="10" width="80" height="80"/>
                </svg>
                """);
        // Center should be red (fill=currentColor resolved to color=red)
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void testCurrentColorDefaultsToBlack() throws Exception {
        // currentColor with no explicit 'color' property -> defaults to black
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect fill="currentColor" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 0);
    }

    // ---- inherit resolution (L221-L225) ----

    @Test
    void testInheritFromParent() throws Exception {
        // L221-L222: inherit resolves to parent's value
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <g fill="red">
                    <rect fill="inherit" x="0" y="0" width="100" height="100"/>
                  </g>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void testInheritWithoutParentValue() throws Exception {
        // L222-L225: inherit with no parent value -> attribute removed
        // The rect should get default fill (black)
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect fill="inherit" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 0);
    }

    // ---- font shorthand expansion (L231-L236) ----

    @Test
    void testFontShorthand() throws Exception {
        // L231: font shorthand is parsed and sub-properties expanded
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <text font="italic bold 20px serif">Hello</text>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));
        Node text = root.children.get(0);
        // font sub-properties should be expanded
        assertNotNull(text.get("font-family"));
    }

    @Test
    void testFontShorthandDoesNotOverrideExplicit() throws Exception {
        // L233-L234: explicit sub-property should not be overwritten
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <text font="italic bold 20px serif" font-size="32">Hello</text>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));
        Node text = root.children.get(0);
        assertEquals("32", text.get("font-size"));
    }

    // ---- inline style !important (L192) ----

    @Test
    void testInlineStyleImportant() throws Exception {
        // L192: inline style with !important
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>rect { fill: blue; }</style>
                  <rect style="fill: red !important" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ---- CSS !important overrides inline style (L199) ----

    @Test
    void testCssImportantOverridesInlineStyle() throws Exception {
        // L199: stylesheet !important overrides inline
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>rect { fill: blue !important; }</style>
                  <rect style="fill: red" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    // ---- empty style attribute (L187) ----

    @Test
    void testEmptyStyleAttribute() throws Exception {
        // L187: style="" should not cause issues
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect style="" fill="red" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ---- getHref fallbacks (L280, L282) ----

    @Test
    void testGetHrefFallbacks() throws Exception {
        // L280: xlink:href first
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg"
                     xmlns:xlink="http://www.w3.org/1999/xlink">
                  <defs><rect id="r1" width="10" height="10" fill="red"/></defs>
                  <use xlink:href="#r1"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));
        Node use = root.children.get(1);
        String href = use.getHref();
        assertNotNull(href);
        assertTrue(href.contains("r1"));
    }

    @Test
    void testGetHrefPlain() throws Exception {
        // L282: plain href
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <defs><rect id="r1" width="10" height="10" fill="red"/></defs>
                  <use href="#r1"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));
        Node use = root.children.get(1);
        String href = use.getHref();
        assertNotNull(href);
        assertTrue(href.contains("r1"));
    }

    @Test
    void testGetHrefNull() throws Exception {
        // All href variants null
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <rect id="r1"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));
        Node rect = root.children.get(0);
        assertNull(rect.getHref());
    }

    // ---- parseTree edge cases (L297-L327) ----

    @Test
    void testParseTreeNullBytesNullUrl() {
        // L301: no input at all -> exception
        assertThrows(IllegalArgumentException.class, () -> Node.parseTree(null, null, null, false));
    }

    @Test
    void testParseTreeGzippedInput() throws Exception {
        // L306: gzipped SVG
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><rect fill=\"red\" width=\"10\" height=\"10\"/></svg>";
        byte[] gzipped = RenderTestHelper.gzip(svg.getBytes(StandardCharsets.UTF_8));
        Node root = Node.parseTree(gzipped);
        assertEquals("svg", root.tag);
        assertFalse(root.children.isEmpty());
    }

    @Test
    void testParseTreeShortInput() throws Exception {
        // L306: input shorter than 2 bytes should not trigger gzip check crash
        // A valid tiny SVG won't be <2 bytes, but the code checks length>2
        // Use a normal SVG to confirm it works
        Node root = Node.parseTree("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8));
        assertEquals("svg", root.tag);
    }

    @Test
    void testParseTreeWithCustomFetcher() throws Exception {
        // L299, L313: custom urlFetcher
        Node root = Node.parseTree(
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>".getBytes(StandardCharsets.UTF_8), null,
                (url, type) -> null, false);
        assertEquals("svg", root.tag);
    }

    @Test
    void testParseTreeUnsafeMode() throws Exception {
        // L313-L315: unsafe=true with no custom fetcher
        Node root = Node.parseTree(
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>".getBytes(StandardCharsets.UTF_8), null, null,
                true);
        assertEquals("svg", root.tag);
        assertTrue(root.unsafe);
    }

    // ---- Feature-conditional processing / <switch> (L392, L401, L408, L448, L460)
    // ----

    @Test
    void testConditionalProcessingSkipsUnsupported() throws Exception {
        // L392, L401: requiredFeatures that aren't supported
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect fill="red" x="0" y="0" width="100" height="100"/>
                  <rect requiredFeatures="http://www.w3.org/TR/SVG11/feature#Animation"
                        fill="blue" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        // Unsupported feature -> blue rect should be skipped, red remains
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void testConditionalProcessingNestedSkip() throws Exception {
        // L392: nested elements inside a skipped subtree
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect fill="red" x="0" y="0" width="100" height="100"/>
                  <g requiredFeatures="http://www.w3.org/TR/SVG11/feature#Animation">
                    <rect fill="blue" x="0" y="0" width="100" height="100"/>
                    <g>
                      <rect fill="green" x="0" y="0" width="100" height="100"/>
                    </g>
                  </g>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void testSwitchElementFirstMatch() throws Exception {
        // L419, L429-L435: <switch> only renders first matching child
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <switch>
                    <rect requiredFeatures="http://www.w3.org/TR/SVG11/feature#SVG"
                          fill="red" x="0" y="0" width="100" height="100"/>
                    <rect fill="blue" x="0" y="0" width="100" height="100"/>
                  </switch>
                </svg>
                """);
        // First child matches (SVG feature is supported), second is skipped
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ---- Foreign namespace elements (L408-L411) ----

    @Test
    void testForeignNamespaceElement() throws Exception {
        // L408-L411: non-SVG namespace -> tag is prefixed with {uri}
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg"
                     xmlns:custom="http://example.com/custom">
                  <custom:widget custom:color="red"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));
        // The foreign-namespace element should be in the tree with a {uri}name tag
        Node child = root.children.get(0);
        assertTrue(child.tag.startsWith("{http://example.com/custom}"));
    }

    // ---- Empty <style> element (L154 in CssProcessor, triggered via Node) ----

    @Test
    void testEmptyStyleElementParsing() throws Exception {
        // Empty style element shouldn't cause issues
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <style></style>
                  <rect fill="red" width="10" height="10"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));
        assertNotNull(root);
        // Should have style + rect children
        assertTrue(root.children.size() >= 1);
    }

    // ---- CSS cascade through rendering ----

    @Test
    void testCssCascadeInheritanceRendering() throws Exception {
        // CSS property inheritance through node tree
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>g { fill: red; }</style>
                  <g>
                    <rect x="0" y="0" width="100" height="100"/>
                  </g>
                </svg>
                """);
        // rect inherits fill=red from g
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void testCssCascadeInlineOverridesStylesheet() throws Exception {
        // Inline style should override stylesheet
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>rect { fill: blue; }</style>
                  <rect style="fill: red" x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ---- Node attribute access ----

    @Test
    void testNodeAttributeAccess() throws Exception {
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50">
                  <rect id="r1" x="10" y="20" width="30" height="40"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));
        Node rect = root.children.get(0);

        assertEquals("10", rect.get("x"));
        assertEquals("20", rect.get("y"));
        assertNull(rect.get("nonexistent"));
        assertEquals("default", rect.get("nonexistent", "default"));
        assertTrue(rect.has("x"));
        assertFalse(rect.has("nonexistent"));

        rect.set("fill", "red");
        assertEquals("red", rect.get("fill"));

        rect.set("opacity", 0.5);
        assertEquals("0.5", rect.get("opacity"));

        rect.remove("fill");
        assertNull(rect.get("fill"));

        assertFalse(rect.entries().isEmpty());
    }

    // ---- Node tree structure ----

    @Test
    void testNodeTreeStructure() throws Exception {
        Node root = Node.parseTree("""
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g>
                    <rect/>
                    <circle/>
                  </g>
                  <text>Hello</text>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));

        assertTrue(root.root);
        assertEquals("svg", root.tag);
        assertEquals(2, root.children.size());

        Node g = root.children.get(0);
        assertEquals("g", g.tag);
        assertEquals(root, g.parent);
        assertEquals(2, g.children.size());

        Node rect = g.children.get(0);
        assertEquals("rect", rect.tag);
        assertEquals(g, rect.parent);

        Node text = root.children.get(1);
        assertEquals("text", text.tag);
        assertEquals("Hello", text.text);
    }

    // ---- parseTree overloads ----

    @Test
    void testParseTreeTwoArgOverload() throws Exception {
        // L348: parseTree(bytes, url, unsafe)
        Node root = Node.parseTree(
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>".getBytes(StandardCharsets.UTF_8), null,
                false);
        assertEquals("svg", root.tag);
    }

    @Test
    void testParseTreeSingleArgOverload() throws Exception {
        // L352: parseTree(bytes)
        Node root = Node
                .parseTree("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>".getBytes(StandardCharsets.UTF_8));
        assertEquals("svg", root.tag);
    }

    // ---- CDATA in style elements ----

    @Test
    void testStyleWithCDATA() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style><![CDATA[
                    rect { fill: red; }
                  ]]></style>
                  <rect x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ---- Attribute inheritance from parent ----

    @Test
    void testAttributeInheritanceFromParentViaCss() throws Exception {
        // L166-L173: re-inheritance from parent during applyCss
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>
                    .parent { --custom-fill: blue; }
                    .child { fill: var(--custom-fill); }
                  </style>
                  <g class="parent">
                    <rect class="child" x="0" y="0" width="100" height="100"/>
                  </g>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    // ---- Multiple comma-separated selectors ----

    @Test
    void testCommaSeparatedSelectors() throws Exception {
        BufferedImage img = RenderTestHelper.render("""
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <style>rect, circle { fill: red; }</style>
                  <rect x="0" y="0" width="100" height="100"/>
                </svg>
                """);
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    // ---- notInheritedAttributes static accessor ----

    @Test
    void testNotInheritedAttributes() {
        var set = Node.notInheritedAttributes();
        assertNotNull(set);
        assertTrue(set.contains("id"));
        assertTrue(set.contains("transform"));
        assertTrue(set.contains("opacity"));
        assertFalse(set.contains("fill"));
    }
}

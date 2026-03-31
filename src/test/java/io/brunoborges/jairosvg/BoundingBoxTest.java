package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.brunoborges.jairosvg.dom.BoundingBox;
import io.brunoborges.jairosvg.dom.Node;

/**
 * Tests for bounding box calculation via SVG rendering with clip/viewBox
 * interactions, plus integration tests exercising BoundingBox indirectly.
 */
class BoundingBoxTest {

    private static Node findChild(Node parent, String tag) {
        for (Node c : parent.children) {
            if (tag.equals(c.tag))
                return c;
            Node found = findChild(c, tag);
            if (found != null)
                return found;
        }
        return null;
    }

    private static Node parseSvgChild(String svg, String tag) throws Exception {
        Node tree = Node.parseTree(svg.getBytes(StandardCharsets.UTF_8), null, null, true);
        Node child = findChild(tree, tag);
        assertNotNull(child, "Expected to find <" + tag + "> in SVG");
        return child;
    }

    @Test
    void rectBoundingBox() throws Exception {
        // Render a rect and verify it fills the expected area
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect x="10" y="20" width="30" height="40" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Inside the rect
        RenderTestHelper.assertPixelColor(img, 25, 40, 255, 0, 0);
        // Outside the rect (origin)
        RenderTestHelper.assertPixelRGBA(img, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void circleBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <circle cx="50" cy="50" r="20" fill="blue"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Center should be blue
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 255);
        // Far corner should be transparent
        RenderTestHelper.assertPixelRGBA(img, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void ellipseBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <ellipse cx="50" cy="50" rx="40" ry="20" fill="green"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Center should be green
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 128, 0);
        // Top-left corner should be transparent
        RenderTestHelper.assertPixelRGBA(img, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void lineBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <line x1="10" y1="10" x2="90" y2="90" stroke="red" stroke-width="4"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Somewhere along the diagonal should have red
        int pixel = img.getRGB(50, 50);
        int r = (pixel >> 16) & 0xFF;
        assertTrue(r > 200, "Diagonal center should be red, red=" + r);
    }

    @Test
    void polylineBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <polyline points="10,10 50,90 90,10" stroke="blue" stroke-width="3" fill="none"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
        assertEquals(100, img.getWidth());
        assertEquals(100, img.getHeight());
    }

    @Test
    void polygonBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <polygon points="50,10 90,90 10,90" fill="yellow"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Center of triangle should have yellow
        int pixel = img.getRGB(50, 60);
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        assertTrue(r > 200 && g > 200, "Triangle center should be yellow");
    }

    @Test
    void pathBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M 10 10 L 90 10 L 90 90 L 10 90 Z" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Center should be red
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
    }

    @Test
    void pathWithRelativeCommands() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M 10 10 l 80 0 l 0 80 l -80 0 z" fill="green"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 128, 0);
    }

    @Test
    void pathWithHorizontalAndVerticalLines() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M 10 10 H 90 V 90 H 10 Z" fill="blue"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        RenderTestHelper.assertPixelColor(img, 50, 50, 0, 0, 255);
    }

    @Test
    void pathWithCubicBezier() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M 10 50 C 10 10 90 10 90 50 C 90 90 10 90 10 50 Z" fill="red"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Center should have red content
        int pixel = img.getRGB(50, 50);
        int r = (pixel >> 16) & 0xFF;
        assertTrue(r > 200, "Bezier center should be red");
    }

    @Test
    void pathWithArc() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M 50 10 A 40 40 0 1 1 50 90 A 40 40 0 1 1 50 10 Z" fill="purple"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
        int pixel = img.getRGB(50, 50);
        int a = (pixel >> 24) & 0xFF;
        assertTrue(a > 0, "Arc circle center should be painted");
    }

    @Test
    void groupBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <g>
                    <rect x="10" y="10" width="30" height="30" fill="red"/>
                    <rect x="60" y="60" width="30" height="30" fill="blue"/>
                  </g>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Both rects should be visible
        RenderTestHelper.assertPixelColor(img, 25, 25, 255, 0, 0);
        RenderTestHelper.assertPixelColor(img, 75, 75, 0, 0, 255);
    }

    @Test
    void clipPathUsesBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <defs>
                    <clipPath id="c1"><rect x="20" y="20" width="60" height="60"/></clipPath>
                  </defs>
                  <rect width="100" height="100" fill="red" clip-path="url(#c1)"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        // Inside clip: red
        RenderTestHelper.assertPixelColor(img, 50, 50, 255, 0, 0);
        // Outside clip: transparent
        RenderTestHelper.assertPixelRGBA(img, 5, 5, 0, 0, 0, 0);
    }

    @Test
    void filterRegionUsesBoundingBox() throws Exception {
        // Blur filter should extend beyond the element's bounds
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <filter id="f1"><feGaussianBlur stdDeviation="5"/></filter>
                  </defs>
                  <rect x="50" y="50" width="100" height="100" fill="red" filter="url(#f1)"/>
                </svg>
                """;
        BufferedImage img = RenderTestHelper.render(svg);
        assertNotNull(img);
        // Center should still be red-ish
        int pixel = img.getRGB(100, 100);
        int r = (pixel >> 16) & 0xFF;
        assertTrue(r > 200, "Filter center should still be predominantly red");
    }

    // ── Direct BoundingBox.calculate tests ──────────────────────────────────

    @Test
    void testPathWithSmoothCubicBezier() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 60 C20 10 40 10 50 60 S80 110 110 60" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Smooth cubic path should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX should include start point");
        assertTrue(box.minX() + box.width() >= 110, "maxX should include end point");
    }

    @Test
    void testPathWithQuadraticBezier() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 60 Q60 10 110 60" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Quadratic bezier path should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX should include start");
        assertTrue(box.minX() + box.width() >= 110, "maxX should include end");
        assertTrue(box.minY() <= 10, "minY should include control point");
    }

    @Test
    void testPathWithSmoothQuadratic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 30 Q30 5 60 30 T110 30" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Smooth quadratic path should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX should include start");
        assertTrue(box.minX() + box.width() >= 110, "maxX should include T endpoint");
    }

    @Test
    void testPathWithRelativeArc() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 10 a20 20 0 0 1 30 30" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Relative arc path should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX should include start");
        assertTrue(box.minX() + box.width() >= 40, "maxX should include arc endpoint (10+30)");
        assertTrue(box.minY() + box.height() >= 40, "maxY should include arc endpoint (10+30)");
    }

    @Test
    void testPathWithUnknownCommand() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 10 L50 50 X20 20" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        // Should not throw — unknown command X is skipped
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Path with unknown command should still produce a box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid from M and L");
        assertTrue(box.minX() <= 10, "minX should include M start");
        assertTrue(box.minX() + box.width() >= 50, "maxX should include L endpoint");
    }

    @Test
    void testPathWithInvalidNumbers() throws Exception {
        // Use H command which parses a single number via Double.parseDouble,
        // triggering NumberFormatException caught by the path parser
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 10 L50 50 Habc" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        // NumberFormatException is caught, partial result returned
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Path with invalid numbers should still return a box");
        assertTrue(BoundingBox.isValid(box), "Partial box from M and L should be valid");
        assertEquals(10, box.minX(), 1.0, "minX from M command");
        assertEquals(10, box.minY(), 1.0, "minY from M command");
    }

    @Test
    void testNestedGroupBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <g><g><rect x="10" y="20" width="30" height="40"/></g></g>
                </svg>
                """;
        // Get the outermost <g>
        Node tree = Node.parseTree(svg.getBytes(StandardCharsets.UTF_8), null, null, true);
        Node outerG = findChild(tree, "g");
        assertNotNull(outerG);
        BoundingBox.Box box = BoundingBox.calculate(null, outerG);
        assertNotNull(box, "Nested group should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertEquals(10, box.minX(), 1.0);
        assertEquals(20, box.minY(), 1.0);
        assertEquals(30, box.width(), 1.0);
        assertEquals(40, box.height(), 1.0);
    }

    @Test
    void testGroupWithTransform() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <g transform="translate(50,50)"><rect width="10" height="10"/></g>
                </svg>
                """;
        Node gNode = parseSvgChild(svg, "g");
        // BoundingBox.group doesn't account for transforms, but should not crash
        BoundingBox.Box box = BoundingBox.calculate(null, gNode);
        assertNotNull(box, "Group with transform should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
    }

    @Test
    void testEmptyGroupBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <g></g>
                </svg>
                """;
        Node gNode = parseSvgChild(svg, "g");
        BoundingBox.Box box = BoundingBox.calculate(null, gNode);
        assertNotNull(box, "Empty group should return EMPTY box");
        // EMPTY box has POSITIVE_INFINITY, so isValid returns false
        assertTrue(!BoundingBox.isValid(box), "Empty group box should not be valid");
    }

    @Test
    void testUseBoundingBox() throws Exception {
        // <use> is not handled in the calculate switch — returns null
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs><rect id="r1" width="30" height="30"/></defs>
                  <use href="#r1" x="10" y="10"/>
                </svg>
                """;
        Node useNode = parseSvgChild(svg, "use");
        BoundingBox.Box box = BoundingBox.calculate(null, useNode);
        // <use> is not in the switch — returns null
        assertNull(box, "use element should return null from calculate");
    }

    @Test
    void testTextBoundingBox() throws Exception {
        // <text> is not handled in the calculate switch — returns null
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <text x="10" y="30" font-size="14">Hello</text>
                </svg>
                """;
        Node textNode = parseSvgChild(svg, "text");
        BoundingBox.Box box = BoundingBox.calculate(null, textNode);
        assertNull(box, "text element should return null from calculate");
    }

    @Test
    void testUnsupportedTagReturnsNull() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <foreignObject x="10" y="10" width="50" height="50"/>
                </svg>
                """;
        Node foNode = parseSvgChild(svg, "foreignObject");
        BoundingBox.Box box = BoundingBox.calculate(null, foNode);
        assertNull(box, "Unsupported tag should return null");
    }

    @Test
    void testGroupWithMixedValidAndInvalidChildren() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <g>
                    <rect x="10" y="10" width="20" height="20"/>
                    <foreignObject x="50" y="50" width="30" height="30"/>
                    <circle cx="80" cy="80" r="10"/>
                  </g>
                </svg>
                """;
        Node gNode = parseSvgChild(svg, "g");
        BoundingBox.Box box = BoundingBox.calculate(null, gNode);
        assertNotNull(box, "Group with mixed children should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        // Rect: 10,10 -> 30,30; Circle: 70,70 -> 90,90; foreignObject ignored
        assertTrue(box.minX() <= 10, "minX should include rect");
        assertTrue(box.minX() + box.width() >= 90, "maxX should include circle");
        assertTrue(box.minY() <= 10, "minY should include rect");
        assertTrue(box.minY() + box.height() >= 90, "maxY should include circle");
    }

    @Test
    void testPathWithMultipleSubpaths() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 10 L40 10 L40 40 Z M60 60 L90 60 L90 90 Z" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Path with multiple subpaths should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX should include first subpath");
        assertTrue(box.minX() + box.width() >= 90, "maxX should include second subpath");
        assertTrue(box.minY() <= 10, "minY should include first subpath");
        assertTrue(box.minY() + box.height() >= 90, "maxY should include second subpath");
    }

    @Test
    void testPathWithOnlyMoveTo() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M50 50" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Path with only M should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertEquals(50, box.minX(), 1.0, "minX should be M x");
        assertEquals(50, box.minY(), 1.0, "minY should be M y");
        assertEquals(0, box.width(), 1.0, "Point-size box should have zero width");
        assertEquals(0, box.height(), 1.0, "Point-size box should have zero height");
    }

    @Test
    void testPathWithRelativeSmoothCubic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 60 c10 -50 30 -50 40 0 s30 50 40 0" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Relative smooth cubic path should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX should include start");
        // End point: 10 + 40 + 40 = 90
        assertTrue(box.minX() + box.width() >= 90, "maxX should include final endpoint");
    }

    @Test
    void testPathWithRelativeSmoothQuadratic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 30 q20 -25 50 0 t50 0" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box, "Relative smooth quadratic path should produce a bounding box");
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX should include start");
        // End point: 10 + 50 + 50 = 110
        assertTrue(box.minX() + box.width() >= 110, "maxX should include final endpoint");
    }

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testEmptyPolylinePoints() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <polyline points="" stroke="black"/>
                </svg>
                """;
        Node node = parseSvgChild(svg, "polyline");
        BoundingBox.Box box = BoundingBox.calculate(null, node);
        // Empty points should produce an empty/invalid bounding box
        assertNotNull(box);
    }

    @Test
    void testPathWithRelativeHorizontalLine() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 50 h80" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX should include M start");
        assertTrue(box.minX() + box.width() >= 90, "maxX should include h endpoint");
    }

    @Test
    void testPathWithRelativeVerticalLine() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M50 10 v80" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minY() <= 10, "minY should include M start");
        assertTrue(box.minY() + box.height() >= 90, "maxY should include v endpoint");
    }

    @Test
    void testPathWithRelativeArcLowercase() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 50 a30 30 0 0 1 60 0" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() + box.width() >= 70, "maxX should include arc endpoint (10+60)");
    }

    @Test
    void testPathWithRelativeMoveTo() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="m10 10 l80 0 l0 80 l-80 0 z" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box), "Box should be valid");
        assertTrue(box.minX() <= 10, "minX from relative moveto");
        assertTrue(box.minX() + box.width() >= 90, "maxX from relative lineto");
    }

    @Test
    void testZeroLengthLineBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <line x1="50" y1="50" x2="50" y2="50" stroke="black"/>
                </svg>
                """;
        Node node = parseSvgChild(svg, "line");
        BoundingBox.Box box = BoundingBox.calculate(null, node);
        assertNotNull(box);
        // Zero-length line: width and height are 0
        assertEquals(0, box.width(), 0.01);
        assertEquals(0, box.height(), 0.01);
    }

    @Test
    void testGroupWithOnlyUnsupportedChildren() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <g>
                    <text x="10" y="10">hello</text>
                    <foreignObject x="50" y="50" width="30" height="30"/>
                  </g>
                </svg>
                """;
        Node gNode = parseSvgChild(svg, "g");
        BoundingBox.Box box = BoundingBox.calculate(null, gNode);
        // Both children return null boxes, group combine produces EMPTY
        assertNotNull(box);
        assertTrue(!BoundingBox.isValid(box), "Group with only unsupported children should be invalid");
    }

    @Test
    void testPathWithAbsoluteHorizontalAndVertical() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 10 H90 V90" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box));
        assertTrue(box.minX() <= 10);
        assertTrue(box.minX() + box.width() >= 90);
        assertTrue(box.minY() + box.height() >= 90);
    }

    @Test
    void testPathWithRelativeLines() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M10 10 l30 0 l0 30 l-30 0 z" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box));
        assertEquals(10, box.minX(), 1.0);
        assertEquals(10, box.minY(), 1.0);
        assertEquals(30, box.width(), 1.0);
        assertEquals(30, box.height(), 1.0);
    }

    @Test
    void testPathWithAbsoluteArc() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <path d="M50 10 A40 40 0 1 1 50 90" fill="none" stroke="black"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box));
        assertTrue(box.minY() <= 10, "minY from start");
        assertTrue(box.minY() + box.height() >= 90, "maxY from endpoint");
    }

    // ── ellipse bounding box ─────────────────────────────────────────────

    @Test
    void testEllipseBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <ellipse cx="100" cy="80" rx="50" ry="30"/>
                </svg>
                """;
        Node ellipseNode = parseSvgChild(svg, "ellipse");
        BoundingBox.Box box = BoundingBox.calculate(null, ellipseNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box));
        assertEquals(50, box.minX(), 1.0);
        assertEquals(50, box.minY(), 1.0);
        assertEquals(100, box.width(), 1.0);
        assertEquals(60, box.height(), 1.0);
    }

    // ── polyline bounding box ────────────────────────────────────────────

    @Test
    void testPolylineBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <polyline points="10,20 50,60 30,80 90,10"/>
                </svg>
                """;
        Node polylineNode = parseSvgChild(svg, "polyline");
        BoundingBox.Box box = BoundingBox.calculate(null, polylineNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box));
        assertEquals(10, box.minX(), 1.0);
        assertEquals(10, box.minY(), 1.0);
        assertEquals(80, box.width(), 1.0);
        assertEquals(70, box.height(), 1.0);
    }

    // ── isNonEmpty with zero dimensions ──────────────────────────────────

    @Test
    void testIsNonEmptyZeroWidth() {
        BoundingBox.Box box = new BoundingBox.Box(10, 10, 0, 50);
        assertFalse(BoundingBox.isNonEmpty(box));
    }

    @Test
    void testIsNonEmptyZeroHeight() {
        BoundingBox.Box box = new BoundingBox.Box(10, 10, 50, 0);
        assertFalse(BoundingBox.isNonEmpty(box));
    }

    @Test
    void testIsNonEmptyValid() {
        BoundingBox.Box box = new BoundingBox.Box(10, 10, 50, 50);
        assertTrue(BoundingBox.isNonEmpty(box));
    }

    // ── path with malformed data ─────────────────────────────────────────

    @Test
    void testPathBoundingBoxMalformedData() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d="M10,10 L50,50 Lxyz"/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        // Malformed data may throw or return partial result
        try {
            BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
            assertNotNull(box);
        } catch (Exception e) {
            // Expected for truly malformed data
        }
    }

    // ── path with empty d attribute ──────────────────────────────────────

    @Test
    void testPathBoundingBoxEmptyD() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <path d=""/>
                </svg>
                """;
        Node pathNode = parseSvgChild(svg, "path");
        BoundingBox.Box box = BoundingBox.calculate(null, pathNode);
        assertNotNull(box);
    }

    // ── circle bounding box ──────────────────────────────────────────────

    @Test
    void testCircleBoundingBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <circle cx="100" cy="100" r="40"/>
                </svg>
                """;
        Node circleNode = parseSvgChild(svg, "circle");
        BoundingBox.Box box = BoundingBox.calculate(null, circleNode);
        assertNotNull(box);
        assertTrue(BoundingBox.isValid(box));
        assertEquals(60, box.minX(), 1.0);
        assertEquals(60, box.minY(), 1.0);
        assertEquals(80, box.width(), 1.0);
        assertEquals(80, box.height(), 1.0);
    }
}

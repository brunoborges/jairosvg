package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * Tests for bounding box calculation via SVG rendering with clip/viewBox
 * interactions, plus integration tests exercising BoundingBox indirectly.
 */
class BoundingBoxTest {

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
}

package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class NestedSvgTest {

    private BufferedImage render(String svg) throws Exception {
        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    private int[] rgba(BufferedImage img, int x, int y) {
        int p = img.getRGB(x, y);
        return new int[]{(p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF, (p >> 24) & 0xFF};
    }

    @Test
    void testBasicNestedSvg() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="lightblue"/>
                  <svg x="50" y="50" width="100" height="100">
                    <rect width="100" height="100" fill="red"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertEquals(200, img.getWidth());
        assertEquals(200, img.getHeight());
        // Center of nested SVG (50+50, 50+50) = (100,100) should be red
        int[] center = rgba(img, 100, 100);
        assertEquals(255, center[0], "red channel");
        assertEquals(0, center[1], "green channel");
        assertEquals(0, center[2], "blue channel");
        // Outer area (10,10) should be lightblue
        int[] outer = rgba(img, 10, 10);
        assertEquals(173, outer[0], "outer red (lightblue)");
        assertEquals(216, outer[1], "outer green (lightblue)");
        assertEquals(230, outer[2], "outer blue (lightblue)");
    }

    @Test
    void testNestedSvgWithViewBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="white"/>
                  <svg x="0" y="0" width="100" height="100" viewBox="0 0 50 50">
                    <circle cx="25" cy="25" r="25" fill="blue"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Center of nested SVG (50,50) should be blue
        int[] center = rgba(img, 50, 50);
        assertTrue(center[2] > 200, "center should be blue");
        // Outside nested SVG (150,150) should be white
        int[] outside = rgba(img, 150, 150);
        assertEquals(255, outside[0], "outside should be white");
        assertEquals(255, outside[1], "outside should be white");
        assertEquals(255, outside[2], "outside should be white");
    }

    @Test
    void testDeeplyNestedSvg() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="white"/>
                  <svg x="20" y="20" width="160" height="160">
                    <rect width="160" height="160" fill="lightgray"/>
                    <svg x="20" y="20" width="120" height="120">
                      <rect width="120" height="120" fill="green"/>
                    </svg>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Center (100,100) should be deep-nested green
        int[] center = rgba(img, 100, 100);
        assertEquals(0, center[0], "red should be 0 for green");
        assertEquals(128, center[1], "green channel");
        assertEquals(0, center[2], "blue should be 0 for green");
    }

    @Test
    void testNestedSvgWithPercentageDimensions() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="white"/>
                  <svg x="0" y="0" width="50%" height="50%">
                    <rect width="100%" height="100%" fill="red"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // 50% of 200 = 100, so (50,50) should be red
        int[] inner = rgba(img, 50, 50);
        assertEquals(255, inner[0], "inside nested SVG should be red");
        assertEquals(0, inner[1]);
        assertEquals(0, inner[2]);
        // (150,150) is outside nested SVG, should be white
        int[] outer = rgba(img, 150, 150);
        assertEquals(255, outer[0], "outside nested SVG should be white");
        assertEquals(255, outer[1]);
        assertEquals(255, outer[2]);
    }

    @Test
    void testNestedSvgClipsOverflow() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="white"/>
                  <svg x="50" y="50" width="50" height="50">
                    <rect x="-10" y="-10" width="80" height="80" fill="blue"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // (45,45) is outside nested SVG viewport - should be white (clipped)
        int[] outside = rgba(img, 45, 45);
        assertEquals(255, outside[0], "overflow should be clipped (white)");
        assertEquals(255, outside[1]);
        assertEquals(255, outside[2]);
        // (75,75) is inside nested SVG - should be blue
        int[] inside = rgba(img, 75, 75);
        assertTrue(inside[2] > 200, "inside nested SVG should be blue");
    }

    @Test
    void testNestedSvgOverflowVisible() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="white"/>
                  <svg x="50" y="50" width="50" height="50" overflow="visible">
                    <rect x="-10" y="-10" width="80" height="80" fill="blue"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // (45,45) is outside nested SVG viewport but overflow=visible - should be blue
        int[] overflow = rgba(img, 45, 45);
        assertTrue(overflow[2] > 200, "overflow=visible should show outside content as blue");
    }

    @Test
    void testNestedSvgWithOpacity() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <svg x="0" y="0" width="100" height="100" opacity="0.5">
                    <rect width="100" height="100" fill="red"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Center should be semi-transparent red blended with white: R=255, G≈127, B≈127
        int[] center = rgba(img, 50, 50);
        assertEquals(255, center[0], "red channel should be 255");
        assertTrue(center[1] > 115 && center[1] < 140, "green channel should be ~127 for 50% opacity red over white");
        assertTrue(center[2] > 115 && center[2] < 140, "blue channel should be ~127 for 50% opacity red over white");
    }

    @Test
    void testGroupOpacity() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <g opacity="0.5">
                    <rect width="100" height="100" fill="red"/>
                  </g>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Center should be semi-transparent red blended with white: R=255, G≈127, B≈127
        int[] center = rgba(img, 50, 50);
        assertEquals(255, center[0], "red channel should be 255");
        assertTrue(center[1] > 115 && center[1] < 140, "green channel should be ~127 for 50% opacity");
        assertTrue(center[2] > 115 && center[2] < 140, "blue channel should be ~127 for 50% opacity");
    }

    @Test
    void testMultipleNestedSvgsSideBySide() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <svg x="0" y="0" width="100" height="100">
                    <rect width="100" height="100" fill="red"/>
                  </svg>
                  <svg x="100" y="0" width="100" height="100">
                    <rect width="100" height="100" fill="blue"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] left = rgba(img, 50, 50);
        assertEquals(255, left[0], "left side should be red");
        assertEquals(0, left[1]);
        assertEquals(0, left[2]);
        int[] right = rgba(img, 150, 50);
        assertEquals(0, right[0], "right side should be blue");
        assertEquals(0, right[1]);
        assertTrue(right[2] > 200);
    }

    @Test
    void testNestedSvgNoWidthHeightOnlyViewBox() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="white"/>
                  <svg x="10" y="10" viewBox="0 0 100 100">
                    <rect width="100" height="100" fill="purple"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] inner = rgba(img, 50, 50);
        assertTrue(inner[0] > 100, "purple has red component");
        assertEquals(0, inner[1], "purple has no green");
        assertTrue(inner[2] > 100, "purple has blue component");
    }

    @Test
    void testNestedSvgUsesOuterGradient() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="0%">
                      <stop offset="0%" stop-color="red"/>
                      <stop offset="100%" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <svg x="0" y="0" width="200" height="200">
                    <rect width="200" height="200" fill="url(#grad)"/>
                  </svg>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Left should be red-ish
        int[] left = rgba(img, 10, 100);
        assertTrue(left[0] > 150, "left should be red-ish");
        assertTrue(left[2] < 100, "left should have low blue");
        // Right should be blue-ish
        int[] right = rgba(img, 190, 100);
        assertTrue(right[2] > 150, "right should be blue-ish");
        assertTrue(right[0] < 100, "right should have low red");
    }
}

package io.brunoborges.jairosvg;

import static io.brunoborges.jairosvg.RenderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class PathDrawerTest {

    @Test
    void testSmoothCubicBezier() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
                  <rect width="120" height="120" fill="white"/>
                  <path d="M10 60 C20 10 40 10 50 60 S80 110 110 60" fill="none" stroke="red" stroke-width="3"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertEquals(120, img.getWidth());
        assertEquals(120, img.getHeight());

        // Check that red pixels exist along the curve
        boolean foundRed = false;
        for (int x = 10; x < 110; x++) {
            for (int y = 0; y < 120; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red pixels along the S-curve");
    }

    @Test
    void testQuadraticBezier() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">
                  <rect width="120" height="120" fill="white"/>
                  <path d="M10 60 Q60 10 110 60" fill="none" stroke="blue" stroke-width="3"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // The midpoint of the curve should be near x=60, elevated above y=60
        boolean foundBlue = false;
        for (int y = 20; y < 60; y++) {
            int[] c = rgba(img, 60, y);
            if (c[2] > 200 && c[0] < 50 && c[1] < 50) {
                foundBlue = true;
                break;
            }
        }
        assertTrue(foundBlue, "Expected blue pixels near midpoint of quadratic bezier");
    }

    @Test
    void testSmoothQuadraticBezier() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="120" height="60">
                  <rect width="120" height="60" fill="white"/>
                  <path d="M10 30 Q30 5 60 30 T110 30" fill="none" stroke="green" stroke-width="3"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // green = #008000 = rgb(0,128,0)
        boolean foundGreen = false;
        for (int x = 10; x < 110; x++) {
            for (int y = 0; y < 60; y++) {
                int[] c = rgba(img, x, y);
                if (c[1] > 100 && c[0] < 30 && c[2] < 30) {
                    foundGreen = true;
                    break;
                }
            }
            if (foundGreen)
                break;
        }
        assertTrue(foundGreen, "Expected green (0,128,0) pixels along the T-curve");
    }

    @Test
    void testRelativeQuadraticCommands() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 50 q20 -40 40 0 t40 0" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        boolean foundRed = false;
        for (int x = 10; x < 90; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red pixels from relative q/t commands");
    }

    @Test
    void testArcWithZeroRadius() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 10 A0 20 0 0 1 80 80" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);

        // Should render as a line when one radius is zero
        boolean foundRed = false;
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected some red pixels from degenerate arc");
    }

    @Test
    void testArcFlagCombinations() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <rect width="200" height="200" fill="white"/>
                  <path d="M60 100 A50 50 0 0 0 140 100" fill="none" stroke="red" stroke-width="3"/>
                  <path d="M60 100 A50 50 0 0 1 140 100" fill="none" stroke="blue" stroke-width="3"/>
                  <path d="M60 100 A50 50 0 1 0 140 100" fill="none" stroke="#008000" stroke-width="3"/>
                  <path d="M60 100 A50 50 0 1 1 140 100" fill="none" stroke="black" stroke-width="3"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        boolean hasRed = false, hasBlue = false, hasGreen = false, hasBlack = false;
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 200; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 180 && c[1] < 80 && c[2] < 80)
                    hasRed = true;
                if (c[2] > 180 && c[0] < 80 && c[1] < 80)
                    hasBlue = true;
                if (c[1] > 80 && c[0] < 30 && c[2] < 30)
                    hasGreen = true;
                if (c[0] < 40 && c[1] < 40 && c[2] < 40 && c[3] > 200)
                    hasBlack = true;
            }
        }
        assertTrue(hasRed, "Expected red arc (0 0)");
        assertTrue(hasBlue, "Expected blue arc (0 1)");
        assertTrue(hasGreen, "Expected green arc (1 0)");
        assertTrue(hasBlack, "Expected black arc (1 1)");
    }

    @Test
    void testEmptyPath() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <path d="" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error with empty path d");
        assertEquals(50, img.getWidth());
    }

    @Test
    void testPathWithOnlyMoveto() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <path d="M10 10" fill="blue"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error with moveto-only path");
        assertEquals(50, img.getWidth());
    }

    @Test
    void testPathWithCloseOnly() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <path d="M10 10 Z" fill="green"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error with moveto+close path");
        assertEquals(50, img.getWidth());
    }

    @Test
    void testImplicitLinetoAfterMoveto() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 10 50 50 90 10 Z" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Center of the triangle (~50,30) should be red
        int[] center = rgba(img, 50, 30);
        assertTrue(center[0] > 200 && center[1] < 50 && center[2] < 50,
                "Expected red fill in the triangle area, got rgb(%d,%d,%d)".formatted(center[0], center[1], center[2]));
    }

    @Test
    void testMultipleSubpaths() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 10 L40 10 L40 40 Z M60 60 L90 60 L90 90 Z" fill="blue"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Top-left triangle area should have blue
        boolean topLeftBlue = false;
        for (int x = 10; x < 40; x++) {
            for (int y = 10; y < 40; y++) {
                int[] c = rgba(img, x, y);
                if (c[2] > 200 && c[0] < 50 && c[1] < 50) {
                    topLeftBlue = true;
                    break;
                }
            }
            if (topLeftBlue)
                break;
        }
        assertTrue(topLeftBlue, "Expected blue pixels in top-left subpath");

        // Bottom-right triangle area should have blue
        boolean bottomRightBlue = false;
        for (int x = 60; x < 90; x++) {
            for (int y = 60; y < 90; y++) {
                int[] c = rgba(img, x, y);
                if (c[2] > 200 && c[0] < 50 && c[1] < 50) {
                    bottomRightBlue = true;
                    break;
                }
            }
            if (bottomRightBlue)
                break;
        }
        assertTrue(bottomRightBlue, "Expected blue pixels in bottom-right subpath");

        // Center (50,50) should be white (between the two subpaths)
        assertPixelColor(img, 50, 50, 255, 255, 255);
    }

    @Test
    void testHorizontalAndVerticalAbsolute() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 10 H90 V90 H10 Z" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Red pixels along top edge
        boolean topRed = false;
        for (int x = 20; x < 80; x++) {
            int[] c = rgba(img, x, 10);
            if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                topRed = true;
                break;
            }
        }
        assertTrue(topRed, "Expected red pixels along top edge (H command)");

        // Red pixels along right edge
        boolean rightRed = false;
        for (int y = 20; y < 80; y++) {
            int[] c = rgba(img, 90, y);
            if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                rightRed = true;
                break;
            }
        }
        assertTrue(rightRed, "Expected red pixels along right edge (V command)");
    }

    @Test
    void testHorizontalAndVerticalRelative() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 10 h80 v80 h-80 Z" fill="none" stroke="blue" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        boolean hasBlue = false;
        for (int x = 20; x < 80; x++) {
            int[] c = rgba(img, x, 10);
            if (c[2] > 200 && c[0] < 50 && c[1] < 50) {
                hasBlue = true;
                break;
            }
        }
        assertTrue(hasBlue, "Expected blue pixels along edges from h/v commands");
    }

    @Test
    void testSmoothCubicWithoutPriorCubic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 50 S50 10 90 50" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        boolean foundRed = false;
        for (int x = 10; x < 90; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red pixels from S command without prior cubic");
    }

    @Test
    void testSmoothQuadraticWithoutPriorQuadratic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 50 T90 50" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Should render without error (T without prior Q degenerates to line)");
    }

    @Test
    void testPathWithScientificNotation() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M1e1 1e1 L9e1 9e1" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Line from (10,10) to (90,90) — red along diagonal
        boolean foundRed = false;
        for (int i = 20; i < 80; i++) {
            for (int dy = -3; dy <= 3; dy++) {
                int y = i + dy;
                if (y >= 0 && y < 100) {
                    int[] c = rgba(img, i, y);
                    if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                        foundRed = true;
                        break;
                    }
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red pixels along diagonal from scientific notation coords");
    }

    @Test
    void testPathWithImplicitDecimalSeparators() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M.5 .5 L99.5 99.5" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        boolean foundRed = false;
        for (int i = 10; i < 90; i++) {
            for (int dy = -3; dy <= 3; dy++) {
                int y = i + dy;
                if (y >= 0 && y < 100) {
                    int[] c = rgba(img, i, y);
                    if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                        foundRed = true;
                        break;
                    }
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red pixels along diagonal from implicit decimal coords");
    }

    @Test
    void testPathWithSignSeparatedNumbers() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 50L90 50" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Horizontal red line at y=50
        boolean foundRed = false;
        for (int x = 20; x < 80; x++) {
            int[] c = rgba(img, x, 50);
            if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                foundRed = true;
                break;
            }
        }
        assertTrue(foundRed, "Expected horizontal red line");
    }

    @Test
    void testArcWithLargeRadiiScaling() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 50 A5 5 0 0 1 90 50" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        // Radii are too small for the distance — should be scaled up
        boolean foundRed = false;
        for (int x = 10; x < 90; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red arc pixels (radii scaled up)");
    }

    @Test
    void testPathWithRepeatedCommand() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 10 L30 10 50 30 70 10 90 10" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);

        boolean foundRed = false;
        for (int x = 10; x < 90; x++) {
            for (int y = 0; y < 40; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red zigzag line from repeated L coordinates");
    }

    // ── Additional branch coverage tests ────────────────────────────────

    @Test
    void testRelativeMoveTo() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="m10 10 l80 0 l0 80 l-80 0 z" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        // Center should be red
        int[] center = rgba(img, 50, 50);
        assertTrue(center[0] > 200, "Expected red fill from relative m command");
    }

    @Test
    void testRelativeArc() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 50 a40 40 0 0 1 80 0" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        boolean foundRed = false;
        for (int x = 10; x < 90; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Expected red arc from relative a command");
    }

    @Test
    void testInvalidArcFlags() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 50 A20 20 0 2 0 90 50" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Invalid arc flags should not crash");
    }

    @Test
    void testPathWithNegativeNumbers() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M50 50L50-10L-10 50Z" fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Negative number parsing should work");
    }

    @Test
    void testPathWithCommaDelimitedCoords() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10,10 L90,10 L90,90 L10,90 Z" fill="blue"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] center = rgba(img, 50, 50);
        assertTrue(center[2] > 200, "Expected blue fill from comma-separated coords");
    }

    @Test
    void testPathWithExponentNotationNegative() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M1e+1 5e1 L9e1 5e1" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        boolean foundRed = false;
        for (int x = 15; x < 85; x++) {
            int[] c = rgba(img, x, 50);
            if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                foundRed = true;
                break;
            }
        }
        assertTrue(foundRed, "Expected red from exponent notation with +");
    }

    @Test
    void testPathMultipleSubpathsWithMarkers() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <marker id="dot" viewBox="0 0 10 10" refX="5" refY="5"
                            markerWidth="5" markerHeight="5">
                      <circle cx="5" cy="5" r="5" fill="red"/>
                    </marker>
                  </defs>
                  <rect width="200" height="200" fill="white"/>
                  <path d="M10 10 L50 10 M100 100 L150 100" fill="none" stroke="black"
                        marker-start="url(#dot)" marker-end="url(#dot)"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img);
    }

    @Test
    void testPathWithSmoothCubicAfterCubic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <path d="M10 50 C20 10 40 10 50 50 S80 90 90 50" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        boolean foundRed = false;
        for (int x = 10; x < 90; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "S command after C should render smooth continuation");
    }

    @Test
    void testPathWithSmoothQuadAfterQuad() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <path d="M10 50 Q30 10 50 50 T90 50" fill="none" stroke="blue" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        boolean foundBlue = false;
        for (int x = 10; x < 90; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[2] > 200 && c[0] < 50 && c[1] < 50) {
                    foundBlue = true;
                    break;
                }
            }
            if (foundBlue)
                break;
        }
        assertTrue(foundBlue, "T command after Q should render smooth continuation");
    }

    @Test
    void testRelativeSmoothCubic() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
                  <rect width="200" height="100" fill="white"/>
                  <path d="M10 50 c10 -40 30 -40 40 0 s30 40 40 0" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        boolean foundRed = false;
        for (int x = 10; x < 90; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Relative s command should work");
    }

    @Test
    void testPathArcBothRadiiZero() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="M10 10 A0 0 0 0 1 80 80" fill="none" stroke="red" stroke-width="2"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        assertNotNull(img, "Both radii zero should produce a line");
        boolean foundRed = false;
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                int[] c = rgba(img, x, y);
                if (c[0] > 200 && c[1] < 50 && c[2] < 50) {
                    foundRed = true;
                    break;
                }
            }
            if (foundRed)
                break;
        }
        assertTrue(foundRed, "Zero-radii arc should produce straight line");
    }

    @Test
    void testPathWithWhitespaceAndNewlines() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="white"/>
                  <path d="  M 10  10
                             L 90  10
                             L 90  90  Z  " fill="red"/>
                </svg>
                """;
        BufferedImage img = render(svg);
        int[] c = rgba(img, 50, 30);
        assertTrue(c[0] > 200, "Path with extra whitespace should render correctly");
    }
}

package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PatternTest {

    @Test
    void testSvgWithPatternFill() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <pattern id="pat1" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
                      <rect width="20" height="20" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="200" height="200" fill="url(#pat1)"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(200, image.getWidth());
        // Center pixel should be filled with the pattern (red)
        int pixel = image.getRGB(100, 100);
        int red = (pixel >> 16) & 0xFF;
        assertTrue(red > 200, "Pattern fill should produce red pixels");
    }

    @Test
    void testSvgWithPatternTransformScale() throws Exception {
        // Pattern: 20×20 tile, left half blue, right half transparent.
        // Without scale, blue repeats every 20px; with scale(2), the tile doubles to
        // 40px.
        // So at x=15 the unscaled pattern would show blue (within first 10px tile
        // repeat),
        // but at x=25 in a scaled pattern the blue half extends to 20px wide.
        String svgNoScale = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <pattern id="patNoScale" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
                      <rect width="10" height="20" fill="blue"/>
                    </pattern>
                  </defs>
                  <rect width="200" height="200" fill="url(#patNoScale)"/>
                </svg>
                """;
        String svgScaled = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <pattern id="patScaled" x="0" y="0" width="20" height="20"
                             patternUnits="userSpaceOnUse"
                             patternTransform="scale(2)">
                      <rect width="10" height="20" fill="blue"/>
                    </pattern>
                  </defs>
                  <rect width="200" height="200" fill="url(#patScaled)"/>
                </svg>
                """;

        BufferedImage imgNoScale = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(svgNoScale.getBytes(StandardCharsets.UTF_8))));
        BufferedImage imgScaled = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(svgScaled.getBytes(StandardCharsets.UTF_8))));

        // Both images should have visible content
        assertEquals(200, imgNoScale.getWidth());
        assertEquals(200, imgScaled.getWidth());

        // At x=5, y=5: both should be blue (within the blue half of the tile)
        int noScaleBlue = imgNoScale.getRGB(5, 5) & 0xFF;
        int scaledBlue = imgScaled.getRGB(5, 5) & 0xFF;
        assertTrue(noScaleBlue > 200, "Unscaled pattern should be blue at (5,5)");
        assertTrue(scaledBlue > 200, "Scaled pattern should be blue at (5,5)");

        // At x=15, y=5: unscaled is in the transparent half (x=15 % 20 = 15 > 10),
        // but scaled tile stretches to 40px, so x=15 is still in the blue half.
        int noScaleAt15 = imgNoScale.getRGB(15, 5) & 0xFF;
        int scaledAt15 = imgScaled.getRGB(15, 5) & 0xFF;
        assertTrue(noScaleAt15 < 50, "Unscaled pattern should NOT be blue at (15,5)");
        assertTrue(scaledAt15 > 200, "Scaled pattern should still be blue at (15,5)");
    }

    @Test
    void testSvgWithPatternTransformRotate() throws Exception {
        // Pattern: 20×20 tile, left half green, right half transparent.
        // Without rotation, pixel at (15,10) is in the transparent half.
        // With rotate(90), the pattern is rotated so the green/transparent boundary
        // changes orientation, altering which pixels are filled.
        String svgNoRotate = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <pattern id="patNoRot" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
                      <rect width="10" height="20" fill="green"/>
                    </pattern>
                  </defs>
                  <rect width="200" height="200" fill="url(#patNoRot)"/>
                </svg>
                """;
        String svgRotated = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <pattern id="patRot" x="0" y="0" width="20" height="20"
                             patternUnits="userSpaceOnUse"
                             patternTransform="rotate(90)">
                      <rect width="10" height="20" fill="green"/>
                    </pattern>
                  </defs>
                  <rect width="200" height="200" fill="url(#patRot)"/>
                </svg>
                """;

        BufferedImage imgNoRot = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(svgNoRotate.getBytes(StandardCharsets.UTF_8))));
        BufferedImage imgRotated = ImageIO
                .read(new ByteArrayInputStream(JairoSVG.svg2png(svgRotated.getBytes(StandardCharsets.UTF_8))));

        assertEquals(200, imgNoRot.getWidth());
        assertEquals(200, imgRotated.getWidth());

        // The two renderings should differ — rotation changes the pattern layout
        // Compare a sampling of pixels; at least some must differ
        int differences = 0;
        for (int y = 0; y < 200; y += 10) {
            for (int x = 0; x < 200; x += 10) {
                if (imgNoRot.getRGB(x, y) != imgRotated.getRGB(x, y)) {
                    differences++;
                }
            }
        }
        assertTrue(differences > 0, "Rotated pattern should differ from unrotated pattern");
    }

    @Test
    void testSvgWithPatternTransformTranslate() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                  <defs>
                    <pattern id="pat4" x="0" y="0" width="20" height="20"
                             patternUnits="userSpaceOnUse"
                             patternTransform="translate(5, 5)">
                      <rect width="20" height="20" fill="red"/>
                    </pattern>
                  </defs>
                  <rect width="200" height="200" fill="url(#pat4)"/>
                </svg>
                """;

        byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        assertNotNull(png);
        assertTrue(png.length > 0);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // Center should be filled with the pattern (red)
        int pixel = image.getRGB(100, 100);
        int red = (pixel >> 16) & 0xFF;
        assertTrue(red > 200, "Pattern with translate transform should still produce red pixels");
    }
}

package com.jairosvg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExternalCssTest {

    @Test
    void testExternalCssStylesheetWithUnsafe(@TempDir Path tempDir) throws Exception {
        // Create external CSS file
        String css = ".myRect { fill: red; }";
        Path cssFile = tempDir.resolve("style.css");
        Files.writeString(cssFile, css);

        // Create SVG that references external CSS
        String svg = """
                <?xml version="1.0" encoding="UTF-8"?>
                <?xml-stylesheet type="text/css" href="style.css"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect class="myRect" x="0" y="0" width="100" height="100"/>
                </svg>
                """;
        Path svgFile = tempDir.resolve("test.svg");
        Files.writeString(svgFile, svg);

        // With unsafe=true, external CSS should be loaded
        byte[] png = JairoSVG.builder().fromFile(svgFile).unsafe(true).toPng();

        assertNotNull(png);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // Center should be red from external CSS
        int pixel = image.getRGB(50, 50);
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        assertEquals(255, red);
        assertEquals(0, green);
        assertEquals(0, blue);
    }

    @Test
    void testExternalCssStylesheetBlockedWithoutUnsafe(@TempDir Path tempDir) throws Exception {
        // Create external CSS file
        String css = ".myRect { fill: red; }";
        Path cssFile = tempDir.resolve("style.css");
        Files.writeString(cssFile, css);

        // Create SVG that references external CSS
        String svg = """
                <?xml version="1.0" encoding="UTF-8"?>
                <?xml-stylesheet type="text/css" href="style.css"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect class="myRect" x="0" y="0" width="100" height="100" fill="blue"/>
                </svg>
                """;
        Path svgFile = tempDir.resolve("test.svg");
        Files.writeString(svgFile, svg);

        // With unsafe=false (default), external CSS should NOT be loaded
        byte[] png = JairoSVG.builder().fromFile(svgFile).toPng();

        assertNotNull(png);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // Center should be blue (inline attribute), not red (blocked external CSS)
        int pixel = image.getRGB(50, 50);
        int red = (pixel >> 16) & 0xFF;
        int blue = pixel & 0xFF;
        assertTrue(blue > 200, "Should be blue since external CSS is blocked");
        assertTrue(red < 50, "Should not be red since external CSS is blocked");
    }
}

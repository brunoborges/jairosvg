package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BuilderApiTest {

    @Test
    void testBuilderApi() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="green"/>
                </svg>
                """;

        byte[] png = JairoSVG.builder().fromString(svg).scale(2).backgroundColor("white").toPng();

        assertNotNull(png);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    void testToImage() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <circle cx="25" cy="25" r="20" fill="yellow"/>
                </svg>
                """;

        BufferedImage image = JairoSVG.builder().fromString(svg).toImage();

        assertNotNull(image);
        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    @Test
    void testFileOutput(@TempDir Path tempDir) throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="blue"/>
                </svg>
                """;

        Path svgFile = tempDir.resolve("test.svg");
        Path pngFile = tempDir.resolve("test.png");
        Files.writeString(svgFile, svg);

        JairoSVG.svg2png(svgFile, pngFile);

        assertTrue(Files.exists(pngFile));
        assertTrue(Files.size(pngFile) > 0);
    }

    @Test
    void testOutputWidthHeight() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="green"/>
                </svg>
                """;

        byte[] png = JairoSVG.builder().fromString(svg).outputWidth(300).outputHeight(200).toPng();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(300, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    void testNegateColors() throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="red"/>
                </svg>
                """;

        byte[] png = JairoSVG.builder().fromString(svg).negateColors(true).toPng();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int pixel = image.getRGB(50, 50);
        // Red negated should be cyan (0, 255, 255)
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        assertEquals(0, red);
        assertEquals(255, green);
        assertEquals(255, blue);
    }

    @Test
    void testSvgzFromBytes() throws Exception {
        byte[] compressed = gzip("""
                <svg xmlns="http://www.w3.org/2000/svg" width="40" height="30">
                  <rect width="40" height="30" fill="green"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));

        byte[] png = JairoSVG.builder().fromBytes(compressed).toPng();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(40, image.getWidth());
        assertEquals(30, image.getHeight());
    }

    @Test
    void testSvgzFromFileAndDataUrl(@TempDir Path tempDir) throws Exception {
        byte[] compressed = gzip("""
                <svg xmlns="http://www.w3.org/2000/svg" width="25" height="15">
                  <rect width="25" height="15" fill="blue"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8));

        Path svgzFile = tempDir.resolve("test.svgz");
        Files.write(svgzFile, compressed);

        byte[] pngFromFile = JairoSVG.builder().fromFile(svgzFile).toPng();
        BufferedImage fileImage = ImageIO.read(new ByteArrayInputStream(pngFromFile));
        assertEquals(25, fileImage.getWidth());
        assertEquals(15, fileImage.getHeight());

        String dataUrl = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(compressed);
        byte[] pngFromUrl = JairoSVG.builder().fromUrl(dataUrl).toPng();
        BufferedImage urlImage = ImageIO.read(new ByteArrayInputStream(pngFromUrl));
        assertEquals(25, urlImage.getWidth());
        assertEquals(15, urlImage.getHeight());
    }

    private static byte[] gzip(byte[] bytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
        }
        return out.toByteArray();
    }
}

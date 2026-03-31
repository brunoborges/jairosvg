package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.brunoborges.jairosvg.cli.Main;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class CliTest {

    @Test
    void testCliJpegOutputFromExtension(@TempDir Path tempDir) throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="blue"/>
                </svg>
                """;
        Path svgFile = tempDir.resolve("test.svg");
        Path jpegFile = tempDir.resolve("test.jpg");
        Files.writeString(svgFile, svg);

        Main.main(new String[]{"-o", jpegFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(jpegFile);
        assertTrue(data.length > 0);
        assertEquals((byte) 0xFF, data[0]);
        assertEquals((byte) 0xD8, data[1]);
    }

    @Test
    void testCliTiffOutputFromExtension(@TempDir Path tempDir) throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <rect width="100" height="100" fill="blue"/>
                </svg>
                """;
        Path svgFile = tempDir.resolve("test.svg");
        Path tiffFile = tempDir.resolve("test.tiff");
        Files.writeString(svgFile, svg);

        Main.main(new String[]{"-o", tiffFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(tiffFile);
        assertTrue(data.length > 0);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
    }

    private static final String SIMPLE_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
              <rect width="100" height="100" fill="blue"/>
            </svg>
            """;

    private Path writeSvg(Path tempDir) throws Exception {
        Path svgFile = tempDir.resolve("input.svg");
        Files.writeString(svgFile, SIMPLE_SVG);
        return svgFile;
    }

    @Test
    void testHelpFlag() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        try {
            System.setOut(new PrintStream(baos));
            Main.main(new String[]{"--help"});
        } finally {
            System.setOut(oldOut);
        }
        String output = baos.toString();
        assertTrue(output.contains("Usage"), "Help output should contain usage info");
        assertTrue(output.contains("--output"), "Help output should describe --output flag");
    }

    @Test
    void testVersionFlag() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        try {
            System.setOut(new PrintStream(baos));
            Main.main(new String[]{"--version"});
        } finally {
            System.setOut(oldOut);
        }
        String output = baos.toString().strip();
        assertFalse(output.isEmpty(), "Version output should not be empty");
    }

    @Test
    void testPngOutputFormat(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path pngFile = tempDir.resolve("out.png");
        Main.main(new String[]{"-f", "png", "-o", pngFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(pngFile);
        assertTrue(data.length > 0);
        // PNG signature
        assertEquals((byte) 0x89, data[0]);
        assertEquals((byte) 0x50, data[1]); // 'P'
        assertEquals((byte) 0x4E, data[2]); // 'N'
        assertEquals((byte) 0x47, data[3]); // 'G'
    }

    @Test
    void testPsOutputFormat(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path psFile = tempDir.resolve("out.ps");
        Main.main(new String[]{"-f", "ps", "-o", psFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(psFile);
        assertTrue(data.length > 0);
        String content = new String(data);
        assertTrue(content.startsWith("%!PS"), "PS output should start with %!PS header");
    }

    @Test
    void testEpsOutputFormat(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path epsFile = tempDir.resolve("out.eps");
        Main.main(new String[]{"-f", "eps", "-o", epsFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(epsFile);
        assertTrue(data.length > 0);
        String content = new String(data);
        assertTrue(content.startsWith("%!PS"), "EPS output should start with %!PS header");
    }

    @Test
    void testScaleFlag(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("scaled.png");
        Main.main(new String[]{"-s", "2", "-o", outFile.toString(), svgFile.toString()});

        BufferedImage image = ImageIO.read(outFile.toFile());
        assertNotNull(image);
        // Scale 2x should double dimensions
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    void testBackgroundColorFlag(@TempDir Path tempDir) throws Exception {
        // Use an SVG without a background rect so the background color is visible
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <circle cx="50" cy="50" r="10" fill="black"/>
                </svg>
                """;
        Path svgFile = tempDir.resolve("nobg.svg");
        Files.writeString(svgFile, svg);

        Path outFile = tempDir.resolve("bg.png");
        Main.main(new String[]{"-b", "#ff0000", "-o", outFile.toString(), svgFile.toString()});

        BufferedImage image = ImageIO.read(outFile.toFile());
        assertNotNull(image);
        // Corner should be red (background)
        int pixel = image.getRGB(2, 2);
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        assertTrue(r > 200, "Background corner should be red, R=" + r);
        assertTrue(g < 50, "Background corner should be red, G=" + g);
        assertTrue(b < 50, "Background corner should be red, B=" + b);
    }

    @Test
    void testNegateColorsFlag(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("negated.png");
        Main.main(new String[]{"-n", "-o", outFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(outFile);
        assertTrue(data.length > 0, "Negated output should produce a non-empty file");

        BufferedImage image = ImageIO.read(outFile.toFile());
        assertNotNull(image);
        // The original SVG is blue (0,0,255). Negated should be (255,255,0) yellow.
        int pixel = image.getRGB(50, 50);
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        assertTrue(r > 200, "Negated blue should have high red: R=" + r);
        assertTrue(g > 200, "Negated blue should have high green: G=" + g);
        assertTrue(b < 50, "Negated blue should have low blue: B=" + b);
    }

    @Test
    void testOutputWidthHeightFlags(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("resized.png");
        Main.main(new String[]{"--output-width", "200", "--output-height", "200", "-o", outFile.toString(),
                svgFile.toString()});

        BufferedImage image = ImageIO.read(outFile.toFile());
        assertNotNull(image);
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    void testNativeImageConfigurationPresent() throws Exception {
        var resourcePath = "/META-INF/native-image/io.brunoborges/jairosvg/native-image.properties";
        try (var in = CliTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(in);
            var props = new Properties();
            props.load(in);
            var args = props.getProperty("Args");
            assertNotNull(args);
            assertTrue(args.contains("--enable-preview"));
            assertTrue(args.contains("-H:Class=io.brunoborges.jairosvg.cli.Main"));
            assertTrue(args.contains("-H:IncludeResources=io/brunoborges/jairosvg/css/colors\\.properties"));
        }
    }
}

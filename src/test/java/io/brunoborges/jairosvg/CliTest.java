package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.brunoborges.jairosvg.cli.Main;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class CliTest {

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

    // ---- Constructor ----

    @Test
    void testConstructor() {
        assertDoesNotThrow(() -> new Main());
    }

    // ---- Help flags ----

    @Test
    void testHelpFlag() throws Exception {
        String output = captureStdout(() -> Main.main(new String[]{"--help"}));
        assertTrue(output.contains("Usage"));
        assertTrue(output.contains("--output"));
    }

    @Test
    void testShortHelpFlag() throws Exception {
        String output = captureStdout(() -> Main.main(new String[]{"-h"}));
        assertTrue(output.contains("Usage"));
    }

    @Test
    void testNoArgsShowsHelp() throws Exception {
        String output = captureStdout(() -> Main.main(new String[]{}));
        assertTrue(output.contains("Usage"));
    }

    // ---- Version flags ----

    @Test
    void testVersionFlag() throws Exception {
        String output = captureStdout(() -> Main.main(new String[]{"--version"}));
        assertFalse(output.strip().isEmpty());
    }

    @Test
    void testShortVersionFlag() throws Exception {
        String output = captureStdout(() -> Main.main(new String[]{"-v"}));
        assertEquals(JairoSVG.VERSION, output.strip());
    }

    // ---- DPI flag ----

    @Test
    void testDpiFlag(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("dpi.png");
        Main.main(new String[]{"-d", "150", "-o", outFile.toString(), svgFile.toString()});

        assertTrue(Files.exists(outFile));
        assertTrue(Files.size(outFile) > 0);
    }

    // ---- Width / Height flags ----

    @Test
    void testWidthHeightFlags(@TempDir Path tempDir) throws Exception {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50%" height="50%">
                  <rect width="100%" height="100%" fill="red"/>
                </svg>
                """;
        Path svgFile = tempDir.resolve("pct.svg");
        Files.writeString(svgFile, svg);
        Path outFile = tempDir.resolve("wh.png");

        Main.main(new String[]{"-W", "200", "-H", "200", "-o", outFile.toString(), svgFile.toString()});

        BufferedImage image = ImageIO.read(outFile.toFile());
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
    }

    // ---- Unsafe flag ----

    @Test
    void testUnsafeFlag(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("unsafe.png");
        Main.main(new String[]{"-u", "-o", outFile.toString(), svgFile.toString()});

        assertTrue(Files.exists(outFile));
        assertTrue(Files.size(outFile) > 0);
    }

    // ---- Unknown flag (ignored) ----

    @Test
    void testUnknownFlagIgnored(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("unknown.png");
        Main.main(new String[]{"--some-unknown-flag", "-o", outFile.toString(), svgFile.toString()});

        assertTrue(Files.exists(outFile));
    }

    // ---- Output format flags ----

    @Test
    void testPngOutputFormat(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path pngFile = tempDir.resolve("out.png");
        Main.main(new String[]{"-f", "png", "-o", pngFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(pngFile);
        assertEquals((byte) 0x89, data[0]);
        assertEquals((byte) 0x50, data[1]);
    }

    @Test
    void testJpegOutputFromExtension(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path jpegFile = tempDir.resolve("test.jpg");
        Main.main(new String[]{"-o", jpegFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(jpegFile);
        assertEquals((byte) 0xFF, data[0]);
        assertEquals((byte) 0xD8, data[1]);
    }

    @Test
    void testTiffOutputFromExtension(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path tiffFile = tempDir.resolve("test.tiff");
        Main.main(new String[]{"-o", tiffFile.toString(), svgFile.toString()});

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(Files.readAllBytes(tiffFile)));
        assertNotNull(image);
        assertEquals(100, image.getWidth());
    }

    @Test
    void testPdfOutputFormat(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path pdfFile = tempDir.resolve("out.pdf");
        Main.main(new String[]{"-f", "pdf", "-o", pdfFile.toString(), svgFile.toString()});

        byte[] data = Files.readAllBytes(pdfFile);
        assertTrue(data.length > 0);
        assertEquals('%', (char) data[0]);
        assertEquals('P', (char) data[1]);
        assertEquals('D', (char) data[2]);
        assertEquals('F', (char) data[3]);
    }

    @Test
    void testPsOutputFormat(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path psFile = tempDir.resolve("out.ps");
        Main.main(new String[]{"-f", "ps", "-o", psFile.toString(), svgFile.toString()});

        String content = Files.readString(psFile);
        assertTrue(content.startsWith("%!PS"));
    }

    @Test
    void testEpsOutputFormat(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path epsFile = tempDir.resolve("out.eps");
        Main.main(new String[]{"-f", "eps", "-o", epsFile.toString(), svgFile.toString()});

        String content = Files.readString(epsFile);
        assertTrue(content.startsWith("%!PS"));
    }

    @Test
    void testDefaultFormatFallback(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("out.xyz");
        Main.main(new String[]{"-o", outFile.toString(), svgFile.toString()});

        // Unknown extension falls back to PNG via getOrDefault
        byte[] data = Files.readAllBytes(outFile);
        assertEquals((byte) 0x89, data[0]);
        assertEquals((byte) 0x50, data[1]);
    }

    @Test
    void testUnknownFormatFlagFallsBackToPng(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("out.dat");
        Main.main(new String[]{"-f", "bmp", "-o", outFile.toString(), svgFile.toString()});

        // Unknown format "BMP" hits default case in writeOutput → falls back to PNG
        byte[] data = Files.readAllBytes(outFile);
        assertEquals((byte) 0x89, data[0]);
        assertEquals((byte) 0x50, data[1]);
    }

    // ---- Scale, background, negate ----

    @Test
    void testScaleFlag(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("scaled.png");
        Main.main(new String[]{"-s", "2", "-o", outFile.toString(), svgFile.toString()});

        BufferedImage image = ImageIO.read(outFile.toFile());
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    void testBackgroundColorFlag(@TempDir Path tempDir) throws Exception {
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
        int pixel = image.getRGB(2, 2);
        assertTrue(((pixel >> 16) & 0xFF) > 200, "Background should be red");
    }

    @Test
    void testNegateColorsFlag(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("negated.png");
        Main.main(new String[]{"-n", "-o", outFile.toString(), svgFile.toString()});

        BufferedImage image = ImageIO.read(outFile.toFile());
        int pixel = image.getRGB(50, 50);
        // Blue negated → yellow (high R, high G, low B)
        assertTrue(((pixel >> 16) & 0xFF) > 200);
        assertTrue(((pixel >> 8) & 0xFF) > 200);
        assertTrue((pixel & 0xFF) < 50);
    }

    @Test
    void testOutputWidthHeightFlags(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        Path outFile = tempDir.resolve("resized.png");
        Main.main(new String[]{"--output-width", "200", "--output-height", "200", "-o", outFile.toString(),
                svgFile.toString()});

        BufferedImage image = ImageIO.read(outFile.toFile());
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    // ---- Stdout output (no -o flag) ----

    @Test
    void testStdoutOutput(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        try {
            System.setOut(new PrintStream(baos));
            Main.main(new String[]{svgFile.toString()});
        } finally {
            System.setOut(oldOut);
        }
        byte[] data = baos.toByteArray();
        assertTrue(data.length > 0, "Stdout should have PNG data");
        // PNG signature
        assertEquals((byte) 0x89, data[0]);
    }

    @Test
    void testStdoutWithFormatFlag(@TempDir Path tempDir) throws Exception {
        Path svgFile = writeSvg(tempDir);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        try {
            System.setOut(new PrintStream(baos));
            Main.main(new String[]{"-f", "jpeg", svgFile.toString()});
        } finally {
            System.setOut(oldOut);
        }
        byte[] data = baos.toByteArray();
        // JPEG signature
        assertEquals((byte) 0xFF, data[0]);
        assertEquals((byte) 0xD8, data[1]);
    }

    // ---- Stdin input ----

    @Test
    void testStdinInput(@TempDir Path tempDir) throws Exception {
        Path outFile = tempDir.resolve("stdin.png");
        InputStream oldIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(SIMPLE_SVG.getBytes()));
            Main.main(new String[]{"-o", outFile.toString(), "-"});
        } finally {
            System.setIn(oldIn);
        }
        assertTrue(Files.exists(outFile));
        BufferedImage image = ImageIO.read(outFile.toFile());
        assertEquals(100, image.getWidth());
    }

    // ---- No input error ----

    @Test
    void testNoInputThrowsException() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Main.main(new String[]{"-d", "96"}));
        assertTrue(ex.getMessage().contains("No input"));
    }

    // ---- Native image config ----

    @Test
    void testNativeImageConfigurationPresent() throws Exception {
        var resourcePath = "/META-INF/native-image/io.brunoborges/jairosvg/native-image.properties";
        try (var in = CliTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(in);
            var props = new Properties();
            props.load(in);
            var args = props.getProperty("Args");
            assertNotNull(args);
            assertFalse(args.contains("--enable-preview"));
            assertTrue(args.contains("-H:Class=io.brunoborges.jairosvg.cli.Main"));
        }
    }

    // ---- Helpers ----

    private static String captureStdout(ThrowingRunnable action) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        try {
            System.setOut(new PrintStream(baos));
            action.run();
        } finally {
            System.setOut(oldOut);
        }
        return baos.toString();
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}

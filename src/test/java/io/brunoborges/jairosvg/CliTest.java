package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.brunoborges.jairosvg.cli.Main;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
}

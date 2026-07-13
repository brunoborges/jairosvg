package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * Verifies that internal output-buffer pooling (recycling the {@code int[]}
 * backing array of the render target across {@code byte[]}-returning
 * conversions) does not alter rendering output. The key correctness risk is a
 * recycled buffer retaining stale pixels; these tests render repeatedly and
 * assert byte-identical results.
 */
class BufferReuseTest {

    private static final String OPAQUE = """
            <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64">
              <rect width="64" height="64" fill="#123456"/>
              <circle cx="32" cy="32" r="20" fill="#abcdef"/>
            </svg>
            """;

    // Content that leaves large transparent regions, so a stale (uncleared)
    // reused buffer would surface as differing pixels.
    private static final String SPARSE = """
            <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64">
              <circle cx="16" cy="16" r="8" fill="red"/>
            </svg>
            """;

    @Test
    void repeatedPngIsByteIdentical() throws Exception {
        byte[] first = JairoSVG.svg2png(OPAQUE.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < 10; i++) {
            byte[] again = JairoSVG.svg2png(OPAQUE.getBytes(StandardCharsets.UTF_8));
            assertArrayEquals(first, again, "PNG output diverged on repeat " + i);
        }
    }

    @Test
    void reusedBufferIsFullyClearedBetweenRenders() throws Exception {
        // Render an opaque full-canvas image, then a sparse one of the same size.
        // If the buffer were reused without clearing, the sparse render's
        // transparent pixels would leak the opaque image's colors.
        JairoSVG.svg2png(OPAQUE.getBytes(StandardCharsets.UTF_8));
        byte[] sparse = JairoSVG.svg2png(SPARSE.getBytes(StandardCharsets.UTF_8));

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(sparse));
        assertNotNull(img);
        // A corner far from the small red circle must be fully transparent.
        int argb = img.getRGB(60, 60);
        assertArrayEquals(new int[]{0}, new int[]{argb >>> 24},
                "Corner alpha must be 0 (transparent); stale pixels leaked from a reused buffer");
    }

    @Test
    void differentSizesDoNotCorruptEachOther() throws Exception {
        String small = """
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32">
                  <rect width="32" height="32" fill="green"/>
                </svg>
                """;
        byte[] bigRef = JairoSVG.svg2png(OPAQUE.getBytes(StandardCharsets.UTF_8));
        JairoSVG.svg2png(small.getBytes(StandardCharsets.UTF_8));
        byte[] bigAgain = JairoSVG.svg2png(OPAQUE.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(bigRef, bigAgain, "Interleaving a different size must not corrupt output");
    }
}

package io.brunoborges.jairosvg.surface;

import java.io.IOException;

/**
 * PNG output surface. Uses a custom parallel PNG encoder that compresses image
 * strips concurrently using virtual threads for large images.
 */
public final class PngSurface extends Surface {

    private int compressionLevel = -1; // -1 = default

    /**
     * Set PNG compression level (0-9). 0 = no compression (fastest), 9 = max
     * compression (smallest). Default uses a balanced level (4).
     */
    public void setCompressionLevel(int level) {
        if (level < 0 || level > 9) {
            throw new IllegalArgumentException("PNG compression level must be 0-9, got: " + level);
        }
        this.compressionLevel = level;
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output != null && image != null) {
            ParallelPngEncoder.encode(image, output, compressionLevel);
        }
    }
}

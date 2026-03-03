package com.jairosvg.surface;

import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * PNG output surface.
 */
public final class PngSurface extends Surface {

    private static final ImageWriter PNG_WRITER;

    static {
        var writers = ImageIO.getImageWritersByFormatName("PNG");
        if (!writers.hasNext()) {
            throw new ExceptionInInitializerError("No PNG ImageWriter found");
        }
        PNG_WRITER = writers.next();
    }

    private int compressionLevel = -1; // -1 = default (JDK default is ~6)

    /**
     * Set PNG compression level (0-9). 0 = no compression (fastest), 9 = max
     * compression (smallest). Default uses the JDK default (~6).
     */
    public void setCompressionLevel(int level) {
        if (level < 0 || level > 9) {
            throw new IllegalArgumentException("PNG compression level must be 0-9, got: " + level);
        }
        this.compressionLevel = level;
    }

    /**
     * Write a BufferedImage as PNG to the given ImageOutputStream.
     */
    static void writePng(java.awt.image.BufferedImage img, javax.imageio.stream.ImageOutputStream ios)
            throws IOException {
        writePng(img, ios, -1);
    }

    static void writePng(java.awt.image.BufferedImage img, javax.imageio.stream.ImageOutputStream ios,
            int compressionLevel) throws IOException {
        synchronized (PNG_WRITER) {
            try {
                PNG_WRITER.setOutput(ios);
                ImageWriteParam param = null;
                if (compressionLevel >= 0) {
                    param = PNG_WRITER.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    // PNG plugin maps quality 0.0 = max compression, 1.0 = no compression
                    param.setCompressionQuality(1.0f - (compressionLevel / 9.0f));
                }
                PNG_WRITER.write(null, new IIOImage(img, null, null), param);
            } finally {
                PNG_WRITER.reset();
            }
        }
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output != null && image != null) {
            try (var ios = new MemoryCacheImageOutputStream(output)) {
                writePng(image, ios, compressionLevel);
            }
        }
    }
}

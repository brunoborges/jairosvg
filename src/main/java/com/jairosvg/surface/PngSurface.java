package com.jairosvg.surface;

import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
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

    /**
     * Write a BufferedImage as PNG to the given ImageOutputStream.
     */
    static void writePng(java.awt.image.BufferedImage img, javax.imageio.stream.ImageOutputStream ios)
            throws IOException {
        synchronized (PNG_WRITER) {
            try {
                PNG_WRITER.setOutput(ios);
                PNG_WRITER.write(null, new IIOImage(img, null, null), null);
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
                writePng(image, ios);
            }
        }
    }
}

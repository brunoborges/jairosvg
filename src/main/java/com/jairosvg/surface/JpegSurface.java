package com.jairosvg.surface;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * JPEG output surface.
 */
public class JpegSurface extends Surface {

    private static final ImageWriter JPEG_WRITER;

    static {
        var writers = ImageIO.getImageWritersByFormatName("JPEG");
        if (!writers.hasNext()) {
            throw new ExceptionInInitializerError("No JPEG ImageWriter found");
        }
        JPEG_WRITER = writers.next();
    }

    @Override
    protected void createSurface(double w, double h) {
        int iw = Math.max(1, (int) Math.round(w));
        int ih = Math.max(1, (int) Math.round(h));
        this.image = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
        this.width = iw;
        this.height = ih;
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output != null && image != null) {
            synchronized (JPEG_WRITER) {
                try (var ios = new MemoryCacheImageOutputStream(output)) {
                    JPEG_WRITER.setOutput(ios);
                    JPEG_WRITER.write(null, new IIOImage(image, null, null), null);
                } finally {
                    JPEG_WRITER.reset();
                }
            }
        }
    }
}

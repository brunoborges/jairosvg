package com.jairosvg.surface;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * JPEG output surface.
 */
public final class JpegSurface extends Surface {

    private static final ImageWriter JPEG_WRITER;

    static {
        var writers = ImageIO.getImageWritersByFormatName("JPEG");
        if (!writers.hasNext()) {
            throw new ExceptionInInitializerError("No JPEG ImageWriter found");
        }
        JPEG_WRITER = writers.next();
    }

    private float quality = -1f; // -1 = default (~0.75)

    /**
     * Set JPEG quality (0.0-1.0). 0.0 = lowest quality (smallest), 1.0 = highest
     * quality (largest). Default uses the JDK default (~0.75).
     */
    public void setQuality(float quality) {
        if (quality < 0f || quality > 1f) {
            throw new IllegalArgumentException("JPEG quality must be 0.0-1.0, got: " + quality);
        }
        this.quality = quality;
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
                    ImageWriteParam param = null;
                    if (quality >= 0f) {
                        param = JPEG_WRITER.getDefaultWriteParam();
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(quality);
                    }
                    JPEG_WRITER.write(null, new IIOImage(image, null, null), param);
                } finally {
                    JPEG_WRITER.reset();
                }
            }
        }
    }
}

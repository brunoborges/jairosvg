package com.jairosvg.surface;

import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * TIFF output surface.
 */
public class TiffSurface extends Surface {

    private static final ImageWriter TIFF_WRITER;

    static {
        var writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new ExceptionInInitializerError("No TIFF ImageWriter found");
        }
        TIFF_WRITER = writers.next();
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output != null && image != null) {
            synchronized (TIFF_WRITER) {
                try (var ios = new MemoryCacheImageOutputStream(output)) {
                    TIFF_WRITER.setOutput(ios);
                    TIFF_WRITER.write(null, new IIOImage(image, null, null), null);
                } finally {
                    TIFF_WRITER.reset();
                }
            }
        }
    }
}

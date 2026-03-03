package com.jairosvg.surface;

import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * TIFF output surface.
 */
public final class TiffSurface extends Surface {

    private static final ImageWriter TIFF_WRITER;

    static {
        var writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new ExceptionInInitializerError("No TIFF ImageWriter found");
        }
        TIFF_WRITER = writers.next();
    }

    private String compressionType; // null = default

    /**
     * Set TIFF compression type. Common values: "Deflate", "LZW", "JPEG", "ZLib",
     * "PackBits", "Uncompressed". Default uses the writer's default.
     */
    public void setCompressionType(String type) {
        this.compressionType = type;
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output != null && image != null) {
            synchronized (TIFF_WRITER) {
                try (var ios = new MemoryCacheImageOutputStream(output)) {
                    TIFF_WRITER.setOutput(ios);
                    ImageWriteParam param = null;
                    if (compressionType != null) {
                        param = TIFF_WRITER.getDefaultWriteParam();
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionType(compressionType);
                    }
                    TIFF_WRITER.write(null, new IIOImage(image, null, null), param);
                } finally {
                    TIFF_WRITER.reset();
                }
            }
        }
    }
}

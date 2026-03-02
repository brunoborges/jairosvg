package com.jairosvg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

/**
 * PNG output surface.
 */
public class PngSurface extends Surface {

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output != null && image != null) {
            ImageIO.write(image, "PNG", output);
        }
    }
}

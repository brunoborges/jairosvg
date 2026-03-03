package com.jairosvg;

import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * TIFF output surface.
 */
public class TiffSurface extends Surface {

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output != null && image != null) {
            ImageIO.write(image, "TIFF", output);
        }
    }
}

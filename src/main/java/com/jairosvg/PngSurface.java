package com.jairosvg;

import java.io.IOException;

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

package com.jairosvg;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * JPEG output surface.
 */
public class JpegSurface extends Surface {

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
            ImageIO.write(image, "JPEG", output);
        }
    }
}

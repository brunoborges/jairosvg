package com.jairosvg;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * JPEG output surface.
 */
public class JpegSurface extends Surface {

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output != null && image != null) {
            // Convert ARGB to RGB for JPEG (no transparency support)
            BufferedImage rgbImage = new BufferedImage(
                image.getWidth(), 
                image.getHeight(), 
                BufferedImage.TYPE_INT_RGB
            );
            rgbImage.getGraphics().drawImage(image, 0, 0, null);
            ImageIO.write(rgbImage, "JPEG", output);
        }
    }
}

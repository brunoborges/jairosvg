package com.jairosvg.surface;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SVG output surface. Re-renders the parsed SVG as a clean SVG document with
 * the rasterized image embedded.
 */
public class SvgSurface extends Surface {

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output == null || image == null)
            return;

        // Encode the rendered image as a PNG data URI within a new SVG
        var pngBytes = new java.io.ByteArrayOutputStream();
        try (var ios = new javax.imageio.stream.MemoryCacheImageOutputStream(pngBytes)) {
            PngSurface.writePng(image, ios);
        }
        String base64 = Base64.getEncoder().encodeToString(pngBytes.toByteArray());

        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + "width=\"%d\" height=\"%d\">\n", image.getWidth(),
                image.getHeight()));
        writer.write(String.format("  <image width=\"%d\" height=\"%d\" " + "href=\"data:image/png;base64,%s\"/>\n",
                image.getWidth(), image.getHeight(), base64));
        writer.write("</svg>\n");
        writer.flush();
    }
}

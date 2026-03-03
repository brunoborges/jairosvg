package com.jairosvg;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * PDF output surface using Apache PDFBox. Renders SVG to a BufferedImage via
 * the base Surface, then embeds it in a PDF page.
 */
public class PdfSurface extends Surface {

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output == null || image == null)
            return;

        try (PDDocument doc = new PDDocument()) {
            float pdfWidth = (float) width;
            float pdfHeight = (float) height;
            PDPage page = new PDPage(new PDRectangle(pdfWidth, pdfHeight));
            doc.addPage(page);

            PDImageXObject pdfImage = LosslessFactory.createFromImage(doc, image);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // PDF coordinate origin is bottom-left, so flip vertically
                cs.drawImage(pdfImage, 0, 0, pdfWidth, pdfHeight);
            }

            doc.save(output);
        }
    }
}

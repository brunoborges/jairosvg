package com.jairosvg;

import java.awt.Graphics2D;
import java.awt.print.*;
import java.io.IOException;

import javax.print.*;
import javax.print.attribute.*;

/**
 * PostScript output surface. Uses Java's printing API to generate PS output.
 */
public class PsSurface extends Surface {

    private boolean eps = false;

    public PsSurface() {
    }

    public PsSurface(boolean eps) {
        this.eps = eps;
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        if (output == null || image == null)
            return;

        // Use Java's print API to render to PostScript
        PrinterJob job = PrinterJob.getPrinterJob();

        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);
        pf.setPaper(paper);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0)
                return Printable.NO_SUCH_PAGE;
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.drawImage(image, 0, 0, null);
            return Printable.PAGE_EXISTS;
        }, pf);

        // Create a PostScript stream
        DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
        String mimeType = eps ? "application/postscript" : "application/postscript";
        StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor,
                mimeType);

        if (factories.length > 0) {
            try {
                StreamPrintService sps = factories[0].getPrintService(output);
                job.setPrintService(sps);
                job.print();
            } catch (java.awt.print.PrinterException e) {
                throw new IOException("Failed to generate PostScript output", e);
            }
        } else {
            // Fallback: write PNG (PS not available)
            javax.imageio.ImageIO.write(image, "PNG", output);
        }
    }
}

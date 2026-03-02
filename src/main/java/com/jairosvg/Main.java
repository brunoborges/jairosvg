package com.jairosvg;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Command-line interface to JairoSVG.
 * Port of CairoSVG __main__.py
 */
public final class Main {

    private static final Map<String, String> FORMAT_EXTENSIONS = Map.of(
        "png", "PNG",
        "pdf", "PDF",
        "ps", "PS",
        "eps", "EPS",
        "svg", "SVG"
    );

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printUsage();
            return;
        }

        if ("--version".equals(args[0]) || "-v".equals(args[0])) {
            System.out.println(JairoSVG.VERSION);
            return;
        }

        // Parse arguments
        String input = null;
        String output = "-";
        String format = null;
        double dpi = 96;
        Double width = null;
        Double height = null;
        double scale = 1;
        String background = null;
        boolean negateColors = false;
        boolean invertImages = false;
        boolean unsafe = false;
        Double outputWidth = null;
        Double outputHeight = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o", "--output" -> output = args[++i];
                case "-f", "--format" -> format = args[++i];
                case "-d", "--dpi" -> dpi = Double.parseDouble(args[++i]);
                case "-W", "--width" -> width = Double.parseDouble(args[++i]);
                case "-H", "--height" -> height = Double.parseDouble(args[++i]);
                case "-s", "--scale" -> scale = Double.parseDouble(args[++i]);
                case "-b", "--background" -> background = args[++i];
                case "-n", "--negate-colors" -> negateColors = true;
                case "-i", "--invert-images" -> invertImages = true;
                case "-u", "--unsafe" -> unsafe = true;
                case "--output-width" -> outputWidth = Double.parseDouble(args[++i]);
                case "--output-height" -> outputHeight = Double.parseDouble(args[++i]);
                default -> {
                    if (!args[i].startsWith("-")) {
                        input = args[i];
                    }
                }
            }
        }

        if (input == null) {
            System.err.println("Error: No input file specified.");
            printUsage();
            System.exit(1);
        }

        // Determine output format
        if (format == null) {
            if (!"-".equals(output)) {
                String ext = output.substring(output.lastIndexOf('.') + 1).toLowerCase();
                format = FORMAT_EXTENSIONS.getOrDefault(ext, "PDF");
            } else {
                format = "PDF";
            }
        } else {
            format = format.toUpperCase();
        }

        // Build conversion
        var builder = JairoSVG.builder()
            .dpi(dpi)
            .scale(scale)
            .unsafe(unsafe)
            .negateColors(negateColors)
            .invertImages(invertImages);

        if (background != null) builder.backgroundColor(background);
        if (width != null) builder.parentWidth(width);
        if (height != null) builder.parentHeight(height);
        if (outputWidth != null) builder.outputWidth(outputWidth);
        if (outputHeight != null) builder.outputHeight(outputHeight);

        // Read input
        if ("-".equals(input)) {
            builder.fromStream(System.in);
        } else {
            builder.fromFile(Path.of(input));
        }

        // Write output
        OutputStream out;
        if ("-".equals(output)) {
            out = System.out;
        } else {
            out = new FileOutputStream(output);
        }

        switch (format) {
            case "PNG" -> builder.toPng(out);
            case "PDF" -> builder.toPdf(out);
            case "SVG" -> builder.toSvg(out);
            case "PS", "EPS" -> {
                byte[] data = builder.toPs();
                out.write(data);
            }
            default -> builder.toPdf(out);
        }

        if (!"-".equals(output)) {
            out.close();
        }
    }

    private static void printUsage() {
        System.out.println("""
            JairoSVG %s - Convert SVG files to other formats

            Usage: jairosvg [options] input.svg

            Options:
              -o, --output FILE      Output filename (default: stdout)
              -f, --format FORMAT    Output format: png, pdf, ps, eps, svg
              -d, --dpi DPI          DPI ratio (default: 96)
              -W, --width PIXELS     Parent container width
              -H, --height PIXELS    Parent container height
              -s, --scale FACTOR     Output scaling factor (default: 1)
              -b, --background COLOR Output background color
              -n, --negate-colors    Negate vector colors
              -i, --invert-images    Invert raster image colors
              -u, --unsafe           Allow external file access and XML entities
              --output-width PIXELS  Desired output width
              --output-height PIXELS Desired output height
              -v, --version          Show version
              -h, --help             Show this help
            """.formatted(JairoSVG.VERSION));
    }
}

package io.brunoborges.jairosvg.cli;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.brunoborges.jairosvg.JairoSVG;

/**
 * Command-line interface to JairoSVG.
 */
public final class Main {

    private static final Map<String, String> FORMAT_EXTENSIONS = Map.of("png", "PNG", "jpeg", "JPEG", "jpg", "JPEG",
            "tiff", "TIFF", "tif", "TIFF", "pdf", "PDF", "ps", "PS", "eps", "EPS");

    public static void main(String[] args) throws Exception {
        ensureJavaHome();

        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printUsage();
            return;
        }

        if ("--version".equals(args[0]) || "-v".equals(args[0])) {
            System.out.println(JairoSVG.VERSION);
            return;
        }

        // Parse arguments
        List<String> inputs = new ArrayList<>();
        String output = "-";
        String format = null;
        double dpi = 96;
        Double width = null;
        Double height = null;
        double scale = 1;
        String background = null;
        boolean negateColors = false;
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
                case "-u", "--unsafe" -> unsafe = true;
                case "--output-width" -> outputWidth = Double.parseDouble(args[++i]);
                case "--output-height" -> outputHeight = Double.parseDouble(args[++i]);
                default -> {
                    if (!args[i].startsWith("-") || "-".equals(args[i])) {
                        inputs.add(args[i]);
                    }
                }
            }
        }

        if (inputs.isEmpty()) {
            System.err.println("Error: No input file specified.");
            printUsage();
            throw new IllegalArgumentException("No input file specified.");
        }

        boolean multiFile = inputs.size() > 1 || (!"-".equals(output) && Files.isDirectory(Path.of(output)));

        // For multiple files, -o must be a directory (or omitted for auto-naming)
        if (multiFile && !"-".equals(output) && !Files.isDirectory(Path.of(output))) {
            Files.createDirectories(Path.of(output));
        }

        // Determine output format
        if (format != null) {
            format = format.toUpperCase();
        } else if (!multiFile && !"-".equals(output)) {
            String ext = output.substring(output.lastIndexOf('.') + 1).toLowerCase();
            format = FORMAT_EXTENSIONS.getOrDefault(ext, "PNG");
        } else {
            format = "PNG";
        }

        String formatExt = switch (format) {
            case "JPEG" -> "jpeg";
            case "TIFF" -> "tiff";
            case "PDF" -> "pdf";
            case "PS", "EPS" -> "ps";
            default -> "png";
        };

        for (String input : inputs) {
            var builder = JairoSVG.builder().dpi(dpi).scale(scale).unsafe(unsafe).negateColors(negateColors);

            if (background != null)
                builder.backgroundColor(background);
            if (width != null)
                builder.parentWidth(width);
            if (height != null)
                builder.parentHeight(height);
            if (outputWidth != null)
                builder.outputWidth(outputWidth);
            if (outputHeight != null)
                builder.outputHeight(outputHeight);

            // Read input
            if ("-".equals(input)) {
                builder.fromStream(System.in);
            } else {
                builder.fromFile(Path.of(input));
            }

            // Determine per-file output path
            String fileOutput;
            if (multiFile) {
                String baseName = Path.of(input).getFileName().toString().replaceFirst("\\.[^.]+$", "");
                if ("-".equals(output)) {
                    fileOutput = baseName + "." + formatExt;
                } else {
                    fileOutput = Path.of(output, baseName + "." + formatExt).toString();
                }
            } else {
                fileOutput = output;
            }

            // Write output
            if ("-".equals(fileOutput)) {
                writeOutput(builder, format, System.out);
            } else {
                try (var out = new FileOutputStream(fileOutput)) {
                    writeOutput(builder, format, out);
                }
            }
        }
    }

    private static void printUsage() {
        System.out.println("""
                JairoSVG %s - Convert SVG files to other formats

                Usage: jairosvg [options] input.svg [input2.svg ...]

                Options:
                  -o, --output FILE|DIR  Output filename or directory (default: stdout)
                  -f, --format FORMAT    Output format: png, jpeg, tiff, pdf, ps, eps
                  -d, --dpi DPI          DPI ratio (default: 96)
                  -W, --width PIXELS     Parent container width
                  -H, --height PIXELS    Parent container height
                  -s, --scale FACTOR     Output scaling factor (default: 1)
                  -b, --background COLOR Output background color
                  -n, --negate-colors    Negate vector colors
                  -u, --unsafe           Allow external file access and XML entities
                  --output-width PIXELS  Desired output width
                  --output-height PIXELS Desired output height
                  -v, --version          Show version
                  -h, --help             Show this help

                When multiple input files are given, -o specifies an output directory.
                Output filenames are derived from input names with the appropriate extension.
                """.formatted(JairoSVG.VERSION));
    }

    private static void writeOutput(JairoSVG.ConversionBuilder builder, String format, OutputStream out)
            throws Exception {
        switch (format) {
            case "PNG" -> builder.toPng(out);
            case "JPEG" -> builder.toJpeg(out);
            case "TIFF" -> builder.toTiff(out);
            case "PDF" -> builder.toPdf(out);
            case "PS", "EPS" -> {
                byte[] data = builder.toPs();
                out.write(data);
            }
            default -> builder.toPng(out);
        }
    }

    /**
     * Ensure java.home is set for font configuration (required in GraalVM native
     * images).
     */
    private static void ensureJavaHome() {
        if (System.getProperty("java.home") == null) {
            String javaHome = System.getenv("JAVA_HOME");
            if (javaHome == null) {
                javaHome = "/usr";
            }
            System.setProperty("java.home", javaHome);
        }
    }
}

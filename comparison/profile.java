///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//JAVA_OPTIONS --enable-preview
//DEPS io.brunoborges:jairosvg:1.0.3-SNAPSHOT
//DEPS com.github.weisj:jsvg:2.0.0

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import io.brunoborges.jairosvg.JairoSVG;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.*;

/**
 * Profiling harness for JairoSVG vs JSVG flame chart generation.
 *
 * Usage:
 *   jbang comparison/profile.java [scenario] [library] [iterations]
 *
 * Examples:
 *   jbang comparison/profile.java gradients jairosvg 2000
 *   jbang comparison/profile.java masks jsvg 2000
 *
 * The harness warms up for half the iterations, then signals ready via
 * a file (profile.ready) so async-profiler can be attached externally,
 * or use --agent mode (see README below).
 *
 * Recommended usage with async-profiler:
 *   java -agentpath:/opt/homebrew/lib/libasyncProfiler.dylib=start,event=cpu,\
 *        file=profile-jairosvg-gradients.html,flamegraph \
 *        -jar ... comparison/profile.java gradients jairosvg 5000
 */
public class profile {

    static final Path SVG_DIR = Path.of("comparison/svg");
    static final int DEFAULT_ITERATIONS = 3000;
    static final int WARMUP_FRACTION = 2; // warmup = iterations / WARMUP_FRACTION

    public static void main(String[] args) throws Exception {
        String scenarioArg = args.length > 0 ? args[0] : "gradients";
        String library    = args.length > 1 ? args[1] : "jairosvg";
        int iterations    = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_ITERATIONS;
        // mode: "full" (parse+render+encode, matches benchmark) or "render" (render only, no PNG encode)
        String mode       = args.length > 3 ? args[3] : "render";
        int warmup        = iterations / WARMUP_FRACTION;

        // Find the SVG file
        List<Path> allSvgs;
        try (var stream = Files.list(SVG_DIR)) { allSvgs = stream.sorted().toList(); }
        Path svgFile = allSvgs.stream()
                .filter(p -> p.getFileName().toString().contains(scenarioArg.toLowerCase().replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No SVG file found for scenario: " + scenarioArg + "\nAvailable: " +
                        allSvgs.stream().map(p -> p.getFileName().toString()).collect(Collectors.joining(", "))));

        byte[] svgBytes = Files.readAllBytes(svgFile);
        String label = svgFile.getFileName().toString().replaceFirst("^\\d+_", "").replace(".svg", "").replace('_', ' ');

        Converter converter = switch (library.toLowerCase() + ":" + mode) {
            case "jairosvg:full"   -> profile::jairosvgConvert;
            case "jairosvg:render" -> profile::jairosvgRenderOnly;
            case "jsvg:full"       -> profile::jsvgConvert;
            case "jsvg:render"     -> profile::jsvgRenderOnly;
            default -> throw new IllegalArgumentException(
                    "Unknown library/mode: " + library + "/" + mode + " (library: jairosvg|jsvg, mode: full|render)");
        };

        System.out.printf("Profiling harness: %s | %s | mode=%s | warmup=%d | iterations=%d%n",
                label, library, mode, warmup, iterations);

        // Warmup phase — let JIT compile everything
        System.out.print("Warming up... ");
        for (int i = 0; i < warmup; i++) {
            converter.convert(svgBytes);
        }
        System.out.println("done.");

        // Signal that profiling window begins
        // (async-profiler in agent mode is already started; this just marks the start of the hot loop)
        System.out.println(">>> PROFILING WINDOW START — " + iterations + " iterations");
        long t0 = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            converter.convert(svgBytes);
        }

        long elapsed = System.nanoTime() - t0;
        System.out.printf(">>> PROFILING WINDOW END — avg=%.3f ms/iter%n",
                elapsed / 1_000_000.0 / iterations);
    }

    @FunctionalInterface
    interface Converter {
        void convert(byte[] svgBytes) throws Exception;
    }

    static void jairosvgConvert(byte[] svgBytes) throws Exception {
        // Matches the benchmark: full parse + render + PNG encode
        JairoSVG.svg2png(svgBytes);
    }

    /** Render only (no PNG encode) — use with mode=render */
    static void jairosvgRenderOnly(byte[] svgBytes) throws Exception {
        JairoSVG.builder().fromBytes(svgBytes).toImage();
    }

    static void jsvgConvert(byte[] svgBytes) throws Exception {
        // Matches the benchmark: full parse + load + render + PNG encode
        SVGLoader loader = new SVGLoader();
        SVGDocument doc = loader.load(
                new ByteArrayInputStream(svgBytes),
                null,
                LoaderContext.createDefault());
        if (doc == null) return;
        var size = doc.size();
        int w = Math.max(1, (int) size.width);
        int h = Math.max(1, (int) size.height);
        var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        doc.render(null, g2);
        g2.dispose();
        // Encode PNG (same as benchmark)
        var baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
    }

    /** Render only (no PNG encode) — use with mode=render */
    static void jsvgRenderOnly(byte[] svgBytes) throws Exception {
        SVGLoader loader = new SVGLoader();
        SVGDocument doc = loader.load(
                new ByteArrayInputStream(svgBytes),
                null,
                LoaderContext.createDefault());
        if (doc == null) return;
        var size = doc.size();
        int w = Math.max(1, (int) size.width);
        int h = Math.max(1, (int) size.height);
        var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        doc.render(null, g2);
        g2.dispose();
    }
}

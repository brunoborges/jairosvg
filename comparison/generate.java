///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS io.brunoborges:jairosvg:1.0.3-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4
//DEPS com.github.weisj:jsvg:2.0.0

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import io.brunoborges.jairosvg.JairoSVG;
import io.sf.carte.echosvg.transcoder.TranscoderInput;
import io.sf.carte.echosvg.transcoder.TranscoderOutput;
import io.sf.carte.echosvg.transcoder.image.PNGTranscoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.imageio.ImageIO;

public class generate {

    static final Path BASE_DIR = Path.of("comparison");
    static final Path SVG_DIR = BASE_DIR.resolve("svg");
    static final Path PNG_JAIRO_DIR = BASE_DIR.resolve("png/jairosvg");
    static final Path PNG_ECHO_DIR = BASE_DIR.resolve("png/echosvg");
    static final Path PNG_CAIRO_DIR = BASE_DIR.resolve("png/cairosvg");
    static final Path PNG_JSVG_DIR = BASE_DIR.resolve("png/jsvg");

    static byte[] renderWithEchoSVG(String svg) throws Exception {
        var transcoder = new PNGTranscoder();
        var input = new TranscoderInput(new StringReader(svg));
        var baos = new ByteArrayOutputStream();
        transcoder.transcode(input, new TranscoderOutput(baos));
        return baos.toByteArray();
    }

    static byte[] renderWithJSVG(String svg) throws Exception {
        SVGLoader loader = new SVGLoader();
        SVGDocument doc = loader.load(
                new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)),
                null,
                LoaderContext.createDefault());
        if (doc == null) throw new RuntimeException("jsvg returned null document");
        var size = doc.size();
        int w = Math.max(1, (int) size.width);
        int h = Math.max(1, (int) size.height);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        doc.render(null, g);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    static byte[] renderWithCairoSVG(Path svgPath, Path pngPath) throws Exception {
        Process process = new ProcessBuilder(
                "python3", "-m", "cairosvg",
                svgPath.toString(),
                "-f", "png",
                "-o", pngPath.toString()
        ).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("python3 -m cairosvg exited with code " + exit
                    + (output.isBlank() ? "" : ": " + output.strip()));
        }
        return Files.readAllBytes(pngPath);
    }

    public static void main(String[] args) throws Exception {
        // Discover SVG test cases from comparison/svg/ directory
        List<Path> svgFiles;
        try (Stream<Path> paths = Files.list(SVG_DIR)) {
            svgFiles = paths
                    .filter(p -> p.toString().endsWith(".svg"))
                    .sorted()
                    .toList();
        }

        if (svgFiles.isEmpty()) {
            System.err.println("No SVG files found in " + SVG_DIR);
            System.exit(1);
        }

        Files.createDirectories(PNG_JAIRO_DIR);
        Files.createDirectories(PNG_ECHO_DIR);
        Files.createDirectories(PNG_CAIRO_DIR);
        Files.createDirectories(PNG_JSVG_DIR);

        System.out.println("=".repeat(72));
        System.out.println("  SVG Comparison Generator — JairoSVG vs EchoSVG vs CairoSVG vs JSVG");
        System.out.printf("  Test cases: %d%n", svgFiles.size());
        System.out.println("=".repeat(72));
        System.out.println();

        // Track results: name -> [jairo ok, echo ok, cairo ok, jsvg ok]
        var results = new LinkedHashMap<String, boolean[]>();

        for (var svgPath : svgFiles) {
            String name = svgPath.getFileName().toString().replace(".svg", "");
            String svg = Files.readString(svgPath, StandardCharsets.UTF_8);

            System.out.println("▸ " + name);

            boolean jairoOk = false;
            boolean echoOk = false;
            boolean cairoOk = false;
            boolean jsvgOk = false;

            // Render with JairoSVG
            try {
                byte[] png = JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
                Path out = PNG_JAIRO_DIR.resolve(name + ".png");
                Files.write(out, png);
                System.out.printf("    JairoSVG   → %s (%,d bytes)%n", out, png.length);
                jairoOk = true;
            } catch (Exception e) {
                System.out.println("    JairoSVG   ✗ FAILED: " + e.getMessage());
            }

            // Render with EchoSVG
            try {
                byte[] png = renderWithEchoSVG(svg);
                Path out = PNG_ECHO_DIR.resolve(name + ".png");
                Files.write(out, png);
                System.out.printf("    EchoSVG    → %s (%,d bytes)%n", out, png.length);
                echoOk = true;
            } catch (Exception e) {
                System.out.println("    EchoSVG    ✗ FAILED: " + e.getMessage());
            }

            // Render with CairoSVG
            try {
                Path out = PNG_CAIRO_DIR.resolve(name + ".png");
                byte[] png = renderWithCairoSVG(svgPath, out);
                System.out.printf("    CairoSVG   → %s (%,d bytes)%n", out, png.length);
                cairoOk = true;
            } catch (Exception e) {
                System.out.println("    CairoSVG   ✗ FAILED: " + e.getMessage());
            }

            // Render with JSVG
            try {
                byte[] png = renderWithJSVG(svg);
                Path out = PNG_JSVG_DIR.resolve(name + ".png");
                Files.write(out, png);
                System.out.printf("    JSVG       → %s (%,d bytes)%n", out, png.length);
                jsvgOk = true;
            } catch (Exception e) {
                System.out.println("    JSVG       ✗ FAILED: " + e.getMessage());
            }

            results.put(name, new boolean[]{jairoOk, echoOk, cairoOk, jsvgOk});
            System.out.println();
        }

        // Summary table
        System.out.println("=".repeat(80));
        System.out.println("  SUMMARY");
        System.out.println("=".repeat(80));
        System.out.printf("  %-28s  %-10s  %-10s  %-10s  %-10s%n", "Test Case", "JairoSVG", "EchoSVG", "CairoSVG", "JSVG");
        System.out.println("  " + "-".repeat(72));

        int jairoPass = 0, echoPass = 0, cairoPass = 0, jsvgPass = 0;
        for (var entry : results.entrySet()) {
            boolean[] r = entry.getValue();
            String jStatus = r[0] ? "✓ OK" : "✗ FAIL";
            String eStatus = r[1] ? "✓ OK" : "✗ FAIL";
            String cStatus = r[2] ? "✓ OK" : "✗ FAIL";
            String jsvgStatus = r[3] ? "✓ OK" : "✗ FAIL";
            System.out.printf("  %-28s  %-10s  %-10s  %-10s  %-10s%n", entry.getKey(), jStatus, eStatus, cStatus, jsvgStatus);
            if (r[0]) jairoPass++;
            if (r[1]) echoPass++;
            if (r[2]) cairoPass++;
            if (r[3]) jsvgPass++;
        }

        System.out.println("  " + "-".repeat(72));
        System.out.printf("  %-28s  %d/%d        %d/%d        %d/%d        %d/%d%n", "TOTAL",
                jairoPass, results.size(), echoPass, results.size(), cairoPass, results.size(), jsvgPass, results.size());
        System.out.println("=".repeat(80));
    }
}

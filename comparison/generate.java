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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

        // Regenerate the Visual Rendering Comparison section of README.md
        generateReadme(svgFiles, results);
    }

    static final Map<String, String> SVG_TITLES = Map.ofEntries(
            Map.entry("01_basic_shapes",    "01 — Basic Shapes"),
            Map.entry("02_gradients",       "02 — Gradients"),
            Map.entry("03_complex_paths",   "03 — Complex Paths"),
            Map.entry("04_text_rendering",  "04 — Text Rendering"),
            Map.entry("05_transforms",      "05 — Transforms"),
            Map.entry("06_stroke_styles",   "06 — Stroke Styles"),
            Map.entry("07_opacity_blend",   "07 — Opacity & Blending"),
            Map.entry("08_viewbox_aspect",  "08 — ViewBox & Aspect Ratio"),
            Map.entry("09_css_styling",     "09 — CSS Styling"),
            Map.entry("10_use_and_defs",    "10 — Use & Defs"),
            Map.entry("11_star_polygon",    "11 — Star Polygon"),
            Map.entry("12_nested_svg",      "12 — Nested SVG"),
            Map.entry("13_patterns",        "13 — Patterns"),
            Map.entry("14_clip_paths",      "14 — Clip Paths"),
            Map.entry("15_masks",           "15 — Masks"),
            Map.entry("16_markers",         "16 — Markers"),
            Map.entry("17_filters",         "17 — Filters"),
            Map.entry("18_embedded_image",  "18 — Embedded Images"),
            Map.entry("19_text_advanced",   "19 — Advanced Text"),
            Map.entry("20_fe_blend_modes",  "20 — feBlend Modes")
    );

    static final Map<String, String> SVG_DESCRIPTIONS = Map.ofEntries(
            Map.entry("01_basic_shapes",    "Rectangles, circles, ellipses, and lines with solid fills and strokes."),
            Map.entry("02_gradients",       "Linear and radial gradients with color stops and spread methods."),
            Map.entry("03_complex_paths",   "Cubic/quadratic Bézier curves, arcs, and complex path commands."),
            Map.entry("04_text_rendering",  "Text rendering with different fonts, sizes, weights, and tspan."),
            Map.entry("05_transforms",      "Translate, rotate, scale, skewX, and nested group transforms."),
            Map.entry("06_stroke_styles",   "Dash arrays, line caps (butt/round/square), and line joins."),
            Map.entry("07_opacity_blend",   "Fill opacity, stroke opacity, and layered element opacity."),
            Map.entry("08_viewbox_aspect",  "viewBox scaling with different preserveAspectRatio values."),
            Map.entry("09_css_styling",     "CSS `<style>` block with class and ID selectors."),
            Map.entry("10_use_and_defs",    "`<use>` element references, `<clipPath>`, and `<defs>` reuse."),
            Map.entry("11_star_polygon",    "Complex star polygon with fill-rule evenodd."),
            Map.entry("12_nested_svg",      "Nested `<svg>` elements with independent viewports."),
            Map.entry("13_patterns",        "Tiled pattern fills: dots, cross-hatch stripes, and grid lines."),
            Map.entry("14_clip_paths",      "Star and text clip paths applied to gradient fills."),
            Map.entry("15_masks",           "Horizontal, vertical, and circular gradient masks with luminance blending."),
            Map.entry("16_markers",         "Arrow, dot, and square markers on lines, polylines, and curves."),
            Map.entry("17_filters",         "Gaussian blur and drop-shadow filters on shapes and text."),
            Map.entry("18_embedded_image",  "Base64-encoded PNG images with clipping, transforms, and opacity."),
            Map.entry("19_text_advanced",   "Multi-span text (tspan), text-decoration, textPath on curves, and rotated text."),
            Map.entry("20_fe_blend_modes",  "feBlend modes: normal, multiply, screen, darken, and lighten.")
    );

    static void generateReadme(List<Path> svgFiles, Map<String, boolean[]> results) throws Exception {
        Path readmePath = BASE_DIR.resolve("README.md");
        if (!Files.exists(readmePath)) {
            System.out.println("README.md not found, skipping regeneration.");
            return;
        }
        String readme = Files.readString(readmePath, StandardCharsets.UTF_8);

        // Build the new visual comparison section content
        var sb = new StringBuilder();
        sb.append("## Visual Rendering Comparison\n\n");
        sb.append("Side-by-side visual comparison of ").append(svgFiles.size())
          .append(" SVG test cases across all four libraries.\n");

        for (var svgPath : svgFiles) {
            String name = svgPath.getFileName().toString().replace(".svg", "");
            String title = SVG_TITLES.getOrDefault(name, name);
            String desc  = SVG_DESCRIPTIONS.getOrDefault(name, "");

            sb.append("\n### ").append(title).append("\n\n");
            if (!desc.isEmpty()) sb.append(desc).append("\n\n");

            sb.append("| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |\n");
            sb.append("| :-------: | :------: | :-----: | :------: | :--: |\n");

            String jairo = Files.exists(PNG_JAIRO_DIR.resolve(name + ".png"))
                    ? "![JairoSVG](png/jairosvg/" + name + ".png)" : "—";
            String echo  = Files.exists(PNG_ECHO_DIR.resolve(name + ".png"))
                    ? "![EchoSVG](png/echosvg/" + name + ".png)" : "—";
            String cairo = Files.exists(PNG_CAIRO_DIR.resolve(name + ".png"))
                    ? "![CairoSVG](png/cairosvg/" + name + ".png)" : "—";
            String jsvg  = Files.exists(PNG_JSVG_DIR.resolve(name + ".png"))
                    ? "![JSVG](png/jsvg/" + name + ".png)" : "—";

            sb.append("| [SVG](svg/").append(name).append(".svg) | ")
              .append(jairo).append(" | ")
              .append(echo).append(" | ")
              .append(cairo).append(" | ")
              .append(jsvg).append(" |\n");
        }

        // Replace the Visual Rendering Comparison section in the README
        // Section starts at "## Visual Rendering Comparison" and ends at the next "---" separator
        String marker = "## Visual Rendering Comparison";
        String endMarker = "\n---\n";
        int start = readme.indexOf(marker);
        if (start < 0) {
            System.out.println("Could not find '## Visual Rendering Comparison' in README.md, skipping.");
            return;
        }
        int end = readme.indexOf(endMarker, start);
        if (end < 0) {
            System.out.println("Could not find section end in README.md, skipping.");
            return;
        }

        String newReadme = readme.substring(0, start) + sb + readme.substring(end);
        Files.writeString(readmePath, newReadme, StandardCharsets.UTF_8);
        System.out.println("\nREADME.md visual comparison section regenerated → " + readmePath);
    }
}

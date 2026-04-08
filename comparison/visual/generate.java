///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//COMPILE_OPTIONS --release 25
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS io.brunoborges:jairosvg:1.0.10-SNAPSHOT-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4
//DEPS com.github.weisj:jsvg:2.0.0
//DEPS com.google.code.gson:gson:2.13.1

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.*;
import javax.imageio.ImageIO;

public class generate {

    static final Path BASE_DIR = Path.of("comparison/visual");
    static final Path SVG_DIR = Path.of("comparison/svg");
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

        // Generate the size comparison chart SVG and render to PNG
        generateSizeComparison(svgFiles);
    }

    static final Map<String, String> SVG_DESCRIPTIONS = Map.ofEntries(
            Map.entry("01_basic_shapes",       "Rectangles, circles, ellipses, and lines with solid fills and strokes."),
            Map.entry("02_gradients",          "Linear and radial gradients with color stops and spread methods."),
            Map.entry("03_complex_paths",      "Cubic/quadratic Bézier curves, arcs, and complex path commands."),
            Map.entry("04_text_rendering",     "Text rendering with different fonts, sizes, weights, and tspan."),
            Map.entry("05_transforms",         "Translate, rotate, scale, skewX, and nested group transforms."),
            Map.entry("06_stroke_styles",      "Dash arrays, line caps (butt/round/square), and line joins."),
            Map.entry("07_opacity_blend",      "Fill opacity, stroke opacity, and layered element opacity."),
            Map.entry("08_viewbox_aspect",     "viewBox scaling with different preserveAspectRatio values."),
            Map.entry("09_css_styling",        "CSS `<style>` block with class and ID selectors."),
            Map.entry("10_use_and_defs",       "`<use>` element references, `<clipPath>`, and `<defs>` reuse."),
            Map.entry("11_star_polygon",       "Complex star polygon with fill-rule evenodd."),
            Map.entry("12_nested_svg",         "Nested `<svg>` elements with independent viewports."),
            Map.entry("13_patterns",           "Tiled pattern fills: dots, cross-hatch stripes, and grid lines."),
            Map.entry("14_clip_paths",         "Star and text clip paths applied to gradient fills."),
            Map.entry("15_masks",              "Horizontal, vertical, and circular gradient masks with luminance blending."),
            Map.entry("16_markers",            "Arrow, dot, and square markers on lines, polylines, and curves."),
            Map.entry("17_filters",            "Gaussian blur and drop-shadow filters on shapes and text."),
            Map.entry("18_embedded_image",     "Base64-encoded PNG images with clipping, transforms, and opacity."),
            Map.entry("19_text_advanced",      "Multi-span text (tspan), text-decoration, textPath on curves, and rotated text."),
            Map.entry("20_fe_blend_modes",     "feBlend modes: normal, multiply, screen, darken, and lighten."),
            Map.entry("21_fe_tile",            "`feTile` filter primitive: repeating input across the filter region."),
            Map.entry("22_feimage_data_uri",   "`feImage` with data-URI PNG source."),
            Map.entry("23_feimage_inline_ref", "`feImage` referencing an inline SVG element by fragment ID."),
            Map.entry("24_localized_masks",    "Masks with localized coordinate systems and gradient fills."),
            Map.entry("25_svg_fonts",          "Custom SVG font with glyph paths and missing-glyph fallback."),
            Map.entry("26_symbol_use",         "Reusable `<symbol>` elements instantiated with `<use>` at different sizes and positions."),
            Map.entry("27_switch_features",    "`<switch>` element with requiredFeatures and systemLanguage conditional rendering."),
            Map.entry("28_css_variables",      "CSS custom properties with `var()` function and fallback values."),
            Map.entry("29_current_color",      "`currentColor` keyword for fill, stroke, and gradient stops with nested inheritance."),
            Map.entry("30_display_visibility", "`display:none` vs `visibility:hidden` behavior, group suppression, and child override."),
            Map.entry("31_nested_overflow",    "Nested `<svg>` elements with `overflow` values: hidden, scroll, visible, and auto."),
            Map.entry("32_stroke_advanced",    "`stroke-dashoffset` phase shifting and `stroke-miterlimit` miter-to-bevel fallback."),
            Map.entry("33_pattern_transforms", "`patternTransform` with scale, rotate, translate, and combined transforms."),
            Map.entry("34_gradient_advanced",  "Gradient `spreadMethod` (reflect/repeat/pad), `fx`/`fy` focus, `href` inheritance, and `userSpaceOnUse`."),
            Map.entry("35_filter_merge_offset", "`feMerge` for compositing layers and `feOffset` for position shifting with shadow effects."),
            Map.entry("36_fe_color_matrix",    "`feColorMatrix` with type matrix, saturate, hueRotate, and luminanceToAlpha."),
            Map.entry("37_fe_morphology",      "`feMorphology` erode and dilate operators on text, shapes, and circles."),
            Map.entry("38_fe_turbulence",      "`feTurbulence` fractalNoise and turbulence types with varying frequency and octaves."),
            Map.entry("39_fe_displacement_map", "`feDisplacementMap` distortion using a turbulence displacement source."),
            Map.entry("40_fe_lighting",        "`feDiffuseLighting` and `feSpecularLighting` with distant and point light sources."),
            Map.entry("41_fe_convolve_matrix", "`feConvolveMatrix` convolution effects: emboss, edge detection, sharpen, and box blur."),
            Map.entry("42_fe_component_transfer", "`feComponentTransfer` with gamma, discrete, linear, and table transfer functions.")
    );

    static final Path COMPARISON_PATH = Path.of("comparison", "COMPARISON.md");
    static final Path JSONL_PATH = Path.of("comparison", "benchmark", "benchmark-results.jsonl");
    static final List<String> BENCH_ENGINES = List.of("jairosvg", "echosvg", "jsvg", "cairosvg");

    record BenchResult(double median) {}

    static Map<String, Map<String, BenchResult>> loadBenchData() {
        var data = new LinkedHashMap<String, Map<String, BenchResult>>();
        if (!Files.exists(JSONL_PATH)) return data;
        try {
            var gson = new Gson();
            for (String line : Files.readAllLines(JSONL_PATH)) {
                line = line.strip();
                if (line.isEmpty()) continue;
                JsonObject obj = gson.fromJson(line, JsonObject.class);
                String engine = obj.has("engine") ? obj.get("engine").getAsString() : null;
                if (engine == null || !BENCH_ENGINES.contains(engine)) continue;
                String caseName = obj.has("case") ? obj.get("case").getAsString() : null;
                if (caseName == null || !obj.has("median")) continue;
                data.computeIfAbsent(caseName, _ -> new LinkedHashMap<>())
                        .put(engine, new BenchResult(obj.get("median").getAsDouble()));
            }
        } catch (IOException e) {
            System.err.println("Warning: could not read " + JSONL_PATH + ": " + e.getMessage());
        }
        return data;
    }

    static String benchCell(Double val, double minVal) {
        if (val == null) return "—";
        String formatted = "%.4f ms".formatted(val);
        return val == minVal ? "**%s** ✅".formatted(formatted) : formatted;
    }

    static String timeRow(String name, Map<String, Map<String, BenchResult>> benchData) {
        String label = name.replaceFirst("^\\d+_", "").replace('_', ' ');
        label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        var caseData = benchData.get(label);
        Double[] vals = new Double[4];
        if (caseData != null) {
            String[] keys = {"jairosvg", "echosvg", "cairosvg", "jsvg"};
            for (int i = 0; i < 4; i++) {
                var r = caseData.get(keys[i]);
                vals[i] = r != null ? r.median() : null;
            }
        }
        double minVal = Arrays.stream(vals).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).min().orElse(Double.MAX_VALUE);
        return "| **Time** | %s | %s | %s | %s |".formatted(
                benchCell(vals[0], minVal), benchCell(vals[1], minVal),
                benchCell(vals[2], minVal), benchCell(vals[3], minVal));
    }

    static void generateReadme(List<Path> svgFiles, Map<String, boolean[]> results) throws Exception {
        var benchData = loadBenchData();

        var sb = new StringBuilder();
        sb.append("# SVG Rendering Comparison\n\n");
        sb.append("Side-by-side rendering, file sizes, and benchmark times for ").append(svgFiles.size())
          .append(" SVG test cases across all four libraries.\n\n");
        sb.append("> **Note:** The **Input SVG** column is rendered live by your browser's built-in SVG engine.")
          .append(" Use it as a reference to compare each library's PNG output against what a modern browser produces.\n");

        for (var svgPath : svgFiles) {
            String name = svgPath.getFileName().toString().replace(".svg", "");
            String desc  = SVG_DESCRIPTIONS.getOrDefault(name, "");

            sb.append("\n### ").append(name).append("\n\n");
            if (!desc.isEmpty()) sb.append(desc).append("\n\n");

            sb.append("| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |\n");
            sb.append("| :-------: | :------: | :-----: | :------: | :--: |\n");

            String jairo = Files.exists(PNG_JAIRO_DIR.resolve(name + ".png"))
                    ? "![JairoSVG](visual/png/jairosvg/" + name + ".png)" : "—";
            String echo  = Files.exists(PNG_ECHO_DIR.resolve(name + ".png"))
                    ? "![EchoSVG](visual/png/echosvg/" + name + ".png)" : "—";
            String cairo = Files.exists(PNG_CAIRO_DIR.resolve(name + ".png"))
                    ? "![CairoSVG](visual/png/cairosvg/" + name + ".png)" : "—";
            String jsvg  = Files.exists(PNG_JSVG_DIR.resolve(name + ".png"))
                    ? "![JSVG](visual/png/jsvg/" + name + ".png)" : "—";

            sb.append("| ![SVG](svg/").append(name).append(".svg) | ")
              .append(jairo).append(" | ")
              .append(echo).append(" | ")
              .append(cairo).append(" | ")
              .append(jsvg).append(" |\n");

            // File size row
            long sJairo = fileSize(PNG_JAIRO_DIR.resolve(name + ".png"));
            long sEcho  = fileSize(PNG_ECHO_DIR.resolve(name + ".png"));
            long sCairo = fileSize(PNG_CAIRO_DIR.resolve(name + ".png"));
            long sJsvg  = fileSize(PNG_JSVG_DIR.resolve(name + ".png"));
            long minSize = LongStream.of(sJairo, sEcho, sCairo, sJsvg)
                    .filter(s -> s > 0).min().orElse(0);
            sb.append("| **Size** | ")
              .append(sizeCell(sJairo, minSize)).append(" | ")
              .append(sizeCell(sEcho, minSize)).append(" | ")
              .append(sizeCell(sCairo, minSize)).append(" | ")
              .append(sizeCell(sJsvg, minSize)).append(" |\n");

            // Benchmark time row
            sb.append(timeRow(name, benchData)).append("\n");
        }

        // Aggregate sections wrapped in markers
        sb.append("\n---\n\n");

        sb.append("""
                > **⚠️ Filters/Masks caveat:** CairoSVG supports only three SVG filter primitives \
                (`feBlend`, `feFlood`, `feOffset`) — all others (`feGaussianBlur`, `feDropShadow`, \
                `feTurbulence`, `feDisplacementMap`, `feLighting`, `feColorMatrix`, `feMorphology`, \
                `feConvolveMatrix`, `feComponentTransfer`, `feComposite`, `feMerge`, `feImage`, `feTile`) \
                are silently skipped. Masks are also rendered incorrectly (missing gradient and shape content). \
                For those tests, CairoSVG appears faster and produces smaller files because it skips the \
                rendering work. JairoSVG and JSVG perform the actual computation. Note: JairoSVG \
                **outperforms CairoSVG on both Masks and feBlend modes** despite rendering them correctly. \
                CairoSVG also **crashes** on CSS custom properties (`var()`) — test 28 produces no output.

                """);

        sb.append("<!-- BEGIN:BENCHMARK_NOTE -->\n");
        sb.append("> **Note:** Benchmarks were run with 50 warm-up iterations and 500 measured iterations");
        sb.append(" per SVG file. Median time reported. Results may vary by hardware and SVG complexity.\n");
        sb.append("<!-- END:BENCHMARK_NOTE -->\n\n");

        sb.append("""
                #### Default Rendering Settings: JairoSVG vs JSVG

                Both JairoSVG and JSVG use Java2D as their rendering backend, but they ship with **different default quality settings**, which directly affects benchmark performance:

                | Setting                       | JairoSVG default             | JSVG default (out-of-the-box)          | Performance impact |
                | ----------------------------- | ---------------------------- | -------------------------------------- | :----------------: |
                | `KEY_ANTIALIASING`            | `VALUE_ANTIALIAS_ON`         | `VALUE_ANTIALIAS_ON` (auto-set)        |        Low         |
                | `KEY_TEXT_ANTIALIASING`       | Not set (platform default)   | Not set (platform default)             |        Low         |
                | `KEY_RENDERING`               | Not set (defaults to speed)  | Not set (defaults to speed)            |      **High**      |
                | `KEY_STROKE_CONTROL`          | `VALUE_STROKE_PURE`          | `VALUE_STROKE_PURE` (auto-set)         |       Equal        |
                | `KEY_FRACTIONALMETRICS`       | Not set (defaults to `OFF`)  | Not set (defaults to `OFF`)            |       Medium       |
                | **PNG compression level**     | 6 (matches CairoSVG/libpng) | N/A (no built-in PNG; user uses `ImageIO`) |       Medium       |

                JSVG automatically sets `KEY_ANTIALIASING` and `KEY_STROKE_CONTROL` to the values above when they are at their defaults. JairoSVG now uses the same defaults as JSVG, so both renderers operate with identical quality settings out of the box. Users can customize any hint via `JairoSVG.builder().renderingHint(key, value)`.

                **In the benchmark**, both JairoSVG and JSVG use identical rendering hints, so the comparison measures SVG engine efficiency directly.

                """);

        sb.append("""
                > **⚠️ Filters/Masks:** Where CairoSVG produces much smaller output, it is because CairoSVG \
                **does not implement** most SVG filter primitives — only `feBlend`, `feFlood`, and `feOffset` \
                are supported; all others are silently skipped. Masks are also rendered without gradient/shape \
                content. This results in simpler images that compress better. JairoSVG and JSVG render these \
                effects correctly, producing visually accurate but larger PNGs.

                ## Running the Benchmark

                Prerequisites: [JBang], Java 25+, Python 3 with CairoSVG (`pip install cairosvg`), \
                JairoSVG installed in local Maven repo.

                ```bash
                ./mvnw install -DskipTests
                jbang comparison/benchmark/benchmark.java
                ```

                Options:

                ```bash
                # Run specific SVG categories only
                jbang comparison/benchmark/benchmark.java filters embedded

                # Skip engines
                jbang comparison/benchmark/benchmark.java --no-cairosvg
                jbang comparison/benchmark/benchmark.java --no-echosvg
                jbang comparison/benchmark/benchmark.java --no-jsvg

                # Disable progress bar output (useful for CI logs)
                jbang comparison/benchmark/benchmark.java --no-progress

                # Adjust warmup and measurement iterations (defaults: 50 and 500)
                jbang comparison/benchmark/benchmark.java --warmup=5 --iterations=100
                ```

                The benchmark writes results to `benchmark/benchmark-results.jsonl` (JSON lines) and logs \
                to a timestamped file. The `update_readme.java` script reads the JSONL and PNG files to \
                regenerate both the timing table and PNG file sizes table in this document.

                <!-- Link references -->

                [JBang]: https://www.jbang.dev/
                """);

        Files.writeString(COMPARISON_PATH, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("\nComparison document regenerated → " + COMPARISON_PATH);
    }

    static void generateSizeComparison(List<Path> svgFiles) throws Exception {
        record CaseSize(String label, long jairo, long echo, long jsvg, long cairo) {
            long max() { return Math.max(Math.max(jairo, echo), Math.max(jsvg, cairo)); }
            long min() {
                long m = Long.MAX_VALUE;
                if (jairo > 0) m = Math.min(m, jairo);
                if (echo > 0) m = Math.min(m, echo);
                if (jsvg > 0) m = Math.min(m, jsvg);
                if (cairo > 0) m = Math.min(m, cairo);
                return m == Long.MAX_VALUE ? 0 : m;
            }
        }

        var cases = new ArrayList<CaseSize>();
        for (var svgPath : svgFiles) {
            String name = svgPath.getFileName().toString().replace(".svg", "");
            String label = name.replaceFirst("^\\d+_", "").replace('_', ' ');
            label = Character.toUpperCase(label.charAt(0)) + label.substring(1);

            long jairo = fileSize(PNG_JAIRO_DIR.resolve(name + ".png"));
            long echo = fileSize(PNG_ECHO_DIR.resolve(name + ".png"));
            long jsvg = fileSize(PNG_JSVG_DIR.resolve(name + ".png"));
            long cairo = fileSize(PNG_CAIRO_DIR.resolve(name + ".png"));

            if (jairo > 0 || echo > 0 || jsvg > 0 || cairo > 0) {
                cases.add(new CaseSize(label, jairo, echo, jsvg, cairo));
            }
        }

        if (cases.isEmpty()) {
            System.out.println("No PNG files found for size comparison chart");
            return;
        }

        long globalMax = cases.stream().mapToLong(CaseSize::max).max().orElse(1);
        var fmt = NumberFormat.getIntegerInstance(Locale.US);

        int rowHeight = 70;
        int headerHeight = 102;
        int svgHeight = headerHeight + cases.size() * rowHeight + 20;
        int svgWidth = 760;
        int barStart = 160;
        int barMaxWidth = 500;

        var svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n"
                .formatted(svgWidth, svgHeight, svgWidth, svgHeight));
        svg.append("  <style>\n");
        svg.append("    text { font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Helvetica, Arial, sans-serif; }\n");
        svg.append("  </style>\n");
        svg.append("  <rect width=\"%d\" height=\"%d\" rx=\"16\" fill=\"#0f172a\"/>\n".formatted(svgWidth, svgHeight));
        svg.append("\n");

        // Title
        svg.append("  <text x=\"380\" y=\"40\" text-anchor=\"middle\" font-size=\"20\" font-weight=\"600\" font-style=\"italic\" fill=\"#f1f5f9\">PNG Output File Sizes (bytes)</text>\n");
        svg.append("  <text x=\"380\" y=\"60\" text-anchor=\"middle\" font-size=\"13\" font-style=\"italic\" fill=\"#64748b\">lower is better — JairoSVG vs EchoSVG vs JSVG vs CairoSVG</text>\n");
        svg.append("\n");

        // Legend
        String[][] legend = {
            {"60",  "#4ade80", "JairoSVG (Java)"},
            {"220", "#f87171", "EchoSVG (Java)"},
            {"390", "#60a5fa", "JSVG (Java)"},
            {"540", "#fb923c", "CairoSVG (Python)"},
        };
        for (var l : legend) {
            svg.append("  <rect x=\"%s\" y=\"75\" width=\"12\" height=\"12\" rx=\"3\" fill=\"%s\"/>\n".formatted(l[0], l[1]));
            svg.append("  <text x=\"%d\" y=\"86\" font-size=\"12\" fill=\"#94a3b8\">%s</text>\n"
                    .formatted(Integer.parseInt(l[0]) + 18, l[2]));
        }
        svg.append("\n");
        svg.append("  <line x1=\"30\" y1=\"%d\" x2=\"730\" y2=\"%d\" stroke=\"#1e293b\" stroke-width=\"1\"/>\n"
                .formatted(headerHeight, headerHeight));
        svg.append("\n");

        // Bars
        String[] colors = {"#4ade80", "#f87171", "#60a5fa", "#fb923c"};
        int barHeight = 12;
        int barGap = 13;

        for (int i = 0; i < cases.size(); i++) {
            var c = cases.get(i);
            int groupY = headerHeight + i * rowHeight + 10;
            long minSize = c.min();
            long[] sizes = {c.jairo, c.echo, c.jsvg, c.cairo};

            // Label
            int labelY = groupY + 2 * barGap + 2;
            svg.append("  <text x=\"152\" y=\"%d\" text-anchor=\"end\" font-size=\"11\" font-weight=\"500\" fill=\"#94a3b8\">%s</text>\n"
                    .formatted(labelY, c.label));

            for (int j = 0; j < 4; j++) {
                long size = sizes[j];
                if (size <= 0) continue;
                int barY = groupY + j * barGap;
                int barW = Math.max(2, (int) (size * barMaxWidth / globalMax));
                int textY = barY + 10;

                svg.append("  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"%s\"/>\n"
                        .formatted(barStart, barY, barW, barHeight, colors[j]));
                svg.append("  <text x=\"700\" y=\"%d\" text-anchor=\"end\" font-size=\"10\" fill=\"%s\">%s</text>\n"
                        .formatted(textY, colors[j], fmt.format(size)));

                if (size == minSize) {
                    svg.append("  <text x=\"720\" y=\"%d\" text-anchor=\"middle\" font-size=\"10\" fill=\"%s\">◄</text>\n"
                            .formatted(textY, colors[j]));
                }
            }
            svg.append("\n");
        }

        svg.append("</svg>\n");

        // Write SVG
        Path svgOut = BASE_DIR.resolve("size-comparison.svg");
        Files.writeString(svgOut, svg.toString(), StandardCharsets.UTF_8);
        System.out.println("Size comparison chart → " + svgOut);

        // Render to PNG using JairoSVG
        byte[] pngBytes = JairoSVG.svg2png(svg.toString().getBytes(StandardCharsets.UTF_8));
        Path pngOut = BASE_DIR.resolve("size-comparison.png");
        Files.write(pngOut, pngBytes);
        System.out.printf("Size comparison PNG   → %s (%,d bytes)%n", pngOut, pngBytes.length);
    }

    static long fileSize(Path path) {
        try { return Files.exists(path) ? Files.size(path) : 0; }
        catch (IOException e) { return 0; }
    }

    static String sizeCell(long size, long minSize) {
        if (size <= 0) return "—";
        String formatted = NumberFormat.getIntegerInstance(Locale.US).format(size) + " bytes";
        return size == minSize ? formatted + " ✅" : formatted;
    }
}

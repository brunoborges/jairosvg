///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview -XX:MaxRAMPercentage=75.0 -XX:CompileThreshold=500
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS io.brunoborges:jairosvg:1.0.7-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4
//DEPS com.github.weisj:jsvg:2.0.0
//DEPS me.tongfei:progressbar:0.10.2
//DEPS com.google.code.gson:gson:2.13.1
//SOURCES update_readme.java

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.brunoborges.jairosvg.JairoSVG;
import io.sf.carte.echosvg.transcoder.TranscoderInput;
import io.sf.carte.echosvg.transcoder.TranscoderOutput;
import io.sf.carte.echosvg.transcoder.image.PNGTranscoder;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class benchmark {

    static class TeeOutputStream extends OutputStream {
        private final OutputStream primary;
        private final OutputStream secondary;

        TeeOutputStream(OutputStream primary, OutputStream secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override public void write(int b) throws IOException { primary.write(b); secondary.write(b); }
        @Override public void write(byte[] b, int off, int len) throws IOException { primary.write(b, off, len); secondary.write(b, off, len); }
        @Override public void flush() throws IOException { primary.flush(); secondary.flush(); }
        @Override public void close() throws IOException { try { primary.close(); } finally { secondary.close(); } }
    }

    static int WARMUP = 50;
    static int ITERATIONS = 500;

    static final Path SVG_DIR = Path.of("comparison", "svg");
    static final Path JSONL_FILE = Path.of("comparison", "benchmark", "benchmark-results.jsonl");

    static final Gson GSON = new Gson();

    record SvgCase(String name, String content, byte[] contentBytes) {}

    /** Append a JSON line to the results file. */
    static void emitJson(String engine, String caseName, double[] times) throws IOException {
        double avg = Arrays.stream(times).average().orElse(0);
        double median = times[times.length / 2];
        double p95 = times[(int) (times.length * 0.95)];
        double min = times[0];
        var obj = new JsonObject();
        obj.addProperty("engine", engine);
        obj.addProperty("case", caseName);
        obj.addProperty("avg", Math.round(avg * 10000.0) / 10000.0);
        obj.addProperty("median", Math.round(median * 10000.0) / 10000.0);
        obj.addProperty("p95", Math.round(p95 * 10000.0) / 10000.0);
        obj.addProperty("min", Math.round(min * 10000.0) / 10000.0);
        Files.writeString(JSONL_FILE, GSON.toJson(obj) + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Append an error JSON line to the results file. */
    static void emitJsonError(String engine, String caseName, String error) throws IOException {
        var obj = new JsonObject();
        obj.addProperty("engine", engine);
        obj.addProperty("case", caseName);
        obj.addProperty("error", error);
        Files.writeString(JSONL_FILE, GSON.toJson(obj) + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    static List<SvgCase> loadSvgCases() throws IOException {
        return Files.list(SVG_DIR)
                .filter(p -> p.toString().endsWith(".svg"))
                .sorted()
                .map(p -> {
                    try {
                        String filename = p.getFileName().toString();
                        // "01_basic_shapes.svg" → "Basic shapes"
                        String label = filename.replaceFirst("^\\d+_", "")
                                .replace(".svg", "")
                                .replace('_', ' ');
                        label = label.substring(0, 1).toUpperCase() + label.substring(1);
                        String content = Files.readString(p);
                        return new SvgCase(label, content, content.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .toList();
    }

    interface SvgConverter {
        byte[] convert(byte[] svgBytes) throws Exception;
    }

    static byte[] echoConvert(byte[] svgBytes) throws Exception {
        var transcoder = new PNGTranscoder();
        var input = new TranscoderInput(new ByteArrayInputStream(svgBytes));
        var baos = new ByteArrayOutputStream();
        transcoder.transcode(input, new TranscoderOutput(baos));
        return baos.toByteArray();
    }

    // PNG writer + compression param cached for JSVG, matching JairoSVG's compression level 6
    private static final ImageWriter JSVG_PNG_WRITER;
    private static final ImageWriteParam JSVG_PNG_PARAM;

    static {
        var writers = ImageIO.getImageWritersByFormatName("PNG");
        if (!writers.hasNext()) throw new RuntimeException("No PNG ImageWriter found");
        JSVG_PNG_WRITER = writers.next();
        JSVG_PNG_PARAM = JSVG_PNG_WRITER.getDefaultWriteParam();
        JSVG_PNG_PARAM.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        // Level 6 → quality 1.0 - (6 / 9.0) ≈ 0.333, matching JairoSVG/CairoSVG default
        JSVG_PNG_PARAM.setCompressionQuality(1.0f - (6 / 9.0f));
    }

    /**
     * JSVG converter with rendering hints and PNG compression normalized to
     * match JairoSVG defaults, so the benchmark compares SVG rendering engines
     * on equal footing rather than measuring quality-setting differences.
     *
     * Hints matched: KEY_ANTIALIASING, KEY_STROKE_CONTROL
     * PNG compression: level 6 (same as JairoSVG/CairoSVG/libpng default)
     */
    static byte[] jsvgConvert(byte[] svgBytes) throws Exception {
        SVGLoader loader = new SVGLoader();
        SVGDocument doc = loader.load(
                new ByteArrayInputStream(svgBytes),
                null,
                LoaderContext.createDefault());
        if (doc == null) throw new RuntimeException("jsvg returned null document");
        var size = doc.size();
        int w = Math.max(1, (int) size.width);
        int h = Math.max(1, (int) size.height);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        // Match JairoSVG's rendering hints for a fair comparison
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        doc.render(null, g);
        g.dispose();
        // Encode PNG with compression level 6, matching JairoSVG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        synchronized (JSVG_PNG_WRITER) {
            try {
                JSVG_PNG_WRITER.setOutput(new MemoryCacheImageOutputStream(baos));
                JSVG_PNG_WRITER.write(null, new IIOImage(image, null, null), JSVG_PNG_PARAM);
            } finally {
                JSVG_PNG_WRITER.reset();
            }
        }
        return baos.toByteArray();
    }

    static void warmup(SvgConverter converter, byte[] svgBytes, int iterations, ProgressBar pb) throws Exception {
        for (int i = 0; i < iterations; i++) {
            byte[] r = converter.convert(svgBytes);
            if (r == null) throw new RuntimeException("null");
            if (pb != null) pb.step();
        }
    }

    static double[] measure(SvgConverter converter, byte[] svgBytes, int iterations, ProgressBar pb) throws Exception {
        double[] times = new double[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            byte[] result = converter.convert(svgBytes);
            long end = System.nanoTime();
            times[i] = (end - start) / 1_000_000.0;
            if (result == null || result.length == 0) throw new RuntimeException("Empty result");
            if (pb != null) pb.step();
        }
        Arrays.sort(times);
        return times;
    }

    static void printStats(String label, double[] times) {
        double avg = Arrays.stream(times).average().orElse(0);
        double median = times[times.length / 2];
        double p95 = times[(int) (times.length * 0.95)];
        double min = times[0];
        System.out.printf("  %-28s  avg=%7.2f ms  median=%7.2f ms  p95=%7.2f ms  min=%7.2f ms%n",
                label, avg, median, p95, min);
    }

    /** Run CairoSVG (Python) benchmark for a given SVG via subprocess. */
    static double[] benchCairoSVG(String svg) throws Exception {
        String pyScript = """
            import time, cairosvg, json, sys
            svg = sys.stdin.read()
            svg_bytes = svg.encode('utf-8')
            warmup = %d
            iters = %d
            for _ in range(warmup):
                cairosvg.svg2png(bytestring=svg_bytes)
            times = []
            for _ in range(iters):
                t0 = time.perf_counter_ns()
                cairosvg.svg2png(bytestring=svg_bytes)
                t1 = time.perf_counter_ns()
                times.append((t1 - t0) / 1_000_000.0)
            times.sort()
            print(json.dumps(times))
            """.formatted(WARMUP, ITERATIONS);

        ProcessBuilder pb = new ProcessBuilder("python3", "-c", pyScript);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.getOutputStream().write(svg.getBytes(StandardCharsets.UTF_8));
        proc.getOutputStream().close();

        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            System.err.println("  CairoSVG error: " + output);
            return null;
        }

        // Parse JSON array of doubles
        double[] times = GSON.fromJson(output, double[].class);
        Arrays.sort(times);
        return times;
    }

    static void printComparison(String nameA, double avgA, String nameB, double avgB) {
        double ratio = avgB / avgA;
        String faster = avgA < avgB ? nameA : nameB;
        double percentFaster = avgA < avgB ? (ratio - 1) * 100 : (1.0 / ratio - 1) * 100;
        System.out.printf("    %s vs %s → %s is %.2f%% faster%n",
                nameA, nameB, faster, percentFaster);
    }

    public static void main(String[] args) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path logFile = Path.of("benchmark-" + timestamp + ".log");
        PrintStream fileOut = new PrintStream(new BufferedOutputStream(Files.newOutputStream(logFile)), true);
        System.setOut(new PrintStream(new TeeOutputStream(System.out, fileOut), true));
        System.out.println("Logging to: " + logFile.toAbsolutePath());

        // Truncate JSONL results file at start of run
        Files.writeString(JSONL_FILE, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        List<SvgCase> allCases = loadSvgCases();

        // Parse args: filter by name substring, --no-cairosvg, --no-echosvg, --no-jsvg,
        //              --no-progress,
        //              --warmup=N, --iterations=N
        Set<String> nameFilters = new LinkedHashSet<>();
        boolean runCairo = true;
        boolean runEcho = true;
        boolean runJsvg = true;
        boolean showProgress = true;
        for (String arg : args) {
            if ("--no-cairosvg".equals(arg)) {
                runCairo = false;
            } else if ("--no-echosvg".equals(arg)) {
                runEcho = false;
            } else if ("--no-jsvg".equals(arg)) {
                runJsvg = false;
            } else if ("--no-progress".equals(arg)) {
                showProgress = false;
            } else if (arg.startsWith("--warmup=")) {
                WARMUP = Integer.parseInt(arg.substring("--warmup=".length()));
            } else if (arg.startsWith("--iterations=")) {
                ITERATIONS = Integer.parseInt(arg.substring("--iterations=".length()));
            } else {
                nameFilters.add(arg.toLowerCase());
            }
        }

        List<SvgCase> cases = nameFilters.isEmpty() ? allCases
                : allCases.stream()
                        .filter(c -> nameFilters.stream()
                                .anyMatch(f -> c.name().toLowerCase().contains(f)))
                        .toList();

        if (cases.isEmpty()) {
            System.err.println("No matching SVG cases. Available:");
            allCases.forEach(c -> System.err.println("  " + c.name()));
            System.exit(1);
        }

        SvgConverter jairosvg = svgBytes -> JairoSVG.svg2png(svgBytes);
        SvgConverter echosvg = svgBytes -> echoConvert(svgBytes);
        SvgConverter jsvg = svgBytes -> jsvgConvert(svgBytes);

        int javaEngines = 1 + (runEcho ? 1 : 0) + (runJsvg ? 1 : 0);

        // ── Phase 1: Collective warmup ──────────────────────────────────────
        // Warm up ALL test cases across ALL Java engines before any measurement.
        // This ensures the JIT C2 compiler profiles all code paths (e.g., every
        // branch in Surface::draw's tag switch) and avoids deoptimization during
        // the measurement phase.
        System.out.println("=".repeat(98));
        System.out.print("  SVG → PNG Benchmark: JairoSVG");
        if (runEcho) System.out.print(" vs EchoSVG");
        if (runJsvg) System.out.print(" vs JSVG");
        if (runCairo) System.out.print(" vs CairoSVG");
        System.out.println();
        System.out.printf("  Warmup: %d iterations × %d cases × %d engines, Measurement: %d iterations%n",
                WARMUP, cases.size(), javaEngines, ITERATIONS);
        System.out.println("=".repeat(98));

        long warmupTotalSteps = (long) javaEngines * cases.size() * WARMUP;
        System.out.println("\n  Warming up all Java engines across all test cases...");

        ProgressBar warmupPb = showProgress
                ? new ProgressBarBuilder()
                        .setTaskName("  Warmup")
                        .setInitialMax(warmupTotalSteps)
                        .setStyle(ProgressBarStyle.ASCII)
                        .setUpdateIntervalMillis(250)
                        .build()
                : null;

        try {
            for (SvgCase c : cases) {
                warmup(jairosvg, c.contentBytes(), WARMUP, warmupPb);
                if (runEcho) warmup(echosvg, c.contentBytes(), WARMUP, warmupPb);
                if (runJsvg) warmup(jsvg, c.contentBytes(), WARMUP, warmupPb);
            }
        } finally {
            if (warmupPb != null) warmupPb.close();
        }

        System.gc();
        Thread.sleep(500);

        // ── Phase 2: Measurement ────────────────────────────────────────────
        System.out.println("\n  Measuring...");

        for (int ci = 0; ci < cases.size(); ci++) {
            SvgCase c = cases.get(ci);
            System.gc();
            Thread.sleep(100);
            System.out.println("\n▸ " + c.name() + "  [" + (ci + 1) + "/" + cases.size() + "]");

            long measureSteps = (long) javaEngines * ITERATIONS + (runCairo ? 1 : 0);

            double[] jTimes, eTimes = null, sTimes = null, cTimes = null;
            ProgressBar pb = showProgress
                    ? new ProgressBarBuilder()
                            .setTaskName("  Progress")
                            .setInitialMax(measureSteps)
                            .setStyle(ProgressBarStyle.ASCII)
                            .setUpdateIntervalMillis(250)
                            .build()
                    : null;

            try {
                jTimes = measure(jairosvg, c.contentBytes(), ITERATIONS, pb);

                if (runEcho) {
                    System.gc(); Thread.sleep(100);
                    eTimes = measure(echosvg, c.contentBytes(), ITERATIONS, pb);
                }

                if (runJsvg) {
                    System.gc(); Thread.sleep(100);
                    sTimes = measure(jsvg, c.contentBytes(), ITERATIONS, pb);
                }

                if (runCairo) {
                    System.gc(); Thread.sleep(100);
                    cTimes = benchCairoSVG(c.content());
                    if (pb != null) pb.step();
                }
            } finally {
                if (pb != null) pb.close();
            }

            // Print results after progress bar is done
            printStats("JairoSVG  (Java/Java2D)", jTimes);
            emitJson("jairosvg", c.name(), jTimes);
            double jAvg = Arrays.stream(jTimes).average().orElse(0);

            double eAvg = 0;
            if (eTimes != null) {
                printStats("EchoSVG   (Java/Batik)", eTimes);
                emitJson("echosvg", c.name(), eTimes);
                eAvg = Arrays.stream(eTimes).average().orElse(0);
            }

            double sAvg = 0;
            if (sTimes != null) {
                printStats("JSVG      (Java/Java2D)", sTimes);
                emitJson("jsvg", c.name(), sTimes);
                sAvg = Arrays.stream(sTimes).average().orElse(0);
            }

            double cAvg = 0;
            if (cTimes != null) {
                printStats("CairoSVG  (Python/Cairo)", cTimes);
                emitJson("cairosvg", c.name(), cTimes);
                cAvg = Arrays.stream(cTimes).average().orElse(0);
            } else if (runCairo) {
                emitJsonError("cairosvg", c.name(), "CairoSVG returned null");
            }

            System.out.println();
            if (eTimes != null) {
                printComparison("JairoSVG", jAvg, "EchoSVG", eAvg);
            }
            if (sTimes != null) {
                printComparison("JairoSVG", jAvg, "JSVG", sAvg);
            }
            if (cTimes != null) {
                printComparison("JairoSVG", jAvg, "CairoSVG", cAvg);
                if (eTimes != null) {
                    printComparison("EchoSVG", eAvg, "CairoSVG", cAvg);
                }
                if (sTimes != null) {
                    printComparison("JSVG", sAvg, "CairoSVG", cAvg);
                }
            }
        }

        System.out.println("\n" + "=".repeat(98));
        System.out.flush();
        fileOut.close();

        // Update README tables from the JSONL results
        System.out.println("\nUpdating README tables...");
        update_readme.main(new String[]{
                "--warmup=" + WARMUP,
                "--iterations=" + ITERATIONS
        });
    }
}

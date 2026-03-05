///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview -Xmx512m
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS io.brunoborges:jairosvg:1.0.3-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4
//DEPS com.github.weisj:jsvg:2.0.0
//DEPS me.tongfei:progressbar:0.10.2

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
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
import java.util.*;
import java.util.stream.*;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class benchmark {

    static int WARMUP = 20;
    static int ITERATIONS = 1000;

    static final Path SVG_DIR = Path.of("comparison/svg");

    record SvgCase(String name, String content) {}

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
                        return new SvgCase(label, Files.readString(p));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .toList();
    }

    interface SvgConverter {
        byte[] convert(String svg) throws Exception;
    }

    static byte[] echoConvert(String svg) throws Exception {
        var transcoder = new PNGTranscoder();
        var input = new TranscoderInput(new StringReader(svg));
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
     * Hints matched: KEY_ANTIALIASING, KEY_TEXT_ANTIALIASING, KEY_RENDERING,
     *                KEY_STROKE_CONTROL, KEY_FRACTIONALMETRICS
     * PNG compression: level 6 (same as JairoSVG/CairoSVG/libpng default)
     */
    static byte[] jsvgConvert(String svg) throws Exception {
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
        // Match JairoSVG's rendering hints for a fair comparison
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
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

    static double[] bench(String label, SvgConverter converter, String svg, ProgressBar pb) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            byte[] r = converter.convert(svg);
            if (r == null) throw new RuntimeException("null");
            pb.step();
        }
        System.gc();
        Thread.sleep(100);

        // Measure
        double[] times = new double[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            byte[] result = converter.convert(svg);
            long end = System.nanoTime();
            times[i] = (end - start) / 1_000_000.0;
            if (result == null || result.length == 0) throw new RuntimeException("Empty result");
            pb.step();
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
        String inner = output.substring(1, output.length() - 1);
        return Arrays.stream(inner.split(",\\s*"))
                     .mapToDouble(Double::parseDouble)
                     .toArray();
    }

    static void printComparison(String nameA, double avgA, String nameB, double avgB) {
        double ratio = avgB / avgA;
        String faster = avgA < avgB ? nameA : nameB;
        System.out.printf("    %s vs %s → %s is %.1fx faster%n",
                nameA, nameB, faster, avgA < avgB ? ratio : 1.0 / ratio);
    }

    public static void main(String[] args) throws Exception {
        List<SvgCase> allCases = loadSvgCases();

        // Parse args: filter by name substring, --no-cairosvg, --no-echosvg, --no-jsvg,
        //              --warmup=N, --iterations=N
        Set<String> nameFilters = new LinkedHashSet<>();
        boolean runCairo = true;
        boolean runEcho = true;
        boolean runJsvg = true;
        for (String arg : args) {
            if ("--no-cairosvg".equals(arg)) {
                runCairo = false;
            } else if ("--no-echosvg".equals(arg)) {
                runEcho = false;
            } else if ("--no-jsvg".equals(arg)) {
                runJsvg = false;
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

        System.out.println("=".repeat(98));
        System.out.print("  SVG → PNG Benchmark: JairoSVG");
        if (runEcho) System.out.print(" vs EchoSVG");
        if (runJsvg) System.out.print(" vs JSVG");
        if (runCairo) System.out.print(" vs CairoSVG");
        System.out.println();
        System.out.printf("  Warmup: %d iterations, Measurement: %d iterations, SVG files: %d%n",
                WARMUP, ITERATIONS, cases.size());
        System.out.println("=".repeat(98));

        SvgConverter jairosvg = svg -> JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        SvgConverter echosvg = svg -> echoConvert(svg);
        SvgConverter jsvg = svg -> jsvgConvert(svg);

        for (int ci = 0; ci < cases.size(); ci++) {
            SvgCase c = cases.get(ci);
            System.gc();
            System.out.println("\n▸ " + c.name() + "  [" + (ci + 1) + "/" + cases.size() + "]");

            // Total steps for Java engines: (warmup + iterations) each
            int javaEngines = 1 + (runEcho ? 1 : 0) + (runJsvg ? 1 : 0);
            int totalSteps = javaEngines * (WARMUP + ITERATIONS) + (runCairo ? 1 : 0);

            double[] jTimes, eTimes = null, sTimes = null, cTimes = null;

            try (var pb = new ProgressBarBuilder()
                    .setTaskName("  Progress")
                    .setInitialMax(totalSteps)
                    .setStyle(ProgressBarStyle.ASCII)
                    .setUpdateIntervalMillis(250)
                    .build()) {

                jTimes = bench("JairoSVG", jairosvg, c.content(), pb);

                if (runEcho) {
                    System.gc(); Thread.sleep(100);
                    eTimes = bench("EchoSVG", echosvg, c.content(), pb);
                }

                if (runJsvg) {
                    System.gc(); Thread.sleep(100);
                    sTimes = bench("JSVG", jsvg, c.content(), pb);
                }

                if (runCairo) {
                    System.gc(); Thread.sleep(100);
                    cTimes = benchCairoSVG(c.content());
                    pb.step();
                }
            }

            // Print results after progress bar is done
            printStats("JairoSVG  (Java/Java2D)", jTimes);
            double jAvg = Arrays.stream(jTimes).average().orElse(0);

            double eAvg = 0;
            if (eTimes != null) {
                printStats("EchoSVG   (Java/Batik)", eTimes);
                eAvg = Arrays.stream(eTimes).average().orElse(0);
            }

            double sAvg = 0;
            if (sTimes != null) {
                printStats("JSVG      (Java/Java2D)", sTimes);
                sAvg = Arrays.stream(sTimes).average().orElse(0);
            }

            double cAvg = 0;
            if (cTimes != null) {
                printStats("CairoSVG  (Python/Cairo)", cTimes);
                cAvg = Arrays.stream(cTimes).average().orElse(0);
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
    }
}

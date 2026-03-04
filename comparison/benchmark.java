///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview -Xmx512m
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS io.brunoborges:jairosvg:1.0.2-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4
//DEPS me.tongfei:progressbar:0.10.2

import io.brunoborges.jairosvg.JairoSVG;
import io.sf.carte.echosvg.transcoder.TranscoderInput;
import io.sf.carte.echosvg.transcoder.TranscoderOutput;
import io.sf.carte.echosvg.transcoder.image.PNGTranscoder;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class benchmark {

    static final int WARMUP = 20;
    static final int ITERATIONS = 1000;

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

        // Parse args: filter by name substring, --no-cairosvg, --no-echosvg
        Set<String> nameFilters = new LinkedHashSet<>();
        boolean runCairo = true;
        boolean runEcho = true;
        for (String arg : args) {
            if ("--no-cairosvg".equals(arg)) {
                runCairo = false;
            } else if ("--no-echosvg".equals(arg)) {
                runEcho = false;
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
        if (runCairo) System.out.print(" vs CairoSVG");
        System.out.println();
        System.out.printf("  Warmup: %d iterations, Measurement: %d iterations, SVG files: %d%n",
                WARMUP, ITERATIONS, cases.size());
        System.out.println("=".repeat(98));

        SvgConverter jairosvg = svg -> JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        SvgConverter echosvg = svg -> echoConvert(svg);

        for (int ci = 0; ci < cases.size(); ci++) {
            SvgCase c = cases.get(ci);
            System.gc();
            System.out.println("\n▸ " + c.name() + "  [" + (ci + 1) + "/" + cases.size() + "]");

            // Total steps for Java engines: (warmup + iterations) each
            int javaEngines = 1 + (runEcho ? 1 : 0);
            int totalSteps = javaEngines * (WARMUP + ITERATIONS) + (runCairo ? 1 : 0);

            double[] jTimes, eTimes = null, cTimes = null;

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

            double cAvg = 0;
            if (cTimes != null) {
                printStats("CairoSVG  (Python/Cairo)", cTimes);
                cAvg = Arrays.stream(cTimes).average().orElse(0);
            }

            System.out.println();
            if (eTimes != null) {
                printComparison("JairoSVG", jAvg, "EchoSVG", eAvg);
            }
            if (cTimes != null) {
                printComparison("JairoSVG", jAvg, "CairoSVG", cAvg);
                if (eTimes != null) {
                    printComparison("EchoSVG", eAvg, "CairoSVG", cAvg);
                }
            }
        }

        System.out.println("\n" + "=".repeat(98));
    }
}

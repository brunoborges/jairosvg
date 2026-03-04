///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview -Xmx512m
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS io.brunoborges:jairosvg:1.0.2-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4

import io.brunoborges.jairosvg.JairoSVG;
import io.sf.carte.echosvg.transcoder.TranscoderInput;
import io.sf.carte.echosvg.transcoder.TranscoderOutput;
import io.sf.carte.echosvg.transcoder.image.PNGTranscoder;

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

    static double[] bench(String label, SvgConverter converter, String svg) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            byte[] r = converter.convert(svg);
            if (r == null) throw new RuntimeException("null");
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
        List<SvgCase> cases = loadSvgCases();

        System.out.println("=".repeat(98));
        System.out.println("  SVG → PNG Benchmark: JairoSVG (Java) vs EchoSVG (Java) vs CairoSVG (Python)");
        System.out.printf("  Warmup: %d iterations, Measurement: %d iterations, SVG files: %d%n",
                WARMUP, ITERATIONS, cases.size());
        System.out.println("=".repeat(98));

        SvgConverter jairosvg = svg -> JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        SvgConverter echosvg = svg -> echoConvert(svg);

        for (SvgCase c : cases) {
            System.gc();
            System.out.println("\n▸ " + c.name());

            double[] jTimes = bench("JairoSVG", jairosvg, c.content());
            printStats("JairoSVG  (Java/Java2D)", jTimes);

            System.gc(); Thread.sleep(100);
            double[] eTimes = bench("EchoSVG", echosvg, c.content());
            printStats("EchoSVG   (Java/Batik)", eTimes);

            System.gc(); Thread.sleep(100);
            double[] cTimes = benchCairoSVG(c.content());
            if (cTimes != null) {
                printStats("CairoSVG  (Python/Cairo)", cTimes);
            }

            double jAvg = Arrays.stream(jTimes).average().orElse(0);
            double eAvg = Arrays.stream(eTimes).average().orElse(0);
            double cAvg = cTimes != null ? Arrays.stream(cTimes).average().orElse(0) : 0;

            System.out.println();
            printComparison("JairoSVG", jAvg, "EchoSVG", eAvg);
            if (cTimes != null) {
                printComparison("JairoSVG", jAvg, "CairoSVG", cAvg);
                printComparison("EchoSVG", eAvg, "CairoSVG", cAvg);
            }
        }

        System.out.println("\n" + "=".repeat(98));
    }
}

///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview -Xmx512m
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS io.brunoborges:jairosvg:1.0.1-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4

import io.brunoborges.jairosvg.JairoSVG;
import io.sf.carte.echosvg.transcoder.TranscoderInput;
import io.sf.carte.echosvg.transcoder.TranscoderOutput;
import io.sf.carte.echosvg.transcoder.image.PNGTranscoder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

public class benchmark {

    static final int WARMUP = 20;
    static final int ITERATIONS = 1000;

    static final String SVG_SIMPLE = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
              <rect x="10" y="10" width="80" height="60" fill="blue"/>
              <circle cx="150" cy="50" r="40" fill="red"/>
              <ellipse cx="100" cy="150" rx="60" ry="30" fill="green"/>
              <line x1="10" y1="190" x2="190" y2="190" stroke="black" stroke-width="2"/>
            </svg>
            """;

    static final String SVG_GRADIENTS = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
              <defs>
                <linearGradient id="lg1" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:rgb(255,255,0);stop-opacity:1"/>
                  <stop offset="100%" style="stop-color:rgb(255,0,0);stop-opacity:1"/>
                </linearGradient>
                <radialGradient id="rg1" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" style="stop-color:white;stop-opacity:1"/>
                  <stop offset="100%" style="stop-color:blue;stop-opacity:1"/>
                </radialGradient>
              </defs>
              <rect width="300" height="300" fill="url(#lg1)"/>
              <circle cx="150" cy="150" r="80" fill="url(#rg1)"/>
              <g transform="rotate(45, 150, 150)">
                <rect x="120" y="120" width="60" height="60" fill="rgba(0,128,0,0.5)" stroke="black"/>
              </g>
              <g transform="translate(50,50) scale(0.5)">
                <polygon points="150,10 190,140 70,50 230,50 110,140" fill="orange" fill-rule="evenodd"/>
              </g>
            </svg>
            """;

    static final String SVG_COMPLEX = """
            <svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400">
              <rect width="400" height="400" fill="#f0f0f0"/>
              <path d="M50,200 C50,100 150,100 150,200 S250,300 250,200" fill="none" stroke="purple" stroke-width="3"/>
              <path d="M300,50 L350,150 L250,150 Z" fill="crimson"/>
              <path d="M50,300 Q100,250 150,300 T250,300" fill="none" stroke="darkgreen" stroke-width="2"/>
              <text x="200" y="30" text-anchor="middle" font-size="24" font-weight="bold" fill="navy">Benchmark Test</text>
              <text x="200" y="380" text-anchor="middle" font-size="14" fill="gray">Complex SVG with paths and text</text>
              <g transform="translate(280,250)">
                <rect x="-40" y="-40" width="80" height="80" rx="10" fill="steelblue" opacity="0.7"/>
                <circle cx="0" cy="0" r="25" fill="gold"/>
              </g>
              <polyline points="20,350 60,320 100,340 140,310 180,330" fill="none" stroke="brown" stroke-width="2"/>
            </svg>
            """;

    static final String SVG_DEFS_USE = """
            <svg xmlns="http://www.w3.org/2000/svg" width="360" height="240" viewBox="0 0 360 240">
              <defs>
                <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#0ea5e9"/>
                  <stop offset="100%" stop-color="#1d4ed8"/>
                </linearGradient>
                <clipPath id="cardClip">
                  <rect x="0" y="0" width="320" height="200" rx="18"/>
                </clipPath>
                <g id="badge">
                  <circle cx="0" cy="0" r="24" fill="#facc15"/>
                  <path d="M-10,2 L-2,10 L12,-8" fill="none" stroke="#1f2937" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
                </g>
              </defs>
              <rect x="20" y="20" width="320" height="200" rx="18" fill="url(#bg)"/>
              <g clip-path="url(#cardClip)" transform="translate(20,20)">
                <rect x="0" y="120" width="320" height="80" fill="rgba(15,23,42,0.28)"/>
                <text x="24" y="58" font-size="28" font-family="sans-serif" font-weight="700" fill="white">Defs + Use</text>
                <text x="24" y="92" font-size="15" font-family="sans-serif" fill="#dbeafe">clipPath, gradients and symbol reuse</text>
                <use href="#badge" transform="translate(276,48)"/>
                <use href="#badge" transform="translate(242,82) scale(0.75)"/>
                <use href="#badge" transform="translate(206,110) scale(0.55)"/>
              </g>
            </svg>
            """;

    static final String SVG_MARKERS_DASH = """
            <svg xmlns="http://www.w3.org/2000/svg" width="420" height="280" viewBox="0 0 420 280">
              <defs>
                <marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
                  <path d="M0,0 L8,4 L0,8 z" fill="#ef4444"/>
                </marker>
              </defs>
              <rect width="420" height="280" fill="#f8fafc"/>
              <path d="M30,230 C100,40 200,40 280,180 S390,240 390,70"
                    fill="none"
                    stroke="#ef4444"
                    stroke-width="4"
                    stroke-dasharray="12 8"
                    stroke-linecap="round"
                    marker-start="url(#arrow)"
                    marker-mid="url(#arrow)"
                    marker-end="url(#arrow)"/>
              <polyline points="30,30 90,70 150,50 210,95 300,55 390,100"
                        fill="none"
                        stroke="#0f766e"
                        stroke-width="6"
                        stroke-linejoin="round"
                        stroke-linecap="round"
                        opacity="0.75"/>
              <text x="210" y="260" text-anchor="middle" font-size="16" font-family="sans-serif" fill="#0f172a">
                markers + dashed strokes + opacity
              </text>
            </svg>
            """;

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
        System.out.println("=".repeat(98));
        System.out.println("  SVG → PNG Benchmark: JairoSVG (Java) vs EchoSVG (Java) vs CairoSVG (Python)");
        System.out.printf("  Warmup: %d iterations, Measurement: %d iterations%n", WARMUP, ITERATIONS);
        System.out.println("=".repeat(98));

        String[][] cases = {
            {"Simple (shapes)", SVG_SIMPLE},
            {"Gradients + Transforms", SVG_GRADIENTS},
            {"Complex (paths + text)", SVG_COMPLEX},
            {"Defs + Use + clipPath", SVG_DEFS_USE},
            {"Markers + dashed strokes", SVG_MARKERS_DASH},
        };

        SvgConverter jairosvg = svg -> JairoSVG.svg2png(svg.getBytes(StandardCharsets.UTF_8));
        SvgConverter echosvg = svg -> echoConvert(svg);

        for (String[] c : cases) {
            System.gc();
            System.out.println("\n▸ " + c[0]);

            double[] jTimes = bench("JairoSVG", jairosvg, c[1]);
            printStats("JairoSVG  (Java/Java2D)", jTimes);

            System.gc(); Thread.sleep(100);
            double[] eTimes = bench("EchoSVG", echosvg, c[1]);
            printStats("EchoSVG   (Java/Batik)", eTimes);

            System.gc(); Thread.sleep(100);
            double[] cTimes = benchCairoSVG(c[1]);
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

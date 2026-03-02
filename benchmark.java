///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview -Xmx512m
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS com.jairosvg:jairosvg:1.0.0-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4

import com.jairosvg.JairoSVG;
import io.sf.carte.echosvg.transcoder.TranscoderInput;
import io.sf.carte.echosvg.transcoder.TranscoderOutput;
import io.sf.carte.echosvg.transcoder.image.PNGTranscoder;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Arrays;

public class benchmark {

    static final int WARMUP = 20;
    static final int ITERATIONS = 50;

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
        double avg = Arrays.stream(times).average().orElse(0);
        double median = times[ITERATIONS / 2];
        double p95 = times[(int) (ITERATIONS * 0.95)];
        double min = times[0];

        System.out.printf("  %-28s  avg=%7.2f ms  median=%7.2f ms  p95=%7.2f ms  min=%7.2f ms%n",
                label, avg, median, p95, min);
        return times;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(90));
        System.out.println("  SVG → PNG Benchmark: JairoSVG vs EchoSVG");
        System.out.printf("  Warmup: %d iterations, Measurement: %d iterations%n", WARMUP, ITERATIONS);
        System.out.println("=".repeat(90));

        String[][] cases = {
            {"Simple (shapes)", SVG_SIMPLE},
            {"Gradients + Transforms", SVG_GRADIENTS},
            {"Complex (paths + text)", SVG_COMPLEX},
        };

        SvgConverter jairosvg = svg -> JairoSVG.svg2png(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        SvgConverter echosvg = svg -> echoConvert(svg);

        for (String[] c : cases) {
            System.gc();
            System.out.println("\n▸ " + c[0]);
            double[] jTimes = bench("JairoSVG", jairosvg, c[1]);
            System.gc(); Thread.sleep(100);
            double[] eTimes = bench("EchoSVG", echosvg, c[1]);
            double jAvg = Arrays.stream(jTimes).average().orElse(0);
            double eAvg = Arrays.stream(eTimes).average().orElse(0);
            double ratio = eAvg / jAvg;
            String faster = jAvg < eAvg ? "JairoSVG" : "EchoSVG";
            System.out.printf("  → %s is %.1fx faster%n", faster, jAvg < eAvg ? ratio : 1.0 / ratio);
        }

        System.out.println("\n" + "=".repeat(90));
    }
}

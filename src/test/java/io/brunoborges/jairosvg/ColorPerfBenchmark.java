package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Manual micro-benchmark for the color-parsing-heavy SVG pipeline.
 *
 * <p>
 * Run explicitly with
 * {@code mvn test -Dtest=ColorPerfBenchmark -DfailIfNoTests=false -P with-perf}
 * (or temporarily remove {@link Disabled}). Compares the time to convert each
 * SVG to PNG bytes; the cases include gradients, filters and CSS-styled trees
 * where color parsing happens frequently.
 *
 * <p>
 * Numbers are wall-clock and noisy — use them for relative comparison between
 * branches, not absolute reporting.
 */
@Disabled("Manual perf benchmark; enable for before/after comparisons")
class ColorPerfBenchmark {

    private static final int WARMUP = 20;
    private static final int ITERS = 100;

    @Test
    void run() throws Exception {
        Path svgDir = Path.of("comparison", "svg");
        // Color-heavy cases; skip ones that rely on external font/image fetches
        List<String> cases = List.of("01_basic_shapes.svg", "02_gradients.svg", "03_complex_paths.svg",
                "05_transforms.svg", "06_stroke_styles.svg", "07_opacity_blend.svg", "09_css_styling.svg",
                "13_patterns.svg", "17_filters.svg", "20_fe_blend_modes.svg");

        System.out.printf(Locale.US, "%n%-28s %10s %10s %10s%n", "case", "avg ms", "p50 ms", "min ms");
        System.out.println("-".repeat(64));
        long totalAvgNs = 0;
        for (String name : cases) {
            byte[] svg = Files.readAllBytes(svgDir.resolve(name));
            for (int i = 0; i < WARMUP; i++) {
                JairoSVG.builder().fromBytes(svg).toPng();
            }
            double[] times = new double[ITERS];
            for (int i = 0; i < ITERS; i++) {
                long t0 = System.nanoTime();
                JairoSVG.builder().fromBytes(svg).toPng();
                times[i] = (System.nanoTime() - t0) / 1_000_000.0;
            }
            Arrays.sort(times);
            double avg = Arrays.stream(times).average().orElse(0);
            double median = times[times.length / 2];
            double min = times[0];
            totalAvgNs += (long) (avg * 1_000_000);
            System.out.printf(Locale.US, "%-28s %10.2f %10.2f %10.2f%n", name, avg, median, min);
        }
        System.out.printf(Locale.US, "%nsum-of-avg: %.1f ms%n", totalAvgNs / 1_000_000.0);
    }
}

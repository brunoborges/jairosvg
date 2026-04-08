///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//COMPILE_OPTIONS --release 25
//DEPS com.google.code.gson:gson:2.13.1

/**
 * Update per-test-case benchmark time rows and iteration note in
 * comparison/COMPARISON.md from benchmark-results.jsonl.
 *
 * Usage:
 *   jbang comparison/benchmark/update_readme.java
 *   jbang comparison/benchmark/update_readme.java --warmup=50 --iterations=500
 */

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;

public class update_readme {

    static final Path JSONL_PATH = Path.of("comparison", "benchmark", "benchmark-results.jsonl");
    static final Path README_PATH = Path.of("comparison", "COMPARISON.md");
    static final Path SVG_DIR = Path.of("comparison", "svg");

    static final List<String> ENGINES = List.of("jairosvg", "echosvg", "jsvg", "cairosvg");

    record SvgCase(String name, int num, String slug) {}
    record BenchResult(String engine, String caseName, double avg, double median, double p95, double min) {}

    static List<SvgCase> loadSvgCases() throws IOException {
        var pattern = Pattern.compile("(\\d+)_(.+)\\.svg");
        return Files.list(SVG_DIR)
                .map(p -> p.getFileName().toString())
                .sorted()
                .map(f -> {
                    var m = pattern.matcher(f);
                    if (!m.matches()) return null;
                    int num = Integer.parseInt(m.group(1));
                    String slug = m.group(2);
                    String name = slug.replace('_', ' ');
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    return new SvgCase(name, num, slug);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    static Map<String, Map<String, BenchResult>> loadJsonl() throws IOException {
        var data = new LinkedHashMap<String, Map<String, BenchResult>>();
        if (!Files.exists(JSONL_PATH)) {
            System.err.println("Warning: " + JSONL_PATH + " not found");
            return data;
        }
        var gson = new Gson();
        for (String line : Files.readAllLines(JSONL_PATH)) {
            line = line.strip();
            if (line.isEmpty()) continue;
            JsonObject obj = gson.fromJson(line, JsonObject.class);
            String engine = obj.has("engine") ? obj.get("engine").getAsString() : null;
            if (engine == null || !ENGINES.contains(engine)) continue;
            String caseName = obj.has("case") ? obj.get("case").getAsString() : null;
            if (caseName == null) continue;
            if (!obj.has("median")) continue;
            data.computeIfAbsent(caseName, _ -> new LinkedHashMap<>())
                    .put(engine, new BenchResult(engine, caseName,
                            obj.get("avg").getAsDouble(),
                            obj.get("median").getAsDouble(),
                            obj.get("p95").getAsDouble(),
                            obj.get("min").getAsDouble()));
        }
        return data;
    }

    static String replaceSection(String content, String tag, String replacement) {
        String begin = "<!-- BEGIN:" + tag + " -->";
        String end = "<!-- END:" + tag + " -->";
        int start = content.indexOf(begin);
        int finish = content.indexOf(end);
        if (start < 0 || finish < 0) return content;
        return content.substring(0, start) + begin + "\n" + replacement + "\n" + end + content.substring(finish + end.length());
    }

    static void updateReadme(int warmup, int iterations,
                             Map<String, Map<String, BenchResult>> data, List<SvgCase> cases) throws IOException {
        String content = Files.readString(README_PATH);

        // Replace iteration note
        String note = "> **Note:** Benchmarks were run with %d warm-up iterations and %d measured iterations"
                .formatted(warmup, iterations)
                + " per SVG file. Median time reported. Results may vary by hardware and SVG complexity.";
        content = replaceSection(content, "BENCHMARK_NOTE", note);

        // Update per-test-case **Time** rows
        for (var svgCase : cases) {
            var caseData = data.get(svgCase.name());
            String[] keys = {"jairosvg", "echosvg", "cairosvg", "jsvg"};
            Double[] vals = new Double[4];
            if (caseData != null) {
                for (int i = 0; i < 4; i++) {
                    var r = caseData.get(keys[i]);
                    vals[i] = r != null ? r.median() : null;
                }
            }
            double minVal = Arrays.stream(vals).filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue).min().orElse(Double.MAX_VALUE);

            String timeRow = "| **Time** | %s | %s | %s | %s |".formatted(
                    benchTimeCell(vals[0], minVal), benchTimeCell(vals[1], minVal),
                    benchTimeCell(vals[2], minVal), benchTimeCell(vals[3], minVal));

            String anchor = "### %02d_%s".formatted(svgCase.num(), svgCase.slug());
            int sectionStart = content.indexOf(anchor);
            if (sectionStart < 0) continue;
            int timeStart = content.indexOf("| **Time** |", sectionStart);
            if (timeStart < 0) continue;
            int timeEnd = content.indexOf("\n", timeStart);
            if (timeEnd < 0) timeEnd = content.length();
            content = content.substring(0, timeStart) + timeRow + content.substring(timeEnd);
        }

        Files.writeString(README_PATH, content);
    }

    static String benchTimeCell(Double val, double minVal) {
        if (val == null) return "—";
        String formatted = "%.4f ms".formatted(val);
        return val == minVal ? "**%s** ✅".formatted(formatted) : formatted;
    }

    public static void main(String[] args) throws Exception {
        var cases = loadSvgCases();
        var data = loadJsonl();

        if (data.isEmpty()) {
            System.err.println("No benchmark data found. Run the benchmark first:");
            System.err.println("  jbang comparison/benchmark/benchmark.java");
            System.exit(1);
        }

        int warmup = 50;
        int iterations = 500;

        for (String arg : args) {
            if (arg.startsWith("--warmup=")) {
                warmup = Integer.parseInt(arg.substring("--warmup=".length()));
            } else if (arg.startsWith("--iterations=")) {
                iterations = Integer.parseInt(arg.substring("--iterations=".length()));
            }
        }

        updateReadme(warmup, iterations, data, cases);

        System.out.println("✅ Updated " + README_PATH);
        System.out.println("   Per-case time rows: " + cases.size() + " test cases");
    }
}

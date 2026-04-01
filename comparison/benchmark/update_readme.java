///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview
//DEPS com.google.code.gson:gson:2.13.1

/**
 * Regenerate the benchmark and PNG file-size tables in comparison/COMPARISON.md
 * from benchmark-results.jsonl and the rendered PNG files.
 *
 * Usage:
 *   jbang comparison/benchmark/update_readme.java
 *   jbang comparison/benchmark/update_readme.java --warmup=50 --iterations=500
 */

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;

public class update_readme {

    static final Path BASE_DIR = Path.of("comparison", "benchmark");
    static final Path README_PATH = Path.of("comparison", "COMPARISON.md");
    static final Path JSONL_PATH = BASE_DIR.resolve("benchmark-results.jsonl");
    static final Path SVG_DIR = Path.of("comparison", "svg");
    static final Path PNG_DIR = Path.of("comparison", "visual", "png");

    static final List<String> ENGINES = List.of("jairosvg", "echosvg", "jsvg", "cairosvg");
    static final List<String> ENGINE_HEADERS = List.of(
            "JairoSVG (Java)", "EchoSVG (Java)", "JSVG (Java)", "CairoSVG (Python)");
    static final List<String> SIZE_ENGINES = List.of("jairosvg", "echosvg", "cairosvg", "jsvg");
    static final Set<String> WARN_CASES = Set.of("Masks", "Filters", "Fe turbulence");

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

    // Map<caseName, Map<engine, BenchResult>>
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
            if (!obj.has("median")) continue; // skip error entries
            data.computeIfAbsent(caseName, _ -> new LinkedHashMap<>())
                    .put(engine, new BenchResult(engine, caseName,
                            obj.get("avg").getAsDouble(),
                            obj.get("median").getAsDouble(),
                            obj.get("p95").getAsDouble(),
                            obj.get("min").getAsDouble()));
        }
        return data;
    }

    static String generateBenchmarkTable(List<SvgCase> cases, Map<String, Map<String, BenchResult>> data) {
        var lines = new ArrayList<String>();
        lines.add("| Test Case | " + String.join(" | ", ENGINE_HEADERS) + " |");
        lines.add("| --- | " + ENGINES.stream().map(_ -> ":---:").collect(Collectors.joining(" | ")) + " |");

        for (var svgCase : cases) {
            var caseData = data.get(svgCase.name());
            if (caseData == null) continue;
            if (ENGINES.stream().noneMatch(e -> caseData.containsKey(e))) continue;

            Double[] vals = new Double[ENGINES.size()];
            for (int i = 0; i < ENGINES.size(); i++) {
                var result = caseData.get(ENGINES.get(i));
                vals[i] = result != null ? result.median() : null;
            }

            double minVal = Arrays.stream(vals).filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue).min().orElse(Double.MAX_VALUE);

            var cells = new ArrayList<String>();
            for (Double v : vals) {
                if (v == null) {
                    cells.add("—");
                } else {
                    String formatted = "%.4f ms".formatted(v);
                    if (v == minVal) {
                        formatted = "**%s** ✅".formatted(formatted);
                    }
                    cells.add(formatted);
                }
            }

            String anchor = "%02d_%s".formatted(svgCase.num(), svgCase.slug());
            String label = "[%s](#%s)".formatted(svgCase.name(), anchor);
            if (WARN_CASES.contains(svgCase.name())) {
                label += " ⚠️";
            }

            lines.add("| %s | %s |".formatted(label, String.join(" | ", cells)));
        }

        return String.join("\n", lines);
    }

    record PngTableResult(String table, String summary) {}

    static PngTableResult generatePngSizesTable(List<SvgCase> cases) {
        record Row(String display, Map<String, Long> sizes) {}

        var rows = new ArrayList<Row>();
        var totals = new LinkedHashMap<String, Long>();
        var hasData = new LinkedHashMap<String, Boolean>();
        SIZE_ENGINES.forEach(e -> { totals.put(e, 0L); hasData.put(e, false); });

        var fmt = NumberFormat.getIntegerInstance(Locale.US);

        for (var svgCase : cases) {
            String pngName = "%02d_%s.png".formatted(svgCase.num(), svgCase.slug());
            var sizes = new LinkedHashMap<String, Long>();
            for (String e : SIZE_ENGINES) {
                Path path = PNG_DIR.resolve(e).resolve(pngName);
                if (Files.exists(path)) {
                    try {
                        long size = Files.size(path);
                        sizes.put(e, size);
                        totals.merge(e, size, Long::sum);
                        hasData.put(e, true);
                    } catch (IOException ex) {
                        sizes.put(e, null);
                    }
                } else {
                    sizes.put(e, null);
                }
            }

            String warn = WARN_CASES.contains(svgCase.name()) ? " ⚠️" : "";
            String display = toTitleCase(svgCase.name()) + warn;
            rows.add(new Row(display, sizes));
        }

        int maxLabel = rows.stream().mapToInt(r -> r.display().length()).max().orElse(14);
        maxLabel = Math.max(maxLabel, 14);

        var lines = new ArrayList<String>();
        lines.add("| %-${w}s | %11s | %11s | %11s | %11s |"
                .replace("${w}", String.valueOf(maxLabel))
                .formatted("Test Case", "JairoSVG", "EchoSVG", "CairoSVG", "JSVG"));
        lines.add("| %s | ----------: | ----------: | ----------: | ----------: |"
                .formatted("-".repeat(maxLabel)));

        for (var row : rows) {
            var cells = new ArrayList<String>();
            for (String e : SIZE_ENGINES) {
                Long size = row.sizes().get(e);
                cells.add(size != null ? "%11s".formatted(fmt.format(size)) : "%11s".formatted("—"));
            }
            lines.add("| %-${w}s | %s |"
                    .replace("${w}", String.valueOf(maxLabel))
                    .formatted(row.display(), String.join(" | ", cells)));
        }

        var totalCells = new ArrayList<String>();
        for (String e : SIZE_ENGINES) {
            if (hasData.get(e)) {
                String val = "**%s**".formatted(fmt.format(totals.get(e)));
                totalCells.add("%11s".formatted(val));
            } else {
                totalCells.add("%11s".formatted("—"));
            }
        }
        lines.add("| %-${w}s | %s |"
                .replace("${w}", String.valueOf(maxLabel))
                .formatted("**Total**", String.join(" | ", totalCells)));

        // Compute summary percentages
        long jTotal = totals.get("jairosvg");
        var comparisons = new ArrayList<String>();
        for (var entry : List.of(
                Map.entry("CairoSVG", "cairosvg"),
                Map.entry("JSVG", "jsvg"),
                Map.entry("EchoSVG", "echosvg"))) {
            long other = totals.get(entry.getValue());
            if (other > 0) {
                double pct = (other - jTotal) / (double) other * 100;
                if (pct > 0) {
                    comparisons.add("**%.1f%% smaller** than %s".formatted(pct, entry.getKey()));
                }
            }
        }

        String summary;
        if (comparisons.size() > 1) {
            summary = "JairoSVG produces compact PNGs — "
                    + String.join(", ", comparisons.subList(0, comparisons.size() - 1))
                    + ", and " + comparisons.getLast();
        } else if (comparisons.size() == 1) {
            summary = "JairoSVG produces compact PNGs — " + comparisons.getFirst();
        } else {
            summary = "JairoSVG produces compact PNGs — comparable in size to other engines";
        }
        summary += " (all using zlib compression level 6 — see "
                + "[default rendering settings](#default-rendering-settings-jairosvg-vs-jsvg)):";

        return new PngTableResult(String.join("\n", lines), summary);
    }

    static String computeSummary(Map<String, Map<String, BenchResult>> data, List<SvgCase> cases) {
        var jairoVsEcho = new ArrayList<Double>();

        for (var svgCase : cases) {
            var caseData = data.get(svgCase.name());
            if (caseData == null || !caseData.containsKey("jairosvg")) continue;
            double j = caseData.get("jairosvg").median();

            var echo = caseData.get("echosvg");
            if (echo != null) {
                jairoVsEcho.add(echo.median() / j);
            }
        }

        String echoDesc;
        if (!jairoVsEcho.isEmpty()) {
            int minR = (int) jairoVsEcho.stream().mapToDouble(Double::doubleValue).min().orElse(1);
            int maxR = (int) jairoVsEcho.stream().mapToDouble(Double::doubleValue).max().orElse(1);
            echoDesc = "**%d–%d× faster** than EchoSVG".formatted(minR, maxR);
        } else {
            echoDesc = "comparable to EchoSVG";
        }

        return "_JairoSVG is %s, **on par with JSVG** in most scenarios, and **faster than CairoSVG** in most scenarios._"
                .formatted(echoDesc);
    }

    static String replaceSection(String content, String tag, String replacement) {
        String begin = "<!-- BEGIN:" + tag + " -->";
        String end = "<!-- END:" + tag + " -->";
        int start = content.indexOf(begin);
        int finish = content.indexOf(end);
        if (start < 0 || finish < 0) return content;
        return content.substring(0, start) + begin + "\n" + replacement + "\n" + end + content.substring(finish + end.length());
    }

    static void updateReadme(String benchmarkTable, String pngTable, String pngSummary,
                             String summary, int warmup, int iterations,
                             Map<String, Map<String, BenchResult>> data, List<SvgCase> cases) throws IOException {
        String content = Files.readString(README_PATH);

        int caseCount = (int) benchmarkTable.lines().count() - 2;

        // Replace aggregate benchmark section
        String benchSection = "## Benchmark\n\n"
                + "SVG → PNG conversion benchmarks across %d SVG test files, median time per render (lower is better):\n\n"
                        .formatted(caseCount)
                + benchmarkTable + "\n\n"
                + summary;
        content = replaceSection(content, "BENCHMARK", benchSection);

        // Replace aggregate PNG sizes section
        String pngSection = "## PNG Output File Sizes\n\n"
                + pngSummary + "\n\n"
                + pngTable;
        content = replaceSection(content, "PNG_SIZES", pngSection);

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

            String tag = "TIME:%02d_%s".formatted(svgCase.num(), svgCase.slug());
            content = replaceSection(content, tag, timeRow);
        }

        Files.writeString(README_PATH, content);
    }

    static String benchTimeCell(Double val, double minVal) {
        if (val == null) return "—";
        String formatted = "%.4f ms".formatted(val);
        return val == minVal ? "**%s** ✅".formatted(formatted) : formatted;
    }

    static String toTitleCase(String s) {
        return Arrays.stream(s.split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
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

        String benchmarkTable = generateBenchmarkTable(cases, data);
        var pngResult = generatePngSizesTable(cases);
        String summary = computeSummary(data, cases);

        updateReadme(benchmarkTable, pngResult.table(), pngResult.summary(), summary,
                warmup, iterations, data, cases);

        int caseCount = (int) benchmarkTable.lines().count() - 2;
        System.out.println("✅ Updated " + README_PATH);
        System.out.println("   Benchmark table: " + caseCount + " test cases");
        System.out.println("   PNG sizes table: " + cases.size() + " test cases");
    }
}

// Builds a Copilot analysis prompt from a GC/JFR report. The prompt embeds the
// headline metrics inline (so the agent has immediate context) and attaches the
// full report.json + jfr views report as files for deeper inspection.

function pct(n) { return n == null ? "?" : `${Number(n).toFixed(2)}%`; }
function ms(n) { return n == null ? "?" : `${Number(n).toFixed(2)} ms`; }
function mb(kb) { return kb == null ? "?" : `${Math.round(kb / 1024)} MB`; }
function num(n, d = 0) { return n == null ? "?" : Number(n).toFixed(d); }

function topList(items, fmt, n = 8) {
    if (!items || !items.length) return "  (none)";
    return items.slice(0, n).map((it, i) => `  ${i + 1}. ${fmt(it)}`).join("\n");
}

function breakdown(obj) {
    const entries = Object.entries(obj || {});
    if (!entries.length) return "  (none)";
    const total = entries.reduce((s, [, v]) => s + v, 0) || 1;
    return entries
        .sort((a, b) => b[1] - a[1])
        .map(([k, v]) => `  - ${k}: ${v} (${((v / total) * 100).toFixed(1)}%)`)
        .join("\n");
}

/**
 * @param {object} report the persisted GC/JFR report (runs/latest.json)
 * @returns {{prompt:string, displayPrompt:string, attachments:Array}}
 */
export function buildAnalysisPrompt(report) {
    const s = report?.gc?.summary ?? {};
    const cfg = report?.config ?? {};
    const gcConfig = report?.gcConfig ?? {};
    const jvm = report?.jvm ?? {};
    const jfr = report?.jfr ?? {};

    const collector = gcConfig.youngCollector
        ? `${gcConfig.youngCollector} / ${gcConfig.oldCollector}`
        : "unknown";

    const prompt = `You are a JVM performance engineer. Analyze the garbage-collection and
JDK Flight Recorder (JFR) telemetry below, captured from a benchmark run of the
**JairoSVG** SVG-to-PNG rendering library, and produce actionable recommendations.

## Run configuration
- Workload: JairoSVG benchmark (warmup=${cfg.warmup}, iterations=${cfg.iterations}, engines=${cfg.engines})
- Max heap: ${cfg.heapMb} MB
- Collector: ${collector} (${num(gcConfig.parallelGCThreads)} parallel / ${num(gcConfig.concurrentGCThreads)} concurrent GC threads)
- JVM: ${jvm.name ?? "?"} ${jvm.version ? "— " + jvm.version.split(" for ")[0] : ""}

## GC summary (Microsoft GCToolkit)
- Throughput (non-GC time): ${pct(s.throughputPercent)}
- Time paused for GC: ${pct(s.percentPaused)} over ${num(s.runtimeSec, 1)} s runtime
- GC events: ${num(s.eventCount)}
- Pause times — total ${ms(s.totalPauseMs)}, avg ${ms(s.avgPauseMs)}, p95 ${ms(s.p95PauseMs)}, p99 ${ms(s.p99PauseMs)}, max ${ms(s.maxPauseMs)}
- Peak heap occupancy: ${mb(s.peakHeapKb)} (of ${mb(s.maxHeapSizeKb)} committed)
- Allocation rate: ${num(s.allocRateMbPerSec)} MB/s

## GC causes
${breakdown(report?.gc?.causes)}

## GC types
${breakdown(report?.gc?.types)}

## Top allocation sites (JFR object sampling, ~${num(jfr.totalAllocatedMb)} MB sampled)
${topList(jfr.topAllocations, (a) => `${a.name} — ${num(a.weightMb, 1)} MB (${a.samples} samples)`)}

## Hot methods (JFR execution sampling, ${num(jfr.sampleCount)} samples)
${topList(jfr.hotMethods, (m) => `${m.name} — ${num(m.pct, 1)}% (${m.samples} samples)`)}

---

The full machine-readable report (\`report.json\`) and the complete
\`jfr view all-views\` text report are attached for deeper inspection.

Please provide:
1. **GC health assessment** — is throughput/pause behavior healthy for this
   workload? Call out anything concerning (e.g. frequent humongous allocations,
   explicit System.gc() calls, high allocation rate, long tail pauses).
2. **JVM flag recommendations** — concrete \`-XX\` flags or collector choices to
   try, with the reasoning and the metric each should improve. Note any current
   flags that look counterproductive.
3. **Allocation / code hotspots** — from the top allocations and hot methods,
   which JairoSVG code paths are worth optimizing to reduce GC pressure, and how.
4. **Suggested next experiment** — the single most valuable follow-up run
   (heap size / collector / iterations) and what you'd expect to see.

Ground every recommendation in the numbers above. Be specific and concise.`;

    const attachments = [];
    const reportPath = report?.artifacts?.dir ? `${report.artifacts.dir}/report.json` : null;
    if (reportPath) attachments.push({ type: "file", path: reportPath, displayName: "gc-jfr-report.json" });
    if (report?.artifacts?.views) attachments.push({ type: "file", path: report.artifacts.views, displayName: "jfr-all-views.txt" });

    const displayPrompt = `Analyze the GC & JFR results (${num(s.eventCount)} GC events, ${pct(s.throughputPercent)} throughput, ${num(s.allocRateMbPerSec)} MB/s allocation) and recommend JVM tuning + code optimizations.`;

    return { prompt, displayPrompt, attachments };
}

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
    const gcConfig = report?.gcConfig ?? {};
    const jvm = report?.jvm ?? {};
    const jfr = report?.jfr ?? {};

    const collector = gcConfig.youngCollector
        ? `${gcConfig.youngCollector} / ${gcConfig.oldCollector}`
        : "unknown";

    const prompt = `You are a JVM performance engineer. Analyze the garbage-collection and
JDK Flight Recorder (JFR) telemetry below, captured from a workload run of the
Java project in this workspace, and produce actionable recommendations.

## Run
- Workload: ${report?.label || "(unspecified — see source artifacts)"}
- GC log: ${report?.source?.gcLogName || "?"}
- Collector: ${collector} (${num(gcConfig.parallelGCThreads)} parallel / ${num(gcConfig.concurrentGCThreads)} concurrent GC threads)
- Max heap: ${mb(s.maxHeapSizeKb)}
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

## Top allocation sites (share of allocation pressure, ≈ ${num(jfr.totalAllocatedMb)} MB total)
${topList(jfr.topAllocations, (a) => `${a.name} — ${num(a.pressurePct, 1)}% of allocations (≈ ${num(a.weightMb, 0)} MB)`)}

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
   which application code paths are worth optimizing to reduce GC pressure, and how.
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

/**
 * Build the prompt that asks Copilot to build + run this project's workload with
 * GC logging and JFR enabled (choosing flags correct for the detected JDK), then
 * hand the resulting artifacts back via the `gc_jfr_ingest` tool.
 *
 * @param {object} o { workspacePath?, hint?, jfrMaxSizeMb? }
 * @returns {{prompt:string, displayPrompt:string}}
 */
export function buildRunPrompt(o = {}) {
    const ws = o.workspacePath || "(the current workspace)";
    const jfrMb = o.jfrMaxSizeMb && Number(o.jfrMaxSizeMb) > 0 ? Number(o.jfrMaxSizeMb) : 100;
    const hint = (o.hint || "").trim();

    const prompt = `Set up and run a GC + JFR profiling run for the Java project in this
workspace so its garbage-collection and flight-recorder telemetry can be
analyzed and visualized in the **GC & JFR Analysis** canvas.

Workspace: ${ws}
${hint ? `Workload guidance from the user: "${hint}"\n` : ""}
Do the following, thinking about which steps apply to *this* project:

1. **Detect the build system and a representative workload.** Inspect the project
   (pom.xml / build.gradle(.kts) / Makefile, main classes, JMH benchmarks,
   runnable/shaded jars, existing benchmark or load scripts). Decide how to build
   it and how to run a workload that exercises the app for roughly 15–60 seconds
   so there is enough telemetry. Prefer, in order: an existing benchmark/JMH
   harness, a runnable jar / main class, or a representative test. If it is
   genuinely ambiguous and there is no reasonable default, ask the user what to
   run before proceeding.

2. **Detect the JDK version** (\`java -version\`) and pick the correct flags for
   the JVM that runs the *application* (not the build tool):
   - **JDK 9+** (unified logging): \`-Xlog:gc*:file=<gcLog>:time,uptime,level,tags\`
   - **JDK 8**: \`-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xloggc:<gcLog>\`
   - **JFR (JDK 11+)**: \`-XX:StartFlightRecording=maxsize=${jfrMb}M,filename=<jfr>,settings=profile,dumponexit=true\`
   - **JFR (Oracle JDK 8u40–8u261)**: additionally prepend \`-XX:+UnlockCommercialFeatures -XX:+FlightRecorder\`.
   Use \`settings=profile\` so JFR captures method-execution and object-allocation samples.

3. **Build, then run** the workload with those flags added. Write the GC log to a
   file named \`gc.log\` and the recording to \`dump.jfr\` in a working directory of
   your choosing (e.g. the project's \`target\`/\`build\` dir or a temp dir). If you
   need to constrain the heap to make GC activity richer, that is fine — just note it.

4. When the run finishes, **call the \`gc_jfr_ingest\` tool** with the absolute
   \`gcLogPath\` and (if produced) \`jfrPath\`, plus a short \`label\` describing the
   workload and any notable flags (heap size, collector). That parses and
   visualizes the data in the canvas.

Keep the run bounded so it completes in a couple of minutes, and report the exact
command you used.`;

    const displayPrompt = `Run this project's workload with GC logging + JFR enabled, then ingest the results into the GC & JFR Analysis canvas.${hint ? ` (Guidance: ${hint})` : ""}`;

    return { prompt, displayPrompt };
}


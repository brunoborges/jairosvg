# JVM Pulse

A Copilot CLI **canvas extension** that profiles **any Java project's** garbage
collection and flight-recording telemetry and visualizes it in an interactive
side panel. It is **Copilot-driven**: the extension doesn't hard-code how to
build or run your project. Instead the **Run analysis** button asks Copilot to
detect the build tool and JDK, run a representative workload with GC logging +
JFR enabled, and hand the resulting artifacts back for analysis.

It combines three data sources:

1. **GC logging** — the workload JVM is launched with GC logging. Copilot picks
   the form that matches the detected JDK (unified `-Xlog:gc*:file=…` on JDK 9+,
   `-Xloggc` + `-XX:+PrintGCDetails` on JDK 8).
2. **Microsoft [GCToolkit](https://github.com/microsoft/gctoolkit)** — parses the
   GC log into per-collection events (pause, cause, heap before/after) and
   derives throughput, pause percentiles and allocation rate.
3. **JFR** — the JVM records a flight recording
   (`-XX:StartFlightRecording=maxsize=…M,filename=dump.jfr,settings=profile`).
   The `jfr` CLI extracts CPU load, heap samples, top allocations and hot
   methods (via `jfr print --json`), and a full `jfr view all-views` report.

## How it works

```
[Run analysis] ──▶ session.send(run prompt) ──▶ Copilot builds + runs the
                                                 workload with GC log + JFR
                                                          │
                                                          ▼
                                          Copilot calls the jvm_pulse_ingest tool
                                          with the gc.log / dump.jfr paths
                                                          │
                                                          ▼
                            GCToolkit + jfr CLI analysis ──▶ canvas visualization
                                                          │
                                          [Analyze with AI] ──▶ session.send(report)
                                                                for tuning advice
```

The `jvm_pulse_ingest` tool is also usable directly: point Copilot at any existing
`gc.log`/`.jfr` on disk and ask it to ingest them — no run required.

## What it shows

- **Overview** — throughput %, GC count, total/avg/max/p99 pause, peak heap,
  allocation rate, GC runtime.
- **GC pause timeline** — every stop-the-world pause, colored by collection type.
- **Heap occupancy** — heap used before/after each GC (GC log) and heap
  used/committed sampled by JFR.
- **GC cause breakdown** — collections per cause.
- **CPU load** — JVM user/system and machine-total utilization over the run.
- **Top allocations** — allocated MB by type (JFR object sampling).
- **Hot methods** — top self-time methods (JFR execution sampling).
- **Environment** — JVM, collector, threads, workload label, GC log source.
- **`jfr view all-views`** — the raw JFR views report, inline.

## Requirements

Runs on the host machine and shells out to:

- A **JDK** with the `jfr` CLI (JDK 11+ for JFR; JDK 9+ for unified GC logging).
- **[jbang](https://www.jbang.dev/)** — runs the GCToolkit analyzer.

Tool paths are auto-resolved from `PATH`, `JAVA_HOME`, and common install
locations; inspect them with the `tool_status` action or `GET /tools`.

## Usage

Open the **JVM Pulse** canvas and click **Run analysis** (optionally
telling Copilot what workload to run), or drive it from chat:

- **`jvm_pulse_ingest`** *(agent tool)* — `{ gcLogPath, jfrPath?, label? }`. Analyzes
  a GC log + optional JFR recording and updates the canvas. Copilot calls this
  after running a workload, or you can invoke it on existing artifacts.
- `run_analysis` *(canvas action)* — `{ hint?, jfrMaxSizeMb? }`. Injects the
  "build + run this project's workload with GC logging + JFR" request into the
  session; Copilot does the project-specific work.
- `analyze_with_copilot` *(canvas action)* — sends the latest report (with the
  full `report.json` + jfr views attached) to Copilot for tuning recommendations.
- `load_results` — return the latest saved analysis without re-running.
- `tool_status` — resolved `jbang` / `jfr` paths.

## Layout

```
extension.mjs        wiring: canvas, HTTP server, SSE, actions, jvm_pulse_ingest tool
lib/pipeline.mjs     analyzeArtifacts(): GC/JFR analysis of provided artifacts
lib/jfr.mjs          jfr CLI extraction + reduction to chart-ready data
lib/prompt.mjs       run prompt + AI-analysis prompt builders
tools/GcLogAnalyzer.java   jbang GCToolkit analyzer → JSON
web/                 self-contained visualization (hand-rolled SVG charts)
runs/                per-run artifacts + latest.json (git-ignored)
```

Each ingest copies the provided `gc.log`/`.jfr` into a fresh `runs/<timestamp>/`
directory so every run is self-contained and re-analyzable.

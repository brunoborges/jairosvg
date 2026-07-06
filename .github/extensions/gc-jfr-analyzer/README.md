# GC & JFR Analysis canvas

A Copilot CLI **canvas extension** that turns the JairoSVG benchmark into a JVM
workload for garbage-collection and flight-recording analysis, then visualizes
the results in an interactive side panel.

It combines three data sources:

1. **GC logging** — the workload JVM is launched with unified logging
   (`-Xlog:gc*:file=…:time,uptime,level,tags`, the Java 25 form).
2. **Microsoft [GCToolkit](https://github.com/microsoft/gctoolkit)** — parses the
   GC log into per-collection events (pause, cause, heap before/after) and
   derives throughput, pause percentiles and allocation rate.
3. **JFR** — the JVM records a flight recording
   (`-XX:StartFlightRecording=maxsize=100M,filename=dump.jfr,settings=profile`).
   The `jfr` CLI extracts CPU load, heap samples, top allocations and hot
   methods (via `jfr print --json`), and a full `jfr view all-views` report.

## What it shows

- **Overview** — throughput %, GC count, total/avg/max/p99 pause, peak heap,
  allocation rate, GC vs. wall time.
- **GC pause timeline** — every stop-the-world pause, colored by collection type.
- **Heap occupancy** — heap used before/after each GC (GC log) and heap
  used/committed sampled by JFR.
- **GC cause breakdown** — collections per cause.
- **CPU load** — JVM user/system and machine-total utilization over the run.
- **Top allocations** — allocated MB by type (JFR object sampling).
- **Hot methods** — top self-time methods (JFR execution sampling).
- **Environment** — JVM, collector, threads, workload command.
- **`jfr view all-views`** — the raw JFR views report, inline.

## Requirements

Runs on the host machine and shells out to:

- **Java 25** (the repo's toolchain) and its bundled `jfr` CLI.
- **[jbang](https://www.jbang.dev/)** — runs the benchmark and the GCToolkit analyzer.
- The repo's Maven wrapper (`./mvnw`) — used to `install` the `io.brunoborges:jairosvg`
  SNAPSHOT the benchmark depends on (skippable via `skipMvnInstall`).

Tool paths are auto-resolved from `PATH`, `JAVA_HOME`, and common install
locations; inspect them with the `tool_status` action or `GET /tools`.

## Usage

Open the **GC & JFR Analysis** canvas and click **Run analysis**, or drive it
from chat with the canvas actions:

- `run_analysis` — `{ warmup?, iterations?, heapMb?, engines?, skipMvnInstall? }`.
  A smaller `heapMb` forces more frequent GC cycles (richer telemetry).
- `load_results` — return the latest saved analysis without re-running.
- `tool_status` — resolved `jbang` / `jfr` / `mvnw` paths.

## Layout

```
extension.mjs        wiring: canvas, HTTP server, SSE, actions
lib/pipeline.mjs     orchestration: workload run + GC/JFR analysis
lib/jfr.mjs          jfr CLI extraction + reduction to chart-ready data
tools/GcLogAnalyzer.java   jbang GCToolkit analyzer → JSON
web/                 self-contained visualization (hand-rolled SVG charts)
runs/                per-run artifacts + latest.json (git-ignored)
```

The benchmark rewrites `comparison/COMPARISON.md`; the pipeline snapshots and
restores it so the working tree stays clean.

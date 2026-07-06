// JFR extraction helpers. Shells out to the `jfr` CLI (bundled with the JDK)
// to pull structured JSON for the event types we visualize, then reduces each
// stream into compact, chart-ready arrays.

import { execFile } from "node:child_process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

// jfr JSON can be large (allocation/execution samples with full stack traces),
// so give the child a generous stdout buffer.
const MAX_BUFFER = 256 * 1024 * 1024;

/** Parse an ISO-8601 duration like "PT0.0033S" / "PT1M2.5S" into seconds. */
export function isoDurationToSeconds(s) {
    if (typeof s !== "string") return 0;
    const m = /^PT(?:(-?\d+(?:\.\d+)?)H)?(?:(-?\d+(?:\.\d+)?)M)?(?:(-?\d+(?:\.\d+)?)S)?$/.exec(s);
    if (!m) return 0;
    const h = parseFloat(m[1] || "0");
    const min = parseFloat(m[2] || "0");
    const sec = parseFloat(m[3] || "0");
    return h * 3600 + min * 60 + sec;
}

function toEpochMs(iso) {
    const t = Date.parse(iso);
    return Number.isNaN(t) ? 0 : t;
}

/** Run `jfr print --json --events <event> <file>` and return the events array. */
async function jfrEvents(jfrBin, file, events, extraArgs = []) {
    const args = ["print", "--json", "--events", events, ...extraArgs, file];
    const { stdout } = await execFileAsync(jfrBin, args, { maxBuffer: MAX_BUFFER });
    const parsed = JSON.parse(stdout);
    return parsed?.recording?.events ?? [];
}

/**
 * Run `jfr view <view> <file>` and parse its fixed-width table. JFR views are
 * server-side aggregations, so their output is bounded regardless of how many
 * raw events the recording holds — unlike `jfr print`, which can emit gigabytes
 * for a long run and blow past any stdout buffer.
 *
 * Rows are parsed from the right: the trailing `numValueCols` whitespace-
 * delimited tokens are the value columns; everything before them is the label
 * (which may itself contain spaces, e.g. a method signature).
 *
 * @returns {Array<{label:string, values:string[]}>}
 */
async function jfrViewRows(jfrBin, file, view, numValueCols) {
    const { stdout } = await execFileAsync(
        jfrBin,
        ["view", "--width", "400", "--cell-height", "1", view, file],
        { maxBuffer: 32 * 1024 * 1024 },
    );
    const lines = stdout.split("\n");
    let started = false;
    const rows = [];
    for (const raw of lines) {
        const line = raw.replace(/\s+$/, "");
        if (!started) {
            // The dashed separator row marks the start of data.
            if (/^-{3,}(\s+-{3,})*\s*$/.test(line)) started = true;
            continue;
        }
        if (!line.trim()) break; // blank line ends the table
        const toks = line.trim().split(/\s+/);
        if (toks.length <= numValueCols) continue;
        const values = toks.slice(toks.length - numValueCols);
        const label = toks.slice(0, toks.length - numValueCols).join(" ");
        rows.push({ label, values });
    }
    return rows;
}

/** Parse a `jfr summary` table into a Map of event name → event count. */
async function jfrEventCounts(jfrBin, file) {
    const counts = new Map();
    try {
        const { stdout } = await execFileAsync(jfrBin, ["summary", file], { maxBuffer: 8 * 1024 * 1024 });
        for (const line of stdout.split("\n")) {
            const m = line.trim().match(/^(jdk\.\S+)\s+(\d+)\s+\d+/);
            if (m) counts.set(m[1], Number(m[2]));
        }
    } catch {}
    return counts;
}

const pctToNumber = (s) => {
    const n = parseFloat(String(s).replace("%", ""));
    return Number.isFinite(n) ? n : 0;
};

/**
 * Extract every JFR stream we care about and reduce it to chart-ready data.
 * All failures are tolerated: if `jfr` or an event stream is unavailable we
 * return what we have and mark the rest empty, so a partial recording still
 * renders.
 */
export async function extractJfr(jfrBin, file, { topN = 15 } = {}) {
    const out = {
        available: false,
        jvm: null,
        gcConfig: null,
        cpuLoad: [],
        heapSummary: [],
        collections: [],
        topAllocations: [],
        hotMethods: [],
        totalAllocatedMb: 0,
        sampleCount: 0,
        allocationSampleCount: 0,
    };

    // Anchor all relative timestamps to the earliest event we observe.
    let originMs = Infinity;
    const rel = (iso) => {
        const ms = toEpochMs(iso);
        if (ms < originMs) originMs = ms;
        return ms;
    };

    // --- JVM information & GC configuration -------------------------------
    try {
        const [jvm] = await jfrEvents(jfrBin, file, "jdk.JVMInformation");
        if (jvm) {
            const v = jvm.values;
            out.jvm = {
                name: v.jvmName,
                version: v.jvmVersion,
                args: v.jvmArguments,
                javaArgs: v.javaArguments,
                pid: v.pid,
                startTime: v.jvmStartTime,
            };
        }
    } catch {}

    try {
        const [cfg] = await jfrEvents(jfrBin, file, "jdk.GCConfiguration");
        if (cfg) {
            const v = cfg.values;
            out.gcConfig = {
                youngCollector: v.youngCollector,
                oldCollector: v.oldCollector,
                parallelGCThreads: v.parallelGCThreads,
                concurrentGCThreads: v.concurrentGCThreads,
                usesDynamicGCThreads: v.usesDynamicGCThreads,
                gcTimeRatio: v.gcTimeRatio,
            };
        }
    } catch {}

    // --- CPU load over time -----------------------------------------------
    try {
        const events = await jfrEvents(jfrBin, file, "jdk.CPULoad");
        out.cpuLoad = events.map((e) => ({
            ms: rel(e.values.startTime),
            jvmUser: e.values.jvmUser,
            jvmSystem: e.values.jvmSystem,
            machineTotal: e.values.machineTotal,
        }));
    } catch {}

    // --- Heap occupancy samples -------------------------------------------
    try {
        const events = await jfrEvents(jfrBin, file, "jdk.GCHeapSummary");
        out.heapSummary = events.map((e) => ({
            ms: rel(e.values.startTime),
            gcId: e.values.gcId,
            when: e.values.when,
            usedMb: (e.values.heapUsed ?? 0) / (1024 * 1024),
            committedMb: (e.values.heapSpace?.committedSize ?? 0) / (1024 * 1024),
        }));
    } catch {}

    // --- Garbage collection pauses ----------------------------------------
    try {
        const events = await jfrEvents(jfrBin, file, "jdk.GarbageCollection");
        out.collections = events.map((e) => ({
            ms: rel(e.values.startTime),
            gcId: e.values.gcId,
            name: e.values.name,
            cause: e.values.cause,
            longestPauseMs: isoDurationToSeconds(e.values.longestPause) * 1000,
            sumOfPausesMs: isoDurationToSeconds(e.values.sumOfPauses) * 1000,
        }));
    } catch {}

    // Bounded, server-side aggregations (jfr view) rather than `jfr print`, so
    // a long recording with millions of samples can't overflow the buffer.
    const counts = await jfrEventCounts(jfrBin, file);
    out.sampleCount = counts.get("jdk.ExecutionSample") ?? 0;
    out.allocationSampleCount = counts.get("jdk.ObjectAllocationSample") ?? 0;

    // --- Top allocations (allocation pressure by class) -------------------
    // The view reports each class's share of total sampled allocation weight
    // ("Allocation Pressure %"). Absolute MB is reconstructed downstream from
    // GCToolkit's accurate allocation rate; the JFR stream alone can't give a
    // trustworthy absolute total for a sampled recording.
    try {
        const rows = await jfrViewRows(jfrBin, file, "allocation-by-class", 1);
        out.topAllocations = rows
            .map((r) => ({ name: r.label, pressurePct: pctToNumber(r.values[0]), weightMb: null }))
            .filter((a) => a.pressurePct > 0)
            .sort((a, b) => b.pressurePct - a.pressurePct)
            .slice(0, topN);
    } catch {}

    // --- Hot methods (execution-sample top frames) ------------------------
    try {
        const rows = await jfrViewRows(jfrBin, file, "hot-methods", 2);
        out.hotMethods = rows
            .map((r) => ({ name: r.label, samples: Number(r.values[0]) || 0, pct: pctToNumber(r.values[1]) }))
            .filter((m) => m.samples > 0)
            .sort((a, b) => b.samples - a.samples)
            .slice(0, topN);
    } catch {}

    // Normalize relative timestamps to seconds from the recording origin.
    if (Number.isFinite(originMs)) {
        const norm = (arr) => arr.forEach((d) => { d.t = Math.max(0, (d.ms - originMs) / 1000); delete d.ms; });
        norm(out.cpuLoad);
        norm(out.heapSummary);
        norm(out.collections);
    }

    out.available =
        out.cpuLoad.length > 0 ||
        out.heapSummary.length > 0 ||
        out.collections.length > 0 ||
        out.topAllocations.length > 0 ||
        out.hotMethods.length > 0;

    return out;
}

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

function topFrameName(event) {
    const frames = event?.values?.stackTrace?.frames;
    if (!Array.isArray(frames) || frames.length === 0) return null;
    const method = frames[0]?.method;
    if (!method) return null;
    const type = method?.type?.name?.replace(/\//g, ".") ?? "?";
    return `${type}.${method.name}`;
}

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

    // --- Top allocation sites (by sampled weight) -------------------------
    try {
        const events = await jfrEvents(jfrBin, file, "jdk.ObjectAllocationSample");
        const byClass = new Map();
        let totalWeight = 0;
        for (const e of events) {
            const cls = (e.values.objectClass?.name ?? "?").replace(/\//g, ".");
            const weight = e.values.weight ?? 0;
            totalWeight += weight;
            const cur = byClass.get(cls) ?? { weight: 0, samples: 0 };
            cur.weight += weight;
            cur.samples += 1;
            byClass.set(cls, cur);
        }
        out.totalAllocatedMb = totalWeight / (1024 * 1024);
        out.topAllocations = [...byClass.entries()]
            .map(([name, v]) => ({ name, weightMb: v.weight / (1024 * 1024), samples: v.samples }))
            .sort((a, b) => b.weightMb - a.weightMb)
            .slice(0, topN);
    } catch {}

    // --- Hot methods (execution-sample top frames) ------------------------
    try {
        const events = await jfrEvents(jfrBin, file, "jdk.ExecutionSample");
        const byMethod = new Map();
        let total = 0;
        for (const e of events) {
            const name = topFrameName(e);
            if (!name) continue;
            total += 1;
            byMethod.set(name, (byMethod.get(name) ?? 0) + 1);
        }
        out.sampleCount = total;
        out.hotMethods = [...byMethod.entries()]
            .map(([name, samples]) => ({ name, samples, pct: total ? (samples / total) * 100 : 0 }))
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

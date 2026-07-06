// Extension: gc-jfr-analyzer
// Generic Java GC + JFR profiling canvas. The "Run analysis" button asks Copilot
// to build and run the current project's workload with GC logging + JFR enabled
// (choosing flags for the detected build tool + JDK). Copilot hands the produced
// gc.log / dump.jfr back via the `gc_jfr_ingest` tool, which analyzes them with
// Microsoft GCToolkit (GC log) and the `jfr` CLI (flight recording) and
// visualizes everything in an interactive canvas.

import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { join, dirname, extname, normalize, isAbsolute } from "node:path";
import { fileURLToPath } from "node:url";
import { joinSession, createCanvas, CanvasError } from "@github/copilot-sdk/extension";
import { analyzeArtifacts, loadLatest, readArtifact, TOOL_PATHS } from "./lib/pipeline.mjs";
import { buildAnalysisPrompt, buildRunPrompt } from "./lib/prompt.mjs";

const __dirname = dirname(fileURLToPath(import.meta.url));
const WEB_DIR = join(__dirname, "web");

const MIME = {
    ".html": "text/html; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".svg": "image/svg+xml",
};

// --- Shared run state (a single analysis runs at a time) --------------------
const runState = {
    running: false,
    lastReport: null,
    subscribers: new Set(), // SSE response objects
};

let sessionRef = null;
const log = (msg, level = "info") => { try { sessionRef?.log(msg, { level }); } catch {} };

function broadcast(event, data) {
    const payload = `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
    for (const res of runState.subscribers) {
        try { res.write(payload); } catch {}
    }
}

async function ingestArtifacts(args) {
    if (runState.running) throw new Error("An analysis is already running.");
    runState.running = true;
    broadcast("progress", { pct: 1, msg: "Ingesting run artifacts…" });
    try {
        const report = await analyzeArtifacts(args, (msg, pct) => broadcast("progress", { pct: pct ?? undefined, msg }));
        runState.lastReport = report;
        broadcast("done", { report });
        log(`GC/JFR analysis complete: ${report.gc?.summary?.eventCount ?? 0} GC events, throughput ${report.gc?.summary?.throughputPercent ?? "?"}%`);
        return report;
    } catch (err) {
        broadcast("failed", { error: String(err?.message || err) });
        log(`GC/JFR analysis failed: ${err?.message || err}`, "error");
        throw err;
    } finally {
        runState.running = false;
    }
}

/**
 * Ask Copilot to build and run this project's workload with GC logging + JFR
 * enabled, then hand the artifacts back via the `gc_jfr_ingest` tool. Injects a
 * user turn into the current session; the agent does the project-specific work.
 */
async function requestRun(opts = {}) {
    const { prompt, displayPrompt } = buildRunPrompt({
        workspacePath: sessionRef?.workspacePath,
        hint: opts.hint,
        jfrMaxSizeMb: opts.jfrMaxSizeMb,
    });
    await sessionRef.send({ prompt, displayPrompt });
    broadcast("awaiting", { msg: "Asked Copilot to run the workload…" });
    log("Asked Copilot to run the project workload with GC logging + JFR.");
    return { requested: true, displayPrompt };
}

/**
 * Hand the latest analysis to the Copilot session for AI review. Injects a user
 * turn (with the full report + jfr views attached) so the agent produces GC
 * tuning and code-optimization recommendations grounded in the run's numbers.
 */
async function sendToCopilot() {
    const report = runState.lastReport ?? (await loadLatest());
    if (!report || !report.gc?.summary) {
        throw new Error("No analysis available yet. Run an analysis first.");
    }
    const { prompt, displayPrompt, attachments } = buildAnalysisPrompt(report);
    await sessionRef.send({ prompt, displayPrompt, attachments });
    log("Sent GC/JFR analysis to Copilot for AI recommendations.");
    return { sent: true, runId: report.runId, displayPrompt };
}

// --- HTTP server (one per open canvas instance) -----------------------------
const servers = new Map();

async function serveStatic(req, res) {
    let path = decodeURIComponent(new URL(req.url, "http://localhost").pathname);
    if (path === "/" || path === "") path = "/index.html";
    // Prevent path traversal: resolve within WEB_DIR only.
    const filePath = normalize(join(WEB_DIR, path));
    if (!filePath.startsWith(WEB_DIR)) { res.writeHead(403).end("forbidden"); return; }
    try {
        const buf = await readFile(filePath);
        res.writeHead(200, { "Content-Type": MIME[extname(filePath)] || "application/octet-stream" });
        res.end(buf);
    } catch {
        res.writeHead(404).end("not found");
    }
}

function readBody(req) {
    return new Promise((resolve) => {
        let b = "";
        req.on("data", (c) => (b += c));
        req.on("end", () => resolve(b));
    });
}

async function handleRequest(req, res) {
    const url = new URL(req.url, "http://localhost");
    const path = url.pathname;

    if (path === "/state") {
        const report = runState.lastReport ?? (await loadLatest());
        res.writeHead(200, { "Content-Type": MIME[".json"] });
        res.end(JSON.stringify(report ?? { empty: true }));
        return;
    }

    if (path === "/views") {
        const report = runState.lastReport ?? (await loadLatest());
        const text = report?.artifacts?.views ? await readArtifact(report.artifacts.views) : null;
        res.writeHead(200, { "Content-Type": "text/plain; charset=utf-8" });
        res.end(text ?? "");
        return;
    }

    if (path === "/events") {
        res.writeHead(200, {
            "Content-Type": "text/event-stream",
            "Cache-Control": "no-cache",
            Connection: "keep-alive",
        });
        res.write(`event: hello\ndata: {"running":${runState.running}}\n\n`);
        runState.subscribers.add(res);
        req.on("close", () => runState.subscribers.delete(res));
        return;
    }

    if (path === "/run" && req.method === "POST") {
        if (runState.running) { res.writeHead(409, { "Content-Type": MIME[".json"] }); res.end(JSON.stringify({ error: "an analysis is already running" })); return; }
        let opts = {};
        try { opts = JSON.parse((await readBody(req)) || "{}"); } catch {}
        try {
            const result = await requestRun(opts);
            res.writeHead(202, { "Content-Type": MIME[".json"] });
            res.end(JSON.stringify({ started: true, ...result }));
        } catch (err) {
            res.writeHead(500, { "Content-Type": MIME[".json"] });
            res.end(JSON.stringify({ error: String(err?.message || err) }));
        }
        return;
    }

    if (path === "/analyze" && req.method === "POST") {
        try {
            const result = await sendToCopilot();
            res.writeHead(200, { "Content-Type": MIME[".json"] });
            res.end(JSON.stringify(result));
        } catch (err) {
            res.writeHead(400, { "Content-Type": MIME[".json"] });
            res.end(JSON.stringify({ error: String(err?.message || err) }));
        }
        return;
    }

    if (path === "/tools") {
        res.writeHead(200, { "Content-Type": MIME[".json"] });
        res.end(JSON.stringify(TOOL_PATHS));
        return;
    }

    await serveStatic(req, res);
}

async function startServer() {
    const server = createServer((req, res) => { handleRequest(req, res).catch(() => { try { res.writeHead(500).end("error"); } catch {} }); });
    await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
    const address = server.address();
    const port = typeof address === "object" && address ? address.port : 0;
    return { server, url: `http://127.0.0.1:${port}/` };
}

// --- Canvas declaration -----------------------------------------------------
sessionRef = await joinSession({
    canvases: [
        createCanvas({
            id: "gc-jfr-analyzer",
            displayName: "GC & JFR Analysis",
            description: "Profile any Java project: Copilot runs its workload with GC logging + JFR, then GCToolkit/JFR analysis is visualized here.",
            inputSchema: { type: "object", properties: {}, additionalProperties: true },
            actions: [
                {
                    name: "run_analysis",
                    description: "Ask Copilot to build and run this project's workload with GC logging + JFR enabled (detecting the build tool and JDK), then ingest the results via the gc_jfr_ingest tool. Does not run anything itself — it injects a request into the session.",
                    inputSchema: {
                        type: "object",
                        properties: {
                            hint: { type: "string", description: "Optional guidance on what workload to run (e.g. 'run the JMH benchmark in module X', 'run mvn verify')." },
                            jfrMaxSizeMb: { type: "integer", description: "Max JFR recording size in MB (default 100)." },
                        },
                        additionalProperties: false,
                    },
                    handler: async (ctx) => requestRun(ctx.input || {}),
                },
                {
                    name: "analyze_with_copilot",
                    description: "Send the latest GC/JFR analysis to the Copilot session for AI review, producing JVM tuning and code-optimization recommendations grounded in the run's metrics.",
                    handler: async () => {
                        try {
                            return await sendToCopilot();
                        } catch (err) {
                            throw new CanvasError("no_analysis", String(err?.message || err));
                        }
                    },
                },
                {
                    name: "load_results",
                    description: "Load the most recent saved analysis and return its summary without re-running the workload.",
                    handler: async () => {
                        const report = runState.lastReport ?? (await loadLatest());
                        if (!report) throw new CanvasError("not_found", "No saved analysis yet. Run an analysis first.");
                        return {
                            runId: report.runId,
                            generatedAt: report.generatedAt,
                            label: report.label ?? null,
                            gc: report.gc?.summary ?? null,
                            gcConfig: report.gcConfig ?? null,
                            jfrAvailable: report.jfr?.available ?? false,
                        };
                    },
                },
                {
                    name: "tool_status",
                    description: "Report the resolved paths of the jbang and jfr tools used by the analyzer.",
                    handler: async () => ({ ...TOOL_PATHS }),
                },
            ],
            open: async (ctx) => {
                let entry = servers.get(ctx.instanceId);
                if (!entry) {
                    entry = await startServer();
                    servers.set(ctx.instanceId, entry);
                }
                return { title: "GC & JFR Analysis", url: entry.url, status: runState.running ? "running" : undefined };
            },
            onClose: async (ctx) => {
                const entry = servers.get(ctx.instanceId);
                if (entry) {
                    servers.delete(ctx.instanceId);
                    await new Promise((resolve) => entry.server.close(() => resolve()));
                }
            },
        }),
    ],
    tools: [
        {
            name: "gc_jfr_ingest",
            description:
                "Ingest a GC log (and optional JFR recording) produced by a Java workload, analyze them with Microsoft GCToolkit + the jfr CLI, and visualize the results in the GC & JFR Analysis canvas. Call this after you have run a workload with GC logging + JFR enabled. Paths must point to files on disk.",
            parameters: {
                type: "object",
                properties: {
                    gcLogPath: { type: "string", description: "Absolute path to the GC log file (unified -Xlog:gc* output or JDK 8 -Xloggc output)." },
                    jfrPath: { type: "string", description: "Absolute path to the .jfr flight recording, if one was produced. Optional but strongly recommended." },
                    label: { type: "string", description: "Short human-readable description of the workload and notable JVM flags (e.g. 'JMH io-bench, -Xmx512m G1')." },
                },
                required: ["gcLogPath"],
                additionalProperties: false,
            },
            skipPermission: true,
            handler: async (args) => {
                const resolveArg = (p) => (p && !isAbsolute(p) && sessionRef?.workspacePath ? join(sessionRef.workspacePath, p) : p);
                const gcLogPath = resolveArg(args?.gcLogPath);
                const jfrPath = resolveArg(args?.jfrPath);
                try {
                    const report = await ingestArtifacts({ gcLogPath, jfrPath, label: args?.label });
                    const s = report.gc?.summary ?? {};
                    const jfr = report.jfr ?? {};
                    const lines = [
                        `Ingested GC/JFR run ${report.runId}${report.label ? ` — ${report.label}` : ""}.`,
                        `GC: ${s.eventCount ?? 0} events, throughput ${s.throughputPercent ?? "?"}%, avg pause ${s.avgPauseMs ?? "?"} ms, p99 ${s.p99PauseMs ?? "?"} ms, alloc ${s.allocRateMbPerSec ?? "?"} MB/s, peak heap ${s.peakHeapKb ? Math.round(s.peakHeapKb / 1024) + " MB" : "?"}.`,
                        jfr.available
                            ? `JFR: ${jfr.sampleCount ?? 0} execution samples, ~${jfr.totalAllocatedMb ?? 0} MB sampled allocations.`
                            : "JFR: not available (no recording ingested).",
                        `Results are now visualized in the GC & JFR Analysis canvas. Use the "Analyze with AI" button (or ask me to analyze) for tuning recommendations.`,
                    ];
                    return lines.join("\n");
                } catch (err) {
                    return `Failed to ingest GC/JFR artifacts: ${err?.message || err}`;
                }
            },
        },
    ],
});

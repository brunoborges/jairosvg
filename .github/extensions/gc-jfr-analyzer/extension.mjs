// Extension: gc-jfr-analyzer
// Runs the JairoSVG benchmark as a JVM workload with GC logging + JFR enabled,
// analyzes the results with Microsoft GCToolkit (GC log) and the `jfr` CLI
// (flight recording), and visualizes everything in an interactive canvas.

import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { join, dirname, extname, normalize } from "node:path";
import { fileURLToPath } from "node:url";
import { joinSession, createCanvas, CanvasError } from "@github/copilot-sdk/extension";
import { runPipeline, loadLatest, readArtifact, TOOL_PATHS } from "./lib/pipeline.mjs";
import { buildAnalysisPrompt } from "./lib/prompt.mjs";

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

async function startAnalysis(opts) {
    if (runState.running) throw new Error("An analysis is already running.");
    runState.running = true;
    broadcast("progress", { pct: 1, msg: "Starting…" });
    try {
        const report = await runPipeline(opts, (msg, pct) => broadcast("progress", { pct: pct ?? undefined, msg }));
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
        if (runState.running) { res.writeHead(409, { "Content-Type": MIME[".json"] }); res.end(JSON.stringify({ error: "already running" })); return; }
        let opts = {};
        try { opts = JSON.parse((await readBody(req)) || "{}"); } catch {}
        res.writeHead(202, { "Content-Type": MIME[".json"] });
        res.end(JSON.stringify({ started: true }));
        // Fire and forget; progress is delivered over SSE.
        startAnalysis(opts).catch(() => {});
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
            description: "Run the JairoSVG benchmark with GC logging + JFR and visualize GCToolkit/JFR analysis.",
            inputSchema: { type: "object", properties: {}, additionalProperties: true },
            actions: [
                {
                    name: "run_analysis",
                    description: "Run the JairoSVG benchmark workload with GC logging + JFR, analyze it, and update the canvas. Returns summary statistics.",
                    inputSchema: {
                        type: "object",
                        properties: {
                            warmup: { type: "integer", description: "Warmup iterations per case (default 20)." },
                            iterations: { type: "integer", description: "Measured iterations per case (default 200)." },
                            heapMb: { type: "integer", description: "Max heap in MB; smaller = more GC activity (default 256)." },
                            engines: { type: "string", enum: ["jairosvg", "all"], description: "'jairosvg' only or 'all' Java engines (default jairosvg)." },
                            skipMvnInstall: { type: "boolean", description: "Skip the mvn install step if JairoSVG is already built." },
                        },
                        additionalProperties: false,
                    },
                    handler: async (ctx) => {
                        const report = await startAnalysis(ctx.input || {});
                        return {
                            runId: report.runId,
                            wallTimeSec: report.durationRealSec,
                            gc: report.gc?.summary ?? null,
                            causes: report.gc?.causes ?? {},
                            types: report.gc?.types ?? {},
                            gcConfig: report.gcConfig ?? null,
                            jfr: {
                                available: report.jfr?.available ?? false,
                                totalAllocatedMb: report.jfr?.totalAllocatedMb ?? 0,
                                topAllocations: (report.jfr?.topAllocations ?? []).slice(0, 5),
                                hotMethods: (report.jfr?.hotMethods ?? []).slice(0, 5),
                            },
                        };
                    },
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
                            gc: report.gc?.summary ?? null,
                            gcConfig: report.gcConfig ?? null,
                            jfrAvailable: report.jfr?.available ?? false,
                        };
                    },
                },
                {
                    name: "tool_status",
                    description: "Report the resolved paths of the jbang, jfr, and mvnw tools used by the pipeline.",
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
});

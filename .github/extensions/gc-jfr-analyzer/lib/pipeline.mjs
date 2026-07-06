// Analyzer: given a GC log and (optionally) a JFR recording produced by any
// Java workload, parse them with Microsoft GCToolkit (GC log) and the `jfr` CLI
// (flight recording) and produce a single combined report. This module is
// project-agnostic — the actual workload is built and run by the Copilot agent,
// which hands the resulting artifact paths to `analyzeArtifacts`.

import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { existsSync } from "node:fs";
import { mkdir, readFile, writeFile, copyFile } from "node:fs/promises";
import { join, dirname, resolve, basename } from "node:path";
import { fileURLToPath } from "node:url";
import { homedir } from "node:os";
import { extractJfr } from "./jfr.mjs";

const execFileAsync = promisify(execFile);
const __dirname = dirname(fileURLToPath(import.meta.url));
const EXT_DIR = resolve(__dirname, "..");
const RUNS_DIR = join(EXT_DIR, "runs");
const LATEST_JSON = join(RUNS_DIR, "latest.json");
const ANALYZER = join(EXT_DIR, "tools", "GcLogAnalyzer.java");

export { RUNS_DIR, LATEST_JSON };

// ---- tool resolution -------------------------------------------------------
function firstExisting(candidates, fallback) {
    for (const c of candidates) {
        if (c && existsSync(c)) return c;
    }
    return fallback;
}

function resolveJfr() {
    const cands = [];
    if (process.env.JAVA_HOME) cands.push(join(process.env.JAVA_HOME, "bin", "jfr"));
    for (const dir of (process.env.PATH || "").split(":")) {
        if (dir) cands.push(join(dir, "jfr"));
    }
    cands.push(join(homedir(), ".sdkman", "candidates", "java", "current", "bin", "jfr"));
    return firstExisting(cands, "jfr");
}

function resolveJbang() {
    const cands = [
        join(homedir(), ".jbang", "bin", "jbang"),
        "/opt/homebrew/bin/jbang",
        "/usr/local/bin/jbang",
    ];
    for (const dir of (process.env.PATH || "").split(":")) {
        if (dir) cands.push(join(dir, "jbang"));
    }
    return firstExisting(cands, "jbang");
}

/**
 * Analyze a GC log + optional JFR recording produced by any Java workload.
 * The artifacts are copied into a fresh, self-contained run directory.
 *
 * @param {object} args { gcLogPath, jfrPath?, label? }
 * @param {(msg:string,pct?:number)=>void} onProgress
 * @returns combined report object (also persisted to runs/latest.json)
 */
export async function analyzeArtifacts(args = {}, onProgress = () => {}) {
    const { gcLogPath, jfrPath, label } = args;
    const progress = (msg, pct) => onProgress(msg, pct);

    if (!gcLogPath || !existsSync(gcLogPath)) {
        throw new Error(`GC log not found: ${gcLogPath || "(missing gcLogPath)"}`);
    }
    const haveJfr = jfrPath && existsSync(jfrPath);

    const jbang = resolveJbang();
    const jfr = resolveJfr();

    const runId = new Date().toISOString().replace(/[:.]/g, "-");
    const runDir = join(RUNS_DIR, runId);
    await mkdir(runDir, { recursive: true });

    // Copy artifacts in so each run is self-contained and re-analyzable.
    progress("Collecting run artifacts…", 8);
    const gcLog = join(runDir, "gc.log");
    await copyFile(gcLogPath, gcLog);
    let jfrFile = null;
    if (haveJfr) {
        jfrFile = join(runDir, "dump.jfr");
        await copyFile(jfrPath, jfrFile);
    }
    const viewsFile = join(runDir, "jfr-views.txt");

    // 1. Analyze the GC log with GCToolkit.
    progress("Analyzing GC log with Microsoft GCToolkit…", 30);
    let gc = null;
    try {
        const { stdout } = await execFileAsync(jbang, [ANALYZER, gcLog], {
            maxBuffer: 128 * 1024 * 1024,
        });
        gc = JSON.parse(stdout);
    } catch (e) {
        gc = { error: String(e?.message || e), summary: null, events: [], causes: {}, types: {} };
    }

    // 2. Extract structured JFR data + the human-readable views report.
    let jfrData = { available: false };
    if (jfrFile) {
        progress("Extracting JFR flight-recording data…", 65);
        try { jfrData = await extractJfr(jfr, jfrFile, { topN: 15 }); } catch (e) {
            jfrData = { available: false, error: String(e?.message || e) };
        }
        try {
            progress("Generating `jfr view all-views` report…", 88);
            const { stdout } = await execFileAsync(jfr, ["view", "all-views", jfrFile], { maxBuffer: 64 * 1024 * 1024 });
            await writeFile(viewsFile, stdout);
        } catch {}
    }

    const report = {
        schema: 1,
        generatedAt: new Date().toISOString(),
        runId,
        label: label || null,
        source: {
            gcLogPath,
            jfrPath: haveJfr ? jfrPath : null,
            gcLogName: basename(gcLogPath),
        },
        jvm: jfrData.jvm ?? null,
        gcConfig: jfrData.gcConfig ?? null,
        gc,
        jfr: jfrData,
        artifacts: {
            dir: runDir,
            gcLog,
            jfr: jfrFile,
            views: existsSync(viewsFile) ? viewsFile : null,
        },
    };

    await writeFile(join(runDir, "report.json"), JSON.stringify(report, null, 2));
    await writeFile(LATEST_JSON, JSON.stringify(report, null, 2));
    progress("Analysis complete.", 100);
    return report;
}

/** Load the most recent persisted report, or null if none exists. */
export async function loadLatest() {
    try {
        return JSON.parse(await readFile(LATEST_JSON, "utf8"));
    } catch {
        return null;
    }
}

/** Read a saved artifact (e.g. the jfr views text) as a string. */
export async function readArtifact(path) {
    try { return await readFile(path, "utf8"); } catch { return null; }
}

export const TOOL_PATHS = { jbang: resolveJbang(), jfr: resolveJfr() };

// Orchestration pipeline: run the JairoSVG benchmark as a JVM workload with GC
// logging + JFR enabled, then analyze the results with Microsoft GCToolkit (GC
// log) and the `jfr` CLI (flight recording), producing a single combined report.

import { spawn } from "node:child_process";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { existsSync } from "node:fs";
import { mkdir, readFile, writeFile, readdir, unlink } from "node:fs/promises";
import { join, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { homedir } from "node:os";
import { extractJfr } from "./jfr.mjs";

const execFileAsync = promisify(execFile);
const __dirname = dirname(fileURLToPath(import.meta.url));
const EXT_DIR = resolve(__dirname, "..");
// The extension lives at <repoRoot>/.github/extensions/gc-jfr-analyzer.
const REPO_ROOT = resolve(EXT_DIR, "..", "..", "..");
const RUNS_DIR = join(EXT_DIR, "runs");
const LATEST_JSON = join(RUNS_DIR, "latest.json");
const ANALYZER = join(EXT_DIR, "tools", "GcLogAnalyzer.java");

export { REPO_ROOT, RUNS_DIR, LATEST_JSON };

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

function resolveMvnw() {
    const w = join(REPO_ROOT, process.platform === "win32" ? "mvnw.cmd" : "mvnw");
    return existsSync(w) ? w : "mvn";
}

// ---- process helper --------------------------------------------------------
function runProc(cmd, args, { cwd, onLine, env } = {}) {
    return new Promise((resolvePromise, reject) => {
        const child = spawn(cmd, args, { cwd, env: env ?? process.env });
        let stdout = "";
        let stderr = "";
        let buf = "";
        const pump = (chunk, isErr) => {
            const text = chunk.toString();
            if (isErr) stderr += text;
            else stdout += text;
            if (!onLine) return;
            buf += text;
            let idx;
            while ((idx = buf.indexOf("\n")) >= 0) {
                const line = buf.slice(0, idx).trim();
                buf = buf.slice(idx + 1);
                if (line) onLine(line);
            }
        };
        child.stdout.on("data", (c) => pump(c, false));
        child.stderr.on("data", (c) => pump(c, true));
        child.on("error", reject);
        child.on("close", (code) => {
            if (buf.trim() && onLine) onLine(buf.trim());
            resolvePromise({ code, stdout, stderr });
        });
    });
}

// ---- pipeline --------------------------------------------------------------
const DEFAULTS = { warmup: 20, iterations: 200, heapMb: 256, jfrMaxSizeMb: 100, engines: "jairosvg" };

/**
 * Run the full workload → analyze pipeline.
 * @param {object} opts        { warmup, iterations, heapMb, jfrMaxSizeMb, engines, skipMvnInstall }
 * @param {(msg:string,pct?:number)=>void} onProgress
 * @returns combined report object (also persisted to runs/latest.json)
 */
export async function runPipeline(opts = {}, onProgress = () => {}) {
    const cfg = { ...DEFAULTS, ...opts };
    const jbang = resolveJbang();
    const jfr = resolveJfr();
    const mvnw = resolveMvnw();

    const runId = new Date().toISOString().replace(/[:.]/g, "-");
    const runDir = join(RUNS_DIR, runId);
    await mkdir(runDir, { recursive: true });
    const gcLog = join(runDir, "gc.log");
    const jfrFile = join(runDir, "dump.jfr");
    const viewsFile = join(runDir, "jfr-views.txt");

    const started = Date.now();
    const progress = (msg, pct) => onProgress(msg, pct);

    // 1. Ensure the JairoSVG snapshot the benchmark depends on is installed.
    if (!cfg.skipMvnInstall) {
        progress("Building JairoSVG (mvn install -DskipTests)…", 5);
        const mvn = await runProc(mvnw, ["-q", "install", "-DskipTests"], {
            cwd: REPO_ROOT,
            onLine: (l) => { if (/BUILD|ERROR/.test(l)) progress(l); },
        });
        if (mvn.code !== 0) {
            throw new Error(`mvn install failed (exit ${mvn.code}). ${mvn.stderr.slice(-500)}`);
        }
    }

    // 2. Snapshot the file the benchmark rewrites so the repo stays clean.
    const comparisonMd = join(REPO_ROOT, "comparison", "COMPARISON.md");
    let comparisonBackup = null;
    try { comparisonBackup = await readFile(comparisonMd, "utf8"); } catch {}
    // The benchmark drops a timestamped log into cwd; remember pre-existing ones.
    const preLogs = new Set(await listBenchmarkLogs());

    // 3. Run the benchmark JVM with GC logging + JFR enabled.
    const engineFlags = cfg.engines === "all" ? ["--no-cairosvg"] : ["--no-cairosvg", "--no-echosvg", "--no-jsvg"];
    const benchArgs = [
        "-R", `-Xlog:gc*:file=${gcLog}:time,uptime,level,tags`,
        "-R", `-XX:StartFlightRecording=maxsize=${cfg.jfrMaxSizeMb}M,filename=${jfrFile},settings=profile`,
        "-R", `-Xmx${cfg.heapMb}m`,
        join("comparison", "benchmark", "benchmark.java"),
        ...engineFlags, "--no-progress", `--warmup=${cfg.warmup}`, `--iterations=${cfg.iterations}`,
    ];
    progress(`Running benchmark workload (warmup=${cfg.warmup}, iterations=${cfg.iterations}, heap=${cfg.heapMb}MB)…`, 15);
    const bench = await runProc(jbang, benchArgs, {
        cwd: REPO_ROOT,
        onLine: (l) => {
            if (/Measuring|Warming|avg=|Benchmark|Updated/.test(l)) progress(l);
        },
    });

    // 4. Restore the benchmark side-effect regardless of outcome.
    if (comparisonBackup != null) {
        try { await writeFile(comparisonMd, comparisonBackup); } catch {}
    }
    // The benchmark writes a timestamped log into cwd; remove any it just made.
    progress("Workload finished, cleaning up transient files…", 55);
    for (const f of await listBenchmarkLogs()) {
        if (!preLogs.has(f)) { try { await unlink(join(REPO_ROOT, f)); } catch {} }
    }

    if (bench.code !== 0 || !existsSync(gcLog)) {
        throw new Error(`Benchmark workload failed (exit ${bench.code}). ${bench.stderr.slice(-600)}`);
    }

    // 5. Analyze the GC log with GCToolkit.
    progress("Analyzing GC log with Microsoft GCToolkit…", 60);
    let gc = null;
    try {
        const { stdout } = await execFileAsync(jbang, [ANALYZER, gcLog], {
            cwd: REPO_ROOT, maxBuffer: 128 * 1024 * 1024,
        });
        gc = JSON.parse(stdout);
    } catch (e) {
        gc = { error: String(e?.message || e), summary: null, events: [], causes: {}, types: {} };
    }

    // 6. Extract structured JFR data + the human-readable views report.
    progress("Extracting JFR flight-recording data…", 78);
    let jfrData = { available: false };
    if (existsSync(jfrFile)) {
        try { jfrData = await extractJfr(jfr, jfrFile, { topN: 15 }); } catch (e) {
            jfrData = { available: false, error: String(e?.message || e) };
        }
        try {
            progress("Generating `jfr view all-views` report…", 90);
            const { stdout } = await execFileAsync(jfr, ["view", "all-views", jfrFile], { maxBuffer: 64 * 1024 * 1024 });
            await writeFile(viewsFile, stdout);
        } catch {}
    }

    const durationRealSec = (Date.now() - started) / 1000;
    const report = {
        schema: 1,
        generatedAt: new Date().toISOString(),
        runId,
        config: cfg,
        durationRealSec: Math.round(durationRealSec * 100) / 100,
        workloadCommand: `${basename(jbang)} ${benchArgs.join(" ")}`,
        jvm: jfrData.jvm ?? null,
        gcConfig: jfrData.gcConfig ?? null,
        gc,
        jfr: jfrData,
        artifacts: {
            dir: runDir,
            gcLog,
            jfr: existsSync(jfrFile) ? jfrFile : null,
            views: existsSync(viewsFile) ? viewsFile : null,
        },
    };

    await writeFile(join(runDir, "report.json"), JSON.stringify(report, null, 2));
    await writeFile(LATEST_JSON, JSON.stringify(report, null, 2));
    progress("Analysis complete.", 100);
    return report;
}

function basename(p) {
    const i = p.lastIndexOf("/");
    return i >= 0 ? p.slice(i + 1) : p;
}

async function listBenchmarkLogs() {
    try {
        return (await readdir(REPO_ROOT)).filter((f) => /^benchmark-.*\.log$/.test(f));
    } catch {
        return [];
    }
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

export const TOOL_PATHS = { jbang: resolveJbang(), jfr: resolveJfr(), mvnw: resolveMvnw(), repoRoot: REPO_ROOT };

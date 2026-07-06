"use strict";

// ---------------------------------------------------------------------------
// Small SVG charting toolkit (no dependencies). Every helper returns an SVG
// string sized to a fixed viewBox and scaled to container width via CSS.
// ---------------------------------------------------------------------------
const NS = "http://www.w3.org/2000/svg";
const fmt = (n, d = 1) => (n == null || Number.isNaN(n) ? "–" : Number(n).toLocaleString(undefined, { maximumFractionDigits: d }));
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));

const TYPE_COLORS = {
  Young: "var(--gc-young)",
  G1GCYoungInitialMark: "var(--gc-young)",
  Initial: "var(--gc-young)",
  Mixed: "var(--gc-mixed)",
  G1GCRemark: "var(--gc-mixed)",
  Remark: "var(--gc-mixed)",
  Full: "var(--gc-old)",
  SystemGC: "var(--gc-system)",
};
const colorForType = (t) => TYPE_COLORS[t] || "var(--accent)";
const PALETTE = ["#2563eb", "#7c3aed", "#059669", "#d97706", "#dc2626", "#0891b2", "#4f46e5", "#db2777", "#65a30d", "#0d9488", "#9333ea", "#ea580c"];

function niceTicks(min, max, count = 5) {
  if (min === max) { max = min + 1; }
  const span = max - min;
  const step = Math.pow(10, Math.floor(Math.log10(span / count)));
  const err = (count * step) / span;
  let mult = 1;
  if (err <= 0.15) mult = 10; else if (err <= 0.35) mult = 5; else if (err <= 0.75) mult = 2;
  const s = mult * step;
  const ticks = [];
  for (let v = Math.ceil(min / s) * s; v <= max + 1e-9; v += s) ticks.push(v);
  return ticks;
}

// Generic Cartesian frame builder. Returns { body, px, py } with scale fns.
function frame({ w = 560, h = 240, xmin, xmax, ymin, ymax, xlabel, ylabel, yfmt = (v) => fmt(v, 0) }) {
  const m = { l: 54, r: 14, t: 12, b: 34 };
  const iw = w - m.l - m.r;
  const ih = h - m.t - m.b;
  const px = (x) => m.l + (xmax === xmin ? 0 : ((x - xmin) / (xmax - xmin)) * iw);
  const py = (y) => m.t + ih - (ymax === ymin ? 0 : ((y - ymin) / (ymax - ymin)) * ih);
  let g = "";
  for (const yt of niceTicks(ymin, ymax)) {
    const y = py(yt);
    g += `<line class="grid-line" x1="${m.l}" y1="${y.toFixed(1)}" x2="${m.l + iw}" y2="${y.toFixed(1)}"/>`;
    g += `<text class="tick" x="${m.l - 6}" y="${(y + 3).toFixed(1)}" text-anchor="end">${esc(yfmt(yt))}</text>`;
  }
  for (const xt of niceTicks(xmin, xmax)) {
    const x = px(xt);
    g += `<text class="tick" x="${x.toFixed(1)}" y="${h - m.b + 16}" text-anchor="middle">${esc(fmt(xt, 0))}</text>`;
  }
  g += `<line class="axis" x1="${m.l}" y1="${m.t}" x2="${m.l}" y2="${m.t + ih}"/>`;
  g += `<line class="axis" x1="${m.l}" y1="${m.t + ih}" x2="${m.l + iw}" y2="${m.t + ih}"/>`;
  if (xlabel) g += `<text class="axis-label" x="${m.l + iw / 2}" y="${h - 2}" text-anchor="middle">${esc(xlabel)}</text>`;
  if (ylabel) g += `<text class="axis-label" x="12" y="${m.t + ih / 2}" text-anchor="middle" transform="rotate(-90 12 ${m.t + ih / 2})">${esc(ylabel)}</text>`;
  return { g, px, py, w, h, m, iw, ih };
}

function svgWrap(w, h, inner) {
  return `<svg viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid meet" role="img">${inner}</svg>`;
}

// Scatter of GC pauses over time, colored by collection type.
function scatterPauses(events, { w = 560, h = 240 } = {}) {
  if (!events.length) return emptyChart(w, h);
  const xmax = Math.max(...events.map((e) => e.t), 1);
  const ymax = Math.max(...events.map((e) => e.durationMs), 1) * 1.05;
  const f = frame({ w, h, xmin: 0, xmax, ymin: 0, ymax, xlabel: "Time (s)", ylabel: "Pause (ms)", yfmt: (v) => fmt(v, 1) });
  let dots = "";
  for (const e of events) {
    dots += `<circle cx="${f.px(e.t).toFixed(1)}" cy="${f.py(e.durationMs).toFixed(1)}" r="3" fill="${colorForType(e.type)}" fill-opacity="0.75"><title>${esc(e.type)} · ${esc(e.cause)} · ${fmt(e.durationMs, 2)} ms @ ${fmt(e.t, 2)}s</title></circle>`;
  }
  return svgWrap(w, h, f.g + dots);
}

// Multi-series line chart. series: [{name,color,points:[{t,v}]}]
function lineChart(series, { w = 560, h = 240, xlabel = "Time (s)", ylabel = "", yfmt = (v) => fmt(v, 0), ymaxForce } = {}) {
  const all = series.flatMap((s) => s.points);
  if (!all.length) return emptyChart(w, h);
  const xmax = Math.max(...all.map((p) => p.t), 1);
  const ymax = ymaxForce != null ? ymaxForce : Math.max(...all.map((p) => p.v), 1) * 1.05;
  const f = frame({ w, h, xmin: 0, xmax, ymin: 0, ymax, xlabel, ylabel, yfmt });
  let lines = "";
  for (const s of series) {
    if (!s.points.length) continue;
    const d = s.points.map((p, i) => `${i ? "L" : "M"}${f.px(p.t).toFixed(1)},${f.py(p.v).toFixed(1)}`).join(" ");
    lines += `<path d="${d}" fill="none" stroke="${s.color}" stroke-width="1.6"/>`;
  }
  return svgWrap(w, h, f.g + lines);
}

// Area chart of heap occupancy after each GC (with committed size line).
function heapArea(events, { w = 560, h = 240 } = {}) {
  const pts = events.filter((e) => e.heapAfterKb >= 0);
  if (!pts.length) return emptyChart(w, h);
  const xmax = Math.max(...pts.map((e) => e.t), 1);
  const ymax = Math.max(...pts.map((e) => Math.max(e.heapBeforeKb, e.heapSizeKb))) / 1024 * 1.05;
  const f = frame({ w, h, xmin: 0, xmax, ymin: 0, ymax, xlabel: "Time (s)", ylabel: "Heap (MB)", yfmt: (v) => fmt(v, 0) });
  const beforeLine = pts.map((e, i) => `${i ? "L" : "M"}${f.px(e.t).toFixed(1)},${f.py(e.heapBeforeKb / 1024).toFixed(1)}`).join(" ");
  const afterD = pts.map((e, i) => `${i ? "L" : "M"}${f.px(e.t).toFixed(1)},${f.py(e.heapAfterKb / 1024).toFixed(1)}`).join(" ");
  const areaD = `${afterD} L${f.px(pts[pts.length - 1].t).toFixed(1)},${f.py(0).toFixed(1)} L${f.px(pts[0].t).toFixed(1)},${f.py(0).toFixed(1)} Z`;
  const area = `<path d="${areaD}" fill="var(--gc-young)" fill-opacity="0.16"/>`;
  const after = `<path d="${afterD}" fill="none" stroke="var(--gc-young)" stroke-width="1.6"/>`;
  const before = `<path d="${beforeLine}" fill="none" stroke="var(--gc-old)" stroke-width="1" stroke-opacity="0.6" stroke-dasharray="3 3"/>`;
  return svgWrap(w, h, f.g + area + after + before);
}

function emptyChart(w, h) {
  return svgWrap(w, h, `<text x="${w / 2}" y="${h / 2}" text-anchor="middle" class="tick">No data</text>`);
}

// Horizontal bar list rendered as HTML (better label handling than SVG).
function barList(items, { max, valueFmt = (v) => fmt(v, 1), unit = "", colorBy } = {}) {
  const m = max ?? Math.max(...items.map((i) => i.value), 1);
  return `<div class="barrow">` + items.map((it, idx) => {
    const pct = Math.max(2, (it.value / m) * 100);
    const color = colorBy ? colorBy(it, idx) : "var(--accent)";
    return `<div class="bar-item"><span class="name" title="${esc(it.name)}">${esc(it.label ?? it.name)}</span>` +
      `<span class="track"><span class="fill" style="width:${pct.toFixed(1)}%;background:${color}"></span></span>` +
      `<span class="val">${esc(valueFmt(it.value))}${unit}</span></div>`;
  }).join("") + `</div>`;
}

function legend(items) {
  return `<div class="legend">` + items.map((i) => `<span class="item"><span class="swatch" style="background:${i.color}"></span>${esc(i.label)}</span>`).join("") + `</div>`;
}

// ---------------------------------------------------------------------------
// Rendering the report
// ---------------------------------------------------------------------------
function statCard(label, value, unit = "") {
  return `<div class="stat"><div class="label">${esc(label)}</div><div class="value">${esc(value)}<span class="unit">${esc(unit)}</span></div></div>`;
}

function renderReport(r) {
  const gc = r.gc || {};
  const s = gc.summary || {};
  const jfr = r.jfr || {};
  const parts = [];

  if (gc.error) parts.push(`<div class="error-banner">GC log analysis error: ${esc(gc.error)}</div>`);

  // Overview stats
  parts.push(`<div class="section-title">Overview</div>`);
  parts.push(`<div class="stats">` + [
    statCard("Throughput", fmt(s.throughputPercent, 2), "%"),
    statCard("GC events", fmt(s.eventCount, 0)),
    statCard("Total pause", fmt(s.totalPauseMs, 1), "ms"),
    statCard("Avg pause", fmt(s.avgPauseMs, 2), "ms"),
    statCard("Max pause", fmt(s.maxPauseMs, 2), "ms"),
    statCard("p99 pause", fmt(s.p99PauseMs, 2), "ms"),
    statCard("Peak heap", fmt((s.peakHeapKb || 0) / 1024, 0), "MB"),
    statCard("Alloc rate", fmt(s.allocRateMbPerSec, 0), "MB/s"),
    statCard("GC runtime", fmt(s.runtimeSec, 1), "s"),
  ].join("") + `</div>`);

  // JVM + config table
  const jvm = r.jvm || {};
  const cfg = r.gcConfig || {};
  const infoRows = [
    ["Collector", cfg.youngCollector ? `${cfg.youngCollector} / ${cfg.oldCollector}` : "–"],
    ["JVM", jvm.name || "–"],
    ["Version", (jvm.version || "").split(" for ")[0] || "–"],
    ["GC threads", cfg.parallelGCThreads != null ? `${cfg.parallelGCThreads} parallel · ${cfg.concurrentGCThreads} concurrent` : "–"],
    ["Java args", jvm.javaArgs || "–"],
    ["Workload", r.label || "–"],
    ["GC log", (r.source && r.source.gcLogName) || "–"],
  ];

  // Build panels
  const panels = [];

  panels.push(panel("GC pause timeline", "Every stop-the-world pause, colored by collection type.",
    scatterPauses(gc.events || []) + typeLegend(gc.types || {})));

  panels.push(panel("Heap occupancy", "Heap used after each GC (solid) vs. before each GC (dashed).",
    heapArea(gc.events || []) + legend([
      { color: "var(--gc-young)", label: "After GC" },
      { color: "var(--gc-old)", label: "Before GC" },
    ])));

  // GC cause breakdown
  const causeItems = Object.entries(gc.causes || {}).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value);
  panels.push(panel("GC cause breakdown", "How many collections each cause triggered.",
    causeItems.length ? barList(causeItems, { valueFmt: (v) => fmt(v, 0), colorBy: (_, i) => PALETTE[i % PALETTE.length] }) : emptyChart(560, 120)));

  // CPU load
  if (jfr.cpuLoad && jfr.cpuLoad.length) {
    panels.push(panel("CPU load", "JVM and machine CPU utilization over the run (from JFR).",
      lineChart([
        { name: "jvmUser", color: "var(--gc-young)", points: jfr.cpuLoad.map((d) => ({ t: d.t, v: d.jvmUser * 100 })) },
        { name: "jvmSystem", color: "var(--gc-system)", points: jfr.cpuLoad.map((d) => ({ t: d.t, v: d.jvmSystem * 100 })) },
        { name: "machineTotal", color: "var(--gc-old)", points: jfr.cpuLoad.map((d) => ({ t: d.t, v: d.machineTotal * 100 })) },
      ], { ylabel: "CPU (%)", yfmt: (v) => fmt(v, 0), ymaxForce: 100 }) + legend([
        { color: "var(--gc-young)", label: "JVM user" },
        { color: "var(--gc-system)", label: "JVM system" },
        { color: "var(--gc-old)", label: "Machine total" },
      ])));
  }

  // Heap used from JFR (finer grained than GC log)
  if (jfr.heapSummary && jfr.heapSummary.length) {
    panels.push(panel("Heap used (JFR)", "Heap used sampled before/after every GC id.",
      lineChart([
        { name: "used", color: "var(--accent)", points: jfr.heapSummary.map((d) => ({ t: d.t, v: d.usedMb })) },
        { name: "committed", color: "var(--gc-old)", points: jfr.heapSummary.map((d) => ({ t: d.t, v: d.committedMb })) },
      ], { ylabel: "Heap (MB)" }) + legend([
        { color: "var(--accent)", label: "Used" },
        { color: "var(--gc-old)", label: "Committed" },
      ])));
  }

  // Top allocations
  if (jfr.topAllocations && jfr.topAllocations.length) {
    const items = jfr.topAllocations.map((a) => ({ name: a.name, label: shortClass(a.name), value: a.weightMb }));
    panels.push(panel("Top allocations", `Allocated memory by type — GC allocation rate × runtime, split by JFR allocation pressure. Total ≈ ${fmt(jfr.totalAllocatedMb, 0)} MB.`,
      barList(items, { valueFmt: (v) => fmt(v, 1), unit: " MB" })));
  }

  // Hot methods
  if (jfr.hotMethods && jfr.hotMethods.length) {
    const items = jfr.hotMethods.map((mth) => ({ name: mth.name, label: shortMethod(mth.name), value: mth.pct }));
    panels.push(panel("Hot methods", `Top self-time methods from ${fmt(jfr.sampleCount, 0)} execution samples (JFR).`,
      barList(items, { valueFmt: (v) => fmt(v, 1), unit: "%", max: 100 })));
  }

  const panelsHtml = `<div class="panels">${panels.join("")}</div>`;
  const infoPanel = panel("Environment", "", `<table class="info">${infoRows.map(([k, v]) => `<tr><td class="k">${esc(k)}</td><td class="v">${esc(v)}</td></tr>`).join("")}</table>`, "wide");

  let html = parts.join("") + `<div class="section-title">Charts</div>` + panelsHtml +
    `<div class="section-title">Details</div><div class="panels">${infoPanel}</div>`;

  // JFR views text (loaded lazily)
  if (r.artifacts && r.artifacts.views) {
    html += `<details class="views"><summary>jfr view all-views (full report)</summary><pre id="views-pre">Loading…</pre></details>`;
  }
  return html;
}

function panel(title, desc, body, extraClass = "") {
  return `<div class="panel ${extraClass}"><h3>${esc(title)}</h3>${desc ? `<p class="desc">${esc(desc)}</p>` : ""}${body}</div>`;
}

function typeLegend(types) {
  const items = Object.keys(types).map((t) => ({ color: colorForType(t), label: `${t} (${types[t]})` }));
  return legend(items);
}

function shortClass(name) {
  let n = name.replace(/^\[+L?/, "").replace(/;$/, "");
  const parts = n.split(".");
  return parts.length > 2 ? parts.slice(-2).join(".") : n;
}
function shortMethod(name) {
  const hashIdx = name.lastIndexOf(".");
  const cls = name.slice(0, hashIdx);
  const method = name.slice(hashIdx + 1);
  const clsParts = cls.split(".");
  return `${clsParts[clsParts.length - 1] || cls}.${method}`;
}

// ---------------------------------------------------------------------------
// App wiring: state fetch, run trigger, SSE progress
// ---------------------------------------------------------------------------
const el = (id) => document.getElementById(id);
let waiting = false;

async function loadState() {
  try {
    const res = await fetch("state");
    const data = await res.json();
    if (data && !data.empty && data.gc) showReport(data);
    else showEmpty();
  } catch {
    showEmpty();
  }
}

function showEmpty() {
  el("empty").classList.remove("hidden");
  el("report").classList.add("hidden");
  el("analyze-btn").classList.add("hidden");
}

function showReport(r) {
  el("empty").classList.add("hidden");
  const rep = el("report");
  rep.innerHTML = renderReport(r);
  rep.classList.remove("hidden");
  el("analyze-btn").classList.remove("hidden");
  if (r.artifacts && r.artifacts.views) {
    fetch("views").then((res) => (res.ok ? res.text() : "")).then((txt) => {
      const pre = el("views-pre");
      if (pre) pre.textContent = txt || "(unavailable)";
    }).catch(() => {});
  }
}

function setProgress(pct, msg) {
  el("progress").classList.remove("hidden");
  if (pct != null) el("progress-fill").style.transform = `scaleX(${Math.max(0, Math.min(100, pct)) / 100})`;
  if (msg) el("progress-msg").textContent = msg;
}

function openConfig() { el("config").classList.remove("hidden"); }
function closeConfig() { el("config").classList.add("hidden"); }

// A single persistent event stream: ingestion is triggered by Copilot (via the
// gc_jfr_ingest tool), so progress/done events can arrive at any time, not just
// during a button click.
function connectEvents() {
  const evtSrc = new EventSource("events");
  evtSrc.addEventListener("awaiting", (e) => {
    try { const d = JSON.parse(e.data); setWaiting(d.msg || "Waiting for Copilot…"); } catch { setWaiting("Waiting for Copilot…"); }
  });
  evtSrc.addEventListener("progress", (e) => {
    try { const d = JSON.parse(e.data); setProgress(d.pct ?? null, d.msg || ""); } catch {}
  });
  evtSrc.addEventListener("done", (e) => {
    finishWaiting();
    setProgress(100, "Analysis complete.");
    try { const d = JSON.parse(e.data); if (d.report) showReport(d.report); else loadState(); } catch { loadState(); }
    setTimeout(() => el("progress").classList.add("hidden"), 1500);
  });
  evtSrc.addEventListener("failed", (e) => {
    finishWaiting();
    let msg = "Analysis failed.";
    try { msg = "Analysis failed: " + (JSON.parse(e.data).error || ""); } catch {}
    el("run-status").textContent = msg;
    setProgress(100, msg);
  });
}

function setWaiting(msg) {
  waiting = true;
  el("run-status").textContent = msg;
  setProgress(4, msg);
}

function finishWaiting() {
  waiting = false;
  el("run-btn").disabled = false;
  el("run-status").textContent = "";
}

async function startRun() {
  closeConfig();
  el("run-btn").disabled = true;
  setWaiting("Asking Copilot to run the workload…");

  const body = {
    hint: el("cfg-hint").value.trim(),
    jfrMaxSizeMb: Number(el("cfg-jfr").value) || 100,
  };

  try {
    const res = await fetch("run", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || "HTTP " + res.status);
    setWaiting("Copilot is running the workload — results appear here when it calls gc_jfr_ingest.");
    // Re-enable so the user can trigger again if needed; ingestion is async.
    el("run-btn").disabled = false;
  } catch (err) {
    finishWaiting();
    el("run-status").textContent = "Failed to ask Copilot: " + (err.message || err);
    el("progress").classList.add("hidden");
  }
}

async function analyzeWithAI() {
  const btn = el("analyze-btn");
  btn.disabled = true;
  const prev = btn.textContent;
  btn.textContent = "Sending…";
  el("run-status").textContent = "";
  try {
    const res = await fetch("analyze", { method: "POST" });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || "HTTP " + res.status);
    el("run-status").textContent = "Sent to Copilot — see the chat for recommendations.";
    btn.textContent = "✓ Sent to Copilot";
    setTimeout(() => { btn.textContent = prev; el("run-status").textContent = ""; }, 6000);
  } catch (err) {
    el("run-status").textContent = "Analyze failed: " + (err.message || err);
    btn.textContent = prev;
  } finally {
    btn.disabled = false;
  }
}

el("run-btn").addEventListener("click", openConfig);
el("config-cancel").addEventListener("click", closeConfig);
el("config-start").addEventListener("click", startRun);
el("analyze-btn").addEventListener("click", analyzeWithAI);

connectEvents();
loadState();

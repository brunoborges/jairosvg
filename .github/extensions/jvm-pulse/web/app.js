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
  return `<svg class="chart" viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid meet" role="img">${inner}</svg>`;
}

let gradSeq = 0;

// Scatter of GC pauses over time, colored by collection type.
function scatterPauses(events, { w = 560, h = 240 } = {}) {
  if (!events.length) return emptyChart(w, h);
  const xmax = Math.max(...events.map((e) => e.t), 1);
  const ymax = Math.max(...events.map((e) => e.durationMs), 1) * 1.05;
  const f = frame({ w, h, xmin: 0, xmax, ymin: 0, ymax, xlabel: "Time (s)", ylabel: "Pause (ms)", yfmt: (v) => fmt(v, 1) });
  let dots = "";
  for (const e of events) {
    dots += `<circle class="dot" cx="${f.px(e.t).toFixed(1)}" cy="${f.py(e.durationMs).toFixed(1)}" r="3" fill="${colorForType(e.type)}" fill-opacity="0.72"><title>${esc(e.type)} · ${esc(e.cause)} · ${fmt(e.durationMs, 2)} ms @ ${fmt(e.t, 2)}s</title></circle>`;
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
  const gid = `heapgrad${++gradSeq}`;
  const beforeLine = pts.map((e, i) => `${i ? "L" : "M"}${f.px(e.t).toFixed(1)},${f.py(e.heapBeforeKb / 1024).toFixed(1)}`).join(" ");
  const afterD = pts.map((e, i) => `${i ? "L" : "M"}${f.px(e.t).toFixed(1)},${f.py(e.heapAfterKb / 1024).toFixed(1)}`).join(" ");
  const areaD = `${afterD} L${f.px(pts[pts.length - 1].t).toFixed(1)},${f.py(0).toFixed(1)} L${f.px(pts[0].t).toFixed(1)},${f.py(0).toFixed(1)} Z`;
  const defs = `<defs><linearGradient id="${gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="var(--gc-young)" stop-opacity="0.28"/><stop offset="100%" stop-color="var(--gc-young)" stop-opacity="0.02"/></linearGradient></defs>`;
  const area = `<path d="${areaD}" fill="url(#${gid})"/>`;
  const after = `<path d="${afterD}" fill="none" stroke="var(--gc-young)" stroke-width="1.6"/>`;
  const before = `<path d="${beforeLine}" fill="none" stroke="var(--gc-old)" stroke-width="1" stroke-opacity="0.6" stroke-dasharray="3 3"/>`;
  return svgWrap(w, h, defs + f.g + area + after + before);
}

function emptyChart(w, h) {
  return svgWrap(w, h, `<text x="${w / 2}" y="${h / 2}" text-anchor="middle" class="tick">No data</text>`);
}

// Horizontal bar list rendered as HTML (better label handling than SVG).
// items: [{ name, label, value, sub?, owner?, color? }]. Fills animate from 0
// via CSS once the `--w` target is applied by animateBars().
function barList(items, { max, valueFmt = (v) => fmt(v, 1), unit = "", colorBy, ranked = false } = {}) {
  const m = max ?? Math.max(...items.map((i) => i.value), 1);
  return `<div class="barrow">` + items.map((it, idx) => {
    const pct = Math.max(1.5, (it.value / m) * 100);
    const color = it.color || (colorBy ? colorBy(it, idx) : "var(--accent)");
    const rank = ranked ? `<span class="rank">${idx + 1}</span>` : `<span class="rank"></span>`;
    const owner = it.owner ? `<span class="owner">${esc(it.owner)}</span>` : "";
    const sub = it.sub ? `<span class="sub">${esc(it.sub)}</span>` : "";
    return `<div class="bar-item">${rank}` +
      `<span class="name" title="${esc(it.name)}">${owner}<span>${esc(it.label ?? it.name)}</span></span>` +
      `<span class="track"><span class="fill" data-w="${pct.toFixed(1)}" style="background:${color}"></span></span>` +
      `<span class="val">${esc(valueFmt(it.value))}${unit}${sub}</span></div>`;
  }).join("") + `</div>`;
}

// Apply target widths after the markup is in the DOM so the CSS width
// transition animates from 0. Called by showReport().
function animateBars(root) {
  const fills = (root || document).querySelectorAll(".bar-item .fill[data-w]");
  const reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reduce) { fills.forEach((f) => (f.style.width = f.dataset.w + "%")); return; }
  requestAnimationFrame(() => requestAnimationFrame(() => {
    fills.forEach((f) => (f.style.width = f.dataset.w + "%"));
  }));
}

function legend(items) {
  return `<div class="legend">` + items.map((i) => `<span class="item"><span class="swatch" style="background:${i.color}"></span>${esc(i.label)}</span>`).join("") + `</div>`;
}

// ---------------------------------------------------------------------------
// Rendering the report
// ---------------------------------------------------------------------------

// Human-friendly memory: promote MB to GB past 1024 so "25.8 GB" beats "25,757 MB".
function humanBytesMb(mb, d = 1) {
  if (mb == null || Number.isNaN(mb)) return "–";
  if (mb >= 1024) return `${fmt(mb / 1024, d)} GB`;
  if (mb >= 1) return `${fmt(mb, mb < 10 ? 1 : 0)} MB`;
  return `${fmt(mb * 1024, 0)} KB`;
}

const worseOf = (a, b) => (["good", "warn", "bad", "neutral"].indexOf(b) > ["good", "warn", "bad", "neutral"].indexOf(a) && b !== "neutral" ? b : a);
const band = (v, goodBelow, warnBelow) => (v == null ? "neutral" : v < goodBelow ? "good" : v <= warnBelow ? "warn" : "bad");
const bandAbove = (v, goodAbove, warnAbove) => (v == null ? "neutral" : v >= goodAbove ? "good" : v >= warnAbove ? "warn" : "bad");
const STATUS_COLOR = { good: "var(--status-good)", warn: "var(--status-warn)", bad: "var(--status-bad)", neutral: "var(--status-neutral)" };
const isJdk = (name) => /^(java|javax|jdk|sun|com\.sun|jakarta|kotlin|scala|groovy|org\.graalvm|com\.oracle)\./.test(name);

// The dashboard's point of view: assess GC health and name the main lever.
function assess(s, jfr) {
  const tp = s.throughputPercent, p99 = s.p99PauseMs, mx = s.maxPauseMs, ar = s.allocRateMbPerSec;
  const tpS = bandAbove(tp, 95, 90);
  const p99S = band(p99, 20, 50);
  const mxS = band(mx, 50, 150);
  const arS = band(ar, 64, 256);
  const peakRatio = s.maxHeapSizeKb ? (s.peakHeapKb || 0) / s.maxHeapSizeKb : null;
  const heapS = peakRatio == null ? "neutral" : peakRatio > 0.92 ? "warn" : "neutral";
  const overall = worseOf(worseOf(tpS, p99S), mxS);
  return { tp, p99, mx, ar, tpS, p99S, mxS, arS, heapS, overall };
}

function verdictBlock(a, s, jfr) {
  const headline = {
    good: "Garbage collection is healthy",
    warn: "Garbage collection needs a look",
    bad: "Garbage collection is a bottleneck",
    neutral: "Garbage collection profile",
  }[a.overall];

  let detail = `${fmt(a.tp, 1)}% throughput`;
  if (a.p99 != null) detail += `, p99 pause ${fmt(a.p99, 1)} ms`;
  detail += a.overall === "good" ? " — GC is not the bottleneck."
    : a.overall === "warn" ? " — there's pause or throughput headroom to reclaim."
    : " — pauses or low throughput are hurting the workload.";
  if (a.ar != null) {
    const churn = jfr && jfr.totalAllocatedMb ? ` (≈ ${humanBytesMb(jfr.totalAllocatedMb)} churned)` : "";
    detail += a.arS === "good"
      ? ` Allocation pressure is low at ${fmt(a.ar, 0)} MB/s.`
      : ` Allocation pressure is ${a.arS === "warn" ? "elevated" : "high"} at ${fmt(a.ar, 0)} MB/s${churn} — the main lever for further gains.`;
  }

  const chips = [];
  const chip = (k, v, st) => `<span class="chip"><span class="dot" style="background:${STATUS_COLOR[st]}"></span><span class="chip-k">${esc(k)}</span> <b>${esc(v)}</b></span>`;
  chips.push(chip("Throughput", `${fmt(a.tp, 1)}%`, a.tpS));
  if (a.p99 != null) chips.push(chip("p99 pause", `${fmt(a.p99, 1)} ms`, a.p99S));
  if (a.ar != null) chips.push(chip("Alloc", `${fmt(a.ar, 0)} MB/s`, a.arS));
  const top = jfr && jfr.topAllocations && jfr.topAllocations[0];
  if (top) chips.push(chip("Top type", `${shortClass(top.name)} ${fmt(top.pressurePct, 0)}%`, "neutral"));

  return `<div class="verdict reveal" data-status="${a.overall}" style="--i:0">
    <span class="badge"><span class="dot"></span>${a.overall === "good" ? "Healthy" : a.overall === "warn" ? "Attention" : a.overall === "bad" ? "Critical" : "Profile"}</span>
    <div class="vbody">
      <p class="headline">${esc(headline)}</p>
      <p class="detail">${esc(detail)}</p>
      <div class="chips">${chips.join("")}</div>
    </div>
  </div>`;
}

function kpiTile(label, value, unit, status, tag, sub) {
  return `<div class="kpi" data-status="${status}">
    <div class="khead"><span class="klabel">${esc(label)}</span>${tag ? `<span class="ktag">${esc(tag)}</span>` : ""}</div>
    <div class="kvalue">${esc(value)}${unit ? `<span class="unit">${esc(unit)}</span>` : ""}</div>
    ${sub ? `<div class="ksub">${esc(sub)}</div>` : ""}
  </div>`;
}

function subStat(label, value, unit) {
  return `<div class="substat"><div class="label">${esc(label)}</div><div class="value">${esc(value)}${unit ? `<span class="unit">${esc(unit)}</span>` : ""}</div></div>`;
}

function sectionHeader(title, count) {
  return `<div class="section"><h2>${esc(title)}</h2><span class="rule"></span>${count != null ? `<span class="count">${esc(count)}</span>` : ""}</div>`;
}

function renderReport(r) {
  const gc = r.gc || {};
  const s = gc.summary || {};
  const jfr = r.jfr || {};
  const out = [];

  if (gc.error) out.push(`<div class="error-banner"><strong>GC log analysis error.</strong> ${esc(gc.error)}</div>`);

  const a = assess(s, jfr);

  // 1. Verdict — lead with a point of view.
  if (s.throughputPercent != null) out.push(verdictBlock(a, s, jfr));

  // 2. Primary KPIs — the four metrics that decide GC health.
  const tagTp = { good: "Healthy", warn: "Fair", bad: "Poor", neutral: "" }[a.tpS];
  const tagP99 = { good: "Tight", warn: "Elevated", bad: "High", neutral: "" }[a.p99S];
  const tagAr = { good: "Low", warn: "Elevated", bad: "High", neutral: "" }[a.arS];
  const maxHeapMb = (s.maxHeapSizeKb || 0) / 1024;
  out.push(`<div class="kpis reveal" style="--i:1">` + [
    kpiTile("Throughput", fmt(s.throughputPercent, 2), "%", a.tpS, tagTp, `${fmt(s.percentPaused, 2)}% of time in GC`),
    kpiTile("p99 pause", fmt(s.p99PauseMs, 1), "ms", a.p99S, tagP99, `max ${fmt(s.maxPauseMs, 1)} ms`),
    kpiTile("Alloc rate", fmt(s.allocRateMbPerSec, 0), "MB/s", a.arS, tagAr, jfr.totalAllocatedMb ? `≈ ${humanBytesMb(jfr.totalAllocatedMb)} churned` : "sustained"),
    kpiTile("Peak heap", fmt((s.peakHeapKb || 0) / 1024, 0), "MB", a.heapS, "", maxHeapMb ? `of ${fmt(maxHeapMb, 0)} MB max` : "committed"),
  ].join("") + `</div>`);

  // Secondary stats — supporting detail, denser.
  out.push(`<div class="substats reveal" style="--i:2">` + [
    subStat("GC events", fmt(s.eventCount, 0)),
    subStat("Total pause", fmt(s.totalPauseMs, 0), "ms"),
    subStat("Avg pause", fmt(s.avgPauseMs, 2), "ms"),
    subStat("GC runtime", fmt(s.runtimeSec, 1), "s"),
  ].join("") + `</div>`);

  // 3. Charts
  const panels = [];
  panels.push(panel("GC pause timeline", "Every stop-the-world pause, colored by collection type.",
    scatterPauses(gc.events || []) + typeLegend(gc.types || {})));

  panels.push(panel("Heap occupancy", "Heap used after each GC (solid) vs. before (dashed).",
    heapArea(gc.events || []) + legend([
      { color: "var(--gc-young)", label: "After GC" },
      { color: "var(--gc-old)", label: "Before GC" },
    ])));

  const causeItems = Object.entries(gc.causes || {}).map(([name, value]) => ({ name, label: name, value })).sort((x, y) => y.value - x.value);
  panels.push(panel("GC cause breakdown", "Collections triggered by each cause.",
    causeItems.length ? barList(causeItems, { valueFmt: (v) => fmt(v, 0), colorBy: (_, i) => PALETTE[i % PALETTE.length] }) : emptyChart(560, 120)));

  if (jfr.cpuLoad && jfr.cpuLoad.length) {
    panels.push(panel("CPU load", "JVM vs. machine CPU utilization over the run (JFR).",
      lineChart([
        { name: "jvmUser", color: "var(--gc-young)", points: jfr.cpuLoad.map((d) => ({ t: d.t, v: d.jvmUser * 100 })) },
        { name: "jvmSystem", color: "var(--gc-system)", points: jfr.cpuLoad.map((d) => ({ t: d.t, v: d.jvmSystem * 100 })) },
        { name: "machineTotal", color: "var(--status-neutral)", points: jfr.cpuLoad.map((d) => ({ t: d.t, v: d.machineTotal * 100 })) },
      ], { ylabel: "CPU (%)", yfmt: (v) => fmt(v, 0), ymaxForce: 100 }) + legend([
        { color: "var(--gc-young)", label: "JVM user" },
        { color: "var(--gc-system)", label: "JVM system" },
        { color: "var(--status-neutral)", label: "Machine total" },
      ])));
  }

  if (jfr.heapSummary && jfr.heapSummary.length) {
    panels.push(panel("Heap used (JFR)", "Fine-grained heap sampled around every GC id.",
      lineChart([
        { name: "used", color: "var(--accent)", points: jfr.heapSummary.map((d) => ({ t: d.t, v: d.usedMb })) },
        { name: "committed", color: "var(--status-neutral)", points: jfr.heapSummary.map((d) => ({ t: d.t, v: d.committedMb })) },
      ], { ylabel: "Heap (MB)" }) + legend([
        { color: "var(--accent)", label: "Used" },
        { color: "var(--status-neutral)", label: "Committed" },
      ])));
  }

  // Top allocations — where the memory churn goes (the lever).
  if (jfr.topAllocations && jfr.topAllocations.length) {
    const items = jfr.topAllocations.map((al) => ({
      name: al.name,
      label: shortClass(al.name),
      value: al.weightMb != null ? al.weightMb : al.pressurePct,
      sub: al.weightMb != null && al.pressurePct != null ? `${fmt(al.pressurePct, 1)}%` : "",
      color: "var(--gc-system)",
    }));
    const useBytes = jfr.topAllocations[0].weightMb != null;
    const desc = jfr.totalAllocatedMb
      ? `By share of allocation pressure. Total ≈ ${humanBytesMb(jfr.totalAllocatedMb)} churned.`
      : "By share of allocation pressure (JFR sampling).";
    panels.push(panel("Top allocations", desc,
      barList(items, { ranked: true, valueFmt: useBytes ? (v) => humanBytesMb(v) : (v) => fmt(v, 1) + "%" })));
  }

  // Hot methods — self time, with your code called out from JDK internals.
  if (jfr.hotMethods && jfr.hotMethods.length) {
    const items = jfr.hotMethods.map((mth) => {
      const app = !isJdk(mth.name);
      return {
        name: mth.name,
        label: shortMethod(mth.name),
        value: mth.pct,
        sub: mth.samples != null ? `${fmt(mth.samples, 0)} smpl` : "",
        owner: app ? "App" : "",
        color: app ? "var(--accent)" : "var(--status-neutral)",
      };
    });
    panels.push(panel("Hot methods", `Top self-time methods from ${fmt(jfr.sampleCount, 0)} execution samples. App code highlighted.`,
      barList(items, { ranked: true, valueFmt: (v) => fmt(v, 1), unit: "%", max: Math.max(items[0].value * 1.05, 10) }), "wide"));
  }

  out.push(sectionHeader("Charts", `${panels.length} panels`));
  out.push(`<div class="panels">` + panels.map((p, i) =>
    p.replace('<div class="panel ', `<div style="--i:${Math.min(i + 3, 9)}" class="panel reveal `)
  ).join("") + `</div>`);

  // 4. Environment
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
  out.push(sectionHeader("Environment"));
  out.push(`<div class="panels">` + panel("", "", `<table class="info">${infoRows.map(([k, v]) => `<tr><td class="k">${esc(k)}</td><td class="v">${esc(v)}</td></tr>`).join("")}</table>`, "wide") + `</div>`);

  let html = out.join("");
  if (r.artifacts && r.artifacts.views) {
    html += `<details class="views"><summary>Raw <code>jfr view all-views</code> report</summary><pre id="views-pre">Loading…</pre></details>`;
  }
  return html;
}

function panel(title, desc, body, extraClass = "") {
  const head = title ? `<div class="phead"><h3>${esc(title)}</h3></div>` : "";
  const d = desc ? `<p class="desc">${esc(desc)}</p>` : "";
  return `<div class="panel ${extraClass}">${head}${d}${body}</div>`;
}

function typeLegend(types) {
  const items = Object.keys(types).map((t) => ({ color: colorForType(t), label: `${t} (${types[t]})` }));
  return legend(items);
}

function shortClass(name) {
  let n = String(name).replace(/^\[+L?/, "").replace(/;$/, "");
  const parts = n.split(".");
  return parts.length > 2 ? parts.slice(-2).join(".") : n;
}
function shortMethod(name) {
  const paren = name.indexOf("(");
  const head = paren >= 0 ? name.slice(0, paren) : name;
  const args = paren >= 0 ? name.slice(paren) : "";
  const dot = head.lastIndexOf(".");
  const cls = head.slice(0, dot);
  const method = head.slice(dot + 1);
  const clsParts = cls.split(".");
  return `${clsParts[clsParts.length - 1] || cls}.${method}${args ? "()" : ""}`;
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
  animateBars(rep);
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
// jvm_pulse_ingest tool), so progress/done events can arrive at any time, not just
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
    setWaiting("Copilot is running the workload — results appear here when it calls jvm_pulse_ingest.");
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

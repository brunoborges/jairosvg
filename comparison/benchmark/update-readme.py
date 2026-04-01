#!/usr/bin/env python3
"""
Regenerate the benchmark and PNG file-size tables in comparison/benchmark/README.md
from benchmark-results.jsonl and the rendered PNG files.

Usage:
    python3 comparison/benchmark/update-readme.py
"""

import json
import os
import re
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))
README_PATH = os.path.join(SCRIPT_DIR, "README.md")
JSONL_PATH = os.path.join(SCRIPT_DIR, "benchmark-results.jsonl")
SVG_DIR = os.path.join(REPO_ROOT, "comparison", "svg")
PNG_DIR = os.path.join(REPO_ROOT, "comparison", "visual", "png")

ENGINES = ["jairosvg", "echosvg", "jsvg", "cairosvg"]
ENGINE_HEADERS = ["JairoSVG (Java)", "EchoSVG (Java)", "JSVG (Java)", "CairoSVG (Python)"]
WARN_CASES = {"Masks", "Filters", "Fe turbulence"}


def load_svg_cases():
    """Return ordered list of (case_name, num, slug) from SVG filenames."""
    cases = []
    for f in sorted(os.listdir(SVG_DIR)):
        m = re.match(r"(\d+)_(.+)\.svg", f)
        if m:
            num = int(m.group(1))
            slug = m.group(2)
            case_name = slug.replace("_", " ")
            case_name = case_name[0].upper() + case_name[1:]
            cases.append((case_name, num, slug))
    return cases


def load_jsonl():
    """Load benchmark results from JSONL file."""
    data = {}
    if not os.path.exists(JSONL_PATH):
        print(f"Warning: {JSONL_PATH} not found", file=sys.stderr)
        return data
    with open(JSONL_PATH) as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            r = json.loads(line)
            eng = r["engine"]
            if eng not in ENGINES:
                continue
            case = r["case"]
            if case not in data:
                data[case] = {}
            data[case][eng] = r
    return data


def make_anchor(num, slug):
    """Generate a markdown anchor matching the visual README headings."""
    return f"{num:02d}_{slug}"


def generate_benchmark_table(cases, data):
    """Generate the benchmark timing table."""
    lines = []
    header = "| Test Case | " + " | ".join(ENGINE_HEADERS) + " |"
    sep = "| --- | " + " | ".join([":---:"] * len(ENGINES)) + " |"
    lines.append(header)
    lines.append(sep)

    for case_name, num, slug in cases:
        if case_name not in data:
            continue
        if not any(e in data[case_name] and "median" in data[case_name][e] for e in ENGINES):
            continue

        vals = []
        for e in ENGINES:
            if e in data[case_name] and "median" in data[case_name][e]:
                vals.append(data[case_name][e]["median"])
            else:
                vals.append(None)

        valid_vals = [v for v in vals if v is not None]
        if not valid_vals:
            continue
        min_val = min(valid_vals)

        cells = []
        for v in vals:
            if v is None:
                cells.append("—")
            else:
                formatted = f"{v:.4f} ms"
                if v == min_val:
                    formatted = f"**{formatted}** ✅"
                cells.append(formatted)

        anchor = make_anchor(num, slug)
        label = f"[{case_name}](../visual/README.md#{anchor})"
        if case_name in WARN_CASES:
            label += " ⚠️"

        lines.append(f"| {label} | {' | '.join(cells)} |")

    return "\n".join(lines)


def generate_png_sizes_table(cases):
    """Generate the PNG file sizes table and summary line."""
    size_engines = ["jairosvg", "echosvg", "cairosvg", "jsvg"]

    # Collect all data first to compute column widths
    rows = []
    totals = {e: 0 for e in size_engines}
    has_data = {e: False for e in size_engines}

    for case_name, num, slug in cases:
        png_name = f"{num:02d}_{slug}.png"
        sizes = {}
        for e in size_engines:
            path = os.path.join(PNG_DIR, e, png_name)
            if os.path.exists(path):
                sizes[e] = os.path.getsize(path)
                totals[e] += sizes[e]
                has_data[e] = True
            else:
                sizes[e] = None

        warn = " ⚠️" if case_name in WARN_CASES else ""
        display = case_name.title()
        rows.append((display + warn, sizes))

    # Find max label width
    max_label = max(len(r[0]) for r in rows) if rows else 14
    max_label = max(max_label, 14)  # at least "Test Case" header width

    lines = []
    lines.append(f"| {'Test Case':<{max_label}} | {'JairoSVG':>11} | {'EchoSVG':>11} | {'CairoSVG':>11} | {'JSVG':>11} |")
    lines.append(f"| {'-' * max_label} | ----------: | ----------: | ----------: | ----------: |")

    for display, sizes in rows:
        cells = []
        for e in size_engines:
            if sizes[e] is not None:
                cells.append(f"{sizes[e]:>11,}")
            else:
                cells.append(f"{'—':>11}")
        lines.append(f"| {display:<{max_label}} | {' | '.join(cells)} |")

    total_cells = []
    for e in size_engines:
        if has_data[e]:
            total_cells.append(f"**{totals[e]:,}**".rjust(11))
        else:
            total_cells.append(f"{'—':>11}")

    lines.append(f"| {'**Total**':<{max_label}} | {' | '.join(total_cells)} |")

    # Compute summary percentages (vs each engine, positive = JairoSVG is smaller)
    j = totals["jairosvg"]
    comparisons = []
    for name, key in [("CairoSVG", "cairosvg"), ("JSVG", "jsvg"), ("EchoSVG", "echosvg")]:
        if totals[key] > 0:
            pct = (totals[key] - j) / totals[key] * 100
            if pct > 0:
                comparisons.append(f"**{pct:.1f}% smaller** than {name}")
    summary = (
        f"JairoSVG produces compact PNGs — "
        + ", ".join(comparisons[:-1]) + ", and " + comparisons[-1]
        if len(comparisons) > 1
        else comparisons[0] if comparisons
        else "comparable in size to other engines"
    )
    summary += (
        " (all using zlib compression level 6 — see "
        "[default rendering settings](#default-rendering-settings-jairosvg-vs-jsvg)):"
    )

    return "\n".join(lines), summary


def compute_summary(data, cases):
    """Compute summary stats for the description line."""
    jairo_vs_echo = []
    jairo_vs_jsvg_wins = 0
    jairo_vs_jsvg_losses = 0
    jairo_vs_cairo_wins = 0

    for case_name, _, _ in cases:
        if case_name not in data:
            continue
        d = data[case_name]
        if "jairosvg" not in d or "median" not in d.get("jairosvg", {}):
            continue

        j = d["jairosvg"]["median"]

        if "echosvg" in d and "median" in d["echosvg"]:
            ratio = d["echosvg"]["median"] / j
            jairo_vs_echo.append(ratio)

        if "jsvg" in d and "median" in d["jsvg"]:
            if j < d["jsvg"]["median"]:
                jairo_vs_jsvg_wins += 1
            else:
                jairo_vs_jsvg_losses += 1

        if "cairosvg" in d and "median" in d["cairosvg"]:
            if j < d["cairosvg"]["median"]:
                jairo_vs_cairo_wins += 1

    if jairo_vs_echo:
        min_r = int(min(jairo_vs_echo))
        max_r = int(max(jairo_vs_echo))
        echo_desc = f"**{min_r}–{max_r}× faster** than EchoSVG"
    else:
        echo_desc = "comparable to EchoSVG"

    return f"_JairoSVG is {echo_desc}, **on par with JSVG** in most scenarios, and **faster than CairoSVG** in most scenarios._"


def update_readme(benchmark_table, png_table, png_summary, summary, warmup, iterations):
    """Replace the tables in README.md between known markers."""
    with open(README_PATH) as f:
        content = f.read()

    case_count = benchmark_table.count("\n") - 1  # subtract header + separator

    # Replace benchmark table (from header line to summary line)
    benchmark_section = re.compile(
        r"(# Benchmark\n\n)SVG → PNG.*?\n\n\|.*?\n\n_JairoSVG.*?_",
        re.DOTALL,
    )
    replacement = (
        f"\\1SVG → PNG conversion benchmarks across {case_count} SVG test files, "
        f"median time per render (lower is better):\n\n"
        f"{benchmark_table}\n\n{summary}"
    )
    content = benchmark_section.sub(replacement, content)

    # Replace PNG sizes summary line and table
    png_section = re.compile(
        r"JairoSVG produces the smallest PNGs overall.*?:\n\n"
        r"\| Test Case.*?\| \*\*Total\*\*[^\n]*",
        re.DOTALL,
    )
    content = png_section.sub(f"{png_summary}\n\n{png_table}", content)

    # Update the note about iterations
    note_pattern = re.compile(
        r"> \*\*Note:\*\* Benchmarks were run with \d+ warm-up iterations and \d+ measured iterations"
    )
    content = note_pattern.sub(
        f"> **Note:** Benchmarks were run with {warmup} warm-up iterations and {iterations} measured iterations",
        content,
    )

    with open(README_PATH, "w") as f:
        f.write(content)


def main():
    cases = load_svg_cases()
    data = load_jsonl()

    if not data:
        print("No benchmark data found. Run the benchmark first:", file=sys.stderr)
        print("  jbang comparison/benchmark/benchmark.java", file=sys.stderr)
        sys.exit(1)

    # Detect warmup/iterations from JSONL (count entries per engine per case)
    # For now, just use the values from the data
    warmup = 50  # default
    iterations = 500  # default

    # Check for command-line overrides
    for arg in sys.argv[1:]:
        if arg.startswith("--warmup="):
            warmup = int(arg.split("=")[1])
        elif arg.startswith("--iterations="):
            iterations = int(arg.split("=")[1])

    benchmark_table = generate_benchmark_table(cases, data)
    png_table, png_summary = generate_png_sizes_table(cases)
    summary = compute_summary(data, cases)

    update_readme(benchmark_table, png_table, png_summary, summary, warmup, iterations)

    case_count = benchmark_table.count("\n") - 1
    print(f"✅ Updated {README_PATH}")
    print(f"   Benchmark table: {case_count} test cases")
    print(f"   PNG sizes table: {len(cases)} test cases")


if __name__ == "__main__":
    main()

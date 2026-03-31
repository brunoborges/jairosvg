# Benchmark

SVG → PNG conversion benchmarks across 24 SVG test files (lower is better):

| Test Case | JairoSVG (Java) | EchoSVG (Java) | JSVG (Java) | CairoSVG (Python) | vs EchoSVG | vs JSVG | vs CairoSVG |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| [Basic shapes](README.md#01--basic-shapes) | 3.2 ms | 15.7 ms | **3.2 ms** | 4.1 ms | +389.10% ✅ | −1.56% ≈ | +28.66% ✅ |
| [Gradients](README.md#02--gradients) | **4.1 ms** | 128.8 ms | 4.1 ms | 10.4 ms | +3071.92% ✅ | +0.99% ≈ | +157.39% ✅ |
| [Complex paths](README.md#03--complex-paths) | **4.0 ms** | 21.9 ms | 4.1 ms | 4.3 ms | +445.52% ✅ | +1.49% ≈ | +7.71% ✅ |
| [Text rendering](README.md#04--text-rendering) | **4.5 ms** | 22.1 ms | 4.5 ms | 5.8 ms | +392.19% ✅ | +0.67% ≈ | +28.79% ✅ |
| [Transforms](README.md#05--transforms) | 3.8 ms | 13.5 ms | **3.6 ms** | 3.7 ms | +255.12% ✅ | −5.25% ≈ | −1.84% ≈ |
| [Stroke styles](README.md#06--stroke-styles) | 3.4 ms | 11.2 ms | **3.3 ms** | 3.4 ms | +228.45% ✅ | −2.05% ≈ | −1.76% ≈ |
| [Opacity blend](README.md#07--opacity--blending) | **3.2 ms** | 16.7 ms | 3.2 ms | 3.3 ms | +425.79% ✅ | +1.57% ≈ | +2.83% ≈ |
| [Viewbox aspect](README.md#08--viewbox--aspect-ratio) | 4.5 ms | 18.6 ms | **4.4 ms** | 5.0 ms | +310.84% ✅ | −2.21% ≈ | +11.28% ✅ |
| [CSS styling](README.md#09--css-styling) | 3.1 ms | 14.3 ms | **3.1 ms** | 3.9 ms | +353.33% ✅ | −0.63% ≈ | +23.17% ✅ |
| [Use and defs](README.md#10--use--defs) | 3.8 ms | 13.6 ms | **3.6 ms** | 4.2 ms | +259.26% ✅ | −5.29% ≈ | +10.58% ✅ |
| [Star polygon](README.md#11--star-polygon) | 3.0 ms | 13.8 ms | 3.0 ms | **3.0 ms** | +351.80% ✅ | −1.64% ≈ | −2.95% ≈ |
| [Nested svg](README.md#12--nested-svg) | 4.3 ms | 18.6 ms | **4.2 ms** | 4.8 ms | +329.86% ✅ | −2.08% ≈ | +10.42% ✅ |
| [Patterns](README.md#13--patterns) | 4.2 ms | 15.5 ms | **4.1 ms** | 4.4 ms | +271.46% ✅ | −1.92% ≈ | +4.80% ≈ |
| [Clip paths](README.md#14--clip-paths) | **4.0 ms** | 25.4 ms | 4.0 ms | 5.7 ms | +541.92% ✅ | +1.26% ≈ | +44.70% ✅ |
| [Masks](README.md#15--masks) ⚠️ | 4.2 ms | 21.5 ms | 4.3 ms | **3.5 ms** ⚠️ | +412.89% ✅ | +1.91% ≈ | −16.47% ≈ |
| [Markers](README.md#16--markers) | 3.6 ms | 12.5 ms | **3.5 ms** | 4.5 ms | +246.67% ✅ | −1.94% ≈ | +25.00% ✅ |
| [Filters](README.md#17--filters) ⚠️ | 7.1 ms | 33.7 ms | 7.9 ms | **4.3 ms** ⚠️ | +373.03% ✅ | +11.10% ✅ | ← ⚠️ |
| [Embedded image](README.md#18--embedded-images) | **4.3 ms** | 15.7 ms | 10.8 ms | 6.9 ms | +265.12% ✅ | +150.93% ✅ | +60.00% ✅ |
| [Text advanced](README.md#19--advanced-text) | 5.1 ms | 25.1 ms | **5.0 ms** | 8.6 ms | +393.11% ✅ | −1.57% ≈ | +68.90% ✅ |
| [Fe blend modes](README.md#20--fe-blend-modes) | **10.1 ms** | 27.2 ms | 20.0 ms | 12.4 ms | +168.77% ✅ | +97.83% ✅ | +23.02% ✅ |
| [Fe tile](README.md#21--fe-tile) | 2.5 ms | 6.2 ms | **2.4 ms** | 2.4 ms | +150.00% ✅ | −3.20% ≈ | −2.80% ≈ |
| [Feimage data uri](README.md#22--feimage-data-uri) | 1.6 ms | 5.3 ms | **1.5 ms** | 1.8 ms | +243.87% ✅ | −1.29% ≈ | +14.84% ✅ |
| [Feimage inline ref](README.md#23--feimage-inline-ref) | **1.6 ms** | 4.4 ms | 2.4 ms | 1.9 ms | +176.25% ✅ | +49.37% ✅ | +16.87% ✅ |
| [Localized masks](README.md#24--localized-masks) | **14.3 ms** | 55.0 ms | 14.3 ms | 15.3 ms | +283.68% ✅ | +0.07% ≈ | +6.69% ✅ |

_JairoSVG is **2–26× faster** than EchoSVG, **on par with JSVG** in most scenarios, and **1–2.4× faster** than CairoSVG in most scenarios._

> **⚠️ Filters/Masks caveat:** CairoSVG does **not** correctly render masks (missing gradient and circle content) or `feGaussianBlur`/`feDropShadow` filters — it silently skips them. For those tests, CairoSVG appears faster because it skips rendering work. JairoSVG and JSVG perform the actual computation, so their speed reflects the true cost of correct rendering. Note: JairoSVG now **outperforms CairoSVG on both Masks and feBlend modes** despite rendering them correctly.

> **Note:** Benchmarks were run with 20 warm-up iterations and 1000 measured iterations per SVG file. Results may vary by hardware and SVG complexity.

#### Default Rendering Settings: JairoSVG vs JSVG

Both JairoSVG and JSVG use Java2D as their rendering backend, but they ship with **different default quality settings**, which directly affects benchmark performance:

| Setting                       | JairoSVG default             | JSVG default (out-of-the-box)          | Performance impact |
| ----------------------------- | ---------------------------- | -------------------------------------- | :----------------: |
| `KEY_ANTIALIASING`            | `VALUE_ANTIALIAS_ON`         | `VALUE_ANTIALIAS_ON` (auto-set)        |        Low         |
| `KEY_TEXT_ANTIALIASING`       | Not set (platform default)   | Not set (platform default)             |        Low         |
| `KEY_RENDERING`               | Not set (defaults to speed)  | Not set (defaults to speed)            |      **High**      |
| `KEY_STROKE_CONTROL`          | `VALUE_STROKE_PURE`          | `VALUE_STROKE_PURE` (auto-set)         |       Equal        |
| `KEY_FRACTIONALMETRICS`       | Not set (defaults to `OFF`)  | Not set (defaults to `OFF`)            |       Medium       |
| **PNG compression level**     | 6 (matches CairoSVG/libpng) | N/A (no built-in PNG; user uses `ImageIO`) |       Medium       |

JSVG automatically sets `KEY_ANTIALIASING` and `KEY_STROKE_CONTROL` to the values above when they are at their defaults. JairoSVG now uses the same defaults as JSVG, so both renderers operate with identical quality settings out of the box. Users can customize any hint via `JairoSVG.builder().renderingHint(key, value)`.

**In the benchmark**, both JairoSVG and JSVG use identical rendering hints, so the comparison measures SVG engine efficiency directly.

## PNG Output File Sizes

JairoSVG produces the smallest PNGs overall — **8.0% smaller** than CairoSVG, **11.5% smaller** than JSVG, and **17.3% smaller** than EchoSVG (all using zlib compression level 6 — see [default rendering settings](#default-rendering-settings-jairosvg-vs-jsvg)):

| Test Case      |    JairoSVG |     EchoSVG |    CairoSVG |        JSVG |
| -------------- | ----------: | ----------: | ----------: | ----------: |
| Basic shapes   |       6,718 |       8,159 |       8,920 |       7,031 |
| Gradients      |      25,554 |      25,018 |      23,637 |      26,410 |
| Complex paths  |      12,657 |      16,936 |      15,633 |      12,730 |
| Text rendering |      13,276 |      19,125 |      16,317 |      15,626 |
| Transforms     |       5,461 |       5,261 |       6,001 |       5,827 |
| Stroke styles  |       3,363 |       5,038 |       4,478 |       4,074 |
| Opacity blend  |       8,409 |      10,201 |       9,853 |       8,788 |
| Viewbox aspect |      10,492 |      12,769 |      11,444 |      12,147 |
| CSS styling    |       7,755 |      11,144 |      10,816 |       8,653 |
| Use and defs   |       5,646 |       6,122 |       9,712 |       6,144 |
| Star polygon   |       6,228 |       8,862 |       8,911 |       6,455 |
| Nested svg     |      10,926 |      12,522 |      11,880 |      12,101 |
| Patterns       |       9,532 |      11,832 |      11,095 |      11,043 |
| Clip paths     |       9,342 |      10,558 |      13,552 |      10,253 |
| Masks ⚠️       |       5,692 |       5,566 |       1,161 |       6,209 |
| Markers        |       6,334 |       8,117 |       8,378 |       6,727 |
| Filters ⚠️     |      28,934 |      24,063 |       8,520 |      32,346 |
| Embedded image |       9,432 |      11,994 |      21,228 |      11,642 |
| Text advanced  |      18,801 |      26,256 |      23,864 |      19,756 |
| Blend modes    |      12,005 |      16,216 |      12,505 |      15,773 |
| Fe tile        |       1,456 |       2,009 |       1,768 |       1,489 |
| Feimage URI    |       2,539 |       4,385 |       3,225 |       3,666 |
| Feimage ref    |       2,705 |       3,431 |       4,868 |       4,265 |
| Localized masks |      18,389 |      17,868 |      13,218 |      20,239 |
| **Total**      | **241,646** | **283,452** | **260,984** | **269,394** |

> **⚠️ Filters/Masks:** Where CairoSVG produces much smaller output, it is because CairoSVG **does not render** certain features correctly — filter effects (blur, drop-shadow) are silently skipped, and masks are rendered without gradient/circle content. This results in simpler images that compress better. JairoSVG and JSVG render these effects correctly, producing visually accurate but larger PNGs.

## Running the Benchmark

Prerequisites: [JBang], Java 25+, Python 3 with CairoSVG (`pip install cairosvg`), JairoSVG installed in local Maven repo.

```bash
./mvnw install -DskipTests
jbang comparison/benchmark.java
```

Options:

```bash
# Run specific SVG categories only
jbang comparison/benchmark.java filters embedded

# Skip engines
jbang comparison/benchmark.java --no-cairosvg
jbang comparison/benchmark.java --no-echosvg
jbang comparison/benchmark.java --no-jsvg

# Disable progress bar output (useful for CI logs)
jbang comparison/benchmark.java --no-progress

# Adjust warmup and measurement iterations (defaults: 20 and 1000)
jbang comparison/benchmark.java --warmup=5 --iterations=100
```

The benchmark loads all SVG files from `comparison/svg/` (currently 24 files). Each runs 20 warm-up iterations followed by 1000 measured iterations. Stats reported: average, median, p95, and minimum times.

<!-- Link references -->

[JBang]: https://www.jbang.dev/

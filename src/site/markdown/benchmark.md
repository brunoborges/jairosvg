# Benchmark

## JairoSVG vs EchoSVG vs JSVG vs CairoSVG

Benchmark comparing SVG → PNG conversion across four libraries:

- **JairoSVG** (Java 25) — This project
- **JSVG** (Java) — Lightweight SVG renderer, [weisj/jsvg](https://github.com/weisj/jsvg)
- **EchoSVG** (Java) — Batik fork, [css4j/echosvg](https://github.com/css4j/echosvg)
- **CairoSVG** (Python) — Native C rendering via Cairo, [cairosvg.org](https://cairosvg.org)

### Results

Each test case was run with 20 warmup iterations and 1000 measurement iterations.

| Test Case | JairoSVG | JSVG | EchoSVG | CairoSVG | JairoSVG vs EchoSVG |
|-----------|:--------:|:----:|:-------:|:--------:|:-------------------:|
| Simple shapes | 3.2 ms | **3.2 ms** | 15.7 ms | 4.1 ms | **4.9x faster** |
| Gradients + transforms | **4.1 ms** | 4.1 ms | 128.8 ms | 10.4 ms | **31x faster** |
| Complex paths + text | **4.0 ms** | 4.1 ms | 21.9 ms | 4.3 ms | **5.5x faster** |
| Defs + use + clipPath | 3.8 ms | **3.6 ms** | 13.6 ms | 4.2 ms | **3.6x faster** |
| Markers + dashed strokes | 3.6 ms | **3.5 ms** | 12.5 ms | 4.5 ms | **3.5x faster** |

<a href="images/benchmark.png" target="_blank"><img src="images/benchmark.png" alt="Benchmark chart" width="560"/></a>

### Analysis

- **JairoSVG is 2.5–31× faster than EchoSVG** across all test cases
- **JairoSVG is on par with JSVG** — both use Java2D for rendering
- **JairoSVG is 1–2.4× faster than CairoSVG** despite CairoSVG using a native C rendering backend
- JairoSVG achieves competitive performance through:
  - Pre-compiled regex patterns
  - Cached SAX parser factories
  - Object reuse (GeneralPath.reset())
  - Efficient string processing
  - Sub-region effect buffers for filters and masks

### PNG Output File Sizes

JairoSVG produces the smallest PNGs overall — **8.0% smaller** than CairoSVG, **11.5% smaller** than JSVG, and **17.3% smaller** than EchoSVG:

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
| Masks          |       5,692 |       5,566 |       1,161 |       6,209 |
| Markers        |       6,334 |       8,117 |       8,378 |       6,727 |
| Filters        |      28,934 |      24,063 |       8,520 |      32,346 |
| Embedded image |       9,432 |      11,994 |      21,228 |      11,642 |
| Text advanced  |      18,801 |      26,256 |      23,864 |      19,756 |
| Blend modes    |      12,005 |      16,216 |      12,505 |      15,773 |
| Fe tile        |       1,456 |       2,009 |       1,768 |       1,489 |
| Feimage URI    |       2,539 |       4,385 |       3,225 |       3,666 |
| Feimage ref    |       2,705 |       3,431 |       4,868 |       4,265 |
| Localized masks |      18,389 |      17,868 |      13,218 |      20,239 |
| **Total**      | **241,646** | **283,452** | **260,984** | **269,394** |

<a href="images/benchmark-size.png" target="_blank"><img src="images/benchmark-size.png" alt="File size comparison chart" width="560"/></a>

### Test SVGs

**Simple shapes**: Rectangle, circle, ellipse, line, polygon, polyline with various fills and strokes.

**Gradients + transforms**: Linear and radial gradients with transforms, opacity, and gradient units.

**Complex paths + text**: Full path commands (M, L, C, S, Q, T, A), text with font properties, nested groups, clip paths.

**Defs + use + clipPath**: Definition elements, `<use>` references, clip paths, nested structure.

**Markers + dashed strokes**: Path markers (start/mid/end), dashed stroke arrays, line caps and joins.

### Running the Benchmark

Prerequisites: [JBang](https://www.jbang.dev/), Python 3 with CairoSVG (`pip install cairosvg`), Java 25+.

```bash
# Install JairoSVG to local Maven repo
./mvnw install -DskipTests

# Run benchmark
jbang comparison/benchmark.java

# Run without progress bar (CI mode)
jbang comparison/benchmark.java --no-progress
```

The benchmark script is in the comparison folder: [`comparison/benchmark.java`](https://github.com/brunoborges/jairosvg/blob/main/comparison/benchmark.java). See [`comparison/README.md`](https://github.com/brunoborges/jairosvg/blob/main/comparison/README.md) for full results, PNG file size comparisons, and feature matrices.

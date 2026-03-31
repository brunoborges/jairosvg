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

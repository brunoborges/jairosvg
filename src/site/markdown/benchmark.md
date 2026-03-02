# Benchmark

## JairoSVG vs EchoSVG vs CairoSVG

Benchmark comparing SVG → PNG conversion across three libraries:

- **JairoSVG** (Java 25) — This project
- **EchoSVG** (Java) — Batik fork, [css4j/echosvg](https://github.com/css4j/echosvg)
- **CairoSVG** (Python) — Native C rendering via Cairo, [cairosvg.org](https://cairosvg.org)

### Results

Each test case was run with 10 warmup iterations and 20 measurement iterations.

| Test Case | JairoSVG | EchoSVG | CairoSVG | JairoSVG vs EchoSVG |
|-----------|:--------:|:-------:|:--------:|:-------------------:|
| Simple shapes | **3.7 ms** | 9.5 ms | 1.9 ms | **2.6x faster** |
| Gradients + transforms | **6.7 ms** | 34.5 ms | 5.2 ms | **5.1x faster** |
| Complex paths + text | **8.1 ms** | 29.7 ms | 6.1 ms | **3.7x faster** |

### Analysis

- **JairoSVG is 2.6-5.1x faster than EchoSVG** across all test cases
- **CairoSVG is 1.3-1.9x faster than JairoSVG** due to its native C rendering backend (Cairo library)
- JairoSVG achieves competitive performance through:
  - Pre-compiled regex patterns
  - Cached XML parser factories
  - Object reuse (GeneralPath.reset())
  - Efficient string processing

### Test SVGs

**Simple shapes**: Rectangle, circle, ellipse, line, polygon, polyline with various fills and strokes.

**Gradients + transforms**: Linear and radial gradients with transforms, opacity, and gradient units.

**Complex paths + text**: Full path commands (M, L, C, S, Q, T, A), text with font properties, nested groups, clip paths.

### Running the Benchmark

Prerequisites: [JBang](https://www.jbang.dev/), Python 3 with CairoSVG (`pip install cairosvg`), Java 25+.

```bash
# Install JairoSVG to local Maven repo
./mvnw install -DskipTests

# Run benchmark
jbang benchmark.java
```

The benchmark script is at the project root: [`benchmark.java`](https://github.com/brunoborges/jairosvg/blob/main/benchmark.java).

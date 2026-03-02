# Feature Comparison: JairoSVG vs EchoSVG

A comprehensive comparison of two Java SVG libraries — **JairoSVG** and **EchoSVG** — to help developers choose the right tool for their SVG rendering needs.

---

## Overview

| | JairoSVG | EchoSVG |
|---|---|---|
| **Origin** | Java port of [CairoSVG](https://cairosvg.org) (Python) | Fork of [Apache Batik](https://xmlgraphics.apache.org/batik/) |
| **Maintainer** | Bruno Borges | css4j project |
| **License** | LGPL-3.0 | Apache-2.0 (inherited from Batik) |
| **Repository** | [github.com/brunoborges/jairosvg](https://github.com/brunoborges/jairosvg) | [github.com/css4j/echosvg](https://github.com/css4j/echosvg) |
| **Current version** | 1.0.0-SNAPSHOT | 2.4 |
| **Minimum Java** | 25 (with preview features) | 11 |

---

## Architecture & Design

### JairoSVG

JairoSVG is a **direct port of CairoSVG's Python codebase** to modern Java, rendering SVG through the standard **Java2D** (`Graphics2D` / `BufferedImage`) API. The architecture is intentionally compact:

| Java Class | Python Module | Role |
|---|---|---|
| `JairoSVG` | `__init__.py` | Public API + Builder |
| `Surface` | `surface.py` | Java2D rendering engine |
| `Node` | `parser.py` | SVG DOM tree |
| `PathDrawer` | `path.py` | SVG path commands |
| `ShapeDrawer` | `shapes.py` | Basic shapes |
| `TextDrawer` | `text.py` | Text rendering |
| `Defs` | `defs.py` | Gradients, clips, use |
| `Colors` | `colors.py` | Color parsing (170+ named) |
| `Helpers` | `helpers.py` | Units, transforms |
| `CssProcessor` | `css.py` | CSS parsing |

**Key technology mapping:**
- `cairo.Context` → `java.awt.Graphics2D`
- `cairo.ImageSurface` → `java.awt.image.BufferedImage`
- `cairo.Matrix` → `java.awt.geom.AffineTransform`
- PDF output via Apache PDFBox 3.0

**Total codebase:** ~4,100 lines of Java across 20 source files — a deliberately minimal footprint.

### EchoSVG

EchoSVG inherits Apache Batik's **modular, enterprise-grade architecture** built around the **GVT (Graphic Vector Toolkit)** scene graph model. It is split across dozens of subprojects:

- `echosvg-dom` — SVG DOM implementation
- `echosvg-parser` — SVG/XML parsing
- `echosvg-gvt` — Graphic Vector Toolkit scene graph
- `echosvg-bridge` — DOM-to-GVT bridge
- `echosvg-css` — CSS engine (powered by css4j)
- `echosvg-transcoder` — SVG → raster/vector conversion
- `echosvg-svggen` — Java2D → SVG generation
- `echosvg-ext` — Extensions
- …and more

EchoSVG uses its own rendering pipeline (GVT) rather than delegating directly to `Graphics2D`, giving it full control over the rendering process at the cost of higher complexity.

---

## SVG Feature Support

### Elements

| SVG Element | JairoSVG | EchoSVG |
|---|:---:|:---:|
| `rect`, `circle`, `ellipse`, `line` | ✅ | ✅ |
| `polygon`, `polyline` | ✅ | ✅ |
| `path` (M, L, C, S, Q, T, A, H, V, Z) | ✅ | ✅ |
| `text`, `tspan` | ✅ | ✅ |
| `textPath` | ✅ | ✅ |
| `image` (raster + nested SVG) | ✅ | ✅ |
| `use`, `defs`, `symbol` | ✅ | ✅ |
| `g` (grouping) | ✅ | ✅ |
| `svg` (nested) | ✅ | ✅ |
| `clipPath` | ✅ | ✅ |
| `mask` | ✅ | ✅ |
| `marker` | ✅ | ✅ |
| `pattern` | ✅ | ✅ |
| `linearGradient` / `radialGradient` | ✅ | ✅ |
| `filter` (basic) | ✅ | ✅ |
| `switch` / conditional processing | ✅ | ✅ |
| `foreignObject` | ❌ | ✅ |
| `animate`, `animateTransform`, `set` | ❌ | ✅ |
| SVG Fonts (`font`, `glyph`) | ❌ | ✅ |
| `script` | ❌ | ✅ (JavaScript via Rhino) |
| `cursor` | ❌ | ✅ |

### Attributes & Features

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| `viewBox` + `preserveAspectRatio` | ✅ | ✅ |
| CSS inline styles | ✅ | ✅ |
| CSS `<style>` blocks | ✅ | ✅ |
| Transforms (translate, rotate, scale, skew, matrix) | ✅ | ✅ |
| Opacity (element, fill, stroke) | ✅ | ✅ |
| `fill-rule` (evenodd, nonzero) | ✅ | ✅ |
| Stroke properties (dasharray, linecap, linejoin) | ✅ | ✅ |
| Gradient `spreadMethod` | ✅ | ✅ |
| Units (px, pt, em, %, cm, mm, in) | ✅ | ✅ |
| `letter-spacing` | ✅ | ✅ |
| `text-anchor` | ✅ | ✅ |
| Named colors (170+) | ✅ | ✅ |
| `rgb()` / `rgba()` / hex colors | ✅ | ✅ |
| `font` shorthand | ✅ | ✅ |

---

## CSS Support

| Capability | JairoSVG | EchoSVG |
|---|:---:|:---:|
| Inline `style` attribute | ✅ | ✅ |
| `<style>` element with type selectors | ✅ | ✅ |
| Class selectors (`.class`) | ✅ | ✅ |
| ID selectors (`#id`) | ✅ | ✅ |
| CSS Level 4 selectors | ❌ | ✅ |
| `calc()` | ❌ | ✅ |
| CSS nesting | ❌ | ❌ |
| `@import` rules | ❌ | ✅ |
| Modern color functions (e.g. `hsl()`) | ❌ | ✅ |
| Pseudo-classes / pseudo-elements | ❌ | Partial |

EchoSVG integrates the **css4j** CSS parser, giving it significantly more advanced CSS support than JairoSVG's lightweight built-in processor. JairoSVG's CSS support covers the common patterns used in SVG files but does not aim for full CSS specification compliance.

---

## Output Formats

| Format | JairoSVG | EchoSVG |
|---|:---:|:---:|
| PNG | ✅ | ✅ |
| PDF | ✅ (via PDFBox 3.0) | ✅ (via Batik transcoder) |
| PostScript (PS) | ✅ | ✅ |
| EPS | ✅ | ❌ |
| SVG (re-render) | ✅ | ✅ |
| JPEG | ❌ | ✅ |
| TIFF | ❌ | ✅ |

---

## API & Developer Experience

### JairoSVG — Simple & Fluent

```java
// One-liner conversion
byte[] png = JairoSVG.svg2png(svgBytes);

// Fluent builder with options
byte[] scaled = JairoSVG.builder()
    .fromBytes(svgBytes)
    .dpi(150)
    .scale(2)
    .backgroundColor("#ffffff")
    .toPng();

// Get BufferedImage directly
BufferedImage image = JairoSVG.builder()
    .fromFile(Path.of("icon.svg"))
    .toImage();
```

JairoSVG provides **static convenience methods** for common conversions and a **fluent builder** for options. The entire public API is in a single class (`JairoSVG`).

### EchoSVG — Transcoder Pattern

```java
PNGTranscoder transcoder = new PNGTranscoder();
TranscoderInput input = new TranscoderInput(new FileInputStream("input.svg"));
ByteArrayOutputStream baos = new ByteArrayOutputStream();
TranscoderOutput output = new TranscoderOutput(baos);
transcoder.transcode(input, output);
byte[] png = baos.toByteArray();
```

EchoSVG uses the **Transcoder API** inherited from Batik. It is more verbose but offers fine-grained control via transcoding hints. EchoSVG also provides a **Swing component** (`JSVGCanvas`) for interactive SVG display — a capability JairoSVG does not have.

### CLI

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| CLI tool included | ✅ | ✅ (rasterizer) |
| DPI control | ✅ | ✅ |
| Scale factor | ✅ | ✅ |
| Background color | ✅ | ✅ |
| Output size override | ✅ | ✅ |
| Color negation | ✅ | ❌ |
| Unsafe mode toggle | ✅ | N/A |

---

## Performance

SVG → PNG conversion benchmarks (from JairoSVG's benchmark suite, lower is better):

| Test Case | JairoSVG (Java) | EchoSVG (Java) | CairoSVG (Python) |
|---|:---:|:---:|:---:|
| Simple shapes | **3.7 ms** | 9.5 ms | 1.9 ms |
| Gradients + transforms | **6.7 ms** | 34.5 ms | 5.2 ms |
| Complex paths + text | **8.1 ms** | 29.7 ms | 6.1 ms |

*JairoSVG is 2–5× faster than EchoSVG and within 1.3–1.9× of CairoSVG's native C backend.*

> **Note:** Benchmarks were run with 20 warm-up iterations and 50 measured iterations. EchoSVG's overhead comes partly from the GVT scene graph construction, which provides capabilities (DOM access, scripting, animation) that JairoSVG does not support. For simple SVG-to-raster conversion, JairoSVG's direct Java2D rendering avoids this overhead. Results may vary by JVM, hardware, and SVG complexity. Reproduce with: `jbang benchmark.java`

---

## Dependencies & Footprint

### JairoSVG

| Metric | Value |
|---|---|
| Runtime dependencies | **1** (Apache PDFBox 3.0 — only needed for PDF output) |
| Source files | 20 |
| Lines of code | ~4,100 |
| Java version required | 25+ (with `--enable-preview`) |
| Build system | Maven |

### EchoSVG

| Metric | Value |
|---|---|
| Runtime dependencies | **Many** (css4j, xml-apis, multiple internal modules) |
| Subprojects/modules | 20+ |
| Lines of code | ~200,000+ (inherited from Batik) |
| Java version required | 11–24 |
| Build system | Gradle |

JairoSVG's minimal dependency tree makes it well-suited for containerized or size-sensitive deployments. EchoSVG's larger footprint reflects its broader feature set and Batik heritage.

---

## Security

| Concern | JairoSVG | EchoSVG |
|---|:---:|:---:|
| XXE protection by default | ✅ | ✅ |
| External resource blocking | ✅ (safe by default, `--unsafe` to allow) | Configurable |
| Script execution | N/A (no scripting support) | ✅ (can be disabled) |
| `SecurityManager` integration | ❌ | ✅ |

JairoSVG's lack of scripting support is itself a security advantage — there is no attack surface for script injection. External file access is blocked by default and must be explicitly enabled with the `unsafe` flag.

---

## Visual Rendering Comparison

A side-by-side visual comparison of 12 SVG test cases is available in the [`comparison/`](comparison/) directory, covering:

1. Basic shapes
2. Gradients
3. Complex paths
4. Text rendering
5. Transforms
6. Stroke styles
7. Opacity & blending
8. ViewBox & aspect ratio
9. CSS styling
10. Use & defs
11. Star polygon (fill-rule)
12. Nested SVG

See the [comparison gallery](comparison/README.md) for rendered PNG output from both libraries. Regenerate with:

```bash
./mvnw install -DskipTests
jbang comparison/generate.java
```

---

## Summary

| Dimension | JairoSVG | EchoSVG |
|---|---|---|
| **Best for** | Fast SVG → raster/PDF conversion | Full SVG toolkit with DOM, scripting, animation |
| **SVG spec** | SVG 1.1 (static) | SVG 1.1 + partial SVG 2 |
| **CSS** | Basic (class, ID, type selectors) | Advanced (CSS Level 4, css4j) |
| **Performance** | 2–5× faster for conversion | Slower, but supports more features |
| **API simplicity** | One-liner / fluent builder | Transcoder pattern (more verbose) |
| **Codebase** | ~4K LOC, 1 dependency | ~200K+ LOC, many modules |
| **Java version** | 25+ | 11–24 |
| **Animation** | ❌ | ✅ |
| **Scripting** | ❌ | ✅ |
| **Swing viewer** | ❌ | ✅ |
| **License** | LGPL-3.0 | Apache-2.0 |

---

## When to Choose Which

**Choose JairoSVG when you need:**
- Fast, lightweight SVG → PNG/PDF conversion
- Minimal dependencies and small deployment footprint
- A simple, fluent Java API
- Server-side batch rendering where startup time and throughput matter
- A secure default configuration with no scripting surface

**Choose EchoSVG when you need:**
- A full SVG toolkit with DOM manipulation, scripting, and animation
- Advanced CSS support (Level 4 selectors, `calc()`, modern color functions)
- A Swing-based SVG viewer component
- Broader output format support (JPEG, TIFF)
- Compatibility with Java 11–24 (JairoSVG requires Java 25+)
- `foreignObject` support or SVG font rendering

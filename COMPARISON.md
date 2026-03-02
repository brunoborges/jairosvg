# Feature Comparison: JairoSVG vs EchoSVG

A comprehensive comparison of two Java SVG libraries — **JairoSVG** and **EchoSVG** — to help developers choose the right tool for their SVG rendering needs.

---

## Overview

| | JairoSVG | EchoSVG |
|---|---|---|
| **Origin** | Java port of [CairoSVG](https://cairosvg.org) (Python) | Fork of [Apache Batik](https://xmlgraphics.apache.org/batik/) (Java) |
| **Maintainer** | Bruno Borges | css4j project |
| **Primary goal** | Fast, lightweight SVG → raster/vector conversion | Full-featured SVG toolkit: render, manipulate, and convert |
| **License** | LGPL-3.0 | Apache-2.0 |
| **Repository** | [github.com/brunoborges/jairosvg](https://github.com/brunoborges/jairosvg) | [github.com/css4j/echosvg](https://github.com/css4j/echosvg) |
| **Current version** | 1.0.0-SNAPSHOT | 2.4 |
| **Minimum Java** | Java 25 (uses preview features) | Java 11 |
| **SVG spec target** | SVG 1.1 | SVG 1.1 + partial SVG 2 |
| **Key strength** | Speed (2–5× faster than EchoSVG) | Feature completeness and standard compliance |

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

| Aspect | JairoSVG | EchoSVG |
|---|---|---|
| **Core rendering** | Java2D (`Graphics2D`, `BufferedImage`) | Java2D via GVT (Graphics Vector Tree) |
| **CSS engine** | Custom lightweight CSS parser | css4j (full CSS4 support) |
| **SVG DOM** | Read-only internal `Node` tree | Full mutable W3C SVG DOM |
| **Module structure** | Single JAR, ~20 focused classes | Multi-module Gradle project (20+ modules) |
| **Animation engine** | None | Full SMIL animation engine |
| **Scripting** | None | Mozilla Rhino (JavaScript) |
| **Filter pipeline** | Basic | Full SVG filter primitive chain |
| **Font handling** | Java AWT font system | Java AWT font system + SVG font support |
| **Extensibility** | Minimal (source-level) | High (custom elements, handlers, bridges) |
| **Test infrastructure** | JUnit 5 unit tests | Reference-image regression suite |

---

## SVG Element Support

| SVG Element | JairoSVG | EchoSVG |
|---|:---:|:---:|
| `<svg>`, `<g>` | ✅ | ✅ |
| `<rect>`, `<circle>`, `<ellipse>` | ✅ | ✅ |
| `<line>`, `<polyline>`, `<polygon>` | ✅ | ✅ |
| `<path>` (all commands) | ✅ | ✅ |
| `<text>`, `<tspan>` | ✅ | ✅ |
| `<textPath>` | ✅ | ✅ |
| `<image>` (raster + nested SVG) | ✅ | ✅ |
| `<use>`, `<defs>`, `<symbol>` | ✅ | ✅ |
| `<linearGradient>`, `<radialGradient>` | ✅ | ✅ |
| `<pattern>` | ✅ | ✅ |
| `<clipPath>` | ✅ | ✅ |
| `<mask>` | ✅ | ✅ |
| `<filter>` | ✅ (basic) | ✅ (full filter primitives) |
| `<marker>` | ✅ | ✅ |
| `<metadata>`, `<title>`, `<desc>` | ✅ (parsed, not rendered) | ✅ |
| `<foreignObject>` | ❌ | ✅ |
| `<animate>`, `<animateTransform>`, `<animateMotion>`, `<set>` | ❌ | ✅ (SMIL) |
| SVG Fonts (`<font>`, `<glyph>`) | ❌ | ✅ |
| `<script>` | ❌ | ✅ (Rhino JS engine) |
| `<cursor>` | ❌ | ✅ |

---

## SVG Attributes & Features

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| `viewBox` + `preserveAspectRatio` | ✅ | ✅ |
| Transforms (translate, rotate, scale, skewX, skewY, matrix) | ✅ | ✅ |
| Nested `<svg>` (independent viewports) | ✅ | ✅ |
| Opacity (element, fill, stroke) | ✅ | ✅ |
| `fill-rule` (nonzero / evenodd) | ✅ | ✅ |
| Stroke properties (dasharray, linecap, linejoin) | ✅ | ✅ |
| Gradient `spreadMethod` (pad / reflect / repeat) | ✅ | ✅ |
| `gradientUnits`, `gradientTransform` | ✅ | ✅ |
| `patternTransform` | ❌ | ✅ |
| `fill="url(#id)"` references | ✅ | ✅ |
| Units (px, pt, em, %, cm, mm, in) | ✅ | ✅ |
| `font` shorthand | ✅ | ✅ |
| `font-family`, `font-size`, `font-weight` | ✅ | ✅ |
| `letter-spacing`, `text-anchor` | ✅ | ✅ |
| `text-decoration` | ❌ | ✅ |
| Named colors (170+) | ✅ | ✅ |
| `currentColor` | ✅ | ✅ |
| `rgb()` / `rgba()` / hex colors | ✅ | ✅ |
| `hsl()` / `hsla()` | ❌ | ✅ |
| CSS Color Level 4 (`oklch`, `lab`, etc.) | ❌ | ✅ |

---

## CSS & Styling

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| Inline `style` attribute | ✅ | ✅ |
| `<style>` block (CSS stylesheet) | ✅ | ✅ |
| External CSS via `<?xml-stylesheet?>` | ❌ | ✅ |
| Class selectors | ✅ | ✅ |
| ID selectors | ✅ | ✅ |
| Descendant / child selectors | ✅ (basic) | ✅ |
| Pseudo-classes / pseudo-elements | ❌ | Partial |
| CSS Level 4 selectors | ❌ | ✅ (via css4j) |
| CSS custom properties (variables) | ❌ | ✅ |
| CSS `calc()` | ❌ | ✅ |
| CSS nesting | ❌ | ❌ |
| `@import` rules | ❌ | ✅ |
| `@supports` rules | ❌ | ✅ |

EchoSVG integrates the **css4j** CSS parser, giving it significantly more advanced CSS support than JairoSVG's lightweight built-in processor. JairoSVG's CSS support covers the common patterns used in SVG files but does not aim for full CSS specification compliance.

---

## Output Formats

| Format | JairoSVG | EchoSVG |
|---|:---:|:---:|
| PNG | ✅ | ✅ |
| PDF | ✅ (via Apache PDFBox 3.0) | ✅ (via FOP or transcoder) |
| PostScript (PS) | ✅ | ✅ |
| EPS | ✅ | ❌ |
| SVG (re-render) | ✅ | ✅ |
| JPEG | ❌ | ✅ |
| TIFF | ❌ | ✅ |
| `BufferedImage` (in-memory Java object) | ✅ | ✅ |

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

### API & Integration Comparison

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| Simple static method API | ✅ (`JairoSVG.svg2png(bytes)`) | ❌ (transcoder API) |
| Fluent builder API | ✅ | ❌ |
| Transcoder API (Batik-style) | ❌ | ✅ |
| Full SVG DOM (W3C DOM Level 2) | ❌ | ✅ |
| SVG DOM manipulation at runtime | ❌ | ✅ |
| Swing viewer component | ❌ | ✅ |
| CLI tool | ✅ | ✅ (rasterizer) |
| JBang support | ✅ | ❌ |
| Maven / Gradle dependency | ✅ | ✅ |
| GraalVM Native Image compatible | ✅ (no reflection) | ⚠️ (limited, reflection-heavy) |
| DPI control | ✅ | ✅ |
| Scale factor | ✅ | ✅ |
| Background color override | ✅ | ✅ |
| Color negation | ✅ | ❌ |
| Output width / height override | ✅ | ✅ |
| External file access control (XXE) | ✅ (disabled by default) | ✅ (configurable) |
| URL input (http/https) | ✅ | ✅ |

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
| Artifact size | ~1 fat JAR (~few MB incl. PDFBox) |
| Source files | 20 |
| Lines of code | ~4,100 |
| Java version required | 25+ (with `--enable-preview`) |
| Build system | Maven |

### EchoSVG

| Metric | Value |
|---|---|
| Runtime dependencies | **Many** (css4j, xml-apis, multiple internal modules) |
| Artifact size | Many modular JARs (each ~100 KB – 1 MB) |
| Subprojects/modules | 20+ |
| Lines of code | ~200,000+ (inherited from Batik) |
| Java version required | 11–24 |
| Build system | Gradle |

JairoSVG's minimal dependency tree makes it well-suited for containerized or size-sensitive deployments. EchoSVG's larger footprint reflects its broader feature set and Batik heritage.

---

## Security

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| XXE protection by default | ✅ | ✅ (configurable) |
| External resource loading disabled by default | ✅ | ✅ |
| `--unsafe` flag to opt-in to external access | ✅ | ✅ |
| Script execution | ❌ (not supported) | ✅ opt-in (Rhino JS) |
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
- GraalVM Native Image compatibility

**Choose EchoSVG when you need:**
- A full SVG toolkit with DOM manipulation, scripting, and animation
- Advanced CSS support (Level 4 selectors, `calc()`, modern color functions)
- A Swing-based SVG viewer component
- Broader output format support (JPEG, TIFF)
- Compatibility with Java 11–24 (JairoSVG requires Java 25+)
- `foreignObject` support or SVG font rendering
- Migrating from Apache Batik

---

**Both libraries are complementary:** JairoSVG excels as a fast conversion engine, while EchoSVG is better suited as a full SVG runtime.

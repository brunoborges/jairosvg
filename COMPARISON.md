# Feature Comparison: JairoSVG vs EchoSVG vs CairoSVG

A comprehensive comparison of three SVG libraries — **JairoSVG** (Java), **EchoSVG** (Java), and **CairoSVG** (Python) — to help developers choose the right tool for their SVG rendering needs. JairoSVG is a Java port of CairoSVG, so this comparison also tracks porting fidelity.

---

## Overview

| | JairoSVG | EchoSVG | CairoSVG |
|---|---|---|---|
| **Language** | Java 25+ | Java 11+ | Python 3.6+ |
| **Origin** | Java port of [CairoSVG](https://cairosvg.org) | Fork of [Apache Batik](https://xmlgraphics.apache.org/batik/) | Original project |
| **Maintainer** | Bruno Borges | css4j project | CourtBouillon / Kozea |
| **Primary goal** | Fast, lightweight SVG → raster/vector conversion | Full-featured SVG toolkit: render, manipulate, and convert | SVG → PNG/PDF/PS conversion |
| **License** | LGPL-3.0 | Apache-2.0 | LGPL-3.0 |
| **Repository** | [brunoborges/jairosvg](https://github.com/brunoborges/jairosvg) | [css4j/echosvg](https://github.com/css4j/echosvg) | [Kozea/CairoSVG](https://github.com/Kozea/CairoSVG) |
| **Current version** | 1.0.0-SNAPSHOT | 2.4 | 2.7+ |
| **SVG spec target** | SVG 1.1 | SVG 1.1 + partial SVG 2 | SVG 1.1 |
| **Rendering backend** | Java2D | GVT (Batik) → Java2D | Cairo (C library) |
| **Key strength** | Speed (2–5× faster than EchoSVG) | Feature completeness and standard compliance | Native C performance, mature ecosystem |

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

### CairoSVG

CairoSVG is a **Python library** built on the **Cairo 2D graphics library** (C). It uses `tinycss2` and `cssselect2` for CSS parsing, `lxml` or `ElementTree` for XML, and `Pillow` for raster image handling. The architecture is a set of Python modules (`surface.py`, `shapes.py`, `path.py`, `text.py`, `defs.py`, etc.) that JairoSVG mirrors directly.

### Architecture Comparison

| Aspect | JairoSVG | EchoSVG | CairoSVG |
|---|---|---|---|
| **Core rendering** | Java2D (`Graphics2D`) | GVT → Java2D | Cairo (C library) |
| **CSS engine** | Custom lightweight | css4j (CSS4 support) | tinycss2 + cssselect2 |
| **SVG DOM** | Read-only `Node` tree | Full mutable W3C DOM | ElementTree (read-only) |
| **Module structure** | Single JAR, ~20 classes | 20+ Gradle modules | Single Python package |
| **Animation engine** | None | Full SMIL | None |
| **Scripting** | None | Mozilla Rhino (JS) | None |
| **Filter pipeline** | Basic | Full primitives | 3 primitives (feBlend, feFlood, feOffset) |
| **Font handling** | Java AWT fonts | AWT + SVG fonts | Cairo font system |
| **Extensibility** | Minimal (source-level) | High (bridges, handlers) | Minimal (source-level) |

---

## SVG Element Support

| SVG Element | JairoSVG | EchoSVG | CairoSVG |
|---|:---:|:---:|:---:|
| `<svg>`, `<g>` | ✅ | ✅ | ✅ |
| `<rect>`, `<circle>`, `<ellipse>` | ✅ | ✅ | ✅ |
| `<line>`, `<polyline>`, `<polygon>` | ✅ | ✅ | ✅ |
| `<path>` (all commands) | ✅ | ✅ | ✅ |
| `<text>`, `<tspan>` | ✅ | ✅ | ✅ |
| `<textPath>` | ✅ | ✅ | ✅ |
| `<image>` (raster + nested SVG) | ✅ | ✅ | ✅ (via Pillow) |
| `<use>`, `<defs>` | ✅ | ✅ | ✅ |
| `<symbol>` | ✅ | ✅ | ❌ |
| `<linearGradient>`, `<radialGradient>` | ✅ | ✅ | ✅ |
| `<pattern>` | ✅ | ✅ | ⚠️ (naive) |
| `<clipPath>` | ✅ | ✅ | ✅ |
| `<mask>` | ✅ | ✅ | ⚠️ (alpha only) |
| `<filter>` | ✅ (basic) | ✅ (full primitives) | ⚠️ (feBlend, feFlood, feOffset only) |
| `<marker>` | ✅ | ✅ | ✅ (basic) |
| `<metadata>`, `<title>`, `<desc>` | ✅ (parsed, not rendered) | ✅ | ❌ (ignored) |
| `<foreignObject>` | ❌ ([#15](https://github.com/brunoborges/jairosvg/issues/15)) | ✅ | ❌ |
| `<animate>`, `<animateTransform>`, `<animateMotion>`, `<set>` | ❌ ([#16](https://github.com/brunoborges/jairosvg/issues/16)) | ✅ (SMIL) | ❌ |
| SVG Fonts (`<font>`, `<glyph>`) | ❌ ([#17](https://github.com/brunoborges/jairosvg/issues/17)) | ✅ | ❌ |
| `<script>` | ❌ ([#18](https://github.com/brunoborges/jairosvg/issues/18)) | ✅ (Rhino JS) | ❌ |
| `<cursor>` | ❌ ([#19](https://github.com/brunoborges/jairosvg/issues/19)) | ✅ | ❌ |

---

## SVG Attributes & Features

| Feature | JairoSVG | EchoSVG | CairoSVG |
|---|:---:|:---:|:---:|
| `viewBox` + `preserveAspectRatio` | ✅ | ✅ | ✅ |
| Transforms (translate, rotate, scale, skewX, skewY, matrix) | ✅ | ✅ | ✅ |
| Nested `<svg>` (independent viewports) | ✅ | ✅ | ✅ |
| Opacity (element, fill, stroke) | ✅ | ✅ | ✅ |
| `fill-rule` (nonzero / evenodd) | ✅ | ✅ | ✅ |
| Stroke properties (dasharray, linecap, linejoin) | ✅ | ✅ | ✅ |
| Gradient `spreadMethod` (pad / reflect / repeat) | ✅ | ✅ | ✅ |
| `gradientUnits`, `gradientTransform` | ✅ | ✅ | ✅ |
| `patternTransform` | ❌ ([#20](https://github.com/brunoborges/jairosvg/issues/20)) | ✅ | ❌ |
| `fill="url(#id)"` references | ✅ | ✅ | ✅ |
| Units (px, pt, em, %, cm, mm, in) | ✅ | ✅ | ✅ |
| `font` shorthand | ✅ | ✅ | ❌ |
| `font-family`, `font-size`, `font-weight` | ✅ | ✅ | ✅ (basic) |
| `letter-spacing`, `text-anchor` | ✅ | ✅ | ✅ |
| `text-decoration` | ❌ ([#21](https://github.com/brunoborges/jairosvg/issues/21)) | ✅ | ❌ |
| Named colors (170+) | ✅ | ✅ | ✅ |
| `currentColor` | ✅ | ✅ | ✅ |
| `rgb()` / `rgba()` / hex colors | ✅ | ✅ | ✅ |
| `hsl()` / `hsla()` | ✅ | ✅ | ❌ |
| CSS Color Level 4 (`oklch`, `lab`, etc.) | ❌ ([#23](https://github.com/brunoborges/jairosvg/issues/23)) | ✅ | ❌ |

---

## CSS & Styling

| Feature | JairoSVG | EchoSVG | CairoSVG |
|---|:---:|:---:|:---:|
| Inline `style` attribute | ✅ | ✅ | ✅ |
| `<style>` block (CSS stylesheet) | ✅ | ✅ | ✅ |
| External CSS via `<?xml-stylesheet?>` | ✅ (requires `--unsafe`) | ✅ | ✅ (basic) |
| Class selectors | ✅ | ✅ | ✅ |
| ID selectors | ✅ | ✅ | ✅ |
| Descendant / child selectors | ✅ (basic) | ✅ | ✅ (via cssselect2) |
| Pseudo-classes / pseudo-elements | ❌ ([#25](https://github.com/brunoborges/jairosvg/issues/25)) | Partial | Partial (via cssselect2) |
| CSS Level 4 selectors | ❌ ([#26](https://github.com/brunoborges/jairosvg/issues/26)) | ✅ (via css4j) | ❌ |
| CSS custom properties (variables) | ❌ ([#27](https://github.com/brunoborges/jairosvg/issues/27)) | ✅ | ❌ |
| CSS `calc()` | ❌ ([#28](https://github.com/brunoborges/jairosvg/issues/28)) | ✅ | ❌ |
| CSS nesting | ❌ | ❌ | ❌ |
| `@import` rules | ❌ ([#29](https://github.com/brunoborges/jairosvg/issues/29)) | ✅ | ❌ |
| `@supports` rules | ❌ ([#30](https://github.com/brunoborges/jairosvg/issues/30)) | ✅ | ❌ |

EchoSVG integrates the **css4j** CSS parser, giving it significantly more advanced CSS support. CairoSVG uses **tinycss2** + **cssselect2**, providing solid basic CSS support. JairoSVG's lightweight built-in processor covers the common patterns used in SVG files.

---

## Output Formats

| Format | JairoSVG | EchoSVG | CairoSVG |
|---|:---:|:---:|:---:|
| PNG | ✅ | ✅ | ✅ |
| PDF | ✅ (via Apache PDFBox 3.0) | ✅ (via FOP or transcoder) | ✅ (via Cairo) |
| PostScript (PS) | ✅ | ✅ | ✅ |
| EPS | ✅ | ❌ | ❌ |
| SVG (re-render) | ✅ | ✅ | ✅ |
| JPEG | ❌ ([#31](https://github.com/brunoborges/jairosvg/issues/31)) | ✅ | ❌ |
| TIFF | ❌ ([#32](https://github.com/brunoborges/jairosvg/issues/32)) | ✅ | ❌ |
| In-memory image object | ✅ (`BufferedImage`) | ✅ (`BufferedImage`) | ✅ (Cairo surface) |

---

## API & Developer Experience

### JairoSVG — Simple & Fluent (Java)

```java
byte[] png = JairoSVG.svg2png(svgBytes);

byte[] scaled = JairoSVG.builder()
    .fromBytes(svgBytes)
    .dpi(150)
    .scale(2)
    .backgroundColor("#ffffff")
    .toPng();

BufferedImage image = JairoSVG.builder()
    .fromFile(Path.of("icon.svg"))
    .toImage();
```

### EchoSVG — Transcoder Pattern (Java)

```java
PNGTranscoder transcoder = new PNGTranscoder();
TranscoderInput input = new TranscoderInput(new FileInputStream("input.svg"));
ByteArrayOutputStream baos = new ByteArrayOutputStream();
transcoder.transcode(input, new TranscoderOutput(baos));
byte[] png = baos.toByteArray();
```

### CairoSVG — Python Functions

```python
import cairosvg

png = cairosvg.svg2png(bytestring=svg_bytes)
cairosvg.svg2pdf(url="input.svg", write_to="output.pdf")
cairosvg.svg2png(url="input.svg", write_to="output.png",
                 dpi=150, scale=2, background_color="#ffffff")
```

### API Comparison

| Feature | JairoSVG | EchoSVG | CairoSVG |
|---|:---:|:---:|:---:|
| Simple static method API | ✅ | ❌ (transcoder API) | ✅ |
| Fluent builder API | ✅ | ❌ | ❌ (keyword args) |
| Transcoder API (Batik-style) | ❌ | ✅ | ❌ |
| Full SVG DOM (W3C DOM) | ❌ | ✅ | ❌ |
| SVG DOM manipulation at runtime | ❌ | ✅ | ❌ |
| Swing / GUI viewer component | ❌ | ✅ | ❌ |
| CLI tool | ✅ | ✅ (rasterizer) | ✅ |
| DPI control | ✅ | ✅ | ✅ |
| Scale factor | ✅ | ✅ | ✅ |
| Background color override | ✅ | ✅ | ✅ |
| Color negation | ✅ | ❌ | ✅ |
| Output width / height override | ✅ | ✅ | ✅ |
| External file access control (XXE) | ✅ (disabled by default) | ✅ (configurable) | ✅ (`unsafe` flag) |
| URL input (http/https) | ✅ | ✅ | ✅ |
| JBang support | ✅ | ❌ | N/A |
| GraalVM Native Image compatible | ✅ (no reflection) | ⚠️ (reflection-heavy) | N/A |

---

## Performance

SVG → PNG conversion benchmarks (lower is better):

| Test Case | JairoSVG (Java) | EchoSVG (Java) | CairoSVG (Python) |
|---|:---:|:---:|:---:|
| Simple shapes | **3.7 ms** | 9.5 ms | 1.9 ms |
| Gradients + transforms | **6.7 ms** | 34.5 ms | 5.2 ms |
| Complex paths + text | **8.1 ms** | 29.7 ms | 6.1 ms |

*JairoSVG is 2–5× faster than EchoSVG and within 1.3–1.9× of CairoSVG's native C backend.*

> **Note:** Benchmarks were run with 20 warm-up iterations and 50 measured iterations. CairoSVG's performance advantage comes from Cairo's native C rendering engine. EchoSVG's overhead comes partly from GVT scene graph construction. Results may vary by hardware and SVG complexity. Reproduce with: `jbang benchmark.java`

---

## Dependencies & Footprint

| Metric | JairoSVG | EchoSVG | CairoSVG |
|---|---|---|---|
| **Runtime dependencies** | 1 (PDFBox) | Many (css4j, xml-apis, …) | 5 (cairocffi, tinycss2, cssselect2, defusedxml, Pillow) |
| **Artifact size** | ~1 fat JAR | Many modular JARs | Single Python package |
| **Source files** | 20 | 20+ modules | ~10 modules |
| **Lines of code** | ~4,100 | ~200,000+ | ~4,000 |
| **Platform req.** | Java 25+ (`--enable-preview`) | Java 11–24 | Python 3.6+ / Cairo C lib |
| **Build system** | Maven | Gradle | pip / setuptools |
| **Native dependency** | None | None | Cairo C library required |

---

## Security

| Feature | JairoSVG | EchoSVG | CairoSVG |
|---|:---:|:---:|:---:|
| XXE protection by default | ✅ | ✅ (configurable) | ✅ (via defusedxml) |
| External resource loading disabled by default | ✅ | ✅ | ✅ |
| `--unsafe` flag to opt-in to external access | ✅ | ✅ | ✅ |
| Script execution | ❌ (not supported) | ✅ opt-in (Rhino JS) | ❌ (not supported) |
| `SecurityManager` integration | ❌ | ✅ | N/A |

JairoSVG and CairoSVG share the same security posture: no scripting support (eliminating script injection), external access blocked by default. EchoSVG offers more configurability but a larger attack surface.

---

## Visual Rendering Comparison

A side-by-side visual comparison of 12 SVG test cases (JairoSVG vs EchoSVG) is available in the [`comparison/`](comparison/) directory, covering:

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

See the [comparison gallery](comparison/README.md) for rendered PNG output. Regenerate with:

```bash
./mvnw install -DskipTests
jbang comparison/generate.java
```

---

## Summary

| Dimension | JairoSVG | EchoSVG | CairoSVG |
|---|---|---|---|
| **Best for** | Fast Java SVG conversion | Full SVG toolkit (DOM, scripting, animation) | Python SVG conversion |
| **SVG spec** | SVG 1.1 (static) | SVG 1.1 + partial SVG 2 | SVG 1.1 (static) |
| **CSS** | Basic (class, ID, type) | Advanced (CSS Level 4, css4j) | Basic (via tinycss2) |
| **Performance** | 2–5× faster than EchoSVG | Slowest (GVT overhead) | Fastest (native C) |
| **API simplicity** | One-liner / builder | Transcoder pattern | One-liner functions |
| **Codebase** | ~4K LOC, 1 dep | ~200K+ LOC, many modules | ~4K LOC, 5 deps |
| **Animation** | ❌ | ✅ | ❌ |
| **Scripting** | ❌ | ✅ | ❌ |
| **GUI viewer** | ❌ | ✅ | ❌ |
| **License** | LGPL-3.0 | Apache-2.0 | LGPL-3.0 |

---

## When to Choose Which

**Choose JairoSVG when you need:**
- Fast, lightweight SVG → PNG/PDF conversion **in Java**
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

**Choose CairoSVG when you need:**
- SVG conversion **in Python**
- The fastest raw conversion speed (native C backend)
- A mature, widely-used library with a large community
- Integration with Python web frameworks or data pipelines
- No JVM dependency

---

### JairoSVG Porting Fidelity

Since JairoSVG is a port of CairoSVG, most features should be at parity. Key differences:

| Feature | JairoSVG (Java port) | CairoSVG (Python original) |
|---|:---:|:---:|
| `<symbol>` element | ✅ | ❌ |
| `font` shorthand | ✅ | ❌ |
| EPS output | ✅ | ❌ |
| External CSS `<?xml-stylesheet?>` | ✅ (requires `--unsafe`) | ✅ |
| Gzip-compressed `.svgz` input | ❌ | ✅ |

JairoSVG adds features beyond CairoSVG (fluent builder API, `BufferedImage` output, EPS support) while maintaining the same core rendering approach.

---

**All three libraries are complementary:** JairoSVG and CairoSVG share DNA and excel as fast conversion engines (Java and Python respectively), while EchoSVG is a full SVG runtime.

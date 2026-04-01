# Feature Comparison: JairoSVG vs EchoSVG vs CairoSVG vs JSVG

A comprehensive comparison of four SVG libraries — **JairoSVG** (Java), **EchoSVG** (Java), **CairoSVG** (Python), and **JSVG** (Java) — to help developers choose the right tool for their SVG rendering needs.

## Table of Contents

- [Overview](#overview)
- [Architecture & Design](#architecture--design)
- [SVG Element Support](#svg-element-support)
- [SVG Attributes & Features](#svg-attributes--features)
- [CSS & Styling](#css--styling)
- [Output Formats](#output-formats)
- [API & Developer Experience](#api--developer-experience)
- [Benchmark](#benchmark)
- [Dependencies & Footprint](#dependencies--footprint)
- [Security](#security)
- [Visual Rendering Comparison](#visual-rendering-comparison)
- [Summary](#summary)
- [When to Choose Which](#when-to-choose-which)
- [What About ImageMagick?](#what-about-imagemagick)
- [Regenerating](#regenerating)

---

## Overview

|                       | JairoSVG                                                       | EchoSVG                                                    | CairoSVG                               | JSVG                                              |
| --------------------- | -------------------------------------------------------------- | ---------------------------------------------------------- | -------------------------------------- | ------------------------------------------------- |
| **Language**          | Java 25+                                                       | Java 8+                                                    | Python 3.6+                            | Java 8+                                           |
| **Origin**            | Independent project, API inspired by [CairoSVG]               | Fork of [Apache Batik]                                     | Original project                       | Independent project                               |
| **Maintainer**        | Bruno Borges                                                   | css4j project                                              | CourtBouillon / Kozea                  | Jannis Weis                                       |
| **Primary goal**      | Fast, lightweight SVG → raster/vector conversion               | Full-featured SVG toolkit: render, manipulate, and convert | SVG → PNG/PDF/PS conversion            | Lightweight SVG renderer for Swing / Java2D       |
| **License**           | LGPL-3.0                                                       | Apache-2.0                                                 | LGPL-3.0                               | MIT                                               |
| **Repository**        | [brunoborges/jairosvg]                                         | [css4j/echosvg]                                            | [Kozea/CairoSVG]                       | [weisJ/jsvg]                                      |
| **Current version**   | 1.0.4                                                          | 2.4                                                        | 2.7+                                   | 2.0.0                                             |
| **SVG spec target**   | SVG 1.1 + [selective SVG 2](../LIMITATIONS.md#svg-2-behavioral-alignment) | SVG 1.1 + partial SVG 2                                    | SVG 1.1                                | SVG 1.1 + partial SVG 2                           |
| **Rendering backend** | Java2D                                                         | GVT (Batik) → Java2D                                       | Cairo (C library)                      | Java2D                                            |
| **Key strength**      | Speed (2–26× faster than EchoSVG, on par with JSVG, 1–2.4× faster than CairoSVG) | Feature completeness and standard compliance               | Native C performance, mature ecosystem | Designed for Swing GUI embedding (IntelliJ, etc.) |

---

## Architecture & Design

### JairoSVG

JairoSVG renders SVG through the standard **Java2D** (`Graphics2D` / `BufferedImage`) API. The architecture is intentionally compact:

| Java Class     | Role                       |
| -------------- | -------------------------- |
| `JairoSVG`     | Public API + Builder       |
| `Surface`      | Java2D rendering engine    |
| `Node`         | SVG DOM tree               |
| `PathDrawer`   | SVG path commands          |
| `ShapeDrawer`  | Basic shapes               |
| `TextDrawer`   | Text rendering             |
| `Defs`         | Gradients, clips, use      |
| `Colors`       | Color parsing (170+ named) |
| `Helpers`      | Units, transforms          |
| `CssProcessor` | CSS parsing                |

**Key technologies:**

- `java.awt.Graphics2D` — 2D rendering context
- `java.awt.image.BufferedImage` — raster image buffer
- `java.awt.geom.AffineTransform` — coordinate transforms
- Apache PDFBox 3.0 — PDF output (optional dependency)

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

### JSVG

JSVG is a **lightweight Java SVG renderer** designed for AWT/Swing applications. It renders SVGs directly onto `Graphics2D` contexts with minimal memory usage (~50% less than svgSalamander, ~98% less than Batik). Used in production by **IntelliJ IDEA**, **Apache NetBeans**, **Eclipse SWT**, and **FlatLaf**. JSVG is a *renderer*, not a converter — it does not produce PNG/PDF output directly; users render to `BufferedImage` and handle file encoding themselves.

### Architecture Comparison

| Aspect               | JairoSVG                | EchoSVG                  | CairoSVG                                  | JSVG                              |
| -------------------- | ----------------------- | ------------------------ | ----------------------------------------- | --------------------------------- |
| **Core rendering**   | Java2D (`Graphics2D`)   | GVT → Java2D             | Cairo (C library)                         | Java2D (`Graphics2D`)            |
| **CSS engine**       | Custom lightweight      | css4j (CSS4 support)     | tinycss2 + cssselect2                     | Built-in (partial)               |
| **SVG DOM**          | Read-only `Node` tree   | Full mutable W3C DOM     | ElementTree (read-only)                   | `SVGDocument` (pre-processed)    |
| **Module structure** | Single JAR, ~20 classes | 20+ Gradle modules       | Single Python package                     | Single JAR, ~30K LOC             |
| **Animation engine** | None                    | Full SMIL                | None                                      | ⚠️ Partial (experimental)         |
| **Scripting**        | None                    | Mozilla Rhino (JS)       | None                                      | None                             |
| **Filter pipeline**  | Full (16 primitives)    | Full primitives          | 3 primitives (feBlend, feFlood, feOffset) | Most primitives (15+ supported)  |
| **Font handling**    | Java AWT fonts          | AWT + SVG fonts          | Cairo font system                         | Java AWT fonts                   |
| **Extensibility**    | Minimal (source-level)  | High (bridges, handlers) | Minimal (source-level)                    | `DomProcessor` + `LoaderContext` |

---

## SVG Element Support

| SVG Element                                                   |                                JairoSVG                                 |       EchoSVG        |                            CairoSVG                             |                                      JSVG                                       |
| ------------------------------------------------------------- | :---------------------------------------------------------------------: | :------------------: | :-------------------------------------------------------------: | :------------------------------------------------------------------------------: |
| `<svg>`, `<g>`                                                |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<rect>`, `<circle>`, `<ellipse>`                             |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<line>`, `<polyline>`, `<polygon>`                           |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<path>` (all commands)                                       |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<text>`, `<tspan>`                                           |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<textPath>`                                                  |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<image>` (raster + nested SVG)                               |                                   ✅                                    |          ✅          |                         ✅ (via Pillow)                         |                                        ✅                                        |
| `<use>`, `<defs>`                                             |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<symbol>`                                                    |                                   ✅                                    |          ✅          |                               ❌                                |                                        ✅                                        |
| `<linearGradient>`, `<radialGradient>`                        |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<pattern>`                                                   |                                   ✅                                    |          ✅          |                           ⚠️ (naive)                            |                                        ✅                                        |
| `<clipPath>`                                                  |                                   ✅                                    |          ✅          |                               ✅                                |                                        ✅                                        |
| `<mask>`                                                      |                                   ✅                                    |          ✅          |                         ⚠️ (alpha only)                         |                                        ✅                                        |
| `<filter>`                                                    | ✅ (16 primitives — full SVG 1.1) | ✅ (full primitives) | ⚠️ (`feBlend`, `feFlood`, `feOffset` only; no blur/drop-shadow) | ✅ (most primitives; no `feImage`, `feTile`, `feMorphology`, lighting effects) |
| `<marker>`                                                    |                                   ✅                                    |          ✅          |                           ✅ (basic)                            |                                        ✅                                        |
| `<metadata>`, `<title>`, `<desc>`                             |                        ✅ (parsed, not rendered)                        |          ✅          |                          ❌ (ignored)                           |                              ✅ (parsed, not rendered)                              |
| `<foreignObject>`                                             |                               ❌ ([#15])                                |          ✅          |                               ❌                                |                                        ❌                                        |
| `<animate>`, `<animateTransform>`, `<animateMotion>`, `<set>` |                               ❌ ([#16])                                |      ✅ (SMIL)       |                               ❌                                |                          ⚠️ (partial `animate`/`animateTransform`)                          |
| SVG Fonts (`<font>`, `<glyph>`)                               |                                   ✅                                    |          ✅          |                               ❌                                |                                        ❌                                        |
| `<script>`                                                    |                               ❌ ([#18])                                |    ✅ (Rhino JS)     |                               ❌                                |                                        ❌                                        |
| `<cursor>`                                                    |                               ❌ ([#19])                                |          ✅          |                               ❌                                |                                        ❌                                        |

---

## SVG Attributes & Features

| Feature                                                     |  JairoSVG  | EchoSVG |  CairoSVG  |   JSVG   |
| ----------------------------------------------------------- | :--------: | :-----: | :--------: | :------: |
| `viewBox` + `preserveAspectRatio`                           |     ✅     |   ✅    |     ✅     |    ✅    |
| Transforms (translate, rotate, scale, skewX, skewY, matrix) |     ✅     |   ✅    |     ✅     |    ✅    |
| Nested `<svg>` (independent viewports)                      |     ✅     |   ✅    |     ✅     |    ✅    |
| Opacity (element, fill, stroke)                             |     ✅     |   ✅    |     ✅     |    ✅    |
| `fill-rule` (nonzero / evenodd)                             |     ✅     |   ✅    |     ✅     |    ✅    |
| Stroke properties (dasharray, linecap, linejoin)            |     ✅     |   ✅    |     ✅     |    ✅    |
| Gradient `spreadMethod` (pad / reflect / repeat)            |     ✅     |   ✅    |     ✅     |    ✅    |
| `gradientUnits`, `gradientTransform`                        |     ✅     |   ✅    |     ✅     |    ✅    |
| `patternTransform`                                          |     ✅     |   ✅    |     ❌     |    ✅    |
| `fill="url(#id)"` references                                |     ✅     |   ✅    |     ✅     |    ✅    |
| Units (px, pt, em, %, cm, mm, in)                           |     ✅     |   ✅    |     ✅     |    ✅    |
| `font` shorthand                                            |     ✅     |   ✅    |     ❌     |    ✅    |
| `font-family`, `font-size`, `font-weight`                   |     ✅     |   ✅    | ✅ (basic) |    ✅    |
| `letter-spacing`, `text-anchor`                             |     ✅     |   ✅    |     ✅     |    ✅    |
| `text-decoration`                                           |     ✅     |   ✅    |     ❌     |    ✅    |
| Named colors (170+)                                         |     ✅     |   ✅    |     ✅     |    ✅    |
| `currentColor`                                              |     ✅     |   ✅    |     ✅     |    ✅    |
| `rgb()` / `rgba()` / hex colors                             |     ✅     |   ✅    |     ✅     |    ✅    |
| `hsl()` / `hsla()`                                          |     ✅     |   ✅    |     ❌     |    ✅    |
| CSS Color Level 4 (`oklch`, `lab`, etc.)                    | ❌ ([#23]) |   ✅    |     ❌     |    ❌    |

---

## CSS & Styling

| Feature                               |                                            JairoSVG                                            |    EchoSVG     |         CairoSVG         |       JSVG        |
| ------------------------------------- | :--------------------------------------------------------------------------------------------: | :------------: | :----------------------: | :---------------: |
| Inline `style` attribute              |                                               ✅                                               |       ✅       |            ✅            |        ✅         |
| `<style>` block (CSS stylesheet)      |                                               ✅                                               |       ✅       |            ✅            |    ⚠️ (partial)    |
| External CSS via `<?xml-stylesheet?>` |                                    ✅ (requires `--unsafe`)                                    |       ✅       |        ✅ (basic)        |        ❌         |
| Class selectors                       |                                               ✅                                               |       ✅       |            ✅            |        ✅         |
| ID selectors                          |                                               ✅                                               |       ✅       |            ✅            |        ✅         |
| Descendant / child selectors          |                                           ✅ (basic)                                           |       ✅       |   ✅ (via cssselect2)    |    ⚠️ (partial)    |
| Pseudo-classes / pseudo-elements      | ✅ (`:first-child`, `:last-child`, `:nth-child()`, `:not()`, `::first-line`, `::first-letter`) |    Partial     | Partial (via cssselect2) |        ❌         |
| CSS Level 4 selectors                 |                                           ❌ ([#26])                                           | ✅ (via css4j) |            ❌            |        ❌         |
| CSS custom properties (variables)     |                                               ✅                                               |       ✅       |            ❌            |        ❌         |
| CSS `calc()`                          |                                           ❌ ([#28])                                           |       ✅       |            ❌            |        ❌         |
| CSS nesting                           |                                               ❌                                               |       ✅       |            ❌            |        ❌         |
| `@import` rules                       |                                               ✅                                               |       ✅       |            ❌            |        ❌         |
| `@supports` rules                     |                                           ❌ ([#30])                                           |       ✅       |            ❌            |        ❌         |

EchoSVG integrates the **css4j** CSS parser, giving it significantly more advanced CSS support. CairoSVG uses **tinycss2** + **cssselect2**, providing solid basic CSS support. JairoSVG's lightweight built-in processor covers the common patterns used in SVG files. JSVG has partial `<style>` support focused on the CSS patterns most common in SVG icon sets.

---

## Output Formats

| Format                 |          JairoSVG          |          EchoSVG           |      CairoSVG      |               JSVG                |
| ---------------------- | :------------------------: | :------------------------: | :----------------: | :-------------------------------: |
| PNG                    |             ✅             |             ✅             |         ✅         | ⚠️ (render + `ImageIO` by user)   |
| PDF                    | ✅ (via Apache PDFBox 3.0) | ✅ (via FOP or transcoder) |   ✅ (via Cairo)   |                ❌                 |
| PostScript (PS)        |             ✅             |             ✅             |         ✅         |                ❌                 |
| EPS                    |             ✅             |             ❌             |         ❌         |                ❌                 |
| JPEG                   |             ✅             |             ✅             |         ❌         | ⚠️ (render + `ImageIO` by user)   |
| TIFF                   |             ✅             |             ✅             |         ❌         | ⚠️ (render + `ImageIO` by user)   |
| In-memory image object |    ✅ (`BufferedImage`)    |    ✅ (`BufferedImage`)    | ✅ (Cairo surface) |       ✅ (`BufferedImage`)        |

> **Note:** JSVG is a *renderer*, not a converter. It renders SVG to any `Graphics2D` context (including `BufferedImage`), but does not include built-in file export. Users must handle PNG/JPEG encoding themselves via `ImageIO`.

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

### JSVG — Loader + Render (Java)

```java
SVGLoader loader = new SVGLoader();
SVGDocument doc = loader.load(new File("icon.svg").toURI().toURL());

FloatSize size = doc.size();
BufferedImage image = new BufferedImage((int) size.width, (int) size.height,
        BufferedImage.TYPE_INT_ARGB);
Graphics2D g = image.createGraphics();
doc.render(null, g);
g.dispose();

ImageIO.write(image, "PNG", new File("output.png"));
```

### API Comparison

| Feature                            |         JairoSVG         |        EchoSVG        |      CairoSVG      |             JSVG              |
| ---------------------------------- | :----------------------: | :-------------------: | :----------------: | :---------------------------: |
| Simple static method API           |            ✅            |  ❌ (transcoder API)  |         ✅         |     ❌ (loader + render)      |
| Fluent builder API                 |            ✅            |          ❌           | ❌ (keyword args)  |              ❌               |
| Transcoder API (Batik-style)       |            ❌            |          ✅           |         ❌         |              ❌               |
| Full SVG DOM (W3C DOM)             |            ❌            |          ✅           |         ❌         |              ❌               |
| SVG DOM manipulation at runtime    |            ❌            |          ✅           |         ❌         |    ⚠️ (via `DomProcessor`)     |
| Swing / GUI viewer component       |            ❌            |          ✅           |         ❌         |  ✅ (render to `Graphics2D`)  |
| CLI tool                           |            ✅            |    ✅ (rasterizer)    |         ✅         |              ❌               |
| DPI control                        |            ✅            |          ✅           |         ✅         | ❌ (user scales via `ViewBox`) |
| Scale factor                       |            ✅            |          ✅           |         ✅         |      ✅ (via `ViewBox`)       |
| Background color override          |            ✅            |          ✅           |         ✅         |    ❌ (user fills manually)    |
| Color negation                     |            ✅            |          ❌           |         ✅         |              ❌               |
| Output width / height override     |            ✅            |          ✅           |         ✅         |      ✅ (via `ViewBox`)       |
| External file access control (XXE) | ✅ (disabled by default) |   ✅ (configurable)   | ✅ (`unsafe` flag) |  ✅ (via `LoaderContext`)     |
| URL input (http/https)             |            ✅            |          ✅           |         ✅         |       ✅ (via `URL`)          |
| JBang support                      |            ✅            |          ❌           |        N/A         |              ❌               |
| GraalVM Native Image compatible    |    ✅ (no reflection)    | ⚠️ (reflection-heavy) |        N/A         |          ✅ (likely)          |

---

## Benchmark

SVG → PNG conversion benchmarks comparing all four libraries across 42 SVG test files. Full results in **[benchmark/](benchmark/)**.

**Highlights** (lower is better):

| Test Case              | JairoSVG (Java) | EchoSVG (Java) | JSVG (Java)  | CairoSVG (Python) |
| ---------------------- | :-------------: | :------------: | :----------: | :---------------: |
| Basic shapes           |     3.2 ms      |    15.7 ms     | **3.2 ms**   |      4.1 ms       |
| Gradients              |   **4.1 ms**    |    128.8 ms    |   4.1 ms     |     10.4 ms       |
| Filters                |   **7.1 ms**    |    33.7 ms     |   7.9 ms     |     4.3 ms ⚠️     |
| Fe blend modes         |  **10.1 ms**    |    27.2 ms     |  20.0 ms     |     12.4 ms       |
| Embedded image         |   **4.3 ms**    |    15.7 ms     |  10.8 ms     |      6.9 ms       |
| Localized masks        |  **14.3 ms**    |    55.0 ms     |  14.3 ms     |     15.3 ms       |

_JairoSVG is **2–26× faster** than EchoSVG, **on par with JSVG** in most scenarios, and **1–2.4× faster** than CairoSVG._

> ⚠️ Where CairoSVG appears faster on filters/masks, it is because it silently **skips** rendering those effects.

JairoSVG also produces the **smallest PNGs** overall — 8% smaller than CairoSVG, 11.5% smaller than JSVG, and 17.3% smaller than EchoSVG.

See **[benchmark/](benchmark/)** for the full timing table, file size comparison, rendering settings analysis, and instructions to run the benchmark yourself.

---

## Dependencies & Footprint

| Metric                   | JairoSVG                                    | EchoSVG                   | CairoSVG                                                | JSVG                            |
| ------------------------ | ------------------------------------------- | ------------------------- | ------------------------------------------------------- | ------------------------------- |
| **Runtime dependencies** | 0 (PDFBox optional)                         | Many (css4j, …)           | 5 (cairocffi, tinycss2, cssselect2, defusedxml, Pillow) | 0                               |
| **Disk footprint**       | **~130 KB** (PNG only), ~2.1 MB with PDFBox | ~5.7 MB (25 JARs)        | ~16.6 MB (Python pkgs + Pillow + Cairo C lib)           | ~350 KB                         |
| **Artifact size**        | 1 JAR (~130 KB) + CLI shaded JAR             | Many modular JARs         | Single Python package                                   | 1 JAR                           |
| **Source files**         | 20                                          | 20+ modules               | ~10 modules                                             | ~30K LOC                        |
| **Lines of code**        | ~4,100                                      | ~200,000+                 | ~4,000                                                  | ~30,000                         |
| **Platform req.**        | Java 25+ (`--enable-preview`)               | Java 8+                   | Python 3.6+ / Cairo C lib                               | Java 11+                        |
| **Build system**         | Maven                                       | Gradle                    | pip / setuptools                                        | Gradle                          |
| **Native dependency**    | None                                        | None                      | Cairo C library required                                | None                            |

---

## Security

| Feature                                       |      JairoSVG      |       EchoSVG        |      CairoSVG       |        JSVG        |
| --------------------------------------------- | :----------------: | :------------------: | :-----------------: | :----------------: |
| XXE protection by default                     |         ✅         |  ✅ (configurable)   | ✅ (via defusedxml) |   ✅ (disabled)    |
| External resource loading disabled by default |         ✅         |          ✅          |         ✅          | ✅ (configurable via `LoaderContext`) |
| `--unsafe` flag to opt-in to external access  |         ✅         |          ✅          |         ✅          | ❌ (no CLI)        |
| Script execution                              | ❌ (not supported) | ✅ opt-in (Rhino JS) | ❌ (not supported)  | ❌ (not supported) |
| `SecurityManager` integration                 |         ❌         |          ✅          |         N/A         |         ❌         |

JairoSVG, CairoSVG, and JSVG share a similar security posture: no scripting support (eliminating script injection), external access blocked or configurable by default. EchoSVG offers more configurability but a larger attack surface.

---

## Visual Rendering Comparison

Side-by-side PNG rendering of 42 SVG test cases across all four libraries. Full gallery in **[visual/](visual/)**.

**Sample** — `20_fe_blend_modes`:

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/20_fe_blend_modes.svg) | ![JairoSVG](visual/png/jairosvg/20_fe_blend_modes.png) | ![EchoSVG](visual/png/echosvg/20_fe_blend_modes.png) | ![CairoSVG](visual/png/cairosvg/20_fe_blend_modes.png) | ![JSVG](visual/png/jsvg/20_fe_blend_modes.png) |

The test suite covers basic shapes, gradients, paths, text, transforms, strokes, opacity, viewBox, CSS styling, patterns, clip paths, masks, markers, filters (blur, blend, tile, image, color matrix, morphology, turbulence, displacement, lighting, convolve matrix, component transfer), embedded images, SVG fonts, symbols, and more.

See **[visual/](visual/)** for the complete gallery with browser-rendered SVG alongside each library's PNG output.

---

## Summary

| Dimension          | JairoSVG                                               | EchoSVG                                      | CairoSVG                                       | JSVG                                   |
| ------------------ | ------------------------------------------------------ | -------------------------------------------- | ---------------------------------------------- | -------------------------------------- |
| **Best for**       | Fast Java SVG conversion                               | Full SVG toolkit (DOM, scripting, animation) | Python SVG conversion                          | SVG rendering in Swing/Java2D GUIs     |
| **SVG spec**       | SVG 1.1 + [selective SVG 2](../LIMITATIONS.md#svg-2-behavioral-alignment) (static) | SVG 1.1 + partial SVG 2                      | SVG 1.1 (static)                               | SVG 1.1 + partial SVG 2               |
| **CSS**            | Basic + structural pseudo selectors                    | Advanced (CSS Level 4, css4j)                | Basic (via tinycss2)                           | Good CSS support                       |
| **Performance**    | 2–31× faster than EchoSVG; on par with JSVG; 1–2.5× faster than CairoSVG | Slowest (GVT overhead)                       | Fast (native C), but skips some filter effects | Fast (lightweight, designed for Swing) |
| **API simplicity** | One-liner / builder                                    | Transcoder pattern                           | One-liner functions                            | SVGLoader + render()                   |
| **Codebase**       | ~4K LOC, 1 dep                                         | ~200K+ LOC, many modules                     | ~4K LOC, 5 deps                                | ~30K LOC, minimal deps                 |
| **Animation**      | ❌                                                     | ✅                                           | ❌                                             | ❌                                     |
| **Scripting**      | ❌                                                     | ✅                                           | ❌                                             | ❌                                     |
| **GUI viewer**     | ❌                                                     | ✅                                           | ❌                                             | ✅ (Swing component)                   |
| **License**        | LGPL-3.0                                               | Apache-2.0                                   | LGPL-3.0                                       | MIT                                    |

---

## When to Choose Which

**Choose JairoSVG when you need:**

- True cross-platform, fast, lightweight SVG → PNG/PDF conversion
- Minimal dependencies and small deployment footprint
- A simple, fluent Java API
- Server-side batch rendering where startup time and throughput matter
- A secure default configuration with no scripting surface
- GraalVM Native Image compatibility

**Choose EchoSVG when you need:**

- A full SVG toolkit with DOM manipulation, scripting, and animation
- Advanced CSS support (Level 4 selectors, `calc()`, modern color functions)
- A Swing-based SVG viewer component
- Advanced SVG toolkit capabilities beyond conversion (DOM, scripting, animation)
- Compatibility with Java 8+ (JairoSVG requires Java 25+)
- `foreignObject` support or SVG font rendering
- Migrating from Apache Batik

**Choose CairoSVG when you need:**

- SVG conversion **in Python**
- The fastest raw conversion speed (native C backend)
- A mature, widely-used library with a large community
- Integration with Python web frameworks or data pipelines
- No JVM dependency

**Choose JSVG when you need:**

- A lightweight SVG renderer to embed in a **Swing/Java2D GUI application**
- A Swing component to display SVG icons or diagrams interactively
- Compatibility with Java 11+ (JairoSVG requires Java 25+)
- MIT-licensed code with minimal dependencies
- An actively maintained renderer optimized for IntelliJ Platform and similar desktop apps

---

### JairoSVG vs CairoSVG

JairoSVG's API design was inspired by CairoSVG's simplicity. Key differences:

| Feature                           |       JairoSVG       |       CairoSVG       |
| --------------------------------- | :------------------: | :------------------: |
| `<symbol>` element                |          ✅          |          ❌          |
| `font` shorthand                  |          ✅          |          ❌          |
| EPS output                        |          ✅          |          ❌          |
| External CSS `<?xml-stylesheet?>` | ✅ (requires `--unsafe`) |          ✅          |
| Gzip-compressed `.svgz` input     |          ✅          |          ✅          |

JairoSVG adds features beyond CairoSVG (fluent builder API, `BufferedImage` output, EPS support, extensive filter primitives) while maintaining a similarly simple API surface.

---

## What About ImageMagick?

[ImageMagick](https://imagemagick.org/) is a popular command-line image processing toolkit that supports SVG as an input format. However, testing against the same 19 SVG test cases used in this comparison reveals that **ImageMagick is not a reliable SVG-to-PNG converter**. Out of 19 test cases, ImageMagick **failed on 11** (58%) — crashing, producing errors, or generating incorrect output.

### Failure Summary

| Category                      | Affected Test Cases        | Error                                                                                                                                                                    |
| ----------------------------- | -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Font resolution**           | 04, 08, 09, 12, 13, 14, 17 | `unable to read font ''` — ImageMagick cannot resolve font families from SVG `<style>` blocks or `font-family` attributes                                                |
| **Crashes (segfaults)**       | 05, 18, 19                 | `malloc: pointer being freed was not allocated` / `Trace/BPT trap` — the built-in MSVG renderer crashes on complex transforms, embedded base64 images, and advanced text |
| **Gradient/paint references** | 15                         | `unrecognized color 'fadeLR'` — fails to resolve `url(#id)` gradient references used in masks                                                                            |

### Root Cause

ImageMagick's built-in SVG renderer (MSVG) is a minimal implementation that lacks:

- **CSS `<style>` parsing** — inline stylesheets are largely ignored
- **Font fallback** — if the exact font isn't found on the system, rendering fails entirely
- **Gradient/paint server resolution** — `url(#id)` references in fill/stroke are not reliably resolved
- **Robust memory management** — complex SVG inputs trigger segfaults and aborts

Even when ImageMagick can be configured to delegate SVG rendering to an external library (e.g., librsvg via `--delegate`), the default installation does not include this, and the built-in renderer is what most users encounter.

### Performance

Even for the 8 test cases where ImageMagick succeeds, performance is significantly worse than all three dedicated SVG libraries. Each conversion spawns a new `magick` process, so there is unavoidable process startup overhead. In the Gradients scenario, for example, **ImageMagick is roughly 10× slower than JairoSVG**.

### Verdict

ImageMagick is an excellent tool for raster image manipulation (resize, crop, compose, format conversion), but its SVG support is too limited for production use. For reliable SVG → PNG conversion, use a dedicated SVG library like JairoSVG, EchoSVG, CairoSVG, or JSVG.

---

## Regenerating

**PNG renders + visual comparison** — regenerates all PNG renders and rewrites [visual/README.md](visual/):

Prerequisites: [JBang], Java 25+, Python 3 with CairoSVG (`python3 -m pip install cairosvg`), JairoSVG installed in local Maven repo.

```bash
./mvnw install -DskipTests
jbang comparison/visual/generate.java
```

**Benchmark data** — the timing table and file size table in [benchmark/README.md](benchmark/) must be updated manually after running:

```bash
jbang comparison/benchmark/benchmark.java
```

Copy the benchmark output into `benchmark/README.md`.

---

**All four libraries are complementary:** JairoSVG and CairoSVG share DNA and excel as fast conversion engines (Java and Python respectively), EchoSVG is a full SVG runtime, and JSVG is designed for lightweight GUI embedding.

<!-- Link references -->

[CairoSVG]: https://cairosvg.org
[Apache Batik]: https://xmlgraphics.apache.org/batik/
[brunoborges/jairosvg]: https://github.com/brunoborges/jairosvg
[css4j/echosvg]: https://github.com/css4j/echosvg
[Kozea/CairoSVG]: https://github.com/Kozea/CairoSVG
[weisJ/jsvg]: https://github.com/weisJ/jsvg
[JBang]: https://www.jbang.dev/
[#15]: https://github.com/brunoborges/jairosvg/issues/15
[#16]: https://github.com/brunoborges/jairosvg/issues/16
[#18]: https://github.com/brunoborges/jairosvg/issues/18
[#19]: https://github.com/brunoborges/jairosvg/issues/19
[#23]: https://github.com/brunoborges/jairosvg/issues/23
[#26]: https://github.com/brunoborges/jairosvg/issues/26
[#28]: https://github.com/brunoborges/jairosvg/issues/28
[#29]: https://github.com/brunoborges/jairosvg/issues/29
[#30]: https://github.com/brunoborges/jairosvg/issues/30

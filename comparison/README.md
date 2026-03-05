# Feature Comparison: JairoSVG vs EchoSVG vs CairoSVG vs JSVG

A comprehensive comparison of four SVG libraries — **JairoSVG** (Java), **EchoSVG** (Java), **CairoSVG** (Python), and **JSVG** (Java) — to help developers choose the right tool for their SVG rendering needs. JairoSVG is a Java port of [CairoSVG], so this comparison also tracks porting fidelity.

## Table of Contents

- [Overview](#overview)
- [Architecture & Design](#architecture--design)
- [SVG Element Support](#svg-element-support)
- [SVG Attributes & Features](#svg-attributes--features)
- [CSS & Styling](#css--styling)
- [Output Formats](#output-formats)
- [API & Developer Experience](#api--developer-experience)
- [Benchmark](#benchmark)
  - [PNG Output File Sizes](#png-output-file-sizes)
  - [Running the Benchmark](#running-the-benchmark)
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
| **Language**          | Java 25+                                                       | Java 11+                                                   | Python 3.6+                            | Java 11+                                          |
| **Origin**            | Java port of [CairoSVG]                                        | Fork of [Apache Batik]                                     | Original project                       | Independent project                               |
| **Maintainer**        | Bruno Borges                                                   | css4j project                                              | CourtBouillon / Kozea                  | Jannis Weis                                       |
| **Primary goal**      | Fast, lightweight SVG → raster/vector conversion               | Full-featured SVG toolkit: render, manipulate, and convert | SVG → PNG/PDF/PS conversion            | Lightweight SVG renderer for Swing / Java2D       |
| **License**           | LGPL-3.0                                                       | Apache-2.0                                                 | LGPL-3.0                               | MIT                                               |
| **Repository**        | [brunoborges/jairosvg]                                         | [css4j/echosvg]                                            | [Kozea/CairoSVG]                       | [weisJ/jsvg]                                      |
| **Current version**   | 1.0.2                                                          | 2.4                                                        | 2.7+                                   | 2.0.0                                             |
| **SVG spec target**   | SVG 1.1                                                        | SVG 1.1 + partial SVG 2                                    | SVG 1.1                                | SVG 1.1 + partial SVG 2                           |
| **Rendering backend** | Java2D                                                         | GVT (Batik) → Java2D                                       | Cairo (C library)                      | Java2D                                            |
| **Key strength**      | Speed (3–31× faster than EchoSVG, 1–2.6× faster than CairoSVG) | Feature completeness and standard compliance               | Native C performance, mature ecosystem | Designed for Swing GUI embedding (IntelliJ, etc.) |

---

## Architecture & Design

### JairoSVG

JairoSVG is a **direct port of CairoSVG's Python codebase** to modern Java, rendering SVG through the standard **Java2D** (`Graphics2D` / `BufferedImage`) API. The architecture is intentionally compact:

| Java Class     | Python Module | Role                       |
| -------------- | ------------- | -------------------------- |
| `JairoSVG`     | `__init__.py` | Public API + Builder       |
| `Surface`      | `surface.py`  | Java2D rendering engine    |
| `Node`         | `parser.py`   | SVG DOM tree               |
| `PathDrawer`   | `path.py`     | SVG path commands          |
| `ShapeDrawer`  | `shapes.py`   | Basic shapes               |
| `TextDrawer`   | `text.py`     | Text rendering             |
| `Defs`         | `defs.py`     | Gradients, clips, use      |
| `Colors`       | `colors.py`   | Color parsing (170+ named) |
| `Helpers`      | `helpers.py`  | Units, transforms          |
| `CssProcessor` | `css.py`      | CSS parsing                |

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

| Aspect               | JairoSVG                | EchoSVG                  | CairoSVG                                  |
| -------------------- | ----------------------- | ------------------------ | ----------------------------------------- |
| **Core rendering**   | Java2D (`Graphics2D`)   | GVT → Java2D             | Cairo (C library)                         |
| **CSS engine**       | Custom lightweight      | css4j (CSS4 support)     | tinycss2 + cssselect2                     |
| **SVG DOM**          | Read-only `Node` tree   | Full mutable W3C DOM     | ElementTree (read-only)                   |
| **Module structure** | Single JAR, ~20 classes | 20+ Gradle modules       | Single Python package                     |
| **Animation engine** | None                    | Full SMIL                | None                                      |
| **Scripting**        | None                    | Mozilla Rhino (JS)       | None                                      |
| **Filter pipeline**  | Basic                   | Full primitives          | 3 primitives (feBlend, feFlood, feOffset) |
| **Font handling**    | Java AWT fonts          | AWT + SVG fonts          | Cairo font system                         |
| **Extensibility**    | Minimal (source-level)  | High (bridges, handlers) | Minimal (source-level)                    |

---

## SVG Element Support

| SVG Element                                                   |                                JairoSVG                                 |       EchoSVG        |                            CairoSVG                             |
| ------------------------------------------------------------- | :---------------------------------------------------------------------: | :------------------: | :-------------------------------------------------------------: |
| `<svg>`, `<g>`                                                |                                   ✅                                    |          ✅          |                               ✅                                |
| `<rect>`, `<circle>`, `<ellipse>`                             |                                   ✅                                    |          ✅          |                               ✅                                |
| `<line>`, `<polyline>`, `<polygon>`                           |                                   ✅                                    |          ✅          |                               ✅                                |
| `<path>` (all commands)                                       |                                   ✅                                    |          ✅          |                               ✅                                |
| `<text>`, `<tspan>`                                           |                                   ✅                                    |          ✅          |                               ✅                                |
| `<textPath>`                                                  |                                   ✅                                    |          ✅          |                               ✅                                |
| `<image>` (raster + nested SVG)                               |                                   ✅                                    |          ✅          |                         ✅ (via Pillow)                         |
| `<use>`, `<defs>`                                             |                                   ✅                                    |          ✅          |                               ✅                                |
| `<symbol>`                                                    |                                   ✅                                    |          ✅          |                               ❌                                |
| `<linearGradient>`, `<radialGradient>`                        |                                   ✅                                    |          ✅          |                               ✅                                |
| `<pattern>`                                                   |                                   ✅                                    |          ✅          |                           ⚠️ (naive)                            |
| `<clipPath>`                                                  |                                   ✅                                    |          ✅          |                               ✅                                |
| `<mask>`                                                      |                                   ✅                                    |          ✅          |                         ⚠️ (alpha only)                         |
| `<filter>`                                                    | ✅ (`feGaussianBlur`, `feDropShadow`, `feOffset`, `feFlood`, `feMerge`) | ✅ (full primitives) | ⚠️ (`feBlend`, `feFlood`, `feOffset` only; no blur/drop-shadow) |
| `<marker>`                                                    |                                   ✅                                    |          ✅          |                           ✅ (basic)                            |
| `<metadata>`, `<title>`, `<desc>`                             |                        ✅ (parsed, not rendered)                        |          ✅          |                          ❌ (ignored)                           |
| `<foreignObject>`                                             |                               ❌ ([#15])                                |          ✅          |                               ❌                                |
| `<animate>`, `<animateTransform>`, `<animateMotion>`, `<set>` |                               ❌ ([#16])                                |      ✅ (SMIL)       |                               ❌                                |
| SVG Fonts (`<font>`, `<glyph>`)                               |                                   ✅                                    |          ✅          |                               ❌                                |
| `<script>`                                                    |                               ❌ ([#18])                                |    ✅ (Rhino JS)     |                               ❌                                |
| `<cursor>`                                                    |                               ❌ ([#19])                                |          ✅          |                               ❌                                |

---

## SVG Attributes & Features

| Feature                                                     |  JairoSVG  | EchoSVG |  CairoSVG  |
| ----------------------------------------------------------- | :--------: | :-----: | :--------: |
| `viewBox` + `preserveAspectRatio`                           |     ✅     |   ✅    |     ✅     |
| Transforms (translate, rotate, scale, skewX, skewY, matrix) |     ✅     |   ✅    |     ✅     |
| Nested `<svg>` (independent viewports)                      |     ✅     |   ✅    |     ✅     |
| Opacity (element, fill, stroke)                             |     ✅     |   ✅    |     ✅     |
| `fill-rule` (nonzero / evenodd)                             |     ✅     |   ✅    |     ✅     |
| Stroke properties (dasharray, linecap, linejoin)            |     ✅     |   ✅    |     ✅     |
| Gradient `spreadMethod` (pad / reflect / repeat)            |     ✅     |   ✅    |     ✅     |
| `gradientUnits`, `gradientTransform`                        |     ✅     |   ✅    |     ✅     |
| `patternTransform`                                          |     ✅     |   ✅    |     ❌     |
| `fill="url(#id)"` references                                |     ✅     |   ✅    |     ✅     |
| Units (px, pt, em, %, cm, mm, in)                           |     ✅     |   ✅    |     ✅     |
| `font` shorthand                                            |     ✅     |   ✅    |     ❌     |
| `font-family`, `font-size`, `font-weight`                   |     ✅     |   ✅    | ✅ (basic) |
| `letter-spacing`, `text-anchor`                             |     ✅     |   ✅    |     ✅     |
| `text-decoration`                                           |     ✅     |   ✅    |     ❌     |
| Named colors (170+)                                         |     ✅     |   ✅    |     ✅     |
| `currentColor`                                              |     ✅     |   ✅    |     ✅     |
| `rgb()` / `rgba()` / hex colors                             |     ✅     |   ✅    |     ✅     |
| `hsl()` / `hsla()`                                          |     ✅     |   ✅    |     ❌     |
| CSS Color Level 4 (`oklch`, `lab`, etc.)                    | ❌ ([#23]) |   ✅    |     ❌     |

---

## CSS & Styling

| Feature                               |                                            JairoSVG                                            |    EchoSVG     |         CairoSVG         |
| ------------------------------------- | :--------------------------------------------------------------------------------------------: | :------------: | :----------------------: |
| Inline `style` attribute              |                                               ✅                                               |       ✅       |            ✅            |
| `<style>` block (CSS stylesheet)      |                                               ✅                                               |       ✅       |            ✅            |
| External CSS via `<?xml-stylesheet?>` |                                    ✅ (requires `--unsafe`)                                    |       ✅       |        ✅ (basic)        |
| Class selectors                       |                                               ✅                                               |       ✅       |            ✅            |
| ID selectors                          |                                               ✅                                               |       ✅       |            ✅            |
| Descendant / child selectors          |                                           ✅ (basic)                                           |       ✅       |   ✅ (via cssselect2)    |
| Pseudo-classes / pseudo-elements      | ✅ (`:first-child`, `:last-child`, `:nth-child()`, `:not()`, `::first-line`, `::first-letter`) |    Partial     | Partial (via cssselect2) |
| CSS Level 4 selectors                 |                                           ❌ ([#26])                                           | ✅ (via css4j) |            ❌            |
| CSS custom properties (variables)     |                                               ✅                                               |       ✅       |            ❌            |
| CSS `calc()`                          |                                           ❌ ([#28])                                           |       ✅       |            ❌            |
| CSS nesting                           |                                               ❌                                               |       ❌       |            ❌            |
| `@import` rules                       |                                           ❌ ([#29])                                           |       ✅       |            ❌            |
| `@supports` rules                     |                                           ❌ ([#30])                                           |       ✅       |            ❌            |

EchoSVG integrates the **css4j** CSS parser, giving it significantly more advanced CSS support. CairoSVG uses **tinycss2** + **cssselect2**, providing solid basic CSS support. JairoSVG's lightweight built-in processor covers the common patterns used in SVG files.

---

## Output Formats

| Format                 |          JairoSVG          |          EchoSVG           |      CairoSVG      |
| ---------------------- | :------------------------: | :------------------------: | :----------------: |
| PNG                    |             ✅             |             ✅             |         ✅         |
| PDF                    | ✅ (via Apache PDFBox 3.0) | ✅ (via FOP or transcoder) |   ✅ (via Cairo)   |
| PostScript (PS)        |             ✅             |             ✅             |         ✅         |
| EPS                    |             ✅             |             ❌             |         ❌         |
| JPEG                   |             ✅             |             ✅             |         ❌         |
| TIFF                   |             ✅             |             ✅             |         ❌         |
| In-memory image object |    ✅ (`BufferedImage`)    |    ✅ (`BufferedImage`)    | ✅ (Cairo surface) |

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

| Feature                            |         JairoSVG         |        EchoSVG        |      CairoSVG      |
| ---------------------------------- | :----------------------: | :-------------------: | :----------------: |
| Simple static method API           |            ✅            |  ❌ (transcoder API)  |         ✅         |
| Fluent builder API                 |            ✅            |          ❌           | ❌ (keyword args)  |
| Transcoder API (Batik-style)       |            ❌            |          ✅           |         ❌         |
| Full SVG DOM (W3C DOM)             |            ❌            |          ✅           |         ❌         |
| SVG DOM manipulation at runtime    |            ❌            |          ✅           |         ❌         |
| Swing / GUI viewer component       |            ❌            |          ✅           |         ❌         |
| CLI tool                           |            ✅            |    ✅ (rasterizer)    |         ✅         |
| DPI control                        |            ✅            |          ✅           |         ✅         |
| Scale factor                       |            ✅            |          ✅           |         ✅         |
| Background color override          |            ✅            |          ✅           |         ✅         |
| Color negation                     |            ✅            |          ❌           |         ✅         |
| Output width / height override     |            ✅            |          ✅           |         ✅         |
| External file access control (XXE) | ✅ (disabled by default) |   ✅ (configurable)   | ✅ (`unsafe` flag) |
| URL input (http/https)             |            ✅            |          ✅           |         ✅         |
| JBang support                      |            ✅            |          ❌           |        N/A         |
| GraalVM Native Image compatible    |    ✅ (no reflection)    | ⚠️ (reflection-heavy) |        N/A         |

---

## Benchmark

SVG → PNG conversion benchmarks across 19 SVG test files (lower is better):

| Test Case                                    | JairoSVG (Java) | EchoSVG (Java) | CairoSVG (Python) | JairoSVG vs EchoSVG | JairoSVG vs CairoSVG |
| -------------------------------------------- | :-------------: | :------------: | :---------------: | :-----------------: | :------------------: :--------------------------------------: |
| [Basic shapes](#01--basic-shapes)            |   **3.5 ms**    |    16.6 ms     |      4.3 ms       |       4.8× ✅       |       1.2× ✅        |
| [Gradients](#02--gradients)                  |   **4.3 ms**    |    134.8 ms    |      11.3 ms      |      31.0× ✅       |       2.6× ✅        |
| [Complex paths](#03--complex-paths)          |   **4.5 ms**    |    23.1 ms     |      4.6 ms       |       5.2× ✅       |        1.0× ≈        |
| [Text rendering](#04--text-rendering)        |   **4.8 ms**    |    22.9 ms     |      6.2 ms       |       4.8× ✅       |       1.3× ✅        |
| [Transforms](#05--transforms)                |     4.1 ms      |    14.6 ms     |    **4.0 ms**     |       3.6× ✅       |        1.0× ≈        |
| [Stroke styles](#06--stroke-styles)          |     3.7 ms      |    11.9 ms     |    **3.5 ms**     |       3.2× ✅       |        1.0× ≈        |
| [Opacity blend](#07--opacity--blending)      |     3.4 ms      |    17.7 ms     |    **3.3 ms**     |       5.2× ✅       |        1.0× ≈        |
| [Viewbox aspect](#08--viewbox--aspect-ratio) |   **4.8 ms**    |    19.7 ms     |      5.3 ms       |       4.1× ✅       |       1.1× ✅        |
| [CSS styling](#09--css-styling)              |   **3.4 ms**    |    15.2 ms     |      4.1 ms       |       4.5× ✅       |       1.2× ✅        |
| [Use and defs](#10--use--defs)               |   **4.0 ms**    |    14.3 ms     |      4.4 ms       |       3.6× ✅       |       1.1× ✅        |
| [Star polygon](#11--star-polygon)            |     3.2 ms      |    14.6 ms     |    **3.1 ms**     |       4.5× ✅       |        1.0× ≈        |
| [Nested svg](#12--nested-svg)                |   **4.6 ms**    |    19.4 ms     |      5.0 ms       |       4.3× ✅       |       1.1× ✅        |
| [Patterns](#13--patterns)                    |     4.6 ms      |    16.2 ms     |    **4.6 ms**     |       3.5× ✅       |        1.0× ≈        |
| [Clip paths](#14--clip-paths)                |   **4.2 ms**    |    26.5 ms     |      6.0 ms       |       6.4× ✅       |       1.4× ✅        |
| [Masks](#15--masks) ⚠️                       |   **8.9 ms**    |    21.5 ms     |     3.7 ms ⚠️     |       2.4× ✅       |         ← ⚠️         |
| [Markers](#16--markers)                      |   **3.9 ms**    |    13.1 ms     |      4.7 ms       |       3.4× ✅       |       1.2× ✅        |
| [Filters](#17--filters) ⚠️                   |   **45.0 ms**   |   35.1 ms ⚠️   |     4.5 ms ⚠️     |        ← ⚠️         |         ← ⚠️         |
| [Embedded image](#18--embedded-images)       |   **4.3 ms**    |    16.4 ms     |      7.2 ms       |       3.8× ✅       |       1.7× ✅        |
| [Text advanced](#19--advanced-text)          |   **5.4 ms**    |    26.1 ms     |      8.9 ms       |       4.8× ✅       |       1.7× ✅        |

_JairoSVG is **3–31× faster** than EchoSVG and **1–2.6× faster** than CairoSVG in most scenarios._

> **⚠️ Filters/Masks caveat:** CairoSVG does **not** correctly render masks (missing gradient and circle content) or `feGaussianBlur`/`feDropShadow` filters — it silently skips them. EchoSVG also has partial filter support. Both appear faster on those tests because they skip rendering work. JairoSVG performs the actual computation, so its speed reflects the true cost of correct rendering.

> **Note:** Benchmarks were run with 20 warm-up iterations and 1000 measured iterations per SVG file. Results may vary by hardware and SVG complexity.

#### Default Rendering Settings: JairoSVG vs JSVG

Both JairoSVG and JSVG use Java2D as their rendering backend, but they ship with **different default quality settings**, which directly affects benchmark performance:

| Setting                       | JairoSVG default             | JSVG default (out-of-the-box)     | Performance impact |
| ----------------------------- | ---------------------------- | --------------------------------- | :----------------: |
| `KEY_ANTIALIASING`            | `VALUE_ANTIALIAS_ON`         | Not set (platform default)        |        Low         |
| `KEY_TEXT_ANTIALIASING`       | `VALUE_TEXT_ANTIALIAS_ON`    | Not set (platform default)        |        Low         |
| `KEY_RENDERING`               | `VALUE_RENDER_QUALITY`       | Not set (defaults to speed)       |      **High**      |
| `KEY_STROKE_CONTROL`          | `VALUE_STROKE_PURE`          | Not set (defaults to `NORMALIZE`) |      **High**      |
| `KEY_FRACTIONALMETRICS`       | `VALUE_FRACTIONALMETRICS_ON` | Not set (defaults to `OFF`)       |       Medium       |
| **PNG compression level**     | 6 (matches CairoSVG/libpng) | `ImageIO.write()` default         |       Medium       |

`VALUE_RENDER_QUALITY` forces Java2D to use higher-fidelity rendering pipelines, and `VALUE_STROKE_PURE` computes precise sub-pixel stroke positions instead of snapping to pixel grid. Both improve visual accuracy at the cost of speed.

**In the benchmark**, we normalize JSVG to use the same rendering hints and PNG compression level as JairoSVG, so that the comparison measures SVG engine efficiency rather than quality-setting differences.

### PNG Output File Sizes

JairoSVG produces **7.8% smaller** PNGs overall compared to CairoSVG (using the same zlib compression level 6):

| Test Case      |    JairoSVG |    CairoSVG | Difference |
| -------------- | ----------: | ----------: | ---------: |
| Basic shapes   |       6,718 |       8,920 |     −24.7% |
| Gradients      |      25,554 |      23,637 |      +8.1% |
| Complex paths  |      12,657 |      15,633 |     −19.0% |
| Text rendering |      14,872 |      16,317 |      −8.9% |
| Transforms     |       5,461 |       6,001 |      −9.0% |
| Stroke styles  |       3,363 |       4,478 |     −24.9% |
| Opacity blend  |       8,409 |       9,853 |     −14.7% |
| Viewbox aspect |      11,616 |      11,444 |      +1.5% |
| CSS styling    |       8,153 |      10,816 |     −24.6% |
| Use and defs   |       5,646 |       9,712 |     −41.9% |
| Star polygon   |       6,228 |       8,911 |     −30.1% |
| Nested svg     |      11,322 |      11,880 |      −4.7% |
| Patterns       |       9,598 |      11,095 |     −13.5% |
| Clip paths     |       9,361 |      13,552 |     −30.9% |
| Masks ⚠️       |       1,458 |       1,161 |     +25.6% |
| Markers        |       6,334 |       8,378 |     −24.4% |
| Filters ⚠️     |      31,059 |       8,520 |    +264.5% |
| Embedded image |       9,995 |      21,228 |     −52.9% |
| Text advanced  |      20,003 |      23,864 |     −16.2% |
| **Total**      | **207,807** | **225,400** |  **−7.8%** |

> **⚠️ Filters/Masks:** Where CairoSVG produces much smaller output, it is because CairoSVG **does not render** certain features correctly — filter effects (blur, drop-shadow) are silently skipped, and masks are rendered without gradient/circle content. This results in simpler images that compress better. JairoSVG renders these effects correctly, producing visually accurate but larger PNGs.

### Running the Benchmark

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

# Adjust warmup and measurement iterations (defaults: 20 and 1000)
jbang comparison/benchmark.java --warmup=5 --iterations=100
```

The benchmark loads all SVG files from `comparison/svg/` (currently 19 files). Each runs 20 warm-up iterations followed by 1000 measured iterations. Stats reported: average, median, p95, and minimum times.

---

## Dependencies & Footprint

| Metric                   | JairoSVG                                    | EchoSVG                   | CairoSVG                                                |
| ------------------------ | ------------------------------------------- | ------------------------- | ------------------------------------------------------- |
| **Runtime dependencies** | 0 (PDFBox optional)                         | Many (css4j, xml-apis, …) | 5 (cairocffi, tinycss2, cssselect2, defusedxml, Pillow) |
| **Disk footprint**       | **~130 KB** (PNG only), ~2.1 MB with PDFBox | ~5.7 MB (25 JARs)         | ~16.6 MB (Python pkgs + Pillow + Cairo C lib)           |
| **Artifact size**        | ~1 fat JAR                                  | Many modular JARs         | Single Python package                                   |
| **Source files**         | 20                                          | 20+ modules               | ~10 modules                                             |
| **Lines of code**        | ~4,100                                      | ~200,000+                 | ~4,000                                                  |
| **Platform req.**        | Java 25+ (`--enable-preview`)               | Java 11–24                | Python 3.6+ / Cairo C lib                               |
| **Build system**         | Maven                                       | Gradle                    | pip / setuptools                                        |
| **Native dependency**    | None                                        | None                      | Cairo C library required                                |

---

## Security

| Feature                                       |      JairoSVG      |       EchoSVG        |      CairoSVG       |
| --------------------------------------------- | :----------------: | :------------------: | :-----------------: |
| XXE protection by default                     |         ✅         |  ✅ (configurable)   | ✅ (via defusedxml) |
| External resource loading disabled by default |         ✅         |          ✅          |         ✅          |
| `--unsafe` flag to opt-in to external access  |         ✅         |          ✅          |         ✅          |
| Script execution                              | ❌ (not supported) | ✅ opt-in (Rhino JS) | ❌ (not supported)  |
| `SecurityManager` integration                 |         ❌         |          ✅          |         N/A         |

JairoSVG and CairoSVG share the same security posture: no scripting support (eliminating script injection), external access blocked by default. EchoSVG offers more configurability but a larger attack surface.

---

## Visual Rendering Comparison

Side-by-side visual comparison of 19 SVG test cases across all four libraries.

### 01 — Basic Shapes

Rectangles, circles, ellipses, and lines with solid fills and strokes.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/01_basic_shapes.svg) | ![JairoSVG](png/jairosvg/01_basic_shapes.png) | ![EchoSVG](png/echosvg/01_basic_shapes.png) | ![CairoSVG](png/cairosvg/01_basic_shapes.png) | ![JSVG](png/jsvg/01_basic_shapes.png) |

### 02 — Gradients

Linear and radial gradients with color stops and spread methods.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/02_gradients.svg) | ![JairoSVG](png/jairosvg/02_gradients.png) | ![EchoSVG](png/echosvg/02_gradients.png) | ![CairoSVG](png/cairosvg/02_gradients.png) | ![JSVG](png/jsvg/02_gradients.png) |

### 03 — Complex Paths

Cubic/quadratic Bézier curves, arcs, and complex path commands.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/03_complex_paths.svg) | ![JairoSVG](png/jairosvg/03_complex_paths.png) | ![EchoSVG](png/echosvg/03_complex_paths.png) | ![CairoSVG](png/cairosvg/03_complex_paths.png) | ![JSVG](png/jsvg/03_complex_paths.png) |

### 04 — Text Rendering

Text rendering with different fonts, sizes, weights, and tspan.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/04_text_rendering.svg) | ![JairoSVG](png/jairosvg/04_text_rendering.png) | ![EchoSVG](png/echosvg/04_text_rendering.png) | ![CairoSVG](png/cairosvg/04_text_rendering.png) | ![JSVG](png/jsvg/04_text_rendering.png) |

### 05 — Transforms

Translate, rotate, scale, skewX, and nested group transforms.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/05_transforms.svg) | ![JairoSVG](png/jairosvg/05_transforms.png) | ![EchoSVG](png/echosvg/05_transforms.png) | ![CairoSVG](png/cairosvg/05_transforms.png) | ![JSVG](png/jsvg/05_transforms.png) |

### 06 — Stroke Styles

Dash arrays, line caps (butt/round/square), and line joins.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/06_stroke_styles.svg) | ![JairoSVG](png/jairosvg/06_stroke_styles.png) | ![EchoSVG](png/echosvg/06_stroke_styles.png) | ![CairoSVG](png/cairosvg/06_stroke_styles.png) | ![JSVG](png/jsvg/06_stroke_styles.png) |

### 07 — Opacity & Blending

Fill opacity, stroke opacity, and layered element opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/07_opacity_blend.svg) | ![JairoSVG](png/jairosvg/07_opacity_blend.png) | ![EchoSVG](png/echosvg/07_opacity_blend.png) | ![CairoSVG](png/cairosvg/07_opacity_blend.png) | ![JSVG](png/jsvg/07_opacity_blend.png) |

### 08 — ViewBox & Aspect Ratio

viewBox scaling with different preserveAspectRatio values.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/08_viewbox_aspect.svg) | ![JairoSVG](png/jairosvg/08_viewbox_aspect.png) | ![EchoSVG](png/echosvg/08_viewbox_aspect.png) | ![CairoSVG](png/cairosvg/08_viewbox_aspect.png) | ![JSVG](png/jsvg/08_viewbox_aspect.png) |

### 09 — CSS Styling

CSS `<style>` block with class and ID selectors.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/09_css_styling.svg) | ![JairoSVG](png/jairosvg/09_css_styling.png) | ![EchoSVG](png/echosvg/09_css_styling.png) | ![CairoSVG](png/cairosvg/09_css_styling.png) | ![JSVG](png/jsvg/09_css_styling.png) |

### 10 — Use & Defs

`<use>` element references, `<clipPath>`, and `<defs>` reuse.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/10_use_and_defs.svg) | ![JairoSVG](png/jairosvg/10_use_and_defs.png) | ![EchoSVG](png/echosvg/10_use_and_defs.png) | ![CairoSVG](png/cairosvg/10_use_and_defs.png) | ![JSVG](png/jsvg/10_use_and_defs.png) |

### 11 — Star Polygon

Complex star polygon with fill-rule evenodd.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/11_star_polygon.svg) | ![JairoSVG](png/jairosvg/11_star_polygon.png) | ![EchoSVG](png/echosvg/11_star_polygon.png) | ![CairoSVG](png/cairosvg/11_star_polygon.png) | ![JSVG](png/jsvg/11_star_polygon.png) |

### 12 — Nested SVG

Nested `<svg>` elements with independent viewports.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/12_nested_svg.svg) | ![JairoSVG](png/jairosvg/12_nested_svg.png) | ![EchoSVG](png/echosvg/12_nested_svg.png) | ![CairoSVG](png/cairosvg/12_nested_svg.png) | ![JSVG](png/jsvg/12_nested_svg.png) |

### 13 — Patterns

Tiled pattern fills: dots, cross-hatch stripes, and grid lines.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/13_patterns.svg) | ![JairoSVG](png/jairosvg/13_patterns.png) | ![EchoSVG](png/echosvg/13_patterns.png) | ![CairoSVG](png/cairosvg/13_patterns.png) | ![JSVG](png/jsvg/13_patterns.png) |

### 14 — Clip Paths

Star and text clip paths applied to gradient fills.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/14_clip_paths.svg) | ![JairoSVG](png/jairosvg/14_clip_paths.png) | ![EchoSVG](png/echosvg/14_clip_paths.png) | ![CairoSVG](png/cairosvg/14_clip_paths.png) | ![JSVG](png/jsvg/14_clip_paths.png) |

### 15 — Masks

Horizontal, vertical, and circular gradient masks with luminance blending.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/15_masks.svg) | ![JairoSVG](png/jairosvg/15_masks.png) | ![EchoSVG](png/echosvg/15_masks.png) | ![CairoSVG](png/cairosvg/15_masks.png) | ![JSVG](png/jsvg/15_masks.png) |

### 16 — Markers

Arrow, dot, and square markers on lines, polylines, and curves.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/16_markers.svg) | ![JairoSVG](png/jairosvg/16_markers.png) | ![EchoSVG](png/echosvg/16_markers.png) | ![CairoSVG](png/cairosvg/16_markers.png) | ![JSVG](png/jsvg/16_markers.png) |

### 17 — Filters

Gaussian blur and drop-shadow filters on shapes and text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/17_filters.svg) | ![JairoSVG](png/jairosvg/17_filters.png) | ![EchoSVG](png/echosvg/17_filters.png) | ![CairoSVG](png/cairosvg/17_filters.png) | ![JSVG](png/jsvg/17_filters.png) |

### 18 — Embedded Images

Base64-encoded PNG images with clipping, transforms, and opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/18_embedded_image.svg) | ![JairoSVG](png/jairosvg/18_embedded_image.png) | ![EchoSVG](png/echosvg/18_embedded_image.png) | ![CairoSVG](png/cairosvg/18_embedded_image.png) | ![JSVG](png/jsvg/18_embedded_image.png) |

### 19 — Advanced Text

Multi-span text (tspan), text-decoration, textPath on curves, and rotated text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/19_text_advanced.svg) | ![JairoSVG](png/jairosvg/19_text_advanced.png) | ![EchoSVG](png/echosvg/19_text_advanced.png) | ![CairoSVG](png/cairosvg/19_text_advanced.png) | ![JSVG](png/jsvg/19_text_advanced.png) |

---

## Summary

| Dimension          | JairoSVG                                               | EchoSVG                                      | CairoSVG                                       | JSVG                                   |
| ------------------ | ------------------------------------------------------ | -------------------------------------------- | ---------------------------------------------- | -------------------------------------- |
| **Best for**       | Fast Java SVG conversion                               | Full SVG toolkit (DOM, scripting, animation) | Python SVG conversion                          | SVG rendering in Swing/Java2D GUIs     |
| **SVG spec**       | SVG 1.1 (static)                                       | SVG 1.1 + partial SVG 2                      | SVG 1.1 (static)                               | SVG 1.1 + partial SVG 2               |
| **CSS**            | Basic + structural pseudo selectors                    | Advanced (CSS Level 4, css4j)                | Basic (via tinycss2)                           | Good CSS support                       |
| **Performance**    | 3–31× faster than EchoSVG; 1–2.5× faster than CairoSVG | Slowest (GVT overhead)                       | Fast (native C), but skips some filter effects | Fast (lightweight, designed for Swing) |
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
- Compatibility with Java 11–24 (JairoSVG requires Java 25+)
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

### JairoSVG Porting Fidelity

Since JairoSVG is a port of CairoSVG, most features should be at parity. Key differences:

| Feature                           |   JairoSVG (Java port)   | CairoSVG (Python original) |
| --------------------------------- | :----------------------: | :------------------------: |
| `<symbol>` element                |            ✅            |             ❌             |
| `font` shorthand                  |            ✅            |             ❌             |
| EPS output                        |            ✅            |             ❌             |
| External CSS `<?xml-stylesheet?>` | ✅ (requires `--unsafe`) |             ✅             |
| Gzip-compressed `.svgz` input     |            ❌            |             ✅             |

JairoSVG adds features beyond CairoSVG (fluent builder API, `BufferedImage` output, EPS support) while maintaining the same core rendering approach.

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

ImageMagick is an excellent tool for raster image manipulation (resize, crop, compose, format conversion), but its SVG support is too limited for production use. For reliable SVG → PNG conversion, use a dedicated SVG library like JairoSVG, EchoSVG, or CairoSVG.

---

## Regenerating

Prerequisites: [JBang], Java 25+, Python 3 with CairoSVG (`python3 -m pip install cairosvg`), JairoSVG installed in local Maven repo.

```bash
./mvnw install -DskipTests
jbang comparison/generate.java
```

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

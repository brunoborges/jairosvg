# Gap Analysis: JairoSVG vs Apache Batik

A detailed comparison of SVG capabilities between **JairoSVG** (1.0.10) and **Apache Batik** (1.19), identifying features where JairoSVG matches, exceeds, or falls short of Batik's functionality.

## Table of Contents

- [Executive Summary](#executive-summary)
- [Project Overview](#project-overview)
- [SVG Element Support](#svg-element-support)
- [Filter Primitives](#filter-primitives)
- [Text & Fonts](#text--fonts)
- [CSS & Styling](#css--styling)
- [Gradients & Patterns](#gradients--patterns)
- [Transforms & Coordinate Systems](#transforms--coordinate-systems)
- [Output Formats](#output-formats)
- [API & Developer Experience](#api--developer-experience)
- [Tooling](#tooling)
- [Dynamic Features](#dynamic-features)
- [Security](#security)
- [Performance & Footprint](#performance--footprint)
- [Feature Gap Summary](#feature-gap-summary)
- [Actionable Recommendations](#actionable-recommendations)
- [References](#references)

---

## Executive Summary

Apache Batik is a mature, enterprise-grade SVG 1.1 toolkit with near-complete spec coverage, full DOM support, scripting (ECMAScript), and SMIL animation. JairoSVG is a lightweight, high-performance SVG 1.1 static renderer focused on conversion (SVG → PNG/PDF/PS/JPEG/TIFF).

**Key findings:**

| Dimension | JairoSVG | Apache Batik |
|-----------|----------|--------------|
| SVG 1.1 element coverage | ~90% of rendering elements | ~98% |
| Filter primitives | 16 of 17 (full SVG 1.1 + `feDropShadow`) | 17 of 17 (full SVG 1.1) |
| CSS support | CSS2 subset + custom properties + `@import` | CSS2 (no custom properties, no `calc()`) |
| Scripting | ❌ (intentional) | ✅ ECMAScript (Rhino) + Java |
| Animation (SMIL) | ❌ (intentional) | ✅ Nearly complete |
| SVG DOM (W3C) | ❌ Read-only Node tree | ✅ Full mutable DOM |
| Performance | 3–30× faster | Baseline (slowest among Java SVG libs) |
| Disk footprint | ~130 KB | ~5.7 MB (25+ JARs) |
| Security posture | XXE disabled by default, no scripting | Configurable, history of CVEs |
| Java requirement | Java 25+ | Java 8+ |

JairoSVG achieves **feature parity or superiority** in static SVG rendering, CSS variables, modern color functions, output format breadth, API ergonomics, performance, and security. Batik leads in **dynamic SVG features** (scripting, animation, DOM manipulation, event handling) and a small number of SVG elements related to interactivity.

---

## Project Overview

|                       | JairoSVG | Apache Batik |
|-----------------------|----------|--------------|
| **Latest version**    | 1.0.10 | 1.19 (May 2025) |
| **License**           | MIT | Apache-2.0 |
| **Language**          | Java 25+ | Java 8+ |
| **SVG spec target**   | SVG 1.1 + selective SVG 2 alignment | SVG 1.1 (94% W3C test suite conformance) |
| **Architecture**      | Direct Java2D rendering (~31 classes) | GVT scene graph → Java2D (~20+ modules) |
| **Primary use case**  | SVG → raster/vector conversion | Full SVG toolkit (render, manipulate, script, animate) |
| **Lines of code**     | ~8,800 | ~200,000+ |
| **Repository**        | [brunoborges/jairosvg](https://github.com/brunoborges/jairosvg) | [apache/xmlgraphics-batik](https://github.com/apache/xmlgraphics-batik) |

---

## SVG Element Support

### Structural & Container Elements

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<svg>` | ✅ | ✅ | — |
| `<g>` | ✅ | ✅ | — |
| `<defs>` | ✅ | ✅ | — |
| `<symbol>` | ✅ | ✅ | — |
| `<use>` | ✅ | ✅ | — |
| `<title>` | ✅ (parsed, not rendered) | ✅ | — |
| `<desc>` | ✅ (parsed, not rendered) | ✅ | — |
| `<metadata>` | ✅ (parsed, not rendered) | ✅ | — |

### Shape Elements

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<rect>` | ✅ | ✅ | — |
| `<circle>` | ✅ | ✅ | — |
| `<ellipse>` | ✅ | ✅ | — |
| `<line>` | ✅ | ✅ | — |
| `<polyline>` | ✅ | ✅ | — |
| `<polygon>` | ✅ | ✅ | — |
| `<path>` (all commands: M, L, C, S, Q, T, A, H, V, Z) | ✅ | ✅ | — |

### Text Elements

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<text>` | ✅ | ✅ | — |
| `<tspan>` | ✅ | ✅ | — |
| `<textPath>` | ✅ | ✅ | — |
| `<altGlyph>` | ❌ | ✅ | 🔴 Batik leads |
| `<altGlyphDef>` | ❌ | ✅ | 🔴 Batik leads |
| `<altGlyphItem>` | ❌ | ✅ | 🔴 Batik leads |
| `<glyphRef>` | ❌ | ✅ | 🔴 Batik leads |
| `<tref>` | ❌ | ✅ | 🔴 Batik leads |

> **Note:** `altGlyph*`, `glyphRef`, and `tref` are deprecated in SVG 2 and rarely used in real-world SVGs. These are low-priority gaps.

### Gradient & Pattern Elements

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<linearGradient>` | ✅ | ✅ | — |
| `<radialGradient>` | ✅ | ✅ | — |
| `<stop>` | ✅ | ✅ | — |
| `<pattern>` | ✅ | ✅ | — |

### Clip, Mask & Compositing

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<clipPath>` | ✅ | ✅ | — |
| `<mask>` | ✅ (luminance-to-alpha) | ✅ | — |

### Marker Elements

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<marker>` | ✅ (incl. `orient="auto"`) | ✅ | — |

### Image Elements

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<image>` (raster + nested SVG) | ✅ | ✅ | — |

### Font Elements

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<font>` | ✅ | ✅ | — |
| `<font-face>` | ✅ | ✅ | — |
| `<glyph>` | ✅ | ✅ | — |
| `<missing-glyph>` | ✅ | ✅ | — |
| `<hkern>` | ❌ | ✅ | 🔴 Batik leads |
| `<vkern>` | ❌ | ✅ | 🔴 Batik leads |
| `<font-face-src>` | ❌ | ✅ | 🔴 Batik leads |
| `<font-face-uri>` | ❌ | ✅ | 🔴 Batik leads |
| `<font-face-format>` | ❌ | ✅ | 🔴 Batik leads |
| `<font-face-name>` | ❌ | ✅ | 🔴 Batik leads |

> **Note:** SVG font elements are deprecated in SVG 2. `hkern`/`vkern` provide kerning data within SVG fonts; `font-face-*` elements are metadata for font sources. These are low-priority gaps.

### Conditional Processing

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<switch>` | ✅ | ✅ | — |
| `requiredFeatures` | ✅ (ignored per SVG 2) | ✅ (evaluated per SVG 1.1) | JairoSVG follows SVG 2 / browser behavior |
| `systemLanguage` | ✅ | ✅ | — |
| `requiredExtensions` | ✅ | ✅ | — |

### Linking

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<a>` | ✅ (rendered as text) | ✅ (interactive links) | 🟡 Batik has interactivity |

### Interactivity & Scripting Elements

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<script>` | ❌ (intentional, security) | ✅ (ECMAScript via Rhino + Java) | 🔴 Batik leads (by design) |
| `<foreignObject>` | ❌ (parsed, not rendered) | ⚠️ Partial | 🟡 Batik leads (limited) |
| `<cursor>` | ✅ (parsed, not rendered) | ✅ | — |

### Animation Elements (SMIL)

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<animate>` | ❌ (intentional) | ✅ | 🔴 Batik leads (by design) |
| `<animateTransform>` | ❌ (intentional) | ✅ | 🔴 Batik leads (by design) |
| `<animateMotion>` | ❌ (intentional) | ✅ | 🔴 Batik leads (by design) |
| `<animateColor>` | ❌ | ✅ | 🔴 Batik leads (by design) |
| `<set>` | ❌ (intentional) | ✅ | 🔴 Batik leads (by design) |

### Color Profile

| Element | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `<color-profile>` | ❌ | ⚠️ Partial | 🟡 Batik leads |

---

## Filter Primitives

| Filter Primitive | JairoSVG | Batik | Gap |
|------------------|:--------:|:-----:|:---:|
| `feGaussianBlur` | ✅ | ✅ | — |
| `feOffset` | ✅ | ✅ | — |
| `feFlood` | ✅ | ✅ | — |
| `feBlend` (normal, multiply, screen, darken, lighten) | ✅ | ✅ | — |
| `feComposite` (over, in, out, atop, xor, arithmetic) | ✅ | ✅ | — |
| `feMerge` / `feMergeNode` | ✅ | ✅ | — |
| `feImage` | ✅ | ✅ | — |
| `feTile` | ✅ | ✅ | — |
| `feColorMatrix` (matrix, saturate, hueRotate, luminanceToAlpha) | ✅ | ✅ | — |
| `feComponentTransfer` (identity, linear, gamma, table, discrete) | ✅ | ✅ | — |
| `feMorphology` (erode, dilate) | ✅ | ✅ | — |
| `feConvolveMatrix` | ✅ | ✅ | — |
| `feTurbulence` (turbulence, fractalNoise) | ✅ | ✅ | — |
| `feDisplacementMap` | ✅ | ✅ | — |
| `feDiffuseLighting` (distant, point, spot lights) | ✅ | ✅ | — |
| `feSpecularLighting` (distant, point, spot lights) | ✅ | ✅ | — |
| `feDropShadow` (SVG 2 / Filter Effects L1) | ✅ | ❌ | 🟢 **JairoSVG leads** |
| **Coverage** | **17/17** (16 SVG 1.1 + 1 SVG 2) | **16/17** (SVG 1.1 only) | JairoSVG has broader filter support |

### Filter Attributes

| Feature | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `filterUnits` (userSpaceOnUse / objectBoundingBox) | ✅ | ✅ | — |
| `primitiveUnits` | ✅ | ✅ | — |
| Filter sub-region optimization | ✅ | ✅ | — |
| `in` / `in2` named inputs (SourceGraphic, SourceAlpha, BackgroundImage, etc.) | ✅ | ✅ | — |
| `result` named outputs | ✅ | ✅ | — |

---

## Text & Fonts

| Feature | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `font-family` | ✅ | ✅ | — |
| `font-size` | ✅ | ✅ | — |
| `font-style` (normal, italic, oblique) | ✅ | ✅ | — |
| `font-weight` (100–900, normal, bold) | ✅ | ✅ | — |
| `font-variant` | ✅ | ✅ | — |
| `font` shorthand | ✅ | ✅ | — |
| `text-anchor` (start, middle, end) | ✅ | ✅ | — |
| `text-decoration` | ✅ | ✅ | — |
| `letter-spacing` | ✅ | ✅ | — |
| `word-spacing` | ⚠️ Limited | ✅ | 🟡 Batik leads |
| `dominant-baseline` | ⚠️ Limited | ✅ | 🟡 Batik leads |
| `alignment-baseline` | ⚠️ Limited | ✅ | 🟡 Batik leads |
| `baseline-shift` | ⚠️ Limited | ✅ | 🟡 Batik leads |
| `writing-mode` (lr, rl, tb) | ❌ | ✅ | 🔴 Batik leads |
| `glyph-orientation-horizontal` | ❌ | ✅ | 🔴 Batik leads |
| `glyph-orientation-vertical` | ❌ | ✅ | 🔴 Batik leads |
| `direction` (ltr, rtl) | ❌ | ✅ | 🔴 Batik leads |
| `unicode-bidi` | ❌ | ✅ | 🔴 Batik leads |
| `kerning` attribute | ❌ | ✅ | 🔴 Batik leads |
| SVG fonts (`<font>`, `<glyph>`) | ✅ | ✅ | — |
| System font fallback for undefined glyphs | ✅ | ✅ | — |
| TrueType font embedding | ❌ | ✅ (via converter tool) | 🔴 Batik leads |

---

## CSS & Styling

| Feature | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| Inline `style` attribute | ✅ | ✅ | — |
| `<style>` block (CSS stylesheets) | ✅ | ✅ | — |
| External CSS via `<?xml-stylesheet?>` | ✅ (requires `--unsafe`) | ✅ | — |
| Type selectors | ✅ | ✅ | — |
| Class selectors | ✅ | ✅ | — |
| ID selectors | ✅ | ✅ | — |
| Descendant combinators | ✅ (basic) | ✅ | — |
| `:first-child` | ✅ | ❌ | 🟢 **JairoSVG leads** |
| `:last-child` | ✅ | ❌ | 🟢 **JairoSVG leads** |
| `:nth-child()` | ✅ | ❌ | 🟢 **JairoSVG leads** |
| `:not()` | ✅ | ❌ | 🟢 **JairoSVG leads** |
| `::first-line`, `::first-letter` | ✅ | ❌ | 🟢 **JairoSVG leads** |
| CSS custom properties (`var()`) | ✅ | ❌ | 🟢 **JairoSVG leads** |
| `@import` rules | ✅ | ✅ | — |
| `currentColor` | ✅ | ✅ | — |
| `inherit` | ✅ | ✅ | — |
| `!important` | ✅ | ✅ | — |
| `calc()` | ❌ | ❌ | — |
| CSS nesting | ❌ | ❌ | — |
| CSS Level 4 selectors | ❌ | ❌ | — |
| `@media` queries | ❌ | ⚠️ Limited | 🟡 Batik leads |
| `@font-face` (CSS fonts) | ❌ | ❌ | — |

---

## Gradients & Patterns

| Feature | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| Linear gradients | ✅ | ✅ | — |
| Radial gradients | ✅ | ✅ | — |
| `spreadMethod` (pad, reflect, repeat) | ✅ | ✅ | — |
| `gradientUnits` (objectBoundingBox, userSpaceOnUse) | ✅ | ✅ | — |
| `gradientTransform` | ✅ | ✅ | — |
| Href chaining (gradient inheritance) | ✅ | ✅ | — |
| `fr` attribute (focal radius, SVG 2) | ❌ | ❌ | — |
| Pattern rendering | ✅ | ✅ | — |
| `patternUnits` | ✅ | ✅ | — |
| `patternContentUnits` | ✅ | ✅ | — |
| `patternTransform` | ✅ | ✅ | — |

---

## Transforms & Coordinate Systems

| Feature | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| `translate(tx, ty)` | ✅ | ✅ | — |
| `rotate(angle)` / `rotate(angle, cx, cy)` | ✅ | ✅ | — |
| `scale(sx, sy)` | ✅ | ✅ | — |
| `skewX(angle)` | ✅ | ✅ | — |
| `skewY(angle)` | ✅ | ✅ | — |
| `matrix(a, b, c, d, e, f)` | ✅ | ✅ | — |
| `viewBox` | ✅ | ✅ | — |
| `preserveAspectRatio` (all 9 align values + meet/slice) | ✅ | ✅ | — |
| Nested `<svg>` with independent viewports | ✅ | ✅ | — |
| `transform-origin` | ✅ | ❌ | 🟢 **JairoSVG leads** |

---

## Output Formats

| Format | JairoSVG | Batik | Gap |
|--------|:--------:|:-----:|:---:|
| PNG | ✅ | ✅ | — |
| JPEG | ✅ | ✅ | — |
| TIFF | ✅ | ✅ | — |
| PDF | ✅ (via Apache PDFBox) | ✅ (via Apache FOP) | — |
| PostScript (PS) | ✅ | ✅ (via Apache FOP) | — |
| EPS | ✅ | ❌ | 🟢 **JairoSVG leads** |
| In-memory `BufferedImage` | ✅ | ✅ | — |
| SVG generation (Java2D → SVG) | ❌ | ✅ (`SVGGraphics2D`) | 🔴 Batik leads |
| WMF → SVG conversion | ❌ | ✅ | 🔴 Batik leads |

---

## API & Developer Experience

| Feature | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| Static one-liner API | ✅ (`JairoSVG.svg2png()`) | ❌ (Transcoder API) | 🟢 **JairoSVG leads** |
| Fluent builder API | ✅ (`JairoSVG.builder()...`) | ❌ | 🟢 **JairoSVG leads** |
| Transcoder API (Batik-style) | ❌ | ✅ | 🔴 Batik leads |
| Full SVG DOM (W3C DOM) | ❌ | ✅ | 🔴 Batik leads |
| DOM manipulation at runtime | ❌ | ✅ | 🔴 Batik leads |
| DPI control | ✅ | ✅ | — |
| Scale factor | ✅ | ✅ | — |
| Background color override | ✅ | ✅ | — |
| Output width/height override | ✅ | ✅ | — |
| Color negation | ✅ | ❌ | 🟢 **JairoSVG leads** |
| Image color inversion | ✅ | ❌ | 🟢 **JairoSVG leads** |
| PNG compression level control | ✅ | ❌ | 🟢 **JairoSVG leads** |
| JPEG quality control | ✅ | ✅ | — |
| Custom rendering hints | ✅ | ✅ | — |
| Stream input/output | ✅ | ✅ | — |
| URL input (http/https) | ✅ | ✅ | — |
| External file access control | ✅ (disabled by default) | ✅ (configurable) | — |
| JBang support | ✅ | ❌ | 🟢 **JairoSVG leads** |
| GraalVM Native Image compatible | ✅ | ❌ (reflection-heavy) | 🟢 **JairoSVG leads** |

---

## Tooling

| Tool | JairoSVG | Batik | Gap |
|------|:--------:|:-----:|:---:|
| CLI converter | ✅ | ✅ (SVG Rasterizer) | — |
| GUI SVG viewer | ❌ | ✅ (Squiggle browser) | 🔴 Batik leads |
| SVG generator (Java2D → SVG) | ❌ | ✅ (`SVGGraphics2D`) | 🔴 Batik leads |
| TrueType → SVG font converter | ❌ | ✅ | 🔴 Batik leads |
| SVG pretty printer | ❌ | ✅ | 🔴 Batik leads |
| Swing `JSVGCanvas` component | ❌ | ✅ | 🔴 Batik leads |

---

## Dynamic Features

These are features that Batik supports due to its full SVG toolkit nature, which are intentionally out of scope for JairoSVG's static rendering model:

| Feature | JairoSVG | Batik | Notes |
|---------|:--------:|:-----:|:------|
| SMIL animation engine | ❌ | ✅ | JairoSVG is a static renderer |
| ECMAScript scripting (Rhino) | ❌ | ✅ | JairoSVG omits scripting for security |
| Java scripting integration | ❌ | ✅ | Requires scripting engine |
| Event handling (onclick, onload, etc.) | ❌ | ✅ | Requires interactivity |
| Zoom / pan / rotate (interactive) | ❌ | ✅ | Requires GUI |
| SVG DOM (live, mutable W3C DOM) | ❌ | ✅ | JairoSVG uses read-only Node tree |
| `pointer-events` property | ❌ | ✅ | Requires interactivity |
| CSS transitions / animations | ❌ | ❌ | Neither supports this |

> These are **intentional architectural differences**, not gaps to be closed. JairoSVG's static rendering focus is a deliberate design choice that enables its performance and security advantages.

---

## Security

| Feature | JairoSVG | Batik | Gap |
|---------|:--------:|:-----:|:---:|
| XXE protection by default | ✅ (disabled by default) | ⚠️ (configurable, not disabled by default) | 🟢 **JairoSVG leads** |
| External resource loading disabled by default | ✅ | ⚠️ (configurable) | 🟢 **JairoSVG leads** |
| `--unsafe` flag to opt-in to external access | ✅ | ✅ | — |
| No scripting surface | ✅ (no JS engine) | ❌ (Rhino JS engine included) | 🟢 **JairoSVG leads** |
| Known CVE history | None | Multiple (CVE-2015-0250, CVE-2017-5662, CVE-2022-44729, CVE-2022-44730, CVE-2022-42890, CVE-2022-41704, CVE-2022-38398, CVE-2022-38648, CVE-2022-40146, CVE-2020-11987) | 🟢 **JairoSVG leads** |
| SSRF vulnerability history | None | Multiple | 🟢 **JairoSVG leads** |
| Attack surface | Minimal (XML parser only) | Large (DOM, scripting, network, extensibility) | 🟢 **JairoSVG leads** |
| `SecurityManager` integration | ❌ | ✅ (deprecated in Java 17+) | N/A |

---

## Performance & Footprint

| Metric | JairoSVG | Batik | Gap |
|--------|:--------:|:-----:|:---:|
| Rendering speed | 3–30× faster | Baseline | 🟢 **JairoSVG leads** |
| Startup time | Fast (direct rendering) | Slow (GVT scene graph construction) | 🟢 **JairoSVG leads** |
| Memory usage | Low (no DOM tree) | High (full DOM + GVT scene graph) | 🟢 **JairoSVG leads** |
| Disk footprint | ~130 KB (1 JAR) | ~5.7 MB (25+ JARs) | 🟢 **JairoSVG leads** |
| Runtime dependencies | 0 (PDFBox optional) | Many (batik-*, xmlgraphics-commons, etc.) | 🟢 **JairoSVG leads** |
| GraalVM Native Image | ✅ | ❌ (reflection-heavy) | 🟢 **JairoSVG leads** |
| Output PNG size | Smallest (9% smaller than JSVG, 17.5% smaller than Batik/EchoSVG) | Larger | 🟢 **JairoSVG leads** |

### Benchmark Highlights (SVG → PNG, lower is better)

| Test Case | JairoSVG | EchoSVG (Batik fork) | Speedup |
|-----------|:--------:|:--------------------:|:-------:|
| Simple shapes | 4.3 ms | 20.9 ms | **4.9×** |
| Gradients | 5.8 ms | 169.5 ms | **29.2×** |
| Filters | 9.8 ms | 45.3 ms | **4.6×** |
| Fe blend modes | 13.6 ms | 37.8 ms | **2.8×** |
| Embedded image | 5.8 ms | 22.3 ms | **3.8×** |
| Localized masks | 19.4 ms | 66.3 ms | **3.4×** |

> **Note:** EchoSVG is a maintained fork of Apache Batik with the same core rendering engine, so these benchmarks are representative of Batik's performance characteristics.

---

## Feature Gap Summary

### Features Where JairoSVG Leads

| Category | Feature | Impact |
|----------|---------|--------|
| Filters | `feDropShadow` (SVG 2) | Common in modern SVGs |
| CSS | `var()` custom properties | Widely used in design tool exports |
| CSS | Structural pseudo-classes (`:first-child`, `:last-child`, `:nth-child()`, `:not()`) | Used in SVG stylesheets |
| CSS | `::first-line`, `::first-letter` pseudo-elements | Niche but supported |
| Colors | `hsl()` / `hsla()` | Modern color syntax |
| Transforms | `transform-origin` | Common in modern SVGs |
| Output | EPS format | Professional publishing |
| API | One-liner + fluent builder | Developer productivity |
| API | Color negation, PNG compression control | Conversion flexibility |
| API | GraalVM Native Image support | Cloud-native deployment |
| Security | XXE disabled by default, no scripting | Secure by default |
| Performance | 3–30× faster, 44× smaller footprint | Server-side throughput |

### Features Where Batik Leads

#### High-Priority Gaps (affect real-world SVGs)

| Category | Feature | Frequency in Real SVGs | Recommendation |
|----------|---------|:----------------------:|----------------|
| Text | `writing-mode` (vertical text) | Medium | Consider implementing |
| Text | `word-spacing` | Medium | Consider implementing |
| Text | `dominant-baseline` / `alignment-baseline` | Medium | Consider improving |
| Text | `baseline-shift` | Medium | Consider improving |
| Text | BiDi (`direction`, `unicode-bidi`) | Low–Medium | Consider for i18n |

#### Low-Priority Gaps (deprecated, rare, or niche)

| Category | Feature | Notes |
|----------|---------|-------|
| Text | `altGlyph*`, `glyphRef`, `tref` | Deprecated in SVG 2 |
| Text | `glyph-orientation-*` | Deprecated in SVG 2 |
| Text | `kerning` attribute | Deprecated in SVG 2; use `font-kerning` |
| Fonts | `hkern`, `vkern` | SVG font kerning (deprecated in SVG 2) |
| Fonts | `font-face-*` sub-elements | SVG font metadata (deprecated in SVG 2) |
| Color | `<color-profile>` | Rarely used |
| CSS | `@media` queries | Limited relevance for static rendering |

#### Intentionally Out-of-Scope Gaps

| Category | Feature | Reason |
|----------|---------|--------|
| Scripting | ECMAScript / Java scripting | Security policy; no JS engine |
| Animation | SMIL (`<animate>`, `<set>`, etc.) | Static renderer only |
| DOM | Full mutable W3C SVG DOM | No live DOM needed |
| Interactivity | Event handling, pointer-events | No interactivity |
| Tooling | GUI viewer (Squiggle) | Conversion-focused, not viewing |
| Tooling | SVGGraphics2D (Java2D → SVG) | SVG generation is out of scope |
| Tooling | TrueType → SVG font converter | Niche utility |

---

## Actionable Recommendations

Based on this gap analysis, the following improvements would bring JairoSVG closer to Batik's rendering coverage for real-world SVGs, while maintaining its performance and simplicity advantages:

### Priority 1 — Text Rendering Improvements

1. **`writing-mode`** — Add vertical text support (`tb`, `tb-rl`). Required for CJK (Chinese, Japanese, Korean) SVGs and some specialized layouts.
2. **`word-spacing`** — Complete implementation of inter-word spacing.
3. **`dominant-baseline` / `alignment-baseline`** — Improve baseline alignment accuracy. Common in text-heavy SVGs from design tools.
4. **`baseline-shift`** — Support `super`, `sub`, and length/percentage values.

### Priority 2 — Internationalization

5. **`direction` + `unicode-bidi`** — Enable right-to-left text for Arabic/Hebrew SVGs.

### Priority 3 — Nice-to-Have

6. **`kerning` attribute** — Though deprecated in SVG 2, may appear in older SVGs.

### Not Recommended

- **Scripting, animation, DOM, interactivity** — These are fundamental architectural differences, not gaps. Adding them would compromise JairoSVG's performance, security, and simplicity.
- **Deprecated SVG font sub-elements** — Low ROI; SVG fonts themselves are deprecated in SVG 2.
- **GUI viewer / SVGGraphics2D** — Outside JairoSVG's scope as a conversion library.

---

## References

- [Apache Batik Homepage](https://xmlgraphics.apache.org/batik/)
- [Batik Implementation Status](https://xmlgraphics.apache.org/batik/status.html)
- [Batik GitHub Repository](https://github.com/apache/xmlgraphics-batik)
- [SVG 1.1 Specification (W3C)](https://www.w3.org/TR/SVG11/)
- [SVG 2 Specification (W3C)](https://www.w3.org/TR/SVG2/)
- [JairoSVG Comparison with EchoSVG, CairoSVG, JSVG](README.md)
- [JairoSVG Limitations](../LIMITATIONS.md)
- [JairoSVG SVG 2 Plan](../SVG2_PLAN.md)
- [Apache XML Graphics Security Page](https://xmlgraphics.apache.org/security.html)

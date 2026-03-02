# Feature Comparison: JairoSVG vs EchoSVG

This document provides a comprehensive feature comparison between **JairoSVG** and **EchoSVG**, two Java-based SVG processing libraries.

---

## Overview

| | JairoSVG | EchoSVG |
|---|---|---|
| **Origin** | Java port of [CairoSVG](https://cairosvg.org) (Python) | Fork of [Apache Batik](https://xmlgraphics.apache.org/batik/) (Java) |
| **Primary goal** | Fast, lightweight SVG → raster/vector conversion | Full-featured SVG toolkit: render, manipulate, and convert |
| **License** | LGPL v3 | Apache 2.0 |
| **Minimum Java** | Java 25 (uses preview features) | Java 8 (Java 11+ for scripting) |
| **SVG spec target** | SVG 1.1 | SVG 1.1 + partial SVG 2 |
| **External dependencies** | Apache PDFBox (PDF output only) | css4j, xml-apis, and numerous transitive dependencies |
| **Artifact size** | ~1 fat JAR (~few MB incl. PDFBox) | Many modular JARs (each ~100 KB – 1 MB) |
| **Key strength** | Speed (2–5× faster than EchoSVG) | Feature completeness and standard compliance |

---

## SVG Element Support

| SVG Element | JairoSVG | EchoSVG |
|---|:---:|:---:|
| `<svg>`, `<g>` | ✅ | ✅ |
| `<rect>`, `<circle>`, `<ellipse>` | ✅ | ✅ |
| `<line>`, `<polyline>`, `<polygon>` | ✅ | ✅ |
| `<path>` (all commands) | ✅ | ✅ |
| `<text>`, `<tspan>` | ✅ | ✅ |
| `<textPath>` | ❌ | ✅ |
| `<image>` (raster + nested SVG) | ✅ | ✅ |
| `<use>`, `<defs>`, `<symbol>` | ✅ (`<use>` / `<defs>`) | ✅ |
| `<linearGradient>`, `<radialGradient>` | ✅ | ✅ |
| `<pattern>` | ❌ | ✅ |
| `<clipPath>` | ✅ | ✅ |
| `<mask>` | ❌ | ✅ |
| `<filter>` and filter primitives | ❌ | ✅ |
| `<marker>` | ✅ (feature-flag only) | ✅ |
| `<foreignObject>` | ❌ | ✅ |
| `<animate>`, `<animateTransform>` | ❌ | ✅ (SMIL) |
| `<animateMotion>` | ❌ | ✅ (SMIL) |
| `<set>` | ❌ | ✅ (SMIL) |
| `<script>` | ❌ | ✅ (Rhino JS engine) |
| `<metadata>`, `<title>`, `<desc>` | ✅ (parsed, not rendered) | ✅ |

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
| Pseudo-classes / pseudo-elements | ❌ | ✅ |
| CSS Level 4 selectors | ❌ | ✅ (via css4j) |
| CSS custom properties (variables) | ❌ | ✅ |
| CSS `calc()` | ❌ | ✅ |
| CSS nesting | ❌ | ✅ |
| `@supports` rules | ❌ | ✅ |
| `@import` (conditional) | ❌ | ✅ |
| `color-scheme` / modern color syntax | ❌ | ✅ |
| `fill`, `stroke`, `opacity` | ✅ | ✅ |
| `fill-rule` (nonzero / evenodd) | ✅ | ✅ |
| `stroke-dasharray`, `linecap`, `linejoin` | ✅ | ✅ |
| `font-family`, `font-size`, `font-weight` | ✅ | ✅ |
| `letter-spacing`, `text-anchor` | ✅ | ✅ |
| `text-decoration` | ❌ | ✅ |

---

## Transforms & Geometry

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| `translate`, `rotate`, `scale` | ✅ | ✅ |
| `skewX`, `skewY` | ✅ | ✅ |
| `matrix(…)` | ✅ | ✅ |
| `viewBox` | ✅ | ✅ |
| `preserveAspectRatio` | ✅ | ✅ |
| Nested `<svg>` (independent viewports) | ✅ | ✅ |
| `gradientTransform` | ✅ | ✅ |
| `patternTransform` | ❌ | ✅ |

---

## Gradients & Paint

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| Linear gradients with `stop` | ✅ | ✅ |
| Radial gradients with `stop` | ✅ | ✅ |
| `spreadMethod` (pad / reflect / repeat) | ✅ | ✅ |
| `gradientUnits` | ✅ | ✅ |
| `<pattern>` fill | ❌ | ✅ |
| `fill="url(#id)"` references | ✅ | ✅ |
| Named colors (170+) | ✅ | ✅ |
| `currentColor` | ✅ | ✅ |
| `rgba()`, `hsla()`, `hsl()` | ✅ | ✅ |
| CSS Color Level 4 (`oklch`, `lab`, etc.) | ❌ | ✅ |

---

## Output Formats

| Format | JairoSVG | EchoSVG |
|---|:---:|:---:|
| PNG | ✅ | ✅ |
| PDF | ✅ (via Apache PDFBox) | ✅ (via FOP or transcoder) |
| PostScript / EPS | ✅ | ✅ |
| SVG (re-render / passthrough) | ✅ | ✅ |
| TIFF | ❌ | ✅ |
| JPEG | ❌ | ✅ |
| `BufferedImage` (in-memory Java object) | ✅ | ✅ |

---

## API & Integration

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

## Security

| Feature | JairoSVG | EchoSVG |
|---|:---:|:---:|
| XXE protection by default | ✅ | ✅ (configurable) |
| External resource loading disabled by default | ✅ | ✅ |
| `--unsafe` flag to opt-in to external access | ✅ | ✅ |
| Script execution | ❌ (not supported) | ✅ opt-in (Rhino JS) |

---

## Performance

Benchmark results for SVG → PNG conversion (lower is better):

| Test Case | JairoSVG | EchoSVG | Ratio |
|---|:---:|:---:|:---:|
| Simple shapes | **3.7 ms** | 9.5 ms | 2.6× faster |
| Gradients + transforms | **6.7 ms** | 34.5 ms | 5.1× faster |
| Complex paths + text | **8.1 ms** | 29.7 ms | 3.7× faster |

*JairoSVG is 2–5× faster than EchoSVG for common conversion workloads.*
*Benchmark run via `jbang benchmark.java` — see [benchmark.java](benchmark.java) for details.*

---

## Architecture & Extensibility

| Aspect | JairoSVG | EchoSVG |
|---|---|---|
| **Core rendering** | Java2D (`Graphics2D`, `BufferedImage`) | Java2D via GVT (Graphics Vector Tree) |
| **CSS engine** | Custom lightweight CSS parser | css4j (full CSS4 support) |
| **SVG DOM** | Read-only internal `Node` tree | Full mutable W3C SVG DOM |
| **Module structure** | Single JAR, ~10 focused classes | Multi-module Maven project (20+ modules) |
| **Animation engine** | None | Full SMIL animation engine |
| **Scripting** | None | Mozilla Rhino (JavaScript) |
| **Filter pipeline** | None | Full SVG filter primitive chain |
| **Font handling** | Java AWT font system | Java AWT font system + SVG font support |
| **Extensibility** | Minimal (source-level) | High (custom elements, handlers, bridges) |
| **Test infrastructure** | JUnit 5 unit tests | Reference-image regression suite |

---

## Use Case Recommendations

| Use Case | Recommended Library |
|---|---|
| Fast SVG → PNG/PDF batch conversion | **JairoSVG** |
| Server-side thumbnail generation | **JairoSVG** |
| Embedding SVG in a Java 25+ app | **JairoSVG** |
| Programmatic SVG DOM manipulation | **EchoSVG** |
| SVG with SMIL animations | **EchoSVG** |
| SVG with `<filter>` effects | **EchoSVG** |
| Advanced CSS (variables, calc, nesting) | **EchoSVG** |
| SVG viewer / Swing component | **EchoSVG** |
| Migrating from Apache Batik | **EchoSVG** |
| Minimal dependency footprint | **JairoSVG** |
| GraalVM Native Image builds | **JairoSVG** |

---

## Summary

**JairoSVG** is purpose-built for speed and simplicity. It provides a clean, modern Java API for converting SVG 1.1 files to PNG, PDF, PostScript, or SVG output with minimal dependencies. Its focus on a direct port of CairoSVG's rendering logic using Java2D delivers benchmark results 2–5× faster than EchoSVG for typical conversion workloads.

**EchoSVG** is a comprehensive SVG toolkit — a modernised fork of Apache Batik — offering full SVG DOM manipulation, SMIL animation, SVG filter effects, an embedded JavaScript engine, and advanced CSS support via css4j. It is the right choice when SVG feature completeness and spec conformance take priority over raw conversion speed.

Both libraries are complementary: JairoSVG excels as a fast conversion engine, while EchoSVG is better suited as a full SVG runtime.

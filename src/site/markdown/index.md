# JairoSVG

**A high-performance SVG 1.1 to PNG, JPEG, TIFF, PDF, and PS/EPS converter powered by Java2D.**

## Why JairoSVG?

- 🎨 **Pure Java** — No native dependencies, runs anywhere Java runs
- ⚡ **Fast** — 3–29x faster than EchoSVG, on par with JSVG, 1–2.6x faster than CairoSVG's native C backend
- 📄 **Multiple formats** — PNG, JPEG, TIFF, PDF, PostScript/EPS output
- 🛡️ **Secure** — XXE protection enabled by default
- 🧰 **Flexible** — Static methods, fluent builder API, and CLI
- 🎭 **Rich SVG support** — Gradients, filters, masks, patterns, clip paths, markers, text, and more

## Quick Install

### Maven

```xml
<dependency>
    <groupId>io.brunoborges</groupId>
    <artifactId>jairosvg</artifactId>
    <version>1.0.4</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.brunoborges:jairosvg:1.0.4'
```

## Quick Example

```java
import io.brunoborges.jairosvg.JairoSVG;

// One-liner SVG to PNG
byte[] png = JairoSVG.svg2png(svgBytes);

// Builder with options
byte[] scaled = JairoSVG.builder()
    .fromBytes(svgBytes)
    .scale(2)
    .backgroundColor("#ffffff")
    .toPng();
```

## Getting Started

Head over to the [Getting Started](getting-started.html) guide for installation and first steps.

# JairoSVG

**A high-performance Java port of [CairoSVG](https://cairosvg.org) — SVG 1.1 to PNG, JPEG, TIFF, PDF, PS/EPS, and SVG converter powered by Java2D.**

## Why JairoSVG?

- 🎨 **Pure Java** — No native dependencies, runs anywhere Java runs
- ⚡ **Fast** — 2-5x faster than EchoSVG, competitive with CairoSVG's C backend
- 📄 **Multiple formats** — PNG, JPEG, TIFF, PDF, PostScript/EPS, SVG output
- 🛡️ **Secure** — XXE protection enabled by default
- 🧰 **Flexible** — Static methods, fluent builder API, and CLI

## Quick Install

### Maven

```xml
<dependency>
    <groupId>io.brunoborges</groupId>
    <artifactId>jairosvg</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.brunoborges:jairosvg:1.0.0-SNAPSHOT'
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

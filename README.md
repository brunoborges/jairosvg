# JairoSVG

[![CI](https://github.com/brunoborges/jairosvg/actions/workflows/ci.yml/badge.svg)](https://github.com/brunoborges/jairosvg/actions/workflows/ci.yml)
[![Benchmark](https://github.com/brunoborges/jairosvg/actions/workflows/benchmark.yml/badge.svg)](https://github.com/brunoborges/jairosvg/actions/workflows/benchmark.yml)
[![Native Image](https://github.com/brunoborges/jairosvg/actions/workflows/native-image.yml/badge.svg)](https://github.com/brunoborges/jairosvg/actions/workflows/native-image.yml)
[![Coverage](https://codecov.io/gh/brunoborges/jairosvg/graph/badge.svg)](https://codecov.io/gh/brunoborges/jairosvg)
[![Maven Central](https://img.shields.io/maven-central/v/io.brunoborges/jairosvg)](https://central.sonatype.com/artifact/io.brunoborges/jairosvg)
[![Javadoc](https://javadoc.io/badge2/io.brunoborges/jairosvg/javadoc.svg)](https://javadoc.io/doc/io.brunoborges/jairosvg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 25+](https://img.shields.io/badge/Java-25%2B-orange)](https://openjdk.org/)

A high-performance SVG 1.1 to PNG, JPEG, TIFF, PDF, and PS/EPS converter powered by Java2D. API inspired by [CairoSVG](https://cairosvg.org).

## Why JairoSVG?

- **Zero native dependencies** — pure Java, runs anywhere the JVM runs
- **No heavyweight frameworks** — no Batik, no Cairo, no WebKit; just Java2D
- **Secure by default** — XXE protection enabled, `<script>` elements ignored, external resource access disabled unless explicitly opted in
- **Fast** — 3-30× faster than EchoSVG (Batik), competitive with JSVG, 1-2× faster than CairoSVG's native C backend
- **Drop-in API** — static one-liners, fluent builder, or CLI — your choice

## Features

- 🎨 **SVG 1.1 rendering** using Java2D — no native dependencies (with [selective SVG 2 alignment](LIMITATIONS.md#svg-2-behavioral-alignment))
- 📄 **Multiple output formats**: PNG, JPEG, TIFF, PDF (via optional [Apache PDFBox](https://pdfbox.apache.org/)), PostScript/EPS
- 🔷 **Full shape support**: rect, circle, ellipse, line, polygon, polyline, path
- 🌈 **Gradients**: linear and radial with stop colors, opacity, and `spreadMethod`
- ✍️ **Text rendering** with font control, letter-spacing, text-anchor, `<tspan>`, and `<textPath>`
- 🔄 **Transforms**: translate, rotate, scale, skewX, skewY, matrix
- 🎭 **Definitions**: `<use>`, `<symbol>`, `<defs>`, clip-path, viewBox, preserveAspectRatio
- 🖌️ **Patterns**: `<pattern>` with `patternTransform` and `patternUnits`
- 🎯 **Markers**: `<marker>` with `orient="auto"` tangent direction support
- 😷 **Masks**: `<mask>` with luminance-to-alpha compositing
- 🔍 **Filters**: `feGaussianBlur`, `feOffset`, `feFlood`, `feBlend`, `feMerge`, `feDropShadow`, `feImage`, `feTile`
- 🔤 **SVG Fonts**: `<font>` and `<glyph>` with system font fallback for undefined characters
- 🎨 **CSS**: stylesheets, inline styles, CSS variables (`var()`), `currentColor`, `inherit`
- 🔀 **Conditional processing**: `<switch>`, `systemLanguage`, `requiredExtensions`
- ⚡ **Fast**: 3-30× faster than EchoSVG (Batik fork), on par with JSVG, 1-2× faster than CairoSVG's native C backend
- 🛡️ **Secure**: XML external entity (XXE) protection by default
- 🧰 **Flexible API**: Static methods, fluent builder, CLI

### Sample SVG → PNG conversion benchmark (lower is better):

| Test Case                | JairoSVG (Java) | EchoSVG (Java) | JSVG (Java) | CairoSVG (Python) |
| ------------------------ | :-------------: | :------------: | :---------: | :---------------: |
| Simple shapes            |     3.4 ms      |    16.7 ms     | **3.3 ms**  |      4.1 ms       |
| Gradients                |   **4.3 ms**    |    131.3 ms    |   4.2 ms    |      10.4 ms      |
| Filters                  |   **6.8 ms**    |    34.2 ms     |   8.2 ms    |       N/A         |
| Fe blend modes           |   **9.5 ms**    |    27.7 ms     |  20.0 ms    |       N/A         |
| Embedded image           |   **4.4 ms**    |    16.8 ms     |  11.0 ms    |      6.2 ms       |
| Localized masks          |  **14.5 ms**    |    56.0 ms     |  15.6 ms    |       N/A         |

_JairoSVG is 3–30× faster than EchoSVG, on par with JSVG for simple SVGs, and significantly faster on filters, blends, masks, and images._

Run the benchmark yourself: `jbang comparison/benchmark/benchmark.java`.

See **[comparison/README.md](comparison/README.md)** for full benchmark results, PNG file size comparisons, and feature matrices across JairoSVG, EchoSVG, CairoSVG, and JSVG.

See **[comparison/COMPARISON.md](comparison/COMPARISON.md)** for side-by-side rendered PNG comparisons, benchmark times, and file sizes across 42 SVG test cases.

## Installation

> **Tip:** Check [Maven Central](https://central.sonatype.com/artifact/io.brunoborges/jairosvg) for the latest released version.

### Maven

```xml
<dependency>
    <groupId>io.brunoborges</groupId>
    <artifactId>jairosvg</artifactId>
    <version>1.0.4</version>
</dependency>
```

> **Note:** PDF output requires [Apache PDFBox](https://pdfbox.apache.org/) on the classpath. It is an **optional** dependency — if you only need PNG, JPEG, TIFF, or PS/EPS output, you do not need to add PDFBox. To enable PDF support, add it explicitly:
>
> ```xml
> <dependency>
>     <groupId>org.apache.pdfbox</groupId>
>     <artifactId>pdfbox</artifactId>
>     <version>3.0.6</version>
> </dependency>
> ```

### Gradle

```groovy
implementation 'io.brunoborges:jairosvg:1.0.4'
```

### JBang (quick run)

```bash
jbang --deps io.brunoborges:jairosvg:1.0.4 MyScript.java
```

## Quick Start

### Library API

```java
import io.brunoborges.jairosvg.JairoSVG;

// SVG bytes → PNG bytes
byte[] png = JairoSVG.svg2png(svgBytes);

// SVG file → PDF file
JairoSVG.svg2pdf(Path.of("input.svg"), Path.of("output.pdf"));

// One-liner conversions for all formats
byte[] jpeg = JairoSVG.svg2jpeg(svgBytes);
byte[] tiff = JairoSVG.svg2tiff(svgBytes);
byte[] ps   = JairoSVG.svg2ps(svgBytes);
byte[] eps  = JairoSVG.svg2eps(svgBytes);

// File-to-file for any format
JairoSVG.svg2jpeg(Path.of("input.svg"), Path.of("output.jpg"));
JairoSVG.svg2tiff(Path.of("input.svg"), Path.of("output.tiff"));
JairoSVG.svg2ps(Path.of("input.svg"), Path.of("output.ps"));
JairoSVG.svg2eps(Path.of("input.svg"), Path.of("output.eps"));

// Builder API with options
byte[] scaled = JairoSVG.builder()
    .fromBytes(svgBytes)
    .dpi(150)
    .scale(2)
    .backgroundColor("#ffffff")
    .toPng();

// Control image compression/quality
byte[] fastPng = JairoSVG.builder()
    .fromFile(Path.of("icon.svg"))
    .pngCompressionLevel(1)   // 0 (fastest) to 9 (smallest)
    .toPng();

byte[] highQualityJpeg = JairoSVG.builder()
    .fromFile(Path.of("photo.svg"))
    .jpegQuality(0.95f)       // 0.0 (smallest) to 1.0 (best)
    .toJpeg();

// Get BufferedImage directly
BufferedImage image = JairoSVG.builder()
    .fromFile(Path.of("icon.svg"))
    .toImage();

// SVG string → PNG
byte[] png = JairoSVG.builder()
    .fromString("<svg>...</svg>")
    .toPng();

// Stream input
byte[] fromStream = JairoSVG.builder()
    .fromStream(inputStream)
    .toPng();

// PS/EPS output (returns byte[])
byte[] ps = JairoSVG.builder()
    .fromFile(Path.of("diagram.svg"))
    .toPs();

byte[] eps = JairoSVG.builder()
    .fromFile(Path.of("diagram.svg"))
    .toEps();

// TIFF output
JairoSVG.builder()
    .fromFile(Path.of("drawing.svg"))
    .toTiff(new FileOutputStream("output.tiff"));

// Negate colors + output dimensions
byte[] inverted = JairoSVG.builder()
    .fromBytes(svgBytes)
    .negateColors(true)
    .outputWidth(800)
    .outputHeight(600)
    .toPng();

// Unsafe mode (allows external file access)
byte[] result = JairoSVG.builder()
    .fromUrl("https://example.com/chart.svg")
    .unsafe(true)
    .toPng();

// Customize Java2D rendering hints
import java.awt.RenderingHints;

byte[] quality = JairoSVG.builder()
    .fromBytes(svgBytes)
    .renderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    .toPng();
```

### Command Line

Install with JBang
```bash
jbang app install io.brunoborges:jairosvg:LATEST

# SVG → PNG
jairosvg input.svg -o output.png

# SVG → PDF with 2x scale
jairosvg input.svg -f pdf -s 2 -o output.pdf
```

Manually build:
```bash
# Build the CLI
mvn package

# SVG → PNG
java --enable-preview -jar target/jairosvg-1.0.4-cli.jar input.svg -o output.png

# SVG → PDF with 2x scale
java --enable-preview -jar target/jairosvg-1.0.4-cli.jar input.svg -f pdf -s 2 -o output.pdf
```

Build a GraalVM native CLI:
```bash
# Create native executable from the shaded CLI JAR
# Required native-image arguments are read from the JAR's META-INF/native-image config
native-image -jar target/jairosvg-1.0.4-cli.jar
./jairosvg input.svg -o output.png
```

### CLI Options

| Option                   | Description                                                     |
| ------------------------ | --------------------------------------------------------------- |
| `-o, --output FILE`      | Output filename                                                 |
| `-f, --format FORMAT`    | Output format: `png`, `jpeg`, `tiff`, `pdf`, `ps`, `eps` |
| `-d, --dpi DPI`          | Resolution (default: 96)                                        |
| `-s, --scale FACTOR`     | Scale factor (default: 1)                                       |
| `-b, --background COLOR` | Background color                                                |
| `-W, --width PIXELS`     | Parent container width                                          |
| `-H, --height PIXELS`    | Parent container height                                         |
| `--output-width PIXELS`  | Desired output width                                            |
| `--output-height PIXELS` | Desired output height                                           |
| `-n, --negate-colors`    | Negate vector colors                                            |
| `-i, --invert-images`    | Invert raster image colors                                      |
| `-u, --unsafe`           | Allow external file access                                      |

## Supported SVG Features

### Elements

- ✅ Basic shapes: `rect`, `circle`, `ellipse`, `line`, `polygon`, `polyline`
- ✅ Path commands: M, L, C, S, Q, T, A, H, V, Z (absolute and relative)
- ✅ `<use>`, `<symbol>`, `<defs>`, `<g>`
- ✅ `<svg>` with nesting and viewBox
- ✅ `<image>` — embedded raster (data URI and external) and nested SVG
- ✅ `<text>`, `<tspan>`, `<textPath>`, `<a>`
- ✅ `<marker>` with `orient="auto"` tangent direction
- ✅ `<clipPath>`
- ✅ `<mask>` with luminance-to-alpha compositing
- ✅ `<pattern>` with `patternTransform` and `patternUnits`
- ✅ `<linearGradient>`, `<radialGradient>` with `spreadMethod` and href chaining
- ✅ `<font>`, `<font-face>`, `<glyph>`, `<missing-glyph>` (SVG fonts)
- ✅ `<switch>` with conditional processing (`systemLanguage`, `requiredExtensions`)
- ✅ `<cursor>`, `<foreignObject>` (parsed but not rendered — see [LIMITATIONS.md](LIMITATIONS.md))

### Filters

- ✅ `<filter>` with `filterUnits` and sub-region optimization
- ✅ `feGaussianBlur`, `feOffset`, `feFlood`, `feBlend` (normal, multiply, screen, darken, lighten)
- ✅ `feMerge`, `feComposite` (over, in, out, atop, xor, arithmetic), `feDropShadow`
- ✅ `feImage` (data URI + inline references), `feTile`
- ✅ `feColorMatrix` (matrix, saturate, hueRotate, luminanceToAlpha)
- ✅ `feComponentTransfer` (identity, linear, gamma, table, discrete)
- ✅ `feMorphology` (erode, dilate), `feConvolveMatrix`
- ✅ `feTurbulence` (turbulence, fractalNoise), `feDisplacementMap`
- ✅ `feDiffuseLighting`, `feSpecularLighting` (distant, point, spot lights)

### Styling

- ✅ CSS stylesheets (embedded `<style>`) and inline `style` attributes
- ✅ CSS `@import` rules (URL resolution with circular import detection)
- ✅ CSS selectors: class, id, element, descendant, `:first-child`, `:last-child`, `:nth-child()`, `:not()`
- ✅ CSS custom properties / variables (`var(--name, fallback)`)
- ✅ `currentColor`, `inherit`
- ✅ Opacity: element, fill, stroke, group
- ✅ `display`, `visibility`

### Other

- ✅ Transforms: translate, rotate, scale, skewX, skewY, matrix
- ✅ viewBox and preserveAspectRatio (all 9 align values + meet/slice)
- ✅ Stroke properties: dasharray, dashoffset, linecap, linejoin, miterlimit, width
- ✅ Named colors (170+), hex, `rgb()`, `rgba()`, `hsl()`, `hsla()`
- ✅ Units: px, pt, em, ex, %, cm, mm, in, pc

## Architecture

JairoSVG renders SVG through the standard Java2D (`Graphics2D` / `BufferedImage`) API. The architecture is intentionally compact:

| Java Class        | Role                                     |
| ----------------- | ---------------------------------------- |
| `JairoSVG`        | Public API + fluent Builder              |
| `Surface`         | Java2D rendering engine + state mgmt     |
| `Node`            | SVG DOM tree with CSS cascade            |
| `PathDrawer`      | SVG path commands → GeneralPath          |
| `ShapeDrawer`     | Basic shape elements                     |
| `TextDrawer`      | Text, tspan, textPath rendering          |
| `Defs`            | Definition elements + clip paths         |
| `GradientPainter` | Linear/radial gradient rendering         |
| `PatternPainter`  | Pattern → TexturePaint                   |
| `MaskPainter`     | Mask luminance-to-alpha compositing      |
| `MarkerDrawer`    | Marker placement and orientation         |
| `FilterRenderer`  | Filter pipeline (blur, blend, merge...) |
| `GaussianBlur`    | Optimized box-blur Gaussian              |
| `BlendCompositor` | feBlend pixel blending modes             |
| `ImageHandler`    | Embedded image handling                  |
| `SvgDrawer`       | Nested `<svg>` viewport handling         |
| `Colors`          | Color parsing (170+ named)               |
| `CssProcessor`    | CSS parsing and selector matching        |
| `SvgFont`         | SVG font glyph caching                  |
| `Helpers`         | Units, transforms, aspect ratio          |
| `Main`            | CLI entry point                          |

**Key technologies:**

- `java.awt.Graphics2D` — 2D rendering context
- `java.awt.image.BufferedImage` — raster image buffer
- `java.awt.geom.AffineTransform` — coordinate transforms
- Apache PDFBox 3.0 — PDF output (optional dependency)

## Building

```bash
# Clone and build
git clone https://github.com/brunoborges/jairosvg.git
cd jairosvg
./mvnw clean verify

# Run tests
./mvnw test

# Generate documentation site
./mvnw site
```

## Limitations

See [LIMITATIONS.md](LIMITATIONS.md) for known and intentional limitations (including unsupported embedded content such as `<foreignObject>`).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on building, testing, and submitting pull requests.

## License

JairoSVG is licensed under the [MIT License](LICENSE).

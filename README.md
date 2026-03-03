# JairoSVG

[![CI](https://github.com/brunoborges/jairosvg/actions/workflows/ci.yml/badge.svg)](https://github.com/brunoborges/jairosvg/actions/workflows/ci.yml)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL_v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)
[![Java 25+](https://img.shields.io/badge/Java-25%2B-orange)](https://openjdk.org/)

A high-performance Java port of [CairoSVG](https://cairosvg.org) — SVG 1.1 to PNG, JPEG, TIFF, PDF, PS/EPS, and SVG converter powered by Java2D.

## Features

- 🎨 **SVG 1.1 rendering** using Java2D — no native dependencies
- 📄 **Multiple output formats**: PNG, JPEG, TIFF, PDF (via Apache PDFBox), PostScript/EPS, SVG
- 🔷 **Full shape support**: rect, circle, ellipse, line, polygon, polyline, path
- 🌈 **Gradients**: linear and radial with stop colors and opacity
- ✍️ **Text rendering** with font control, letter-spacing, text-anchor
- 🔄 **Transforms**: translate, rotate, scale, skew, matrix
- 🎭 **Advanced features**: clip-path, viewBox, preserveAspectRatio, `<use>`, CSS stylesheets
- ⚡ **Fast**: 2-5x faster than EchoSVG (Batik fork), competitive with CairoSVG's C backend
- 🛡️ **Secure**: XML external entity (XXE) protection by default
- 🧰 **Flexible API**: Static methods, fluent builder, CLI

## Benchmark

SVG → PNG conversion (lower is better):

| Test Case | JairoSVG (Java) | EchoSVG (Java) | CairoSVG (Python) |
|-----------|:---:|:---:|:---:|
| Simple shapes | **5.9 ms** | 9.9 ms | 2.0 ms |
| Gradients + transforms | **7.4 ms** | 35.5 ms | 5.3 ms |
| Complex paths + text | **11.1 ms** | 30.6 ms | 6.3 ms |

*JairoSVG is 1.7-4.8x faster than EchoSVG and within 1.4-2.9x of CairoSVG's native C backend.*

Run the benchmark yourself: `jbang benchmark.java`

## Installation

### Maven

```xml
<dependency>
    <groupId>com.jairosvg</groupId>
    <artifactId>jairosvg</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.jairosvg:jairosvg:1.0.0-SNAPSHOT'
```

### JBang (quick run)

```bash
jbang --deps com.jairosvg:jairosvg:1.0.0-SNAPSHOT MyScript.java
```

## Quick Start

### Library API

```java
import com.jairosvg.JairoSVG;

// SVG bytes → PNG bytes
byte[] png = JairoSVG.svg2png(svgBytes);

// SVG file → PDF file
JairoSVG.svg2pdf(Path.of("input.svg"), Path.of("output.pdf"));

// Builder API with options
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

### Command Line

```bash
# Build the CLI
mvn package

# SVG → PNG
java --enable-preview -jar target/jairosvg-1.0.0-SNAPSHOT-cli.jar input.svg -o output.png

# SVG → PDF with 2x scale
java --enable-preview -jar target/jairosvg-1.0.0-SNAPSHOT-cli.jar input.svg -f pdf -s 2 -o output.pdf
```

### CLI Options

| Option | Description |
|--------|-------------|
| `-o, --output FILE` | Output filename |
| `-f, --format FORMAT` | Output format: `png`, `jpeg`, `tiff`, `pdf`, `ps`, `eps`, `svg` |
| `-d, --dpi DPI` | Resolution (default: 96) |
| `-s, --scale FACTOR` | Scale factor (default: 1) |
| `-b, --background COLOR` | Background color |
| `-W, --width PIXELS` | Parent container width |
| `-H, --height PIXELS` | Parent container height |
| `--output-width PIXELS` | Desired output width |
| `--output-height PIXELS` | Desired output height |
| `-n, --negate-colors` | Negate vector colors |
| `-i, --invert-images` | Invert raster image colors |
| `-u, --unsafe` | Allow external file access |

## Supported SVG Features

- ✅ Basic shapes: `rect`, `circle`, `ellipse`, `line`, `polygon`, `polyline`
- ✅ Path commands: M, L, C, S, Q, T, A, H, V, Z (absolute and relative)
- ✅ Linear and radial gradients with `spreadMethod`
- ✅ CSS stylesheets and inline styles
- ✅ Text and tspan with font properties
- ✅ Transforms: translate, rotate, scale, skewX, skewY, matrix
- ✅ `<use>` element and `<defs>`
- ✅ Clip paths
- ✅ viewBox and preserveAspectRatio
- ✅ Opacity (element, fill, stroke)
- ✅ Image embedding (raster and nested SVG)
- ✅ Stroke properties: dasharray, linecap, linejoin, width

## Architecture

JairoSVG is a module-by-module port of CairoSVG's Python codebase to Java 25:

| Java Class | Python Module | Role |
|------------|---------------|------|
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
| `Main` | `__main__.py` | CLI |

**Key technology mapping:**
- `cairo.Context` → `java.awt.Graphics2D`
- `cairo.ImageSurface` → `java.awt.image.BufferedImage`
- `cairo.Matrix` → `java.awt.geom.AffineTransform`
- PDF output via Apache PDFBox 3.0

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

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on building, testing, and submitting pull requests.

## License

JairoSVG is based on [CairoSVG](https://cairosvg.org) and is licensed under the [GNU Lesser General Public License v3.0](LICENSE).

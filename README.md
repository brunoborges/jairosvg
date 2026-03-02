# JairoSVG

A Java port of [CairoSVG](https://cairosvg.org) — SVG 1.1 to PNG, PDF, PS and SVG converter.

## Features

- SVG 1.1 parsing and rendering using Java2D
- Output formats: **PNG**, **PDF** (via Apache PDFBox), **SVG**, **PS**
- Shapes: rect, circle, ellipse, line, polygon, polyline, path
- Full SVG path command support (M, L, C, S, Q, T, A, H, V, Z)
- Linear and radial gradients
- CSS stylesheets and inline styles
- Text rendering with font control
- Image embedding (raster and nested SVG)
- Transforms, clip paths, viewBox, preserveAspectRatio
- `<use>` element support
- Command-line interface
- Fluent builder API

## Requirements

- Java 25+
- Maven 3.9+

## Quick Start

### Library API

```java
import com.jairosvg.JairoSVG;

// SVG bytes to PNG bytes
byte[] png = JairoSVG.svg2png(svgBytes);

// SVG file to PDF file
JairoSVG.svg2pdf(Path.of("input.svg"), Path.of("output.pdf"));

// Builder API with options
byte[] png = JairoSVG.builder()
    .fromString("<svg ...>...</svg>")
    .dpi(150)
    .scale(2)
    .backgroundColor("#ffffff")
    .toPng();

// Get BufferedImage directly
BufferedImage image = JairoSVG.builder()
    .fromFile(Path.of("input.svg"))
    .toImage();
```

### Command Line

```bash
# Build
mvn package

# Convert SVG to PNG
java --enable-preview -jar target/jairosvg-1.0.0-SNAPSHOT.jar input.svg -o output.png

# Convert to PDF
java --enable-preview -jar target/jairosvg-1.0.0-SNAPSHOT.jar input.svg -f pdf -o output.pdf

# Scale output
java --enable-preview -jar target/jairosvg-1.0.0-SNAPSHOT.jar input.svg -s 2 -o output.png
```

### CLI Options

| Option | Description |
|--------|-------------|
| `-o, --output FILE` | Output filename (default: stdout) |
| `-f, --format FORMAT` | Output format: png, pdf, ps, eps, svg |
| `-d, --dpi DPI` | DPI ratio (default: 96) |
| `-W, --width PIXELS` | Parent container width |
| `-H, --height PIXELS` | Parent container height |
| `-s, --scale FACTOR` | Output scaling factor (default: 1) |
| `-b, --background COLOR` | Output background color |
| `-n, --negate-colors` | Negate vector colors |
| `-u, --unsafe` | Allow external file access |
| `--output-width PIXELS` | Desired output width |
| `--output-height PIXELS` | Desired output height |

## Architecture

Port of CairoSVG's Python modules to Java:

| Java Class | Python Module | Description |
|------------|---------------|-------------|
| `JairoSVG` | `__init__.py` | Public API |
| `Surface` | `surface.py` | Rendering engine (Java2D) |
| `Node` | `parser.py` | SVG DOM tree |
| `Colors` | `colors.py` | Color parsing |
| `Helpers` | `helpers.py` | Size, transform utilities |
| `CssProcessor` | `css.py` | CSS parsing |
| `PathDrawer` | `path.py` | SVG path commands |
| `ShapeDrawer` | `shapes.py` | Basic shapes |
| `TextDrawer` | `text.py` | Text rendering |
| `ImageHandler` | `image.py` | Image embedding |
| `Defs` | `defs.py` | Gradients, patterns, clips |
| `BoundingBox` | `bounding_box.py` | Bounding box calculations |
| `Main` | `__main__.py` | CLI |

## License

Based on CairoSVG which is LGPLv3-licensed.

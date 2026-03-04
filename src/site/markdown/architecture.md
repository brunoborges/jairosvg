# Architecture

JairoSVG is a module-by-module port of [CairoSVG](https://cairosvg.org)'s Python codebase to Java 25.

## Module Mapping

| Java Class | Python Module | Responsibility |
|------------|---------------|----------------|
| `JairoSVG` | `__init__.py` | Public API and ConversionBuilder |
| `Surface` | `surface.py` | Core Java2D rendering engine |
| `PngSurface` | `surface.py` | PNG output via `javax.imageio` |
| `PdfSurface` | `surface.py` | PDF output via Apache PDFBox |
| `PsSurface` | `surface.py` | PostScript output via `PrinterJob` |
| `Node` | `parser.py` | SVG DOM tree with CSS application |
| `PathDrawer` | `path.py` | SVG path command parser (M, L, C, Q, A, ...) |
| `ShapeDrawer` | `shapes.py` | Basic shapes (rect, circle, ellipse, ...) |
| `TextDrawer` | `text.py` | Text and tspan rendering |
| `Defs` | `defs.py` | Gradients, clip paths, use, markers |
| `ImageHandler` | `image.py` | Embedded image handling |
| `Colors` | `colors.py` | SVG/CSS color parsing (170+ named colors) |
| `Helpers` | `helpers.py` | Units, transforms, size calculations |
| `CssProcessor` | `css.py` | CSS stylesheet and selector matching |
| `BoundingBox` | `bounding_box.py` | Element bounding box computation |
| `Features` | `features.py` | SVG feature detection |
| `UrlHelper` | `url.py` | URL resolution and fetching |
| `SvgDrawer` | `svg.py` | Root `<svg>` element handler |
| `Main` | `__main__.py` | CLI entry point |

## Technology Mapping

| CairoSVG (Python) | JairoSVG (Java) |
|--------------------|-----------------|
| `cairocffi.Context` | `java.awt.Graphics2D` |
| `cairocffi.ImageSurface` | `java.awt.image.BufferedImage` |
| `cairocffi.Matrix` | `java.awt.geom.AffineTransform` |
| `cairocffi.LinearGradient` | `java.awt.LinearGradientPaint` |
| `cairocffi.RadialGradient` | `java.awt.RadialGradientPaint` |
| `cairocffi` stroke/fill | `java.awt.BasicStroke` + `Graphics2D` |
| `tinycss2` | Custom CSS declaration parser |
| `cssselect2` | Custom CSS selector matcher |
| `defusedxml` | `DocumentBuilderFactory` with secure config |
| `Pillow (PIL)` | `javax.imageio.ImageIO` |
| PDF via Cairo | PDF via Apache PDFBox 3.0 |

## Rendering Pipeline

```
SVG Input (bytes/file/URL)
    ↓
XML Parsing (secure DocumentBuilderFactory)
    ↓
Node Tree (DOM with attribute inheritance)
    ↓
CSS Application (stylesheet + inline styles)
    ↓
Surface.draw() — recursive element rendering
    ├── ShapeDrawer (rect, circle, ellipse, line, polygon)
    ├── PathDrawer (M, L, C, S, Q, T, A, H, V, Z)
    ├── TextDrawer (text, tspan)
    ├── ImageHandler (embedded raster/SVG images)
    ├── Defs (gradients, clip-path, use, markers)
    └── SvgDrawer (nested <svg> elements)
    ↓
Output Surface
    ├── PngSurface → BufferedImage → ImageIO.write()
    ├── PdfSurface → BufferedImage → PDFBox PDPage
    └── PsSurface → BufferedImage → PrinterJob
```

## Key Design Decisions

1. **Java2D over JavaFX**: Java2D is available in all JDK distributions without extra modules
2. **PDFBox for PDF**: Mature, well-maintained, pure Java PDF library
3. **No external CSS library**: SVG CSS needs are limited; a minimal parser avoids heavy dependencies
4. **Secure by default**: XML parsing hardened against XXE, with opt-in `unsafe` mode
5. **Static API + Builder**: Simple cases use static methods; complex cases use the builder pattern

## Performance Optimizations

- Pre-compiled regex patterns as static fields (Helpers, PathDrawer, CssProcessor)
- Cached `DocumentBuilderFactory` singletons (secure and unsafe variants)
- `GeneralPath.reset()` reuse instead of allocation per element
- `indexOf`-based token extraction replacing `String.split()` in hot paths

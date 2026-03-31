# Architecture

JairoSVG is a module-by-module port of [CairoSVG](https://cairosvg.org)'s Python codebase to Java 25.

## Module Mapping

| Java Class | Python Module | Responsibility |
|------------|---------------|----------------|
| `JairoSVG` | `__init__.py` | Public API and ConversionBuilder |
| `Surface` | `surface.py` | Core Java2D rendering engine |
| `PngSurface` | `surface.py` | PNG output via `javax.imageio` |
| `JpegSurface` | `surface.py` | JPEG output via `javax.imageio` |
| `TiffSurface` | `surface.py` | TIFF output via `javax.imageio` |
| `PdfSurface` | `surface.py` | PDF output via Apache PDFBox |
| `PsSurface` | `surface.py` | PostScript/EPS output via `PrinterJob` |
| `Node` | `parser.py` | SVG DOM tree with CSS application (SAX-based) |
| `PathDrawer` | `path.py` | SVG path command parser (M, L, C, Q, A, H, V, ...) |
| `ShapeDrawer` | `shapes.py` | Basic shapes (rect, circle, ellipse, line, polygon, polyline) |
| `TextDrawer` | `text.py` | Text, tspan, and textPath rendering |
| `Defs` | `defs.py` | Clip paths, `<use>`, `<symbol>` handling |
| `GradientPainter` | `defs.py` | Linear and radial gradient rendering with href chains |
| `PatternPainter` | `defs.py` | Pattern fill rendering with `patternTransform` |
| `FilterRenderer` | `defs.py` | SVG filter pipeline (feGaussianBlur, feBlend, feMerge, ...) |
| `BlendCompositor` | `defs.py` | Pixel-level blend mode compositing |
| `GaussianBlur` | `defs.py` | Optimized box-blur Gaussian approximation |
| `MarkerDrawer` | `defs.py` | Marker rendering on paths and shapes |
| `MaskPainter` | `defs.py` | Luminance-based mask compositing |
| `ImageHandler` | `image.py` | Embedded raster and SVG image handling |
| `Colors` | `colors.py` | SVG/CSS color parsing (170+ named, hex, rgb, hsl) |
| `Helpers` | `helpers.py` | Units, transforms, size calculations, path normalization |
| `CssProcessor` | `css.py` | CSS stylesheet parsing and selector matching |
| `BoundingBox` | `bounding_box.py` | Element bounding box computation |
| `Features` | `features.py` | SVG conditional feature detection |
| `UrlHelper` | `url.py` | URL resolution, fetching, and data URI parsing |
| `SvgDrawer` | `svg.py` | Root `<svg>` element handler |
| `SvgFont` | `text.py` | SVG font glyph parsing and caching |
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
| `defusedxml` | SAX parser with secure configuration |
| `Pillow (PIL)` | `javax.imageio.ImageIO` |
| PDF via Cairo | PDF via Apache PDFBox 3.0 |

## Rendering Pipeline

```
SVG Input (bytes/file/URL/string/stream)
    ↓
SAX Parsing (secure, with gzip auto-detection)
    ↓
Node Tree (DOM-like with attribute inheritance)
    ↓
CSS Application (stylesheets + inline styles + cascade)
    ↓
Surface.draw() — recursive element rendering
    ├── ShapeDrawer (rect, circle, ellipse, line, polygon, polyline)
    ├── PathDrawer (M, L, C, S, Q, T, A, H, V, Z)
    ├── TextDrawer (text, tspan, textPath)
    ├── ImageHandler (embedded raster/SVG images)
    ├── SvgDrawer (nested <svg> elements)
    ├── GradientPainter (linearGradient, radialGradient)
    ├── PatternPainter (pattern fills)
    ├── FilterRenderer → BlendCompositor, GaussianBlur
    │   (feGaussianBlur, feOffset, feFlood, feBlend, feMerge,
    │    feDropShadow, feImage, feTile, feColorMatrix,
    │    feComposite, feComponentTransfer, feMorphology)
    ├── MaskPainter (luminance-based masks)
    ├── MarkerDrawer (marker-start/mid/end)
    └── Defs (clipPath, use, symbol)
    ↓
Output Surface
    ├── PngSurface  → BufferedImage → ImageIO.write("png")
    ├── JpegSurface → BufferedImage → ImageIO.write("jpeg")
    ├── TiffSurface → BufferedImage → ImageIO.write("tiff")
    ├── PdfSurface  → BufferedImage → PDFBox PDPage
    └── PsSurface   → BufferedImage → PrinterJob
```

## Key Design Decisions

1. **Java2D over JavaFX**: Java2D is available in all JDK distributions without extra modules
2. **SAX over DOM**: SAX parsing builds the Node tree directly, avoiding the overhead of a full DOM
3. **PDFBox for PDF**: Mature, well-maintained, pure Java PDF library (optional dependency)
4. **No external CSS library**: SVG CSS needs are limited; a minimal parser avoids heavy dependencies
5. **Secure by default**: XML parsing hardened against XXE, with opt-in `unsafe` mode
6. **Static API + Builder**: Simple cases use static methods; complex cases use the builder pattern
7. **Granular draw classes**: Filter, gradient, pattern, mask, and marker logic extracted into focused classes

## Performance Optimizations

- Pre-compiled regex patterns as static fields (Helpers, PathDrawer, CssProcessor)
- Cached SAX parser factory singletons (secure and unsafe variants)
- `GeneralPath.reset()` reuse instead of allocation per element
- `indexOf`-based token extraction replacing `String.split()` in hot paths
- Sub-region effect buffers for filters and masks (BoundingBox-based)
- Optimized three-pass box blur for Gaussian approximation (GaussianBlur)

# Architecture

JairoSVG is a pure Java SVG 1.1 renderer using Java2D.

## Module Overview

| Java Class | Responsibility |
|------------|----------------|
| `JairoSVG` | Public API and ConversionBuilder |
| `Surface` | Core Java2D rendering engine |
| `PngSurface` | PNG output via `javax.imageio` |
| `JpegSurface` | JPEG output via `javax.imageio` |
| `TiffSurface` | TIFF output via `javax.imageio` |
| `PdfSurface` | PDF output via Apache PDFBox |
| `PsSurface` | PostScript/EPS output via `PrinterJob` |
| `Node` | SVG DOM tree with CSS application (SAX-based) |
| `PathDrawer` | SVG path command parser (M, L, C, Q, A, H, V, ...) |
| `ShapeDrawer` | Basic shapes (rect, circle, ellipse, line, polygon, polyline) |
| `TextDrawer` | Text, tspan, and textPath rendering |
| `Defs` | Clip paths, `<use>`, `<symbol>` handling |
| `GradientPainter` | Linear and radial gradient rendering with href chains |
| `PatternPainter` | Pattern fill rendering with `patternTransform` |
| `FilterRenderer` | SVG filter pipeline (feGaussianBlur, feBlend, feMerge, ...) |
| `BlendCompositor` | Pixel-level blend mode compositing |
| `GaussianBlur` | Optimized box-blur Gaussian approximation |
| `MarkerDrawer` | Marker rendering on paths and shapes |
| `MaskPainter` | Luminance-based mask compositing |
| `ImageHandler` | Embedded raster and SVG image handling |
| `Colors` | SVG/CSS color parsing (170+ named, hex, rgb, hsl) |
| `Helpers` | Units, transforms, size calculations, path normalization |
| `CssProcessor` | CSS stylesheet parsing and selector matching |
| `BoundingBox` | Element bounding box computation |
| `Features` | SVG conditional feature detection |
| `UrlHelper` | URL resolution, fetching, and data URI parsing |
| `SvgDrawer` | Root `<svg>` element handler |
| `SvgFont` | SVG font glyph parsing and caching |
| `Main` | CLI entry point |

## Key Technologies

| Technology | Usage |
|------------|-------|
| `java.awt.Graphics2D` | 2D rendering context |
| `java.awt.image.BufferedImage` | Raster image buffer |
| `java.awt.geom.AffineTransform` | Coordinate transforms |
| `java.awt.LinearGradientPaint` | Linear gradient rendering |
| `java.awt.RadialGradientPaint` | Radial gradient rendering |
| `java.awt.BasicStroke` | Stroke styling |
| `javax.imageio.ImageIO` | PNG/JPEG/TIFF encoding |
| Apache PDFBox 3.0 | PDF output (optional) |
| SAX parser | Secure XML parsing |
| Custom CSS parser | SVG stylesheet support |

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
    │    feComposite, feComponentTransfer, feMorphology,
    │    feConvolveMatrix, feDisplacementMap, feTurbulence,
    │    feDiffuseLighting, feSpecularLighting)
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

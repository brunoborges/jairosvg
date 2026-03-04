# Copilot Instructions for JairoSVG

## Project Overview

JairoSVG is a Java port of [CairoSVG](https://cairosvg.org) (Python) — an SVG 1.1 to PNG/PDF/PS converter using Java2D. The architecture mirrors CairoSVG's module structure, mapping `cairo.Context` → `Graphics2D`, `cairo.ImageSurface` → `BufferedImage`, and `cairo.Matrix` → `AffineTransform`.

## Dependencies

- **Required**: Java 25 with preview features
- **Optional**: Apache PDFBox 3.0 — only needed for PDF output (`toPdf()`/`svg2pdf()`). All other formats (PNG, JPEG, TIFF, PS/EPS) work without it. Calling `toPdf()` without PDFBox on the classpath throws `UnsupportedOperationException`.

## Build & Test

Requires **Java 25** with preview features enabled. Uses the Maven wrapper (`./mvnw`).

```bash
# Build and run all tests
./mvnw clean verify

# Run tests only
./mvnw test

# Run a single test method
./mvnw test -Dtest="JairoSVGTest#testMethodName"

# Apply code formatting (Spotless with Eclipse formatter)
./mvnw spotless:apply

# Install to local Maven repo (needed before running JBang scripts)
./mvnw install -DskipTests

# Run benchmark (requires jbang + local install)
jbang comparison/benchmark.java

# Generate comparison PNGs (requires jbang + local install + python3 cairosvg)
jbang comparison/generate.java
```

## Architecture

### Rendering Pipeline

`Surface.draw()` is the central dispatcher — a switch on `node.tag` that routes to specialized static drawer classes:

| Tag | Handler |
|-----|---------|
| `rect`, `circle`, `ellipse`, `line`, `polygon`, `polyline` | `ShapeDrawer` |
| `path` | `PathDrawer` |
| `text`, `tspan`, `textPath`, `a` | `TextDrawer` |
| `image` | `ImageHandler` |
| `linearGradient`, `radialGradient`, `clipPath`, `pattern`, `use`, `mask`, `marker`, `filter` | `Defs` |
| `svg` | `SvgDrawer` |

### Key Class Roles (mapped to CairoSVG Python modules)

- **`JairoSVG`** — Public API: static convenience methods (`svg2png`, `svg2pdf`) + fluent `ConversionBuilder`
- **`Surface`** — Rendering engine holding `Graphics2D` context, `GeneralPath`, and def maps (gradients, patterns, masks, fonts, etc.). Manages save/restore of graphics state per node
- **`Node`** — SVG DOM wrapper with CSS cascade: inherits attributes from parents, applies CSS rules, resolves `currentColor`/`inherit`
- **`Defs`** — Static utility class for definition elements. Gradients support href chaining for inherited stops/attributes. Patterns render to `BufferedImage` then wrap in `TexturePaint`
- **`Helpers`** — Units (`size()` handles %, em, px, pt, cm, etc.), transform parsing (`parseTransform()`), `preserveAspectRatio` calculation, path normalization
- **`Colors`** — Parses 170+ named colors, hex, `rgb()`, `rgba()`, `hsl()`, `hsla()`. Uses `RGBA` record with float 0–1 components
- **`CssProcessor`** — CSS selector matching supporting `:first-child`, `:last-child`, `:nth-child()`, `:not()`, and `::pseudo-elements`
- **`SvgFont`** — Caches pre-parsed `GeneralPath` per glyph; keyed by `font-family` (not `id`) in `Surface.fonts`
- **`Features`** — SVG 1.1 conditional processing (`requiredFeatures`, `systemLanguage`)

### Adding a New SVG Element

1. Add a case to the switch in `Surface.draw()`
2. If non-visual (definition-only), add to `INVISIBLE_TAGS` in `Surface`
3. If it's a def type, register it in `Defs.parseDef()` into the appropriate `Surface` map

### External Resource Resolution

Always use `UrlHelper.parseUrl(href, baseUrl)` before fetching external resources.

## Conventions

- **Static drawer methods**: All drawer classes (`ShapeDrawer`, `PathDrawer`, `TextDrawer`, `Defs`, `ImageHandler`) use static methods that take `(Surface, Node)` as parameters
- **Formatting**: 4-space indentation, Eclipse formatter via Spotless. Run `./mvnw spotless:apply` before committing
- **Testing**: JUnit Jupiter (JUnit 6). Single test class `JairoSVGTest` with integration-style tests. Use `@TempDir` for file I/O tests. Float assertions use epsilon tolerance (0.01)
- **Preview features**: `--enable-preview` is required everywhere (compiler, surefire, CLI jar). This is configured in pom.xml
- **CI**: Runs on ubuntu, macOS, and Windows via GitHub Actions. JaCoCo coverage uploaded from Ubuntu

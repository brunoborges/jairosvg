# FAQ

## General

### What is JairoSVG?

JairoSVG is a Java port of [CairoSVG](https://cairosvg.org), a Python SVG converter. It converts SVG 1.1 images to PNG, PDF, PostScript, and SVG formats using pure Java (Java2D for rendering, PDFBox for PDF).

### Why "JairoSVG"?

**J** (Java) + Cairo → Jairo. It's also a common name in Portuguese and Spanish.

### What Java version is required?

Java 25 or later with `--enable-preview` flag. The project uses Java 25 preview features.

### Does it require native libraries?

No. JairoSVG is pure Java. Unlike CairoSVG (which depends on Cairo, a C library), JairoSVG has no native dependencies.

## SVG Support

### What SVG features are supported?

- Basic shapes: rect, circle, ellipse, line, polygon, polyline
- Full path commands: M, L, H, V, C, S, Q, T, A, Z
- Linear and radial gradients
- CSS stylesheets and inline styles
- Text and tspan
- Transforms (translate, rotate, scale, skew, matrix)
- `<use>` and `<defs>` elements
- Clip paths
- viewBox and preserveAspectRatio
- Opacity (element, fill, stroke)
- Image embedding

### What is NOT supported?

- SVG filters (partial: feOffset, feBlend only)
- CSS descendant/sibling selectors
- Animations (SMIL)
- JavaScript
- Fonts via `@font-face` (system fonts are used)
- Patterns and masks (simplified support)

### My SVG renders incorrectly. What should I do?

1. Test your SVG in a browser to confirm it's valid
2. Try CairoSVG (Python) with the same SVG — if it also fails, it may be an SVG feature not supported by CairoSVG
3. Open a [bug report](https://github.com/brunoborges/jairosvg/issues/new?template=bug_report.md) with the SVG (or a minimal reproduction)

## Security

### Is it safe to process untrusted SVGs?

By default, yes. JairoSVG's XML parser:
- Disallows DOCTYPE declarations (prevents XXE attacks)
- Enables secure processing features
- Does not resolve external entities

### What does the `unsafe` flag do?

The `unsafe` flag allows DOCTYPE declarations in SVG files. Only use it with trusted input. Some SVGs use DOCTYPE for entity definitions.

## Performance

### How fast is it?

JairoSVG is 2-5x faster than EchoSVG (a Batik fork) and within 1.3-1.9x of CairoSVG's native C backend. See the [benchmark page](benchmark.html) for details.

### How can I improve performance?

- Reuse `JairoSVG.builder()` instances for repeated conversions
- For batch processing, convert in parallel using virtual threads
- Ensure adequate heap space for large SVGs: `-Xmx512m`

## Troubleshooting

### `UnsupportedClassVersionError`

You need Java 25 or later. Check with `java -version`.

### `Preview features not enabled`

Add `--enable-preview` to your JVM arguments.

### `OutOfMemoryError`

Large or complex SVGs may need more heap space: `java -Xmx512m --enable-preview -jar jairosvg-cli.jar ...`

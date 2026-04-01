# FAQ

## General

### What is JairoSVG?

JairoSVG is a Java port of [CairoSVG](https://cairosvg.org), a Python SVG converter. It converts SVG 1.1 images to PNG, JPEG, TIFF, PDF, and PostScript/EPS formats using pure Java (Java2D for rendering, optional PDFBox for PDF).

### Why "JairoSVG"?

**J** (Java) + Cairo → Jairo. It's also a common name in Portuguese and Spanish.

### What Java version is required?

Java 25 or later with `--enable-preview` flag. The project uses Java 25 preview features.

### Does it require native libraries?

No. JairoSVG is pure Java. Unlike CairoSVG (which depends on Cairo, a C library), JairoSVG has no native dependencies.

## SVG Support

### What SVG features are supported?

- **Shapes**: rect, circle, ellipse, line, polygon, polyline
- **Paths**: Full path commands — M, L, H, V, C, S, Q, T, A, Z (absolute and relative)
- **Gradients**: Linear and radial with stop colors, opacity, spread methods, href inheritance
- **Patterns**: Pattern fills with `patternUnits`, `patternTransform`, href chains
- **Filters**: feGaussianBlur, feOffset, feFlood, feBlend, feMerge, feDropShadow, feImage, feTile, feColorMatrix, feComposite, feComponentTransfer, feMorphology, feConvolveMatrix, feDisplacementMap, feTurbulence, feDiffuseLighting, feSpecularLighting
- **Masks**: Luminance-based masking with `<mask>` elements
- **Clip paths**: `<clipPath>` with arbitrary clip shapes
- **Markers**: `marker-start`, `marker-mid`, `marker-end` with orient auto/auto-start-reverse
- **Text**: `<text>`, `<tspan>`, `<textPath>`, text-anchor, letter-spacing, text-decoration, font properties
- **CSS**: Stylesheets (`<style>`), `@import`, inline styles, selector matching (type, class, ID, attribute, pseudo-classes including `:nth-child`, `:not`, `:first-child`, `:last-child`), `!important`, custom properties (`var()`)
- **Transforms**: translate, rotate, scale, skewX, skewY, matrix, transform-origin
- **Structure**: `<use>`, `<defs>`, `<symbol>`, `<g>`, nested `<svg>`, `<switch>`
- **ViewBox**: `viewBox` with full `preserveAspectRatio` support (all alignments, meet/slice)
- **Opacity**: element, fill, stroke, and stop opacity
- **Images**: Embedded raster images (data URI and external), SVG-in-SVG
- **SVG Fonts**: `<font>`, `<glyph>`, `<missing-glyph>` elements
- **Conditional processing**: `requiredFeatures`, `systemLanguage`
- **Compressed SVG**: Automatic gzip/SVGZ detection and decompression

### What is NOT supported?

- **Animations**: SMIL elements (`<animate>`, `<animateTransform>`, `<animateMotion>`, `<set>`) — JairoSVG is a static renderer
- **Scripting**: `<script>` elements are intentionally ignored for security
- **Foreign content**: `<foreignObject>` and embedded HTML/XHTML are skipped
- **Some CSS features**: `calc()`, CSS nesting, and `@supports` rules are not supported in stylesheets

See [LIMITATIONS.md](https://github.com/brunoborges/jairosvg/blob/main/LIMITATIONS.md) for full details.

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

JairoSVG is 2–26x faster than EchoSVG (a Batik fork), on par with JSVG, and 1–2.4x faster than CairoSVG's native C backend. See the [benchmark page](benchmark.html) for details.

### How can I improve performance?

- For batch processing, convert in parallel using virtual threads
- Use `.toImage()` if you only need a `BufferedImage` (avoids PNG encoding)
- Ensure adequate heap space for large SVGs: `-Xmx512m`

## PDF Output

### How do I enable PDF output?

Add Apache PDFBox to your classpath:

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.7</version>
</dependency>
```

Without PDFBox, calling `.toPdf()` throws `UnsupportedOperationException`.

## Troubleshooting

### `UnsupportedClassVersionError`

You need Java 25 or later. Check with `java -version`.

### `Preview features not enabled`

Add `--enable-preview` to your JVM arguments.

### `OutOfMemoryError`

Large or complex SVGs may need more heap space: `java -Xmx512m --enable-preview -jar jairosvg-cli.jar ...`

### `UnsupportedOperationException: PDF output requires Apache PDFBox`

Add `org.apache.pdfbox:pdfbox` to your classpath. See [PDF Output](#how-do-i-enable-pdf-output) above.

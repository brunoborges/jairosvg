# API Reference

## Static Methods

The `JairoSVG` class provides convenient static methods for common conversions:

### SVG to PNG

```java
// From byte array
byte[] png = JairoSVG.svg2png(byte[] svgData);

// From URL string
byte[] png = JairoSVG.svg2png(String url);

// File to file
JairoSVG.svg2png(Path input, Path output);
```

### SVG to PDF

> Requires Apache PDFBox on the classpath (optional dependency).

```java
byte[] pdf = JairoSVG.svg2pdf(byte[] svgData);
byte[] pdf = JairoSVG.svg2pdf(String url);
JairoSVG.svg2pdf(Path input, Path output);
```

## Builder API

For more control, use the fluent builder:

```java
JairoSVG.builder()
    .fromBytes(svgBytes)       // or .fromString(), .fromFile(), .fromUrl(), .fromStream()
    .dpi(150)                  // Resolution (default: 96)
    .scale(2)                  // Scale factor (default: 1)
    .backgroundColor("#fff")   // Background color
    .parentWidth(800)          // Parent container width for %-based sizing
    .parentHeight(600)         // Parent container height for %-based sizing
    .outputWidth(400)          // Desired output width
    .outputHeight(300)         // Desired output height
    .negateColors(true)        // Invert vector colors
    .unsafe(false)             // Allow external entities (default: false)
    .pngCompressionLevel(6)    // PNG: 0 (fastest) to 9 (smallest), default ~6
    .jpegQuality(0.75f)        // JPEG: 0.0 (smallest) to 1.0 (best), default ~0.75
    .tiffCompressionType("LZW") // TIFF: "Deflate", "LZW", "JPEG", "PackBits", etc.
    .renderingHint(key, value) // Custom Java2D rendering hints
    .toPng();                  // or .toJpeg(), .toTiff(), .toPdf(), .toPs(), .toEps(), .toImage()
```

### Input Methods

| Method | Description |
|--------|-------------|
| `.fromBytes(byte[])` | SVG content as bytes |
| `.fromString(String)` | SVG content as a string |
| `.fromFile(Path)` | Read from file path |
| `.fromStream(InputStream)` | Read from input stream |
| `.fromUrl(String)` | Fetch from URL |

### Output Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `.toPng()` | `byte[]` | PNG image data |
| `.toPng(OutputStream)` | `void` | Write PNG to stream |
| `.toJpeg()` | `byte[]` | JPEG image data |
| `.toJpeg(OutputStream)` | `void` | Write JPEG to stream |
| `.toTiff()` | `byte[]` | TIFF image data |
| `.toTiff(OutputStream)` | `void` | Write TIFF to stream |
| `.toPdf()` | `byte[]` | PDF document data |
| `.toPdf(OutputStream)` | `void` | Write PDF to stream |
| `.toPs()` | `byte[]` | PostScript data |
| `.toEps()` | `byte[]` | Encapsulated PostScript data |
| `.toImage()` | `BufferedImage` | Java2D image object |

### Image Compression / Quality

| Method | Format | Range | Default | Description |
|--------|--------|-------|---------|-------------|
| `.pngCompressionLevel(int)` | PNG | 0–9 | ~6 | 0 = no compression (fastest), 9 = max compression (smallest) |
| `.jpegQuality(float)` | JPEG | 0.0–1.0 | ~0.75 | 0.0 = lowest quality (smallest), 1.0 = highest quality (largest) |
| `.tiffCompressionType(String)` | TIFF | — | writer default | "Deflate", "LZW", "JPEG", "ZLib", "PackBits", "Uncompressed" |

### Rendering Hints

Override Java2D rendering hints for fine-grained control:

```java
JairoSVG.builder()
    .fromBytes(svg)
    .renderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    .renderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    .toPng();
```

Defaults (matching JSVG): `KEY_ANTIALIASING → VALUE_ANTIALIAS_ON`, `KEY_STROKE_CONTROL → VALUE_STROKE_PURE`.

## Thread Safety

All static methods and builder instances are thread-safe. Each conversion creates its own rendering context.

## Error Handling

Conversion methods throw `Exception`. Common underlying causes:
- `org.xml.sax.SAXException` — Malformed SVG/XML
- `java.io.IOException` — I/O errors
- `IllegalArgumentException` — No input provided
- `UnsupportedOperationException` — PDF output requested without PDFBox on classpath

## Security

By default, JairoSVG protects against XML External Entity (XXE) attacks:
- DOCTYPE declarations are disallowed
- External entity processing is disabled
- Secure processing feature is enabled

Use `.unsafe(true)` only with trusted SVG input to allow DOCTYPE declarations.

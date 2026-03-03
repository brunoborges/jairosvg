# API Reference

## Static Methods

The `JairoSVG` class provides convenient static methods for common conversions:

### SVG to PNG

```java
// From byte array
byte[] png = JairoSVG.svg2png(byte[] svgData);

// From URL string
byte[] png = JairoSVG.svg2png(String url);
```

### SVG to PDF

```java
byte[] pdf = JairoSVG.svg2pdf(byte[] svgData);
byte[] pdf = JairoSVG.svg2pdf(String url);
```

### SVG to PostScript

```java
byte[] ps = JairoSVG.svg2ps(byte[] svgData);
```

### SVG to SVG (re-render)

```java
byte[] svg = JairoSVG.svg2svg(byte[] svgData);
```

### File to File

```java
JairoSVG.svg2png(Path input, Path output);
JairoSVG.svg2pdf(Path input, Path output);
```

## Builder API

For more control, use the fluent builder:

```java
JairoSVG.builder()
    .fromBytes(svgBytes)       // or .fromFile(path) or .fromUrl(url)
    .dpi(150)                  // Resolution (default: 96)
    .scale(2)                  // Scale factor (default: 1)
    .backgroundColor("#fff")   // Background color
    .parentWidth(800)          // Parent container width
    .parentHeight(600)         // Parent container height
    .outputWidth(400)          // Desired output width
    .outputHeight(300)         // Desired output height
    .negateColors(true)        // Invert colors
    .unsafe(false)             // Allow external entities (default: false)
    .toPng();                  // or .toJpeg(), .toTiff(), .toPdf(), .toPs(), .toSvg(), .toImage()
```

### Output Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `.toPng()` | `byte[]` | PNG image data |
| `.toJpeg()` | `byte[]` | JPEG image data |
| `.toTiff()` | `byte[]` | TIFF image data |
| `.toPdf()` | `byte[]` | PDF document data |
| `.toPs()` | `byte[]` | PostScript data |
| `.toSvg()` | `byte[]` | Re-rendered SVG |
| `.toImage()` | `BufferedImage` | Java2D image object |

### Input Methods

| Method | Description |
|--------|-------------|
| `.fromBytes(byte[])` | SVG content as bytes |
| `.fromFile(Path)` | Read from file path |
| `.fromUrl(String)` | Fetch from URL |

## Thread Safety

All static methods and builder instances are thread-safe. Each conversion creates its own rendering context.

## Error Handling

Methods throw `RuntimeException` wrapping the underlying cause:
- `javax.xml.parsers.ParserConfigurationException` — XML parser setup failure
- `org.xml.sax.SAXException` — Malformed SVG/XML
- `java.io.IOException` — I/O errors

## Security

By default, JairoSVG protects against XML External Entity (XXE) attacks:
- DOCTYPE declarations are disallowed
- External entity processing is disabled
- Secure processing feature is enabled

Use `.unsafe(true)` only with trusted SVG input to allow DOCTYPE declarations.

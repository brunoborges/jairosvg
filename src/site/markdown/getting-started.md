# Getting Started

## Prerequisites

- **Java 25+** with preview features enabled
- **Maven 3.9+** (or use the included `./mvnw` wrapper)

## Installation

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.brunoborges</groupId>
    <artifactId>jairosvg</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.brunoborges:jairosvg:1.0.1'
```

### JBang

For quick scripting:

```bash
jbang --deps io.brunoborges:jairosvg:1.0.1 MyScript.java
```

### CLI (Fat JAR)

```bash
git clone https://github.com/brunoborges/jairosvg.git
cd jairosvg
./mvnw package
java --enable-preview -jar target/jairosvg-1.0.1-cli.jar --help
```

## First Conversion

### Using the API

```java
import io.brunoborges.jairosvg.JairoSVG;
import java.nio.file.Files;
import java.nio.file.Path;

// Read SVG file
byte[] svg = Files.readAllBytes(Path.of("input.svg"));

// Convert to PNG
byte[] png = JairoSVG.svg2png(svg);
Files.write(Path.of("output.png"), png);
```

### Using the Builder

```java
byte[] result = JairoSVG.builder()
    .fromFile(Path.of("input.svg"))
    .dpi(150)
    .scale(2)
    .backgroundColor("#ffffff")
    .toPng();
```

### Using the CLI

```bash
# SVG to PNG
java --enable-preview -jar jairosvg-cli.jar input.svg -o output.png

# SVG to PDF at 2x scale
java --enable-preview -jar jairosvg-cli.jar input.svg -f pdf -s 2 -o output.pdf

# From URL
java --enable-preview -jar jairosvg-cli.jar https://example.com/image.svg -o output.png
```

## Next Steps

- [API Reference](api-reference.html) — Full API documentation
- [CLI Reference](cli.html) — All command-line options
- [Architecture](architecture.html) — How JairoSVG works internally

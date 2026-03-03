# CLI Reference

## Usage

```bash
java --enable-preview -jar jairosvg-cli.jar [OPTIONS] INPUT
```

Where `INPUT` is a path to an SVG file or a URL.

## Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--output FILE` | `-o` | Output filename | stdout |
| `--format FORMAT` | `-f` | Output format: `png`, `jpeg`, `tiff`, `pdf`, `ps`, `svg` | `png` |
| `--dpi DPI` | `-d` | Resolution in dots per inch | `96` |
| `--scale FACTOR` | `-s` | Scale factor | `1` |
| `--background COLOR` | `-b` | Background color (name, hex, rgb) | transparent |
| `--width PIXELS` | `-W` | Parent container width | — |
| `--height PIXELS` | `-H` | Parent container height | — |
| `--output-width PX` | | Desired output width | — |
| `--output-height PX` | | Desired output height | — |
| `--negate-colors` | `-n` | Negate (invert) vector colors | off |
| `--unsafe` | `-u` | Allow external file access | off |
| `--version` | | Print version and exit | |
| `--help` | `-h` | Print help and exit | |

## Examples

### Basic conversion

```bash
# SVG to PNG
java --enable-preview -jar jairosvg-cli.jar logo.svg -o logo.png

# SVG to PDF
java --enable-preview -jar jairosvg-cli.jar diagram.svg -f pdf -o diagram.pdf
```

### Scaling and DPI

```bash
# 2x scale for retina displays
java --enable-preview -jar jairosvg-cli.jar icon.svg -s 2 -o icon@2x.png

# High DPI output
java --enable-preview -jar jairosvg-cli.jar chart.svg -d 300 -o chart-print.png
```

### Background and colors

```bash
# White background
java --enable-preview -jar jairosvg-cli.jar logo.svg -b white -o logo-white.png

# Dark mode (negate colors)
java --enable-preview -jar jairosvg-cli.jar logo.svg -n -o logo-dark.png
```

### From URL

```bash
java --enable-preview -jar jairosvg-cli.jar https://example.com/image.svg -o output.png
```

### Output to stdout (pipe)

```bash
java --enable-preview -jar jairosvg-cli.jar input.svg | display
```

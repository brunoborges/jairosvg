# SVG Rendering Comparison: JairoSVG vs EchoSVG vs CairoSVG

This folder contains side-by-side comparisons of SVG→PNG rendering between **JairoSVG** (this project), **EchoSVG** (a Batik fork), and **CairoSVG** (Python original). Each test case exercises a different SVG feature to highlight rendering differences between the three libraries.

---

## 01 — Basic Shapes
Rectangles, circles, ellipses, and lines with solid fills and strokes.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/01_basic_shapes.svg) | ![JairoSVG](png/jairosvg/01_basic_shapes.png) | ![EchoSVG](png/echosvg/01_basic_shapes.png) | ![CairoSVG](png/cairosvg/01_basic_shapes.png) |

## 02 — Gradients
Linear and radial gradients with color stops and spread methods.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/02_gradients.svg) | ![JairoSVG](png/jairosvg/02_gradients.png) | ![EchoSVG](png/echosvg/02_gradients.png) | ![CairoSVG](png/cairosvg/02_gradients.png) |

## 03 — Complex Paths
Cubic/quadratic Bézier curves, arcs, and complex path commands.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/03_complex_paths.svg) | ![JairoSVG](png/jairosvg/03_complex_paths.png) | ![EchoSVG](png/echosvg/03_complex_paths.png) | ![CairoSVG](png/cairosvg/03_complex_paths.png) |

## 04 — Text Rendering
Text rendering with different fonts, sizes, weights, and tspan.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/04_text_rendering.svg) | ![JairoSVG](png/jairosvg/04_text_rendering.png) | ![EchoSVG](png/echosvg/04_text_rendering.png) | ![CairoSVG](png/cairosvg/04_text_rendering.png) |

## 05 — Transforms
Translate, rotate, scale, skewX, and nested group transforms.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/05_transforms.svg) | ![JairoSVG](png/jairosvg/05_transforms.png) | ![EchoSVG](png/echosvg/05_transforms.png) | ![CairoSVG](png/cairosvg/05_transforms.png) |

## 06 — Stroke Styles
Dash arrays, line caps (butt/round/square), and line joins.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/06_stroke_styles.svg) | ![JairoSVG](png/jairosvg/06_stroke_styles.png) | ![EchoSVG](png/echosvg/06_stroke_styles.png) | ![CairoSVG](png/cairosvg/06_stroke_styles.png) |

## 07 — Opacity & Blending
Fill opacity, stroke opacity, and layered element opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/07_opacity_blend.svg) | ![JairoSVG](png/jairosvg/07_opacity_blend.png) | ![EchoSVG](png/echosvg/07_opacity_blend.png) | ![CairoSVG](png/cairosvg/07_opacity_blend.png) |

## 08 — ViewBox & Aspect Ratio
viewBox scaling with different preserveAspectRatio values.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/08_viewbox_aspect.svg) | ![JairoSVG](png/jairosvg/08_viewbox_aspect.png) | ![EchoSVG](png/echosvg/08_viewbox_aspect.png) | ![CairoSVG](png/cairosvg/08_viewbox_aspect.png) |

## 09 — CSS Styling
CSS `<style>` block with class and ID selectors.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/09_css_styling.svg) | ![JairoSVG](png/jairosvg/09_css_styling.png) | ![EchoSVG](png/echosvg/09_css_styling.png) | ![CairoSVG](png/cairosvg/09_css_styling.png) |

## 10 — Use & Defs
`<use>` element references, `<clipPath>`, and `<defs>` reuse.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/10_use_and_defs.svg) | ![JairoSVG](png/jairosvg/10_use_and_defs.png) | ![EchoSVG](png/echosvg/10_use_and_defs.png) | ![CairoSVG](png/cairosvg/10_use_and_defs.png) |

## 11 — Star Polygon
Complex star polygon with fill-rule evenodd.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/11_star_polygon.svg) | ![JairoSVG](png/jairosvg/11_star_polygon.png) | ![EchoSVG](png/echosvg/11_star_polygon.png) | ![CairoSVG](png/cairosvg/11_star_polygon.png) |

## 12 — Nested SVG
Nested `<svg>` elements with independent viewports.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/12_nested_svg.svg) | ![JairoSVG](png/jairosvg/12_nested_svg.png) | ![EchoSVG](png/echosvg/12_nested_svg.png) | ![CairoSVG](png/cairosvg/12_nested_svg.png) |

## 13 — Patterns
Tiled pattern fills: dots, cross-hatch stripes, and grid lines.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/13_patterns.svg) | ![JairoSVG](png/jairosvg/13_patterns.png) | ![EchoSVG](png/echosvg/13_patterns.png) | ![CairoSVG](png/cairosvg/13_patterns.png) |

## 14 — Clip Paths
Star and text clip paths applied to gradient fills.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/14_clip_paths.svg) | ![JairoSVG](png/jairosvg/14_clip_paths.png) | ![EchoSVG](png/echosvg/14_clip_paths.png) | ![CairoSVG](png/cairosvg/14_clip_paths.png) |

## 15 — Masks
Horizontal, vertical, and circular gradient masks with luminance blending.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/15_masks.svg) | ![JairoSVG](png/jairosvg/15_masks.png) | ![EchoSVG](png/echosvg/15_masks.png) | ![CairoSVG](png/cairosvg/15_masks.png) |

## 16 — Markers
Arrow, dot, and square markers on lines, polylines, and curves.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/16_markers.svg) | ![JairoSVG](png/jairosvg/16_markers.png) | ![EchoSVG](png/echosvg/16_markers.png) | ![CairoSVG](png/cairosvg/16_markers.png) |

## 17 — Filters
Gaussian blur and drop-shadow filters on shapes and text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/17_filters.svg) | ![JairoSVG](png/jairosvg/17_filters.png) | ![EchoSVG](png/echosvg/17_filters.png) | ![CairoSVG](png/cairosvg/17_filters.png) |

## 18 — Embedded Images
Base64-encoded PNG images with clipping, transforms, and opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/18_embedded_image.svg) | ![JairoSVG](png/jairosvg/18_embedded_image.png) | ![EchoSVG](png/echosvg/18_embedded_image.png) | ![CairoSVG](png/cairosvg/18_embedded_image.png) |

## 19 — Advanced Text
Multi-span text (tspan), text-decoration, textPath on curves, and rotated text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG |
|:---------:|:--------:|:-------:|:--------:|
| [SVG](svg/19_text_advanced.svg) | ![JairoSVG](png/jairosvg/19_text_advanced.png) | ![EchoSVG](png/echosvg/19_text_advanced.png) | ![CairoSVG](png/cairosvg/19_text_advanced.png) |

---

## Regenerating

Prerequisites: JBang, Java 25+, Python 3 with CairoSVG (`python3 -m pip install cairosvg`), JairoSVG installed in local Maven repo.

```bash
./mvnw install -DskipTests
jbang comparison/generate.java
```

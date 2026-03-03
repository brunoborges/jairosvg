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

---

## Regenerating

Prerequisites: JBang, Java 25+, Python 3 with CairoSVG (`python3 -m pip install cairosvg`), JairoSVG installed in local Maven repo.

```bash
./mvnw install -DskipTests
jbang comparison/generate.java
```

# Visual Rendering Comparison

Side-by-side visual comparison of 42 SVG test cases across all four libraries.

### 01 — Basic Shapes

Rectangles, circles, ellipses, and lines with solid fills and strokes.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/01_basic_shapes.svg) | ![JairoSVG](png/jairosvg/01_basic_shapes.png) | ![EchoSVG](png/echosvg/01_basic_shapes.png) | ![CairoSVG](png/cairosvg/01_basic_shapes.png) | ![JSVG](png/jsvg/01_basic_shapes.png) |

### 02 — Gradients

Linear and radial gradients with color stops and spread methods.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/02_gradients.svg) | ![JairoSVG](png/jairosvg/02_gradients.png) | ![EchoSVG](png/echosvg/02_gradients.png) | ![CairoSVG](png/cairosvg/02_gradients.png) | ![JSVG](png/jsvg/02_gradients.png) |

### 03 — Complex Paths

Cubic/quadratic Bézier curves, arcs, and complex path commands.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/03_complex_paths.svg) | ![JairoSVG](png/jairosvg/03_complex_paths.png) | ![EchoSVG](png/echosvg/03_complex_paths.png) | ![CairoSVG](png/cairosvg/03_complex_paths.png) | ![JSVG](png/jsvg/03_complex_paths.png) |

### 04 — Text Rendering

Text rendering with different fonts, sizes, weights, and tspan.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/04_text_rendering.svg) | ![JairoSVG](png/jairosvg/04_text_rendering.png) | ![EchoSVG](png/echosvg/04_text_rendering.png) | ![CairoSVG](png/cairosvg/04_text_rendering.png) | ![JSVG](png/jsvg/04_text_rendering.png) |

### 05 — Transforms

Translate, rotate, scale, skewX, and nested group transforms.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/05_transforms.svg) | ![JairoSVG](png/jairosvg/05_transforms.png) | ![EchoSVG](png/echosvg/05_transforms.png) | ![CairoSVG](png/cairosvg/05_transforms.png) | ![JSVG](png/jsvg/05_transforms.png) |

### 06 — Stroke Styles

Dash arrays, line caps (butt/round/square), and line joins.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/06_stroke_styles.svg) | ![JairoSVG](png/jairosvg/06_stroke_styles.png) | ![EchoSVG](png/echosvg/06_stroke_styles.png) | ![CairoSVG](png/cairosvg/06_stroke_styles.png) | ![JSVG](png/jsvg/06_stroke_styles.png) |

### 07 — Opacity & Blending

Fill opacity, stroke opacity, and layered element opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/07_opacity_blend.svg) | ![JairoSVG](png/jairosvg/07_opacity_blend.png) | ![EchoSVG](png/echosvg/07_opacity_blend.png) | ![CairoSVG](png/cairosvg/07_opacity_blend.png) | ![JSVG](png/jsvg/07_opacity_blend.png) |

### 08 — ViewBox & Aspect Ratio

viewBox scaling with different preserveAspectRatio values.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/08_viewbox_aspect.svg) | ![JairoSVG](png/jairosvg/08_viewbox_aspect.png) | ![EchoSVG](png/echosvg/08_viewbox_aspect.png) | ![CairoSVG](png/cairosvg/08_viewbox_aspect.png) | ![JSVG](png/jsvg/08_viewbox_aspect.png) |

### 09 — CSS Styling

CSS `<style>` block with class and ID selectors.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/09_css_styling.svg) | ![JairoSVG](png/jairosvg/09_css_styling.png) | ![EchoSVG](png/echosvg/09_css_styling.png) | ![CairoSVG](png/cairosvg/09_css_styling.png) | ![JSVG](png/jsvg/09_css_styling.png) |

### 10 — Use & Defs

`<use>` element references, `<clipPath>`, and `<defs>` reuse.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/10_use_and_defs.svg) | ![JairoSVG](png/jairosvg/10_use_and_defs.png) | ![EchoSVG](png/echosvg/10_use_and_defs.png) | ![CairoSVG](png/cairosvg/10_use_and_defs.png) | ![JSVG](png/jsvg/10_use_and_defs.png) |

### 11 — Star Polygon

Complex star polygon with fill-rule evenodd.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/11_star_polygon.svg) | ![JairoSVG](png/jairosvg/11_star_polygon.png) | ![EchoSVG](png/echosvg/11_star_polygon.png) | ![CairoSVG](png/cairosvg/11_star_polygon.png) | ![JSVG](png/jsvg/11_star_polygon.png) |

### 12 — Nested SVG

Nested `<svg>` elements with independent viewports.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/12_nested_svg.svg) | ![JairoSVG](png/jairosvg/12_nested_svg.png) | ![EchoSVG](png/echosvg/12_nested_svg.png) | ![CairoSVG](png/cairosvg/12_nested_svg.png) | ![JSVG](png/jsvg/12_nested_svg.png) |

### 13 — Patterns

Tiled pattern fills: dots, cross-hatch stripes, and grid lines.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/13_patterns.svg) | ![JairoSVG](png/jairosvg/13_patterns.png) | ![EchoSVG](png/echosvg/13_patterns.png) | ![CairoSVG](png/cairosvg/13_patterns.png) | ![JSVG](png/jsvg/13_patterns.png) |

### 14 — Clip Paths

Star and text clip paths applied to gradient fills.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/14_clip_paths.svg) | ![JairoSVG](png/jairosvg/14_clip_paths.png) | ![EchoSVG](png/echosvg/14_clip_paths.png) | ![CairoSVG](png/cairosvg/14_clip_paths.png) | ![JSVG](png/jsvg/14_clip_paths.png) |

### 15 — Masks

Horizontal, vertical, and circular gradient masks with luminance blending.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/15_masks.svg) | ![JairoSVG](png/jairosvg/15_masks.png) | ![EchoSVG](png/echosvg/15_masks.png) | ![CairoSVG](png/cairosvg/15_masks.png) | ![JSVG](png/jsvg/15_masks.png) |

### 16 — Markers

Arrow, dot, and square markers on lines, polylines, and curves.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/16_markers.svg) | ![JairoSVG](png/jairosvg/16_markers.png) | ![EchoSVG](png/echosvg/16_markers.png) | ![CairoSVG](png/cairosvg/16_markers.png) | ![JSVG](png/jsvg/16_markers.png) |

### 17 — Filters

Gaussian blur and drop-shadow filters on shapes and text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/17_filters.svg) | ![JairoSVG](png/jairosvg/17_filters.png) | ![EchoSVG](png/echosvg/17_filters.png) | ![CairoSVG](png/cairosvg/17_filters.png) | ![JSVG](png/jsvg/17_filters.png) |

### 18 — Embedded Images

Base64-encoded PNG images with clipping, transforms, and opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/18_embedded_image.svg) | ![JairoSVG](png/jairosvg/18_embedded_image.png) | ![EchoSVG](png/echosvg/18_embedded_image.png) | ![CairoSVG](png/cairosvg/18_embedded_image.png) | ![JSVG](png/jsvg/18_embedded_image.png) |

### 19 — Advanced Text

Multi-span text (tspan), text-decoration, textPath on curves, and rotated text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/19_text_advanced.svg) | ![JairoSVG](png/jairosvg/19_text_advanced.png) | ![EchoSVG](png/echosvg/19_text_advanced.png) | ![CairoSVG](png/cairosvg/19_text_advanced.png) | ![JSVG](png/jsvg/19_text_advanced.png) |

### 20 — feBlend Modes

feBlend modes: normal, multiply, screen, darken, and lighten.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/20_fe_blend_modes.svg) | ![JairoSVG](png/jairosvg/20_fe_blend_modes.png) | ![EchoSVG](png/echosvg/20_fe_blend_modes.png) | ![CairoSVG](png/cairosvg/20_fe_blend_modes.png) | ![JSVG](png/jsvg/20_fe_blend_modes.png) |

### 20_fe_tile

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/20_fe_tile.svg) | ![JairoSVG](png/jairosvg/20_fe_tile.png) | ![EchoSVG](png/echosvg/20_fe_tile.png) | ![CairoSVG](png/cairosvg/20_fe_tile.png) | ![JSVG](png/jsvg/20_fe_tile.png) |

### 20_feimage_data_uri

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/20_feimage_data_uri.svg) | ![JairoSVG](png/jairosvg/20_feimage_data_uri.png) | ![EchoSVG](png/echosvg/20_feimage_data_uri.png) | ![CairoSVG](png/cairosvg/20_feimage_data_uri.png) | ![JSVG](png/jsvg/20_feimage_data_uri.png) |

### 21_feimage_inline_ref

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/21_feimage_inline_ref.svg) | ![JairoSVG](png/jairosvg/21_feimage_inline_ref.png) | ![EchoSVG](png/echosvg/21_feimage_inline_ref.png) | ![CairoSVG](png/cairosvg/21_feimage_inline_ref.png) | ![JSVG](png/jsvg/21_feimage_inline_ref.png) |

### 22_localized_masks

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/22_localized_masks.svg) | ![JairoSVG](png/jairosvg/22_localized_masks.png) | ![EchoSVG](png/echosvg/22_localized_masks.png) | ![CairoSVG](png/cairosvg/22_localized_masks.png) | ![JSVG](png/jsvg/22_localized_masks.png) |

### 23 — SVG Fonts

Custom SVG font with glyph paths and missing-glyph fallback.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/23_svg_fonts.svg) | ![JairoSVG](png/jairosvg/23_svg_fonts.png) | ![EchoSVG](png/echosvg/23_svg_fonts.png) | ![CairoSVG](png/cairosvg/23_svg_fonts.png) | ![JSVG](png/jsvg/23_svg_fonts.png) |

### 24 — Symbol + Use

Reusable `<symbol>` elements instantiated with `<use>` at different sizes and positions.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/24_symbol_use.svg) | ![JairoSVG](png/jairosvg/24_symbol_use.png) | ![EchoSVG](png/echosvg/24_symbol_use.png) | ![CairoSVG](png/cairosvg/24_symbol_use.png) | ![JSVG](png/jsvg/24_symbol_use.png) |

### 25 — Switch & requiredFeatures

`<switch>` element with requiredFeatures and systemLanguage conditional rendering.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/25_switch_features.svg) | ![JairoSVG](png/jairosvg/25_switch_features.png) | ![EchoSVG](png/echosvg/25_switch_features.png) | ![CairoSVG](png/cairosvg/25_switch_features.png) | ![JSVG](png/jsvg/25_switch_features.png) |

### 26 — CSS Custom Properties

CSS custom properties with `var()` function and fallback values.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/26_css_variables.svg) | ![JairoSVG](png/jairosvg/26_css_variables.png) | ![EchoSVG](png/echosvg/26_css_variables.png) | — | ![JSVG](png/jsvg/26_css_variables.png) |

### 27 — currentColor

`currentColor` keyword for fill, stroke, and gradient stops with nested inheritance.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/27_current_color.svg) | ![JairoSVG](png/jairosvg/27_current_color.png) | ![EchoSVG](png/echosvg/27_current_color.png) | ![CairoSVG](png/cairosvg/27_current_color.png) | ![JSVG](png/jsvg/27_current_color.png) |

### 28 — Display & Visibility

`display:none` vs `visibility:hidden` behavior, group suppression, and child override.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/28_display_visibility.svg) | ![JairoSVG](png/jairosvg/28_display_visibility.png) | ![EchoSVG](png/echosvg/28_display_visibility.png) | ![CairoSVG](png/cairosvg/28_display_visibility.png) | ![JSVG](png/jsvg/28_display_visibility.png) |

### 29 — Nested SVG Overflow

Nested `<svg>` elements with `overflow` values: hidden, scroll, visible, and auto.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/29_nested_overflow.svg) | ![JairoSVG](png/jairosvg/29_nested_overflow.png) | ![EchoSVG](png/echosvg/29_nested_overflow.png) | ![CairoSVG](png/cairosvg/29_nested_overflow.png) | ![JSVG](png/jsvg/29_nested_overflow.png) |

### 30 — Advanced Strokes

`stroke-dashoffset` phase shifting and `stroke-miterlimit` miter-to-bevel fallback.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/30_stroke_advanced.svg) | ![JairoSVG](png/jairosvg/30_stroke_advanced.png) | ![EchoSVG](png/echosvg/30_stroke_advanced.png) | ![CairoSVG](png/cairosvg/30_stroke_advanced.png) | ![JSVG](png/jsvg/30_stroke_advanced.png) |

### 31 — Pattern Transforms

`patternTransform` with scale, rotate, translate, and combined transforms.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/31_pattern_transforms.svg) | ![JairoSVG](png/jairosvg/31_pattern_transforms.png) | ![EchoSVG](png/echosvg/31_pattern_transforms.png) | ![CairoSVG](png/cairosvg/31_pattern_transforms.png) | ![JSVG](png/jsvg/31_pattern_transforms.png) |

### 32 — Advanced Gradients

Gradient `spreadMethod` (reflect/repeat/pad), `fx`/`fy` focus, `href` inheritance, and `userSpaceOnUse`.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/32_gradient_advanced.svg) | ![JairoSVG](png/jairosvg/32_gradient_advanced.png) | ![EchoSVG](png/echosvg/32_gradient_advanced.png) | ![CairoSVG](png/cairosvg/32_gradient_advanced.png) | ![JSVG](png/jsvg/32_gradient_advanced.png) |

### 33 — feMerge & feOffset

`feMerge` for compositing layers and `feOffset` for position shifting with shadow effects.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/33_filter_merge_offset.svg) | ![JairoSVG](png/jairosvg/33_filter_merge_offset.png) | ![EchoSVG](png/echosvg/33_filter_merge_offset.png) | ![CairoSVG](png/cairosvg/33_filter_merge_offset.png) | ![JSVG](png/jsvg/33_filter_merge_offset.png) |

### 34_fe_color_matrix

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/34_fe_color_matrix.svg) | ![JairoSVG](png/jairosvg/34_fe_color_matrix.png) | ![EchoSVG](png/echosvg/34_fe_color_matrix.png) | ![CairoSVG](png/cairosvg/34_fe_color_matrix.png) | ![JSVG](png/jsvg/34_fe_color_matrix.png) |

### 35_fe_morphology

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/35_fe_morphology.svg) | ![JairoSVG](png/jairosvg/35_fe_morphology.png) | ![EchoSVG](png/echosvg/35_fe_morphology.png) | ![CairoSVG](png/cairosvg/35_fe_morphology.png) | ![JSVG](png/jsvg/35_fe_morphology.png) |

### 36_fe_turbulence

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/36_fe_turbulence.svg) | ![JairoSVG](png/jairosvg/36_fe_turbulence.png) | ![EchoSVG](png/echosvg/36_fe_turbulence.png) | ![CairoSVG](png/cairosvg/36_fe_turbulence.png) | ![JSVG](png/jsvg/36_fe_turbulence.png) |

### 37_fe_displacement_map

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/37_fe_displacement_map.svg) | ![JairoSVG](png/jairosvg/37_fe_displacement_map.png) | ![EchoSVG](png/echosvg/37_fe_displacement_map.png) | ![CairoSVG](png/cairosvg/37_fe_displacement_map.png) | ![JSVG](png/jsvg/37_fe_displacement_map.png) |

### 38_fe_lighting

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/38_fe_lighting.svg) | ![JairoSVG](png/jairosvg/38_fe_lighting.png) | ![EchoSVG](png/echosvg/38_fe_lighting.png) | ![CairoSVG](png/cairosvg/38_fe_lighting.png) | ![JSVG](png/jsvg/38_fe_lighting.png) |

### 39_fe_convolve_matrix

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/39_fe_convolve_matrix.svg) | ![JairoSVG](png/jairosvg/39_fe_convolve_matrix.png) | ![EchoSVG](png/echosvg/39_fe_convolve_matrix.png) | ![CairoSVG](png/cairosvg/39_fe_convolve_matrix.png) | ![JSVG](png/jsvg/39_fe_convolve_matrix.png) |

### 40_fe_component_transfer

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| [SVG](svg/40_fe_component_transfer.svg) | ![JairoSVG](png/jairosvg/40_fe_component_transfer.png) | ![EchoSVG](png/echosvg/40_fe_component_transfer.png) | ![CairoSVG](png/cairosvg/40_fe_component_transfer.png) | ![JSVG](png/jsvg/40_fe_component_transfer.png) |

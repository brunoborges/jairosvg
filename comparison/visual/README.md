# Visual Rendering Comparison

Side-by-side visual comparison of 42 SVG test cases across all four libraries.

> **Note:** The **Input SVG** column is rendered live by your browser's built-in SVG engine. Use it as a reference to compare each library's PNG output against what a modern browser produces.

### 01_basic_shapes

Rectangles, circles, ellipses, and lines with solid fills and strokes.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/01_basic_shapes.svg) | ![JairoSVG](png/jairosvg/01_basic_shapes.png) | ![EchoSVG](png/echosvg/01_basic_shapes.png) | ![CairoSVG](png/cairosvg/01_basic_shapes.png) | ![JSVG](png/jsvg/01_basic_shapes.png) |
| | 6,718 bytes ✅ | 8,159 bytes | 8,920 bytes | 7,031 bytes |

### 02_gradients

Linear and radial gradients with color stops and spread methods.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/02_gradients.svg) | ![JairoSVG](png/jairosvg/02_gradients.png) | ![EchoSVG](png/echosvg/02_gradients.png) | ![CairoSVG](png/cairosvg/02_gradients.png) | ![JSVG](png/jsvg/02_gradients.png) |
| | 25,554 bytes | 25,018 bytes | 23,637 bytes ✅ | 26,410 bytes |

### 03_complex_paths

Cubic/quadratic Bézier curves, arcs, and complex path commands.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/03_complex_paths.svg) | ![JairoSVG](png/jairosvg/03_complex_paths.png) | ![EchoSVG](png/echosvg/03_complex_paths.png) | ![CairoSVG](png/cairosvg/03_complex_paths.png) | ![JSVG](png/jsvg/03_complex_paths.png) |
| | 12,657 bytes ✅ | 16,936 bytes | 15,633 bytes | 12,730 bytes |

### 04_text_rendering

Text rendering with different fonts, sizes, weights, and tspan.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/04_text_rendering.svg) | ![JairoSVG](png/jairosvg/04_text_rendering.png) | ![EchoSVG](png/echosvg/04_text_rendering.png) | ![CairoSVG](png/cairosvg/04_text_rendering.png) | ![JSVG](png/jsvg/04_text_rendering.png) |
| | 13,276 bytes ✅ | 19,125 bytes | 16,317 bytes | 15,626 bytes |

### 05_transforms

Translate, rotate, scale, skewX, and nested group transforms.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/05_transforms.svg) | ![JairoSVG](png/jairosvg/05_transforms.png) | ![EchoSVG](png/echosvg/05_transforms.png) | ![CairoSVG](png/cairosvg/05_transforms.png) | ![JSVG](png/jsvg/05_transforms.png) |
| | 5,461 bytes | 5,261 bytes ✅ | 6,001 bytes | 5,827 bytes |

### 06_stroke_styles

Dash arrays, line caps (butt/round/square), and line joins.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/06_stroke_styles.svg) | ![JairoSVG](png/jairosvg/06_stroke_styles.png) | ![EchoSVG](png/echosvg/06_stroke_styles.png) | ![CairoSVG](png/cairosvg/06_stroke_styles.png) | ![JSVG](png/jsvg/06_stroke_styles.png) |
| | 3,363 bytes ✅ | 5,038 bytes | 4,478 bytes | 4,074 bytes |

### 07_opacity_blend

Fill opacity, stroke opacity, and layered element opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/07_opacity_blend.svg) | ![JairoSVG](png/jairosvg/07_opacity_blend.png) | ![EchoSVG](png/echosvg/07_opacity_blend.png) | ![CairoSVG](png/cairosvg/07_opacity_blend.png) | ![JSVG](png/jsvg/07_opacity_blend.png) |
| | 8,409 bytes ✅ | 10,201 bytes | 9,853 bytes | 8,788 bytes |

### 08_viewbox_aspect

viewBox scaling with different preserveAspectRatio values.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/08_viewbox_aspect.svg) | ![JairoSVG](png/jairosvg/08_viewbox_aspect.png) | ![EchoSVG](png/echosvg/08_viewbox_aspect.png) | ![CairoSVG](png/cairosvg/08_viewbox_aspect.png) | ![JSVG](png/jsvg/08_viewbox_aspect.png) |
| | 10,492 bytes ✅ | 12,769 bytes | 11,444 bytes | 12,147 bytes |

### 09_css_styling

CSS `<style>` block with class and ID selectors.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/09_css_styling.svg) | ![JairoSVG](png/jairosvg/09_css_styling.png) | ![EchoSVG](png/echosvg/09_css_styling.png) | ![CairoSVG](png/cairosvg/09_css_styling.png) | ![JSVG](png/jsvg/09_css_styling.png) |
| | 7,755 bytes ✅ | 11,144 bytes | 10,816 bytes | 8,653 bytes |

### 10_use_and_defs

`<use>` element references, `<clipPath>`, and `<defs>` reuse.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/10_use_and_defs.svg) | ![JairoSVG](png/jairosvg/10_use_and_defs.png) | ![EchoSVG](png/echosvg/10_use_and_defs.png) | ![CairoSVG](png/cairosvg/10_use_and_defs.png) | ![JSVG](png/jsvg/10_use_and_defs.png) |
| | 5,448 bytes ✅ | 6,122 bytes | 9,712 bytes | 6,144 bytes |

### 11_star_polygon

Complex star polygon with fill-rule evenodd.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/11_star_polygon.svg) | ![JairoSVG](png/jairosvg/11_star_polygon.png) | ![EchoSVG](png/echosvg/11_star_polygon.png) | ![CairoSVG](png/cairosvg/11_star_polygon.png) | ![JSVG](png/jsvg/11_star_polygon.png) |
| | 6,228 bytes ✅ | 8,862 bytes | 8,911 bytes | 6,455 bytes |

### 12_nested_svg

Nested `<svg>` elements with independent viewports.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/12_nested_svg.svg) | ![JairoSVG](png/jairosvg/12_nested_svg.png) | ![EchoSVG](png/echosvg/12_nested_svg.png) | ![CairoSVG](png/cairosvg/12_nested_svg.png) | ![JSVG](png/jsvg/12_nested_svg.png) |
| | 10,926 bytes ✅ | 12,522 bytes | 11,880 bytes | 12,101 bytes |

### 13_patterns

Tiled pattern fills: dots, cross-hatch stripes, and grid lines.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/13_patterns.svg) | ![JairoSVG](png/jairosvg/13_patterns.png) | ![EchoSVG](png/echosvg/13_patterns.png) | ![CairoSVG](png/cairosvg/13_patterns.png) | ![JSVG](png/jsvg/13_patterns.png) |
| | 9,532 bytes ✅ | 11,832 bytes | 11,095 bytes | 11,043 bytes |

### 14_clip_paths

Star and text clip paths applied to gradient fills.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/14_clip_paths.svg) | ![JairoSVG](png/jairosvg/14_clip_paths.png) | ![EchoSVG](png/echosvg/14_clip_paths.png) | ![CairoSVG](png/cairosvg/14_clip_paths.png) | ![JSVG](png/jsvg/14_clip_paths.png) |
| | 9,342 bytes ✅ | 10,558 bytes | 13,552 bytes | 10,253 bytes |

### 15_masks

Horizontal, vertical, and circular gradient masks with luminance blending.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/15_masks.svg) | ![JairoSVG](png/jairosvg/15_masks.png) | ![EchoSVG](png/echosvg/15_masks.png) | ![CairoSVG](png/cairosvg/15_masks.png) | ![JSVG](png/jsvg/15_masks.png) |
| | 5,692 bytes | 5,566 bytes | 1,161 bytes ✅ | 6,209 bytes |

### 16_markers

Arrow, dot, and square markers on lines, polylines, and curves.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/16_markers.svg) | ![JairoSVG](png/jairosvg/16_markers.png) | ![EchoSVG](png/echosvg/16_markers.png) | ![CairoSVG](png/cairosvg/16_markers.png) | ![JSVG](png/jsvg/16_markers.png) |
| | 9,796 bytes ✅ | 12,642 bytes | 12,655 bytes | 10,041 bytes |

### 17_filters

Gaussian blur and drop-shadow filters on shapes and text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/17_filters.svg) | ![JairoSVG](png/jairosvg/17_filters.png) | ![EchoSVG](png/echosvg/17_filters.png) | ![CairoSVG](png/cairosvg/17_filters.png) | ![JSVG](png/jsvg/17_filters.png) |
| | 28,934 bytes | 24,063 bytes | 8,520 bytes ✅ | 32,346 bytes |

### 18_embedded_image

Base64-encoded PNG images with clipping, transforms, and opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/18_embedded_image.svg) | ![JairoSVG](png/jairosvg/18_embedded_image.png) | ![EchoSVG](png/echosvg/18_embedded_image.png) | ![CairoSVG](png/cairosvg/18_embedded_image.png) | ![JSVG](png/jsvg/18_embedded_image.png) |
| | 9,432 bytes ✅ | 11,994 bytes | 21,228 bytes | 11,642 bytes |

### 19_text_advanced

Multi-span text (tspan), text-decoration, textPath on curves, and rotated text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/19_text_advanced.svg) | ![JairoSVG](png/jairosvg/19_text_advanced.png) | ![EchoSVG](png/echosvg/19_text_advanced.png) | ![CairoSVG](png/cairosvg/19_text_advanced.png) | ![JSVG](png/jsvg/19_text_advanced.png) |
| | 18,801 bytes ✅ | 26,256 bytes | 23,864 bytes | 19,756 bytes |

### 20_fe_blend_modes

feBlend modes: normal, multiply, screen, darken, and lighten.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/20_fe_blend_modes.svg) | ![JairoSVG](png/jairosvg/20_fe_blend_modes.png) | ![EchoSVG](png/echosvg/20_fe_blend_modes.png) | ![CairoSVG](png/cairosvg/20_fe_blend_modes.png) | ![JSVG](png/jsvg/20_fe_blend_modes.png) |
| | 12,005 bytes ✅ | 16,216 bytes | 12,505 bytes | 15,773 bytes |

### 21_fe_tile

`feTile` filter primitive: repeating input across the filter region.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/21_fe_tile.svg) | ![JairoSVG](png/jairosvg/21_fe_tile.png) | ![EchoSVG](png/echosvg/21_fe_tile.png) | ![CairoSVG](png/cairosvg/21_fe_tile.png) | ![JSVG](png/jsvg/21_fe_tile.png) |
| | 1,456 bytes ✅ | 2,009 bytes | 1,768 bytes | 1,489 bytes |

### 22_feimage_data_uri

`feImage` with data-URI PNG source.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/22_feimage_data_uri.svg) | ![JairoSVG](png/jairosvg/22_feimage_data_uri.png) | ![EchoSVG](png/echosvg/22_feimage_data_uri.png) | ![CairoSVG](png/cairosvg/22_feimage_data_uri.png) | ![JSVG](png/jsvg/22_feimage_data_uri.png) |
| | 2,633 bytes ✅ | 4,406 bytes | 3,206 bytes | 3,639 bytes |

### 23_feimage_inline_ref

`feImage` referencing an inline SVG element by fragment ID.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/23_feimage_inline_ref.svg) | ![JairoSVG](png/jairosvg/23_feimage_inline_ref.png) | ![EchoSVG](png/echosvg/23_feimage_inline_ref.png) | ![CairoSVG](png/cairosvg/23_feimage_inline_ref.png) | ![JSVG](png/jsvg/23_feimage_inline_ref.png) |
| | 2,702 bytes ✅ | 3,642 bytes | 4,903 bytes | 4,380 bytes |

### 24_localized_masks

Masks with localized coordinate systems and gradient fills.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/24_localized_masks.svg) | ![JairoSVG](png/jairosvg/24_localized_masks.png) | ![EchoSVG](png/echosvg/24_localized_masks.png) | ![CairoSVG](png/cairosvg/24_localized_masks.png) | ![JSVG](png/jsvg/24_localized_masks.png) |
| | 18,389 bytes | 17,868 bytes | 13,218 bytes ✅ | 20,239 bytes |

### 25_svg_fonts

Custom SVG font with glyph paths and missing-glyph fallback.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/25_svg_fonts.svg) | ![JairoSVG](png/jairosvg/25_svg_fonts.png) | ![EchoSVG](png/echosvg/25_svg_fonts.png) | ![CairoSVG](png/cairosvg/25_svg_fonts.png) | ![JSVG](png/jsvg/25_svg_fonts.png) |
| | 10,331 bytes ✅ | 14,274 bytes | 15,233 bytes | 12,607 bytes |

### 26_symbol_use

Reusable `<symbol>` elements instantiated with `<use>` at different sizes and positions.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/26_symbol_use.svg) | ![JairoSVG](png/jairosvg/26_symbol_use.png) | ![EchoSVG](png/echosvg/26_symbol_use.png) | ![CairoSVG](png/cairosvg/26_symbol_use.png) | ![JSVG](png/jsvg/26_symbol_use.png) |
| | 15,665 bytes ✅ | 24,513 bytes | 21,625 bytes | 18,260 bytes |

### 27_switch_features

`<switch>` element with requiredFeatures and systemLanguage conditional rendering.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/27_switch_features.svg) | ![JairoSVG](png/jairosvg/27_switch_features.png) | ![EchoSVG](png/echosvg/27_switch_features.png) | ![CairoSVG](png/cairosvg/27_switch_features.png) | ![JSVG](png/jsvg/27_switch_features.png) |
| | 11,535 bytes | 18,040 bytes | 14,493 bytes | 8,503 bytes ✅ |

### 28_css_variables

CSS custom properties with `var()` function and fallback values.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/28_css_variables.svg) | ![JairoSVG](png/jairosvg/28_css_variables.png) | ![EchoSVG](png/echosvg/28_css_variables.png) | — | ![JSVG](png/jsvg/28_css_variables.png) |
| | 11,574 bytes ✅ | 17,016 bytes | — | 12,509 bytes |

### 29_current_color

`currentColor` keyword for fill, stroke, and gradient stops with nested inheritance.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/29_current_color.svg) | ![JairoSVG](png/jairosvg/29_current_color.png) | ![EchoSVG](png/echosvg/29_current_color.png) | ![CairoSVG](png/cairosvg/29_current_color.png) | ![JSVG](png/jsvg/29_current_color.png) |
| | 10,037 bytes ✅ | 14,642 bytes | 11,006 bytes | 13,030 bytes |

### 30_display_visibility

`display:none` vs `visibility:hidden` behavior, group suppression, and child override.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/30_display_visibility.svg) | ![JairoSVG](png/jairosvg/30_display_visibility.png) | ![EchoSVG](png/echosvg/30_display_visibility.png) | ![CairoSVG](png/cairosvg/30_display_visibility.png) | ![JSVG](png/jsvg/30_display_visibility.png) |
| | 11,009 bytes ✅ | 17,473 bytes | 13,218 bytes | 14,263 bytes |

### 31_nested_overflow

Nested `<svg>` elements with `overflow` values: hidden, scroll, visible, and auto.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/31_nested_overflow.svg) | ![JairoSVG](png/jairosvg/31_nested_overflow.png) | ![EchoSVG](png/echosvg/31_nested_overflow.png) | ![CairoSVG](png/cairosvg/31_nested_overflow.png) | ![JSVG](png/jsvg/31_nested_overflow.png) |
| | 11,273 bytes ✅ | 16,322 bytes | 13,738 bytes | 13,737 bytes |

### 32_stroke_advanced

`stroke-dashoffset` phase shifting and `stroke-miterlimit` miter-to-bevel fallback.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/32_stroke_advanced.svg) | ![JairoSVG](png/jairosvg/32_stroke_advanced.png) | ![EchoSVG](png/echosvg/32_stroke_advanced.png) | ![CairoSVG](png/cairosvg/32_stroke_advanced.png) | ![JSVG](png/jsvg/32_stroke_advanced.png) |
| | 9,287 bytes ✅ | 14,507 bytes | 12,246 bytes | 11,702 bytes |

### 33_pattern_transforms

`patternTransform` with scale, rotate, translate, and combined transforms.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/33_pattern_transforms.svg) | ![JairoSVG](png/jairosvg/33_pattern_transforms.png) | ![EchoSVG](png/echosvg/33_pattern_transforms.png) | ![CairoSVG](png/cairosvg/33_pattern_transforms.png) | ![JSVG](png/jsvg/33_pattern_transforms.png) |
| | 9,052 bytes ✅ | 16,101 bytes | 13,061 bytes | 16,273 bytes |

### 34_gradient_advanced

Gradient `spreadMethod` (reflect/repeat/pad), `fx`/`fy` focus, `href` inheritance, and `userSpaceOnUse`.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/34_gradient_advanced.svg) | ![JairoSVG](png/jairosvg/34_gradient_advanced.png) | ![EchoSVG](png/echosvg/34_gradient_advanced.png) | ![CairoSVG](png/cairosvg/34_gradient_advanced.png) | ![JSVG](png/jsvg/34_gradient_advanced.png) |
| | 31,339 bytes | 35,647 bytes | 30,960 bytes ✅ | 35,070 bytes |

### 35_filter_merge_offset

`feMerge` for compositing layers and `feOffset` for position shifting with shadow effects.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/35_filter_merge_offset.svg) | ![JairoSVG](png/jairosvg/35_filter_merge_offset.png) | ![EchoSVG](png/echosvg/35_filter_merge_offset.png) | ![CairoSVG](png/cairosvg/35_filter_merge_offset.png) | ![JSVG](png/jsvg/35_filter_merge_offset.png) |
| | 9,348 bytes ✅ | 14,868 bytes | 14,168 bytes | 12,184 bytes |

### 36_fe_color_matrix

`feColorMatrix` with type matrix, saturate, hueRotate, and luminanceToAlpha.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/36_fe_color_matrix.svg) | ![JairoSVG](png/jairosvg/36_fe_color_matrix.png) | ![EchoSVG](png/echosvg/36_fe_color_matrix.png) | ![CairoSVG](png/cairosvg/36_fe_color_matrix.png) | ![JSVG](png/jsvg/36_fe_color_matrix.png) |
| | 13,131 bytes | 15,605 bytes | 10,152 bytes ✅ | 14,503 bytes |

### 37_fe_morphology

`feMorphology` erode and dilate operators on text, shapes, and circles.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/37_fe_morphology.svg) | ![JairoSVG](png/jairosvg/37_fe_morphology.png) | ![EchoSVG](png/echosvg/37_fe_morphology.png) | ![CairoSVG](png/cairosvg/37_fe_morphology.png) | ![JSVG](png/jsvg/37_fe_morphology.png) |
| | 9,850 bytes | 13,914 bytes | 10,313 bytes | 9,544 bytes ✅ |

### 38_fe_turbulence

`feTurbulence` fractalNoise and turbulence types with varying frequency and octaves.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/38_fe_turbulence.svg) | ![JairoSVG](png/jairosvg/38_fe_turbulence.png) | ![EchoSVG](png/echosvg/38_fe_turbulence.png) | ![CairoSVG](png/cairosvg/38_fe_turbulence.png) | ![JSVG](png/jsvg/38_fe_turbulence.png) |
| | 77,590 bytes | 64,097 bytes | 9,371 bytes ✅ | 76,437 bytes |

### 39_fe_displacement_map

`feDisplacementMap` distortion using a turbulence displacement source.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/39_fe_displacement_map.svg) | ![JairoSVG](png/jairosvg/39_fe_displacement_map.png) | ![EchoSVG](png/echosvg/39_fe_displacement_map.png) | ![CairoSVG](png/cairosvg/39_fe_displacement_map.png) | ![JSVG](png/jsvg/39_fe_displacement_map.png) |
| | 9,151 bytes ✅ | 17,633 bytes | 9,684 bytes | 10,208 bytes |

### 40_fe_lighting

`feDiffuseLighting` and `feSpecularLighting` with distant and point light sources.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/40_fe_lighting.svg) | ![JairoSVG](png/jairosvg/40_fe_lighting.png) | ![EchoSVG](png/echosvg/40_fe_lighting.png) | ![CairoSVG](png/cairosvg/40_fe_lighting.png) | ![JSVG](png/jsvg/40_fe_lighting.png) |
| | 12,213 bytes | 18,277 bytes | 9,006 bytes ✅ | 10,269 bytes |

### 41_fe_convolve_matrix

`feConvolveMatrix` convolution effects: emboss, edge detection, sharpen, and box blur.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/41_fe_convolve_matrix.svg) | ![JairoSVG](png/jairosvg/41_fe_convolve_matrix.png) | ![EchoSVG](png/echosvg/41_fe_convolve_matrix.png) | ![CairoSVG](png/cairosvg/41_fe_convolve_matrix.png) | ![JSVG](png/jsvg/41_fe_convolve_matrix.png) |
| | 12,104 bytes | 6,858 bytes ✅ | 9,045 bytes | 8,834 bytes |

### 42_fe_component_transfer

`feComponentTransfer` with gamma, discrete, linear, and table transfer functions.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](../svg/42_fe_component_transfer.svg) | ![JairoSVG](png/jairosvg/42_fe_component_transfer.png) | ![EchoSVG](png/echosvg/42_fe_component_transfer.png) | ![CairoSVG](png/cairosvg/42_fe_component_transfer.png) | ![JSVG](png/jsvg/42_fe_component_transfer.png) |
| | 9,118 bytes ✅ | 12,543 bytes | 9,950 bytes | 10,463 bytes |

# Visual Rendering Comparison

Side-by-side visual comparison of 42 SVG test cases across all four libraries.

> **Note:** The **Input SVG** column is rendered live by your browser's built-in SVG engine. Use it as a reference to compare each library's PNG output against what a modern browser produces.

### 01_basic_shapes

Rectangles, circles, ellipses, and lines with solid fills and strokes.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/01_basic_shapes.svg) | ![JairoSVG](visual/png/jairosvg/01_basic_shapes.png) | ![EchoSVG](visual/png/echosvg/01_basic_shapes.png) | ![CairoSVG](visual/png/cairosvg/01_basic_shapes.png) | ![JSVG](visual/png/jsvg/01_basic_shapes.png) |
| **Size** | 6,718 bytes ✅ | 8,159 bytes | 8,920 bytes | 7,031 bytes |
<!-- BEGIN:TIME:01_basic_shapes -->| **Time** | 3.3224 ms | 15.4451 ms | 4.0982 ms | **3.1553 ms** ✅ |<!-- END:TIME:01_basic_shapes -->

### 02_gradients

Linear and radial gradients with color stops and spread methods.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/02_gradients.svg) | ![JairoSVG](visual/png/jairosvg/02_gradients.png) | ![EchoSVG](visual/png/echosvg/02_gradients.png) | ![CairoSVG](visual/png/cairosvg/02_gradients.png) | ![JSVG](visual/png/jsvg/02_gradients.png) |
| **Size** | 25,554 bytes | 25,018 bytes | 23,637 bytes ✅ | 26,410 bytes |
<!-- BEGIN:TIME:02_gradients -->| **Time** | **4.1492 ms** ✅ | 127.9740 ms | 10.5992 ms | 4.1983 ms |<!-- END:TIME:02_gradients -->

### 03_complex_paths

Cubic/quadratic Bézier curves, arcs, and complex path commands.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/03_complex_paths.svg) | ![JairoSVG](visual/png/jairosvg/03_complex_paths.png) | ![EchoSVG](visual/png/echosvg/03_complex_paths.png) | ![CairoSVG](visual/png/cairosvg/03_complex_paths.png) | ![JSVG](visual/png/jsvg/03_complex_paths.png) |
| **Size** | 12,657 bytes ✅ | 16,936 bytes | 15,633 bytes | 12,730 bytes |
<!-- BEGIN:TIME:03_complex_paths -->| **Time** | **4.0290 ms** ✅ | 21.7461 ms | 4.3188 ms | 4.2354 ms |<!-- END:TIME:03_complex_paths -->

### 04_text_rendering

Text rendering with different fonts, sizes, weights, and tspan.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/04_text_rendering.svg) | ![JairoSVG](visual/png/jairosvg/04_text_rendering.png) | ![EchoSVG](visual/png/echosvg/04_text_rendering.png) | ![CairoSVG](visual/png/cairosvg/04_text_rendering.png) | ![JSVG](visual/png/jsvg/04_text_rendering.png) |
| **Size** | 13,276 bytes ✅ | 19,125 bytes | 16,317 bytes | 15,626 bytes |
<!-- BEGIN:TIME:04_text_rendering -->| **Time** | 4.6488 ms | 22.7574 ms | 6.0069 ms | **4.6150 ms** ✅ |<!-- END:TIME:04_text_rendering -->

### 05_transforms

Translate, rotate, scale, skewX, and nested group transforms.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/05_transforms.svg) | ![JairoSVG](visual/png/jairosvg/05_transforms.png) | ![EchoSVG](visual/png/echosvg/05_transforms.png) | ![CairoSVG](visual/png/cairosvg/05_transforms.png) | ![JSVG](visual/png/jsvg/05_transforms.png) |
| **Size** | 5,461 bytes | 5,261 bytes ✅ | 6,001 bytes | 5,827 bytes |
<!-- BEGIN:TIME:05_transforms -->| **Time** | 3.8863 ms | 13.9228 ms | **3.7330 ms** ✅ | 3.9663 ms |<!-- END:TIME:05_transforms -->

### 06_stroke_styles

Dash arrays, line caps (butt/round/square), and line joins.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/06_stroke_styles.svg) | ![JairoSVG](visual/png/jairosvg/06_stroke_styles.png) | ![EchoSVG](visual/png/echosvg/06_stroke_styles.png) | ![CairoSVG](visual/png/cairosvg/06_stroke_styles.png) | ![JSVG](visual/png/jsvg/06_stroke_styles.png) |
| **Size** | 3,363 bytes ✅ | 5,038 bytes | 4,478 bytes | 4,074 bytes |
<!-- BEGIN:TIME:06_stroke_styles -->| **Time** | 3.7028 ms | 12.1811 ms | **3.3127 ms** ✅ | 3.6614 ms |<!-- END:TIME:06_stroke_styles -->

### 07_opacity_blend

Fill opacity, stroke opacity, and layered element opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/07_opacity_blend.svg) | ![JairoSVG](visual/png/jairosvg/07_opacity_blend.png) | ![EchoSVG](visual/png/echosvg/07_opacity_blend.png) | ![CairoSVG](visual/png/cairosvg/07_opacity_blend.png) | ![JSVG](visual/png/jsvg/07_opacity_blend.png) |
| **Size** | 8,409 bytes ✅ | 10,201 bytes | 9,853 bytes | 8,788 bytes |
<!-- BEGIN:TIME:07_opacity_blend -->| **Time** | 3.4613 ms | 17.9580 ms | **3.1673 ms** ✅ | 3.4066 ms |<!-- END:TIME:07_opacity_blend -->

### 08_viewbox_aspect

viewBox scaling with different preserveAspectRatio values.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/08_viewbox_aspect.svg) | ![JairoSVG](visual/png/jairosvg/08_viewbox_aspect.png) | ![EchoSVG](visual/png/echosvg/08_viewbox_aspect.png) | ![CairoSVG](visual/png/cairosvg/08_viewbox_aspect.png) | ![JSVG](visual/png/jsvg/08_viewbox_aspect.png) |
| **Size** | 10,492 bytes ✅ | 12,769 bytes | 11,444 bytes | 12,147 bytes |
<!-- BEGIN:TIME:08_viewbox_aspect -->| **Time** | **4.8629 ms** ✅ | 20.2394 ms | 4.9917 ms | 4.8867 ms |<!-- END:TIME:08_viewbox_aspect -->

### 09_css_styling

CSS `<style>` block with class and ID selectors.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/09_css_styling.svg) | ![JairoSVG](visual/png/jairosvg/09_css_styling.png) | ![EchoSVG](visual/png/echosvg/09_css_styling.png) | ![CairoSVG](visual/png/cairosvg/09_css_styling.png) | ![JSVG](visual/png/jsvg/09_css_styling.png) |
| **Size** | 7,755 bytes ✅ | 11,144 bytes | 10,816 bytes | 8,653 bytes |
<!-- BEGIN:TIME:09_css_styling -->| **Time** | **3.5098 ms** ✅ | 15.8148 ms | 3.8613 ms | 3.9325 ms |<!-- END:TIME:09_css_styling -->

### 10_use_and_defs

`<use>` element references, `<clipPath>`, and `<defs>` reuse.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/10_use_and_defs.svg) | ![JairoSVG](visual/png/jairosvg/10_use_and_defs.png) | ![EchoSVG](visual/png/echosvg/10_use_and_defs.png) | ![CairoSVG](visual/png/cairosvg/10_use_and_defs.png) | ![JSVG](visual/png/jsvg/10_use_and_defs.png) |
| **Size** | 5,448 bytes ✅ | 6,122 bytes | 9,712 bytes | 6,144 bytes |
<!-- BEGIN:TIME:10_use_and_defs -->| **Time** | 4.1753 ms | 14.7024 ms | 4.1843 ms | **3.6191 ms** ✅ |<!-- END:TIME:10_use_and_defs -->

### 11_star_polygon

Complex star polygon with fill-rule evenodd.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/11_star_polygon.svg) | ![JairoSVG](visual/png/jairosvg/11_star_polygon.png) | ![EchoSVG](visual/png/echosvg/11_star_polygon.png) | ![CairoSVG](visual/png/cairosvg/11_star_polygon.png) | ![JSVG](visual/png/jsvg/11_star_polygon.png) |
| **Size** | 6,228 bytes ✅ | 8,862 bytes | 8,911 bytes | 6,455 bytes |
<!-- BEGIN:TIME:11_star_polygon -->| **Time** | 3.1363 ms | 14.7665 ms | **2.8726 ms** ✅ | 3.1643 ms |<!-- END:TIME:11_star_polygon -->

### 12_nested_svg

Nested `<svg>` elements with independent viewports.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/12_nested_svg.svg) | ![JairoSVG](visual/png/jairosvg/12_nested_svg.png) | ![EchoSVG](visual/png/echosvg/12_nested_svg.png) | ![CairoSVG](visual/png/cairosvg/12_nested_svg.png) | ![JSVG](visual/png/jsvg/12_nested_svg.png) |
| **Size** | 10,926 bytes ✅ | 12,522 bytes | 11,880 bytes | 12,101 bytes |
<!-- BEGIN:TIME:12_nested_svg -->| **Time** | **4.3992 ms** ✅ | 18.8711 ms | 4.7425 ms | 4.6450 ms |<!-- END:TIME:12_nested_svg -->

### 13_patterns

Tiled pattern fills: dots, cross-hatch stripes, and grid lines.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/13_patterns.svg) | ![JairoSVG](visual/png/jairosvg/13_patterns.png) | ![EchoSVG](visual/png/echosvg/13_patterns.png) | ![CairoSVG](visual/png/cairosvg/13_patterns.png) | ![JSVG](visual/png/jsvg/13_patterns.png) |
| **Size** | 9,532 bytes ✅ | 11,832 bytes | 11,095 bytes | 11,043 bytes |
<!-- BEGIN:TIME:13_patterns -->| **Time** | 4.4608 ms | 16.2910 ms | 4.3478 ms | **4.3360 ms** ✅ |<!-- END:TIME:13_patterns -->

### 14_clip_paths

Star and text clip paths applied to gradient fills.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/14_clip_paths.svg) | ![JairoSVG](visual/png/jairosvg/14_clip_paths.png) | ![EchoSVG](visual/png/echosvg/14_clip_paths.png) | ![CairoSVG](visual/png/cairosvg/14_clip_paths.png) | ![JSVG](visual/png/jsvg/14_clip_paths.png) |
| **Size** | 9,342 bytes ✅ | 10,558 bytes | 13,552 bytes | 10,253 bytes |
<!-- BEGIN:TIME:14_clip_paths -->| **Time** | **4.1520 ms** ✅ | 26.3523 ms | 5.6643 ms | 4.2836 ms |<!-- END:TIME:14_clip_paths -->

### 15_masks

Horizontal, vertical, and circular gradient masks with luminance blending.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/15_masks.svg) | ![JairoSVG](visual/png/jairosvg/15_masks.png) | ![EchoSVG](visual/png/echosvg/15_masks.png) | ![CairoSVG](visual/png/cairosvg/15_masks.png) | ![JSVG](visual/png/jsvg/15_masks.png) |
| **Size** | 5,692 bytes | 5,566 bytes | 1,161 bytes ✅ | 6,209 bytes |
<!-- BEGIN:TIME:15_masks -->| **Time** | 4.4610 ms | 22.5966 ms | **3.4835 ms** ✅ | 4.5897 ms |<!-- END:TIME:15_masks -->

### 16_markers

Arrow, dot, and square markers on lines, polylines, and curves.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/16_markers.svg) | ![JairoSVG](visual/png/jairosvg/16_markers.png) | ![EchoSVG](visual/png/echosvg/16_markers.png) | ![CairoSVG](visual/png/cairosvg/16_markers.png) | ![JSVG](visual/png/jsvg/16_markers.png) |
| **Size** | 9,796 bytes ✅ | 12,642 bytes | 12,655 bytes | 10,041 bytes |
<!-- BEGIN:TIME:16_markers -->| **Time** | 4.5350 ms | 17.7833 ms | 5.4295 ms | **4.5235 ms** ✅ |<!-- END:TIME:16_markers -->

### 17_filters

Gaussian blur and drop-shadow filters on shapes and text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/17_filters.svg) | ![JairoSVG](visual/png/jairosvg/17_filters.png) | ![EchoSVG](visual/png/echosvg/17_filters.png) | ![CairoSVG](visual/png/cairosvg/17_filters.png) | ![JSVG](visual/png/jsvg/17_filters.png) |
| **Size** | 28,934 bytes | 24,063 bytes | 8,520 bytes ✅ | 32,346 bytes |
<!-- BEGIN:TIME:17_filters -->| **Time** | 7.0368 ms | 33.5862 ms | **4.3053 ms** ✅ | 8.0875 ms |<!-- END:TIME:17_filters -->

### 18_embedded_image

Base64-encoded PNG images with clipping, transforms, and opacity.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/18_embedded_image.svg) | ![JairoSVG](visual/png/jairosvg/18_embedded_image.png) | ![EchoSVG](visual/png/echosvg/18_embedded_image.png) | ![CairoSVG](visual/png/cairosvg/18_embedded_image.png) | ![JSVG](visual/png/jsvg/18_embedded_image.png) |
| **Size** | 9,432 bytes ✅ | 11,994 bytes | 21,228 bytes | 11,642 bytes |
<!-- BEGIN:TIME:18_embedded_image -->| **Time** | **4.2668 ms** ✅ | 15.5121 ms | 6.8259 ms | 9.5429 ms |<!-- END:TIME:18_embedded_image -->

### 19_text_advanced

Multi-span text (tspan), text-decoration, textPath on curves, and rotated text.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/19_text_advanced.svg) | ![JairoSVG](visual/png/jairosvg/19_text_advanced.png) | ![EchoSVG](visual/png/echosvg/19_text_advanced.png) | ![CairoSVG](visual/png/cairosvg/19_text_advanced.png) | ![JSVG](visual/png/jsvg/19_text_advanced.png) |
| **Size** | 18,801 bytes ✅ | 26,256 bytes | 23,864 bytes | 19,756 bytes |
<!-- BEGIN:TIME:19_text_advanced -->| **Time** | **5.0387 ms** ✅ | 25.8489 ms | 8.4705 ms | 5.0668 ms |<!-- END:TIME:19_text_advanced -->

### 20_fe_blend_modes

feBlend modes: normal, multiply, screen, darken, and lighten.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/20_fe_blend_modes.svg) | ![JairoSVG](visual/png/jairosvg/20_fe_blend_modes.png) | ![EchoSVG](visual/png/echosvg/20_fe_blend_modes.png) | ![CairoSVG](visual/png/cairosvg/20_fe_blend_modes.png) | ![JSVG](visual/png/jsvg/20_fe_blend_modes.png) |
| **Size** | 12,005 bytes ✅ | 16,216 bytes | 12,505 bytes | 15,773 bytes |
<!-- BEGIN:TIME:20_fe_blend_modes -->| **Time** | **9.6381 ms** ✅ | 27.2628 ms | 12.2996 ms | 20.3193 ms |<!-- END:TIME:20_fe_blend_modes -->

### 21_fe_tile

`feTile` filter primitive: repeating input across the filter region.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/21_fe_tile.svg) | ![JairoSVG](visual/png/jairosvg/21_fe_tile.png) | ![EchoSVG](visual/png/echosvg/21_fe_tile.png) | ![CairoSVG](visual/png/cairosvg/21_fe_tile.png) | ![JSVG](visual/png/jsvg/21_fe_tile.png) |
| **Size** | 1,456 bytes ✅ | 2,009 bytes | 1,768 bytes | 1,489 bytes |
<!-- BEGIN:TIME:21_fe_tile -->| **Time** | 2.4961 ms | 6.4566 ms | **2.4295 ms** ✅ | 2.4466 ms |<!-- END:TIME:21_fe_tile -->

### 22_feimage_data_uri

`feImage` with data-URI PNG source.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/22_feimage_data_uri.svg) | ![JairoSVG](visual/png/jairosvg/22_feimage_data_uri.png) | ![EchoSVG](visual/png/echosvg/22_feimage_data_uri.png) | ![CairoSVG](visual/png/cairosvg/22_feimage_data_uri.png) | ![JSVG](visual/png/jsvg/22_feimage_data_uri.png) |
| **Size** | 2,633 bytes ✅ | 4,406 bytes | 3,206 bytes | 3,639 bytes |
<!-- BEGIN:TIME:22_feimage_data_uri -->| **Time** | 1.5829 ms | 5.4897 ms | 1.8363 ms | **1.5635 ms** ✅ |<!-- END:TIME:22_feimage_data_uri -->

### 23_feimage_inline_ref

`feImage` referencing an inline SVG element by fragment ID.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/23_feimage_inline_ref.svg) | ![JairoSVG](visual/png/jairosvg/23_feimage_inline_ref.png) | ![EchoSVG](visual/png/echosvg/23_feimage_inline_ref.png) | ![CairoSVG](visual/png/cairosvg/23_feimage_inline_ref.png) | ![JSVG](visual/png/jsvg/23_feimage_inline_ref.png) |
| **Size** | 2,702 bytes ✅ | 3,642 bytes | 4,903 bytes | 4,380 bytes |
<!-- BEGIN:TIME:23_feimage_inline_ref -->| **Time** | **1.5585 ms** ✅ | 4.3672 ms | 2.2383 ms | 2.9431 ms |<!-- END:TIME:23_feimage_inline_ref -->

### 24_localized_masks

Masks with localized coordinate systems and gradient fills.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/24_localized_masks.svg) | ![JairoSVG](visual/png/jairosvg/24_localized_masks.png) | ![EchoSVG](visual/png/echosvg/24_localized_masks.png) | ![CairoSVG](visual/png/cairosvg/24_localized_masks.png) | ![JSVG](visual/png/jsvg/24_localized_masks.png) |
| **Size** | 18,389 bytes | 17,868 bytes | 13,218 bytes ✅ | 20,239 bytes |
<!-- BEGIN:TIME:24_localized_masks -->| **Time** | **14.3141 ms** ✅ | 54.0010 ms | 15.8612 ms | 14.3180 ms |<!-- END:TIME:24_localized_masks -->

### 25_svg_fonts

Custom SVG font with glyph paths and missing-glyph fallback.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/25_svg_fonts.svg) | ![JairoSVG](visual/png/jairosvg/25_svg_fonts.png) | ![EchoSVG](visual/png/echosvg/25_svg_fonts.png) | ![CairoSVG](visual/png/cairosvg/25_svg_fonts.png) | ![JSVG](visual/png/jsvg/25_svg_fonts.png) |
| **Size** | 10,331 bytes ✅ | 14,274 bytes | 15,233 bytes | 12,607 bytes |
<!-- BEGIN:TIME:25_svg_fonts -->| **Time** | 3.3620 ms | 16.1210 ms | 4.1728 ms | **3.3583 ms** ✅ |<!-- END:TIME:25_svg_fonts -->

### 26_symbol_use

Reusable `<symbol>` elements instantiated with `<use>` at different sizes and positions.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/26_symbol_use.svg) | ![JairoSVG](visual/png/jairosvg/26_symbol_use.png) | ![EchoSVG](visual/png/echosvg/26_symbol_use.png) | ![CairoSVG](visual/png/cairosvg/26_symbol_use.png) | ![JSVG](visual/png/jsvg/26_symbol_use.png) |
| **Size** | 15,665 bytes ✅ | 24,513 bytes | 21,625 bytes | 18,260 bytes |
<!-- BEGIN:TIME:26_symbol_use -->| **Time** | 4.3219 ms | 23.5184 ms | 8.6190 ms | **4.2348 ms** ✅ |<!-- END:TIME:26_symbol_use -->

### 27_switch_features

`<switch>` element with requiredFeatures and systemLanguage conditional rendering.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/27_switch_features.svg) | ![JairoSVG](visual/png/jairosvg/27_switch_features.png) | ![EchoSVG](visual/png/echosvg/27_switch_features.png) | ![CairoSVG](visual/png/cairosvg/27_switch_features.png) | ![JSVG](visual/png/jsvg/27_switch_features.png) |
| **Size** | 11,535 bytes | 18,040 bytes | 14,493 bytes | 8,503 bytes ✅ |
<!-- BEGIN:TIME:27_switch_features -->| **Time** | 3.8951 ms | 19.5066 ms | 5.8029 ms | **3.1255 ms** ✅ |<!-- END:TIME:27_switch_features -->

### 28_css_variables

CSS custom properties with `var()` function and fallback values.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/28_css_variables.svg) | ![JairoSVG](visual/png/jairosvg/28_css_variables.png) | ![EchoSVG](visual/png/echosvg/28_css_variables.png) | — | ![JSVG](visual/png/jsvg/28_css_variables.png) |
| **Size** | 11,574 bytes ✅ | 17,016 bytes | — | 12,509 bytes |
<!-- BEGIN:TIME:28_css_variables -->| **Time** | 3.9022 ms | 18.9834 ms | — | **3.6660 ms** ✅ |<!-- END:TIME:28_css_variables -->

### 29_current_color

`currentColor` keyword for fill, stroke, and gradient stops with nested inheritance.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/29_current_color.svg) | ![JairoSVG](visual/png/jairosvg/29_current_color.png) | ![EchoSVG](visual/png/echosvg/29_current_color.png) | ![CairoSVG](visual/png/cairosvg/29_current_color.png) | ![JSVG](visual/png/jsvg/29_current_color.png) |
| **Size** | 10,037 bytes ✅ | 14,642 bytes | 11,006 bytes | 13,030 bytes |
<!-- BEGIN:TIME:29_current_color -->| **Time** | 3.8647 ms | 17.0837 ms | 5.6971 ms | **3.6656 ms** ✅ |<!-- END:TIME:29_current_color -->

### 30_display_visibility

`display:none` vs `visibility:hidden` behavior, group suppression, and child override.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/30_display_visibility.svg) | ![JairoSVG](visual/png/jairosvg/30_display_visibility.png) | ![EchoSVG](visual/png/echosvg/30_display_visibility.png) | ![CairoSVG](visual/png/cairosvg/30_display_visibility.png) | ![JSVG](visual/png/jsvg/30_display_visibility.png) |
| **Size** | 11,009 bytes ✅ | 17,473 bytes | 13,218 bytes | 14,263 bytes |
<!-- BEGIN:TIME:30_display_visibility -->| **Time** | 4.2502 ms | 19.7640 ms | 7.1646 ms | **4.0279 ms** ✅ |<!-- END:TIME:30_display_visibility -->

### 31_nested_overflow

Nested `<svg>` elements with `overflow` values: hidden, scroll, visible, and auto.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/31_nested_overflow.svg) | ![JairoSVG](visual/png/jairosvg/31_nested_overflow.png) | ![EchoSVG](visual/png/echosvg/31_nested_overflow.png) | ![CairoSVG](visual/png/cairosvg/31_nested_overflow.png) | ![JSVG](visual/png/jsvg/31_nested_overflow.png) |
| **Size** | 11,273 bytes ✅ | 16,322 bytes | 13,738 bytes | 13,737 bytes |
<!-- BEGIN:TIME:31_nested_overflow -->| **Time** | 4.0464 ms | 21.3446 ms | 5.6789 ms | **3.9465 ms** ✅ |<!-- END:TIME:31_nested_overflow -->

### 32_stroke_advanced

`stroke-dashoffset` phase shifting and `stroke-miterlimit` miter-to-bevel fallback.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/32_stroke_advanced.svg) | ![JairoSVG](visual/png/jairosvg/32_stroke_advanced.png) | ![EchoSVG](visual/png/echosvg/32_stroke_advanced.png) | ![CairoSVG](visual/png/cairosvg/32_stroke_advanced.png) | ![JSVG](visual/png/jsvg/32_stroke_advanced.png) |
| **Size** | 9,287 bytes ✅ | 14,507 bytes | 12,246 bytes | 11,702 bytes |
<!-- BEGIN:TIME:32_stroke_advanced -->| **Time** | **3.6221 ms** ✅ | 17.9280 ms | 5.6720 ms | 3.6605 ms |<!-- END:TIME:32_stroke_advanced -->

### 33_pattern_transforms

`patternTransform` with scale, rotate, translate, and combined transforms.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/33_pattern_transforms.svg) | ![JairoSVG](visual/png/jairosvg/33_pattern_transforms.png) | ![EchoSVG](visual/png/echosvg/33_pattern_transforms.png) | ![CairoSVG](visual/png/cairosvg/33_pattern_transforms.png) | ![JSVG](visual/png/jsvg/33_pattern_transforms.png) |
| **Size** | 9,052 bytes ✅ | 16,101 bytes | 13,061 bytes | 16,273 bytes |
<!-- BEGIN:TIME:33_pattern_transforms -->| **Time** | 4.0038 ms | 21.2433 ms | 6.1535 ms | **3.9560 ms** ✅ |<!-- END:TIME:33_pattern_transforms -->

### 34_gradient_advanced

Gradient `spreadMethod` (reflect/repeat/pad), `fx`/`fy` focus, `href` inheritance, and `userSpaceOnUse`.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/34_gradient_advanced.svg) | ![JairoSVG](visual/png/jairosvg/34_gradient_advanced.png) | ![EchoSVG](visual/png/echosvg/34_gradient_advanced.png) | ![CairoSVG](visual/png/cairosvg/34_gradient_advanced.png) | ![JSVG](visual/png/jsvg/34_gradient_advanced.png) |
| **Size** | 31,339 bytes | 35,647 bytes | 30,960 bytes ✅ | 35,070 bytes |
<!-- BEGIN:TIME:34_gradient_advanced -->| **Time** | 7.0048 ms | 44.4804 ms | 12.0088 ms | **6.4108 ms** ✅ |<!-- END:TIME:34_gradient_advanced -->

### 35_filter_merge_offset

`feMerge` for compositing layers and `feOffset` for position shifting with shadow effects.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/35_filter_merge_offset.svg) | ![JairoSVG](visual/png/jairosvg/35_filter_merge_offset.png) | ![EchoSVG](visual/png/echosvg/35_filter_merge_offset.png) | ![CairoSVG](visual/png/cairosvg/35_filter_merge_offset.png) | ![JSVG](visual/png/jsvg/35_filter_merge_offset.png) |
| **Size** | 9,348 bytes ✅ | 14,868 bytes | 14,168 bytes | 12,184 bytes |
<!-- BEGIN:TIME:35_filter_merge_offset -->| **Time** | **4.9186 ms** ✅ | 18.5218 ms | 6.0389 ms | 8.4652 ms |<!-- END:TIME:35_filter_merge_offset -->

### 36_fe_color_matrix

`feColorMatrix` with type matrix, saturate, hueRotate, and luminanceToAlpha.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/36_fe_color_matrix.svg) | ![JairoSVG](visual/png/jairosvg/36_fe_color_matrix.png) | ![EchoSVG](visual/png/echosvg/36_fe_color_matrix.png) | ![CairoSVG](visual/png/cairosvg/36_fe_color_matrix.png) | ![JSVG](visual/png/jsvg/36_fe_color_matrix.png) |
| **Size** | 13,131 bytes | 15,605 bytes | 10,152 bytes ✅ | 14,503 bytes |
<!-- BEGIN:TIME:36_fe_color_matrix -->| **Time** | **3.3644 ms** ✅ | 16.8168 ms | 5.3086 ms | 3.3788 ms |<!-- END:TIME:36_fe_color_matrix -->

### 37_fe_morphology

`feMorphology` erode and dilate operators on text, shapes, and circles.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/37_fe_morphology.svg) | ![JairoSVG](visual/png/jairosvg/37_fe_morphology.png) | ![EchoSVG](visual/png/echosvg/37_fe_morphology.png) | ![CairoSVG](visual/png/cairosvg/37_fe_morphology.png) | ![JSVG](visual/png/jsvg/37_fe_morphology.png) |
| **Size** | 9,850 bytes | 13,914 bytes | 10,313 bytes | 9,544 bytes ✅ |
<!-- BEGIN:TIME:37_fe_morphology -->| **Time** | **3.6122 ms** ✅ | 17.1733 ms | 5.2294 ms | 3.9805 ms |<!-- END:TIME:37_fe_morphology -->

### 38_fe_turbulence

`feTurbulence` fractalNoise and turbulence types with varying frequency and octaves.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/38_fe_turbulence.svg) | ![JairoSVG](visual/png/jairosvg/38_fe_turbulence.png) | ![EchoSVG](visual/png/echosvg/38_fe_turbulence.png) | ![CairoSVG](visual/png/cairosvg/38_fe_turbulence.png) | ![JSVG](visual/png/jsvg/38_fe_turbulence.png) |
| **Size** | 77,590 bytes | 64,097 bytes | 9,371 bytes ✅ | 76,437 bytes |
<!-- BEGIN:TIME:38_fe_turbulence -->| **Time** | 8.7963 ms | 34.0668 ms | **5.5963 ms** ✅ | 7.7530 ms |<!-- END:TIME:38_fe_turbulence -->

### 39_fe_displacement_map

`feDisplacementMap` distortion using a turbulence displacement source.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/39_fe_displacement_map.svg) | ![JairoSVG](visual/png/jairosvg/39_fe_displacement_map.png) | ![EchoSVG](visual/png/echosvg/39_fe_displacement_map.png) | ![CairoSVG](visual/png/cairosvg/39_fe_displacement_map.png) | ![JSVG](visual/png/jsvg/39_fe_displacement_map.png) |
| **Size** | 9,151 bytes ✅ | 17,633 bytes | 9,684 bytes | 10,208 bytes |
<!-- BEGIN:TIME:39_fe_displacement_map -->| **Time** | 4.9033 ms | 19.0484 ms | **4.0421 ms** ✅ | 9.5448 ms |<!-- END:TIME:39_fe_displacement_map -->

### 40_fe_lighting

`feDiffuseLighting` and `feSpecularLighting` with distant and point light sources.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/40_fe_lighting.svg) | ![JairoSVG](visual/png/jairosvg/40_fe_lighting.png) | ![EchoSVG](visual/png/echosvg/40_fe_lighting.png) | ![CairoSVG](visual/png/cairosvg/40_fe_lighting.png) | ![JSVG](visual/png/jsvg/40_fe_lighting.png) |
| **Size** | 12,213 bytes | 18,277 bytes | 9,006 bytes ✅ | 10,269 bytes |
<!-- BEGIN:TIME:40_fe_lighting -->| **Time** | 4.6776 ms | 21.2214 ms | 4.7658 ms | **4.1237 ms** ✅ |<!-- END:TIME:40_fe_lighting -->

### 41_fe_convolve_matrix

`feConvolveMatrix` convolution effects: emboss, edge detection, sharpen, and box blur.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/41_fe_convolve_matrix.svg) | ![JairoSVG](visual/png/jairosvg/41_fe_convolve_matrix.png) | ![EchoSVG](visual/png/echosvg/41_fe_convolve_matrix.png) | ![CairoSVG](visual/png/cairosvg/41_fe_convolve_matrix.png) | ![JSVG](visual/png/jsvg/41_fe_convolve_matrix.png) |
| **Size** | 12,104 bytes | 6,858 bytes ✅ | 9,045 bytes | 8,834 bytes |
<!-- BEGIN:TIME:41_fe_convolve_matrix -->| **Time** | **3.8507 ms** ✅ | 9.6118 ms | 4.7904 ms | 6.5445 ms |<!-- END:TIME:41_fe_convolve_matrix -->

### 42_fe_component_transfer

`feComponentTransfer` with gamma, discrete, linear, and table transfer functions.

| Input SVG | JairoSVG | EchoSVG | CairoSVG | JSVG |
| :-------: | :------: | :-----: | :------: | :--: |
| ![SVG](svg/42_fe_component_transfer.svg) | ![JairoSVG](visual/png/jairosvg/42_fe_component_transfer.png) | ![EchoSVG](visual/png/echosvg/42_fe_component_transfer.png) | ![CairoSVG](visual/png/cairosvg/42_fe_component_transfer.png) | ![JSVG](visual/png/jsvg/42_fe_component_transfer.png) |
| **Size** | 9,118 bytes ✅ | 12,543 bytes | 9,950 bytes | 10,463 bytes |
<!-- BEGIN:TIME:42_fe_component_transfer -->| **Time** | 3.1299 ms | 16.0676 ms | 5.5329 ms | **3.0983 ms** ✅ |<!-- END:TIME:42_fe_component_transfer -->

---

<!-- BEGIN:BENCHMARK -->
## Benchmark

SVG → PNG conversion benchmarks across 42 SVG test files, median time per render (lower is better):

| Test Case | JairoSVG (Java) | EchoSVG (Java) | JSVG (Java) | CairoSVG (Python) |
| --- | :---: | :---: | :---: | :---: |
| [Basic shapes](#01_basic_shapes) | 3.3224 ms | 15.4451 ms | **3.1553 ms** ✅ | 4.0982 ms |
| [Gradients](#02_gradients) | **4.1492 ms** ✅ | 127.9740 ms | 4.1983 ms | 10.5992 ms |
| [Complex paths](#03_complex_paths) | **4.0290 ms** ✅ | 21.7461 ms | 4.2354 ms | 4.3188 ms |
| [Text rendering](#04_text_rendering) | 4.6488 ms | 22.7574 ms | **4.6150 ms** ✅ | 6.0069 ms |
| [Transforms](#05_transforms) | 3.8863 ms | 13.9228 ms | 3.9663 ms | **3.7330 ms** ✅ |
| [Stroke styles](#06_stroke_styles) | 3.7028 ms | 12.1811 ms | 3.6614 ms | **3.3127 ms** ✅ |
| [Opacity blend](#07_opacity_blend) | 3.4613 ms | 17.9580 ms | 3.4066 ms | **3.1673 ms** ✅ |
| [Viewbox aspect](#08_viewbox_aspect) | **4.8629 ms** ✅ | 20.2394 ms | 4.8867 ms | 4.9917 ms |
| [Css styling](#09_css_styling) | **3.5098 ms** ✅ | 15.8148 ms | 3.9325 ms | 3.8613 ms |
| [Use and defs](#10_use_and_defs) | 4.1753 ms | 14.7024 ms | **3.6191 ms** ✅ | 4.1843 ms |
| [Star polygon](#11_star_polygon) | 3.1363 ms | 14.7665 ms | 3.1643 ms | **2.8726 ms** ✅ |
| [Nested svg](#12_nested_svg) | **4.3992 ms** ✅ | 18.8711 ms | 4.6450 ms | 4.7425 ms |
| [Patterns](#13_patterns) | 4.4608 ms | 16.2910 ms | **4.3360 ms** ✅ | 4.3478 ms |
| [Clip paths](#14_clip_paths) | **4.1520 ms** ✅ | 26.3523 ms | 4.2836 ms | 5.6643 ms |
| [Masks](#15_masks) ⚠️ | 4.4610 ms | 22.5966 ms | 4.5897 ms | **3.4835 ms** ✅ |
| [Markers](#16_markers) | 4.5350 ms | 17.7833 ms | **4.5235 ms** ✅ | 5.4295 ms |
| [Filters](#17_filters) ⚠️ | 7.0368 ms | 33.5862 ms | 8.0875 ms | **4.3053 ms** ✅ |
| [Embedded image](#18_embedded_image) | **4.2668 ms** ✅ | 15.5121 ms | 9.5429 ms | 6.8259 ms |
| [Text advanced](#19_text_advanced) | **5.0387 ms** ✅ | 25.8489 ms | 5.0668 ms | 8.4705 ms |
| [Fe blend modes](#20_fe_blend_modes) | **9.6381 ms** ✅ | 27.2628 ms | 20.3193 ms | 12.2996 ms |
| [Fe tile](#21_fe_tile) | 2.4961 ms | 6.4566 ms | 2.4466 ms | **2.4295 ms** ✅ |
| [Feimage data uri](#22_feimage_data_uri) | 1.5829 ms | 5.4897 ms | **1.5635 ms** ✅ | 1.8363 ms |
| [Feimage inline ref](#23_feimage_inline_ref) | **1.5585 ms** ✅ | 4.3672 ms | 2.9431 ms | 2.2383 ms |
| [Localized masks](#24_localized_masks) | **14.3141 ms** ✅ | 54.0010 ms | 14.3180 ms | 15.8612 ms |
| [Svg fonts](#25_svg_fonts) | 3.3620 ms | 16.1210 ms | **3.3583 ms** ✅ | 4.1728 ms |
| [Symbol use](#26_symbol_use) | 4.3219 ms | 23.5184 ms | **4.2348 ms** ✅ | 8.6190 ms |
| [Switch features](#27_switch_features) | 3.8951 ms | 19.5066 ms | **3.1255 ms** ✅ | 5.8029 ms |
| [Css variables](#28_css_variables) | 3.9022 ms | 18.9834 ms | **3.6660 ms** ✅ | — |
| [Current color](#29_current_color) | 3.8647 ms | 17.0837 ms | **3.6656 ms** ✅ | 5.6971 ms |
| [Display visibility](#30_display_visibility) | 4.2502 ms | 19.7640 ms | **4.0279 ms** ✅ | 7.1646 ms |
| [Nested overflow](#31_nested_overflow) | 4.0464 ms | 21.3446 ms | **3.9465 ms** ✅ | 5.6789 ms |
| [Stroke advanced](#32_stroke_advanced) | **3.6221 ms** ✅ | 17.9280 ms | 3.6605 ms | 5.6720 ms |
| [Pattern transforms](#33_pattern_transforms) | 4.0038 ms | 21.2433 ms | **3.9560 ms** ✅ | 6.1535 ms |
| [Gradient advanced](#34_gradient_advanced) | 7.0048 ms | 44.4804 ms | **6.4108 ms** ✅ | 12.0088 ms |
| [Filter merge offset](#35_filter_merge_offset) | **4.9186 ms** ✅ | 18.5218 ms | 8.4652 ms | 6.0389 ms |
| [Fe color matrix](#36_fe_color_matrix) | **3.3644 ms** ✅ | 16.8168 ms | 3.3788 ms | 5.3086 ms |
| [Fe morphology](#37_fe_morphology) | **3.6122 ms** ✅ | 17.1733 ms | 3.9805 ms | 5.2294 ms |
| [Fe turbulence](#38_fe_turbulence) ⚠️ | 8.7963 ms | 34.0668 ms | 7.7530 ms | **5.5963 ms** ✅ |
| [Fe displacement map](#39_fe_displacement_map) | 4.9033 ms | 19.0484 ms | 9.5448 ms | **4.0421 ms** ✅ |
| [Fe lighting](#40_fe_lighting) | 4.6776 ms | 21.2214 ms | **4.1237 ms** ✅ | 4.7658 ms |
| [Fe convolve matrix](#41_fe_convolve_matrix) | **3.8507 ms** ✅ | 9.6118 ms | 6.5445 ms | 4.7904 ms |
| [Fe component transfer](#42_fe_component_transfer) | 3.1299 ms | 16.0676 ms | **3.0983 ms** ✅ | 5.5329 ms |

_JairoSVG is **2–30× faster** than EchoSVG, **on par with JSVG** in most scenarios, and **faster than CairoSVG** in most scenarios._
<!-- END:BENCHMARK -->

> **⚠️ Filters/Masks caveat:** CairoSVG does **not** correctly render masks (missing gradient and circle content) or `feGaussianBlur`/`feDropShadow` filters — it silently skips them. For those tests, CairoSVG appears faster because it skips rendering work. JairoSVG and JSVG perform the actual computation, so their speed reflects the true cost of correct rendering. Note: JairoSVG now **outperforms CairoSVG on both Masks and feBlend modes** despite rendering them correctly.

<!-- BEGIN:BENCHMARK_NOTE -->
> **Note:** Benchmarks were run with 50 warm-up iterations and 500 measured iterations per SVG file. Median time reported. Results may vary by hardware and SVG complexity.
<!-- END:BENCHMARK_NOTE -->

#### Default Rendering Settings: JairoSVG vs JSVG

Both JairoSVG and JSVG use Java2D as their rendering backend, but they ship with **different default quality settings**, which directly affects benchmark performance:

| Setting                       | JairoSVG default             | JSVG default (out-of-the-box)          | Performance impact |
| ----------------------------- | ---------------------------- | -------------------------------------- | :----------------: |
| `KEY_ANTIALIASING`            | `VALUE_ANTIALIAS_ON`         | `VALUE_ANTIALIAS_ON` (auto-set)        |        Low         |
| `KEY_TEXT_ANTIALIASING`       | Not set (platform default)   | Not set (platform default)             |        Low         |
| `KEY_RENDERING`               | Not set (defaults to speed)  | Not set (defaults to speed)            |      **High**      |
| `KEY_STROKE_CONTROL`          | `VALUE_STROKE_PURE`          | `VALUE_STROKE_PURE` (auto-set)         |       Equal        |
| `KEY_FRACTIONALMETRICS`       | Not set (defaults to `OFF`)  | Not set (defaults to `OFF`)            |       Medium       |
| **PNG compression level**     | 6 (matches CairoSVG/libpng) | N/A (no built-in PNG; user uses `ImageIO`) |       Medium       |

JSVG automatically sets `KEY_ANTIALIASING` and `KEY_STROKE_CONTROL` to the values above when they are at their defaults. JairoSVG now uses the same defaults as JSVG, so both renderers operate with identical quality settings out of the box. Users can customize any hint via `JairoSVG.builder().renderingHint(key, value)`.

**In the benchmark**, both JairoSVG and JSVG use identical rendering hints, so the comparison measures SVG engine efficiency directly.

<!-- BEGIN:PNG_SIZES -->
## PNG Output File Sizes

JairoSVG produces compact PNGs — **9.0% smaller** than JSVG, and **17.5% smaller** than EchoSVG (all using zlib compression level 6 — see [default rendering settings](#default-rendering-settings-jairosvg-vs-jsvg)):

| Test Case             |    JairoSVG |     EchoSVG |    CairoSVG |        JSVG |
| --------------------- | ----------: | ----------: | ----------: | ----------: |
| Basic Shapes          |       6,718 |       8,159 |       8,920 |       7,031 |
| Gradients             |      25,554 |      25,018 |      23,637 |      26,410 |
| Complex Paths         |      12,657 |      16,936 |      15,633 |      12,730 |
| Text Rendering        |      13,276 |      19,125 |      16,317 |      15,626 |
| Transforms            |       5,461 |       5,261 |       6,001 |       5,827 |
| Stroke Styles         |       3,363 |       5,038 |       4,478 |       4,074 |
| Opacity Blend         |       8,409 |      10,201 |       9,853 |       8,788 |
| Viewbox Aspect        |      10,492 |      12,769 |      11,444 |      12,147 |
| Css Styling           |       7,755 |      11,144 |      10,816 |       8,653 |
| Use And Defs          |       5,448 |       6,122 |       9,712 |       6,144 |
| Star Polygon          |       6,228 |       8,862 |       8,911 |       6,455 |
| Nested Svg            |      10,926 |      12,522 |      11,880 |      12,101 |
| Patterns              |       9,532 |      11,832 |      11,095 |      11,043 |
| Clip Paths            |       9,342 |      10,558 |      13,552 |      10,253 |
| Masks ⚠️              |       5,692 |       5,566 |       1,161 |       6,209 |
| Markers               |       9,796 |      12,642 |      12,655 |      10,041 |
| Filters ⚠️            |      28,934 |      24,063 |       8,520 |      32,346 |
| Embedded Image        |       9,432 |      11,994 |      21,228 |      11,642 |
| Text Advanced         |      18,801 |      26,256 |      23,864 |      19,756 |
| Fe Blend Modes        |      12,005 |      16,216 |      12,505 |      15,773 |
| Fe Tile               |       1,456 |       2,009 |       1,768 |       1,489 |
| Feimage Data Uri      |       2,633 |       4,406 |       3,206 |       3,639 |
| Feimage Inline Ref    |       2,702 |       3,642 |       4,903 |       4,380 |
| Localized Masks       |      18,389 |      17,868 |      13,218 |      20,239 |
| Svg Fonts             |      10,331 |      14,274 |      15,233 |      12,607 |
| Symbol Use            |      15,665 |      24,513 |      21,625 |      18,260 |
| Switch Features       |      11,535 |      18,040 |      14,493 |       8,503 |
| Css Variables         |      11,574 |      17,016 |           — |      12,509 |
| Current Color         |      10,037 |      14,642 |      11,006 |      13,030 |
| Display Visibility    |      11,009 |      17,473 |      13,218 |      14,263 |
| Nested Overflow       |      11,273 |      16,322 |      13,738 |      13,737 |
| Stroke Advanced       |       9,287 |      14,507 |      12,246 |      11,702 |
| Pattern Transforms    |       9,052 |      16,101 |      13,061 |      16,273 |
| Gradient Advanced     |      31,339 |      35,647 |      30,960 |      35,070 |
| Filter Merge Offset   |       9,348 |      14,868 |      14,168 |      12,184 |
| Fe Color Matrix       |      13,131 |      15,605 |      10,152 |      14,503 |
| Fe Morphology         |       9,850 |      13,914 |      10,313 |       9,544 |
| Fe Turbulence ⚠️      |      77,590 |      64,097 |       9,371 |      76,437 |
| Fe Displacement Map   |       9,151 |      17,633 |       9,684 |      10,208 |
| Fe Lighting           |      12,213 |      18,277 |       9,006 |      10,269 |
| Fe Convolve Matrix    |      12,104 |       6,858 |       9,045 |       8,834 |
| Fe Component Transfer |       9,118 |      12,543 |       9,950 |      10,463 |
| **Total**             | **528,608** | **640,539** | **492,546** | **581,192** |
<!-- END:PNG_SIZES -->

> **⚠️ Filters/Masks:** Where CairoSVG produces much smaller output, it is because CairoSVG **does not render** certain features correctly — filter effects (blur, drop-shadow) are silently skipped, and masks are rendered without gradient/circle content. This results in simpler images that compress better. JairoSVG and JSVG render these effects correctly, producing visually accurate but larger PNGs.

## Running the Benchmark

Prerequisites: [JBang], Java 25+, Python 3 with CairoSVG (`pip install cairosvg`), JairoSVG installed in local Maven repo.

```bash
./mvnw install -DskipTests
jbang comparison/benchmark/benchmark.java
```

Options:

```bash
# Run specific SVG categories only
jbang comparison/benchmark/benchmark.java filters embedded

# Skip engines
jbang comparison/benchmark/benchmark.java --no-cairosvg
jbang comparison/benchmark/benchmark.java --no-echosvg
jbang comparison/benchmark/benchmark.java --no-jsvg

# Disable progress bar output (useful for CI logs)
jbang comparison/benchmark/benchmark.java --no-progress

# Adjust warmup and measurement iterations (defaults: 50 and 500)
jbang comparison/benchmark/benchmark.java --warmup=5 --iterations=100
```

The benchmark writes results to `benchmark/benchmark-results.jsonl` (JSON lines) and logs to a timestamped file. The `update_readme.java` script reads the JSONL and PNG files to regenerate both the timing table and PNG file sizes table in this document.

<!-- Link references -->

[JBang]: https://www.jbang.dev/

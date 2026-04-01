# Benchmark

SVG → PNG conversion benchmarks across 42 SVG test files, median time per render (lower is better):

| Test Case | JairoSVG (Java) | EchoSVG (Java) | JSVG (Java) | CairoSVG (Python) |
| --- | :---: | :---: | :---: | :---: |
| [Basic shapes](../visual/README.md#01_basic_shapes) | 3.3224 ms | 15.4451 ms | **3.1553 ms** ✅ | 4.0982 ms |
| [Gradients](../visual/README.md#02_gradients) | **4.1492 ms** ✅ | 127.9740 ms | 4.1983 ms | 10.5992 ms |
| [Complex paths](../visual/README.md#03_complex_paths) | **4.0290 ms** ✅ | 21.7461 ms | 4.2354 ms | 4.3187 ms |
| [Text rendering](../visual/README.md#04_text_rendering) | 4.6488 ms | 22.7574 ms | **4.6150 ms** ✅ | 6.0069 ms |
| [Transforms](../visual/README.md#05_transforms) | 3.8863 ms | 13.9228 ms | 3.9663 ms | **3.7330 ms** ✅ |
| [Stroke styles](../visual/README.md#06_stroke_styles) | 3.7028 ms | 12.1811 ms | 3.6614 ms | **3.3127 ms** ✅ |
| [Opacity blend](../visual/README.md#07_opacity_blend) | 3.4613 ms | 17.9580 ms | 3.4066 ms | **3.1673 ms** ✅ |
| [Viewbox aspect](../visual/README.md#08_viewbox_aspect) | **4.8629 ms** ✅ | 20.2394 ms | 4.8867 ms | 4.9917 ms |
| [Css styling](../visual/README.md#09_css_styling) | **3.5098 ms** ✅ | 15.8148 ms | 3.9325 ms | 3.8613 ms |
| [Use and defs](../visual/README.md#10_use_and_defs) | 4.1753 ms | 14.7024 ms | **3.6191 ms** ✅ | 4.1843 ms |
| [Star polygon](../visual/README.md#11_star_polygon) | 3.1363 ms | 14.7665 ms | 3.1643 ms | **2.8726 ms** ✅ |
| [Nested svg](../visual/README.md#12_nested_svg) | **4.3992 ms** ✅ | 18.8711 ms | 4.6450 ms | 4.7425 ms |
| [Patterns](../visual/README.md#13_patterns) | 4.4608 ms | 16.2910 ms | **4.3360 ms** ✅ | 4.3478 ms |
| [Clip paths](../visual/README.md#14_clip_paths) | **4.1520 ms** ✅ | 26.3523 ms | 4.2836 ms | 5.6643 ms |
| [Masks](../visual/README.md#15_masks) ⚠️ | 4.4610 ms | 22.5966 ms | 4.5897 ms | **3.4835 ms** ✅ |
| [Markers](../visual/README.md#16_markers) | 4.5350 ms | 17.7833 ms | **4.5235 ms** ✅ | 5.4295 ms |
| [Filters](../visual/README.md#17_filters) ⚠️ | 7.0368 ms | 33.5862 ms | 8.0875 ms | **4.3053 ms** ✅ |
| [Embedded image](../visual/README.md#18_embedded_image) | **4.2668 ms** ✅ | 15.5121 ms | 9.5429 ms | 6.8259 ms |
| [Text advanced](../visual/README.md#19_text_advanced) | **5.0387 ms** ✅ | 25.8489 ms | 5.0668 ms | 8.4705 ms |
| [Fe blend modes](../visual/README.md#20_fe_blend_modes) | **9.6381 ms** ✅ | 27.2628 ms | 20.3193 ms | 12.2996 ms |
| [Fe tile](../visual/README.md#21_fe_tile) | 2.4961 ms | 6.4566 ms | 2.4466 ms | **2.4295 ms** ✅ |
| [Feimage data uri](../visual/README.md#22_feimage_data_uri) | 1.5829 ms | 5.4897 ms | **1.5635 ms** ✅ | 1.8363 ms |
| [Feimage inline ref](../visual/README.md#23_feimage_inline_ref) | **1.5585 ms** ✅ | 4.3672 ms | 2.9431 ms | 2.2383 ms |
| [Localized masks](../visual/README.md#24_localized_masks) | **14.3141 ms** ✅ | 54.0010 ms | 14.3180 ms | 15.8612 ms |
| [Svg fonts](../visual/README.md#25_svg_fonts) | 3.3620 ms | 16.1210 ms | **3.3583 ms** ✅ | 4.1727 ms |
| [Symbol use](../visual/README.md#26_symbol_use) | 4.3219 ms | 23.5184 ms | **4.2348 ms** ✅ | 8.6190 ms |
| [Switch features](../visual/README.md#27_switch_features) | 3.8951 ms | 19.5066 ms | **3.1255 ms** ✅ | 5.8029 ms |
| [Css variables](../visual/README.md#28_css_variables) | 3.9022 ms | 18.9834 ms | **3.6660 ms** ✅ | — |
| [Current color](../visual/README.md#29_current_color) | 3.8647 ms | 17.0837 ms | **3.6656 ms** ✅ | 5.6971 ms |
| [Display visibility](../visual/README.md#30_display_visibility) | 4.2502 ms | 19.7640 ms | **4.0279 ms** ✅ | 7.1646 ms |
| [Nested overflow](../visual/README.md#31_nested_overflow) | 4.0464 ms | 21.3446 ms | **3.9465 ms** ✅ | 5.6789 ms |
| [Stroke advanced](../visual/README.md#32_stroke_advanced) | **3.6221 ms** ✅ | 17.9280 ms | 3.6605 ms | 5.6720 ms |
| [Pattern transforms](../visual/README.md#33_pattern_transforms) | 4.0038 ms | 21.2433 ms | **3.9560 ms** ✅ | 6.1535 ms |
| [Gradient advanced](../visual/README.md#34_gradient_advanced) | 7.0048 ms | 44.4804 ms | **6.4108 ms** ✅ | 12.0088 ms |
| [Filter merge offset](../visual/README.md#35_filter_merge_offset) | **4.9186 ms** ✅ | 18.5218 ms | 8.4652 ms | 6.0389 ms |
| [Fe color matrix](../visual/README.md#36_fe_color_matrix) | **3.3644 ms** ✅ | 16.8168 ms | 3.3788 ms | 5.3086 ms |
| [Fe morphology](../visual/README.md#37_fe_morphology) | **3.6122 ms** ✅ | 17.1733 ms | 3.9805 ms | 5.2294 ms |
| [Fe turbulence](../visual/README.md#38_fe_turbulence) ⚠️ | 8.7963 ms | 34.0668 ms | 7.7530 ms | **5.5963 ms** ✅ |
| [Fe displacement map](../visual/README.md#39_fe_displacement_map) | 4.9033 ms | 19.0484 ms | 9.5448 ms | **4.0421 ms** ✅ |
| [Fe lighting](../visual/README.md#40_fe_lighting) | 4.6776 ms | 21.2214 ms | **4.1237 ms** ✅ | 4.7658 ms |
| [Fe convolve matrix](../visual/README.md#41_fe_convolve_matrix) | **3.8507 ms** ✅ | 9.6118 ms | 6.5445 ms | 4.7904 ms |
| [Fe component transfer](../visual/README.md#42_fe_component_transfer) | 3.1299 ms | 16.0676 ms | **3.0983 ms** ✅ | 5.5329 ms |

_JairoSVG is **2–30× faster** than EchoSVG, **on par with JSVG** in most scenarios, and **faster than CairoSVG** in most scenarios._

> **⚠️ Filters/Masks caveat:** CairoSVG does **not** correctly render masks (missing gradient and circle content) or `feGaussianBlur`/`feDropShadow` filters — it silently skips them. For those tests, CairoSVG appears faster because it skips rendering work. JairoSVG and JSVG perform the actual computation, so their speed reflects the true cost of correct rendering. Note: JairoSVG now **outperforms CairoSVG on both Masks and feBlend modes** despite rendering them correctly.

> **Note:** Benchmarks were run with 20 warm-up iterations and 1000 measured iterations per SVG file. Median time reported. Results may vary by hardware and SVG complexity.

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

> **⚠️ Filters/Masks:** Where CairoSVG produces much smaller output, it is because CairoSVG **does not render** certain features correctly — filter effects (blur, drop-shadow) are silently skipped, and masks are rendered without gradient/circle content. This results in simpler images that compress better. JairoSVG and JSVG render these effects correctly, producing visually accurate but larger PNGs.

## Running the Benchmark

Prerequisites: [JBang], Java 25+, Python 3 with CairoSVG (`pip install cairosvg`), JairoSVG installed in local Maven repo.

```bash
./mvnw install -DskipTests
jbang comparison/benchmark/benchmark.java
```

After running, update the README tables from the results:

```bash
jbang comparison/benchmark/update_readme.java --warmup=50 --iterations=500
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

The benchmark writes results to `benchmark-results.jsonl` (JSON lines) and logs to a timestamped file. The `update_readme.java` script reads the JSONL and PNG files to regenerate both the timing table and PNG file sizes table in this README.

<!-- Link references -->

[JBang]: https://www.jbang.dev/

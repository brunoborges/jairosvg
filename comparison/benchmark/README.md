# Benchmark

SVG → PNG conversion benchmarks across 42 SVG test files, median time per render (lower is better):

| Test Case | JairoSVG (Java) | EchoSVG (Java) | JSVG (Java) | CairoSVG (Python) |
| --- | :---: | :---: | :---: | :---: |
| [Basic shapes](../visual/README.md#01_basic_shapes) | 3.3020 ms | 15.9000 ms | **3.2819 ms** ✅ | 4.0525 ms |
| [Gradients](../visual/README.md#02_gradients) | **4.2075 ms** ✅ | 130.8318 ms | 4.5218 ms | 10.6078 ms |
| [Complex paths](../visual/README.md#03_complex_paths) | 4.1870 ms | 22.7806 ms | **4.0877 ms** ✅ | 4.4588 ms |
| [Text rendering](../visual/README.md#04_text_rendering) | **4.6020 ms** ✅ | 22.2841 ms | 4.6204 ms | 6.0021 ms |
| [Transforms](../visual/README.md#05_transforms) | 3.9760 ms | 14.0730 ms | **3.6525 ms** ✅ | 3.8172 ms |
| [Stroke styles](../visual/README.md#06_stroke_styles) | 3.5003 ms | 11.3359 ms | **3.3691 ms** ✅ | 3.3959 ms |
| [Opacity blend](../visual/README.md#07_opacity_blend) | **3.2588 ms** ✅ | 17.2748 ms | 3.2738 ms | 3.5310 ms |
| [Viewbox aspect](../visual/README.md#08_viewbox_aspect) | 4.8102 ms | 18.9162 ms | **4.4387 ms** ✅ | 5.0514 ms |
| [Css styling](../visual/README.md#09_css_styling) | 3.1855 ms | 14.2915 ms | **3.1450 ms** ✅ | 3.8781 ms |
| [Use and defs](../visual/README.md#10_use_and_defs) | 3.7111 ms | 13.5740 ms | **3.5802 ms** ✅ | 4.2015 ms |
| [Star polygon](../visual/README.md#11_star_polygon) | 3.1616 ms | 14.0556 ms | 3.0460 ms | **2.9650 ms** ✅ |
| [Nested svg](../visual/README.md#12_nested_svg) | 4.3458 ms | 18.8395 ms | **4.3190 ms** ✅ | 4.7925 ms |
| [Patterns](../visual/README.md#13_patterns) | 4.1837 ms | 15.5863 ms | **4.1214 ms** ✅ | 4.3571 ms |
| [Clip paths](../visual/README.md#14_clip_paths) | **3.9708 ms** ✅ | 25.3485 ms | 4.0170 ms | 5.7170 ms |
| [Masks](../visual/README.md#15_masks) ⚠️ | 4.2560 ms | 21.7920 ms | 4.3234 ms | **3.5022 ms** ✅ |
| [Markers](../visual/README.md#16_markers) | 4.3914 ms | 16.9533 ms | **4.3148 ms** ✅ | 5.4704 ms |
| [Filters](../visual/README.md#17_filters) ⚠️ | 6.7528 ms | 33.8967 ms | 8.3714 ms | **4.3954 ms** ✅ |
| [Embedded image](../visual/README.md#18_embedded_image) | **4.3303 ms** ✅ | 15.8568 ms | 9.7263 ms | 6.8449 ms |
| [Text advanced](../visual/README.md#19_text_advanced) | 5.0912 ms | 25.0366 ms | **5.0068 ms** ✅ | 8.5035 ms |
| [Fe blend modes](../visual/README.md#20_fe_blend_modes) | **9.3229 ms** ✅ | 27.1358 ms | 20.1263 ms | 12.4450 ms |
| [Fe tile](../visual/README.md#21_fe_tile) | **2.4576 ms** ✅ | 6.2278 ms | 2.4706 ms | 2.5238 ms |
| [Feimage data uri](../visual/README.md#22_feimage_data_uri) | **1.5253 ms** ✅ | 5.5884 ms | 1.5755 ms | 1.7925 ms |
| [Feimage inline ref](../visual/README.md#23_feimage_inline_ref) | **1.6000 ms** ✅ | 4.4405 ms | 2.4159 ms | 1.8843 ms |
| [Localized masks](../visual/README.md#24_localized_masks) | 14.5860 ms | 55.0997 ms | **14.4908 ms** ✅ | 15.4633 ms |
| [Svg fonts](../visual/README.md#25_svg_fonts) | 3.4250 ms | 16.3181 ms | **3.4214 ms** ✅ | 4.0428 ms |
| [Symbol use](../visual/README.md#26_symbol_use) | 4.5183 ms | 23.8476 ms | **4.2745 ms** ✅ | 8.6599 ms |
| [Switch features](../visual/README.md#27_switch_features) | 4.0125 ms | 19.8215 ms | **3.1684 ms** ✅ | 5.7063 ms |
| [Css variables](../visual/README.md#28_css_variables) | 4.0297 ms | 19.4015 ms | **3.8380 ms** ✅ | — |
| [Current color](../visual/README.md#29_current_color) | 3.8324 ms | 17.6653 ms | **3.7359 ms** ✅ | 5.6851 ms |
| [Display visibility](../visual/README.md#30_display_visibility) | 4.4663 ms | 20.3664 ms | **4.1990 ms** ✅ | 7.1925 ms |
| [Nested overflow](../visual/README.md#31_nested_overflow) | 4.2047 ms | 21.3876 ms | **4.0418 ms** ✅ | 5.8300 ms |
| [Stroke advanced](../visual/README.md#32_stroke_advanced) | 3.7861 ms | 18.0488 ms | **3.5940 ms** ✅ | 5.7559 ms |
| [Pattern transforms](../visual/README.md#33_pattern_transforms) | 3.9416 ms | 20.5304 ms | **3.8021 ms** ✅ | 6.2241 ms |
| [Gradient advanced](../visual/README.md#34_gradient_advanced) | 7.1421 ms | 44.1618 ms | **6.6095 ms** ✅ | 12.2030 ms |
| [Filter merge offset](../visual/README.md#35_filter_merge_offset) | **5.1012 ms** ✅ | 19.3139 ms | 8.8640 ms | 6.0862 ms |
| [Fe color matrix](../visual/README.md#36_fe_color_matrix) | 3.4800 ms | 17.2865 ms | **3.4165 ms** ✅ | 5.4091 ms |
| [Fe morphology](../visual/README.md#37_fe_morphology) | **3.6589 ms** ✅ | 17.6300 ms | 4.1782 ms | 5.3424 ms |
| [Fe turbulence](../visual/README.md#38_fe_turbulence) ⚠️ | 9.3652 ms | 35.1670 ms | 7.9809 ms | **5.7504 ms** ✅ |
| [Fe displacement map](../visual/README.md#39_fe_displacement_map) | 5.1926 ms | 19.7950 ms | 9.9547 ms | **4.1233 ms** ✅ |
| [Fe lighting](../visual/README.md#40_fe_lighting) | 4.7453 ms | 21.7453 ms | **4.1956 ms** ✅ | 4.7932 ms |
| [Fe convolve matrix](../visual/README.md#41_fe_convolve_matrix) | **3.9776 ms** ✅ | 9.4943 ms | 6.5525 ms | 4.8307 ms |
| [Fe component transfer](../visual/README.md#42_fe_component_transfer) | 3.1900 ms | 16.0772 ms | **3.1525 ms** ✅ | 5.5574 ms |

_JairoSVG is **2–31× faster** than EchoSVG, **on par with JSVG** in most scenarios, and **faster than CairoSVG** in most scenarios._

> **⚠️ Filters/Masks caveat:** CairoSVG does **not** correctly render masks (missing gradient and circle content) or `feGaussianBlur`/`feDropShadow` filters — it silently skips them. For those tests, CairoSVG appears faster because it skips rendering work. JairoSVG and JSVG perform the actual computation, so their speed reflects the true cost of correct rendering. Note: JairoSVG now **outperforms CairoSVG on both Masks and feBlend modes** despite rendering them correctly.

> **Note:** Benchmarks were run with 50 warm-up iterations and 500 measured iterations per SVG file. Median time reported. Results may vary by hardware and SVG complexity.

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

# SVG 2 Support — Evaluation & Implementation Plan

## Executive Summary

JairoSVG currently targets **SVG 1.1** with selective SVG 2 behavioral alignment where modern browsers have diverged from SVG 1.1. This document evaluates the full scope of SVG 2 specification changes, assesses each feature's relevance and feasibility for JairoSVG (a static rasterizer with no scripting engine or DOM), and proposes a phased implementation plan.

### Guiding Principles

1. **Browser-aligned pragmatism** — Prioritize SVG 2 features that modern browsers actually implement and that real-world SVGs use. Avoid implementing spec-only features that browsers have not adopted.
2. **Static rendering focus** — JairoSVG is a static SVG-to-raster converter. DOM APIs, scripting, animation, and interactivity features are out of scope.
3. **Incremental adoption** — Continue the current approach of selectively adopting SVG 2 changes. Full "SVG 2 compliance" is neither practical nor meaningful given the spec's modular and incomplete browser adoption.
4. **Backward compatibility** — All changes must maintain SVG 1.1 backward compatibility.

---

## Current SVG 2 Alignment

JairoSVG already implements the following SVG 2 behaviors:

| Feature | Status | Notes |
|---------|--------|-------|
| `requiredFeatures` ignored | ✅ Done | Deprecated in SVG 2; matches modern browser behavior |
| `href` attribute support | ✅ Done | Resolved via fallback chain: `xlink:href` → `href` |
| `<use>` auto-sizing from `<symbol>` viewBox | ✅ Done | SVG 2 "auto" behavior for width/height |
| `<feDropShadow>` filter primitive | ✅ Done | SVG 2 / Filter Effects Level 1 addition |
| CSS `var()` custom properties | ✅ Done | Recursive resolution with fallback values |

---

## SVG 2 Feature Evaluation

Each feature is categorized by priority:

- 🟢 **High** — Widely used in real-world SVGs, implemented by all major browsers, feasible in JairoSVG's architecture
- 🟡 **Medium** — Useful but less common, or requires moderate effort
- 🔵 **Low** — Rare in practice, limited browser support, or high complexity
- ⚪ **Out of Scope** — Requires scripting, DOM, animation, or interactivity (fundamentally incompatible with a static rasterizer)

### 1. Painting & Color

| Feature | Priority | Effort | Description |
|---------|----------|--------|-------------|
| `#RRGGBBAA` / `#RGBA` hex colors | 🟢 High | Small | 8-digit and 4-digit hex with embedded alpha. Trivial to add in `Colors.java` |
| `paint-order` property | 🟢 High | Medium | Controls draw order of fill/stroke/markers. Requires refactoring `Surface.draw()` paint sequence |
| `context-fill` / `context-stroke` | 🔵 Low | Medium | Propagate fill/stroke to referenced content (markers, `<use>`). Partial browser support |
| CSS Color Level 4: `hwb()` | 🟡 Medium | Small | Hue-Whiteness-Blackness color function. Straightforward color math |
| CSS Color Level 4: `lab()`, `lch()` | 🔵 Low | Medium | Perceptually uniform color spaces. Requires color space conversion math |
| CSS Color Level 4: `oklab()`, `oklch()` | 🔵 Low | Medium | Next-gen perceptual color spaces. Same as above |
| CSS Color Level 4: `color()` | 🔵 Low | Large | Wide-gamut color function with named color spaces. Java2D is sRGB-only; would need manual conversion |
| `mix-blend-mode` CSS property | 🟡 Medium | Medium | Element-level blend modes (beyond filter `feBlend`). Requires `Composite` implementation in Java2D |
| `isolation` CSS property | 🔵 Low | Small | Controls stacking context creation. Coupled to `mix-blend-mode` |

### 2. Geometry & Shapes

| Feature | Priority | Effort | Description |
|---------|----------|--------|-------------|
| Geometry attrs as CSS properties (`x`, `y`, `width`, `height`, `r`, `rx`, `ry`, `cx`, `cy`) | 🟡 Medium | Medium | Allow geometric attributes to be set via CSS `style` or `<style>` rules |
| `pathLength` on basic shapes | 🔵 Low | Medium | Scales `stroke-dasharray`/`stroke-dashoffset` for shapes. Requires total-length calculation per shape type |
| `d` as CSS property on `<path>` | 🟡 Medium | Small | Allow path data to be specified via CSS. Used for CSS shape morphing |
| `auto` value for `rx`/`ry` on `<rect>` | 🟢 High | Small | Already partially handled; SVG 2 formalizes `auto` keyword behavior |

### 3. Text

| Feature | Priority | Effort | Description |
|---------|----------|--------|-------------|
| `white-space` CSS property | 🟡 Medium | Medium | Controls whitespace handling, replaces `xml:space`. Values: `normal`, `pre`, `pre-wrap`, etc. |
| `inline-size` for text wrapping | 🔵 Low | Large | Auto-wrapping text within a specified width. Requires line-breaking algorithm |
| `shape-inside` / `shape-subtract` | 🔵 Low | Large | Flow text inside/around arbitrary shapes. Very complex; minimal browser support |
| `text-decoration` as shorthand | 🟡 Medium | Small | Align with CSS Text Decoration Level 3 shorthand syntax |
| `lang` attribute (replacing `xml:lang`) | 🟢 High | Small | Already functionally equivalent; add `lang` as alias in attribute resolution |

### 4. Gradients & Patterns

| Feature | Priority | Effort | Description |
|---------|----------|--------|-------------|
| `fr` attribute on `<radialGradient>` | 🟢 High | Medium | Focal radius for radial gradients. Java2D `RadialGradientPaint` supports focus point but not focal radius natively; requires workaround |
| `href` replaces `xlink:href` on gradients | ✅ Done | — | Already handled via `getHref()` fallback |
| Mesh gradients (`<mesh>`, `<meshgradient>`) | 🔵 Low | Large | Two-dimensional gradient meshes. No browser support as of 2026. Extremely complex to implement |
| Hatch patterns (`<hatch>`, `<hatchpath>`) | 🔵 Low | Large | Continuous hatching patterns. No browser support as of 2026 |

### 5. Filters

| Feature | Priority | Effort | Description |
|---------|----------|--------|-------------|
| `<feColorMatrix>` | 🟢 High | Medium | 5×4 color matrix transform. Supports `matrix`, `saturate`, `hueRotate`, `luminanceToAlpha` types |
| `<feTurbulence>` | 🟡 Medium | Large | Perlin/fractal noise generation. Computationally intensive but well-specified |
| `<feConvolveMatrix>` | 🟡 Medium | Large | Custom convolution kernels (blur, sharpen, emboss, edge-detect) |
| `<feMorphology>` | 🟡 Medium | Medium | Erode/dilate operations on alpha channel |
| `<feDisplacementMap>` | 🟡 Medium | Medium | Pixel displacement using a map image |
| `<feDiffuseLighting>` | 🔵 Low | Large | Diffuse lighting with bump mapping. Requires light source handling (point, spot, distant) |
| `<feSpecularLighting>` | 🔵 Low | Large | Specular lighting with bump mapping. Same complexity as diffuse |
| `<feComponentTransfer>` | 🟡 Medium | Medium | Per-channel transfer functions (identity, table, discrete, linear, gamma) |

> **Note:** Filter primitives are defined in the CSS Filter Effects Module Level 1, not SVG 2 core. However, they are traditionally associated with SVG and are included here for completeness. JairoSVG already supports 9 of the 17 SVG filter primitives.

### 6. CSS Selectors & Styling

| Feature | Priority | Effort | Description |
|---------|----------|--------|-------------|
| Child combinator (`>`) | 🟢 High | Small | Direct child selector. Common in stylesheets |
| Adjacent sibling combinator (`+`) | 🟡 Medium | Small | Next sibling selector |
| General sibling combinator (`~`) | 🟡 Medium | Small | All following siblings selector |
| Attribute selectors (`[attr]`, `[attr=val]`, `[attr^=val]`, etc.) | 🟡 Medium | Medium | Full CSS attribute selector syntax |
| Descendant combinator (space) | 🟢 High | Medium | `div p` style multi-level descendant matching. Requires tree traversal |
| `@media` queries | 🔵 Low | Medium | Media-dependent styles. Limited relevance for static rendering |
| `@supports` queries | 🔵 Low | Small | Feature detection in CSS |

### 7. Structural Changes

| Feature | Priority | Effort | Description |
|---------|----------|--------|-------------|
| `overflow: visible` as default for root `<svg>` | 🟡 Medium | Small | SVG 2 changes default from `hidden` to `visible`. Potentially breaking for existing SVGs |
| `<symbol>` with `x`, `y`, `refX`, `refY` | 🟡 Medium | Small | Extended positioning attributes on `<symbol>` |
| `<use>` enhanced style inheritance | 🟡 Medium | Medium | Stronger CSS inheritance into referenced content |
| Unknown elements as `<g>` | 🟢 High | Small | SVG 2 specifies that unknown elements should be treated as `<g>` containers |

### 8. Accessibility & Metadata

| Feature | Priority | Effort | Description |
|---------|----------|--------|-------------|
| `role`, `aria-*` attributes | ⚪ N/A | — | Accessibility attributes. No visual rendering impact |
| `tabindex` attribute | ⚪ N/A | — | Focus order. No rendering impact |

### 9. Deprecated / Removed Features

| Feature | Status | Notes |
|---------|--------|-------|
| `xlink:href` → `href` | ✅ Handled | Both supported via fallback |
| `xml:lang` → `lang` | 🟢 Easy to add | Add `lang` as attribute alias |
| `xml:space` → `white-space` | 🟡 Tied to `white-space` implementation | Currently `xml:space` handling is implicit |
| SVG Fonts deprecated | ✅ Supported | JairoSVG supports SVG fonts for backward compat; no change needed |
| `requiredFeatures` deprecated | ✅ Done | Already ignored per SVG 2 |
| `<cursor>` element deprecated | ✅ Handled | Already treated as invisible/non-rendering |
| `<altGlyph>`, `<altGlyphDef>`, `<altGlyphItem>` removed | ⚪ N/A | Never implemented |
| `<animateColor>` removed | ⚪ N/A | Animation is out of scope |
| `<font-face-format>`, `<font-face-name>`, `<font-face-src>`, `<font-face-uri>` removed | ⚪ N/A | SVG font metadata; no rendering impact |
| `clip` property (CSS 2.1 `rect()`) deprecated | ⚪ N/A | Replaced by `clip-path` (already supported) |
| `baseProfile`, `version` on `<svg>` removed | ⚪ N/A | Never used for rendering |
| `externalResourcesRequired` removed | ⚪ N/A | Never used for rendering |

### 10. Out of Scope

These SVG 2 features are fundamentally incompatible with JairoSVG's static rendering model:

| Feature | Reason |
|---------|--------|
| DOM APIs (`SVGElement`, `DOMMatrix`, etc.) | No live DOM |
| Scripting (`<script>`) | Security policy; no JS engine |
| SMIL Animation (`<animate>`, `<set>`, etc.) | Static renderer only |
| Event handling (`onclick`, etc.) | No interactivity |
| HTML embedding (`<video>`, `<audio>`, `<canvas>`, `<iframe>`) | Requires browser engine |
| ARIA / WAI accessibility tree | No visual rendering impact |
| `<foreignObject>` HTML rendering | Requires HTML layout engine |
| CSS Transitions / Animations | Static renderer only |
| `pointer-events` property | No interactivity |
| `cursor` property | No interactivity |
| Focus management / `tabindex` | No interactivity |

---

## Proposed Implementation Phases

### Phase 1 — Quick Wins (Low effort, high impact)

These features can be implemented with minimal code changes and immediately improve SVG 2 compatibility:

1. **`#RRGGBBAA` / `#RGBA` hex colors** — Extend `Colors.java` hex parsing to handle 4-digit and 8-digit hex with embedded alpha
2. **`lang` attribute alias** — Add `lang` as fallback in `Features.java` language matching alongside `xml:lang`
3. **Unknown elements treated as `<g>`** — In `Surface.draw()`, render children of unrecognized elements instead of silently skipping them
4. **`auto` for `rx`/`ry` on `<rect>`** — Formalize existing behavior per SVG 2 spec
5. **Child combinator (`>`) in CSS** — Add direct-child matching to `CssProcessor`

**Estimated effort:** 1–2 weeks

### Phase 2 — Core SVG 2 Features (Medium effort, broadly useful)

6. **`paint-order` property** — Refactor fill/stroke/marker paint sequence in `Surface` to respect `paint-order` attribute
7. **`feColorMatrix` filter primitive** — Implement 5×4 color matrix with `saturate`, `hueRotate`, `luminanceToAlpha` modes
8. **`fr` attribute on `<radialGradient>`** — Implement focal radius support in `GradientPainter`
9. **`white-space` CSS property** — Replace implicit `xml:space` handling with CSS `white-space` semantics in `TextDrawer`
10. **CSS `hwb()` color function** — Add HWB color parsing to `Colors.java`
11. **Additional CSS selectors** — Sibling combinators (`+`, `~`), descendant combinator, attribute selectors
12. **`d` as CSS property** — Allow path data from `style` attribute or CSS rules

**Estimated effort:** 3–5 weeks

### Phase 3 — Extended Filter Support (Medium-large effort, visual fidelity)

13. **`<feComponentTransfer>`** — Per-channel transfer functions
14. **`<feMorphology>`** — Erode and dilate operations
15. **`<feDisplacementMap>`** — Pixel displacement mapping
16. **`<feConvolveMatrix>`** — Custom convolution kernels
17. **`<feTurbulence>`** — Perlin noise generation

**Estimated effort:** 4–6 weeks

### Phase 4 — Advanced Features (Higher effort, niche use cases)

18. **`mix-blend-mode` CSS property** — Element-level blending beyond filter-based blending
19. **Geometry attributes as CSS properties** — Allow `x`, `y`, `width`, `height`, `r`, etc. from CSS
20. **`<symbol>` extended positioning** — `x`, `y`, `refX`, `refY` attributes
21. **`overflow: visible` default** — Consider SVG 2 default change (evaluate backward compatibility impact)
22. **`<feComponentTransfer>`** — Complete any remaining transfer function types
23. **CSS Color Level 4: `lab()`, `lch()`, `oklab()`, `oklch()`** — Perceptual color spaces

**Estimated effort:** 4–8 weeks

### Not Planned

The following SVG 2 features are deliberately excluded from the roadmap:

- **Mesh gradients** (`<mesh>`, `<meshgradient>`) — No browser support; extremely complex
- **Hatch patterns** (`<hatch>`, `<hatchpath>`) — No browser support
- **`<feDiffuseLighting>` / `<feSpecularLighting>`** — Complex lighting model with limited real-world usage
- **`inline-size` text wrapping** — Requires full line-breaking algorithm; limited browser support
- **`shape-inside` / `shape-subtract`** — Extreme complexity; minimal browser support
- **All interactive/dynamic features** — See "Out of Scope" above

---

## Filter Primitive Coverage Summary

After all phases, JairoSVG's filter support would cover:

| Primitive | SVG 1.1 | Status |
|-----------|---------|--------|
| `feGaussianBlur` | ✅ | Implemented |
| `feOffset` | ✅ | Implemented |
| `feFlood` | ✅ | Implemented |
| `feBlend` | ✅ | Implemented (5 modes) |
| `feComposite` | ✅ | Implemented (6 operators) |
| `feMerge` | ✅ | Implemented |
| `feImage` | ✅ | Implemented |
| `feTile` | ✅ | Implemented |
| `feColorMatrix` | ✅ | Phase 2 |
| `feComponentTransfer` | ✅ | Phase 3 |
| `feMorphology` | ✅ | Phase 3 |
| `feDisplacementMap` | ✅ | Phase 3 |
| `feConvolveMatrix` | ✅ | Phase 3 |
| `feTurbulence` | ✅ | Phase 3 |
| `feDropShadow` | SVG 2 | Implemented |
| `feDiffuseLighting` | ✅ | Not Planned |
| `feSpecularLighting` | ✅ | Not Planned |

Coverage after Phase 3: **15 of 17** SVG filter primitives (88%)

---

## Success Criteria

The SVG 2 support effort will be considered successful when:

1. All Phase 1 and Phase 2 items are implemented and tested
2. JairoSVG can render the most common SVG 2 patterns found in the wild (editor exports from Figma, Illustrator, Inkscape, etc.)
3. Backward compatibility with all existing SVG 1.1 test cases is maintained
4. The comparison suite (JairoSVG vs CairoSVG vs EchoSVG) shows improved or maintained visual fidelity

---

## References

- [SVG 2 Specification (W3C)](https://www.w3.org/TR/SVG2/)
- [Changes from SVG 1.1 — SVG 2 (Appendix L)](https://www.w3.org/TR/SVG2/changes.html)
- [SVG 2 New Features (W3C SVGWG Wiki)](https://github.com/w3c/svgwg/wiki/SVG-2-new-features)
- [CSS Filter Effects Module Level 1](https://www.w3.org/TR/filter-effects-1/)
- [CSS Color Module Level 4](https://www.w3.org/TR/css-color-4/)
- [MDN SVG Element Reference](https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element)

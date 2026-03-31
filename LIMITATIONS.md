# Project Limitations

JairoSVG focuses on SVG 1.1 rendering with Java2D and currently has a few intentional limitations.

## No Animation Timeline Support

- JairoSVG is a static renderer and does not evaluate SVG animation timelines.
- SMIL animation elements are not supported, including `<animate>`, `<animateTransform>`, `<animateMotion>`, and `<set>`.

## Unsupported Embedded Content

- `<foreignObject>` is not rendered.
- Embedded non-SVG content inside `<foreignObject>` (such as XHTML/HTML) is skipped gracefully.

A full implementation of `<foreignObject>` would require integrating an external HTML/XML layout and rendering engine, which is currently out of scope.

## Unsupported Scripting

- `<script>` elements are not executed.

JairoSVG intentionally omits SVG scripting support as a security measure. Supporting `<script>` would require embedding a JavaScript engine and exposing SVG DOM APIs, which is currently out of scope.

## SVG 2 Behavioral Alignment

JairoSVG targets SVG 1.1 but selectively adopts SVG 2 behavioral changes where modern browsers have diverged from SVG 1.1 and strict 1.1 compliance would produce visually incorrect output compared to browser rendering.

Current SVG 2 alignments:

- **`requiredFeatures` ignored**: SVG 2 deprecated the `requiredFeatures` conditional processing attribute. Modern browsers ignore it entirely, so JairoSVG does the same. This primarily affects `<switch>` elements — browsers render the first child regardless of `requiredFeatures`, and JairoSVG now matches that behavior. The `systemLanguage` and `requiredExtensions` attributes are unaffected and still evaluated per SVG 1.1/SVG 2.

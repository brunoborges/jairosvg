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

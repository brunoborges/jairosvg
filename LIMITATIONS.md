# Project Limitations

JairoSVG focuses on SVG 1.1 rendering with Java2D and currently has a few intentional limitations.

## Unsupported Embedded Content

- `<foreignObject>` is not rendered.
- Embedded non-SVG content inside `<foreignObject>` (such as XHTML/HTML) is skipped gracefully.

A full implementation of `<foreignObject>` would require integrating an external HTML/XML layout and rendering engine, which is currently out of scope.

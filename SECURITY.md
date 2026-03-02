# Security Policy

## Reporting a Vulnerability

Please report security vulnerabilities by opening a [GitHub Issue](https://github.com/brunoborges/jairosvg/issues) with the `security` label, or contact the maintainers directly.

## XML Security

JairoSVG processes SVG files which are XML documents. By default, the parser is hardened against:

- **XML External Entity (XXE) attacks**: DOCTYPE declarations are disallowed
- **XML bomb / billion laughs**: Secure processing features are enabled

### The `unsafe` flag

The `--unsafe` flag (CLI) or `unsafe` parameter (API) disables these protections to allow processing SVGs that use DOCTYPE declarations. **Only use this with trusted input.**

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.x     | ✅        |

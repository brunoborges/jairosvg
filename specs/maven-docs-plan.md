# JairoSVG — Maven Setup, Documentation & GitHub Pages

## Goal
Elevate JairoSVG from a working prototype to a polished open-source project with production-grade Maven configuration, beautiful documentation, and GitHub Pages site.

## Learnings from copilot-sdk-java

**What it does well (adopt):**
- Maven Central publishing via `central-publishing-maven-plugin` with `release` profile
- GPG signing for artifact verification
- Source + Javadoc JAR generation
- Spotless code formatting (Eclipse formatter)
- Maven Wrapper (`mvnw`) for build reproducibility
- Proper POM metadata: `<url>`, `<licenses>`, `<scm>`, `<developers>`, `<description>`

**What it lacks (go beyond):**
- No CI/CD workflows
- No GitHub Pages / documentation site
- No CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md
- No badges in README
- No Maven site generation

---

## Implementation Plan

### Phase 1: POM Overhaul
- [ ] Add full POM metadata: `<url>`, `<licenses>` (LGPLv3), `<scm>`, `<developers>`, `<organization>`, `<issueManagement>`, `<distributionManagement>`
- [ ] Switch to `maven.compiler.release=25` (replaces source/target)
- [ ] Add **release** profile with:
  - `maven-source-plugin` — attach source JAR
  - `maven-javadoc-plugin` — attach Javadoc JAR (with `--enable-preview` doclint config)
  - `maven-gpg-plugin` — artifact signing
  - `central-publishing-maven-plugin` — Maven Central deploy
- [ ] Add `spotless-maven-plugin` for consistent formatting (Eclipse 4.33, 4-space indentation, remove unused imports)
- [ ] Add Maven Wrapper (`mvnw` / `mvnw.cmd`) via `mvn wrapper:wrapper`
- [ ] Add `maven-shade-plugin` or `maven-assembly-plugin` for fat JAR with dependencies (CLI use)
- [ ] Add `maven-site-plugin` + `maven-project-info-reports-plugin` for Maven site generation
- [ ] Add reproducible build config: `<project.build.outputTimestamp>`

### Phase 2: GitHub Actions CI/CD
- [ ] `.github/workflows/ci.yml` — Build + test on push/PR (matrix: Java 25, ubuntu/macos/windows)
- [ ] `.github/workflows/release.yml` — Publish to Maven Central on GitHub Release (uses release profile, GPG from secrets)
- [ ] `.github/workflows/site.yml` — Build and deploy documentation to GitHub Pages on push to main
- [ ] Add status badges to README (CI, Maven Central version, License, Java version)

### Phase 3: GitHub Pages Documentation Site
- [ ] Use **Maven Site** with a modern skin (e.g., `maven-fluido-skin`) for auto-generated reports
- [ ] Create `src/site/site.xml` with custom navigation: Home, Getting Started, API Reference, CLI, Architecture, Benchmark, Contributing
- [ ] Create `src/site/markdown/` pages:
  - `index.md` — Landing page with hero banner, feature highlights, quick install snippet
  - `getting-started.md` — Installation (Maven/Gradle/JBang), first conversion, CLI usage
  - `api-reference.md` — Full builder API docs with examples for each output format
  - `cli.md` — Detailed CLI reference with examples
  - `architecture.md` — Module mapping table, rendering pipeline diagram, design decisions
  - `benchmark.md` — JairoSVG vs EchoSVG benchmark results with charts
  - `faq.md` — Common questions, limitations, troubleshooting
- [ ] Add Javadoc generation linked from site (`maven-javadoc-plugin` report)
- [ ] Add test coverage report (`jacoco-maven-plugin`)
- [ ] Configure GitHub Pages deploy from `gh-pages` branch via the site workflow

### Phase 4: README Overhaul
- [ ] Add badges: CI status, Maven Central, License (LGPLv3), Java 25
- [ ] Add logo/banner (SVG-based, naturally!)
- [ ] Restructure with sections:
  - Overview + visual example (show SVG → PNG conversion with actual image)
  - Installation (Maven, Gradle, JBang one-liner)
  - Quick Start (3 usage patterns: static methods, builder API, CLI)
  - Benchmark results table (vs EchoSVG)
  - Supported SVG features checklist
  - Link to full documentation site
  - Contributing section
  - License
- [ ] Add JBang install example: `jbang io.brunoborges:jairosvg:1.0.2 input.svg -o output.png`

### Phase 5: Community Health Files
- [ ] `LICENSE` — LGPLv3 full text file
- [ ] `CONTRIBUTING.md` — How to build, test, submit PRs, code style (reference Spotless)
- [ ] `CODE_OF_CONDUCT.md` — Contributor Covenant
- [ ] `SECURITY.md` — Security policy (XML external entity handling, `unsafe` flag)
- [ ] `.github/ISSUE_TEMPLATE/bug_report.md` — Bug template (SVG input, expected/actual output)
- [ ] `.github/ISSUE_TEMPLATE/feature_request.md` — Feature request template
- [ ] `.github/PULL_REQUEST_TEMPLATE.md` — PR checklist (tests, formatting, docs)

### Phase 6: Extras
- [ ] Add `dependabot.yml` for automated dependency updates
- [ ] Add `.editorconfig` for cross-editor consistency
- [ ] Consider GraalVM native-image compatibility note/profile for AOT CLI usage
- [ ] Add JBang catalog entry (`jbang-catalog.json`) for `jbang jairosvg@yourorg input.svg`

---

## Key Differences vs copilot-sdk-java

| Aspect | copilot-sdk-java | JairoSVG (planned) |
|--------|------------------|-------------------|
| POM metadata | ✅ Complete | ⬜ Needs all metadata |
| Maven Central publishing | ✅ Release profile | ⬜ Adopt same approach |
| Spotless formatting | ✅ | ⬜ Add |
| Maven Wrapper | ✅ | ⬜ Add |
| CI/CD | ❌ None | ⬜ Full matrix CI + release |
| GitHub Pages | ❌ None | ⬜ Maven site + fluido skin |
| Javadoc site | ❌ JAR only | ⬜ Published on Pages |
| Community files | ❌ None | ⬜ Full set |
| README badges | ❌ None | ⬜ CI, version, license, Java |
| Test coverage | ❌ None | ⬜ JaCoCo reports on site |
| Fat JAR / CLI distribution | ❌ Not applicable | ⬜ Shade plugin for CLI |

## Approach
- Use **Java2D** (`Graphics2D`, `AffineTransform`, `BufferedImage`) as the rendering engine (replaces Cairo)
- Use **Apache PDFBox** for PDF output
- Use Java's built-in **javax.xml.parsers** (with security hardening) for XML parsing
- Implement minimal CSS parsing for SVG inline styles
- Use **javax.imageio** for PNG/image I/O
- Leverage Java 25 features: records, pattern matching, sealed interfaces, text blocks
- Maven project with `io.brunoborges.jairosvg` package

## Module Mapping (Python → Java)
| Python Module     | Java Class(es)                          |
|-------------------|-----------------------------------------|
| `__init__.py`     | `JairoSVG` (public API)                 |
| `surface.py`      | `Surface`, `PngSurface`, `PdfSurface`, `PsSurface`, `EpsSurface` |
| `parser.py`       | `Node`, `Tree`                          |
| `colors.py`       | `Colors`                                |
| `helpers.py`      | `Helpers`                               |
| `css.py`          | `CssProcessor`                          |
| `defs.py`         | `Defs`                                  |
| `path.py`         | `PathDrawer`                            |
| `shapes.py`       | `ShapeDrawer`                           |
| `text.py`         | `TextDrawer`                            |
| `image.py`        | `ImageHandler`                          |
| `url.py`          | `UrlHelper`                             |
| `bounding_box.py` | `BoundingBox`                           |
| `features.py`     | `Features`                              |
| `svg.py`          | `SvgDrawer`                             |
| `__main__.py`     | `Main` (CLI)                            |

## Todos
1. **project-setup**: Maven project, pom.xml, directory structure
2. **colors**: Port SVG color parsing (named colors, hex, rgb, rgba)
3. **url-helper**: Port URL handling utilities
4. **features**: Port SVG feature detection
5. **helpers**: Port surface helpers (size, transform, normalize, etc.)
6. **bounding-box**: Port bounding box calculations
7. **css-processor**: Port CSS parsing (minimal for SVG)
8. **parser-node-tree**: Port SVG parser, Node and Tree classes
9. **shape-drawer**: Port shape drawing (rect, circle, ellipse, line, polygon, polyline)
10. **path-drawer**: Port SVG path command parser and drawer
11. **text-drawer**: Port text rendering
12. **image-handler**: Port image handling
13. **defs**: Port definitions (gradients, patterns, clips, masks, filters, markers)
14. **surface-core**: Port abstract Surface class with Java2D rendering
15. **svg-drawer**: Port root SVG element handler
16. **png-surface**: PNG output surface
17. **pdf-surface**: PDF output surface (using PDFBox)
18. **public-api**: JairoSVG main API class (svg2png, svg2pdf, etc.)
20. **cli**: Command-line interface
21. **tests**: Basic integration tests

## Notes
- Cairo Matrix → `java.awt.geom.AffineTransform`
- Cairo Context → `java.awt.Graphics2D`
- Cairo surfaces → `BufferedImage` for PNG, PDFBox for PDF, custom XML writer for SVG
- cairocffi constants → Java2D equivalents (BasicStroke, RenderingHints, etc.)
- cssselect2 → Basic CSS matcher implementation
- tinycss2 → Basic CSS declaration parser
- defusedxml → Secure XMLInputFactory/DocumentBuilderFactory
- PIL/Pillow → javax.imageio + BufferedImage

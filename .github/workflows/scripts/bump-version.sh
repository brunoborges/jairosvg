#!/usr/bin/env bash
#
# bump-version.sh — Update version references across the JairoSVG repo.
#
# Usage:
#   ./.github/workflows/scripts/bump-version.sh release 1.0.2        # Before tagging: set release version everywhere
#   ./.github/workflows/scripts/bump-version.sh snapshot 1.0.3        # After tagging: bump to next SNAPSHOT
#
# "release"  → updates all user-facing docs/code from the OLD release to the NEW release version
# "snapshot" → updates pom.xml and dev scripts from the OLD snapshot to the NEW snapshot version
#
set -euo pipefail

MODE="${1:-}"
NEW_VERSION="${2:-}"

if [[ -z "$MODE" || -z "$NEW_VERSION" ]]; then
    echo "Usage: $0 {release|snapshot} <new-version>"
    echo ""
    echo "Examples:"
    echo "  $0 release  1.0.2    # Update all release references to 1.0.2"
    echo "  $0 snapshot 1.0.3    # Update pom.xml & dev scripts to 1.0.3-SNAPSHOT"
    exit 1
fi

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"

# Portable sed -i with extended regex (macOS vs GNU)
sedi() {
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' -E "$@"
    else
        sed -i -E "$@"
    fi
}

if [[ "$MODE" == "release" ]]; then
    echo "Bumping release version → $NEW_VERSION"
    echo ""

    # Regex matching any jairosvg version (with optional -SNAPSHOT suffix, possibly repeated)
    # Uses extended regex via sed -E for portability
    VER_RE='[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)*'

    # 1. pom.xml — set release version (first <version> tag only)
    sedi "1,/<version>/s|<version>${VER_RE}</version>|<version>$NEW_VERSION</version>|" "$ROOT/pom.xml"
    echo "  ✓ pom.xml"

    # 2. README.md — Maven, Gradle, JBang, CLI examples
    sedi "s|io\.brunoborges:jairosvg:${VER_RE}|io.brunoborges:jairosvg:$NEW_VERSION|g" "$ROOT/README.md"
    sedi "/artifactId>jairosvg/{ n; s|<version>${VER_RE}</version>|<version>$NEW_VERSION</version>|; }" "$ROOT/README.md"
    sedi "s|jairosvg-${VER_RE}-cli\.jar|jairosvg-$NEW_VERSION-cli.jar|g" "$ROOT/README.md"
    echo "  ✓ README.md"

    # 3. jbang-catalog.json
    sedi "s|io\.brunoborges:jairosvg:${VER_RE}|io.brunoborges:jairosvg:$NEW_VERSION|g" "$ROOT/jbang-catalog.json"
    echo "  ✓ jbang-catalog.json"

    # 4. comparison/README.md — version in comparison table (only in "Current version" row)
    sedi "/Current version/s|${VER_RE}|$NEW_VERSION|" "$ROOT/comparison/README.md"
    echo "  ✓ comparison/README.md"

    # 5. Site docs
    for f in index.md getting-started.md; do
        filepath="$ROOT/src/site/markdown/$f"
        if [[ -f "$filepath" ]]; then
            sedi "s|io\.brunoborges:jairosvg:${VER_RE}|io.brunoborges:jairosvg:$NEW_VERSION|g" "$filepath"
            sedi "/artifactId>jairosvg/{ n; s|<version>${VER_RE}</version>|<version>$NEW_VERSION</version>|; }" "$filepath"
            sedi "s|jairosvg-${VER_RE}-cli\.jar|jairosvg-$NEW_VERSION-cli.jar|g" "$filepath"
            echo "  ✓ src/site/markdown/$f"
        fi
    done

    # 6. comparison JBang scripts — set to release version (no -SNAPSHOT)
    for f in benchmark/benchmark.java visual/generate.java profiling/profile.java; do
        filepath="$ROOT/comparison/$f"
        if [[ -f "$filepath" ]]; then
            sedi "s|io\.brunoborges:jairosvg:${VER_RE}|io.brunoborges:jairosvg:$NEW_VERSION|g" "$filepath"
            echo "  ✓ comparison/$f"
        fi
    done

    echo ""
    echo "Done. Release version updated to $NEW_VERSION"
    echo "Next step: commit, tag, and run JReleaser."

elif [[ "$MODE" == "snapshot" ]]; then
    NEW_SNAPSHOT="${NEW_VERSION}-SNAPSHOT"

    echo "Bumping snapshot version → $NEW_SNAPSHOT"
    echo ""

    # Regex matching any jairosvg version (with optional -SNAPSHOT suffix)
    VER_RE='[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)*'

    # 1. pom.xml (first <version> tag only)
    sedi "1,/<version>/s|<version>${VER_RE}</version>|<version>$NEW_SNAPSHOT</version>|" "$ROOT/pom.xml"
    echo "  ✓ pom.xml"

    # 2. comparison JBang scripts
    for f in benchmark/benchmark.java visual/generate.java profiling/profile.java; do
        filepath="$ROOT/comparison/$f"
        if [[ -f "$filepath" ]]; then
            sedi "s|io\.brunoborges:jairosvg:${VER_RE}|io.brunoborges:jairosvg:$NEW_SNAPSHOT|g" "$filepath"
            echo "  ✓ comparison/$f"
        fi
    done

    echo ""
    echo "Done. Snapshot version updated to $NEW_SNAPSHOT"
    echo "Next step: commit and push."

else
    echo "ERROR: Unknown mode '$MODE'. Use 'release' or 'snapshot'."
    exit 1
fi

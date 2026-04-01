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

# Portable sed -i (macOS vs GNU)
sedi() {
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

if [[ "$MODE" == "release" ]]; then
    # ── Detect the current release version from pom.xml ──
    OLD_SNAPSHOT=$(grep '<version>' "$ROOT/pom.xml" | head -1 | sed 's/.*<version>//;s/<\/version>.*//' | tr -d ' ')
    OLD_VERSION="${OLD_SNAPSHOT%-SNAPSHOT}"

    if [[ -z "$OLD_VERSION" ]]; then
        echo "ERROR: Could not detect current version from pom.xml"
        exit 1
    fi

    echo "Bumping release version: $OLD_VERSION → $NEW_VERSION"
    echo ""

    # 1. pom.xml — set release version (remove -SNAPSHOT)
    sedi "s|<version>$OLD_SNAPSHOT</version>|<version>$NEW_VERSION</version>|" "$ROOT/pom.xml"
    echo "  ✓ pom.xml"

    # 2. README.md — Maven, Gradle, JBang, CLI examples
    sedi "s|io.brunoborges:jairosvg:$OLD_VERSION|io.brunoborges:jairosvg:$NEW_VERSION|g" "$ROOT/README.md"
    sedi "s|<version>$OLD_VERSION</version>|<version>$NEW_VERSION</version>|g" "$ROOT/README.md"
    sedi "s|jairosvg-$OLD_VERSION-cli.jar|jairosvg-$NEW_VERSION-cli.jar|g" "$ROOT/README.md"
    echo "  ✓ README.md"

    # 3. jbang-catalog.json
    sedi "s|io.brunoborges:jairosvg:$OLD_VERSION|io.brunoborges:jairosvg:$NEW_VERSION|g" "$ROOT/jbang-catalog.json"
    echo "  ✓ jbang-catalog.json"

    # 4. comparison/README.md — version in comparison table
    sedi "s|$OLD_VERSION|$NEW_VERSION|g" "$ROOT/comparison/README.md"
    echo "  ✓ comparison/README.md"

    # 5. Site docs
    for f in index.md getting-started.md; do
        filepath="$ROOT/src/site/markdown/$f"
        if [[ -f "$filepath" ]]; then
            sedi "s|io.brunoborges:jairosvg:$OLD_VERSION|io.brunoborges:jairosvg:$NEW_VERSION|g" "$filepath"
            sedi "s|<version>$OLD_VERSION</version>|<version>$NEW_VERSION</version>|g" "$filepath"
            sedi "s|jairosvg-$OLD_VERSION-cli.jar|jairosvg-$NEW_VERSION-cli.jar|g" "$filepath"
            echo "  ✓ src/site/markdown/$f"
        fi
    done

    # 6. comparison JBang scripts
    for f in benchmark/benchmark.java visual/generate.java; do
        filepath="$ROOT/comparison/$f"
        if [[ -f "$filepath" ]]; then
            sedi "s|io.brunoborges:jairosvg:$OLD_VERSION|io.brunoborges:jairosvg:$NEW_VERSION|g" "$filepath"
            echo "  ✓ comparison/$f"
        fi
    done

    echo ""
    echo "Done. Release version updated to $NEW_VERSION"
    echo "Next step: commit, tag, and run JReleaser."

elif [[ "$MODE" == "snapshot" ]]; then
    NEW_SNAPSHOT="${NEW_VERSION}-SNAPSHOT"

    # ── Detect old version from pom.xml ──
    OLD_POM_VERSION=$(grep '<version>' "$ROOT/pom.xml" | head -1 | sed 's/.*<version>//;s/<\/version>.*//' | tr -d ' ')

    if [[ -z "$OLD_POM_VERSION" ]]; then
        echo "ERROR: Could not detect current version from pom.xml"
        exit 1
    fi

    echo "Bumping snapshot version: $OLD_POM_VERSION → $NEW_SNAPSHOT"
    echo ""

    # 1. pom.xml
    sedi "s|<version>$OLD_POM_VERSION</version>|<version>$NEW_SNAPSHOT</version>|" "$ROOT/pom.xml"
    echo "  ✓ pom.xml"

    # 2. comparison JBang scripts
    for f in benchmark/benchmark.java visual/generate.java; do
        filepath="$ROOT/comparison/$f"
        if [[ -f "$filepath" ]]; then
            sedi "s|io.brunoborges:jairosvg:$OLD_POM_VERSION|io.brunoborges:jairosvg:$NEW_SNAPSHOT|g" "$filepath"
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

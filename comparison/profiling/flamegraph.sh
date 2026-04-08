#!/usr/bin/env bash
# Generate async-profiler CPU flame charts for JairoSVG and JSVG.
#
# Usage:
#   ./comparison/profiling/flamegraph.sh [scenario] [iterations] [mode]
#
# Mode: "render" (default) = SVG parse+render only, no PNG encode
#       "full"             = parse+render+PNG encode (matches benchmark)
#
# Examples:
#   ./comparison/profiling/flamegraph.sh gradients 3000 render
#   ./comparison/profiling/flamegraph.sh masks 3000 full
#   ./comparison/profiling/flamegraph.sh          # profiles all default scenarios in render mode
#
# Output: comparison/profiling/flamegraphs/<scenario>-<library>-<mode>.html

set -euo pipefail

ASYNC_PROFILER_LIB="/opt/homebrew/lib/libasyncProfiler.dylib"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$SCRIPT_DIR/flamegraphs"
ITERATIONS="${2:-3000}"
MODE="${3:-render}"

# Scenarios to profile when no argument given
DEFAULT_SCENARIOS=("gradients" "masks" "text_rendering" "complex_paths" "clip_paths")

if [[ -n "${1:-}" ]]; then
    SCENARIOS=("$1")
else
    SCENARIOS=("${DEFAULT_SCENARIOS[@]}")
fi

mkdir -p "$OUT_DIR"

profile_one() {
    local scenario="$1"
    local library="$2"
    local outfile="$OUT_DIR/${scenario}-${library}-${MODE}.html"

    echo ""
    echo "=== Profiling: $scenario | $library | mode=$MODE ==="

    jbang \
        -R="-agentpath:${ASYNC_PROFILER_LIB}=start,event=cpu,file=${outfile},flamegraph,title=${scenario}-${library}-${MODE}" \
        "$SCRIPT_DIR/profile.java" \
        "$scenario" \
        "$library" \
        "$ITERATIONS" \
        "$MODE"

    echo "  → saved: $outfile"
}

echo "Flame chart generator — async-profiler 4.x"
echo "Output dir: $OUT_DIR"
echo "Iterations: $ITERATIONS  Mode: $MODE"

for scenario in "${SCENARIOS[@]}"; do
    profile_one "$scenario" "jairosvg"
    profile_one "$scenario" "jsvg"
done

echo ""
echo "Done! Open in a browser:"
ls -1 "$OUT_DIR"/*.html 2>/dev/null | sed 's/^/  /'

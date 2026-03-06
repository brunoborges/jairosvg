#!/usr/bin/env bash
# A/B benchmark: mask rendering between two branches.
# Usage: ./scripts/bench-mask.sh [warmup] [iterations]
#   defaults: warmup=30, iterations=2000
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

WARMUP=${1:-30}
ITERS=${2:-2000}
BRANCH_A="main"
BRANCH_B="copilot/optimize-mask-luminance-loop"
SVG_FILE="comparison/svg/15_masks.svg"
ORIG_BRANCH=$(git branch --show-current)
STASH_NEEDED=false

# Stash any uncommitted changes
if ! git diff --quiet || ! git diff --cached --quiet; then
    STASH_NEEDED=true
    git stash push -q -m "bench-mask temp stash"
fi

cleanup() {
    git checkout -q "$ORIG_BRANCH" 2>/dev/null || true
    if $STASH_NEEDED; then
        git stash pop -q 2>/dev/null || true
    fi
}
trap cleanup EXIT

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  JairoSVG Mask Rendering A/B Benchmark                     ║"
echo "║  A: $BRANCH_A"
echo "║  B: $BRANCH_B"
echo "║  SVG: $SVG_FILE"
echo "║  Warmup: $WARMUP  Iterations: $ITERS"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# --- Build branch A ---
echo "▸ Building branch A ($BRANCH_A)..."
git checkout -q "$BRANCH_A"
./mvnw install -DskipTests -q 2>&1 | tail -1
JAR_A=$(find target -name 'jairosvg-*.jar' ! -name '*-sources*' ! -name '*-javadoc*' | head -1)
cp "$JAR_A" /tmp/jairosvg-branchA.jar
echo "  ✓ Built → /tmp/jairosvg-branchA.jar"

# --- Build branch B ---
echo "▸ Building branch B ($BRANCH_B)..."
git checkout -q "origin/$BRANCH_B"
./mvnw install -DskipTests -q 2>&1 | tail -1
JAR_B=$(find target -name 'jairosvg-*.jar' ! -name '*-sources*' ! -name '*-javadoc*' | head -1)
cp "$JAR_B" /tmp/jairosvg-branchB.jar
echo "  ✓ Built → /tmp/jairosvg-branchB.jar"

# Go back so SVG file is accessible
git checkout -q "$ORIG_BRANCH"

# --- Write the benchmark Java source ---
BENCH_SRC=/tmp/MaskBench.java
cat > "$BENCH_SRC" << 'JAVA_EOF'
import io.brunoborges.jairosvg.JairoSVG;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class MaskBench {
    public static void main(String[] args) throws Exception {
        int warmup = Integer.parseInt(args[0]);
        int iters = Integer.parseInt(args[1]);
        String svg = Files.readString(Path.of(args[2]));
        byte[] svgBytes = svg.getBytes(StandardCharsets.UTF_8);

        // Warmup
        for (int i = 0; i < warmup; i++) {
            JairoSVG.svg2png(svgBytes);
        }
        System.gc();
        Thread.sleep(200);

        // Measure
        double[] times = new double[iters];
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            JairoSVG.svg2png(svgBytes);
            long t1 = System.nanoTime();
            times[i] = (t1 - t0) / 1_000_000.0;
        }

        Arrays.sort(times);
        double avg = Arrays.stream(times).average().orElse(0);
        double median = times[times.length / 2];
        double p5 = times[(int)(times.length * 0.05)];
        double p95 = times[(int)(times.length * 0.95)];
        double min = times[0];
        double max = times[times.length - 1];

        // Output as parseable line
        System.out.printf("RESULT avg=%.4f median=%.4f p5=%.4f p95=%.4f min=%.4f max=%.4f%n",
                avg, median, p5, p95, min, max);
    }
}
JAVA_EOF

JAVA_OPTS="--enable-preview --source 25"

echo ""
echo "▸ Running benchmark A ($BRANCH_A): warmup=$WARMUP, iterations=$ITERS..."
RESULT_A=$(java $JAVA_OPTS -cp /tmp/jairosvg-branchA.jar "$BENCH_SRC" "$WARMUP" "$ITERS" "$SVG_FILE" | grep "^RESULT")
echo "  $RESULT_A"

echo ""
echo "▸ Running benchmark B ($BRANCH_B): warmup=$WARMUP, iterations=$ITERS..."
RESULT_B=$(java $JAVA_OPTS -cp /tmp/jairosvg-branchB.jar "$BENCH_SRC" "$WARMUP" "$ITERS" "$SVG_FILE" | grep "^RESULT")
echo "  $RESULT_B"

# Parse results
AVG_A=$(echo "$RESULT_A" | sed 's/.*avg=\([^ ]*\).*/\1/')
AVG_B=$(echo "$RESULT_B" | sed 's/.*avg=\([^ ]*\).*/\1/')
MED_A=$(echo "$RESULT_A" | sed 's/.*median=\([^ ]*\).*/\1/')
MED_B=$(echo "$RESULT_B" | sed 's/.*median=\([^ ]*\).*/\1/')
P95_A=$(echo "$RESULT_A" | sed 's/.*p95=\([^ ]*\).*/\1/')
P95_B=$(echo "$RESULT_B" | sed 's/.*p95=\([^ ]*\).*/\1/')

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  COMPARISON (all times in ms)"
echo "───────────────────────────────────────────────────────────────"
printf "  %-12s %12s %12s %12s\n" "" "avg" "median" "p95"
printf "  %-12s %12s %12s %12s\n" "A (main)" "$AVG_A" "$MED_A" "$P95_A"
printf "  %-12s %12s %12s %12s\n" "B (optimized)" "$AVG_B" "$MED_B" "$P95_B"
echo "───────────────────────────────────────────────────────────────"

# Compute speedup using awk
awk "BEGIN {
    a=$AVG_A; b=$AVG_B;
    if (b < a) {
        printf \"  Avg speedup: %.2fx faster (B over A) — saved %.4f ms/render\n\", a/b, a-b
    } else if (a < b) {
        printf \"  Avg regression: %.2fx slower (B vs A) — added %.4f ms/render\n\", b/a, b-a
    } else {
        print \"  No measurable difference\"
    }
}"
awk "BEGIN {
    a=$MED_A; b=$MED_B;
    if (b < a) {
        printf \"  Median speedup: %.2fx faster (B over A)\n\", a/b
    } else if (a < b) {
        printf \"  Median regression: %.2fx slower (B vs A)\n\", b/a
    } else {
        print \"  Median: no measurable difference\"
    }
}"
echo "═══════════════════════════════════════════════════════════════"

# Cleanup temp files
rm -f /tmp/jairosvg-branchA.jar /tmp/jairosvg-branchB.jar "$BENCH_SRC"

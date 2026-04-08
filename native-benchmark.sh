#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGE_NAME="jairosvg-native"

echo "=== Building native image container ==="
docker build -f "$SCRIPT_DIR/Dockerfile.native" -t "$IMAGE_NAME" "$SCRIPT_DIR"

echo ""
echo "=== Smoke test ==="
docker run --rm "$IMAGE_NAME" --help

echo ""
echo "=== Benchmark (running inside container) ==="
docker run --rm --entrypoint /bin/bash "$IMAGE_NAME" -c '
  OUT_DIR=$(mktemp -d)
  printf "%-40s %10s\n" "SVG Test Case" "Time (ms)"
  printf "%-40s %10s\n" "$(printf "%0.s-" $(seq 1 40))" "----------"
  total_ms=0
  count=0
  failed=0
  for svg in comparison/svg/*.svg; do
    name=$(basename "$svg" .svg)
    out="$OUT_DIR/${name}.png"
    start=$(date +%s%N)
    if /app/jairosvg "$svg" -o "$out" 2>/dev/null; then
      end=$(date +%s%N)
      elapsed_ms=$(( (end - start) / 1000000 ))
      total_ms=$((total_ms + elapsed_ms))
      count=$((count + 1))
      printf "%-40s %10d\n" "$name" "$elapsed_ms"
    else
      end=$(date +%s%N)
      elapsed_ms=$(( (end - start) / 1000000 ))
      failed=$((failed + 1))
      printf "%-40s %10s\n" "$name" "FAILED (${elapsed_ms}ms)"
    fi
  done
  printf "%-40s %10s\n" "$(printf "%0.s-" $(seq 1 40))" "----------"
  printf "%-40s %10d\n" "TOTAL ($count passed, $failed failed)" "$total_ms"
  if [ "$count" -gt 0 ]; then
    avg=$((total_ms / count))
    printf "%-40s %10d\n" "AVERAGE" "$avg"
  fi
  rm -rf "$OUT_DIR"
'

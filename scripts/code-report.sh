#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# JairoSVG Code Metrics Report
# Generates a summary of lines of code, classes, methods,
# records, enums, and interfaces across the Java source tree.
# ──────────────────────────────────────────────────────────────
set -euo pipefail

BOLD='\033[1m'
DIM='\033[2m'
CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RESET='\033[0m'

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || echo ".")"
MAIN_SRC="$ROOT/src/main/java"
TEST_SRC="$ROOT/src/test/java"

# ── helpers ──────────────────────────────────────────────────

count_lines() {
    # Total lines, blank lines, comment lines, code lines
    local dir="$1"
    local total=0 blank=0 comment=0 code=0
    while IFS= read -r file; do
        local in_block=0
        while IFS= read -r line; do
            total=$((total + 1))
            stripped="${line#"${line%%[![:space:]]*}"}" # trim leading ws
            if [[ -z "$stripped" ]]; then
                blank=$((blank + 1))
            elif [[ $in_block -eq 1 ]]; then
                comment=$((comment + 1))
                [[ "$stripped" == *"*/" ]] && in_block=0
            elif [[ "$stripped" == "/*"* ]]; then
                comment=$((comment + 1))
                [[ "$stripped" != *"*/" ]] && in_block=1
            elif [[ "$stripped" == "//"* ]]; then
                comment=$((comment + 1))
            else
                code=$((code + 1))
            fi
        done < "$file"
    done < <(find "$dir" -name "*.java" 2>/dev/null)
    echo "$total $blank $comment $code"
}

separator() {
    printf '%0.s─' {1..72}
    echo
}

# ── per-file metrics (class / methods / lines) ──────────────

print_file_metrics() {
    local dir="$1"
    local label="$2"

    printf "\n${BOLD}${CYAN}%s${RESET}\n" "$label"
    separator
    printf "${BOLD}%-40s %7s %7s %7s${RESET}\n" "Class/File" "Lines" "Methods" "Fields"
    separator

    local total_files=0 total_lines=0 total_methods=0 total_fields=0

    while IFS= read -r file; do
        local rel="${file#"$dir/"}"
        local name="${rel%.java}"
        name="${name//\//.}"

        local lines
        lines=$(wc -l < "$file" | tr -d ' ')

        # Count methods: lines with access modifier or type followed by name(
        # Exclude control flow keywords and annotations
        local methods
        methods=$(grep -E '^\s*(public|private|protected|static|final|abstract|synchronized|default|void|int|long|short|byte|char|float|double|boolean|String|var)[[:space:]]' "$file" 2>/dev/null \
            | grep -cE '[a-zA-Z_][a-zA-Z0-9_]*\s*\(' 2>/dev/null || echo 0)
        methods=$(echo "$methods" | tr -d '[:space:]')

        # Count field declarations (instance/static vars with access modifiers)
        local fields
        fields=$(grep -cE '^\s*(public|private|protected)\s+(static\s+)?(final\s+)?(volatile\s+)?(transient\s+)?.+\s+[a-zA-Z_][a-zA-Z0-9_]*\s*(=|;)' "$file" 2>/dev/null || echo 0)
        # Remove false positives: method-like lines with parens
        local field_fp
        field_fp=$(grep -E '^\s*(public|private|protected)\s+(static\s+)?(final\s+)?(volatile\s+)?(transient\s+)?.+\s+[a-zA-Z_][a-zA-Z0-9_]*\s*(=|;)' "$file" 2>/dev/null \
            | grep -cE '\(' 2>/dev/null || echo 0)
        fields=$(( $(echo "$fields" | tr -d '[:space:]') - $(echo "$field_fp" | tr -d '[:space:]') ))
        fields=$((fields < 0 ? 0 : fields))

        printf "%-40s %7d %7d %7d\n" "$name" "$lines" "$methods" "$fields"

        total_files=$((total_files + 1))
        total_lines=$((total_lines + lines))
        total_methods=$((total_methods + methods))
        total_fields=$((total_fields + fields))
    done < <(find "$dir" -name "*.java" | sort)

    separator
    printf "${BOLD}%-40s %7d %7d %7d${RESET}\n" "TOTAL ($total_files files)" "$total_lines" "$total_methods" "$total_fields"
}

# ── type declarations ────────────────────────────────────────

count_type_decls() {
    local dir="$1"
    local classes interfaces enums records annotations
    classes=$(grep -rlE '^\s*(public\s+|abstract\s+|final\s+)*(class)\s+' "$dir" --include='*.java' 2>/dev/null | wc -l | tr -d ' ')
    interfaces=$(grep -rcE '^\s*(public\s+)?interface\s+' "$dir" --include='*.java' 2>/dev/null | awk -F: '{s+=$2} END{print s+0}')
    enums=$(grep -rcE '^\s*(public\s+)?enum\s+' "$dir" --include='*.java' 2>/dev/null | awk -F: '{s+=$2} END{print s+0}')
    records=$(grep -rcE '^\s*(public\s+)?record\s+' "$dir" --include='*.java' 2>/dev/null | awk -F: '{s+=$2} END{print s+0}')
    annotations=$(grep -rcE '^\s*(public\s+)?@interface\s+' "$dir" --include='*.java' 2>/dev/null | awk -F: '{s+=$2} END{print s+0}')
    echo "$classes $interfaces $enums $records $annotations"
}

# ── main ─────────────────────────────────────────────────────

echo
printf "${BOLD}${GREEN}═══════════════════════════════════════════════════════════════════════${RESET}\n"
printf "${BOLD}${GREEN}  JairoSVG  ·  Code Metrics Report${RESET}\n"
printf "${BOLD}${GREEN}═══════════════════════════════════════════════════════════════════════${RESET}\n"
printf "${DIM}  Generated: $(date '+%Y-%m-%d %H:%M:%S')${RESET}\n"

# ── Source overview ──────────────────────────────────────────
main_count=$(find "$MAIN_SRC" -name "*.java" 2>/dev/null | wc -l | tr -d ' ')
test_count=$(find "$TEST_SRC" -name "*.java" 2>/dev/null | wc -l | tr -d ' ')

printf "\n${BOLD}${CYAN}Source Overview${RESET}\n"
separator
printf "  Main source files:  %d\n" "$main_count"
printf "  Test source files:  %d\n" "$test_count"
printf "  Total Java files:   %d\n" "$((main_count + test_count))"

# ── Lines of code breakdown ─────────────────────────────────
printf "\n${BOLD}${CYAN}Lines of Code (Main Sources)${RESET}\n"
separator
read -r m_total m_blank m_comment m_code <<< "$(count_lines "$MAIN_SRC")"
printf "  Total lines:     %6d\n" "$m_total"
printf "  Code lines:      %6d\n" "$m_code"
printf "  Comment lines:   %6d\n" "$m_comment"
printf "  Blank lines:     %6d\n" "$m_blank"

printf "\n${BOLD}${CYAN}Lines of Code (Test Sources)${RESET}\n"
separator
read -r t_total t_blank t_comment t_code <<< "$(count_lines "$TEST_SRC")"
printf "  Total lines:     %6d\n" "$t_total"
printf "  Code lines:      %6d\n" "$t_code"
printf "  Comment lines:   %6d\n" "$t_comment"
printf "  Blank lines:     %6d\n" "$t_blank"

printf "\n${BOLD}${CYAN}Lines of Code (Combined)${RESET}\n"
separator
printf "  Total lines:     %6d\n" "$((m_total + t_total))"
printf "  Code lines:      %6d\n" "$((m_code + t_code))"
printf "  Comment lines:   %6d\n" "$((m_comment + t_comment))"
printf "  Blank lines:     %6d\n" "$((m_blank + t_blank))"

# ── Type declarations ────────────────────────────────────────
printf "\n${BOLD}${CYAN}Type Declarations (Main Sources)${RESET}\n"
separator
read -r mc mi me mr ma <<< "$(count_type_decls "$MAIN_SRC")"
printf "  Classes:       %4d\n" "$mc"
printf "  Interfaces:    %4d\n" "$mi"
printf "  Enums:         %4d\n" "$me"
printf "  Records:       %4d\n" "$mr"
printf "  Annotations:   %4d\n" "$ma"

printf "\n${BOLD}${CYAN}Type Declarations (Test Sources)${RESET}\n"
separator
read -r tc ti te tr_ ta <<< "$(count_type_decls "$TEST_SRC")"
printf "  Classes:       %4d\n" "$tc"
printf "  Interfaces:    %4d\n" "$ti"
printf "  Enums:         %4d\n" "$te"
printf "  Records:       %4d\n" "$tr_"
printf "  Annotations:   %4d\n" "$ta"

# ── Per-file breakdown ───────────────────────────────────────
print_file_metrics "$MAIN_SRC" "Per-File Breakdown (Main Sources)"
print_file_metrics "$TEST_SRC" "Per-File Breakdown (Test Sources)"

# ── Largest files ────────────────────────────────────────────
printf "\n${BOLD}${CYAN}Top 10 Largest Files (by lines)${RESET}\n"
separator
printf "${BOLD}%-50s %7s${RESET}\n" "File" "Lines"
separator
find "$MAIN_SRC" "$TEST_SRC" -name "*.java" -exec wc -l {} + 2>/dev/null \
    | grep -v ' total$' \
    | sort -rn \
    | head -10 \
    | while read -r lines file; do
        rel="${file#"$ROOT/"}"
        printf "%-50s %7d\n" "$rel" "$lines"
    done

# ── Package breakdown ────────────────────────────────────────
printf "\n${BOLD}${CYAN}Package Breakdown (Main Sources)${RESET}\n"
separator
printf "${BOLD}%-40s %7s %7s${RESET}\n" "Package" "Files" "Lines"
separator
find "$MAIN_SRC" -name "*.java" -not -name "module-info.java" -print0 2>/dev/null | while IFS= read -r -d '' file; do
    dir=$(dirname "$file")
    rel="${dir#"$MAIN_SRC/"}"
    pkg="${rel//\//.}"
    lines=$(wc -l < "$file" | tr -d ' ')
    echo "$pkg $lines"
done | awk '{pkg[$1]++; lines[$1]+=$2} END{for(p in pkg) printf "%-40s %7d %7d\n", p, pkg[p], lines[p]}' | sort

separator
echo
printf "${DIM}  Report complete.${RESET}\n\n"

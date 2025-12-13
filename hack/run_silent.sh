#!/bin/bash
set -e  # Exit immediately if any command fails

# Helper functions for running commands with clean output
# Reduces token usage in AI agent sessions by ~40%
# Based on: https://github.com/steveyegge/abacus/blob/main/hack/run_silent.sh

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if verbose mode is enabled
VERBOSE=${VERBOSE:-0}

# Run command silently, show output only on failure
run_silent() {
    local description="$1"
    local command="$2"

    if [ "$VERBOSE" = "1" ]; then
        echo "  → Running: $command"
        eval "$command"
        return $?
    fi

    local tmp_file=$(mktemp)
    if eval "$command" > "$tmp_file" 2>&1; then
        printf "  ${GREEN}✓${NC} %s\n" "$description"
        rm -f "$tmp_file"
        return 0
    else
        local exit_code=$?
        printf "  ${RED}✗${NC} %s\n" "$description"
        printf "${RED}Command failed: %s${NC}\n" "$command"
        cat "$tmp_file"
        rm -f "$tmp_file"
        return $exit_code
    fi
}

# Run command with native quiet flags (output shown on failure)
run_with_quiet() {
    local description="$1"
    local command="$2"

    if [ "$VERBOSE" = "1" ]; then
        echo "  → Running: $command"
        eval "$command"
        return $?
    fi

    local tmp_file=$(mktemp)
    if eval "$command" > "$tmp_file" 2>&1; then
        printf "  ${GREEN}✓${NC} %s\n" "$description"
        rm -f "$tmp_file"
        return 0
    else
        local exit_code=$?
        printf "  ${RED}✗${NC} %s\n" "$description"
        cat "$tmp_file"
        rm -f "$tmp_file"
        return $exit_code
    fi
}

# Run test command and extract test count
run_silent_with_test_count() {
    local description="$1"
    local command="$2"
    local test_type="${3:-maven}"  # Default to maven for this project

    if [ "$VERBOSE" = "1" ]; then
        echo "  → Running: $command"
        eval "$command"
        return $?
    fi

    local tmp_file=$(mktemp)
    local test_count=""
    local duration=""

    if eval "$command" > "$tmp_file" 2>&1; then
        # Extract test count based on test type
        case "$test_type" in
            maven)
                # Look for Maven Surefire/Failsafe summary
                # Format: "Tests run: 45, Failures: 0, Errors: 0, Skipped: 2"
                local summary=$(grep -E "Tests run: [0-9]+" "$tmp_file" | tail -1)
                if [ -n "$summary" ]; then
                    test_count=$(echo "$summary" | grep -oE "Tests run: [0-9]+" | grep -oE "[0-9]+")
                    local failures=$(echo "$summary" | grep -oE "Failures: [0-9]+" | grep -oE "[0-9]+")
                    local errors=$(echo "$summary" | grep -oE "Errors: [0-9]+" | grep -oE "[0-9]+")
                    local skipped=$(echo "$summary" | grep -oE "Skipped: [0-9]+" | grep -oE "[0-9]+")

                    # Extract time from "[INFO] Total time:" line
                    duration=$(grep -E "Total time:" "$tmp_file" | grep -oE "[0-9.]+ s" | tail -1)

                    if [ -n "$test_count" ]; then
                        local extra=""
                        [ "$skipped" != "0" ] && [ -n "$skipped" ] && extra=", $skipped skipped"
                        printf "  ${GREEN}✓${NC} %s (%s tests%s%s)\n" "$description" "$test_count" "$extra" "${duration:+, $duration}"
                    else
                        printf "  ${GREEN}✓${NC} %s\n" "$description"
                    fi
                else
                    # No test summary found (might be compile-only)
                    printf "  ${GREEN}✓${NC} %s\n" "$description"
                fi
                ;;
            pytest)
                # Look for pytest summary line like "45 passed in 2.3s"
                test_count=$(grep -E "[0-9]+ passed" "$tmp_file" | grep -oE "^[0-9]+ passed" | awk '{print $1}' | tail -1)
                if [ -n "$test_count" ]; then
                    duration=$(grep -E "[0-9]+ passed" "$tmp_file" | grep -oE "in [0-9.]+s" | tail -1)
                    printf "  ${GREEN}✓${NC} %s (%s tests%s)\n" "$description" "$test_count" "${duration:+, $duration}"
                else
                    printf "  ${GREEN}✓${NC} %s\n" "$description"
                fi
                ;;
            jest)
                # For jest with --json output
                test_count=$(jq -r '.numTotalTests // empty' "$tmp_file" 2>/dev/null)
                if [ -n "$test_count" ]; then
                    printf "  ${GREEN}✓${NC} %s (%s tests)\n" "$description" "$test_count"
                else
                    printf "  ${GREEN}✓${NC} %s\n" "$description"
                fi
                ;;
            go)
                # For go test -json output
                test_count=$(grep -c '"Action":"pass"' "$tmp_file" 2>/dev/null || true)
                if [ "$test_count" -gt 0 ]; then
                    printf "  ${GREEN}✓${NC} %s (%s tests)\n" "$description" "$test_count"
                else
                    printf "  ${GREEN}✓${NC} %s\n" "$description"
                fi
                ;;
            vitest)
                # Look for vitest summary
                test_count=$(grep -E "Test Files.*passed" "$tmp_file" | grep -oE "[0-9]+ passed" | awk '{print $1}' | head -1)
                if [ -n "$test_count" ]; then
                    printf "  ${GREEN}✓${NC} %s (%s test files)\n" "$description" "$test_count"
                else
                    printf "  ${GREEN}✓${NC} %s\n" "$description"
                fi
                ;;
            *)
                printf "  ${GREEN}✓${NC} %s\n" "$description"
                ;;
        esac
        rm -f "$tmp_file"
        return 0
    else
        local exit_code=$?
        printf "  ${RED}✗${NC} %s\n" "$description"
        printf "${RED}Command failed: %s${NC}\n" "$command"

        # For Maven failures, show relevant error info
        if [ "$test_type" = "maven" ]; then
            printf "\n${YELLOW}=== Error Summary ===${NC}\n"

            # Show test summary if tests ran
            grep -E "Tests run:.*Failures: [1-9]|Tests run:.*Errors: [1-9]" "$tmp_file" 2>/dev/null | tail -3

            # Trust Maven's [ERROR] tagging - it knows what's important
            # Show all ERROR lines with 2 lines of context after
            grep -A 2 "^\[ERROR\]" "$tmp_file" 2>/dev/null | head -50

            # If no [ERROR] lines found, show last 20 lines as fallback
            if ! grep -q "^\[ERROR\]" "$tmp_file" 2>/dev/null; then
                printf "\n${YELLOW}(No [ERROR] lines found, showing tail)${NC}\n"
                tail -20 "$tmp_file"
            fi
        else
            cat "$tmp_file"
        fi
        rm -f "$tmp_file"
        return $exit_code
    fi
}

# Print section header
print_header() {
    local module="$1"
    local description="$2"
    printf "${BLUE}[%s]${NC} %s:\n" "$module" "$description"
}

# Print main section header
print_main_header() {
    local title="$1"
    printf "\n=== %s ===\n\n" "$title"
}

# Check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Maven-specific helpers

# Run Maven with quiet output, show full output on failure
mvn_silent() {
    local description="$1"
    local goals="$2"
    local extra_args="${3:-}"

    run_silent_with_test_count "$description" "mvn $goals $extra_args" "maven"
}

# Run Maven tests
# Uses full output to capture test count, but filters on failure
mvn_test() {
    local description="${1:-Unit tests}"
    local test_pattern="${2:-}"
    local quiet="${3:-false}"  # Set to "true" for minimal failure output

    local cmd="mvn test"
    [ -n "$test_pattern" ] && cmd="$cmd -Dtest=$test_pattern"
    [ "$quiet" = "true" ] && cmd="$cmd -q"

    if [ "$quiet" = "true" ]; then
        run_silent "$description" "$cmd"
    else
        run_silent_with_test_count "$description" "$cmd" "maven"
    fi
}

# Run Maven verify (includes integration tests)
mvn_verify() {
    local description="${1:-All tests}"
    local skip_its="${2:-false}"

    local cmd="mvn verify"
    [ "$skip_its" = "true" ] && cmd="$cmd -DskipITs"

    run_silent_with_test_count "$description" "$cmd" "maven"
}

# Run Maven compile
mvn_compile() {
    local description="${1:-Compile}"
    run_silent "$description" "mvn compile"
}

# Run spotless
mvn_spotless() {
    local description="${1:-Format code}"
    run_silent "$description" "mvn spotless:apply"
}

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
    local test_type="${3:-gradle}"

    if [ "$VERBOSE" = "1" ]; then
        echo "  → Running: $command"
        eval "$command"
        return $?
    fi

    local tmp_file=$(mktemp)
    local marker_file=$(mktemp)
    local start_time=$(timer_now)
    local test_count=""
    local duration=""

    if [ "$test_type" = "gradle" ]; then
        find . -path "*/build/test-results" -type d -prune -exec rm -rf {} + 2>/dev/null
    fi

    if eval "$command" > "$tmp_file" 2>&1; then
        duration=$(elapsed_since "$start_time")
        # Extract test count based on test type
        case "$test_type" in
            gradle)
                print_gradle_summary "$description" "$marker_file" "$duration"
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
        rm -f "$marker_file"
        return 0
    else
        local exit_code=$?
        duration=$(elapsed_since "$start_time")
        printf "  ${RED}✗${NC} %s\n" "$description"
        printf "${RED}Command failed: %s${NC}\n" "$command"

        if [ "$test_type" = "gradle" ]; then
            print_gradle_failure_summary "$marker_file" "$tmp_file" "$duration"
        else
            cat "$tmp_file"
        fi
        rm -f "$tmp_file"
        rm -f "$marker_file"
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

timer_now() {
    perl -MTime::HiRes=time -e 'printf "%.6f", time'
}

elapsed_since() {
    local start_time="$1"
    local end_time
    end_time=$(timer_now)
    perl -e 'printf "%.2f s", $ARGV[1] - $ARGV[0]' "$start_time" "$end_time"
}

gradle_test_reports() {
    local marker_file="$1"
    find . -path "*/build/test-results/*/TEST-*.xml" -newer "$marker_file" -print0 2>/dev/null
}

gradle_test_summary() {
    local marker_file="$1"
    gradle_test_reports "$marker_file" \
        | xargs -0 perl -ne '
            if (/<testsuite\b([^>]*)>/) {
                $seen = 1;
                $attrs = $1;
                $tests += attr($attrs, "tests");
                $failures += attr($attrs, "failures");
                $errors += attr($attrs, "errors");
                $skipped += attr($attrs, "skipped");
                $time += attr($attrs, "time");
            }
            END {
                printf "%d\t%d\t%d\t%d\t%.3f\n", $tests, $failures, $errors, $skipped, $time if $seen;
            }
            sub attr {
                my ($attrs, $name) = @_;
                return $1 if $attrs =~ /\b\Q$name\E="([^"]+)"/;
                return 0;
            }
        ' 2>/dev/null
}

print_gradle_summary() {
    local description="$1"
    local marker_file="$2"
    local duration_text="$3"
    local summary
    summary=$(gradle_test_summary "$marker_file")

    if [ -z "$summary" ]; then
        printf "  ${GREEN}✓${NC} %s\n" "$description"
        return
    fi

    local test_count failures errors skipped test_duration
    IFS=$'\t' read -r test_count failures errors skipped test_duration <<< "$summary"
    printf "  ${GREEN}✓${NC} %s (%s tests, %s failures, %s errors, %s skipped, %s)\n" \
        "$description" "$test_count" "$failures" "$errors" "$skipped" "$duration_text"
}

print_gradle_failure_summary() {
    local marker_file="$1"
    local tmp_file="$2"
    local duration_text="$3"
    local summary
    summary=$(gradle_test_summary "$marker_file")

    printf "\n${YELLOW}=== Error Summary ===${NC}\n"

    if [ -n "$summary" ]; then
        local test_count failures errors skipped test_duration test_duration_text
        IFS=$'\t' read -r test_count failures errors skipped test_duration <<< "$summary"
        test_duration_text=$(awk -v seconds="$test_duration" 'BEGIN { printf "%.2f s", seconds }')
        printf "Tests run: %s, Failures: %s, Errors: %s, Skipped: %s, Time: %s\n" \
            "$test_count" "$failures" "$errors" "$skipped" "$duration_text"
        printf "Summed test time: %s\n" "$test_duration_text"

        local failed_tests
        failed_tests=$(gradle_test_reports "$marker_file" \
            | xargs -0 perl -0777 -ne '
                s{<testcase\b[^>]*/>}{}sg;
                while (m{<testcase\b([^>]*)>(.*?)</testcase>}sg) {
                    my ($case_attrs, $body) = ($1, $2);
                    next unless $body =~ m{<(failure|error)\b([^>]*)>(.*?)</\1>}s
                        || $body =~ m{<(failure|error)\b([^>]*)/>}s;
                    my ($type, $failure_attrs, $body_text) = ($1, $2, $3 // "");
                    my $classname = attr($case_attrs, "classname");
                    my $name = attr($case_attrs, "name");
                    my $message = attr($failure_attrs, "message");
                    $message = $body_text if $message eq "";
                    $message =~ s/\R.*//s;
                    $message =~ s/^\s+|\s+$//g;
                    $message = "(no message)" if $message eq "";
                    print xml_unescape("$classname#$name: $type: $message\n");
                }
                sub attr {
                    my ($attrs, $name) = @_;
                    return $1 if $attrs =~ /\b\Q$name\E="([^"]*)"/;
                    return "";
                }
                sub xml_unescape {
                    my ($text) = @_;
                    $text =~ s/&quot;/"/g;
                    $text =~ s/&apos;/'\''/g;
                    $text =~ s/&lt;/</g;
                    $text =~ s/&gt;/>/g;
                    $text =~ s/&amp;/&/g;
                    return $text;
                }
            ' 2>/dev/null | head -20)
        if [ -n "$failed_tests" ]; then
            printf "\nFailed tests:\n%s\n" "$failed_tests"
        fi
    else
        printf "No Gradle test XML reports were generated. Showing command output instead.\n"
    fi

    printf "\n${YELLOW}=== Gradle Output ===${NC}\n"
    grep -E "FAILED|Execution failed|There were failing tests|See the report at|> Task .*FAILED|\\[ERROR\\]" "$tmp_file" 2>/dev/null | head -80 || true
    if ! grep -Eq "FAILED|Execution failed|There were failing tests|See the report at|> Task .*FAILED|\\[ERROR\\]" "$tmp_file" 2>/dev/null; then
        tail -80 "$tmp_file"
    fi
}

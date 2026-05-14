#!/usr/bin/env bash
set -euo pipefail

WORKSPACE="$(pwd)"
LOG_FILE="$(mktemp)"
START_MS="$(($(date +%s) * 1000))"
GATE_NAME="gradle-migration"

cleanup() {
    rm -f "$LOG_FILE"
}
trap cleanup EXIT

log_gate() {
    local status="$1"
    local assertion="$2"
    local now_ms
    now_ms="$(($(date +%s) * 1000))"
    local duration_ms=$((now_ms - START_MS))
    printf 'gate=%s envTarget=local requestId=not_applicable traceId=not_applicable toolName=%s authOutcome=not_applicable httpStatus=not_applicable mcpStatus=not_applicable durationMs=%s downstreamStatus=%s downstreamCategory=build retry=not_attempted refresh=not_applicable assertion="%s"\n' \
        "$GATE_NAME" "$GATE_NAME" "$duration_ms" "$status" "$assertion"
}

sanitize() {
    sed -E \
        -e "s#${WORKSPACE}#<workspace>#g" \
        -e 's#/Users/[^[:space:]]+#<path>#g' \
        -e 's#/home/runner/work/[^[:space:]]+#<path>#g' \
        -e 's/Bearer [A-Za-z0-9._-]+/Bearer <redacted>/g' \
        -e 's/CONTRAST_API_KEY=[^[:space:]]+/CONTRAST_API_KEY=<redacted>/g' \
        -e 's/CONTRAST_SERVICE_KEY=[^[:space:]]+/CONTRAST_SERVICE_KEY=<redacted>/g' \
        -e 's/CONTRAST_USERNAME=[^[:space:]]+/CONTRAST_USERNAME=<redacted>/g' \
        -e 's/CONTRAST_ORG_ID=[^[:space:]]+/CONTRAST_ORG_ID=<redacted>/g'
}

run_checked() {
    local description="$1"
    shift
    log_gate "running" "command=$description"
    if "$@" >>"$LOG_FILE" 2>&1; then
        log_gate "success" "command=$description"
        return 0
    fi

    log_gate "failure" "command=$description"
    sanitize < "$LOG_FILE" | tail -80
    return 1
}

assert_log_is_sanitized() {
    local sanitized_log
    sanitized_log="$(mktemp)"
    sanitize < "$LOG_FILE" > "$sanitized_log"
    local forbidden='Bearer |eyJ[A-Za-z0-9_-]*\.|CONTRAST_API_KEY=|CONTRAST_SERVICE_KEY=|CONTRAST_USERNAME=|CONTRAST_ORG_ID=|/Users/|/home/runner/work/|Exception|at com\.contrast'
    if grep -E "$forbidden" "$sanitized_log" >/dev/null; then
        log_gate "failure" "sanitized-log-check"
        tail -80 "$sanitized_log"
        rm -f "$sanitized_log"
        return 1
    fi
    rm -f "$sanitized_log"
    log_gate "success" "sanitized-log-check"
}

assert_no_preview_flags() {
    if grep -R --line-number -- '--enable-preview' build.gradle settings.gradle gradle.properties contrast-mcp-core contrast-mcp-stdio-app .github Dockerfile Makefile README.md CLAUDE.md INTEGRATION_TESTS.md WORKFLOW.md >/dev/null; then
        log_gate "failure" "no-enable-preview"
        return 1
    fi
    log_gate "success" "no-enable-preview"
}

run_checked "java-version" java -version
run_checked "gradle-version" ./gradlew --version --no-daemon
run_checked "gradle-projects" ./gradlew projects --no-daemon
run_checked "core-boundary-test" ./gradlew :contrast-mcp-core:test --tests com.contrast.labs.ai.mcp.contrast.CoreBoundaryTest --no-daemon
run_checked "stdio-boot-jar" ./gradlew :contrast-mcp-stdio-app:bootJar -x test --no-daemon
assert_no_preview_flags
assert_log_is_sanitized
log_gate "success" "summary=Gradle migration diagnostics passed"

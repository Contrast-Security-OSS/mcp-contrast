#!/usr/bin/env bash
# Temporary S4C slice gate — remove once first-tool local parity is covered by permanent CI gates.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_FILE="$(mktemp)"
START_MS="$(date +%s)000"

cleanup() {
  rm -f "${OUTPUT_FILE}"
}
trap cleanup EXIT

log() {
  printf '[s4c-local-parity] %s\n' "$*"
}

finish() {
  local status="$1"
  local assertion_summary="$2"
  local end_ms duration_ms
  end_ms="$(date +%s)000"
  duration_ms="$((end_ms - START_MS))"
  log "gate=s4c-local-parity toolName=list_vulnerability_types environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> authOutcome=local-noop mcpStatus=${status} durationMs=${duration_ms} downstreamStatus=mocked downstreamCategory=contrast-sdk retryRefresh=not-applicable assertionSummary=\"${assertion_summary}\""
}

log "command=\"./gradlew --no-daemon :contrast-mcp-core:test --tests '*BaseToolAuthenticationStrategyTest' --tests '*ListVulnerabilityTypesToolTest' :contrast-mcp-stdio-app:test --tests '*ListVulnerabilityTypesLocalParityTest' --tests '*GetSastResultsToolTest'\" environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> authOutcome=local-noop downstreamStatus=mocked"

if ! (
  cd "${ROOT_DIR}"
  ./gradlew --no-daemon \
    :contrast-mcp-core:test \
    --tests '*BaseToolAuthenticationStrategyTest' \
    --tests '*ListVulnerabilityTypesToolTest' \
    :contrast-mcp-stdio-app:test \
    --tests '*ListVulnerabilityTypesLocalParityTest' \
    --tests '*GetSastResultsToolTest'
) >"${OUTPUT_FILE}" 2>&1; then
  finish "failed" "targeted unit/local parity checks failed; sanitized Gradle tail follows"
  sed "s#${ROOT_DIR}#<repo-root>#g" "${OUTPUT_FILE}" | tail -80
  exit 1
fi

if grep -Eiq 'bearer[[:space:]]+[A-Za-z0-9._-]+|api[_-]?key[[:space:]]*[=:]|service[_-]?key[[:space:]]*[=:]|raw-token-value|local-org-id-should-not-serialize' "${OUTPUT_FILE}"; then
  finish "failed" "targeted checks passed but diagnostic output contained a forbidden secret marker"
  exit 1
fi

finish "passed" "byte-equivalent golden JSON, ContrastApiClient mock coverage, auth failure error response, and local-only get_scan_results regressions passed"

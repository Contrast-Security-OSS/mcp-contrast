#!/usr/bin/env bash
# Temporary S4B slice gate — remove once the first-tool move is covered by permanent CI gates.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S4B-FIRST-TOOL-BOUNDARY"
TOOL_NAME="list_vulnerability_types"
START_SECONDS="$(date +%s)"

log() {
  printf '[s4b-first-tool-boundary] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

assert_file_exists() {
  local assertion="$1"
  local file="$2"

  if [[ ! -f "${file}" ]]; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=${file#${ROOT_DIR}/} requestId=not-applicable traceId=not-applicable"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=${file#${ROOT_DIR}/} requestId=not-applicable traceId=not-applicable"
}

assert_file_absent() {
  local assertion="$1"
  local file="$2"

  if [[ -e "${file}" ]]; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=${file#${ROOT_DIR}/} requestId=not-applicable traceId=not-applicable"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=${file#${ROOT_DIR}/} requestId=not-applicable traceId=not-applicable"
}

assert_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if ! grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=${file#${ROOT_DIR}/} pattern=<redacted> requestId=not-applicable traceId=not-applicable"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=${file#${ROOT_DIR}/} requestId=not-applicable traceId=not-applicable"
}

assert_not_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=${file#${ROOT_DIR}/} pattern=<redacted> requestId=not-applicable traceId=not-applicable"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=${file#${ROOT_DIR}/} requestId=not-applicable traceId=not-applicable"
}

assert_equals() {
  local assertion="$1"
  local expected="$2"
  local actual="$3"

  if [[ "${expected}" != "${actual}" ]]; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed expected=${expected} actual=${actual} requestId=not-applicable traceId=not-applicable"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed expected=${expected} actual=${actual} requestId=not-applicable traceId=not-applicable"
}

cd "${ROOT_DIR}"

log "gate=${GATE_NAME} step=tests command='./gradlew --no-daemon :contrast-mcp-core:test :contrast-mcp-stdio-app:compileJava' envTarget=local requestId=not-applicable traceId=not-applicable toolName=${TOOL_NAME} authOutcome=local-noop-and-toolcontext-forwarding-tested httpStatus=not-applicable mcpStatus=not-applicable downstreamStatus=mocked downstreamCategory=teamserver-rules retryRefresh=not-applicable credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"
./gradlew --no-daemon :contrast-mcp-core:test :contrast-mcp-stdio-app:compileJava

CORE_TOOL="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/ListVulnerabilityTypesTool.java"
STDIO_TOOL="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/ListVulnerabilityTypesTool.java"
CORE_SCAN_RESULTS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.java"

assert_file_exists "moved_tool_lives_in_core" "${CORE_TOOL}"
assert_file_absent "moved_tool_removed_from_stdio_app" "${STDIO_TOOL}"
assert_file_absent "get_scan_results_remains_out_of_core" "${CORE_SCAN_RESULTS}"
assert_contains "moved_tool_uses_contrast_api_client" "${CORE_TOOL}" 'ContrastApiClient'
assert_contains "moved_tool_calls_rules_through_client" "${CORE_TOOL}" 'contrastApiClient\.getRules\(\)'
assert_contains "tool_method_declares_tool_context" "${CORE_TOOL}" 'listVulnerabilityTypes\(ToolContext toolContext\)'
assert_contains "tool_context_forwarded_to_pipeline" "${CORE_TOOL}" 'executePipeline\(ListVulnerabilityTypesParams::of, toolContext\)'
assert_not_contains "moved_tool_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_TOOL}" 'ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "moved_tool_has_no_hidden_auth_or_org_parameters" "${CORE_TOOL}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'

CORE_TOOL_COUNT="$(
  grep -R --include='*.java' -l 'org.springframework.ai.tool.annotation.Tool' \
    "${ROOT_DIR}/contrast-mcp-core/src/main/java" | wc -l | tr -d '[:space:]'
)"
assert_equals "core_contains_exactly_one_production_tool" "1" "${CORE_TOOL_COUNT}"

log "gate=${GATE_NAME} assertion_summary=passed command='./gradlew --no-daemon :contrast-mcp-core:test :contrast-mcp-stdio-app:compileJava' envTarget=local requestId=not-applicable traceId=not-applicable toolName=${TOOL_NAME} authOutcome=sanitized httpStatus=not-applicable mcpStatus=not-applicable durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-rules retryRefresh=not-applicable credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

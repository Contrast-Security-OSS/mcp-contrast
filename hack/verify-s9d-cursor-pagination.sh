#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S9D-CURSOR-PAGINATION"
TOOLS="list_issue_observations,list_incident_observations"
OUTPUT_FILE="$(mktemp)"
START_SECONDS="$(date +%s)"

cleanup() {
  rm -f "${OUTPUT_FILE}"
}
trap cleanup EXIT

log() {
  printf '[s9d-cursor-pagination] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

fail_with_tail() {
  local assertion_summary="$1"
  log "gate=${GATE_NAME} status=failed command='<redacted-gradle-command>' environmentTarget=local-core requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=failed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=cursor-client-gate retryRefresh=not-applicable cursor=present assertionSummary=\"${assertion_summary}\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> responseBody=<redacted>"
  sed "s#${ROOT_DIR}#<repo-root>#g" "${OUTPUT_FILE}" | tail -80
  exit 1
}

assert_file_exists() {
  local assertion="$1"
  local file="$2"

  if [[ ! -f "${file}" ]]; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted> cursor=absent"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted> cursor=absent"
}

assert_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if ! grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} pattern=<redacted> requestId=<redacted> traceId=<redacted> cursor=absent"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted> cursor=absent"
}

assert_not_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} pattern=<redacted> requestId=<redacted> traceId=<redacted> cursor=absent"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted> cursor=absent"
}

cd "${ROOT_DIR}"

COMMAND=(
  ./gradlew --no-daemon
  :contrast-mcp-core:test
  --tests '*Cursor*'
)

log "gate=${GATE_NAME} status=start command='./gradlew --no-daemon <focused cursor tests>' environmentTarget=local-core requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=not-started durationMs=0 downstreamStatus=mocked downstreamCategory=cursor-client-gate retryRefresh=not-applicable cursor=absent credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> responseBody=<redacted>"

if ! "${COMMAND[@]}" >"${OUTPUT_FILE}" 2>&1; then
  fail_with_tail "focused cursor tests failed"
fi

if grep -Eiq 'eyJ[A-Za-z0-9._-]*|bearer[[:space:]]+[A-Za-z0-9._-]+|api[_-]?key[[:space:]]*[=:]|service[_-]?key[[:space:]]*[=:]|raw-token-value|local-org-id-should-not-serialize|AuthenticatedMcpPrincipal|BearerTokenContext|McpTransportContext|responseBodySecret|opaque\.cursor/with\+symbols==' "${OUTPUT_FILE}"; then
  fail_with_tail "focused checks passed but Gradle output contained a forbidden secret, context, path, cursor, or response-body marker"
fi

BASE_DIR="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/base"
CURSOR_TOOL="${BASE_DIR}/CursorPaginatedTool.java"
CURSOR_PARAMS="${BASE_DIR}/CursorPaginationParams.java"
CURSOR_RESULT="${BASE_DIR}/CursorExecutionResult.java"
CURSOR_RESPONSE="${BASE_DIR}/CursorToolResponse.java"
LOGGING_KEYS="${BASE_DIR}/LoggingKeys.java"
STDIO_APP="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/McpContrastApplication.java"

assert_file_exists "cursor_paginated_tool_exists" "${CURSOR_TOOL}"
assert_file_exists "cursor_pagination_params_exists" "${CURSOR_PARAMS}"
assert_file_exists "cursor_execution_result_exists" "${CURSOR_RESULT}"
assert_file_exists "cursor_tool_response_exists" "${CURSOR_RESPONSE}"

assert_contains "cursor_tool_uses_final_pipeline" "${CURSOR_TOOL}" 'protected final CursorToolResponse<R> executePipeline'
assert_contains "cursor_tool_accepts_opaque_cursor" "${CURSOR_TOOL}" '@Nullable String cursor'
assert_contains "cursor_tool_returns_next_cursor" "${CURSOR_TOOL}" 'result\.nextCursor\(\)'
assert_contains "cursor_tool_logs_presence_marker" "${CURSOR_TOOL}" 'LoggingKeys\.CURSOR, pagination\.cursorPresence\(\)'
assert_contains "cursor_logging_key_registered" "${LOGGING_KEYS}" 'public static final String CURSOR = "cursor";'
assert_contains "cursor_params_defaults_to_shared_page_size" "${CURSOR_PARAMS}" 'DEFAULT_PAGE_SIZE'
assert_contains "cursor_params_caps_to_shared_max" "${CURSOR_PARAMS}" 'MAX_PAGE_SIZE'

assert_not_contains "cursor_tool_does_not_log_cursor_value" "${CURSOR_TOOL}" 'LoggingKeys\.CURSOR, (cursor([^A-Za-z0-9_]|$)|pagination\.cursor\(\)|result\.nextCursor\(\))'
assert_not_contains "cursor_main_surface_excludes_sort_after" "${BASE_DIR}/CursorPaginationParams.java" 'sortAfter|sort_after'
assert_not_contains "cursor_response_excludes_random_access_fields" "${CURSOR_RESPONSE}" 'totalPages|hasMorePages|totalItems|offset'
assert_not_contains "cursor_params_excludes_random_access_fields" "${CURSOR_PARAMS}" 'totalPages|hasMorePages|totalItems|offset'
assert_not_contains "stdio_app_does_not_register_observation_cursor_tools" "${STDIO_APP}" 'list_issue_observations|list_incident_observations|ListIssueObservations|ListIncidentObservations'

log "gate=${GATE_NAME} status=passed command='./gradlew --no-daemon <focused cursor tests>' environmentTarget=local-core requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=passed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=cursor-client-gate retryRefresh=not-applicable cursor=present assertionSummary=\"CursorPaginatedTool, CursorPaginationParams, CursorExecutionResult, and CursorToolResponse expose opaque cursor/pageSize semantics, omit random-access page fields, keep observation cursor tools unregistered in stdio, and avoid cursor values, sortAfter internals, credentials, principal context, stack traces, internal paths, and response-body secrets in verification artifacts\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> responseBody=<redacted>"

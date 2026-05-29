#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S8-APPLICATIONS"
TOOLS="search_applications,get_session_metadata"
OUTPUT_FILE="$(mktemp)"
START_SECONDS="$(date +%s)"

cleanup() {
  rm -f "${OUTPUT_FILE}"
}
trap cleanup EXIT

log() {
  printf '[s8-applications] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

fail_with_tail() {
  local assertion_summary="$1"
  log "gate=${GATE_NAME} status=failed command='<redacted-gradle-command>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=failed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-applications retryRefresh=not-applicable assertionSummary=\"${assertion_summary}\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"
  sed "s#${ROOT_DIR}#<repo-root>#g" "${OUTPUT_FILE}" | tail -80
  exit 1
}

assert_file_exists() {
  local assertion="$1"
  local file="$2"

  if [[ ! -f "${file}" ]]; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
}

assert_file_absent() {
  local assertion="$1"
  local file="$2"

  if [[ -e "${file}" ]]; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
}

assert_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if ! grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} pattern=<redacted> requestId=<redacted> traceId=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
}

assert_not_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} pattern=<redacted> requestId=<redacted> traceId=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
}

cd "${ROOT_DIR}"

COMMAND=(
  ./gradlew --no-daemon
  :contrast-mcp-core:test
  --tests '*CoreBoundaryTest'
  --tests '*SearchApplicationsToolTest'
  --tests '*GetSessionMetadataToolTest'
  --tests '*ApplicationFilterParamsTest'
  --tests '*GetSessionMetadataParamsTest'
  :contrast-mcp-stdio-app:test
  --tests '*SearchApplicationsToolTest'
  --tests '*GetSessionMetadataToolTest'
)

log "gate=${GATE_NAME} status=start command='./gradlew --no-daemon <focused application-family tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=not-started durationMs=0 downstreamStatus=mocked downstreamCategory=teamserver-applications retryRefresh=not-applicable credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

if ! "${COMMAND[@]}" >"${OUTPUT_FILE}" 2>&1; then
  fail_with_tail "focused application-family tests failed"
fi

if grep -Eiq 'eyJ[A-Za-z0-9._-]*|bearer[[:space:]]+[A-Za-z0-9._-]+|api[_-]?key[[:space:]]*[=:]|service[_-]?key[[:space:]]*[=:]|raw-token-value|local-org-id-should-not-serialize|AuthenticatedMcpPrincipal|BearerTokenContext|McpTransportContext|responseBodySecret' "${OUTPUT_FILE}"; then
  fail_with_tail "focused checks passed but Gradle output contained a forbidden secret, context, path, or response-body marker"
fi

CORE_SEARCH="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/application/SearchApplicationsTool.java"
CORE_SESSION="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/application/GetSessionMetadataTool.java"
CORE_SEARCH_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/application/params/ApplicationFilterParams.java"
CORE_SESSION_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/application/params/GetSessionMetadataParams.java"
STDIO_SEARCH="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/application/SearchApplicationsTool.java"
STDIO_SESSION="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/application/GetSessionMetadataTool.java"
CORE_SCAN_RESULTS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.java"

assert_file_exists "search_applications_moved_to_core" "${CORE_SEARCH}"
assert_file_exists "get_session_metadata_moved_to_core" "${CORE_SESSION}"
assert_file_exists "application_filter_params_moved_to_core" "${CORE_SEARCH_PARAMS}"
assert_file_exists "get_session_metadata_params_moved_to_core" "${CORE_SESSION_PARAMS}"
assert_file_absent "search_applications_removed_from_stdio_app" "${STDIO_SEARCH}"
assert_file_absent "get_session_metadata_removed_from_stdio_app" "${STDIO_SESSION}"
assert_file_absent "get_scan_results_remains_out_of_core" "${CORE_SCAN_RESULTS}"

assert_contains "search_applications_tool_name_preserved" "${CORE_SEARCH}" 'name = "search_applications"'
assert_contains "get_session_metadata_tool_name_preserved" "${CORE_SESSION}" 'name = "get_session_metadata"'
assert_contains "search_applications_uses_contrast_api_client" "${CORE_SEARCH}" 'ContrastApiClient'
assert_contains "get_session_metadata_uses_contrast_api_client" "${CORE_SESSION}" 'ContrastApiClient'
assert_contains "search_applications_calls_client_search" "${CORE_SEARCH}" 'contrastApiClient\.searchApplications'
assert_contains "search_applications_uses_metadata_field_lookup" "${CORE_SEARCH}" 'contrastApiClient\.getApplicationMetadataFields\(\)'
assert_contains "get_session_metadata_calls_client_method" "${CORE_SESSION}" 'contrastApiClient\.getSessionMetadata\(params\.appId\(\)\)'
assert_contains "search_applications_declares_tool_context" "${CORE_SEARCH}" 'ToolContext toolContext'
assert_contains "get_session_metadata_declares_tool_context" "${CORE_SESSION}" 'ToolContext toolContext'
assert_contains "search_applications_forwards_tool_context" "${CORE_SEARCH}" 'ApplicationFilterParams\.of\(name, tag, metadataFilters\), toolContext'
assert_contains "get_session_metadata_forwards_tool_context" "${CORE_SESSION}" 'GetSessionMetadataParams\.of\(appId\), toolContext'
assert_not_contains "application_tools_have_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_SEARCH}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "session_tool_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_SESSION}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "search_applications_has_no_hidden_auth_or_org_parameters" "${CORE_SEARCH}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'
assert_not_contains "get_session_metadata_has_no_hidden_auth_or_org_parameters" "${CORE_SESSION}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'

log "gate=${GATE_NAME} status=passed command='./gradlew --no-daemon <focused application-family tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=passed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-applications retryRefresh=not-applicable assertionSummary=\"application tools moved to core, local SdkApiClient parity, metadata filters, pagination, 403/429 mappings, schema-source guardrails, and raw SARIF exclusion passed\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S8-VULNERABILITIES"
TOOLS="search_vulnerabilities,search_app_vulnerabilities,get_vulnerability"
OUTPUT_FILE="$(mktemp)"
START_SECONDS="$(date +%s)"

cleanup() {
  rm -f "${OUTPUT_FILE}"
}
trap cleanup EXIT

log() {
  printf '[s8-vulnerabilities] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

fail_with_tail() {
  local assertion_summary="$1"
  log "gate=${GATE_NAME} status=failed command='<redacted-gradle-command>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=failed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-vulnerabilities retryRefresh=not-applicable assertionSummary=\"${assertion_summary}\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"
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
  --tests '*SearchVulnerabilitiesToolTest'
  --tests '*SearchAppVulnerabilitiesToolTest'
  --tests '*GetVulnerabilityToolTest'
  --tests '*VulnerabilityFilterParamsTest'
  --tests '*SearchAppVulnerabilitiesParamsTest'
  --tests '*GetVulnerabilityParamsTest'
  :contrast-mcp-stdio-app:test
  --tests '*SearchVulnerabilitiesToolTest'
  --tests '*SearchAppVulnerabilitiesToolTest'
  --tests '*GetVulnerabilityToolTest'
  --tests '*HttpRequestRedactorTest'
  --tests '*VulnerabilityMapperTest'
)

log "gate=${GATE_NAME} status=start command='./gradlew --no-daemon <focused vulnerability-family tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=not-started durationMs=0 downstreamStatus=mocked downstreamCategory=teamserver-vulnerabilities retryRefresh=not-applicable credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

if ! "${COMMAND[@]}" >"${OUTPUT_FILE}" 2>&1; then
  fail_with_tail "focused vulnerability-family tests failed"
fi

if grep -Eiq 'eyJ[A-Za-z0-9._-]*|bearer[[:space:]]+[A-Za-z0-9._-]+|api[_-]?key[[:space:]]*[=:]|service[_-]?key[[:space:]]*[=:]|raw-token-value|local-org-id-should-not-serialize|AuthenticatedMcpPrincipal|BearerTokenContext|McpTransportContext|responseBodySecret' "${OUTPUT_FILE}"; then
  fail_with_tail "focused checks passed but Gradle output contained a forbidden secret, context, path, or response-body marker"
fi

CORE_SEARCH="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/SearchVulnerabilitiesTool.java"
CORE_APP_SEARCH="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/SearchAppVulnerabilitiesTool.java"
CORE_DETAIL="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/GetVulnerabilityTool.java"
CORE_MAPPER="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/VulnerabilityMapper.java"
CORE_REDACTOR="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/HttpRequestRedactor.java"
CORE_FILTER_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/params/VulnerabilityFilterParams.java"
CORE_APP_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/params/SearchAppVulnerabilitiesParams.java"
CORE_DETAIL_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/params/GetVulnerabilityParams.java"
STDIO_SEARCH="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/SearchVulnerabilitiesTool.java"
STDIO_APP_SEARCH="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/SearchAppVulnerabilitiesTool.java"
STDIO_DETAIL="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/GetVulnerabilityTool.java"
STDIO_MAPPER="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/VulnerabilityMapper.java"
STDIO_REDACTOR="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/HttpRequestRedactor.java"
CORE_SCAN_RESULTS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.java"

assert_file_exists "search_vulnerabilities_moved_to_core" "${CORE_SEARCH}"
assert_file_exists "search_app_vulnerabilities_moved_to_core" "${CORE_APP_SEARCH}"
assert_file_exists "get_vulnerability_moved_to_core" "${CORE_DETAIL}"
assert_file_exists "vulnerability_mapper_moved_to_core" "${CORE_MAPPER}"
assert_file_exists "http_request_redactor_moved_to_core" "${CORE_REDACTOR}"
assert_file_exists "vulnerability_filter_params_moved_to_core" "${CORE_FILTER_PARAMS}"
assert_file_exists "search_app_vulnerabilities_params_moved_to_core" "${CORE_APP_PARAMS}"
assert_file_exists "get_vulnerability_params_moved_to_core" "${CORE_DETAIL_PARAMS}"
assert_file_absent "search_vulnerabilities_removed_from_stdio_app" "${STDIO_SEARCH}"
assert_file_absent "search_app_vulnerabilities_removed_from_stdio_app" "${STDIO_APP_SEARCH}"
assert_file_absent "get_vulnerability_removed_from_stdio_app" "${STDIO_DETAIL}"
assert_file_absent "vulnerability_mapper_removed_from_stdio_app" "${STDIO_MAPPER}"
assert_file_absent "http_request_redactor_removed_from_stdio_app" "${STDIO_REDACTOR}"
assert_file_absent "get_scan_results_remains_out_of_core" "${CORE_SCAN_RESULTS}"

assert_contains "search_vulnerabilities_tool_name_preserved" "${CORE_SEARCH}" 'name = "search_vulnerabilities"'
assert_contains "search_app_vulnerabilities_tool_name_preserved" "${CORE_APP_SEARCH}" 'name = "search_app_vulnerabilities"'
assert_contains "get_vulnerability_tool_name_preserved" "${CORE_DETAIL}" 'name = "get_vulnerability"'
assert_contains "search_vulnerabilities_uses_contrast_api_client" "${CORE_SEARCH}" 'ContrastApiClient'
assert_contains "search_app_vulnerabilities_uses_contrast_api_client" "${CORE_APP_SEARCH}" 'ContrastApiClient'
assert_contains "get_vulnerability_uses_contrast_api_client" "${CORE_DETAIL}" 'ContrastApiClient'
assert_contains "search_vulnerabilities_calls_client_search" "${CORE_SEARCH}" 'contrastApiClient\.searchVulnerabilities'
assert_contains "search_app_vulnerabilities_calls_client_search" "${CORE_APP_SEARCH}" 'contrastApiClient\.searchAppVulnerabilities'
assert_contains "search_app_vulnerabilities_uses_latest_session_client" "${CORE_APP_SEARCH}" 'contrastApiClient\.getLatestSessionMetadata'
assert_contains "search_app_vulnerabilities_uses_session_metadata_client" "${CORE_APP_SEARCH}" 'contrastApiClient\.getSessionMetadata'
assert_contains "get_vulnerability_calls_client_detail" "${CORE_DETAIL}" 'contrastApiClient\.getVulnerability'
assert_contains "get_vulnerability_calls_recommendation_client" "${CORE_DETAIL}" 'contrastApiClient\.getRecommendation'
assert_contains "get_vulnerability_calls_http_request_client" "${CORE_DETAIL}" 'contrastApiClient\.getHttpRequest'
assert_contains "get_vulnerability_calls_event_summary_client" "${CORE_DETAIL}" 'contrastApiClient\.getEventSummary'
assert_contains "get_vulnerability_calls_library_client" "${CORE_DETAIL}" 'contrastApiClient\.getAllLibraries'
assert_contains "get_vulnerability_calls_library_observation_client" "${CORE_DETAIL}" 'contrastApiClient\.getLibraryObservations'
assert_contains "search_vulnerabilities_declares_tool_context" "${CORE_SEARCH}" 'ToolContext toolContext'
assert_contains "search_app_vulnerabilities_declares_tool_context" "${CORE_APP_SEARCH}" 'ToolContext toolContext'
assert_contains "get_vulnerability_declares_tool_context" "${CORE_DETAIL}" 'ToolContext toolContext'
assert_contains "get_vulnerability_emits_enrichment_telemetry" "${CORE_DETAIL}" 'durationMs'
assert_not_contains "search_vulnerabilities_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_SEARCH}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "search_app_vulnerabilities_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_APP_SEARCH}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "get_vulnerability_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_DETAIL}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "search_vulnerabilities_has_no_hidden_auth_or_org_parameters" "${CORE_SEARCH}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'
assert_not_contains "search_app_vulnerabilities_has_no_hidden_auth_or_org_parameters" "${CORE_APP_SEARCH}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'
assert_not_contains "get_vulnerability_has_no_hidden_auth_or_org_parameters" "${CORE_DETAIL}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'

log "gate=${GATE_NAME} status=passed command='./gradlew --no-daemon <focused vulnerability-family tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=passed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-vulnerabilities retryRefresh=not-applicable assertionSummary=\"vulnerability tools moved to core, local SdkApiClient parity, pagination, filters, latest-session/session-metadata behavior, detail enrichment client routing, sanitized 403/429/partial-enrichment responses, telemetry, and raw SARIF exclusion passed\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

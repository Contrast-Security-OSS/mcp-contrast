#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S8-LIBRARIES"
TOOLS="list_application_libraries,list_applications_by_cve"
OUTPUT_FILE="$(mktemp)"
START_SECONDS="$(date +%s)"

cleanup() {
  rm -f "${OUTPUT_FILE}"
}
trap cleanup EXIT

log() {
  printf '[s8-libraries] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

fail_with_tail() {
  local assertion_summary="$1"
  log "gate=${GATE_NAME} status=failed command='<redacted-gradle-command>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=failed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-libraries retryRefresh=not-applicable assertionSummary=\"${assertion_summary}\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"
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
  --tests '*ListApplicationLibrariesToolTest'
  --tests '*ListApplicationsByCveToolTest'
  --tests '*ListApplicationLibrariesParamsTest'
  --tests '*ListApplicationsByCveParamsTest'
  :contrast-mcp-stdio-app:test
  --tests '*ListApplicationLibrariesToolTest'
  --tests '*ListApplicationsByCveToolTest'
)

log "gate=${GATE_NAME} status=start command='./gradlew --no-daemon <focused library-family tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=not-started durationMs=0 downstreamStatus=mocked downstreamCategory=teamserver-libraries retryRefresh=not-applicable credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

if ! "${COMMAND[@]}" >"${OUTPUT_FILE}" 2>&1; then
  fail_with_tail "focused library-family tests failed"
fi

if grep -Eiq 'eyJ[A-Za-z0-9._-]*|bearer[[:space:]]+[A-Za-z0-9._-]+|api[_-]?key[[:space:]]*[=:]|service[_-]?key[[:space:]]*[=:]|raw-token-value|local-org-id-should-not-serialize|AuthenticatedMcpPrincipal|BearerTokenContext|McpTransportContext|responseBodySecret' "${OUTPUT_FILE}"; then
  fail_with_tail "focused checks passed but Gradle output contained a forbidden secret, context, path, or response-body marker"
fi

CORE_LIST_LIBRARIES="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/library/ListApplicationLibrariesTool.java"
CORE_BY_CVE="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/library/ListApplicationsByCveTool.java"
CORE_LIST_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/library/params/ListApplicationLibrariesParams.java"
CORE_BY_CVE_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/library/params/ListApplicationsByCveParams.java"
STDIO_LIST_LIBRARIES="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/library/ListApplicationLibrariesTool.java"
STDIO_BY_CVE="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/library/ListApplicationsByCveTool.java"
CORE_SCAN_RESULTS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.java"

assert_file_exists "list_application_libraries_moved_to_core" "${CORE_LIST_LIBRARIES}"
assert_file_exists "list_applications_by_cve_moved_to_core" "${CORE_BY_CVE}"
assert_file_exists "list_application_libraries_params_moved_to_core" "${CORE_LIST_PARAMS}"
assert_file_exists "list_applications_by_cve_params_moved_to_core" "${CORE_BY_CVE_PARAMS}"
assert_file_absent "list_application_libraries_removed_from_stdio_app" "${STDIO_LIST_LIBRARIES}"
assert_file_absent "list_applications_by_cve_removed_from_stdio_app" "${STDIO_BY_CVE}"
assert_file_absent "get_scan_results_remains_out_of_core" "${CORE_SCAN_RESULTS}"

assert_contains "list_application_libraries_tool_name_preserved" "${CORE_LIST_LIBRARIES}" 'name = "list_application_libraries"'
assert_contains "list_applications_by_cve_tool_name_preserved" "${CORE_BY_CVE}" 'name = "list_applications_by_cve"'
assert_contains "list_application_libraries_uses_contrast_api_client" "${CORE_LIST_LIBRARIES}" 'ContrastApiClient'
assert_contains "list_applications_by_cve_uses_contrast_api_client" "${CORE_BY_CVE}" 'ContrastApiClient'
assert_contains "list_application_libraries_calls_client_page" "${CORE_LIST_LIBRARIES}" 'contrastApiClient\.getLibraryPage'
assert_contains "list_applications_by_cve_calls_client_cve" "${CORE_BY_CVE}" 'contrastApiClient\.getApplicationsByCve'
assert_contains "list_applications_by_cve_calls_library_client" "${CORE_BY_CVE}" 'contrastApiClient\.getAllLibraries'
assert_contains "list_application_libraries_declares_tool_context" "${CORE_LIST_LIBRARIES}" 'ToolContext toolContext'
assert_contains "list_applications_by_cve_declares_tool_context" "${CORE_BY_CVE}" 'ToolContext toolContext'
assert_contains "list_application_libraries_forwards_tool_context" "${CORE_LIST_LIBRARIES}" 'ListApplicationLibrariesParams\.of\(appId\), toolContext'
assert_contains "list_applications_by_cve_forwards_tool_context" "${CORE_BY_CVE}" 'ListApplicationsByCveParams\.of\(cveId\), toolContext'
assert_contains "list_application_libraries_caps_page_size_50" "${CORE_LIST_LIBRARIES}" 'ValidationConstants\.API_MAX_PAGE_SIZE'
assert_contains "list_applications_by_cve_emits_enrichment_telemetry" "${CORE_BY_CVE}" 'durationMs'
assert_not_contains "list_application_libraries_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_LIST_LIBRARIES}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "list_applications_by_cve_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_BY_CVE}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "list_application_libraries_has_no_hidden_auth_or_org_parameters" "${CORE_LIST_LIBRARIES}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'
assert_not_contains "list_applications_by_cve_has_no_hidden_auth_or_org_parameters" "${CORE_BY_CVE}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'

log "gate=${GATE_NAME} status=passed command='./gradlew --no-daemon <focused library-family tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=passed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-libraries retryRefresh=not-applicable assertionSummary=\"library tools moved to core, local SdkApiClient parity, pageSize max 50, CVE single-response behavior, class-usage enrichment client routing, sanitized 403/429/partial-enrichment responses, telemetry, and raw SARIF exclusion passed\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

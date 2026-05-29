#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S8-SAST-METADATA"
TOOLS="get_scan_project"
OUTPUT_FILE="$(mktemp)"
START_SECONDS="$(date +%s)"

cleanup() {
  rm -f "${OUTPUT_FILE}"
}
trap cleanup EXIT

log() {
  printf '[s8-sast] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

fail_with_tail() {
  local assertion_summary="$1"
  log "gate=${GATE_NAME} status=failed command='<redacted-gradle-command>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=failed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-sast-metadata retryRefresh=not-applicable assertionSummary=\"${assertion_summary}\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"
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
  --tests '*ContrastApiClientContractTest'
  --tests '*GetSastProjectToolTest'
  --tests '*GetSastProjectParamsTest'
  :contrast-mcp-stdio-app:test
  --tests '*GetSastProjectLocalParityTest'
  --tests '*GetSastResultsToolTest'
  --tests '*GetSastResultsParamsTest'
)

log "gate=${GATE_NAME} status=start command='./gradlew --no-daemon <focused sast tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=not-started durationMs=0 downstreamStatus=mocked downstreamCategory=teamserver-sast-metadata retryRefresh=not-applicable credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

if ! "${COMMAND[@]}" >"${OUTPUT_FILE}" 2>&1; then
  fail_with_tail "focused SAST metadata and raw SARIF exclusion tests failed"
fi

if grep -Eiq 'eyJ[A-Za-z0-9._-]*|bearer[[:space:]]+[A-Za-z0-9._-]+|api[_-]?key[[:space:]]*[=:]|service[_-]?key[[:space:]]*[=:]|raw-token-value|local-org-id-should-not-serialize|AuthenticatedMcpPrincipal|BearerTokenContext|McpTransportContext|responseBodySecret|/ng/org/scan' "${OUTPUT_FILE}"; then
  fail_with_tail "focused checks passed but Gradle output contained a forbidden secret, context, path, or response-body marker"
fi

CORE_TOOL="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastProjectTool.java"
CORE_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/params/GetSastProjectParams.java"
CORE_CLIENT="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/client/ContrastApiClient.java"
CORE_SCAN_RESULTS_TOOL="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.java"
CORE_SCAN_RESULTS_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/params/GetSastResultsParams.java"
STDIO_TOOL="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastProjectTool.java"
STDIO_PARAMS="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/params/GetSastProjectParams.java"
STDIO_SCAN_RESULTS_TOOL="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.java"
STDIO_SCAN_RESULTS_PARAMS="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/params/GetSastResultsParams.java"

assert_file_exists "get_scan_project_moved_to_core" "${CORE_TOOL}"
assert_file_exists "get_scan_project_params_moved_to_core" "${CORE_PARAMS}"
assert_file_absent "get_scan_project_removed_from_stdio_app" "${STDIO_TOOL}"
assert_file_absent "get_scan_project_params_removed_from_stdio_app" "${STDIO_PARAMS}"
assert_file_exists "local_raw_sarif_tool_remains_in_stdio_app" "${STDIO_SCAN_RESULTS_TOOL}"
assert_file_exists "local_raw_sarif_params_remain_in_stdio_app" "${STDIO_SCAN_RESULTS_PARAMS}"
assert_file_absent "raw_sarif_tool_absent_from_core" "${CORE_SCAN_RESULTS_TOOL}"
assert_file_absent "raw_sarif_params_absent_from_core" "${CORE_SCAN_RESULTS_PARAMS}"

assert_contains "get_scan_project_tool_name_preserved" "${CORE_TOOL}" 'name = "get_scan_project"'
assert_contains "get_scan_project_uses_single_tool" "${CORE_TOOL}" 'extends SingleTool<GetSastProjectParams, ScanProject>'
assert_contains "get_scan_project_uses_contrast_api_client" "${CORE_TOOL}" 'ContrastApiClient'
assert_contains "get_scan_project_calls_client_scan_project" "${CORE_TOOL}" 'contrastApiClient\.getScanProject'
assert_contains "get_scan_project_declares_tool_context" "${CORE_TOOL}" 'ToolContext toolContext'
assert_contains "contrast_api_client_exposes_scan_project_metadata" "${CORE_CLIENT}" 'getScanProject\(String projectName\)'
assert_not_contains "contrast_api_client_has_no_raw_sarif_path" "${CORE_CLIENT}" 'getScanResults|get_scan_results|SarifResult|sarif|SARIF|InputStream'
assert_not_contains "get_scan_project_description_has_no_raw_sarif_tools_list_leak" "${CORE_TOOL}" 'get_scan_results|GetSastResultsTool|SarifResult|sarif|SARIF|InputStream'
assert_not_contains "get_scan_project_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_TOOL}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult|InputStream'
assert_not_contains "get_scan_project_params_have_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_PARAMS}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult|InputStream'
assert_not_contains "get_scan_project_has_no_hidden_auth_or_org_parameters" "${CORE_TOOL}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'
assert_not_contains "get_scan_project_params_have_no_hidden_auth_or_org_parameters" "${CORE_PARAMS}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'

log "gate=${GATE_NAME} status=passed command='./gradlew --no-daemon <focused sast tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=passed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-sast-metadata retryRefresh=not-applicable assertionSummary=\"get_scan_project moved to core, local SdkApiClient parity preserved, ToolContext forwarding and sanitized 401/403/404/429/5xx/parsing errors covered, core/client/publication surfaces exclude raw SARIF, and OOM exclusion probe confirms no shared raw SARIF bearer-token path\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

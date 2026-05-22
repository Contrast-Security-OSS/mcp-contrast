#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S8-ROUTE-COVERAGE"
TOOLS="get_route_coverage"
OUTPUT_FILE="$(mktemp)"
START_SECONDS="$(date +%s)"

cleanup() {
  rm -f "${OUTPUT_FILE}"
}
trap cleanup EXIT

log() {
  printf '[s8-route-coverage] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

fail_with_tail() {
  local assertion_summary="$1"
  log "gate=${GATE_NAME} status=failed command='<redacted-gradle-command>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=failed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-route-coverage retryRefresh=not-applicable assertionSummary=\"${assertion_summary}\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"
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
  --tests '*GetRouteCoverageToolTest'
  --tests '*RouteMapperTest'
  --tests '*RouteCoverageParamsTest'
  :contrast-mcp-stdio-app:test
  --tests '*GetRouteCoverageLocalParityTest'
)

log "gate=${GATE_NAME} status=start command='./gradlew --no-daemon <focused route-coverage tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=not-started durationMs=0 downstreamStatus=mocked downstreamCategory=teamserver-route-coverage retryRefresh=not-applicable credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

if ! "${COMMAND[@]}" >"${OUTPUT_FILE}" 2>&1; then
  fail_with_tail "focused route-coverage tests failed"
fi

if grep -Eiq 'eyJ[A-Za-z0-9._-]*|bearer[[:space:]]+[A-Za-z0-9._-]+|api[_-]?key[[:space:]]*[=:]|service[_-]?key[[:space:]]*[=:]|raw-token-value|local-org-id-should-not-serialize|AuthenticatedMcpPrincipal|BearerTokenContext|McpTransportContext|responseBodySecret|/ng/org/route' "${OUTPUT_FILE}"; then
  fail_with_tail "focused checks passed but Gradle output contained a forbidden secret, context, path, or response-body marker"
fi

CORE_TOOL="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/coverage/GetRouteCoverageTool.java"
CORE_MAPPER="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/coverage/RouteMapper.java"
CORE_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/coverage/params/RouteCoverageParams.java"
STDIO_TOOL="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/coverage/GetRouteCoverageTool.java"
STDIO_MAPPER="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/coverage/RouteMapper.java"
STDIO_PARAMS="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/coverage/params/RouteCoverageParams.java"
CORE_SCAN_RESULTS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.java"

assert_file_exists "get_route_coverage_moved_to_core" "${CORE_TOOL}"
assert_file_exists "route_mapper_moved_to_core" "${CORE_MAPPER}"
assert_file_exists "route_coverage_params_moved_to_core" "${CORE_PARAMS}"
assert_file_absent "get_route_coverage_removed_from_stdio_app" "${STDIO_TOOL}"
assert_file_absent "route_mapper_removed_from_stdio_app" "${STDIO_MAPPER}"
assert_file_absent "route_coverage_params_removed_from_stdio_app" "${STDIO_PARAMS}"
assert_file_absent "get_scan_results_remains_out_of_core" "${CORE_SCAN_RESULTS}"

assert_contains "get_route_coverage_tool_name_preserved" "${CORE_TOOL}" 'name = "get_route_coverage"'
assert_contains "get_route_coverage_uses_single_tool" "${CORE_TOOL}" 'extends SingleTool<RouteCoverageParams, RouteCoverageResponseLight>'
assert_contains "get_route_coverage_uses_contrast_api_client" "${CORE_TOOL}" 'ContrastApiClient'
assert_contains "get_route_coverage_calls_client_route_coverage" "${CORE_TOOL}" 'contrastApiClient\.getRouteCoverage'
assert_contains "get_route_coverage_calls_client_latest_session" "${CORE_TOOL}" 'contrastApiClient\.getLatestSessionMetadata'
assert_contains "get_route_coverage_declares_tool_context" "${CORE_TOOL}" 'ToolContext toolContext'
assert_contains "get_route_coverage_forwards_tool_context" "${CORE_TOOL}" 'toolContext\);'
assert_contains "route_mapper_keeps_light_response" "${CORE_MAPPER}" 'RouteCoverageResponseLight'
assert_not_contains "get_route_coverage_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_TOOL}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "route_mapper_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_MAPPER}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "route_coverage_params_have_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_PARAMS}" 'LocalSdkPaginatedTool|LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "get_route_coverage_has_no_hidden_auth_or_org_parameters" "${CORE_TOOL}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'
assert_not_contains "route_coverage_params_have_no_hidden_auth_or_org_parameters" "${CORE_PARAMS}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'
assert_not_contains "get_route_coverage_does_not_log_raw_session_id" "${CORE_TOOL}" 'Using latest session ID'

log "gate=${GATE_NAME} status=passed command='./gradlew --no-daemon <focused route-coverage tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=passed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-route-coverage retryRefresh=not-applicable assertionSummary=\"get_route_coverage moved to core, remains single-response, local SdkApiClient parity, latest-session and metadata filters route through ContrastApiClient, ToolContext forwarding, sanitized 403/429/5xx responses, no hidden auth/org parameters, and raw SARIF exclusion passed\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-16

This release represents a major overhaul of the MCP server, consolidating 27 inconsistently-named tools into 13 well-designed tools with consistent naming, fixing critical bugs that prevented core functionality from working, and adding significant new capabilities.

### Breaking Changes

**Tool Consolidation**: All tools now use `appId` instead of `app_name`. Use `search_applications` first to find application IDs.

| Old Tool Name | New Tool Name |
|---------------|---------------|
| `list_all_applications`, `list_applications_with_name`, `get_applications_by_tag`, `get_applications_by_metadata`, `get_applications_by_metadata_name` | `search_applications` |
| `list_vulnerabilities`, `list_vulnerabilities_with_id`, `list_vulnerabilities_by_application_and_session_metadata`, `list_vulnerabilities_by_application_and_latest_session` | `search_app_vulnerabilities` |
| `list_all_vulnerabilities` | `search_vulnerabilities` |
| `get_vulnerability`, `get_vulnerability_by_id` | `get_vulnerability` |
| `get_application_route_coverage`, `get_application_route_coverage_by_app_id`, plus 4 session metadata variants | `get_route_coverage` |
| `get_ADR_Protect_Rules`, `get_ADR_Protect_Rules_by_app_id` | `get_protect_rules` |
| `list_application_libraries`, `list_application_libraries_by_app_id` | `list_application_libraries` |
| `list_applications_vulnerable_to_cve` | `list_applications_by_cve` |
| `list_session_metadata_for_application` | `get_session_metadata` |
| `list_Scan_Project` | `get_scan_project` |
| `list_Scan_Results` | `get_scan_results` |

**Session Metadata Filtering**: The `sessionMetadataName` and `sessionMetadataValue` parameters are replaced by `sessionMetadataFilters` JSON parameter supporting AND/OR logic:
```json
{"branch":"main","developer":["Ellen","Sam"]}
```

**Field Rename**: `classedUsed` corrected to `classesUsed` in library responses.

### Critical Bug Fixes

These bugs prevented core functionality from working correctly:

**Date filtering caused HTTP 400 errors**: Using `lastSeenAfter` or `lastSeenBefore` parameters caused API errors because dates were serialized as strings instead of epoch milliseconds. Date filtering now works correctly.

**Status filtering was ignored**: The `statuses` parameter was accepted but never sent to the API. Status filters like `"Reported,Confirmed"` now work correctly. Status values are also now case-insensitive.

**Multi-word keyword search returned 0 results**: `search_attacks` with `keyword="SQL Injection"` failed due to incorrect URL encoding. Fixed.

**Tool names exceeded 64-character limit**: Several tool names were too long for Claude API (e.g., `get_application_route_coverage_by_app_id_and_session_metadata` at 77 characters). All tools now comply with the 64-character limit.

**Invalid resource IDs showed misleading errors**: Invalid app IDs returned "Authentication failed" which was confusing. Now returns "Authentication failed or resource not found. Verify credentials and that the resource ID is correct."

### New Capabilities

**JSON-based session metadata filtering**: Filter vulnerabilities by multiple metadata fields with AND/OR logic:
- AND across fields: `{"branch":"main","developer":"Ellen"}`
- OR within a field: `{"developer":["Ellen","Sam"]}`
- Combined: `{"branch":"main","developer":["Ellen","Sam"]}`

**Vulnerability results include application context**: `search_vulnerabilities` now returns `appId` and `appName` with each vulnerability, eliminating the need for additional API calls to determine ownership.

**Rules parameter for attack search**: Filter attacks by exact rule ID:
- Single rule: `rules="sql-injection"`
- Multiple rules: `rules="sql-injection,xss-reflected"`

**Pagination added to consolidated tools**: The old tools returned all results in a single response with no pagination. The new consolidated tools all support pagination:
- `search_applications` - replaces 5 non-paginated tools
- `search_app_vulnerabilities` - replaces 4 non-paginated tools
- `list_application_libraries` - now paginated (max 50 per page)

**Critical vulnerabilities field**: Library responses now include `criticalVulnerabilities` count separate from `highVulnerabilities`, making it easier to prioritize remediation.

**Vulnerability type discovery**: New `list_vulnerability_types` tool returns all available vulnerability types (rule names) for use in search filters.

**Sort validation for attacks**: Invalid sort fields now return helpful error messages listing valid options: `sourceIP`, `status`, `startTime`, `endTime`, `type` (prefix with `-` for descending).

**Empty metadata filter validation**: Empty, null, or whitespace-only metadata filter values now return clear validation errors instead of silently returning 0 results.

**Unknown CVE handling**: `list_applications_by_cve` now returns `found: false` with a helpful message for unrecognized CVEs instead of an error, making it easier for AI agents to handle typos or very new CVEs.

**Attack keyword search documentation**: The `keyword` parameter now accurately documents all 11 searchable fields (source IP, server name, application name, rule name, attack UUID, etc.) with OR logic across fields.

### Performance Improvements

**Application search 31x faster**: `search_applications` now uses server-side filtering instead of fetching all applications and filtering in memory. Response time improved from 21 seconds (for large numbers of apps) to ~670ms.

**Route coverage N+1 elimination**: `get_route_coverage` now makes a single API call instead of 1 + N calls (one per route). Also returns aggregate statistics (`totalRoutes`, `exercisedCount`, `discoveredCount`, `coveragePercent`) and lightweight `RouteLight` responses with ~60% smaller payloads.

**SDK instance caching**: The Contrast SDK instance is now cached instead of being recreated for each tool call.

### Other Improvements

**Structured error/warning separation**: Tool responses now separate errors (blocking issues) from warnings (informational messages), making it easier for AI agents to handle responses appropriately.

**Execution timing**: All tool responses now include `durationMs` for performance visibility.

**Startup credential validation**: The server now validates Contrast credentials at startup and fails fast with clear error messages if credentials are missing or invalid.

**Deprecation warning for SAST results**: `get_scan_results` now warns that it returns raw SARIF JSON which may be very large and exceed AI context limits.

### Security

**Docker security patches**: Updated Alpine base image and added `apk upgrade` to address CVEs in libpng and busybox, resolving Wiz scan failures.

### Documentation

**Installation guides**: Added separate installation guides for Claude Desktop, VS Code, IntelliJ, Cline, and oterm.

**MCP tool naming standards**: Created MCP_STANDARDS.md documenting the `action_entity` naming convention and design patterns for consistent tool development.

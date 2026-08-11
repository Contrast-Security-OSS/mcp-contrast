# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Bug Fixes

**`get_vulnerability` now returns clean Markdown remediation content**: TeamServer Recommendation
markup in `howToFix` is now rendered as Markdown, including links, code, paragraphs, and emphasis,
with a scrubbed plain-text fallback. Unmapped or missing Vulnerability types now receive the full
general `remediationHint` guidance without the type being concatenated onto the final sentence.

## [2.4.0] - 2026-08-10

### Bug Fixes

**`list_applications_by_cve` no longer returns misleading error for unrecognized CVEs**: When
the upstream endpoint returned HTTP 500 for CVEs unknown to the SCA library data, the tool
returned a generic "Narrow filters or reduce page size" message that made no sense for a tool
with neither filters nor pagination. It now explains that the CVE may not be recognized by the
SCA library data and suggests verifying the CVE identifier.

### Improvements

**`list_applications_by_cve` and `search_applications` explain lastSeen-zero applications**:
When any application in the response has a `lastSeen` value of 0, the tool now emits a notice
explaining that the application has never been observed running, typically a static or SCA-only
upload. Previously agents misread the zero timestamp as January 1970 or missing data.

**`list_applications_by_cve` notes that server status can lag live state**: The tool description
now explains that `lastSeen` and server status reflect last-known agent reports and may not match
current state, directing agents to `search_servers` for fresher data.

**Date parameters now reject out-of-range values with actionable messages**: Tools that accept
date filters now reject epoch timestamps outside the supported range (1970-01-01 through
9999-12-31) and return validation errors that include the valid range bounds. Previously,
negative or far-future timestamps were silently accepted.

## [2.3.0] - 2026-08-07

### Improvements

**`CursorToolResponse` now carries optional `totalItems`**: Cursor-paginated tools can
report total result count on the first page, matching the capability offset-paginated tools
already have via `PaginatedToolResponse.totalItems`. The field is nullable and defaults to
null, so existing cursor tools are unaffected.

## [2.2.0] - 2026-08-04

### Bug Fixes

**`search_vulnerabilities` example used wrong vulnerability type name**: The tool
description's example filter for reflected XSS used the incorrect type `xss-reflected`
instead of `reflected-xss`, causing agents to construct queries that returned no results.
Fixed to use the correct type name.

**`get_vulnerability` no longer crashes on missing HTTP request data**: When the Contrast
SDK returned a null HTTP request response, the tool threw a NullPointerException instead
of returning the vulnerability with the HTTP request field omitted. Fixed.

### Improvements

**Response envelope field `warnings` renamed to `notices`**: The informational messages
attached to every tool response were renamed from `warnings` to `notices` because the old
name caused AI agents to treat normal teaching notes as problems. The `Vulnerability`
field `hint` was also renamed to `remediationHint` for clarity.

**Tool descriptions rewritten to reduce token usage by ~650 tokens per session**: Every
tool description was trimmed to a word-budget template, cutting tool description text by
55% (1,079 to 490 words). Some parameter semantics moved from tool bodies down to
individual parameter descriptions where agents need them at call time, bringing the net
reduction across all agent-visible text to ~30%.

**Response-shape caveats moved to conditional notices**: Several static description
paragraphs that warned about edge-case response shapes now appear as runtime notices only
when the response actually contains the edge case, instead of adding to the token cost of
every call.

**Server instructions deliver catalog-wide conventions**: The MCP server now emits an
`instructions` field during initialization, teaching agents conventions that apply
uniformly across all tools. These facts no longer repeat in individual tool descriptions,
further reducing per-session token usage.

### Documentation

**Vulnerability environment filter semantics clarified**: Tool descriptions now explain
that environment filters match any historical vulnerability instance, while the returned
`environments` field reflects only the latest instance and may omit the environment that
caused the match.

**README documents hosted server authorization scope**: The README now explains that the
hosted MCP server's OAuth scopes are identity-only by design, authorization is enforced
per-request by the platform using existing RBAC, and every tool call is audited.

## [2.1.0] - 2026-07-20

### Breaking Changes

**CVE score accessors now use `Double`**: The published `contrast-mcp-core` `Cve` model changed `getScore()` and `setScore()` from primitive `double` to nullable `Double` so unavailable scores can be represented accurately. Consumers compiled against the previous signature must recompile.

### Bug Fixes

**Session-filtered route coverage statistics were always zero**: Filtered route coverage now reports
the correct total, exercised, and discovered route counts and coverage percentage. Filtered routes
that were missing a status now get one derived from their `exercised` timestamp instead of reporting
`null`, and filtered-route `environments` is documented as empty because the source omits it.

**VS Code one-click install button was broken**: The "Install contrast-mcp" button in the README and
the VS Code guide referenced five input variables that were never defined, so it installed an
unauthenticated, non-working server configuration. It now defines those inputs and prompts for all
five Contrast credentials.

### Improvements

**New `search_servers` tool**: Search the EAC-visible server inventory by agent health,
environment, application, tag, and Protect coverage with pagination and sorting.

**Richer CVE scoring details**: `list_applications_by_cve` responses now include nested `cvssv2` and `cvssv3` metrics plus a preferred `severity`. CVSS v3 scores remain available through `score`; v2-only CVEs omit `score` instead of reporting `0.0`. A null `score` is also omitted from `get_protect_rules` CVE output.

### Security

**Release SBOMs added**: GitHub releases now include CycloneDX and SPDX JSON SBOMs for both the release JAR and Docker image. Both JAR SBOM formats are bound to the JAR with GitHub attestations.

**Spring Boot 4.1.0 and Spring AI 1.1.7 baseline**: Upgraded the build and stdio
runtime baseline from Spring Boot 3.5.7/Spring AI 1.1.4 to Spring Boot 4.1.0/Spring
AI 1.1.7. This also moves the managed Spring Framework line to 7.0.8.

### Documentation

**README leads with the Hosted MCP Server**: The README now presents the Contrast Hosted MCP
Server (remote, OAuth, run by Contrast) as the recommended path, with connect steps, a
supported-clients matrix, and a new per-client hosted install guide. Local stdio setup is
retained as the secondary path, with the detailed local reference moved to
`docs/local-mcp-server.md`.

## [2.0.1] - 2026-06-05

### Bug Fixes

**Duplicate servers in `list_applications_by_cve`**: The `servers` array could list
the same server more than once because of a TeamServer API bug (TS-42992). Servers are
now deduplicated by ID before the response is built.

**`search_attacks` sort syntax was inconsistent**: The sort parameter now uses the same
`property,DIRECTION` convention as every other tool, replacing the one-off `-property`
descending syntax. Invalid sort input returns a clear error listing the expected format.

### Improvements

**CVE ID input is normalized**: `list_applications_by_cve` now trims whitespace and
uppercases the CVE ID, so values such as `cve-2021-44228` are accepted.

**Timestamp parameter validation**: The core validation framework now validates ISO
timestamp and epoch-millisecond inputs and rejects reversed time ranges before they
reach downstream APIs.

## [2.0.0] - 2026-06-01

### Breaking Changes

**Java 21 required**: The minimum Java version is now 21, up from 17. If you run the JAR directly, upgrade your JDK before updating. Docker users are unaffected since the container bundles its own JRE.

**Build system changed to Gradle**: If you build from source, the build command is now `./gradlew :contrast-mcp-stdio-app:bootJar` instead of `mvn package`. Maven is no longer supported.

### Bug Fixes

**Library vulnerability counts were incorrect**: The `criticalVulnerabilities` and `highVulnerabilities` counts in `list_application_libraries` responses could report wrong numbers. Fixed.

**`get_vulnerability` crashed on missing stack trace data**: Vulnerabilities without event stack information caused an error instead of returning partial results. The tool now gracefully handles missing stack data.

**Duplicate warnings cluttered tool responses**: Some tools repeated the same warning multiple times or echoed errors as warnings. Cleaned up so each message appears once.

**Safe libraries triggered unnecessary processing**: Libraries with no known vulnerabilities were still run through vulnerability enrichment, adding latency. Skipped.

**Forbidden resource errors were vague**: Accessing an application or resource you lack permissions for now returns a clear "forbidden" message instead of a generic authentication error.

**401 errors gave ambiguous guidance**: Authentication failures now distinguish between invalid credentials, expired keys, and wrong organization IDs with specific remediation steps.

### Improvements

**Better error messages across all tools**: Error responses now provide more actionable guidance, helping AI agents recover from common mistakes like invalid IDs or expired credentials without manual intervention.

**Sensitive headers redacted more thoroughly**: HTTP request data included in vulnerability details now redacts a broader set of credential-bearing headers.

### Security

**Docker base images pinned by digest**: The Docker image now pins both the builder and runtime base images to specific SHA256 digests, preventing silent base image changes.

**Contrast SDK updated to 3.7.0**: Picks up upstream fixes and improvements from the Contrast Java SDK.

**Build provenance attestations**: Release JARs are now signed with GitHub build provenance attestations. Verify a download with `gh attestation verify mcp-contrast-X.X.X.jar --repo Contrast-Security-OSS/mcp-contrast`.

**Docker images signed with DCT**: Published Docker images are now signed with Docker Content Trust for tamper detection.

**Dependency soak window enforced**: All dependency upgrades now require a 7-day waiting period after publication before adoption, reducing supply chain risk.

## [1.0.0] - 2026-01-16

This release represents a major overhaul of the MCP server, consolidating 27 inconsistently-named tools into 13 well-designed tools with consistent naming, fixing critical bugs that prevented core functionality from working, and adding significant new capabilities.

### Major Changes

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
- Multiple rules: `rules="sql-injection,reflected-xss"`

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

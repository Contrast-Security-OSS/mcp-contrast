# Feature Proposal: `search_servers` MCP Tool

Status: v2 — post design review (Claude drafted, Codex critiqued, both converged; factual claims verified against TeamServer source)
Repo: mcp-contrast

## 1. Problem and motivation

A Customer Solutions Architect (Jason Easterday) relayed a request from a large customer ($800K ARR): they want to answer questions like "what are the online servers and do they have Protect enabled?", "where are agents deployed?", and "what's actually on?" through the MCP server instead of the Contrast UI. The MCP server today has zero server-domain tools — agents cannot see the server/agent-deployment dimension of Contrast at all.

Servers are the deployment/coverage backbone of Contrast: every agent instance registers a server (host + path + language), applications run on servers (many-to-many), Protect licenses are consumed per server, and agent health (online/offline, out-of-date) is a server property.

## 2. Customer questions this tool must answer (acceptance scenarios)

1. "Which servers are online, and do they have Protect enabled?" → `quickFilter=ONLINE`, read `protectEnabled` per item (nullable — see §4).
2. "Which production servers do NOT have Protect?" → `quickFilter=UNPROTECTED, environments=PRODUCTION`.
3. "Which servers have out-of-date agents?" → `quickFilter=OUT_OF_DATE`, items show `agentVersion` vs `latestAgentVersion`.
4. "What servers is app X running on?" → `applicationIds=<appId>` (also matches parent app IDs of merged apps; subject to EAC).
5. "Which servers went dark / are stale?" → `quickFilter=OFFLINE, sort=lastActivity,ASC`.
6. "What's deployed in QA?" → `environments=QA, includeApplications=true`.
7. "Do we have servers with no applications?" → `withoutApplications=true`.
8. "Which servers still log at DEBUG?" → `logLevels=DEBUG`.

All eight verified against actual TeamServer filter/DAO semantics (scenarios 6 and 7 required design corrections found in review — see §5.8/§5.9).

## 3. Data source: TeamServer POST filter endpoint via SDKExtension (not the stock SDK)

`POST /ng/{orgId}/servers/filter` (JSON body, paging/sort/expand as query params) through the existing `SDKExtension` pattern. Rationale (each individually sufficient):

- The GET endpoint the SDK uses is `@ContrastDeprecated` since TS 3.7.10; POST is the current API.
- SDK's `ServerFilterForm` cannot filter by environment or agent version — environment is table stakes ("Protect in production?").
- SDK's `Servers` wrapper drops the total `count`, degrading `totalItems`/`hasMorePages`.
- SDK `Server` model lacks fields the API returns (language, first_activity, locked flags) and declares fields the API no longer emits (last_trace_received, container) — silent nulls.
- Precedent: vulnerability and attack search already use POST filter bodies via `SDKExtension`.
- The public v4 API has no server endpoints; ng is the only server surface.

Wire spellings (`applicationsIds`, `q`, `defend`, `-property` sort, `"None"` sentinel) stay behind the `ContrastApiClient` seam; the MCP contract never exposes them.

## 4. Tool specification

### Name
`search_servers` — `search_*` verb per MCP_STANDARDS (multiple optional filters, paginated). `SearchServersTool extends PaginatedTool<ServerFilterParams, ServerSummary>` in new package `tool/server/`.

### Parameters (all optional, camelCase)

| Param | Type | Maps to | Validation / notes |
|---|---|---|---|
| `page` | Integer | offset | default 1 |
| `pageSize` | Integer | limit | default 50, max 100 |
| `keyword` | String | `q` | substring match over name, hostname, path, tag |
| `environments` | String, comma-sep | `serverEnvironments` | uppercase; DEVELOPMENT, QA, PRODUCTION |
| `quickFilter` | String | `quickFilter` | uppercase; ALL, ONLINE, OFFLINE, PROTECTED, UNPROTECTED, OUT_OF_DATE; silent default ALL |
| `logLevels` | String, comma-sep | `logLevels` | uppercase; ERROR, WARN, INFO, DEBUG, TRACE (exact TeamServer allowlist) |
| `tags` | String, comma-sep | `tags` | exact values |
| `applicationIds` | String, comma-sep | `applicationsIds` | app UUIDs; mutually exclusive with `withoutApplications` |
| `withoutApplications` | Boolean | `applicationsIds=["None"]` | servers hosting no applications; adapter translates the case-sensitive wire sentinel |
| `agentVersions` | String, comma-sep | `agentVersions` | exact match |
| `includeApplications` | Boolean | expand | default false; false → `expand=num_apps`; true → `expand=applications` ONLY (TeamServer's expand branches are if/else-if — requesting both returns only the count; `setApplications` populates both list and count) |
| `sort` | String | `sort` | `property,DIRECTION`; properties `name`→serverName, `environment`, `lastActivity`, `agentVersion`→version; ASC/DESC (DESC → `-` prefix); default `lastActivity,DESC`; errors list valid options |

Filter semantics documented in the description: values within one parameter are ORed; different parameters are ANDed; `quickFilter` is single-valued but combines with all field filters. To intersect two quick-filter dimensions (online AND protected), filter on one and read per-item fields for the other. `agentVersion` sort is backend string ordering, not semver.

Design choices with review dissent noted:
- `quickFilter` name kept for suite consistency with `search_attacks` (Codex preferred `condition` as clearer; agreed defensible either way — flagged for the human's final call).
- PARTIALLY_PROTECTED excluded from v1: it is a per-app instrumentation concept the summary cannot explain, TeamServer's own filter catalog hides it without Protect entitlement, and exposing it may bypass a product-visibility rule. Roadmap item pending Product approval.

### Response

Standard `PaginatedToolResponse<ServerSummary>` envelope. `totalItems` from TeamServer `count` with one correction: TeamServer sets `count=0` for any empty page (verified `ServerFilterService.java:132-134`), so an out-of-range page would misreport the total. When page>1 returns empty with count=0, the adapter issues one fallback request (same filters, `limit=1, offset=0`, no expand) to recover the true total; if that is also empty, 0 is genuine.

```java
public record ServerSummary(
    long serverId,
    String name,
    String hostname,
    String path,               // distinguishes multiple agents per host
    String serverType,         // container type, e.g. tomcat
    String environment,        // DEVELOPMENT | QA | PRODUCTION
    String status,             // ONLINE | OFFLINE — computed by TeamServer's activity threshold (typically ~50 min)
    String language,           // agent language
    String agentVersion,
    String latestAgentVersion, // newest agent TeamServer can serve for that language
    Boolean agentOutOfDate,    // null = latest version absent/"NA" (wire is primitive; mapper derives null)
    boolean assessEnabled,     // primitive on wire; may reflect first visible app's effective config (see below)
    boolean assessPending,     // pending = change takes effect on agent restart
    Boolean protectEnabled,    // null = unknown/unavailable — NEVER coerced to false (see semantics below)
    Boolean protectPending,
    String logLevel,
    List<String> tags,
    long applicationCount,     // EAC-visible, non-archived apps only
    List<ServerApplicationSummary> applications, // null = not requested; empty = requested, none visible
    String lastActivityAt) {   // ISO-8601 with offset and millis
  public record ServerApplicationSummary(String appId, String name, String language) {}
}
```

Contract decisions:
- **`defend` → `protect`** rename (customer-facing product term), preserving nullability. Semantics (verified in `ServerResourceFactory`): the entitlement-gated `setDefendField` populates `defend` from the server model only for Protect-entitled orgs, but a post-build step then selects the server's first visible application (sorted by app ID) and, when instrumentation state exists, overwrites `defend` from that app's effective configuration (`setProtectStatusFromInstrumentationState`, lines 248-253); `assess` is likewise overwritten when the config source is `EFFECTIVE_CONFIG` (line 270). Contract wording therefore: `protectEnabled=null` means unknown/unavailable; a non-null value is the effective state TeamServer returned, which may reflect the first visible application's effective configuration rather than a server-wide aggregate. Same caveat documented for `assessEnabled`. The mapper must never turn null into false — that would corrupt the central customer question. A wire fixture pins this post-build overwrite path.
- **Single ISO-8601 timestamp** (`lastActivityAt`), no raw-millis duplicates (test convenience must not shape the interface; ITs parse the ISO string). `firstActivity`/`lastStartup` deferred to a future `get_server`.
- Errors vs empty: `success=false`, null envelope, or malformed JSON → error response with request ref, never an empty success (an agent must not conclude "no servers" from a failure). Valid empty result → success + standard no-results warning. HTTP 400/401/403/404/429/5xx → sanitized messages, no TeamServer bodies echoed.

### @Tool description (compact — high leverage per prompt token)

States: EAC scoping (results limited to servers/apps visible to the credentials); AND/OR filter algebra; quickFilter single-valued constraint; null-Protect (unknown/unavailable, non-null may reflect first visible app's effective config) and null-agentOutOfDate semantics; ONLINE/OFFLINE = TeamServer activity threshold (typically ~50 min); OUT_OF_DATE = older than newest agent TeamServer can serve for that language; three examples (online+Protect inspection, production unprotected, QA with apps); related tools one line (search_applications for app IDs, search_attacks accepts server filters, get_route_coverage, get_session_metadata).

## 5. Key design decisions (converged)

1. SDKExtension over stock SDK (§3) — Codex: AGREE.
2. Single `quickFilter` enum, not decomposed booleans — fake combinability would force client-side post-filtering and corrupt totalItems/hasMorePages. Codex: AGREE (prefers name `condition`; user decides).
3. `protect*` naming with nullable semantics — Codex: AGREE.
4. Compact summary, ~20 fields, single timestamp — converged after Codex pushback (originally 24+ fields with duplicated timestamps).
5. `expand=num_apps` default, `applications` opt-in — Codex: AGREE; implementation corrected for TeamServer's if/else-if expand handling.
6. No UI links, no filter echo — Codex: AGREE.
7. Detail fields (syslog, sampling, config-source/locked, license) deferred to future `get_server` — Codex: AGREE. Assess fields stay as primitives, with the documented caveat that `assessEnabled` may reflect the first visible application's effective configuration (Codex withdrew omission as a blocker once documented).
8. Scenario 6 fix: `includeApplications=true` sends `expand=applications` only (both list and count populated).
9. Scenario 7 fix: `withoutApplications` boolean; adapter sends case-sensitive `"None"` sentinel.
10. Silent ALL default — no warning spam on unfiltered calls.

## 6. Entity relationships (agent chaining)

- servers ↔ applications: many-to-many; `applicationIds` filter here; `includeApplications` for the reverse view.
- servers ↔ attacks: `search_attacks` already accepts server filters — this tool supplies the IDs/names.
- servers ↔ vulnerabilities: per-server traces exist in TeamServer (`servertraces`) — future tool.
- servers ↔ licensing: Protect licenses consumed per server; PROTECTED/UNPROTECTED counts are license-usage questions.
- EAC caveat: `totalItems` and `applicationCount` are visibility-scoped, not org-global; servers-with-no-apps requires the `SERVERS_WITH_NO_APPS` org resource; a user with no app access gets an empty success indistinguishable from an empty org (documented limitation).

## 7. Out of scope (roadmap candidates, separate tickets)

- `get_server` (single-server detail: settings, properties incl. container/OS, license, syslog, locked flags, lifecycle timestamps)
- Per-server vulnerability/attack breakdowns
- Server settings writes (toggle Assess/Protect, log level, bulk ops) — policy decision
- Quick-filter counts endpoint as a cheap org-health summary
- PARTIALLY_PROTECTED condition (needs Product/Security approval + per-app protect state in the result shape)

## 8. Implementation plan

contrast-mcp-core:
- `tool/server/SearchServersTool.java` (@Service, PaginatedTool subclass, @Tool method + no-ToolContext overload)
- `tool/server/params/ServerFilterParams.java` (BaseToolParams, ToolValidationContext, mutual-exclusion validation, sort parse/translate, `toServerFilterBody()`)
- `result/ServerSummary.java`
- `sdkextension/data/server/{ServerFilterBody, ServersResponse, ServerDetail}.java` (Gson wire DTOs incl. `count`, nullable Booleans)
- `client/ContrastApiClient.java`: `searchServers(ServerFilterBody, int limit, int offset, String sort, boolean includeApplications)`

contrast-mcp-stdio-app:
- `SDKExtension.getServersFiltered(...)`: POST via `makeRequestWithBody`, expand selection logic, count-fallback request
- `SdkApiClient.searchServers(...)` impl + contract-test touchpoint
- Register in `McpContrastApplication.tools()` bean

Cross-repo delivery: adding the `ContrastApiClient` method requires the hosted MCP adapter (aiml-hosted-mcp-server) to implement it before upgrading its core dependency, plus its own registration. Explicit coordination step, not an afterthought.

Tests (converged plan):
- Unit: filter-body capture tests (incl. translated sort, expand selection, `"None"` translation, mutual exclusion), validation errors → `verifyNoInteractions`, pageSize cap, envelope-failure → error (not empty success), 400/401/403/404/429/5xx sanitized + no-secret-leak, null Protect / unknown-latest mapping (never coerced), count-fallback behavior, wire fixture tests with omitted nullable Protect fields AND with the post-build first-app overwrite shape (regression for instrumentation-state-derived assess/protect values)
- IT: `allSatisfy` per filter dimension, boolean-algebra (OR within, AND across), sort sweep ASC/DESC per property with non-vacuous data preconditions (`isSortedAccordingTo` over ≥2 distinct values), pagination disjointness by serverId + count consistency across pages, empty page>1 totalItems correctness, error paths asserting full message shape incl. valid options, EAC-scoped expectations, seeded-data preconditions failing loudly with `.as(...)`
- `SearchServersLocalParityTest`; tool registration/schema check (`tools/list`)

## 9. Review provenance

Codex (independent reviewer) verdict: AGREE-WITH-CHANGES, then CONVERGED after one exchange. All blocking findings adopted after source verification (expand if/else-if, `"None"` sentinel, nullable defend, count=0 on empty pages, log-level allowlist, PARTIALLY_PROTECTED entitlement risk, first-visible-app overwrite of assess/protect — Codex sustained this with line citations and Claude's pushback was withdrawn on verification). Codex accepted keeping `quickFilter` (suite consistency over `condition`). Assess stays primitive with a documented first-app caveat. No open disagreements.

Full critique: /tmp/orca/search-servers-critique/critique.md
Convergence exchange: /tmp/orca/search-servers-critique/claude-convergence.md

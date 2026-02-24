# Package Restructuring Plan: Naming Improvements

**Status**: Proposed
**Created**: January 2025
**Approach**: Rename for clarity, don't restructure what works

## Executive Summary

This plan addresses naming debt and organizational ambiguity in the mcp-contrast codebase while preserving the existing architecture that is fundamentally sound. The approach prioritizes **renaming over restructuring** to minimize risk and maintain domain cohesion.

**Total files affected**: ~90 (primarily renames + moves, no logic changes)

---

## Analysis: What's Actually Wrong

### The Architecture Is Sound

The existing two-layer data architecture is **correct**:

| Layer | Package | Purpose | Example |
|-------|---------|---------|---------|
| **API Response DTOs** | `sdkextension/data/` | Map Contrast API JSON → Java (Gson `@SerializedName`) | `Application.java`, `Attack.java` |
| **MCP Response DTOs** | `data/` | Returned by tools to AI (records, AI-friendly) | `Vulnerability`, `VulnLight`, `AttackSummary` |

This is a correct separation. **The problem is naming, not architecture.**

### Real Problems (Not Aesthetic Preferences)

| Problem | Impact | Priority |
|---------|--------|----------|
| **Duplicate class names** (`Server` x3, `Application` x3, `App` x2, `Metadata` x2) | IDE autocomplete confusion, import errors | HIGH |
| **Cryptic acronyms** (`adr`, `sca`, `sast`) | Requires domain knowledge to understand | MEDIUM |
| **Vague package names** (`sdkextension`, `base`, `data`) | Unclear purpose | MEDIUM |
| **Root-level orphans** (`FilterHelper`, `PaginationParams`, `PromptService`) | Inconsistent organization | LOW |
| **Params in subpackages** | Separates what changes together | LOW |

### Why Duplicate Classes Cannot Be Consolidated

#### Three `Server` Classes - All Different API Responses:

| Class | Context | Key Fields | Reason for Existence |
|-------|---------|------------|---------------------|
| `sdkextension/data/Server.java` | CVE response | `server_id, name, hostname, path, type, environment, status` | Server affected by a CVE |
| `sdkextension/data/routecoverage/Server.java` | Route coverage | `serverId, name, hostname, serverpath, environment, agentVersion` | Server in route observations |
| `sdkextension/data/adr/Server.java` | Attack response | 30+ fields (assess, defend, agent_version, config_source...) | Full server in attack context |

**These CANNOT be consolidated** - they're different API responses with different fields.

#### Three `Application` Classes - Different Shapes:

| Class | Context | Key Fields |
|-------|---------|------------|
| `sdkextension/data/application/Application.java` | Full app from `/applications` | 40+ fields, full metadata |
| `sdkextension/data/adr/Application.java` | Embedded in attack response | `appId, name, language` (lightweight) |
| `data/ApplicationData.java` | MCP tool response DTO | `name, status, appID, lastSeenAt, language, metadata, tags, technologies` |

#### Key Class Responsibilities (Actual)

| Current Name | What It Actually Does | Better Name |
|--------------|----------------------|-------------|
| `SDKExtension` | API client wrapping ContrastSDK, makes HTTP requests for endpoints not in base SDK | `ContrastApiClient` |
| `SDKHelper` | Caching service + SDK factory utilities (TWO responsibilities!) | Split: `CachedDataService` + keep factory in `ContrastSDKFactory` |
| `FilterHelper` | Parameter parsing: dates, comma-lists, timestamps | `ParameterParser` |
| `VulnerabilityMapper` | Transforms SDK `Trace` → MCP `Vulnerability`/`VulnLight` | ✅ Correct |

---

## Proposed Package Structure

```
com.contrast.labs.ai.mcp.contrast/
├── McpContrastApplication.java
│
├── config/                                    # ✅ Keep as-is
│   ├── ContrastProperties.java
│   └── ContrastSDKFactory.java
│
├── api/                                       # Was: sdkextension
│   ├── ContrastApiClient.java                 # Renamed from SDKExtension
│   ├── CachedDataService.java                 # Extracted from SDKHelper (caching only)
│   │
│   └── response/                              # Was: sdkextension/data
│       │                                      # These are API response DTOs from Contrast
│       ├── ApplicationResponse.java           # Was: application/Application
│       ├── ApplicationsResponse.java          # Was: application/ApplicationsResponse
│       ├── ApplicationMetadata.java           # Was: application/Metadata
│       ├── ApplicationField.java              # Was: application/Field
│       │
│       ├── AttackResponse.java                # Was: adr/Attack
│       ├── AttacksResponse.java               # Was: adr/AttacksResponse
│       ├── AttacksFilterBody.java             # Was: adr/AttacksFilterBody
│       ├── AttackEvent.java                   # Was: adr/AttackEvent
│       ├── AttackServer.java                  # Was: adr/Server (30+ fields)
│       ├── AttackApplication.java             # Was: adr/Application (lightweight)
│       ├── AttackStory.java                   # Was: adr/Story
│       ├── AttackChapter.java                 # Was: adr/Chapter
│       ├── AttackEventDetails.java            # Was: adr/EventDetails
│       ├── AttackEventSummary.java            # Was: adr/EventSummary
│       ├── AttackHttpRequest.java             # Was: adr/HttpRequest
│       ├── AttackRequest.java                 # Was: adr/Request
│       ├── AttackUserInput.java               # Was: adr/UserInput
│       ├── AttackStackFrame.java              # Was: adr/StackFrame
│       ├── Event.java                         # Was: adr/Event (generic)
│       │
│       ├── LibraryResponse.java               # Was: Library
│       ├── LibrariesResponse.java             # Was: LibrariesExtended
│       ├── LibraryExtended.java               # ✅ Keep
│       ├── LibraryVulnerability.java          # Was: LibraryVulnerabilityExtended
│       ├── LibraryObservation.java            # Was: sca/LibraryObservation
│       ├── LibraryObservationsResponse.java   # Was: sca/LibraryObservationsResponse
│       │
│       ├── CveResponse.java                   # Was: Cve
│       ├── CveDataResponse.java               # Was: CveData
│       ├── CvssV3.java                        # ✅ Keep
│       ├── CveServer.java                     # Was: data/Server (CVE context)
│       ├── CveApp.java                        # Was: data/App
│       │
│       ├── RouteCoverageResponse.java         # Was: routecoverage/RouteCoverageResponse
│       ├── RouteDetailsResponse.java          # Was: routecoverage/RouteDetailsResponse
│       ├── Route.java                         # Was: routecoverage/Route
│       ├── RouteObservation.java              # Was: routecoverage/Observation
│       ├── RouteServer.java                   # Was: routecoverage/Server
│       ├── RouteApp.java                      # Was: routecoverage/App
│       ├── RouteCoverageRequest.java          # Was: RouteCoverageBySessionIDAndMetadataRequestExtended
│       │
│       ├── SessionMetadataResponse.java       # Was: sessionmetadata/SessionMetadataResponse
│       ├── AgentSession.java                  # Was: sessionmetadata/AgentSession
│       ├── MetadataField.java                 # Was: sessionmetadata/MetadataField
│       ├── MetadataSession.java               # Was: sessionmetadata/MetadataSession
│       │
│       ├── ProtectRulesResponse.java          # Was: ProtectData
│       ├── ProtectRule.java                   # Was: Rule
│       └── ImpactStats.java                   # ✅ Keep
│
├── model/                                     # Was: data (MCP response DTOs)
│   │                                          # These are returned TO the AI from tools
│   ├── VulnerabilityDetail.java               # Was: Vulnerability (full detail view)
│   ├── VulnerabilitySummary.java              # Was: VulnLight (list view)
│   ├── ApplicationSummary.java                # Was: ApplicationData
│   ├── AttackSummary.java                     # ✅ Keep
│   ├── LibraryUsage.java                      # Was: LibraryLibraryObservation
│   ├── StackLibrary.java                      # Was: StackLib
│   ├── ScanProject.java                       # Was: sast/ScanProject
│   ├── ApplicationMetadataEntry.java          # Was: Metadata (in data/)
│   └── RunBook.java                           # Was: RunBookEnum
│
├── tool/                                      # MCP tools
│   ├── support/                               # Was: base (infrastructure)
│   │   ├── BaseContrastTool.java
│   │   ├── PaginatedTool.java                 # Was: BasePaginatedTool
│   │   ├── SingleItemTool.java                # Was: BaseSingleTool
│   │   ├── ToolParams.java
│   │   ├── BaseToolParams.java
│   │   ├── ExecutionResult.java
│   │   ├── PaginatedToolResponse.java
│   │   └── SingleToolResponse.java
│   │
│   ├── validation/                            # ✅ Keep as-is
│   │   ├── ToolValidationContext.java
│   │   ├── ValidationConstants.java
│   │   ├── DateSpec.java
│   │   ├── IntSpec.java
│   │   ├── StringSpec.java
│   │   ├── StringListSpec.java
│   │   └── EnumSetSpec.java
│   │
│   ├── vulnerability/                         # Was: assess
│   │   ├── GetVulnerabilityTool.java
│   │   ├── GetVulnerabilityParams.java        # Was: params/GetVulnerabilityParams
│   │   ├── SearchVulnerabilitiesTool.java
│   │   ├── SearchAppVulnerabilitiesTool.java
│   │   ├── SearchAppVulnerabilitiesParams.java
│   │   ├── VulnerabilityFilterParams.java     # Colocated (was in params/)
│   │   ├── ListVulnerabilityTypesTool.java
│   │   └── ListVulnerabilityTypesParams.java
│   │
│   ├── application/                           # Was: applications
│   │   ├── SearchApplicationsTool.java
│   │   ├── ApplicationFilterParams.java
│   │   ├── GetSessionMetadataTool.java
│   │   └── GetSessionMetadataParams.java
│   │
│   ├── attack/                                # Was: adr
│   │   ├── SearchAttacksTool.java
│   │   ├── AttackFilterParams.java
│   │   ├── GetProtectRulesTool.java
│   │   └── GetProtectRulesParams.java
│   │
│   ├── library/                               # Was: sca
│   │   ├── ListApplicationLibrariesTool.java
│   │   ├── ListApplicationLibrariesParams.java
│   │   ├── ListApplicationsByCveTool.java
│   │   └── ListApplicationsByCveParams.java
│   │
│   ├── coverage/                              # ✅ Keep
│   │   ├── GetRouteCoverageTool.java
│   │   └── RouteCoverageParams.java
│   │
│   └── scan/                                  # Was: sast
│       ├── GetScanProjectTool.java
│       ├── GetScanProjectParams.java
│       ├── GetScanResultsTool.java
│       └── GetScanResultsParams.java
│
├── hints/                                     # ✅ Keep as-is (cross-cutting)
│   ├── HintGenerator.java
│   ├── HintProvider.java
│   ├── HintUtils.java
│   └── RuleHints.java
│
├── mapper/                                    # ✅ Keep as-is
│   ├── VulnerabilityMapper.java
│   └── VulnerabilityContext.java
│
├── prompt/                                    # Was: root-level files
│   ├── PromptRegistration.java
│   └── PromptService.java
│
└── util/                                      # Was: utils + root-level orphans
    ├── ParameterParser.java                   # Was: FilterHelper (root)
    ├── PaginationHandler.java                 # Was: utils/PaginationHandler
    └── PaginationParams.java                  # Was: root-level orphan
```

---

## Explicit Rename Tables

### Package Renames (8 packages)

| Current | New | Rationale |
|---------|-----|-----------|
| `sdkextension/` | `api/` | Clearer - it's API integration |
| `sdkextension/data/` | `api/response/` | These are API response DTOs |
| `data/` | `model/` | MCP tool output models |
| `tool/base/` | `tool/support/` | Supporting infrastructure |
| `tool/assess/` | `tool/vulnerability/` | Domain term, not product name |
| `tool/adr/` | `tool/attack/` | Readable - spell out acronym |
| `tool/sca/` | `tool/library/` | Readable - spell out acronym |
| `tool/sast/` | `tool/scan/` | Readable - spell out acronym |

### Class Renames - Clarity Improvements (18 classes)

| Current Path | New Path | Reason |
|--------------|----------|--------|
| `sdkextension/SDKExtension.java` | `api/ContrastApiClient.java` | It's an API client, not an "extension" |
| `sdkextension/SDKHelper.java` | `api/CachedDataService.java` | Focus on caching responsibility |
| `FilterHelper.java` | `util/ParameterParser.java` | It parses parameters, not "filters" |
| `data/Vulnerability.java` | `model/VulnerabilityDetail.java` | Distinguishes from summary |
| `data/VulnLight.java` | `model/VulnerabilitySummary.java` | "Light" is vague |
| `data/ApplicationData.java` | `model/ApplicationSummary.java` | Consistent naming |
| `data/LibraryLibraryObservation.java` | `model/LibraryUsage.java` | Remove stuttering |
| `data/StackLib.java` | `model/StackLibrary.java` | Full word |
| `data/RunBookEnum.java` | `model/RunBook.java` | Drop "Enum" suffix |
| `sdkextension/data/ProtectData.java` | `api/response/ProtectRulesResponse.java` | Describes content |
| `sdkextension/data/Rule.java` | `api/response/ProtectRule.java` | Context-specific |
| `sdkextension/data/LibrariesExtended.java` | `api/response/LibrariesResponse.java` | "Extended" is meaningless |
| `sdkextension/data/LibraryVulnerabilityExtended.java` | `api/response/LibraryVulnerability.java` | Drop "Extended" |
| `tool/base/BasePaginatedTool.java` | `tool/support/PaginatedTool.java` | Drop redundant "Base" |
| `tool/base/BaseSingleTool.java` | `tool/support/SingleItemTool.java` | Clearer intent |
| `sdkextension/data/Cve.java` | `api/response/CveResponse.java` | Consistent naming |
| `sdkextension/data/CveData.java` | `api/response/CveDataResponse.java` | Consistent naming |
| `sdkextension/data/RouteCoverageBySessionIDAndMetadataRequestExtended.java` | `api/response/RouteCoverageRequest.java` | Way too long! |

### Class Renames - Disambiguating Duplicates (18 classes)

| Current | New | Why |
|---------|-----|-----|
| `sdkextension/data/Server.java` | `api/response/CveServer.java` | Server in CVE context |
| `sdkextension/data/adr/Server.java` | `api/response/AttackServer.java` | Server in attack context |
| `sdkextension/data/routecoverage/Server.java` | `api/response/RouteServer.java` | Server in route context |
| `sdkextension/data/App.java` | `api/response/CveApp.java` | App in CVE context |
| `sdkextension/data/routecoverage/App.java` | `api/response/RouteApp.java` | App in route context |
| `sdkextension/data/adr/Application.java` | `api/response/AttackApplication.java` | Lightweight app in attack |
| `sdkextension/data/application/Application.java` | `api/response/ApplicationResponse.java` | Full app from API |
| `sdkextension/data/application/Metadata.java` | `api/response/ApplicationMetadata.java` | App metadata |
| `data/Metadata.java` | `model/ApplicationMetadataEntry.java` | MCP response metadata |
| `sdkextension/data/routecoverage/Observation.java` | `api/response/RouteObservation.java` | Route observation |
| `sdkextension/data/adr/HttpRequest.java` | `api/response/AttackHttpRequest.java` | HTTP request in attack |
| `sdkextension/data/adr/Request.java` | `api/response/AttackRequest.java` | Request in attack |
| `sdkextension/data/adr/Story.java` | `api/response/AttackStory.java` | Story in attack |
| `sdkextension/data/adr/Chapter.java` | `api/response/AttackChapter.java` | Chapter in attack |
| `sdkextension/data/adr/EventDetails.java` | `api/response/AttackEventDetails.java` | Event details in attack |
| `sdkextension/data/adr/EventSummary.java` | `api/response/AttackEventSummary.java` | Event summary in attack |
| `sdkextension/data/adr/UserInput.java` | `api/response/AttackUserInput.java` | User input in attack |
| `sdkextension/data/adr/StackFrame.java` | `api/response/AttackStackFrame.java` | Stack frame in attack |

### Params Consolidation (6 subpackages → inline)

Flatten `params/` subpackages into parent tool packages:

| Current | New |
|---------|-----|
| `tool/assess/params/GetVulnerabilityParams.java` | `tool/vulnerability/GetVulnerabilityParams.java` |
| `tool/assess/params/VulnerabilityFilterParams.java` | `tool/vulnerability/VulnerabilityFilterParams.java` |
| `tool/adr/params/AttackFilterParams.java` | `tool/attack/AttackFilterParams.java` |
| `tool/applications/params/ApplicationFilterParams.java` | `tool/application/ApplicationFilterParams.java` |
| `tool/sca/params/*.java` | `tool/library/*.java` |
| `tool/sast/params/*.java` | `tool/scan/*.java` |

**Rationale**: Params change with their tools (Common Closure Principle), are used together (Common Reuse Principle), and should be together.

### File Relocations (5 files)

| Current Location | New Location | Rationale |
|------------------|--------------|-----------|
| `FilterHelper.java` (root) | `util/ParameterParser.java` | Belongs with utilities |
| `PaginationParams.java` (root) | `util/PaginationParams.java` | Belongs with utilities |
| `PromptRegistration.java` (root) | `prompt/PromptRegistration.java` | Group prompt-related |
| `PromptService.java` (root) | `prompt/PromptService.java` | Group prompt-related |
| `sdkextension/SessionMetadata.java` | `api/SessionMetadataParser.java` | Rename + relocate (it's a parser) |

---

## Summary of Changes

| Category | Count | Description |
|----------|-------|-------------|
| **Package renames** | 8 | `sdkextension` → `api`, `assess` → `vulnerability`, etc. |
| **Class renames (clarity)** | 18 | `SDKExtension` → `ContrastApiClient`, `VulnLight` → `VulnerabilitySummary`, etc. |
| **Class renames (disambiguation)** | 18 | `Server` → `CveServer`/`AttackServer`/`RouteServer`, etc. |
| **Params flattening** | 6 | Remove `params/` subpackages |
| **Root orphans moved** | 5 | `FilterHelper`, `PaginationParams`, `Prompt*` |
| **No change** | ~50 | Most files just move with package rename |

**Total unique file operations**: ~90

---

## Why This Approach Is Better Than Full Restructuring

1. **Preserves domain cohesion** - `api/response/attack/` keeps attack-related DTOs together (CRP)
2. **Minimal structural change** - Same hierarchy, just better names
3. **Fixes real problems** - Duplicate names resolved, acronyms spelled out
4. **Lower risk** - Fewer changes = fewer bugs introduced
5. **Easier to review** - Package renames are mechanical, class renames are isolated

---

## Implementation Strategy

### Phase 1: Package Renames (Mechanical)
1. Rename `sdkextension/` → `api/`
2. Rename `sdkextension/data/` → `api/response/`
3. Rename `data/` → `model/`
4. Rename `tool/base/` → `tool/support/`
5. Rename tool domain packages (`assess` → `vulnerability`, etc.)

### Phase 2: Class Renames (Careful)
1. Disambiguate duplicate class names first (highest impact)
2. Rename clarity improvements
3. Update all import statements
4. Run full test suite after each batch

### Phase 3: File Relocations
1. Move root orphans to proper packages
2. Flatten params subpackages
3. Final verification

### Phase 4: Verification
1. Run `make check-test`
2. Run `make verify` (integration tests)
3. Verify IDE autocomplete works correctly
4. Update CLAUDE.md with new package names

---

## References

- Clean Code by Robert C. Martin (Uncle Bob)
- SOLID Principles: SRP, CCP (Common Closure Principle), CRP (Common Reuse Principle)
- Original analysis session: January 2025

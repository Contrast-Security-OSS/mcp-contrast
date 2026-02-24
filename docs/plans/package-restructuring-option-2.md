# Package Restructuring Plan: Clean Architecture with Domain Layer

**Status**: Superseded by Option 1
**Created**: January 2025
**Approach**: Full restructure with dedicated `domain/` layer

## Executive Summary

This was the **initial proposal** before being refined into Option 1. It applies Uncle Bob's Clean Architecture principles with a dedicated `domain/` layer for business models. While architecturally sound, it was deemed **over-engineered** for the actual problems in the codebase.

**Note**: This option was superseded by Option 1 (Naming Improvements) after deeper analysis showed the existing architecture was fundamentally correct.

---

## Analysis: Current Problems

### The Good
- **Tool-per-class pattern** follows SRP well - each tool does ONE thing
- **Base class hierarchy** (BaseContrastTool → BasePaginatedTool/BaseSingleTool) follows OCP
- **Validation is isolated** in its own package with composable specs (ISP)
- **Domain-organized tools** (assess, adr, sca, sast) have clear boundaries

### The Problems (Uncle Bob would be disappointed)

**1. Root-Level Orphans** - SRP Violation
```
FilterHelper.java    ← What package does this belong to?
PaginationParams.java ← Utility lost in the root
PromptRegistration.java
PromptService.java
```
Classes floating at root level have no clear home.

**2. Two `data` Packages** - Confusing Names
```
data/                      ← MCP response DTOs
sdkextension/data/         ← SDK response wrappers
```
A developer asks: "Where do I put a new data class?" The answer isn't obvious.

**3. Duplicate Class Names** - Naming Failure
- `Application.java` exists in 3 packages
- `Server.java` exists in 3 packages
- `Metadata.java` exists in 2 packages
- `App.java` exists in 2 packages

**4. `sdkextension/data/` is 4 Levels Deep** - Unnecessary Nesting
```
sdkextension/data/adr/Attack.java  ← 4 levels before the class
```

**5. Params Buried in Subpackages** - Artificial Separation
```
tool/assess/
├── GetVulnerabilityTool.java
└── params/
    └── GetVulnerabilityParams.java  ← Why separate?
```
These change together (CCP), are used together (CRP), and should be together.

---

## Proposed Structure

Applying Clean Architecture principles with domain-centric organization:

```
com.contrast.labs.ai.mcp.contrast/
├── McpContrastApplication.java
│
├── config/                              # Spring configuration
│   ├── ContrastProperties.java
│   └── ContrastSDKFactory.java
│
├── domain/                              # Core business models (framework-agnostic)
│   ├── vulnerability/
│   │   ├── Vulnerability.java           # Rich domain model
│   │   ├── VulnLight.java               # Lightweight projection
│   │   └── StackTrace.java
│   ├── application/
│   │   ├── ApplicationInfo.java         # Renamed from Application
│   │   ├── SessionMetadata.java
│   │   └── ServerInfo.java              # Renamed from Server
│   ├── attack/
│   │   ├── AttackEvent.java
│   │   ├── AttackSummary.java
│   │   └── ProtectRule.java
│   ├── library/
│   │   ├── LibraryInfo.java
│   │   ├── LibraryCve.java
│   │   └── LibraryObservation.java
│   ├── route/
│   │   ├── RouteInfo.java
│   │   └── RouteCoverage.java
│   └── scan/
│       └── ScanProject.java
│
├── sdk/                                 # Contrast SDK integration
│   ├── ContrastApiClient.java           # Renamed from SDKExtension
│   ├── SdkResponseMapper.java           # Renamed from SDKHelper
│   └── response/                        # Raw API response wrappers
│       ├── ApplicationsApiResponse.java
│       ├── AttacksApiResponse.java
│       ├── LibrariesApiResponse.java
│       └── RoutesApiResponse.java
│
├── tool/                                # MCP tools layer
│   ├── support/                         # Tool infrastructure (was: base)
│   │   ├── BaseContrastTool.java
│   │   ├── PaginatedTool.java           # Renamed from BasePaginatedTool
│   │   ├── SingleItemTool.java          # Renamed from BaseSingleTool
│   │   ├── ToolResponse.java
│   │   └── ExecutionResult.java
│   ├── validation/
│   │   ├── ToolValidationContext.java
│   │   ├── ValidationConstants.java
│   │   └── spec/
│   │       ├── DateSpec.java
│   │       ├── IntSpec.java
│   │       └── StringSpec.java
│   ├── vulnerability/                   # Domain-aligned (not product-aligned)
│   │   ├── GetVulnerabilityTool.java
│   │   ├── GetVulnerabilityParams.java  # Colocated with tool
│   │   ├── SearchVulnerabilitiesTool.java
│   │   ├── VulnerabilitySearchParams.java
│   │   └── ListVulnerabilityTypesTool.java
│   ├── application/
│   │   ├── SearchApplicationsTool.java
│   │   ├── ApplicationSearchParams.java
│   │   ├── GetSessionMetadataTool.java
│   │   └── SessionMetadataParams.java
│   ├── attack/                          # Was: adr (cryptic acronym)
│   │   ├── SearchAttacksTool.java
│   │   ├── AttackSearchParams.java
│   │   ├── GetProtectRulesTool.java
│   │   └── ProtectRulesParams.java
│   ├── library/                         # Was: sca (cryptic acronym)
│   │   ├── ListApplicationLibrariesTool.java
│   │   ├── ListLibrariesParams.java
│   │   ├── ListApplicationsByCveTool.java
│   │   └── CveSearchParams.java
│   ├── coverage/
│   │   ├── GetRouteCoverageTool.java
│   │   └── RouteCoverageParams.java
│   └── scan/                            # Was: sast (cryptic acronym)
│       ├── GetScanProjectTool.java
│       ├── GetScanResultsTool.java
│       └── ScanParams.java
│
├── hints/                               # Remediation hints (cross-cutting)
│   ├── HintGenerator.java
│   ├── HintProvider.java
│   └── RuleHints.java
│
├── prompt/                              # MCP prompts
│   ├── PromptRegistration.java
│   └── PromptService.java
│
└── util/                                # Shared utilities
    ├── PaginationHandler.java
    ├── FilterHelper.java
    └── DateUtils.java
```

---

## Key Changes Explained

| Change | Rationale (SOLID Principle) |
|--------|----------------------------|
| `domain/` layer | **SRP** - Domain models have ONE reason to change: business rules |
| Flatten `params/` subpackages | **CCP** - Params change with their tools, keep together |
| Rename `sdkextension` → `sdk` | **Names reveal intent** - "extension" says nothing |
| Rename `assess` → `vulnerability` | **Names reveal intent** - domain noun, not product name |
| Rename `adr` → `attack` | **Names reveal intent** - acronyms require mental translation |
| Rename `sca` → `library` | **Names reveal intent** - say what it IS |
| Rename `base` → `support` | **Names reveal intent** - "base" is vague |
| Unique class names | **Avoid ambiguity** - `ApplicationInfo` vs `ApplicationApiResponse` |
| Group `sdk/response/` | **CRP** - SDK response classes used together |
| `tool/validation/spec/` | **ISP** - Small, focused validation specs |

---

## Why This Was Superseded

After deeper analysis, this proposal was found to be **over-engineered**:

1. **Flattening `sdkextension/data/` subpackages** - Would create 35+ files in one directory, losing domain cohesion
2. **Creating a `domain/` layer** - The existing two-layer architecture (API DTOs vs MCP DTOs) is actually correct
3. **Renaming entities like `Attack` to `AttackResponse`** - It's not a response, it's an entity within `AttacksResponse`

### The Architecture Is Already Sound

| Layer | Package | Purpose | Example |
|-------|---------|---------|---------|
| **API Response DTOs** | `sdkextension/data/` | Map Contrast API JSON → Java | `Application.java`, `Attack.java` |
| **MCP Response DTOs** | `data/` | Returned by tools to AI | `Vulnerability`, `VulnLight`, `AttackSummary` |

The problem was **naming**, not **architecture**.

---

## Comparison with Option 1 (Recommended)

| Aspect | Option 2 (This) | Option 1 (Recommended) |
|--------|-----------------|------------------------|
| **Approach** | Full restructure | Naming improvements only |
| **Risk** | Higher - many structural changes | Lower - mostly renames |
| **Files affected** | ~150+ | ~90 |
| **Architecture change** | Yes - adds `domain/` layer | No - preserves existing |
| **Domain cohesion** | Lost (flattened packages) | Preserved |
| **Review difficulty** | Harder | Easier (mechanical changes) |

---

## Summary

This proposal was a good first attempt but was **too aggressive**. The refined Option 1 achieves the same goals (clear naming, no duplicates, readable package names) with less risk and fewer changes.

**Recommendation**: Use Option 1 instead.

---

## References

- Clean Code by Robert C. Martin (Uncle Bob)
- Clean Architecture by Robert C. Martin
- SOLID Principles: SRP, OCP, LSP, ISP, DIP
- CCP (Common Closure Principle), CRP (Common Reuse Principle)

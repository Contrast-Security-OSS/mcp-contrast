# Package Restructuring Plan: Feature-Sliced with Shared Core

**Status**: Proposed
**Created**: January 2025
**Approach**: Three-layer architecture with consolidated models and thin feature packages

## Executive Summary

This proposal reorganizes the codebase into three clear layers: **core** (infrastructure), **model** (all data), and **feature** (domain tools). It consolidates the dual data packages (`data/` and `sdkextension/data/`) into a single `model/` package while keeping thin, domain-focused tool packages.

**Key insight**: The current structure has sound domain separation in tools but confusing data organization. This approach fixes the data layer without over-engineering the tool layer.

**Total files affected**: ~115 (all files move, but structure is preserved within domains)

---

## First Principles Analysis

This proposal was developed by analyzing the codebase against five architectural principles:

| Principle | Definition | Application to This Codebase |
|-----------|------------|------------------------------|
| **Cohesion** | Group things that change together | Tools with params, models with mappers |
| **Coupling** | Dependencies flow one direction | feature/ → model/ → core/ (never reverse) |
| **Abstraction** | Consistent abstraction levels | Don't mix infrastructure with features |
| **Discoverability** | Self-explanatory package names | Developer finds changes quickly |
| **Testability** | Clear boundaries for mocking | Isolated layers, thin features |

---

## Analysis: Current Problems

### What Works Well

- **Tool-per-class pattern** - Each tool is a focused `@Service` with `@Tool` annotation
- **Base class hierarchy** - `BasePaginatedTool`/`BaseSingleTool` enforce consistent pipeline
- **Validation framework** - Composable specs in `ToolValidationContext`
- **Domain organization in tools** - `assess/`, `adr/`, `sca/` have clear boundaries
- **SDK extension pattern** - Extends base SDK with AI-friendly models

### What Doesn't Work

| Problem | Impact | Root Cause |
|---------|--------|------------|
| **Dual data packages** | Confusing: "Where do I put a new model?" | `data/` vs `sdkextension/data/` unclear |
| **55+ classes in sdkextension/data/** | Large surface area, hard to navigate | No consolidation strategy |
| **4 levels deep** | `sdkextension/data/adr/Attack.java` | Unnecessary nesting |
| **Root-level orphans** | `FilterHelper`, `PaginationParams` homeless | No `util/` package |
| **Duplicate class names** | `Server` x3, `Application` x3 | Domain context lost in flat packages |
| **Cryptic package names** | `sdkextension`, `adr`, `sca` | Acronyms require domain knowledge |

---

## Proposed Structure

```
com.contrast.labs.ai.mcp.contrast/
├── McpContrastApplication.java     # Entry point (stays at root)
├── PromptService.java              # Stays at root
├── PromptRegistration.java         # Stays at root
│
├── core/                           # ══════════════════════════════════════
│   │                               # SHARED INFRASTRUCTURE (stable, no business logic)
│   │
│   ├── config/
│   │   ├── ContrastProperties.java
│   │   └── ContrastSDKFactory.java
│   │
│   ├── sdk/                        # SDK integration (was: sdkextension/)
│   │   ├── SDKExtension.java       # API client wrapping ContrastSDK
│   │   ├── SDKHelper.java          # Caching service + utilities
│   │   ├── SessionMetadata.java    # Session tracking utilities
│   │   └── ExtendedTraceFilterBody.java
│   │
│   ├── tool/                       # Tool infrastructure (was: tool/base/)
│   │   ├── BasePaginatedTool.java
│   │   ├── BaseSingleTool.java
│   │   ├── BaseContrastTool.java
│   │   ├── BaseToolParams.java
│   │   ├── ToolParams.java
│   │   ├── ExecutionResult.java
│   │   ├── PaginatedToolResponse.java
│   │   └── SingleToolResponse.java
│   │
│   ├── validation/                 # Validation framework (was: tool/validation/)
│   │   ├── ToolValidationContext.java
│   │   ├── IntSpec.java
│   │   ├── StringSpec.java
│   │   ├── StringListSpec.java
│   │   ├── EnumSetSpec.java
│   │   ├── DateSpec.java
│   │   ├── MetadataJsonFilterSpec.java
│   │   ├── UnresolvedMetadataFilter.java
│   │   └── ValidationConstants.java
│   │
│   └── util/                       # Utilities (was: root orphans)
│       ├── PaginationParams.java
│       └── FilterHelper.java
│
├── model/                          # ══════════════════════════════════════
│   │                               # ALL DATA MODELS (consolidated)
│   │
│   ├── api/                        # MCP response records (AI-facing)
│   │   │                           # Was: data/
│   │   ├── Vulnerability.java
│   │   ├── VulnLight.java
│   │   ├── RouteLight.java
│   │   ├── RouteCoverageResponseLight.java
│   │   ├── ApplicationData.java
│   │   ├── AttackSummary.java
│   │   ├── Metadata.java
│   │   ├── StackLib.java
│   │   ├── LibraryLibraryObservation.java
│   │   ├── ScanProject.java
│   │   └── RunBookEnum.java
│   │
│   ├── sdk/                        # SDK response models (from Contrast API)
│   │   │                           # Was: sdkextension/data/
│   │   │
│   │   ├── common/                 # Shared across domains
│   │   │   ├── Library.java
│   │   │   ├── LibraryExtended.java
│   │   │   ├── LibraryVulnerabilityExtended.java
│   │   │   ├── LibrariesExtended.java
│   │   │   ├── CveData.java
│   │   │   ├── CvssV3.java
│   │   │   ├── ImpactStats.java
│   │   │   ├── ProtectData.java
│   │   │   └── Rule.java
│   │   │
│   │   ├── attack/                 # ADR responses (was: adr/)
│   │   │   ├── Attack.java
│   │   │   ├── AttackEvent.java
│   │   │   ├── AttacksResponse.java
│   │   │   ├── AttacksFilterBody.java
│   │   │   ├── EventSummary.java
│   │   │   ├── EventDetails.java
│   │   │   ├── Event.java
│   │   │   ├── Server.java         # Attack-context server
│   │   │   ├── Application.java    # Lightweight app in attack
│   │   │   ├── Story.java
│   │   │   ├── Chapter.java
│   │   │   ├── HttpRequest.java
│   │   │   ├── Request.java
│   │   │   ├── UserInput.java
│   │   │   └── StackFrame.java
│   │   │
│   │   ├── application/            # Application responses
│   │   │   ├── Application.java    # Full app from API
│   │   │   ├── ApplicationsResponse.java
│   │   │   ├── Metadata.java
│   │   │   └── Field.java
│   │   │
│   │   ├── route/                  # Route coverage (was: routecoverage/)
│   │   │   ├── Route.java
│   │   │   ├── RouteCoverageResponse.java
│   │   │   ├── RouteCoverageBySessionIDAndMetadataRequestExtended.java
│   │   │   ├── Observation.java
│   │   │   ├── Server.java         # Route-context server
│   │   │   └── App.java
│   │   │
│   │   ├── library/                # SCA responses (was: sca/)
│   │   │   ├── LibraryObservation.java
│   │   │   └── LibraryObservationsResponse.java
│   │   │
│   │   └── session/                # Session metadata (was: sessionmetadata/)
│   │       ├── SessionMetadataResponse.java
│   │       ├── AgentSession.java
│   │       ├── MetadataField.java
│   │       └── MetadataSession.java
│   │
│   └── mapper/                     # Data transformations
│       ├── VulnerabilityMapper.java
│       ├── VulnerabilityContext.java
│       └── RouteMapper.java
│
├── feature/                        # ══════════════════════════════════════
│   │                               # DOMAIN-SPECIFIC TOOLS (thin, focused)
│   │
│   ├── assess/                     # Vulnerability analysis
│   │   ├── SearchVulnerabilitiesTool.java
│   │   ├── SearchAppVulnerabilitiesTool.java
│   │   ├── GetVulnerabilityTool.java
│   │   ├── ListVulnerabilityTypesTool.java
│   │   └── params/
│   │       ├── VulnerabilityFilterParams.java
│   │       ├── SearchAppVulnerabilitiesParams.java
│   │       ├── GetVulnerabilityParams.java
│   │       └── ListVulnerabilityTypesParams.java
│   │
│   ├── protect/                    # ADR/runtime protection (was: adr/)
│   │   ├── SearchAttacksTool.java
│   │   ├── GetProtectRulesTool.java
│   │   └── params/
│   │       ├── AttackFilterParams.java
│   │       └── GetProtectRulesParams.java
│   │
│   ├── sca/                        # Library analysis
│   │   ├── ListApplicationLibrariesTool.java
│   │   ├── ListApplicationsByCveTool.java
│   │   └── params/
│   │       ├── ListApplicationLibrariesParams.java
│   │       └── ListApplicationsByCveParams.java
│   │
│   ├── sast/                       # Static analysis
│   │   ├── GetSastProjectTool.java
│   │   ├── GetSastResultsTool.java
│   │   └── params/
│   │       ├── GetSastProjectParams.java
│   │       └── GetSastResultsParams.java
│   │
│   ├── coverage/                   # Route coverage
│   │   ├── GetRouteCoverageTool.java
│   │   └── params/
│   │       └── RouteCoverageParams.java
│   │
│   └── applications/               # Application management
│       ├── SearchApplicationsTool.java
│       ├── GetSessionMetadataTool.java
│       └── params/
│           ├── ApplicationFilterParams.java
│           └── GetSessionMetadataParams.java
│
└── hint/                           # ══════════════════════════════════════
    │                               # HINT GENERATION (cross-cutting)
    ├── HintGenerator.java
    ├── HintProvider.java
    ├── RuleHints.java
    └── HintUtils.java
```

---

## Key Design Decisions

### 1. Three-Layer Architecture

```
┌──────────────────────────────────────┐
│              feature/                │  (depends on model, core)
│  assess/ protect/ sca/ sast/ etc.   │
└──────────────────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────┐
│              hint/                   │  (depends on model, core)
│     HintGenerator, HintProvider      │
└──────────────────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────┐
│              model/                  │  (depends on core/sdk)
│   api/ (records)  sdk/  mapper/     │
└──────────────────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────┐
│              core/                   │  (no internal dependencies)
│   config/  sdk/  tool/  validation/ │
└──────────────────────────────────────┘
```

**Dependency rule**: Upper layers depend on lower layers, never reverse.

### 2. Consolidated Model Package

The dual-data confusion is solved by consolidating under `model/`:

| Subpackage | Contains | Purpose |
|------------|----------|---------|
| `model/api/` | Java records | MCP tool responses (AI-facing) |
| `model/sdk/` | POJOs with `@SerializedName` | Contrast API JSON mapping |
| `model/mapper/` | Transformation logic | SDK models → API models |

**Clear answer**: "Where do I put a new data class?" → `model/api/` for tool responses, `model/sdk/{domain}/` for API responses.

### 3. Thin Feature Packages

Feature packages contain ONLY:
- Tool classes (`*Tool.java`)
- Params classes (`*Params.java`)

No models, no mappers, no infrastructure. This keeps features focused and easy to add.

### 4. Maximum Depth = 3

| Current (4 levels) | Proposed (3 levels) |
|--------------------|---------------------|
| `sdkextension/data/adr/Attack.java` | `model/sdk/attack/Attack.java` |
| `sdkextension/data/routecoverage/Server.java` | `model/sdk/route/Server.java` |

### 5. Domain Cohesion Preserved

Unlike Option 2 (which flattens everything), SDK models remain grouped by domain:
- `model/sdk/attack/` - All attack-related API responses together
- `model/sdk/application/` - All application-related responses together
- `model/sdk/route/` - All route coverage responses together

This preserves the context that disambiguates duplicate class names.

---

## Comparison with Other Options

| Aspect | Option 1 | Option 2 | Option 3 | **Option 4** |
|--------|----------|----------|----------|--------------|
| **Approach** | Rename only | Clean Arch layers | Full DDD bounded contexts | Feature-sliced with shared core |
| **Files affected** | ~90 | ~150+ | ~150+ | ~115 |
| **Risk level** | Low | Medium | Medium | Low-Medium |
| **Architecture change** | No | Yes (adds `domain/`) | Yes (bounded contexts) | Yes (consolidates models) |
| **Solves dual-data confusion** | Partially (renames) | Yes (but flattens) | Yes | **Yes (consolidates)** |
| **Preserves domain cohesion** | Yes | No (flattens) | Yes | **Yes** |
| **Max package depth** | 4 | 4 | 3 | **3** |
| **SDKExtension handling** | Renamed | Split into mappers | Split into clients | **Moved to core/sdk/** |

---

## Why This Approach

### vs Option 1 (Naming Improvements)

Option 1 renames packages but doesn't solve the dual-data confusion structurally. Developers still need to understand that `api/response/` is API DTOs while `model/` is MCP DTOs.

**Option 4 advantage**: Single `model/` package with clear subpackages (`api/` vs `sdk/`) makes the mental model obvious.

### vs Option 2 (Clean Architecture)

Option 2 flattens `sdkextension/data/` into a single `sdk/response/` package, losing domain cohesion (35+ files in one directory).

**Option 4 advantage**: Keeps domain subpackages (`model/sdk/attack/`, `model/sdk/route/`) for natural grouping.

### vs Option 3 (DDD Bounded Contexts)

Option 3 splits SDKExtension into per-domain clients and puts models inside domain packages. This creates duplication and makes cross-domain changes harder.

**Option 4 advantage**: Keeps shared infrastructure in `core/`, shared models in `model/`, with thin `feature/` packages that don't duplicate.

---

## Migration Path

### Phase 1: Create Core Package
1. Create `core/config/` - Move `ContrastProperties`, `ContrastSDKFactory`
2. Create `core/sdk/` - Move `SDKExtension`, `SDKHelper`, `SessionMetadata`, `ExtendedTraceFilterBody`
3. Create `core/tool/` - Move all base classes from `tool/base/`
4. Create `core/validation/` - Move all validation classes from `tool/validation/`
5. Create `core/util/` - Move `FilterHelper`, `PaginationParams`

### Phase 2: Create Model Package
1. Create `model/api/` - Move all classes from `data/`
2. Create `model/sdk/common/` - Move shared SDK classes
3. Create `model/sdk/attack/` - Move from `sdkextension/data/adr/`
4. Create `model/sdk/application/` - Move from `sdkextension/data/application/`
5. Create `model/sdk/route/` - Move from `sdkextension/data/routecoverage/`
6. Create `model/sdk/library/` - Move from `sdkextension/data/sca/`
7. Create `model/sdk/session/` - Move from `sdkextension/data/sessionmetadata/`
8. Create `model/mapper/` - Move `VulnerabilityMapper`, `RouteMapper`, `VulnerabilityContext`

### Phase 3: Create Feature Package
1. Create `feature/assess/` - Move from `tool/assess/`
2. Create `feature/protect/` - Move from `tool/adr/`
3. Create `feature/sca/` - Move from `tool/sca/`
4. Create `feature/sast/` - Move from `tool/sast/`
5. Create `feature/coverage/` - Move from `tool/coverage/`
6. Create `feature/applications/` - Move from `tool/applications/`

### Phase 4: Cleanup
1. Move `hints/` to `hint/` (singular)
2. Delete empty original packages
3. Update CLAUDE.md with new structure
4. Run full test suite

### Phase 5: Verification
1. Run `make check-test`
2. Run `make verify` (integration tests)
3. Verify IDE navigation works
4. Test component scanning finds all `@Service` classes

---

## Package Rename Summary

| Current | Proposed | Rationale |
|---------|----------|-----------|
| `sdkextension/` | `core/sdk/` | SDK integration is infrastructure |
| `sdkextension/data/` | `model/sdk/` | Consolidate all models |
| `sdkextension/data/adr/` | `model/sdk/attack/` | Clearer name, readable |
| `sdkextension/data/routecoverage/` | `model/sdk/route/` | Shorter |
| `sdkextension/data/sessionmetadata/` | `model/sdk/session/` | Shorter |
| `sdkextension/data/sca/` | `model/sdk/library/` | Domain term |
| `data/` | `model/api/` | Clear purpose: API responses |
| `tool/base/` | `core/tool/` | Infrastructure belongs in core |
| `tool/validation/` | `core/validation/` | Infrastructure belongs in core |
| `tool/assess/` | `feature/assess/` | Feature, not infrastructure |
| `tool/adr/` | `feature/protect/` | Product name alignment |
| `tool/sca/` | `feature/sca/` | Keep acronym (well-known in security) |
| `tool/sast/` | `feature/sast/` | Keep acronym (well-known in security) |
| `tool/coverage/` | `feature/coverage/` | Feature, not infrastructure |
| `tool/applications/` | `feature/applications/` | Feature, not infrastructure |
| `hints/` | `hint/` | Singular (consistency) |
| Root `FilterHelper` | `core/util/FilterHelper` | Proper home |
| Root `PaginationParams` | `core/util/PaginationParams` | Proper home |

---

## Benefits Summary

1. **Solves dual-data confusion** - Single `model/` package with clear `api/` vs `sdk/` distinction
2. **Preserves domain cohesion** - `model/sdk/attack/` keeps attack models together
3. **Reduces depth** - Max 3 levels vs current 4
4. **Clear dependency flow** - feature/ → model/ → core/
5. **Thin features** - Easy to add new domains
6. **Infrastructure isolated** - `core/` is stable, features change
7. **Discoverable** - Package names match developer mental model
8. **Testable** - Clear boundaries for mocking

---

## When to Choose This Option

Choose **Option 4 (Feature-Sliced)** when:
- You want to solve the dual-data confusion structurally
- You want a clear three-layer architecture
- You want to preserve domain cohesion in models
- You want thin, focused feature packages
- You don't need per-domain API clients (Option 3)

Choose **Option 1 (Naming)** when:
- You want minimal change with low risk
- The dual-data confusion isn't causing real problems
- Quick wins are more valuable than structural improvement

Choose **Option 3 (DDD)** when:
- You need strong domain isolation
- Team ownership is divided by domain
- You want to eventually extract domains to services

---

## References

- Feature-Sliced Design architecture pattern
- Clean Architecture by Robert C. Martin
- SOLID Principles: SRP, CCP (Common Closure Principle), CRP (Common Reuse Principle)
- Original analysis session: January 2025

# Package Restructuring Plan: Surgical Fixes (Clean Code Approach)

**Status**: Proposed
**Created**: January 2025
**Approach**: Preserve existing structure, fix DI violations, seal abstraction leaks, clarify naming

## Executive Summary

This proposal takes a conservative, surgical approach: **fix what's broken without wholesale restructuring**. The existing architecture has sound foundations - tool-per-class pattern, feature-based organization, mapper layer. The problems are execution details (DI violations, static methods, leaky abstractions) and naming clarity, not structure.

**Key insight**: "The code has good bones. Don't demolish the house to fix the plumbing."

**Total files affected**: ~25-30 (targeted fixes, not mass moves)
**Risk level**: Low

---

## First Principles Analysis

Before restructuring, verify the problems actually exist and warrant the proposed solutions:

| Principle | Current State | Verdict |
|-----------|--------------|---------|
| **Cohesion** | Tools with params ✓, mappers separate ✓ | GOOD - preserve |
| **Coupling** | SDKExtension not injected, leaky DTOs | NEEDS FIX |
| **Abstraction** | Two data packages serve different layers | GOOD INTENT - clarify naming |
| **Discoverability** | Cryptic names, orphaned files | NEEDS FIX |
| **Testability** | Static methods in SDKHelper block mocking | NEEDS FIX |

---

## What Works Well (Don't Change)

### 1. Tool-per-Class Pattern
Each tool is a focused `@Service` with `@Tool` annotation. This is exactly right.

### 2. Feature-Based Organization in `tool/`
```
tool/
├── assess/       # Vulnerability tools
├── adr/          # Attack/protect tools
├── sca/          # Library tools
├── coverage/     # Route coverage
└── applications/ # Application tools
```
This maps to Contrast product domains. Keep it.

### 3. Base Class Hierarchy
`BasePaginatedTool` and `BaseSingleTool` enforce consistent pipeline (validate → execute → respond). Don't change.

### 4. Validation Framework
`ToolValidationContext` with composable specs is sophisticated and reusable. Keep in `tool/validation/`.

### 5. Two-Layer Data Architecture
The intent is correct:
- `data/` = MCP response DTOs (what AI sees)
- `sdkextension/data/` = SDK response models (what API returns)

The problem is naming, not structure.

---

## What Needs Fixing

### Priority 1: High Impact, Low Risk

#### 1.1 Make SDKExtension a Spring Bean

**Current** (9 occurrences):
```java
var sdkExtension = new SDKExtension(getContrastSDK());
```

**Proposed**:
```java
@Service
public class SDKExtension {
    private final ContrastSDK contrastSDK;

    public SDKExtension(ContrastSDK contrastSDK) {
        this.contrastSDK = contrastSDK;
    }
}
```

Tools then inject it:
```java
@Service
@RequiredArgsConstructor
public class SearchVulnerabilitiesTool extends BasePaginatedTool<...> {
    private final SDKExtension sdkExtension;  // injected
}
```

**Impact**: Testability, singleton reuse, foundation for caching
**Files changed**: ~10 (SDKExtension + 9 tools)

#### 1.2 Delete Dead Code

Delete `PromptService.java` - empty class with no purpose.

**Files changed**: 1

#### 1.3 Move Root-Level Orphans

| File | From | To |
|------|------|-----|
| `FilterHelper.java` | root | `tool/validation/FilterParser.java` |
| `PaginationParams.java` | root | `tool/base/PaginationParams.java` |

**Files changed**: 2 moves + import updates

---

### Priority 2: Medium Impact, Medium Risk

#### 2.1 Fix SDKHelper Static Anti-Pattern

**Current**:
```java
@Component
public class SDKHelper {
    private static final Cache<...> libraryCache = ...;
    public static List<LibraryExtended> getLibsForID(...) { ... }
}
```

**Proposed** - Two options:

**Option A: Convert to instance methods** (preferred)
```java
@Service
public class ContrastApiCache {
    private final Cache<...> libraryCache;

    public List<LibraryExtended> getLibrariesForHash(...) { ... }
}
```

**Option B: Pure static utility** (simpler)
Remove `@Component`, keep as static utility class, but then need different cache strategy.

**Files changed**: 1 class + ~5 callers

#### 2.2 Seal Abstraction Leaks in DTOs

Create MCP-facing summary types to replace SDK types in record fields:

| Current | Create | Replace In |
|---------|--------|------------|
| `SessionMetadata` (SDK) | `SessionMetadataSummary` | `Vulnerability`, `VulnLight` |
| `LibraryExtended` (SDK) | `LibrarySummary` | `Vulnerability`, `LibraryLibraryObservation` |
| `Observation` (SDK) | `ObservationSummary` | `RouteLight` |

Example transformation:
```java
// Before: Leaks SDK type
public record Vulnerability(
    List<LibraryExtended> vulnerableLibraries,  // SDK type leaked
    ...
) {}

// After: Clean MCP type
public record Vulnerability(
    List<LibrarySummary> vulnerableLibraries,   // MCP-facing type
    ...
) {}

// New summary record
public record LibrarySummary(
    String hash,
    String filename,
    String version,
    String grade,
    int cveCount
) {
    public static LibrarySummary from(LibraryExtended lib) {
        return new LibrarySummary(
            lib.getHash(), lib.getFilename(), lib.getVersion(),
            lib.getGrade(), lib.getVulnerabilities().size()
        );
    }
}
```

**Files changed**: ~8 (3 new records, 5 existing DTOs updated)

---

### Priority 3: Naming Clarity (Optional)

#### 3.1 Rename Packages for Clarity

| Current | Proposed | Rationale |
|---------|----------|-----------|
| `data/` | `dto/` | Standard Java term |
| `sdkextension/` | `sdk/` | Shorter, clearer |
| `sdkextension/data/` | `sdk/model/` | Not "data", but "models" |

This is the smallest rename that clarifies intent.

#### 3.2 Rename "Light" Suffix

| Current | Proposed |
|---------|----------|
| `VulnLight` | `VulnerabilitySummary` |
| `RouteLight` | `RouteSummary` |
| `RouteCoverageResponseLight` | `RouteCoverageSummary` |

"Summary" is clearer than "Light" (light compared to what?).

---

## Proposed Structure (After Fixes)

```
com.contrast.labs.ai.mcp.contrast/
├── McpContrastApplication.java
├── PromptRegistration.java
│
├── config/                              # No change
│   ├── ContrastProperties.java
│   └── ContrastSDKFactory.java
│
├── dto/                                 # Renamed from data/
│   ├── VulnerabilitySummary.java        # Renamed from VulnLight
│   ├── VulnerabilityDetail.java         # Renamed from Vulnerability
│   ├── RouteSummary.java                # Renamed from RouteLight
│   ├── RouteCoverageSummary.java        # Renamed from RouteCoverageResponseLight
│   ├── AttackSummary.java
│   ├── ApplicationData.java
│   ├── Metadata.java
│   ├── StackLib.java
│   ├── LibraryLibraryObservation.java
│   ├── RunBookEnum.java
│   ├── SessionMetadataSummary.java      # NEW - replaces SDK SessionMetadata
│   ├── LibrarySummary.java              # NEW - replaces SDK LibraryExtended
│   ├── ObservationSummary.java          # NEW - replaces SDK Observation
│   └── sast/
│       └── ScanProject.java
│
├── sdk/                                 # Renamed from sdkextension/
│   ├── ContrastApiClient.java           # Renamed from SDKExtension, now @Service
│   ├── ContrastApiCache.java            # Extracted from SDKHelper
│   ├── SessionMetadata.java
│   ├── ExtendedTraceFilterBody.java
│   └── model/                           # Renamed from data/
│       ├── adr/                         # Attack models (no change internally)
│       ├── application/                 # Application models
│       ├── routecoverage/               # Route models
│       ├── sca/                         # Library models
│       ├── sessionmetadata/             # Session models
│       └── [common classes]             # Library.java, CveData.java, etc.
│
├── tool/                                # Minimal changes
│   ├── base/
│   │   ├── BaseContrastTool.java
│   │   ├── BasePaginatedTool.java
│   │   ├── BaseSingleTool.java
│   │   ├── BaseToolParams.java
│   │   ├── ToolParams.java
│   │   ├── ExecutionResult.java
│   │   ├── PaginatedToolResponse.java
│   │   ├── SingleToolResponse.java
│   │   └── PaginationParams.java        # Moved from root
│   │
│   ├── validation/
│   │   ├── ToolValidationContext.java
│   │   ├── FilterParser.java            # Renamed from FilterHelper
│   │   └── [other specs unchanged]
│   │
│   ├── assess/                          # No structural change
│   ├── adr/                             # No structural change
│   ├── sca/                             # No structural change
│   ├── sast/                            # No structural change
│   ├── coverage/                        # No structural change
│   └── applications/                    # No structural change
│
├── mapper/                              # No change
│   ├── VulnerabilityMapper.java
│   ├── VulnerabilityContext.java
│   └── RouteMapper.java
│
└── hints/                               # No change
    ├── HintGenerator.java
    ├── HintProvider.java
    ├── RuleHints.java
    └── HintUtils.java
```

---

## Comparison with Other Options

| Aspect | Option 1 | Option 4 | **Option 5** |
|--------|----------|----------|--------------|
| **Approach** | Rename only | Three-layer restructure | Surgical fixes |
| **Files affected** | ~90 | ~115 | **~25-30** |
| **Risk level** | Low | Medium | **Low** |
| **Architecture change** | No | Yes (core/model/feature) | **No** |
| **Solves DI violations** | No | No | **Yes** |
| **Fixes abstraction leaks** | No | No | **Yes** |
| **Preserves existing structure** | Yes | No (major restructure) | **Yes** |
| **Time to implement** | 1-2 days | 3-5 days | **1-2 days** |

---

## Why This Approach

### vs Option 1 (Naming Only)
Option 1 renames packages but doesn't fix the architectural issues (DI violations, static methods, leaky abstractions).

**Option 5 advantage**: Fixes root causes, not just symptoms.

### vs Option 4 (Three-Layer Restructure)
Option 4 moves 115 files into a new core/model/feature structure. This is a major change that could introduce bugs.

**Option 5 advantage**: Same benefits (clarity, testability) with far less disruption.

### The Uncle Bob Principle

> "Always leave the code cleaner than you found it."

You don't need to rewrite everything. Every time you touch a file:
- Fix one DI violation
- Seal one abstraction leak
- Clarify one name

**Incremental improvement compounds.**

---

## Migration Path

### Phase 1: Quick Wins (Day 1)
1. Delete `PromptService.java`
2. Move `FilterHelper.java` → `tool/validation/FilterParser.java`
3. Move `PaginationParams.java` → `tool/base/PaginationParams.java`
4. Run `make check-test`

### Phase 2: DI Fixes (Day 1-2)
1. Convert `SDKExtension` to `@Service`
2. Update all 9 tools to inject SDKExtension
3. Run `make check-test`

### Phase 3: SDKHelper Refactor (Day 2)
1. Extract caching to `ContrastApiCache` service
2. Convert static methods to instance methods
3. Inject into callers
4. Run `make verify` (integration tests)

### Phase 4: Seal Abstraction Leaks (Day 2-3)
1. Create `SessionMetadataSummary` record
2. Create `LibrarySummary` record
3. Create `ObservationSummary` record
4. Update DTOs to use new types
5. Update mappers to create summary types
6. Run `make verify`

### Phase 5: Rename for Clarity (Day 3, Optional)
1. Rename `data/` → `dto/`
2. Rename `sdkextension/` → `sdk/`
3. Rename `sdkextension/data/` → `sdk/model/`
4. Rename "Light" classes to "Summary"
5. Run `make verify`

---

## Benefits Summary

1. **Low risk** - Preserves working structure
2. **Fixes root causes** - DI violations, static anti-patterns, leaky abstractions
3. **Improves testability** - Can mock SDKExtension and ContrastApiCache
4. **Clarifies intent** - `dto/` vs `sdk/model/` is clear
5. **Incremental** - Can stop after any phase with improvement
6. **Fast** - 1-3 days vs 3-5 days for full restructure

---

## When to Choose This Option

Choose **Option 5 (Surgical Fixes)** when:
- The existing structure works and you don't want to disrupt it
- You want to fix testability issues (DI, static methods)
- You want to fix abstraction leaks without mass restructuring
- You want low-risk improvements that can be done incrementally
- Time is limited

Choose **Option 4 (Three-Layer)** when:
- You want a clean-slate three-layer architecture
- You have time for a larger refactoring effort
- The team agrees on the new structure

Choose **Option 1 (Naming)** when:
- You only want naming improvements with minimal change
- DI violations and abstraction leaks aren't causing problems

---

## Files Changed Summary

| Change | Files | Risk |
|--------|-------|------|
| Delete PromptService | 1 | None |
| Move root orphans | 2 | Low |
| SDKExtension → @Service | 10 | Low |
| SDKHelper refactor | 6 | Medium |
| New summary records | 3 | Low |
| Update DTOs for sealed abstractions | 5 | Medium |
| Package renames (optional) | ~40 | Low |

**Total without renames**: ~25 files
**Total with renames**: ~65 files

---

## References

- Clean Code by Robert C. Martin
- "Always leave the code cleaner than you found it"
- SOLID Principles: SRP, DIP (Dependency Inversion)
- Analysis session: January 2025 Clean Code Review

# Package Restructuring Options Overview

**Created**: January 2025
**Status**: Planning

This document summarizes four package restructuring approaches that were explored for the mcp-contrast codebase.

---

## Quick Comparison

| Aspect | Option 1: Naming | Option 2: Clean Arch | Option 3: DDD | Option 4: Feature-Sliced |
|--------|------------------|---------------------|---------------|--------------------------|
| **Approach** | Rename for clarity | Full restructure with `domain/` layer | Bounded contexts per domain | Three-layer with consolidated models |
| **Files affected** | ~90 | ~150+ | ~150+ | ~115 |
| **Risk level** | Low | Medium | Medium | Low-Medium |
| **Architecture change** | No | Yes | Yes | Yes (model consolidation) |
| **SDKExtension handling** | Renamed to `ContrastApiClient` | Split into response mappers | Split into per-domain clients | Moved to `core/sdk/` |
| **Solves dual-data confusion** | Partially | Yes (flattens) | Yes | **Yes (consolidates)** |
| **Preserves domain cohesion** | Yes | No | Yes | **Yes** |
| **Status** | Recommended | Superseded | Alternative | **Alternative** |

---

## Option 1: Naming Improvements (Recommended)

**File**: [package-restructuring-option-1.md](./package-restructuring-option-1.md)

**Philosophy**: The existing architecture is sound. The problem is naming, not structure.

**Key changes**:
- Package renames: `sdkextension` → `api`, `assess` → `vulnerability`, `adr` → `attack`, etc.
- Class renames: Disambiguate duplicates (`Server` → `CveServer`/`AttackServer`/`RouteServer`)
- Flatten `params/` subpackages into parent tool packages
- Move root orphans to proper packages

**Pros**:
- Lowest risk - mostly mechanical renames
- Preserves existing domain cohesion
- Easy to review
- Quick to implement

**Cons**:
- Doesn't address SDKExtension god class
- Doesn't improve domain isolation

---

## Option 2: Clean Architecture with Domain Layer

**File**: [package-restructuring-option-2.md](./package-restructuring-option-2.md)

**Philosophy**: Apply Uncle Bob's Clean Architecture with dedicated `domain/` layer.

**Key changes**:
- Add `domain/` layer for business models
- Rename `sdkextension` → `sdk`
- Flatten SDK response classes into single package
- Same tool package renames as Option 1

**Pros**:
- Clear separation of concerns
- Framework-agnostic domain layer

**Cons**:
- **Superseded** - found to be over-engineered
- Flattening `sdkextension/data/` loses domain cohesion
- More changes than necessary

**Status**: This was the initial proposal that was refined into Option 1 after deeper analysis.

---

## Option 3: Domain-Driven with Bounded Contexts

**File**: [package-restructuring-option-3.md](./package-restructuring-option-3.md)

**Philosophy**: Each domain is a self-contained bounded context with its own models, client, and tools.

**Key changes**:
- Create `core/` package for shared infrastructure
- Each domain (`assess/`, `protect/`, `sca/`, `coverage/`, `sast/`) is a bounded context
- Split SDKExtension into per-domain clients (`VulnerabilityClient`, `AttackClient`, etc.)
- All domain code lives together (models + client + tools)

**Pros**:
- Natural team ownership boundaries
- Eliminates SDKExtension god class
- Easy to find all code for a domain
- Prepares for potential microservice extraction

**Cons**:
- More code duplication (similar patterns repeated)
- Cross-domain changes require touching multiple packages
- Higher initial effort

**Status**: Valid alternative for teams needing strong domain isolation.

---

## Option 4: Feature-Sliced with Shared Core

**File**: [package-restructuring-option-4.md](./package-restructuring-option-4.md)

**Philosophy**: Three-layer architecture with consolidated models and thin feature packages.

**Key changes**:
- Create `core/` package for all infrastructure (config, sdk, tool bases, validation, util)
- Create `model/` package consolidating `data/` and `sdkextension/data/` with clear substructure
- Create `feature/` package for domain-specific tools (thin, focused)
- Rename domain packages: `adr` → `protect`, `routecoverage` → `route`, `sessionmetadata` → `session`
- Reduce max depth from 4 to 3 levels

**Pros**:
- Solves dual-data confusion structurally (single `model/` package)
- Preserves domain cohesion in SDK models (`model/sdk/attack/`, `model/sdk/route/`)
- Clear three-layer dependency flow (feature → model → core)
- Thin feature packages - easy to add new domains
- Reduces nesting depth

**Cons**:
- More structural change than Option 1
- Requires updating all imports
- `model/` package is large (mitigated by clear subpackages)

**Status**: Alternative approach that balances structural improvement with domain cohesion.

---

## Decision Matrix

| If you need... | Choose |
|----------------|--------|
| Quick wins with minimal risk | **Option 1** |
| Clear naming without architecture change | **Option 1** |
| Solve dual-data confusion structurally | **Option 4** |
| Three-layer architecture with preserved domain cohesion | **Option 4** |
| Domain isolation and team ownership | **Option 3** |
| To eliminate SDKExtension god class | **Option 3** |
| Traditional Clean Architecture layers | Option 2 (but consider Option 1 first) |

---

## Recommendation

**Start with Option 1** (Naming Improvements):
1. Low risk, high impact
2. Fixes the actual problems (naming debt, duplicates)
3. Quick to implement and review
4. Preserves what's working

**Consider Option 4** (Feature-Sliced) if:
- The dual-data confusion (`data/` vs `sdkextension/data/`) is causing real developer friction
- You want a cleaner three-layer architecture
- You want to reduce nesting depth while preserving domain cohesion

**Consider Option 3** (DDD) as a follow-up if:
- Team grows and needs domain ownership
- SDKExtension becomes a maintenance burden
- Domains need to evolve independently

---

## Common Elements Across All Options

All four options agree on:
- Moving root orphans (`FilterHelper`, `PaginationParams`, `PromptService`, etc.) to proper packages
- Renaming `adr/` to something more readable (`attack/` or `protect/`)
- Moving infrastructure classes out of `tool/` to a shared location

**Naming differences:**
| Package | Options 1-3 | Option 4 |
|---------|-------------|----------|
| `assess/` | `vulnerability/` | `assess/` (keep) |
| `adr/` | `attack/` | `protect/` |
| `sca/` | `library/` | `sca/` (keep - well-known acronym) |
| `sast/` | `scan/` | `sast/` (keep - well-known acronym) |
| `tool/base/` | `tool/support/` | `core/tool/` |

---

## Files

- [Option 1: Naming Improvements](./package-restructuring-option-1.md) **(Recommended)**
- [Option 2: Clean Architecture](./package-restructuring-option-2.md) *(Superseded)*
- [Option 3: Domain-Driven Design](./package-restructuring-option-3.md) *(Alternative)*
- [Option 4: Feature-Sliced with Shared Core](./package-restructuring-option-4.md) *(Alternative)*

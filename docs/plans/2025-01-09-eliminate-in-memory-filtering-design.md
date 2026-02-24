# Design: Eliminate In-Memory Vulnerability Filtering

**Bead:** mcp-z71e
**Date:** 2025-01-09

## Background

The in-memory filtering path in `SearchAppVulnerabilitiesTool` was added because the AI implementing it didn't realize the TeamServer API and SDK already supported session metadata filtering server-side.

**Investigation confirmed:**
- TeamServer API fully supports `agentSessionId` and `metadataFilters` parameters
- SDK's `TraceFilterBody` properly serializes both fields (verified in contrast-sdk-java)
- Current MCP code already sends filters to API, then redundantly filters in-memory

**Problems with current approach:**
1. Performance - fetches up to 50k traces for in-memory filtering
2. Complexity - two execution paths with different behavior
3. Bugs - odd issues have appeared with in-memory filtering logic

## Solution

Delete the in-memory filtering path entirely and trust server-side filtering.

## Code Changes

### Delete from `SearchAppVulnerabilitiesTool.java`

| Lines | What | Notes |
|-------|------|-------|
| 41 | `import java.util.function.Predicate` | No longer needed |
| 65-66 | `maxTracesForSessionFiltering` field | Config for deleted feature |
| 179-183 | Branching in `doExecute()` | Replace with single path |
| 219-314 | `executeWithInMemorySessionFiltering()` | Entire method |
| 324-336 | `SessionFilteringResult` record | Only used by in-memory path |
| 338-447 | `fetchTracesWithEarlyTermination()` | In-memory pagination loop |
| 454-511 | `buildSessionFilterPredicate()` | In-memory predicate |

### Keep (still needed for field resolution)

| Lines | What | Why Keep |
|-------|------|----------|
| 517-525 | `normalizeFilterValue()` | Used by `resolveSessionMetadataFilters()` |
| 537-571 | `resolveSessionMetadataFilters()` | API requires numeric field IDs |
| 581-596 | `buildFieldNameToIdMapping()` | Helper for resolution |

### Modify

**`executeWithServerSidePagination()`** - expand to handle session params:
1. If `useLatestSession=true`, fetch latest session ID via `getLatestSessionMetadata()`
2. If `sessionMetadataFilters` present, resolve field names to IDs
3. Build `ExtendedTraceFilterBody` with all filters (standard + session)
4. Single API call with server-side filtering + pagination

### Delete from `SearchAppVulnerabilitiesParams.java`

- `needsSessionFiltering()` method (lines 186-189)

## New Execution Flow

```
doExecute()
  ├── Fetch latest session ID (if useLatestSession=true)
  ├── Resolve metadata field names to IDs (if sessionMetadataFilters present)
  ├── Build ExtendedTraceFilterBody with all params
  ├── Single API call with server-side filtering
  └── Map results and return
```

No branching. Same path for all requests.

## Test Changes

**Delete:**
- `needsSessionFiltering()` tests
- In-memory filtering behavior tests (early termination, truncation warnings, predicate logic)

**Update:**
- Tests that mock two-path behavior → verify single path with session params

**Add:**
- Verify `agentSessionId` passed to API when `useLatestSession=true`
- Verify `metadataFilters` with resolved IDs passed to API

## Impact

- **Net reduction:** ~210 lines
- **Risk:** Low - API already receives these filters today
- **Behavior change:** None expected - server applies same filters

## Verification

Before implementation, verified server-side support:
1. TeamServer source (`VulnerabilityMetadataFilterService.java`) applies metadata filters in database query
2. SDK `TraceFilterBody` has `agentSessionId` and `metadataFilters` fields with serialization tests
3. `ExtendedTraceFilterBody` already sets these fields correctly

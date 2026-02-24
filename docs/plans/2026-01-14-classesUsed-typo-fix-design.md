# Fix 'classedUsed' Typo in LibraryExtended

**Bead:** mcp-3b9v
**Date:** 2026-01-14
**Status:** Ready for implementation

## Summary

Fix the typo in `LibraryExtended.java` where the field `classedUsed` should be `classesUsed`. The typo originated from copying the SDK's misspelled getter name (`getClassedUsed()`), though the SDK's field is correctly named.

## Change

**File:** `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/data/LibraryExtended.java`

**Line 46:** Rename field `classedUsed` → `classesUsed`

```java
// Before
@SerializedName("classes_used")
private int classedUsed;

// After
@SerializedName("classes_used")
private int classesUsed;
```

## Design Decisions

1. **No `@JsonProperty` annotation** - Jackson uses field name directly, outputting `classesUsed` (camelCase), matching the codebase standard (see `VulnLight` and other result DTOs)

2. **Keep `@SerializedName("classes_used")`** - Required for Gson deserialization from Contrast API

3. **No backwards compatibility shim** - MCP clients are AI agents that adapt to field name changes

4. **No upstream SDK report** - Out of scope; the SDK typo is their concern

## JSON Output Change

```json
// Before
{"classCount": 78, "classedUsed": 0, ...}

// After
{"classCount": 78, "classesUsed": 0, ...}
```

## Verification

1. `make format && make check-test` - Build and unit tests pass
2. `make verify` - Integration tests pass
3. Manual: Call `list_application_libraries` via MCP, confirm JSON shows `classesUsed`

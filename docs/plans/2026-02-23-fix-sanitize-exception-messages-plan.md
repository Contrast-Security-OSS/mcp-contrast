---
title: "fix: Sanitize exception messages returned to MCP clients"
type: fix
status: active
date: 2026-02-23
bead: mcp-21rd
parent-bead: mcp-2tsm
---

# fix: Sanitize exception messages returned to MCP clients

## Overview

`SingleTool` and `PaginatedTool` return raw `e.getMessage()` content to MCP clients in their `errors` and `warnings` lists. Several tool-level catch blocks do the same. Exception messages from the Contrast SDK — particularly `HttpResponseException.getMessage()` — can contain full API paths (including org IDs and app IDs), HTTP methods, status codes, response bodies, and internal server error pages. These are serialized directly to AI agents consuming the MCP tools.

This fix replaces all 9 identified leak points with generic user-safe messages while preserving full exception details in server-side logs.

## Problem Statement

Exception messages are a security boundary. The README warns that vulnerability data flowing to AI agents can be logged or used for model training. `HttpResponseException.getMessage()` is the primary threat vector — it constructs a multi-line string containing the HTTP method, the full API path (with org/app IDs), status code, and the **entire response body**. `ResourceNotFoundException` extends this class and inherits the same behavior.

Current code in both base classes:
```java
// SingleTool.java:103 — leaks full exception to MCP client
return SingleToolResponse.error("Internal error: " + e.getMessage());

// PaginatedTool.java:109 — leaks ResourceNotFoundException details
return handleException(e, pagination, requestId, "Resource not found: " + e.getMessage());
```

## Proposed Solution

**Approach: Direct string replacement at each leak point.** No centralized utility needed — each fix is a one-line change. The exception details are already logged server-side in all cases. We just stop forwarding them to the response.

**Include `requestId` in error-level messages** for log correlation: `"An internal error occurred (ref: a1b2c3d4)"`. This lets users report issues without exposing exception internals. Omit from warning-level messages (optional data failures) since those are non-critical.

**Upgrade tool-level logging** from `DEBUG` with `e.getMessage()` only to `WARN` with `setCause(e)` for full stack traces. After removing the client-facing messages, server logs become the sole diagnostic source.

## Replacement Messages

| # | Location | Current | Replacement |
|---|----------|---------|-------------|
| 1 | `SingleTool.java:87` | `e.getMessage()` (ResourceNotFoundException) | `"Resource not found"` |
| 2 | `SingleTool.java:103` | `"Internal error: " + e.getMessage()` | `"An internal error occurred (ref: " + requestId + ")"` |
| 3 | `PaginatedTool.java:109` | `"Resource not found: " + e.getMessage()` | `"Resource not found"` |
| 4 | `PaginatedTool.java:119` | `"Internal error: " + e.getMessage()` | `"An internal error occurred (ref: " + requestId + ")"` |
| 5 | `GetVulnerabilityTool.java:134` | `"Recommendation data not available: " + e.getMessage()` | `"Recommendation data not available"` |
| 6 | `GetVulnerabilityTool.java:145` | `"HTTP request data not available: " + e.getMessage()` | `"HTTP request data not available"` |
| 7 | `GetVulnerabilityTool.java:153` | `"Stack trace data not available: " + e.getMessage()` | `"Stack trace data not available"` |
| 8 | `ListApplicationsByCveTool.java:146-150` | `"Could not fetch class usage data for application '" + app.getName() + "': " + e.getMessage()` | `"Could not fetch class usage data for application '" + app.getName() + "'"` |
| 9 | `MetadataJsonFilterSpec.java:82-86` | `"Invalid JSON for %s: %s"` with `e.getMessage()` | `"Invalid JSON for %s. Expected format: {\"field\":\"value\"} or {\"field\":[\"value1\",\"value2\"]}"` |

**Design decisions:**
- `app.getName()` in #8 is kept — the tool already returns app names in its normal response data, so this is not a leak
- `requestId` is included only in error-level messages (#2, #4) — not in warnings (#5-8) where failures are optional/non-critical
- MetadataJsonFilterSpec (#9) drops the Gson parse error and keeps just the format guidance — the user already has the expected format in the message

## Acceptance Criteria

- [ ] All 9 leak points replaced with generic messages per the table above
- [ ] No `e.getMessage()` content appears in any `errors` or `warnings` list returned to MCP clients
- [ ] Full exception details preserved in server-side logs at all 9 locations
- [ ] Tool-level catch blocks in GetVulnerabilityTool and ListApplicationsByCveTool upgraded to `WARN` level with `setCause(e)`
- [ ] Existing tests updated to assert new generic messages
- [ ] New negative tests verify exception message text does NOT appear in responses
- [ ] `make check-test` passes with 0 failures
- [ ] `make verify` passes (integration tests)

## File Changes

### `SingleTool.java` (2 changes)

**Line 87** — ResourceNotFoundException handler:
```java
// Before:
return SingleToolResponse.notFound(e.getMessage(), warnings);

// After:
return SingleToolResponse.notFound("Resource not found", warnings);
```

**Lines 97-103** — Generic Exception handler:
```java
// Before:
return SingleToolResponse.error("Internal error: " + e.getMessage());

// After:
return SingleToolResponse.error("An internal error occurred (ref: " + requestId + ")");
```

### `PaginatedTool.java` (2 changes)

**Line 109** — ResourceNotFoundException handler:
```java
// Before:
return handleException(e, pagination, requestId, "Resource not found: " + e.getMessage());

// After:
return handleException(e, pagination, requestId, "Resource not found");
```

**Lines 112-119** — Generic Exception handler:
```java
// Before:
return PaginatedToolResponse.error(
    pagination.page(), pagination.pageSize(), "Internal error: " + e.getMessage());

// After:
return PaginatedToolResponse.error(
    pagination.page(), pagination.pageSize(),
    "An internal error occurred (ref: " + requestId + ")");
```

### `GetVulnerabilityTool.java` (3 changes)

**Lines 132-134** — Recommendation fetch:
```java
// Before:
log.debug("Could not fetch recommendation for {}: {}", vulnId, e.getMessage());
warnings.add("Recommendation data not available: " + e.getMessage());

// After:
log.atWarn().addKeyValue("vulnId", vulnId).setCause(e)
    .setMessage("Could not fetch recommendation").log();
warnings.add("Recommendation data not available");
```

**Lines 143-145** — HTTP request fetch:
```java
// Before:
log.debug("Could not fetch HTTP request for {}: {}", vulnId, e.getMessage());
warnings.add("HTTP request data not available: " + e.getMessage());

// After:
log.atWarn().addKeyValue("vulnId", vulnId).setCause(e)
    .setMessage("Could not fetch HTTP request").log();
warnings.add("HTTP request data not available");
```

**Lines 151-153** — Stack trace fetch:
```java
// Before:
log.debug("Could not fetch stack trace data for {}: {}", vulnId, e.getMessage());
warnings.add("Stack trace data not available: " + e.getMessage());

// After:
log.atWarn().addKeyValue("vulnId", vulnId).setCause(e)
    .setMessage("Could not fetch stack trace data").log();
warnings.add("Stack trace data not available");
```

### `ListApplicationsByCveTool.java` (1 change)

**Lines 145-150** — Library data fetch:
```java
// Before:
log.debug("Could not fetch library data for app {}: {}", app.getAppId(), e.getMessage());
warnings.add(
    "Could not fetch class usage data for application '"
        + app.getName()
        + "': "
        + e.getMessage());

// After:
log.atWarn().addKeyValue("appId", app.getAppId()).setCause(e)
    .setMessage("Could not fetch library data").log();
warnings.add("Could not fetch class usage data for application '" + app.getName() + "'");
```

### `MetadataJsonFilterSpec.java` (1 change)

**Lines 82-86** — JSON parse error:
```java
// Before:
ctx.addError(
    String.format(
        "Invalid JSON for %s: %s. Expected format: {\"field\":\"value\"} or "
            + "{\"field\":[\"value1\",\"value2\"]}",
        name, e.getMessage()));

// After:
log.atWarn().addKeyValue("filterName", name).setCause(e)
    .setMessage("Invalid JSON in metadata filter").log();
ctx.addError(
    String.format(
        "Invalid JSON for %s. Expected format: {\"field\":\"value\"} or "
            + "{\"field\":[\"value1\",\"value2\"]}",
        name));
```

Note: `MetadataJsonFilterSpec` will need `@Slf4j` added since it doesn't currently have a logger.

### Test Updates

**`SingleToolTest.java`:**
- Line 94: `assertThat(result.warnings()).anyMatch(w -> w.contains("not found"))` — should still pass with new message `"Resource not found"`
- Line 168: Change `assertThat(result.errors()).containsExactly("Internal error: Unexpected failure")` to assert `"An internal error occurred (ref: "` prefix

**`PaginatedToolTest.java`:**
- Line 98: Change `assertThat(result.errors().get(0)).startsWith("Resource not found:")` to assert exact `"Resource not found"`
- Line 155: Change `assertThat(result.errors()).containsExactly("Internal error: Unexpected failure")` to assert `"An internal error occurred (ref: "` prefix

**New negative tests** (add to `SingleToolTest` and `PaginatedToolTest`):
- Verify that when a `RuntimeException("sensitive: /api/ng/org-id/traces")` is thrown, the response error does NOT contain `/api/ng/` or `org-id` or `traces`
- Verify the response error starts with `"An internal error occurred (ref: "`

**Tool-level tests** (GetVulnerabilityToolTest, ListApplicationsByCveToolTest):
- Update warning assertions to match truncated messages (without `+ e.getMessage()` suffix)

## Out of Scope

- **`SearchAppVulnerabilitiesTool.java:236` IllegalArgumentException**: This tool uses `throw new IllegalArgumentException(...)` to communicate a user error about invalid metadata field names. After this fix, that message would become generic. This should be addressed separately by converting it to a validation error (adding to the errors list directly rather than throwing). Filed as a follow-up concern under the parent bead mcp-2tsm.
- **`paramsSupplier.get()` exception path**: If the params supplier throws outside the try/catch block, the exception propagates to the Spring AI MCP framework layer. This is a framework-level concern, not specific to exception message sanitization.
- **Structural safeguard (ArchUnit test)**: An automated test preventing future `e.getMessage()` in response strings would be nice but is over-engineering for a 9-point fix. A note in MCP_STANDARDS.md about not including exception messages in responses is sufficient.

## Sources

- Bead mcp-21rd (Sanitize exception messages returned to MCP clients)
- Parent bead mcp-2tsm (AIML-481: Security Hardening)
- Related brainstorm: `docs/brainstorms/2026-02-23-httpRequest-redaction-brainstorm.md` (denylist approach for sensitive data — same parent bead)
- Contrast SDK `HttpResponseException.getMessage()` constructs multi-line strings with HTTP method, full API path, status code, and response body

---
title: "fix: Redact sensitive headers in get_vulnerability httpRequest output"
type: fix
status: active
date: 2026-02-23
origin: docs/brainstorms/2026-02-23-httpRequest-redaction-brainstorm.md
bead: mcp-1v4u
parent-bead: mcp-2tsm
---

# fix: Redact Sensitive Headers in get_vulnerability httpRequest Output

## Overview

`GetVulnerabilityTool` passes raw HTTP request text from the Contrast SDK directly to MCP clients/AI agents. This text can contain `Authorization` headers, cookies, bearer tokens, and other live credentials. These are session artifacts — not vulnerability data — and should not be forwarded to AI agents.

This plan introduces `HttpRequestRedactor`, a static utility that sanitizes HTTP request data using the SDK's structured fields (with a regex fallback for raw text) before it enters the response pipeline.

## Problem Statement / Motivation

The current code at `GetVulnerabilityTool.java:141` calls `requestResponse.getHttpRequest().getText()` and passes the raw string through `VulnerabilityContext` → `VulnerabilityMapper` → `Vulnerability` record → AI agent with zero transformation. This leaks:

- `Authorization: Bearer <token>` — live API/session tokens
- `Cookie: session=<value>` — session cookies
- `Set-Cookie` — server-issued credentials
- Custom auth headers (`X-API-Key`, `X-Contrast-Token`, `Proxy-Authorization`)

While the data flows locally (Contrast API → local MCP server → local AI agent), forwarding live credentials to AI models is unnecessary risk that should be eliminated (see brainstorm: `docs/brainstorms/2026-02-23-httpRequest-redaction-brainstorm.md`).

## Proposed Solution

Create a static utility `HttpRequestRedactor` in `tool/base/` that:

1. **Primary path (structured fields):** Uses `getMethod()`, `getUri()`, `getVersion()`, `getHeaders()` (as `List<NameValuePair>`) to reconstruct a sanitized request text. Sensitive headers get their values replaced with `[REDACTED]`. Body is extracted from `getText()` and appended unchanged.

2. **Fallback path (regex on raw text):** When `getHeaders()` is null/empty, parses `getText()` line-by-line and replaces values of lines matching sensitive header patterns with `[REDACTED]`.

3. **Null safety:** Returns `null` for null input. Handles null/empty fields at every level.

## Technical Considerations

- **SDK `HttpRequest` has no `getBody()` method** — body content exists only inside `getText()`. The hybrid approach extracts the body by splitting `getText()` on the double-newline separator (`\r?\n\r?\n`), using everything after the separator as the body.
- **Structured `headers` field may not always be populated** by the Contrast API. The `text` field appears to always be present. The fallback regex path handles this case.
- **Case-insensitive matching** — HTTP header names are case-insensitive per RFC 7230. Both paths must match regardless of case.
- **Duplicate headers** — HTTP allows multiple headers with the same name (e.g., multiple `Set-Cookie`). Both paths must handle all occurrences.
- **`NameValuePair` null safety** — SDK's `NameValuePair.getName()` may return null. Skip entries with null names.
- **No existing redaction patterns** in the codebase — this is a new utility following the `FilterHelper` pattern (see brainstorm decisions).

## Acceptance Criteria

- [ ] `HttpRequestRedactor.sanitize(HttpRequest)` returns sanitized text with sensitive header values replaced by `[REDACTED]`
- [ ] Denylist headers: `Authorization`, `Cookie`, `Set-Cookie`, `X-API-Key`, `X-Contrast-Token`, `Proxy-Authorization` (case-insensitive)
- [ ] Non-sensitive headers pass through unchanged
- [ ] Request body preserved unchanged
- [ ] Structured fields path: reconstructs from `getMethod()`, `getUri()`, `getVersion()`, `getHeaders()`
- [ ] Fallback path: regex on `getText()` when structured headers unavailable
- [ ] Null input returns null; null individual fields handled gracefully
- [ ] `GetVulnerabilityTool.buildVulnerabilityContext()` updated to use `HttpRequestRedactor.sanitize()` instead of `getText()`
- [ ] Unit tests for `HttpRequestRedactor` covering all paths and edge cases
- [ ] Existing tests continue to pass
- [ ] `make check-test` passes

## Implementation

### File: `src/main/java/com/contrast/labs/ai/mcp/contrast/tool/base/HttpRequestRedactor.java` (NEW)

Static utility class following the `FilterHelper` pattern in the same package.

```java
@Slf4j
public final class HttpRequestRedactor {

  private static final Set<String> SENSITIVE_HEADERS = Set.of(
      "authorization",
      "cookie",
      "set-cookie",
      "x-api-key",
      "x-contrast-token",
      "proxy-authorization"
  );

  private static final String REDACTED = "[REDACTED]";

  private HttpRequestRedactor() {}

  /**
   * Sanitize an HttpRequest by replacing sensitive header values with [REDACTED].
   * Uses structured fields when available, falls back to regex on raw text.
   *
   * @param httpRequest the SDK HttpRequest object (may be null)
   * @return sanitized request text, or null if input is null
   */
  public static String sanitize(HttpRequest httpRequest) {
    if (httpRequest == null) {
      return null;
    }

    var headers = httpRequest.getHeaders();
    if (headers != null && !headers.isEmpty()) {
      return sanitizeFromStructuredFields(httpRequest);
    }

    // Fallback: regex on raw text
    var text = httpRequest.getText();
    if (!StringUtils.hasText(text)) {
      return null;
    }
    return sanitizeFromRawText(text);
  }
}
```

**`sanitizeFromStructuredFields(HttpRequest)`** — Build request line from `getMethod()` + `getUri()` + `getVersion()`. Iterate `getHeaders()`, replacing sensitive header values with `[REDACTED]`. Extract body from `getText()` (split on `\r?\n\r?\n`), append if present.

**`sanitizeFromRawText(String text)`** — Split text by `\r?\n`. First line is the request line (pass through). Subsequent lines until blank line are headers — match `^(HeaderName):\s*(.*)$` pattern, replace value with `[REDACTED]` if name matches denylist. Everything after blank line is body (pass through).

**`isSensitive(String headerName)`** — Case-insensitive check against `SENSITIVE_HEADERS` set.

### File: `src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/GetVulnerabilityTool.java` (MODIFY)

Change lines 138-146 in `buildVulnerabilityContext()`:

```java
// Before:
try {
  var requestResponse = sdk.getHttpRequest(orgId, vulnId);
  if (requestResponse != null && requestResponse.getHttpRequest() != null) {
    httpRequestText = requestResponse.getHttpRequest().getText();
  }
}

// After:
try {
  var requestResponse = sdk.getHttpRequest(orgId, vulnId);
  if (requestResponse != null && requestResponse.getHttpRequest() != null) {
    httpRequestText = HttpRequestRedactor.sanitize(requestResponse.getHttpRequest());
  }
}
```

Single line change: `.getText()` → `HttpRequestRedactor.sanitize(...)`. Import added for `HttpRequestRedactor`.

### File: `src/test/java/com/contrast/labs/ai/mcp/contrast/tool/base/HttpRequestRedactorTest.java` (NEW)

Unit tests covering:

| Test Case | Input | Expected Output |
|-----------|-------|-----------------|
| `sanitize_should_return_null_when_input_null` | `null` | `null` |
| `sanitize_should_return_null_when_text_empty` | HttpRequest with null text, null headers | `null` |
| `sanitize_should_redact_sensitive_headers_from_structured_fields` | Headers with Authorization + Cookie | Values replaced with `[REDACTED]` |
| `sanitize_should_preserve_nonsensitive_headers` | Host, Content-Type headers | Headers unchanged |
| `sanitize_should_be_case_insensitive` | `aUtHoRiZaTiOn` header | Value redacted |
| `sanitize_should_handle_duplicate_sensitive_headers` | Multiple Set-Cookie entries | All redacted |
| `sanitize_should_skip_headers_with_null_name` | NameValuePair with null name | Skipped, no NPE |
| `sanitize_should_preserve_body_from_getText` | POST with body in getText() | Body preserved after headers |
| `sanitize_should_fallback_to_regex_when_headers_null` | Headers null, getText() has full request | Sensitive header values redacted via regex |
| `sanitize_should_fallback_to_regex_when_headers_empty` | Empty headers list, getText() present | Regex fallback |
| `sanitize_should_preserve_body_in_fallback_path` | getText() with body | Body preserved |
| `sanitize_should_build_request_line_from_fields` | Method=GET, URI=/api/test, Version=1.1 | `GET /api/test HTTP/1.1` |
| `sanitize_should_handle_all_denylist_headers` | One of each denied header | All redacted |

## Success Metrics

- Zero sensitive header values in `get_vulnerability` output
- All existing tests pass unchanged
- `make check-test` green

## Dependencies & Risks

- **Risk: Structured headers not populated by API** — Mitigated by regex fallback path. Integration tests should verify which path is exercised against real data.
- **Risk: Body extraction from getText() is fragile** — HTTP double-newline separator is well-specified (RFC 7230). Edge case: if getText() doesn't follow standard format, body may be lost. Acceptable since getText() is the fallback data source.
- **Out of scope**: ADR `Event.httpRequest` redaction (separate code path with different model). Tracked under parent epic `mcp-2tsm`.

## Sources & References

- **Origin brainstorm:** [docs/brainstorms/2026-02-23-httpRequest-redaction-brainstorm.md](../brainstorms/2026-02-23-httpRequest-redaction-brainstorm.md) — Key decisions: structured fields over raw text, denylist strategy, headers-only scope, static utility pattern
- SDK HttpRequest model: `/contrast-sdk-java/sdk/src/main/java/com/contrastsecurity/models/HttpRequest.java`
- SDK NameValuePair: `/contrast-sdk-java/sdk/src/main/java/com/contrastsecurity/models/NameValuePair.java`
- FilterHelper pattern: `src/main/java/com/contrast/labs/ai/mcp/contrast/tool/base/FilterHelper.java`
- Current code: `src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/GetVulnerabilityTool.java:138-146`

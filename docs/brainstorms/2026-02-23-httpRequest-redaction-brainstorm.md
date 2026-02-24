---
date: 2026-02-23
topic: httpRequest-redaction
bead: mcp-1v4u
parent-bead: mcp-2tsm
---

# Redact Sensitive Data in get_vulnerability httpRequest Output

## What We're Building

A static utility that sanitizes HTTP request data before it reaches MCP clients/AI agents. The `GetVulnerabilityTool` currently passes the raw HTTP request text from Contrast's SDK directly to the output, which can include Authorization headers, cookies, bearer tokens, and other live credentials. These are not vulnerability data — they're session artifacts that shouldn't be forwarded to AI agents.

The utility will use the SDK's structured `HttpRequest` fields (method, URI, headers as `List<NameValuePair>`) to reconstruct a clean text representation with sensitive headers stripped, rather than parsing the raw text blob.

## Why This Approach

**Structured fields over raw text parsing:** The SDK `HttpRequest` model already exposes headers as `List<NameValuePair>` alongside `getMethod()`, `getUri()`, etc. Using these structured fields gives us precise header-level filtering without fragile regex/line-parsing on the raw `getText()` blob. More reliable, easier to test, and handles edge cases like multi-line header values.

**Denylist over allowlist:** A denylist (blocking known sensitive headers) preserves useful debugging context — headers like Host, Content-Type, Accept, User-Agent, X-Forwarded-For are valuable for understanding vulnerability context. An allowlist would be stricter but would lose useful information and require maintenance every time a new useful header appears.

**Headers only, not body:** The request body is typically the most useful part for understanding vulnerability context (SQL injection payloads, XSS vectors, etc.). Credential exposure in request bodies is less common and harder to detect reliably. Stripping auth headers addresses the primary risk without sacrificing vulnerability analysis quality.

**Always include, no opt-in parameter:** HTTP request context is valuable for vulnerability analysis. Redaction makes it safe to include by default. No parameter changes needed — keeps the tool interface stable.

## Key Decisions

- **Data source**: Use SDK structured fields (`getHeaders()`, `getMethod()`, `getUri()`) instead of `getText()`
- **Filter strategy**: Denylist — strip specific sensitive headers, pass everything else through
- **Denied headers**: `Authorization`, `Cookie`, `Set-Cookie`, `X-API-Key`, `X-Contrast-Token`, `Proxy-Authorization` (case-insensitive matching)
- **Body handling**: Pass through unchanged — body is critical for vulnerability context
- **Design pattern**: Static utility class `HttpRequestRedactor` following the `FilterHelper` pattern
- **Location**: `tool/base/` or `tool/security/` package
- **Opt-in**: Not needed — always include redacted httpRequest (current behavior preserved)
- **Injection point**: In `GetVulnerabilityTool.buildVulnerabilityContext()` — sanitize before placing into `VulnerabilityContext`

## Open Questions

None — all design decisions resolved during brainstorming.

## Next Steps

-> `/workflows:plan` for implementation details

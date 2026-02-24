---
title: "fix: Enforce HTTPS-only protocol for Contrast API communication"
type: fix
status: active
date: 2026-02-23
origin: docs/brainstorms/2026-02-23-protocol-validation-brainstorm.md
bead: mcp-24s9
parent-bead: mcp-2tsm
---

# fix: Enforce HTTPS-only Protocol for Contrast API Communication

## Overview

The MCP server must refuse to start if the Contrast API protocol is anything other than HTTPS. Currently, `SDKHelper.getProtocolAndServer()` silently accepts `http` as a protocol, and `ContrastSDKFactory.validateConfiguration()` does not validate the protocol at all. This allows API keys, service keys, and vulnerability data to be sent in plaintext over HTTP.

This plan adds startup validation that rejects HTTP (and any non-HTTPS scheme) via both the `contrast.api.protocol` property and schemes embedded in `CONTRAST_HOST_NAME` (see brainstorm: `docs/brainstorms/2026-02-23-protocol-validation-brainstorm.md`).

## Problem Statement / Motivation

Two code paths allow insecure HTTP:

1. **Protocol property**: Setting `contrast.api.protocol=http` causes `SDKHelper.getProtocolAndServer()` to prepend `http://` to bare hostnames. No validation, no warning.
2. **Hostname scheme**: Setting `CONTRAST_HOST_NAME=http://app.example.com` is accepted as valid by `getProtocolAndServer()` line 197. The protocol property is ignored entirely.

Both paths send Contrast API credentials (`CONTRAST_API_KEY`, `CONTRAST_SERVICE_KEY`) unencrypted over the network. Garbage values like `ftp`, `ws`, or typos like `htps` are also silently accepted for the protocol property.

## Proposed Solution

Validate at startup in `ContrastSDKFactory.validateConfiguration()` (existing `@PostConstruct` method). Reject with `IllegalStateException` — same pattern as missing credential validation. Two checks:

1. **Protocol property**: If set, must be `https` (case-insensitive). Blank/null defaults to `https` (existing behavior preserved).
2. **Hostname scheme**: If `CONTRAST_HOST_NAME` contains `://`, the scheme must be `https://` (case-insensitive). Reject `http://`.

Additionally, harden `SDKHelper.getProtocolAndServer()` as defense-in-depth — reject `http://` there too, since it's a public static method callable from other contexts.

## Technical Considerations

- **Fail-fast at startup**: Both checks run in `@PostConstruct`, before any SDK call. The hostname scheme check currently only runs lazily in `getProtocolAndServer()` (first tool invocation). Moving it to `validateConfiguration()` ensures consistent fail-fast behavior.
- **Case-insensitive matching**: Normalize to lowercase before comparison. `HTTPS`, `Https` → accepted. `HTTP`, `Http` → rejected with security-specific message. This fixes a current bug where `HTTP://example.com` falls through to a misleading "invalid protocol" error.
- **Protocol property with `://` suffix**: Reject `contrast.api.protocol=https://` with a clear message ("use 'https', not 'https://'"). Currently produces a malformed URL at runtime.
- **Existing tests use HTTP**: `SDKHelperTest` has 4 tests that explicitly expect HTTP to succeed (`testGetProtocolAndServer_WithHttpProtocol`, etc.). These must be updated to expect rejection.
- **application.properties comment is now stale**: Line 18 says "This can be http or https" — must be updated.
- **CLAUDE.md says protocol is "configurable for local development"** — must be updated.
- **Integration test config** (`IntegrationTestConfig.java:58`) reads `contrast.api.protocol` directly with default `https` — no change needed since it defaults to HTTPS.
- **Property indirection chain**: `contrast.protocol` resolves from `contrast.api.protocol`. Validation reads `properties.protocol()` which is the final resolved value, so all override paths (env var, CLI, system property) are covered.

## Acceptance Criteria

- [ ] Server refuses to start with `contrast.api.protocol=http` — throws `IllegalStateException`
- [ ] Server refuses to start with `CONTRAST_HOST_NAME=http://...` — throws `IllegalStateException`
- [ ] Server refuses to start with invalid protocols (`ftp`, `ws`, `htps`, etc.) — throws `IllegalStateException`
- [ ] Server refuses to start with `contrast.api.protocol=https://` (with `://` suffix) — throws `IllegalStateException` with helpful message
- [ ] Case-insensitive: `HTTPS`, `Https` accepted; `HTTP`, `Http` rejected
- [ ] Blank/null protocol defaults to `https` (existing behavior preserved)
- [ ] Bare hostname without scheme works with default HTTPS
- [ ] `CONTRAST_HOST_NAME=https://...` accepted
- [ ] Error messages are clear and actionable, telling users exactly what to fix
- [ ] `application.properties` comment updated
- [ ] `CLAUDE.md` protocol description updated
- [ ] All existing tests updated and passing
- [ ] New unit tests for all validation scenarios
- [ ] `make check-test` passes

## Implementation

### File: `src/main/java/com/contrast/labs/ai/mcp/contrast/config/ContrastSDKFactory.java` (MODIFY)

Add `@Slf4j` annotation (class currently has no logger). Add protocol validation to the existing `validateConfiguration()` method, after the credential checks:

```java
// After the existing missing-credentials check:

// Validate protocol
var protocol = properties.protocol();
if (StringUtils.hasText(protocol)) {
  var normalizedProtocol = protocol.strip().toLowerCase();
  if (normalizedProtocol.contains("://")) {
    throw new IllegalStateException(
        "Invalid contrast.api.protocol value: '" + protocol + "'. "
            + "Set to 'https', not 'https://'. The protocol separator is added automatically.");
  }
  if (!"https".equals(normalizedProtocol)) {
    throw new IllegalStateException(
        "Insecure protocol configured: '" + protocol + "'. "
            + "The MCP server requires HTTPS to protect API credentials. "
            + "Set contrast.api.protocol=https or remove the property to use the default.");
  }
}

// Validate hostname scheme
var hostName = properties.hostName();
if (StringUtils.hasText(hostName) && hostName.strip().toLowerCase().contains("://")) {
  if (!hostName.strip().toLowerCase().startsWith("https://")) {
    throw new IllegalStateException(
        "Insecure protocol in CONTRAST_HOST_NAME: '" + hostName + "'. "
            + "Use 'https://' or provide the hostname without a scheme to use HTTPS by default.");
  }
}
```

### File: `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKHelper.java` (MODIFY)

Harden `getProtocolAndServer()` as defense-in-depth. In the branch where `hostName.contains("://")` (line 190), add case-insensitive HTTP rejection before the existing invalid-protocol check:

```java
if (hostName.contains("://")) {
  var lowerHostName = hostName.toLowerCase();
  if (lowerHostName.startsWith("http://")) {
    throw new IllegalArgumentException(
        "Insecure protocol in hostname: '" + hostName + "'. Use https:// instead.");
  }
  if (!lowerHostName.startsWith("https://")) {
    throw new IllegalArgumentException(
        "Invalid protocol in hostname: '" + hostName + "'. Only https:// is supported.");
  }
  result = hostName;
}
```

In the `else` branch (bare hostname), validate the effective protocol:

```java
var effectiveProtocol = StringUtils.hasText(protocol) ? protocol.strip().toLowerCase() : "https";
if (!"https".equals(effectiveProtocol)) {
  throw new IllegalArgumentException(
      "Insecure protocol: '" + protocol + "'. Only 'https' is supported.");
}
result = effectiveProtocol + "://" + hostName;
```

### File: `src/main/resources/application.properties` (MODIFY)

Update the comment on line 18:

```properties
# Only HTTPS is supported. The server will refuse to start with HTTP or other protocols.
contrast.api.protocol=https
```

### File: `CLAUDE.md` (MODIFY)

Update the configuration section. Change:
```
- `contrast.api.protocol=https` (configurable for local development)
```
To:
```
- `contrast.api.protocol=https` (HTTPS only — server rejects HTTP at startup)
```

### File: `src/test/java/com/contrast/labs/ai/mcp/contrast/config/ContrastSDKFactoryTest.java` (MODIFY)

Add protocol validation tests following the existing pattern:

| Test Case | Input | Expected |
|-----------|-------|----------|
| `validateConfiguration_should_throw_when_protocol_is_http` | protocol="http" | IllegalStateException with "Insecure protocol" |
| `validateConfiguration_should_throw_when_protocol_is_HTTP_uppercase` | protocol="HTTP" | IllegalStateException |
| `validateConfiguration_should_throw_when_protocol_is_ftp` | protocol="ftp" | IllegalStateException |
| `validateConfiguration_should_throw_when_protocol_has_scheme_suffix` | protocol="https://" | IllegalStateException with "not 'https://'" |
| `validateConfiguration_should_pass_when_protocol_is_https` | protocol="https" | No exception |
| `validateConfiguration_should_pass_when_protocol_is_HTTPS_uppercase` | protocol="HTTPS" | No exception |
| `validateConfiguration_should_pass_when_protocol_is_blank` | protocol="" | No exception (defaults to https) |
| `validateConfiguration_should_pass_when_protocol_is_null` | protocol=null | No exception (defaults to https) |
| `validateConfiguration_should_throw_when_hostname_has_http_scheme` | hostName="http://app.example.com" | IllegalStateException |
| `validateConfiguration_should_pass_when_hostname_has_https_scheme` | hostName="https://app.example.com" | No exception |
| `validateConfiguration_should_throw_when_hostname_has_HTTP_uppercase_scheme` | hostName="HTTP://app.example.com" | IllegalStateException |

### File: `src/test/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKHelperTest.java` (MODIFY)

Update existing tests that expect HTTP to succeed — they must now expect `IllegalArgumentException`:

- `testGetProtocolAndServer_WithHttpProtocol` → expect exception
- `testGetProtocolAndServer_WithHttpProtocolConfig` → expect exception
- `testGetProtocolAndServer_WithHttpProtocolAndTrailingSlash` → expect exception
- `testGetSDK_WithHttpProtocol` → expect exception

Add new tests:

| Test Case | Input | Expected |
|-----------|-------|----------|
| `testGetProtocolAndServer_should_reject_http_hostname` | hostName="http://example.com" | IllegalArgumentException |
| `testGetProtocolAndServer_should_reject_HTTP_uppercase_hostname` | hostName="HTTP://example.com" | IllegalArgumentException |
| `testGetProtocolAndServer_should_accept_https_hostname` | hostName="https://example.com" | "https://example.com" |
| `testGetProtocolAndServer_should_accept_HTTPS_uppercase_hostname` | hostName="HTTPS://example.com" | "HTTPS://example.com" |
| `testGetProtocolAndServer_should_reject_http_protocol_property` | protocol="http" | IllegalArgumentException |
| `testGetProtocolAndServer_should_accept_bare_hostname_with_default` | hostName="example.com", protocol=null | "https://example.com" |

## Success Metrics

- Server refuses to start with any non-HTTPS configuration
- All existing tests updated and passing
- `make check-test` green

## Dependencies & Risks

- **Risk: Breaking existing users who use HTTP** — Mitigated by the fact that HTTP should never have been used with remote Contrast instances. The default is already HTTPS. Only users who explicitly opted into HTTP are affected, and they receive a clear error message explaining how to fix it.
- **Risk: Integration test environments** — The `.env.integration-test.template` does not include a protocol setting, so tests default to HTTPS. Verified no HTTP usage in integration test config.
- **Out of scope**: Updating installation guide documentation (follow-up task). Simplifying the `contrast.protocol`/`contrast.api.protocol` indirection chain (separate refactor).

## Sources & References

- **Origin brainstorm:** [docs/brainstorms/2026-02-23-protocol-validation-brainstorm.md](../brainstorms/2026-02-23-protocol-validation-brainstorm.md) — Key decisions: HTTPS only, reject at startup, validate both entry points, blank defaults to HTTPS
- Existing validation: `src/main/java/com/contrast/labs/ai/mcp/contrast/config/ContrastSDKFactory.java:76-101`
- URL construction: `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKHelper.java:179-216`
- Config properties: `src/main/java/com/contrast/labs/ai/mcp/contrast/config/ContrastProperties.java:30-39`
- Existing SDK tests: `src/test/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKHelperTest.java`
- Existing factory tests: `src/test/java/com/contrast/labs/ai/mcp/contrast/config/ContrastSDKFactoryTest.java`

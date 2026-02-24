---
date: 2026-02-23
topic: protocol-validation
bead: mcp-24s9
parent-bead: mcp-2tsm
---

# Validate contrast.protocol and Reject Insecure HTTP

## What We're Building

Startup validation that enforces HTTPS-only communication with Contrast TeamServer. The server must refuse to start if the protocol is anything other than HTTPS. This applies both to the `contrast.api.protocol` property and to schemes embedded in `CONTRAST_HOST_NAME`.

Currently, `SDKHelper.getProtocolAndServer()` accepts any protocol string when the hostname lacks a scheme, and `ContrastSDKFactory.validateConfiguration()` does not validate the protocol at all. This means a misconfiguration like `contrast.api.protocol=http` silently sends API keys, service keys, and vulnerability data in plaintext.

## Why This Approach

**Hard reject over warn:** API credentials (API key, service key) are sent with every request to Contrast TeamServer. Sending these over plaintext HTTP is never acceptable in production or development — even localhost scenarios can use HTTPS. A warning is too easily ignored. Failing fast at startup prevents credentials from ever being transmitted insecurely.

**Validate both entry points:** HTTP can sneak in via two paths — the `contrast.api.protocol` property or an `http://` prefix in `CONTRAST_HOST_NAME`. Both must be validated to close the gap completely.

**Default to HTTPS on blank:** If the protocol is blank/null, default to HTTPS (current behavior preserved). The user shouldn't need to explicitly set it.

## Key Decisions

- **HTTPS only**: No HTTP allowed, period. No localhost exceptions.
- **Reject at startup**: Throw `IllegalStateException` from `@PostConstruct` validation, preventing the server from starting. Same pattern as missing credential validation.
- **Validate both paths**: Reject `contrast.api.protocol=http` AND `CONTRAST_HOST_NAME=http://...`
- **Blank defaults to HTTPS**: If protocol property is blank/null, assume HTTPS (existing behavior).
- **Non-http/https schemes also rejected**: Values like `ftp`, `ws`, typos like `htps` — all rejected.
- **Validation location**: `ContrastSDKFactory.validateConfiguration()` (existing `@PostConstruct` method that already validates credentials) plus `SDKHelper.getProtocolAndServer()` (existing method that builds the URL).
- **Error messages**: Clear, actionable messages telling the user exactly what to fix.

## Open Questions

None — all design decisions resolved.

## Next Steps

-> `/workflows:plan` for implementation details

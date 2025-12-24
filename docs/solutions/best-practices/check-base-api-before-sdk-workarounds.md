---
module: MCP Server
date: 2025-12-05
problem_type: best_practice
component: service_object
symptoms:
  - "Built ~200 lines of in-memory filtering code to work around perceived SDK limitation"
  - "Complex SessionFilteringResult record with early termination, pagination handling"
  - "Performance concerns from fetching all data then filtering client-side"
root_cause: wrong_api
resolution_type: workflow_improvement
severity: high
tags: [sdk-design, api-investigation, workaround-prevention, contrast-sdk]
---

# Best Practice: Check Base API Before Building SDK Workarounds

## Problem

Built complex in-memory session filtering (~200 lines) in AssessService because the SDK's `TraceFilterForm` (GET) doesn't support session filtering. Assumed SDK exposed all available functionality.

## Environment

- Module: MCP Server - AssessService
- Affected Component: `search_app_vulnerabilities` tool
- Date: 2025-12-05

## Symptoms

- Complex `SessionFilteringResult` record with early termination logic
- `fetchTracesWithEarlyTermination()` method spanning 100+ lines
- `buildSessionFilterPredicate()` for in-memory filtering
- Performance concerns: fetching up to 50,000 traces to filter client-side
- Truncation warnings when hitting limits

## What Didn't Work

**Assumed SDK represents full API capability:**
- Used `TraceFilterForm` with GET endpoint because that's what existing code used
- Didn't investigate TeamServer API directly
- Built elaborate workaround instead of checking alternatives

## Solution

**Always investigate the base API (TeamServer) before assuming SDK limitations.**

Investigation revealed:
1. TeamServer has POST endpoint `/ng/{org}/traces/{app}/filter` that accepts `NgVulnerabilityFilterRequest`
2. This request object supports `agentSessionId` and `metadataFilters` natively
3. SDK already has `TraceFilterBody` class that maps to this, but it's incomplete
4. SDK has `getTraces(orgId, appId, TraceFilterBody, expand)` method - just needs pagination

**Before building workarounds, always:**

```bash
# 1. Check TeamServer controller for the endpoint
grep -r "traces.*filter" teamserver-app/src/main/java/contrast/teamserver/rest/

# 2. Check the request/response DTOs
find teamserver-app -name "*FilterRequest*" -o -name "*FilterBody*"

# 3. Compare SDK classes to TeamServer DTOs
diff <(grep -E "private.*;" SDK/TraceFilterBody.java) \
     <(grep -E "private.*;" TeamServer/NgVulnerabilityFilterRequest.java)

# 4. Check SDK for alternative methods
grep -A5 "TraceFilterBody" sdk/src/main/java/com/contrastsecurity/sdk/ContrastSDK.java
```

## Why This Works

SDKs are often incomplete representations of APIs:
- SDK maintainers may not expose every endpoint
- Older SDK methods may use deprecated endpoints
- Newer API features may not be added to SDK yet
- SDK may have multiple methods for same data (GET vs POST)

TeamServer is the source of truth. When SDK seems limited:
1. The API might support what you need via different endpoint
2. SDK might have the method but with different name/class
3. Small SDK enhancement may be easier than complex workaround

## Prevention

**Before building any workaround for perceived API limitation:**

1. **Check TeamServer controllers** - Find the actual endpoint
2. **Check TeamServer DTOs** - See what parameters are supported
3. **Check SDK for alternatives** - Different method names, POST vs GET
4. **Compare SDK to DTO** - Identify gaps that need SDK enhancement
5. **Evaluate: SDK fix vs workaround** - Often SDK fix is cleaner

**Questions to ask:**
- "Does the API support this natively?"
- "Is there a POST version of this GET endpoint?"
- "What does the TeamServer request object actually accept?"
- "Is the SDK class complete, or missing fields?"

## Outcome

Created beads to properly fix this:
- `mcp-3dcc` - Audit SDK `TraceFilterBody` vs TeamServer `NgVulnerabilityFilterRequest`
- `mcp-42zo` - Enhance SDK with missing fields and pagination
- `mcp-m4n2` - Implement proper `TraceFilterBody` builder in MCP
- `mcp-lxrt` - Delete the 200 lines of in-memory filtering code

Result: Native API filtering, better performance, simpler code.

## Related Issues

- `mcp-g9dd` - Epic: Native Session Filtering via TraceFilterBody

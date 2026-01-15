# Test Cases for `get_session_metadata` Tool

## Overview

The `get_session_metadata` tool retrieves session metadata for a specific application by its ID. This metadata can be used to filter vulnerabilities in `search_app_vulnerabilities` and routes in `get_route_coverage`.

**Supported parameters:**
- `appId` (required) - Application ID (use search_applications to find)

**Response structure:**
- `data.filters` - Array of metadata field definitions
- Each filter contains: `label`, `id`, `values` (array with `value` and `count`)

**Related tools:**
- `search_applications` - Find application IDs by name, tag, or metadata
- `search_app_vulnerabilities` - Search vulnerabilities with session filtering
- `get_route_coverage` - Get route coverage with session filtering

---

## Known Test Data

### Applications WITH Session Metadata

| App Name | App ID | Language | Metadata Fields | Values |
|----------|--------|----------|-----------------|--------|
| thib-contrast-cargo-cats-dataservice | 03f49f62-efd2-4f7b-9402-8f5f399b0d36 | Java | artifactHash | 593a4019, 3954e560, da09ff43 |
| thib-contrast-cargo-cats-frontgateservice | 03c0a8d2-a6e6-46aa-b602-f242811d10bf | Java | artifactHash | 233b5776, 6ac21e8d, 32a95cd8 |
| Harshaa-MSSentinel-Incident-Event-Data-contrast-cargo-cats-frontgateservice | 25d24972-d71b-41ec-a41b-22e13564a8b9 | Java | artifactHash | 303d65c3, db4ebd74 |
| Harshaa-MSSentinel-Incident-Event-Data-contrast-cargo-cats-dataservice | 48985fed-84e2-45b2-93e0-450e67cf5217 | Java | artifactHash | 2d3467fa, 40bf340a |
| Chris-Cargo-Cats-SmartFix-Demo-contrast-cargo-cats-frontgateservice | cac113a8-04a0-4c74-b289-20b90ea0672e | Java | artifactHash | ae8237ea |
| Chris-Cargo-Cats-SmartFix-Demo-contrast-cargo-cats-dataservice | 8ba749b2-e110-4f9d-b5f6-726a60fbd86f | Java | artifactHash | b7247b03 |
| chris-employee-management-service-smartfix-demo | 3d66538f-da29-4579-8349-792a9c0a6230 | Java | artifactHash | 3143cfdc |
| spring-petclinic | f5f4e038-0bbf-4b5c-b1d1-9ec30ad18c19 | Java | artifactHash | 54f5f4b2 |

### Applications with EMPTY Filters (Sessions exist but no custom metadata)

| App Name | App ID | Language |
|----------|--------|----------|
| spring-petclinic-live-example | 7949c260-6ae9-477f-970a-60d8f95a6f3c | Java |
| employee-management-service | a7f3c2ca-b9d5-4826-b50d-81b4159f806f | Java |
| WebGoatInternDemo-Ian | 04a4b5e6-fe05-4eeb-b25c-6f211ac4ce07 | Java |
| WebGoatDemo | f1716e65-cf6d-4955-a65a-eeb14ffdf1c1 | Java |

---

## Basic Functionality Tests

### Test 1: Retrieve session metadata for app with single metadata value
**Purpose:** Verify tool returns session metadata for an application with one metadata field and one value.

**Prompt:**
```
use contrast mcp to get session metadata for application cac113a8-04a0-4c74-b289-20b90ea0672e
```

**Expected Result:**
- Response status: success
- `found: true`
- `data.filters` contains 1 metadata field
- Field label is "artifactHash"
- Values array contains 1 entry: "ae8237ea"
- Each value has a `count` property indicating session count

---

### Test 2: Retrieve session metadata for app with multiple metadata values
**Purpose:** Verify tool returns all available values for a metadata field.

**Prompt:**
```
use contrast mcp to get session metadata for application 03f49f62-efd2-4f7b-9402-8f5f399b0d36
```

**Expected Result:**
- Response status: success
- `found: true`
- `data.filters` contains 1 metadata field (artifactHash)
- Values array contains 3 entries: "593a4019", "3954e560", "da09ff43"
- Each value has a `count` property

---

### Test 3: Retrieve session metadata for app with multiple values (alternate app)
**Purpose:** Verify consistent behavior across different applications with metadata.

**Prompt:**
```
use contrast mcp to get session metadata for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf
```

**Expected Result:**
- Response status: success
- `found: true`
- `data.filters` contains artifactHash field
- Values array contains 3 entries: "233b5776", "6ac21e8d", "32a95cd8"

---

## Empty/Edge Case Tests

### Test 4: Application with sessions but empty metadata filters
**Purpose:** Verify graceful handling when application has sessions but no custom metadata fields.

**Prompt:**
```
use contrast mcp to get session metadata for application 7949c260-6ae9-477f-970a-60d8f95a6f3c
```

**Expected Result:**
- Response status: success
- `found: true`
- `data.filters` is an empty array `[]`
- No error thrown
- This is valid - the app has sessions but no custom metadata configured

---

### Test 5: Another app with empty filters
**Purpose:** Verify consistent behavior for apps with no custom metadata.

**Prompt:**
```
use contrast mcp to get session metadata for application a7f3c2ca-b9d5-4826-b50d-81b4159f806f
```

**Expected Result:**
- Response status: success
- `found: true`
- `data.filters` is empty array
- No warnings or errors

---

### Test 6: Older application with empty filters
**Purpose:** Verify behavior for older applications that may have limited session data.

**Prompt:**
```
use contrast mcp to get session metadata for application 04a4b5e6-fe05-4eeb-b25c-6f211ac4ce07
```

**Expected Result:**
- Response status: success
- `found: true`
- `data.filters` is empty array (WebGoatInternDemo-Ian from 2020)

---

## Validation Tests

### Test 7: Missing appId parameter
**Purpose:** Verify validation error for missing required parameter.

**Prompt:**
```
use contrast mcp to get session metadata without specifying an application
```

**Expected Result:**
- Validation error returned
- Error message indicates appId is required
- No API call made

---

### Test 8: Empty string appId
**Purpose:** Verify validation catches empty string parameter.

**Prompt:**
```
use contrast mcp to get session metadata for application ""
```

**Expected Result:**
- Validation error returned
- Error message: "appId is required" or similar
- No crash

---

### Test 9: Invalid appId format (not UUID)
**Purpose:** Verify behavior with malformed application ID.

**Prompt:**
```
use contrast mcp to get session metadata for application "invalid-format-12345"
```

**Expected Result:**
- Either validation error or API error
- Clear error message indicating invalid ID
- No crash or hang

---

### Test 10: Nonexistent application ID (valid UUID format)
**Purpose:** Verify behavior when appId doesn't exist in the organization.

**Prompt:**
```
use contrast mcp to get session metadata for application 00000000-0000-0000-0000-000000000000
```

**Expected Result:**
- Error response or null data with warning
- Appropriate error message (e.g., "Application not found")
- No crash

---

### Test 11: Special characters in appId
**Purpose:** Verify handling of special characters in parameter.

**Prompt:**
```
use contrast mcp to get session metadata for application "!!invalid!!"
```

**Expected Result:**
- Validation error or API error
- Clear error message
- No crash

---

### Test 12: Whitespace-only appId
**Purpose:** Verify handling of whitespace-only parameter.

**Prompt:**
```
use contrast mcp to get session metadata for application "   "
```

**Expected Result:**
- Validation error: appId is required (or similar)
- Whitespace should be treated as empty/missing

---

## Integration Workflow Tests

### Test 13: Workflow - Search applications then get metadata
**Purpose:** Verify typical workflow of finding app by name, then getting metadata.

**Prompt:**
```
use contrast mcp to search for applications with name "thib-contrast-cargo-cats-dataservice", then get the session metadata for that application
```

**Expected Result:**
- search_applications returns app with ID 03f49f62-efd2-4f7b-9402-8f5f399b0d36
- get_session_metadata returns artifactHash metadata with 3 values
- Both calls succeed
- IDs match between calls

---

### Test 14: Workflow - Use metadata to filter vulnerabilities
**Purpose:** Verify metadata can be used with search_app_vulnerabilities.

**Prompt:**
```
use contrast mcp to:
1. Get session metadata for application 03f49f62-efd2-4f7b-9402-8f5f399b0d36
2. Then search for vulnerabilities in that application filtered by one of the artifactHash values
```

**Expected Result:**
- get_session_metadata returns artifactHash values (593a4019, 3954e560, da09ff43)
- search_app_vulnerabilities with sessionMetadataFilters works without error
- Returned vulnerabilities are filtered by the specified metadata

---

### Test 15: Workflow - Use metadata with route coverage
**Purpose:** Verify metadata can be used with get_route_coverage.

**Prompt:**
```
use contrast mcp to:
1. Get session metadata for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf
2. Then get route coverage for that application filtered by one of the metadata values
```

**Expected Result:**
- get_session_metadata returns artifactHash values
- get_route_coverage with session metadata filter succeeds
- Routes are filtered by the specified metadata value

---

## Response Structure Tests

### Test 16: Verify response schema structure
**Purpose:** Verify response conforms to expected structure.

**Prompt:**
```
use contrast mcp to get session metadata for application 48985fed-84e2-45b2-93e0-450e67cf5217
```

**Expected Result:**
- Response has `data`, `errors`, `warnings`, `found`, `success` fields
- `data.filters` is an array
- Each filter object has `label`, `id`, `values` fields
- Each value object has `value` (string) and `count` (number) fields
- Example structure:
```json
{
  "data": {
    "filters": [
      {
        "label": "artifactHash",
        "id": "270",
        "values": [
          {"count": 10, "value": "2d3467fa"},
          {"count": 10, "value": "40bf340a"}
        ]
      }
    ]
  },
  "found": true,
  "success": true
}
```

---

### Test 17: Verify value counts are accurate
**Purpose:** Verify that count values reflect actual session counts.

**Prompt:**
```
use contrast mcp to get session metadata for application 25d24972-d71b-41ec-a41b-22e13564a8b9
```

**Expected Result:**
- Response contains artifactHash with 2 values
- Each value has a `count` property > 0
- Counts represent number of sessions with that metadata value
- Values: "303d65c3" (count ~13), "db4ebd74" (count ~11)

---

## Cross-Application Consistency Tests

### Test 18: Compare metadata across related applications (same deployment)
**Purpose:** Verify related applications in same deployment may share metadata patterns.

**Prompt:**
```
use contrast mcp to get session metadata for these two applications:
1. 03f49f62-efd2-4f7b-9402-8f5f399b0d36 (thib-dataservice)
2. 03c0a8d2-a6e6-46aa-b602-f242811d10bf (thib-frontgateservice)
```

**Expected Result:**
- Both apps return artifactHash metadata
- Both have the same metadata field structure
- Values may differ between applications (different artifact hashes)
- Consistent response format

---

### Test 19: Compare applications with and without metadata
**Purpose:** Verify clear distinction between apps with metadata vs empty metadata.

**Prompt:**
```
use contrast mcp to compare session metadata between:
1. 8ba749b2-e110-4f9d-b5f6-726a60fbd86f (has metadata)
2. 7949c260-6ae9-477f-970a-60d8f95a6f3c (no metadata)
```

**Expected Result:**
- First app: filters array with artifactHash values
- Second app: empty filters array
- Both return `found: true` and `success: true`
- No errors for either

---

## Repeated Calls / Consistency Tests

### Test 20: Repeated calls return consistent results
**Purpose:** Verify consistent results across multiple calls.

**Prompt:**
```
use contrast mcp to get session metadata for application 03f49f62-efd2-4f7b-9402-8f5f399b0d36 three times and compare results
```

**Expected Result:**
- All 3 calls return identical data
- Same filters, same values, same counts
- Consistent performance (similar response times)
- No variation unless new sessions were created

---

## Different Language Applications Tests

### Test 21: Java application metadata
**Purpose:** Verify metadata retrieval for Java application.

**Prompt:**
```
use contrast mcp to get session metadata for application 03f49f62-efd2-4f7b-9402-8f5f399b0d36
```

**Expected Result:**
- Success for Java app (thib-contrast-cargo-cats-dataservice)
- Returns artifactHash metadata

---

### Test 22: Verify metadata for application identified by search
**Purpose:** End-to-end workflow using application name search.

**Prompt:**
```
use contrast mcp to:
1. Search for the application named "Chris-Cargo-Cats-SmartFix-Demo-contrast-cargo-cats-dataservice"
2. Get the session metadata for the found application
```

**Expected Result:**
- search_applications finds app ID: 8ba749b2-e110-4f9d-b5f6-726a60fbd86f
- get_session_metadata returns artifactHash with value "b7247b03"

---

## Performance Tests

### Test 23: Response time for app with metadata
**Purpose:** Verify acceptable response time.

**Prompt:**
```
use contrast mcp to get session metadata for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and note the response time
```

**Expected Result:**
- Response time < 3 seconds for normal case
- Successful response with metadata

---

### Test 24: Response time for app with empty metadata
**Purpose:** Verify response time is similar for apps without metadata.

**Prompt:**
```
use contrast mcp to get session metadata for application 7949c260-6ae9-477f-970a-60d8f95a6f3c and note the response time
```

**Expected Result:**
- Response time < 3 seconds
- Empty filters returned quickly (no extra processing)

---

## Error Handling Tests

### Test 25: Graceful handling of API errors
**Purpose:** Verify graceful error handling (simulated by invalid credentials or network issues).

**Prompt:**
```
(This test requires misconfigured credentials)
use contrast mcp to get session metadata for any application
```

**Expected Result:**
- Error response returned (not crash)
- Error message indicates connection/authentication issue
- Graceful failure

---

## Summary

| Test # | Category | Scenario | Expected Behavior |
|--------|----------|----------|-------------------|
| 1 | Basic | Single metadata value | Returns 1 artifactHash value |
| 2 | Basic | Multiple metadata values | Returns 3 artifactHash values |
| 3 | Basic | Alternate app with metadata | Consistent behavior |
| 4 | Edge Case | Empty filters (sessions exist) | Empty array, no error |
| 5 | Edge Case | Another empty filters app | Consistent empty handling |
| 6 | Edge Case | Older app empty filters | Works for older apps |
| 7 | Validation | Missing appId | Validation error |
| 8 | Validation | Empty string appId | Validation error |
| 9 | Validation | Invalid format appId | Error message |
| 10 | Validation | Nonexistent UUID | Error or null with warning |
| 11 | Validation | Special characters | Error handling |
| 12 | Validation | Whitespace-only | Validation error |
| 13 | Integration | Search then get metadata | Both calls succeed |
| 14 | Integration | Use with search_app_vulnerabilities | Filtering works |
| 15 | Integration | Use with get_route_coverage | Filtering works |
| 16 | Response | Schema structure | Correct JSON structure |
| 17 | Response | Value counts | Counts > 0, accurate |
| 18 | Consistency | Related apps comparison | Same structure, different values |
| 19 | Consistency | With vs without metadata | Clear distinction |
| 20 | Consistency | Repeated calls | Identical results |
| 21 | Language | Java app | Works correctly |
| 22 | Workflow | Name search + metadata | End-to-end success |
| 23 | Performance | App with metadata | < 3 seconds |
| 24 | Performance | App without metadata | < 3 seconds |
| 25 | Error | API/connection errors | Graceful failure |

---

## Test Data Reference

### Quick Reference - Apps with Metadata
```
03f49f62-efd2-4f7b-9402-8f5f399b0d36  # thib-dataservice (3 values)
03c0a8d2-a6e6-46aa-b602-f242811d10bf  # thib-frontgateservice (3 values)
25d24972-d71b-41ec-a41b-22e13564a8b9  # Harshaa-frontgateservice (2 values)
48985fed-84e2-45b2-93e0-450e67cf5217  # Harshaa-dataservice (2 values)
cac113a8-04a0-4c74-b289-20b90ea0672e  # Chris-SmartFix-frontgateservice (1 value)
8ba749b2-e110-4f9d-b5f6-726a60fbd86f  # Chris-SmartFix-dataservice (1 value)
```

### Quick Reference - Apps with Empty Metadata
```
7949c260-6ae9-477f-970a-60d8f95a6f3c  # spring-petclinic-live-example
a7f3c2ca-b9d5-4826-b50d-81b4159f806f  # employee-management-service
04a4b5e6-fe05-4eeb-b25c-6f211ac4ce07  # WebGoatInternDemo-Ian
f1716e65-cf6d-4955-a65a-eeb14ffdf1c1  # WebGoatDemo
```

### Invalid IDs for Testing
```
00000000-0000-0000-0000-000000000000  # Valid UUID format, doesn't exist
invalid-format-12345                   # Invalid format
!!invalid!!                            # Special characters
```

---

## Notes

1. **Metadata Types:** The test organization primarily uses `artifactHash` as session metadata. Other potential metadata fields like `branchName`, `buildNumber`, `version` depend on how applications are configured during deployment.

2. **Session Counts:** The `count` field in metadata values indicates how many sessions were recorded with that specific metadata value.

3. **Empty vs Null:** An empty `filters` array means the application has sessions but no custom metadata configured. This is different from `data: null` which would indicate no sessions exist at all.

4. **Integration Usage:** The primary use case for this tool is discovering available metadata fields that can be used to filter vulnerabilities or route coverage by session.

# Manual Test Plan: list_all_vulnerabilities Tool

## Overview
This test plan validates the pagination and filtering behavior of the `list_all_vulnerabilities` tool after refactoring to use the params pattern.

## Test Environment Setup
```bash
# Start the MCP server
java -jar target/mcp-contrast-0.0.15-SNAPSHOT.jar \
  --CONTRAST_HOST_NAME=<your-host> \
  --CONTRAST_API_KEY=<your-key> \
  --CONTRAST_SERVICE_KEY=<your-key> \
  --CONTRAST_USERNAME=<your-user> \
  --CONTRAST_ORG_ID=<your-org>
```

---

## Part 1: Pagination Parameters (Soft Failures)

### Test 1.1: Valid Pagination - Default Values
**Input:**
```json
{"page": null, "pageSize": null}
```
**Expected:**
- ✅ Page defaults to 1
- ✅ PageSize defaults to 50
- ✅ Results returned successfully
- ✅ No warning messages

**Validation:**
- Response has `"page": 1`
- Response has `"pageSize": 50`
- Response has `"items"` array with results (if data exists)

---

### Test 1.2: Valid Pagination - Custom Values
**Input:**
```json
{"page": 2, "pageSize": 25}
```
**Expected:**
- ✅ Page = 2
- ✅ PageSize = 25
- ✅ Results returned successfully
- ✅ No warning messages

**Validation:**
- Response has `"page": 2`
- Response has `"pageSize": 25`
- Correct offset applied (SDK receives offset=25, limit=25)

---

### Test 1.3: Invalid Page - Negative (Soft Failure)
**Input:**
```json
{"page": -5, "pageSize": 50}
```
**Expected:**
- ⚠️ Page clamped to 1
- ✅ Results returned (query executes)
- ⚠️ Warning message: "Invalid page number -5, using page 1"

**Validation:**
- Response has `"page": 1`
- Response has `"message"` containing "Invalid page number -5"
- Response has non-empty `"items"` array (if data exists)

---

### Test 1.4: Invalid Page - Zero (Soft Failure)
**Input:**
```json
{"page": 0, "pageSize": 50}
```
**Expected:**
- ⚠️ Page clamped to 1
- ✅ Results returned (query executes)
- ⚠️ Warning message: "Invalid page number 0, using page 1"

**Validation:**
- Response has `"page": 1`
- Response has `"message"` containing "Invalid page number 0"

---

### Test 1.5: Invalid PageSize - Negative (Soft Failure)
**Input:**
```json
{"page": 1, "pageSize": -10}
```
**Expected:**
- ⚠️ PageSize clamped to 50 (default)
- ✅ Results returned (query executes)
- ⚠️ Warning message: "Invalid pageSize -10, using default 50"

**Validation:**
- Response has `"pageSize": 50`
- Response has `"message"` containing "Invalid pageSize -10"

---

### Test 1.6: Invalid PageSize - Zero (Soft Failure)
**Input:**
```json
{"page": 1, "pageSize": 0}
```
**Expected:**
- ⚠️ PageSize clamped to 50 (default)
- ✅ Results returned (query executes)
- ⚠️ Warning message: "Invalid pageSize 0, using default 50"

**Validation:**
- Response has `"pageSize": 50`
- Response has `"message"` containing "Invalid pageSize 0"

---

### Test 1.7: Invalid PageSize - Exceeds Maximum (Soft Failure)
**Input:**
```json
{"page": 1, "pageSize": 200}
```
**Expected:**
- ⚠️ PageSize capped to 100 (maximum)
- ✅ Results returned (query executes)
- ⚠️ Warning message: "Requested pageSize 200 exceeds maximum 100, capped to 100"

**Validation:**
- Response has `"pageSize": 100`
- Response has `"message"` containing "exceeds maximum 100"

---

### Test 1.8: Multiple Pagination Errors (Soft Failures)
**Input:**
```json
{"page": -5, "pageSize": 200}
```
**Expected:**
- ⚠️ Page clamped to 1
- ⚠️ PageSize capped to 100
- ✅ Results returned (query executes)
- ⚠️ Warning messages for BOTH errors

**Validation:**
- Response has `"page": 1`, `"pageSize": 100`
- Response has `"message"` containing both "Invalid page number -5" AND "exceeds maximum 100"

---

## Part 2: Filter Parameters - Hard Failures

### Test 2.1: Invalid Severity - Single (Hard Failure)
**Input:**
```json
{"page": 1, "pageSize": 50, "severities": "SUPER_HIGH"}
```
**Expected:**
- ❌ Query does NOT execute
- ❌ Empty items array
- ❌ Error message: "Invalid severity 'SUPER_HIGH'. Valid: CRITICAL, HIGH, MEDIUM, LOW, NOTE. Example: 'CRITICAL,HIGH'"

**Validation:**
- Response has `"items": []`
- Response has `"totalItems": 0`
- Response has `"hasMorePages": false`
- Response has error message in `"message"`
- SDK getTracesInOrg() was NOT called

---

### Test 2.2: Invalid Severity - Mixed Valid/Invalid (Hard Failure)
**Input:**
```json
{"page": 1, "pageSize": 50, "severities": "CRITICAL,SUPER_HIGH,HIGH"}
```
**Expected:**
- ❌ Query does NOT execute (even though CRITICAL and HIGH are valid)
- ❌ Empty items array
- ❌ Error message: "Invalid severity 'SUPER_HIGH'..."

**Validation:**
- Response has `"items": []`
- Response has error message mentioning "SUPER_HIGH"

---

### Test 2.3: Invalid Status (Hard Failure)
**Input:**
```json
{"page": 1, "pageSize": 50, "statuses": "Reported,BadStatus"}
```
**Expected:**
- ❌ Query does NOT execute
- ❌ Empty items array
- ❌ Error message: "Invalid status 'BadStatus'. Valid: Reported, Suspicious, Confirmed, Remediated, Fixed. Example: 'Reported,Confirmed'"

**Validation:**
- Response has `"items": []`
- Response has error message listing valid statuses

---

### Test 2.4: Invalid Environment (Hard Failure)
**Input:**
```json
{"page": 1, "pageSize": 50, "environments": "PRODUCTION,STAGING"}
```
**Expected:**
- ❌ Query does NOT execute
- ❌ Empty items array
- ❌ Error message: "Invalid environment 'STAGING'. Valid: DEVELOPMENT, QA, PRODUCTION. Example: 'PRODUCTION,QA'"

**Validation:**
- Response has `"items": []`
- Response has error message mentioning "STAGING"

---

### Test 2.5: Unparseable Date - Invalid Format (Hard Failure)
**Input:**
```json
{"page": 1, "pageSize": 50, "lastSeenAfter": "Jan 15 2025"}
```
**Expected:**
- ❌ Query does NOT execute
- ❌ Empty items array
- ❌ Error message: "Invalid lastSeenAfter date 'Jan 15 2025'. Expected ISO format (YYYY-MM-DD) like '2025-01-15' or epoch timestamp like '1705276800000'."

**Validation:**
- Response has `"items": []`
- Response has error message with example formats

---

### Test 2.6: Unparseable Date - Garbage Input (Hard Failure)
**Input:**
```json
{"page": 1, "pageSize": 50, "lastSeenBefore": "not-a-date"}
```
**Expected:**
- ❌ Query does NOT execute
- ❌ Empty items array
- ❌ Error message about invalid date format

**Validation:**
- Response has `"items": []`
- Response has error message for lastSeenBefore

---

### Test 2.7: Date Range Contradiction (Hard Failure)
**Input:**
```json
{"page": 1, "pageSize": 50, "lastSeenAfter": "2025-12-31", "lastSeenBefore": "2025-01-01"}
```
**Expected:**
- ❌ Query does NOT execute
- ❌ Empty items array
- ❌ Error message: "Invalid date range: lastSeenAfter must be before lastSeenBefore. Example: lastSeenAfter='2025-01-01', lastSeenBefore='2025-12-31'"

**Validation:**
- Response has `"items": []`
- Response has error message about date range

---

### Test 2.8: Multiple Filter Errors (Hard Failure)
**Input:**
```json
{"page": 1, "pageSize": 50, "severities": "SUPER_HIGH", "statuses": "BadStatus", "environments": "STAGING"}
```
**Expected:**
- ❌ Query does NOT execute
- ❌ Empty items array
- ❌ Error messages for ALL three validation failures

**Validation:**
- Response has `"items": []`
- Response has `"message"` containing errors for severity, status, AND environment

---

## Part 3: Filter Parameters - Valid Cases

### Test 3.1: Valid Severity Filter
**Input:**
```json
{"page": 1, "pageSize": 50, "severities": "CRITICAL,HIGH"}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Results filtered by CRITICAL and HIGH severities
- ✅ No error messages

**Validation:**
- Response has `"items"` array
- SDK received filter with CRITICAL and HIGH severities
- All returned items have severity in [CRITICAL, HIGH]

---

### Test 3.2: Valid Status Filter - Explicit
**Input:**
```json
{"page": 1, "pageSize": 50, "statuses": "Reported,Confirmed"}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Results filtered by specified statuses
- ✅ NO smart defaults warning (explicit values provided)

**Validation:**
- Response has `"items"` array
- NO message about "excluding Fixed and Remediated"
- All returned items have status in [Reported, Confirmed]

---

### Test 3.3: Status Filter - Smart Defaults
**Input:**
```json
{"page": 1, "pageSize": 50}
```
(No statuses parameter provided)

**Expected:**
- ✅ Query executes successfully
- ⚠️ Warning message: "Showing actionable vulnerabilities only (excluding Fixed and Remediated). To see all statuses, specify statuses parameter explicitly."
- ✅ Results filtered to [Reported, Suspicious, Confirmed]

**Validation:**
- Response has `"message"` containing "excluding Fixed and Remediated"
- SDK received status filter with [Reported, Suspicious, Confirmed]

---

### Test 3.4: Valid Date Filter - ISO Format
**Input:**
```json
{"page": 1, "pageSize": 50, "lastSeenAfter": "2025-01-01", "lastSeenBefore": "2025-12-31"}
```
**Expected:**
- ✅ Query executes successfully
- ⚠️ Warning message: "Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date."
- ✅ Results filtered by date range

**Validation:**
- Response has `"items"` array
- Response has `"message"` containing "LAST ACTIVITY DATE"
- SDK received startDate and endDate

---

### Test 3.5: Valid Date Filter - Epoch Timestamp
**Input:**
```json
{"page": 1, "pageSize": 50, "lastSeenAfter": "1704067200000"}
```
(Epoch timestamp for 2024-01-01)

**Expected:**
- ✅ Query executes successfully
- ⚠️ Warning message about LAST ACTIVITY DATE
- ✅ Results filtered by date

**Validation:**
- Response has `"items"` array
- Response has warning message
- SDK received valid Date object

---

### Test 3.6: Valid Environment Filter
**Input:**
```json
{"page": 1, "pageSize": 50, "environments": "PRODUCTION,QA"}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Results filtered by PRODUCTION and QA environments
- ✅ No error messages

**Validation:**
- Response has `"items"` array
- SDK received environment filter with PRODUCTION and QA

---

### Test 3.7: Valid Vulnerability Types Filter
**Input:**
```json
{"page": 1, "pageSize": 50, "vulnTypes": "sql-injection,xss-reflected"}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Results filtered by specified vulnerability types
- ✅ No validation errors (types are not validated, passed through)

**Validation:**
- Response has `"items"` array
- SDK received vulnTypes filter with specified values

---

### Test 3.8: Valid Vulnerability Tags Filter
**Input:**
```json
{"page": 1, "pageSize": 50, "vulnTags": "SmartFix Remediated,reviewed"}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Results filtered by specified tags (case-sensitive)
- ✅ No validation errors

**Validation:**
- Response has `"items"` array
- SDK received filterTags with exact case preserved

---

### Test 3.9: Valid Application ID Filter
**Input:**
```json
{"page": 1, "pageSize": 50, "appId": "your-app-id"}
```
**Expected:**
- ✅ Query executes successfully
- ✅ App-specific API endpoint used (not org-level)
- ✅ Results filtered to specified app

**Validation:**
- Response has `"items"` array
- SDK.getTraces(orgId, appId, filterForm) was called (not getTracesInOrg)

---

### Test 3.10: Combined Valid Filters
**Input:**
```json
{
  "page": 1,
  "pageSize": 50,
  "severities": "CRITICAL,HIGH",
  "statuses": "Reported,Confirmed",
  "environments": "PRODUCTION",
  "vulnTypes": "sql-injection,xss-reflected",
  "lastSeenAfter": "2025-01-01"
}
```
**Expected:**
- ✅ Query executes successfully
- ⚠️ Warning messages: smart defaults NOT used (explicit statuses), time filter note
- ✅ All filters applied correctly

**Validation:**
- Response has `"items"` array
- Response has `"message"` about time filters (but NOT about smart defaults)
- All filters passed to SDK

---

## Part 4: Edge Cases

### Test 4.1: Whitespace Handling in Comma-Separated Lists
**Input:**
```json
{"page": 1, "pageSize": 50, "severities": "CRITICAL , HIGH , "}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Whitespace trimmed, empty values filtered out
- ✅ Equivalent to "CRITICAL,HIGH"

**Validation:**
- Response has `"items"` array
- SDK received clean list: [CRITICAL, HIGH]

---

### Test 4.2: Empty Filter Values
**Input:**
```json
{"page": 1, "pageSize": 50, "severities": ""}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Empty string treated as no filter
- ✅ No validation errors

**Validation:**
- Response has `"items"` array
- SDK did NOT receive severity filter

---

### Test 4.3: Case Sensitivity - Severities (Case-Insensitive)
**Input:**
```json
{"page": 1, "pageSize": 50, "severities": "critical,HIGH"}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Case normalized to uppercase
- ✅ Equivalent to "CRITICAL,HIGH"

**Validation:**
- Response has `"items"` array
- SDK received [CRITICAL, HIGH]

---

### Test 4.4: Case Sensitivity - Vulnerability Tags (Case-Sensitive)
**Input:**
```json
{"page": 1, "pageSize": 50, "vulnTags": "SmartFix Remediated,smartfix remediated"}
```
**Expected:**
- ✅ Query executes successfully
- ✅ Case preserved (treated as two different tags)
- ✅ Results match exact case

**Validation:**
- Response has `"items"` array
- SDK received both tags with case preserved

---

### Test 4.5: Soft + Hard Failure Combination
**Input:**
```json
{"page": -5, "pageSize": 200, "severities": "SUPER_HIGH"}
```
**Expected:**
- ❌ Query does NOT execute (hard failure takes precedence)
- ❌ Empty items array
- ❌ Error message about invalid severity

**Validation:**
- Response has `"items": []`
- Response has error message about severity
- Pagination warnings are NOT included (hard failure stopped execution)

---

## Part 5: Response Format Validation

### Test 5.1: Successful Response Structure
**For any successful query:**
```json
{
  "items": [...],          // Array of VulnLight objects (may be empty)
  "page": 1,               // 1-based page number
  "pageSize": 50,          // Actual page size used
  "totalItems": 100,       // Total count (or null if unavailable)
  "hasMorePages": true,    // Boolean indicating more pages exist
  "message": "..."         // Optional warning/info messages
}
```

**Validation:**
- `items` is always an array (never null)
- `page` is always ≥ 1
- `pageSize` is always 1-100
- `totalItems` is integer or null
- `hasMorePages` is boolean
- `message` is string or null

---

### Test 5.2: Hard Failure Response Structure
**For any hard failure:**
```json
{
  "items": [],             // Always empty array
  "page": 1,               // Validated page value
  "pageSize": 50,          // Validated pageSize value
  "totalItems": 0,         // Always 0
  "hasMorePages": false,   // Always false
  "message": "Error: ..."  // Error description
}
```

**Validation:**
- Items is empty array (not null)
- totalItems is 0
- hasMorePages is false
- message contains descriptive error

---

## Summary Checklist

### Soft Failures (Execute Query, Return Warnings)
- [ ] Invalid page (< 1) → clamped to 1
- [ ] Invalid pageSize (< 1) → clamped to 50
- [ ] Invalid pageSize (> 100) → capped to 100

### Hard Failures (Stop Execution, Return Error)
- [ ] Invalid severity value
- [ ] Invalid status value
- [ ] Invalid environment value
- [ ] Unparseable date (lastSeenAfter/lastSeenBefore)
- [ ] Date range contradiction (startDate > endDate)

### Valid Filters
- [ ] Severities (case-insensitive, enum validated)
- [ ] Statuses (case-sensitive, enum validated)
- [ ] Environments (case-insensitive, enum validated)
- [ ] VulnTypes (case-insensitive, NOT validated)
- [ ] VulnTags (case-sensitive, NOT validated)
- [ ] Dates (ISO format or epoch timestamp)
- [ ] AppId (pass-through)

### Warning Messages
- [ ] Smart defaults applied (status filter)
- [ ] Time filters apply to lastTimeSeen
- [ ] Pagination clamping warnings

---

## Pass Criteria
- All soft failures return results with warnings
- All hard failures return empty results with errors
- All valid filters execute successfully
- Response format matches specification
- Warning messages are descriptive and actionable

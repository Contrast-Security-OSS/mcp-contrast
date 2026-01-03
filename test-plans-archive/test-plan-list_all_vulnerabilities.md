# Test Plan: list_all_vulnerabilities Tool

## Overview
This test plan provides comprehensive coverage for the `list_all_vulnerabilities` tool in the AssessService class. This is the most complex tool in the MCP server with extensive filtering and pagination capabilities.

**Tool Location**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java` (lines 428-588)

**Test Execution Context**: These tests should be executed by an AI agent using the MCP server against a live Contrast Security instance with appropriate test data.

---

## Test Categories

### 1. Pagination Tests

#### Test 1.1: First Page with Default Page Size
**Description**: Request the first page without specifying pagination parameters.

**Input**:
```
page: null (or omitted)
pageSize: null (or omitted)
```

**Expected Behavior**:
- Returns page 1 with pageSize 50 (default values)
- Response includes: `page: 1`, `pageSize: 50`
- `items` array contains up to 50 vulnerabilities
- `hasMorePages: true` if more than 50 vulnerabilities exist, `false` otherwise
- `totalItems` may be null or contain total count

**Test Data Assumptions**:
- Assume vulnerabilities exist in the organization

---

#### Test 1.2: Subsequent Pages
**Description**: Request pages 2, 3, etc. to verify pagination works correctly.

**Input**:
```
page: 2
pageSize: 50
```

**Expected Behavior**:
- Returns page 2 with 50 items per page
- Items should be different from page 1
- `hasMorePages` indicates if more pages exist
- Consistent ordering across pages

**Test Data Assumptions**:
- Assume at least 100 vulnerabilities exist (to have page 2)

---

#### Test 1.3: Custom Page Size
**Description**: Test different page sizes (small, medium, large).

**Input**:
```
Test cases:
- page: 1, pageSize: 10
- page: 1, pageSize: 25
- page: 1, pageSize: 100
```

**Expected Behavior**:
- Each returns the specified number of items (or fewer if insufficient data)
- Response reflects the correct `pageSize` value
- `hasMorePages` calculated correctly based on page size

**Test Data Assumptions**:
- Assume sufficient vulnerabilities to populate different page sizes

---

#### Test 1.4: Maximum Page Size Boundary
**Description**: Test page size at and beyond the maximum limit (100).

**Input**:
```
Test cases:
- pageSize: 100 (valid max)
- pageSize: 101 (exceeds max)
- pageSize: 500 (far exceeds max)
```

**Expected Behavior**:
- pageSize: 100 → Returns up to 100 items
- pageSize: 101+ → Clamped to 100, warning in `message` field: "Requested pageSize {N} exceeds maximum 100, capped to 100"
- Response shows `pageSize: 100`

**Test Data Assumptions**:
- Assume at least 100 vulnerabilities exist

---

#### Test 1.5: Invalid Page Numbers
**Description**: Test zero, negative, and boundary page numbers.

**Input**:
```
Test cases:
- page: 0
- page: -1
- page: -100
```

**Expected Behavior**:
- All invalid page numbers clamped to 1
- Warning in `message` field: "Invalid page number {N}, using page 1"
- Returns first page of results

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 1.6: Invalid Page Size
**Description**: Test zero, negative page sizes.

**Input**:
```
Test cases:
- pageSize: 0
- pageSize: -1
- pageSize: -50
```

**Expected Behavior**:
- All invalid page sizes default to 50
- Warning in `message` field: "Invalid pageSize {N}, using default 50"
- Returns 50 items per page

**Test Data Assumptions**:
- Assume at least 50 vulnerabilities exist

---

#### Test 1.7: Page Beyond Available Data
**Description**: Request a page number that exceeds available data.

**Input**:
```
page: 999
pageSize: 50
```

**Expected Behavior**:
- Returns empty `items` array
- `hasMorePages: false`
- `totalItems` shows actual count (if available)
- No error, just empty results

**Test Data Assumptions**:
- Assume fewer than 49,900 vulnerabilities (999 * 50)

---

### 2. Severity Filtering Tests

#### Test 2.1: Single Severity Filter
**Description**: Filter by each severity level individually.

**Input**:
```
Test cases:
- severities: "CRITICAL"
- severities: "HIGH"
- severities: "MEDIUM"
- severities: "LOW"
- severities: "NOTE"
```

**Expected Behavior**:
- Returns only vulnerabilities matching the specified severity
- All items in response have matching `severity` field
- No warnings or errors

**Test Data Assumptions**:
- Assume vulnerabilities exist at each severity level

---

#### Test 2.2: Multiple Severities (Comma-Separated)
**Description**: Filter by multiple severity levels.

**Input**:
```
Test cases:
- severities: "CRITICAL,HIGH"
- severities: "MEDIUM,LOW,NOTE"
- severities: "CRITICAL,MEDIUM"
```

**Expected Behavior**:
- Returns vulnerabilities matching any of the specified severities
- Each item's `severity` field matches one of the requested severities
- No warnings or errors

**Test Data Assumptions**:
- Assume vulnerabilities exist at the specified severity levels

---

#### Test 2.3: Invalid Severity Values
**Description**: Test invalid or misspelled severity values.

**Input**:
```
Test cases:
- severities: "INVALID"
- severities: "critical" (lowercase)
- severities: "URGENT" (non-existent)
- severities: "CRITICAL,INVALID,HIGH"
```

**Expected Behavior**:
- HARD FAILURE: Returns error response
- Empty `items` array
- Error in `message` field: "Invalid severity '{value}'. Valid: CRITICAL, HIGH, MEDIUM, LOW, NOTE. Example: 'CRITICAL,HIGH'"
- No data returned (execution stops)

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 2.4: No Severity Filter (All Severities)
**Description**: Omit severity filter to get all severity levels.

**Input**:
```
severities: null (or omitted)
```

**Expected Behavior**:
- Returns vulnerabilities of all severity levels
- Response includes mix of CRITICAL, HIGH, MEDIUM, LOW, NOTE
- No warnings about severity

**Test Data Assumptions**:
- Assume vulnerabilities exist across multiple severity levels

---

#### Test 2.5: Case Sensitivity
**Description**: Test that severity values are case-insensitive (converted to uppercase).

**Input**:
```
Test cases:
- severities: "critical"
- severities: "High"
- severities: "MeDiUm"
```

**Expected Behavior**:
- According to FilterHelper.parseCommaSeparatedUpperCase(), values are converted to uppercase
- Should work correctly or return appropriate error
- Verify behavior matches implementation

**Test Data Assumptions**:
- Assume CRITICAL, HIGH, MEDIUM vulnerabilities exist

---

### 3. Status Filtering Tests

#### Test 3.1: Single Status Filter
**Description**: Filter by each status individually.

**Input**:
```
Test cases:
- statuses: "Reported"
- statuses: "Suspicious"
- statuses: "Confirmed"
- statuses: "Remediated"
- statuses: "Fixed"
```

**Expected Behavior**:
- Returns only vulnerabilities matching the specified status
- All items have matching `status` field
- No "smart defaults" warning (explicit status provided)

**Test Data Assumptions**:
- Assume vulnerabilities exist in each status

---

#### Test 3.2: Multiple Statuses (Comma-Separated)
**Description**: Filter by multiple statuses.

**Input**:
```
Test cases:
- statuses: "Reported,Confirmed"
- statuses: "Suspicious,Confirmed"
- statuses: "Remediated,Fixed"
- statuses: "Reported,Suspicious,Confirmed"
```

**Expected Behavior**:
- Returns vulnerabilities matching any of the specified statuses
- Each item's `status` matches one of the requested values
- No warnings

**Test Data Assumptions**:
- Assume vulnerabilities exist in the specified statuses

---

#### Test 3.3: Default Behavior (No Status Filter)
**Description**: Test smart defaults when status filter is omitted.

**Input**:
```
statuses: null (or omitted)
```

**Expected Behavior**:
- Uses smart defaults: Reported, Suspicious, Confirmed
- Excludes Fixed and Remediated (focus on actionable items)
- Warning in `message` field: "Showing actionable vulnerabilities only (excluding Fixed and Remediated). To see all statuses, specify statuses parameter explicitly."
- Response includes only Reported, Suspicious, Confirmed vulnerabilities

**Test Data Assumptions**:
- Assume vulnerabilities exist in Reported, Suspicious, Confirmed statuses
- Assume Fixed and Remediated vulnerabilities exist (to verify exclusion)

---

#### Test 3.4: Invalid Status Values
**Description**: Test invalid or misspelled status values.

**Input**:
```
Test cases:
- statuses: "Open" (invalid)
- statuses: "Closed" (invalid)
- statuses: "reported" (wrong case)
- statuses: "Reported,Invalid,Confirmed"
```

**Expected Behavior**:
- HARD FAILURE: Returns error response
- Empty `items` array
- Error in `message` field: "Invalid status '{value}'. Valid: Reported, Suspicious, Confirmed, Remediated, Fixed. Example: 'Reported,Confirmed'"
- No data returned

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 3.5: Include Fixed/Remediated Explicitly
**Description**: Verify that Fixed and Remediated can be included when explicitly requested.

**Input**:
```
Test cases:
- statuses: "Remediated"
- statuses: "Fixed"
- statuses: "Reported,Suspicious,Confirmed,Remediated,Fixed"
```

**Expected Behavior**:
- Returns vulnerabilities in specified statuses including Fixed/Remediated
- No "smart defaults" warning (explicit filter provided)
- Response includes Fixed/Remediated vulnerabilities

**Test Data Assumptions**:
- Assume Fixed and Remediated vulnerabilities exist

---

### 4. Application ID Filtering Tests

#### Test 4.1: Valid Application ID
**Description**: Filter vulnerabilities to a specific application.

**Input**:
```
appId: "<valid-app-id>"
```

**Expected Behavior**:
- Uses app-specific API endpoint (more efficient)
- Returns only vulnerabilities for the specified application
- All items have matching application context (in sessionMetadata)
- Log message indicates: "Using app-specific API for appId: {appId}"

**Test Data Assumptions**:
- Assume a valid application ID exists with vulnerabilities
- Note the application ID before testing

---

#### Test 4.2: Invalid Application ID
**Description**: Test with a non-existent or invalid application ID.

**Input**:
```
Test cases:
- appId: "invalid-app-id-12345"
- appId: "00000000-0000-0000-0000-000000000000"
```

**Expected Behavior**:
- Returns empty results or error from SDK
- Empty `items` array
- Message may indicate no vulnerabilities found or invalid app ID

**Test Data Assumptions**:
- Use an application ID that definitely doesn't exist

---

#### Test 4.3: Empty Results for Application
**Description**: Test with a valid application that has no vulnerabilities.

**Input**:
```
appId: "<valid-app-id-no-vulns>"
```

**Expected Behavior**:
- Returns empty `items` array
- `totalItems: 0`
- `hasMorePages: false`
- No errors, just empty results

**Test Data Assumptions**:
- Assume a valid application exists with zero vulnerabilities

---

#### Test 4.4: No Application Filter (Org-Level Query)
**Description**: Omit appId to query all applications.

**Input**:
```
appId: null (or omitted)
```

**Expected Behavior**:
- Uses organization-level API endpoint
- Returns vulnerabilities from all applications in the organization
- May trigger fallback to app-by-app approach if org API fails
- Log message indicates: "Using org-level API" or "using fallback approach"

**Test Data Assumptions**:
- Assume multiple applications exist with vulnerabilities

---

### 5. Vulnerability Type Filtering Tests

#### Test 5.1: Single Vulnerability Type
**Description**: Filter by specific vulnerability types.

**Input**:
```
Test cases:
- vulnTypes: "sql-injection"
- vulnTypes: "xss-reflected"
- vulnTypes: "xss-stored"
- vulnTypes: "path-traversal"
- vulnTypes: "cmd-injection"
```

**Expected Behavior**:
- Returns only vulnerabilities of the specified type
- All items have matching `type` field
- No validation errors (vulnerability types are passed through to SDK)

**Test Data Assumptions**:
- Assume vulnerabilities exist for the specified types
- Use vulnerability types that exist in your test environment

---

#### Test 5.2: Multiple Vulnerability Types
**Description**: Filter by multiple types.

**Input**:
```
Test cases:
- vulnTypes: "sql-injection,xss-reflected"
- vulnTypes: "crypto-bad-mac,crypto-bad-ciphers"
- vulnTypes: "xss-reflected,xss-stored,path-traversal"
```

**Expected Behavior**:
- Returns vulnerabilities matching any of the specified types
- Each item's `type` matches one of the requested types
- No errors

**Test Data Assumptions**:
- Assume vulnerabilities exist for the specified types

---

#### Test 5.3: Invalid Vulnerability Type
**Description**: Test with non-existent vulnerability type.

**Input**:
```
Test cases:
- vulnTypes: "non-existent-type"
- vulnTypes: "invalid-injection"
```

**Expected Behavior**:
- No validation error (types passed through to SDK)
- Returns empty results (SDK finds no matching vulnerabilities)
- Empty `items` array

**Test Data Assumptions**:
- Use vulnerability type names that don't exist

---

#### Test 5.4: No Vulnerability Type Filter
**Description**: Omit vulnerability type filter to get all types.

**Input**:
```
vulnTypes: null (or omitted)
```

**Expected Behavior**:
- Returns vulnerabilities of all types
- Response includes various vulnerability types
- No warnings

**Test Data Assumptions**:
- Assume vulnerabilities exist across multiple types

---

#### Test 5.5: Case Sensitivity
**Description**: Test case handling for vulnerability types.

**Input**:
```
Test cases:
- vulnTypes: "SQL-INJECTION" (uppercase)
- vulnTypes: "Sql-Injection" (mixed case)
```

**Expected Behavior**:
- According to FilterHelper.parseCommaSeparatedLowerCase(), values converted to lowercase
- Should work correctly (converted to "sql-injection")
- Returns matching vulnerabilities

**Test Data Assumptions**:
- Assume sql-injection vulnerabilities exist

---

### 6. Environment Filtering Tests

#### Test 6.1: Single Environment Filter
**Description**: Filter by each environment individually.

**Input**:
```
Test cases:
- environments: "DEVELOPMENT"
- environments: "QA"
- environments: "PRODUCTION"
```

**Expected Behavior**:
- Returns only vulnerabilities seen in the specified environment
- Items' `environments` array includes the requested environment
- Note: `environments` field shows ALL environments where vulnerability has been seen historically

**Test Data Assumptions**:
- Assume vulnerabilities exist in each environment
- Understand that a vulnerability may appear in multiple environments

---

#### Test 6.2: Multiple Environments
**Description**: Filter by multiple environments.

**Input**:
```
Test cases:
- environments: "PRODUCTION,QA"
- environments: "DEVELOPMENT,QA,PRODUCTION"
- environments: "QA,PRODUCTION"
```

**Expected Behavior**:
- Returns vulnerabilities seen in any of the specified environments
- Each item's `environments` includes at least one requested environment
- May include vulnerabilities seen in multiple environments

**Test Data Assumptions**:
- Assume vulnerabilities exist across multiple environments

---

#### Test 6.3: Invalid Environment Values
**Description**: Test invalid or misspelled environment values.

**Input**:
```
Test cases:
- environments: "PROD" (abbreviation)
- environments: "STAGING" (not valid)
- environments: "production" (lowercase)
- environments: "PRODUCTION,INVALID"
```

**Expected Behavior**:
- HARD FAILURE: Returns error response
- Empty `items` array
- Error in `message` field: "Invalid environment '{value}'. Valid: DEVELOPMENT, QA, PRODUCTION. Example: 'PRODUCTION,QA'"
- No data returned

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 6.4: No Environment Filter
**Description**: Omit environment filter to get all environments.

**Input**:
```
environments: null (or omitted)
```

**Expected Behavior**:
- Returns vulnerabilities from all environments
- Response includes vulnerabilities from DEVELOPMENT, QA, PRODUCTION
- No warnings

**Test Data Assumptions**:
- Assume vulnerabilities exist across multiple environments

---

### 7. Date Filtering Tests

#### Test 7.1: Valid ISO Date Format (lastSeenAfter)
**Description**: Filter vulnerabilities active after a specific date.

**Input**:
```
Test cases:
- lastSeenAfter: "2025-01-01"
- lastSeenAfter: "2025-10-01"
- lastSeenAfter: "2024-01-01"
```

**Expected Behavior**:
- Returns only vulnerabilities with `lastSeenAt` after the specified date
- Warning in `message`: "Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date."
- All items have `lastSeenAt` >= specified date

**Test Data Assumptions**:
- Assume vulnerabilities exist with lastSeenAt dates before and after the filter date
- Know the date range of your test data

---

#### Test 7.2: Valid ISO Date Format (lastSeenBefore)
**Description**: Filter vulnerabilities active before a specific date.

**Input**:
```
Test cases:
- lastSeenBefore: "2025-12-31"
- lastSeenBefore: "2025-10-01"
- lastSeenBefore: "2024-12-31"
```

**Expected Behavior**:
- Returns only vulnerabilities with `lastSeenAt` before the specified date
- Warning in `message`: "Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date."
- All items have `lastSeenAt` <= specified date

**Test Data Assumptions**:
- Assume vulnerabilities exist with lastSeenAt dates before and after the filter date

---

#### Test 7.3: Epoch Timestamp Format
**Description**: Test date filtering with epoch timestamps.

**Input**:
```
Test cases:
- lastSeenAfter: "1704067200000" (2025-01-01 in milliseconds)
- lastSeenBefore: "1735689599000" (2024-12-31 in milliseconds)
```

**Expected Behavior**:
- Accepts epoch timestamps (milliseconds since epoch)
- Filters correctly based on the timestamp
- Warning about last activity date

**Test Data Assumptions**:
- Assume vulnerabilities exist around the specified timestamp dates

---

#### Test 7.4: Date Range (Both After and Before)
**Description**: Filter vulnerabilities within a date range.

**Input**:
```
lastSeenAfter: "2025-01-01"
lastSeenBefore: "2025-12-31"
```

**Expected Behavior**:
- Returns vulnerabilities with `lastSeenAt` between the two dates
- Warning in `message`: "Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date."
- All items fall within the specified range

**Test Data Assumptions**:
- Assume vulnerabilities exist with lastSeenAt dates within the specified range

---

#### Test 7.5: Invalid Date Formats
**Description**: Test various invalid date formats.

**Input**:
```
Test cases:
- lastSeenAfter: "2025/01/01" (wrong separator)
- lastSeenAfter: "01-01-2025" (wrong order)
- lastSeenAfter: "invalid-date"
- lastSeenAfter: "2025-13-01" (invalid month)
```

**Expected Behavior**:
- HARD FAILURE: Returns error response
- Empty `items` array
- Error in `message` field with parsing failure details
- No data returned

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 7.6: Invalid Date Range (After > Before)
**Description**: Test when lastSeenAfter is later than lastSeenBefore.

**Input**:
```
lastSeenAfter: "2025-12-31"
lastSeenBefore: "2025-01-01"
```

**Expected Behavior**:
- HARD FAILURE: Returns error response
- Error in `message`: "Invalid date range: lastSeenAfter must be before lastSeenBefore. Example: lastSeenAfter='2025-01-01', lastSeenBefore='2025-12-31'"
- Empty `items` array

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 7.7: Edge Case - Same Date for After and Before
**Description**: Test when lastSeenAfter equals lastSeenBefore.

**Input**:
```
lastSeenAfter: "2025-10-15"
lastSeenBefore: "2025-10-15"
```

**Expected Behavior**:
- Valid query (not an error)
- Returns vulnerabilities with `lastSeenAt` exactly on 2025-10-15
- May return empty results if no vulnerabilities seen on that specific date

**Test Data Assumptions**:
- Assume vulnerabilities may or may not exist on the exact date

---

### 8. Tag Filtering Tests

#### Test 8.1: Single Tag Filter
**Description**: Filter by vulnerability tag.

**Input**:
```
Test cases:
- vulnTags: "reviewed"
- vulnTags: "urgent"
- vulnTags: "false-positive"
```

**Expected Behavior**:
- Returns only vulnerabilities with the specified tag
- Tags are case-sensitive
- URL encoding handled automatically for tags with spaces

**Test Data Assumptions**:
- Assume vulnerabilities exist with the specified tags
- Create test vulnerabilities with known tags

---

#### Test 8.2: Special Tag - "SmartFix Remediated"
**Description**: Test the special SmartFix Remediated tag (contains space).

**Input**:
```
vulnTags: "SmartFix Remediated"
```

**Expected Behavior**:
- Returns vulnerabilities that were remediated using SmartFix
- Space in tag name is URL-encoded automatically (internal workaround for AIML-193)
- Commonly combined with statuses="Remediated"

**Test Data Assumptions**:
- Assume SmartFix-remediated vulnerabilities exist with this tag

---

#### Test 8.3: Multiple Tags (Comma-Separated)
**Description**: Filter by multiple tags.

**Input**:
```
Test cases:
- vulnTags: "reviewed,urgent"
- vulnTags: "SmartFix Remediated,reviewed"
```

**Expected Behavior**:
- Returns vulnerabilities matching any of the specified tags
- Each tag is URL-encoded if it contains spaces
- Case-sensitive matching

**Test Data Assumptions**:
- Assume vulnerabilities exist with the specified tags

---

#### Test 8.4: Tag with Special Characters
**Description**: Test tags with various special characters.

**Input**:
```
Test cases:
- vulnTags: "tag-with-dash"
- vulnTags: "tag_with_underscore"
- vulnTags: "tag with multiple spaces"
```

**Expected Behavior**:
- URL encoding handles special characters correctly
- Returns matching vulnerabilities
- No errors due to special characters

**Test Data Assumptions**:
- Create test vulnerabilities with tags containing special characters

---

#### Test 8.5: No Tag Filter
**Description**: Omit tag filter to get all tags.

**Input**:
```
vulnTags: null (or omitted)
```

**Expected Behavior**:
- Returns vulnerabilities regardless of tags
- Includes both tagged and untagged vulnerabilities
- No warnings

**Test Data Assumptions**:
- Assume vulnerabilities exist with and without tags

---

#### Test 8.6: Non-Existent Tag
**Description**: Test with a tag that doesn't exist on any vulnerability.

**Input**:
```
vulnTags: "nonexistent-tag-xyz"
```

**Expected Behavior**:
- Returns empty results
- Empty `items` array
- `totalItems: 0`
- No errors (just no matches)

**Test Data Assumptions**:
- Use a tag name that definitely doesn't exist

---

#### Test 8.7: Case Sensitivity Verification
**Description**: Verify that tags are case-sensitive.

**Input**:
```
Test cases (assume tag "Reviewed" exists):
- vulnTags: "Reviewed" (matches)
- vulnTags: "reviewed" (doesn't match)
- vulnTags: "REVIEWED" (doesn't match)
```

**Expected Behavior**:
- Only exact case match returns results
- Different cases return empty results or different results
- Demonstrates case-sensitive behavior

**Test Data Assumptions**:
- Create a vulnerability with tag "Reviewed" (capital R)

---

### 9. Combined Filter Tests

#### Test 9.1: Production Critical Issues
**Description**: Combine severity and environment filters.

**Input**:
```
severities: "CRITICAL"
environments: "PRODUCTION"
```

**Expected Behavior**:
- Returns only CRITICAL vulnerabilities in PRODUCTION
- All items have `severity: "CRITICAL"` and `environments` includes "PRODUCTION"
- Demonstrates multiple filters working together

**Test Data Assumptions**:
- Assume CRITICAL vulnerabilities exist in PRODUCTION environment

---

#### Test 9.2: High-Priority Open Issues with Recent Activity
**Description**: Combine severity, status, and date filters.

**Input**:
```
severities: "CRITICAL,HIGH"
statuses: "Reported,Confirmed"
lastSeenAfter: "2025-09-01"
```

**Expected Behavior**:
- Returns CRITICAL or HIGH severity vulnerabilities
- Status is Reported or Confirmed
- Last seen after September 1, 2025
- Warning about status defaults NOT shown (explicit status provided)
- Warning about time filter on last activity date

**Test Data Assumptions**:
- Assume CRITICAL/HIGH vulnerabilities exist in Reported/Confirmed status with recent activity

---

#### Test 9.3: Specific App's SQL Injection Issues
**Description**: Combine application ID and vulnerability type filters.

**Input**:
```
appId: "<valid-app-id>"
vulnTypes: "sql-injection"
```

**Expected Behavior**:
- Uses app-specific API endpoint
- Returns only sql-injection vulnerabilities
- All items belong to the specified application
- Efficient query (focused on one app)

**Test Data Assumptions**:
- Assume the application has sql-injection vulnerabilities

---

#### Test 9.4: SmartFix Remediated Vulnerabilities
**Description**: Combine tag filter with status filter.

**Input**:
```
vulnTags: "SmartFix Remediated"
statuses: "Remediated"
```

**Expected Behavior**:
- Returns vulnerabilities remediated by SmartFix
- All items have status "Remediated" and tag "SmartFix Remediated"
- No smart defaults warning (explicit status provided)

**Test Data Assumptions**:
- Assume SmartFix-remediated vulnerabilities exist

---

#### Test 9.5: Kitchen Sink (All Filters)
**Description**: Test with all filter parameters specified.

**Input**:
```
page: 2
pageSize: 25
severities: "CRITICAL,HIGH"
statuses: "Reported,Confirmed"
appId: "<valid-app-id>"
vulnTypes: "sql-injection,xss-reflected"
environments: "PRODUCTION"
lastSeenAfter: "2025-01-01"
lastSeenBefore: "2025-12-31"
vulnTags: "reviewed"
```

**Expected Behavior**:
- All filters applied simultaneously
- Returns page 2 with 25 items per page
- Results match all specified criteria
- Multiple warnings possible (time filter warning)
- Demonstrates filter composition

**Test Data Assumptions**:
- Assume vulnerabilities exist matching all criteria (may be challenging to find)
- May return empty results if criteria too restrictive

---

#### Test 9.6: Conflicting Filters (Empty Results)
**Description**: Test filter combinations that should logically return no results.

**Input**:
```
Test cases:
- severities: "CRITICAL", vulnTypes: "non-existent-type"
- appId: "<valid-app-id>", vulnTypes: "sql-injection" (app has no SQL injection)
- lastSeenAfter: "2025-12-01", lastSeenBefore: "2025-12-31", environments: "PRODUCTION" (no production vulns in date range)
```

**Expected Behavior**:
- Returns empty `items` array
- `totalItems: 0`
- `hasMorePages: false`
- Message may indicate no results found
- No errors (valid query, just no matching data)

**Test Data Assumptions**:
- Intentionally use filter combinations that won't match any vulnerabilities

---

### 10. Empty Results Tests

#### Test 10.1: No Vulnerabilities in Organization
**Description**: Test against an organization with no vulnerabilities.

**Input**:
```
(No filters, or any filters)
```

**Expected Behavior**:
- Returns empty `items` array
- `totalItems: 0`
- `hasMorePages: false`
- Message may indicate organization has no vulnerabilities or no access

**Test Data Assumptions**:
- Use an organization with zero vulnerabilities
- Or test immediately after all vulnerabilities are remediated

---

#### Test 10.2: All Vulnerabilities Filtered Out
**Description**: Test filters that exclude all vulnerabilities.

**Input**:
```
Test cases:
- severities: "CRITICAL" (when no CRITICAL vulns exist)
- lastSeenAfter: "2099-01-01" (date in future)
- environments: "PRODUCTION" (when no production vulns exist)
```

**Expected Behavior**:
- Returns empty `items` array
- `totalItems: 0`
- `hasMorePages: false`
- No errors, just no matches

**Test Data Assumptions**:
- Use filters that intentionally don't match any existing vulnerabilities

---

#### Test 10.3: Smart Defaults Exclude All (No Actionable Vulns)
**Description**: Test when smart defaults filter out all vulnerabilities.

**Input**:
```
statuses: null (omitted, uses smart defaults)
```

**Expected Behavior**:
- Applies smart defaults: Reported, Suspicious, Confirmed
- If all vulnerabilities are Fixed or Remediated, returns empty results
- Warning: "Showing actionable vulnerabilities only (excluding Fixed and Remediated)..."
- Empty `items` array

**Test Data Assumptions**:
- Assume all vulnerabilities are in Fixed or Remediated status (no actionable ones)

---

### 11. API Fallback Tests

#### Test 11.1: Org-Level API Success
**Description**: Verify organization-level API is used successfully.

**Input**:
```
appId: null (omitted)
(Any other filters)
```

**Expected Behavior**:
- Uses organization-level API: `contrastSDK.getTracesInOrg()`
- Returns vulnerabilities from all applications
- Log message: Does NOT show "using fallback approach"
- Efficient single API call

**Test Data Assumptions**:
- Assume organization-level API is working properly
- Requires appropriate EAC (Enterprise Access Control) permissions

---

#### Test 11.2: Fallback to App-by-App
**Description**: Test fallback when organization-level API returns null.

**Input**:
```
appId: null (omitted)
(Test in environment where org-level API fails or returns null)
```

**Expected Behavior**:
- Organization-level API returns null
- Automatically falls back to app-by-app approach
- Fetches vulnerabilities for each application individually
- Warning in log: "Organization-level API returned no results, using fallback approach"
- Returns combined results from all applications
- Pagination is in-memory (on combined results)

**Test Data Assumptions**:
- Requires environment where org-level API doesn't work
- May occur with certain permission configurations

---

#### Test 11.3: App-Specific API (No Fallback)
**Description**: When appId is provided, app-specific API is used directly.

**Input**:
```
appId: "<valid-app-id>"
```

**Expected Behavior**:
- Uses app-specific API: `contrastSDK.getTraces(orgID, appId, filterForm)`
- No fallback mechanism triggered
- Log message: "Using app-specific API for appId: {appId}"
- More efficient than org-level or fallback

**Test Data Assumptions**:
- Use a valid application ID

---

#### Test 11.4: Partial Failure in Fallback
**Description**: Test when some applications fail during fallback approach.

**Input**:
```
appId: null (omitted)
(Trigger fallback scenario)
```

**Expected Behavior**:
- Fallback approach iterates through applications
- If one application fails, logs warning and continues with others
- Warning: "Failed to get vulnerabilities for application {name}: {error}"
- Returns vulnerabilities from successful applications
- Does not abort entire operation due to one failure

**Test Data Assumptions**:
- Requires scenario where one or more applications have access issues
- May need specific permission setup

---

### 12. Response Validation Tests

#### Test 12.1: Response Structure Validation
**Description**: Verify all response fields are present and correctly typed.

**Input**:
```
(Any valid query)
```

**Expected Behavior**:
- Response contains all required fields:
  - `items`: array of VulnLight objects
  - `page`: integer >= 1
  - `pageSize`: integer (1-100)
  - `totalItems`: integer or null
  - `hasMorePages`: boolean
  - `message`: string or null
- All fields have correct types

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 12.2: VulnLight Object Structure
**Description**: Verify each vulnerability item has correct structure.

**Input**:
```
(Any query that returns vulnerabilities)
```

**Expected Behavior**:
- Each item in `items` array contains:
  - `title`: string
  - `type`: string (vulnerability type)
  - `vulnID`: string (unique identifier)
  - `severity`: string (CRITICAL, HIGH, MEDIUM, LOW, NOTE)
  - `sessionMetadata`: array (application context)
  - `lastSeenAt`: string (ISO timestamp)
  - `status`: string (Reported, Suspicious, Confirmed, Remediated, Fixed)
  - `firstSeenAt`: string (ISO timestamp)
  - `closedAt`: string or null
  - `environments`: array of strings (DEVELOPMENT, QA, PRODUCTION)

**Test Data Assumptions**:
- Assume vulnerabilities exist to validate structure

---

#### Test 12.3: totalItems Accuracy
**Description**: Verify totalItems matches actual count when available.

**Input**:
```
page: 1
pageSize: 10
(Filters that return known count)
```

**Expected Behavior**:
- If `totalItems` is provided (not null), it represents total across all pages
- Pagination calculations should be consistent
- `hasMorePages` should be true if `totalItems > (page * pageSize)`
- Note: `totalItems` may be null in fallback mode

**Test Data Assumptions**:
- Assume a dataset with known total count
- Test with different page sizes to verify calculations

---

#### Test 12.4: hasMorePages Accuracy
**Description**: Verify hasMorePages flag is correctly set.

**Input**:
```
Test cases:
- page: 1, pageSize: 10 (when 50 total items exist)
- page: 5, pageSize: 10 (when 50 total items exist)
- page: 6, pageSize: 10 (when 50 total items exist)
```

**Expected Behavior**:
- page 1, pageSize 10, 50 total: `hasMorePages: true`
- page 5, pageSize 10, 50 total: `hasMorePages: false` (last page)
- page 6, pageSize 10, 50 total: `hasMorePages: false` (beyond last page)

**Test Data Assumptions**:
- Assume a dataset with known total count (e.g., 50 vulnerabilities)

---

#### Test 12.5: Message Field Warnings
**Description**: Verify warning messages appear correctly in message field.

**Input**:
```
Test cases:
- Default status filter (should show smart defaults warning)
- Date filters (should show last activity warning)
- Invalid page/pageSize (should show clamping warnings)
- Multiple warnings (combine above)
```

**Expected Behavior**:
- `message` field contains warning text when applicable
- Multiple warnings are combined (space or newline separated)
- Warnings are informative and actionable
- Examples:
  - "Showing actionable vulnerabilities only (excluding Fixed and Remediated)..."
  - "Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date."
  - "Requested pageSize 150 exceeds maximum 100, capped to 100"

**Test Data Assumptions**:
- Any test data that triggers warnings

---

#### Test 12.6: Error Message Format
**Description**: Verify error messages for validation failures.

**Input**:
```
Test cases:
- Invalid severity: "INVALID"
- Invalid status: "Closed"
- Invalid environment: "STAGING"
- Invalid date: "invalid-date"
- Invalid date range: after > before
```

**Expected Behavior**:
- `items`: empty array
- `totalItems: 0`
- `hasMorePages: false`
- `message`: contains descriptive error with:
  - What's invalid
  - Valid options
  - Example of correct format

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 12.7: Empty Items Array Never Null
**Description**: Verify items array is always present, never null.

**Input**:
```
(Any query, especially those returning no results)
```

**Expected Behavior**:
- `items` field is always an array
- Empty results: `items: []` (not null)
- Never causes null pointer exceptions

**Test Data Assumptions**:
- Test with queries that return no results

---

#### Test 12.8: Environments Field - Historical Data
**Description**: Verify environments field shows historical environment presence.

**Input**:
```
(Query vulnerabilities)
```

**Expected Behavior**:
- `environments` array shows ALL environments where vulnerability has been seen
- A vulnerability may have been seen in DEVELOPMENT, then moved to PRODUCTION
- `environments` would show: ["DEVELOPMENT", "PRODUCTION"]
- This is historical, not just current location

**Test Data Assumptions**:
- Assume vulnerabilities that have moved across environments over time
- Understand this is different from "current environment"

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server is running and connected to a valid Contrast Security instance
2. Ensure test environment has:
   - Multiple applications with vulnerabilities
   - Vulnerabilities across different severities, statuses, types
   - Vulnerabilities in different environments (DEVELOPMENT, QA, PRODUCTION)
   - Vulnerabilities with various tags
   - Vulnerabilities with different lastSeenAt dates
   - Mix of open, remediated, and fixed vulnerabilities

### During Testing
1. Record all request parameters for each test
2. Capture complete response including all fields
3. Verify response structure matches expected format
4. Check log files for debugging information
5. Note any unexpected behavior or edge cases

### Test Data Recommendations
For comprehensive testing, the test environment should ideally have:
- At least 150 vulnerabilities (for pagination testing)
- Vulnerabilities at all severity levels (CRITICAL, HIGH, MEDIUM, LOW, NOTE)
- Vulnerabilities in all statuses (Reported, Suspicious, Confirmed, Remediated, Fixed)
- At least 5-10 different vulnerability types (sql-injection, xss, etc.)
- Vulnerabilities across all environments (DEVELOPMENT, QA, PRODUCTION)
- Vulnerabilities with lastSeenAt dates spanning several months
- Vulnerabilities with various tags, including "SmartFix Remediated"
- Multiple applications (at least 3-5) for org-level and fallback testing

### Success Criteria
Each test passes when:
1. Response structure matches expected format
2. Data filtering works correctly
3. Pagination calculations are accurate
4. Warning and error messages are appropriate
5. No unexpected exceptions or errors occur
6. Performance is acceptable (log timing information)

### Known Limitations and Workarounds
1. **Tag URL Encoding**: Tags with spaces (e.g., "SmartFix Remediated") are URL-encoded internally as workaround for AIML-193
2. **totalItems Availability**: May be null in fallback mode (app-by-app approach)
3. **EAC Permissions**: Organization-level API requires appropriate Enterprise Access Control permissions
4. **Historical Environments**: `environments` field shows historical data, not current location only

---

## Appendix: Example Test Execution Commands

These examples show how an AI agent might invoke the tool during testing:

```json
// Test 1.1: First page with defaults
{
  "page": null,
  "pageSize": null
}

// Test 2.2: Multiple severities
{
  "severities": "CRITICAL,HIGH"
}

// Test 3.3: Default status behavior
{
  "statuses": null
}

// Test 9.2: Combined filters
{
  "severities": "CRITICAL,HIGH",
  "statuses": "Reported,Confirmed",
  "lastSeenAfter": "2025-09-01"
}

// Test 12.5: Multiple warnings
{
  "page": 0,
  "pageSize": 150,
  "lastSeenAfter": "2025-01-01"
}
```

---

## Test Coverage Summary

This test plan covers:
- ✓ 7 pagination test cases
- ✓ 5 severity filtering test cases
- ✓ 5 status filtering test cases
- ✓ 4 application ID filtering test cases
- ✓ 5 vulnerability type filtering test cases
- ✓ 4 environment filtering test cases
- ✓ 7 date filtering test cases
- ✓ 7 tag filtering test cases
- ✓ 6 combined filter test cases
- ✓ 3 empty results test cases
- ✓ 4 API fallback test cases
- ✓ 8 response validation test cases

**Total: 65 test cases**

Each test case is designed to be executed by an AI agent using the MCP server, with clear input parameters, expected behaviors, and test data assumptions.

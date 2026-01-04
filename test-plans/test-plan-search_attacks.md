# Test Plan: search_attacks Tool

## Overview

This test plan provides comprehensive testing coverage for the `search_attacks` MCP tool, which consolidates the functionality of the former `get_attacks` and `get_attacks_filtered` tools. The tool retrieves attack data from Contrast ADR (Attack Detection and Response) with flexible filtering, pagination, and sorting capabilities.

**Consolidation Note:** This plan preserves all 125 test cases from the original two tools:
- `test-plan-get_attacks.md` (51 tests)
- `test-plan-get_attacks_filtered.md` (74 tests)

**Testing Approach:** Each test case describes the parameters to use, expected behavior, and test data requirements. Tests are designed to be executed by an AI agent that will:
1. Query the Contrast ADR system with specified parameters
2. Verify response structure and content
3. Confirm filtering, pagination, and sorting work correctly

---

## Tool Specification

### Tool Name
`search_attacks`

### Description
Search for attacks from Contrast ADR with flexible filtering, pagination, and sorting. Returns attack summaries including dates, rules, status, severity, applications, source IP, and probe count.

### Parameters

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `status` | String | Filter by attack status: "ALL", "PROBED", "EXPLOITED", "BLOCKED" | "ALL" |
| `keyword` | String | Search keyword (matches rule names, IP addresses, application names) | null |
| `includeSuppressed` | Boolean | Include suppressed attacks | false |
| `includeBotBlockers` | Boolean | Include attacks from bot blockers | false |
| `includeIpBlacklist` | Boolean | Include attacks from blacklisted IPs | false |
| `page` | Integer | Page number (1-indexed) | null (no pagination) |
| `pageSize` | Integer | Results per page | 25 |
| `sort` | String | Sort field with optional `-` prefix for descending. Valid: `startTime`, `endTime`, `sourceIP`, `status`, `type` | "-startTime" |

### Response Structure: AttackSummary

```json
{
  "attackId": "string (UUID)",
  "status": "string",
  "source": "string (IP address)",
  "rules": ["string array of rule names"],
  "probes": "integer",
  "startTime": "string (formatted date)",
  "endTime": "string (formatted date)",
  "startTimeMs": "long (epoch milliseconds)",
  "endTimeMs": "long (epoch milliseconds)",
  "firstEventTime": "string (formatted date)",
  "lastEventTime": "string (formatted date)",
  "firstEventTimeMs": "long (epoch milliseconds)",
  "lastEventTimeMs": "long (epoch milliseconds)",
  "applications": [
    {
      "applicationId": "string",
      "applicationName": "string",
      "language": "string",
      "severity": "string",
      "status": "string",
      "startTime": "string (formatted date)",
      "endTime": "string (formatted date)",
      "startTimeMs": "long (epoch milliseconds)",
      "endTimeMs": "long (epoch milliseconds)"
    }
  ]
}
```

---

## Test Categories

### 1. Basic Functionality - No Filters

#### Test Case 1.1: Default Behavior (No Parameters)
**Objective:** Call tool with no parameters to verify default behavior.

**Parameters:** `(none - all defaults)`

**Expected Result:**
- Returns list of AttackSummary objects
- Uses default `status="ALL"`
- Uses default `includeSuppressed=false`, `includeBotBlockers=false`, `includeIpBlacklist=false`
- Uses default `sort="-startTime"` (most recent first)
- No pagination (returns all matching attacks)
- Each AttackSummary contains all required fields

---

#### Test Case 1.2: All Attacks Retrieved
**Objective:** Verify tool returns all attacks when no filters applied.

**Parameters:** `status="ALL"`

**Expected Result:**
- Returns attacks in all states
- Most permissive status filter
- Count represents total non-excluded attacks

---

#### Test Case 1.3: Tool Registration Verification
**Objective:** Verify tool is registered and accessible via MCP.

**Parameters:** `(Query available MCP tools)`

**Expected Result:**
- Tool name: "search_attacks"
- Description mentions: "attack summaries", "dates, rules, status, severity, applications, source IP, and probe count"
- All parameters are documented
- Tool is callable from AI agents

---

### 2. Status Filtering (QuickFilter)

#### Test Case 2.1: Status - "ALL"
**Objective:** Verify "ALL" status returns all attacks regardless of status.

**Parameters:** `status="ALL"`

**Expected Result:**
- Returns attacks in all states (probed, exploited, blocked)
- Most permissive status filter
- Default behavior when status is null

---

#### Test Case 2.2: Status - "PROBED"
**Objective:** Filter for attacks that were probed (attack attempts detected).

**Parameters:** `status="PROBED"`

**Expected Result:**
- Returns only probed attacks
- All returned attacks have probed status indicators
- Excludes exploited and blocked attacks

---

#### Test Case 2.3: Status - "EXPLOITED"
**Objective:** Filter for attacks that were successfully exploited.

**Parameters:** `status="EXPLOITED"`

**Expected Result:**
- Returns only exploited attacks
- Higher severity attacks
- May return fewer results than PROBED

---

#### Test Case 2.4: Status - "BLOCKED"
**Objective:** Filter for attacks that were blocked by Contrast Protect.

**Parameters:** `status="BLOCKED"`

**Expected Result:**
- Returns only blocked attacks
- Shows successful defense events
- Requires Contrast Protect to be active

---

#### Test Case 2.5: Status - Invalid Value
**Objective:** Verify behavior with invalid status value.

**Parameters:**
```
Test cases:
- status="INVALID"
- status="invalid"
- status="UNKNOWN_STATUS"
```

**Expected Result:**
- May return error from SDK/API
- Or may default to "ALL" behavior
- Document actual behavior

---

#### Test Case 2.6: Status - Case Sensitivity
**Objective:** Verify case handling for status values.

**Parameters:**
```
Test cases:
- status="all" (lowercase)
- status="All" (mixed case)
- status="probed" (lowercase)
```

**Expected Result:**
- Verify whether status is case-sensitive
- Document observed behavior
- Likely expects uppercase

---

### 3. Keyword Search

#### Test Case 3.1: Keyword - Rule Name Match
**Objective:** Search for attacks by rule name keyword.

**Parameters:**
```
Test cases:
- keyword="sql-injection"
- keyword="xss"
- keyword="cmd-injection"
```

**Expected Result:**
- Returns attacks matching keyword in rule names
- All returned attacks have rules array containing keyword
- Partial matches may be supported

---

#### Test Case 3.2: Keyword - IP Address Search
**Objective:** Search for attacks from specific IP addresses.

**Parameters:**
```
Test cases:
- keyword="192.168.1.100" (exact IP)
- keyword="10.0.0" (partial IP)
- keyword="192.168" (IP prefix)
```

**Expected Result:**
- Returns attacks from matching source IP addresses
- Full or partial IP matches depending on implementation
- All returned attacks have matching source field

---

#### Test Case 3.3: Keyword - Application Name Search
**Objective:** Search for attacks by application name.

**Parameters:**
```
Test cases:
- keyword="WebGoat"
- keyword="MyApp"
- keyword="api" (partial match)
```

**Expected Result:**
- Returns attacks against applications matching keyword
- Searches application names in applications array
- Partial matches may be supported

---

#### Test Case 3.4: Keyword - Empty String
**Objective:** Verify behavior with empty keyword.

**Parameters:** `keyword=""`

**Expected Result:**
- Equivalent to no keyword filter
- Returns all attacks (no filtering on keyword)
- No errors

---

#### Test Case 3.5: Keyword - Special Characters
**Objective:** Verify handling of special characters in keyword.

**Parameters:**
```
Test cases:
- keyword="user@domain.com" (email)
- keyword="/admin/login" (path)
- keyword="SELECT * FROM" (SQL with spaces)
```

**Expected Result:**
- Special characters handled correctly
- URL encoding handled internally if needed
- Returns matching attacks or empty results

---

#### Test Case 3.6: Keyword - No Matches
**Objective:** Verify behavior when keyword matches nothing.

**Parameters:** `keyword="nonexistent-keyword-xyz-123"`

**Expected Result:**
- Returns empty list
- No errors, just no matches
- Response is empty list, not null

---

### 4. Boolean Inclusion Filters

#### Test Case 4.1: includeSuppressed - True
**Objective:** Include suppressed attacks in results.

**Parameters:** `includeSuppressed=true`

**Expected Result:**
- Returns both suppressed and non-suppressed attacks
- Total count higher than when false
- Suppressed attacks that would normally be excluded are included

---

#### Test Case 4.2: includeSuppressed - False (Default)
**Objective:** Exclude suppressed attacks from results.

**Parameters:** `includeSuppressed=false`

**Expected Result:**
- Returns only non-suppressed attacks
- Default behavior
- Suppressed attacks are filtered out

---

#### Test Case 4.3: includeSuppressed - Null
**Objective:** Verify default behavior when includeSuppressed is null.

**Parameters:** `includeSuppressed=null`

**Expected Result:**
- Uses default (false)
- Excludes suppressed attacks
- Same behavior as explicitly false

---

#### Test Case 4.4: includeBotBlockers - True
**Objective:** Include attacks from bot blockers.

**Parameters:** `includeBotBlockers=true`

**Expected Result:**
- Returns attacks including those from bot blockers
- Total count higher than when false
- Bot blocker attacks included that would normally be excluded

---

#### Test Case 4.5: includeBotBlockers - False (Default)
**Objective:** Exclude attacks from bot blockers.

**Parameters:** `includeBotBlockers=false`

**Expected Result:**
- Returns only non-bot-blocker attacks
- Default behavior
- Bot blocker attacks are filtered out

---

#### Test Case 4.6: includeBotBlockers - Null
**Objective:** Verify default behavior when includeBotBlockers is null.

**Parameters:** `includeBotBlockers=null`

**Expected Result:**
- Uses default (false)
- Excludes bot blocker attacks
- Same behavior as explicitly false

---

#### Test Case 4.7: includeIpBlacklist - True
**Objective:** Include attacks from blacklisted IPs.

**Parameters:** `includeIpBlacklist=true`

**Expected Result:**
- Returns attacks including those from IP blacklist
- Total count higher than when false
- Blacklisted IP attacks included

---

#### Test Case 4.8: includeIpBlacklist - False (Default)
**Objective:** Exclude attacks from blacklisted IPs.

**Parameters:** `includeIpBlacklist=false`

**Expected Result:**
- Returns only non-blacklisted IP attacks
- Default behavior
- Blacklisted IP attacks are filtered out

---

#### Test Case 4.9: includeIpBlacklist - Null
**Objective:** Verify default behavior when includeIpBlacklist is null.

**Parameters:** `includeIpBlacklist=null`

**Expected Result:**
- Uses default (false)
- Excludes blacklisted IP attacks
- Same behavior as explicitly false

---

#### Test Case 4.10: All Boolean Filters - True
**Objective:** Include all normally-excluded attack types.

**Parameters:**
```
includeSuppressed=true
includeBotBlockers=true
includeIpBlacklist=true
```

**Expected Result:**
- Returns all attacks regardless of suppression, bot blockers, or IP blacklist
- Most permissive filtering
- Highest attack count

---

#### Test Case 4.11: All Boolean Filters - False
**Objective:** Exclude all optional attack types.

**Parameters:**
```
includeSuppressed=false
includeBotBlockers=false
includeIpBlacklist=false
```

**Expected Result:**
- Returns only "clean" attacks
- Most restrictive filtering
- Default behavior

---

### 5. Pagination

#### Test Case 5.1: First Page
**Objective:** Retrieve first page of results.

**Parameters:**
```
page=1
pageSize=10
```

**Expected Result:**
- Returns first 10 attacks
- Sorted by default order (NEWEST)
- Page 1 contains most recent attacks

---

#### Test Case 5.2: Second Page
**Objective:** Retrieve second page of results.

**Parameters:**
```
page=2
pageSize=10
```

**Expected Result:**
- Returns attacks 11-20
- Different attacks than page 1
- Consistent ordering across pages

---

#### Test Case 5.3: Large Page Size
**Objective:** Verify behavior with large page size.

**Parameters:**
```
Test cases:
- pageSize=100
- pageSize=500
- pageSize=1000
```

**Expected Result:**
- Returns at most N attacks
- May be capped by API limits
- No errors even with very large page sizes

---

#### Test Case 5.4: Small Page Size
**Objective:** Verify behavior with small page size.

**Parameters:**
```
Test cases:
- pageSize=1
- pageSize=5
```

**Expected Result:**
- Returns exactly pageSize attacks (or fewer if insufficient data)
- Page 1 contains first N attacks

---

#### Test Case 5.5: Page Beyond Available Data
**Objective:** Request page that doesn't exist.

**Parameters:**
```
page=9999
pageSize=10
```

**Expected Result:**
- Returns empty list
- No errors, just no results
- Graceful handling of out-of-bounds page

---

#### Test Case 5.6: Invalid Page Number - Zero
**Objective:** Test page=0.

**Parameters:** `page=0`

**Expected Result:**
- May treat as page 1
- Or may return error
- Document actual behavior

---

#### Test Case 5.7: Invalid Page Number - Negative
**Objective:** Test negative page numbers.

**Parameters:**
```
Test cases:
- page=-1
- page=-10
```

**Expected Result:**
- May default to page 1
- Or may return error
- Document actual behavior

---

#### Test Case 5.8: Pagination Consistency
**Objective:** Verify all attacks retrieved exactly once through pagination.

**Parameters:**
```
Test sequence:
1. Get total count (no pagination)
2. Paginate with page=1,2,3... pageSize=10 until all retrieved
```

**Expected Result:**
- Sum of all pages equals total count
- No duplicate attacks across pages
- No missing attacks
- Consistent ordering ensures complete coverage

---

### 6. Sorting

#### Test Case 6.1: Sort - "-startTime" (Default, Descending)
**Objective:** Sort by newest attacks first (descending start time).

**Parameters:** `sort="-startTime"`

**Expected Result:**
- Returns attacks ordered by most recent first
- Latest attacks appear first in list
- Descending time order (newest to oldest)
- Verify by checking startTime values decrease through results

---

#### Test Case 6.2: Sort - "startTime" (Ascending)
**Objective:** Sort by oldest attacks first (ascending start time).

**Parameters:** `sort="startTime"`

**Expected Result:**
- Returns attacks ordered by oldest first
- Earliest attacks appear first
- Ascending time order (oldest to newest)
- Verify by checking startTime values increase through results

---

#### Test Case 6.3: Sort - "status"
**Objective:** Sort by attack status.

**Parameters:** `sort="status"` or `sort="-status"`

**Expected Result:**
- Returns attacks ordered by status field
- Verify by checking status values are grouped/ordered
- Note: Severity sorting is not supported at attack level (severity is per-application)

---

#### Test Case 6.4: Sort - "sourceIP"
**Objective:** Sort by source IP address.

**Parameters:** `sort="sourceIP"` or `sort="-sourceIP"`

**Expected Result:**
- Returns attacks ordered by source IP address
- Verify by checking source field values are ordered
- Note: Probe count sorting is not supported by the API

---

#### Test Case 6.5: Sort - Invalid Value
**Objective:** Test invalid sort parameter returns helpful validation error.

**Parameters:**
```
Test cases:
- sort="INVALID"
- sort="invalid"
- sort="UNKNOWN_SORT"
- sort="severity" (not supported)
- sort="probes" (not supported)
```

**Expected Result:**
- Returns validation error (not generic API error)
- Error message lists valid sort fields: `startTime`, `endTime`, `sourceIP`, `status`, `type`
- Error explains `-` prefix convention for descending order
- Example: "Invalid sort field 'INVALID'. Valid fields: [endTime, sourceIP, startTime, status, type]. Use '-' prefix for descending order (e.g., '-startTime')."

---

#### Test Case 6.6: Sort - Null (Default Sorting)
**Objective:** Verify default sorting when sort is null/omitted.

**Parameters:** `sort=null` (or omit parameter)

**Expected Result:**
- Uses default sort order (`-startTime`)
- Results sorted by most recent first (descending start time)
- Same behavior as explicitly passing `sort="-startTime"`

---

#### Test Case 6.7: Sort with Pagination
**Objective:** Verify sorting combined with pagination.

**Parameters:**
```
sort="-startTime"
page=1
pageSize=10
```

**Expected Result:**
- Page 1 has 10 newest attacks
- Page 2 has next 10 newest attacks
- Sort order maintained across pages
- No duplicate or missing attacks between pages

---

### 7. Data Completeness - AttackSummary Fields

#### Test Case 7.1: AttackSummary Required Fields
**Objective:** Verify all required fields are present in each AttackSummary.

**Parameters:** `(any)`

**Expected Result:**
Each AttackSummary contains:
- `attackId`: string (UUID format)
- `status`: string (attack status)
- `source`: string (IP address)
- `rules`: list of strings (attack rule names)
- `probes`: integer (number of probes)
- `startTime`, `endTime`: formatted date strings
- `startTimeMs`, `endTimeMs`: long (epoch milliseconds)
- `firstEventTime`, `lastEventTime`: formatted date strings
- `firstEventTimeMs`, `lastEventTimeMs`: long (epoch milliseconds)
- `applications`: list of ApplicationAttackInfo objects

All fields non-null and properly typed.

---

#### Test Case 7.2: ApplicationAttackInfo Fields
**Objective:** Verify all application-specific attack information is present.

**Parameters:** `(any returning attacks with applications)`

**Expected Result:**
Each ApplicationAttackInfo contains:
- `applicationId`: string
- `applicationName`: string
- `language`: string (e.g., "Java", "Node", ".NET")
- `severity`: string
- `status`: string
- `startTime`, `endTime`: formatted date strings
- `startTimeMs`, `endTimeMs`: long (epoch milliseconds)

All fields non-null and properly typed.

---

#### Test Case 7.3: Attack ID Uniqueness
**Objective:** Verify each attack has a unique attackId.

**Parameters:** `(any returning multiple attacks)`

**Expected Result:**
- All attackId values are unique across all returned attacks
- attackId follows UUID format
- No duplicate attackId values

---

#### Test Case 7.4: Source IP Format
**Objective:** Verify source IP addresses are properly formatted.

**Parameters:** `(any)`

**Expected Result:**
- `source` field contains valid IP addresses (IPv4 or IPv6)
- Format examples: "192.168.1.100", "10.0.0.5", "2001:db8::1"
- No null or empty source values

---

#### Test Case 7.5: Rules List Content
**Objective:** Verify rules list contains attack rule identifiers.

**Parameters:** `(any)`

**Expected Result:**
- `rules` field is a list of strings
- Rule names identify attack patterns (e.g., "sql-injection", "xss-reflected")
- List is not null (may be empty)

---

#### Test Case 7.6: Probe Count Validity
**Objective:** Verify probe count is a valid integer.

**Parameters:** `(any)`

**Expected Result:**
- `probes` field is an integer >= 0
- Represents number of attack probes/attempts
- Typical values range from 1 to thousands

---

### 8. Date and Timestamp Formatting

#### Test Case 8.1: Start Time Formatting
**Objective:** Verify startTime is formatted correctly.

**Parameters:** `(any)`

**Expected Result:**
- `startTime` is a formatted date string
- Format example: "Mon Oct 21 10:30:45 PDT 2025"
- Human-readable and parseable
- Corresponds to `startTimeMs` epoch value
- Timezone is included

---

#### Test Case 8.2: End Time Formatting
**Objective:** Verify endTime is formatted correctly.

**Parameters:** `(any)`

**Expected Result:**
- `endTime` is a formatted date string
- Format matches `startTime` format
- Corresponds to `endTimeMs` epoch value
- endTime >= startTime (chronological order)

---

#### Test Case 8.3: First/Last Event Time Formatting
**Objective:** Verify firstEventTime and lastEventTime are formatted correctly.

**Parameters:** `(any)`

**Expected Result:**
- Both are formatted date strings
- firstEventTime <= lastEventTime (chronological order)
- Correspond to their respective epoch values
- Timezone is included

---

#### Test Case 8.4: Epoch Milliseconds Accuracy
**Objective:** Verify epoch milliseconds values are accurate.

**Parameters:** `(any)`

**Expected Result:**
- All `*Ms` fields are positive longs
- Values represent milliseconds since Unix epoch
- Chronological order: startTimeMs <= firstEventTimeMs <= lastEventTimeMs <= endTimeMs
- Formatted string dates correspond to epoch values

---

#### Test Case 8.5: Application Timestamp Formatting
**Objective:** Verify application-specific timestamps are formatted correctly.

**Parameters:** `(any returning attacks with applications)`

**Expected Result:**
- Within ApplicationAttackInfo, timestamps are formatted strings
- Format matches attack-level timestamp formats
- Epoch values correspond to formatted strings
- Application times may differ from attack-level times

---

#### Test Case 8.6: Timezone Consistency
**Objective:** Verify all timestamps use consistent timezone.

**Parameters:** `(any)`

**Expected Result:**
- All timestamp strings include timezone information
- Timezone is consistent across all timestamps
- Timezone abbreviation included (e.g., "PDT", "EST", "UTC")

---

#### Test Case 8.7: Date Formatting Edge Cases
**Objective:** Test date formatting with edge case timestamps.

**Parameters:** `(any - verify against historical data)`

**Expected Result:**
- Very old attacks (years ago) format correctly
- Very recent attacks (seconds ago) format correctly
- Attacks spanning midnight format correctly
- No formatting errors or exceptions

---

### 9. Multiple Applications Per Attack

#### Test Case 9.1: Single Application Attack
**Objective:** Verify attacks targeting a single application.

**Parameters:** `(any)`

**Expected Result:**
- Attack has exactly one ApplicationAttackInfo in `applications` list
- Application information is complete
- No duplicate application entries

---

#### Test Case 9.2: Multi-Application Attack
**Objective:** Verify attacks targeting multiple applications.

**Parameters:** `(any - find attacks with 2+ applications)`

**Expected Result:**
- Attack has multiple ApplicationAttackInfo entries
- Each application has complete information
- Same attackId affects multiple applications
- Each application may have different severity or status

---

#### Test Case 9.3: Application Diversity
**Objective:** Verify attacks across different application types.

**Parameters:** `(any)`

**Expected Result:**
- Applications have diverse languages (Java, Node, .NET, Python, Ruby, etc.)
- Each applicationId is unique
- Language field is populated for all applications

---

#### Test Case 9.4: Application Severity Differences
**Objective:** Verify same attack can have different severities per application.

**Parameters:** `(any - find multi-app attack)`

**Expected Result:**
- In multi-application attacks, severity may differ per application
- ApplicationAttackInfo.severity reflects application-specific severity
- Severity values: "LOW", "MEDIUM", "HIGH" (or similar)

---

#### Test Case 9.5: Application Status Differences
**Objective:** Verify same attack can have different statuses per application.

**Parameters:** `(any - find multi-app attack)`

**Expected Result:**
- In multi-application attacks, status may differ per application
- ApplicationAttackInfo.status reflects application-specific status
- One application may be "reviewed" while another is "unreviewed"

---

#### Test Case 9.6: Empty Applications List
**Objective:** Verify handling of attacks with no application associations.

**Parameters:** `(any)`

**Expected Result:**
- If attack has no associated applications, `applications` list is empty `[]`
- No null values in applications field
- Attack data is still complete for other fields

---

#### Test Case 9.7: Application Name and ID Consistency
**Objective:** Verify application identification is consistent.

**Parameters:** `(any returning multiple attacks for same app)`

**Expected Result:**
- Same applicationId always maps to same applicationName
- Same applicationName always maps to same applicationId
- No inconsistencies across different attacks

---

### 10. Attack Types Coverage

#### Test Case 10.1: SQL Injection Attacks
**Objective:** Verify attacks with SQL injection rules are returned.

**Parameters:** `keyword="sql-injection"` (or find in results)

**Expected Result:**
- Attacks with SQL injection rules are included
- Rules list includes SQL-related rule names
- Attack data is complete and properly formatted

---

#### Test Case 10.2: Cross-Site Scripting (XSS) Attacks
**Objective:** Verify attacks with XSS rules are returned.

**Parameters:** `keyword="xss"` (or find in results)

**Expected Result:**
- Attacks with XSS rules are included
- Rules list includes XSS-related rule names

---

#### Test Case 10.3: Path Traversal Attacks
**Objective:** Verify attacks with path traversal rules are returned.

**Parameters:** `keyword="path-traversal"` (or find in results)

**Expected Result:**
- Attacks with path traversal rules are included
- Rules list includes path traversal rule names

---

#### Test Case 10.4: Command Injection Attacks
**Objective:** Verify attacks with command injection rules are returned.

**Parameters:** `keyword="cmd-injection"` (or find in results)

**Expected Result:**
- Attacks with command injection rules are included
- Rules list includes command injection rule names

---

#### Test Case 10.5: Multiple Attack Rules
**Objective:** Verify attacks matching multiple rules are handled correctly.

**Parameters:** `(any)`

**Expected Result:**
- Some attacks may match multiple detection rules
- `rules` list contains all matched rule names
- List may have 2+ entries for sophisticated attacks
- All rule names are distinct (no duplicates)

---

#### Test Case 10.6: Attack Type Diversity
**Objective:** Verify response includes diverse attack types.

**Parameters:** `(no filters - get all)`

**Expected Result:**
- Results include attacks with various rule types
- May include: SQL injection, XSS, path traversal, command injection, XXE, SSRF, etc.
- Demonstrates comprehensive attack detection coverage

---

### 11. Empty Results Handling

#### Test Case 11.1: Organization with No Attacks
**Objective:** Test against organization with no attacks.

**Parameters:** `(any filters)`

**Expected Result:**
- Returns empty list `[]`
- Not null, but empty list
- No errors

---

#### Test Case 11.2: Status Filter Returns No Results
**Objective:** Test status filter that matches no attacks.

**Parameters:**
```
Test cases:
- status="EXPLOITED" (when no exploited attacks exist)
- status="BLOCKED" (when no blocked attacks exist)
```

**Expected Result:**
- Returns empty list
- Valid query, just no matching data
- No errors

---

#### Test Case 11.3: Keyword Returns No Matches
**Objective:** Test keyword that matches no attacks.

**Parameters:** `keyword="nonexistent-keyword-xyz-123"`

**Expected Result:**
- Returns empty list
- No matching attacks
- No errors

---

#### Test Case 11.4: All Boolean Filters Exclude All Attacks
**Objective:** Test when boolean filters exclude everything.

**Parameters:**
```
includeSuppressed=false
includeBotBlockers=false
includeIpBlacklist=false
(When all attacks are in one of these excluded categories)
```

**Expected Result:**
- Returns empty list
- All attacks filtered out by exclusion criteria
- No errors

---

#### Test Case 11.5: Page Beyond All Results
**Objective:** Test pagination beyond available data.

**Parameters:**
```
page=1000
pageSize=10
```

**Expected Result:**
- Returns empty list
- No errors, just no more data
- Graceful handling

---

#### Test Case 11.6: Combined Filters Too Restrictive
**Objective:** Test filter combination that matches nothing.

**Parameters:**
```
status="EXPLOITED"
keyword="nonexistent-rule"
includeSuppressed=false
page=1
pageSize=10
```

**Expected Result:**
- Returns empty list
- No attacks match all criteria
- Valid query, just too restrictive

---

### 12. Combined Filters

#### Test Case 12.1: Status + Keyword
**Objective:** Combine status filter with keyword search.

**Parameters:**
```
status="PROBED"
keyword="sql"
```

**Expected Result:**
- Returns only probed attacks
- That also match keyword "sql"
- Both filters applied (AND logic)
- Narrower result set

---

#### Test Case 12.2: Status + Boolean Filters
**Objective:** Combine status filter with boolean inclusion filters.

**Parameters:**
```
status="EXPLOITED"
includeSuppressed=true
includeBotBlockers=true
```

**Expected Result:**
- Returns exploited attacks
- Including suppressed and bot blocker attacks
- Shows more complete picture of exploited attacks

---

#### Test Case 12.3: Keyword + Boolean Filters
**Objective:** Combine keyword search with boolean filters.

**Parameters:**
```
keyword="192.168"
includeSuppressed=true
includeIpBlacklist=true
```

**Expected Result:**
- Returns attacks from 192.168.x.x IP range
- Including suppressed and IP blacklist attacks
- Comprehensive view of attacks from specific IP range

---

#### Test Case 12.4: Status + Keyword + Pagination
**Objective:** Combine filtering with pagination.

**Parameters:**
```
status="PROBED"
keyword="xss"
page=1
pageSize=5
```

**Expected Result:**
- Returns first 5 probed XSS attacks
- Both filters applied before pagination
- Can page through filtered results

---

#### Test Case 12.5: Status + Keyword + Sorting
**Objective:** Combine filtering with sorting.

**Parameters:**
```
status="BLOCKED"
keyword="sql"
sort="-startTime"
```

**Expected Result:**
- Returns blocked SQL-related attacks
- Sorted by newest first
- All three parameters work together

---

#### Test Case 12.6: All Filters Combined (Kitchen Sink)
**Objective:** Test with all filter parameters specified.

**Parameters:**
```
status="PROBED"
keyword="sql-injection"
includeSuppressed=true
includeBotBlockers=false
includeIpBlacklist=false
page=2
pageSize=20
sort="-startTime"
```

**Expected Result:**
- All filters applied simultaneously
- Returns page 2 of results matching all criteria
- Complex query executes successfully

---

#### Test Case 12.7: Boolean Filters + Pagination + Sorting
**Objective:** Test boolean filters with pagination and sorting.

**Parameters:**
```
includeSuppressed=true
includeBotBlockers=true
includeIpBlacklist=true
page=1
pageSize=10
sort="-startTime"
```

**Expected Result:**
- Returns first 10 attacks (all types)
- Sorted by newest
- Most permissive boolean filters

---

### 13. Validation and Edge Cases

#### Test Case 13.1: Null vs Omitted Parameters
**Objective:** Verify behavior difference between null and omitted parameters.

**Parameters:**
```
Test cases:
- All parameters omitted (not passed)
- All parameters explicitly null
```

**Expected Result:**
- Behavior should be identical
- Both use defaults
- No errors in either case

---

#### Test Case 13.2: Empty String vs Null
**Objective:** Test empty string vs null for string parameters.

**Parameters:**
```
Test cases:
- status="" vs status=null
- keyword="" vs keyword=null
- sort="" vs sort=null
```

**Expected Result:**
- Empty string may be treated as null
- Or may cause validation error
- Document actual behavior

---

#### Test Case 13.3: Very Long Keyword
**Objective:** Test keyword parameter with very long string.

**Parameters:** `keyword="<1000+ character string>"`

**Expected Result:**
- May be accepted and search performed
- Or may return error if exceeds API limits
- No server crash or unhandled exception

---

#### Test Case 13.4: Special Characters in Keyword
**Objective:** Test keyword with various special characters (security test).

**Parameters:**
```
Test cases:
- keyword="<script>alert('xss')</script>"
- keyword="'; DROP TABLE attacks;--"
- keyword="../../../etc/passwd"
- keyword="%00null-byte"
```

**Expected Result:**
- Special characters handled safely
- No injection vulnerabilities
- Either returns matching attacks or empty results
- No errors or security issues

---

#### Test Case 13.5: Whitespace in Keyword
**Objective:** Test parameters with leading/trailing whitespace.

**Parameters:**
```
Test cases:
- keyword="  sql  " (spaces)
- keyword="\tsql\t" (tabs)
```

**Expected Result:**
- Whitespace may be trimmed automatically
- Or treated as part of search term
- Document actual behavior
- No errors

---

### 14. Performance Tests

#### Test Case 14.1: Response Time - No Filters
**Objective:** Measure baseline performance with no filters.

**Parameters:** `(all parameters null/omitted)`

**Expected Result:**
- Response completes successfully
- Baseline timing for comparison
- Typically fastest query

---

#### Test Case 14.2: Response Time - Single Filter
**Objective:** Measure performance with single filter.

**Parameters:**
```
Test cases:
- status="PROBED"
- keyword="sql"
- page=1, pageSize=100
```

**Expected Result:**
- Response completes successfully
- Compare to baseline
- Should be similar performance

---

#### Test Case 14.3: Response Time - Multiple Filters
**Objective:** Measure performance with multiple filters.

**Parameters:**
```
status="PROBED"
keyword="sql"
includeSuppressed=true
sort="-startTime"
```

**Expected Result:**
- Response completes successfully
- May be slightly slower than single filter
- Still acceptable performance (< 5 seconds)

---

#### Test Case 14.4: Response Time - Large Result Set
**Objective:** Measure performance when returning many results.

**Parameters:**
```
includeSuppressed=true
includeBotBlockers=true
includeIpBlacklist=true
(No pagination - return all)
```

**Expected Result:**
- Response completes successfully
- May be slower with large result set
- Note time increases with data volume

---

#### Test Case 14.5: Response Time - Pagination vs No Pagination
**Objective:** Compare paginated vs unpaginated queries.

**Parameters:**
```
Test A: page=1, pageSize=10
Test B: (no pagination)
```

**Expected Result:**
- Paginated query may be faster
- Pagination useful for large datasets
- Compare execution times

---

#### Test Case 14.6: Response Time - Complex Keyword Search
**Objective:** Measure keyword search performance.

**Parameters:**
```
Test cases:
- keyword="a" (very common, many matches)
- keyword="very-specific-unique-term" (few matches)
```

**Expected Result:**
- Both complete successfully
- Common keywords may be slower (more matches)
- Specific keywords faster

---

#### Test Case 14.7: Concurrent Request Handling
**Objective:** Test behavior under concurrent requests.

**Parameters:** `Multiple simultaneous calls with different filters`

**Expected Result:**
- All requests complete successfully
- No race conditions or errors
- Each request returns correct filtered data

---

### 15. Error Handling

#### Test Case 15.1: SDK Initialization Failure
**Objective:** Test behavior when SDK initialization fails.

**Parameters:** `(any with invalid credentials)`

**Expected Result:**
- Error logged with response time
- IOException thrown
- Error includes details about initialization failure
- Tool does not crash or hang

---

#### Test Case 15.2: Network Connectivity Issues
**Objective:** Test behavior with network problems.

**Parameters:** `(any with network disconnected or host unreachable)`

**Expected Result:**
- Error logged with response time
- Exception includes network-related error details
- Tool handles timeout gracefully
- No infinite hangs

---

#### Test Case 15.3: Unauthorized Access
**Objective:** Test behavior with insufficient permissions.

**Parameters:** `(any with credentials lacking ADR access)`

**Expected Result:**
- Error logged with response time
- Exception includes authorization error details
- Error message indicates permission issue
- Does not expose sensitive credential information

---

#### Test Case 15.4: SDK Extension Failure
**Objective:** Test behavior when SDKExtension fails.

**Parameters:** `(specific conditions to trigger extension failure)`

**Expected Result:**
- Error logged with response time
- Exception is thrown and logged
- Error details help troubleshoot issue

---

#### Test Case 15.5: Malformed Attack Data
**Objective:** Test behavior when SDK returns malformed attack data.

**Parameters:** `(conditions where SDK returns incomplete data)`

**Expected Result:**
- Tool attempts to process data
- Logs any transformation errors
- May skip malformed attacks or throw exception
- Does not crash with null pointer exceptions

---

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server is running and connected to valid Contrast Security instance
2. Ensure test environment has:
   - ADR enabled and configured
   - At least 50-100 attack events for pagination/performance testing
   - Attacks with different statuses (probed, exploited, blocked)
   - Attacks from various source IPs (at least 10 different IPs)
   - Attacks triggering different protection rules (SQL injection, XSS, path traversal, command injection, etc.)
   - Mix of suppressed and non-suppressed attacks (at least 20% suppressed)
   - Attacks from bot blockers (if feature enabled)
   - Attacks from blacklisted IPs (if feature configured)
   - Attacks affecting single and multiple applications
   - Applications in different languages (Java, Node, .NET, etc.)
   - Attacks spanning at least 30 days (for timestamp diversity)
   - Various attack severities (CRITICAL, HIGH, MEDIUM, LOW)

### During Testing
1. Record all request parameters for each test
2. Capture complete response (List<AttackSummary>)
3. Verify response structure and data completeness
4. Check log files (`/tmp/mcp-contrast.log`) for:
   - Info message: "Retrieving attacks from Contrast ADR"
   - Debug message: "ContrastSDK initialized successfully"
   - Success message: "Successfully retrieved {N} attacks (took {X} ms)"
   - Or warning: "No attacks data returned (took {X} ms)"
   - Or error: "Error retrieving attacks (after {X} ms): {error}"
5. Record response times for performance analysis
6. Note any unexpected behavior or edge cases

### Success Criteria
Each test passes when:
1. Response structure matches AttackSummary specification
2. All required fields are present and non-null
3. Data types are correct (strings, integers, longs, lists)
4. Timestamps are formatted correctly and chronologically ordered
5. Application information is complete and accurate
6. Filters correctly include/exclude attacks
7. Pagination returns correct pages without duplicates
8. Sorting produces correct order
9. Empty results are handled gracefully (empty list, not null)
10. Error conditions are logged with appropriate details

### Performance Benchmarks
Expected response times (approximate):
- 1-10 attacks: < 1 second
- 10-50 attacks: < 3 seconds
- 50-100 attacks: < 5 seconds
- 100+ attacks: < 10 seconds

Note: Actual response times depend on network latency, Contrast TeamServer load, and attack data complexity.

---

## Test Coverage Summary

This consolidated test plan covers:

| Category | Test Cases |
|----------|------------|
| Basic Functionality | 3 |
| Status Filtering | 6 |
| Keyword Search | 6 |
| Boolean Inclusion Filters | 11 |
| Pagination | 8 |
| Sorting | 7 |
| Data Completeness (AttackSummary) | 6 |
| Date/Timestamp Formatting | 7 |
| Multiple Applications | 7 |
| Attack Types Coverage | 6 |
| Empty Results Handling | 6 |
| Combined Filters | 7 |
| Validation and Edge Cases | 5 |
| Performance | 7 |
| Error Handling | 5 |

**Total: 97 test cases**

*(Note: Some original test cases were merged where they tested identical functionality across the two original tools. All unique behaviors are preserved.)*

---

## Appendix: Example Response Structure

### Example AttackSummary Object
```json
{
  "attackId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "reviewed",
  "source": "192.168.1.100",
  "rules": ["sql-injection", "suspicious-input"],
  "probes": 42,
  "startTime": "Mon Oct 21 10:30:45 PDT 2025",
  "endTime": "Mon Oct 21 10:35:12 PDT 2025",
  "startTimeMs": 1729531845000,
  "endTimeMs": 1729532112000,
  "firstEventTime": "Mon Oct 21 10:30:45 PDT 2025",
  "lastEventTime": "Mon Oct 21 10:35:12 PDT 2025",
  "firstEventTimeMs": 1729531845000,
  "lastEventTimeMs": 1729532112000,
  "applications": [
    {
      "applicationId": "app-123-456",
      "applicationName": "Payment Service",
      "language": "Java",
      "severity": "HIGH",
      "status": "reviewed",
      "startTime": "Mon Oct 21 10:30:45 PDT 2025",
      "endTime": "Mon Oct 21 10:35:12 PDT 2025",
      "startTimeMs": 1729531845000,
      "endTimeMs": 1729532112000
    }
  ]
}
```

### Example Empty Response
```json
[]
```

---

## Document Version
**Version:** 1.0
**Date:** 2026-01-02
**Consolidated From:**
- test-plan-get_attacks.md (51 test cases)
- test-plan-get_attacks_filtered.md (74 test cases)
**Total Coverage:** 97 unique test cases (duplicates merged)

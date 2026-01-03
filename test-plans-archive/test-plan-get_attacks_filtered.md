# Test Plan: get_attacks_filtered Tool

## Overview
This test plan provides comprehensive coverage for the `get_attacks_filtered` tool in the ADRService class. This tool allows filtering of attack data from Contrast ADR (Attack Detection and Response) using multiple filter parameters.

**Tool Location**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/ADRService.java` (lines 171-212)

**Test Execution Context**: These tests should be executed by an AI agent using the MCP server against a live Contrast Security instance with appropriate test data including ADR attack events.

---

## Test Categories

### 1. Basic Functionality Tests

#### Test 1.1: No Filters (Default Behavior)
**Description**: Call the tool without any filter parameters to verify default behavior.

**Input**:
```
quickFilter: null (or omitted)
keyword: null (or omitted)
includeSuppressed: null (or omitted)
includeBotBlockers: null (or omitted)
includeIpBlacklist: null (or omitted)
limit: null (or omitted)
offset: null (or omitted)
sort: null (or omitted)
```

**Expected Behavior**:
- Returns all attacks matching default AttacksFilterBody settings
- Default quickFilter is "ALL" (per AttacksFilterBody constructor)
- Default boolean filters are false (per AttacksFilterBody)
- No pagination limits applied (returns all results)
- Response is List<AttackSummary> with attack data
- Each AttackSummary includes: attackId, status, source, rules, probes, timestamps, applications

**Test Data Assumptions**:
- Assume attacks exist in the organization
- Assume mix of different attack types and statuses

---

#### Test 1.2: Single Parameter Filter
**Description**: Test each filter parameter individually.

**Input**:
```
Test cases:
- quickFilter: "PROBED" (all others null)
- keyword: "sql" (all others null)
- includeSuppressed: true (all others null)
- includeBotBlockers: true (all others null)
- includeIpBlacklist: true (all others null)
- limit: 10 (all others null)
- offset: 5 (all others null)
- sort: "NEWEST" (all others null)
```

**Expected Behavior**:
- Each parameter filters or modifies results independently
- Results reflect the single filter applied
- Other filters remain at default values
- Response structure is consistent

**Test Data Assumptions**:
- Assume sufficient attacks exist to test each filter
- Assume attacks with various characteristics (suppressed, bot blockers, etc.)

---

### 2. QuickFilter Tests

#### Test 2.1: QuickFilter - "ALL"
**Description**: Test the "ALL" quick filter value.

**Input**:
```
quickFilter: "ALL"
```

**Expected Behavior**:
- Returns all attacks regardless of status
- Should include attacks in all states
- Most permissive filter setting
- Default behavior when quickFilter is null

**Test Data Assumptions**:
- Assume attacks exist in various states
- Assume mix of probed, exploited, blocked attacks

---

#### Test 2.2: QuickFilter - "PROBED"
**Description**: Test filtering for probed attacks.

**Input**:
```
quickFilter: "PROBED"
```

**Expected Behavior**:
- Returns only attacks that were probed
- Excludes attacks that were blocked or had other outcomes
- All returned attacks should have probed status indicators

**Test Data Assumptions**:
- Assume probed attacks exist in the organization
- Understand the definition of "probed" in Contrast ADR context

---

#### Test 2.3: QuickFilter - "EXPLOITED"
**Description**: Test filtering for exploited attacks.

**Input**:
```
quickFilter: "EXPLOITED"
```

**Expected Behavior**:
- Returns only attacks that were successfully exploited
- Higher severity attacks
- May return fewer results than PROBED

**Test Data Assumptions**:
- Assume exploited attacks exist
- These are attacks that succeeded past detection/blocking

---

#### Test 2.4: QuickFilter - "BLOCKED"
**Description**: Test filtering for blocked attacks.

**Input**:
```
quickFilter: "BLOCKED"
```

**Expected Behavior**:
- Returns only attacks that were blocked by Contrast Protect
- Shows successful defense events
- Attacks prevented from reaching application

**Test Data Assumptions**:
- Assume blocked attacks exist
- Requires Contrast Protect to be active

---

#### Test 2.5: QuickFilter - Invalid Value
**Description**: Test with an invalid quick filter value.

**Input**:
```
Test cases:
- quickFilter: "INVALID"
- quickFilter: "invalid"
- quickFilter: "UNKNOWN_STATUS"
```

**Expected Behavior**:
- May return error from SDK/API
- Or may return empty results
- Or may default to "ALL" behavior
- Verify the actual behavior matches implementation

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 2.6: QuickFilter - Case Sensitivity
**Description**: Test case handling for quick filter values.

**Input**:
```
Test cases:
- quickFilter: "all" (lowercase)
- quickFilter: "All" (mixed case)
- quickFilter: "probed" (lowercase)
```

**Expected Behavior**:
- Verify whether quick filter is case-sensitive
- Document observed behavior
- Likely expects uppercase based on default "ALL"

**Test Data Assumptions**:
- Assume attacks exist to test filtering

---

### 3. Keyword Filtering Tests

#### Test 3.1: Keyword - Rule Name Match
**Description**: Search for attacks by rule name keyword.

**Input**:
```
Test cases:
- keyword: "sql-injection"
- keyword: "xss"
- keyword: "cmd-injection"
```

**Expected Behavior**:
- Returns attacks matching the keyword in rule names
- All returned attacks have rules array containing keyword
- Case-insensitive search (verify behavior)
- Partial matches may be supported

**Test Data Assumptions**:
- Assume attacks exist with various protection rule types
- Know which rule names exist in test environment

---

#### Test 3.2: Keyword - IP Address Search
**Description**: Search for attacks from specific IP addresses.

**Input**:
```
Test cases:
- keyword: "192.168.1.100"
- keyword: "10.0.0"
- keyword: "192.168" (partial IP)
```

**Expected Behavior**:
- Returns attacks from matching source IP addresses
- Full or partial IP matches depending on implementation
- All returned attacks have matching source field

**Test Data Assumptions**:
- Assume attacks from known IP addresses exist
- Note IP addresses in test environment

---

#### Test 3.3: Keyword - Application Name Search
**Description**: Search for attacks by application name.

**Input**:
```
Test cases:
- keyword: "WebGoat"
- keyword: "MyApp"
- keyword: "api" (partial match)
```

**Expected Behavior**:
- Returns attacks against applications matching keyword
- Searches application names in applications array
- Partial matches may be supported

**Test Data Assumptions**:
- Assume attacks exist against various applications
- Know application names in test environment

---

#### Test 3.4: Keyword - Empty String
**Description**: Test with empty keyword string.

**Input**:
```
keyword: ""
```

**Expected Behavior**:
- Equivalent to no keyword filter
- Returns all attacks (no filtering on keyword)
- No errors

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 3.5: Keyword - Special Characters
**Description**: Test keywords with special characters.

**Input**:
```
Test cases:
- keyword: "user@domain.com" (email)
- keyword: "/admin/login" (path)
- keyword: "SELECT * FROM" (SQL with spaces)
```

**Expected Behavior**:
- Special characters are handled correctly
- URL encoding handled internally if needed
- Returns matching attacks or empty results

**Test Data Assumptions**:
- Assume attacks with various payloads/sources exist

---

#### Test 3.6: Keyword - No Matches
**Description**: Test keyword that matches no attacks.

**Input**:
```
keyword: "nonexistent-keyword-xyz-123"
```

**Expected Behavior**:
- Returns empty List<AttackSummary>
- No errors, just no matches
- Response is empty list, not null

**Test Data Assumptions**:
- Use keyword that definitely doesn't exist in attack data

---

### 4. Boolean Filter Tests

#### Test 4.1: includeSuppressed - True
**Description**: Include suppressed attacks in results.

**Input**:
```
includeSuppressed: true
```

**Expected Behavior**:
- Returns both suppressed and non-suppressed attacks
- Suppressed attacks are included that would normally be excluded
- Total count higher than when false

**Test Data Assumptions**:
- Assume some attacks have been suppressed
- Suppressed attacks exist in the organization

---

#### Test 4.2: includeSuppressed - False
**Description**: Exclude suppressed attacks from results.

**Input**:
```
includeSuppressed: false
```

**Expected Behavior**:
- Returns only non-suppressed attacks
- Default behavior (per AttacksFilterBody)
- Suppressed attacks are filtered out

**Test Data Assumptions**:
- Assume non-suppressed attacks exist
- Some suppressed attacks exist to verify exclusion

---

#### Test 4.3: includeSuppressed - Null
**Description**: Test default behavior when includeSuppressed is null.

**Input**:
```
includeSuppressed: null
```

**Expected Behavior**:
- Uses AttacksFilterBody default (false)
- Excludes suppressed attacks
- Same behavior as explicitly false

**Test Data Assumptions**:
- Assume non-suppressed attacks exist

---

#### Test 4.4: includeBotBlockers - True
**Description**: Include attacks from bot blockers.

**Input**:
```
includeBotBlockers: true
```

**Expected Behavior**:
- Returns attacks including those from bot blockers
- Bot blocker attacks included that would normally be excluded
- Total count higher than when false

**Test Data Assumptions**:
- Assume attacks from bot blockers exist
- Bot blocker detection is active

---

#### Test 4.5: includeBotBlockers - False
**Description**: Exclude attacks from bot blockers.

**Input**:
```
includeBotBlockers: false
```

**Expected Behavior**:
- Returns only non-bot-blocker attacks
- Default behavior (per AttacksFilterBody)
- Bot blocker attacks are filtered out

**Test Data Assumptions**:
- Assume non-bot-blocker attacks exist
- Some bot blocker attacks exist to verify exclusion

---

#### Test 4.6: includeBotBlockers - Null
**Description**: Test default behavior when includeBotBlockers is null.

**Input**:
```
includeBotBlockers: null
```

**Expected Behavior**:
- Uses AttacksFilterBody default (false)
- Excludes bot blocker attacks
- Same behavior as explicitly false

**Test Data Assumptions**:
- Assume non-bot-blocker attacks exist

---

#### Test 4.7: includeIpBlacklist - True
**Description**: Include attacks from blacklisted IPs.

**Input**:
```
includeIpBlacklist: true
```

**Expected Behavior**:
- Returns attacks including those from IP blacklist
- Blacklisted IP attacks included that would normally be excluded
- Total count higher than when false

**Test Data Assumptions**:
- Assume attacks from blacklisted IPs exist
- IP blacklist is configured

---

#### Test 4.8: includeIpBlacklist - False
**Description**: Exclude attacks from blacklisted IPs.

**Input**:
```
includeIpBlacklist: false
```

**Expected Behavior**:
- Returns only non-blacklisted IP attacks
- Default behavior (per AttacksFilterBody)
- Blacklisted IP attacks are filtered out

**Test Data Assumptions**:
- Assume non-blacklisted IP attacks exist
- Some blacklisted IP attacks exist to verify exclusion

---

#### Test 4.9: includeIpBlacklist - Null
**Description**: Test default behavior when includeIpBlacklist is null.

**Input**:
```
includeIpBlacklist: null
```

**Expected Behavior**:
- Uses AttacksFilterBody default (false)
- Excludes blacklisted IP attacks
- Same behavior as explicitly false

**Test Data Assumptions**:
- Assume non-blacklisted IP attacks exist

---

#### Test 4.10: All Boolean Filters - True
**Description**: Test with all boolean filters set to true.

**Input**:
```
includeSuppressed: true
includeBotBlockers: true
includeIpBlacklist: true
```

**Expected Behavior**:
- Returns all attacks regardless of suppression, bot blockers, or IP blacklist
- Most permissive filtering
- Highest attack count
- Includes attacks normally filtered out

**Test Data Assumptions**:
- Assume attacks exist in all categories

---

#### Test 4.11: All Boolean Filters - False
**Description**: Test with all boolean filters explicitly set to false.

**Input**:
```
includeSuppressed: false
includeBotBlockers: false
includeIpBlacklist: false
```

**Expected Behavior**:
- Returns only "clean" attacks (no suppressed, bot blockers, or blacklisted IPs)
- Most restrictive filtering
- Lower attack count
- Default behavior

**Test Data Assumptions**:
- Assume some "clean" attacks exist

---

### 5. Pagination Tests

#### Test 5.1: Limit Only - Small Limit
**Description**: Test limit parameter with small values.

**Input**:
```
Test cases:
- limit: 1
- limit: 5
- limit: 10
```

**Expected Behavior**:
- Returns at most N attacks (where N is the limit)
- Results may be fewer if insufficient data
- No offset, starts from beginning
- First N attacks in the dataset

**Test Data Assumptions**:
- Assume at least 10 attacks exist

---

#### Test 5.2: Limit Only - Large Limit
**Description**: Test limit parameter with large values.

**Input**:
```
Test cases:
- limit: 100
- limit: 500
- limit: 1000
```

**Expected Behavior**:
- Returns at most N attacks
- May be capped by API limits
- No errors even with very large limits
- Verify no performance issues

**Test Data Assumptions**:
- Assume sufficient attacks exist or verify behavior when limit exceeds data

---

#### Test 5.3: Offset Only - Skip Records
**Description**: Test offset parameter to skip records.

**Input**:
```
Test cases:
- offset: 5
- offset: 10
- offset: 50
```

**Expected Behavior**:
- Skips first N attacks
- Returns remaining attacks after offset
- No limit on result count
- Useful for pagination

**Test Data Assumptions**:
- Assume at least 100 attacks exist to test various offsets

---

#### Test 5.4: Limit and Offset - Pagination
**Description**: Test limit and offset together for pagination.

**Input**:
```
Test cases:
- limit: 10, offset: 0 (page 1)
- limit: 10, offset: 10 (page 2)
- limit: 10, offset: 20 (page 3)
- limit: 25, offset: 0 (page 1, larger page size)
- limit: 25, offset: 25 (page 2, larger page size)
```

**Expected Behavior**:
- Returns page of results with specified size
- Each page contains different attacks
- Consistent ordering across pages
- Enables page-by-page navigation

**Test Data Assumptions**:
- Assume at least 75 attacks exist for thorough testing
- Results should be deterministic with consistent ordering

---

#### Test 5.5: Offset Beyond Available Data
**Description**: Test offset that exceeds total attack count.

**Input**:
```
offset: 9999
```

**Expected Behavior**:
- Returns empty List<AttackSummary>
- No errors, just no results
- Graceful handling of out-of-bounds offset

**Test Data Assumptions**:
- Assume fewer than 9999 attacks exist

---

#### Test 5.6: Limit Zero or Negative
**Description**: Test invalid limit values.

**Input**:
```
Test cases:
- limit: 0
- limit: -1
- limit: -100
```

**Expected Behavior**:
- May return all results (no limit)
- Or may return error
- Or may return empty results
- Verify actual SDK/API behavior

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 5.7: Offset Negative
**Description**: Test invalid offset values.

**Input**:
```
Test cases:
- offset: -1
- offset: -10
```

**Expected Behavior**:
- May default to 0 (no offset)
- Or may return error
- Verify actual SDK/API behavior

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 5.8: Pagination Consistency
**Description**: Verify pagination returns all records exactly once.

**Input**:
```
Test sequence:
1. Get total count (no limit/offset)
2. Paginate with limit: 10, offset: 0, 10, 20, 30... until all retrieved
```

**Expected Behavior**:
- Sum of all pages equals total count
- No duplicate attacks across pages
- No missing attacks
- Consistent ordering ensures complete coverage

**Test Data Assumptions**:
- Assume stable attack dataset during test
- At least 50 attacks for meaningful test

---

### 6. Sorting Tests

#### Test 6.1: Sort - "NEWEST"
**Description**: Test sorting by newest attacks first.

**Input**:
```
sort: "NEWEST"
```

**Expected Behavior**:
- Returns attacks ordered by most recent first
- Latest attacks appear first in list
- Verify by checking startTime or lastEventTime timestamps
- Descending time order

**Test Data Assumptions**:
- Assume attacks with different timestamps exist
- Attacks span a time range

---

#### Test 6.2: Sort - "OLDEST"
**Description**: Test sorting by oldest attacks first.

**Input**:
```
sort: "OLDEST"
```

**Expected Behavior**:
- Returns attacks ordered by oldest first
- Earliest attacks appear first in list
- Verify by checking startTime or firstEventTime timestamps
- Ascending time order

**Test Data Assumptions**:
- Assume attacks with different timestamps exist
- Historical attacks exist

---

#### Test 6.3: Sort - "SEVERITY" (if supported)
**Description**: Test sorting by severity.

**Input**:
```
sort: "SEVERITY"
```

**Expected Behavior**:
- Returns attacks ordered by severity
- Higher severity attacks first (CRITICAL → HIGH → MEDIUM → LOW)
- Verify by checking severity in ApplicationAttackInfo

**Test Data Assumptions**:
- Assume attacks with various severities exist
- Severity information is available in attack data

---

#### Test 6.4: Sort - "PROBES" (if supported)
**Description**: Test sorting by probe count.

**Input**:
```
sort: "PROBES"
```

**Expected Behavior**:
- Returns attacks ordered by probe count
- Attacks with most probes first
- Verify by checking probes field
- Descending probe count order

**Test Data Assumptions**:
- Assume attacks with varying probe counts exist

---

#### Test 6.5: Sort - Invalid Value
**Description**: Test with invalid sort parameter.

**Input**:
```
Test cases:
- sort: "INVALID"
- sort: "invalid"
- sort: "UNKNOWN_SORT"
```

**Expected Behavior**:
- May return error
- Or may default to a default sort order
- Or may ignore sort parameter
- Verify actual behavior

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 6.6: Sort - Null (Default Sorting)
**Description**: Test default sorting when sort is null.

**Input**:
```
sort: null
```

**Expected Behavior**:
- Uses default API sort order
- Likely newest first or by significance
- Results are deterministic
- Document observed default behavior

**Test Data Assumptions**:
- Assume attacks exist

---

#### Test 6.7: Sort with Pagination
**Description**: Test sorting combined with pagination.

**Input**:
```
sort: "NEWEST"
limit: 10
offset: 0
```

**Expected Behavior**:
- Results are sorted within each page
- Pagination maintains sort order across pages
- First page has 10 newest attacks
- Second page (offset: 10) has next 10 newest attacks

**Test Data Assumptions**:
- Assume at least 20 attacks with different timestamps

---

### 7. Combined Filter Tests

#### Test 7.1: QuickFilter + Keyword
**Description**: Combine quick filter with keyword search.

**Input**:
```
quickFilter: "PROBED"
keyword: "sql"
```

**Expected Behavior**:
- Returns only probed attacks
- That also match keyword "sql"
- Both filters applied (AND logic)
- Narrower result set

**Test Data Assumptions**:
- Assume probed SQL-related attacks exist

---

#### Test 7.2: QuickFilter + Boolean Filters
**Description**: Combine quick filter with boolean inclusion filters.

**Input**:
```
quickFilter: "EXPLOITED"
includeSuppressed: true
includeBotBlockers: true
```

**Expected Behavior**:
- Returns exploited attacks
- Including suppressed and bot blocker attacks
- Shows more complete picture of exploited attacks

**Test Data Assumptions**:
- Assume exploited attacks exist in various categories

---

#### Test 7.3: Keyword + Boolean Filters
**Description**: Combine keyword search with boolean filters.

**Input**:
```
keyword: "192.168"
includeSuppressed: true
includeIpBlacklist: true
```

**Expected Behavior**:
- Returns attacks from 192.168.x.x IP range
- Including suppressed and IP blacklist attacks
- Comprehensive view of attacks from specific IP range

**Test Data Assumptions**:
- Assume attacks from 192.168.x.x IPs exist

---

#### Test 7.4: QuickFilter + Keyword + Pagination
**Description**: Combine filtering with pagination.

**Input**:
```
quickFilter: "PROBED"
keyword: "xss"
limit: 5
offset: 0
```

**Expected Behavior**:
- Returns first 5 probed XSS attacks
- Both filters applied before pagination
- Can page through filtered results

**Test Data Assumptions**:
- Assume at least 10 probed XSS attacks exist

---

#### Test 7.5: QuickFilter + Keyword + Sorting
**Description**: Combine filtering with sorting.

**Input**:
```
quickFilter: "BLOCKED"
keyword: "sql"
sort: "NEWEST"
```

**Expected Behavior**:
- Returns blocked SQL-related attacks
- Sorted by newest first
- All three parameters work together

**Test Data Assumptions**:
- Assume blocked SQL attacks exist with different timestamps

---

#### Test 7.6: All Filters Combined (Kitchen Sink)
**Description**: Test with all filter parameters specified.

**Input**:
```
quickFilter: "PROBED"
keyword: "sql-injection"
includeSuppressed: true
includeBotBlockers: false
includeIpBlacklist: false
limit: 20
offset: 10
sort: "NEWEST"
```

**Expected Behavior**:
- All filters applied simultaneously
- Returns page of results matching all criteria
- Demonstrates full filter composition
- Complex query executes successfully

**Test Data Assumptions**:
- Assume attacks exist matching criteria
- May return fewer results due to restrictive filtering

---

#### Test 7.7: Boolean Filters + Pagination + Sorting
**Description**: Test boolean filters with pagination and sorting.

**Input**:
```
includeSuppressed: true
includeBotBlockers: true
includeIpBlacklist: true
limit: 10
offset: 0
sort: "NEWEST"
```

**Expected Behavior**:
- Returns first 10 attacks (all types)
- Sorted by newest
- Most permissive boolean filters
- Highest attack count with pagination

**Test Data Assumptions**:
- Assume at least 20 attacks exist

---

### 8. Empty Results Tests

#### Test 8.1: No Attacks in Organization
**Description**: Test against organization with no attacks.

**Input**:
```
(Any filters or no filters)
```

**Expected Behavior**:
- Returns empty List<AttackSummary>
- Not null, but empty list
- Log message indicates no attacks found
- No errors

**Test Data Assumptions**:
- Use organization with zero attacks
- Or test immediately after attacks are cleared

---

#### Test 8.2: QuickFilter Returns No Results
**Description**: Test quick filter that matches no attacks.

**Input**:
```
Test cases:
- quickFilter: "EXPLOITED" (when no exploited attacks exist)
- quickFilter: "BLOCKED" (when no blocked attacks exist)
```

**Expected Behavior**:
- Returns empty List<AttackSummary>
- Valid query, just no matching data
- No errors

**Test Data Assumptions**:
- Know which attack types don't exist in test environment

---

#### Test 8.3: Keyword Returns No Matches
**Description**: Test keyword that matches no attacks.

**Input**:
```
keyword: "nonexistent-keyword-xyz-123"
```

**Expected Behavior**:
- Returns empty List<AttackSummary>
- No matching attacks
- No errors

**Test Data Assumptions**:
- Use keyword that definitely doesn't exist

---

#### Test 8.4: All Boolean Filters Exclude All Attacks
**Description**: Test when boolean filters exclude everything.

**Input**:
```
includeSuppressed: false
includeBotBlockers: false
includeIpBlacklist: false
(When all attacks are in one of these excluded categories)
```

**Expected Behavior**:
- Returns empty List<AttackSummary>
- All attacks filtered out by exclusion criteria
- Valid scenario, just no "clean" attacks exist

**Test Data Assumptions**:
- All attacks are suppressed, bot blockers, or blacklisted
- No "clean" attacks exist

---

#### Test 8.5: Offset Beyond All Results
**Description**: Test pagination offset that exceeds available data.

**Input**:
```
offset: 1000
(When fewer than 1000 attacks exist)
```

**Expected Behavior**:
- Returns empty List<AttackSummary>
- No errors, just no more data
- Graceful handling

**Test Data Assumptions**:
- Fewer than 1000 attacks exist

---

#### Test 8.6: Combined Filters Too Restrictive
**Description**: Test filter combination that matches nothing.

**Input**:
```
quickFilter: "EXPLOITED"
keyword: "nonexistent-rule"
includeSuppressed: false
limit: 10
```

**Expected Behavior**:
- Returns empty List<AttackSummary>
- No attacks match all criteria
- Valid query, just too restrictive

**Test Data Assumptions**:
- Intentionally use filter combination that won't match

---

### 9. Validation Tests

#### Test 9.1: Null vs Omitted Parameters
**Description**: Verify behavior difference between null and omitted parameters.

**Input**:
```
Test cases:
- All parameters omitted (not passed)
- All parameters explicitly null
```

**Expected Behavior**:
- Behavior should be identical
- Both use AttacksFilterBody defaults
- No errors in either case

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 9.2: Empty String vs Null
**Description**: Test empty string vs null for string parameters.

**Input**:
```
Test cases:
- quickFilter: "" vs quickFilter: null
- keyword: "" vs keyword: null
- sort: "" vs sort: null
```

**Expected Behavior**:
- Empty string may be treated as null
- Or may cause validation error
- Document actual behavior

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 9.3: Very Long Keyword
**Description**: Test keyword parameter with very long string.

**Input**:
```
keyword: "<1000+ character string>"
```

**Expected Behavior**:
- May be accepted and search performed
- Or may return error if exceeds API limits
- Or may be truncated
- No server crash or unhandled exception

**Test Data Assumptions**:
- Any test data is acceptable

---

#### Test 9.4: Special Characters in Keyword
**Description**: Test keyword with various special characters.

**Input**:
```
Test cases:
- keyword: "<script>alert('xss')</script>"
- keyword: "'; DROP TABLE attacks;--"
- keyword: "../../../etc/passwd"
- keyword: "%00null-byte"
```

**Expected Behavior**:
- Special characters handled safely
- No injection vulnerabilities
- Either returns matching attacks or empty results
- No errors or security issues

**Test Data Assumptions**:
- Security test - any test data acceptable

---

#### Test 9.5: Whitespace in Parameters
**Description**: Test parameters with leading/trailing whitespace.

**Input**:
```
Test cases:
- keyword: "  sql  " (spaces)
- keyword: "\tsql\t" (tabs)
- keyword: "\nsql\n" (newlines)
```

**Expected Behavior**:
- Whitespace may be trimmed automatically
- Or treated as part of search term
- Document actual behavior
- No errors

**Test Data Assumptions**:
- Any test data is acceptable

---

### 10. Performance Tests

#### Test 10.1: Response Time - No Filters
**Description**: Measure baseline performance with no filters.

**Input**:
```
(All parameters null/omitted)
```

**Expected Behavior**:
- Response completes successfully
- Log shows execution time (from startTime/duration)
- Baseline timing for comparison
- Typically fastest query

**Test Data Assumptions**:
- Assume moderate attack count (100-1000 attacks)

---

#### Test 10.2: Response Time - Single Filter
**Description**: Measure performance with single filter.

**Input**:
```
Test cases:
- quickFilter: "PROBED"
- keyword: "sql"
- limit: 100
```

**Expected Behavior**:
- Response completes successfully
- Log shows execution time
- Compare to baseline (Test 10.1)
- Should be similar performance

**Test Data Assumptions**:
- Same dataset as Test 10.1

---

#### Test 10.3: Response Time - Multiple Filters
**Description**: Measure performance with multiple filters.

**Input**:
```
quickFilter: "PROBED"
keyword: "sql"
includeSuppressed: true
sort: "NEWEST"
```

**Expected Behavior**:
- Response completes successfully
- Log shows execution time
- May be slightly slower than single filter
- Still acceptable performance (< 5 seconds typically)

**Test Data Assumptions**:
- Same dataset as previous tests

---

#### Test 10.4: Response Time - Large Result Set
**Description**: Measure performance when returning many results.

**Input**:
```
includeSuppressed: true
includeBotBlockers: true
includeIpBlacklist: true
(No limit - return all attacks)
```

**Expected Behavior**:
- Response completes successfully
- Execution time logged
- May be slower with large result set
- Verify acceptable performance
- Note time increases with data volume

**Test Data Assumptions**:
- Large attack dataset (1000+ attacks)

---

#### Test 10.5: Response Time - Pagination vs No Pagination
**Description**: Compare performance of paginated vs unpaginated queries.

**Input**:
```
Test A: limit: 10, offset: 0
Test B: (no limit or offset)
```

**Expected Behavior**:
- Paginated query (Test A) may be faster
- Unpaginated query (Test B) returns all data
- Compare execution times
- Pagination useful for large datasets

**Test Data Assumptions**:
- Large attack dataset

---

#### Test 10.6: Response Time - Complex Keyword Search
**Description**: Measure performance of keyword search.

**Input**:
```
Test cases:
- keyword: "a" (very common, many matches)
- keyword: "very-specific-unique-term" (few matches)
```

**Expected Behavior**:
- Both complete successfully
- Common keywords may return more results (slower)
- Specific keywords faster (fewer matches)
- Document timing difference

**Test Data Assumptions**:
- Varied attack data for keyword matching

---

#### Test 10.7: Concurrent Request Handling
**Description**: Test behavior under concurrent requests (if possible).

**Input**:
```
Multiple simultaneous calls with different filters
```

**Expected Behavior**:
- All requests complete successfully
- No race conditions or errors
- Each request returns correct filtered data
- Performance degrades gracefully under load

**Test Data Assumptions**:
- Depends on testing framework capabilities
- May not be testable via MCP protocol

---

### 11. Comparison with get_attacks Tool

#### Test 11.1: Consistency - No Filters vs get_attacks
**Description**: Compare get_attacks_filtered with no filters to get_attacks.

**Input**:
```
get_attacks_filtered: (all parameters null)
get_attacks: (no parameters)
```

**Expected Behavior**:
- Results should be similar or identical
- Both return all attacks with default filtering
- AttackSummary structure identical
- May have slight differences due to default filters (check AttacksFilterBody defaults)
- Compare attack counts and content

**Test Data Assumptions**:
- Stable attack dataset during test

---

#### Test 11.2: Consistency - QuickFilter "ALL"
**Description**: Verify quickFilter "ALL" matches get_attacks behavior.

**Input**:
```
get_attacks_filtered: quickFilter: "ALL"
get_attacks: (no parameters)
```

**Expected Behavior**:
- Should return same or very similar results
- "ALL" quick filter means no status filtering
- Verify consistency of attack data

**Test Data Assumptions**:
- Stable attack dataset during test

---

#### Test 11.3: Consistency - Attack Data Structure
**Description**: Verify AttackSummary structure is identical between tools.

**Input**:
```
Call both tools and compare result structure
```

**Expected Behavior**:
- Both return List<AttackSummary>
- Each AttackSummary has identical fields:
  - attackId, status, source, rules, probes
  - startTime, endTime, startTimeMs, endTimeMs
  - firstEventTime, lastEventTime, firstEventTimeMs, lastEventTimeMs
  - applications (List<ApplicationAttackInfo>)
- Field values match for same attack

**Test Data Assumptions**:
- At least one attack exists in both result sets

---

#### Test 11.4: Consistency - Timestamp Formats
**Description**: Verify timestamp formatting is consistent.

**Input**:
```
Call both tools and compare timestamp fields
```

**Expected Behavior**:
- Timestamp strings (startTime, endTime, etc.) use same format
- Millisecond values (startTimeMs, endTimeMs, etc.) are identical
- Both use Date.toString() for string timestamps
- Millisecond values are long integers

**Test Data Assumptions**:
- At least one attack exists

---

#### Test 11.5: Consistency - Application Information
**Description**: Verify application attack info is consistent.

**Input**:
```
Call both tools and compare applications array
```

**Expected Behavior**:
- ApplicationAttackInfo structure identical
- Fields: applicationId, applicationName, language, severity, status
- Timestamp fields: startTime, endTime, startTimeMs, endTimeMs
- Same application data for same attack

**Test Data Assumptions**:
- At least one attack with application info exists

---

#### Test 11.6: Feature Difference - Filtering Capability
**Description**: Demonstrate filtering features not available in get_attacks.

**Input**:
```
get_attacks_filtered: quickFilter: "EXPLOITED", keyword: "sql"
get_attacks: (no filtering capability)
```

**Expected Behavior**:
- get_attacks_filtered returns filtered subset
- get_attacks returns all attacks (no filtering)
- Demonstrates value of filtered tool
- Filtered tool provides more precise results

**Test Data Assumptions**:
- Mix of exploited and non-exploited attacks
- SQL-related and non-SQL attacks exist

---

#### Test 11.7: Feature Difference - Pagination
**Description**: Demonstrate pagination not available in get_attacks.

**Input**:
```
get_attacks_filtered: limit: 10, offset: 0
get_attacks: (no pagination)
```

**Expected Behavior**:
- get_attacks_filtered returns 10 attacks
- get_attacks returns all attacks
- Filtered tool enables pagination
- Better for handling large attack datasets

**Test Data Assumptions**:
- More than 10 attacks exist

---

#### Test 11.8: Performance Comparison
**Description**: Compare execution time between tools.

**Input**:
```
get_attacks_filtered: (no filters)
get_attacks: (no parameters)
```

**Expected Behavior**:
- Both complete successfully
- Check logs for execution time
- Performance should be similar for equivalent queries
- get_attacks may be slightly faster (less parameter processing)
- Difference should be minimal

**Test Data Assumptions**:
- Same dataset, run tests close together in time

---

#### Test 11.9: Error Handling Comparison
**Description**: Compare error handling between tools.

**Input**:
```
Test in error scenarios (invalid org, no connectivity, etc.)
```

**Expected Behavior**:
- Both tools throw exceptions appropriately
- Error messages are informative
- Logging is consistent
- IOException thrown for connectivity issues
- No unhandled exceptions

**Test Data Assumptions**:
- Requires ability to simulate error conditions

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server is running and connected to valid Contrast Security instance
2. Ensure test environment has:
   - Multiple attack events (at least 50-100 for thorough testing)
   - Attacks with different statuses (probed, exploited, blocked)
   - Attacks from various sources (different IPs)
   - Attacks triggering different protection rules (SQL injection, XSS, etc.)
   - Mix of suppressed and non-suppressed attacks
   - Attacks from bot blockers (if bot blocker feature is enabled)
   - Attacks from blacklisted IPs (if IP blacklist is configured)
   - Attacks against multiple applications
   - Attacks spanning a time range (days or weeks)
   - Various attack severities (CRITICAL, HIGH, MEDIUM, LOW)

### During Testing
1. Record all request parameters for each test
2. Capture complete response (List<AttackSummary>)
3. Verify response structure matches expected AttackSummary format
4. Check log files (/tmp/mcp-contrast.log) for:
   - Execution timing (startTime to duration)
   - Debug messages about SDK initialization
   - Info messages about attack counts
   - Any warnings or errors
5. Note any unexpected behavior or edge cases

### Test Data Recommendations
For comprehensive testing, the test environment should ideally have:
- At least 100 attack events for pagination and performance testing
- Attacks in multiple status categories (probed, exploited, blocked)
- Attacks from at least 10 different source IPs
- Attacks triggering at least 5-10 different protection rules
- At least 20% suppressed attacks
- At least 10% bot blocker attacks (if feature enabled)
- At least 10% blacklisted IP attacks (if feature configured)
- Attacks against multiple applications (3-5 apps minimum)
- Attack events spanning at least 1-2 weeks for time-based testing
- Mix of single-probe and multi-probe attacks
- Attacks with various severities in application context

### Success Criteria
Each test passes when:
1. Response structure is correct (List<AttackSummary>)
2. Data filtering works as expected
3. Pagination calculations are accurate
4. Sorting produces correct order
5. Boolean filters include/exclude appropriately
6. No unexpected exceptions or errors occur
7. Performance is acceptable (logged timing is reasonable)
8. Behavior matches expected based on parameters
9. Empty results handled gracefully (empty list, not null)
10. Logs show successful execution with timing

### Known Behaviors and Considerations
1. **Default Filters**: AttacksFilterBody defaults:
   - quickFilter: "ALL"
   - keyword: "" (empty)
   - includeSuppressed: false
   - includeBotBlockers: false
   - includeIpBlacklist: false

2. **Null Parameters**: When parameters are null:
   - Not set on AttacksFilterBody (uses defaults)
   - Equivalent to omitting the parameter

3. **SDK Delegation**: Tool delegates to SDKExtension.getAttacks()
   - Filter validation happens in SDK/API
   - Invalid parameters may return API errors

4. **Response Time**: Typical response times:
   - Simple queries: < 2 seconds
   - Complex filters: 2-5 seconds
   - Large result sets: 5-10 seconds
   - Log timing for comparison

5. **AttackSummary Structure**: Consistent across all tool calls
   - Uses AttackSummary.fromAttack() for conversion
   - Timestamps provided in both string and millisecond formats

---

## Appendix: Example Test Execution Commands

These examples show how an AI agent might invoke the tool during testing:

```json
// Test 1.1: No filters (defaults)
{
  "quickFilter": null,
  "keyword": null,
  "includeSuppressed": null,
  "includeBotBlockers": null,
  "includeIpBlacklist": null,
  "limit": null,
  "offset": null,
  "sort": null
}

// Test 2.2: QuickFilter - PROBED
{
  "quickFilter": "PROBED",
  "keyword": null,
  "includeSuppressed": null,
  "includeBotBlockers": null,
  "includeIpBlacklist": null,
  "limit": null,
  "offset": null,
  "sort": null
}

// Test 3.1: Keyword search
{
  "quickFilter": null,
  "keyword": "sql-injection",
  "includeSuppressed": null,
  "includeBotBlockers": null,
  "includeIpBlacklist": null,
  "limit": null,
  "offset": null,
  "sort": null
}

// Test 4.10: All boolean filters true
{
  "quickFilter": null,
  "keyword": null,
  "includeSuppressed": true,
  "includeBotBlockers": true,
  "includeIpBlacklist": true,
  "limit": null,
  "offset": null,
  "sort": null
}

// Test 5.4: Pagination - page 2
{
  "quickFilter": null,
  "keyword": null,
  "includeSuppressed": null,
  "includeBotBlockers": null,
  "includeIpBlacklist": null,
  "limit": 10,
  "offset": 10,
  "sort": null
}

// Test 6.1: Sort by newest
{
  "quickFilter": null,
  "keyword": null,
  "includeSuppressed": null,
  "includeBotBlockers": null,
  "includeIpBlacklist": null,
  "limit": null,
  "offset": null,
  "sort": "NEWEST"
}

// Test 7.6: Kitchen sink - all filters
{
  "quickFilter": "PROBED",
  "keyword": "sql-injection",
  "includeSuppressed": true,
  "includeBotBlockers": false,
  "includeIpBlacklist": false,
  "limit": 20,
  "offset": 10,
  "sort": "NEWEST"
}
```

---

## Test Coverage Summary

This test plan covers:
- ✓ 2 basic functionality test cases
- ✓ 6 quickFilter test cases
- ✓ 6 keyword filtering test cases
- ✓ 11 boolean filter test cases (includeSuppressed, includeBotBlockers, includeIpBlacklist)
- ✓ 8 pagination test cases (limit and offset)
- ✓ 7 sorting test cases
- ✓ 7 combined filter test cases
- ✓ 6 empty results test cases
- ✓ 5 validation test cases
- ✓ 7 performance test cases
- ✓ 9 comparison test cases (vs get_attacks tool)

**Total: 74 test cases**

Each test case is designed to be executed by an AI agent using the MCP server, with clear input parameters, expected behaviors, and test data assumptions.

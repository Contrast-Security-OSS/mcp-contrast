# Test Cases for `search_attacks` Tool

## Overview

The `search_attacks` tool retrieves attacks from Contrast ADR (Attack Detection and Response) with optional filtering and sorting. It returns a paginated list of attack summaries with key information including rule names, status, severity, affected applications, source IP, and probe counts.

**Supported parameters:**
- `page`, `pageSize` - Pagination (max 100 items per page)
- `quickFilter` - Attack categorization: ALL, ACTIVE, MANUAL, AUTOMATED, PRODUCTION, EFFECTIVE
- `statusFilter` - Attack outcome: EXPLOITED, PROBED, BLOCKED, BLOCKED_PERIMETER, PROBED_PERIMETER, SUSPICIOUS
- `keyword` - Match against rule names, sources, or notes
- `includeSuppressed` - Include suppressed attacks (default: false)
- `includeBotBlockers` - Include attacks flagged as bot blockers
- `includeIpBlacklist` - Include attacks from blacklisted IPs
- `sort` - Sort field: sourceIP, status, startTime, endTime, type (prefix with '-' for descending)

---

## Pre-Test Setup (REQUIRED)

**Attack data has a 30-day rolling window.** Before executing tests, you MUST run a baseline query to establish current data state.

### Step 1: Run Baseline Query

```
search_attacks(quickFilter="ALL", pageSize=100)
```

### Step 2: Derive Expected Values Using Code

**CRITICAL: Do NOT manually count values by visually inspecting the JSON response.**
Manual counting is error-prone, especially for nested arrays like `rules`. You MUST use
`jq` or equivalent code to compute all baseline metrics programmatically.

Save the baseline JSON response to a variable (e.g., `$BASELINE`), then compute each metric:

#### Status Counts
```bash
# TOTAL_ATTACKS - from response metadata
echo "$BASELINE" | jq '.totalItems'

# EXPLOITED_COUNT
echo "$BASELINE" | jq '[.items[] | select(.status == "EXPLOITED")] | length'

# BLOCKED_COUNT
echo "$BASELINE" | jq '[.items[] | select(.status == "BLOCKED")] | length'

# PROBED_COUNT
echo "$BASELINE" | jq '[.items[] | select(.status == "PROBED")] | length'

# EFFECTIVE_COUNT (TOTAL - PROBED)
echo "$BASELINE" | jq '[.items[] | select(.status != "PROBED")] | length'
```

#### Rule-Based Counts
```bash
# SQL_INJECTION_COUNT - attacks with "SQL Injection" in rules array
echo "$BASELINE" | jq '[.items[] | select(.rules[] | contains("SQL Injection"))] | unique_by(.attackId) | length'

# COMMAND_INJECTION_COUNT
echo "$BASELINE" | jq '[.items[] | select(.rules[] | contains("Command Injection"))] | unique_by(.attackId) | length'

# XXE_COUNT - matches "XML External Entity" in rules
echo "$BASELINE" | jq '[.items[] | select(.rules[] | contains("XML External Entity"))] | unique_by(.attackId) | length'

# LOG4SHELL_COUNT - matches "Log4" in rules
echo "$BASELINE" | jq '[.items[] | select(.rules[] | contains("Log4"))] | unique_by(.attackId) | length'

# DESERIALIZATION_COUNT
echo "$BASELINE" | jq '[.items[] | select(.rules[] | contains("Deserialization"))] | unique_by(.attackId) | length'

# PATH_TRAVERSAL_COUNT
echo "$BASELINE" | jq '[.items[] | select(.rules[] | contains("Path Traversal"))] | unique_by(.attackId) | length'

# XSS_COUNT
echo "$BASELINE" | jq '[.items[] | select(.rules[] | contains("Cross-Site Scripting"))] | unique_by(.attackId) | length'
```

#### Temporal and Aggregate Metrics
```bash
# OLDEST_ATTACK - attack with earliest startTimeMs
echo "$BASELINE" | jq '.items | min_by(.startTimeMs) | {attackId, startTime, source}'

# NEWEST_ATTACK - attack with latest startTimeMs
echo "$BASELINE" | jq '.items | max_by(.startTimeMs) | {attackId, startTime, source}'

# HIGH_PROBE_ATTACK - attack with highest probe count
echo "$BASELINE" | jq '.items | max_by(.probes) | {attackId, probes, source}'

# MULTI_APP_ATTACKS - attacks affecting more than 1 application
echo "$BASELINE" | jq '[.items[] | select((.applications | length) > 1)] | length'

# UNIQUE_SOURCE_IPS - map of source IP to attack IDs
echo "$BASELINE" | jq 'reduce .items[] as $item ({}; .[$item.source] += [$item.attackId])'
```

#### Quick Reference Table

| Metric | jq Command |
|--------|------------|
| `TOTAL_ATTACKS` | `.totalItems` |
| `EXPLOITED_COUNT` | `[.items[] \| select(.status == "EXPLOITED")] \| length` |
| `BLOCKED_COUNT` | `[.items[] \| select(.status == "BLOCKED")] \| length` |
| `PROBED_COUNT` | `[.items[] \| select(.status == "PROBED")] \| length` |
| `EFFECTIVE_COUNT` | `[.items[] \| select(.status != "PROBED")] \| length` |
| `SQL_INJECTION_COUNT` | `[.items[] \| select(.rules[] \| contains("SQL Injection"))] \| unique_by(.attackId) \| length` |
| `COMMAND_INJECTION_COUNT` | `[.items[] \| select(.rules[] \| contains("Command Injection"))] \| unique_by(.attackId) \| length` |
| `XXE_COUNT` | `[.items[] \| select(.rules[] \| contains("XML External Entity"))] \| unique_by(.attackId) \| length` |
| `LOG4SHELL_COUNT` | `[.items[] \| select(.rules[] \| contains("Log4"))] \| unique_by(.attackId) \| length` |
| `DESERIALIZATION_COUNT` | `[.items[] \| select(.rules[] \| contains("Deserialization"))] \| unique_by(.attackId) \| length` |
| `PATH_TRAVERSAL_COUNT` | `[.items[] \| select(.rules[] \| contains("Path Traversal"))] \| unique_by(.attackId) \| length` |
| `XSS_COUNT` | `[.items[] \| select(.rules[] \| contains("Cross-Site Scripting"))] \| unique_by(.attackId) \| length` |
| `OLDEST_ATTACK` | `.items \| min_by(.startTimeMs) \| .attackId` |
| `NEWEST_ATTACK` | `.items \| max_by(.startTimeMs) \| .attackId` |
| `HIGH_PROBE_ATTACK` | `.items \| max_by(.probes) \| .attackId` |
| `MULTI_APP_ATTACKS` | `[.items[] \| select((.applications \| length) > 1)] \| length` |

### Step 3: Execute Tests

Use the derived values as expected results for all tests below. Tests reference these values using `{METRIC_NAME}` notation.

---

## Status Filter Tests

### Test 1: Single status filter (EXPLOITED)
**Purpose:** Verify filtering by EXPLOITED status returns only exploited attacks.

**Prompt:**
```
use contrast mcp to search for attacks with status EXPLOITED
```

**Expected Result:**
- Count equals `{EXPLOITED_COUNT}` from baseline
- All returned attacks have `status: "EXPLOITED"`
- Should NOT include any BLOCKED or PROBED attacks

---

### Test 2: Single status filter (BLOCKED)
**Purpose:** Verify filtering by BLOCKED status returns only blocked attacks.

**Prompt:**
```
use contrast mcp to search for attacks with status BLOCKED
```

**Expected Result:**
- Count equals `{BLOCKED_COUNT}` from baseline
- All returned attacks have `status: "BLOCKED"`
- Attack IDs match those identified in baseline with BLOCKED status

---

### Test 3: Single status filter (PROBED)
**Purpose:** Verify filtering by PROBED status returns only probed attacks.

**Prompt:**
```
use contrast mcp to search for attacks with status PROBED
```

**Expected Result:**
- Count equals `{PROBED_COUNT}` from baseline
- All returned attacks have `status: "PROBED"`
- Attack IDs match those identified in baseline with PROBED status

---

### Test 4: Status filter case sensitivity
**Purpose:** Verify status filter is case-insensitive.

**Prompt:**
```
use contrast mcp to search for attacks with status exploited
```

**Expected Result:** Either:
- Returns same results as Test 1 (case-insensitive), OR
- Returns 0 results or error (case-sensitive - requires uppercase)

---

### Test 5: Non-matching status filter
**Purpose:** Verify filtering by a status with no matching attacks.

**Prompt:**
```
use contrast mcp to search for attacks with status BLOCKED_PERIMETER
```

**Expected Result:** 0 attacks returned (empty list)
- Should show warning: "No results found matching the specified criteria"

---

## Quick Filter Tests

### Test 6: Quick filter EFFECTIVE
**Purpose:** Verify EFFECTIVE filter excludes probed-only attacks.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter EFFECTIVE
```

**Expected Result:**
- Count equals `{EFFECTIVE_COUNT}` (= `{TOTAL_ATTACKS}` - `{PROBED_COUNT}`)
- Should include all EXPLOITED and BLOCKED attacks
- Should NOT include any PROBED-status attacks
- No "No quickFilter applied" warning

---

### Test 7: Quick filter ALL
**Purpose:** Verify ALL filter returns all attack types.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter ALL
```

**Expected Result:**
- Count equals `{TOTAL_ATTACKS}` from baseline
- Should include all statuses: EXPLOITED, BLOCKED, PROBED
- No "No quickFilter applied" warning

---

### Test 8: No quick filter (default behavior)
**Purpose:** Verify default behavior without quickFilter shows warning.

**Prompt:**
```
use contrast mcp to search for all attacks
```

**Expected Result:**
- Count equals `{TOTAL_ATTACKS}` from baseline
- Warnings present:
  - "No quickFilter applied - showing all attack types"
  - "Excluding suppressed attacks by default..."

---

### Test 9: Quick filter ACTIVE
**Purpose:** Verify ACTIVE filter returns ongoing attacks.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter ACTIVE
```

**Expected Result:** Attacks that are currently active/ongoing
- Results depend on attack timing relative to current time
- Count may be 0 if no attacks are currently active

---

### Test 10: Quick filter MANUAL
**Purpose:** Verify MANUAL filter returns human-initiated attacks.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter MANUAL
```

**Expected Result:** Human-initiated attacks only
- Verify all returned attacks are categorized as manual (not automated)

---

### Test 11: Quick filter AUTOMATED
**Purpose:** Verify AUTOMATED filter returns bot attacks.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter AUTOMATED
```

**Expected Result:** Bot/automated attacks only
- Verify all returned attacks are categorized as automated

---

### Test 12: Quick filter PRODUCTION
**Purpose:** Verify PRODUCTION filter returns attacks in production environment.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter PRODUCTION
```

**Expected Result:** Attacks on production environment applications only

---

## Keyword Filter Tests

### Test 13: Keyword filter by rule name (sql)
**Purpose:** Verify keyword filter matches rule names.

**Prompt:**
```
use contrast mcp to search for attacks with keyword sql
```

**Expected Result:**
- Count equals `{SQL_INJECTION_COUNT}` from baseline
- All returned attacks have "SQL Injection" in their rules array

---

### Test 14: Keyword filter by rule name (Command)
**Purpose:** Verify keyword filter matches partial rule names.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Command
```

**Expected Result:**
- Count equals `{COMMAND_INJECTION_COUNT}` from baseline
- All returned attacks have "Command Injection" in their rules array

---

### Test 15: Keyword filter by rule name (Deserialization)
**Purpose:** Verify keyword filter matches "Untrusted Deserialization" rule.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Deserialization
```

**Expected Result:**
- Count equals `{DESERIALIZATION_COUNT}` from baseline
- All returned attacks have "Untrusted Deserialization" in their rules array

---

### Test 16: Keyword filter by source IP
**Purpose:** Verify keyword filter matches source IP addresses.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {ANY_SOURCE_IP_WITH_ONE_ATTACK}
```

**Expected Result:**
- Returns attack(s) matching that source IP from baseline
- Verify source field matches the searched IP

---

### Test 17: Keyword filter by partial IP
**Purpose:** Verify keyword filter matches partial IP patterns.

**Prompt:**
```
use contrast mcp to search for attacks with keyword 10.1.1
```

**Expected Result:**
- Returns all attacks from baseline where source IP starts with "10.1.1"
- Note: Also matches IPs like "10.1.10.x" since "10.1.1" is a substring

---

### Test 18: Keyword filter non-matching
**Purpose:** Verify non-matching keyword returns empty results.

**Prompt:**
```
use contrast mcp to search for attacks with keyword NonExistentKeyword123
```

**Expected Result:** 0 attacks returned
- Warning: "No results found matching the specified criteria"

---

### Test 19: Keyword filter case sensitivity
**Purpose:** Verify keyword filter case behavior.

**Prompt:**
```
use contrast mcp to search for attacks with keyword SQL
```

**Expected Result:** Should match same as "sql" if case-insensitive
- Count equals `{SQL_INJECTION_COUNT}` from baseline

---

## Sorting Tests

### Test 20: Sort by startTime descending (default)
**Purpose:** Verify default sort is by startTime descending.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5
```

**Expected Result:** 5 most recent attacks first
- First result should be `{NEWEST_ATTACK}` from baseline
- Attacks ordered by startTime descending

---

### Test 21: Sort by startTime ascending
**Purpose:** Verify ascending sort by startTime.

**Prompt:**
```
use contrast mcp to search for attacks sorted by startTime ascending
```

**Expected Result:** Oldest attacks first
- First result should be `{OLDEST_ATTACK}` from baseline

---

### Test 22: Sort by sourceIP ascending
**Purpose:** Verify sorting by source IP address.

**Prompt:**
```
use contrast mcp to search for attacks sorted by sourceIP
```

**Expected Result:** Attacks sorted by source IP lexicographically
- Lower IP addresses first (e.g., 10.1.1.x before 10.1.10.x)

---

### Test 23: Sort by sourceIP descending
**Purpose:** Verify descending sort by source IP.

**Prompt:**
```
use contrast mcp to search for attacks sorted by sourceIP descending (use -sourceIP)
```

**Expected Result:** Attacks sorted by source IP descending
- Higher IP addresses first (e.g., 10.1.10.x before 10.1.1.x)

---

### Test 24: Sort by status
**Purpose:** Verify sorting by attack status.

**Prompt:**
```
use contrast mcp to search for attacks sorted by status
```

**Expected Result:** Attacks grouped by status
- Verify attacks are sorted/grouped by their status field

---

## Pagination Tests

### Test 25: Pagination with pageSize=5
**Purpose:** Verify pagination returns limited results with hasMorePages indicator.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5
```

**Expected Result:**
- Exactly 5 attacks returned
- `totalItems` equals `{TOTAL_ATTACKS}` from baseline
- `hasMorePages: true` (if `{TOTAL_ATTACKS}` > 5)

---

### Test 26: Pagination page 2
**Purpose:** Verify retrieving the second page of results.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5 and page 2
```

**Expected Result:**
- Up to 5 attacks returned (different from page 1)
- Attack IDs should not overlap with page 1 results

---

### Test 27: Pagination page 3 (partial page)
**Purpose:** Verify last page returns remaining results.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5 and page 3
```

**Expected Result:**
- Returns `{TOTAL_ATTACKS} - 10` attacks (remaining after pages 1-2)
- `hasMorePages: false` (if this is the last page)

---

### Test 28: Pagination beyond results
**Purpose:** Verify requesting page beyond available data.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5 and page 100
```

**Expected Result:** 0 attacks returned (empty list)
- `hasMorePages: false`

---

### Test 29: Large page size
**Purpose:** Verify maximum page size handling.

**Prompt:**
```
use contrast mcp to search for attacks with page size 100
```

**Expected Result:**
- All `{TOTAL_ATTACKS}` returned (if <= 100)
- `hasMorePages: false`

---

## Combined Filter Tests

### Test 30: Quick filter + Status filter combined
**Purpose:** Verify combining quickFilter and statusFilter (AND logic).

**Prompt:**
```
use contrast mcp to search for attacks with quick filter EFFECTIVE and status EXPLOITED
```

**Expected Result:**
- Count equals `{EXPLOITED_COUNT}` from baseline
- Same as EXPLOITED filter since EFFECTIVE already excludes PROBED

---

### Test 31: Status filter + Keyword combined
**Purpose:** Verify combining statusFilter and keyword (AND logic).

**Prompt:**
```
use contrast mcp to search for attacks with status EXPLOITED and keyword sql
```

**Expected Result:**
- Count equals attacks from baseline with BOTH `status: "EXPLOITED"` AND "SQL Injection" in rules
- Should NOT include any PROBED or BLOCKED attacks

---

### Test 32: Keyword + Sort combined
**Purpose:** Verify keyword filter with custom sorting.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Command sorted by sourceIP
```

**Expected Result:**
- Count equals `{COMMAND_INJECTION_COUNT}` from baseline
- Results sorted by source IP ascending

---

### Test 33: Status filter + Pagination combined
**Purpose:** Verify status filter with pagination.

**Prompt:**
```
use contrast mcp to search for attacks with status EXPLOITED, page size 3, page 1
```

**Expected Result:**
- 3 EXPLOITED attacks returned
- `totalItems` equals `{EXPLOITED_COUNT}` from baseline
- `hasMorePages: true` (if `{EXPLOITED_COUNT}` > 3)

---

### Test 34: Quick filter + Keyword + Pagination combined
**Purpose:** Verify multiple filters with pagination.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter EFFECTIVE, keyword sql, page size 3
```

**Expected Result:**
- Up to 3 attacks with SQL Injection rule, excluding PROBED status
- Verify count matches baseline-derived intersection

---

## Include Flags Tests

### Test 35: Include suppressed attacks
**Purpose:** Verify includeSuppressed flag includes suppressed attacks.

**Prompt:**
```
use contrast mcp to search for attacks including suppressed attacks
```

**Expected Result:**
- All attacks including any suppressed ones
- No "Excluding suppressed attacks" warning

---

### Test 36: Include bot blockers
**Purpose:** Verify includeBotBlockers flag includes bot blocker attacks.

**Prompt:**
```
use contrast mcp to search for attacks including bot blockers
```

**Expected Result:** All attacks including those flagged as bot blockers

---

### Test 37: Include IP blacklist attacks
**Purpose:** Verify includeIpBlacklist flag includes blacklisted IP attacks.

**Prompt:**
```
use contrast mcp to search for attacks including attacks from blacklisted IPs
```

**Expected Result:** All attacks including those from blacklisted IPs

---

### Test 38: All include flags combined
**Purpose:** Verify all include flags together.

**Prompt:**
```
use contrast mcp to search for attacks including suppressed, bot blockers, and blacklisted IPs
```

**Expected Result:** Maximum set of attacks with all include flags enabled

---

## Edge Case Tests

### Test 39: No filters (default behavior with warnings)
**Purpose:** Verify default behavior returns all attacks with appropriate warnings.

**Prompt:**
```
use contrast mcp to search for attacks
```

**Expected Result:**
- Count equals `{TOTAL_ATTACKS}` from baseline
- Warnings present:
  - "No quickFilter applied - showing all attack types"
  - "Excluding suppressed attacks by default..."

---

### Test 40: Multiple applications affected by single attack
**Purpose:** Verify attacks affecting multiple applications show all apps.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Path
```

**Expected Result:**
- Returns attacks with "Path Traversal" rule
- For any `{MULTI_APP_ATTACKS}` from baseline, verify applications array contains all affected apps

---

### Test 41: Attack with many rules
**Purpose:** Verify attacks with multiple rules display all rules.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {SOURCE_IP_OF_ATTACK_WITH_MOST_RULES}
```

**Expected Result:**
- Returns attack(s) from that IP
- Verify all rules from baseline are present in the response

---

### Test 42: Single probe attack
**Purpose:** Verify attacks with minimal probes are included.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {SOURCE_IP_OF_ATTACK_WITH_1_PROBE}
```

**Expected Result:**
- Returns the attack with 1 probe
- Verify `probes: 1` in response

---

### Test 43: High probe count attack
**Purpose:** Verify attacks with high probe counts display correctly.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {SOURCE_IP_OF_HIGH_PROBE_ATTACK}
```

**Expected Result:**
- Returns `{HIGH_PROBE_ATTACK}` from baseline
- Verify probe count matches baseline

---

## Response Field Verification Tests

### Test 44: Verify attackId field format
**Purpose:** Confirm attackId is UUID format.

**Prompt:**
```
use contrast mcp to search for attacks with page size 1
```

**Expected Result:** Attack with attackId in UUID format
- Pattern: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

---

### Test 45: Verify timestamp fields
**Purpose:** Confirm timestamp fields are present and formatted.

**Prompt:**
```
use contrast mcp to search for attacks with page size 1
```

**Expected Result:** Attack with timestamp fields:
- `startTime`, `endTime` - ISO format with timezone
- `startTimeMs`, `endTimeMs` - Millisecond timestamps
- `firstEventTime`, `lastEventTime` - ISO format
- `firstEventTimeMs`, `lastEventTimeMs` - Milliseconds

---

### Test 46: Verify application details in response
**Purpose:** Confirm application details are complete.

**Prompt:**
```
use contrast mcp to search for attacks with keyword sql page size 1
```

**Expected Result:** Attack with applications array containing:
- `applicationId` - UUID format
- `applicationName` - Full application name
- `language` - Java, Python, .NET Core, Node, etc.
- `severity` - LOW, MEDIUM, HIGH, CRITICAL
- `status` - Attack status for this specific app
- `startTime`, `endTime`, `startTimeMs`, `endTimeMs`

---

### Test 47: Verify pagination metadata
**Purpose:** Confirm pagination metadata is accurate.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5
```

**Expected Result:** Response includes:
- `page: 1`
- `pageSize: 5`
- `totalItems` equals `{TOTAL_ATTACKS}` from baseline
- `hasMorePages: true` (if `{TOTAL_ATTACKS}` > 5)
- `success: true`
- `durationMs` - Query duration in milliseconds

---

## Cross-Application Attack Tests

### Test 48: Attack spanning multiple applications
**Purpose:** Verify attacks that hit multiple apps at once.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {SOURCE_IP_WITH_MULTI_APP_ATTACKS}
```

**Expected Result:**
- Returns attacks from that source IP
- For multi-app attacks, verify applications array contains all affected apps from baseline

---

### Test 49: Application language diversity
**Purpose:** Verify attacks across different application languages.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter EFFECTIVE
```

**Expected Result:** Attacks affecting applications in multiple languages
- Verify languages from baseline are represented (e.g., Java, Python, .NET Core, Node)

---

## Rule Type Verification Tests

### Test 50: SQL Injection attacks
**Purpose:** Find all SQL Injection attacks.

**Prompt:**
```
use contrast mcp to search for attacks with keyword "SQL Injection"
```

**Expected Result:**
- Count equals `{SQL_INJECTION_COUNT}` from baseline

---

### Test 51: Command Injection attacks
**Purpose:** Find all Command Injection attacks.

**Prompt:**
```
use contrast mcp to search for attacks with keyword "Command Injection"
```

**Expected Result:**
- Count equals `{COMMAND_INJECTION_COUNT}` from baseline

---

### Test 52: XXE attacks
**Purpose:** Find all XML External Entity attacks.

**Prompt:**
```
use contrast mcp to search for attacks with keyword XML
```

**Expected Result:**
- Count equals `{XXE_COUNT}` from baseline
- All returned attacks have XXE-related rules

---

### Test 53: Log4Shell attacks
**Purpose:** Find attacks exploiting Log4Shell vulnerability.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Log4
```

**Expected Result:**
- Count equals `{LOG4SHELL_COUNT}` from baseline

---

### Test 54: Deserialization attacks
**Purpose:** Find Untrusted Deserialization attacks.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Deserialization
```

**Expected Result:**
- Count equals `{DESERIALIZATION_COUNT}` from baseline

---

## Error Handling Tests

### Test 55: Invalid quickFilter value
**Purpose:** Verify error handling for invalid quickFilter.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter INVALID_FILTER
```

**Expected Result:**
- `success: false`
- Error message indicating invalid quickFilter value
- Lists valid values: ACTIVE, AUTOMATED, ALL, MANUAL, PRODUCTION, EFFECTIVE

---

### Test 56: Invalid statusFilter value
**Purpose:** Verify error handling for invalid statusFilter.

**Prompt:**
```
use contrast mcp to search for attacks with status INVALID_STATUS
```

**Expected Result:**
- `success: false`
- Error message indicating invalid statusFilter value
- Lists valid values: EXPLOITED, PROBED, BLOCKED, etc.

---

### Test 57: Invalid sort field
**Purpose:** Verify error handling for invalid sort field.

**Prompt:**
```
use contrast mcp to search for attacks sorted by invalidField
```

**Expected Result:**
- `success: false`
- Error message indicating invalid sort field
- Lists valid fields: sourceIP, status, startTime, endTime, type

---

### Test 58: Negative page number
**Purpose:** Verify handling of invalid page numbers.

**Prompt:**
```
use contrast mcp to search for attacks with page -1
```

**Expected Result:**
- Warning: "Invalid page number -1, using page 1"
- Defaults to page 1
- Returns valid results

---

### Test 59: Zero page size
**Purpose:** Verify handling of zero page size.

**Prompt:**
```
use contrast mcp to search for attacks with page size 0
```

**Expected Result:**
- Warning: "Invalid pageSize 0, using default 50"
- Defaults to pageSize 50
- Returns valid results

---

### Test 60: Page size exceeding maximum
**Purpose:** Verify handling of page size beyond maximum.

**Prompt:**
```
use contrast mcp to search for attacks with page size 500
```

**Expected Result:**
- Warning: "Requested pageSize 500 exceeds maximum 100, capped to 100"
- pageSize capped at 100
- Returns valid results

---

## Additional Keyword Field Tests

These tests verify keyword search works across all documented fields (source IP, server name/hostname, application name, rule name, attack UUID, forwarded IP/path, attack tags).

### Test 61: Keyword filter by application name prefix
**Purpose:** Verify keyword filter matches application names.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {COMMON_APP_NAME_PREFIX}
```

**Expected Result:**
- Returns attacks from applications matching that prefix
- Count matches baseline count of attacks with that app name prefix

---

### Test 62: Keyword filter by specific application name
**Purpose:** Verify keyword filter matches specific application name component.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {UNIQUE_APP_NAME_COMPONENT}
```

**Expected Result:**
- Returns attack(s) targeting that specific application
- Count matches baseline

---

### Test 63: Keyword filter by application service name
**Purpose:** Verify keyword filter matches service-specific applications.

**Prompt:**
```
use contrast mcp to search for attacks with keyword docservice
```

**Expected Result:**
- Returns attacks targeting docservice applications
- Count matches baseline count of docservice attacks

---

### Test 64: Keyword filter by partial attack UUID
**Purpose:** Verify keyword filter matches partial attack UUID.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {FIRST_8_CHARS_OF_ANY_ATTACK_ID}
```

**Expected Result:**
- Returns 1+ attacks containing that UUID prefix
- Confirms keyword searches attack UUID field

---

### Test 65: Keyword filter by partial UUID (specific status)
**Purpose:** Verify keyword filter matches UUID of attack with specific status.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {FIRST_8_CHARS_OF_BLOCKED_ATTACK_ID}
```

**Expected Result:**
- Returns the BLOCKED attack from baseline
- Verify status and other fields match

---

### Test 66: Keyword filter by full attack UUID
**Purpose:** Verify keyword filter matches full attack UUID.

**Prompt:**
```
use contrast mcp to search for attacks with keyword {FULL_ATTACK_UUID}
```

**Expected Result:**
- Returns exactly 1 attack with that UUID
- Verify all fields match baseline

---

## Summary

| Test # | Category | Filter Type | Expected Behavior |
|--------|----------|-------------|-------------------|
| 1 | Status | EXPLOITED | Returns `{EXPLOITED_COUNT}` attacks |
| 2 | Status | BLOCKED | Returns `{BLOCKED_COUNT}` attacks |
| 3 | Status | PROBED | Returns `{PROBED_COUNT}` attacks |
| 4 | Status | Case sensitivity | Tests case handling |
| 5 | Status | Non-matching | Returns 0 (BLOCKED_PERIMETER) |
| 6 | QuickFilter | EFFECTIVE | Returns `{EFFECTIVE_COUNT}` attacks |
| 7 | QuickFilter | ALL | Returns `{TOTAL_ATTACKS}` attacks |
| 8 | QuickFilter | Default | Shows warnings, returns all |
| 9 | QuickFilter | ACTIVE | Returns ongoing attacks |
| 10 | QuickFilter | MANUAL | Returns human-initiated attacks |
| 11 | QuickFilter | AUTOMATED | Returns bot attacks |
| 12 | QuickFilter | PRODUCTION | Returns production attacks |
| 13 | Keyword | sql | Returns `{SQL_INJECTION_COUNT}` attacks |
| 14 | Keyword | Command | Returns `{COMMAND_INJECTION_COUNT}` attacks |
| 15 | Keyword | Deserialization | Returns `{DESERIALIZATION_COUNT}` attacks |
| 16 | Keyword | Source IP (full) | Returns attacks from that IP |
| 17 | Keyword | Source IP (partial) | Returns matching IP attacks |
| 18 | Keyword | Non-matching | Returns 0 attacks |
| 19 | Keyword | Case sensitivity | Tests SQL vs sql |
| 20 | Sort | Default (-startTime) | Most recent first |
| 21 | Sort | startTime ascending | Oldest first |
| 22 | Sort | sourceIP ascending | Sorted by IP |
| 23 | Sort | -sourceIP descending | Reverse IP sort |
| 24 | Sort | status | Grouped by status |
| 25 | Pagination | pageSize=5 | 5 results, correct totalItems |
| 26 | Pagination | page=2, pageSize=5 | Next 5 results |
| 27 | Pagination | page=3, pageSize=5 | Remaining results |
| 28 | Pagination | Beyond results | Empty list |
| 29 | Pagination | pageSize=100 | All results |
| 30 | Combined | EFFECTIVE + EXPLOITED | AND logic |
| 31 | Combined | EXPLOITED + keyword | AND logic |
| 32 | Combined | Keyword + sort | Combined filter/sort |
| 33 | Combined | Status + pagination | Paginated filtered results |
| 34 | Combined | Quick + keyword + page | Multiple filters |
| 35 | Include | includeSuppressed | Includes suppressed |
| 36 | Include | includeBotBlockers | Includes bot blockers |
| 37 | Include | includeIpBlacklist | Includes blacklisted IPs |
| 38 | Include | All flags | Maximum result set |
| 39 | Edge Case | No filters | Default with warnings |
| 40 | Edge Case | Multi-app attack | Shows all affected apps |
| 41 | Edge Case | Many rules | Shows all rules |
| 42 | Edge Case | Single probe | Includes minimal attacks |
| 43 | Edge Case | High probes | Shows correct probe count |
| 44 | Response | attackId format | UUID format |
| 45 | Response | Timestamps | ISO + milliseconds |
| 46 | Response | Application details | Complete app info |
| 47 | Response | Pagination metadata | Accurate counts |
| 48 | Cross-App | Multi-app attack | Lists all apps |
| 49 | Cross-App | Language diversity | Multiple languages |
| 50 | Rule Type | SQL Injection | Returns `{SQL_INJECTION_COUNT}` attacks |
| 51 | Rule Type | Command Injection | Returns `{COMMAND_INJECTION_COUNT}` attacks |
| 52 | Rule Type | XXE | Returns `{XXE_COUNT}` attacks |
| 53 | Rule Type | Log4Shell | Returns `{LOG4SHELL_COUNT}` attacks |
| 54 | Rule Type | Deserialization | Returns `{DESERIALIZATION_COUNT}` attacks |
| 55 | Error | Invalid quickFilter | Error with valid options |
| 56 | Error | Invalid statusFilter | Error with valid options |
| 57 | Error | Invalid sort field | Error with valid fields |
| 58 | Error | Negative page | Warning, defaults to page 1 |
| 59 | Error | Zero pageSize | Warning, defaults to 50 |
| 60 | Error | Excessive pageSize | Warning, capped to 100 |
| 61 | Keyword | Application name prefix | Matches app name |
| 62 | Keyword | Specific app component | Matches specific app |
| 63 | Keyword | Service name | Matches service apps |
| 64 | Keyword | Partial attack UUID | Matches UUID substring |
| 65 | Keyword | Partial UUID (specific) | Matches specific attack |
| 66 | Keyword | Full attack UUID | Exact match |

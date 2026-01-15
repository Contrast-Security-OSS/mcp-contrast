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

## Known Test Data

The organization has 11 total attacks across multiple applications. Below are representative samples:

| Attack ID | Status | Source IP | Severity | Probes | Rules | App Name |
|-----------|--------|-----------|----------|--------|-------|----------|
| 8edd3316-4692-42c6-b9dd-56cb6eb8057c | EXPLOITED | 10.1.1.122 | MEDIUM | 44 | Command Injection, XSS, JNDI, Log4Shell, Path Traversal, SQLi, Deserialization | thib-...-frontgateservice |
| 5ff46e79-1327-4239-ae14-d22708c3d5ce | EXPLOITED | 10.1.1.72 | MEDIUM | 22 | Command Injection, XSS, JNDI, Log4Shell, Path Traversal, SQLi, Deserialization | thib-...-frontgateservice |
| 32983644-9e46-4cd8-aa28-bc24a5e92f62 | EXPLOITED | 10.1.1.128 | LOW | 15 | XSS, Path Traversal, SQL Injection | thib-...-dataservice, imageservice, labelservice |
| 518c2a79-c346-4171-a9bb-d5ebfaea44ac | EXPLOITED | 10.1.1.75 | LOW | 9 | XSS, Path Traversal, SQL Injection | thib-...-dataservice, labelservice, imageservice |
| 1d841151-1584-44c2-b02c-987ff561bde1 | EXPLOITED | 10.1.10.78 | MEDIUM | 6 | Untrusted Deserialization | Harshaa-...-frontgateservice |
| 70958c6a-e622-4017-a5b0-ea046184e0dc | EXPLOITED | 10.1.10.174 | LOW | 2 | SQL Injection | Harshaa-...-dataservice |
| 17323c1d-bc65-4d7c-b50a-c8aa6daf140e | EXPLOITED | 10.1.1.128 | LOW | 2 | XXE | thib-...-docservice |
| 65ab87d9-22cf-4dcf-82fd-f16c690e6197 | BLOCKED | 10.1.10.74 | MEDIUM | 2 | Command Injection | Harshaa-...-webhookservice |
| 4f7833b8-28d4-488e-b78a-d7bcd36a4c26 | PROBED | 10.1.10.177 | LOW | 1 | SQL Injection | Harshaa-...-frontgateservice |
| 05fb6a0a-222a-46d5-8cf6-571a5620a6c2 | EXPLOITED | 10.1.1.75 | LOW | 1 | XXE | thib-...-docservice |
| 86096285-1537-4d07-b2a3-5e25e903d568 | EXPLOITED | 10.1.9.199 | LOW | 1 | XXE | Harshaa-...-docservice |

---

## Status Filter Tests

### Test 1: Single status filter (EXPLOITED)
**Purpose:** Verify filtering by EXPLOITED status returns only exploited attacks.

**Prompt:**
```
use contrast mcp to search for attacks with status EXPLOITED
```

**Expected Result:** 9 attacks returned, all with status "EXPLOITED"
- Should include attacks from sources: 10.1.1.122, 10.1.1.72, 10.1.1.128, 10.1.1.75, etc.
- Should NOT include the BLOCKED attack (65ab87d9) or PROBED attack (4f7833b8)

---

### Test 2: Single status filter (BLOCKED)
**Purpose:** Verify filtering by BLOCKED status returns only blocked attacks.

**Prompt:**
```
use contrast mcp to search for attacks with status BLOCKED
```

**Expected Result:** 1 attack returned
- Attack ID: 65ab87d9-22cf-4dcf-82fd-f16c690e6197
- Source: 10.1.10.74
- Rules: Command Injection
- App: Harshaa-MSSentinel-Incident-Event-Data-contrast-cargo-cats-webhookservice

---

### Test 3: Single status filter (PROBED)
**Purpose:** Verify filtering by PROBED status returns only probed attacks.

**Prompt:**
```
use contrast mcp to search for attacks with status PROBED
```

**Expected Result:** 1 attack returned
- Attack ID: 4f7833b8-28d4-488e-b78a-d7bcd36a4c26
- Source: 10.1.10.177
- Rules: SQL Injection
- App: Harshaa-MSSentinel-Incident-Event-Data-contrast-cargo-cats-frontgateservice

---

### Test 4: Status filter case sensitivity
**Purpose:** Verify status filter is case-sensitive (should use uppercase).

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

**Expected Result:** 10 attacks returned (excludes PROBED-only attacks)
- Should include all EXPLOITED and BLOCKED attacks
- Should NOT include attack 4f7833b8 (PROBED status)
- No "No quickFilter applied" warning

---

### Test 7: Quick filter ALL
**Purpose:** Verify ALL filter returns all attack types.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter ALL
```

**Expected Result:** 11 attacks returned
- Should include all statuses: EXPLOITED, BLOCKED, PROBED
- No "No quickFilter applied" warning

---

### Test 8: No quick filter (default behavior)
**Purpose:** Verify default behavior without quickFilter shows warning.

**Prompt:**
```
use contrast mcp to search for all attacks
```

**Expected Result:** 11 attacks returned with warnings:
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

---

### Test 10: Quick filter MANUAL
**Purpose:** Verify MANUAL filter returns human-initiated attacks.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter MANUAL
```

**Expected Result:** Human-initiated attacks only (if any exist)

---

### Test 11: Quick filter AUTOMATED
**Purpose:** Verify AUTOMATED filter returns bot attacks.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter AUTOMATED
```

**Expected Result:** Bot/automated attacks only (if any exist)

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

**Expected Result:** 6 attacks returned containing "SQL Injection" rule
- Includes: 70958c6a, 4f7833b8, 32983644, 8edd3316, 518c2a79, 5ff46e79

---

### Test 14: Keyword filter by rule name (Command)
**Purpose:** Verify keyword filter matches partial rule names.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Command
```

**Expected Result:** 3 attacks returned containing "Command Injection" rule
- Includes: 8edd3316, 65ab87d9, 5ff46e79

---

### Test 15: Keyword filter by rule name (Deserialization)
**Purpose:** Verify keyword filter matches "Untrusted Deserialization" rule.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Deserialization
```

**Expected Result:** 3 attacks returned
- Includes: 8edd3316, 1d841151, 5ff46e79 (all with Untrusted Deserialization rule)

---

### Test 16: Keyword filter by source IP
**Purpose:** Verify keyword filter matches source IP addresses.

**Prompt:**
```
use contrast mcp to search for attacks with keyword 10.1.1.122
```

**Expected Result:** 1 attack returned
- Attack ID: 8edd3316-4692-42c6-b9dd-56cb6eb8057c
- Source: 10.1.1.122
- 44 probes, MEDIUM severity

---

### Test 17: Keyword filter by partial IP
**Purpose:** Verify keyword filter matches partial IP patterns.

**Prompt:**
```
use contrast mcp to search for attacks with keyword 10.1.1
```

**Expected Result:** Multiple attacks from 10.1.1.x subnet
- Should include: 10.1.1.122, 10.1.1.72, 10.1.1.128, 10.1.1.75

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
- 6 attacks with SQL Injection rule

---

## Sorting Tests

### Test 20: Sort by startTime descending (default)
**Purpose:** Verify default sort is by startTime descending.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5
```

**Expected Result:** 5 most recent attacks first
- First result should be most recent attack (70958c6a or 4f7833b8 from 2026-01-12)

---

### Test 21: Sort by startTime ascending
**Purpose:** Verify ascending sort by startTime.

**Prompt:**
```
use contrast mcp to search for attacks sorted by startTime ascending
```

**Expected Result:** Oldest attacks first
- First result should be oldest attack (86096285 from 2025-12-16)

---

### Test 22: Sort by sourceIP ascending
**Purpose:** Verify sorting by source IP address.

**Prompt:**
```
use contrast mcp to search for attacks sorted by sourceIP
```

**Expected Result:** Attacks sorted by source IP
- Should start with 10.1.1.x addresses before 10.1.10.x addresses

---

### Test 23: Sort by sourceIP descending
**Purpose:** Verify descending sort by source IP.

**Prompt:**
```
use contrast mcp to search for attacks sorted by sourceIP descending (use -sourceIP)
```

**Expected Result:** Attacks sorted by source IP descending
- Should start with higher IP addresses (10.1.10.x before 10.1.1.x)

---

### Test 24: Sort by status
**Purpose:** Verify sorting by attack status.

**Prompt:**
```
use contrast mcp to search for attacks sorted by status
```

**Expected Result:** Attacks grouped by status
- BLOCKED, EXPLOITED, PROBED (alphabetical or by severity)

---

## Pagination Tests

### Test 25: Pagination with pageSize=5
**Purpose:** Verify pagination returns limited results with hasMorePages indicator.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5
```

**Expected Result:** 5 attacks returned
- `hasMorePages: true`
- `totalItems: 11`

---

### Test 26: Pagination page 2
**Purpose:** Verify retrieving the second page of results.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5 and page 2
```

**Expected Result:** 5 different attacks (page 2 of results)
- Should not overlap with page 1 results

---

### Test 27: Pagination page 3 (partial page)
**Purpose:** Verify last page returns remaining results.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5 and page 3
```

**Expected Result:** 1 attack returned (11 total, pages 1-2 had 10)
- `hasMorePages: false`

---

### Test 28: Pagination beyond results
**Purpose:** Verify requesting page beyond available data.

**Prompt:**
```
use contrast mcp to search for attacks with page size 5 and page 10
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

**Expected Result:** All 11 attacks returned
- `hasMorePages: false`

---

## Combined Filter Tests

### Test 30: Quick filter + Status filter combined
**Purpose:** Verify combining quickFilter and statusFilter (AND logic).

**Prompt:**
```
use contrast mcp to search for attacks with quick filter EFFECTIVE and status EXPLOITED
```

**Expected Result:** 9 EXPLOITED attacks (EFFECTIVE excludes PROBED)
- Same as EXPLOITED filter since EFFECTIVE already excludes PROBED

---

### Test 31: Status filter + Keyword combined
**Purpose:** Verify combining statusFilter and keyword (AND logic).

**Prompt:**
```
use contrast mcp to search for attacks with status EXPLOITED and keyword sql
```

**Expected Result:** EXPLOITED attacks with SQL Injection rule
- Should include: 70958c6a, 32983644, 8edd3316, 518c2a79, 5ff46e79
- Should NOT include: 4f7833b8 (PROBED status)

---

### Test 32: Keyword + Sort combined
**Purpose:** Verify keyword filter with custom sorting.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Command sorted by sourceIP
```

**Expected Result:** 3 Command Injection attacks sorted by source IP
- Sorted order by IP: 10.1.1.72, 10.1.1.122, 10.1.10.74

---

### Test 33: Status filter + Pagination combined
**Purpose:** Verify status filter with pagination.

**Prompt:**
```
use contrast mcp to search for attacks with status EXPLOITED, page size 3, page 1
```

**Expected Result:** 3 EXPLOITED attacks (page 1 of 3 pages)
- `totalItems: 9`
- `hasMorePages: true`

---

### Test 34: Quick filter + Keyword + Pagination combined
**Purpose:** Verify multiple filters with pagination.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter EFFECTIVE, keyword sql, page size 3
```

**Expected Result:** SQL-related effective attacks, paginated
- Returns attacks with SQL rules, excluding PROBED status

---

## Include Flags Tests

### Test 35: Include suppressed attacks
**Purpose:** Verify includeSuppressed flag includes suppressed attacks.

**Prompt:**
```
use contrast mcp to search for attacks including suppressed attacks
```

**Expected Result:** All attacks including any suppressed ones
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

**Expected Result:** 11 attacks with warnings:
- "No quickFilter applied - showing all attack types"
- "Excluding suppressed attacks by default. To see all attacks including suppressed, set includeSuppressed=true."

---

### Test 40: Multiple applications affected by single attack
**Purpose:** Verify attacks affecting multiple applications show all apps.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Path
```

**Expected Result:** Attacks with "Path Traversal" rule
- Attack 32983644 should show 3 applications: dataservice, imageservice, labelservice
- Attack 518c2a79 should show 3 applications: dataservice, labelservice, imageservice

---

### Test 41: Attack with many rules
**Purpose:** Verify attacks with multiple rules display all rules.

**Prompt:**
```
use contrast mcp to search for attacks with keyword 10.1.1.122
```

**Expected Result:** Attack 8edd3316 with 7 rules:
- Command Injection, Cross-Site Scripting, JNDI Injection
- Log4Shell CVE-2021-45046, Path Traversal, SQL Injection, Untrusted Deserialization

---

### Test 42: Single probe attack
**Purpose:** Verify attacks with minimal probes are included.

**Prompt:**
```
use contrast mcp to search for attacks with keyword 10.1.10.177
```

**Expected Result:** Attack 4f7833b8 with 1 probe
- Status: PROBED
- Rules: SQL Injection

---

### Test 43: High probe count attack
**Purpose:** Verify attacks with high probe counts display correctly.

**Prompt:**
```
use contrast mcp to search for attacks with keyword 10.1.1.122
```

**Expected Result:** Attack 8edd3316 with 44 probes
- Highest probe count in the dataset

---

## Response Field Verification Tests

### Test 44: Verify attackId field format
**Purpose:** Confirm attackId is UUID format.

**Prompt:**
```
use contrast mcp to search for attacks with page size 1
```

**Expected Result:** Attack with attackId in UUID format
- Example: "8edd3316-4692-42c6-b9dd-56cb6eb8057c"

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
- `totalItems: 11`
- `hasMorePages: true`
- `success: true`
- `durationMs` - Query duration

---

## Cross-Application Attack Tests

### Test 48: Attack spanning multiple applications
**Purpose:** Verify attacks that hit multiple apps at once.

**Prompt:**
```
use contrast mcp to search for attacks with keyword 10.1.1.128
```

**Expected Result:** 2 attacks from source 10.1.1.128:
1. Attack 32983644 - affects 3 apps (dataservice, imageservice, labelservice)
2. Attack 17323c1d - affects 1 app (docservice)

---

### Test 49: Application language diversity
**Purpose:** Verify attacks across different application languages.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter EFFECTIVE
```

**Expected Result:** Attacks affecting applications in multiple languages:
- Java: frontgateservice, dataservice
- Python: docservice, webhookservice
- .NET Core: imageservice
- Node: labelservice

---

## Rule Type Verification Tests

### Test 50: SQL Injection attacks
**Purpose:** Find all SQL Injection attacks.

**Prompt:**
```
use contrast mcp to search for attacks with keyword "SQL Injection"
```

**Expected Result:** 6 attacks with SQL Injection rule

---

### Test 51: Command Injection attacks
**Purpose:** Find all Command Injection attacks.

**Prompt:**
```
use contrast mcp to search for attacks with keyword "Command Injection"
```

**Expected Result:** 3 attacks with Command Injection rule

---

### Test 52: XXE attacks
**Purpose:** Find all XML External Entity attacks.

**Prompt:**
```
use contrast mcp to search for attacks with keyword XML
```

**Expected Result:** 3 attacks with XXE rule
- Attack IDs: 17323c1d, 05fb6a0a, 86096285

---

### Test 53: Log4Shell attacks
**Purpose:** Find attacks exploiting Log4Shell vulnerability.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Log4
```

**Expected Result:** 2 attacks with Log4Shell rule
- Attack IDs: 8edd3316, 5ff46e79

---

### Test 54: Deserialization attacks
**Purpose:** Find Untrusted Deserialization attacks.

**Prompt:**
```
use contrast mcp to search for attacks with keyword Deserialization
```

**Expected Result:** 3 attacks with Deserialization rule
- Includes: 8edd3316, 1d841151, 5ff46e79

---

## Error Handling Tests

### Test 55: Invalid quickFilter value
**Purpose:** Verify error handling for invalid quickFilter.

**Prompt:**
```
use contrast mcp to search for attacks with quick filter INVALID_FILTER
```

**Expected Result:** Either:
- Error message indicating invalid quickFilter value, OR
- Treated as no filter applied

---

### Test 56: Invalid statusFilter value
**Purpose:** Verify error handling for invalid statusFilter.

**Prompt:**
```
use contrast mcp to search for attacks with status INVALID_STATUS
```

**Expected Result:** Either:
- Error message indicating invalid statusFilter value, OR
- Returns 0 results

---

### Test 57: Invalid sort field
**Purpose:** Verify error handling for invalid sort field.

**Prompt:**
```
use contrast mcp to search for attacks sorted by invalidField
```

**Expected Result:** Either:
- Error message indicating invalid sort field, OR
- Falls back to default sort (-startTime)

---

### Test 58: Negative page number
**Purpose:** Verify handling of invalid page numbers.

**Prompt:**
```
use contrast mcp to search for attacks with page -1
```

**Expected Result:** Either:
- Error message, OR
- Defaults to page 1

---

### Test 59: Zero page size
**Purpose:** Verify handling of zero page size.

**Prompt:**
```
use contrast mcp to search for attacks with page size 0
```

**Expected Result:** Either:
- Error message, OR
- Defaults to page size 50

---

### Test 60: Page size exceeding maximum
**Purpose:** Verify handling of page size beyond maximum.

**Prompt:**
```
use contrast mcp to search for attacks with page size 500
```

**Expected Result:** Either:
- Capped at maximum (100), OR
- Error message about maximum page size

---

## Summary

| Test # | Category | Filter Type | Expected Behavior |
|--------|----------|-------------|-------------------|
| 1 | Status | EXPLOITED | Returns 9 exploited attacks |
| 2 | Status | BLOCKED | Returns 1 blocked attack |
| 3 | Status | PROBED | Returns 1 probed attack |
| 4 | Status | Case sensitivity | Tests uppercase requirement |
| 5 | Status | Non-matching | Returns 0 (BLOCKED_PERIMETER) |
| 6 | QuickFilter | EFFECTIVE | Excludes PROBED-only attacks |
| 7 | QuickFilter | ALL | Returns all 11 attacks |
| 8 | QuickFilter | Default | Shows "No quickFilter" warning |
| 9 | QuickFilter | ACTIVE | Returns ongoing attacks |
| 10 | QuickFilter | MANUAL | Returns human-initiated attacks |
| 11 | QuickFilter | AUTOMATED | Returns bot attacks |
| 12 | QuickFilter | PRODUCTION | Returns production attacks |
| 13 | Keyword | sql | 6 attacks with SQL Injection |
| 14 | Keyword | Command | 3 attacks with Command Injection |
| 15 | Keyword | Deserialization | 3 attacks with Deserialization |
| 16 | Keyword | Source IP (full) | 1 attack from 10.1.1.122 |
| 17 | Keyword | Source IP (partial) | Multiple from 10.1.1.x |
| 18 | Keyword | Non-matching | Returns 0 attacks |
| 19 | Keyword | Case sensitivity | Tests SQL vs sql |
| 20 | Sort | Default (-startTime) | Most recent first |
| 21 | Sort | startTime ascending | Oldest first |
| 22 | Sort | sourceIP ascending | Sorted by IP |
| 23 | Sort | -sourceIP descending | Reverse IP sort |
| 24 | Sort | status | Grouped by status |
| 25 | Pagination | pageSize=5 | 5 results, hasMorePages=true |
| 26 | Pagination | page=2, pageSize=5 | Next 5 results |
| 27 | Pagination | page=3, pageSize=5 | Last 1 result |
| 28 | Pagination | Beyond results | Empty list |
| 29 | Pagination | pageSize=100 | All 11 results |
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
| 41 | Edge Case | Many rules | Shows all 7 rules |
| 42 | Edge Case | Single probe | Includes minimal attacks |
| 43 | Edge Case | High probes | Shows 44 probes |
| 44 | Response | attackId format | UUID format |
| 45 | Response | Timestamps | ISO + milliseconds |
| 46 | Response | Application details | Complete app info |
| 47 | Response | Pagination metadata | Accurate counts |
| 48 | Cross-App | Multi-app attack | Lists all apps |
| 49 | Cross-App | Language diversity | Multiple languages |
| 50 | Rule Type | SQL Injection | 6 attacks |
| 51 | Rule Type | Command Injection | 3 attacks |
| 52 | Rule Type | XXE | 3 attacks |
| 53 | Rule Type | Log4Shell | 2 attacks |
| 54 | Rule Type | Deserialization | 3 attacks |
| 55 | Error | Invalid quickFilter | Error or default |
| 56 | Error | Invalid statusFilter | Error or empty |
| 57 | Error | Invalid sort field | Error or default |
| 58 | Error | Negative page | Error or default |
| 59 | Error | Zero pageSize | Error or default |
| 60 | Error | Excessive pageSize | Capped or error |

# Test Cases for `search_app_vulnerabilities` Tool

## Known Test Data

**Application:** DemoRouteSession (appId: `9e18a607-2b01-41c7-b35b-52a256840fea`)

**Vulnerabilities:**

| Vuln ID | Type | Severity | Status | Developer | Commit | Repo | Environment | First Seen |
|---------|------|----------|--------|-----------|--------|------|-------------|------------|
| 0VOL-VW0M-Y0C1-Q97K | sql-injection | Note | Suspicious | Ellen | 100 | TS | DEVELOPMENT | 2023-03-09 |
| LPZ0-A2BF-SKL9-UBWO | log-injection | Note | Reported | Sam | 200 | TS | DEVELOPMENT | 2023-03-09 |

---

## Severity Filter Tests

### Test 1: Single severity filter (matching)
**Purpose:** Verify filtering by a single severity that matches both vulnerabilities.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with severity NOTE
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 2: Single severity filter (non-matching)
**Purpose:** Verify filtering by a severity that matches no vulnerabilities returns empty results.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with severity CRITICAL
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

### Test 3: Multiple severities filter
**Purpose:** Verify comma-separated severities use OR logic.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with severity CRITICAL or NOTE
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 4: Severity case insensitivity
**Purpose:** Verify severity filter is case-insensitive.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with severity note
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

## Status Filter Tests

### Test 5: Single status filter (Suspicious)
**Purpose:** Verify filtering by Suspicious status.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with status Suspicious
```

**Expected Result:** 1 vulnerability returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)

---

### Test 6: Single status filter (non-matching)
**Purpose:** Verify filtering by a status that matches no vulnerabilities.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with status Confirmed
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

### Test 7: Include Fixed and Remediated statuses
**Purpose:** Verify that Fixed and Remediated statuses can be explicitly included.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with status Reported, Suspicious, Confirmed, Remediated, or Fixed
```

**Expected Result:** 2 vulnerabilities returned (same as default, but no warning about excluding Fixed/Remediated)
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

## Environment Filter Tests

### Test 8: Single environment filter (matching)
**Purpose:** Verify filtering by DEVELOPMENT environment.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app in the DEVELOPMENT environment
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 9: Single environment filter (non-matching)
**Purpose:** Verify filtering by an environment that matches no vulnerabilities.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app in the PRODUCTION environment
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

### Test 10: Multiple environments filter
**Purpose:** Verify comma-separated environments use OR logic.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app in DEVELOPMENT or PRODUCTION environments
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

## Vulnerability Type Filter Tests

### Test 11: Single vulnerability type (sql-injection)
**Purpose:** Verify filtering by sql-injection type.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with type sql-injection
```

**Expected Result:** 1 vulnerability returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)

---

### Test 12: Single vulnerability type (log-injection)
**Purpose:** Verify filtering by log-injection type.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with type log-injection
```

**Expected Result:** 1 vulnerability returned
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 13: Multiple vulnerability types
**Purpose:** Verify comma-separated vulnerability types use OR logic.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with type sql-injection or log-injection
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 14: Non-matching vulnerability type
**Purpose:** Verify filtering by a type that matches no vulnerabilities.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with type xss-reflected
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

## Session Metadata Filter Tests

### Test 15: Single metadata field (non-matching value)
**Purpose:** Verify filtering by a session metadata value that doesn't exist.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with session metadata of developer=John
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

### Test 16: Session metadata value case sensitivity
**Purpose:** Verify that session metadata values are case-sensitive.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with session metadata of developer=ellen
```

**Expected Result:** 0 vulnerabilities returned - values are case-sensitive, "ellen" does NOT match "Ellen"

---

### Test 17: Session metadata field case insensitivity
**Purpose:** Verify that session metadata field names are case-insensitive.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with session metadata of DEVELOPER=Ellen
```

**Expected Result:** 1 vulnerability returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)

---

### Test 18: Multiple metadata values OR within single field
**Purpose:** Verify OR logic for multiple values within one field using array syntax.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with session metadata where commit is 100 or 200
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection) - commit=100
- LPZ0-A2BF-SKL9-UBWO (log-injection) - commit=200

---

### Test 19: Multiple metadata fields AND (non-matching combination)
**Purpose:** Verify AND logic between fields when combination doesn't exist.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with session metadata of developer=Ellen and commit=200
```

**Expected Result:** 0 vulnerabilities returned (empty list) - Ellen has commit=100, not 200

---

### Test 20: Combined OR and AND in session metadata
**Purpose:** Verify complex filter with OR within a field and AND across fields.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with session metadata where developer is Ellen or Sam, and repo is TS
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

## Date Filter Tests

### Test 21: lastSeenAfter filter (matching)
**Purpose:** Verify filtering vulnerabilities seen after a date before the test data.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app last seen after 2023-01-01
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 22: lastSeenAfter filter (non-matching)
**Purpose:** Verify filtering vulnerabilities seen after a date after the test data.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app last seen after 2024-01-01
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

### Test 23: lastSeenBefore filter (matching)
**Purpose:** Verify filtering vulnerabilities seen before a date after the test data.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app last seen before 2024-01-01
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 24: lastSeenBefore filter (non-matching)
**Purpose:** Verify filtering vulnerabilities seen before a date before the test data.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app last seen before 2023-01-01
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

### Test 25: Date range filter
**Purpose:** Verify filtering with both lastSeenAfter and lastSeenBefore.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app last seen between 2023-03-01 and 2023-03-31
```

**Expected Result:** 2 vulnerabilities returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

## Latest Session Filter Tests

### Test 26: Use latest session filter
**Purpose:** Verify filtering to only the latest session's vulnerabilities.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app using the latest session only
```

**Expected Result:** Vulnerabilities from the most recent session (depends on which session is latest - likely 1 or both vulnerabilities depending on session structure)

---

## Pagination Tests

### Test 27: Pagination with pageSize=1
**Purpose:** Verify pagination returns limited results with hasMorePages indicator.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with page size 1
```

**Expected Result:** 1 vulnerability returned, `hasMorePages: true`, `totalItems: 2`

---

### Test 28: Pagination page 2
**Purpose:** Verify retrieving the second page of results.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with page size 1 and page 2
```

**Expected Result:** 1 vulnerability returned (the other one not returned in Test 27)

---

## Combined Filter Tests

### Test 29: Severity + Status combined
**Purpose:** Verify combining severity and status filters (AND logic between different filter types).

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with severity NOTE and status Suspicious
```

**Expected Result:** 1 vulnerability returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)

---

### Test 30: Environment + Vulnerability type combined
**Purpose:** Verify combining environment and vulnerability type filters.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app in DEVELOPMENT environment with type log-injection
```

**Expected Result:** 1 vulnerability returned
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 31: Session metadata + Status combined
**Purpose:** Verify combining session metadata with status filter.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with session metadata of developer=Sam and status Reported
```

**Expected Result:** 1 vulnerability returned
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 32: Session metadata + Status combined (non-matching)
**Purpose:** Verify combined filters that produce no results.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with session metadata of developer=Sam and status Suspicious
```

**Expected Result:** 0 vulnerabilities returned (empty list) - Sam's vuln is Reported, not Suspicious

---

### Test 33: Triple filter combination
**Purpose:** Verify combining three different filter types.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with severity NOTE, status Reported, and type log-injection
```

**Expected Result:** 1 vulnerability returned
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

## Edge Case Tests

### Test 34: No filters (default behavior)
**Purpose:** Verify default behavior returns actionable vulnerabilities with warning.

**Prompt:**
```
use contrast mcp to get all vulnerabilities from the DemoRouteSession app
```

**Expected Result:** 2 vulnerabilities returned with warning about excluding Fixed/Remediated
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)
- LPZ0-A2BF-SKL9-UBWO (log-injection)

---

### Test 35: Multiple non-matching filters
**Purpose:** Verify that combined non-matching filters return empty results.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with severity CRITICAL in PRODUCTION environment
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

## Vulnerability Tags Filter Tests

### Test 36: Vulnerability tags filter (matching)
**Purpose:** Verify filtering by vulnerability tags that exist on the SQL injection vuln.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with vulnerability tag "VulnTagBulk 0uh"
```

**Expected Result:** 1 vulnerability returned
- 0VOL-VW0M-Y0C1-Q97K (sql-injection)

---

### Test 37: Vulnerability tags filter (non-matching)
**Purpose:** Verify filtering by a tag that doesn't exist.

**Prompt:**
```
use contrast mcp to get the vulnerabilities from the DemoRouteSession app with vulnerability tag "NonExistentTag"
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

## Summary

| Test # | Category | Filter Type | Expected Count |
|--------|----------|-------------|----------------|
| 1 | Severity | NOTE | 2 |
| 2 | Severity | CRITICAL | 0 |
| 3 | Severity | CRITICAL,NOTE | 2 |
| 4 | Severity | note (lowercase) | 2 |
| 5 | Status | Suspicious | 1 |
| 6 | Status | Confirmed | 0 |
| 7 | Status | All statuses | 2 |
| 8 | Environment | DEVELOPMENT | 2 |
| 9 | Environment | PRODUCTION | 0 |
| 10 | Environment | DEVELOPMENT,PRODUCTION | 2 |
| 11 | VulnType | sql-injection | 1 |
| 12 | VulnType | log-injection | 1 |
| 13 | VulnType | sql-injection,log-injection | 2 |
| 14 | VulnType | xss-reflected | 0 |
| 15 | Session Metadata | developer=John | 0 |
| 16 | Session Metadata | developer=ellen (case-sensitive) | 0 |
| 17 | Session Metadata | DEVELOPER=Ellen | 1 |
| 18 | Session Metadata | commit=[100,200] | 2 |
| 19 | Session Metadata | developer=Ellen,commit=200 | 0 |
| 20 | Session Metadata | developer=[Ellen,Sam],repo=TS | 2 |
| 21 | Date | lastSeenAfter 2023-01-01 | 2 |
| 22 | Date | lastSeenAfter 2024-01-01 | 0 |
| 23 | Date | lastSeenBefore 2024-01-01 | 2 |
| 24 | Date | lastSeenBefore 2023-01-01 | 0 |
| 25 | Date | Range 2023-03 | 2 |
| 26 | Latest Session | useLatestSession=true | varies |
| 27 | Pagination | pageSize=1 | 1 |
| 28 | Pagination | page=2, pageSize=1 | 1 |
| 29 | Combined | NOTE + Suspicious | 1 |
| 30 | Combined | DEVELOPMENT + log-injection | 1 |
| 31 | Combined | developer=Sam + Reported | 1 |
| 32 | Combined | developer=Sam + Suspicious | 0 |
| 33 | Combined | NOTE + Reported + log-injection | 1 |
| 34 | Edge Case | No filters | 2 |
| 35 | Edge Case | CRITICAL + PRODUCTION | 0 |
| 36 | VulnTags | VulnTagBulk 0uh | 1 |
| 37 | VulnTags | NonExistentTag | 0 |

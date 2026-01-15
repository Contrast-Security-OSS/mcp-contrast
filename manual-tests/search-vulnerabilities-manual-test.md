# Test Cases for `search_vulnerabilities` Tool

## Overview

The `search_vulnerabilities` tool is an **organization-level** search that queries vulnerabilities across ALL applications. Unlike `search_app_vulnerabilities`, it does NOT support:
- `appId` parameter (searches all apps)
- `sessionMetadataFilters` (no session-based filtering)
- `useLatestSession` (no latest session filtering)

**Supported parameters:**
- `page`, `pageSize` - Pagination
- `severities` - CRITICAL, HIGH, MEDIUM, LOW, NOTE
- `statuses` - Reported, Suspicious, Confirmed, Remediated, Fixed
- `vulnTypes` - sql-injection, cmd-injection, path-traversal, etc.
- `environments` - DEVELOPMENT, QA, PRODUCTION
- `lastSeenAfter`, `lastSeenBefore` - Date filters
- `vulnTags` - Vulnerability-level tags

---

## Known Test Data (Sample)

The organization has ~9,947 total actionable vulnerabilities across multiple applications. Below are representative samples:

| Vuln ID | Type | Severity | Status | Environment | App Name | Tags |
|---------|------|----------|--------|-------------|----------|------|
| B1LP-8GPZ-GQRP-0V6C | sql-injection | Critical | Reported | DEVELOPMENT | WebGoat | TestDemo, rsmya |
| 0X7L-VAL3-2RCU-3RED | sql-injection | Critical | Reported | QA | Harshaa-...-dataservice | |
| 7853-XTL7-27CK-CE22 | sql-injection | Critical | Reported | PRODUCTION | harshaa-...-dataservice | |
| BVQS-FE0F-PO4T-EFKG | jndi-injection | Critical | Reported | DEVELOPMENT | thib-...-frontgateservice | |
| PLWD-X4U6-KQEH-K290 | cmd-injection | High | Reported | QA | Harshaa-...-webhookservice | |
| 4BKG-MQ3Z-XPL2-T1FD | cmd-injection | High | Reported | PRODUCTION | harshaa-...-webhookservice | |
| CM17-QW2T-GBQU-0MT7 | path-traversal | High | Reported | QA | thib-...-imageservice | |
| V7WW-D584-CGJD-QVI2 | crypto-bad-mac | Medium | Reported | QA | Chris-...-frontgateservice | |
| MLRZ-YHKL-QADD-9GPP | csp-header-missing | Note | Suspicious | PRODUCTION | harshaa-...-frontgateservice | |
| W0D6-RBYD-5DJY-IYN5 | crypto-weak-randomness | High | Suspicious | DEVELOPMENT, QA | WebGoat | VulnTagBulk... |

---

## Severity Filter Tests

### Test 1: Single severity filter (CRITICAL)
**Purpose:** Verify filtering by CRITICAL severity returns only critical vulnerabilities.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity CRITICAL
```

**Expected Result:** Multiple vulnerabilities returned, all with severity "Critical"
- Should include sql-injection, jndi-injection types
- Examples: B1LP-8GPZ-GQRP-0V6C, 0X7L-VAL3-2RCU-3RED, BVQS-FE0F-PO4T-EFKG

---

### Test 2: Single severity filter (HIGH)
**Purpose:** Verify filtering by HIGH severity.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity HIGH
```

**Expected Result:** Multiple vulnerabilities returned, all with severity "High"
- Should include cmd-injection, path-traversal, untrusted-deserialization types
- Examples: PLWD-X4U6-KQEH-K290, CM17-QW2T-GBQU-0MT7

---

### Test 3: Multiple severities filter
**Purpose:** Verify comma-separated severities use OR logic.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity CRITICAL or HIGH
```

**Expected Result:** Vulnerabilities with severity "Critical" OR "High"
- Should include both sql-injection (Critical) and cmd-injection (High) types

---

### Test 4: Severity case insensitivity
**Purpose:** Verify severity filter is case-insensitive.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity critical
```

**Expected Result:** Same results as Test 1 - severity filter should be case-insensitive

---

### Test 5: Single severity filter (NOTE)
**Purpose:** Verify filtering by NOTE (lowest) severity.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity NOTE
```

**Expected Result:** Multiple vulnerabilities with severity "Note"
- Should include csp-header-missing, autocomplete-missing, clickjacking-control-missing types

---

### Test 6: Single severity filter (MEDIUM)
**Purpose:** Verify filtering by MEDIUM severity.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity MEDIUM
```

**Expected Result:** Vulnerabilities with severity "Medium"
- Should include crypto-bad-mac, cookie-flags-missing types
- Example: V7WW-D584-CGJD-QVI2

---

## Status Filter Tests

### Test 7: Single status filter (Reported)
**Purpose:** Verify filtering by Reported status.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with status Reported
```

**Expected Result:** Vulnerabilities with status "Reported" only
- Most vulnerabilities should match this filter

---

### Test 8: Single status filter (Suspicious)
**Purpose:** Verify filtering by Suspicious status.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with status Suspicious
```

**Expected Result:** Vulnerabilities with status "Suspicious" only (~27 total)
- Examples: MLRZ-YHKL-QADD-9GPP, W0D6-RBYD-5DJY-IYN5

---

### Test 9: Single status filter (Confirmed)
**Purpose:** Verify filtering by Confirmed status.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with status Confirmed
```

**Expected Result:** Vulnerabilities with status "Confirmed" (may be 0 or more depending on data)

---

### Test 10: Multiple statuses filter
**Purpose:** Verify comma-separated statuses use OR logic.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with status Reported or Suspicious
```

**Expected Result:** Vulnerabilities with status "Reported" OR "Suspicious"

---

### Test 11: Include Fixed and Remediated statuses
**Purpose:** Verify that Fixed and Remediated statuses can be explicitly included.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with status Reported, Suspicious, Confirmed, Remediated, or Fixed
```

**Expected Result:** All vulnerabilities regardless of status (no warning about excluding Fixed/Remediated)

---

### Test 12: Status case insensitivity
**Purpose:** Verify status filter is case-insensitive.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with status REPORTED
```

**Expected Result:** Same results as Test 7 - status filter should be case-insensitive

---

## Environment Filter Tests

### Test 13: Single environment filter (DEVELOPMENT)
**Purpose:** Verify filtering by DEVELOPMENT environment.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization in DEVELOPMENT environment
```

**Expected Result:** Vulnerabilities that have been seen in DEVELOPMENT environment
- Examples: B1LP-8GPZ-GQRP-0V6C (WebGoat), 8F0Z-KYZZ-JWZ6-G9WA, BVQS-FE0F-PO4T-EFKG

---

### Test 14: Single environment filter (QA)
**Purpose:** Verify filtering by QA environment.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization in QA environment
```

**Expected Result:** Vulnerabilities that have been seen in QA environment
- Examples: 0X7L-VAL3-2RCU-3RED, PLWD-X4U6-KQEH-K290, CM17-QW2T-GBQU-0MT7

---

### Test 15: Single environment filter (PRODUCTION)
**Purpose:** Verify filtering by PRODUCTION environment.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization in PRODUCTION environment
```

**Expected Result:** Vulnerabilities that have been seen in PRODUCTION environment (~91 total)
- Examples: 7853-XTL7-27CK-CE22, 4BKG-MQ3Z-XPL2-T1FD, MLRZ-YHKL-QADD-9GPP

---

### Test 16: Multiple environments filter
**Purpose:** Verify comma-separated environments use OR logic.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization in DEVELOPMENT or PRODUCTION environments
```

**Expected Result:** Vulnerabilities seen in DEVELOPMENT OR PRODUCTION

---

## Vulnerability Type Filter Tests

### Test 17: Single vulnerability type (sql-injection)
**Purpose:** Verify filtering by sql-injection type.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with type sql-injection
```

**Expected Result:** SQL injection vulnerabilities only (~213 total)
- Examples: B1LP-8GPZ-GQRP-0V6C, 0X7L-VAL3-2RCU-3RED, 7853-XTL7-27CK-CE22

---

### Test 18: Single vulnerability type (cmd-injection)
**Purpose:** Verify filtering by cmd-injection type.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with type cmd-injection
```

**Expected Result:** Command injection vulnerabilities only
- Examples: PLWD-X4U6-KQEH-K290, 4BKG-MQ3Z-XPL2-T1FD

---

### Test 19: Single vulnerability type (path-traversal)
**Purpose:** Verify filtering by path-traversal type.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with type path-traversal
```

**Expected Result:** Path traversal vulnerabilities only
- Example: CM17-QW2T-GBQU-0MT7

---

### Test 20: Multiple vulnerability types
**Purpose:** Verify comma-separated vulnerability types use OR logic.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with type sql-injection or cmd-injection
```

**Expected Result:** Vulnerabilities of type sql-injection OR cmd-injection

---

### Test 21: Non-matching vulnerability type
**Purpose:** Verify filtering by a type that may have no results.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with type xxe
```

**Expected Result:** XXE vulnerabilities if they exist, otherwise empty list

---

## Date Filter Tests

### Test 22: lastSeenAfter filter (recent activity)
**Purpose:** Verify filtering vulnerabilities with recent activity.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization last seen after 2026-01-01
```

**Expected Result:** Vulnerabilities with lastSeenAt after January 1, 2026
- Should include recently active vulnerabilities

---

### Test 23: lastSeenAfter filter (older date)
**Purpose:** Verify filtering vulnerabilities seen after an older date.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization last seen after 2025-01-01
```

**Expected Result:** Most vulnerabilities should match (seen in 2025 or later)

---

### Test 24: lastSeenAfter filter (future date - no matches)
**Purpose:** Verify filtering with a future date returns no results.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization last seen after 2027-01-01
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

### Test 25: lastSeenBefore filter
**Purpose:** Verify filtering vulnerabilities seen before a specific date.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization last seen before 2025-01-01
```

**Expected Result:** Vulnerabilities with lastSeenAt before January 1, 2025 (older vulnerabilities)

---

### Test 26: Date range filter
**Purpose:** Verify filtering with both lastSeenAfter and lastSeenBefore.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization last seen between 2025-12-01 and 2025-12-31
```

**Expected Result:** Vulnerabilities with activity in December 2025

---

## Vulnerability Tags Filter Tests

### Test 27: Vulnerability tags filter (matching)
**Purpose:** Verify filtering by vulnerability tags.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with vulnerability tag "TestDemo"
```

**Expected Result:** 1 vulnerability returned
- B1LP-8GPZ-GQRP-0V6C (sql-injection in WebGoat)

---

### Test 28: Vulnerability tags filter (non-matching)
**Purpose:** Verify filtering by a tag that doesn't exist.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with vulnerability tag "NonExistentTag123"
```

**Expected Result:** 0 vulnerabilities returned (empty list)

---

### Test 29: Multiple vulnerability tags
**Purpose:** Verify filtering by multiple tags.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with vulnerability tags "TestDemo" or "rsmya"
```

**Expected Result:** Vulnerabilities with either tag
- B1LP-8GPZ-GQRP-0V6C has both tags

---

## Pagination Tests

### Test 30: Pagination with pageSize=5
**Purpose:** Verify pagination returns limited results with hasMorePages indicator.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with page size 5
```

**Expected Result:** 5 vulnerabilities returned, `hasMorePages: true`, `totalItems` should be large (~9947)

---

### Test 31: Pagination page 2
**Purpose:** Verify retrieving the second page of results.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with page size 5 and page 2
```

**Expected Result:** 5 different vulnerabilities (page 2 of results)

---

### Test 32: Large page size
**Purpose:** Verify maximum page size handling.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with page size 100
```

**Expected Result:** Up to 100 vulnerabilities returned (max allowed)

---

## Combined Filter Tests

### Test 33: Severity + Environment combined
**Purpose:** Verify combining severity and environment filters (AND logic).

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity CRITICAL in PRODUCTION environment
```

**Expected Result:** Critical vulnerabilities in PRODUCTION only
- Example: 7853-XTL7-27CK-CE22 (sql-injection)

---

### Test 34: Severity + Status combined
**Purpose:** Verify combining severity and status filters.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity HIGH and status Suspicious
```

**Expected Result:** High severity vulnerabilities with Suspicious status
- Example: W0D6-RBYD-5DJY-IYN5 (crypto-weak-randomness)

---

### Test 35: Environment + Vulnerability type combined
**Purpose:** Verify combining environment and vulnerability type filters.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization in DEVELOPMENT environment with type sql-injection
```

**Expected Result:** SQL injection vulnerabilities in DEVELOPMENT environment
- Examples: B1LP-8GPZ-GQRP-0V6C, 8F0Z-KYZZ-JWZ6-G9WA

---

### Test 36: Triple filter combination
**Purpose:** Verify combining three different filter types.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity CRITICAL, status Reported, and type sql-injection
```

**Expected Result:** Critical SQL injection vulnerabilities with Reported status
- Examples: B1LP-8GPZ-GQRP-0V6C, 0X7L-VAL3-2RCU-3RED

---

### Test 37: Severity + Environment + Date combined
**Purpose:** Verify combining severity, environment, and date filters.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity CRITICAL in QA environment last seen after 2025-12-01
```

**Expected Result:** Critical vulnerabilities in QA with recent activity
- Example: 0X7L-VAL3-2RCU-3RED

---

### Test 38: All filters combined
**Purpose:** Verify complex multi-filter query.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity CRITICAL, status Reported, type sql-injection, in QA environment, last seen after 2025-12-01
```

**Expected Result:** Narrow result set matching all criteria

---

## Edge Case Tests

### Test 39: No filters (default behavior)
**Purpose:** Verify default behavior returns actionable vulnerabilities with warning.

**Prompt:**
```
use contrast mcp to search for all vulnerabilities across the organization
```

**Expected Result:** Many vulnerabilities returned (~9947) with warning about excluding Fixed/Remediated
- Default statuses: Reported, Suspicious, Confirmed

---

### Test 40: Multiple non-matching filters
**Purpose:** Verify that combined non-matching filters return empty results.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with severity LOW in PRODUCTION environment with type xxe
```

**Expected Result:** 0 or very few vulnerabilities (filters are very restrictive)

---

### Test 41: Cross-application results verification
**Purpose:** Verify that results come from multiple applications (org-wide search).

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with type sql-injection and page size 10
```

**Expected Result:** SQL injection vulnerabilities from DIFFERENT applications
- Should see different appName values: WebGoat, thib-dataservice, harshaa-dataservice, etc.
- This confirms org-wide search (not app-specific)

---

## Comparison with search_app_vulnerabilities

### Test 42: Verify no appId parameter
**Purpose:** Confirm that appId is NOT a valid parameter for this tool.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization from app 9e18a607-2b01-41c7-b35b-52a256840fea
```

**Expected Result:** Either:
- Tool ignores appId and returns org-wide results, OR
- Error indicating appId is not a valid parameter
- Should NOT filter to single app like search_app_vulnerabilities does

---

### Test 43: Verify no session metadata filtering
**Purpose:** Confirm that session metadata filtering is NOT available.

**Prompt:**
```
use contrast mcp to search for vulnerabilities across the organization with session metadata developer=Ellen
```

**Expected Result:** Either:
- Tool ignores session metadata filter and returns all org results, OR
- Error indicating sessionMetadataFilters is not supported
- For session filtering, use search_app_vulnerabilities instead

---

## Summary

| Test # | Category | Filter Type | Expected Behavior |
|--------|----------|-------------|-------------------|
| 1 | Severity | CRITICAL | Returns critical vulns only |
| 2 | Severity | HIGH | Returns high vulns only |
| 3 | Severity | CRITICAL,HIGH | OR logic |
| 4 | Severity | critical (lowercase) | Case-insensitive |
| 5 | Severity | NOTE | Returns note vulns only |
| 6 | Severity | MEDIUM | Returns medium vulns only |
| 7 | Status | Reported | Returns reported vulns |
| 8 | Status | Suspicious | Returns suspicious vulns (~27) |
| 9 | Status | Confirmed | Returns confirmed vulns |
| 10 | Status | Reported,Suspicious | OR logic |
| 11 | Status | All statuses | No exclusion warning |
| 12 | Status | REPORTED (uppercase) | Case-insensitive |
| 13 | Environment | DEVELOPMENT | Returns dev vulns |
| 14 | Environment | QA | Returns QA vulns |
| 15 | Environment | PRODUCTION | Returns prod vulns (~91) |
| 16 | Environment | DEVELOPMENT,PRODUCTION | OR logic |
| 17 | VulnType | sql-injection | Returns SQLi vulns (~213) |
| 18 | VulnType | cmd-injection | Returns cmd-injection vulns |
| 19 | VulnType | path-traversal | Returns path-traversal vulns |
| 20 | VulnType | sql-injection,cmd-injection | OR logic |
| 21 | VulnType | xxe | May return empty |
| 22 | Date | lastSeenAfter 2026-01-01 | Recent vulns only |
| 23 | Date | lastSeenAfter 2025-01-01 | Most vulns match |
| 24 | Date | lastSeenAfter 2027-01-01 | Empty (future date) |
| 25 | Date | lastSeenBefore 2025-01-01 | Older vulns only |
| 26 | Date | Range Dec 2025 | Activity in date range |
| 27 | VulnTags | TestDemo | 1 vuln (B1LP...) |
| 28 | VulnTags | NonExistentTag | Empty |
| 29 | VulnTags | TestDemo,rsmya | Vulns with either tag |
| 30 | Pagination | pageSize=5 | 5 results, hasMorePages=true |
| 31 | Pagination | page=2, pageSize=5 | Next 5 results |
| 32 | Pagination | pageSize=100 | Max 100 results |
| 33 | Combined | CRITICAL + PRODUCTION | AND logic |
| 34 | Combined | HIGH + Suspicious | AND logic |
| 35 | Combined | DEVELOPMENT + sql-injection | AND logic |
| 36 | Combined | CRITICAL + Reported + sql-injection | AND logic |
| 37 | Combined | CRITICAL + QA + date | AND logic |
| 38 | Combined | All filters | Narrow result set |
| 39 | Edge Case | No filters | Default behavior, warning |
| 40 | Edge Case | Non-matching combo | Empty or few results |
| 41 | Edge Case | Cross-app verification | Multiple apps in results |
| 42 | Comparison | appId param | Not supported |
| 43 | Comparison | sessionMetadata | Not supported |

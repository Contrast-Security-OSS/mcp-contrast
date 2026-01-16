# Test Cases for `list_application_libraries` Tool

## Overview

The `list_application_libraries` tool retrieves all third-party libraries used by a specific application. It returns library metadata including version information, vulnerability data (CVEs), class usage statistics, and security grades.

**Supported parameters:**
- `appId` (required) - Application ID (use search_applications to find)
- `page`, `pageSize` - Pagination (max 50 per page)

**Key response fields per library:**
- `filename` - Library file name (e.g., "log4j-core-2.17.1.jar")
- `version` - Current version in use
- `hash` - Unique library identifier
- `classCount` - Total classes in the library
- `classesUsed` - Classes actually loaded by the application
- `totalVulnerabilities` - Total CVE count
- `criticalVulnerabilities` - CRITICAL severity CVE count only
- `highVulnerabilities` - HIGH severity CVE count only (does not include CRITICAL)
- `vulnerabilities` - List of CVE details
- `grade` - Security grade (A, B, C, D, F)
- `monthsOutdated` - Months since latest version
- `latestVersion` - Latest available version

---

## Known Test Data

### Test Applications

| App Name | App ID | Library Count | Language |
|----------|--------|---------------|----------|
| webgoat-t1 | 1d5cdd44-19b9-44df-88b1-ad02c2f88826 | 154 | Java |
| spring-petclinic-live-example | 7949c260-6ae9-477f-970a-60d8f95a6f3c | 83 | Java |
| thib-contrast-cargo-cats-frontgateservice | 03c0a8d2-a6e6-46aa-b602-f242811d10bf | 44 | Java |

### Sample Libraries (from webgoat-t1)

> **Note:** `monthsOutdated` values increase over time. Values shown are approximate as of test creation.

| Filename | Version | Grade | Vulns | High Vulns | Classes Used | Months Outdated |
|----------|---------|-------|-------|------------|--------------|-----------------|
| tomcat-embed-core-10.1.39.jar | 10.1.39 | F | 6 | 5 | 384 | ~8 |
| spring-web-6.2.5.jar | 6.2.5 | F | 1 | 0 | 294 | ~8 |
| thymeleaf-3.1.2.release.jar | 3.1.2.RELEASE | B | 0 | 0 | 377 | >12 |
| unbescape-1.1.6.release.jar | 1.1.6.RELEASE | A | 0 | 0 | 7 | 0 |
| xstream-1.4.5.jar | 1.4.5 | F | 0 | 0 | 0 | >100 |
| wiremock-standalone-3.12.1.jar | 3.12.1 | F | 0 | 0 | 0 | ~5 |

### Sample Vulnerable Libraries (from spring-petclinic-live-example)

| Filename | CVE Count | Critical CVEs | High CVEs | Key CVEs |
|----------|-----------|---------------|-----------|----------|
| tomcat-embed-core-10.1.12.jar | 16 | 7 | 9 | CVE-2024-50379, CVE-2025-24813, CVE-2024-56337 |
| spring-security-core-6.1.3.jar | 3 | 1 | 2 | CVE-2024-22257, CVE-2024-22234 |
| spring-security-crypto-6.1.3.jar | 1 | 0 | 1 | CVE-2025-22228 |

---

## Basic Functionality Tests

### Test 1: Retrieve libraries for valid application
**Purpose:** Verify the tool returns libraries for a valid appId.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826
```

**Expected Result:**
- Returns library list with 154 total items
- First page contains up to 50 libraries
- Each library has filename, version, hash, grade fields
- `hasMorePages: true` (since 154 > 50)

---

### Test 2: Retrieve libraries using application name workflow
**Purpose:** Verify the search_applications → list_application_libraries workflow.

**Prompt:**
```
use contrast mcp to search for the application named "webgoat-t1" and then list its libraries
```

**Expected Result:**
- First finds app with appId `1d5cdd44-19b9-44df-88b1-ad02c2f88826`
- Then returns libraries for that application
- Libraries include Spring, Tomcat, Thymeleaf frameworks

---

### Test 3: Application with fewer libraries
**Purpose:** Verify behavior with smaller library count.

**Prompt:**
```
use contrast mcp to list libraries for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf
```

**Expected Result:**
- Returns ~44 libraries
- Single page result (44 < 50)
- `hasMorePages: false` or minimal pagination

---

## Vulnerability Tests

### Test 4: Identify libraries with vulnerabilities
**Purpose:** Find libraries with known CVEs.

**Prompt:**
```
use contrast mcp to list libraries for application 7949c260-6ae9-477f-970a-60d8f95a6f3c
```

**Expected Result:**
- tomcat-embed-core-10.1.12.jar has `totalVulnerabilities: 16`, `highVulnerabilities: 9`
- spring-security-core-6.1.3.jar has `totalVulnerabilities: 3`, `highVulnerabilities: 2`
- spring-security-crypto-6.1.3.jar has `totalVulnerabilities: 1`, `highVulnerabilities: 1`
- Vulnerability details include CVE names, descriptions, severity codes

---

### Test 5: Verify vulnerability severity distribution
**Purpose:** Confirm `highVulnerabilities` counts HIGH severity CVEs only (not CRITICAL).

**Prompt:**
```
use contrast mcp to list libraries for application 7949c260-6ae9-477f-970a-60d8f95a6f3c and examine the tomcat-embed-core library vulnerabilities
```

**Expected Result:**
- tomcat-embed-core-10.1.12.jar vulnerabilities include:
  - CVE-2024-50379 (CRITICAL)
  - CVE-2025-31651 (CRITICAL)
  - CVE-2025-24813 (CRITICAL)
  - CVE-2024-56337 (CRITICAL)
  - Multiple HIGH severity CVEs
- `criticalVulnerabilities` counts CRITICAL severity CVEs only
- `highVulnerabilities` counts only HIGH severity CVEs (not CRITICAL)
- `totalVulnerabilities` = `criticalVulnerabilities` + `highVulnerabilities` + other severities

---

### Test 6: Find high-risk libraries (used + vulnerable)
**Purpose:** Identify libraries that are both actively used AND have critical vulnerabilities.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and identify which vulnerable libraries have classes being used
```

**Expected Result:**
- tomcat-embed-core-10.1.39.jar: `classesUsed: 384`, `highVulnerabilities: 5` → HIGH RISK
- spring-web-6.2.5.jar: `classesUsed: 294`, `totalVulnerabilities: 1` → MEDIUM RISK
- These should be prioritized for remediation

---

## Class Usage Tests

### Test 7: Libraries with zero class usage (transitive dependencies)
**Purpose:** Identify unused libraries that are likely transitive dependencies.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and find libraries with zero classes used
```

**Expected Result:**
- xstream-1.4.5.jar: `classCount: 414`, `classesUsed: 0`
- xpp3_min-1.1.4c.jar: `classCount: 3`, `classesUsed: 0`
- wiremock-standalone-3.12.1.jar: `classCount: 9590`, `classesUsed: 0`
- These are lower risk even if they have vulnerabilities

---

### Test 8: Libraries with active class usage
**Purpose:** Identify actively used libraries.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and find libraries with high class usage
```

**Expected Result:**
- thymeleaf-3.1.2.release.jar: `classCount: 578`, `classesUsed: 377` (65% usage)
- tomcat-embed-core-10.1.39.jar: `classCount: 1524`, `classesUsed: 384` (25% usage)
- spring-web-6.2.5.jar: `classCount: 1240`, `classesUsed: 294` (24% usage)

---

### Test 9: Verify classesUsed <= classCount invariant
**Purpose:** Confirm class usage logic is consistent.

**Prompt:**
```
use contrast mcp to list all libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and verify no library has classesUsed greater than classCount
```

**Expected Result:**
- For ALL libraries: `classesUsed <= classCount`
- No negative values for either field
- Class counts are non-negative integers

---

## Library Grade Tests

### Test 10: Grade A libraries (current, secure)
**Purpose:** Identify well-maintained libraries with good security grades.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and find libraries with grade A
```

**Expected Result:**
- unbescape-1.1.6.release.jar: `grade: "A"`, `monthsOutdated: 0`
- thymeleaf-extras-springsecurity6-3.1.3.release.jar: `grade: "A"`, `monthsOutdated: 0`
- angus-activation-2.0.2.jar: `grade: "A"`, `monthsOutdated: 0`
- Grade A libraries should have `monthsOutdated: 0` and no vulnerabilities

---

### Test 11: Grade F libraries (outdated, insecure)
**Purpose:** Identify libraries needing immediate attention.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and find libraries with grade F
```

**Expected Result:**
- tomcat-embed-core-10.1.39.jar: `grade: "F"`, `monthsOutdated` > 0
- xstream-1.4.5.jar: `grade: "F"`, `monthsOutdated` > 100 (severely outdated)
- spring-webmvc-6.2.5.jar: `grade: "F"`, `monthsOutdated` > 0
- Grade F indicates significant outdatedness or security concerns

---

### Test 12: Grade distribution across application
**Purpose:** Get overview of library health.

**Prompt:**
```
use contrast mcp to list all libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and summarize the grade distribution
```

**Expected Result:**
- Mix of grades A, B, C, D, F across libraries
- Grade correlates with `monthsOutdated` and vulnerability count
- Can calculate overall application library health score

---

## Outdated Library Tests

### Test 13: Severely outdated libraries
**Purpose:** Find libraries that are significantly behind latest version.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and find libraries more than 12 months outdated
```

**Expected Result:**
- xstream-1.4.5.jar: `monthsOutdated` > 100, `latestVersion` newer than "1.4.5"
- xmlpull-1.1.3.1.jar: `monthsOutdated` > 40
- thymeleaf-3.1.2.release.jar: `monthsOutdated` > 12

> **Note:** Exact `monthsOutdated` values increase over time as libraries age.

---

### Test 14: Current libraries (not outdated)
**Purpose:** Identify libraries that are up to date.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and find libraries with monthsOutdated equal to 0
```

**Expected Result:**
- unbescape-1.1.6.release.jar: `monthsOutdated: 0`, `version` equals `latestVersion`
- angus-activation-2.0.2.jar: `monthsOutdated: 0`
- These libraries are current and don't need version upgrades

---

### Test 15: Version comparison
**Purpose:** Verify version and latestVersion fields are populated correctly.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and compare version vs latestVersion for outdated libraries
```

**Expected Result:**
- xstream-1.4.5.jar: `version: "1.4.5"`, `latestVersion` > "1.4.5" (e.g., "1.4.21")
- txw2-4.0.5.jar: `version: "4.0.5"`, `latestVersion` > "4.0.5"
- When `monthsOutdated > 0`, version should differ from latestVersion

> **Note:** `latestVersion` values change as new library versions are released.

---

## Pagination Tests

### Test 16: Default pagination
**Purpose:** Verify default page and pageSize behavior.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826
```

**Expected Result:**
- `page: 1`
- `pageSize: 50`
- Returns up to 50 libraries
- `totalItems: 154`
- `hasMorePages: true`

---

### Test 17: Custom page size
**Purpose:** Test pagination with smaller page size.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 with page size 10
```

**Expected Result:**
- Returns exactly 10 libraries
- `pageSize: 10`
- `hasMorePages: true`
- `totalItems: 154`

---

### Test 18: Second page of results
**Purpose:** Verify retrieving subsequent pages.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 with page 2 and page size 50
```

**Expected Result:**
- `page: 2`
- Returns different libraries than page 1
- Libraries 51-100 of total 154
- `hasMorePages: true`

---

### Test 19: Last page of results
**Purpose:** Verify behavior on final page.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 with page 4 and page size 50
```

**Expected Result:**
- `page: 4`
- Returns 4 libraries (154 - 150 = 4 remaining)
- `hasMorePages: false`
- Libraries include antlr4-runtime, angus-activation, etc.

---

### Test 20: Page beyond results
**Purpose:** Verify behavior when page exceeds data.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 with page 100 and page size 50
```

**Expected Result:**
- `items: []` (empty array)
- `totalItems: 154`
- `hasMorePages: false`
- No error (graceful handling)

---

### Test 21: Maximum page size limit
**Purpose:** Verify pageSize is capped at 50.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 with page size 100
```

**Expected Result:**
- `pageSize: 50` (capped, not 100)
- Warning message about exceeding maximum
- Returns 50 libraries

---

### Test 22: Invalid pagination values
**Purpose:** Test handling of invalid page values.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 with page 0
```

**Expected Result:**
- Either validation error OR defaults to page 1 with warning
- No crash or unexpected behavior

---

## Validation Tests

### Test 23: Missing appId parameter
**Purpose:** Verify error handling for missing required parameter.

**Prompt:**
```
use contrast mcp to list application libraries
```

**Expected Result:**
- Validation error returned
- Error message indicates appId is required
- No API call made

---

### Test 24: Invalid appId format
**Purpose:** Test behavior with malformed application ID.

**Prompt:**
```
use contrast mcp to list libraries for application "invalid-not-a-uuid"
```

**Expected Result:**
- Either validation error or API error
- Clear error message about invalid appId
- Graceful failure (no crash)

---

### Test 25: Non-existent application ID
**Purpose:** Verify behavior when appId doesn't exist.

**Prompt:**
```
use contrast mcp to list libraries for application 00000000-0000-0000-0000-000000000000
```

**Expected Result:**
- Error response or empty result with warning
- Appropriate error message about application not found
- No crash

---

## Integration Tests

### Test 26: Cross-reference with list_applications_by_cve
**Purpose:** Verify library data consistency with CVE lookup.

**Prompt:**
```
use contrast mcp to find applications affected by CVE-2021-44228 and then list the libraries for thib-contrast-cargo-cats-frontgateservice to find the log4j library
```

**Expected Result:**
- CVE lookup shows thib-contrast-cargo-cats-frontgateservice (03c0a8d2-a6e6-46aa-b602-f242811d10bf) is affected
- Library list includes log4j-core with CVE-2021-44228 vulnerability
- CVE data is consistent between tools

---

### Test 27: Multiple applications comparison
**Purpose:** Compare library profiles across different applications.

**Prompt:**
```
use contrast mcp to list libraries for webgoat-t1 and spring-petclinic-live-example and compare their vulnerability counts
```

**Expected Result:**
- webgoat-t1: 154 libraries, includes tomcat-embed-core-10.1.39.jar with 6 CVEs
- spring-petclinic: 83 libraries, includes tomcat-embed-core-10.1.12.jar with 16 CVEs
- Different Tomcat versions have different CVE counts

---

## Edge Cases

### Test 28: Application with many libraries (stress test)
**Purpose:** Verify pagination integrity with large library count.

**Prompt:**
```
use contrast mcp to list ALL libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 by paginating through all pages
```

**Expected Result:**
- Total collected equals 154 libraries
- No duplicate library hashes across pages
- Each page returns correct count (50, 50, 50, 4)

---

### Test 29: Library hash uniqueness
**Purpose:** Verify each library has unique identifier.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and check for duplicate hashes
```

**Expected Result:**
- All library hashes are unique
- Hash format is consistent (SHA-like string)
- No empty or null hashes

---

### Test 30: Response time for large application
**Purpose:** Verify acceptable performance.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 with page size 50
```

**Expected Result:**
- Response within reasonable time (< 5 seconds)
- `durationMs` field shows execution time
- No timeout errors

---

## Combined Scenario Tests

### Test 31: Security audit workflow
**Purpose:** Simulate a security audit finding high-risk libraries.

**Prompt:**
```
use contrast mcp to list libraries for application 7949c260-6ae9-477f-970a-60d8f95a6f3c and identify the top 3 highest risk libraries based on vulnerability count and class usage
```

**Expected Result:**
1. tomcat-embed-core-10.1.12.jar: 16 CVEs, 383 classes used → CRITICAL
2. spring-security-core-6.1.3.jar: 3 CVEs (2 HIGH), 103 classes used → HIGH
3. spring-security-crypto-6.1.3.jar: 1 CVE (HIGH), 20 classes used → HIGH

---

### Test 32: Dependency cleanup workflow
**Purpose:** Identify unused dependencies that could be removed.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and find libraries with 0 classesUsed that could potentially be removed
```

**Expected Result:**
- xstream-1.4.5.jar: 0 classes used (potential removal candidate)
- wiremock-standalone-3.12.1.jar: 0 classes used (likely test dependency)
- Review transitive dependencies before removal

---

### Test 33: Upgrade planning workflow
**Purpose:** Identify libraries needing version upgrades.

**Prompt:**
```
use contrast mcp to list libraries for application 1d5cdd44-19b9-44df-88b1-ad02c2f88826 and create an upgrade plan for libraries with monthsOutdated > 12
```

**Expected Result:**
- xstream-1.4.5.jar: upgrade from 1.4.5 to latest (>100 months behind)
- thymeleaf-3.1.2.release.jar: upgrade from 3.1.2.RELEASE to latest (>12 months behind)
- Prioritize based on vulnerability impact and class usage

> **Note:** Exact `monthsOutdated` values and `latestVersion` change over time.

---

## Summary

| Test # | Category | Description | Key Validation |
|--------|----------|-------------|----------------|
| 1 | Basic | Valid appId | Returns library list |
| 2 | Basic | Name workflow | search → list integration |
| 3 | Basic | Smaller app | Single page result |
| 4 | Vulnerability | Find CVEs | Vuln counts accurate |
| 5 | Vulnerability | Severity check | HIGH only (not CRITICAL) |
| 6 | Vulnerability | High risk | Used + vulnerable |
| 7 | Class Usage | Zero usage | Transitive deps |
| 8 | Class Usage | Active usage | Used libraries |
| 9 | Class Usage | Invariant | used <= count |
| 10 | Grade | Grade A | Current, secure |
| 11 | Grade | Grade F | Outdated, insecure |
| 12 | Grade | Distribution | Health overview |
| 13 | Outdated | Severely | >12 months behind |
| 14 | Outdated | Current | monthsOutdated=0 |
| 15 | Outdated | Version compare | version vs latest |
| 16 | Pagination | Default | page=1, size=50 |
| 17 | Pagination | Custom size | size=10 |
| 18 | Pagination | Page 2 | Different results |
| 19 | Pagination | Last page | hasMorePages=false |
| 20 | Pagination | Beyond data | Empty array |
| 21 | Pagination | Max size | Capped at 50 |
| 22 | Pagination | Invalid page | Graceful handling |
| 23 | Validation | Missing appId | Error returned |
| 24 | Validation | Invalid format | Error message |
| 25 | Validation | Non-existent | Not found error |
| 26 | Integration | CVE cross-ref | Data consistency |
| 27 | Integration | Multi-app | Compare profiles |
| 28 | Edge Case | Many libraries | Pagination integrity |
| 29 | Edge Case | Hash unique | No duplicates |
| 30 | Edge Case | Performance | < 5 seconds |
| 31 | Combined | Security audit | Risk prioritization |
| 32 | Combined | Cleanup | Unused removal |
| 33 | Combined | Upgrade plan | Version planning |

---

## Appendix: Response Structure Examples

### Successful Response
```json
{
  "items": [
    {
      "filename": "tomcat-embed-core-10.1.39.jar",
      "version": "10.1.39",
      "hash": "f6acead04214d5aaea82c2639392208df33b3abe",
      "classCount": 1524,
      "classesUsed": 384,
      "grade": "F",
      "totalVulnerabilities": 6,
      "criticalVulnerabilities": 1,
      "highVulnerabilities": 5,
      "vulnerabilities": [
        {
          "name": "CVE-2025-31651",
          "severityCode": "CRITICAL",
          "description": "Improper Neutralization..."
        }
      ],
      "monthsOutdated": 8,
      "latestVersion": "10.1.50",
      "appLanguage": "Java"
    }
  ],
  "page": 1,
  "pageSize": 50,
  "totalItems": 154,
  "hasMorePages": true,
  "errors": [],
  "warnings": [],
  "durationMs": 468,
  "success": true
}
```

### Empty Response (No Libraries)
```json
{
  "items": [],
  "page": 1,
  "pageSize": 50,
  "totalItems": 0,
  "hasMorePages": false,
  "errors": [],
  "warnings": ["No libraries found for this application..."],
  "durationMs": 42,
  "success": true
}
```

### Error Response
```json
{
  "items": [],
  "page": 1,
  "pageSize": 50,
  "totalItems": 0,
  "hasMorePages": false,
  "errors": ["appId is required"],
  "warnings": [],
  "durationMs": null,
  "success": false
}
```

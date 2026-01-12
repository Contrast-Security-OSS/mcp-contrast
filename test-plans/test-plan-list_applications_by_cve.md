# Test Plan: list_applications_by_cve Tool

## Overview

This test plan provides comprehensive testing guidance for the `list_applications_by_cve` MCP tool. This tool queries applications vulnerable to a specific CVE and returns detailed vulnerability information including affected applications, libraries, and enriched class usage data.

### Migration Notes

**This plan replaces:**
- `test-plan-list_applications_vulnerable_to_cve.md` (original at root level)

**Key Changes from Original Tool:**
- **Tool renamed**: `list_applications_vulnerable_to_cve` → `list_applications_by_cve`
- **Follows tool-per-class pattern**: Uses `SingleTool<ListApplicationsByCveParams, CveData>`
- **Enhanced validation**: Uses `ListApplicationsByCveParams` with CVE format validation
- **Improved class usage enrichment**: Better null handling and warnings for enrichment failures

### Tool Signature

**MCP Tool Name:** `list_applications_by_cve`

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `cveId` | String | Yes | CVE identifier (e.g., CVE-2021-44228) |

**Validation:**
- CVE format: Must match pattern `CVE-YYYY-NNNNN` (4-digit year, 4+ digit sequence)
- Examples: `CVE-2021-44228`, `CVE-2023-123456`

### Response Structure

**Returns:** `SingleToolResponse<CveData>`

```java
SingleToolResponse {
    CveData data,      // CVE details, affected apps, libraries
    String message,    // Warnings or info messages
    boolean found      // True if data returned
}

// CveData structure:
CveData {
    Cve cve,                    // CVE details (name, score, description)
    ImpactStats impactStats,    // Impact statistics
    List<Library> libraries,    // Vulnerable library versions
    List<App> apps,             // Affected applications
    List<Server> servers        // Affected servers
}

// App structure (with enrichment):
App {
    String name,
    String app_id,
    long last_seen,
    long first_seen,
    String importance_description,
    int classCount,     // Total classes in vulnerable library
    int classUsage      // Classes actually used (0 = likely not exploitable)
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **Class Usage Interpretation** | `classUsage = 0` means library code likely NOT being executed |
| **Exploitability Indicator** | Prioritize apps with `classUsage > 0` for remediation |
| **Null Handling** | Empty lists return gracefully with warnings |
| **Enrichment Failures** | Logged and reported as warnings, don't block response |

---

## 1. Basic Functionality Tests

### Test Case 1.1: Valid CVE Query - Success

**Objective:** Verify tool successfully retrieves data for a known CVE.

**Prerequisites:**
- Environment with applications using libraries with known CVEs
- At least one high-profile CVE present (e.g., Log4Shell, Spring4Shell)

**Test Steps:**
1. Call `list_applications_by_cve(cveId="CVE-2021-44228")`
2. Examine the response

**Expected Results:**
- Response status: success
- `data` contains CveData object
- `data.cve.name` equals requested CVE
- `data.apps` list populated (if CVE affects apps)
- `data.libraries` list populated
- `found: true`

---

### Test Case 1.2: CveData Structure Verification

**Objective:** Verify all CveData fields are populated correctly.

**Test Steps:**
1. Query a known CVE affecting applications
2. Verify each section of CveData

**Expected Results:**

**cve object contains:**
- `name`: CVE ID string (matches request)
- `description`: Text description of vulnerability
- `score`: CVSS score (0.0-10.0)
- `availabilityImpact`, `confidentialityImpact`, `integrityImpact`: Impact ratings
- `accessVector`, `accessComplexity`: Access information
- `references`: List of reference URLs

**impactStats object contains:**
- `impactedAppCount`: Number of affected applications
- `totalAppCount`: Total applications in organization
- `impactedServerCount`: Number of affected servers
- `appPercentage`, `serverPercentage`: Percentage calculations

**libraries list contains:**
- Each Library has: `hash`, `version`, `file_name`, `group`

**apps list contains:**
- Each App has: `name`, `app_id`, enriched `classCount` and `classUsage`

---

### Test Case 1.3: Class Usage Enrichment

**Objective:** Verify classCount and classUsage fields are correctly enriched.

**Prerequisites:**
- CVE affecting applications with class usage data available

**Test Steps:**
1. Call tool with CVE affecting multiple applications
2. Examine each App object in the response
3. Check `classCount` and `classUsage` fields

**Expected Results:**
- For apps using vulnerable library classes: `classUsage > 0`
- `classCount` reflects total classes in vulnerable library
- `classUsage <= classCount` (usage cannot exceed total)
- Apps with `classUsage = 0` are present but flagged as lower priority

---

### Test Case 1.4: Apps with Zero Class Usage

**Objective:** Verify apps with `classUsage = 0` are handled correctly.

**Test Steps:**
1. Query CVE affecting applications with varying class usage
2. Identify apps with `classUsage = 0`

**Expected Results:**
- Apps with `classUsage = 0` are returned in results
- These apps are marked as "unlikely to be vulnerable"
- `classUsage` field is explicitly 0 (not null)
- Interpretation: library present but classes not executed

---

## 2. CVE Format Validation Tests

### Test Case 2.1: Valid CVE Formats

**Objective:** Verify tool accepts properly formatted CVE IDs.

**Test Steps:**
Test with multiple valid CVE ID formats:
1. Standard format: `CVE-2021-44228`
2. Older CVE format: `CVE-1999-0001`
3. Recent CVE format: `CVE-2024-12345`
4. CVE with longer sequence: `CVE-2023-123456`

**Expected Results:**
- All valid formats accepted
- Tool queries Contrast API successfully
- Returns appropriate results or empty results if CVE not in environment

---

### Test Case 2.2: Invalid CVE Formats

**Objective:** Verify tool rejects malformed CVE IDs.

**Test Steps:**
Test with invalid CVE ID formats:
1. Missing CVE prefix: `2021-44228`
2. Wrong prefix: `CWE-2021-44228`
3. Missing year: `CVE-44228`
4. Missing sequence: `CVE-2021-`
5. Too short sequence: `CVE-2021-123` (less than 4 digits)
6. Invalid characters: `CVE-2021-ABC`
7. Empty string: ``
8. Lowercase: `cve-2021-44228` (check case sensitivity)

**Expected Results:**
- Validation error returned for invalid formats
- Error message indicates CVE format requirement
- No API call made for invalid formats
- Example error: "cveId must be in CVE format (e.g., CVE-2021-44228)"

---

### Test Case 2.3: Missing cveId Parameter

**Objective:** Verify validation error for missing required parameter.

**Test Steps:**
1. Call `list_applications_by_cve` without cveId (null or empty)

**Expected Results:**
- Validation error returned
- Error message: "cveId is required"
- No API call made

---

## 3. Empty Results Tests

### Test Case 3.1: CVE with No Vulnerable Applications

**Objective:** Verify tool handles CVEs not affecting any applications.

**Test Steps:**
1. Call tool with valid CVE that doesn't affect any apps in your environment
2. Examine response

**Expected Results:**
- Response status: success (not error)
- `data.apps` is empty list
- `message` contains warning about no applications found
- Warning explains: "The CVE may not affect any libraries in your organization, or the CVE ID may be invalid."
- `found: true` (CVE exists, just no affected apps)

---

### Test Case 3.2: Non-Existent CVE

**Objective:** Verify tool handles CVE IDs that don't exist.

**Test Steps:**
1. Call tool with non-existent CVE ID: `CVE-9999-99999`
2. Observe response

**Expected Results:**
- Either error response or empty results
- Appropriate message explaining CVE not found
- No crash or unexpected error

---

## 4. High-Profile CVE Tests

### Test Case 4.1: Log4Shell (CVE-2021-44228)

**Objective:** Test with Log4Shell, a widely known critical CVE.

**Prerequisites:**
- Environment with Log4j 2.x (versions 2.0-beta9 through 2.14.1)

**Test Steps:**
1. Call `list_applications_by_cve(cveId="CVE-2021-44228")`
2. Verify comprehensive results

**Expected Results:**
- Complete CVE details with CVSS score 10.0
- All applications using affected Log4j versions returned
- Library data shows log4j-core versions
- Class usage data distinguishes actively vulnerable apps

**Verification Points:**
- `cve.name = "CVE-2021-44228"`
- `cve.score` is high (9.0+)
- `cve.description` contains "Log4j" or "JNDI"
- Libraries contain log4j-core artifacts
- Apps with `classUsage > 0` are primary remediation targets

---

### Test Case 4.2: Spring4Shell (CVE-2022-22965)

**Objective:** Test with Spring4Shell vulnerability.

**Prerequisites:**
- Environment with Spring Framework 5.3.0-5.3.17 or 5.2.0-5.2.19

**Test Steps:**
1. Call `list_applications_by_cve(cveId="CVE-2022-22965")`
2. Analyze Spring-specific results

**Expected Results:**
- CVE details specific to Spring Framework
- Applications using vulnerable Spring versions identified
- Libraries show spring-framework or spring-beans artifacts

---

### Test Case 4.3: Text4Shell (CVE-2022-42889)

**Objective:** Test with Apache Commons Text vulnerability.

**Test Steps:**
1. Call `list_applications_by_cve(cveId="CVE-2022-42889")`
2. Analyze results

**Expected Results:**
- CVE details for Commons Text
- Affected applications identified
- Library matches commons-text artifacts

---

## 5. Library Correlation Tests

### Test Case 5.1: Library-Application Matching

**Objective:** Verify libraries list correlates with applications.

**Test Steps:**
1. Query CVE affecting 2+ applications
2. For each App in apps list:
   - Verify corresponding Library exists in libraries list
   - Match by library hash

**Expected Results:**
- Every vulnerable library used by returned apps is in libraries list
- Library hashes match for enrichment correlation
- Library versions are accurately represented

---

### Test Case 5.2: Multiple Library Versions

**Objective:** Verify multiple versions of same library are properly represented.

**Test Steps:**
1. Query CVE with multiple vulnerable versions (e.g., Log4j 2.12.1, 2.13.0, 2.14.1)
2. Examine libraries list for version diversity

**Expected Results:**
- Each vulnerable version appears as separate Library entry
- Different hashes for different versions
- Apps correctly associated with their specific version

---

## 6. Server Information Tests

### Test Case 6.1: Server Data Completeness

**Objective:** Verify server information is properly included.

**Test Steps:**
1. Query CVE affecting server-deployed applications
2. Examine servers list

**Expected Results:**
- All servers hosting vulnerable applications listed
- Each Server object contains:
  - `server_id`: Valid integer
  - `name`: Server name
  - `hostname`: Resolvable hostname or IP
  - `path`: Deployment path
  - `type`: Server type (e.g., "Tomcat", "Spring Boot")
  - `environment`: Environment designation
  - `status`: Current status

---

## 7. Error Handling Tests

### Test Case 7.1: Enrichment Failure Handling

**Objective:** Verify enrichment failures don't block response.

**Test Steps:**
1. Query CVE (enrichment may fail for some apps)
2. Check for warnings in response

**Expected Results:**
- Response succeeds even if some enrichment fails
- Warnings added for failed enrichments
- Example warning: "Could not fetch class usage data for application 'X': {error}"
- Partial data returned (apps without class usage data)

---

### Test Case 7.2: API Connection Failure

**Objective:** Verify tool handles Contrast API connection issues.

**Prerequisites:**
- Simulate network issues or invalid credentials

**Test Steps:**
1. Configure invalid hostname or credentials
2. Call tool

**Expected Results:**
- Error response with descriptive message
- MCP server remains stable
- Error logged appropriately

---

### Test Case 7.3: Authentication Failure

**Objective:** Verify tool handles authentication problems.

**Test Steps:**
1. Configure invalid API credentials
2. Call tool

**Expected Results:**
- Authentication error returned
- Security credentials not exposed in logs
- Clear error message

---

## 8. Performance Tests

### Test Case 8.1: CVE Affecting Many Applications

**Objective:** Verify tool performance with large result sets.

**Prerequisites:**
- Widespread CVE affecting 20+ applications (e.g., Log4Shell)

**Test Steps:**
1. Call tool with widespread CVE
2. Measure response time
3. Verify all data returned

**Expected Results:**
- Tool completes within reasonable time (< 30 seconds)
- All applications returned (no truncation)
- No timeout exceptions
- Memory usage acceptable

**Performance Notes:**
- Tool makes multiple API calls (one per app for enrichment)
- Enrichment loop is O(n*m*k) where:
  - n = number of apps
  - m = libraries per app
  - k = vulnerable libraries

---

### Test Case 8.2: Repeated Queries

**Objective:** Verify tool handles repeated identical queries.

**Test Steps:**
1. Call tool with same CVE 5 times in succession
2. Compare results and timing

**Expected Results:**
- Results consistent across calls
- No caching-related issues
- Performance remains stable

---

## 9. Integration Tests

### Test Case 9.1: Workflow with list_application_libraries

**Objective:** Verify data consistency with related tools.

**Test Steps:**
1. Call `list_applications_by_cve(cveId="CVE-2021-44228")`
2. Pick an affected app from results
3. Call `list_application_libraries(appId="<app-id>")`
4. Verify the vulnerable library appears in app's library list

**Expected Results:**
- Library from CVE results appears in app's library list
- Library hash matches
- Version information consistent

---

### Test Case 9.2: AI Agent Remediation Workflow

**Objective:** Verify tool supports typical AI remediation workflow.

**Test Steps:**
1. Query CVE to find affected applications
2. Filter results to apps with `classUsage > 0` (actively vulnerable)
3. Use `search_applications` to get more details on high-priority apps
4. Prioritize based on:
   - `classUsage > 0`
   - PRODUCTION environment servers
   - High CVSS score

**Expected Results:**
- AI can successfully prioritize remediation
- Data supports actionable recommendations
- Workflow completes without errors

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server running with valid credentials
2. Identify CVEs present in your environment
3. Note at least one high-profile CVE for testing
4. Know expected application counts for validation

### Workflow for Finding Test Data
```
1. Start with known CVE (e.g., CVE-2021-44228)
2. Run list_applications_by_cve
3. Note affected applications and libraries
4. Use for subsequent integration tests
```

### Success Criteria
The `list_applications_by_cve` tool passes testing if:
- Valid CVE queries succeed (TC 1.1-1.4)
- CVE format validation works (TC 2.1-2.3)
- Empty results handled gracefully (TC 3.1-3.2)
- High-profile CVEs return complete data (TC 4.1-4.3)
- Library correlation is accurate (TC 5.1-5.2)
- Error handling is graceful (TC 7.1-7.3)
- Performance is acceptable (< 30 seconds)

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Basic Functionality | 4 | Core behavior, enrichment |
| CVE Format Validation | 3 | Valid/invalid formats |
| Empty Results | 2 | No apps, non-existent CVE |
| High-Profile CVEs | 3 | Log4Shell, Spring4Shell, Text4Shell |
| Library Correlation | 2 | Matching, versions |
| Server Information | 1 | Completeness |
| Error Handling | 3 | Enrichment, API, auth |
| Performance | 2 | Large results, repeated |
| Integration | 2 | Workflow, remediation |

**Total: 22 test cases**

---

## Appendix: Expected Behavior Examples

### Successful Response Example
```json
{
  "data": {
    "cve": {
      "name": "CVE-2021-44228",
      "description": "Apache Log4j2 JNDI features...",
      "score": 10.0,
      "availabilityImpact": "HIGH",
      "confidentialityImpact": "HIGH",
      "integrityImpact": "HIGH"
    },
    "impactStats": {
      "impactedAppCount": 5,
      "totalAppCount": 20,
      "impactedServerCount": 8,
      "appPercentage": 25.0,
      "serverPercentage": 40.0
    },
    "libraries": [
      {
        "hash": "abc123...",
        "version": "2.14.1",
        "file_name": "log4j-core-2.14.1.jar",
        "group": "org.apache.logging.log4j"
      }
    ],
    "apps": [
      {
        "name": "WebApp",
        "app_id": "app-123",
        "classCount": 1500,
        "classUsage": 45,
        "importance_description": "CRITICAL"
      },
      {
        "name": "BackendService",
        "app_id": "app-456",
        "classCount": 1500,
        "classUsage": 0,
        "importance_description": "HIGH"
      }
    ],
    "servers": [...]
  },
  "message": null,
  "found": true
}
```

### No Affected Apps Response
```json
{
  "data": {
    "cve": {...},
    "impactStats": {
      "impactedAppCount": 0,
      ...
    },
    "libraries": [],
    "apps": [],
    "servers": []
  },
  "message": "No applications found with this CVE. The CVE may not affect any libraries in your organization, or the CVE ID may be invalid.",
  "found": true
}
```

### Validation Error Response
```json
{
  "data": null,
  "message": "Validation failed: cveId must be in CVE format (e.g., CVE-2021-44228). Format: CVE-YYYY-NNNNN where YYYY is the year and NNNNN is a sequence number.",
  "found": false
}
```

---

## Logging Verification

Check MCP server logs (`/tmp/mcp-contrast.log`) for:
- Request received with cveId
- "Retrieving applications vulnerable to CVE: {cveId}"
- "Found {} applications vulnerable to {}, enriching with class usage data"
- "Successfully retrieved CVE data for {}: {} vulnerable applications"
- Warning logs for enrichment failures

---

## References

- **Tool Implementation**: `tool/library/ListApplicationsByCveTool.java`
- **Params Class**: `tool/library/params/ListApplicationsByCveParams.java`
- **Related Tools**: `list_application_libraries`, `search_applications`
- **Old Test Plan**: `test-plan-list_applications_vulnerable_to_cve.md` (root level)

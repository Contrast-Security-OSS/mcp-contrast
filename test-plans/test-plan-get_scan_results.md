# Test Plan: get_scan_results Tool

## Overview

This test plan provides comprehensive testing guidance for the `get_scan_results` MCP tool. This tool retrieves the latest SAST scan results for a project in SARIF format.

### Migration Notes

**This plan replaces:**
- `test-plan-list_Scan_Results.md` (original at root level)

**Key Changes from Original Tool:**
- **Tool renamed**: `list_Scan_Results` → `get_scan_results`
- **Follows tool-per-class pattern**: Uses `SingleTool<GetSastResultsParams, String>`
- **Enhanced validation**: Uses `GetSastResultsParams` for parameter validation
- **Deprecation warning**: Tool marked as deprecated due to large SARIF responses
- **Better null handling**: Explicit checks for lastScanId and scan existence

### Tool Signature

**MCP Tool Name:** `get_scan_results`

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `projectName` | String | Yes | Scan project name (case-sensitive, must match exactly) |

### Response Structure

**Returns:** `SingleToolResponse<String>`

```java
SingleToolResponse {
    String data,       // SARIF JSON as string
    String message,    // Warnings (including deprecation warning)
    boolean found      // True if results found
}
```

**SARIF Structure (returned in data):**
```json
{
  "version": "2.1.0",
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/...",
  "runs": [{
    "tool": {
      "driver": {
        "name": "Contrast Scan",
        "rules": [...]
      }
    },
    "results": [
      {
        "ruleId": "sql-injection",
        "level": "error",
        "message": { "text": "..." },
        "locations": [...]
      }
    ]
  }]
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **DEPRECATED** | Tool returns raw SARIF which may be very large |
| **Case Sensitivity** | Project names are case-sensitive (exact match required) |
| **Latest Scan Only** | Returns results from most recent scan only |
| **No Scans** | Returns null with warning if project has no completed scans |
| **Memory Usage** | Large SARIF files loaded into memory (potential OOM for very large files) |

---

## 1. Basic Functionality Tests

### Test Case 1.1: Retrieve Latest Scan Results - Success

**Objective:** Verify tool successfully retrieves SARIF data for valid project.

**Prerequisites:**
- SAST project with at least one completed scan
- Scan has findings

**Test Steps:**
1. Call `get_scan_results(projectName="<project-with-scans>")`
2. Verify response structure

**Expected Results:**
- Response status: success
- `data` contains valid SARIF JSON string
- `message` contains deprecation warning
- `found: true`

---

### Test Case 1.2: SARIF Format Validation

**Objective:** Verify returned data is valid SARIF 2.1.0 JSON.

**Test Steps:**
1. Call `get_scan_results` with valid project
2. Parse returned string as JSON
3. Verify SARIF schema compliance

**Expected Results:**
- Returned string parses as valid JSON
- Contains required SARIF fields:
  - `version`: "2.1.0"
  - `$schema`: SARIF schema URL
  - `runs`: Array of run objects
- No parsing errors

---

### Test Case 1.3: SARIF Contains Findings

**Objective:** Verify SARIF includes actual scan findings.

**Prerequisites:**
- Project with scan that found vulnerabilities

**Test Steps:**
1. Call `get_scan_results`
2. Parse SARIF JSON
3. Check `runs[0].results` array

**Expected Results:**
- Results array contains findings
- Each result has:
  - `ruleId`: Vulnerability type identifier
  - `level`: Severity (error, warning, note)
  - `message`: Description text
  - `locations`: File and line information

---

### Test Case 1.4: SARIF Contains Rule Definitions

**Objective:** Verify SARIF includes rule definitions.

**Test Steps:**
1. Parse returned SARIF
2. Check `runs[0].tool.driver.rules` array

**Expected Results:**
- Rules array present and populated
- Each rule has:
  - `id`: Rule identifier
  - `shortDescription`: Brief description
  - Additional metadata

---

### Test Case 1.5: SARIF Contains Location Information

**Objective:** Verify findings include file/line location data.

**Test Steps:**
1. Parse returned SARIF
2. Check locations in results

**Expected Results:**
- Each result has locations array
- Each location has:
  - `physicalLocation.artifactLocation.uri`: File path
  - `physicalLocation.region.startLine`: Line number
  - Optional: `startColumn`, `endLine`, `endColumn`

---

## 2. Empty/Edge Case Tests

### Test Case 2.1: Project with No Scans

**Objective:** Verify behavior when project has no completed scans.

**Prerequisites:**
- Project that exists but has never been scanned

**Test Steps:**
1. Call `get_scan_results(projectName="<project-no-scans>")`
2. Check response

**Expected Results:**
- `data` is null
- `message` contains warning about no scan results
- Warning indicates project exists but has no completed scans
- No exception thrown

---

### Test Case 2.2: Scan with No Findings (Clean Scan)

**Objective:** Verify behavior with clean scan (zero findings).

**Prerequisites:**
- Project with completed scan that found no vulnerabilities

**Test Steps:**
1. Call `get_scan_results` for clean project
2. Parse SARIF

**Expected Results:**
- Valid SARIF returned
- `runs[0].results` is empty array
- No error (clean scan is valid result)

---

### Test Case 2.3: Empty SARIF Stream

**Objective:** Verify handling of empty scan data.

**Test Steps:**
1. Call tool (if scan returns empty data)
2. Check response

**Expected Results:**
- Either empty string or error
- No crash
- Graceful handling

---

## 3. Project Resolution Tests

### Test Case 3.1: Valid Project Name - Exact Match

**Objective:** Verify tool finds project with exact name match.

**Test Steps:**
1. Call `get_scan_results(projectName="<exact-name>")`
2. Verify project found and results retrieved

**Expected Results:**
- Project resolved successfully
- SARIF returned

---

### Test Case 3.2: Non-Existent Project Name

**Objective:** Verify behavior when project doesn't exist.

**Test Steps:**
1. Call `get_scan_results(projectName="NonExistentProject123XYZ")`

**Expected Results:**
- `data` is null
- `found: false`
- Clear indication project not found

---

### Test Case 3.3: Case Sensitivity

**Objective:** Verify case-sensitive project name matching.

**Prerequisites:**
- Know exact casing of project name (e.g., "MyProject")

**Test Steps:**
1. Call with exact case: `"MyProject"`
2. Call with wrong case: `"myproject"`

**Expected Results:**
- Exact case: Success
- Wrong case: Not found

---

### Test Case 3.4: Project Name with Special Characters

**Objective:** Verify handling of special characters in names.

**Test Steps:**
Test with project names containing:
1. Dashes: `"frontend-web-app"`
2. Underscores: `"backend_api_v2"`
3. Spaces: `"My Java App"` (if supported)

**Expected Results:**
- Special characters handled correctly
- No URL encoding issues

---

## 4. Validation Tests

### Test Case 4.1: Missing projectName Parameter

**Objective:** Verify validation error for missing parameter.

**Test Steps:**
1. Call `get_scan_results` without projectName

**Expected Results:**
- Validation error returned
- Error message: "projectName is required"
- No API call made

---

### Test Case 4.2: Empty String Project Name

**Objective:** Verify handling of empty string.

**Test Steps:**
1. Call `get_scan_results(projectName="")`

**Expected Results:**
- Validation error: "projectName is required"
- Empty string treated as missing

---

### Test Case 4.3: Whitespace-Only Project Name

**Objective:** Verify handling of whitespace-only input.

**Test Steps:**
1. Call `get_scan_results(projectName="   ")`

**Expected Results:**
- Validation error or not found
- Graceful handling

---

## 5. SARIF Content Tests

### Test Case 5.1: SARIF Character Encoding

**Objective:** Verify UTF-8 encoding handled correctly.

**Prerequisites:**
- Scan with findings containing special characters in file paths or messages

**Test Steps:**
1. Call `get_scan_results`
2. Check for special characters in response

**Expected Results:**
- UTF-8 characters preserved correctly
- No encoding corruption

---

### Test Case 5.2: Large SARIF File

**Objective:** Verify handling of large result sets.

**Prerequisites:**
- Project with scan containing many findings (1000+)

**Test Steps:**
1. Call `get_scan_results` for project with many findings
2. Measure response time and memory usage

**Expected Results:**
- Returns complete SARIF (no truncation)
- Response time reasonable (< 30 seconds)
- No OutOfMemoryError

**Performance Notes:**
- Tool loads entire SARIF into memory
- Large files (50MB+) may cause issues
- This is why tool is deprecated

---

### Test Case 5.3: Malformed SARIF Handling

**Objective:** Verify tool returns data as-is (no validation).

**Note:** This tests that tool doesn't validate SARIF - it's a pass-through.

**Expected Results:**
- Tool returns whatever SDK provides
- Validation is consumer's responsibility

---

## 6. Error Handling Tests

### Test Case 6.1: API Connection Failure

**Objective:** Verify graceful handling of connection issues.

**Test Steps:**
1. Configure invalid hostname
2. Call `get_scan_results`

**Expected Results:**
- Error response returned
- Clear error message
- MCP server remains stable

---

### Test Case 6.2: Authentication Failure

**Objective:** Verify handling of invalid credentials.

**Test Steps:**
1. Configure invalid API credentials
2. Call `get_scan_results`

**Expected Results:**
- Authentication error returned
- No credentials exposed in logs

---

### Test Case 6.3: Scan Retrieval Failure

**Objective:** Verify handling when scan exists but can't be retrieved.

**Test Steps:**
1. If possible, test with permission-restricted scan
2. Check error handling

**Expected Results:**
- Appropriate error returned
- Clear indication of issue

---

### Test Case 6.4: Concurrent Requests

**Objective:** Verify concurrent requests handled correctly.

**Test Steps:**
1. Call `get_scan_results` for 3 different projects concurrently
2. Verify each returns correct data

**Expected Results:**
- All requests complete successfully
- Each returns correct SARIF (no mixing)
- No thread safety issues

---

## 7. Performance Tests

### Test Case 7.1: Response Time Baseline

**Objective:** Measure acceptable response time.

**Test Steps:**
1. Call `get_scan_results` for small project
2. Measure response time
3. Repeat 5 times

**Expected Results:**
- Small project (< 100 findings): < 5 seconds
- Average case: < 10 seconds

---

### Test Case 7.2: Large Project Performance

**Objective:** Verify performance with many findings.

**Prerequisites:**
- Project with 10,000+ findings

**Test Steps:**
1. Call `get_scan_results`
2. Measure response time
3. Monitor memory usage

**Expected Results:**
- Completes without timeout
- Memory usage acceptable
- Response time documented

**Performance Benchmarks:**
- 10 MB SARIF: < 10 seconds
- 50 MB SARIF: < 30 seconds
- 100 MB SARIF: May cause issues (document behavior)

---

## 8. Integration Tests

### Test Case 8.1: Workflow with get_scan_project

**Objective:** Verify typical SAST workflow.

**Test Steps:**
1. Call `get_scan_project(projectName="<project>")`
2. Check if `lastScanId` exists
3. If exists, call `get_scan_results(projectName="<project>")`

**Expected Results:**
- Project info helps determine if results available
- Both tools work with same project name
- Data is consistent

---

### Test Case 8.2: Verify Results Match Project

**Objective:** Verify SARIF results are for correct project.

**Test Steps:**
1. Get project info with `get_scan_project`
2. Get results with `get_scan_results`
3. Compare finding counts

**Expected Results:**
- Finding counts in SARIF align with project summary
- Same project/scan data

---

## 9. Deprecation Warning Tests

### Test Case 9.1: Deprecation Warning Present

**Objective:** Verify deprecation warning always included.

**Test Steps:**
1. Call `get_scan_results` (any valid project)
2. Check `message` field

**Expected Results:**
- Warning present in message
- Warning indicates tool is deprecated
- Warning mentions large SARIF files

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server running with valid credentials
2. Ensure SAST functionality is enabled
3. Have at least 2-3 SAST projects:
   - Project with multiple scans and findings
   - Project with clean scan (no findings)
   - Project with no scans

### Workflow for Finding Test Data
```
1. Log into Contrast TeamServer
2. Navigate to Scan section
3. Find projects with completed scans
4. Note finding counts for comparison
5. Identify one project with many findings for performance testing
```

### Success Criteria
The `get_scan_results` tool passes testing if:
- Valid project retrieval succeeds (TC 1.1-1.5)
- Empty cases handled gracefully (TC 2.1-2.3)
- Project resolution is case-sensitive (TC 3.1-3.4)
- Validation catches invalid input (TC 4.1-4.3)
- SARIF content is valid (TC 5.1-5.2)
- Error handling is graceful (TC 6.1-6.4)
- Deprecation warning present (TC 9.1)

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Basic Functionality | 5 | Core behavior, SARIF structure |
| Empty/Edge Cases | 3 | No scans, clean scan, empty |
| Project Resolution | 4 | Name matching, case sensitivity |
| Validation | 3 | Required params, edge cases |
| SARIF Content | 3 | Encoding, large files, malformed |
| Error Handling | 4 | Connection, auth, concurrent |
| Performance | 2 | Response time, large files |
| Integration | 2 | Workflow with get_scan_project |
| Deprecation | 1 | Warning verification |

**Total: 27 test cases**

---

## Appendix: Expected Behavior Examples

### Successful Response Example
```json
{
  "data": "{\"version\":\"2.1.0\",\"$schema\":\"https://...\",\"runs\":[{\"tool\":{\"driver\":{\"name\":\"Contrast Scan\",\"rules\":[...]}},\"results\":[{\"ruleId\":\"sql-injection\",\"level\":\"error\",\"message\":{\"text\":\"SQL Injection vulnerability\"},\"locations\":[{\"physicalLocation\":{\"artifactLocation\":{\"uri\":\"src/main/java/UserController.java\"},\"region\":{\"startLine\":42}}}]}]}]}",
  "message": "DEPRECATED: This tool returns raw SARIF which may be very large. Consider using future paginated SAST search tools for better AI-friendly access.",
  "found": true
}
```

### No Scans Response
```json
{
  "data": null,
  "message": "No scan results available for project: MyProject. Project exists but has no completed scans.",
  "found": true
}
```

### Project Not Found Response
```json
{
  "data": null,
  "message": null,
  "found": false
}
```

### Validation Error Response
```json
{
  "data": null,
  "message": "Validation failed: projectName is required",
  "found": false
}
```

---

## Appendix: SARIF 2.1.0 Schema Reference

### Required Top-Level Fields
- `version`: String, must be "2.1.0"
- `$schema`: String, URL to SARIF schema
- `runs`: Array of run objects

### Run Object Fields
- `tool`: Object describing the analysis tool
- `results`: Array of result objects (findings)

### Result Object Fields (Finding)
- `ruleId`: String, identifier for the rule
- `level`: String, severity (error, warning, note)
- `message`: Object with text property
- `locations`: Array of location objects

### Further Reading
- [SARIF Specification](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html)

---

## Known Issues and Recommendations

### Issue 1: Memory Inefficiency for Large Files
**Location:** Tool uses `Collectors.joining()` which loads entire SARIF into memory
**Impact:** Large SARIF files (100+ MB) may cause OutOfMemoryError
**Recommendation:** Use future paginated SAST tools instead

### Issue 2: No SARIF Validation
**Description:** Tool returns any data from stream, even if malformed
**Recommendation:** Validation is consumer's responsibility

### Issue 3: Tool is Deprecated
**Description:** Raw SARIF often exceeds AI context limits
**Recommendation:** Use future paginated SAST search tools when available

---

## Logging Verification

Check MCP server logs (`/tmp/mcp-contrast.log`) for:
- "Retrieving latest scan results in SARIF format for project: {projectName}"
- "Found project with id: {}"
- "Retrieved scans for project, last scan id: {}"
- "Successfully retrieved SARIF data for project: {}"
- "Project not found: {}" (for not found cases)
- "No scan results available for project: {}" (for no scans)

---

## References

- **Tool Implementation**: `tool/sast/GetSastResultsTool.java`
- **Params Class**: `tool/sast/params/GetSastResultsParams.java`
- **Related Tools**: `get_scan_project`
- **Old Test Plan**: `test-plan-list_Scan_Results.md` (root level)

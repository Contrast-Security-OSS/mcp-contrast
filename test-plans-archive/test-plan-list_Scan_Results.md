# Test Plan: list_Scan_Results Tool

## Overview
This test plan covers comprehensive testing of the `list_Scan_Results` MCP tool in `SastService.java` (lines 81-108). The tool retrieves the latest scan results for a SAST project and returns them in SARIF (Static Analysis Results Interchange Format) format.

## Tool Signature
```java
@Tool(name = "list_Scan_Results",
      description = "takes a scan project name and returns the latest results in Sarif format")
public String getLatestScanResult(String projectName) throws IOException
```

## Test Execution Approach
Tests can be executed as:
1. **Unit Tests** - Mock SDK calls, focus on logic and error handling (recommended for most tests)
2. **Integration Tests** - Against a real Contrast instance with test data (for end-to-end validation)
3. **MCP Tests** - Via MCP protocol using Claude or other AI tools (for AI integration testing)

---

## Test Categories

### 1. Basic Functionality Tests

#### Test 1.1: Retrieve Latest Scan Results - Happy Path
**Objective**: Verify tool successfully retrieves and returns SARIF data for a valid project with scans.

**Test Data Needed**:
- Project name: "test-java-project"
- Project ID: "proj-123"
- Last scan ID: "scan-456"
- SARIF content: Valid SARIF JSON with at least one finding

**Steps**:
1. Call `list_Scan_Results` with `projectName = "test-java-project"`
2. Verify SDK calls made in correct sequence:
   - `contrastSDK.scan(orgID).projects().findByName("test-java-project")`
   - `contrastSDK.scan(orgID).scans(project.id())`
   - `scans.get(project.lastScanId())`
   - `scan.sarif()`
3. Verify method returns non-null String
4. Verify returned string contains valid JSON

**Expected Result**:
- Returns SARIF JSON as String
- No exceptions thrown
- Logger shows info messages for project and SARIF retrieval

**Mock Setup**:
```java
Project mockProject = mock(Project.class);
when(mockProject.id()).thenReturn("proj-123");
when(mockProject.lastScanId()).thenReturn("scan-456");

Scans mockScans = mock(Scans.class);
Scan mockScan = mock(Scan.class);
InputStream sarifStream = new ByteArrayInputStream(VALID_SARIF_JSON.getBytes());
when(mockScan.sarif()).thenReturn(sarifStream);

when(mockContrastSDK.scan(orgID).projects().findByName("test-java-project"))
    .thenReturn(Optional.of(mockProject));
when(mockContrastSDK.scan(orgID).scans("proj-123")).thenReturn(mockScans);
when(mockScans.get("scan-456")).thenReturn(mockScan);
```

---

### 2. SARIF Format Validation Tests

#### Test 2.1: Valid SARIF JSON Structure
**Objective**: Verify returned data is valid JSON.

**Test Data Needed**:
- Minimal valid SARIF JSON:
```json
{
  "version": "2.1.0",
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
  "runs": []
}
```

**Steps**:
1. Mock scan.sarif() to return minimal SARIF
2. Call tool
3. Parse returned string as JSON using Jackson or Gson
4. Verify no parsing exceptions

**Expected Result**:
- Returned string parses successfully as JSON
- No JSON parse errors

#### Test 2.2: SARIF Schema Compliance - Valid Version
**Objective**: Verify SARIF contains required top-level fields per SARIF 2.1.0 spec.

**Test Data Needed**:
- SARIF with version 2.1.0 and required schema URL
- SARIF with valid `runs` array

**Steps**:
1. Parse returned SARIF JSON
2. Verify presence of required fields:
   - `version` = "2.1.0"
   - `$schema` (schema URL)
   - `runs` (array)

**Expected Result**:
- All required SARIF 2.1.0 fields present
- Version matches expected value

#### Test 2.3: SARIF Character Encoding
**Objective**: Verify tool correctly handles UTF-8 encoded SARIF data.

**Test Data Needed**:
- SARIF containing special characters:
  - Unicode characters (émoji, 中文)
  - Escaped characters (\n, \t, \")
  - File paths with spaces

**Steps**:
1. Mock scan.sarif() to return UTF-8 encoded SARIF
2. Call tool
3. Verify returned string maintains proper encoding

**Expected Result**:
- Special characters preserved correctly
- No encoding corruption

---

### 3. Data Completeness Tests

#### Test 3.1: SARIF Contains Findings/Results
**Objective**: Verify SARIF includes actual scan findings.

**Test Data Needed**:
- SARIF with populated `runs[0].results` array containing findings:
```json
{
  "version": "2.1.0",
  "runs": [{
    "results": [
      {
        "ruleId": "sql-injection",
        "level": "error",
        "message": { "text": "SQL Injection vulnerability detected" }
      }
    ]
  }]
}
```

**Steps**:
1. Call tool
2. Parse returned SARIF
3. Verify `runs[0].results` array exists and is not empty
4. Verify each result has required fields (ruleId, level, message)

**Expected Result**:
- Results array contains at least one finding
- Each finding has complete required fields

#### Test 3.2: SARIF Contains Rules Definitions
**Objective**: Verify SARIF includes rule definitions.

**Test Data Needed**:
- SARIF with populated `runs[0].tool.driver.rules` array:
```json
{
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "Contrast Scan",
        "rules": [
          {
            "id": "sql-injection",
            "shortDescription": { "text": "SQL Injection" }
          }
        ]
      }
    },
    "results": [...]
  }]
}
```

**Steps**:
1. Parse returned SARIF
2. Verify `runs[0].tool.driver.rules` exists
3. Verify rules array is not empty

**Expected Result**:
- Rules array present and populated
- Each rule has id and description

#### Test 3.3: SARIF Contains Location Information
**Objective**: Verify findings include location data (file, line, column).

**Test Data Needed**:
- SARIF with results containing locations:
```json
{
  "results": [{
    "ruleId": "xss",
    "locations": [{
      "physicalLocation": {
        "artifactLocation": {
          "uri": "src/main/java/com/example/UserController.java"
        },
        "region": {
          "startLine": 42,
          "startColumn": 15
        }
      }
    }]
  }]
}
```

**Steps**:
1. Parse returned SARIF
2. For each result, verify locations array exists
3. Verify physicalLocation contains file URI and line/column info

**Expected Result**:
- Each result has at least one location
- Locations contain file path and position data

#### Test 3.4: Empty Scan Results
**Objective**: Verify tool handles scans with no findings (clean scan).

**Test Data Needed**:
- SARIF with empty results array:
```json
{
  "version": "2.1.0",
  "runs": [{
    "tool": { "driver": { "name": "Contrast Scan" } },
    "results": []
  }]
}
```

**Steps**:
1. Mock scan with zero findings
2. Call tool
3. Verify returns valid SARIF with empty results

**Expected Result**:
- Tool succeeds (no error)
- Returns valid SARIF structure
- Results array is empty but present

---

### 4. Project Resolution Tests

#### Test 4.1: Valid Project Name - Exact Match
**Objective**: Verify tool finds project with exact name match.

**Test Data Needed**:
- Project name: "MyJavaApp"

**Steps**:
1. Mock `projects().findByName("MyJavaApp")` to return project
2. Call tool with "MyJavaApp"
3. Verify project found and scan retrieved

**Expected Result**:
- Project resolved successfully
- Logger shows: "Found project with id: proj-123"

#### Test 4.2: Invalid Project Name - Not Found
**Objective**: Verify tool throws IOException when project doesn't exist.

**Test Data Needed**:
- Non-existent project name: "NonExistentProject"

**Steps**:
1. Mock `projects().findByName("NonExistentProject")` to return `Optional.empty()`
2. Call tool with "NonExistentProject"
3. Verify IOException thrown

**Expected Result**:
- IOException with message "Project not found"
- Logger shows error: "Failed to find project NonExistentProject"

**Mock Setup**:
```java
when(mockContrastSDK.scan(orgID).projects().findByName("NonExistentProject"))
    .thenReturn(Optional.empty());

// Act & Assert
IOException exception = assertThrows(IOException.class, () -> {
    sastService.getLatestScanResult("NonExistentProject");
});
assertEquals("Project not found", exception.getMessage());
```

#### Test 4.3: Project Name with Special Characters
**Objective**: Verify tool handles project names with spaces, hyphens, underscores.

**Test Data Needed**:
- Project names:
  - "My Java App"
  - "frontend-web-app"
  - "backend_api_v2"

**Steps**:
1. For each project name, mock successful project lookup
2. Call tool
3. Verify project resolved

**Expected Result**:
- All special character variations handled correctly
- No URL encoding issues

#### Test 4.4: Case Sensitivity in Project Names
**Objective**: Determine if project name lookup is case-sensitive.

**Test Data Needed**:
- Project name variations:
  - "MyApp"
  - "myapp"
  - "MYAPP"

**Steps**:
1. Mock project with name "MyApp"
2. Try calling tool with each variation
3. Document behavior (likely case-sensitive based on SDK)

**Expected Result**:
- Document whether lookup is case-sensitive
- If case-sensitive: only exact match succeeds
- If case-insensitive: all variations succeed

---

### 5. Scan Existence Tests

#### Test 5.1: Project with No Scans
**Objective**: Verify tool handles projects that have never been scanned.

**Test Data Needed**:
- Project with `lastScanId = null`

**Steps**:
1. Mock project with null lastScanId
2. Call tool
3. Expect NullPointerException or IOException

**Expected Result**:
- Exception thrown (NullPointerException on line 95)
- Error logged

**Note**: This reveals a bug - tool should check if `project.lastScanId()` is null before calling `scans.get()`.

**Mock Setup**:
```java
Project mockProject = mock(Project.class);
when(mockProject.id()).thenReturn("proj-123");
when(mockProject.lastScanId()).thenReturn(null);  // No scans yet

when(mockContrastSDK.scan(orgID).projects().findByName("test-project"))
    .thenReturn(Optional.of(mockProject));

// Should throw NullPointerException or handle gracefully
```

#### Test 5.2: Project with Multiple Scans - Verify Latest Returned
**Objective**: Verify tool returns the most recent scan, not an older one.

**Test Data Needed**:
- Project with multiple scans:
  - Scan 1 (older): scan-100, timestamp: 2025-01-01
  - Scan 2 (latest): scan-200, timestamp: 2025-01-15
- Project.lastScanId = "scan-200"

**Steps**:
1. Mock project with lastScanId pointing to latest scan
2. Mock scans collection with multiple scans
3. Call tool
4. Verify it retrieves scan with ID "scan-200" (not older scans)

**Expected Result**:
- Tool calls `scans.get(project.lastScanId())`
- Returns SARIF from latest scan only
- Does not iterate through all scans

**Verification**:
```java
verify(mockScans).get("scan-200");  // Latest scan
verify(mockScans, never()).get("scan-100");  // Older scan not accessed
```

#### Test 5.3: In-Progress Scan
**Objective**: Determine behavior when lastScanId points to a running scan.

**Test Data Needed**:
- Project with lastScanId = "scan-in-progress"
- Scan in "RUNNING" or "QUEUED" state

**Steps**:
1. Mock project with in-progress scan ID
2. Mock scan.sarif() to return partial/empty SARIF or throw exception
3. Call tool
4. Document behavior

**Expected Result**:
- Either returns partial results or throws exception
- Document actual SDK behavior for in-progress scans

---

### 6. Error Handling Tests

#### Test 6.1: Missing lastScanId
**Objective**: Verify graceful handling when project exists but has no scans.

**Test Data Needed**:
- Project with null lastScanId

**Steps**:
1. Mock project.lastScanId() returning null
2. Call tool
3. Verify appropriate exception

**Expected Result**:
- IOException thrown with descriptive message
- Logger shows error

**Current Behavior**:
- Line 95 will throw NullPointerException
- **Recommendation**: Add null check before line 95

#### Test 6.2: SARIF Stream Read Error
**Objective**: Verify error handling when stream reading fails.

**Test Data Needed**:
- Mock InputStream that throws IOException on read

**Steps**:
1. Mock scan.sarif() to return InputStream that throws IOException
2. Call tool
3. Verify IOException propagated with context

**Expected Result**:
- IOException caught and re-thrown
- Logger shows: "Error retrieving SARIF data for project X"

**Mock Setup**:
```java
InputStream failingStream = mock(InputStream.class);
when(failingStream.read()).thenThrow(new IOException("Network timeout"));
when(mockScan.sarif()).thenReturn(failingStream);

IOException exception = assertThrows(IOException.class, () -> {
    sastService.getLatestScanResult("test-project");
});
assertTrue(exception.getMessage().contains("Error retrieving SARIF data"));
```

#### Test 6.3: Malformed SARIF Data
**Objective**: Verify tool returns malformed data as-is (tool doesn't validate).

**Test Data Needed**:
- Invalid JSON: `{ "invalid": `
- Non-JSON text: `This is not JSON`

**Steps**:
1. Mock scan.sarif() to return malformed data
2. Call tool
3. Verify method returns data without validation

**Expected Result**:
- Tool returns malformed data as String
- No validation performed (validation is consumer's responsibility)
- Method succeeds without error

**Rationale**: Tool is a pass-through - consumers should validate SARIF.

#### Test 6.4: Empty SARIF Stream
**Objective**: Verify handling of empty scan results.

**Test Data Needed**:
- Empty InputStream (zero bytes)

**Steps**:
1. Mock scan.sarif() to return empty stream
2. Call tool
3. Verify returns empty string

**Expected Result**:
- Returns empty string ""
- No exception thrown
- Logger shows SARIF retrieval message

**Mock Setup**:
```java
InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
when(mockScan.sarif()).thenReturn(emptyStream);
```

#### Test 6.5: SDK Authentication Failure
**Objective**: Verify error handling when SDK authentication fails.

**Test Data Needed**:
- Invalid credentials configured

**Steps**:
1. Mock SDK to throw UnauthorizedException
2. Call tool
3. Verify exception propagated with context

**Expected Result**:
- IOException thrown
- Logger shows authentication error

#### Test 6.6: SDK Network Timeout
**Objective**: Verify timeout handling for slow/unresponsive API.

**Test Data Needed**:
- Mock SDK calls that timeout

**Steps**:
1. Mock SDK call to throw SocketTimeoutException
2. Call tool
3. Verify exception handled

**Expected Result**:
- IOException thrown
- Error logged with context

#### Test 6.7: Project Exists But Scan Retrieval Fails
**Objective**: Test error when project found but scan cannot be retrieved.

**Test Data Needed**:
- Valid project
- scans.get() throws exception

**Steps**:
1. Mock project found successfully
2. Mock scans.get() to throw IOException
3. Call tool

**Expected Result**:
- IOException thrown
- Error logged: "Error retrieving SARIF data for project X"

---

### 7. Large SARIF File Tests

#### Test 7.1: Large SARIF File - Memory Efficiency
**Objective**: Verify tool handles large SARIF files efficiently without OutOfMemoryError.

**Test Data Needed**:
- Large SARIF file: 50 MB - 100 MB
- Contains thousands of findings (10,000+ results)

**Steps**:
1. Mock scan.sarif() to return large InputStream
2. Call tool
3. Monitor memory usage
4. Verify completion without OOM

**Expected Result**:
- Tool completes successfully
- Memory usage reasonable (streaming, not loading entire file at once)
- Response time documented

**Note**: Current implementation uses `Collectors.joining()` which loads entire file into memory. This may cause OOM for very large files.

**Performance Benchmark**:
- 10 MB SARIF: < 2 seconds
- 50 MB SARIF: < 10 seconds
- 100 MB SARIF: < 30 seconds

#### Test 7.2: SARIF with Many Findings
**Objective**: Verify tool handles SARIF with thousands of results.

**Test Data Needed**:
- SARIF with 10,000 results
- Each result has multiple locations

**Steps**:
1. Generate or mock large SARIF with many findings
2. Call tool
3. Verify entire SARIF returned
4. Spot-check first and last results present

**Expected Result**:
- All findings included in response
- No truncation
- No data loss

#### Test 7.3: SARIF with Deep Nesting
**Objective**: Verify tool handles deeply nested JSON structures.

**Test Data Needed**:
- SARIF with deeply nested code flows (20+ levels)
- Complex location data with nested regions

**Steps**:
1. Mock SARIF with deep nesting
2. Call tool
3. Verify entire structure returned intact

**Expected Result**:
- Deep nesting preserved
- No JSON parsing issues from consumer perspective

#### Test 7.4: Performance - Concurrent Requests
**Objective**: Verify tool handles concurrent requests for different projects.

**Test Data Needed**:
- 10 different projects with scans

**Steps**:
1. Call tool concurrently from 10 threads
2. Each thread requests different project
3. Verify all succeed
4. Check for race conditions or resource contention

**Expected Result**:
- All 10 requests succeed
- No thread safety issues
- No resource leaks

#### Test 7.5: Stream Resource Cleanup
**Objective**: Verify InputStream and BufferedReader properly closed.

**Test Data Needed**:
- Any valid SARIF

**Steps**:
1. Call tool multiple times
2. Verify try-with-resources closes streams
3. Check for resource leaks using profiler

**Expected Result**:
- No resource leaks
- All streams closed after method returns
- Try-with-resources on line 98 ensures cleanup

---

## Test Data Setup

### Minimal Test Projects Required

1. **happy-path-project**
   - Has multiple scans
   - Latest scan has valid SARIF with findings
   - Used for basic functionality tests

2. **empty-results-project**
   - Has scans
   - Latest scan has zero findings
   - SARIF has empty results array

3. **no-scans-project**
   - Project exists
   - Never been scanned (lastScanId = null)
   - Used for error handling tests

4. **large-scan-project**
   - Latest scan has large SARIF (50+ MB)
   - 10,000+ findings
   - Used for performance tests

5. **special-chars-project**
   - Project name: "Test Project (Dev)"
   - Used for name resolution tests

### SARIF Test Fixtures

Create reusable SARIF JSON files in `src/test/resources/sarif/`:

1. **minimal-valid.sarif.json** - Minimal valid SARIF 2.1.0
2. **with-findings.sarif.json** - SARIF with 5 diverse findings
3. **empty-results.sarif.json** - Valid SARIF, zero findings
4. **large-file.sarif.json** - 10 MB SARIF with 1000+ findings
5. **unicode.sarif.json** - SARIF with special characters
6. **malformed.json** - Invalid JSON for error testing

---

## Test Implementation Template

```java
package com.contrast.labs.ai.mcp.contrast;

import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.scan.Project;
import com.contrastsecurity.sdk.scan.Scan;
import com.contrastsecurity.sdk.scan.Scans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SastServiceTest {

    private SastService sastService;

    @Mock
    private ContrastSDK mockContrastSDK;

    private MockedStatic<SDKHelper> mockedSDKHelper;

    private static final String TEST_ORG_ID = "test-org-123";
    private static final String TEST_HOST = "https://test.contrast.local";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_SERVICE_KEY = "test-service-key";
    private static final String TEST_USERNAME = "test-user";

    private static final String VALID_SARIF_JSON = """
        {
          "version": "2.1.0",
          "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
          "runs": [{
            "tool": {
              "driver": {
                "name": "Contrast Scan",
                "version": "1.0"
              }
            },
            "results": [{
              "ruleId": "sql-injection",
              "level": "error",
              "message": {
                "text": "SQL Injection vulnerability detected"
              },
              "locations": [{
                "physicalLocation": {
                  "artifactLocation": {
                    "uri": "src/main/java/UserController.java"
                  },
                  "region": {
                    "startLine": 42
                  }
                }
              }]
            }]
          }]
        }
        """;

    @BeforeEach
    void setUp() {
        sastService = new SastService();

        // Mock static SDKHelper
        mockedSDKHelper = mockStatic(SDKHelper.class);
        mockedSDKHelper.when(() -> SDKHelper.getSDK(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(mockContrastSDK);

        // Set required configuration fields
        ReflectionTestUtils.setField(sastService, "orgID", TEST_ORG_ID);
        ReflectionTestUtils.setField(sastService, "hostName", TEST_HOST);
        ReflectionTestUtils.setField(sastService, "apiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(sastService, "serviceKey", TEST_SERVICE_KEY);
        ReflectionTestUtils.setField(sastService, "userName", TEST_USERNAME);
        ReflectionTestUtils.setField(sastService, "httpProxyHost", "");
        ReflectionTestUtils.setField(sastService, "httpProxyPort", "");
    }

    @AfterEach
    void tearDown() {
        if (mockedSDKHelper != null) {
            mockedSDKHelper.close();
        }
    }

    @Test
    void testGetLatestScanResult_HappyPath() throws Exception {
        // Arrange
        Project mockProject = mock(Project.class);
        when(mockProject.id()).thenReturn("proj-123");
        when(mockProject.lastScanId()).thenReturn("scan-456");

        Scans mockScans = mock(Scans.class);
        Scan mockScan = mock(Scan.class);
        InputStream sarifStream = new ByteArrayInputStream(VALID_SARIF_JSON.getBytes());
        when(mockScan.sarif()).thenReturn(sarifStream);

        // Mock SDK call chain
        when(mockContrastSDK.scan(TEST_ORG_ID).projects().findByName("test-project"))
            .thenReturn(Optional.of(mockProject));
        when(mockContrastSDK.scan(TEST_ORG_ID).scans("proj-123")).thenReturn(mockScans);
        when(mockScans.get("scan-456")).thenReturn(mockScan);

        // Act
        String result = sastService.getLatestScanResult("test-project");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("\"version\": \"2.1.0\""));
        assertTrue(result.contains("sql-injection"));
        verify(mockScans).get("scan-456");
    }

    @Test
    void testGetLatestScanResult_ProjectNotFound() throws Exception {
        // Arrange
        when(mockContrastSDK.scan(TEST_ORG_ID).projects().findByName("non-existent"))
            .thenReturn(Optional.empty());

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            sastService.getLatestScanResult("non-existent");
        });
        assertEquals("Project not found", exception.getMessage());
    }

    @Test
    void testGetLatestScanResult_NullLastScanId() throws Exception {
        // Arrange
        Project mockProject = mock(Project.class);
        when(mockProject.id()).thenReturn("proj-123");
        when(mockProject.lastScanId()).thenReturn(null);

        when(mockContrastSDK.scan(TEST_ORG_ID).projects().findByName("no-scans-project"))
            .thenReturn(Optional.of(mockProject));

        // Act & Assert - Currently throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            sastService.getLatestScanResult("no-scans-project");
        });
    }

    @Test
    void testGetLatestScanResult_EmptyStream() throws Exception {
        // Arrange
        Project mockProject = mock(Project.class);
        when(mockProject.id()).thenReturn("proj-123");
        when(mockProject.lastScanId()).thenReturn("scan-456");

        Scans mockScans = mock(Scans.class);
        Scan mockScan = mock(Scan.class);
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        when(mockScan.sarif()).thenReturn(emptyStream);

        when(mockContrastSDK.scan(TEST_ORG_ID).projects().findByName("empty-project"))
            .thenReturn(Optional.of(mockProject));
        when(mockContrastSDK.scan(TEST_ORG_ID).scans("proj-123")).thenReturn(mockScans);
        when(mockScans.get("scan-456")).thenReturn(mockScan);

        // Act
        String result = sastService.getLatestScanResult("empty-project");

        // Assert
        assertNotNull(result);
        assertEquals("", result);
    }

    // Add more test methods following this pattern...
}
```

---

## Success Criteria

### Unit Test Coverage
- Minimum 90% code coverage for `getLatestScanResult` method
- All error paths tested
- All SDK interaction points verified

### Integration Test Coverage
- At least 3 end-to-end tests against real Contrast instance
- Tests cover: happy path, no scans, large SARIF

### MCP Test Coverage
- Verify tool callable via MCP protocol
- Test with Claude or other AI client
- Verify SARIF data usable by AI for analysis

### Performance Benchmarks
- 10 MB SARIF: < 2 seconds
- 50 MB SARIF: < 10 seconds
- No memory leaks across 100 sequential calls

---

## Known Issues and Recommendations

### Issue 1: No Null Check for lastScanId
**Line**: 95
**Issue**: If project has no scans, `project.lastScanId()` returns null, causing NullPointerException
**Recommendation**: Add null check before line 95:
```java
if (project.lastScanId() == null) {
    throw new IOException("Project has no scans");
}
```

### Issue 2: Memory Inefficiency for Large Files
**Line**: 100
**Issue**: `Collectors.joining()` loads entire SARIF into memory
**Recommendation**: For very large SARIFs (100+ MB), consider streaming response or pagination

### Issue 3: No SARIF Validation
**Issue**: Tool returns any data from stream, even if malformed
**Recommendation**: Consider optional validation flag, or document that validation is consumer's responsibility

---

## Test Automation

### CI/CD Integration
1. Run unit tests on every commit
2. Run integration tests on PR to main branch
3. Run performance tests nightly
4. Fail build if coverage drops below 90%

### Test Reporting
- Generate JaCoCo coverage reports
- Track test execution time trends
- Alert on performance regressions

---

## Appendix: SARIF 2.1.0 Schema Reference

### Required Top-Level Fields
- `version`: String, must be "2.1.0"
- `$schema`: String, URL to SARIF schema
- `runs`: Array of run objects

### Run Object Fields
- `tool`: Object describing the analysis tool
- `results`: Array of result objects (findings)
- `artifacts`: Optional, list of scanned files
- `invocations`: Optional, tool invocation details

### Result Object Fields (Finding)
- `ruleId`: String, identifier for the rule
- `level`: String, severity (error, warning, note)
- `message`: Object with text property
- `locations`: Array of location objects
- `fixes`: Optional, suggested remediation

### Further Reading
- [SARIF Specification](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html)
- [SARIF Tutorials](https://github.com/microsoft/sarif-tutorials)

---

## Document Version
- **Version**: 1.0
- **Date**: 2025-01-21
- **Author**: Test Plan Generator
- **Tool Version**: list_Scan_Results (lines 81-108 in SastService.java)

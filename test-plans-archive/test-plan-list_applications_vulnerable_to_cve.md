# Test Plan: list_applications_vulnerable_to_cve MCP Tool

## Overview

This test plan provides comprehensive testing guidance for the `list_applications_vulnerable_to_cve` MCP tool in the Contrast Security MCP server. This tool queries applications vulnerable to a specific CVE and returns detailed vulnerability information including affected applications, servers, and libraries.

**Tool Location**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/SCAService.java` (lines 95-127)

**Tool Signature**:
```java
@Tool(name="list_applications_vulnerable_to_cve",
      description="takes a cve id and returns the applications and servers vulnerable to the cve. Please note if the application class usage is 0, its unlikely to be vulnerable")
public CveData listCVESForApplication(String cveid) throws IOException
```

## Test Data Requirements

Before executing tests, ensure your Contrast Security environment has:
- Multiple applications deployed with instrumented agents
- Applications using libraries with known CVEs (both historical and recent)
- Diverse library versions across different applications
- At least one high-profile CVE in the environment (e.g., Log4Shell, Spring4Shell)
- Some applications with classUsage > 0 and some with classUsage = 0

## Test Categories

---

### 1. Basic Functionality - Querying Applications by CVE

#### Test 1.1: Valid CVE Query
**Objective**: Verify the tool successfully retrieves data for a known CVE

**Test Data Needed**: A valid CVE ID that exists in your Contrast environment (e.g., CVE-2021-44228 for Log4Shell)

**Test Steps**:
1. Call the MCP tool: `list_applications_vulnerable_to_cve` with parameter `cveid="CVE-2021-44228"`
2. Verify the tool returns a CveData object
3. Check that the response completes without throwing an IOException

**Expected Results**:
- Tool returns successfully
- CveData object is not null
- No exceptions thrown

**Verification**:
- Response contains valid CveData structure
- Logs show: "Successfully retrieved data for CVE: CVE-2021-44228, found X vulnerable applications"

---

#### Test 1.2: Case Sensitivity
**Objective**: Verify CVE ID case handling

**Test Data Needed**: A known CVE ID

**Test Steps**:
1. Call tool with uppercase: `CVE-2021-44228`
2. Call tool with lowercase: `cve-2021-44228`
3. Call tool with mixed case: `CvE-2021-44228`

**Expected Results**:
- All variations should work (or fail consistently)
- Document the expected behavior based on Contrast API requirements

---

### 2. CVE ID Format Validation

#### Test 2.1: Valid CVE Formats
**Objective**: Verify tool accepts properly formatted CVE IDs

**Test Data Needed**: CVEs with various valid formats

**Test Steps**:
Test with multiple valid CVE ID formats:
1. Standard format: `CVE-2021-44228`
2. Older CVE format: `CVE-1999-0001`
3. Recent CVE format: `CVE-2024-12345`
4. CVE with longer sequence: `CVE-2023-123456`

**Expected Results**:
- All valid formats are accepted
- Tool queries the Contrast API successfully
- Returns appropriate results or empty results if CVE not in environment

---

#### Test 2.2: Invalid CVE Formats
**Objective**: Verify tool handles malformed CVE IDs appropriately

**Test Data Needed**: Various malformed CVE strings

**Test Steps**:
Test with invalid CVE ID formats:
1. Missing CVE prefix: `2021-44228`
2. Wrong prefix: `CWE-2021-44228`
3. Missing year: `CVE-44228`
4. Missing sequence: `CVE-2021-`
5. Invalid characters: `CVE-2021-ABC`
6. Empty string: ``
7. Null value: `null`
8. Special characters: `CVE-2021-44228; DROP TABLE;`

**Expected Results**:
- Tool handles invalid formats gracefully
- Returns appropriate error messages
- Does not crash the MCP server
- Logs error: "Error retrieving applications vulnerable to CVE: {cveid}"

---

### 3. Data Completeness - CveData Structure Verification

#### Test 3.1: Complete CveData Object
**Objective**: Verify all CveData fields are populated correctly

**Test Data Needed**: A CVE affecting at least one application in your environment

**Test Steps**:
1. Call tool with a known CVE: `CVE-2021-44228`
2. Examine the returned CveData object

**Expected Results**:
Verify CveData contains:

**cve** object with:
- name: CVE ID string
- uuid: UUID string
- description: Text description of the vulnerability
- status: Status string
- score: CVSS score (0.0-10.0)
- availabilityImpact: Impact rating
- confidentialityImpact: Impact rating
- integrityImpact: Impact rating
- accessVector: Access vector type
- accessComplexity: Complexity rating
- authentication: Authentication requirements
- references: List of reference URLs

**impactStats** object with:
- impactedAppCount: Number of affected applications
- totalAppCount: Total applications in organization
- impactedServerCount: Number of affected servers
- totalServerCount: Total servers in organization
- appPercentage: Percentage calculation
- serverPercentage: Percentage calculation

**libraries** list with each Library containing:
- hash: SHA hash of the library
- version: Library version string
- file_name: Filename (e.g., "log4j-core-2.14.1.jar")
- group: Maven group ID or package group

**apps** list with each App containing:
- name: Application name
- app_id: Application UUID
- last_seen: Timestamp (epoch milliseconds)
- first_seen: Timestamp (epoch milliseconds)
- importance_description: Importance level
- classCount: Total classes in vulnerable library (populated by enrichment)
- classUsage: Classes used by application (populated by enrichment)

**servers** list with each Server containing:
- server_id: Server ID integer
- name: Server name
- hostname: Server hostname
- path: Deployment path
- type: Server type (e.g., "Tomcat", "Spring Boot")
- environment: Environment name (e.g., "PRODUCTION", "QA")
- status: Server status

---

#### Test 3.2: Partial Data Scenarios
**Objective**: Verify tool handles scenarios with incomplete data

**Test Data Needed**: CVEs with varying data availability

**Test Steps**:
1. Query a CVE affecting applications but no servers
2. Query a CVE affecting servers but no applications
3. Query a CVE with minimal CVE metadata

**Expected Results**:
- Tool returns successfully even with partial data
- Missing fields are null or empty lists (not causing exceptions)
- Available data is correctly populated

---

### 4. Class Usage Enrichment

#### Test 4.1: Class Count and Usage Population
**Objective**: Verify classCount and classUsage fields are correctly enriched

**Test Data Needed**: A CVE affecting at least one application with class usage data

**Test Steps**:
1. Call tool with CVE affecting multiple applications
2. Examine each App object in the returned apps list
3. Check classCount and classUsage fields

**Expected Results**:
- For apps actually using vulnerable library classes: classUsage > 0
- classCount reflects total classes in the vulnerable library
- classUsage <= classCount (usage cannot exceed total)
- Enrichment logic (lines 109-121) successfully matches libraries by hash

**Verification Code Path**:
```java
// From SCAService.java lines 109-121
for(App app : result.getApps()) {
    List<LibraryExtended> libData = SDKHelper.getLibsForID(app.getApp_id(), orgID, extendedSDK);
    for(LibraryExtended lib:libData) {
        for(Library vulnLib:vulnerableLibs) {
            if(lib.getHash().equals(vulnLib.getHash())) {
                if(lib.getClassedUsed()>0) {
                    app.setClassCount(lib.getClassCount());
                    app.setClassUsage(lib.getClassedUsed());
                }
            }
        }
    }
}
```

---

#### Test 4.2: Zero Class Usage Interpretation
**Objective**: Verify handling of applications with classUsage = 0

**Test Data Needed**: Application with vulnerable library but classUsage = 0

**Test Steps**:
1. Query CVE affecting applications with varying class usage
2. Identify apps with classUsage = 0

**Expected Results**:
- Apps with classUsage = 0 are still returned in the results
- These apps should be considered "unlikely to be vulnerable" per tool description
- classUsage field is explicitly 0 (not null or unset)

**Interpretation Guidance**:
As noted in the tool description: "if the application class usage is 0, its unlikely to be vulnerable"
- classUsage = 0: Library present but classes not used
- classUsage > 0: Library classes actively used, vulnerable

---

#### Test 4.3: Multiple Libraries Matching
**Objective**: Verify correct handling when multiple vulnerable libraries exist

**Test Data Needed**: Application using multiple versions of the same library, or multiple libraries with same CVE

**Test Steps**:
1. Query CVE affecting applications with multiple library matches
2. Verify enrichment logic handles multiple iterations correctly

**Expected Results**:
- All matching libraries are processed
- classCount and classUsage reflect the appropriate values
- No duplicate or incorrect enrichment

---

### 5. Multiple Applications

#### Test 5.1: CVE Affecting Multiple Applications
**Objective**: Verify tool returns all affected applications

**Test Data Needed**: A widespread CVE affecting 5+ applications (e.g., Log4Shell)

**Test Steps**:
1. Call tool with widespread CVE
2. Count returned applications
3. Verify each application has complete data

**Expected Results**:
- All affected applications are returned in apps list
- Each App object is properly populated
- impactStats.impactedAppCount matches apps.size()
- Log message shows correct count: "found X vulnerable applications"

---

#### Test 5.2: Diverse Application Types
**Objective**: Verify tool handles different application types and languages

**Test Data Needed**: CVE affecting Java, .NET, Node.js, or other supported application types

**Test Steps**:
1. Query CVE affecting diverse applications
2. Examine app details for each type

**Expected Results**:
- Applications of different types are all returned
- Language-specific details are preserved
- Enrichment works across different language ecosystems

---

### 6. Empty Results

#### Test 6.1: CVE with No Vulnerable Applications
**Objective**: Verify tool handles CVEs not affecting any applications

**Test Data Needed**: Valid CVE ID that exists in CVE database but doesn't affect deployed applications

**Test Steps**:
1. Call tool with CVE not in your environment (e.g., `CVE-2023-99999` if not used)
2. Examine returned CveData

**Expected Results**:
- Tool returns successfully without exceptions
- CveData object returned (not null)
- apps list is empty or null
- libraries list is empty or null
- servers list is empty or null
- impactStats shows impactedAppCount = 0
- Log shows: "found 0 vulnerable applications"

---

#### Test 6.2: Non-Existent CVE
**Objective**: Verify tool handles CVE IDs that don't exist in CVE database

**Test Data Needed**: Fake CVE ID (e.g., `CVE-9999-99999`)

**Test Steps**:
1. Call tool with non-existent CVE ID
2. Observe error handling

**Expected Results**:
- Tool handles gracefully
- Returns appropriate error or empty result
- Logs error appropriately
- IOException with message "Failed to retrieve CVE data: ..."

---

### 7. High-Profile CVEs

#### Test 7.1: Log4Shell (CVE-2021-44228)
**Objective**: Test with Log4Shell, the most famous recent CVE

**Test Data Needed**: Environment with Log4j 2.x (versions 2.0-beta9 through 2.14.1)

**Test Steps**:
1. Call tool with `CVE-2021-44228`
2. Verify comprehensive results

**Expected Results**:
- Complete CVE details with high CVSS score (10.0)
- All applications using affected Log4j versions are returned
- Library data shows log4j-core versions
- Class usage data distinguishes actively vulnerable apps

**Verification Points**:
- cve.name = "CVE-2021-44228"
- cve.score = 10.0
- cve.description contains "Log4j" and "JNDI"
- libraries contain log4j-core artifacts
- Apps with classUsage > 0 are primary remediation targets

---

#### Test 7.2: Spring4Shell (CVE-2022-22965)
**Objective**: Test with Spring4Shell vulnerability

**Test Data Needed**: Environment with Spring Framework 5.3.0 through 5.3.17 or 5.2.0 through 5.2.19

**Test Steps**:
1. Call tool with `CVE-2022-22965`
2. Analyze Spring-specific results

**Expected Results**:
- CVE details specific to Spring Framework
- Applications using vulnerable Spring versions
- Libraries show spring-framework or spring-beans artifacts

---

#### Test 7.3: Struts2 RCE (CVE-2017-5638)
**Objective**: Test with historical high-impact CVE

**Test Data Needed**: Environment with Apache Struts 2.3.5 through 2.3.31 or 2.5.0 through 2.5.10

**Test Steps**:
1. Call tool with `CVE-2017-5638`
2. Verify results for older CVE

**Expected Results**:
- Historical CVE data retrieved correctly
- Applications using Struts2 are identified
- Library versions match vulnerability window

---

#### Test 7.4: Text4Shell (CVE-2022-42889)
**Objective**: Test with Apache Commons Text vulnerability

**Test Data Needed**: Environment with Apache Commons Text 1.5 through 1.9

**Test Steps**:
1. Call tool with `CVE-2022-42889`
2. Analyze results

**Expected Results**:
- CVE details for Commons Text
- Affected applications identified
- Library matches commons-text artifacts

---

### 8. Library Correlation

#### Test 8.1: Library-Application Matching
**Objective**: Verify libraries list correlates with applications

**Test Data Needed**: CVE affecting 2+ applications with same or different library versions

**Test Steps**:
1. Query CVE affecting multiple applications
2. For each App in apps list:
   - Verify corresponding Library exists in libraries list
   - Match by library hash
   - Confirm version correlation

**Expected Results**:
- Every vulnerable library used by returned apps is in libraries list
- Library hashes match between LibraryExtended (used in enrichment) and Library (in results)
- Library versions are accurately represented

---

#### Test 8.2: Library Deduplication
**Objective**: Verify libraries list doesn't contain duplicates

**Test Data Needed**: CVE where multiple apps use the same library version

**Test Steps**:
1. Query CVE where 3+ apps use identical library
2. Count unique libraries by hash in libraries list

**Expected Results**:
- Each unique library appears once in libraries list
- Multiple apps can reference same library
- Hash-based deduplication works correctly

---

#### Test 8.3: Version Variation
**Objective**: Verify multiple versions of same library are properly represented

**Test Data Needed**: CVE affecting multiple versions of a library (e.g., Log4j 2.12.1, 2.13.0, 2.14.1)

**Test Steps**:
1. Query CVE with multiple vulnerable versions
2. Examine libraries list for version diversity

**Expected Results**:
- Each vulnerable version appears as separate Library entry
- Different hashes for different versions
- Apps correctly associated with their specific version

---

### 9. Server Information

#### Test 9.1: Server Data Completeness
**Objective**: Verify server information is properly included

**Test Data Needed**: CVE affecting applications deployed on multiple servers

**Test Steps**:
1. Query CVE affecting server-deployed applications
2. Examine servers list

**Expected Results**:
- All servers hosting vulnerable applications are listed
- Each Server object contains:
  - server_id: Valid integer
  - name: Server name
  - hostname: Resolvable hostname or IP
  - path: Deployment path
  - type: Server type (e.g., "Tomcat", "WebLogic", "Spring Boot")
  - environment: Environment designation
  - status: Current status

---

#### Test 9.2: Server-Application Relationship
**Objective**: Verify logical relationship between servers and applications

**Test Data Needed**: Multi-server deployment of vulnerable applications

**Test Steps**:
1. Query CVE affecting distributed applications
2. Cross-reference apps and servers

**Expected Results**:
- Servers list correlates with applications returned
- impactStats.impactedServerCount matches servers.size()
- Server count makes sense relative to application count

---

#### Test 9.3: Environment-Based Filtering
**Objective**: Understand server environment representation

**Test Data Needed**: Vulnerable applications in PRODUCTION, QA, and DEV environments

**Test Steps**:
1. Query CVE affecting multiple environments
2. Examine server.environment field

**Expected Results**:
- All environments are represented
- Environment field accurately reflects deployment
- Helps prioritize PRODUCTION vulnerabilities

---

### 10. Performance

#### Test 10.1: CVE Affecting Many Applications
**Objective**: Verify tool performance with large result sets

**Test Data Needed**: Widespread CVE affecting 20+ applications (e.g., Log4Shell in large organization)

**Test Steps**:
1. Call tool with widespread CVE
2. Measure response time
3. Verify all data is returned

**Expected Results**:
- Tool completes within reasonable time (< 30 seconds)
- All applications are returned (no truncation)
- No timeout exceptions
- Memory usage remains acceptable

**Performance Notes**:
- Tool makes multiple API calls (line 110: getLibsForID for each app)
- Enrichment loop is O(n*m*k) where:
  - n = number of apps
  - m = libraries per app
  - k = vulnerable libraries
- For large result sets, expect longer execution times

---

#### Test 10.2: Repeated Queries
**Objective**: Verify tool handles repeated identical queries

**Test Data Needed**: Any valid CVE

**Test Steps**:
1. Call tool with same CVE 5 times in succession
2. Compare results and timing

**Expected Results**:
- Results are consistent across calls
- No caching-related issues
- Performance remains stable
- No connection pool exhaustion

---

#### Test 10.3: Concurrent Queries
**Objective**: Verify tool handles concurrent CVE queries

**Test Data Needed**: Multiple different CVE IDs

**Test Steps**:
1. Query 3-5 different CVEs concurrently
2. Verify all return successfully

**Expected Results**:
- All queries complete successfully
- No resource contention errors
- Results are correct for each CVE
- No data mixing between queries

---

## Error Scenarios

### Test E.1: API Connection Failure
**Objective**: Verify tool handles Contrast API connection issues

**Test Steps**:
1. Simulate network issues (invalid hostname, firewall block)
2. Call tool

**Expected Results**:
- IOException thrown with descriptive message
- Error logged: "Error retrieving applications vulnerable to CVE: {cveid}"
- MCP server remains stable

---

### Test E.2: Authentication Failure
**Objective**: Verify tool handles authentication problems

**Test Steps**:
1. Configure invalid API credentials
2. Call tool

**Expected Results**:
- Authentication error returned
- IOException with auth failure message
- Security credentials not exposed in logs

---

### Test E.3: Malformed API Response
**Objective**: Verify tool handles unexpected API responses

**Test Steps**:
1. Query CVE that triggers malformed response (if possible)
2. Observe error handling

**Expected Results**:
- Tool catches parsing exceptions
- IOException with "Failed to retrieve CVE data"
- Stack trace logged for debugging

---

## Integration Testing

### Test I.1: MCP Protocol Compliance
**Objective**: Verify tool works correctly through MCP protocol

**Test Steps**:
1. Connect to MCP server using MCP client
2. List available tools and verify `list_applications_vulnerable_to_cve` is present
3. Invoke tool through MCP protocol with valid CVE
4. Verify JSON serialization of CveData

**Expected Results**:
- Tool appears in MCP tool list
- Tool description is clear and accurate
- Parameters are correctly described
- Return value is properly serialized to JSON
- All nested objects (Cve, ImpactStats, Library, App, Server) serialize correctly

---

### Test I.2: AI Agent Usage
**Objective**: Verify tool is usable by AI agents

**Test Steps**:
1. Have AI agent invoke tool with natural language query like:
   - "What applications are vulnerable to Log4Shell?"
   - "Show me apps affected by CVE-2021-44228"
2. Verify AI correctly interprets results

**Expected Results**:
- AI successfully invokes tool with correct CVE ID
- AI interprets CveData structure correctly
- AI understands classUsage = 0 implications
- AI can present results in human-readable format

---

## Logging and Observability

### Test L.1: Log Message Verification
**Objective**: Verify appropriate log messages are generated

**Test Data Needed**: Any valid CVE

**Test Steps**:
1. Enable DEBUG logging: `--logging.level.root=DEBUG`
2. Call tool with valid CVE
3. Examine logs at `/tmp/mcp-contrast.log`

**Expected Log Messages**:
```
INFO: Retrieving applications vulnerable to CVE: CVE-2021-44228
DEBUG: ContrastSDK initialized with host: {hostname}
INFO: Successfully retrieved data for CVE: CVE-2021-44228, found X vulnerable applications
INFO: {CveData.toString() output}
```

For errors:
```
ERROR: Error retrieving applications vulnerable to CVE: CVE-2021-44228
```

---

### Test L.2: Sensitive Data Protection
**Objective**: Verify credentials and sensitive data are not logged

**Test Steps**:
1. Enable DEBUG logging
2. Call tool
3. Search logs for API keys, service keys, usernames

**Expected Results**:
- No API credentials in logs
- No service keys in logs
- Hostnames logged but not full URLs with auth
- CVE data logged but properly sanitized

---

## Regression Testing

### Test R.1: Backward Compatibility
**Objective**: Ensure tool works with existing CVE queries

**Test Data Needed**: List of CVE IDs previously tested

**Test Steps**:
1. Re-run all CVE queries from previous test runs
2. Compare results

**Expected Results**:
- All previously working CVEs still work
- Data structure remains compatible
- No breaking changes in API

---

## Test Execution Checklist

- [ ] All 10 main test categories executed
- [ ] Error scenarios tested
- [ ] Integration tests completed
- [ ] Logging verified
- [ ] Performance benchmarks recorded
- [ ] All high-profile CVEs tested (Log4Shell, Spring4Shell, etc.)
- [ ] Edge cases documented
- [ ] Results documented with screenshots/logs
- [ ] Issues filed for any bugs found
- [ ] Test coverage report generated

---

## Expected Test Data Summary

To successfully execute this test plan, your Contrast Security environment should have:

1. **CVE Coverage**:
   - At least 3-5 different CVEs affecting your applications
   - At least one high-profile CVE (Log4Shell recommended)
   - Mix of high, medium, and low severity CVEs

2. **Application Diversity**:
   - Minimum 5 applications with Contrast agents
   - Multiple application types (web, API, microservices)
   - Applications in different environments (PROD, QA, DEV)

3. **Library Variations**:
   - Multiple versions of same library across applications
   - Some applications with classUsage > 0
   - Some applications with classUsage = 0

4. **Server Deployment**:
   - Multiple servers hosting applications
   - Different server types (Tomcat, JBoss, Spring Boot, etc.)

5. **Edge Cases**:
   - At least one CVE with no affected applications
   - At least one CVE affecting 10+ applications

---

## Notes for AI Test Executors

When executing this test plan as an AI agent:

1. **Start Simple**: Begin with Test 1.1 (Basic Functionality) to ensure connectivity
2. **Document Results**: For each test, record:
   - CVE ID used
   - Number of apps/libraries/servers returned
   - Any errors encountered
   - Execution time
3. **Class Usage Context**: Always check classUsage when assessing vulnerability impact
4. **Prioritization**: Focus on:
   - CVEs with classUsage > 0
   - PRODUCTION environment servers
   - High CVSS score CVEs (>= 7.0)
5. **Error Handling**: If a test fails, try to determine if it's:
   - Tool bug
   - Test data issue
   - Environment configuration issue
6. **Security**: Never log or expose actual API credentials or organization IDs

---

## Success Criteria

This tool is considered fully tested and production-ready when:

- ✅ All 10 main test categories pass
- ✅ Error scenarios are handled gracefully
- ✅ Performance is acceptable (< 30s for large result sets)
- ✅ MCP protocol integration works correctly
- ✅ AI agents can successfully use the tool
- ✅ Logging is appropriate and secure
- ✅ High-profile CVEs (Log4Shell, etc.) return accurate results
- ✅ Class usage enrichment works correctly
- ✅ No security vulnerabilities in error handling or logging

---

## Revision History

- **Version 1.0** (2025-10-21): Initial comprehensive test plan created

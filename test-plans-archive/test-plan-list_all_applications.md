# Test Plan: list_all_applications Tool

## Overview
This test plan provides comprehensive testing guidance for the `list_all_applications` tool in AssessService.java (lines 404-426). This tool takes no parameters and returns all applications with their complete data from a Contrast Security organization.

## Tool Specification

**Method**: `getAllApplications()`
**MCP Tool Name**: `list_all_applications`
**Parameters**: None
**Returns**: `List<ApplicationData>`

### ApplicationData Structure
```java
record ApplicationData(
    String name,              // Application name
    String status,            // Application status (e.g., "enabled", "disabled")
    String appID,             // Unique application identifier
    String lastSeenAt,        // ISO-8601 formatted timestamp with timezone
    String language,          // Programming language (e.g., "Java", "Python")
    List<Metadata> metadata,  // List of name-value metadata pairs
    List<String> tags,        // User-defined tags
    List<String> technologies // Technologies/frameworks used
)
```

### Metadata Structure
```java
record Metadata(String name, String value)
```

## Test Categories

---

## 1. Basic Functionality Tests

### Test Case 1.1: Retrieve All Applications - Success Path
**Objective**: Verify the tool successfully retrieves all applications from an organization

**Prerequisites**:
- Valid Contrast Security credentials configured
- Organization with at least 3-5 applications
- Applications in various states (enabled, disabled)

**Test Data Requirements**:
- Organization ID: Standard test organization
- Applications: 3-5 applications with varying configurations

**Test Steps**:
1. Call `list_all_applications` tool with no parameters
2. Verify HTTP 200 response from Contrast API
3. Verify method returns List<ApplicationData>
4. Verify list is not null
5. Verify list contains expected number of applications (>= 3)

**Expected Results**:
- Tool executes without exceptions
- Returns non-null List<ApplicationData>
- List size matches actual application count in organization
- Log entry: "Listing all applications"
- Log entry: "Found {N} applications"

**Validation Points**:
- No IOException thrown
- No NullPointerException
- Response time < 5 seconds (without cache)

---

### Test Case 1.2: SDK Initialization
**Objective**: Verify proper SDK initialization with correct credentials

**Prerequisites**:
- Valid configuration properties set

**Test Data Requirements**:
- hostName: Valid Contrast TeamServer URL
- apiKey: Valid API key
- serviceKey: Valid service key
- userName: Valid username
- orgID: Valid organization ID
- httpProxyHost/Port: Configured if proxy required

**Test Steps**:
1. Call `list_all_applications` tool
2. Verify SDK initialization occurs with correct parameters
3. Verify SDK connection is established

**Expected Results**:
- SDK initialized via SDKHelper.getSDK()
- Connection established to Contrast TeamServer
- Authentication succeeds
- No connection errors

---

## 2. Data Completeness Tests

### Test Case 2.1: Verify All ApplicationData Fields
**Objective**: Verify all 8 fields of ApplicationData are populated correctly

**Prerequisites**:
- Organization with at least 2 applications
- Applications with complete data (all fields populated)

**Test Data Requirements**:
Create/use applications with:
- **name**: "Test Application 1"
- **status**: "enabled"
- **appID**: Valid UUID format
- **lastSeenAt**: Recent timestamp (within last 7 days)
- **language**: "Java"
- **metadata**: At least 2 metadata entries (e.g., "environment"="production", "team"="security")
- **tags**: At least 2 tags (e.g., "critical", "payment-processing")
- **technologies**: At least 2 technologies (e.g., "Spring Boot", "PostgreSQL")

**Test Steps**:
1. Call `list_all_applications` tool
2. Select first application from results
3. Verify each field is populated:
   - `name` is non-null, non-empty string
   - `status` is non-null string (valid values: "enabled", "disabled")
   - `appID` is non-null, valid UUID format
   - `lastSeenAt` is non-null, ISO-8601 formatted timestamp
   - `language` is non-null string
   - `metadata` is non-null list (may be empty)
   - `tags` is non-null list (may be empty)
   - `technologies` is non-null list (may be empty)

**Expected Results**:
- All 8 fields present in response
- No null values for required fields
- Collections (metadata, tags, technologies) are non-null (empty lists acceptable)
- Field types match specification

**Validation Points**:
```
name != null && !name.isEmpty()
status != null && (status.equals("enabled") || status.equals("disabled"))
appID != null && appID.matches("[0-9a-f-]{36}")
lastSeenAt != null && lastSeenAt.matches("\\d{4}-\\d{2}-\\d{2}T.*")
language != null
metadata != null
tags != null
technologies != null
```

---

### Test Case 2.2: Timestamp Formatting
**Objective**: Verify lastSeenAt timestamp is correctly formatted

**Prerequisites**:
- Organization with applications
- Applications with lastSeen values

**Test Data Requirements**:
- Application with lastSeen timestamp set to known value (e.g., 1736938200000L = 2025-01-15T10:30:00Z)

**Test Steps**:
1. Call `list_all_applications` tool
2. Extract lastSeenAt field from first application
3. Verify format matches ISO-8601 with timezone offset
4. Parse timestamp and verify it's valid

**Expected Results**:
- Format matches: `yyyy-MM-dd'T'HH:mm:ss±HH:MM` (e.g., "2025-01-15T05:30:00-05:00")
- Timestamp is parseable by standard ISO-8601 parsers
- Timezone offset is included (not 'Z' format)
- Uses numeric timezone offset (e.g., "-05:00" not "EST")

**Validation Points**:
- FilterHelper.formatTimestamp() converts epoch milliseconds correctly
- Pattern matches: `\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}`
- No 'Z' suffix (uses explicit numeric offset)

---

### Test Case 2.3: Metadata Field Completeness
**Objective**: Verify metadata is correctly transformed from SDK Application model

**Prerequisites**:
- Organization with applications
- At least one application with metadata entities

**Test Data Requirements**:
Application with metadata:
- name: "environment", value: "production"
- name: "team", value: "backend"
- name: "compliance", value: "PCI-DSS"

**Test Steps**:
1. Call `list_all_applications` tool
2. Find application with metadata
3. Verify metadata list structure
4. Verify each metadata entry has name and value
5. Verify metadata count matches source

**Expected Results**:
- metadata field is List<Metadata>
- Each entry has non-null name and value
- All metadata from source Application is included
- No metadata entries are lost during transformation

**Validation Points**:
```
for (Metadata m : app.metadata()) {
    assertNotNull(m.name());
    assertNotNull(m.value());
}
```

---

### Test Case 2.4: Tags Field Verification
**Objective**: Verify tags are correctly included in response

**Prerequisites**:
- Organization with applications
- Applications with user-defined tags

**Test Data Requirements**:
- Application with tags: ["critical", "customer-facing", "payment-processing"]
- Application with no tags: []

**Test Steps**:
1. Call `list_all_applications` tool
2. Find application with tags
3. Verify tags list contains expected values
4. Find application without tags
5. Verify empty list (not null)

**Expected Results**:
- tags field is List<String>
- Tagged app has non-empty list with correct values
- Untagged app has empty list (not null)
- Tag order is preserved

---

### Test Case 2.5: Technologies Field Verification
**Objective**: Verify technologies/frameworks are correctly included

**Prerequisites**:
- Organization with applications
- Applications using various technologies

**Test Data Requirements**:
- Java application with techs: ["Spring Boot", "Hibernate", "PostgreSQL"]
- Python application with techs: ["Django", "Celery", "Redis"]

**Test Steps**:
1. Call `list_all_applications` tool
2. Find Java application
3. Verify technologies list contains Java frameworks
4. Find Python application
5. Verify technologies list contains Python frameworks

**Expected Results**:
- technologies field is List<String>
- Each application's tech stack is correctly represented
- Technology names are non-null, non-empty strings

---

## 3. Empty Results Tests

### Test Case 3.1: Organization with No Applications
**Objective**: Verify tool handles organization with zero applications gracefully

**Prerequisites**:
- Access to empty organization OR ability to create new organization
- Valid credentials for empty organization

**Test Data Requirements**:
- Organization ID with no applications registered

**Test Steps**:
1. Configure tool with empty organization credentials
2. Call `list_all_applications` tool
3. Verify response structure

**Expected Results**:
- No exceptions thrown
- Returns empty List<ApplicationData> (not null)
- List.size() == 0
- Log entry: "Found 0 applications"

**Validation Points**:
```
List<ApplicationData> result = getAllApplications();
assertNotNull(result);
assertTrue(result.isEmpty());
assertEquals(0, result.size());
```

---

### Test Case 3.2: Applications with Minimal Data
**Objective**: Verify tool handles applications with minimal/sparse data

**Prerequisites**:
- Organization with newly onboarded applications
- Applications without metadata, tags, or technologies

**Test Data Requirements**:
- Application with only required fields:
  - name: "Minimal App"
  - status: "enabled"
  - appID: valid UUID
  - lastSeen: timestamp
  - language: "Java"
  - metadata: [] (empty)
  - tags: [] (empty)
  - technologies: [] (empty)

**Test Steps**:
1. Call `list_all_applications` tool
2. Find minimal application
3. Verify required fields are populated
4. Verify optional collections are empty lists (not null)

**Expected Results**:
- Application included in results
- Required fields populated
- Empty collections are empty lists, not null
- No NullPointerException during processing

---

## 4. Volume Tests

### Test Case 4.1: Large Number of Applications (100+)
**Objective**: Verify tool handles organizations with many applications

**Prerequisites**:
- Organization with 100+ applications OR
- Ability to create test organization with bulk applications

**Test Data Requirements**:
- Organization with 100-500 applications
- Mix of enabled/disabled applications
- Various languages and technologies

**Test Steps**:
1. Call `list_all_applications` tool
2. Measure response time
3. Verify all applications are returned
4. Check memory usage during processing

**Expected Results**:
- All applications returned (count matches organization total)
- Response time < 30 seconds
- No OutOfMemoryError
- No timeout exceptions
- Proper pagination handling in SDK

**Performance Benchmarks**:
- 100 apps: < 10 seconds
- 500 apps: < 30 seconds
- Memory usage: < 512MB heap

---

### Test Case 4.2: Applications with Large Metadata Sets
**Objective**: Verify tool handles applications with extensive metadata

**Prerequisites**:
- Applications with large metadata sets

**Test Data Requirements**:
- Application with 50+ metadata entries
- Application with 20+ tags
- Application with 15+ technologies

**Test Steps**:
1. Call `list_all_applications` tool
2. Find application with large metadata
3. Verify all metadata entries are included
4. Verify no truncation occurs

**Expected Results**:
- All metadata entries present in response
- All tags present
- All technologies present
- No data truncation
- Response size proportional to data volume

---

### Test Case 4.3: Concurrent Request Handling
**Objective**: Verify tool handles multiple concurrent requests

**Prerequisites**:
- Organization with applications
- Ability to make parallel requests

**Test Steps**:
1. Make 5 concurrent calls to `list_all_applications`
2. Verify all requests complete successfully
3. Verify results are consistent
4. Check for race conditions

**Expected Results**:
- All 5 requests succeed
- Results are consistent across all responses
- No deadlocks or race conditions
- Cache behaves correctly under concurrent load

---

## 5. Caching Tests

### Test Case 5.1: Cache Hit on Second Request
**Objective**: Verify caching mechanism improves performance on subsequent requests

**Prerequisites**:
- Organization with applications
- Fresh application instance (no cached data)

**Test Data Requirements**:
- Organization with 10+ applications
- Cache TTL: 10 minutes (default)

**Test Steps**:
1. Clear application cache (if possible) or restart service
2. Call `list_all_applications` tool (Request 1 - Cache Miss)
3. Record response time (T1)
4. Immediately call `list_all_applications` again (Request 2 - Cache Hit)
5. Record response time (T2)
6. Verify cache hit in logs

**Expected Results**:
- Request 1: Log shows "Cache miss for applications in org: {orgId}, fetching from API"
- Request 2: Log shows "Cache hit for applications in org: {orgId}"
- T2 significantly faster than T1 (T2 < T1/10)
- Both requests return identical data
- Request 2 does NOT hit Contrast API

**Performance Expectations**:
- Cache miss (T1): 2-5 seconds
- Cache hit (T2): < 100ms
- Speed improvement: 20x-50x faster

**Validation Points**:
```
T2 < 200ms
T2 < T1 / 10
verify(contrastSDK, times(1)).getApplications(orgId)  // Only called once
```

---

### Test Case 5.2: Cache Key Isolation
**Objective**: Verify cache keys are properly isolated per organization

**Prerequisites**:
- Access to 2+ organizations
- Each organization has different applications

**Test Data Requirements**:
- Organization A with apps: ["App-A1", "App-A2"]
- Organization B with apps: ["App-B1", "App-B2"]

**Test Steps**:
1. Configure tool for Organization A
2. Call `list_all_applications` (loads Org A into cache)
3. Verify results contain App-A1, App-A2
4. Reconfigure tool for Organization B
5. Call `list_all_applications` (loads Org B into cache)
6. Verify results contain App-B1, App-B2 (NOT App-A1, App-A2)

**Expected Results**:
- Cache key format: "applications:{orgId}"
- Org A results cached separately from Org B
- No cross-contamination between organization caches
- Each organization returns only its own applications

---

### Test Case 5.3: Cache Expiration After TTL
**Objective**: Verify cache expires and refreshes after 10-minute TTL

**Prerequisites**:
- Organization with applications
- Ability to wait or manipulate system time

**Test Data Requirements**:
- Organization with known applications
- Cache TTL: 10 minutes

**Test Steps**:
1. Call `list_all_applications` (populates cache at T0)
2. Verify cache hit log on second call at T0+1min
3. Wait or advance time to T0+11min (past TTL)
4. Call `list_all_applications` again
5. Verify cache miss log (cache expired)

**Expected Results**:
- T0+1min: Cache hit log appears
- T0+11min: Cache miss log appears
- T0+11min: API call made to refresh data
- Cache configuration: `expireAfterWrite(10, TimeUnit.MINUTES)`

**Note**: If time manipulation is not possible, this test may need to run as a 10-minute integration test.

---

### Test Case 5.4: Cache Size Limits
**Objective**: Verify cache respects maximum size configuration

**Prerequisites**:
- Understanding of cache configuration

**Test Data Requirements**:
- Cache configuration: `maximumSize(500000)`

**Test Steps**:
1. Review cache configuration in SDKHelper.java
2. Verify cache size limit is set appropriately
3. (If feasible) Load cache with many entries and verify eviction

**Expected Results**:
- Cache configuration includes: `maximumSize(500000)`
- Cache evicts oldest entries when limit reached
- No OutOfMemoryError due to unbounded cache growth

**Configuration Verification**:
```java
private static final Cache<String, List<Application>> applicationsCache = CacheBuilder.newBuilder()
    .maximumSize(500000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build();
```

---

### Test Case 5.5: Cache Consistency After Data Changes
**Objective**: Verify behavior when underlying data changes during cache validity period

**Prerequisites**:
- Organization with applications
- Ability to modify applications in Contrast TeamServer

**Test Data Requirements**:
- Organization with 3 applications

**Test Steps**:
1. Call `list_all_applications` (caches 3 applications)
2. Add new application in Contrast TeamServer
3. Call `list_all_applications` again (within cache TTL)
4. Verify response still shows 3 applications (cached)
5. Wait for cache expiration or clear cache
6. Call `list_all_applications` again
7. Verify response now shows 4 applications

**Expected Results**:
- Within cache TTL: Returns cached data (3 apps)
- After cache expiration: Returns fresh data (4 apps)
- This is expected behavior - cache prioritizes performance over real-time accuracy
- Cache TTL of 10 minutes is reasonable trade-off

**Note**: Document this behavior as expected - caching means up to 10-minute delay in reflecting data changes.

---

## 6. Error Handling Tests

### Test Case 6.1: Invalid Credentials
**Objective**: Verify proper error handling for authentication failures

**Prerequisites**:
- Invalid credentials configured

**Test Data Requirements**:
- Invalid API key or service key
- Valid organization ID

**Test Steps**:
1. Configure tool with invalid credentials
2. Call `list_all_applications` tool
3. Verify error handling

**Expected Results**:
- IOException thrown with descriptive message
- Log entry: "Error listing all applications"
- Error message includes: "Failed to list applications: {reason}"
- No sensitive credential information in logs

---

### Test Case 6.2: Network Connectivity Issues
**Objective**: Verify error handling for network failures

**Prerequisites**:
- Ability to simulate network issues

**Test Data Requirements**:
- Invalid hostname OR network disconnection

**Test Steps**:
1. Configure tool with unreachable host
2. Call `list_all_applications` tool
3. Verify error handling

**Expected Results**:
- IOException thrown
- Error message indicates connection failure
- Log entry shows error details
- No infinite retry loops

---

### Test Case 6.3: API Rate Limiting
**Objective**: Verify behavior when Contrast API rate limits are hit

**Prerequisites**:
- Ability to trigger rate limiting (make many rapid requests)

**Test Steps**:
1. Make many rapid calls to `list_all_applications`
2. Trigger rate limiting response from API
3. Verify error handling

**Expected Results**:
- Appropriate exception thrown (IOException)
- Error message indicates rate limiting
- No application crash
- Subsequent requests succeed after rate limit window

---

## 7. Integration Tests

### Test Case 7.1: MCP Protocol Integration
**Objective**: Verify tool works correctly through MCP protocol

**Prerequisites**:
- MCP server running
- MCP client connected
- Valid configuration

**Test Steps**:
1. Connect MCP client to server
2. List available tools (verify `list_all_applications` appears)
3. Call `list_all_applications` via MCP protocol
4. Verify response format matches MCP specification

**Expected Results**:
- Tool appears in MCP tool list
- Tool description: "Takes no argument and list all the applications"
- Tool accepts no parameters
- Response is valid JSON array of ApplicationData objects
- MCP response structure is correct

---

### Test Case 7.2: Real Contrast TeamServer Integration
**Objective**: End-to-end test with real Contrast Security instance

**Prerequisites**:
- Access to real Contrast TeamServer (dev/staging)
- Valid credentials
- Known test applications

**Test Data Requirements**:
- Real organization with known applications
- Documented expected application count

**Test Steps**:
1. Configure tool with real credentials
2. Call `list_all_applications` tool
3. Verify results match expectations
4. Cross-reference with Contrast UI

**Expected Results**:
- Results match Contrast UI application list
- All visible applications are returned
- Data fields match UI display
- No discrepancies between tool and UI

---

## Test Execution Order

### Recommended Execution Sequence:
1. **Setup Phase**: Test Case 6.1 (verify valid credentials work)
2. **Basic Tests**: Test Cases 1.1, 1.2 (verify basic functionality)
3. **Data Tests**: Test Cases 2.1-2.5 (verify all data fields)
4. **Edge Cases**: Test Cases 3.1, 3.2 (empty results)
5. **Caching Tests**: Test Cases 5.1-5.5 (verify caching behavior)
6. **Volume Tests**: Test Cases 4.1-4.3 (performance and scalability)
7. **Error Tests**: Test Cases 6.1-6.3 (error handling)
8. **Integration Tests**: Test Cases 7.1, 7.2 (end-to-end validation)

---

## Test Data Setup Guide

### Minimal Test Environment
Requires:
- 1 organization with 3-5 applications
- At least 1 application with complete data (all fields populated)
- At least 1 application with minimal data (empty collections)

### Comprehensive Test Environment
Requires:
- 2-3 organizations (for cache isolation testing)
- Organization A: 3-5 applications with varied data
- Organization B: 100+ applications (for volume testing)
- Organization C: 0 applications (empty organization)
- Applications with diverse:
  - Languages: Java, Python, .NET, Node.js
  - Statuses: enabled, disabled
  - Metadata: 0-50 entries per app
  - Tags: 0-20 tags per app
  - Technologies: 0-15 techs per app

---

## Success Criteria

### Overall Test Pass Criteria:
- ✅ All basic functionality tests pass (Test Cases 1.x)
- ✅ All data completeness tests pass (Test Cases 2.x)
- ✅ Empty result handling works correctly (Test Cases 3.x)
- ✅ Performance meets benchmarks for volume (Test Cases 4.x)
- ✅ Caching provides expected performance improvement (Test Cases 5.x)
- ✅ Error handling is robust (Test Cases 6.x)
- ✅ MCP integration works correctly (Test Case 7.1)

### Performance Benchmarks:
- Cache hit response: < 100ms
- Cache miss response (10 apps): < 5 seconds
- Large organization (100 apps): < 10 seconds
- Cache speed improvement: > 20x faster
- Memory usage: < 512MB for 500 apps

### Quality Metrics:
- Zero NullPointerExceptions
- Zero data field omissions
- Zero cache cross-contamination issues
- 100% consistent results on repeated calls (within cache TTL)

---

## Known Limitations

1. **Cache Staleness**: Data can be up to 10 minutes stale due to caching
2. **No Filtering**: Tool returns ALL applications - no filtering capability
3. **No Pagination**: Returns entire list in single response (may be large for big orgs)
4. **Read-Only**: Tool only retrieves data, cannot modify applications

---

## Logging and Debugging

### Key Log Entries to Monitor:
```
INFO  - Listing all applications
DEBUG - Retrieved {N} total applications from Contrast
INFO  - Cache hit for applications in org: {orgId}
INFO  - Cache miss for applications in org: {orgId}, fetching from API
INFO  - Successfully retrieved {N} applications from organization: {orgId}
INFO  - Found {N} applications
ERROR - Error listing all applications [+ exception details]
```

### Debug Mode:
Run with `--logging.level.root=DEBUG` to see:
- Cache hit/miss details
- SDK interaction details
- Detailed application counts
- Timestamp conversion details

---

## Test Report Template

```markdown
# Test Execution Report: list_all_applications

**Date**: YYYY-MM-DD
**Tester**: [Name]
**Environment**: [Dev/Staging/Production]
**MCP Server Version**: 0.0.11

## Test Summary
- Total Test Cases: 25
- Passed: X
- Failed: Y
- Skipped: Z

## Failed Test Cases
[List any failures with details]

## Performance Metrics
- Cache hit response time: X ms
- Cache miss response time: Y ms
- 100-app retrieval time: Z seconds

## Issues Found
[List any defects or concerns]

## Recommendations
[Any suggestions for improvement]
```

---

## Automation Considerations

### Automatable Tests (JUnit):
- Test Cases 1.1, 1.2 (basic functionality)
- Test Cases 2.1-2.5 (data completeness)
- Test Cases 3.1, 3.2 (empty results)
- Test Cases 5.1, 5.2, 5.4 (caching - with mocks)
- Test Cases 6.1, 6.2 (error handling - with mocks)

### Manual/Integration Tests:
- Test Cases 4.1-4.3 (volume - requires large dataset)
- Test Case 5.3 (cache expiration - requires 10-minute wait)
- Test Case 5.5 (cache consistency - requires data modification)
- Test Cases 7.1, 7.2 (integration - requires live environment)

### Example JUnit Test Structure:
```java
@ExtendWith(MockitoExtension.class)
class ListAllApplicationsTest {
    @Mock
    private ContrastSDK mockContrastSDK;

    private MockedStatic<SDKHelper> mockedSDKHelper;
    private AssessService assessService;

    @Test
    void testGetAllApplications_Success() {
        // Test Case 1.1 implementation
    }

    @Test
    void testGetAllApplications_DataCompleteness() {
        // Test Case 2.1 implementation
    }

    @Test
    void testGetAllApplications_EmptyOrganization() {
        // Test Case 3.1 implementation
    }

    @Test
    void testGetAllApplications_CacheHit() {
        // Test Case 5.1 implementation
    }
}
```

---

## Appendix: Code References

### Main Implementation
- **File**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java`
- **Lines**: 404-426
- **Method**: `getAllApplications()`

### Related Code
- **ApplicationData**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/data/ApplicationData.java`
- **Metadata**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/data/Metadata.java`
- **SDKHelper**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/sdkexstension/SDKHelper.java` (lines 259-279)
- **Cache Config**: SDKHelper.java (lines 68-71)
- **FilterHelper**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/FilterHelper.java`

---

## Version History
- **v1.0** (2025-10-21): Initial comprehensive test plan creation

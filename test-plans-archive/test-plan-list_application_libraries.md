# Test Plan: list_application_libraries Tool

> **NOTE (AIML-189)**: As of the consolidation in AIML-189, the duplicate `list_application_libraries` (app_name variant) tool has been removed. The remaining tool has been renamed from `list_application_libraries_by_app_id` to `list_application_libraries` and now exclusively uses application ID as input. Users should call `list_applications_with_name` first to get the application ID from a name.

## Overview
This test plan provides comprehensive coverage for the `list_application_libraries` tool (formerly `list_application_libraries_by_app_id`) in the SCAService class. This tool retrieves library information for a specific application, including CVE vulnerability data and class usage metrics.

**Tool Location**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/SCAService.java` (lines 65-74)

**Test Execution Context**: These tests should be executed by an AI agent using the MCP server against a live Contrast Security instance with appropriate test data.

---

## Test Categories

### 1. Basic Functionality Tests

#### Test 1.1: Valid Application ID with Libraries
**Description**: Request libraries for a valid application that has libraries.

**Input**:
```
appID: "<valid-app-id-with-libraries>"
```

**Expected Behavior**:
- Returns a list of LibraryExtended objects
- List is non-empty (contains at least one library)
- Each library object has complete data structure
- No errors or exceptions

**Test Data Assumptions**:
- Assume a valid application ID exists with at least one library

**Validation Checklist**:
- Response is a List, not null
- List size > 0
- Each item is a LibraryExtended object

---

#### Test 1.2: Valid Application ID with No Libraries
**Description**: Request libraries for a valid application that has no libraries (unlikely but possible).

**Input**:
```
appID: "<valid-app-id-no-libraries>"
```

**Expected Behavior**:
- Returns an empty list (not null)
- No errors or exceptions
- Operation completes successfully

**Test Data Assumptions**:
- Assume a valid application exists with no libraries (or create a test application with no dependencies)

**Validation Checklist**:
- Response is a List, not null
- List size = 0
- No exceptions thrown

---

#### Test 1.3: Invalid Application ID Format
**Description**: Test with an invalid or malformed application ID.

**Input**:
```
Test cases:
- appID: "invalid-app-id-12345"
- appID: "not-a-uuid"
- appID: "12345"
```

**Expected Behavior**:
- Should throw IOException or return empty list
- Error message indicates invalid or not found application
- No server crash or unexpected errors

**Test Data Assumptions**:
- Use application IDs that definitely don't exist in the system

**Validation Checklist**:
- Error handling is graceful
- Error message is descriptive
- No unhandled exceptions

---

#### Test 1.4: Non-Existent Application ID
**Description**: Test with a well-formed UUID that doesn't exist in the system.

**Input**:
```
appID: "00000000-0000-0000-0000-000000000000"
```

**Expected Behavior**:
- Should throw IOException or return empty list
- Error message indicates application not found
- No server crash

**Test Data Assumptions**:
- Use a UUID format that doesn't match any existing application

**Validation Checklist**:
- Error is caught and handled properly
- Response indicates no data found

---

#### Test 1.5: Null or Empty Application ID
**Description**: Test with null or empty string as application ID.

**Input**:
```
Test cases:
- appID: null
- appID: ""
- appID: "   " (whitespace only)
```

**Expected Behavior**:
- Should throw IOException or IllegalArgumentException
- Error message indicates missing or invalid app ID
- Fails gracefully

**Test Data Assumptions**:
- No specific data needed

**Validation Checklist**:
- Proper error handling for null/empty input
- Descriptive error message

---

### 2. Library Data Completeness Tests

#### Test 2.1: Core Library Identification Fields
**Description**: Verify essential library identification fields are populated.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Each library has the following fields populated:
  - `fileName` (not null, not empty)
  - `hash` (unique identifier)
  - `libraryId` (numeric ID)
  - `version` (may be null for some libraries)
  - `fileVersion` (may differ from version)

**Test Data Assumptions**:
- Assume application has standard libraries (Java JAR, .NET DLL, etc.)

**Validation Checklist**:
- fileName exists for all libraries
- hash is present and non-empty
- libraryId is a valid long value

---

#### Test 2.2: Library Metadata Fields
**Description**: Verify library metadata is present.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Libraries have metadata populated:
  - `group` (Maven group ID or equivalent)
  - `grade` (library security grade)
  - `libScore` (numeric library score)
  - `custom` (boolean indicating custom/internal library)

**Test Data Assumptions**:
- Assume application uses mix of open-source and potentially custom libraries

**Validation Checklist**:
- Metadata fields present (may be null for some libraries)
- grade values are valid (A, B, C, D, F, or null)
- libScore is a double value
- custom is boolean

---

#### Test 2.3: Version Information
**Description**: Verify version and outdated information is populated.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Version fields are present:
  - `version` (current version in use)
  - `latestVersion` (latest available version)
  - `fileVersion` (version from file manifest)
  - `monthsOutdated` (integer, 0 if current)
  - `releaseDate` (epoch timestamp)
  - `latestReleaseDate` (epoch timestamp)

**Test Data Assumptions**:
- Assume application uses some outdated libraries

**Validation Checklist**:
- Version strings are present for known libraries
- monthsOutdated >= 0
- Date fields are valid epoch timestamps (or 0)
- latestVersion shows newer version when monthsOutdated > 0

---

#### Test 2.4: Application Context Fields
**Description**: Verify application context fields are populated in library objects.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Each library includes application context:
  - `appId` (matches input appID)
  - `app_name` (application name)
  - `appLanguage` (Java, .NET, Node.js, etc.)
  - `appContextPath` (web context path)
  - `apps` (list of applications using this library)
  - `servers` (list of servers where library is deployed)

**Test Data Assumptions**:
- Assume application has proper configuration in Contrast

**Validation Checklist**:
- appId matches the requested application ID
- app_name is not null or empty
- appLanguage is valid (Java, .NET, Node, Python, Ruby, etc.)
- apps and servers lists are present (not null)

---

#### Test 2.5: Manifest Data
**Description**: Verify manifest data is captured for libraries.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Libraries have `manifest` field populated
- Manifest contains plaintext MANIFEST.MF or equivalent metadata
- May be null for libraries without manifest files

**Test Data Assumptions**:
- Assume Java application with JAR files (which have MANIFEST.MF)

**Validation Checklist**:
- manifest field exists (may be null)
- If populated, contains text data
- For Java JARs, should contain standard manifest entries

---

### 3. Class Usage Analysis Tests

#### Test 3.1: Libraries with Zero Class Usage
**Description**: Identify libraries where classesUsed = 0 (unused libraries).

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Response includes libraries with `classesUsed = 0`
- These libraries are considered "unlikely to be used" per tool description
- `classCount` > 0 but `classesUsed = 0`

**Test Data Assumptions**:
- Assume application has some transitive dependencies that aren't actively used

**Validation Checklist**:
- Can identify libraries with classesUsed = 0
- classCount is a positive integer
- classesUsed is exactly 0

---

#### Test 3.2: Libraries with Active Class Usage
**Description**: Identify libraries that are actively used (classesUsed > 0).

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Response includes libraries with `classesUsed > 0`
- `classesUsed` <= `classCount` (used classes can't exceed total)
- Indicates actual library usage by the application

**Test Data Assumptions**:
- Assume application actively uses some of its libraries

**Validation Checklist**:
- classesUsed > 0
- classesUsed <= classCount
- Both values are positive integers

---

#### Test 3.3: Class Usage Percentage Calculation
**Description**: Calculate usage percentage to identify heavily vs lightly used libraries.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Can calculate: (classesUsed / classCount) * 100
- Identifies libraries with high usage (>50%)
- Identifies libraries with low usage (<10%)
- Identifies libraries with partial usage (10-50%)

**Test Data Assumptions**:
- Assume mix of heavily and lightly used libraries

**Validation Checklist**:
- Can compute usage percentage for all libraries with classCount > 0
- Percentages range from 0% to 100%
- Zero-usage libraries identified

---

#### Test 3.4: Class Usage Edge Cases
**Description**: Test edge cases in class usage data.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Libraries where `classCount = 0` (unusual but possible)
- Libraries where `classesUsed = classCount` (100% usage)
- Libraries with very large class counts (>1000 classes)
- Libraries with very small class counts (1-5 classes)

**Test Data Assumptions**:
- Assume diverse set of libraries with various sizes

**Validation Checklist**:
- classCount >= 0
- classesUsed >= 0
- No negative values
- Handle division by zero when calculating percentages

---

### 4. Vulnerable Libraries Tests

#### Test 4.1: Libraries with CVE Vulnerabilities
**Description**: Identify libraries that have known CVE vulnerabilities.

**Input**:
```
appID: "<valid-app-id-with-vulnerable-libs>"
```

**Expected Behavior**:
- Response includes libraries with non-empty `vulns` list
- `totalVulnerabilities` > 0
- `highVulnerabilities` >= 0 (count of high/critical)
- Each vulnerability in `vulns` has CVE information

**Test Data Assumptions**:
- Assume application uses some libraries with known CVEs (e.g., older versions of log4j, spring-core, etc.)

**Validation Checklist**:
- vulns list is not null
- totalVulnerabilities matches vulns.size()
- Each LibraryVulnerabilityExtended object has severityCode

---

#### Test 4.2: Libraries without Vulnerabilities
**Description**: Identify clean libraries with no known vulnerabilities.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Some libraries have empty `vulns` list
- `totalVulnerabilities = 0`
- `highVulnerabilities = 0`
- Library may still have other issues (outdated, low grade)

**Test Data Assumptions**:
- Assume some libraries are up-to-date with no known CVEs

**Validation Checklist**:
- vulns list is empty (or null)
- totalVulnerabilities = 0
- highVulnerabilities = 0

---

#### Test 4.3: Vulnerability Severity Distribution
**Description**: Analyze severity levels of vulnerabilities across libraries.

**Input**:
```
appID: "<valid-app-id-with-vulnerable-libs>"
```

**Expected Behavior**:
- Libraries with HIGH/CRITICAL vulnerabilities have `highVulnerabilities > 0`
- Can access `severityCode` for each vulnerability
- Severity codes include: CRITICAL, HIGH, MEDIUM, LOW
- `highVulnerabilities` counts only HIGH and CRITICAL

**Test Data Assumptions**:
- Assume vulnerable libraries have mix of severity levels

**Validation Checklist**:
- severityCode is populated for vulnerabilities
- highVulnerabilities count is accurate
- Can filter libraries by vulnerability severity

---

#### Test 4.4: CVE Details in Vulnerabilities
**Description**: Verify CVE vulnerability details are accessible.

**Input**:
```
appID: "<valid-app-id-with-vulnerable-libs>"
```

**Expected Behavior**:
- Each LibraryVulnerabilityExtended object contains:
  - CVE identifier (from parent class)
  - severityCode
  - Additional metadata from LibraryVulnerability base class

**Test Data Assumptions**:
- Assume libraries with documented CVEs

**Validation Checklist**:
- CVE IDs are present and well-formed (CVE-YYYY-NNNNN)
- severityCode is not null
- Can access all vulnerability details

---

#### Test 4.5: Unused Libraries with Vulnerabilities
**Description**: Identify high-risk scenario: vulnerable libraries that aren't used.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Can identify libraries where:
  - `totalVulnerabilities > 0` AND
  - `classesUsed = 0`
- These are lower risk (library present but not used)
- Important for prioritization decisions

**Test Data Assumptions**:
- Assume some transitive dependencies are vulnerable but unused

**Validation Checklist**:
- Can identify unused vulnerable libraries
- classesUsed = 0
- totalVulnerabilities > 0
- Lower priority for remediation

---

#### Test 4.6: Used Libraries with High-Severity Vulnerabilities
**Description**: Identify highest-risk scenario: actively used libraries with critical CVEs.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Can identify libraries where:
  - `classesUsed > 0` AND
  - `highVulnerabilities > 0` (has CRITICAL/HIGH CVEs)
- These are highest priority for remediation
- Actual security risk to the application

**Test Data Assumptions**:
- Assume at least one actively used library has high-severity vulnerabilities

**Validation Checklist**:
- Can identify high-risk libraries
- classesUsed > 0
- highVulnerabilities > 0
- Highest priority for action

---

### 5. Validation Tests

#### Test 5.1: Invalid App ID - Various Formats
**Description**: Test comprehensive set of invalid application ID formats.

**Input**:
```
Test cases:
- appID: "not-a-uuid"
- appID: "12345"
- appID: "app-123-456"
- appID: "INVALID"
- appID: "javascript:alert('xss')" (injection attempt)
```

**Expected Behavior**:
- All invalid formats handled gracefully
- IOException thrown or empty list returned
- Error messages are descriptive
- No security vulnerabilities exposed

**Test Data Assumptions**:
- Use intentionally malformed input

**Validation Checklist**:
- Consistent error handling across all invalid formats
- No exceptions leak sensitive information
- No SQL injection or other security issues

---

#### Test 5.2: Special Characters in App ID
**Description**: Test application IDs with special characters.

**Input**:
```
Test cases:
- appID: "<app-id-with-spaces>"
- appID: "app%20id"
- appID: "app\nid"
- appID: "app'id"
```

**Expected Behavior**:
- Special characters handled properly
- URL encoding/decoding handled by SDK
- Invalid characters rejected gracefully

**Test Data Assumptions**:
- Use various special character combinations

**Validation Checklist**:
- No code injection possible
- Proper input sanitization
- Clear error messages

---

#### Test 5.3: Very Long Application ID
**Description**: Test with abnormally long input string.

**Input**:
```
appID: "<string-with-10000-characters>"
```

**Expected Behavior**:
- Request fails gracefully
- No buffer overflow or memory issues
- Reasonable error message
- Response time is reasonable (not hanging)

**Test Data Assumptions**:
- Generate very long string

**Validation Checklist**:
- No system crash
- Reasonable timeout handling
- Proper error response

---

### 6. Empty Results Tests

#### Test 6.1: Application with No Libraries (Baseline)
**Description**: Test application with absolutely no dependencies.

**Input**:
```
appID: "<app-id-no-libraries>"
```

**Expected Behavior**:
- Returns empty list (not null)
- No errors or warnings
- Clean response structure

**Test Data Assumptions**:
- Create or find application with no library dependencies

**Validation Checklist**:
- Response is empty List (size = 0)
- Response is not null
- No exceptions

---

#### Test 6.2: Newly Created Application
**Description**: Test against a brand new application with no data yet.

**Input**:
```
appID: "<newly-created-app-id>"
```

**Expected Behavior**:
- Returns empty list or minimal data
- No errors
- May have zero libraries until application runs

**Test Data Assumptions**:
- Create a new application in Contrast that hasn't been deployed yet

**Validation Checklist**:
- Handles new applications gracefully
- Returns consistent response structure
- No null pointer exceptions

---

#### Test 6.3: Application with Filtered Results
**Description**: While this tool doesn't have filters, test applications that might return empty due to SDK filtering.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Returns all libraries (no filtering by this tool)
- Internal SDK filtering (limit=50, pagination) handled transparently
- Complete library list returned

**Test Data Assumptions**:
- Assume normal application with libraries

**Validation Checklist**:
- All libraries returned
- Pagination handled internally (transparent to user)
- No libraries omitted unexpectedly

---

### 7. Volume Handling Tests

#### Test 7.1: Application with Few Libraries (<10)
**Description**: Test small application with minimal dependencies.

**Input**:
```
appID: "<app-id-few-libraries>"
```

**Expected Behavior**:
- Returns complete list quickly
- All libraries have full data
- Single API call to SDK (no pagination needed)

**Test Data Assumptions**:
- Simple application with 1-10 libraries

**Validation Checklist**:
- Response time < 2 seconds
- Complete data for all libraries
- No pagination artifacts

---

#### Test 7.2: Application with Moderate Libraries (10-50)
**Description**: Test typical application with normal dependency count.

**Input**:
```
appID: "<app-id-moderate-libraries>"
```

**Expected Behavior**:
- Returns complete list
- May require 1-2 API calls (pagination threshold is 50)
- Reasonable response time
- Cache improves subsequent requests

**Test Data Assumptions**:
- Typical Spring Boot or .NET application with 10-50 libraries

**Validation Checklist**:
- Response time < 5 seconds
- All libraries returned
- Pagination handled transparently

---

#### Test 7.3: Application with Many Libraries (50-100)
**Description**: Test larger application with many dependencies.

**Input**:
```
appID: "<app-id-many-libraries>"
```

**Expected Behavior**:
- Returns complete list
- Requires multiple API calls (page size = 50)
- Pagination handled automatically by SDKHelper
- Complete data despite multiple calls
- Response time acceptable

**Test Data Assumptions**:
- Large application with 50-100 libraries (e.g., enterprise application)

**Validation Checklist**:
- All libraries returned (verify count)
- No duplicates from pagination
- Response time < 10 seconds
- Subsequent calls use cache (< 1 second)

---

#### Test 7.4: Application with Very Many Libraries (>100)
**Description**: Test very large application with extensive dependencies.

**Input**:
```
appID: "<app-id-very-many-libraries>"
```

**Expected Behavior**:
- Returns complete list
- Multiple API calls (3+ calls for 100+ libraries)
- All libraries included
- Pagination logic robust
- May take longer but completes successfully

**Test Data Assumptions**:
- Very large monolithic application or application with many transitive dependencies

**Validation Checklist**:
- All 100+ libraries returned
- No pagination boundary issues
- No truncation of results
- Response time < 30 seconds
- Cache significantly improves subsequent requests

---

#### Test 7.5: Pagination Boundary Conditions
**Description**: Test exactly at pagination boundaries (50, 100, 150 libraries).

**Input**:
```
Test cases:
- appID with exactly 50 libraries
- appID with exactly 51 libraries
- appID with exactly 100 libraries
```

**Expected Behavior**:
- 50 libraries: Single API call, all returned
- 51 libraries: Two API calls (50 + 1), all returned
- 100 libraries: Two API calls (50 + 50), all returned
- No off-by-one errors
- Pagination logic handles boundaries correctly

**Test Data Assumptions**:
- Applications with precise library counts at boundaries

**Validation Checklist**:
- Correct number of libraries returned
- No missing libraries
- No duplicate libraries
- Pagination threshold (50) handled correctly

---

### 8. Caching Behavior Tests

#### Test 8.1: First Request (Cache Miss)
**Description**: Test initial request before cache is populated.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Log message indicates: "Cache miss for appID: {appID}"
- Full SDK query executed
- Response time reflects API call latency
- Result stored in cache

**Test Data Assumptions**:
- Clear cache before test or use app ID not in cache

**Validation Checklist**:
- Log shows "Cache miss"
- Data retrieved from SDK
- Response returned successfully

---

#### Test 8.2: Subsequent Request (Cache Hit)
**Description**: Test repeat request that should hit cache.

**Input**:
```
appID: "<same-app-id-as-previous-test>"
```

**Expected Behavior**:
- Log message indicates: "Cache hit for appID: {appID}"
- No SDK API call made
- Much faster response time (< 100ms)
- Identical data returned

**Test Data Assumptions**:
- Request same app ID within 10 minute cache window

**Validation Checklist**:
- Log shows "Cache hit"
- Response time dramatically faster
- Data identical to first request

---

#### Test 8.3: Cache Expiration (After 10 Minutes)
**Description**: Test that cache expires after configured time.

**Input**:
```
appID: "<valid-app-id>"
(Wait 11 minutes, then request again)
```

**Expected Behavior**:
- After 10 minute expiration, next request is cache miss
- Log indicates: "Cache miss"
- Fresh data retrieved from SDK
- New cache entry created

**Test Data Assumptions**:
- Ability to wait 11+ minutes between requests

**Validation Checklist**:
- Cache expiration honors 10 minute timeout
- Fresh data retrieved after expiration
- New cache entry populated

---

#### Test 8.4: Cache Size Limits
**Description**: Test cache behavior with maximum size (500,000 entries).

**Input**:
```
(Request libraries for 500,000+ different applications)
```

**Expected Behavior**:
- Cache maintains maximum 500,000 entries
- Oldest entries evicted when limit reached
- No memory overflow issues
- LRU (Least Recently Used) eviction policy

**Test Data Assumptions**:
- Ability to generate requests for many applications (may not be practical to test fully)

**Validation Checklist**:
- Cache respects size limit
- No memory issues
- Eviction policy works correctly

---

### 9. Error Handling and Edge Cases

#### Test 9.1: Network Timeout
**Description**: Test behavior when SDK API call times out.

**Input**:
```
appID: "<valid-app-id>"
(Simulate network latency or timeout)
```

**Expected Behavior**:
- IOException thrown after timeout
- Error message indicates timeout or connection issue
- No hanging or infinite wait
- Proper error propagated to caller

**Test Data Assumptions**:
- Ability to simulate network issues

**Validation Checklist**:
- Timeout handled gracefully
- Descriptive error message
- No resource leaks

---

#### Test 9.2: SDK Authentication Failure
**Description**: Test with invalid credentials or expired token.

**Input**:
```
appID: "<valid-app-id>"
(Use invalid API credentials)
```

**Expected Behavior**:
- IOException or UnauthorizedException thrown
- Error indicates authentication failure
- No sensitive information in error message
- Proper exception handling

**Test Data Assumptions**:
- Ability to test with invalid credentials

**Validation Checklist**:
- Authentication errors handled
- Error message appropriate
- No credential leakage in logs

---

#### Test 9.3: Partial Data from SDK
**Description**: Test when SDK returns incomplete library data.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Gracefully handles missing optional fields
- Required fields still validated
- Partial data still usable
- No null pointer exceptions

**Test Data Assumptions**:
- Some libraries may have incomplete data

**Validation Checklist**:
- Handles null optional fields
- No crashes on missing data
- Returns available data

---

#### Test 9.4: Concurrent Requests for Same App ID
**Description**: Test multiple simultaneous requests for same application.

**Input**:
```
(Make 5 concurrent requests for same appID)
```

**Expected Behavior**:
- First request populates cache
- Subsequent requests may hit cache or wait for first
- No race conditions or duplicate API calls
- All requests return consistent data
- Cache coherency maintained

**Test Data Assumptions**:
- Ability to make concurrent requests

**Validation Checklist**:
- Thread-safe cache access
- Consistent results across all requests
- No cache corruption

---

#### Test 9.5: Unicode and International Characters
**Description**: Test with application IDs or library names containing international characters.

**Input**:
```
appID: "<valid-app-id>"
(Application or libraries with non-ASCII names)
```

**Expected Behavior**:
- Unicode characters handled correctly
- Proper encoding/decoding
- Library file names with international characters display correctly
- No encoding errors

**Test Data Assumptions**:
- Application or libraries with Unicode names exist

**Validation Checklist**:
- UTF-8 encoding handled properly
- International characters display correctly
- No encoding exceptions

---

### 10. Data Quality and Consistency Tests

#### Test 10.1: Hash Uniqueness
**Description**: Verify library hash values are unique identifiers.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Each library has unique hash value
- Hash is consistent for same library file
- Different versions of same library have different hashes
- Hash can be used as unique identifier

**Test Data Assumptions**:
- Application with multiple libraries

**Validation Checklist**:
- No duplicate hash values (unless same library appears twice)
- Hash is non-empty for all libraries
- Hash format is consistent

---

#### Test 10.2: Version Consistency
**Description**: Verify version fields are consistent and logical.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- When `monthsOutdated > 0`, `latestVersion` should be different from `version`
- When `monthsOutdated = 0`, library is on latest version
- `latestReleaseDate` >= `releaseDate` (latest is newer or same)
- Version strings follow semantic versioning when applicable

**Test Data Assumptions**:
- Mix of current and outdated libraries

**Validation Checklist**:
- Version logic is consistent
- monthsOutdated correlates with version difference
- Date fields are logical

---

#### Test 10.3: Class Count Consistency
**Description**: Verify class count fields are logical and consistent.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- `classesUsed` <= `classCount` (always)
- Both values >= 0
- `classCount = 0` only for unusual libraries (resources only, native, etc.)
- Relationship makes logical sense

**Test Data Assumptions**:
- Standard applications with normal libraries

**Validation Checklist**:
- classesUsed never exceeds classCount
- No negative values
- Reasonable class counts for library size

---

#### Test 10.4: Vulnerability Count Consistency
**Description**: Verify vulnerability counts match actual vulnerability list.

**Input**:
```
appID: "<valid-app-id-with-vulnerable-libs>"
```

**Expected Behavior**:
- `totalVulnerabilities` = `vulns.size()`
- `highVulnerabilities` = count of CRITICAL + HIGH in `vulns`
- Counts are accurate and match list
- No discrepancies

**Test Data Assumptions**:
- Application with vulnerable libraries

**Validation Checklist**:
- totalVulnerabilities matches list size
- highVulnerabilities count is accurate
- Can verify by counting severities in vulns list

---

#### Test 10.5: Grade and Score Correlation
**Description**: Verify library grade correlates with library score.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Grade "A" should have high libScore
- Grade "F" should have low libScore
- Grades follow expected scoring ranges
- Grade may be null, but if present, correlates with score

**Test Data Assumptions**:
- Libraries with various grades

**Validation Checklist**:
- Grade and score are logically consistent
- Score ranges make sense for grades
- Null grades handled properly

---

### 11. Integration and Cross-Reference Tests

#### Test 11.1: Application Context Consistency
**Description**: Verify application context fields match the requested application.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- All libraries have `appId` matching input
- `app_name` is consistent across all libraries in response
- `appLanguage` matches application's actual language
- `appContextPath` is consistent

**Test Data Assumptions**:
- Normal application

**Validation Checklist**:
- appId matches input for all libraries
- Application metadata is consistent
- No mixed application data

---

#### Test 11.2: Multi-Application Libraries
**Description**: Test libraries that are used by multiple applications.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Libraries may have `apps` list with multiple applications
- Same library (same hash) used across multiple apps
- Current response focuses on requested app
- `apps` list shows all applications using this library

**Test Data Assumptions**:
- Common libraries (e.g., log4j) used by multiple apps

**Validation Checklist**:
- apps list is populated
- Can identify shared libraries
- Current app is in apps list

---

#### Test 11.3: Server Deployment Information
**Description**: Verify server deployment information in library objects.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- `servers` list shows all servers where library is deployed
- Server information includes server IDs and metadata
- Can identify which servers have specific library versions
- Useful for deployment tracking

**Test Data Assumptions**:
- Application deployed to one or more servers

**Validation Checklist**:
- servers list is not null
- Server information is complete
- Can track library deployment across servers

---

### 12. Performance and Scalability Tests

#### Test 12.1: Response Time - Small Application
**Description**: Measure response time for application with few libraries.

**Input**:
```
appID: "<app-id-with-10-libraries>"
```

**Expected Behavior**:
- First request (cache miss): < 2 seconds
- Subsequent requests (cache hit): < 100ms
- Acceptable performance for small dataset

**Test Data Assumptions**:
- Application with ~10 libraries

**Validation Checklist**:
- Response times within acceptable range
- Cache provides significant speedup

---

#### Test 12.2: Response Time - Large Application
**Description**: Measure response time for application with many libraries.

**Input**:
```
appID: "<app-id-with-100-libraries>"
```

**Expected Behavior**:
- First request (cache miss): < 20 seconds
- Subsequent requests (cache hit): < 200ms
- Multiple API calls handled efficiently
- Acceptable performance despite large dataset

**Test Data Assumptions**:
- Application with 100+ libraries

**Validation Checklist**:
- Response times acceptable for large dataset
- Pagination doesn't significantly degrade performance
- Cache provides major improvement

---

#### Test 12.3: Memory Usage
**Description**: Monitor memory usage during library retrieval.

**Input**:
```
(Request libraries for multiple large applications)
```

**Expected Behavior**:
- Memory usage grows with cache size
- No memory leaks
- Cache size limits prevent unlimited growth
- Garbage collection works properly

**Test Data Assumptions**:
- Multiple applications with libraries

**Validation Checklist**:
- Memory usage is reasonable
- No memory leaks over time
- Cache eviction works

---

#### Test 12.4: API Call Efficiency
**Description**: Verify efficient use of Contrast SDK API calls.

**Input**:
```
appID: "<valid-app-id>"
```

**Expected Behavior**:
- Minimum number of API calls needed
- Pagination handled efficiently (50 per call)
- VULNS expansion included (efficient single call)
- No redundant API calls

**Test Data Assumptions**:
- Application with known library count

**Validation Checklist**:
- API call count = ceiling(library_count / 50)
- No duplicate calls
- Expansion used efficiently

---

## Test Execution Guidelines

### Pre-Test Setup

1. **Environment Preparation**:
   - Verify MCP server is running and configured correctly
   - Confirm connection to Contrast Security instance
   - Verify API credentials are valid and have proper permissions
   - Ensure organization ID is correct

2. **Test Data Preparation**:
   - Identify test applications with various characteristics:
     - Small app (< 10 libraries)
     - Medium app (10-50 libraries)
     - Large app (50-100 libraries)
     - Very large app (100+ libraries)
     - App with vulnerable libraries
     - App with outdated libraries
     - App with custom/internal libraries
   - Document application IDs for test cases
   - Know expected library counts for validation

3. **Cache Management**:
   - Clear cache before running tests if needed
   - Understand cache expiration (10 minutes)
   - Plan tests around cache behavior

### During Testing

1. **Execution Best Practices**:
   - Record all inputs and outputs
   - Capture timestamps for performance testing
   - Monitor logs for cache hits/misses
   - Note any warnings or errors
   - Document unexpected behavior

2. **Validation Approach**:
   - Verify response structure for each test
   - Check all field types and values
   - Validate data consistency
   - Confirm error handling
   - Measure performance metrics

3. **Logging and Monitoring**:
   - Check `/tmp/mcp-contrast.log` for detailed logs
   - Monitor cache behavior
   - Track API call counts
   - Note any SDK errors

### Test Data Recommendations

**Ideal Test Environment Should Have**:

1. **Application Diversity**:
   - At least 5-10 test applications
   - Various sizes (10, 50, 100, 150+ libraries)
   - Different languages (Java, .NET, Node.js)
   - Mix of frameworks (Spring Boot, .NET Core, Express)

2. **Library Characteristics**:
   - Mix of open-source and internal libraries
   - Some outdated libraries (monthsOutdated > 0)
   - Some vulnerable libraries with CVEs
   - Libraries with zero class usage (unused dependencies)
   - Libraries with active usage (classesUsed > 0)
   - Range of library sizes (small and large)

3. **Vulnerability Distribution**:
   - Libraries with CRITICAL vulnerabilities
   - Libraries with HIGH vulnerabilities
   - Libraries with MEDIUM/LOW vulnerabilities
   - Libraries with no vulnerabilities
   - Mix of used and unused vulnerable libraries

4. **Edge Cases**:
   - Application with no libraries (if possible)
   - Newly created application
   - Invalid application IDs for error testing
   - Application at pagination boundaries (50, 100, 150 libraries)

### Success Criteria

**Each Test Passes When**:

1. **Functional Correctness**:
   - Response matches expected structure
   - Data is complete and accurate
   - Filtering and pagination work correctly
   - Error handling is appropriate

2. **Data Integrity**:
   - All required fields populated
   - Values are logically consistent
   - No null pointer exceptions
   - Relationships between fields are valid

3. **Performance**:
   - Response times within acceptable limits
   - Cache provides expected speedup
   - Memory usage is reasonable
   - API calls are efficient

4. **Error Handling**:
   - Invalid inputs handled gracefully
   - Error messages are descriptive
   - No sensitive information leakage
   - Proper exception types thrown

5. **Consistency**:
   - Repeated requests return same results
   - Cache behavior is predictable
   - Concurrent requests handled properly
   - Data is consistent across calls

### Known Limitations and Considerations

1. **Caching**:
   - 10-minute cache means data may be stale
   - Cache is in-memory (lost on server restart)
   - Large applications cache large amounts of data

2. **Pagination**:
   - Handled internally and transparently
   - Page size fixed at 50 per SDK call
   - May require multiple calls for large applications

3. **Data Availability**:
   - Vulnerability data depends on Contrast's CVE database
   - Class usage requires agent instrumentation
   - Some fields may be null for certain library types

4. **Performance**:
   - First request slower (cache miss)
   - Large applications (100+ libraries) may take 10-20 seconds
   - Network latency affects response time

5. **SDK Dependencies**:
   - Relies on Contrast SDK API availability
   - SDK version affects available features
   - API changes may affect behavior

---

## Appendix: Example Test Execution

### Example Test Invocation

```
Tool: list_application_libraries_by_app_id
Input: {
  "appID": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Example Successful Response Structure

```json
[
  {
    "fileName": "spring-core-5.3.10.jar",
    "version": "5.3.10",
    "hash": "abc123def456...",
    "libraryId": 12345,
    "classCount": 1250,
    "classesUsed": 345,
    "totalVulnerabilities": 2,
    "highVulnerabilities": 1,
    "vulns": [
      {
        "severityCode": "HIGH"
      },
      {
        "severityCode": "MEDIUM"
      }
    ],
    "grade": "C",
    "libScore": 65.5,
    "monthsOutdated": 8,
    "version": "5.3.10",
    "latestVersion": "5.3.23",
    "appId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "app_name": "MyTestApplication",
    "appLanguage": "Java",
    "custom": false
  },
  ...
]
```

### Example Validation Checks

```
1. Response is a List (not null)
2. List contains > 0 items
3. Each item has fileName
4. Each item has hash
5. classesUsed <= classCount for all items
6. totalVulnerabilities matches vulns.size()
7. monthsOutdated >= 0
8. appId matches input for all items
```

---

## Test Coverage Summary

This test plan covers:

- ✓ 5 basic functionality test cases
- ✓ 5 library data completeness test cases
- ✓ 4 class usage analysis test cases
- ✓ 6 vulnerable libraries test cases
- ✓ 3 validation test cases
- ✓ 3 empty results test cases
- ✓ 5 volume handling test cases
- ✓ 4 caching behavior test cases
- ✓ 5 error handling test cases
- ✓ 5 data quality test cases
- ✓ 3 integration test cases
- ✓ 4 performance test cases

**Total: 52 test cases**

Each test case is designed to be executed by an AI agent using the MCP server, with clear input parameters, expected behaviors, test data assumptions, and validation checklists.

---

## Priority Test Execution Order

### Phase 1: Critical Path (Must Pass)
1. Test 1.1 - Valid Application ID with Libraries
2. Test 2.1 - Core Library Identification Fields
3. Test 4.1 - Libraries with CVE Vulnerabilities
4. Test 3.1 - Libraries with Zero Class Usage
5. Test 1.3 - Invalid Application ID Format

### Phase 2: Core Functionality
6. Test 7.2 - Moderate Library Count (10-50)
7. Test 3.2 - Libraries with Active Class Usage
8. Test 4.6 - Used Libraries with High-Severity Vulnerabilities
9. Test 8.1 - First Request (Cache Miss)
10. Test 8.2 - Subsequent Request (Cache Hit)

### Phase 3: Edge Cases and Validation
11. Test 6.1 - Application with No Libraries
12. Test 7.4 - Very Many Libraries (>100)
13. Test 1.4 - Non-Existent Application ID
14. Test 9.1 - Network Timeout
15. Test 10.1 - Hash Uniqueness

### Phase 4: Advanced and Performance
16-52. Remaining tests as needed for comprehensive coverage

This prioritization ensures critical functionality is validated first, followed by common use cases, edge cases, and finally advanced scenarios.

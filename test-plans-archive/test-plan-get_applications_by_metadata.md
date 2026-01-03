# Test Plan: get_applications_by_metadata Tool

## Overview
This test plan provides comprehensive testing coverage for the `get_applications_by_metadata` tool in AssessService.java (lines 371-386). The tool filters applications based on metadata name/value pairs using case-insensitive matching.

## Tool Behavior Summary
- **Purpose**: Returns applications that have a specific metadata name/value pair
- **Parameters**:
  - `metadata_name` (String): The metadata field name to match
  - `metadata_value` (String): The metadata field value to match
- **Matching Logic**: Case-insensitive comparison using `equalsIgnoreCase()` for both name and value
- **Return Type**: List<ApplicationData>
- **Null Handling**: Safely handles null metadata lists, null entries, and null name/value fields

## Test Categories

### 1. Basic Functionality - Filtering by Metadata Name/Value

#### Test Case 1.1: Single Application with Exact Metadata Match
**Objective**: Verify the tool correctly identifies an application with matching metadata.

**Test Data Required**:
- Application A with metadata: `[{name: "environment", value: "production"}]`
- Application B with metadata: `[{name: "team", value: "engineering"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("environment", "production")
```

**Expected Result**:
- Returns list containing only Application A
- List size should be 1

#### Test Case 1.2: Multiple Applications with Same Metadata
**Objective**: Verify the tool returns all applications with matching metadata.

**Test Data Required**:
- Application A with metadata: `[{name: "tier", value: "critical"}]`
- Application B with metadata: `[{name: "tier", value: "critical"}]`
- Application C with metadata: `[{name: "tier", value: "standard"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("tier", "critical")
```

**Expected Result**:
- Returns list containing Application A and Application B
- List size should be 2
- Application C should not be included

#### Test Case 1.3: Filter by Different Metadata Names
**Objective**: Verify filtering works correctly for different metadata field names.

**Test Data Required**:
- Application A with metadata: `[{name: "environment", value: "staging"}]`
- Application B with metadata: `[{name: "region", value: "us-east-1"}]`
- Application C with metadata: `[{name: "owner", value: "security-team"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("environment", "staging")
Call 2: get_applications_by_metadata("region", "us-east-1")
Call 3: get_applications_by_metadata("owner", "security-team")
```

**Expected Result**:
- Call 1: Returns only Application A
- Call 2: Returns only Application B
- Call 3: Returns only Application C

### 2. Metadata Matching - Case Sensitivity

#### Test Case 2.1: Case-Insensitive Name Matching
**Objective**: Verify metadata name matching is case-insensitive.

**Test Data Required**:
- Application A with metadata: `[{name: "Environment", value: "production"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("environment", "production")
Call 2: get_applications_by_metadata("ENVIRONMENT", "production")
Call 3: get_applications_by_metadata("EnViRoNmEnT", "production")
```

**Expected Result**:
- All three calls should return Application A
- Case variations of the metadata name should all match

#### Test Case 2.2: Case-Insensitive Value Matching
**Objective**: Verify metadata value matching is case-insensitive.

**Test Data Required**:
- Application A with metadata: `[{name: "status", value: "Active"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("status", "active")
Call 2: get_applications_by_metadata("status", "ACTIVE")
Call 3: get_applications_by_metadata("status", "AcTiVe")
```

**Expected Result**:
- All three calls should return Application A
- Case variations of the metadata value should all match

#### Test Case 2.3: Case-Insensitive Name and Value Together
**Objective**: Verify case-insensitive matching works for both name and value simultaneously.

**Test Data Required**:
- Application A with metadata: `[{name: "DeploymentType", value: "Kubernetes"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("deploymenttype", "kubernetes")
```

**Expected Result**:
- Returns Application A
- Mixed case in both name and value should match

#### Test Case 2.4: Exact Match Required (Not Partial)
**Objective**: Verify the tool requires exact matches, not substring or partial matches.

**Test Data Required**:
- Application A with metadata: `[{name: "environment", value: "production"}]`
- Application B with metadata: `[{name: "environ", value: "prod"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("environ", "production")
Call 2: get_applications_by_metadata("environment", "prod")
Call 3: get_applications_by_metadata("environ", "prod")
```

**Expected Result**:
- Call 1: Returns empty list (name doesn't match)
- Call 2: Returns empty list (value doesn't match)
- Call 3: Returns Application B only

### 3. Multiple Metadata - Applications with Multiple Metadata Entries

#### Test Case 3.1: Match One Metadata in Multi-Metadata Application
**Objective**: Verify the tool can find a match when an application has multiple metadata entries.

**Test Data Required**:
- Application A with metadata:
  ```
  [
    {name: "environment", value: "production"},
    {name: "team", value: "security"},
    {name: "criticality", value: "high"}
  ]
  ```

**Test Execution**:
```
Call 1: get_applications_by_metadata("environment", "production")
Call 2: get_applications_by_metadata("team", "security")
Call 3: get_applications_by_metadata("criticality", "high")
```

**Expected Result**:
- All three calls should return Application A
- Any matching metadata entry should result in the application being included

#### Test Case 3.2: Same Name, Different Value
**Objective**: Verify the tool correctly handles applications with duplicate metadata names but different values.

**Test Data Required**:
- Application A with metadata:
  ```
  [
    {name: "tag", value: "frontend"},
    {name: "tag", value: "backend"}
  ]
  ```

**Test Execution**:
```
Call 1: get_applications_by_metadata("tag", "frontend")
Call 2: get_applications_by_metadata("tag", "backend")
Call 3: get_applications_by_metadata("tag", "database")
```

**Expected Result**:
- Call 1: Returns Application A (matches first tag)
- Call 2: Returns Application A (matches second tag)
- Call 3: Returns empty list (no matching tag)

#### Test Case 3.3: Multiple Applications with Overlapping Metadata
**Objective**: Verify filtering works correctly when applications share some but not all metadata.

**Test Data Required**:
- Application A with metadata:
  ```
  [{name: "tier", value: "critical"}, {name: "region", value: "us-east"}]
  ```
- Application B with metadata:
  ```
  [{name: "tier", value: "critical"}, {name: "region", value: "eu-west"}]
  ```
- Application C with metadata:
  ```
  [{name: "tier", value: "standard"}, {name: "region", value: "us-east"}]
  ```

**Test Execution**:
```
Call 1: get_applications_by_metadata("tier", "critical")
Call 2: get_applications_by_metadata("region", "us-east")
```

**Expected Result**:
- Call 1: Returns Application A and B
- Call 2: Returns Application A and C

### 4. Empty Results - No Matching Metadata

#### Test Case 4.1: No Applications Have Metadata
**Objective**: Verify the tool returns an empty list when no applications have any metadata.

**Test Data Required**:
- Application A with metadata: `null` or `[]`
- Application B with metadata: `null` or `[]`

**Test Execution**:
```
Call: get_applications_by_metadata("any_name", "any_value")
```

**Expected Result**:
- Returns empty list
- No exceptions thrown

#### Test Case 4.2: Metadata Name Doesn't Exist
**Objective**: Verify the tool returns an empty list when searching for a non-existent metadata name.

**Test Data Required**:
- Application A with metadata: `[{name: "environment", value: "production"}]`
- Application B with metadata: `[{name: "team", value: "engineering"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("nonexistent", "value")
```

**Expected Result**:
- Returns empty list
- No exceptions thrown

#### Test Case 4.3: Metadata Value Doesn't Exist
**Objective**: Verify the tool returns an empty list when searching for a non-existent metadata value.

**Test Data Required**:
- Application A with metadata: `[{name: "status", value: "active"}]`
- Application B with metadata: `[{name: "status", value: "inactive"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("status", "unknown")
```

**Expected Result**:
- Returns empty list
- No exceptions thrown

#### Test Case 4.4: Name Matches but Value Doesn't
**Objective**: Verify the tool requires both name AND value to match, not just the name.

**Test Data Required**:
- Application A with metadata: `[{name: "tier", value: "critical"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("tier", "standard")
```

**Expected Result**:
- Returns empty list
- Application A is not included since value doesn't match

#### Test Case 4.5: Value Matches but Name Doesn't
**Objective**: Verify the tool requires both name AND value to match, not just the value.

**Test Data Required**:
- Application A with metadata: `[{name: "priority", value: "high"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("criticality", "high")
```

**Expected Result**:
- Returns empty list
- Application A is not included since name doesn't match

### 5. Validation - Null and Empty Values

#### Test Case 5.1: Application with Null Metadata List
**Objective**: Verify the tool safely handles applications with null metadata lists.

**Test Data Required**:
- Application A with metadata: `null`
- Application B with metadata: `[{name: "team", value: "engineering"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("team", "engineering")
```

**Expected Result**:
- Returns only Application B
- Application A is safely excluded (no NullPointerException)

#### Test Case 5.2: Application with Empty Metadata List
**Objective**: Verify the tool handles applications with empty metadata lists.

**Test Data Required**:
- Application A with metadata: `[]`
- Application B with metadata: `[{name: "team", value: "security"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("team", "security")
```

**Expected Result**:
- Returns only Application B
- Application A is excluded (empty list means no metadata to match)

#### Test Case 5.3: Metadata Entry with Null Name
**Objective**: Verify the tool safely handles metadata entries with null names.

**Test Data Required**:
- Application A with metadata:
  ```
  [
    {name: null, value: "production"},
    {name: "team", value: "engineering"}
  ]
  ```

**Test Execution**:
```
Call 1: get_applications_by_metadata("environment", "production")
Call 2: get_applications_by_metadata("team", "engineering")
```

**Expected Result**:
- Call 1: Returns empty list (null name cannot match)
- Call 2: Returns Application A (valid metadata entry matches)

#### Test Case 5.4: Metadata Entry with Null Value
**Objective**: Verify the tool safely handles metadata entries with null values.

**Test Data Required**:
- Application A with metadata:
  ```
  [
    {name: "environment", value: null},
    {name: "team", value: "security"}
  ]
  ```

**Test Execution**:
```
Call 1: get_applications_by_metadata("environment", "production")
Call 2: get_applications_by_metadata("team", "security")
```

**Expected Result**:
- Call 1: Returns empty list (null value cannot match)
- Call 2: Returns Application A (valid metadata entry matches)

#### Test Case 5.5: Null Metadata Entry in List
**Objective**: Verify the tool safely handles null entries within the metadata list.

**Test Data Required**:
- Application A with metadata:
  ```
  [
    null,
    {name: "tier", value: "critical"},
    null
  ]
  ```

**Test Execution**:
```
Call: get_applications_by_metadata("tier", "critical")
```

**Expected Result**:
- Returns Application A
- Null entries are safely skipped (no NullPointerException)

#### Test Case 5.6: Empty String Metadata Name
**Objective**: Verify the tool handles empty string metadata names.

**Test Data Required**:
- Application A with metadata: `[{name: "", value: "production"}]`
- Application B with metadata: `[{name: "environment", value: "production"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("", "production")
Call 2: get_applications_by_metadata("environment", "production")
```

**Expected Result**:
- Call 1: Returns Application A (exact match on empty string)
- Call 2: Returns Application B

#### Test Case 5.7: Empty String Metadata Value
**Objective**: Verify the tool handles empty string metadata values.

**Test Data Required**:
- Application A with metadata: `[{name: "notes", value: ""}]`
- Application B with metadata: `[{name: "notes", value: "important"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("notes", "")
Call 2: get_applications_by_metadata("notes", "important")
```

**Expected Result**:
- Call 1: Returns Application A (exact match on empty string)
- Call 2: Returns Application B

#### Test Case 5.8: Whitespace-Only Metadata Values
**Objective**: Verify the tool treats whitespace as significant (no trimming).

**Test Data Required**:
- Application A with metadata: `[{name: "tag", value: "  production  "}]`
- Application B with metadata: `[{name: "tag", value: "production"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("tag", "production")
Call 2: get_applications_by_metadata("tag", "  production  ")
```

**Expected Result**:
- Call 1: Returns only Application B (exact match required)
- Call 2: Returns only Application A (whitespace is significant)

### 6. Edge Cases - Applications with No Metadata

#### Test Case 6.1: Mix of Applications with and without Metadata
**Objective**: Verify the tool correctly filters when some applications have metadata and others don't.

**Test Data Required**:
- Application A with metadata: `[{name: "team", value: "security"}]`
- Application B with metadata: `null`
- Application C with metadata: `[]`
- Application D with metadata: `[{name: "team", value: "engineering"}]`

**Test Execution**:
```
Call: get_applications_by_metadata("team", "security")
```

**Expected Result**:
- Returns only Application A
- Applications B, C, and D are excluded

#### Test Case 6.2: All Applications Have No Metadata
**Objective**: Verify the tool returns an empty list when all applications lack metadata.

**Test Data Required**:
- Application A with metadata: `null`
- Application B with metadata: `[]`
- Application C with metadata: `null`

**Test Execution**:
```
Call: get_applications_by_metadata("any_name", "any_value")
```

**Expected Result**:
- Returns empty list
- No exceptions thrown

#### Test Case 6.3: Special Characters in Metadata
**Objective**: Verify the tool handles special characters in metadata names and values.

**Test Data Required**:
- Application A with metadata: `[{name: "app-name", value: "my-app"}]`
- Application B with metadata: `[{name: "version", value: "1.0.0"}]`
- Application C with metadata: `[{name: "config.key", value: "value@123"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("app-name", "my-app")
Call 2: get_applications_by_metadata("version", "1.0.0")
Call 3: get_applications_by_metadata("config.key", "value@123")
```

**Expected Result**:
- Call 1: Returns Application A
- Call 2: Returns Application B
- Call 3: Returns Application C
- Special characters should match exactly

#### Test Case 6.4: Very Long Metadata Values
**Objective**: Verify the tool handles long metadata values without issues.

**Test Data Required**:
- Application A with metadata:
  ```
  [
    {name: "description", value: "This is a very long description that contains multiple sentences and goes on for quite some time to test the handling of large metadata values in the filtering logic."}
  ]
  ```

**Test Execution**:
```
Call: get_applications_by_metadata("description", "This is a very long description that contains multiple sentences and goes on for quite some time to test the handling of large metadata values in the filtering logic.")
```

**Expected Result**:
- Returns Application A
- Long values should be handled correctly

#### Test Case 6.5: Unicode and International Characters
**Objective**: Verify the tool handles Unicode and international characters in metadata.

**Test Data Required**:
- Application A with metadata: `[{name: "owner", value: "Müller"}]`
- Application B with metadata: `[{name: "location", value: "東京"}]`
- Application C with metadata: `[{name: "team", value: "équipe"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("owner", "Müller")
Call 2: get_applications_by_metadata("location", "東京")
Call 3: get_applications_by_metadata("team", "équipe")
```

**Expected Result**:
- Call 1: Returns Application A
- Call 2: Returns Application B
- Call 3: Returns Application C
- Unicode characters should be handled correctly

#### Test Case 6.6: Numeric Metadata Values (as Strings)
**Objective**: Verify the tool handles numeric values stored as strings.

**Test Data Required**:
- Application A with metadata: `[{name: "port", value: "8080"}]`
- Application B with metadata: `[{name: "instances", value: "5"}]`

**Test Execution**:
```
Call 1: get_applications_by_metadata("port", "8080")
Call 2: get_applications_by_metadata("instances", "5")
```

**Expected Result**:
- Call 1: Returns Application A
- Call 2: Returns Application B
- Numeric strings should match exactly

## Test Execution Guidance

### Setup Requirements
1. Ensure the MCP server is running and properly configured with Contrast Security credentials
2. Verify test applications exist in the Contrast Security instance with the required metadata
3. Confirm the getAllApplications() method is functioning correctly as it's used internally

### Test Execution Order
1. Start with Basic Functionality tests (Category 1) to validate core behavior
2. Proceed to Metadata Matching tests (Category 2) to verify case-insensitive logic
3. Test Multiple Metadata scenarios (Category 3) to ensure proper filtering with complex data
4. Verify Empty Results handling (Category 4) to confirm no false positives
5. Run Validation tests (Category 5) to ensure robust null/empty handling
6. Finish with Edge Cases (Category 6) to cover unusual but possible scenarios

### Expected Logging Behavior
Each tool call should produce log entries:
- INFO level: "Retrieving applications with metadata - Name: X, Value: Y"
- DEBUG level: "Retrieved N total applications, filtering by metadata"
- INFO level: "Found N applications with metadata - Name: X, Value: Y"

### Error Handling Verification
The tool should never throw exceptions for:
- Null metadata lists
- Null metadata entries
- Null metadata names or values
- Empty metadata lists
- Invalid or non-existent metadata names/values

All error conditions should result in an empty list being returned.

### Performance Considerations
For large application sets:
- Monitor execution time as it filters through all applications
- Verify memory usage remains reasonable with large metadata sets
- Test with datasets of 10, 100, and 1000+ applications

## Success Criteria
- All test cases pass without exceptions
- Empty lists are returned for invalid/non-matching queries (no errors)
- Case-insensitive matching works correctly for both names and values
- Null safety is maintained throughout
- Exact matching is enforced (no partial matches)
- Applications with multiple metadata entries are handled correctly
- Logging output matches expected patterns

## Test Data Setup Scripts
For automated testing, consider creating a test data setup utility that can:
1. Create applications with specific metadata via the Contrast API
2. Verify the applications are visible through the getAllApplications() call
3. Clean up test data after test execution
4. Support test data variations for different test scenarios

## Notes for AI Testing
When executing this test plan as an AI agent:
1. Verify the MCP server connection before starting tests
2. Check that test applications exist or can be identified in the live environment
3. Document actual results for each test case
4. Report any discrepancies between expected and actual behavior
5. Verify log output matches expected patterns
6. Note any performance issues with large application sets
7. If test data doesn't exist in the environment, document which tests cannot be executed

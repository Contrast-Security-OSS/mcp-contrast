# Tool Params Pattern for MCP Server

**Status:** Approved
**Created:** 2025-10-18
**Last Updated:** 2025-10-21 (Added Spring AI exception pattern, renamed warnings→messages)

## Overview

Standard pattern for handling complex input parameters in MCP tools. Provides consistent validation, graceful error handling with AI feedback, and clean separation between our code and SDK boundaries.

**Key features:**
- Distinguishes between hard errors (throw ToolExecutionException) and informational messages (return in response)
- Hard errors stop execution immediately via exceptions (Spring AI pattern)
- Informational messages provide conversational context for AI understanding
- Validates inputs and provides descriptive feedback for AI self-correction
- Immutable parameter objects with static factory methods
- Clear separation between parsing/validation and business logic

## Problem Statement

MCP tool methods were becoming bloated with:
- Inline parameter validation scattered throughout method bodies
- Manual message accumulation using `StringBuilder` (error-prone, inconsistent)
- Tight coupling between validation logic and business logic
- Difficulty testing validation in isolation
- Duplication across similar endpoints (e.g., pagination logic repeated)

**Example problem code:**
```java
public PaginatedResponse<VulnLight> getAllVulnerabilities(
    Integer page, Integer pageSize, String severities, ...
) {
    // 150+ lines mixing validation, SDK calls, response building
    StringBuilder messageBuilder = new StringBuilder();
    int actualPage = page != null && page > 0 ? page : 1;
    if (page != null && page < 1) {
        messageBuilder.append("Invalid page...");
    }
    // ... 50 more lines of validation
    // ... 50 lines of SDK interaction
    // ... 50 lines of response building
}
```

## Solution: Params Pattern

### Core Principle

**Separate input parsing/validation from business logic using immutable parameter objects with static factory methods.**

Each parameter group becomes a record with:
- `.of()` static factory that validates and transforms inputs
- Immutable fields with validated/clamped values
- Accumulated validation messages for AI feedback
- Conversion methods to SDK types (`.toXXX()`) when crossing boundaries

### Pattern Structure

```java
public record SomeParams(
    // Parsed/validated values
    TypeA valueA,
    TypeB valueB,

    // Feedback for AI (two channels)
    List<String> messages,  // Informational context - execution continues
    List<String> errors     // Hard failures - will throw exception
) {
    /**
     * Parse and validate input parameters.
     * Returns object with validation status and feedback messages.
     */
    public static SomeParams of(TypeA rawA, TypeB rawB) {
        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Informational: clamp to default, add message
        TypeA validatedA = validateA(rawA, messages);

        // Hard error: cannot proceed if invalid
        TypeB validatedB = validateB(rawB, errors);

        return new SomeParams(
            validatedA,
            validatedB,
            List.copyOf(messages),
            List.copyOf(errors)
        );
    }

    /**
     * Returns true if parameters are valid enough to execute query.
     * Check errors() for details if false.
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    // Conversion to SDK types (if needed)
    public SdkType toSdkType() {
        return new SdkType(valueA, valueB);
    }
}
```

## Two Communication Channels: Errors vs Messages

Validation results use two distinct channels for AI communication:

### Hard Errors (Throw Exception)

**Definition:** Invalid inputs that stop execution. Communicated via `ToolExecutionException`.

**Examples:**
- Invalid enum values that can't be parsed ("SUPER_HIGH" for severity)
- Unparseable dates ("garbage" instead of "2025-01-15")
- Logical contradictions (startDate > endDate)
- Multiple incompatible parameters
- Values that would cause SDK exceptions

**Handling:**
- Add descriptive message to `errors` list
- Service method checks `isValid()`
- If invalid: `throw new ToolExecutionException(String.join("; ", filters.errors()))`
- Execution STOPS immediately
- Spring AI sends exception message to AI model
- AI receives error and must correct input

**Example:**
```java
// Input: lastSeenAfter="not-a-date"
// Result: errors=["Invalid date 'not-a-date'. Expected ISO format (YYYY-MM-DD) like '2025-01-15'"]
// Action: throw ToolExecutionException → AI gets error message
```

### Informational Messages (Return in Response)

**Definition:** Conversational context that helps AI understand what happened. Execution continues.

**Examples:**
- Smart defaults applied: "Excluding Fixed/Remediated vulnerabilities (default)"
- Value corrections: "Page size adjusted from 200 to 100 (maximum allowed)"
- Interpretation notes: "Time filters apply to LAST ACTIVITY DATE, not discovery date"
- Empty result context: "No attacks found - ADR may not be enabled"
- Execution details: "Searched 50 applications in 450ms"

**Handling:**
- Add descriptive message to `messages` list
- Apply correction/default silently
- Continue execution normally
- Messages returned in `response.message` field
- AI sees context and understands what happened

**Example:**
```java
// Input: page=200 (no more data)
// Result: messages=["Page size adjusted from 200 to 100 (maximum)"]
// Action: Execute query with pageSize=100, return results + message
```

### Key Insight: Messages Are Not Warnings

**Previous terminology:** "warnings" suggested something might be wrong.

**Current terminology:** "messages" are conversational context - they help AI understand:
- What defaults were applied
- What corrections were made
- How to interpret results
- Why results might be empty
- Performance characteristics

**Messages are not failures** - they're helpful context for the AI to understand execution.

### Decision Tree: Error vs Message?

**Ask: Can execution proceed safely?**

✅ **Message** (return context, execution continues):
- Parameter is optional and can be ignored
- Value can be safely clamped/corrected
- Degraded functionality is acceptable
- User's intent can still be satisfied

❌ **Error** (throw exception, execution stops):
- Parameter is required for correct results
- No safe default exists
- Execution would return misleading/incorrect results
- SDK would throw exception
- Logical contradiction in parameters

**Example: Date Filter**
```java
// Optional filter, unparseable date
if (lastSeenAfter != null && !lastSeenAfter.trim().isEmpty()) {
    Date parsed = parseDate(lastSeenAfter);
    if (parsed == null) {
        // Cannot parse - this is always a hard error
        // AI provided invalid input, needs to fix it
        errors.add("Invalid date '" + lastSeenAfter + "'. Expected ISO format (YYYY-MM-DD)");
    } else {
        // Successfully parsed, add interpretation note
        messages.add("Time filters apply to LAST ACTIVITY DATE, not discovery date");
    }
}
```

## Standard Params Objects

### PaginationParams

**Purpose:** Validate page/pageSize, calculate offset/limit, enforce constraints.

**Usage:** Every paginated MCP tool endpoint.

```java
public record PaginationParams(
    int page,              // Validated 1-based page number (min: 1)
    int pageSize,          // Validated page size (range: 1-100, default: 50)
    int offset,            // Calculated 0-based offset for SDK
    int limit,             // Same as pageSize, for SDK clarity
    List<String> messages  // Informational messages (no hard errors)
) {
    public static PaginationParams of(Integer page, Integer pageSize) {
        List<String> messages = new ArrayList<>();

        // Informational: invalid page → clamp to 1
        int actualPage = page != null && page > 0 ? page : 1;
        if (page != null && page < 1) {
            messages.add(String.format(
                "Invalid page number %d, using page 1", page
            ));
        }

        // Informational: invalid pageSize → clamp to range
        int actualSize = pageSize != null && pageSize > 0 ? pageSize : 50;
        if (pageSize != null && pageSize < 1) {
            messages.add(String.format(
                "Invalid pageSize %d, using default 50", pageSize
            ));
            actualSize = 50;
        } else if (pageSize != null && pageSize > 100) {
            messages.add(String.format(
                "Requested pageSize %d exceeds maximum 100, capped to 100", pageSize
            ));
            actualSize = 100;
        }

        return new PaginationParams(
            actualPage,
            actualSize,
            (actualPage - 1) * actualSize,  // 0-based offset
            actualSize,                      // limit
            List.copyOf(messages)
        );
    }

    /**
     * Pagination params are always valid (no hard errors).
     * Invalid values are clamped to acceptable defaults.
     */
    public boolean isValid() {
        return true;  // Always valid - uses graceful degradation
    }
}
```

### VulnerabilityFilterParams

**Purpose:** Parse and validate vulnerability filter inputs, produce TraceFilterForm for SDK.

**Usage:** Vulnerability filtering endpoints (getAllVulnerabilities, etc.).

```java
public record VulnerabilityFilterParams(
    TraceFilterForm form,       // SDK filter object
    String appId,               // For routing decision (null = org-level)
    List<String> messages,      // Informational context for AI
    List<String> errors         // Hard failures (will throw)
) {
    public static VulnerabilityFilterParams of(
        String severities, String statuses, String appId,
        String vulnTypes, String environments,
        String lastSeenAfter, String lastSeenBefore, String vulnTags
    ) {
        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        TraceFilterForm form = new TraceFilterForm();

        // Hard error: invalid severity enum value
        if (severities != null) {
            List<String> severityList = FilterHelper.parseCommaSeparatedUpperCase(severities);
            if (severityList != null) {
                EnumSet<RuleSeverity> severitySet = EnumSet.noneOf(RuleSeverity.class);
                for (String sev : severityList) {
                    try {
                        severitySet.add(RuleSeverity.valueOf(sev));
                    } catch (IllegalArgumentException e) {
                        errors.add(String.format(
                            "Invalid severity '%s'. Valid: CRITICAL, HIGH, MEDIUM, LOW, NOTE. Example: 'CRITICAL,HIGH'",
                            sev
                        ));
                    }
                }
                if (!severitySet.isEmpty() && errors.isEmpty()) {
                    form.setSeverities(severitySet);
                }
            }
        }

        // Informational: smart defaults for status
        List<String> statusList;
        if (statuses == null || statuses.trim().isEmpty()) {
            statusList = List.of("Reported", "Suspicious", "Confirmed");
            messages.add(
                "Using default statuses: Reported, Suspicious, Confirmed (excludes Fixed/Remediated). " +
                "To see all statuses, specify statuses parameter explicitly."
            );
        } else {
            statusList = FilterHelper.parseCommaSeparated(statuses);
        }
        if (statusList != null) {
            form.setStatus(statusList);
        }

        // Hard error: date parsing fails
        if (lastSeenAfter != null && !lastSeenAfter.trim().isEmpty()) {
            FilterHelper.ParseResult<Date> result =
                FilterHelper.parseDateWithValidation(lastSeenAfter, "lastSeenAfter");
            if (result.hasValidationMessage()) {
                // Cannot parse date - hard error
                errors.add(result.getValidationMessage());
            } else if (result.getValue() != null) {
                form.setStartDate(result.getValue());
                // Informational: help AI understand what the date filter does
                messages.add("Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date");
            }
        }

        // Hard error: logical contradiction
        if (form.getStartDate() != null && form.getEndDate() != null) {
            if (form.getStartDate().after(form.getEndDate())) {
                errors.add(
                    "Invalid date range: lastSeenAfter must be before lastSeenBefore. " +
                    "Example: lastSeenAfter='2025-01-01', lastSeenBefore='2025-12-31'"
                );
            }
        }

        // ... parse other filters similarly

        return new VulnerabilityFilterParams(
            form,
            appId,
            List.copyOf(messages),
            List.copyOf(errors)
        );
    }

    /**
     * Returns true if filters are valid enough to execute query.
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Convert to SDK TraceFilterForm.
     * Explicitly marks SDK boundary crossing.
     */
    public TraceFilterForm toTraceFilterForm() {
        return form;
    }
}
```

## Service Method Pattern

### Structure

```java
@Tool(name = "some_method", description = "...")
public PaginatedResponse<T> someMethod(
    @ToolParam(description = "Page number (1-based), default: 1", required = false)
    Integer page,

    @ToolParam(description = "Page size (max 100), default: 50", required = false)
    Integer pageSize,

    @ToolParam(description = "Filter A description", required = false)
    String filterA,

    @ToolParam(description = "Filter B description", required = false)
    String filterB
    // ... more filters
) {
    // 1. Parse and validate all inputs
    PaginationParams pagination = PaginationParams.of(page, pageSize);
    SomeFilterParams filters = SomeFilterParams.of(filterA, filterB, ...);

    // 2. Check for hard errors - throw immediately if invalid
    if (!filters.isValid()) {
        // Stop execution, send error to AI via exception
        throw new ToolExecutionException(String.join("; ", filters.errors()));
    }

    // 3. Accumulate informational messages
    List<String> allMessages = new ArrayList<>();
    allMessages.addAll(pagination.messages());
    allMessages.addAll(filters.messages());

    // 4. Execute SDK call (try/catch for system errors)
    try {
        ContrastSDK sdk = SDKHelper.getSDK(...);
        SdkRequestType request = filters.toSdkRequestType();
        request.setOffset(pagination.offset());
        request.setLimit(pagination.limit());

        SdkResponseType response = sdk.someQuery(orgID, request);

        // 5. Convert SDK response to our types
        List<T> items = convertToOurType(response.getData());

        // 6. Build final response with informational messages
        return paginationHandler.wrapApiPaginatedItems(
            items,
            pagination,
            response.getTotalCount(),
            allMessages
        );

    } catch (Exception e) {
        // System/runtime error - throw ToolExecutionException
        throw new ToolExecutionException(
            "Failed to execute query: " + e.getMessage(),
            e
        );
    }
}
```

### Benefits

**Clean separation of concerns:**
- Lines 1-2: Input parsing/validation
- Lines 3-4: Hard error check - throw exception if invalid (Spring AI pattern)
- Lines 6-8: Informational message accumulation
- Lines 10-18: SDK interaction (try/catch for system errors)
- Lines 23-29: Build response with informational messages

**Service method responsibilities:**
- Orchestrate the flow
- Throw ToolExecutionException for hard errors (stops execution)
- Accumulate informational messages from all sources
- Execute business logic only when inputs are valid
- Wrap system/SDK errors in ToolExecutionException
- Build final response with conversational context

**What the service does NOT do:**
- Validate individual parameters (delegated to params objects)
- Decide what's an error vs message (params objects handle it)
- Know validation rules (encapsulated in params)
- Return error responses (throws exceptions instead)

## Design Rationale

### Why Static `.of()` Factory?

**Familiarity:** Matches Java standard library (`List.of()`, `Set.of()`, `Map.of()`) and Spring Data (`PageRequest.of()`).

**Clear intent:** `PaginationParams.of(1, 50)` reads naturally - "create pagination params from these values".

**Single point of construction:** Can't bypass validation by calling constructor directly.

### Why Records?

**Immutability:** Thread-safe by default, values can't be changed after creation.

**Conciseness:** Automatic constructor, accessors, `equals()`, `hashCode()`, `toString()`.

**Modern Java:** Idiomatic in Java 14+, embraced by Spring Boot 3.x.

### Why `.toXXX()` Convention?

**Java convention:** Matches `.toString()`, `.toArray()`, `.toList()` pattern.

**Explicit conversion:** Clear that we're crossing a boundary (our code → SDK).

**Self-documenting:** Name tells you exactly what type you're getting.

**Example:**
```java
TraceFilterForm form = filters.toTraceFilterForm();  // Clear conversion
AttackFilterForm form = filters.toAttackFilterForm(); // Different SDK target
```

### Why `List<String> messages` Not Custom Type?

**Simplicity:** Standard library type, no learning curve.

**Interoperability:** Easy to combine, filter, or transform messages.

**Serialization:** Works with any JSON library out of the box.

**Adequate:** We only need to collect strings, no complex message objects needed.

### Why Graceful Degradation?

**AI-friendly:** AI can self-correct when it receives descriptive messages.

**Progressive enhancement:** Tool still works with correctable inputs, provides context about corrections.

**User experience:** Better than failing completely when input can be safely corrected.

**Example:**
```
Input: page=-5, pageSize=200
Response: "Invalid page number -5, using page 1. Requested pageSize 200 exceeds maximum 100, capped to 100."
Result: Returns page 1 with 100 items + informational messages
```

AI sees the messages and understands what corrections were applied.

### Why ToolExecutionException for Hard Errors?

**Spring AI convention:** Framework expects runtime exceptions for tool failures.

**Clear failure signal:** Exception stops execution immediately, no ambiguity.

**Error message delivery:** Spring AI sends exception message directly to AI model.

**No checked exceptions:** ToolExecutionException is unchecked (RuntimeException), no `throws` declarations needed.

**Example:**
```java
// Hard error: unparseable date
throw new ToolExecutionException(
    "Invalid date 'garbage'. Expected ISO format (YYYY-MM-DD) like '2025-01-15'"
);
// → Spring AI sends this message to AI model
// → AI sees error and can retry with corrected input
```

### Why Two Communication Channels?

**Clarity:** Errors stop execution (throw), messages provide context (return).

**Spring AI alignment:** Exceptions for failures, structured responses for success.

**No ambiguity:** Clear distinction between "execution failed" vs "here's what happened".

**AI understanding:** Messages help AI understand tool behavior without implying failure.

**Example:**
```java
// Error channel: Cannot proceed
if (!filters.isValid()) {
    throw new ToolExecutionException(String.join("; ", filters.errors()));
}

// Message channel: Helpful context
return new PaginatedResponse<>(items, page, pageSize, total, hasMore,
    String.join(" | ", allMessages));  // "Using defaults: ...", "Searched 50 apps", etc.
```

## Testing Strategy

### Params Object Tests

Test each params class in isolation:

```java
@Test
void testPaginationParams_InvalidPage() {
    PaginationParams params = PaginationParams.of(-5, 50);

    assertEquals(1, params.page());  // Clamped to 1
    assertEquals(50, params.pageSize());
    assertEquals(0, params.offset());  // Calculated correctly
    assertTrue(params.messages().stream()
        .anyMatch(m -> m.contains("Invalid page number -5")));
}

@Test
void testVulnerabilityFilterParams_InvalidSeverity() {
    VulnerabilityFilterParams params = VulnerabilityFilterParams.of(
        "CRITICAL,SUPER_HIGH",  // SUPER_HIGH is invalid
        null, null, null, null, null, null, null
    );

    // Should have error (hard failure)
    assertFalse(params.isValid());
    assertTrue(params.errors().stream()
        .anyMatch(e -> e.contains("Invalid severity 'SUPER_HIGH'")));
}

@Test
void testVulnerabilityFilterParams_SmartDefaults() {
    VulnerabilityFilterParams params = VulnerabilityFilterParams.of(
        null, null, null, null, null, null, null, null  // All null
    );

    // Should be valid with smart defaults
    assertTrue(params.isValid());
    // Should have informational message about defaults
    assertTrue(params.messages().stream()
        .anyMatch(m -> m.contains("default statuses")));
}
```

### Integration Tests

Test service method with various param combinations:

```java
@Test
void testServiceMethod_WithFilters() {
    // Test that params are correctly applied to SDK calls
    var response = service.someMethod(1, 50, "CRITICAL", "Reported", ...);

    // Verify SDK was called with correct filters
    verify(mockSDK).someQuery(eq(orgID), filterCaptor.capture());
    TraceFilterForm form = filterCaptor.getValue();
    assertEquals(0, form.getOffset());  // page 1
    assertEquals(50, form.getLimit());
    assertNotNull(form.getSeverities());
}
```

## Guidelines for New Tools

### When to Use This Pattern

✅ **Use params objects when:**
- Multiple related parameters (3+ parameters that work together)
- Complex validation logic (ranges, enums, cross-field validation)
- Parameters will be reused across tools (e.g., pagination)
- Need to provide AI feedback on invalid inputs

❌ **Don't use params objects when:**
- Single simple parameter (e.g., `String appId`)
- No validation needed (pass-through to SDK)
- One-off parameter that won't be reused

### Creating New Params Objects

**Step 1: Identify parameter groups**
```
Pagination: page, pageSize → PaginationParams
Filters: severities, statuses, ... → SomeFilterParams
Time range: startDate, endDate → TimeRangeParams
```

**Step 2: Name consistently**
```
Pattern: <Domain><Purpose>Params
Examples:
  - PaginationParams
  - VulnerabilityFilterParams
  - AttackFilterParams
  - TimeRangeParams
```

**Step 3: Implement validation**
```java
public static SomeParams of(...) {
    List<String> messages = new ArrayList<>();

    // For each parameter:
    // 1. Parse/transform input
    // 2. Validate constraints
    // 3. Add message if invalid
    // 4. Use valid default if needed

    return new SomeParams(..., List.copyOf(messages));
}
```

**Step 4: Add conversion methods if needed**
```java
// Only if you need to convert to SDK types
public SdkType toSdkType() {
    return new SdkType(field1, field2);
}
```

**Step 5: Write isolated tests**
```java
@Test
void testSomeParams_ValidInput() { }

@Test
void testSomeParams_InvalidInput_ProducesMessage() { }

@Test
void testSomeParams_EdgeCase() { }
```

## Examples in Codebase

### Current Implementation

- `PaginationParams` - Used by `getAllVulnerabilities()`
- `VulnerabilityFilterParams` - Used by `getAllVulnerabilities()`

### Planned Applications

- `AttackFilterParams` - For `getAttacks()`, `getAttacksFiltered()`
- `LibraryFilterParams` - For SCA vulnerability filtering
- `RouteFilterParams` - For route coverage filtering
- `TimeRangeParams` - Reusable across multiple endpoints

## Anti-Patterns to Avoid

❌ **Don't create generic wrappers:**
```java
// Bad: Unnecessary abstraction
class ValidationResult<T> {
    T value;
    List<String> messages;
}
```

❌ **Don't make params objects mutable:**
```java
// Bad: Mutable, not thread-safe
class SomeParams {
    private int page;
    public void setPage(int page) { this.page = page; }
}
```

❌ **Don't mix validation with business logic:**
```java
// Bad: Params object calls SDK
public static SomeParams of(...) {
    ContrastSDK sdk = SDKHelper.getSDK(...);  // WRONG!
    // Params should only parse/validate inputs
}
```

❌ **Don't bypass the factory:**
```java
// Bad: Direct construction bypasses validation
var params = new PaginationParams(page, pageSize, offset, limit, messages);

// Good: Use factory
var params = PaginationParams.of(page, pageSize);
```

❌ **Don't fail silently:**
```java
// Bad: Invalid input, no message
if (page < 1) {
    page = 1;  // Clamped but AI doesn't know why
}

// Good: Clamp and inform
if (page < 1) {
    warnings.add("Invalid page " + page + ", using page 1");
    page = 1;
}
```

❌ **Don't skip validation check:**
```java
// Bad: Execute query without checking if params are valid
VulnerabilityFilterParams filters = VulnerabilityFilterParams.of(...);
Traces traces = sdk.getTracesInOrg(orgID, filters.toTraceFilterForm());  // WRONG!

// Good: Check isValid() first, throw if invalid
VulnerabilityFilterParams filters = VulnerabilityFilterParams.of(...);
if (!filters.isValid()) {
    throw new ToolExecutionException(String.join("; ", filters.errors()));
}
Traces traces = sdk.getTracesInOrg(orgID, filters.toTraceFilterForm());
```

❌ **Don't mix messages and errors:**
```java
// Bad: Treating hard error as informational message
if (unparseable date) {
    messages.add("Invalid date");  // Should be error!
    // Execution continues with wrong results
}

// Good: Use errors for hard failures
if (unparseable date) {
    errors.add("Invalid date format. Expected ISO format (YYYY-MM-DD)...");
    // isValid() returns false, tool method throws exception
}
```

❌ **Don't return error responses, throw exceptions:**
```java
// Bad: Returning error response for validation failures
if (!filters.isValid()) {
    return PaginatedResponse.error(page, pageSize, errorMessage);
}

// Good: Throw exception for validation failures
if (!filters.isValid()) {
    throw new ToolExecutionException(String.join("; ", filters.errors()));
}
```

## Migration Guide

### Converting Existing Tools

**Before:**
```java
public PaginatedResponse<T> someMethod(Integer page, Integer pageSize, ...) {
    StringBuilder messageBuilder = new StringBuilder();

    // 50 lines of validation
    int actualPage = page != null && page > 0 ? page : 1;
    if (page != null && page < 1) {
        messageBuilder.append("Invalid page...");
    }
    // ... more validation

    // 50 lines of business logic
    ContrastSDK sdk = ...;
    Response response = sdk.query(...);

    // 50 lines of response building
    return new PaginatedResponse<>(..., messageBuilder.toString());
}
```

**After:**
```java
@Tool(name = "some_method", description = "...")
public PaginatedResponse<T> someMethod(
    @ToolParam(description = "Page number", required = false) Integer page,
    @ToolParam(description = "Page size", required = false) Integer pageSize,
    ...
) {
    // 2 lines of validation
    PaginationParams pagination = PaginationParams.of(page, pageSize);
    SomeFilterParams filters = SomeFilterParams.of(...);

    // 2 lines hard error check
    if (!filters.isValid()) {
        throw new ToolExecutionException(String.join("; ", filters.errors()));
    }

    // 3 lines message accumulation
    List<String> messages = new ArrayList<>();
    messages.addAll(pagination.messages());
    messages.addAll(filters.messages());

    // 10 lines of business logic (with exception handling)
    try {
        ContrastSDK sdk = ...;
        Response response = sdk.query(filters.toSdkType(), pagination.offset(), ...);

        // Return with messages
        return paginationHandler.wrapApiPaginatedItems(
            items, pagination, total, messages
        );
    } catch (Exception e) {
        throw new ToolExecutionException("Failed: " + e.getMessage(), e);
    }
}
```

**Improvement:**
- 150 lines → 40 lines
- Validation logic extracted and testable
- Clear separation of concerns
- Reusable across endpoints

## Implementation Notes

### PaginatedResponse Message Field

The pattern uses `PaginatedResponse.message` field for informational context:

```java
public record PaginatedResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    Integer totalItems,
    boolean hasMorePages,
    String message              // Informational context for AI
) {
    // No error() factory method - throw ToolExecutionException instead
}
```

**Message field usage:**
- Informational context about execution
- Smart defaults applied
- Corrections made to inputs
- Empty result explanations
- Performance metrics

**NOT used for errors** - hard errors throw `ToolExecutionException`.

### FilterHelper Enhancements

`FilterHelper.parseDateWithValidation()` should return validation messages:

```java
public static class ParseResult<T> {
    private final T value;
    private final String validationMessage;

    public ParseResult(T value, String validationMessage) { ... }
    public T getValue() { return value; }
    public String getValidationMessage() { return validationMessage; }
    public boolean hasValidationMessage() {
        return validationMessage != null && !validationMessage.isEmpty();
    }
}
```

## Success Metrics

✅ **Code quality:**
- Service methods < 50 lines
- Each params class < 100 lines
- 90%+ test coverage on params objects

✅ **Maintainability:**
- Validation logic centralized
- Easy to add new filters/params
- Clear where to add new validation

✅ **AI experience:**
- Descriptive error messages for all invalid inputs
- AI can self-correct from feedback
- Graceful degradation, never fails completely

## References

- AssessService.java:443 - `getAllVulnerabilities()` using this pattern
- FilterHelper.java - Reusable parsing utilities
- PaginationParams (planned) - Standard pagination handling
- VulnerabilityFilterParams (planned) - Vulnerability filter parsing

---

**Last reviewed:** 2025-10-21

## Changes from Previous Version

### 2025-10-21: Spring AI Exception Pattern

**Key changes:**
1. **Terminology:** "warnings" → "messages" (informational context, not failures)
2. **Error handling:** Hard errors throw `ToolExecutionException` (Spring AI pattern)
3. **Removed:** `PaginatedResponse.error()` factory method
4. **Added:** `@ToolParam` annotations in examples
5. **Added:** Two communication channels section (errors vs messages)
6. **Updated:** All code examples to use exception-based pattern

**Rationale:**
- Aligns with Spring AI conventions (runtime exceptions for tool failures)
- Clearer separation: errors stop execution (throw), messages provide context (return)
- Better AI experience: descriptive exception messages sent directly to AI model
- No ambiguity: "messages" are conversational context, not warnings/failures

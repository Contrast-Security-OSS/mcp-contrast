# Contrast MCP Server - Pagination Specification v1.0

**Status:** Approved
**Created:** 2025-10-15
**Last Updated:** 2025-10-15

## Overview

This document defines the standard pagination pattern for all list-returning endpoints in the Contrast MCP Server. The specification ensures consistent behavior, AI-friendly responses, and efficient resource usage across all paginated tools.

---

## Core Response Structure

### Generic Paginated Response

All paginated endpoints return a consistent `PaginatedResponse<T>` wrapper:

```java
/**
 * Generic paginated response wrapper for all list-returning MCP tools.
 * Provides consistent pagination metadata and messaging across all endpoints.
 */
public record PaginatedResponse<T>(
    List<T> items,              // The data for the current page
    int page,                   // 1-based page number (first page = 1)
    int pageSize,               // Number of items per page
    Integer totalItems,         // Total count across all pages (null if unavailable/expensive)
    boolean hasMorePages,       // true if additional pages exist beyond this page
    String message              // Optional message for the AI (validation warnings, empty results, etc.)
) {
    /**
     * Creates a paginated response with no message
     */
    public PaginatedResponse(List<T> items, int page, int pageSize,
                            Integer totalItems, boolean hasMorePages) {
        this(items, page, pageSize, totalItems, hasMorePages, null);
    }

    /**
     * Creates an empty paginated response with a message
     */
    public static <T> PaginatedResponse<T> empty(int page, int pageSize, String message) {
        return new PaginatedResponse<>(List.of(), page, pageSize, 0, false, message);
    }
}
```

### Field Specifications

| Field | Type | Description |
|-------|------|-------------|
| `items` | `List<T>` | The actual data items for the current page. Never null (empty list if no results). |
| `page` | `int` | The page number returned (1-based). Always ≥ 1. |
| `pageSize` | `int` | Items per page used for this response. Range: 1-100. |
| `totalItems` | `Integer` | Total count across all pages. **Nullable** - `null` if unavailable or expensive to compute. |
| `hasMorePages` | `boolean` | `true` if additional pages exist beyond this page. |
| `message` | `String` | Optional informational message for the AI. Used for validation warnings, empty result explanations, etc. **Nullable**. |

---

## Parameter Standards

### Default Values

- **`page`**: Default = **1** (1-based indexing)
- **`pageSize`**: Default = **50**, Maximum = **100**

### Parameter Ordering Convention

Pagination parameters **always appear last** in method signatures:

```java
// Correct parameter ordering
method(businessParam1, businessParam2, ..., Integer page, Integer pageSize)

// Examples
PaginatedResponse<VulnLight> getAllVulnerabilities(Integer page, Integer pageSize)
PaginatedResponse<VulnLight> listVulnsByAppId(String appId, Integer page, Integer pageSize)
PaginatedResponse<AttackSummary> getAttacks(Integer page, Integer pageSize)
```

### Validation & Clamping Rules

All invalid input parameters are **clamped to valid values** and a message is added to inform the AI:

| Input Parameter Condition | Action | Message Added |
|--------------------------|--------|---------------|
| `page == null` | Set to `1` | None (default behavior) |
| `page < 1` | Clamp to `1` | `"Invalid page number {original}, using page 1"` |
| `page > totalPages` | Return empty page with requested page number | `"Requested page {requested} exceeds available pages"` |
| `pageSize == null` | Set to `50` | None (default behavior) |
| `pageSize < 1` | Clamp to `50` | `"Invalid pageSize {original}, using default 50"` |
| `pageSize > 100` | Clamp to `100` | `"Requested pageSize {original} exceeds maximum 100, capped to 100"` |

### Implementation Pattern

```java
StringBuilder messageBuilder = new StringBuilder();

// Validate page parameter
int actualPage = page != null && page > 0 ? page : 1;
if (page != null && page < 1) {
    logger.warn("Invalid page number {}, clamping to 1", page);
    messageBuilder.append(String.format("Invalid page number %d, using page 1. ", page));
    actualPage = 1;
}

// Validate pageSize parameter
int actualPageSize = pageSize != null && pageSize > 0 ? pageSize : 50;
if (pageSize != null && pageSize < 1) {
    logger.warn("Invalid pageSize {}, using default 50", pageSize);
    messageBuilder.append(String.format("Invalid pageSize %d, using default 50. ", pageSize));
    actualPageSize = 50;
}
if (pageSize != null && pageSize > 100) {
    logger.warn("Requested pageSize {} exceeds maximum 100, capping", pageSize);
    messageBuilder.append(String.format("Requested pageSize %d exceeds maximum 100, capped to 100. ", pageSize));
    actualPageSize = 100;
}

String message = messageBuilder.length() > 0 ? messageBuilder.toString().trim() : null;
```

---

## Edge Case Behaviors

### Empty Results (No Data Found)

When no data matches the query, return a properly structured empty response:

```java
// Example: Organization has zero vulnerabilities
PaginatedResponse(
    items = [],
    page = 1,
    pageSize = 50,
    totalItems = 0,
    hasMorePages = false,
    message = "No vulnerabilities found"
)
```

### Page Beyond Available Data

When requesting a page number that exceeds available data, return an empty page with explanatory message:

```java
// Example: Only 3 pages exist (150 items total), user requests page 10
PaginatedResponse(
    items = [],
    page = 10,
    pageSize = 50,
    totalItems = 150,
    hasMorePages = false,
    message = "Requested page 10 exceeds available pages (total: 3)"
)
```

**Calculating total pages for message:**
```java
if (vulnerabilities.isEmpty() && actualPage > 1 && totalItems != null) {
    int totalPages = (int) Math.ceil((double) totalItems / actualPageSize);
    messageBuilder.append(String.format(
        "Requested page %d exceeds available pages (total: %d). ",
        actualPage, totalPages
    ));
}
```

### Single Page of Results

When all results fit in a single page:

```java
// Example: Only 25 items total, fits in one page
PaginatedResponse(
    items = [25 items],
    page = 1,
    pageSize = 50,
    totalItems = 25,
    hasMorePages = false,
    message = null
)
```

---

## totalItems Strategy

Return `totalItems` **only when efficiently available**. Do not make extra queries just to get the count.

| Scenario | totalItems Value | Rationale |
|----------|------------------|-----------|
| Organization API includes count in response | Use the count | "Free" metadata from API |
| Organization API doesn't include count | `null` | Don't make expensive COUNT query |
| Fallback path (iterating apps) | `null` | Too expensive to pre-count all apps |
| Cached data available | Use cached count if fresh | Reuse existing data |

### hasMorePages Calculation

**When totalItems is available:**
```java
boolean hasMorePages = (actualPage * actualPageSize) < totalItems;
```

Note: `actualPage` is the validated/clamped value from the input `page` parameter.

**When totalItems is null (fallback heuristic):**
```java
// If we got a full page of results, assume more exist
boolean hasMorePages = items.size() == actualPageSize;
```

**Complete implementation:**
```java
boolean hasMorePages;
if (totalItems != null) {
    hasMorePages = (actualPage * actualPageSize) < totalItems;
} else {
    // Heuristic: if we got a full page, assume more exist
    hasMorePages = items.size() == actualPageSize;
}
```

---

## Implementation Internals

### SDK Offset Conversion (1-based to 0-based)

The Contrast SDK uses 0-based offsets internally. Convert 1-based page numbers:

```java
// Convert 1-based page to SDK's 0-based offset
int offset = (actualPage - 1) * actualPageSize;
int limit = actualPageSize;

// Use with Contrast SDK
TraceFilterForm filterForm = new TraceFilterForm();
filterForm.setOffset(offset);
filterForm.setLimit(limit);

Traces traces = contrastSDK.getTracesInOrg(orgID, filterForm);
```

### Logging Strategy

Log all validation warnings and performance metrics:

```java
// Log warnings for clamped values
if (page != null && page < 1) {
    logger.warn("Invalid page number {} received, clamping to 1", page);
}
if (pageSize != null && pageSize > 100) {
    logger.warn("Requested pageSize {} exceeds maximum 100, capping", pageSize);
}

// Log performance metrics
long startTime = System.currentTimeMillis();
// ... perform query ...
long duration = System.currentTimeMillis() - startTime;

logger.info("Retrieved {} items for page {} (pageSize: {}, totalItems: {}, took {} ms)",
    items.size(), actualPage, actualPageSize, totalItems, duration);
```

---

## Endpoints Requiring Pagination

Paginate endpoints that **return >100 items OR consume significant AI context**:

| Endpoint | Paginate? | Rationale |
|----------|-----------|-----------|
| `getAllVulnerabilities()` | ✅ **YES** | Can return 1000s of items, high context usage |
| `listVulnsByAppId(appId, ...)` | ✅ **YES** | Can return 100s of items per application |
| `listVulnsInAppByName(name, ...)` | ✅ **YES** | Delegates to listVulnsByAppId |
| `getAttacks(...)` | ✅ **YES** | Can return 1000s of attack events |
| `getAttacksFiltered(...)` | ✅ **YES** | Already has limit/offset, standardize to spec |
| `getAllApplications()` | ⚠️ **EVALUATE** | Depends on typical org size (usually <100) |
| `listVulnsInAppByNameAndSessionMetadata()` | ❌ **NO** | Heavily filtered, typically <50 results |
| `listVulnsInAppByNameForLatestSession()` | ❌ **NO** | Session-scoped, typically <50 results |
| `getApplications(name)` | ❌ **NO** | Name-filtered, small result sets |

### Decision Criteria

Paginate an endpoint if:
- Expected result count > 100 items, OR
- Result consumes >50K tokens of AI context, OR
- Query performance degrades with large result sets

---

## Standardized Tool Documentation Template

Every paginated endpoint **must** include pagination documentation in the `@Tool` annotation:

### Full Template

```java
@Tool(
    name = "...",
    description = """
        [Brief description of what the endpoint does]

        Returns paginated results.

        Parameters:
        - [business parameters...]
        - page: Current page number (1-based, starts at 1, default: 1)
        - pageSize: Number of items per page (default: 50, max: 100)

        Response:
        - items: The data for the requested page
        - page: The page number returned
        - pageSize: Items per page used
        - totalItems: Total count of all items (null if unavailable)
        - hasMorePages: true if more pages exist
        - message: Optional validation warnings or informational messages

        Each page is a point-in-time snapshot; data may change between requests.
        """
)
```

### Simplified Template (Recommended)

For cleaner, more concise documentation:

```java
@Tool(
    name = "list_all_vulnerabilities",
    description = """
        Gets all vulnerabilities across all applications in the organization.

        Pagination: page (default: 1), pageSize (default: 50, max: 100)
        Returns pagination metadata including totalItems (when available) and hasMorePages.
        Check 'message' field for validation warnings or empty result info.
        """
)
```

---

## Data Consistency

### Point-in-Time Snapshots

- Each page request is an **independent point-in-time query**
- Data may change between page requests:
  - New items may be added
  - Existing items may be deleted
  - Item order may change
- **No cursor-based pagination** (accept potential inconsistency)
- This is standard REST API behavior

### Documentation

Document this behavior in tool descriptions where relevant:

```
Each page is a point-in-time snapshot; data may change between requests.
```

---

## Testing Requirements

### Required Test Cases

Implement the following test cases for every paginated endpoint:

#### 1. First Page
- Request page 1 with default pageSize
- Verify `page=1`, `items.size() <= 50`

#### 2. Middle Page
- Request page 2
- Verify `page=2`, correct offset applied
- Different items than page 1

#### 3. Last Page
- Request final page (when totalItems known)
- Verify `hasMorePages=false`
- Verify `items.size() <= pageSize`

#### 4. Beyond Last Page
- Request page N+1 where N is last page
- Verify `items=[]`, `hasMorePages=false`, `message` present
- Message should explain page exceeds available pages

#### 5. Empty Results
- Query with no matching data
- Verify `items=[]`, `totalItems=0`, `message="No ... found"`

#### 6. Page Size Boundaries
- Request `pageSize=50` (default) → works correctly
- Request `pageSize=100` (max) → works correctly
- Request `pageSize=101` → clamped to 100, message present

#### 7. Invalid Page Input
- `page=0` → clamped to 1, message present
- `page=-1` → clamped to 1, message present
- Verify correct data returned despite invalid input

#### 8. Invalid PageSize Input
- `pageSize=0` → clamped to 50, message present
- `pageSize=-10` → clamped to 50, message present
- Verify correct data returned despite invalid input

#### 9. Single Page Results
- Query returns <50 total items
- Verify single page, `hasMorePages=false`
- All items returned on page 1

#### 10. Null Parameters
- `page=null` → defaults to 1, no message
- `pageSize=null` → defaults to 50, no message
- Verify default behavior works

#### 11. totalItems Scenarios
- When available: verify accurate count
- When null: verify `hasMorePages` heuristic works correctly

### Test Helper Utility

Create reusable test utilities for pagination validation:

```java
public class PaginationTestHelper {

    /**
     * Asserts that a paginated response has valid structure and values
     */
    public static <T> void assertValidPaginatedResponse(
        PaginatedResponse<T> response,
        int expectedPage,
        int expectedPageSize
    ) {
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.items(), "Items list should not be null");
        assertEquals(expectedPage, response.page(), "Page mismatch");
        assertEquals(expectedPageSize, response.pageSize(), "Page size mismatch");
        assertTrue(response.page() >= 1, "Page should be 1-based");
        assertTrue(response.pageSize() >= 1 && response.pageSize() <= 100,
                   "Page size should be 1-100");

        // Items should not exceed pageSize
        assertTrue(response.items().size() <= response.pageSize(),
                   "Items count exceeds pageSize");

        // If totalItems is present, validate hasMorePages
        if (response.totalItems() != null) {
            boolean expectedHasMore = (response.page() * response.pageSize())
                                    < response.totalItems();
            assertEquals(expectedHasMore, response.hasMorePages(),
                        "hasMorePages inconsistent with totalItems");
        }

        // If empty, totalItems should be 0 or null
        if (response.items().isEmpty() && response.totalItems() != null) {
            assertTrue(response.totalItems() >= 0, "TotalItems should be non-negative");
        }
    }

    /**
     * Asserts that response contains a validation message
     */
    public static <T> void assertHasValidationMessage(PaginatedResponse<T> response) {
        assertNotNull(response.message(), "Expected validation message");
        assertFalse(response.message().isEmpty(), "Message should not be empty");
    }

    /**
     * Asserts that response represents an empty page
     */
    public static <T> void assertEmptyPage(PaginatedResponse<T> response) {
        assertTrue(response.items().isEmpty(), "Expected empty items");
        assertFalse(response.hasMorePages(), "Empty page should have no more pages");
    }

    /**
     * Asserts that hasMorePages logic is correct
     */
    public static <T> void assertHasMorePagesLogic(PaginatedResponse<T> response) {
        if (response.totalItems() != null) {
            // When totalItems available, hasMorePages should be accurate
            int itemsFetched = response.page() * response.pageSize();
            boolean expectedHasMore = itemsFetched < response.totalItems();
            assertEquals(expectedHasMore, response.hasMorePages(),
                        "hasMorePages logic incorrect");
        } else {
            // When totalItems null, hasMorePages uses heuristic
            // (can't validate without knowing actual total)
            assertNotNull(response.hasMorePages(), "hasMorePages should not be null");
        }
    }
}
```

---

## Implementation Checklist

Use this checklist when adding pagination to an endpoint:

- [ ] **Update method signature**: Add `Integer page, Integer pageSize` parameters (last in parameter list)
- [ ] **Change return type**: From `List<T>` to `PaginatedResponse<T>`
- [ ] **Implement parameter validation**: Validate and clamp with message building
- [ ] **Convert page to offset**: `offset = (actualPage - 1) * actualPageSize`
- [ ] **Apply limit and offset**: Use with SDK call (`setLimit()`, `setOffset()`)
- [ ] **Calculate totalItems**: If efficiently available from API, else `null`
- [ ] **Calculate hasMorePages**: Use `totalItems` or heuristic
- [ ] **Build message string**: Include validation warnings and empty result messages
- [ ] **Construct response**: `new PaginatedResponse<>(items, actualPage, actualPageSize, totalItems, hasMorePages, message)` where `actualPage` becomes the response's `page` field
- [ ] **Update @Tool annotation**: Add pagination documentation template
- [ ] **Add logging**: Log validation warnings and performance metrics
- [ ] **Write unit tests**: Cover all required test cases
- [ ] **Update documentation**: Update any READMEs or examples

---

## Example Implementation

Complete example of `getAllVulnerabilities()` with pagination:

```java
@Tool(
    name = "list_all_vulnerabilities",
    description = """
        Gets all vulnerabilities across all applications in the organization.

        Pagination: page (default: 1), pageSize (default: 50, max: 100)
        Returns pagination metadata including totalItems (when available) and hasMorePages.
        Check 'message' field for validation warnings or empty result info.
        """
)
public PaginatedResponse<VulnLight> getAllVulnerabilities(Integer page, Integer pageSize)
    throws IOException {

    logger.info("Listing all vulnerabilities - page: {}, pageSize: {}", page, pageSize);
    long startTime = System.currentTimeMillis();

    // Validate and clamp parameters
    StringBuilder messageBuilder = new StringBuilder();
    int actualPage = page != null && page > 0 ? page : 1;
    if (page != null && page < 1) {
        logger.warn("Invalid page number {}, clamping to 1", page);
        messageBuilder.append(String.format("Invalid page number %d, using page 1. ", page));
        actualPage = 1;
    }

    int actualPageSize = pageSize != null && pageSize > 0 ? pageSize : 50;
    if (pageSize != null && pageSize < 1) {
        logger.warn("Invalid pageSize {}, using default 50", pageSize);
        messageBuilder.append(String.format("Invalid pageSize %d, using default 50. ", pageSize));
        actualPageSize = 50;
    }
    if (pageSize != null && pageSize > 100) {
        logger.warn("Requested pageSize {} exceeds maximum 100, capping", pageSize);
        messageBuilder.append(String.format("Requested pageSize %d exceeds maximum 100, capped to 100. ", pageSize));
        actualPageSize = 100;
    }

    ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,
                                                httpProxyHost, httpProxyPort);

    try {
        // Convert to SDK offset (0-based)
        int offset = (actualPage - 1) * actualPageSize;
        int limit = actualPageSize;

        TraceFilterForm filterForm = new TraceFilterForm();
        filterForm.setLimit(limit);
        filterForm.setOffset(offset);

        Traces traces = contrastSDK.getTracesInOrg(orgID, filterForm);

        // Build vulnerability list
        List<VulnLight> vulnerabilities = new ArrayList<>();
        if (traces != null && traces.getTraces() != null) {
            for (Trace trace : traces.getTraces()) {
                vulnerabilities.add(new VulnLight(
                    trace.getTitle(),
                    trace.getRule(),
                    trace.getUuid(),
                    trace.getSeverity(),
                    new ArrayList<>(),
                    new Date(trace.getLastTimeSeen()).toString(),
                    trace.getLastTimeSeen(),
                    trace.getStatus(),
                    trace.getFirstTimeSeen(),
                    trace.getClosedTime()
                ));
            }
        }

        // Get totalItems if available from SDK response (don't make extra query)
        Integer totalItems = (traces != null && traces.getCount() != null)
                           ? traces.getCount()
                           : null;

        // Calculate hasMorePages
        boolean hasMorePages;
        if (totalItems != null) {
            hasMorePages = (actualPage * actualPageSize) < totalItems;
        } else {
            // Heuristic: if we got a full page, assume more exist
            hasMorePages = vulnerabilities.size() == actualPageSize;
        }

        // Handle empty results messaging
        if (vulnerabilities.isEmpty() && actualPage == 1) {
            messageBuilder.append("No vulnerabilities found. ");
        } else if (vulnerabilities.isEmpty() && actualPage > 1) {
            if (totalItems != null) {
                int totalPages = (int) Math.ceil((double) totalItems / actualPageSize);
                messageBuilder.append(String.format(
                    "Requested page %d exceeds available pages (total: %d). ",
                    actualPage, totalPages
                ));
            } else {
                messageBuilder.append(String.format(
                    "Requested page %d returned no results. ",
                    actualPage
                ));
            }
        }

        String message = messageBuilder.length() > 0 ? messageBuilder.toString().trim() : null;

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved {} vulnerabilities for page {} (pageSize: {}, totalItems: {}, took {} ms)",
                   vulnerabilities.size(), actualPage, actualPageSize, totalItems, duration);

        return new PaginatedResponse<>(
            vulnerabilities,
            actualPage,
            actualPageSize,
            totalItems,
            hasMorePages,
            message
        );

    } catch (Exception e) {
        logger.error("Error listing all vulnerabilities", e);
        throw new IOException("Failed to list all vulnerabilities: " + e.getMessage(), e);
    }
}
```

---

## Design Rationale

### Why 1-Based Pagination?

- **Natural language alignment**: AI agents describe "page 1, page 2" naturally
- **REST API conventions**: GitHub, Stripe, most public APIs use 1-based
- **Better error messages**: "Page 0" sounds like an error state
- **Intuitive for humans**: Non-technical users expect pages to start at 1

### Why Generic Response Wrapper?

- **Consistency**: All paginated endpoints return same structure
- **Maintainability**: Single definition, less code duplication
- **AI-friendly**: Consistent parsing across different endpoints
- **Extensibility**: Easy to add fields (e.g., sorting metadata) later

### Why Message Field?

- **AI awareness**: AI agents can understand why their input was changed
- **Debugging**: Helps identify issues with invalid inputs
- **User experience**: End users see explanations for empty results
- **No exceptions**: Graceful handling instead of errors

### Why Default 50, Max 100?

- **Context efficiency**: 50 items ≈ 10K-25K tokens (reasonable)
- **Response time**: Fast even in fallback mode
- **Industry standard**: Aligns with GitHub (30), Jira (50), Stripe (10-100)
- **Flexibility**: Max 100 allows power users to reduce API calls

### Why Nullable totalItems?

- **Performance**: Don't make expensive COUNT queries
- **Graceful degradation**: Works even when count unavailable
- **Efficiency**: Use count only when "free" from API response

---

## Summary of Key Decisions

✅ **Generic `PaginatedResponse<T>` with `message` field**
✅ **Paginate high-volume endpoints only (>100 items or high context usage)**
✅ **Clamp invalid inputs, populate `message` field with warnings**
✅ **Return paginated structure even for empty results (with message)**
✅ **Page beyond end returns empty page with explanatory message**
✅ **No sorting support (defer to v2)**
✅ **Pagination params always last in method signatures**
✅ **No backward compatibility needed (not yet deployed)**
✅ **Return `totalItems` only when efficiently available**
✅ **No timeout support yet**
✅ **Accept point-in-time data inconsistency**
✅ **Simple standardized documentation template**
✅ **Comprehensive test cases with helper utilities**
✅ **Defaults: page=1, pageSize=50, max=100**
✅ **1-based page numbering (convert to 0-based offset internally)**

---

## Future Enhancements (Out of Scope for v1.0)

- **Sorting**: Add `sort` parameter for ordering results
- **Cursor-based pagination**: For guaranteed consistency across pages
- **Field filtering**: Return only specific fields to reduce context
- **Response compression**: For large result sets
- **Caching**: Cache multi-page fetches of same dataset
- **Async pagination**: Stream results for very large datasets
- **Configurable defaults**: Allow per-deployment default page sizes

---

## References

- MCP Contrast Server CLAUDE.md: Project overview and architecture
- AssessService.java: Current implementation of vulnerability queries
- ADRService.java: Example of existing limit/offset patterns
- Contrast SDK Documentation: API capabilities and limitations

---

**END OF SPECIFICATION**

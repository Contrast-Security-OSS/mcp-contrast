# Params Pattern Design Audit

**Date:** 2025-10-18
**Status:** Pre-implementation verification

## Summary

Audit of the Tool Params Pattern design document against existing codebase to identify discrepancies, missing pieces, and implementation blockers.

## ✅ What's Already Implemented

### FilterHelper.ParseResult<T>
**Location:** `src/main/java/com/contrast/labs/ai/mcp/contrast/FilterHelper.java:41-65`

**Status:** ✅ Fully implemented, matches design exactly

```java
public static class ParseResult<T> {
    private final T value;
    private final String validationMessage;

    public T getValue() { }
    public String getValidationMessage() { }
    public boolean hasValidationMessage() { }
}
```

**Notes:**
- Design document correctly references this
- Already used in `parseDateWithValidation()`
- No changes needed

### FilterHelper Parsing Methods
**Status:** ✅ Implemented

- `parseCommaSeparated(String)` - ✅ Works as designed
- `parseCommaSeparatedUpperCase(String)` - ✅ Works as designed
- `parseCommaSeparatedLowerCase(String)` - ✅ Works as designed
- `parseDateWithValidation(String, String)` - ✅ Returns ParseResult<Date>

**Notes:** All helper methods exist and work as documented

### PaginatedResponse Record
**Location:** `src/main/java/com/contrast/labs/ai/mcp/contrast/data/PaginatedResponse.java`

**Status:** ✅ Structure matches, ⚠️ Missing error() method

**Current:**
```java
public record PaginatedResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    Integer totalItems,
    boolean hasMorePages,
    String message
) {
    // Has empty() static method
    public static <T> PaginatedResponse<T> empty(int page, int pageSize, String message)
}
```

**Issue:** Design calls for `error()` method, but code has `empty()` method instead.

**Options:**
1. Rename `empty()` to `error()` (breaking change if anything uses it)
2. Add `error()` as alias to `empty()`
3. Keep `empty()` and update design doc

**Recommendation:** Add `error()` as an alias - semantically clearer for validation failures

```java
public static <T> PaginatedResponse<T> error(int page, int pageSize, String errorMessage) {
    return empty(page, pageSize, errorMessage);
}
```

## ❌ What's Missing

### 1. PaginationParams Class
**Status:** ❌ Not implemented

**Location:** Should be `src/main/java/com/contrast/labs/ai/mcp/contrast/PaginationParams.java`

**Current State:** Pagination validation is inline in AssessService.java:485-502 using StringBuilder

**Impact:** HIGH - Core component of the pattern, needed for all paginated endpoints

**Implementation Required:**
```java
public record PaginationParams(
    int page,
    int pageSize,
    int offset,
    int limit,
    List<String> warnings
) {
    public static PaginationParams of(Integer page, Integer pageSize) { ... }
    public boolean isValid() { return true; }
}
```

**Estimated LOC:** ~50 lines

### 2. VulnerabilityFilterParams Class
**Status:** ❌ Not implemented

**Location:** Should be `src/main/java/com/contrast/labs/ai/mcp/contrast/VulnerabilityFilterParams.java`

**Current State:** Filter validation is inline in AssessService.java:504-605 using StringBuilder

**Impact:** HIGH - Core component, needed for vulnerability filtering

**Implementation Required:**
```java
public record VulnerabilityFilterParams(
    TraceFilterForm form,
    String appId,
    List<String> warnings,
    List<String> errors
) {
    public static VulnerabilityFilterParams of(...) { ... }
    public boolean isValid() { return errors.isEmpty(); }
    public TraceFilterForm toTraceFilterForm() { return form; }
}
```

**Estimated LOC:** ~150 lines (lots of filter parsing logic)

### 3. PaginatedResponse.error() Method
**Status:** ⚠️ Has empty(), needs error() alias

**Implementation Required:**
```java
public static <T> PaginatedResponse<T> error(int page, int pageSize, String errorMessage) {
    return new PaginatedResponse<>(List.of(), page, pageSize, 0, false, errorMessage);
}
```

**Estimated LOC:** 3 lines

## 🔧 What Needs Refactoring

### AssessService.getAllVulnerabilities()
**Location:** `src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java:462-693`

**Current State:** ~230 lines mixing validation, SDK calls, response building

**Current Pattern (Old):**
```java
// Lines 484-502: Inline pagination validation with StringBuilder
StringBuilder messageBuilder = new StringBuilder();
int actualPage = page != null && page > 0 ? page : 1;
if (page != null && page < 1) {
    messageBuilder.append("Invalid page...");
}
// ... 18 more lines

// Lines 504-605: Inline filter validation with StringBuilder
List<String> severityList = FilterHelper.parseCommaSeparatedUpperCase(severities);
// ... 100 more lines of validation

// Lines 607-693: SDK calls and response building mixed together
Traces traces = contrastSDK.getTracesInOrg(...);
// ... 86 more lines
```

**Target Pattern (New):**
```java
// Lines 1-2: Parse params
PaginationParams pagination = PaginationParams.of(page, pageSize);
VulnerabilityFilterParams filters = VulnerabilityFilterParams.of(...);

// Lines 3-9: Check validity
if (!filters.isValid()) {
    return PaginatedResponse.error(..., String.join(" ", filters.errors()));
}

// Lines 10-13: Accumulate warnings
List<String> warnings = new ArrayList<>();
warnings.addAll(pagination.warnings());
warnings.addAll(filters.warnings());

// Lines 14-30: SDK calls and response building
TraceFilterForm form = filters.toTraceFilterForm();
form.setOffset(pagination.offset());
// ... execute query, build response
```

**Reduction:** ~230 lines → ~50 lines in service method + ~200 lines in params classes (testable separately)

**Breaking Changes:** None (method signature stays the same)

## ⚠️ Design Issues Found

### Issue 1: Date Validation is Currently Soft Failure

**Problem:** Current code treats unparseable dates as warnings (soft failures)

**Current Behavior (AssessService.java:514-518):**
```java
if (startDateResult.hasValidationMessage()) {
    messageBuilder.append(startDateResult.getValidationMessage()).append(" ");
}
// Execution continues even with unparseable date
```

**Expected Behavior:** Should be hard failure if date is required/meaningful

**Impact:** This is the core issue that prompted the hard/soft failure distinction

**Resolution:** VulnerabilityFilterParams.of() must add unparseable dates to `errors` list, not `warnings`

### Issue 2: No Logical Contradiction Checks

**Problem:** Current code doesn't validate date ranges

**Current Behavior:** Can set startDate > endDate, SDK might fail or return incorrect results

**Expected Behavior:** VulnerabilityFilterParams should check:
```java
if (form.getStartDate() != null && form.getEndDate() != null) {
    if (form.getStartDate().after(form.getEndDate())) {
        errors.add("lastSeenAfter must be before lastSeenBefore");
    }
}
```

**Impact:** MEDIUM - Would improve AI experience

**Resolution:** Add to VulnerabilityFilterParams.of()

### Issue 3: Smart Defaults Message Placement

**Current Behavior (AssessService.java:564-566):**
```java
if (usingSmartDefaults) {
    messageBuilder.append("Showing actionable vulnerabilities only...");
}
```

**Issue:** Message appears AFTER status filter is applied, mixed with other validation

**Expected Behavior:** Message should be part of status filter validation in params object

**Impact:** LOW - Works but not clean separation

**Resolution:** Move to VulnerabilityFilterParams.of()

## 📋 Implementation Checklist

### Phase 1: Foundation (No breaking changes)
- [ ] Add `PaginatedResponse.error()` static method (3 lines)
- [ ] Create `PaginationParams` class (~50 lines)
- [ ] Write `PaginationParamsTest` (~200 lines)

### Phase 2: Filter Params (No breaking changes)
- [ ] Create `VulnerabilityFilterParams` class (~150 lines)
- [ ] Add date range validation (startDate < endDate check)
- [ ] Add hard failure for unparseable dates
- [ ] Write `VulnerabilityFilterParamsTest` (~300 lines)

### Phase 3: Refactor Service Method (No breaking changes)
- [ ] Refactor `getAllVulnerabilities()` to use params objects
- [ ] Update to check `filters.isValid()` before execution
- [ ] Update to accumulate warnings from both params objects
- [ ] Run existing tests to verify no regressions

### Phase 4: Test Updates
- [ ] Update `AssessServiceTest` expectations (smart defaults now produce messages)
- [ ] Verify all 36 tests still pass

### Phase 5: Documentation
- [ ] Update design doc to match implementation (error vs empty)
- [ ] Add JavaDoc to params classes

## 🚫 Potential Blockers

### Blocker 1: NONE IDENTIFIED
All required infrastructure exists:
- FilterHelper with ParseResult<T> ✅
- PaginatedResponse structure ✅
- Comma-separated parsing ✅
- Date parsing with validation ✅

### Risk 1: Test Expectations Changed
**Risk:** Tests expect no message when using defaults

**Current Tests:** Some tests assert `assertNull(response.message())`

**New Behavior:** Smart defaults produce message about filtering

**Mitigation:** Already fixed in commit 54abdfa - tests updated to not assert on message field for pagination-only tests

**Status:** ✅ Already handled

### Risk 2: Breaking Change to Tool Signature
**Risk:** Params pattern might require signature changes

**Mitigation:** Pattern uses static factories internally, tool signature unchanged

**Status:** ✅ No risk - signature stays identical

## 📊 Effort Estimate

**Total Implementation:**
- PaginationParams class: 1 hour
- PaginationParams tests: 1 hour
- VulnerabilityFilterParams class: 2 hours
- VulnerabilityFilterParams tests: 2 hours
- Refactor getAllVulnerabilities: 1 hour
- Update existing tests: 0.5 hours
- PaginatedResponse.error(): 5 minutes

**Total: ~7.5 hours**

## ✅ Recommendation

**PROCEED WITH IMPLEMENTATION**

No blockers identified. All dependencies exist. Pattern can be implemented incrementally without breaking changes.

**Suggested Order:**
1. Add PaginatedResponse.error() (quick win)
2. Implement PaginationParams (reusable across all tools)
3. Implement VulnerabilityFilterParams (vulnerability-specific)
4. Refactor getAllVulnerabilities to use both
5. Apply pattern to other paginated tools (getAttacks, etc.)

## 📝 Design Doc Updates Needed

Minor discrepancies to fix in design doc:

1. **PaginatedResponse.error() vs empty()**: Document should mention both methods or note that empty() can be used for errors
2. **Actual package locations**: Design shows examples but should reference actual file locations
3. **Import statements**: Examples in design don't show imports, might confuse readers

These are documentation-only issues, not implementation blockers.

---

**Audit Complete:** Design is sound and implementable with no blocking issues.

# Attack API Performance Concerns

## Executive Summary

The attack retrieval API has **severe performance issues** due to an N+1 query problem in the resource assembler. A single API call with the current MCP defaults (limit=1000) triggers approximately **6,000-11,000 database queries**, which matches historical "attack event API" performance complaints.

**Critical Issue**: `NgAttackResourceAssembler` makes 6-11 database queries per attack to enrich response data.

## Current MCP Behavior

### Request Path

1. `ADRService.getAttacks()` → `SDKExtension.getAttacks(orgID)`
2. SDK sends: `POST /ng/{orgId}/attacks?limit=1000&offset=0&sort=-startTime&expand=skip_links`
3. TeamServer processes via `NgAttackRestController.getAttacksV2()`
4. Each attack passes through `NgAttackResourceAssembler.toResource()`

**Source References**:
- MCP: `src/main/java/com/contrast/labs/ai/mcp/contrast/ADRService.java:150`
- SDK: `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtension.java:373,378`
- TeamServer Controller: `teamserver-app/src/main/java/contrast/teamserver/rest/ng/rasp/discovery/attack/NgAttackRestController.java:266,275`
- Assembler: `teamserver-app/src/main/java/contrast/teamserver/rest/ng/rasp/discovery/attack/resource/NgAttackResourceAssembler.java:101-187`

## Performance Analysis

### Database Query Breakdown

**Initial queries (3 total)**:
1. ID selection query with filters/pagination
2. Attack entity hydration query
3. Total count query (for pagination metadata)

**Per-Attack Assembler Queries (6-11 per attack)**:

Always executed (6 queries/attack):
- Line 101: `attackService.getAttackers(attack)` - get attacker IPs
- Line 108: `attackApplicationService.getAttackApplicationsByAttackID()` - get applications
- Line 116: `eventService.countUnsuppressedEvents()` - count probes
- Line 120: `eventService.getEventNamesByAttackId()` - get rule names
- Line 124: `attackService.getServersByAttackID()` - get servers
- Line 155: `attackService.getAttackIntervalDates()` - get time window

With "notes" expand (+3-5 queries/attack):
- Line 141: `attackService.countByServerAndAttack()` - per server counts (nested loop!)
- Line 139: `attackService.getAttackServerIntervalTime()` - per server timing (nested loop!)
- Line 165: `attackService.findMaxApplicationImportanceByAttackId()` - app importance
- Line 171: `protectionRuleService.findProtectionRuleMaxImpactByAttackId()` - rule impact
- Line 180: `attackService.getUserAgentsFromAttack()` - user agents

With "source_name" expand (+1 query/attack):
- Line 187: `sourceNameService.findMatchByAttack()` - source name lookup

### Query Volume Calculations

**Current MCP defaults (limit=1000, no expand)**:
```
3 initial queries
+ (6 queries/attack × 1000 attacks)
= ~6,003 queries per API call
```

**With "notes" expand (assuming 2 servers/attack)**:
```
3 initial queries
+ (6 base queries/attack × 1000)
+ (3 queries/attack × 1000)
+ (2 server queries/attack × 2 servers × 1000)
= ~13,003 queries per API call
```

**With lower limit=50**:
```
3 initial queries
+ (6 queries/attack × 50 attacks)
= ~303 queries per API call
```

## Root Causes

### 1. N+1 Query Anti-Pattern

The assembler was designed for small result sets (TeamServer UI default is limit=10). It fetches supplemental data per-attack instead of batching.

### 2. Rich Data Enrichment

Unlike simple list endpoints, this endpoint decorates each attack with:
- Attacker information
- Related applications with nested assembly
- Event/probe counts
- Protection rule details
- Server information
- Time window calculations

### 3. MCP Defaults Too Aggressive

The SDK extension defaults to `limit=1000`, which is 100x larger than the TeamServer UI default of 10. This was likely chosen to minimize pagination overhead but has the opposite effect.

### 4. Query Complexity

The base attack query involves 7+ joins depending on filters:
- `AttackEvent` (for filtering by protection rules, bot blockers, IP denylist)
- `AgentMessage`
- `AgentMessageAttacker` (for attacker filtering)
- `AttackApplication` (for EAC - Enterprise Access Control)
- `Application` (for app filters, importance)
- `Server` (for server/environment filters)
- `AttackTag` (for tag filtering)

**Source**: `teamserver-app/src/main/java/contrast/teamserver/dao/rasp/AttackFiltersSpecification.java`

## Impact Assessment

### Performance Characteristics

With limit=1000:
- **~6,000-13,000 SQL queries** per API call
- **Minutes of execution time** for large result sets
- **Database connection pool exhaustion** under load
- **Memory pressure** from holding thousands of intermediate results

### User Impact

- MCP tools experience **long delays** when retrieving attacks
- Risk of **timeouts** (default 2-minute timeout for MCP)
- **Poor user experience** for AI-assisted security analysis
- Potential **system instability** during high-volume retrieval

## Recommendations

### Immediate Actions (MCP Changes)

#### 1. Lower Default Limit to 50-100

**Priority**: CRITICAL

Change `SDKExtension.java:373`:
```java
// Before
if (limit == null) limit = 1000;

// After
if (limit == null) limit = 50;  // Start conservative, tune upward
```

**Impact**: Reduces queries from ~6,000 to ~300 per call (20x improvement)

#### 2. Keep skip_links Parameter

**Priority**: INFORMATIONAL

The MCP currently sends `expand=skip_links` which is **correct and should be kept**.

**What skip_links does**:
- Prevents generation of HATEOAS link objects in JSON responses
- Reduces JSON payload size by omitting `_links` arrays
- Does NOT cause additional database queries
- Improves network transfer performance

**Implementation**: `NgResourceAssemblerHelper.java:45-48,117-124` checks for `skip_links` in the expand list and skips link generation when present.

**Recommendation**: Keep this parameter - it reduces response size without performance cost.

#### 3. Implement Smarter Pagination

**Priority**: MEDIUM

Add logic to ADRService to:
- Start with small pages (25-50)
- Paginate automatically when needed
- Allow AI to request specific ranges
- Stop fetching if sufficient results found

Example:
```java
public List<AttackSummary> getAttacksWithPagination(Integer maxResults) {
    List<AttackSummary> allResults = new ArrayList<>();
    int pageSize = 50;
    int offset = 0;

    while (allResults.size() < maxResults) {
        List<Attack> page = extendedSDK.getAttacks(orgID, null, pageSize, offset, null);
        if (page.isEmpty()) break;

        allResults.addAll(page.stream()
            .map(AttackSummary::fromAttack)
            .collect(Collectors.toList()));

        offset += pageSize;
    }

    return allResults;
}
```

#### 4. Consider Using /attacks/ids Endpoint

**Priority**: LOW (if applicable)

For use cases that only need attack identifiers:
- Use `GET /ng/{orgId}/attacks/ids` endpoint
- Skips expensive assembler entirely
- Returns UUIDs only
- Follow up with individual attack details only when needed

**Source**: `NgAttackRestController.java:298-357`

### Long-Term Solutions (Require TeamServer Changes)

#### 5. Batch Queries in Assembler

**Priority**: HIGH (TeamServer team)

Refactor `NgAttackResourceAssembler` to:
- Accept `List<Attack>` and return `List<NgAttackResource>`
- Fetch all supplemental data in batched queries
- Map results back to attacks in memory

Example pattern:
```java
// Instead of per-attack queries
for (Attack attack : attacks) {
    resource.setAttackers(attackService.getAttackers(attack));  // N queries
}

// Batch query pattern
Set<Long> attackIds = attacks.stream()
    .map(Attack::getId)
    .collect(Collectors.toSet());
Map<Long, List<String>> attackersMap = attackService.getAttackersByAttackIds(attackIds);  // 1 query
for (Attack attack : attacks) {
    resource.setAttackers(attackersMap.get(attack.getId()));
}
```

**Impact**: Reduces queries from O(N×M) to O(M) where M is number of enrichment types

#### 6. Add Database Indexes

**Priority**: MEDIUM (TeamServer team)

Verify/add indexes on foreign keys used in joins:
- `AttackEvent.attack_id`
- `AttackApplication.attack_id`
- `AttackApplication.application_id`
- `Attack.organization_id`
- `Attack.start_time` (for sorting)
- `AgentMessage.attack_event_id`

#### 7. Implement Result Caching

**Priority**: LOW (TeamServer team)

Cache frequently accessed reference data:
- Protection rule names/details
- Application information
- Server lists
- User agents (relatively static)

Use short TTL (30-60 seconds) to balance freshness and performance.

#### 8. Create Lightweight Endpoint Variant

**Priority**: MEDIUM (TeamServer team)

Create `/ng/{orgId}/attacks/summary` endpoint that:
- Returns minimal attack information
- Skips expensive enrichment
- Designed for listing/filtering use cases
- Reserve full `/attacks` endpoint for detail views

## Pagination Effectiveness

**Question**: Does pagination help with this API?

**Answer**: YES - dramatically!

Pagination directly limits the number of attacks processed by the assembler, which is the dominant cost:

| Limit | Queries (no expand) | Queries (with notes) | Improvement |
|-------|---------------------|----------------------|-------------|
| 1000  | ~6,003             | ~13,003              | Baseline    |
| 100   | ~603               | ~1,303               | 10x faster  |
| 50    | ~303               | ~653                 | 20x faster  |
| 25    | ~153               | ~353                 | 40x faster  |

However, pagination does **not** help with:
- The count query (always runs full scan)
- The initial ID selection query (runs before pagination)

These are secondary concerns compared to the assembler N+1 problem.

## Implementation Priority

1. **Immediate** (this week): Lower SDK default limit to 50
2. **Short-term** (this sprint): Implement smarter pagination in MCP
3. **Long-term** (coordinate with TeamServer): Batch queries in assembler

Note: The `expand=skip_links` parameter should be kept as it reduces JSON payload size without performance cost.

## Monitoring & Validation

After implementing changes:

1. **Monitor query counts**: Enable SQL logging and verify reduction
2. **Track response times**: Measure end-to-end latency improvement
3. **Test with production data**: Verify performance with realistic attack volumes
4. **User feedback**: Confirm MCP tools respond acceptably

Target metrics:
- Response time: < 5 seconds for 50 attacks
- Query count: < 500 per API call
- Timeout rate: 0%

## References

### MCP Server Code
- `src/main/java/com/contrast/labs/ai/mcp/contrast/ADRService.java:138-169`
- `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtension.java:368-425`

### TeamServer Code
- Controller: `teamserver-app/src/main/java/contrast/teamserver/rest/ng/rasp/discovery/attack/NgAttackRestController.java`
- Service: `teamserver-app/src/main/java/contrast/teamserver/service/AttackService.java:127-139`
- DAO: `teamserver-app/src/main/java/contrast/teamserver/dao/rasp/AttackJpaDao.java:226-240`
- Assembler: `teamserver-app/src/main/java/contrast/teamserver/rest/ng/rasp/discovery/attack/resource/NgAttackResourceAssembler.java:59-200`
- Filters: `teamserver-app/src/main/java/contrast/teamserver/dao/rasp/AttackFiltersSpecification.java`
- skip_links Helper: `teamserver-app/src/main/java/contrast/teamserver/rest/ng/utils/NgResourceAssemblerHelper.java:45-48,117-124`

## Acknowledgments

This analysis combines findings from two independent investigations:
- Initial analysis focused on query complexity and joins
- Follow-up analysis identified the N+1 assembler problem (critical finding)

The N+1 problem in the assembler is the dominant performance issue and should be addressed first.

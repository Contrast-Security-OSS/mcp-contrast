# Test Plan: `search_servers` Tool

## Purpose

Validate server inventory search, agent-health and Protect fields, filter algebra, pagination,
sorting, application expansion, and safe empty/error behavior against a live TeamServer.

## Preconditions

* Credentials can view at least two servers.
* Prefer data spanning multiple environments, statuses, agent versions, and applications.
* Tag tests require at least one visible tagged server.
* `withoutApplications=true` requires the organization permission and matching seeded data.

## Test Cases

### 1. Baseline inventory

Call `search_servers()` and verify success, non-empty unique server IDs, valid ONLINE/OFFLINE
statuses, non-negative application counts, and pagination metadata consistent with the items.

### 2. Filter dimensions

Exercise `quickFilter`, `environments`, `logLevels`, `tags`, `applicationIds`,
`withoutApplications`, `agentVersions`, and `keyword`. Verify every returned item satisfies each
requested filter. For comma-separated values, verify OR within a parameter and AND across
different parameters.

### 3. Application expansion

Compare the default response with `includeApplications=true`. The default must omit the
application list while retaining `applicationCount`; expansion must return a non-null list whose
size and identities agree with the visible application count.

### 4. Sorting

For `name`, `environment`, `lastActivity`, and `agentVersion`, test both ASC and DESC over data
with at least two distinct non-null values. Environment ordering is lexical because TeamServer
sorts its persisted enum string; agent-version ordering is string ordering, not semantic version
ordering.

### 5. Pagination

Request two adjacent small pages and verify disjoint server IDs, stable `totalItems`, and correct
`hasMorePages`. Request an out-of-range page and verify an empty successful page retains the true
total count.

### 6. Empty and validation behavior

Search for a guaranteed-missing exact tag and verify success, an empty item list, zero total, and
the standard no-results warning. Verify invalid enum values, mutually exclusive application
filters, and malformed sorts return validation errors without exposing downstream details.

### 7. Nullable coverage fields

Confirm `protectEnabled=null` remains unknown rather than false, and that a missing or unavailable
latest agent version yields `agentOutOfDate=null`. Treat non-null Assess/Protect values as the
effective state TeamServer returned, which may reflect the first visible application.

# MCP Contrast Test Plans

Test plans for validating MCP Contrast server tools.

## Quick Reference

| Tool | Test Plan | Consolidates |
|------|-----------|--------------|
| search_applications | [test-plan-search_applications.md](test-plan-search_applications.md) | 5 old plans |
| search_attacks | [test-plan-search_attacks.md](test-plan-search_attacks.md) | 2 old plans |
| search_vulnerabilities | [test-plan-search_vulnerabilities.md](test-plan-search_vulnerabilities.md) | 1 old plan |
| search_app_vulnerabilities | [test-plan-search_app_vulnerabilities.md](test-plan-search_app_vulnerabilities.md) | 1 old plan |
| get_vulnerability | [test-plan-get_vulnerability.md](test-plan-get_vulnerability.md) | 1 old plan |
| list_vulnerability_types | [test-plan-list_vulnerability_types.md](test-plan-list_vulnerability_types.md) | 1 old plan |
| get_session_metadata | [test-plan-get_session_metadata.md](test-plan-get_session_metadata.md) | 1 old plan |
| get_protect_rules | [test-plan-get_protect_rules.md](test-plan-get_protect_rules.md) | 1 old plan |
| list_application_libraries | [test-plan-list_application_libraries.md](test-plan-list_application_libraries.md) | 1 old plan |
| list_applications_by_cve | [test-plan-list_applications_by_cve.md](test-plan-list_applications_by_cve.md) | 1 old plan |
| get_route_coverage | [test-plan-get_route_coverage.md](test-plan-get_route_coverage.md) | 1 old plan |
| get_scan_project | [test-plan-get_scan_project.md](test-plan-get_scan_project.md) | 1 old plan |
| get_scan_results | [test-plan-get_scan_results.md](test-plan-get_scan_results.md) | 1 old plan |

## Recommended Execution Order

### 1. Discovery Tools (run first)
- **search_applications** - Find available applications and their IDs
- **list_vulnerability_types** - Discover vulnerability rule types

### 2. Vulnerability Tools
- **search_vulnerabilities** - Org-level vulnerability search
- **search_app_vulnerabilities** - App-level vulnerability search
- **get_vulnerability** - Single vulnerability details

### 3. Attack/Protection Tools
- **search_attacks** - Attack event search
- **get_protect_rules** - ADR/Protect rule configuration

### 4. Supporting Tools
- **get_session_metadata** - Session metadata for applications
- **list_application_libraries** - Library inventory
- **list_applications_by_cve** - CVE exposure search
- **get_route_coverage** - Route/endpoint coverage
- **get_scan_project** - SAST project info
- **get_scan_results** - SAST scan results

## Test Data Requirements

Before running tests, ensure you have access to:
- At least one application with vulnerabilities
- Applications with various tags and metadata for filtering
- Attack events (if testing Protect features)
- SAST scan data (if testing SAST tools)

## Running Tests

Use the [Test Plan Runner Prompt](../test-plan-RUNNER-prompt.md):

```
TEST_TOOL=search_applications
```

Results are written to `results-{TEST_TOOL}.md`.

## Migration from Old Plans

Old test plans (18 files) have been archived to `../test-plans-archive/`. See that directory's README for the mapping from old to new plans.

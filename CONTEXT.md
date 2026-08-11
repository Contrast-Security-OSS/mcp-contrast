# Contrast MCP Server

Glossary for the mcp-contrast repository. Tool contracts, docs, and code must use these terms consistently.

## Language

**Vulnerability status**:
One of seven canonical filter values on the legacy TeamServer vulnerability model. The values are Reported, Suspicious, Confirmed, NotAProblem, Remediated, Fixed, and AutoRemediated. `ValidationConstants.VALID_VULN_STATUSES_CSV` is the source of truth for documentation and validation.
_Avoid_: Issue Status, trace status

**Issue Status**:
The NorthStar status vocabulary on Issue entities. The values are REPORTED, CONFIRMED, REMEDIATED, NOT_A_PROBLEM, and FIXED. Suspicious and AutoRemediated exist only as Vulnerability statuses, never as Issue Statuses.
_Avoid_: using this term for legacy Vulnerability statuses

**Status display label**:
The human-readable status text TeamServer returns on vulnerability records, for example "Remediated - Auto-Verified" for a vulnerability in the AutoRemediated status. Filters take canonical Vulnerability statuses. Results may carry display labels that differ from the filter value.
_Avoid_: treating a display label as a valid filter value

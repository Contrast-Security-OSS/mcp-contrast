# Contrast MCP Server

Glossary for the mcp-contrast repository. Tool contracts, docs, and code must use these terms consistently.

## Language

**Vulnerability status**:
One of seven canonical filter values on the legacy TeamServer vulnerability model. The values are Reported, Suspicious, Confirmed, NotAProblem, Remediated, Fixed, and AutoRemediated. `ValidationConstants.VALID_VULN_STATUSES_CSV` is the source of truth for documentation and validation.
_Avoid_: Issue Status, trace status

**AutoRemediated**:
A Vulnerability status, and a misnomer. It means the Contrast Agent verified at runtime, by observing data flow through the previously vulnerable code, that the vulnerability was fixed, so Contrast marked it Remediated. Contrast did not fix the vulnerability. The API value is frozen legacy terminology; the mandated term today is Auto-Verified, shown in the display label "Remediated - Auto-Verified".
_Avoid_: describing this status as Contrast fixing, remediating, or auto-remediating a vulnerability

**SmartFix**:
The Contrast product that actually auto-remediates vulnerabilities, using AI to produce fixes. No Vulnerability status represents a SmartFix remediation yet.
_Avoid_: conflating SmartFix with the AutoRemediated status or with Auto-Verified

**Issue Status**:
The NorthStar status vocabulary on Issue entities. The values are REPORTED, CONFIRMED, REMEDIATED, NOT_A_PROBLEM, and FIXED. Suspicious and AutoRemediated exist only as Vulnerability statuses, never as Issue Statuses.
_Avoid_: using this term for legacy Vulnerability statuses

**Status display label**:
The human-readable status text TeamServer returns on vulnerability records, for example "Remediated - Auto-Verified" for a vulnerability in the AutoRemediated status. Filters take canonical Vulnerability statuses, and this server's tool results carry canonical Vulnerability statuses, not display labels.
_Avoid_: treating a display label as a valid filter value or exposing one in a tool result

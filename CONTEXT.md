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

**Vulnerability type**:
The kind of vulnerability a finding represents, for example `sql-injection` or `reflected-xss`. Values are lowercase-hyphenated and org-specific, discoverable via `list_vulnerability_types`. Synonymous with the TeamServer term "rule name", a historical artifact: the Contrast Agent used a detection rule per vulnerability type, so the `sql-injection` rule found SQL injection vulnerabilities. Vulnerability type is the canonical term.
_Avoid_: rule name, rule type, except when naming the raw TeamServer API surface (e.g. `getRules()`)

**Licensed application**:
An application whose merged license service level in TeamServer is exactly Licensed, meaning it holds an Assess license. TeamServer refuses route coverage and single-vulnerability detail reads for unlicensed applications with an opaque HTTP 403 "Authorization failure".
_Avoid_: describing those 403s as credential problems, missing data, or language limitations

**Route coverage**:
The set of routes an Assess agent observed in an application, each either discovered (found in the code) or exercised (hit by an HTTP request).
_Avoid_: route data, endpoint coverage

**Status display label**:
The human-readable status text TeamServer returns on vulnerability records, for example "Remediated - Auto-Verified" for a vulnerability in the AutoRemediated status. Filters take canonical Vulnerability statuses, and this server's tool results carry canonical Vulnerability statuses, not display labels.
_Avoid_: treating a display label as a valid filter value or exposing one in a tool result

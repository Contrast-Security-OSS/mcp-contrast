# Test Cases for `get_protect_rules` Tool

## Overview

The `get_protect_rules` tool retrieves Protect (ADR) rules configured for a specific application. It returns the protection configuration including rule names, modes for each environment, and rule-specific settings.

**Required Parameter:**
- `appId` - Application ID (use `search_applications` to find)

**Response includes:**
- Rule name and type (Protect Rule, CVE Shield, Virtual Patch)
- Description of the vulnerability the rule protects against
- Mode settings per environment: `development`, `qa`, `production`
- Mode values: `MONITORING`, `BLOCKING`, `BLOCKING_PERIMETER`, `OFF`
- Blocking capabilities: `canBlock`, `canBlockAtPerimeter`, `isMonitorAtPerimeter`
- CVE information for CVE Shield rules (name, score, description)
- Parent rule relationships for sub-rules

---

## Known Test Data

### Test Applications

| App Name | App ID | Language | Notable Rules |
|----------|--------|----------|---------------|
| thib-contrast-cargo-cats-frontgateservice | 03c0a8d2-a6e6-46aa-b602-f242811d10bf | Java | Full rule set with CVE Shields, Virtual Patch |
| thib-contrast-cargo-cats-dataservice | 03f49f62-efd2-4f7b-9402-8f5f399b0d36 | Java | Full rule set with CVE Shields, Virtual Patch |
| thib-contrast-cargo-cats-imageservice | d112a86c-c664-4773-a323-40e327540b8a | .NET Core | Reduced rule set, semantic sub-rules |
| thib-contrast-cargo-cats-labelservice | 7bcc00dd-62cd-43d0-a9cc-1a4b874aa3c9 | Node | NoSQL Injection, Server-Side JS Injection |
| thib-contrast-cargo-cats-docservice | 84280ca7-b7e4-4501-bd33-90f313937df2 | Python | Minimal rule set |
| WebGoatProtect | 25ade401-024f-4b85-9601-508657991aa3 | Java | Command Injection in BLOCKING mode across all envs |
| Harshaa-MSSentinel-...-frontgateservice | 25d24972-d71b-41ec-a41b-22e13564a8b9 | Java | Full rule set with CVE Shields, Virtual Patch |

### Rule Types

**Protect Rules (Core Protection):**
- Path Traversal (uuid: path-traversal)
- SQL Injection (uuid: sql-injection)
- Cross-Site Scripting (uuid: reflected-xss)
- XML External Entity Injection (uuid: xxe)
- Command Injection (uuid: cmd-injection)
- Untrusted Deserialization (uuid: untrusted-deserialization)
- JNDI Injection (uuid: jndi-injection)
- NoSQL Injection (uuid: nosql-injection) - Node apps
- Server-Side JavaScript Injection (uuid: ssjs-injection) - Node apps

**CVE Shields:**
- Log4Shell (uuid: cve-2021-44228) - CVE-2021-44228, CVE-2021-45046
- Struts 2 File Upload RCE (uuid: cve-2017-5638)
- Apache Tomcat Arbitrary Code Execution (uuid: cve-2017-12617)
- Spring Expression Language Injection (uuid: cve-2011-2730)

**Virtual Patches:**
- Test VP (custom virtual patch)

---

## Basic Retrieval Tests

### Test 1: Get protect rules for Java application
**Purpose:** Verify retrieval of protect rules for a Java application with full rule set.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf
```

**Expected Result:**
- Success response with multiple rules
- Should include Protect Rules: Path Traversal, SQL Injection, Cross-Site Scripting, Command Injection, XXE, Untrusted Deserialization, JNDI Injection, etc.
- Should include CVE Shields: Log4Shell (cve-2021-44228), Struts vulnerabilities, Tomcat vulnerabilities
- Should include Virtual Patch: Test VP
- Each rule should have development, qa, production mode settings

---

### Test 2: Get protect rules for .NET Core application
**Purpose:** Verify retrieval of protect rules for a .NET Core application with reduced rule set.

**Prompt:**
```
use contrast mcp to get the protect rules for application d112a86c-c664-4773-a323-40e327540b8a
```

**Expected Result:**
- Success response with rules appropriate for .NET Core
- Should include: Path Traversal, SQL Injection, Cross-Site Scripting, XXE, Command Injection
- Should include semantic sub-rules: Path Traversal - File Security Bypass, Command Injection - Command Backdoors, Command Injection - Chained Commands, Command Injection - Dangerous Paths
- Should NOT include Java-specific CVE Shields (Struts, Log4j)

---

### Test 3: Get protect rules for Node application
**Purpose:** Verify retrieval of protect rules for a Node.js application with language-specific rules.

**Prompt:**
```
use contrast mcp to get the protect rules for application 7bcc00dd-62cd-43d0-a9cc-1a4b874aa3c9
```

**Expected Result:**
- Success response with Node-specific rules
- Should include Node-specific rules: NoSQL Injection (uuid: nosql-injection), Server-Side JavaScript Injection (uuid: ssjs-injection)
- Should NOT include Java-specific rules like OGNL Injection, Expression Language Injection, or Java CVE Shields

---

### Test 4: Get protect rules for Python application
**Purpose:** Verify retrieval of protect rules for a Python application.

**Prompt:**
```
use contrast mcp to get the protect rules for application 84280ca7-b7e4-4501-bd33-90f313937df2
```

**Expected Result:**
- Success response with Python-appropriate rules
- Should include core rules: Path Traversal, SQL Injection, Cross-Site Scripting, XXE, Command Injection, NoSQL Injection
- Reduced rule set compared to Java applications

---

### Test 5: Get protect rules by application name (using search first)
**Purpose:** Verify workflow of finding app by name then getting protect rules.

**Prompt:**
```
use contrast mcp to find the application "thib-contrast-cargo-cats-frontgateservice" and get its protect rules
```

**Expected Result:**
- AI should first use search_applications to find the app ID
- Then use get_protect_rules with the found ID
- Returns full protect rules configuration

---

## Rule Mode Tests

### Test 6: Verify rules with BLOCKING mode
**Purpose:** Verify rules configured in BLOCKING mode are correctly identified.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and identify which rules are set to BLOCKING mode
```

**Expected Result:**
- Command Injection should have `production: "BLOCKING"`, `qa: "BLOCKING_PERIMETER"`
- Path Traversal should have `development: "BLOCKING"`
- Other rules may be in MONITORING mode

---

### Test 7: Verify rules with MONITORING mode
**Purpose:** Verify rules configured in MONITORING mode are correctly identified.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and identify which rules are set to MONITORING mode in production
```

**Expected Result:**
- Most rules should be in MONITORING mode for production
- SQL Injection, Cross-Site Scripting, XXE, Path Traversal (production) should show `production: "MONITORING"`

---

### Test 8: Verify rules with OFF mode
**Purpose:** Verify rules that are disabled (OFF mode) are correctly identified.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and identify which rules are turned OFF
```

**Expected Result:**
- Zip File Overwrite: `development: "OFF"`, `qa: "OFF"`, `production: "OFF"`
- Unsafe File Upload: `development: "OFF"`, `qa: "OFF"`, `production: "OFF"`
- Command Injection - Process Hardening: `development: "OFF"`, `qa: "OFF"`, `production: "OFF"`

---

### Test 9: Verify rules with BLOCKING_PERIMETER mode
**Purpose:** Verify rules configured in BLOCKING_PERIMETER mode.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and show me rules using BLOCKING_PERIMETER mode
```

**Expected Result:**
- Command Injection should have `qa: "BLOCKING_PERIMETER"`
- This mode blocks at the perimeter before the request reaches the application

---

### Test 10: Application with all BLOCKING mode for Command Injection
**Purpose:** Verify an application with aggressive blocking configuration.

**Prompt:**
```
use contrast mcp to get the protect rules for application 25ade401-024f-4b85-9601-508657991aa3 and check the Command Injection rule mode
```

**Expected Result:**
- Command Injection should show: `development: "BLOCKING"`, `qa: "BLOCKING"`, `production: "BLOCKING"`
- This is the WebGoatProtect application with aggressive blocking enabled

---

## Rule Type Tests

### Test 11: Identify Protect Rules (core protection)
**Purpose:** Verify identification of standard Protect Rules.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and list only the core Protect Rules (not CVE Shields or Virtual Patches)
```

**Expected Result:**
- Rules with `type: "Protect Rule"` should be listed
- Examples: Path Traversal, SQL Injection, Cross-Site Scripting, XXE, Untrusted Deserialization, Command Injection, Expression Language Injection, HTTP Method Tampering, OGNL Injection, Zip File Overwrite, Unsafe File Upload, Class Loader Manipulation, Signature Tampering, JNDI Injection, Command Injection - Process Hardening

---

### Test 12: Identify CVE Shield rules
**Purpose:** Verify identification of CVE Shield rules with CVE information.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and list only the CVE Shield rules
```

**Expected Result:**
- Rules with `type: "CVE Shield"` should be listed
- Should include CVE information: name, score, description
- Examples:
  - Log4j2 Remote Code Execution (Log4Shell) - CVE-2021-44228 (score: 10.0)
  - Struts 2 File Upload RCE - CVE-2017-5638 (score: 10.0)
  - Struts 2 XSLTResult RCE - CVE-2016-3082 (score: 10.0)
  - Apache Tomcat Arbitrary Code Execution - CVE-2017-12617 (score: 6.8)

---

### Test 13: Identify Virtual Patch rules
**Purpose:** Verify identification of Virtual Patch rules.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and list any Virtual Patches
```

**Expected Result:**
- Rules with `type: "Virtual Patch"` should be listed
- Test VP should be present
- Virtual Patches have `enabledDev`, `enabledQa`, `enabledProd` fields instead of mode strings

---

### Test 14: Verify CVE details in CVE Shield rules
**Purpose:** Verify CVE information is correctly populated in CVE Shield rules.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and show me the CVE details for the Log4Shell protection rule
```

**Expected Result:**
- Log4j2 Remote Code Execution Vulnerability (Log4Shell)
- CVEs should include:
  - CVE-2021-44228 (score: 10.0, accessVector: Network)
  - CVE-2021-45046 (score: 2.6, accessVector: Network)
- Description should mention Log4j2 and JNDI/LDAP lookups

---

## Parent-Child Rule Relationship Tests

### Test 15: Verify parent rule relationships
**Purpose:** Verify sub-rules have correct parent rule references.

**Prompt:**
```
use contrast mcp to get the protect rules for application d112a86c-c664-4773-a323-40e327540b8a and show me the Command Injection related rules and their relationships
```

**Expected Result:**
- Command Injection (parent rule): `parentRuleUuid: "cmd-injection"`, `parentRuleName: "Command Injection"`
- Command Injection - Command Backdoors: `parentRuleUuid: "cmd-injection"`, `parentRuleName: "Command Injection"`
- Command Injection - Chained Commands: `parentRuleUuid: "cmd-injection"`, `parentRuleName: "Command Injection"`
- Command Injection - Dangerous Paths: `parentRuleUuid: "cmd-injection"`, `parentRuleName: "Command Injection"`
- Command Injection - Process Hardening: `parentRuleUuid: "cmd-injection"`, `parentRuleName: "Command Injection"`

---

### Test 16: Verify Path Traversal sub-rules
**Purpose:** Verify Path Traversal sub-rules and their relationships.

**Prompt:**
```
use contrast mcp to get the protect rules for application d112a86c-c664-4773-a323-40e327540b8a and show me the Path Traversal related rules
```

**Expected Result:**
- Path Traversal: `uuid: "path-traversal"`
- Path Traversal - File Security Bypass: `parentRuleUuid: "path-traversal"`, `parentRuleName: "Path Traversal"`, `uuid: "path-traversal-semantic-file-security-bypass"`

---

## Blocking Capability Tests

### Test 17: Verify canBlock capability
**Purpose:** Verify rules with canBlock capability are correctly identified.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and show me which rules can be configured to block attacks
```

**Expected Result:**
- Most rules should have `canBlock: true`
- All Protect Rules and CVE Shields should be blockable

---

### Test 18: Verify canBlockAtPerimeter capability
**Purpose:** Verify rules that can block at perimeter are correctly identified.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and identify which rules can block attacks at the perimeter
```

**Expected Result:**
- Rules with `canBlockAtPerimeter: true`:
  - Path Traversal
  - SQL Injection
  - Cross-Site Scripting
  - Command Injection
  - HTTP Method Tampering
  - Unsafe File Upload
- Rules with `canBlockAtPerimeter: false`:
  - XXE
  - Untrusted Deserialization
  - Expression Language Injection
  - OGNL Injection
  - Most CVE Shields

---

### Test 19: Verify isMonitorAtPerimeter setting
**Purpose:** Verify rules configured to monitor at perimeter.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and show me which rules are set to monitor at perimeter
```

**Expected Result:**
- Unsafe File Upload: `isMonitorAtPerimeter: true`
- Most other rules: `isMonitorAtPerimeter: false`

---

## Error Handling Tests

### Test 20: Invalid application ID
**Purpose:** Verify error handling for invalid application ID.

**Prompt:**
```
use contrast mcp to get the protect rules for application invalid-app-id-12345
```

**Expected Result:**
- Error response with `success: false`
- Error message: "Authentication failed. Check API credentials." or similar
- `found: false`

---

### Test 21: Empty application ID
**Purpose:** Verify error handling for missing/empty application ID.

**Prompt:**
```
use contrast mcp to get the protect rules without specifying an application
```

**Expected Result:**
- Error or request for appId parameter
- Tool should require appId to be specified

---

## Language-Specific Rule Tests

### Test 22: Java-specific rules
**Purpose:** Verify Java applications include Java-specific rules.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and identify Java-specific rules
```

**Expected Result:**
- Java-specific Protect Rules:
  - Expression Language Injection
  - OGNL Injection
  - Class Loader Manipulation
  - Signature Tampering
  - JNDI Injection
- Java-specific CVE Shields:
  - Log4Shell (Log4j)
  - All Struts vulnerabilities
  - Apache Tomcat vulnerabilities
  - Spring vulnerabilities

---

### Test 23: Node.js-specific rules
**Purpose:** Verify Node.js applications include Node-specific rules.

**Prompt:**
```
use contrast mcp to get the protect rules for application 7bcc00dd-62cd-43d0-a9cc-1a4b874aa3c9 and identify Node.js-specific rules
```

**Expected Result:**
- Node-specific rules should be present:
  - NoSQL Injection (uuid: nosql-injection)
  - Server-Side JavaScript Injection (uuid: ssjs-injection)
- Java-specific rules should NOT be present:
  - No Expression Language Injection
  - No OGNL Injection
  - No Struts CVE Shields
  - No Log4j CVE Shields

---

### Test 24: Compare rules across different languages
**Purpose:** Verify rule sets vary appropriately by application language.

**Prompt:**
```
use contrast mcp to get the protect rules for the Java app 03c0a8d2-a6e6-46aa-b602-f242811d10bf and the Node app 7bcc00dd-62cd-43d0-a9cc-1a4b874aa3c9 and compare the rule sets
```

**Expected Result:**
- Java app should have ~32+ rules including CVE Shields and Virtual Patches
- Node app should have ~14 rules (no CVE Shields, language-specific rules)
- Common rules: Path Traversal, SQL Injection, Cross-Site Scripting, XXE, Command Injection
- Java-only: OGNL Injection, Expression Language Injection, CVE Shields
- Node-only: NoSQL Injection, Server-Side JavaScript Injection

---

## Environment Mode Comparison Tests

### Test 25: Compare modes across environments
**Purpose:** Verify rules can have different modes in different environments.

**Prompt:**
```
use contrast mcp to get the protect rules for application d112a86c-c664-4773-a323-40e327540b8a and find rules where the mode differs between development, QA, and production
```

**Expected Result:**
- Path Traversal: `development: "BLOCKING"`, `qa: "MONITORING"`, `production: "MONITORING"`
- Command Injection: `development: "MONITORING"`, `qa: "BLOCKING_PERIMETER"`, `production: "BLOCKING"`
- Command Injection - Chained Commands: `development: "OFF"`, `qa: "MONITORING"`, `production: "MONITORING"`

---

### Test 26: Find rules with consistent modes across environments
**Purpose:** Verify rules with same mode in all environments.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and find rules where the mode is the same across all environments
```

**Expected Result:**
- Rules with consistent MONITORING mode: SQL Injection, Cross-Site Scripting, XXE, Untrusted Deserialization, most CVE Shields
- Rules with consistent OFF mode: Zip File Overwrite, Unsafe File Upload, Command Injection - Process Hardening

---

## CVE Score Tests

### Test 27: Find high-severity CVE Shields
**Purpose:** Verify CVE Shields with high CVSS scores can be identified.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and find CVE Shield rules with CVSS score of 9.0 or higher
```

**Expected Result:**
- Log4j2 RCE (Log4Shell): CVE-2021-44228 (score: 10.0)
- Struts 2 File Upload RCE: CVE-2017-5638 (score: 10.0)
- Struts 2 XSLTResult RCE: CVE-2016-3082 (score: 10.0)
- Struts 2 Action Prefix OGNL RCE: CVE-2013-2251 (score: 9.3)
- Struts 2 Dynamic Method Invocation RCE: CVE-2016-3081 (score: 9.3)

---

### Test 28: Find CVE Shields with multiple CVEs
**Purpose:** Verify CVE Shields that protect against multiple CVEs.

**Prompt:**
```
use contrast mcp to get the protect rules for application 03c0a8d2-a6e6-46aa-b602-f242811d10bf and find CVE Shield rules that protect against multiple CVEs
```

**Expected Result:**
- Log4j2 RCE (Log4Shell): CVE-2021-44228, CVE-2021-45046
- Struts 2 REST Plugin RCE: CVE-2016-4438, CVE-2016-3087

---

## Semantic Sub-Rule Tests

### Test 29: Identify semantic sub-rules
**Purpose:** Verify semantic sub-rules are correctly identified with their parent relationships.

**Prompt:**
```
use contrast mcp to get the protect rules for application d112a86c-c664-4773-a323-40e327540b8a and list all semantic sub-rules
```

**Expected Result:**
- Path Traversal - File Security Bypass (uuid: path-traversal-semantic-file-security-bypass)
- Command Injection - Command Backdoors (uuid: cmd-injection-command-backdoors)
- Command Injection - Chained Commands (uuid: cmd-injection-semantic-chained-commands)
- Command Injection - Dangerous Paths (uuid: cmd-injection-semantic-dangerous-paths)

---

### Test 30: Verify sub-rule modes can differ from parent
**Purpose:** Verify sub-rules can have different modes than their parent rule.

**Prompt:**
```
use contrast mcp to get the protect rules for application d112a86c-c664-4773-a323-40e327540b8a and compare Command Injection rule modes with its sub-rule modes
```

**Expected Result:**
- Command Injection (parent): `development: "MONITORING"`, `qa: "BLOCKING_PERIMETER"`, `production: "BLOCKING"`
- Command Injection - Chained Commands (sub): `development: "OFF"`, `qa: "MONITORING"`, `production: "MONITORING"`
- Sub-rules can be configured independently from parent rules

---

## Summary

| Test # | Category | Test Description | Key Verification |
|--------|----------|------------------|------------------|
| 1 | Basic | Java app protect rules | Full rule set with CVE Shields |
| 2 | Basic | .NET Core app protect rules | Reduced rule set, semantic sub-rules |
| 3 | Basic | Node app protect rules | Language-specific rules (NoSQL, SSJS) |
| 4 | Basic | Python app protect rules | Minimal Python rule set |
| 5 | Basic | Find app then get rules | search_applications + get_protect_rules workflow |
| 6 | Mode | Rules in BLOCKING mode | Command Injection BLOCKING |
| 7 | Mode | Rules in MONITORING mode | SQL Injection, XSS MONITORING |
| 8 | Mode | Rules in OFF mode | Zip File Overwrite, Unsafe File Upload OFF |
| 9 | Mode | Rules in BLOCKING_PERIMETER mode | Command Injection qa BLOCKING_PERIMETER |
| 10 | Mode | All BLOCKING configuration | WebGoatProtect cmd-injection all BLOCKING |
| 11 | Type | Core Protect Rules | Type: "Protect Rule" |
| 12 | Type | CVE Shield rules | Type: "CVE Shield" with CVE info |
| 13 | Type | Virtual Patch rules | Type: "Virtual Patch" |
| 14 | CVE | CVE details for Log4Shell | CVE-2021-44228, CVE-2021-45046 |
| 15 | Parent-Child | Command Injection sub-rules | parentRuleUuid relationships |
| 16 | Parent-Child | Path Traversal sub-rules | path-traversal-semantic-file-security-bypass |
| 17 | Capability | canBlock capability | Most rules canBlock: true |
| 18 | Capability | canBlockAtPerimeter | SQL Injection, XSS can block at perimeter |
| 19 | Capability | isMonitorAtPerimeter | Unsafe File Upload monitors at perimeter |
| 20 | Error | Invalid app ID | Authentication failed error |
| 21 | Error | Empty app ID | Error/validation message |
| 22 | Language | Java-specific rules | OGNL, EL Injection, Struts CVEs |
| 23 | Language | Node-specific rules | NoSQL Injection, SSJS Injection |
| 24 | Language | Cross-language comparison | Rule set varies by language |
| 25 | Environment | Different modes per environment | Path Traversal BLOCKING in dev only |
| 26 | Environment | Consistent modes | SQL Injection MONITORING all envs |
| 27 | CVE | High-severity CVE Shields | Log4Shell, Struts CVSS >= 9.0 |
| 28 | CVE | Multi-CVE protections | Log4Shell: 2 CVEs, REST Plugin: 2 CVEs |
| 29 | Sub-Rules | Semantic sub-rules | cmd-injection-command-backdoors etc |
| 30 | Sub-Rules | Sub-rule mode independence | Sub-rules can differ from parent mode |

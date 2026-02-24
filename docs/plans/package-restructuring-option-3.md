# Package Restructuring Plan: Domain-Driven with Bounded Contexts

**Status**: Alternative Proposal
**Created**: January 2025
**Approach**: Full domain-driven design with bounded contexts and per-domain clients

## Executive Summary

This proposal reorganizes the codebase using **Domain-Driven Design (DDD)** principles with bounded contexts. Each domain (assess, protect, sca, coverage, sast) becomes a self-contained module with its own models, client, and tools.

**Key difference from Option 1/2**: This approach creates per-domain API clients instead of a shared SDK extension, and organizes ALL code for a domain together (models + client + tools).

**Estimated effort**: ~2-3 hours of mechanical refactoring

---

## Design Philosophy

### Bounded Contexts

Each domain package is a bounded context with:
- **Models** - Domain entities, only this domain defines them
- **Client** - Single class owning all API calls for domain
- **Tools** - MCP tools exposing domain capabilities
- **Internal services** - Mappers, hints (only where needed)

### Dependency Rules

```
┌─────────────────────────────────────────────────────────────┐
│                          core/                               │
│   (config, sdk, tool bases, validation, util)               │
└─────────────────────────────────────────────────────────────┘
                              ▲
         ┌────────────────────┼────────────────────┐
         │                    │                    │
    ┌────┴────┐         ┌─────┴─────┐        ┌────┴────┐
    │ assess/ │         │ protect/  │        │  sca/   │
    └────┬────┘         └─────┬─────┘        └────┬────┘
         │                    │                   │
         │              ┌─────┴─────┐             │
         └──────────────┤application├─────────────┘
                        └───────────┘
```

- **Domains depend on `core/`** - never the reverse
- **Domains may depend on `application/`** - apps are foundational
- **Domains do NOT depend on each other** - assess doesn't import protect

---

## Proposed Structure

Root package: `com.contrastsecurity.mcp`

```
com.contrastsecurity.mcp/
│
├── McpContrastApplication.java
│
├── core/                                    # ══════════════════════════════════════
│   │                                        # Shared infrastructure - NO business logic
│   │
│   ├── config/
│   │   └── ContrastConfig.java              # SDK/credentials configuration
│   │
│   ├── sdk/
│   │   └── ContrastSdkClient.java           # Thin wrapper - HTTP, error handling, JSON
│   │
│   ├── tool/
│   │   ├── BaseContrastTool.java            # Abstract base - SDK access, error mapping
│   │   ├── BasePaginatedTool.java           # Template: validate → execute → paginate
│   │   ├── BaseSingleTool.java              # Template: validate → execute → respond
│   │   ├── ToolParams.java                  # Marker interface for all params
│   │   ├── PaginatedResponse.java           # Standard paginated response envelope
│   │   ├── SingleResponse.java              # Standard single-item response envelope
│   │   └── ExecutionResult.java             # Result wrapper with success/warnings
│   │
│   ├── validation/
│   │   ├── ValidationContext.java           # Fluent validation builder
│   │   ├── ValidationConstants.java         # Shared limits, patterns
│   │   ├── DateSpec.java                    # Date/timestamp validation
│   │   ├── IntSpec.java                     # Integer range validation
│   │   ├── StringSpec.java                  # String format validation
│   │   ├── StringListSpec.java              # CSV list validation
│   │   └── EnumSetSpec.java                 # Enum allowlist validation
│   │
│   ├── util/
│   │   └── PaginationHandler.java           # Page token/offset logic
│   │
│   └── prompt/
│       ├── PromptRegistration.java          # MCP prompt registration
│       └── PromptService.java               # Prompt execution
│
│
├── application/                             # ══════════════════════════════════════
│   │                                        # DOMAIN: Application management
│   │                                        # Owner of: apps, metadata, sessions
│   │
│   ├── model/
│   │   ├── Application.java                 # Core app entity
│   │   ├── ApplicationSummary.java          # Lightweight app reference
│   │   ├── Metadata.java                    # App-level metadata container
│   │   ├── MetadataField.java               # Single metadata key-value
│   │   └── AgentSession.java                # Test session with metadata
│   │
│   ├── ApplicationClient.java               # API: GET /applications, /agent-sessions
│   │
│   └── tool/
│       ├── SearchApplicationsTool.java
│       ├── SearchApplicationsParams.java
│       ├── GetSessionMetadataTool.java
│       └── GetSessionMetadataParams.java
│
│
├── assess/                                  # ══════════════════════════════════════
│   │                                        # DOMAIN: IAST vulnerability detection
│   │                                        # Owner of: traces, vulns, remediation hints
│   │
│   ├── model/
│   │   ├── Vulnerability.java               # Full vulnerability with story/events
│   │   ├── VulnerabilityLight.java          # Summary for list views
│   │   ├── VulnerabilityStack.java          # Stack trace with frames
│   │   ├── StackFrame.java                  # Single frame: class, method, line
│   │   ├── TriggerEvent.java                # Event that triggered the vuln
│   │   ├── DataFlowEvent.java               # Propagation through code
│   │   └── HttpRequest.java                 # Request that triggered detection
│   │
│   ├── VulnerabilityClient.java             # API: POST /traces, GET /traces/{id}
│   │
│   ├── tool/
│   │   ├── SearchVulnerabilitiesTool.java   # Org-wide vuln search
│   │   ├── SearchVulnerabilitiesParams.java
│   │   ├── SearchAppVulnerabilitiesTool.java # App-scoped with session filtering
│   │   ├── SearchAppVulnerabilitiesParams.java
│   │   ├── GetVulnerabilityTool.java        # Single vuln with full story
│   │   ├── GetVulnerabilityParams.java
│   │   ├── ListVulnerabilityTypesTool.java  # Available rule names
│   │   └── ListVulnerabilityTypesParams.java
│   │
│   ├── mapper/
│   │   ├── VulnerabilityMapper.java         # SDK Trace → domain Vulnerability
│   │   └── VulnerabilityContext.java        # Mapping context/state
│   │
│   └── hint/
│       ├── HintGenerator.java               # Generates AI remediation hints
│       ├── HintProvider.java                # Interface for hint sources
│       ├── HintUtils.java                   # Hint formatting utilities
│       └── RuleHints.java                   # Rule-specific hint content
│
│
├── protect/                                 # ══════════════════════════════════════
│   │                                        # DOMAIN: Runtime attack detection (ADR)
│   │                                        # Owner of: attacks, events, protect rules
│   │
│   ├── model/
│   │   ├── Attack.java                      # Attack aggregate with events
│   │   ├── AttackSummary.java               # Summary for list views
│   │   ├── AttackEvent.java                 # Single attack event
│   │   ├── EventDetails.java                # Event specifics: input, stack
│   │   ├── UserInput.java                   # Malicious input detected
│   │   ├── ProtectRule.java                 # Protection rule config
│   │   └── RuleMode.java                    # BLOCK, MONITOR, OFF
│   │
│   ├── AttackClient.java                    # API: POST /attacks, GET /protection/policy
│   │
│   └── tool/
│       ├── SearchAttacksTool.java           # Attack search with filters
│       ├── SearchAttacksParams.java
│       ├── GetProtectRulesTool.java         # App's protect configuration
│       └── GetProtectRulesParams.java
│
│
├── sca/                                     # ══════════════════════════════════════
│   │                                        # DOMAIN: Software Composition Analysis
│   │                                        # Owner of: libraries, CVEs, usage
│   │
│   ├── model/
│   │   ├── Library.java                     # Library with version, hash
│   │   ├── LibrarySummary.java              # Lightweight for lists
│   │   ├── LibraryVulnerability.java        # Vuln affecting a library
│   │   ├── Cve.java                         # CVE details
│   │   ├── CvssV3.java                      # CVSS scoring
│   │   ├── LibraryObservation.java          # Class usage observation
│   │   └── ImpactStats.java                 # Usage statistics
│   │
│   ├── LibraryClient.java                   # API: GET /libraries, /cves
│   │
│   └── tool/
│       ├── ListApplicationLibrariesTool.java  # Libraries for an app
│       ├── ListApplicationLibrariesParams.java
│       ├── ListApplicationsByCveTool.java   # Apps affected by CVE
│       └── ListApplicationsByCveParams.java
│
│
├── coverage/                                # ══════════════════════════════════════
│   │                                        # DOMAIN: Route/endpoint coverage
│   │                                        # Owner of: routes, observations
│   │
│   ├── model/
│   │   ├── Route.java                       # HTTP route: verb + path
│   │   ├── RouteObservation.java            # When/how route was hit
│   │   ├── RouteCoverageStatus.java         # DISCOVERED, EXERCISED
│   │   └── RouteServer.java                 # Server where observed
│   │
│   ├── RouteClient.java                     # API: GET /route, POST /route/coverage
│   │
│   └── tool/
│       ├── GetRouteCoverageTool.java        # Route coverage for app
│       └── GetRouteCoverageParams.java
│
│
└── sast/                                    # ══════════════════════════════════════
    │                                        # DOMAIN: Static Application Security Testing
    │                                        # Owner of: scan projects, results
    │
    ├── model/
    │   ├── ScanProject.java                 # Scan project with metadata
    │   ├── ScanResult.java                  # Individual finding
    │   └── ScanSeverity.java                # CRITICAL, HIGH, MEDIUM, LOW
    │
    ├── SastClient.java                      # API: GET /scan/projects
    │
    └── tool/
        ├── GetSastProjectTool.java          # Project details
        ├── GetSastProjectParams.java
        ├── GetSastResultsTool.java          # SARIF results
        └── GetSastResultsParams.java
```

---

## Design Principles

### 1. Package-Private by Default

Within each domain:
```java
// application/model/Application.java
public class Application { ... }           // Public - used by other domains

// application/ApplicationClient.java
class ApplicationClient { ... }            // Package-private - internal

// application/tool/SearchApplicationsTool.java
@Service
public class SearchApplicationsTool { ... } // Public - Spring needs to find it
```

### 2. Model Ownership

Each model class lives in exactly ONE domain:

| Model | Owner Domain | Reasoning |
|-------|--------------|-----------|
| `Application` | `application/` | Core entity, others reference by ID |
| `Vulnerability` | `assess/` | IAST-specific concept |
| `Attack` | `protect/` | ADR-specific concept |
| `Library` | `sca/` | SCA-specific concept |
| `Route` | `coverage/` | Coverage-specific concept |
| `Cve` | `sca/` | CVE is a library vulnerability concept |

### 3. Client Responsibility

One `*Client.java` per domain handles ALL API calls:
```java
// assess/VulnerabilityClient.java
@Component
class VulnerabilityClient {

    Traces searchTraces(String orgId, TraceFilterBody filters) { ... }

    Trace getTrace(String orgId, String appId, String traceId) { ... }

    List<String> getVulnerabilityTypes(String orgId) { ... }
}
```

**Key benefit**: Replaces the 400-line `SDKExtension` god class with 6 focused clients (~60-80 lines each).

---

## Comparison with Other Options

| Aspect | Option 1 (Naming) | Option 2 (Clean Arch) | Option 3 (DDD) |
|--------|-------------------|----------------------|----------------|
| **Find "vulnerability" code** | Check `api/`, `model/`, `tool/` | Check `domain/`, `sdk/`, `tool/` | Look in `assess/` |
| **Add new domain** | Touch 3+ packages | Touch 4+ packages | Create 1 new package |
| **Cross-domain change** | Easy (layers together) | Medium | Harder (must coordinate) |
| **Team scaling** | Harder to split ownership | Harder to split ownership | Natural domain ownership |
| **Code duplication** | Less (shared patterns) | Less (shared patterns) | More (similar structure repeated) |
| **Navigate unfamiliar code** | Need to understand layers | Need to understand layers | Find domain, explore within |
| **SDK god class** | Renamed, still monolithic | Split into response mappers | Split into domain clients |

---

## Migration Path

1. **Create `core/`** - Move base classes, validation, config
2. **Create domain packages** - One at a time, starting with `application/`
3. **Move tools with their params** - Each tool brings its param class
4. **Split SDKExtension** - Extract methods to domain clients
5. **Consolidate models** - Move from `data/` and `sdkextension/data/` to domain models
6. **Delete orphans** - Remove now-empty packages

---

## Pros and Cons

### Pros
- **Domain encapsulation** - All code for a domain is together
- **Natural team boundaries** - Easy to assign domain ownership
- **Reduced coupling** - Domains are independent
- **Easier onboarding** - Find domain, explore within
- **Eliminates god class** - SDKExtension split into focused clients

### Cons
- **Code duplication** - Similar patterns repeated in each domain
- **More packages** - 6 domain packages vs current structure
- **Cross-domain changes harder** - Must touch multiple packages
- **Higher initial effort** - More significant restructure than Option 1

---

## When to Choose This Option

Choose Option 3 (DDD) when:
- Team is growing and needs clear ownership boundaries
- Domains are evolving independently
- You want to eventually extract domains into separate modules/services
- The SDKExtension god class is causing maintenance issues

Choose Option 1 (Naming) when:
- Team is small and everyone works across domains
- Quick wins with minimal risk is the priority
- The existing architecture works well enough

---

## Summary

This proposal provides the most comprehensive restructure with clear domain boundaries. However, it requires more effort than Option 1 and introduces more code duplication.

**Recommendation**: Start with Option 1 (naming improvements), then consider Option 3 as a follow-up if domain isolation becomes more valuable.

---

## References

- Domain-Driven Design by Eric Evans
- Clean Architecture by Robert C. Martin
- Bounded Contexts pattern
- SOLID Principles

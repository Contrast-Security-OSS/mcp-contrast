# PMD static analysis with baselines

PMD and CPD (copy-paste detection) are added to the Gradle check lifecycle, gated through a per-module baseline file rather than failing on every finding. This is the same pattern used for CRAP scores and the ArchUnit freeze store: new violations fail the build, existing violations are tracked in a committed file that shrinks over time.

The rules are ported from aiml-services, evaluated for applicability to this repo. Three custom XPath rules (AvoidInlineLoggingKeyLiteral, FluentLogInCatchRequiresCause, PreferLombokNoArgsConstructor) are kept. AvoidRawMockitoAny was removed after audit: PMD XPath lacks type resolution, so the rule matched any zero-arg `any()` call, not just Mockito's, and checkstyle already enforces the typed-mock convention. Three rules specific to aiml-services conventions (UseJakartaNullable, UseDecimalValidationForFloatingPoint, CredentialFieldRequiresJsonIgnore) are dropped because this repo uses Spring's Nullable, has no bean-validation annotations, and has no credential DTOs with JSON serialization.

## Considered options

- **Fail on every finding from day one.** Rejected: the codebase has 90+ existing violations across two modules. Fixing them all upfront would mix cleanup with the gate itself, and fixing the wrong one could introduce regressions. The baseline records the debt and prevents new additions.
- **Single shared baseline file.** Rejected: with two subprojects, a `writePmdBaseline` that rewrites one file from one module's reports would clobber the other module's entries. Per-module baselines (`contrast-mcp-core/pmd-baseline.txt`, `contrast-mcp-stdio-app/pmd-baseline.txt`) follow the per-module `crap4j-baseline.json` precedent.
- **Skip custom XPath rules.** Rejected: two of the three rules enforce conventions already documented in CLAUDE.md (LoggingKeys constants, fluent log setCause in catch blocks). Catching violations at analysis time is faster than catching them in review.
- **Wire changed-file PMD into check.** Rejected: same reason as changed-file coverage. `check` has no base ref to diff against, so a working-tree run would pass vacuously. Changed-file PMD runs from the pre-push hook and PR CI, same as coverage.

## Consequences

- `make check` adds PMD, CPD, baseline verification, and positive-control verification to the full Gradle check lifecycle. Wall-clock increase is roughly 3-5 seconds on a warm build.
- Per-module baselines are committed files. Use `./gradlew :<module>:writePmdBaseline` to regenerate (user-sanctioned, same category as CRAP baselines). Use `./gradlew :<module>:pmdBaselineTighten` to remove slack after fixing violations.
- The pre-push hook runs `verifyPmdChangedFilesBaseline` alongside `jacocoChangedFileCoverageVerification`, sharing the same `staticAnalysisChangedBase` property.
- PR CI runs `verifyPmdChangedFilesBaseline` on every pull request, using the PR base SHA.
- PMD and CPD HTML reports are uploaded as build artifacts alongside JaCoCo and PIT reports.
- PMD version is pinned in `gradle.properties` under the ENTSEC-1742 soak policy.

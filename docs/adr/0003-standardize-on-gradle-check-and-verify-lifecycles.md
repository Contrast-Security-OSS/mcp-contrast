# Standardize verification on the Gradle lifecycle tasks check and verify

The Makefile, PR CI, and the release workflow each enumerated Gradle task names by hand, so every new verification had to be added to every invoker separately. The crap4j gate (AIML-1227) proved the failure mode: `crapCheck` was registered but no gate anywhere ran it, and the invokers had already drifted from each other (pitest ran on PRs but not releases, `verifyCoverageMinimums` reached CI only via a transitive dependency). We decided that Gradle lifecycle tasks are the single source of truth and invokers call them by lifecycle name:

- `check` = every verification provable from the repository alone: Spotless, Checkstyle, unit tests (including ArchUnit), JaCoCo floors plus `verifyCoverageMinimums`, `crapCheck` (via `crap4j { attachToCheck = true }`), and PIT.
- `verify` = a custom root lifecycle task, borrowing Maven's name: `check` plus `:contrast-mcp-stdio-app:integrationTest`. The only delta is the credential-gated live-API contract (see ADR 0004).

Make targets become thin quiet-output wrappers that forward to one lifecycle task each; the composite targets (`check-test`, `test-coverage`) that defined their own version of the gate are deleted. `make lint` survives as a deliberately named fast subset (format + checkstyle, ~4s) so the inner loop keeps a quick path without overloading the word `check`.

## Considered options

- **Keep enumerated task lists** in make and CI. Rejected: enumeration is the mechanism by which `crapCheck` ran nowhere, and it will recur for every future gate.
- **Keep PIT out of `check`** as a slow-path extra. Rejected after measurement: a full PIT run is 38s locally and cached by Gradle up-to-date checks when core is unchanged. Carving it out would recreate the enumeration disease for one task.
- **Keep `make check` as the fast no-test loop.** Rejected: a make target named `check` that does less than `gradlew check` is exactly the ambiguity this decision removes. The fast path gets its own name (`lint`).

## Consequences

- `make check` grows from a 5s lint pass to the full gate (~5s clean, ~60s when core changed, PIT dominating).
- Merge-to-main and release CI gain PIT (~3 minutes on runner hardware); both previously shipped without mutation testing.
- The pre-push hook runs `gradlew check` before the changed-file coverage check. Up-to-date checking makes it ~6s when `make check` already ran, and the full cost lands exactly on pushes that skipped it. One human-only bypass, `SKIP_PUSH_GATE=1`, is available for emergencies.
- The deliberate exceptions stay outside `check` and remain enumerated: `jacocoChangedFileCoverageVerification` (needs a base ref), buildSrc checks (separate Gradle build), integration tests (see ADR 0004), and the pre-commit hook's fix-first subset (`spotlessApply` fixes, `check` only rejects).

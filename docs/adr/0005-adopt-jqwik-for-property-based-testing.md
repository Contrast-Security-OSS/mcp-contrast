# Adopt jqwik for property-based testing

The existing example-based tests verify chosen inputs thoroughly, but they can't cover the full input space of validation and pagination code that accepts arbitrary integers, strings, and enums. Property-based testing generates inputs automatically and checks invariants across hundreds of combinations, catching edge cases that hand-picked examples miss.

We decided:

- **Library: jqwik 1.10.1.** A spike verified it runs on this repo's JUnit Platform 6.0.3 stack. jqwik's own platform dependencies (1.14.4) get force-upgraded on the classpath and the engine still discovers and runs properties. This is binary compatibility, not an officially supported pairing. jqwik's future releases are uncertain (release notes say upcoming Platform 6 releases will happen "if ever realised"). Accepted risk because there is no mature Platform-6-native alternative.
- **Location: ordinary test source set.** Property tests live in `contrast-mcp-core/src/test/java`, named `*PropertyTest`. The Gradle `check` lifecycle inherits them automatically (ADR 0003). They feed JaCoCo floors, the CRAP gate, and PIT test strength.
- **Tries budget: 100 per property by default** (`jqwik.tries.default=100` in `junit-platform.properties`). Keeps PIT runtime manageable since PIT reruns tests per mutant.
- **Pilot scope: `tool/validation` and `tool/base` pagination.** Covers `PaginationParams`, `CursorPaginationParams`, `IntSpec`, `StringListSpec`, `EnumSetSpec`, `ToolSortParser`, and `ToolValidationContext`.
- **Bug policy:** a failing property never merges. Fix the bug when small. Otherwise narrow the generator to documented bounds, add a WHY comment citing the bug, and file a bead for the fix.

## Considered options

- **QuickCheck-style libraries (junit-quickcheck, quicktheories).** junit-quickcheck is unmaintained and does not support JUnit Platform 5+. quicktheories has limited shrinking and no JUnit Platform integration.
- **Kotest property testing.** Requires Kotlin. This is a Java project.
- **No property-based testing.** Status quo. Leaves the input-space coverage gap unaddressed. Rejected.

## Maintenance risk

jqwik is a single-maintainer project whose Platform 6 support is uncertain. If jqwik becomes unusable on a future JUnit Platform upgrade, the fallback is to convert property tests to parameterized JUnit tests with representative edge cases. The property tests are self-contained (no shared state, no custom lifecycle), so conversion is mechanical.

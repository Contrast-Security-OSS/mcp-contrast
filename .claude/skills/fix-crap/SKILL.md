---
name: fix-crap
description: Fix methods that fail the CRAP metric gate. Use when crapCheck fails, when a method appears in the crap4j baseline, when the user asks to reduce a method's CRAP score, or when make check fails with a CRAP violation. Covers diagnosis, the test-or-simplify decision, and baseline management.
---

# Fixing CRAP Violations

CRAP (Change Risk Anti-Patterns) flags methods that are both complex and under-tested. The formula is `cc^2 * (1 - coverage)^3 + cc` where `cc` is cyclomatic complexity and `coverage` is branch coverage (0 to 1). A method fails at CRAP > 15 with complexity capped at 15.

## When this skill applies

- `crapCheck` fails during `make check`
- A method appears in a module's `crap4j-baseline.json`
- The user asks to reduce a CRAP score or clear the baseline

## Diagnosis

Run the advisory report first to see all flagged methods and their scores:

```
./gradlew :<module>:crapReport
```

For each flagged method, open the JaCoCo HTML report at `<module>/build/reports/jacoco/test/html/` and find the method. Lines marked `pc bpc` (partial branch coverage) or `nc` (not covered) are the missed branches. Count them. This tells you whether the fix is testing, simplification, or both.

## The decision: test, simplify, or both

CRAP has two levers: complexity and coverage. Pick the right one.

### Test when the method is readable as-is

A configuration method, a builder, or a mapping function may have moderate complexity from iteration and null guards, but no confusing control flow. The complexity is structural, not cognitive. Adding tests is the right fix because the method does not need to change.

Indicators that testing is the right fix:
- The method is a flat sequence of similar operations (registrations, field mappings, guard clauses)
- The complexity comes from `forEach` loops, null checks, or switch/if chains over a known set
- A reader can understand the method top to bottom without backtracking
- Only a few branches are uncovered in JaCoCo

Calculate the coverage needed: solve `cc^2 * (1 - cov)^3 + cc < 15` for your method's `cc`. For cc=14, you need >83% branch coverage. For cc=10, you need >68%. For cc=6, any coverage clears it.

Write tests that verify real behavior. Each test should assert something meaningful about the method's output or side effects. Do not write tests that exist only to execute a branch without checking the result.

### Simplify when the method is genuinely hard to follow

A method with deeply nested conditionals, interleaved concerns, or multiple responsibilities should be split. Extraction makes each piece independently testable and reduces the complexity score.

Indicators that simplification is the right fix:
- The method mixes unrelated responsibilities (e.g., parameter resolution AND API calls AND response mapping)
- Nested if/else chains three or more levels deep
- You need to read the whole method to understand any single part of it
- The method has both high complexity AND low coverage, and covering it would require elaborate test setup

Good extractions:
- Pull a self-contained phase into a private method (e.g., `buildFilterBody`, `resolveLatestSessionId`)
- Extract a loop body that matches/transforms items into its own method
- Separate guard-clause chains from the work they protect

### Do not simplify just to satisfy the metric

Never split a method solely because the CRAP score is high. If the method reads clearly top to bottom, testing is the fix. Splitting a readable method into fragments trades clarity for a number.

Anti-patterns:
- Extracting a 3-line null check into `handleNullCase()` to reduce branch count
- Creating a helper that has exactly one caller and makes the flow harder to follow
- Moving code to a new method without giving it a meaningful name and responsibility
- Restructuring production code to eliminate equivalent-mutant sites (see CLAUDE.md PIT guidance)

## Fixing the violation

1. **Find the missed branches.** Open the JaCoCo HTML report. Identify which branches are `nc` or `pc bpc`.
2. **Decide: test or simplify** using the criteria above. Often one test covering a missed branch is enough.
3. **Write the fix.** Tests go in the existing `*Test.java` file for the class. Simplification stays in the production class.
4. **Run the CRAP report** to confirm the method clears the threshold:
   ```
   ./gradlew :<module>:test :<module>:jacocoTestReport :<module>:crapReport
   ```
5. **Tighten the baseline** to lock in the progress:
   ```
   ./gradlew :<module>:crapBaselineTighten
   ```
6. **Run `make check`** to confirm everything passes.
7. **Commit the tightened baseline** alongside the code and test changes.

## Baseline management

Each module has a `crap4j-baseline.json` that excuses known violations. The baseline is shrink-only.

- `crapReport` shows baselined methods as "Baselined debt" and methods that improved past their allowance as "Slack."
- `crapBaselineTighten` removes slack entries (methods that now pass on their own). Run it after fixing violations.
- `crapBaseline` regenerates the baseline from scratch. Use only when adding a new baseline entry is unavoidable (rare).
- Never inflate the baseline to absorb new violations. Fix the code or cover it with tests.

## Quick reference: coverage needed by complexity

| Complexity | Coverage needed for CRAP < 15      |
|------------|------------------------------------|
| 15         | 100% (simplification required)     |
| 14         | > 83%                              |
| 12         | > 72%                              |
| 10         | > 63%                              |
| 8          | > 52%                              |
| 6          | > 37%                              |
| 4          | > 12% (almost any test clears it)  |

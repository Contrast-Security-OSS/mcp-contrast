---
name: archunit
description: Fix ArchUnit architectural violations. Use when ArchUnit tests fail, when fixing grandfathered violations from the freeze store, or when new code introduces a dependency the architecture rules forbid. Covers the fix-then-shrink workflow, SDK containment patterns, and anti-patterns to avoid.
---

# ArchUnit Violation Workflow

ArchUnit rules in `ArchitectureTest` enforce layering, SDK containment, domain isolation, and conventions. Violations are design feedback. Fix the design, never work around the test.

## When this skill applies

- An ArchUnit test fails during `make check-test` or `./gradlew :contrast-mcp-core:test`
- You are fixing a grandfathered violation from the freeze store
- New code you wrote introduces a dependency the architecture rules forbid

## The freeze store

Grandfathered violations live in `contrast-mcp-core/src/test/resources/archunit-store/`. Each file corresponds to one frozen rule (mapping in `stored.rules`).

The store is **shrink-only by design**. `-ParchStoreUpdate` lets ArchUnit remove stale entries for violations that no longer exist. New violations always fail regardless of any flag. ArchUnit will never auto-add new violations to the store.

**Never edit store files manually.** Only `./gradlew :contrast-mcp-core:test -ParchStoreUpdate` may modify them.

## Fix-then-shrink workflow

1. **Fix the code.** Change the production code so the violation no longer exists.
2. **Run the store update.** `./gradlew :contrast-mcp-core:test -ParchStoreUpdate` removes fixed entries.
3. **If tests still fail,** the remaining violations need code fixes, not more store entries. Go back to step 1.
4. **Run the full gate.** `make check-test` must pass.
5. **Commit the shrunken store** alongside the code changes.

## Fixing SDK containment violations

The rule "Only client and sdkextension may depend on the Contrast SDK directly" means tool classes and result models must not import `com.contrastsecurity` types.

**The fix is always a new abstraction in `sdkextension.data`.** Create a record or class that wraps the SDK type's fields, with a `from(SdkType)` factory method. Have the caller map through `YourType.from(sdkObject)` and pass your type downstream.

Do not replace a typed parameter with loose strings. Do not decompose a domain object into primitive arguments. The goal is to move the SDK boundary, not remove the type contract.

Example from this repo: `RecommendationData.from(Recommendation)` in `sdkextension.data` wraps the SDK `Recommendation` so `RecommendationMarkdownRenderer` never imports the SDK.

**For SDK exceptions**, the same pattern applies: define a domain exception that wraps the SDK exception, catch and rethrow at the boundary, and let callers catch the domain type. Example from this repo: `ContrastAccessDeniedException.from(UnauthorizedException)` in `client` lets tool-layer code handle access denials without importing the SDK.

## Fixing layering violations

**result depends on tool.** Extract the shared utility into `util` (a top-level package alongside `result`, `tool`, `client`). Both `result` and `tool` may import from `util`.

**tool domain cross-imports.** Move shared code to `tool.base`. If it does not fit there, consider `util`.

## Anti-patterns

- **Manually editing store files.** Never. Only the store update Gradle task modifies them.
- **Weakening types to dodge a dependency.** Replacing `Recommendation` with three `String` parameters satisfies the rule but degrades the design. Changing `SdkException` to `RuntimeException` or `Exception` to avoid the import is the same mistake: it removes the SDK dependency by destroying the type contract instead of moving it behind an abstraction.
- **Ignoring violations from a parent branch.** If rebasing or stacking surfaces violations not in the store, they are yours to fix on your branch.
- **Running `-ParchStoreUpdate` hoping it will absorb new violations.** It will not. It only removes stale entries.

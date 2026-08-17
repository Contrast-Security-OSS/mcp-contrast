# open-crap4j local integration research

Research date: 2026-08-16. Sources are the local `open-crap4j` checkout at
`/Users/chrisedwards/projects/oss/open-crap4j`, the current `mcp-contrast` checkout, and read-only
`br show` output for `mcp-y232` and `crap-0w4.14`.

## Conclusion

The local library can be consumed without publishing by adding its build to `pluginManagement`
and applying plugin id `com.architester.crap4j` to both Java modules. The plugin already owns the
JaCoCo-to-CRAP calculation, per-module baseline workflow, enforcing/advisory tasks, JSON output, and
JUnit sidecars. No CRAP implementation belongs in `mcp-contrast/buildSrc` under this approach.

`crap-0w4.14` supersedes the older `mcp-y232` design. This branch therefore uses the local plugin,
default per-module enforcement, two baselines, and the changed-file CLI. It does not add a custom
`buildSrc` implementation or the older `make crap` and `make crap-changed` interfaces.

## Local consumption and plugin contract

* `crap-0w4.14` explicitly selects `pluginManagement { includeBuild("<path-to-open-crap4j>") }`
  and says publishing is separate. The publishing plan also says the first proof is an
  `mcp-contrast` branch using `pluginManagement includeBuild`, with nothing published first
  (`open-crap4j/research/publishing-spec.md:16-21`).
* The plugin id is `com.architester.crap4j`, implemented by `Crap4jPlugin`
  (`open-crap4j/gradle-plugin/build.gradle.kts:23-29`). The documented application point is each
  module's plugin block (`open-crap4j/README.md:31-42`). With these checkouts, a portable local path
  from the `mcp-contrast` root is `../../oss/open-crap4j`.
* Applying the plugin to a Java project auto-applies JaCoCo, forces JaCoCo XML on, binds the
  extension to `build/reports/jacoco/test/jacocoTestReport.xml`, and makes all four CRAP tasks depend
  on `jacocoTestReport` (`open-crap4j/gradle-plugin/src/main/java/com/architester/crap4j/gradle/Crap4jPlugin.java:33-52`).
  This matches the existing report path in `mcp-contrast` (`build.gradle:203-228`). The existing
  class-directory filter, including `McpContrastApplication`, therefore remains upstream in the
  XML consumed by CRAP (`build.gradle:211-228`).
* Defaults are threshold 15.0, complexity cap 15, enforcement on, detached from Gradle `check`,
  loose-baseline warnings, and both JSON and JUnit enabled
  (`open-crap4j/gradle-plugin/src/main/java/com/architester/crap4j/gradle/Crap4jExtension.java:48-59`).
* Each module gets `crapCheck`, `crapReport`, `crapBaseline`, and `crapBaselineTighten`. Baselines
  default to `<module>/crap4j-baseline.json`; JSON and JUnit outputs default to
  `<module>/build/reports/crap4j/<task>/report.json` and `junit.xml`
  (`open-crap4j/gradle-plugin/src/main/java/com/architester/crap4j/gradle/Crap4jPlugin.java:20-31,61-99`).
* `crapCheck` fails after writing reports when unbaselined/regressed violations exist; setting
  `advisory` disables the failure. `crapReport` is always advisory
  (`open-crap4j/gradle-plugin/src/main/java/com/architester/crap4j/gradle/CrapCheck.java:7-15` and
  `CrapReport.java:5-10`). Functional tests cover failing report creation, baseline pass,
  `attachToCheck`, conventional baselines, configurable formats, and tightening
  (`open-crap4j/gradle-plugin/src/functionalTest/java/com/architester/crap4j/gradle/Crap4jPluginFunctionalTest.java:43-131,172-249`).
* `mcp-contrast` uses Gradle 8.14 (`gradle/wrapper/gradle-wrapper.properties:1-4`). The plugin's
  declared minimum is 8.5 (`open-crap4j/README.md:31-35`) and its TestKit suite tests 8.5 plus the
  latest Gradle (`open-crap4j/research/build-layout-spec.md:36-40`).

## Expected adoption result

`crap-0w4.14` calls for per-module gating on `contrast-mcp-core` and
`contrast-mcp-stdio-app`, then this lifecycle:

1. regenerate coverage and run `crapCheck`;
2. observe two default-policy core violations;
3. run `crapBaseline`, commit one baseline per module, and confirm `crapCheck` passes with the debt
   baselined;
4. confirm `crapBaselineTighten` is a no-op;
5. exercise `requireTightBaseline` and advisory mode;
6. validate both JUnit sidecars;
7. exercise CLI `--changed-files -` from real merge-base diff output and its outdated-report warning;
8. cross-check every method against `tools/crap-metrics.py`.

The recorded case study found 430 methods, no complexity-cap violations, and two CRAP-threshold
violations at the defaults: `RecommendationMarkdownRenderer.registerKnownTags` at 38.50 and
`SearchAppVulnerabilitiesTool.doExecute` at 18.50
(`open-crap4j/research/mcp-contrast-case-study.md:1-41`). These numbers are historical and the
acceptance bead permits ordinary upstream drift.

The CLI is a separate fat jar, not part of plugin composite resolution. Its current local artifact
is built by the `:cli:jar` task as `cli/build/libs/crap4j-cli-0.1.0.jar`; the build constructs the
fat jar from runtime classpath (`open-crap4j/cli/build.gradle.kts:13-26`). Changed-file input accepts
a file or stdin, selects whole classes by source-file path, reports unmatched paths as skipped, and
warns when a matched source is newer than the JaCoCo XML
(`open-crap4j/cli/src/main/java/com/architester/crap4j/cli/Crap4jCli.java:135-155,227-260`). Thus
changed-file granularity is file-level, consistent with the original `mcp-y232` decision.

## Lifecycle integration risk

`attachToCheck = true` only makes Gradle's module `check` tasks depend on `crapCheck`
(`Crap4jPlugin.java:50-52`). It does **not** put CRAP into this repository's normal gates:

* `make check` invokes `spotlessCheck checkstyleMain checkstyleTest`, not `check`
  (`Makefile:15-30`).
* `make check-test` composes that target with buildSrc, coverage, and mutation targets, none of
  which invokes `crapCheck` (`Makefile:147-170`).
* CI likewise invokes an explicit task list and never invokes `check` or `crapCheck`
  (`.github/workflows/build.yml:39-62`).

If “gate mcp-contrast end to end” means the normal local and CI lifecycle, implementation must add
`crapCheck` to those explicit task lists (and upload the sidecars/reports). If it means only proving
the plugin through direct `./gradlew crapCheck`, no Make or CI change is needed. A local
`includeBuild` cannot run on GitHub-hosted CI unless CI also checks out open-crap4j, so CI wiring is
incompatible with the stated pre-publication, local-only phase unless extra checkout plumbing is
authorized.

## Verification risks

* Implementation started on the existing `AIML-1227-add-crap-metrics` branch, stacked on the
  current feature stack. The open-crap4j checkout was clean on `main` at `eb6ca40`.
* The `open-crap4j` Python oracle is hard-coded to the two mcp module report paths, so it should run
  with `mcp-contrast` as the working directory (`open-crap4j/tools/crap-metrics.py:112-126`).
  However, its command-line output contains only the top 15 methods, not every scored method
  (`open-crap4j/tools/crap-metrics.py:172-179`). Satisfying the bead's “full per-method diff” needs
  a temporary comparison harness importing `parse_report`, or a change to that oracle; ordinary
  script output alone cannot prove zero disagreements.
* The local composite path must not be merged unchanged. The release plan explicitly replaces it
  with published coordinates only after this proof (`open-crap4j/research/publishing-spec.md:16-21`).
* The plugin produces a conventional baseline only when `crapBaseline` runs. Explicitly setting a
  missing baseline is an error, whereas leaving the convention unset permits the first run
  (`Crap4jPluginFunctionalTest.java:134-169`). Avoid explicitly configuring the conventional file
  before generating it.

## Decisions and dogfooding result

The user selected the newer acceptance design: commit the relative composite build for a strictly
local, unpushed proof; expose enforcement through direct Gradle tasks; leave Make and CI unchanged;
and omit the old Make interfaces.

The plugin analyzed 383 core methods and 84 stdio methods. The first `crapCheck` failed on the two
expected core methods, `RecommendationMarkdownRenderer.registerKnownTags` at 38.50 and
`SearchAppVulnerabilitiesTool.doExecute` at 18.52. After generating the two per-module baselines,
`crapCheck` passed with two baselined core methods and no stdio debt. Both baselines were tight,
strict-baseline mode passed, advisory mode passed, and all JSON and JUnit sidecars were well-formed.

CLI changed-file checks passed against the real stack diff and emitted the expected outdated-report
warning. A descriptor-aware independent parser matched all 467 plugin results with zero complexity,
coverage-kind, or two-decimal CRAP disagreements. The checked-in Python oracle itself reported only
436 methods because it keys raw methods by name and collapses overloads; it needs a separate fix
before it can perform the acceptance bead's promised full per-method comparison directly.

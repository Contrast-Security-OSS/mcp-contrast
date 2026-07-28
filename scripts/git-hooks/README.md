# Git hook scripts

Hooks that catch problems before code reaches CI.

Install them with:

```shell
make install-hooks     # or: ./gradlew installGitHooks
```

The task copies every file in this directory into the hooks directory git reports for this
checkout, which is not always `.git/hooks`, and marks each one executable for you only. It is
idempotent.

Only one script can own a hook name, and this repo also uses the beads hooks. When the task
replaces a hook whose content differs, it saves the old one as `<name>.bak-<timestamp>` and says
so. Merge the two by hand if both are wanted.

## Installed hooks

* `pre-push` runs `jacocoChangedFileCoverageVerification`, which fails when a changed
  `src/main/java` file has less than the line coverage required by
  `changedFileCoverageMinimum` in the root `build.gradle`.

  It compares against the remote ref being pushed when there is one, then
  `COVERAGE_BASE_REF` when explicitly set, the pushed branch's own upstream, and finally
  `origin/main`. Git records the upstream requested by `git push -u` only after the first push
  succeeds, so name the parent when first pushing a new stacked branch:

  ```shell
  COVERAGE_BASE_REF=origin/parent-branch git push -u origin my-stacked-branch
  ```

  When no base with a common ancestor resolves, it says so and skips that ref rather than
  blocking the push.

  It measures only the ref whose commit is the checked-out `HEAD`. Coverage comes from compiling
  and testing the working tree, so no other pushed ref can be scored honestly. Other refs are
  named and reported as not measured, and CI gates them.

  When coverage-relevant working-tree files differ from the commit — source files, Gradle
  configuration and wrapper files, Lombok configuration, or main/test resources — the hook warns
  and still runs. Its result then describes the current files on disk, while pull-request CI
  verifies the exact pushed commit from a clean checkout. Unrelated changes such as Markdown
  files do not produce a warning. Refs that change no Java files skip Gradle entirely, since
  Gradle startup is most of the hook's cost.

  Set `SKIP_COVERAGE_HOOK=1` to bypass the gate for a push.

## Running the gate by hand

Without a base ref the task measures the working tree, which is what you want mid-change:

```shell
make coverage-changed
```

Against a specific base, matching what the hook does:

```shell
./gradlew jacocoChangedFileCoverageVerification -PjacocoChangedBase=origin/main
```

## What counts as a pass

A changed file missing from the JaCoCo report fails the gate. The one exception is a source path
listed in `coverageExcludedClassFiles` in the root `build.gradle` (today only
`McpContrastApplication`). This used to pass silently. It does not anymore.

A changed file that is in the report but has no `LINE` counter passes as "nothing to measure."
JaCoCo found nothing countable in the file, typically an interface or a type whose members are
entirely Lombok-generated. This is not rare: roughly 49 of the 128 `contrast-mcp-core` sources
hit this path today.

A missing or empty JaCoCo report is a hard failure, not a skip.

## References

https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks

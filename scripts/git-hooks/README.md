# Git hook scripts

Hooks that catch problems before code reaches CI.

Install them with:

```shell
make install-hooks     # or: ./gradlew installGitHooks
```

The task copies every file in this directory into `.git/hooks` and marks it executable. It is
idempotent, but it **overwrites** any hook of the same name that is already installed. Check
`.git/hooks` first if you keep hooks from another tool there, such as the beads hooks.

## Installed hooks

* `pre-push` runs `jacocoChangedFileCoverageVerification`, which fails when a changed
  `src/main/java` file has less than the line coverage required by
  `changedFileCoverageMinimum` in the root `build.gradle`.

  It compares against the remote ref being pushed when there is one, then the branch upstream,
  then `origin/main`. Using the branch upstream first keeps a stacked branch measured against
  its parent rather than against `main`.

  Only committed files in the push are inspected, so unrelated work in progress cannot block a
  clean push. Refs that change no Java files skip Gradle entirely, since Gradle startup is most
  of the hook's cost.

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

## What the gate cannot see

A changed file missing from the JaCoCo report is reported as skipped rather than failed. Today
that means a class listed in `coverageExcludedClassFiles` in the root `build.gradle`. The task
names every skipped file so a silent gap does not read as a pass.

## References

https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks

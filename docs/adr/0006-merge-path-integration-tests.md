# Run integration tests on the PR merge path

Extends ADR 0004. Previously, integration tests ran only during manual release dispatches (`gradle-release.yml`) and local `make verify`. PRs merged on `check` alone, which excludes `*IT.class`. Whether a filter narrows, a sort orders, or live JSON maps to the right fields was unverified until release.

We decided:

- A separate `integration-test` job in `build.yml` runs `integrationTest` on every internal pull request, after the `build` job passes.
- The job runs `requireIntegrationTestCredentials` before `integrationTest` so it fails loudly when secrets are missing, matching the `verify` behavior from ADR 0004 rather than silently skipping.
- Fork PRs are excluded (`github.event.pull_request.head.repo.fork != true`) because they have no access to repo secrets.
- Dependabot PRs are excluded (`github.actor != 'dependabot[bot]'`) because they only change dependency versions and do not need live TeamServer verification. This avoids unnecessary load on the test org.
- A per-PR concurrency group with `cancel-in-progress: true` prevents rapid pushes from stacking up concurrent runs against the same org.
- The job does not run on push to `main`. Release `verify` already covers that path.

## Considered options

- **Run `verify` instead of bare `integrationTest`.** Rejected: `verify` reruns `check` (static analysis, unit tests, coverage, CRAP, PIT), which the `build` job already completed. Running only `requireIntegrationTestCredentials` + `integrationTest` avoids the redundant work.
- **Run on all PRs including Dependabot.** Rejected: Dependabot PRs change dependency versions only. Running live API tests for every dependency bump consumes test-org capacity without verifying application behavior.
- **Run on push to `main` as a heartbeat.** Not adopted in this change. Release `verify` covers main, and a scheduled heartbeat could be added later if external API drift becomes a problem.
- **Label-gated trigger (only run when a label is applied).** Rejected: adds manual friction to every PR. The concurrency group and Dependabot exclusion are enough to keep load manageable.
- **`pull_request_target` instead of `pull_request`.** Rejected: `pull_request_target` runs the workflow from the base branch, not the PR branch, so it would test the wrong code. The fork exclusion on `pull_request` achieves the same secret-protection goal.

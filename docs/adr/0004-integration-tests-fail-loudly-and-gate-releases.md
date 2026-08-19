# Integration tests fail loudly without credentials and gate releases

Every `*IT` class is guarded by `@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", ...)`, and nothing loaded `.env.integration-test` automatically. The result: the release workflow invoked `integrationTest` with no credentials and went green having run zero tests, and a bare `make verify` in a fresh shell did the same. The repo's own testing standards forbid silent skips, and the build itself was the violator.

We decided:

- Gradle sources `.env.integration-test` itself when the file exists. Real environment variables win over file values, so CI and one-off shells can override without editing files.
- `verify` (ADR 0003) fails loudly when no credentials are available from either source. A gate that can pass on zero integration tests is not a gate.
- Bare `integrationTest` keeps the skip behavior, so forks and credential-less checkouts can still run the rest of the build.
- Release CI runs `verify` with the five `CONTRAST_*` values as plain GitHub repository secrets, so integration tests genuinely gate releases. The secrets initially hold a maintainer's personal eval-org credentials to unblock the work; AIML-1408 (high priority) migrates them to a dedicated service account.
- `verify` is the maintainer gate. Contributors without credentials run `check`; CI on the clean checkout stays authoritative.

## Considered options

- **Soften fail-loud to a warning** so `verify` passes without credentials. Rejected: that reintroduces the silent-skip disease in dilute form.
- **Drop `integrationTest` from release validation** and keep `verify` local-only. Rejected: a release is precisely when the live TeamServer contract should be tested.
- **A GitHub environment with protection rules** instead of plain repo secrets. Rejected: the release workflow is already gated on manual dispatch by maintainers, and fork PRs can never read repo secrets, so the extra layer duplicates an existing control.
- **`verify` in a git hook.** Rejected: live-API tests are minutes long, network- and credential-dependent, and occasionally flaky for reasons unrelated to the change. Hooks that fail spuriously teach people to bypass hooks.

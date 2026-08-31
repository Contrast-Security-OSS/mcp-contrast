# Integration Tests

This project includes integration tests that run against a real Contrast TeamServer instance.

## Setup

1. **Copy the environment template:**
   ```bash
   cp .env.integration-test.template .env.integration-test
   ```

2. **Fill in your Contrast credentials:**
   Edit `.env.integration-test` with your actual credentials:
   - `CONTRAST_HOST_NAME` - Your TeamServer host (e.g., `app.contrastsecurity.com`)
   - `CONTRAST_API_KEY` - Your API key
   - `CONTRAST_SERVICE_KEY` - Your service key
   - `CONTRAST_USERNAME` - Your username
   - `CONTRAST_ORG_ID` - Your organization ID

That's it. Gradle sources `.env.integration-test` itself; no `source` step is needed. Real environment variables override file values, so CI secrets and one-off shells win without editing the file.

## Library Test Seed Data

The library integration tests require at least one application whose libraries contain all of the following:

- A populated vulnerabilities array
- A vulnerability whose name starts with `CVE-`
- Actively used classes (`classesUsed > 0`)

Discovery first queries `/ng/{orgId}/libraries/filter` across the organization with
`quickFilter=VULNERABLE` and `expand=vulns,apps`, then confirms candidate data through the
application-scoped library endpoint. Set `CONTRAST_TEST_SEED_APP_ID` to check a known-good
application before that organization-wide query. The pinned application is used only when it
satisfies the full seed-data contract; otherwise discovery logs a warning and continues normally.
The normalized pin is part of the disk-cache identity, so setting or changing it cannot reuse
discovery data cached for another pin.

Discovery responses are cached under `contrast-mcp-stdio-app/test-cache/`. Use these controls when
troubleshooting changing seed data:

- `CONTRAST_TEST_CACHE_DISABLE=true` disables the disk cache.
- `CONTRAST_TEST_CACHE_CLEAR=true` clears cached entries before the next run.
- `CONTRAST_TEST_CACHE_TTL_HOURS=N` overrides the cache lifetime.

If discovery reports `requires seeded app with vulnerable CVE — see INTEGRATION_TESTS.md`, seed the
organization with a qualifying application or set `CONTRAST_TEST_SEED_APP_ID` to one that already
qualifies. Editing or weakening the tests does not fix this missing-data precondition.

## Running Integration Tests

### Full gate plus integration tests (the pre-review command):
```bash
make verify
```

### Run integration tests only:
```bash
./gradlew :contrast-mcp-stdio-app:integrationTest
```

### Run only unit tests (default):
```bash
./gradlew test
```

## How It Works

- **Unit tests** (`*Test.java`) run with the Gradle `test` task
- **Integration tests** (`*IT.java`) run with the Gradle `:contrast-mcp-stdio-app:integrationTest` task
- Credentials resolve from real environment variables first, then `.env.integration-test`
- `verify` fails loudly when no credentials are available (ADR 0004) — a verify that ran zero integration tests has not verified
- Bare `integrationTest` skips instead when `CONTRAST_HOST_NAME` is unset, so forks and credential-less checkouts still build

## GitHub Actions / CI

For GitHub Actions, add these secrets to your repository:
- `CONTRAST_HOST_NAME`
- `CONTRAST_API_KEY`
- `CONTRAST_SERVICE_KEY`
- `CONTRAST_USERNAME`
- `CONTRAST_ORG_ID`

Integration tests run in two CI contexts:

- **PR builds** (`build.yml`): The `integration-test` job runs on every internal PR (fork PRs are skipped because they have no access to secrets). It runs after the `build` job passes.
- **Release builds** (`gradle-release.yml`): The `verify` lifecycle runs integration tests as part of the release validation build.

## Adding New Integration Tests

1. Create a new test class in `contrast-mcp-stdio-app/src/test/java` with the `IT` suffix (e.g., `MyFeatureIT.java`)
2. Annotate with `@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")`
3. Use real Contrast SDK calls (no mocking)
4. Run with `./gradlew :contrast-mcp-stdio-app:integrationTest` to execute

## Troubleshooting

**Integration tests don't run:**
- Verify environment variables are set: `echo $CONTRAST_HOST_NAME`
- Make sure you're running `./gradlew :contrast-mcp-stdio-app:integrationTest` (not just `./gradlew test`)
- Check that test class name ends with `IT.java`

**Tests fail with authentication errors:**
- Verify your credentials are correct
- Check that your API key has appropriate permissions
- Ensure your organization ID is correct

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

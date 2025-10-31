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

3. **Source the environment file:**
   ```bash
   source .env.integration-test
   ```

## Running Integration Tests

### Run integration tests only:
```bash
mvn verify
```

### Run all tests (unit + integration):
```bash
mvn clean verify
```

### Skip integration tests:
```bash
mvn verify -DskipITs
```

### Run only unit tests (default):
```bash
mvn test
```

## How It Works

- **Unit tests** (`*Test.java`) run during the `test` phase via Maven Surefire
- **Integration tests** (`*IT.java`) run during the `verify` phase via Maven Failsafe
- Integration tests only execute if `CONTRAST_HOST_NAME` environment variable is set
- If environment variables are missing, integration tests are automatically skipped

## GitHub Actions / CI

For GitHub Actions, add these secrets to your repository:
- `CONTRAST_HOST_NAME`
- `CONTRAST_API_KEY`
- `CONTRAST_SERVICE_KEY`
- `CONTRAST_USERNAME`
- `CONTRAST_ORG_ID`

Example GitHub Actions workflow:

```yaml
- name: Run integration tests
  run: mvn verify
  env:
    CONTRAST_HOST_NAME: ${{ secrets.CONTRAST_HOST_NAME }}
    CONTRAST_API_KEY: ${{ secrets.CONTRAST_API_KEY }}
    CONTRAST_SERVICE_KEY: ${{ secrets.CONTRAST_SERVICE_KEY }}
    CONTRAST_USERNAME: ${{ secrets.CONTRAST_USERNAME }}
    CONTRAST_ORG_ID: ${{ secrets.CONTRAST_ORG_ID }}
```

## Current Integration Tests

### EnvironmentsIT.java

Tests that environments and tags are properly populated from TeamServer API:
- `testEnvironmentsAndTagsArePopulated()` - Verifies vulnerability responses include environments and tags
- `testVulnerabilitiesHaveBasicFields()` - Verifies basic vulnerability fields are present

## Adding New Integration Tests

1. Create a new test class in `src/test/java` with the `IT` suffix (e.g., `MyFeatureIT.java`)
2. Annotate with `@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")`
3. Use real Contrast SDK calls (no mocking)
4. Run with `mvn verify` to execute

## Troubleshooting

**Integration tests don't run:**
- Verify environment variables are set: `echo $CONTRAST_HOST_NAME`
- Make sure you're running `mvn verify` (not just `mvn test`)
- Check that test class name ends with `IT.java`

**Tests fail with authentication errors:**
- Verify your credentials are correct
- Check that your API key has appropriate permissions
- Ensure your organization ID is correct

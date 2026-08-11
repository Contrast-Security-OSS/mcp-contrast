# Repo context

## Definitions

Domain terms live in [CONTEXT.md](./CONTEXT.md). Read it before touching tool contracts. It covers the Vulnerability status vocabulary, the distinct Issue Status vocabulary, and status display labels.

## Design docs

Planning design docs are saved in the bead's design field via `scripts/br-set-design <bead-id> <file>`. They are not committed to the repo.

## Architecture
<!-- High-level architecture notes -->

## Running & testing

**Prerequisites:** Java 21+, Gradle wrapper (`./gradlew`), Docker (optional).

**Build and verify:**
```bash
make check-test    # static analysis + unit tests + coverage (standard local gate)
make verify        # all tests including integration (requires .env.integration-test)
make mutation      # PIT mutation testing on contrast-mcp-core
```

**Run locally:**
```bash
./gradlew :contrast-mcp-stdio-app:bootJar
java -jar contrast-mcp-stdio-app/build/libs/mcp-contrast-*.jar \
  --CONTRAST_HOST_NAME=<host> --CONTRAST_API_KEY=<key> \
  --CONTRAST_SERVICE_KEY=<key> --CONTRAST_USERNAME=<user> \
  --CONTRAST_ORG_ID=<org>
```

**Docker:**
```bash
docker build -t mcp-contrast .
docker run -e CONTRAST_HOST_NAME=<host> -e CONTRAST_API_KEY=<key> \
  -e CONTRAST_SERVICE_KEY=<key> -e CONTRAST_USERNAME=<user> \
  -e CONTRAST_ORG_ID=<org> -i --rm mcp-contrast:latest -t stdio
```

**Integration tests** require Contrast credentials in `.env.integration-test` (copy from `.env.integration-test.template`). They are skipped when credentials are absent.

## Planning checklist

- [ ] **Cross-component data-contract changes** — does this change the public API surface of `contrast-mcp-core` (classes, method signatures, response shapes)? If so, verify compatibility with `aiml-hosted-mcp-server` and any other consumers before merging.
- [ ] **Integration/E2E test updates** — does this change or add behavior covered by integration tests (`*IT.java`)? If so, update or add ITs to match. Run `make verify` to confirm.

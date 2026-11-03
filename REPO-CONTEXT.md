# Repo context

## Definitions

Domain terms live in [CONTEXT.md](./CONTEXT.md). Read it before touching tool contracts. It covers the Vulnerability status vocabulary, the AutoRemediated misnomer, SmartFix, the distinct Issue Status vocabulary, and status display labels.

## Architecture
<!-- High-level architecture notes -->

## Running & testing

**Prerequisites:** Java 21+, Gradle wrapper (`./gradlew`), Docker (optional).

**Build and verify:**
```bash
make check         # standard local gate: static analysis + unit tests + coverage + CRAP + mutation
make verify        # check + integration tests (requires credentials, fails loudly without)
make lint          # fast inner loop: format + checkstyle only, not a gate
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

- [ ] **Cross-component build breaks** — does this change the public API surface of `contrast-mcp-core` (classes, method signatures, response shapes)? Contract changes are fine by design and are never a reason to reject or water down a design (see "No Backwards Compatibility Constraints" in CLAUDE.md). If the change breaks the `aiml-hosted-mcp-server` build, fix that build as part of the core version bump.
- [ ] **Integration/E2E test updates** — does this change or add behavior covered by integration tests (`*IT.java`)? If so, update or add ITs to match. Run `make verify` to confirm.

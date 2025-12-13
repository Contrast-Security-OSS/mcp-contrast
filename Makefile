MVN ?= mvn

.PHONY: help build test test-verbose check check-verbose check-test format clean verify verify-verbose

help: ## Display available make targets
	@awk 'BEGIN {FS=":.*##"; printf "\nUsage: make <target>\n\nTargets:\n"} /^[a-zA-Z0-9_\-]+:.*##/ {printf "  %-12s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Build the project (compile + package)
	@if [ -n "$$VERBOSE" ]; then \
		$(MVN) package -DskipTests; \
	else \
		. ./hack/run_silent.sh && run_silent "Building mcp-contrast" "$(MVN) package -DskipTests"; \
	fi

## Check targets (formatting and static analysis)

check: ## Run format check (quiet output)
	@if [ -n "$$VERBOSE" ]; then \
		$(MVN) spotless:check; \
	else \
		$(MAKE) check-quiet; \
	fi

check-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running Checks"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Static analysis"
	@. ./hack/run_silent.sh && run_with_quiet "Format check passed" "$(MVN) spotless:check"

check-verbose: ## Run checks with verbose output
	@VERBOSE=1 $(MAKE) check

## Test targets

test: ## Run unit tests (quiet output)
	@if [ -n "$$VERBOSE" ]; then \
		$(MVN) test; \
	else \
		$(MAKE) test-quiet; \
	fi

test-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running Tests"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Unit tests"
	@. ./hack/run_silent.sh && run_silent_with_test_count "Unit tests passed" "$(MVN) test" "maven"

test-verbose: ## Run tests with verbose output
	@VERBOSE=1 $(MAKE) test

## Verify targets (unit + integration tests)

verify: ## Run all tests including integration (quiet output)
	@if [ -n "$$VERBOSE" ]; then \
		$(MVN) verify; \
	else \
		$(MAKE) verify-quiet; \
	fi

verify-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running All Tests"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Unit + Integration tests"
	@. ./hack/run_silent.sh && run_silent_with_test_count "All tests passed" "$(MVN) verify" "maven"

verify-verbose: ## Run all tests with verbose output
	@VERBOSE=1 $(MAKE) verify

## Combined targets

check-test: ## Run all checks and tests
	@$(MAKE) check
	@$(MAKE) test

## Other targets

format: ## Auto-format code with Spotless
	@if [ -n "$$VERBOSE" ]; then \
		$(MVN) spotless:apply; \
	else \
		. ./hack/run_silent.sh && run_silent "Formatting code" "$(MVN) spotless:apply"; \
	fi

clean: ## Remove build artifacts
	@if [ -n "$$VERBOSE" ]; then \
		$(MVN) clean; \
	else \
		. ./hack/run_silent.sh && run_silent "Cleaning" "$(MVN) clean"; \
	fi

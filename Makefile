GRADLE ?= ./gradlew

.PHONY: help build test test-verbose check check-verbose check-test workflow-check format clean verify verify-verbose

help: ## Display available make targets
	@awk 'BEGIN {FS=":.*##"; printf "\nUsage: make <target>\n\nTargets:\n"} /^[a-zA-Z0-9_\-]+:.*##/ {printf "  %-12s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Build the project (compile + package)
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) :contrast-mcp-stdio-app:bootJar -x test; \
	else \
		. ./hack/run_silent.sh && run_silent "Building mcp-contrast" "$(GRADLE) :contrast-mcp-stdio-app:bootJar -x test"; \
	fi

## Check targets (formatting and static analysis)

check: format ## Run format and static analysis checks (quiet output)
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) spotlessCheck checkstyleMain checkstyleTest; \
	else \
		$(MAKE) check-quiet; \
	fi

check-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running Checks"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Static analysis"
	@. ./hack/run_silent.sh && run_with_quiet "All checks passed" "$(GRADLE) spotlessCheck checkstyleMain checkstyleTest"

check-verbose: ## Run checks with verbose output
	@VERBOSE=1 $(MAKE) check

## Test targets

test: ## Run unit tests (quiet output)
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) test; \
	else \
		$(MAKE) test-quiet; \
	fi

test-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running Tests"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Unit tests"
	@. ./hack/run_silent.sh && run_silent_with_test_count "Unit tests passed" "$(GRADLE) test" "gradle"

test-verbose: ## Run tests with verbose output
	@VERBOSE=1 $(MAKE) test

## Verify targets (unit + integration tests)

verify: ## Run all tests including integration (quiet output)
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) test :contrast-mcp-stdio-app:integrationTest; \
	else \
		$(MAKE) verify-quiet; \
	fi

verify-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running All Tests"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Unit + Integration tests"
	@. ./hack/run_silent.sh && run_silent_with_test_count "All tests passed" "$(GRADLE) test :contrast-mcp-stdio-app:integrationTest" "gradle"

verify-verbose: ## Run all tests with verbose output
	@VERBOSE=1 $(MAKE) verify

## Combined targets

check-test: ## Run all checks and tests
	@$(MAKE) workflow-check
	@$(MAKE) check
	@$(MAKE) test

## Other targets

workflow-check: ## Verify public Gradle/docs/CI workflow alignment
	@./hack/verify-public-workflow-alignment.sh

format: ## Auto-format code with Spotless
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) spotlessApply; \
	else \
		. ./hack/run_silent.sh && run_silent "Formatting code" "$(GRADLE) spotlessApply"; \
	fi

clean: ## Remove build artifacts
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) clean; \
	else \
		. ./hack/run_silent.sh && run_silent "Cleaning" "$(GRADLE) clean"; \
	fi

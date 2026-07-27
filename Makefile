GRADLE ?= ./gradlew

.PHONY: help build test test-verbose check check-verbose check-test buildsrc-check buildsrc-check-verbose coverage coverage-verbose coverage-changed coverage-changed-verbose test-coverage test-coverage-verbose install-hooks format clean verify verify-verbose

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

# buildSrc is a separate Gradle build, so `gradlew test` and `gradlew check` in the root
# build never schedule its tests. The changed-file coverage gate lives here, so it needs
# its own invocation or nothing verifies it.
buildsrc-check: ## Run buildSrc static analysis and tests
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) -p buildSrc check; \
	else \
		$(MAKE) buildsrc-check-quiet; \
	fi

buildsrc-check-quiet:
	@. ./hack/run_silent.sh && print_main_header "Checking buildSrc"
	@. ./hack/run_silent.sh && print_header "buildSrc" "Static analysis + tests"
	@. ./hack/run_silent.sh && run_with_quiet "buildSrc checks passed" "$(GRADLE) -p buildSrc check"

buildsrc-check-verbose: ## Run buildSrc checks with verbose output
	@VERBOSE=1 $(MAKE) buildsrc-check

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

## Coverage targets

coverage: ## Verify coverage floors and print the summary
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) --continue jacocoTestCoverageVerification coverageSummary; \
	else \
		$(MAKE) coverage-quiet; \
	fi

# The summary runs after verification and regardless of its result, because a breached
# floor is exactly when the numbers are wanted. The verification exit code is held and
# re-raised at the end so a failure still fails the target.
coverage-quiet:
	@. ./hack/run_silent.sh && print_main_header "Checking Coverage"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Coverage floors"
	@status=0; \
	. ./hack/run_silent.sh && run_with_quiet "Coverage floors met" "$(GRADLE) jacocoTestCoverageVerification" || status=$$?; \
	$(GRADLE) --quiet coverageSummary; \
	exit $$status

coverage-verbose: ## Run coverage with verbose output
	@VERBOSE=1 $(MAKE) coverage

# Measures the working tree by default. Pass BASE to compare against a ref instead, which is
# what the pre-push hook does: make coverage-changed BASE=origin/main
coverage-changed: ## Check changed src/main/java files meet the changed-file coverage minimum
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) jacocoChangedFileCoverageVerification $(if $(BASE),-PjacocoChangedBase=$(BASE)); \
	else \
		$(MAKE) coverage-changed-quiet; \
	fi

coverage-changed-quiet:
	@. ./hack/run_silent.sh && print_main_header "Checking Changed-File Coverage"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Changed-file coverage"
	@. ./hack/run_silent.sh && run_with_quiet "Changed-file coverage met" \
		"$(GRADLE) jacocoChangedFileCoverageVerification $(if $(BASE),-PjacocoChangedBase=$(BASE))"

coverage-changed-verbose: ## Run changed-file coverage with verbose output
	@VERBOSE=1 $(MAKE) coverage-changed

# One invocation on purpose. bootBuildInfo rewrites build-info.properties every run, so the
# app's test task is never UP-TO-DATE and separate `make test` + `make coverage` runs it twice.
test-coverage: ## Run unit tests and verify coverage floors in one invocation
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) --continue test jacocoTestCoverageVerification coverageSummary; \
	else \
		$(MAKE) test-coverage-quiet; \
	fi

# Held exit code as in coverage-quiet: the summary is wanted precisely when a floor breaks.
test-coverage-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running Tests and Coverage"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Unit tests + coverage floors"
	@status=0; \
	. ./hack/run_silent.sh && run_silent_with_test_count "Tests passed, coverage floors met" \
		"$(GRADLE) test jacocoTestCoverageVerification" "gradle" || status=$$?; \
	$(GRADLE) --quiet coverageSummary; \
	exit $$status

test-coverage-verbose: ## Run tests and coverage with verbose output
	@VERBOSE=1 $(MAKE) test-coverage

## Combined targets

check-test: ## Run all checks, tests, and coverage
	@$(MAKE) check
	@$(MAKE) buildsrc-check
	@$(MAKE) test-coverage

## Other targets

# Deliberately not wrapped in run_silent. Installing a hook can displace one another tool owns,
# and run_silent discards that warning on success.
install-hooks: ## Install the repository git hooks into the git hooks directory
	@$(GRADLE) installGitHooks

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

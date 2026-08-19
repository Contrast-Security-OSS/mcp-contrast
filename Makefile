GRADLE ?= ./gradlew

.PHONY: help build lint lint-verbose format check check-verbose verify verify-verbose buildsrc-check buildsrc-check-verbose coverage-changed coverage-changed-verbose install-hooks clean

help: ## Display available make targets
	@awk 'BEGIN {FS=":.*##"; printf "\nUsage: make <target>\n\nTargets:\n"} /^[a-zA-Z0-9_\-]+:.*##/ {printf "  %-16s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Build the project (compile + package)
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) :contrast-mcp-stdio-app:bootJar -x test; \
	else \
		. ./hack/run_silent.sh && run_silent "Building mcp-contrast" "$(GRADLE) :contrast-mcp-stdio-app:bootJar -x test"; \
	fi

# check and verify are defined in Gradle (ADR 0003); make adds only quiet output and
# auto-formatting. Attach new verifications to the Gradle check lifecycle, not here.

## Lint (fast inner loop, deliberately NOT a gate)

lint: ## Auto-format and run style checks only (fast, no tests)
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) spotlessApply checkstyleMain checkstyleTest; \
	else \
		$(MAKE) lint-quiet; \
	fi

lint-quiet:
	@. ./hack/run_silent.sh && print_main_header "Linting"
	@. ./hack/run_silent.sh && run_silent "Formatting code" "$(GRADLE) spotlessApply"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "Checkstyle"
	@. ./hack/run_silent.sh && run_with_quiet "Lint passed" "$(GRADLE) checkstyleMain checkstyleTest"

lint-verbose: ## Run lint with verbose output
	@VERBOSE=1 $(MAKE) lint

## Check (the standard local gate: everything provable from the repo alone)

check: ## Auto-format, then run the full Gradle check lifecycle and print the coverage summary
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) spotlessApply && $(GRADLE) --continue check coverageSummary; \
	else \
		$(MAKE) check-quiet; \
	fi

# Summary prints even when check fails (held exit code): a breached floor is when the
# numbers are wanted.
check-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running Check"
	@. ./hack/run_silent.sh && run_silent "Formatting code" "$(GRADLE) spotlessApply"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "check (static analysis, tests, coverage, CRAP, mutation)"
	@status=0; \
	. ./hack/run_silent.sh && run_silent_with_test_count "check passed" "$(GRADLE) check" "gradle" || status=$$?; \
	$(GRADLE) --quiet coverageSummary; \
	exit $$status

check-verbose: ## Run check with verbose output
	@VERBOSE=1 $(MAKE) check

## Verify (check plus the credential-gated integration tests)

verify: ## Run check plus integration tests (requires Contrast credentials, fails loudly without them)
	@if [ -n "$$VERBOSE" ]; then \
		$(GRADLE) spotlessApply && $(GRADLE) --continue verify coverageSummary; \
	else \
		$(MAKE) verify-quiet; \
	fi

verify-quiet:
	@. ./hack/run_silent.sh && print_main_header "Running Verify"
	@. ./hack/run_silent.sh && run_silent "Formatting code" "$(GRADLE) spotlessApply"
	@. ./hack/run_silent.sh && print_header "mcp-contrast" "verify (check + integration tests)"
	@status=0; \
	. ./hack/run_silent.sh && run_silent_with_test_count "verify passed" "$(GRADLE) verify" "gradle" || status=$$?; \
	$(GRADLE) --quiet coverageSummary; \
	exit $$status

verify-verbose: ## Run verify with verbose output
	@VERBOSE=1 $(MAKE) verify

## Gates that genuinely cannot live in the check lifecycle

# buildSrc is a separate Gradle build, so `gradlew check` in the root build never schedules
# its tests. The changed-file coverage gate's logic lives here, so it needs its own
# invocation or nothing verifies it.
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

# Needs a base ref, so it cannot join check (a working-tree run in CI would pass vacuously).
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

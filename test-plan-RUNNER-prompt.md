# Test Plan Runner Prompt

Set the TEST_TOOL variable to the tool you want to test:

```
TEST_TOOL=search_vulnerabilities
```

## Available Tools

| Tool | Description |
|------|-------------|
| search_applications | Search and filter applications |
| search_attacks | Search attack events with filters |
| search_vulnerabilities | Search vulnerabilities (org-level) |
| search_app_vulnerabilities | Search vulnerabilities (app-level) |
| get_vulnerability | Get single vulnerability details |
| list_vulnerability_types | List all vulnerability rule types |
| get_session_metadata | Get application session metadata |
| get_protect_rules | Get ADR/Protect rules |
| list_application_libraries | List libraries for an application |
| list_applications_by_cve | Find applications by CVE |
| get_route_coverage | Get route/endpoint coverage |
| get_scan_project | Get SAST scan project |
| get_scan_results | Get SAST scan results |

## Instructions

TEST_TOOL=get_scan_project

Read `test-plans/test-plan-{TEST_TOOL}.md` which is a test plan for the `{TEST_TOOL}` MCP server tool. Your job is to test this tool.

1. **Check for existing results**: Look for `test-plan-results/YYYY-MM-DD/results-{TEST_TOOL}.md` (using today's date) first to see if you already started and need to pick up where you left off.

2. **Create results file**: Create the directory `test-plan-results/YYYY-MM-DD/` (using today's date, e.g., `2026-01-02`) if it doesn't exist. Write a checklist of what you are doing to `test-plan-results/YYYY-MM-DD/results-{TEST_TOOL}.md` and start working through it.

3. **Data discovery**: You may need to do some discovery of what data exists in the server you are connected to. For instance, use `search_applications` to find apps, or `search_vulnerabilities` to find vulnerabilities. Write your findings to your results file.

4. **Track progress**: Keep results file updated with your progress and notes. I may need to restart you when context runs out - your notes will help you proceed efficiently.

5. **Record assertions**: Write the assertions you made and whether they were SUCCESS or FAILURE as sub-bullets under each test case in your results file.

Run each test in a separate subagent or you will run out of context. Ensure they write to the correct results file, in the test-plans folder.

Use the existing contrast mcp server to run the tests. DO NOT RUN THE JAR FILE DIRECTLY!
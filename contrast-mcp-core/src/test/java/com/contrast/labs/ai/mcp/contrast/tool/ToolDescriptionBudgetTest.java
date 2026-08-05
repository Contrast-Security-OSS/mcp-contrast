/*
 * Copyright 2026 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrast.labs.ai.mcp.contrast.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.tool.annotation.Tool;

/** Enforces the per-tool description budgets defined in {@code MCP_STANDARDS.md}. */
class ToolDescriptionBudgetTest {

  private static final String TOOL_PACKAGE = "com.contrast.labs.ai.mcp.contrast.tool";
  private static final int SEARCH_WORD_CEILING = 150;
  private static final int LIST_WORD_CEILING = 150;
  private static final int GET_WORD_CEILING = 40;
  private static final int UPDATE_WORD_CEILING = 60;

  // There were 13 core tools on 2026-08-04. Additions should not change this sanity floor.
  private static final int MINIMUM_TOOL_COUNT = 13;

  private static final Map<String, AllowlistEntry> ALLOWLIST = Map.of();
  private static final List<Method> TOOL_METHODS = ToolTestReflection.toolMethods(TOOL_PACKAGE);

  static Stream<Arguments> toolMethods() {
    return TOOL_METHODS.stream()
        .map(method -> Arguments.of(method.getAnnotation(Tool.class).name(), method));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("toolMethods")
  void description_should_not_exceed_its_verb_budget(String toolName, Method toolMethod) {
    var budget = budgetFor(toolName);
    var wordCount = wordCount(toolMethod.getAnnotation(Tool.class).description());
    var allowlistEntry = ALLOWLIST.get(toolName);

    if (allowlistEntry != null) {
      assertThat(wordCount)
          .as("%s is now within its base ceiling; remove the stale allowlist entry", toolName)
          .isGreaterThan(budget.ceiling());
      assertThat(justificationNamesSibling(toolName, allowlistEntry.justification()))
          .as(
              "%s allowlist justification must name the sibling tool it disambiguates against",
              toolName)
          .isTrue();
    }

    var effectiveCeiling =
        allowlistEntry == null ? budget.ceiling() : allowlistEntry.grantedCeiling();
    var ceilingSource =
        allowlistEntry == null
            ? budget.prefix() + " prefix"
            : "reviewed allowlist; base " + budget.prefix() + " prefix ceiling " + budget.ceiling();

    assertThat(wordCount)
        .overridingErrorMessage(
            "%s description is %d words, ceiling %d (%s). Cut words or add a reviewed allowlist "
                + "entry (sibling-disambiguation overage only).",
            toolName, wordCount, effectiveCeiling, ceilingSource)
        .isLessThanOrEqualTo(effectiveCeiling);
  }

  @Test
  void tool_scan_should_find_the_known_minimum_number_of_tools() {
    assertThat(TOOL_METHODS)
        .as("@Tool methods discovered under %s", TOOL_PACKAGE)
        .hasSizeGreaterThanOrEqualTo(MINIMUM_TOOL_COUNT);
  }

  @Test
  void allowlist_should_not_name_tools_missing_from_the_catalog() {
    Set<String> toolNames =
        TOOL_METHODS.stream()
            .map(method -> method.getAnnotation(Tool.class).name())
            .collect(Collectors.toSet());

    assertThat(ALLOWLIST.keySet())
        .as("Allowlist entries must name @Tool methods discovered under %s", TOOL_PACKAGE)
        .isSubsetOf(toolNames);
  }

  private static Budget budgetFor(String toolName) {
    if (toolName.startsWith("search_")) {
      return new Budget(SEARCH_WORD_CEILING, "search_");
    }
    if (toolName.startsWith("list_")) {
      return new Budget(LIST_WORD_CEILING, "list_");
    }
    if (toolName.startsWith("get_")) {
      return new Budget(GET_WORD_CEILING, "get_");
    }
    if (toolName.startsWith("update_")) {
      return new Budget(UPDATE_WORD_CEILING, "update_");
    }
    throw new AssertionError(
        toolName + " uses a new verb shape that needs a ceiling decision in MCP_STANDARDS.md");
  }

  private static int wordCount(String description) {
    // Word count is the plain whitespace-delimited token count of the @Tool description body.
    // @ToolParam text is excluded. This matches the method pinned in MCP_STANDARDS.md Enforcement.
    return description.trim().split("\\s+").length;
  }

  private static boolean justificationNamesSibling(String toolName, String justification) {
    return TOOL_METHODS.stream()
        .map(method -> method.getAnnotation(Tool.class).name())
        .filter(candidate -> !candidate.equals(toolName))
        .anyMatch(justification::contains);
  }

  private record Budget(int ceiling, String prefix) {}

  private record AllowlistEntry(int grantedCeiling, String justification) {}
}

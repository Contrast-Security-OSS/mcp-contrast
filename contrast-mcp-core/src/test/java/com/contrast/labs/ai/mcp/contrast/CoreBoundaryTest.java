/*
 * Copyright 2025 Contrast Security
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
package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CoreBoundaryTest {

  private static final Path CORE_MAIN = Path.of("src/main/java");
  private static final List<Path> REQUIRED_SUPPORT_TYPES =
      List.of(
          sourcePath("com/contrast/labs/ai/mcp/contrast/client/ContrastApiClient.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/AuthenticationStrategy.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/BaseTool.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/SingleTool.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/PaginatedTool.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/FilterHelper.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/LoggingKeys.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/ToolParams.java"),
          sourcePath(
              "com/contrast/labs/ai/mcp/contrast/tool/validation/ToolValidationContext.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/hints/HintGenerator.java"));
  private static final List<Path> EXPECTED_CORE_PRODUCTION_TOOLS =
      List.of(
          sourcePath(
              "com/contrast/labs/ai/mcp/contrast/tool/application/"
                  + "GetSessionMetadataTool.java"),
          sourcePath(
              "com/contrast/labs/ai/mcp/contrast/tool/application/SearchApplicationsTool.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/attack/GetProtectRulesTool.java"),
          sourcePath(
              "com/contrast/labs/ai/mcp/contrast/tool/vulnerability/GetVulnerabilityTool.java"),
          sourcePath(
              "com/contrast/labs/ai/mcp/contrast/tool/vulnerability/"
                  + "ListVulnerabilityTypesTool.java"),
          sourcePath(
              "com/contrast/labs/ai/mcp/contrast/tool/vulnerability/"
                  + "SearchAppVulnerabilitiesTool.java"),
          sourcePath(
              "com/contrast/labs/ai/mcp/contrast/tool/vulnerability/"
                  + "SearchVulnerabilitiesTool.java"));
  private static final List<String> LOCAL_ONLY_TYPES =
      List.of(
          "McpContrastApplication.java",
          "ContrastProperties.java",
          "ContrastSDKFactory.java",
          "SDKExtensionFactory.java",
          "SdkApiClient.java",
          "SDKHelper.java",
          "SDKExtension.java",
          "GetSastResultsTool.java");
  private static final List<LocalOnlyPattern> LOCAL_ONLY_TEXT =
      List.of(
          new LocalOnlyPattern(
              "stdio app configuration", "com.contrast.labs.ai.mcp.contrast.config"),
          new LocalOnlyPattern("Spring AI transport/runtime dependency", "spring-ai"),
          new LocalOnlyPattern(
              "Spring AI transport/runtime dependency", "org.springframework.ai.support"),
          new LocalOnlyPattern("Spring application configuration", "@Configuration"),
          new LocalOnlyPattern("local SDK class", "ContrastSDK"),
          new LocalOnlyPattern("local SDK extension factory", "SDKExtensionFactory"),
          new LocalOnlyPattern("local SDK API client", "SdkApiClient"),
          new LocalOnlyPattern("local SDK helper/cache implementation", "SDKHelper"),
          new LocalOnlyPattern("stdio application bootstrap", "McpContrastApplication"),
          new LocalOnlyPattern("local SDK helper/cache implementation", "com.google.common.cache"),
          new LocalOnlyPattern("local-only raw SARIF tool", "get_scan_results"),
          new LocalOnlyPattern("local-only raw SARIF tool", "GetSastResultsTool"),
          new LocalOnlyPattern("local-only raw SARIF tool", "SarifResult"));

  @Test
  void core_should_include_current_shared_support_types() throws IOException {
    var sourceFiles = javaSources().toList();

    assertThat(sourceFiles).as("core module must contain Java sources").isNotEmpty();
    assertThat(sourceFiles.stream().map(CORE_MAIN::relativize)).containsAll(REQUIRED_SUPPORT_TYPES);
  }

  @Test
  void core_should_not_include_local_only_type_names() throws IOException {
    var sourceFiles = javaSources().toList();

    assertThat(sourceFiles)
        .extracting(path -> path.getFileName().toString())
        .doesNotContainAnyElementsOf(LOCAL_ONLY_TYPES);
  }

  @Test
  void core_should_not_reference_local_app_runtime_sdk_wiring_cache_or_sarif_tools()
      throws IOException {
    var localOnlyMatches =
        javaSources().flatMap(CoreBoundaryTest::localOnlyMatches).distinct().toList();

    assertThat(localOnlyMatches)
        .as("core source must not reference local-only stdio, SDK, cache, or SARIF concerns")
        .isEmpty();
  }

  @Test
  void core_should_include_migrated_shared_production_tools() throws IOException {
    var productionToolSources =
        javaSources()
            .filter(
                path -> sourceText(path).contains("org.springframework.ai.tool.annotation.Tool"))
            .map(CORE_MAIN::relativize)
            .sorted()
            .toList();

    assertThat(productionToolSources)
        .as("core contains only the shared production tools migrated so far")
        .containsExactlyElementsOf(EXPECTED_CORE_PRODUCTION_TOOLS);
  }

  private static Stream<Path> javaSources() throws IOException {
    try (var sourcePaths = Files.walk(CORE_MAIN)) {
      return sourcePaths.filter(path -> path.toString().endsWith(".java")).toList().stream();
    }
  }

  private static Path sourcePath(String slashSeparatedPath) {
    var parts = slashSeparatedPath.split("/");
    return Path.of(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
  }

  private static Stream<String> localOnlyMatches(Path path) {
    var text = sourceText(path);
    return LOCAL_ONLY_TEXT.stream()
        .filter(pattern -> text.contains(pattern.token()))
        .map(
            pattern ->
                "%s references %s token `%s`"
                    .formatted(CORE_MAIN.relativize(path), pattern.category(), pattern.token()));
  }

  private static String sourceText(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + path, e);
    }
  }

  private record LocalOnlyPattern(String category, String token) {}
}

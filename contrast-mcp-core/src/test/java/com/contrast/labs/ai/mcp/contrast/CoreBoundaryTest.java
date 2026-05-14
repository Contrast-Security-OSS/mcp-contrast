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
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CoreBoundaryTest {

  private static final Path CORE_MAIN = Path.of("src/main/java");
  private static final List<String> REQUIRED_SUPPORT_TYPES =
      List.of(
          "com/contrast/labs/ai/mcp/contrast/tool/base/FilterHelper.java",
          "com/contrast/labs/ai/mcp/contrast/tool/base/LoggingKeys.java",
          "com/contrast/labs/ai/mcp/contrast/tool/base/ToolParams.java",
          "com/contrast/labs/ai/mcp/contrast/tool/validation/ToolValidationContext.java",
          "com/contrast/labs/ai/mcp/contrast/hints/HintGenerator.java");
  private static final List<String> FORBIDDEN_TYPES =
      List.of(
          "BaseTool.java",
          "SingleTool.java",
          "PaginatedTool.java",
          "ListVulnerabilityTypesTool.java",
          "GetSastResultsTool.java");
  private static final List<String> FORBIDDEN_TEXT =
      List.of(
          "com.contrast.labs.ai.mcp.contrast.config",
          "ContrastSDKFactory",
          "SDKExtensionFactory",
          "SDKHelper",
          "com.google.common.cache",
          "get_scan_results",
          "sarif",
          "SARIF");

  @Test
  void core_should_contain_only_transport_neutral_support_types() throws IOException {
    var sourceFiles = javaSources().toList();

    assertThat(sourceFiles).as("core module must contain Java sources").isNotEmpty();
    assertThat(sourceFiles.stream().map(CORE_MAIN::relativize).map(Path::toString))
        .containsAll(REQUIRED_SUPPORT_TYPES);
    assertThat(sourceFiles)
        .extracting(path -> path.getFileName().toString())
        .doesNotContainAnyElementsOf(FORBIDDEN_TYPES);
  }

  @Test
  void core_should_not_reference_local_sdk_wiring_cache_or_sarif_tools() throws IOException {
    var forbiddenMatches =
        javaSources()
            .flatMap(CoreBoundaryTest::forbiddenMatches)
            .map(Path::toString)
            .distinct()
            .toList();

    assertThat(forbiddenMatches).isEmpty();
  }

  private static Stream<Path> javaSources() throws IOException {
    try (var sourcePaths = Files.walk(CORE_MAIN)) {
      return sourcePaths.filter(path -> path.toString().endsWith(".java")).toList().stream();
    }
  }

  private static Stream<Path> forbiddenMatches(Path path) {
    try {
      var text = Files.readString(path, StandardCharsets.UTF_8);
      if (FORBIDDEN_TEXT.stream().anyMatch(text::contains)) {
        return Stream.of(path);
      }
      return Stream.empty();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + path, e);
    }
  }
}

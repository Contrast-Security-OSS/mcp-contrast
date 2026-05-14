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
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/FilterHelper.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/LoggingKeys.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/tool/base/ToolParams.java"),
          sourcePath(
              "com/contrast/labs/ai/mcp/contrast/tool/validation/ToolValidationContext.java"),
          sourcePath("com/contrast/labs/ai/mcp/contrast/hints/HintGenerator.java"));
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
  private static final List<String> LOCAL_ONLY_TEXT =
      List.of(
          "com.contrast.labs.ai.mcp.contrast.config",
          "ContrastSDKFactory",
          "SDKExtensionFactory",
          "SdkApiClient",
          "SDKHelper",
          "McpContrastApplication",
          "com.google.common.cache",
          "get_scan_results",
          "sarif",
          "SARIF");

  @Test
  void core_should_include_current_shared_support_types_and_no_local_only_types()
      throws IOException {
    var sourceFiles = javaSources().toList();

    assertThat(sourceFiles).as("core module must contain Java sources").isNotEmpty();
    assertThat(sourceFiles.stream().map(CORE_MAIN::relativize)).containsAll(REQUIRED_SUPPORT_TYPES);
    assertThat(sourceFiles)
        .extracting(path -> path.getFileName().toString())
        .doesNotContainAnyElementsOf(LOCAL_ONLY_TYPES);
  }

  @Test
  void core_should_not_reference_local_app_runtime_sdk_wiring_cache_or_sarif_tools()
      throws IOException {
    var localOnlyMatches =
        javaSources()
            .flatMap(CoreBoundaryTest::localOnlyMatches)
            .map(Path::toString)
            .distinct()
            .toList();

    assertThat(localOnlyMatches).isEmpty();
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

  private static Stream<Path> localOnlyMatches(Path path) {
    try {
      var text = Files.readString(path, StandardCharsets.UTF_8);
      if (LOCAL_ONLY_TEXT.stream().anyMatch(text::contains)) {
        return Stream.of(path);
      }
      return Stream.empty();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + path, e);
    }
  }
}

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class BuildCompatibilityTest {

  private static final Path ROOT_PROJECT = findRootProject();

  @Test
  void build_should_use_boot4_springAi11_and_validated_gradle8_wrapper() throws IOException {
    var gradleProperties = readProperties(ROOT_PROJECT.resolve("gradle.properties"));
    var wrapperProperties =
        readProperties(ROOT_PROJECT.resolve("gradle/wrapper/gradle-wrapper.properties"));

    assertThat(gradleProperties.getProperty("springBootVersion")).startsWith("4.");
    assertThat(gradleProperties.getProperty("springAiVersion")).startsWith("1.1.");
    assertThat(wrapperProperties.getProperty("distributionUrl"))
        .startsWith("https://services.gradle.org/distributions/gradle-8.")
        .endsWith("-bin.zip");
    assertThat(wrapperProperties.getProperty("distributionSha256Sum")).matches("[0-9a-f]{64}");
    assertThat(wrapperProperties).containsEntry("validateDistributionUrl", "true");
  }

  @Test
  void build_should_not_configure_discontinued_mockito_inline_artifact() throws IOException {
    var gradleProperties = readProperties(ROOT_PROJECT.resolve("gradle.properties"));
    var coreBuild = readString(ROOT_PROJECT.resolve("contrast-mcp-core/build.gradle"));
    var stdioBuild = readString(ROOT_PROJECT.resolve("contrast-mcp-stdio-app/build.gradle"));

    assertThat(gradleProperties).doesNotContainKey("mockitoInlineVersion");
    assertThat(coreBuild).doesNotContain("mockito-inline");
    assertThat(stdioBuild).doesNotContain("mockito-inline");
  }

  @Test
  void stdio_app_should_keep_boot4_jackson2_compatibility_shim() throws IOException {
    var coreBuild = readString(ROOT_PROJECT.resolve("contrast-mcp-core/build.gradle"));
    var stdioBuild = readString(ROOT_PROJECT.resolve("contrast-mcp-stdio-app/build.gradle"));

    assertThat(stdioBuild).contains("org.springframework.boot:spring-boot-jackson2");
    assertThat(coreBuild).doesNotContain("org.springframework.boot:spring-boot-jackson2");
  }

  private static Properties readProperties(Path path) throws IOException {
    assertThat(path).as("Expected %s to exist", path).isRegularFile();
    var properties = new Properties();
    try (var input = Files.newInputStream(path)) {
      properties.load(input);
    }
    return properties;
  }

  private static String readString(Path path) throws IOException {
    assertThat(path).as("Expected %s to exist", path).isRegularFile();
    return Files.readString(path);
  }

  private static Path findRootProject() {
    var current = Path.of("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isRegularFile(current.resolve("settings.gradle"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException(
        "Could not locate repository root by walking up from the current working directory.");
  }
}

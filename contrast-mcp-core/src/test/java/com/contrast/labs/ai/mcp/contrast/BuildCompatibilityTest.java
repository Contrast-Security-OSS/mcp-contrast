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

  private static final Path ROOT_PROJECT = Path.of("..").toAbsolutePath().normalize();

  @Test
  void build_should_pin_boot4_springAi117_and_gradle814() throws IOException {
    var gradleProperties = readProperties(ROOT_PROJECT.resolve("gradle.properties"));
    var wrapperProperties =
        readProperties(ROOT_PROJECT.resolve("gradle/wrapper/gradle-wrapper.properties"));

    assertThat(gradleProperties)
        .containsEntry("springBootVersion", "4.1.0")
        .containsEntry("springAiVersion", "1.1.7")
        .doesNotContainKey("mockitoInlineVersion");
    assertThat(wrapperProperties)
        .containsEntry(
            "distributionUrl", "https://services.gradle.org/distributions/gradle-8.14-bin.zip")
        .containsEntry(
            "distributionSha256Sum",
            "61ad310d3c7d3e5da131b76bbf22b5a4c0786e9d892dae8c1658d4b484de3caa");
  }

  @Test
  void build_should_use_bom_managed_mockito_core_not_discontinued_inline_artifact()
      throws IOException {
    assertThat(Files.readString(ROOT_PROJECT.resolve("contrast-mcp-core/build.gradle")))
        .contains("testImplementation 'org.mockito:mockito-core'")
        .doesNotContain("mockito-inline");
    assertThat(Files.readString(ROOT_PROJECT.resolve("contrast-mcp-stdio-app/build.gradle")))
        .contains("testImplementation 'org.mockito:mockito-core'")
        .doesNotContain("mockito-inline");
  }

  private static Properties readProperties(Path path) throws IOException {
    var properties = new Properties();
    try (var input = Files.newInputStream(path)) {
      properties.load(input);
    }
    return properties;
  }
}

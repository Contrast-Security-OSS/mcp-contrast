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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class LibraryExtendedTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializesLegacyJsonPropertyNames() throws Exception {
    var library = new LibraryExtended();
    library.setFilename("log4j-1.2.17.jar");
    library.setAppName("Demo Application");

    JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(library));

    assertThat(node.get("fileName").asText()).isEqualTo("log4j-1.2.17.jar");
    assertThat(node.get("app_name").asText()).isEqualTo("Demo Application");
    assertThat(node.has("appName")).isFalse();
  }
}

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
package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Verifies the MCP server instructions configured in application.properties bind to the same {@link
 * McpServerProperties} the Spring AI MCP server autoconfiguration consumes, so the instructions
 * field is emitted in the initialize result (AIML-942 server-instructions channel).
 */
class McpServerInstructionsTest {

  private McpServerProperties boundServerProperties() throws IOException {
    var properties =
        PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
    var source = new MapConfigurationPropertySource(properties);
    return new Binder(source)
        .bind("spring.ai.mcp.server", Bindable.of(McpServerProperties.class))
        .get();
  }

  @Test
  void applicationProperties_should_populate_server_instructions_when_bound_to_mcp_properties()
      throws IOException {
    assertThat(boundServerProperties().getInstructions()).isNotBlank();
  }

  @Test
  void instructions_should_carry_uniform_conventions_when_configured() throws IOException {
    var instructions = boundServerProperties().getInstructions();

    // The two catalog-wide conventions owned by this channel (mcp-4ubb): credential-scoped
    // visibility and comma-separated filter semantics. Their per-tool body copies are removed
    // in the same change, so losing either from the instructions would leave the fact homeless.
    assertThat(instructions)
        .contains("visible to the configured credentials")
        .contains("ORed")
        .contains("ANDed");
  }

  @Test
  void instructions_should_stay_repository_local_when_configured() throws IOException {
    var instructions = boundServerProperties().getInstructions();

    // Repository boundary (MCP_STANDARDS.md): the public server must never reference
    // hosted-only aiml-services tools, because it can be deployed without them.
    assertThat(instructions)
        .doesNotContain("search_issues")
        .doesNotContain("search_incidents")
        .doesNotContain("aiml");
  }
}

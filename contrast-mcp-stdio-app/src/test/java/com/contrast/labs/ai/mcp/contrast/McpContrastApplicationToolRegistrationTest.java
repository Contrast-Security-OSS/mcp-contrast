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
import static org.mockito.Mockito.mock;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.tool.server.SearchServersTool;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

class McpContrastApplicationToolRegistrationTest {

  @Test
  void toolsList_should_describe_public_searchServers_contract_without_wire_spellings() {
    ContrastApiClient client = mock();
    var callbacks = ToolCallbacks.from(new SearchServersTool(client));

    assertThat(callbacks)
        .singleElement()
        .satisfies(
            callback -> {
              var definition = callback.getToolDefinition();
              assertThat(definition.name()).isEqualTo("search_servers");
              assertThat(definition.inputSchema())
                  .contains(
                      "keyword",
                      "environments",
                      "quickFilter",
                      "applicationIds",
                      "includeApplications",
                      "sort")
                  .doesNotContain(
                      "withoutApplications", "applicationsIds", "\"q\"", "defend", "num_apps");
            });
  }

  @Test
  void toolsBean_should_explicitly_register_searchServersTool() throws Exception {
    var toolsMethod =
        Arrays.stream(McpContrastApplication.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("tools"))
            .findFirst()
            .orElseThrow();
    var tools =
        Arrays.stream(toolsMethod.getParameterTypes())
            .map(McpContrastApplicationToolRegistrationTest::instantiateTool)
            .toArray();

    @SuppressWarnings("unchecked")
    var callbacks = (List<ToolCallback>) toolsMethod.invoke(new McpContrastApplication(), tools);

    assertThat(callbacks)
        .extracting(callback -> callback.getToolDefinition().name())
        .contains("search_servers");
  }

  private static Object instantiateTool(Class<?> toolType) {
    try {
      Constructor<?> constructor =
          Arrays.stream(toolType.getDeclaredConstructors())
              .max(
                  (left, right) ->
                      Integer.compare(left.getParameterCount(), right.getParameterCount()))
              .orElseThrow();
      constructor.setAccessible(true);
      var dependencies =
          Arrays.stream(constructor.getParameterTypes()).map(type -> mock(type)).toArray();
      return constructor.newInstance(dependencies);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(
          "Could not instantiate " + toolType.getSimpleName(), exception);
    }
  }
}

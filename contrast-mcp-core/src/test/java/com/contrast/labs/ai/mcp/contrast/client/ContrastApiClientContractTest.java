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
package com.contrast.labs.ai.mcp.contrast.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServersResponse;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContrastApiClientContractTest {

  private static final Path CLIENT_SOURCE =
      Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/client/ContrastApiClient.java");
  private static final List<String> FORBIDDEN_AUTH_PARAMETER_TOKENS =
      List.of(
          "orgId",
          "organizationId",
          "organizationUuid",
          "bearer",
          "token",
          "apiKey",
          "serviceKey",
          "credential");
  private static final List<String> FORBIDDEN_RAW_SARIF_TOKENS =
      List.of("getScanResults", "get_scan_results", "SarifResult", "sarif");

  @Test
  void contrastApiClient_should_be_pure_interface_without_default_or_static_methods() {
    assertThat(ContrastApiClient.class.isInterface()).isTrue();
    assertThat(ContrastApiClient.class.getDeclaredMethods())
        .as("shared API boundary must not hide behavior in interface methods")
        .allSatisfy(
            method -> {
              assertThat(method.isDefault()).isFalse();
              assertThat(Modifier.isStatic(method.getModifiers())).isFalse();
            });
  }

  @Test
  void contrastApiClient_method_signatures_should_not_expose_auth_or_org_parameters()
      throws IOException {
    var source = Files.readString(CLIENT_SOURCE, StandardCharsets.UTF_8);

    assertThat(FORBIDDEN_AUTH_PARAMETER_TOKENS)
        .as("ContrastApiClient must resolve auth/org context inside each implementation")
        .allSatisfy(token -> assertThat(source).doesNotContain(token));
  }

  @Test
  void contrastApiClient_should_expose_scan_project_but_no_raw_sarif_path() throws IOException {
    var source = Files.readString(CLIENT_SOURCE, StandardCharsets.UTF_8);

    assertThat(ContrastApiClient.class.getDeclaredMethods())
        .extracting(method -> method.getName())
        .contains("getScanProject")
        .doesNotContain("getScanResults");
    assertThat(FORBIDDEN_RAW_SARIF_TOKENS)
        .as("raw SARIF retrieval must stay local-only and off the shared client boundary")
        .allSatisfy(token -> assertThat(source).doesNotContain(token));
  }

  @Test
  void contrastApiClient_should_expose_transport_neutral_server_search() {
    assertThat(ContrastApiClient.class.getDeclaredMethods())
        .filteredOn(method -> method.getName().equals("searchServers"))
        .singleElement()
        .satisfies(
            method -> {
              assertThat(method.getReturnType()).isEqualTo(ServersResponse.class);
              assertThat(method.getParameterTypes())
                  .containsExactly(
                      ServerFilterBody.class, int.class, int.class, String.class, boolean.class);
            });
  }
}

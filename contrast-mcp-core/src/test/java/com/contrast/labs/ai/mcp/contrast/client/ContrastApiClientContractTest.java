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
}

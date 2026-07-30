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
package com.contrast.labs.ai.mcp.contrast.tool.base;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponseEnvelopeSerializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void paginatedResponse_should_serialize_notices_without_legacy_warnings_field() throws Exception {
    var response =
        PaginatedToolResponse.success(
            List.of("item"), 1, 50, 1, false, List.of("Applied default"), 1L);

    assertNoticesWireShape(response);
  }

  @Test
  void singleResponse_should_serialize_notices_without_legacy_warnings_field() throws Exception {
    var response = SingleToolResponse.success("item", List.of("Optional enrichment unavailable"));

    assertNoticesWireShape(response);
  }

  @Test
  void cursorResponse_should_serialize_notices_without_legacy_warnings_field() throws Exception {
    var response =
        CursorToolResponse.success(
            List.of("item"), 50, "next-cursor", true, List.of("Applied default"), 1L);

    assertNoticesWireShape(response);
  }

  private void assertNoticesWireShape(Object response) throws Exception {
    var json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"notices\":[").doesNotContain("\"warnings\"");
  }
}

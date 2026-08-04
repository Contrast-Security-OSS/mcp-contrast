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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.util.json.JsonParser;

class ResponseEnvelopeSerializationTest {

  @Test
  void paginatedResponse_should_serialize_notices_without_legacy_warnings_field() throws Exception {
    var response =
        PaginatedToolResponse.success(
            List.of("item"), 1, 50, 1, false, List.of("Applied default"), 1L);

    assertNoticesWireShape(serialize(response));
  }

  @Test
  void singleResponse_should_serialize_notices_without_legacy_warnings_field() throws Exception {
    var response = SingleToolResponse.success("item", List.of("Optional enrichment unavailable"));

    assertNoticesWireShape(serialize(response));
  }

  @Test
  void cursorResponse_should_serialize_notices_without_legacy_warnings_field() throws Exception {
    var response =
        CursorToolResponse.success(
            List.of("item"), 50, "next-cursor", true, List.of("Applied default"), 1L);

    assertNoticesWireShape(serialize(response));
  }

  @Test
  void responseEnvelopes_should_serialize_empty_notices_arrays() throws Exception {
    assertEmptyNoticesWireShape(
        serialize(PaginatedToolResponse.success(List.of("item"), 1, 50, 1, false, List.of(), 1L)));
    assertEmptyNoticesWireShape(serialize(SingleToolResponse.success("item", List.of())));
    assertEmptyNoticesWireShape(
        serialize(CursorToolResponse.success(List.of("item"), 50, null, false, List.of(), 1L)));
  }

  private void assertNoticesWireShape(JsonNode json) {
    assertThat(json.has("notices")).isTrue();
    assertThat(json.path("notices").isArray()).isTrue();
    assertThat(json.has("warnings")).isFalse();
  }

  private void assertEmptyNoticesWireShape(JsonNode json) {
    assertNoticesWireShape(json);
    assertThat(json.path("notices").size()).isZero();
  }

  private JsonNode serialize(Object response) throws Exception {
    return parse(JsonParser.toJson(response));
  }

  private JsonNode parse(String json) throws Exception {
    return JsonParser.getObjectMapper().readTree(json);
  }
}

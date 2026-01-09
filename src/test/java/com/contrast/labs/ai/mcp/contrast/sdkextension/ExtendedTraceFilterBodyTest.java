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
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.ServerEnvironment;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.TraceMetadataFilter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExtendedTraceFilterBodyTest {

  @Test
  void status_field_should_be_settable_and_gettable() {
    var body = new ExtendedTraceFilterBody();
    body.setStatus(Set.of("Reported", "Confirmed"));

    assertThat(body.getStatus()).containsExactlyInAnyOrder("Reported", "Confirmed");
  }

  @Test
  void withSessionFilters_should_copy_all_base_filters() {
    // Given a fully populated source filter body
    var source = new TraceFilterBody();
    source.setSeverities(List.of(RuleSeverity.CRITICAL, RuleSeverity.HIGH));
    source.setVulnTypes(List.of("sql-injection", "xss-reflected"));
    source.setEnvironments(List.of(ServerEnvironment.PRODUCTION));
    var startDate = new Date();
    var endDate = new Date();
    source.setStartDate(startDate);
    source.setEndDate(endDate);
    source.setFilterTags(List.of("tag1", "tag2"));
    source.setTracked(true);
    source.setUntracked(false);

    // When creating extended filter with no session params
    var result = ExtendedTraceFilterBody.withSessionFilters(source, null, null);

    // Then all base filters should be copied
    assertThat(result.getSeverities()).containsExactly(RuleSeverity.CRITICAL, RuleSeverity.HIGH);
    assertThat(result.getVulnTypes()).containsExactly("sql-injection", "xss-reflected");
    assertThat(result.getEnvironments()).containsExactly(ServerEnvironment.PRODUCTION);
    assertThat(result.getStartDate()).isEqualTo(startDate);
    assertThat(result.getEndDate()).isEqualTo(endDate);
    assertThat(result.getFilterTags()).containsExactly("tag1", "tag2");
    assertThat(result.isTracked()).isTrue();
    assertThat(result.isUntracked()).isFalse();
  }

  @Test
  void withSessionFilters_should_handle_null_source_fields() {
    // Given a source with null fields
    var source = new TraceFilterBody();

    // When creating extended filter
    var result = ExtendedTraceFilterBody.withSessionFilters(source, null, null);

    // Then null fields should remain null
    assertThat(result.getSeverities()).isNull();
    assertThat(result.getVulnTypes()).isNull();
    assertThat(result.getEnvironments()).isNull();
    assertThat(result.getStartDate()).isNull();
    assertThat(result.getEndDate()).isNull();
    assertThat(result.getFilterTags()).isNull();
  }

  @Test
  void withSessionFilters_should_set_agent_session_id() {
    // Given
    var source = new TraceFilterBody();
    var sessionId = "agent-session-123";

    // When
    var result = ExtendedTraceFilterBody.withSessionFilters(source, sessionId, null);

    // Then
    assertThat(result.getAgentSessionId()).isEqualTo(sessionId);
  }

  @Test
  void withSessionFilters_should_set_single_metadata_filter() {
    // Given
    var source = new TraceFilterBody();
    var filter = new TraceMetadataFilter("87", "main");

    // When
    var result = ExtendedTraceFilterBody.withSessionFilters(source, null, List.of(filter));

    // Then
    assertThat(result.getMetadataFilters()).hasSize(1);
    assertThat(result.getMetadataFilters().get(0).getFieldID()).isEqualTo("87");
    assertThat(result.getMetadataFilters().get(0).getValues()).containsExactly("main");
  }

  @Test
  void withSessionFilters_should_set_multiple_metadata_filters() {
    // Given
    var source = new TraceFilterBody();
    var filters =
        List.of(
            new TraceMetadataFilter("87", "main"),
            new TraceMetadataFilter("88", List.of("Ellen", "Sam")));

    // When
    var result = ExtendedTraceFilterBody.withSessionFilters(source, null, filters);

    // Then
    assertThat(result.getMetadataFilters()).hasSize(2);
    assertThat(result.getMetadataFilters().get(0).getFieldID()).isEqualTo("87");
    assertThat(result.getMetadataFilters().get(0).getValues()).containsExactly("main");
    assertThat(result.getMetadataFilters().get(1).getFieldID()).isEqualTo("88");
    assertThat(result.getMetadataFilters().get(1).getValues()).containsExactly("Ellen", "Sam");
  }

  @Test
  void withSessionFilters_should_handle_null_metadata_filters() {
    // Given
    var source = new TraceFilterBody();

    // When
    var result = ExtendedTraceFilterBody.withSessionFilters(source, null, null);

    // Then
    assertThat(result.getMetadataFilters()).isNull();
  }

  @Test
  void withSessionFilters_should_handle_empty_metadata_filters() {
    // Given
    var source = new TraceFilterBody();

    // When
    var result = ExtendedTraceFilterBody.withSessionFilters(source, null, List.of());

    // Then
    assertThat(result.getMetadataFilters()).isNull();
  }

  @Test
  void withSessionFilters_should_set_all_session_params_together() {
    // Given
    var source = new TraceFilterBody();
    source.setSeverities(List.of(RuleSeverity.CRITICAL));
    var sessionId = "agent-session-456";
    var filters =
        List.of(
            new TraceMetadataFilter("89", "Sam"),
            new TraceMetadataFilter("90", List.of("feature-1", "feature-2")));

    // When
    var result = ExtendedTraceFilterBody.withSessionFilters(source, sessionId, filters);

    // Then all params should be set
    assertThat(result.getSeverities()).containsExactly(RuleSeverity.CRITICAL);
    assertThat(result.getAgentSessionId()).isEqualTo(sessionId);
    assertThat(result.getMetadataFilters()).hasSize(2);
    assertThat(result.getMetadataFilters().get(0).getFieldID()).isEqualTo("89");
    assertThat(result.getMetadataFilters().get(0).getValues()).containsExactly("Sam");
    assertThat(result.getMetadataFilters().get(1).getFieldID()).isEqualTo("90");
    assertThat(result.getMetadataFilters().get(1).getValues())
        .containsExactly("feature-1", "feature-2");
  }

  @Test
  void withSessionFilters_should_reject_null_source() {
    assertThatNullPointerException()
        .isThrownBy(() -> ExtendedTraceFilterBody.withSessionFilters(null, null, null))
        .withMessage("source TraceFilterBody must not be null");
  }
}

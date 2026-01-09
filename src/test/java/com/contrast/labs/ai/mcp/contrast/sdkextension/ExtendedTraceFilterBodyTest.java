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

import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.ServerEnvironment;
import com.contrastsecurity.models.TraceFilterBody;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExtendedTraceFilterBodyTest {

  @Test
  void fromWithSession_should_copy_all_base_filters() {
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
    var result = ExtendedTraceFilterBody.fromWithSession(source, null, null, null);

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
  void fromWithSession_should_handle_null_source_fields() {
    // Given a source with null fields
    var source = new TraceFilterBody();

    // When creating extended filter
    var result = ExtendedTraceFilterBody.fromWithSession(source, null, null, null);

    // Then null fields should remain null
    assertThat(result.getSeverities()).isNull();
    assertThat(result.getVulnTypes()).isNull();
    assertThat(result.getEnvironments()).isNull();
    assertThat(result.getStartDate()).isNull();
    assertThat(result.getEndDate()).isNull();
    assertThat(result.getFilterTags()).isNull();
  }

  @Test
  void fromWithSession_should_set_agent_session_id() {
    // Given
    var source = new TraceFilterBody();
    var sessionId = "agent-session-123";

    // When
    var result = ExtendedTraceFilterBody.fromWithSession(source, sessionId, null, null);

    // Then
    assertThat(result.getAgentSessionId()).isEqualTo(sessionId);
  }

  @Test
  void fromWithSession_should_set_metadata_filter_with_value() {
    // Given
    var source = new TraceFilterBody();
    var resolvedFieldId = "87";
    var metadataValue = "main";

    // When
    var result =
        ExtendedTraceFilterBody.fromWithSession(source, null, resolvedFieldId, metadataValue);

    // Then
    assertThat(result.getMetadataFilters()).hasSize(1);
    var filter = result.getMetadataFilters().get(0);
    assertThat(filter.getFieldID()).isEqualTo("87");
    assertThat(filter.getValues()).containsExactly("main");
  }

  @Test
  void fromWithSession_should_set_metadata_filter_without_value() {
    // Given
    var source = new TraceFilterBody();
    var resolvedFieldId = "88";

    // When creating filter with field ID but no value (match any value)
    var result = ExtendedTraceFilterBody.fromWithSession(source, null, resolvedFieldId, null);

    // Then filter should be set with empty values list
    assertThat(result.getMetadataFilters()).hasSize(1);
    var filter = result.getMetadataFilters().get(0);
    assertThat(filter.getFieldID()).isEqualTo("88");
    assertThat(filter.getValues()).isEmpty();
  }

  @Test
  void fromWithSession_should_not_set_metadata_filter_when_field_id_null() {
    // Given
    var source = new TraceFilterBody();

    // When
    var result = ExtendedTraceFilterBody.fromWithSession(source, null, null, "someValue");

    // Then no metadata filter should be set
    assertThat(result.getMetadataFilters()).isNull();
  }

  @Test
  void fromWithSession_should_not_set_metadata_filter_when_field_id_blank() {
    // Given
    var source = new TraceFilterBody();

    // When
    var result = ExtendedTraceFilterBody.fromWithSession(source, null, "  ", "someValue");

    // Then no metadata filter should be set
    assertThat(result.getMetadataFilters()).isNull();
  }

  @Test
  void fromWithSession_should_set_all_session_params_together() {
    // Given
    var source = new TraceFilterBody();
    source.setSeverities(List.of(RuleSeverity.CRITICAL));
    var sessionId = "agent-session-456";
    var resolvedFieldId = "89";
    var metadataValue = "Sam";

    // When
    var result =
        ExtendedTraceFilterBody.fromWithSession(source, sessionId, resolvedFieldId, metadataValue);

    // Then all params should be set
    assertThat(result.getSeverities()).containsExactly(RuleSeverity.CRITICAL);
    assertThat(result.getAgentSessionId()).isEqualTo(sessionId);
    assertThat(result.getMetadataFilters()).hasSize(1);
    assertThat(result.getMetadataFilters().get(0).getFieldID()).isEqualTo("89");
    assertThat(result.getMetadataFilters().get(0).getValues()).containsExactly("Sam");
  }
}

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
package com.contrast.labs.ai.mcp.contrast.tool.assess.params;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.sdkextension.ExtendedTraceFilterBody;
import com.contrast.labs.ai.mcp.contrast.tool.validation.UnresolvedMetadataFilter;
import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.ServerEnvironment;
import org.junit.jupiter.api.Test;

class SearchAppVulnerabilitiesParamsTest {

  private static final String VALID_APP_ID = "app-123";

  // -- appId validation tests --

  @Test
  void of_should_require_appId() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            null, null, null, null, null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).contains("appId is required");
  }

  @Test
  void of_should_reject_empty_appId() {
    var params =
        SearchAppVulnerabilitiesParams.of("", null, null, null, null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).contains("appId is required");
  }

  @Test
  void of_should_reject_whitespace_appId() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            "   ", null, null, null, null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).contains("appId is required");
  }

  @Test
  void of_should_accept_valid_appId() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.appId()).isEqualTo(VALID_APP_ID);
  }

  // -- Severity tests --

  @Test
  void of_should_parse_valid_severities() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, "CRITICAL,HIGH", null, null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getSeverities())
        .containsExactlyInAnyOrder(RuleSeverity.CRITICAL, RuleSeverity.HIGH);
  }

  @Test
  void of_should_reject_invalid_severity() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, "INVALID", null, null, null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid severities") && e.contains("INVALID"));
  }

  // -- Status tests --

  @Test
  void of_should_apply_default_statuses_with_warning() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getStatuses()).containsExactly("Reported", "Suspicious", "Confirmed");
    assertThat(params.warnings()).anyMatch(w -> w.contains("excluding Fixed and Remediated"));
  }

  @Test
  void of_should_parse_explicit_statuses() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, "Reported,Fixed", null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getStatuses()).containsExactly("Reported", "Fixed");
  }

  @Test
  void of_should_reject_invalid_status() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, "Invalid", null, null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid statuses") && e.contains("Invalid"));
  }

  // -- Environment tests --

  @Test
  void of_should_parse_valid_environments() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, "PRODUCTION,QA", null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getEnvironments())
        .containsExactlyInAnyOrder(ServerEnvironment.PRODUCTION, ServerEnvironment.QA);
  }

  // -- sessionMetadataFilters tests --

  @Test
  void of_should_parse_valid_sessionMetadataFilters() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, null, null, null, null, "{\"branch\":\"main\"}", null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getSessionMetadataFilters()).hasSize(1);
    assertThat(params.getSessionMetadataFilters().get(0).fieldName()).isEqualTo("branch");
    assertThat(params.getSessionMetadataFilters().get(0).values()).containsExactly("main");
  }

  @Test
  void of_should_parse_sessionMetadataFilters_with_multiple_fields() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{\"developer\":\"Ellen\",\"commit\":\"100\"}",
            null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getSessionMetadataFilters()).hasSize(2);
    // LinkedHashMap preserves order
    var filters = params.getSessionMetadataFilters();
    assertThat(filters.stream().map(UnresolvedMetadataFilter::fieldName))
        .containsExactly("developer", "commit");
  }

  @Test
  void of_should_parse_sessionMetadataFilters_with_array_values() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{\"developer\":[\"Ellen\",\"Sam\"]}",
            null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getSessionMetadataFilters()).hasSize(1);
    assertThat(params.getSessionMetadataFilters().get(0).fieldName()).isEqualTo("developer");
    assertThat(params.getSessionMetadataFilters().get(0).values()).containsExactly("Ellen", "Sam");
  }

  @Test
  void of_should_reject_invalid_json_for_sessionMetadataFilters() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, null, null, null, null, "not valid json", null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("Invalid JSON"));
  }

  @Test
  void of_should_reject_complex_objects_in_sessionMetadataFilters() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{\"nested\":{\"a\":\"b\"}}",
            null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("expected string or array"));
  }

  // -- useLatestSession tests --

  @Test
  void of_should_accept_useLatestSession_true() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, null, null, null, null, null, true);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getUseLatestSession()).isTrue();
  }

  @Test
  void of_should_accept_useLatestSession_false() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, null, null, null, null, null, false);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getUseLatestSession()).isFalse();
  }

  @Test
  void of_should_reject_useLatestSession_with_sessionMetadataFilters() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, null, null, null, null, "{\"branch\":\"main\"}", true);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(
            e ->
                e.contains("useLatestSession")
                    && e.contains("sessionMetadataFilters")
                    && e.contains("mutually exclusive"));
  }

  // -- toTraceFilterBody tests (POST endpoint) --

  @Test
  void toTraceFilterBody_should_map_all_fields() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID,
            "CRITICAL,HIGH",
            "Reported,Confirmed",
            "sql-injection",
            "PRODUCTION",
            "2025-01-01",
            "2025-01-31",
            "reviewed",
            null,
            null);

    var body = params.toTraceFilterBody();

    assertThat(body.getSeverities())
        .containsExactlyInAnyOrder(RuleSeverity.CRITICAL, RuleSeverity.HIGH);
    assertThat(body.getFilterTags()).containsExactly("reviewed");
    assertThat(body.getEnvironments()).containsExactly(ServerEnvironment.PRODUCTION);
    assertThat(body.getVulnTypes()).containsExactly("sql-injection");
    assertThat(body.getStartDate()).isNotNull();
    assertThat(body.getEndDate()).isNotNull();
    // tracked/untracked stay at defaults (false) = return all
    assertThat(body.isTracked()).isFalse();
    assertThat(body.isUntracked()).isFalse();
  }

  @Test
  void toTraceFilterBody_empty_should_return_all_vulnerabilities() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, null, null, null, null, null, null, null, null);

    var body = params.toTraceFilterBody();

    // With no filters and tracked/untracked both false, API returns ALL vulnerabilities
    assertThat(body.isTracked()).isFalse();
    assertThat(body.isUntracked()).isFalse();
  }

  @Test
  void toTraceFilterBody_should_return_ExtendedTraceFilterBody_with_status() {
    var params =
        SearchAppVulnerabilitiesParams.of(
            VALID_APP_ID, null, "Reported,Confirmed", null, null, null, null, null, null, null);

    var body = params.toTraceFilterBody();

    assertThat(body).isInstanceOf(ExtendedTraceFilterBody.class);
    assertThat(body.getStatus()).containsExactlyInAnyOrder("Reported", "Confirmed");
  }
}

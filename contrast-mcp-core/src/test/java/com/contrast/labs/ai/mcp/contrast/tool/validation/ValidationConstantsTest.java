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
package com.contrast.labs.ai.mcp.contrast.tool.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidationConstantsTest {

  @Test
  void pagination_constants_should_have_expected_values() {
    assertThat(ValidationConstants.DEFAULT_PAGE).isEqualTo(1);
    assertThat(ValidationConstants.DEFAULT_PAGE_SIZE).isEqualTo(50);
    assertThat(ValidationConstants.MAX_PAGE_SIZE).isEqualTo(100);
    assertThat(ValidationConstants.MIN_PAGE).isEqualTo(1);
  }

  @Test
  void valid_vuln_statuses_should_contain_expected_values() {
    assertThat(ValidationConstants.VALID_VULN_STATUSES)
        .containsExactlyInAnyOrder(
            "Reported",
            "Suspicious",
            "Confirmed",
            ValidationConstants.NOT_A_PROBLEM_VULN_STATUS,
            "Remediated",
            "Fixed",
            ValidationConstants.AUTO_REMEDIATED_VULN_STATUS);
  }

  @Test
  void vulnerability_status_collections_should_match_documentation_csv_constants() {
    assertThat(ValidationConstants.VALID_VULN_STATUSES)
        .containsExactlyInAnyOrder(ValidationConstants.VALID_VULN_STATUSES_CSV.split(","));
    assertThat(ValidationConstants.DEFAULT_VULN_STATUSES)
        .containsExactly(ValidationConstants.DEFAULT_VULN_STATUSES_CSV.split(","));
    assertThat(ValidationConstants.VALID_VULN_STATUSES)
        .containsAll(ValidationConstants.DEFAULT_VULN_STATUSES);
  }

  @Test
  void default_vuln_statuses_should_contain_actionable_only() {
    assertThat(ValidationConstants.DEFAULT_VULN_STATUSES)
        .containsExactly("Reported", "Suspicious", "Confirmed");

    // Actionable defaults omit terminal statuses.
    assertThat(ValidationConstants.DEFAULT_VULN_STATUSES)
        .doesNotContain(
            "Fixed",
            "Remediated",
            ValidationConstants.NOT_A_PROBLEM_VULN_STATUS,
            ValidationConstants.AUTO_REMEDIATED_VULN_STATUS);
  }

  @Test
  void valid_vuln_statuses_should_be_immutable() {
    assertThat(ValidationConstants.VALID_VULN_STATUSES).isUnmodifiable();
  }

  @Test
  void default_vuln_statuses_should_be_immutable() {
    assertThat(ValidationConstants.DEFAULT_VULN_STATUSES).isUnmodifiable();
  }
}

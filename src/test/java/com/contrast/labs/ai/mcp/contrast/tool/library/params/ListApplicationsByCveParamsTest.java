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
package com.contrast.labs.ai.mcp.contrast.tool.library.params;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ListApplicationsByCveParamsTest {

  @Test
  void of_should_accept_valid_cve_id() {
    var params = ListApplicationsByCveParams.of("CVE-2021-44228");

    assertThat(params.isValid()).isTrue();
    assertThat(params.cveId()).isEqualTo("CVE-2021-44228");
    assertThat(params.errors()).isEmpty();
    assertThat(params.warnings()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"CVE-2023-12345", "CVE-1999-0001", "CVE-2024-123456"})
  void of_should_accept_various_valid_cve_formats(String cveId) {
    var params = ListApplicationsByCveParams.of(cveId);

    assertThat(params.isValid()).isTrue();
    assertThat(params.cveId()).isEqualTo(cveId);
  }

  @Test
  void of_should_reject_null_cve_id() {
    var params = ListApplicationsByCveParams.of(null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("cveId") && e.contains("required"));
  }

  @Test
  void of_should_reject_empty_cve_id() {
    var params = ListApplicationsByCveParams.of("");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("cveId") && e.contains("required"));
  }

  @Test
  void of_should_reject_blank_cve_id() {
    var params = ListApplicationsByCveParams.of("   ");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("cveId") && e.contains("required"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"not-a-cve", "CVE2021-44228", "CVE-21-44228", "CVE-2021-123", "cve-2021-44228"})
  void of_should_reject_invalid_cve_format(String invalidCve) {
    var params = ListApplicationsByCveParams.of(invalidCve);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("cveId") && e.contains("CVE format"));
  }
}

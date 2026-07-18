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
package com.contrast.labs.ai.mcp.contrast.tool.server.params;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ServerFilterParamsTest {

  @Test
  void of_should_apply_silent_defaults_when_optional_values_are_absent() {
    var params = params(null, null, null, null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings()).isEmpty();
    assertThat(params.getQuickFilter()).isEqualTo("ALL");
    assertThat(params.getSort()).isEqualTo("-lastActivity");
    assertThat(params.isIncludeApplications()).isFalse();
    assertThat(params.isWithoutApplications()).isFalse();
  }

  @Test
  void of_should_parse_and_normalize_all_filter_lists() {
    var params =
        params(
            " api ",
            "development, QA",
            "online",
            "debug, warn",
            "blue, critical",
            "app-a, app-b",
            false,
            "1.2.3, 2.0.0",
            true,
            "name,ASC");
    var body = params.toServerFilterBody();

    assertThat(params.isValid()).isTrue();
    assertThat(body.getQ()).isEqualTo("api");
    assertThat(body.getServerEnvironments()).containsExactly("DEVELOPMENT", "QA");
    assertThat(body.getQuickFilter()).isEqualTo("ONLINE");
    assertThat(body.getLogLevels()).containsExactly("DEBUG", "WARN");
    assertThat(body.getTags()).containsExactly("blue", "critical");
    assertThat(body.getApplicationsIds()).containsExactly("app-a", "app-b");
    assertThat(body.getAgentVersions()).containsExactly("1.2.3", "2.0.0");
    assertThat(params.isIncludeApplications()).isTrue();
    assertThat(params.getSort()).isEqualTo("serverName");
  }

  @Test
  void toServerFilterBody_should_translate_withoutApplications_to_case_sensitive_wire_sentinel() {
    var params = params(null, null, null, null, null, null, true, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.toServerFilterBody().getApplicationsIds()).containsExactly("None");
  }

  @Test
  void of_should_reject_applicationIds_with_withoutApplications() {
    var params = params(null, null, null, null, null, "app-a", true, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .containsExactly(
            "applicationIds and withoutApplications are mutually exclusive: choose application"
                + " IDs or servers without applications, not both");
  }

  @ParameterizedTest
  @MethodSource("sortTranslations")
  void of_should_translate_public_sort_to_wire_sort(String publicSort, String wireSort) {
    var params = params(null, null, null, null, null, null, null, null, null, publicSort);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getSort()).isEqualTo(wireSort);
  }

  @ParameterizedTest
  @MethodSource("invalidSorts")
  void of_should_reject_invalid_sort_with_all_valid_options(String sort) {
    var params = params(null, null, null, null, null, null, null, null, null, sort);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .singleElement()
        .satisfies(
            error ->
                assertThat(error)
                    .contains(
                        "Expected format: property,DIRECTION",
                        "agentVersion",
                        "environment",
                        "lastActivity",
                        "name",
                        "ASC",
                        "DESC"));
  }

  @Test
  void of_should_reject_invalid_enum_filters_with_valid_values() {
    var params =
        params(
            null, "INVALID", "PARTIALLY_PROTECTED", "VERBOSE", null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anySatisfy(
            error ->
                assertThat(error)
                    .contains("Invalid environments", "DEVELOPMENT", "QA", "PRODUCTION"))
        .anySatisfy(
            error ->
                assertThat(error)
                    .contains("Invalid quickFilter", "ONLINE", "OFFLINE", "OUT_OF_DATE"))
        .anySatisfy(
            error -> assertThat(error).contains("Invalid logLevels", "ERROR", "WARN", "TRACE"));
  }

  private static Stream<Arguments> sortTranslations() {
    return Stream.of(
        Arguments.of("name,ASC", "serverName"),
        Arguments.of("name,DESC", "-serverName"),
        Arguments.of("environment,ASC", "environment"),
        Arguments.of("environment,DESC", "-environment"),
        Arguments.of("lastActivity,ASC", "lastActivity"),
        Arguments.of("lastActivity,DESC", "-lastActivity"),
        Arguments.of("agentVersion,ASC", "version"),
        Arguments.of(" agentVersion , desc ", "-version"));
  }

  private static Stream<String> invalidSorts() {
    return Stream.of("name", "-name", "name,DOWN", "name,DESC,environment", "NAME,ASC");
  }

  private static ServerFilterParams params(
      String keyword,
      String environments,
      String quickFilter,
      String logLevels,
      String tags,
      String applicationIds,
      Boolean withoutApplications,
      String agentVersions,
      Boolean includeApplications,
      String sort) {
    return ServerFilterParams.of(
        keyword,
        environments,
        quickFilter,
        logLevels,
        tags,
        applicationIds,
        withoutApplications,
        agentVersions,
        includeApplications,
        sort);
  }
}

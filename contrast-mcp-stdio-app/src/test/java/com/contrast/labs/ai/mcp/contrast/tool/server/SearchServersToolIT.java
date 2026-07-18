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
package com.contrast.labs.ai.mcp.contrast.tool.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.result.ServerSummary;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

/** Live contract tests for the TeamServer server-filter endpoint and MCP mapping. */
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class SearchServersToolIT {

  private static final int FULL_PAGE = 100;
  private static final int PAGINATION_PAGE_SIZE = 2;

  @Autowired private SearchServersTool searchServersTool;

  @Test
  void searchServers_should_return_eacScoped_server_summaries() {
    var response = allServers(1, FULL_PAGE, false);

    assertSuccess(response);
    assertThat(response.items())
        .as("requires EAC-visible seeded servers — see INTEGRATION_TESTS.md")
        .isNotEmpty()
        .allSatisfy(
            server -> {
              assertThat(server.serverId()).isPositive();
              assertThat(server.name()).isNotBlank();
              assertThat(server.status()).isIn("ONLINE", "OFFLINE");
              assertThat(server.applications()).isNull();
              assertThat(server.applicationCount()).isNotNegative();
            });
  }

  @Test
  void searchServers_should_filter_by_quickFilter() {
    var response = search(1, FULL_PAGE, null, null, "ONLINE", null, null, null, null, false, null);

    assertSuccess(response);
    assertThat(response.items())
        .as("requires seeded ONLINE servers — see INTEGRATION_TESTS.md")
        .isNotEmpty()
        .allSatisfy(server -> assertThat(server.status()).isEqualTo("ONLINE"));
  }

  @Test
  void searchServers_should_filter_by_environment() {
    var environment = requiredValue(ServerSummary::environment, "server environment");
    var response =
        search(1, FULL_PAGE, null, environment, null, null, null, null, null, false, null);

    assertSuccess(response);
    assertThat(response.items())
        .as("requires servers in environment %s", environment)
        .isNotEmpty()
        .allSatisfy(server -> assertThat(server.environment()).isEqualTo(environment));
  }

  @Test
  void searchServers_should_filter_by_logLevel() {
    var logLevel = requiredValue(ServerSummary::logLevel, "server log level");
    var response = search(1, FULL_PAGE, null, null, null, logLevel, null, null, null, false, null);

    assertSuccess(response);
    assertThat(response.items())
        .as("requires servers with log level %s", logLevel)
        .isNotEmpty()
        .allSatisfy(server -> assertThat(server.logLevel()).isEqualTo(logLevel));
  }

  @Test
  void searchServers_should_filter_by_tag() {
    var tag =
        seededServers().stream()
            .flatMap(server -> server.tags().stream())
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError("requires a seeded server tag — see INTEGRATION_TESTS.md"));
    var response = search(1, FULL_PAGE, null, null, null, null, tag, null, null, false, null);

    assertSuccess(response);
    assertThat(response.items())
        .as("requires servers tagged %s", tag)
        .isNotEmpty()
        .allSatisfy(server -> assertThat(server.tags()).contains(tag));
  }

  @Test
  void searchServers_should_filter_by_applicationId() {
    var expanded = allServers(1, FULL_PAGE, true);
    assertSuccess(expanded);
    var appId =
        expanded.items().stream()
            .flatMap(server -> server.applications().stream())
            .map(ServerSummary.ServerApplicationSummary::appId)
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError(
                        "requires a seeded server application — see INTEGRATION_TESTS.md"));
    var response = search(1, FULL_PAGE, null, null, null, null, null, appId, null, true, null);

    assertSuccess(response);
    assertThat(response.items())
        .as("requires servers hosting application %s", appId)
        .isNotEmpty()
        .allSatisfy(
            server ->
                assertThat(server.applications())
                    .extracting(ServerSummary.ServerApplicationSummary::appId)
                    .contains(appId));
  }

  @Test
  void searchServers_should_filter_by_agentVersion() {
    var agentVersion = requiredValue(ServerSummary::agentVersion, "server agent version");
    var response =
        search(1, FULL_PAGE, null, null, null, null, null, null, agentVersion, false, null);

    assertSuccess(response);
    assertThat(response.items())
        .as("requires servers with agent version %s", agentVersion)
        .isNotEmpty()
        .allSatisfy(server -> assertThat(server.agentVersion()).isEqualTo(agentVersion));
  }

  @Test
  void searchServers_should_filter_by_keyword_using_seeded_server_name() {
    var seeded = seededServers().getFirst();
    var keyword = seeded.name();
    var response = search(1, FULL_PAGE, keyword, null, null, null, null, null, null, false, null);

    assertSuccess(response);
    assertThat(response.items())
        .as("requires servers matching keyword %s", keyword)
        .isNotEmpty()
        .allSatisfy(
            server ->
                assertThat(searchableFields(server))
                    .as(
                        "server %s name, hostname, path, or tags should match keyword %s",
                        server.serverId(), keyword)
                    .anySatisfy(field -> assertThat(field).containsIgnoringCase(keyword)));
  }

  @Test
  void searchServers_should_include_applications_only_when_requested() {
    var withoutExpansion = allServers(1, FULL_PAGE, false);
    var withApplications = allServers(1, FULL_PAGE, true);

    assertSuccess(withoutExpansion);
    assertSuccess(withApplications);
    assertThat(withApplications.items())
        .as("requires EAC-visible seeded servers — see INTEGRATION_TESTS.md")
        .isNotEmpty()
        .allSatisfy(
            server -> {
              assertThat(server.applications()).isNotNull();
              assertThat((long) server.applications().size())
                  .as("expanded application count for server %s", server.serverId())
                  .isEqualTo(server.applicationCount());
            });
    assertThat(withApplications.items())
        .as("requires at least one seeded server application — see INTEGRATION_TESTS.md")
        .anySatisfy(server -> assertThat(server.applications()).isNotEmpty());
    assertThat(withoutExpansion.items())
        .isNotEmpty()
        .allSatisfy(server -> assertThat(server.applications()).isNull());
  }

  @Test
  void searchServers_should_or_values_within_one_filter() {
    var environments =
        seededServers().stream()
            .map(ServerSummary::environment)
            .filter(StringUtils::hasText)
            .distinct()
            .limit(PAGINATION_PAGE_SIZE)
            .toList();
    assertThat(environments)
        .as("requires seeded servers in at least two environments — see INTEGRATION_TESTS.md")
        .hasSize(PAGINATION_PAGE_SIZE);
    var response =
        search(
            1,
            FULL_PAGE,
            null,
            String.join(",", environments),
            null,
            null,
            null,
            null,
            null,
            false,
            null);

    assertSuccess(response);
    assertThat(response.items())
        .as("OR-within environments must return the selected environments")
        .isNotEmpty()
        .allSatisfy(server -> assertThat(server.environment()).isIn(environments));
  }

  @Test
  void searchServers_should_and_values_across_filters() {
    var seed =
        seededServers().stream()
            .filter(server -> StringUtils.hasText(server.environment()))
            .filter(server -> StringUtils.hasText(server.logLevel()))
            .filter(server -> StringUtils.hasText(server.agentVersion()))
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError(
                        "requires a seeded server with environment, log level, and agent version"));
    var response =
        search(
            1,
            FULL_PAGE,
            null,
            seed.environment(),
            null,
            seed.logLevel(),
            null,
            null,
            seed.agentVersion(),
            false,
            null);

    assertSuccess(response);
    assertThat(response.items())
        .as("AND-across filters must satisfy every supplied dimension")
        .isNotEmpty()
        .allSatisfy(
            server -> {
              assertThat(server.environment()).isEqualTo(seed.environment());
              assertThat(server.logLevel()).isEqualTo(seed.logLevel());
              assertThat(server.agentVersion()).isEqualTo(seed.agentVersion());
            });
  }

  @ParameterizedTest(name = "{0},{1}")
  @MethodSource("sortCases")
  void searchServers_should_sort_each_public_property_in_both_directions(
      String property, String direction) {
    var response =
        search(
            1,
            FULL_PAGE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            property + "," + direction);

    assertSuccess(response);
    var items = sortedSample(response, property, direction);
    assertThat(items)
        .as("requires seeded servers for sort %s,%s", property, direction)
        .isNotEmpty()
        .allSatisfy(server -> assertThat(sortValue(server, property)).isNotNull());
    assertThat(items.stream().map(server -> sortValue(server, property)).distinct())
        .as("sort %s requires at least two distinct values", property)
        .hasSizeGreaterThanOrEqualTo(PAGINATION_PAGE_SIZE);
    var comparator = sortComparator(property);
    if ("DESC".equals(direction)) {
      comparator = comparator.reversed();
    }
    assertThat(items).isSortedAccordingTo(comparator);
  }

  @Test
  void searchServers_should_return_disjoint_pages_with_consistent_count() {
    var first = allServers(1, PAGINATION_PAGE_SIZE, false);
    assertSuccess(first);
    assertThat(first.totalItems())
        .as("requires at least three seeded servers for pagination")
        .isGreaterThan(PAGINATION_PAGE_SIZE);
    var second = allServers(PAGINATION_PAGE_SIZE, PAGINATION_PAGE_SIZE, false);
    assertSuccess(second);

    var firstIds = first.items().stream().map(ServerSummary::serverId).collect(Collectors.toSet());
    var secondIds =
        second.items().stream().map(ServerSummary::serverId).collect(Collectors.toSet());
    assertThat(first.items()).hasSize(PAGINATION_PAGE_SIZE);
    assertThat(second.items())
        .hasSize(Math.min(PAGINATION_PAGE_SIZE, first.totalItems() - PAGINATION_PAGE_SIZE));
    assertThat(second.totalItems()).isEqualTo(first.totalItems());
    assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
  }

  @Test
  void searchServers_should_preserve_totalItems_on_empty_later_page() {
    var first = allServers(1, PAGINATION_PAGE_SIZE, false);
    assertSuccess(first);
    assertThat(first.totalItems())
        .as("requires seeded servers for out-of-range pagination")
        .isPositive();
    int outOfRangePage = first.totalItems() / PAGINATION_PAGE_SIZE + PAGINATION_PAGE_SIZE;
    var emptyPage = allServers(outOfRangePage, PAGINATION_PAGE_SIZE, false);

    assertSuccess(emptyPage);
    assertThat(emptyPage.items()).isEmpty();
    assertThat(emptyPage.totalItems()).isEqualTo(first.totalItems());
    assertThat(emptyPage.hasMorePages()).isFalse();
  }

  @Test
  void searchServers_should_return_full_validation_message_for_invalid_sort() {
    var response =
        search(1, FULL_PAGE, null, null, null, null, null, null, null, false, "invalid,DOWN");

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errors())
        .singleElement()
        .satisfies(
            error ->
                assertThat(error)
                    .contains(
                        "Invalid sort: 'invalid,DOWN'",
                        "Expected format: property,DIRECTION",
                        "agentVersion",
                        "environment",
                        "lastActivity",
                        "name",
                        "ASC",
                        "DESC"));
    assertThat(response.errors()).noneMatch(error -> error.contains("Contrast API error"));
  }

  @Test
  void searchServers_should_return_full_validation_messages_for_invalid_filters() {
    var response =
        search(
            1,
            FULL_PAGE,
            null,
            "STAGING",
            "PARTIALLY_PROTECTED",
            "VERBOSE",
            null,
            null,
            null,
            false,
            null);

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errors())
        .anySatisfy(
            error ->
                assertThat(error)
                    .contains("Invalid environments: 'STAGING'", "DEVELOPMENT", "QA", "PRODUCTION"))
        .anySatisfy(
            error ->
                assertThat(error)
                    .contains(
                        "Invalid quickFilter: 'PARTIALLY_PROTECTED'",
                        "ALL",
                        "ONLINE",
                        "OUT_OF_DATE"))
        .anySatisfy(
            error ->
                assertThat(error)
                    .contains("Invalid logLevels: 'VERBOSE'", "ERROR", "WARN", "TRACE"));
    assertThat(response.errors()).noneMatch(error -> error.contains("Contrast API error"));
  }

  private List<ServerSummary> seededServers() {
    var response = allServers(1, FULL_PAGE, true);
    assertSuccess(response);
    assertThat(response.items())
        .as("requires EAC-visible seeded servers — see INTEGRATION_TESTS.md")
        .isNotEmpty();
    return response.items();
  }

  private String requiredValue(Function<ServerSummary, String> extractor, String description) {
    return seededServers().stream()
        .map(extractor)
        .filter(StringUtils::hasText)
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "requires seeded " + description + " — see INTEGRATION_TESTS.md"));
  }

  private PaginatedToolResponse<ServerSummary> allServers(
      Integer page, Integer pageSize, Boolean includeApplications) {
    return search(
        page, pageSize, null, null, null, null, null, null, null, includeApplications, null);
  }

  private PaginatedToolResponse<ServerSummary> search(
      Integer page,
      Integer pageSize,
      String keyword,
      String environments,
      String quickFilter,
      String logLevels,
      String tags,
      String applicationIds,
      String agentVersions,
      Boolean includeApplications,
      String sort) {
    return searchServersTool.searchServers(
        page,
        pageSize,
        keyword,
        environments,
        quickFilter,
        logLevels,
        tags,
        applicationIds,
        agentVersions,
        includeApplications,
        sort);
  }

  private static void assertSuccess(PaginatedToolResponse<ServerSummary> response) {
    assertThat(response.errors()).as("server search errors").isEmpty();
    assertThat(response.isSuccess()).isTrue();
  }

  private static List<String> searchableFields(ServerSummary server) {
    return Stream.concat(
            Stream.of(server.name(), server.hostname(), server.path()), server.tags().stream())
        .filter(Objects::nonNull)
        .toList();
  }

  private static Object sortValue(ServerSummary server, String property) {
    return switch (property) {
      case "name" -> server.name();
      case "environment" -> server.environment();
      case "lastActivity" ->
          server.lastActivityAt() == null ? null : OffsetDateTime.parse(server.lastActivityAt());
      case "agentVersion" -> server.agentVersion();
      default -> throw new IllegalArgumentException("Unsupported sort property: " + property);
    };
  }

  private static Comparator<ServerSummary> sortComparator(String property) {
    return switch (property) {
      case "name" ->
          // TeamServer orders the raw serverName column using its case-insensitive schema
          // collation.
          Comparator.comparing(ServerSummary::name, String.CASE_INSENSITIVE_ORDER);
      case "environment" ->
          // Server.environment is persisted as EnumType.STRING, so the DAO order is lexical.
          Comparator.comparing(ServerSummary::environment);
      case "lastActivity" ->
          Comparator.comparing(server -> OffsetDateTime.parse(server.lastActivityAt()));
      case "agentVersion" -> Comparator.comparing(ServerSummary::agentVersion);
      default -> throw new IllegalArgumentException("Unsupported sort property: " + property);
    };
  }

  private List<ServerSummary> sortedSample(
      PaginatedToolResponse<ServerSummary> firstPage, String property, String direction) {
    var firstPageValues =
        firstPage.items().stream().map(server -> sortValue(server, property)).distinct().toList();
    if (firstPageValues.size() >= PAGINATION_PAGE_SIZE) {
      return firstPage.items();
    }

    assertThat(firstPage.totalItems())
        .as("sort %s requires enough seeded servers to reach a second value", property)
        .isGreaterThan(FULL_PAGE);
    int lastPageNumber = (firstPage.totalItems() - 1) / FULL_PAGE + 1;
    var lastPage =
        search(
            lastPageNumber,
            FULL_PAGE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            property + "," + direction);
    assertSuccess(lastPage);
    return Stream.concat(firstPage.items().stream(), lastPage.items().stream()).toList();
  }

  private static Stream<Arguments> sortCases() {
    return Stream.of("name", "environment", "lastActivity", "agentVersion")
        .flatMap(
            property -> Stream.of(Arguments.of(property, "ASC"), Arguments.of(property, "DESC")));
  }
}

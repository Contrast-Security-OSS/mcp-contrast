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

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerFilterBody;
import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import com.contrastsecurity.http.ServerEnvironment;
import com.contrastsecurity.http.ServerFilterForm.ServerQuickFilterType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/** Validated public parameters and wire translation for server searches. */
@Getter
public class ServerFilterParams extends BaseToolParams {

  private static final Map<String, String> SORT_FIELDS =
      Map.of(
          "name", "serverName",
          "environment", "environment",
          "lastActivity", "lastActivity",
          "agentVersion", "version");

  public static final Set<String> VALID_ENVIRONMENTS =
      Arrays.stream(ServerEnvironment.values())
          .map(Enum::name)
          .collect(Collectors.toUnmodifiableSet());
  public static final Set<String> VALID_QUICK_FILTERS =
      Arrays.stream(ServerQuickFilterType.values())
          .map(Enum::name)
          .collect(Collectors.toUnmodifiableSet());
  public static final Set<String> VALID_LOG_LEVELS =
      Set.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE");
  public static final Set<String> VALID_SORT_FIELDS = Set.copyOf(SORT_FIELDS.keySet());

  private static final String DEFAULT_QUICK_FILTER = ServerQuickFilterType.ALL.name();
  private static final String DEFAULT_SORT = "-lastActivity";
  private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("ASC", "DESC");

  private String keyword;
  private List<String> environments;
  private String quickFilter;
  private List<String> logLevels;
  private List<String> tags;
  private List<String> applicationIds;
  private List<String> agentVersions;
  private boolean includeApplications;
  @Nullable private String sort;

  private ServerFilterParams() {}

  /** Parses and validates the public MCP parameters. */
  public static ServerFilterParams of(
      String keyword,
      String environments,
      String quickFilter,
      String logLevels,
      String tags,
      String applicationIds,
      String agentVersions,
      Boolean includeApplications,
      String sort) {
    var params = new ServerFilterParams();
    var ctx = new ToolValidationContext();

    params.keyword = ctx.stringParam(keyword, "keyword").get();
    params.environments =
        ctx.stringListParam(environments, "environments")
            .toUpperCase()
            .allowedValues(VALID_ENVIRONMENTS)
            .get();
    params.quickFilter =
        ctx.stringParam(quickFilter, "quickFilter")
            .toUpperCase()
            .allowedValues(VALID_QUICK_FILTERS)
            .get();
    if (!StringUtils.hasText(quickFilter)) {
      params.quickFilter = DEFAULT_QUICK_FILTER;
    }
    params.logLevels =
        ctx.stringListParam(logLevels, "logLevels")
            .toUpperCase()
            .allowedValues(VALID_LOG_LEVELS)
            .get();
    params.tags = ctx.stringListParam(tags, "tags").get();
    params.applicationIds = ctx.stringListParam(applicationIds, "applicationIds").get();
    params.agentVersions = ctx.stringListParam(agentVersions, "agentVersions").get();
    params.includeApplications = Boolean.TRUE.equals(includeApplications);
    params.sort = parseSort(ctx, sort);

    params.setValidationResult(ctx);
    return params;
  }

  // TODO: Share sort parsing with AttackFilterParams while preserving its case-sensitive contract.
  private static String parseSort(@NonNull ToolValidationContext ctx, @Nullable String sort) {
    if (!StringUtils.hasText(sort)) {
      return DEFAULT_SORT;
    }

    var trimmedSort = sort.trim();
    var parts = Arrays.stream(trimmedSort.split(",", -1)).map(String::trim).toList();
    if (parts.size() != 2) {
      ctx.errorIf(true, sortError(trimmedSort));
      return DEFAULT_SORT;
    }

    var property =
        SORT_FIELDS.keySet().stream()
            .filter(validProperty -> validProperty.equalsIgnoreCase(parts.getFirst()))
            .findFirst();
    var direction = parts.get(1).toUpperCase(Locale.ROOT);
    var valid = property.isPresent() && VALID_SORT_DIRECTIONS.contains(direction);
    if (!valid) {
      ctx.errorIf(true, sortError(trimmedSort));
      return DEFAULT_SORT;
    }

    var wireProperty = SORT_FIELDS.get(property.orElseThrow());
    return "DESC".equals(direction) ? "-" + wireProperty : wireProperty;
  }

  private static String sortError(String sort) {
    return String.format(
        "Invalid sort: '%s'. Expected format: property,DIRECTION. Valid properties: %s. Valid"
            + " directions: %s.",
        sort,
        VALID_SORT_FIELDS.stream().sorted().collect(Collectors.joining(", ")),
        VALID_SORT_DIRECTIONS.stream().sorted().collect(Collectors.joining(", ")));
  }

  /** Converts public parameters to the TeamServer wire request. */
  public ServerFilterBody toServerFilterBody() {
    return ServerFilterBody.builder()
        .applicationsIds(applicationIds)
        .logLevels(logLevels)
        .tags(tags)
        .agentVersions(agentVersions)
        .serverEnvironments(environments)
        .q(keyword)
        .quickFilter(quickFilter)
        .build();
  }
}

/*
 * Copyright 2026 Contrast Security
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
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolSortParser;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import com.contrastsecurity.http.ServerEnvironment;
import com.contrastsecurity.http.ServerFilterForm.ServerQuickFilterType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.lang.Nullable;

/** Validated public parameters and wire translation for server searches. */
@Getter
public class ServerFilterParams extends BaseToolParams {

  private static final String LAST_ACTIVITY_SORT_FIELD = "lastActivity";
  private static final Map<String, String> SORT_FIELDS =
      Map.of(
          "name",
          "serverName",
          "environment",
          "environment",
          LAST_ACTIVITY_SORT_FIELD,
          LAST_ACTIVITY_SORT_FIELD,
          "agentVersion",
          "version");

  public static final Set<String> VALID_ENVIRONMENTS =
      Arrays.stream(ServerEnvironment.values())
          .map(Enum::name)
          .collect(Collectors.toUnmodifiableSet());
  public static final Set<String> VALID_QUICK_FILTERS =
      Arrays.stream(ServerQuickFilterType.values())
          .map(Enum::name)
          .collect(Collectors.toUnmodifiableSet());
  // Unlike environments and quick filters, the SDK exposes no enum for agent log levels, so this
  // allow-list is maintained by hand and must track the levels TeamServer/agents actually accept.
  public static final Set<String> VALID_LOG_LEVELS =
      Set.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE");

  private static final String DEFAULT_QUICK_FILTER = ServerQuickFilterType.ALL.name();
  private static final String DEFAULT_SORT =
      "-" + Objects.requireNonNull(SORT_FIELDS.get(LAST_ACTIVITY_SORT_FIELD));

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
            .defaultToQuietly(DEFAULT_QUICK_FILTER)
            .allowedValues(VALID_QUICK_FILTERS)
            .get();
    params.logLevels =
        ctx.stringListParam(logLevels, "logLevels")
            .toUpperCase()
            .allowedValues(VALID_LOG_LEVELS)
            .get();
    params.tags = ctx.stringListParam(tags, "tags").get();
    params.applicationIds = ctx.stringListParam(applicationIds, "applicationIds").get();
    params.agentVersions = ctx.stringListParam(agentVersions, "agentVersions").get();
    params.includeApplications = Boolean.TRUE.equals(includeApplications);
    params.sort = ToolSortParser.parse(ctx, sort, SORT_FIELDS, false, DEFAULT_SORT);

    params.setValidationResult(ctx);
    return params;
  }

  /** Converts public parameters to the TeamServer wire request. */
  public ServerFilterBody toServerFilterBody() {
    // The field renames below are TeamServer's server-filter wire spellings: keyword -> q,
    // applicationIds -> applicationsIds, environments -> serverEnvironments. Keeping the
    // translation here isolates those wire names from the public MCP contract.
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

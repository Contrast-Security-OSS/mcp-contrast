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
package com.contrast.labs.ai.mcp.contrast.result;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerDetail;
import com.contrast.labs.ai.mcp.contrast.tool.base.FilterHelper;
import java.util.List;
import java.util.Optional;
import org.springframework.util.StringUtils;

/** AI-friendly server inventory, health, and Protect coverage summary. */
public record ServerSummary(
    long serverId,
    String name,
    String hostname,
    String path,
    String serverType,
    String environment,
    String status,
    String language,
    String agentVersion,
    String latestAgentVersion,
    Boolean agentOutOfDate,
    boolean assessEnabled,
    boolean assessPending,
    Boolean protectEnabled,
    String logLevel,
    List<String> tags,
    long applicationCount,
    List<ServerApplicationSummary> applications,
    String lastActivityAt) {

  // TeamServer emits the literal "NA" (ServerService.NO_VERSION_AVAILABLE) for latest_agent_version
  // when it cannot determine the newest agent for a server's language. Treat it as unknown, not a
  // real version, so agentOutOfDate resolves to null instead of a misleading true/false.
  private static final String UNKNOWN_LATEST_VERSION = "NA";

  /** Maps a TeamServer wire DTO to the public MCP contract. */
  public static ServerSummary fromServer(ServerDetail server, boolean includeApplications) {
    var wireApplications = Optional.ofNullable(server.getApplications()).orElse(List.of());
    var applications =
        includeApplications
            ? wireApplications.stream()
                .map(ServerApplicationSummary::fromServerApplication)
                .toList()
            : null;
    long applicationCount =
        Optional.ofNullable(server.getApplicationCount()).orElse((long) wireApplications.size());

    // Protect/Assess null semantics, verified against TeamServer's ServerResourceFactory.
    // TeamServer fills `defend` only for Protect-entitled orgs, then a post-build step overwrites
    // it from the first visible application's effective instrumentation config. A non-null
    // protectEnabled is therefore effective state that may reflect one app, not a server-wide
    // aggregate; null means unknown/unavailable. Never coerce null to false: that would answer
    // "is Protect on?" with a confident, wrong "no". `defend` stays a nullable Boolean for this
    // reason, while `assess`/`assessPending` are primitive because TeamServer always projects them.
    return new ServerSummary(
        Optional.ofNullable(server.getServerId()).orElse(0L),
        server.getName(),
        server.getHostname(),
        server.getServerPath(),
        server.getServerType(),
        server.getEnvironment(),
        server.getStatus(),
        server.getLanguage(),
        server.getAgentVersion(),
        server.getLatestAgentVersion(),
        mapOutOfDate(server),
        server.isAssess(),
        server.isAssessPending(),
        server.getDefend(),
        server.getLogLevel(),
        Optional.ofNullable(server.getTags()).orElse(List.of()),
        applicationCount,
        applications,
        // The server contract retains backend millisecond precision for activity sorting.
        FilterHelper.formatTimestampWithMillis(server.getLastActivity()));
  }

  private static Boolean mapOutOfDate(ServerDetail server) {
    var latestVersion = server.getLatestAgentVersion();
    if (!StringUtils.hasText(latestVersion) || UNKNOWN_LATEST_VERSION.equals(latestVersion)) {
      return null;
    }
    return server.isOutOfDate();
  }

  /** Application identity returned only when includeApplications is requested. */
  public record ServerApplicationSummary(String appId, String name, String language) {

    private static ServerApplicationSummary fromServerApplication(
        ServerDetail.ServerApplicationDetail application) {
      return new ServerApplicationSummary(
          application.getAppId(), application.getName(), application.getLanguage());
    }
  }
}

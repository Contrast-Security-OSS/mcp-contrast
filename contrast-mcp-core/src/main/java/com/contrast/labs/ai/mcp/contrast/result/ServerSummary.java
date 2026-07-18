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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    // TeamServer ServerJpaDao's filter projection mirrors defend into this pending-state slot.
    Boolean protectPending,
    String logLevel,
    List<String> tags,
    long applicationCount,
    List<ServerApplicationSummary> applications,
    String lastActivityAt) {

  private static final String UNKNOWN_LATEST_VERSION = "NA";
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

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
        server.getDefendPending(),
        server.getLogLevel(),
        Optional.ofNullable(server.getTags()).orElse(List.of()),
        applicationCount,
        applications,
        formatTimestamp(server.getLastActivity()));
  }

  private static Boolean mapOutOfDate(ServerDetail server) {
    var latestVersion = server.getLatestAgentVersion();
    if (!StringUtils.hasText(latestVersion) || UNKNOWN_LATEST_VERSION.equals(latestVersion)) {
      return null;
    }
    return server.isOutOfDate();
  }

  private static String formatTimestamp(Long epochMillis) {
    if (epochMillis == null) {
      return null;
    }
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(TIMESTAMP_FORMATTER);
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

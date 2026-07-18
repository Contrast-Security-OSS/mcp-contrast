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

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServersResponse;
import com.google.gson.Gson;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ServerSummaryTest {

  private final Gson gson = new Gson();

  @Test
  void fromServer_should_preserve_null_protect_and_unknown_latest_agent_semantics() {
    var response =
        parse(
            """
            {
              "success": true,
              "count": 1,
              "servers": [{
                "server_id": 42,
                "name": "prod-1",
                "latest_agent_version": "NA",
                "out_of_date": true,
                "assess": true,
                "assessPending": false,
                "num_apps": 0,
                "last_activity": 1712345678123
              }]
            }
            """);

    var summary = ServerSummary.fromServer(response.getServers().getFirst(), false);

    assertThat(summary.serverId()).isEqualTo(42L);
    assertThat(summary.name()).isEqualTo("prod-1");
    assertThat(summary.protectEnabled()).isNull();
    assertThat(summary.agentOutOfDate()).isNull();
    assertThat(summary.assessEnabled()).isTrue();
    assertThat(summary.assessPending()).isFalse();
    assertThat(summary.applicationCount()).isZero();
    assertThat(summary.applications()).isNull();
    assertThat(summary.lastActivityAt()).isEqualTo("2024-04-05T19:34:38.123+00:00");
    assertThat(OffsetDateTime.parse(summary.lastActivityAt()).toInstant().toEpochMilli())
        .isEqualTo(1712345678123L);
  }

  @Test
  void fromServer_should_map_wire_identity_and_version_fields() {
    var response =
        parse(
            """
            {
              "success": true,
              "count": 1,
              "servers": [{
                "server_id": 11,
                "name": "prod-11",
                "hostname": "host-11.example.test",
                "path": "/opt/contrast/server-11",
                "type": "TOMCAT",
                "environment": "PRODUCTION",
                "status": "ONLINE",
                "language": "JAVA",
                "agent_version": "5.0.0",
                "latest_agent_version": "5.1.0"
              }]
            }
            """);

    var summary = ServerSummary.fromServer(response.getServers().getFirst(), false);

    assertThat(summary.serverId()).isEqualTo(11L);
    assertThat(summary.name()).isEqualTo("prod-11");
    assertThat(summary.hostname()).isEqualTo("host-11.example.test");
    assertThat(summary.path()).isEqualTo("/opt/contrast/server-11");
    assertThat(summary.serverType()).isEqualTo("TOMCAT");
    assertThat(summary.environment()).isEqualTo("PRODUCTION");
    assertThat(summary.status()).isEqualTo("ONLINE");
    assertThat(summary.language()).isEqualTo("JAVA");
    assertThat(summary.agentVersion()).isEqualTo("5.0.0");
    assertThat(summary.latestAgentVersion()).isEqualTo("5.1.0");
    assertThat(summary.lastActivityAt()).isNull();
  }

  @Test
  void fromServer_should_preserve_first_visible_application_effective_configuration_shape() {
    var response =
        parse(
            """
            {
              "success": true,
              "count": 1,
              "servers": [{
                "server_id": 7,
                "name": "qa-1",
                "agent_version": "5.0.0",
                "latest_agent_version": "5.1.0",
                "out_of_date": true,
                "assess": false,
                "assessPending": true,
                "defend": true,
                "defendPending": false,
                "num_apps": 1,
                "applications": [{
                  "app_id": "app-first-visible",
                  "name": "checkout",
                  "language": "JAVA"
                }]
              }]
            }
            """);

    var summary = ServerSummary.fromServer(response.getServers().getFirst(), true);

    assertThat(summary.assessEnabled()).isFalse();
    assertThat(summary.assessPending()).isTrue();
    assertThat(summary.protectEnabled()).isTrue();
    assertThat(summary.agentOutOfDate()).isTrue();
    assertThat(summary.applicationCount()).isEqualTo(1L);
    assertThat(summary.applications())
        .containsExactly(
            new ServerSummary.ServerApplicationSummary("app-first-visible", "checkout", "JAVA"));
  }

  @Test
  void fromServer_should_return_unknown_outOfDate_when_latest_agent_version_is_absent() {
    var response =
        parse(
            """
            {
              "success": true,
              "count": 1,
              "servers": [{
                "server_id": 8,
                "out_of_date": false
              }]
            }
            """);

    var summary = ServerSummary.fromServer(response.getServers().getFirst(), false);

    assertThat(summary.agentOutOfDate()).isNull();
  }

  @Test
  void fromServer_should_fall_back_to_application_list_size_when_count_is_absent() {
    var response =
        parse(
            """
            {
              "success": true,
              "count": 1,
              "servers": [{
                "server_id": 8,
                "applications": [
                  {"app_id": "app-a", "name": "checkout", "language": "JAVA"},
                  {"app_id": "app-b", "name": "catalog", "language": "JAVA"}
                ]
              }]
            }
            """);

    var summary = ServerSummary.fromServer(response.getServers().getFirst(), true);

    assertThat(summary.applicationCount()).isEqualTo(2L);
    assertThat(summary.applications())
        .extracting(ServerSummary.ServerApplicationSummary::appId)
        .containsExactly("app-a", "app-b");
  }

  @Test
  void fromServer_should_prefer_application_count_over_application_list_size() {
    var response =
        parse(
            """
            {
              "success": true,
              "count": 1,
              "servers": [{
                "server_id": 9,
                "num_apps": 3,
                "applications": [
                  {"app_id": "app-a", "name": "checkout", "language": "JAVA"}
                ]
              }]
            }
            """);

    var summary = ServerSummary.fromServer(response.getServers().getFirst(), false);

    assertThat(summary.applicationCount()).isEqualTo(3L);
    assertThat(summary.applications()).isNull();
  }

  private ServersResponse parse(String json) {
    return gson.fromJson(json, ServersResponse.class);
  }
}

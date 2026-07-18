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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data.server;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

/** Gson DTO for the server-filter response. Wire names remain isolated from MCP results. */
@Data
public class ServerDetail {

  @SerializedName("server_id")
  private Long serverId;

  private String name;
  private String hostname;

  @SerializedName("path")
  private String serverPath;

  @SerializedName("type")
  private String serverType;

  private String environment;
  private String status;
  private String language;

  @SerializedName("agent_version")
  private String agentVersion;

  @SerializedName("latest_agent_version")
  private String latestAgentVersion;

  @SerializedName("out_of_date")
  private boolean outOfDate;

  private boolean assess;
  private boolean assessPending;
  private Boolean defend;

  // Intentionally not mapped to the MCP contract: TeamServer's server-filter projection
  // (ServerJpaDao.findServersByFilters) selects defend into this slot, so it deterministically
  // mirrors defend rather than the real pending-restart flag. Filed as TS-43252 (previously
  // reported in TS-20210, never fixed). Do not expose it until that projection defect is fixed.
  private Boolean defendPending;

  private String logLevel;
  private List<String> tags;

  @SerializedName("num_apps")
  private Long applicationCount;

  private List<ServerApplicationDetail> applications;

  @SerializedName("last_activity")
  private Long lastActivity;

  /** Gson DTO for applications expanded on a server response. */
  @Data
  public static class ServerApplicationDetail {

    @SerializedName("app_id")
    private String appId;

    private String name;
    private String language;
  }
}

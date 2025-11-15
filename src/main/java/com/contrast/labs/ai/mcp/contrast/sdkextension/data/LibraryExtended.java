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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data;

import com.contrastsecurity.models.Application;
import com.contrastsecurity.models.Server;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

/** An application library. */
@Data
public class LibraryExtended {

  @SerializedName("file_name")
  private String filename;

  private String version;

  @SerializedName("apps")
  private List<Application> applications;

  private List<Server> servers;

  @SerializedName("vulns")
  private List<LibraryVulnerabilityExtended> vulnerabilities;

  @SerializedName("class_count")
  private int classCount;

  @SerializedName("classes_used")
  private int classedUsed;

  private String manifest;

  @SerializedName("library_id")
  private long libraryId;

  private String grade;
  private String hash;
  private String group;

  @SerializedName("file_version")
  private String fileVersion;

  @SerializedName("app_id")
  private String appId;

  @SerializedName("app_name")
  private String appName;

  @SerializedName("app_context_path")
  private String appContextPath;

  @SerializedName("app_language")
  private String appLanguage;

  @SerializedName("latest_version")
  private String latestVersion;

  @SerializedName("release_date")
  private long releaseDate;

  @SerializedName("latest_release_date")
  private long latestReleaseDate;

  @SerializedName("total_vulnerabilities")
  private int totalVulnerabilities;

  @SerializedName("high_vulnerabilities")
  private int highVulnerabilities;

  private boolean custom;

  @SerializedName("lib_score")
  private double libScore;

  @SerializedName("months_outdated")
  private int monthsOutdated;
}

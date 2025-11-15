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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr;

import lombok.Data;

/** Class representing the server information in an attack event. */
@Data
public class Server {
  private String environment;
  private String status;
  private boolean noPending;
  private Object tags;
  private String logLevel;
  private Object logPath;
  private boolean assess;
  private boolean assessPending;
  private boolean defend;
  private boolean defendPending;
  private boolean logEnhancerPending;
  private String language;
  private long server_id;
  private long last_startup;
  private long last_activity;
  private long first_activity;
  private String name;
  private String hostname;
  private String path;
  private String type;
  private String agent_version;
  private boolean out_of_date;
  private String latest_agent_version;
  private boolean assess_sensors;
  private long assess_last_update;
  private boolean defend_sensors;
  private long defense_last_update;
  private boolean diagnostic_collection_enabled;
  private boolean syslog_enabled;
  private Object is_assess_status_locked;
  private Object is_protect_status_locked;
  private String config_source_assess;
  private String config_source_protect;
  private String protect_enable_property_name;
  private String assess_enable_property_name;
  private String protect_enable_location;
  private String assess_enable_location;
}

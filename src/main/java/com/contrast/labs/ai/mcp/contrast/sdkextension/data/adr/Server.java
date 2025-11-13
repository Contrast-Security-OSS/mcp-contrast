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

/** Class representing the server information in an attack event. */
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

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isNoPending() {
    return noPending;
  }

  public void setNoPending(boolean noPending) {
    this.noPending = noPending;
  }

  public Object getTags() {
    return tags;
  }

  public void setTags(Object tags) {
    this.tags = tags;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public Object getLogPath() {
    return logPath;
  }

  public void setLogPath(Object logPath) {
    this.logPath = logPath;
  }

  public boolean isAssess() {
    return assess;
  }

  public void setAssess(boolean assess) {
    this.assess = assess;
  }

  public boolean isAssessPending() {
    return assessPending;
  }

  public void setAssessPending(boolean assessPending) {
    this.assessPending = assessPending;
  }

  public boolean isDefend() {
    return defend;
  }

  public void setDefend(boolean defend) {
    this.defend = defend;
  }

  public boolean isDefendPending() {
    return defendPending;
  }

  public void setDefendPending(boolean defendPending) {
    this.defendPending = defendPending;
  }

  public boolean isLogEnhancerPending() {
    return logEnhancerPending;
  }

  public void setLogEnhancerPending(boolean logEnhancerPending) {
    this.logEnhancerPending = logEnhancerPending;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public long getServer_id() {
    return server_id;
  }

  public void setServer_id(long server_id) {
    this.server_id = server_id;
  }

  public long getLast_startup() {
    return last_startup;
  }

  public void setLast_startup(long last_startup) {
    this.last_startup = last_startup;
  }

  public long getLast_activity() {
    return last_activity;
  }

  public void setLast_activity(long last_activity) {
    this.last_activity = last_activity;
  }

  public long getFirst_activity() {
    return first_activity;
  }

  public void setFirst_activity(long first_activity) {
    this.first_activity = first_activity;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAgent_version() {
    return agent_version;
  }

  public void setAgent_version(String agent_version) {
    this.agent_version = agent_version;
  }

  public boolean isOut_of_date() {
    return out_of_date;
  }

  public void setOut_of_date(boolean out_of_date) {
    this.out_of_date = out_of_date;
  }

  public String getLatest_agent_version() {
    return latest_agent_version;
  }

  public void setLatest_agent_version(String latest_agent_version) {
    this.latest_agent_version = latest_agent_version;
  }

  public boolean isAssess_sensors() {
    return assess_sensors;
  }

  public void setAssess_sensors(boolean assess_sensors) {
    this.assess_sensors = assess_sensors;
  }

  public long getAssess_last_update() {
    return assess_last_update;
  }

  public void setAssess_last_update(long assess_last_update) {
    this.assess_last_update = assess_last_update;
  }

  public boolean isDefend_sensors() {
    return defend_sensors;
  }

  public void setDefend_sensors(boolean defend_sensors) {
    this.defend_sensors = defend_sensors;
  }

  public long getDefense_last_update() {
    return defense_last_update;
  }

  public void setDefense_last_update(long defense_last_update) {
    this.defense_last_update = defense_last_update;
  }

  public boolean isDiagnostic_collection_enabled() {
    return diagnostic_collection_enabled;
  }

  public void setDiagnostic_collection_enabled(boolean diagnostic_collection_enabled) {
    this.diagnostic_collection_enabled = diagnostic_collection_enabled;
  }

  public boolean isSyslog_enabled() {
    return syslog_enabled;
  }

  public void setSyslog_enabled(boolean syslog_enabled) {
    this.syslog_enabled = syslog_enabled;
  }

  public Object getIs_assess_status_locked() {
    return is_assess_status_locked;
  }

  public void setIs_assess_status_locked(Object is_assess_status_locked) {
    this.is_assess_status_locked = is_assess_status_locked;
  }

  public Object getIs_protect_status_locked() {
    return is_protect_status_locked;
  }

  public void setIs_protect_status_locked(Object is_protect_status_locked) {
    this.is_protect_status_locked = is_protect_status_locked;
  }

  public String getConfig_source_assess() {
    return config_source_assess;
  }

  public void setConfig_source_assess(String config_source_assess) {
    this.config_source_assess = config_source_assess;
  }

  public String getConfig_source_protect() {
    return config_source_protect;
  }

  public void setConfig_source_protect(String config_source_protect) {
    this.config_source_protect = config_source_protect;
  }

  public String getProtect_enable_property_name() {
    return protect_enable_property_name;
  }

  public void setProtect_enable_property_name(String protect_enable_property_name) {
    this.protect_enable_property_name = protect_enable_property_name;
  }

  public String getAssess_enable_property_name() {
    return assess_enable_property_name;
  }

  public void setAssess_enable_property_name(String assess_enable_property_name) {
    this.assess_enable_property_name = assess_enable_property_name;
  }

  public String getProtect_enable_location() {
    return protect_enable_location;
  }

  public void setProtect_enable_location(String protect_enable_location) {
    this.protect_enable_location = protect_enable_location;
  }

  public String getAssess_enable_location() {
    return assess_enable_location;
  }

  public void setAssess_enable_location(String assess_enable_location) {
    this.assess_enable_location = assess_enable_location;
  }
}

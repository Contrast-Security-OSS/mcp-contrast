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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr;

import java.util.List;

/** Class representing the event information in an attack event. */
public class Event {
  private long received;
  private String source;
  private String result;
  private String displayName;
  private String rule;
  private String url;
  private boolean suppressed;
  private Application application;
  private Server server;
  private List<Object> attackers;
  private String type;
  private String detectionType;
  private String details;
  private HttpRequest httpRequest;
  private String callLocation;
  private Object tags;
  private String event_uuid;
  private int rule_id;
  private String rule_uuid;
  private UserInput user_input;
  private boolean exclusion_created;
  private String attack_type;
  private String attack_uuid;
  private boolean attack_suppressed;

  public long getReceived() {
    return received;
  }

  public void setReceived(long received) {
    this.received = received;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRule() {
    return rule;
  }

  public void setRule(String rule) {
    this.rule = rule;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isSuppressed() {
    return suppressed;
  }

  public void setSuppressed(boolean suppressed) {
    this.suppressed = suppressed;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public Server getServer() {
    return server;
  }

  public void setServer(Server server) {
    this.server = server;
  }

  public List<Object> getAttackers() {
    return attackers;
  }

  public void setAttackers(List<Object> attackers) {
    this.attackers = attackers;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDetectionType() {
    return detectionType;
  }

  public void setDetectionType(String detectionType) {
    this.detectionType = detectionType;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public HttpRequest getHttpRequest() {
    return httpRequest;
  }

  public void setHttpRequest(HttpRequest httpRequest) {
    this.httpRequest = httpRequest;
  }

  public String getCallLocation() {
    return callLocation;
  }

  public void setCallLocation(String callLocation) {
    this.callLocation = callLocation;
  }

  public Object getTags() {
    return tags;
  }

  public void setTags(Object tags) {
    this.tags = tags;
  }

  public String getEvent_uuid() {
    return event_uuid;
  }

  public void setEvent_uuid(String event_uuid) {
    this.event_uuid = event_uuid;
  }

  public int getRule_id() {
    return rule_id;
  }

  public void setRule_id(int rule_id) {
    this.rule_id = rule_id;
  }

  public String getRule_uuid() {
    return rule_uuid;
  }

  public void setRule_uuid(String rule_uuid) {
    this.rule_uuid = rule_uuid;
  }

  public UserInput getUser_input() {
    return user_input;
  }

  public void setUser_input(UserInput user_input) {
    this.user_input = user_input;
  }

  public boolean isExclusion_created() {
    return exclusion_created;
  }

  public void setExclusion_created(boolean exclusion_created) {
    this.exclusion_created = exclusion_created;
  }

  public String getAttack_type() {
    return attack_type;
  }

  public void setAttack_type(String attack_type) {
    this.attack_type = attack_type;
  }

  public String getAttack_uuid() {
    return attack_uuid;
  }

  public void setAttack_uuid(String attack_uuid) {
    this.attack_uuid = attack_uuid;
  }

  public boolean isAttack_suppressed() {
    return attack_suppressed;
  }

  public void setAttack_suppressed(boolean attack_suppressed) {
    this.attack_suppressed = attack_suppressed;
  }
}

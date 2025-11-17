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

import java.util.List;
import lombok.Data;

/** Class representing the event information in an attack event. */
@Data
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
}

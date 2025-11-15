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

/** Class representing the application information in an attack event. */
@Data
public class Application {
  private boolean primary;
  private boolean master;
  private boolean child;
  private String status;
  private int importance;

  private String app_id;
  private String name;
  private String parent_app_id;
  private int total_modules;
  private String language;
  private String context_path;
  private long last_seen;
  private String license_level;
  private String importance_description;
  private long first_seen;
}

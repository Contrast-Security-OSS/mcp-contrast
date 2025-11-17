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

import lombok.Data;

/** Class representing application information affected by a vulnerability. */
@Data
public class App {

  private String name;
  private String app_id;
  private long last_seen;
  private Object last_reset; // Using Object as last_reset was null in the example
  private long first_seen;
  private String importance_description;

  private int classCount;

  private int classUsage;
}

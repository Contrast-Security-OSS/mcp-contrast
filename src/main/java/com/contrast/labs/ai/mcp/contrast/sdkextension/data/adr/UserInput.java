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

/** Class representing user input information in an attack event. */
public class UserInput {
  private String value;
  private String name;
  private String type;
  private boolean omitted_value;
  private boolean truncated_value;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isOmitted_value() {
    return omitted_value;
  }

  public void setOmitted_value(boolean omitted_value) {
    this.omitted_value = omitted_value;
  }

  public boolean isTruncated_value() {
    return truncated_value;
  }

  public void setTruncated_value(boolean truncated_value) {
    this.truncated_value = truncated_value;
  }
}

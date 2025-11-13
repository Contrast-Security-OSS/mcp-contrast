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

/** Root class for deserializing attack event data from JSON. */
public class AttackEvent {
  private boolean success;
  private List<String> messages;
  private EventSummary summary;

  private String runBook;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public List<String> getMessages() {
    return messages;
  }

  public void setMessages(List<String> messages) {
    this.messages = messages;
  }

  public EventSummary getSummary() {
    return summary;
  }

  public void setSummary(EventSummary summary) {
    this.summary = summary;
  }

  public String getRunBook() {
    return runBook;
  }

  public void setRunBook(String runBook) {
    this.runBook = runBook;
  }
}

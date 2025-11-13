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
import lombok.Getter;
import lombok.Setter;

/**
 * Response wrapper for attacks API calls. Captures both the attack data and pagination metadata
 * from the API response.
 */
@Getter
@Setter
public class AttacksResponse {
  private List<Attack> attacks;

  /** Total count of attacks matching the query (across all pages). */
  private Integer count;

  /** Alternative total count field (some APIs use 'total' instead of 'count'). */
  private Integer total;

  /**
   * Get the total count from either count or total field. Some APIs use 'count', others use
   * 'total'.
   *
   * @return The total count, preferring 'count' over 'total', or null if neither present
   */
  public Integer getTotalCount() {
    if (count != null) {
      return count;
    }
    return total;
  }
}

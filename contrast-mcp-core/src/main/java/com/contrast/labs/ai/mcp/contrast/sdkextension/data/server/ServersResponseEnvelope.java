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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data.server;

import java.io.IOException;

/** Validates server-filter envelopes and normalizes TeamServer's empty-tag response quirk. */
public final class ServersResponseEnvelope {

  private ServersResponseEnvelope() {}

  /**
   * Validates a server-filter response without treating downstream failures as empty results.
   *
   * <p>TeamServer's tag prefilter returns before registering success when no visible server has a
   * requested tag. That one path has a non-null empty server list, count zero, no messages, and
   * success=false. Normalize only that response shape when the request actually included tags.
   */
  public static void validateAndNormalize(ServersResponse response, ServerFilterBody filterBody)
      throws IOException {
    if (response == null || response.getServers() == null || response.getCount() == null) {
      throw new IOException("Invalid server response envelope");
    }

    if (response.isSuccess()) {
      return;
    }

    if (isKnownEmptyTagResponse(response, filterBody)) {
      response.setSuccess(true);
      return;
    }

    throw new IOException("Invalid server response envelope");
  }

  private static boolean isKnownEmptyTagResponse(
      ServersResponse response, ServerFilterBody filterBody) {
    return filterBody != null
        && filterBody.getTags() != null
        && !filterBody.getTags().isEmpty()
        && response.getServers().isEmpty()
        && response.getCount() == 0L
        && (response.getMessages() == null || response.getMessages().isEmpty());
  }
}

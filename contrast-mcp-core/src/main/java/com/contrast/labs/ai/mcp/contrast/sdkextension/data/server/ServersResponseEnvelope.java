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
import lombok.extern.slf4j.Slf4j;

/** Validates server-filter envelopes and normalizes TeamServer's empty-tag response quirk. */
@Slf4j
public final class ServersResponseEnvelope {

  private ServersResponseEnvelope() {}

  /**
   * Validates a server-filter response without treating downstream failures as empty results.
   *
   * <p>TeamServer's tag prefilter is the only server-filter path that returns before registering
   * success. All other empty filters pass through the normal query path and register success. Keep
   * this normalization tag-scoped so genuine failures with an empty envelope remain failures.
   */
  public static void validateAndNormalize(ServersResponse response, ServerFilterBody filterBody)
      throws IOException {
    if (response == null || response.getServers() == null || response.getCount() == null) {
      throw new IOException("Malformed server response envelope");
    }

    if (response.isSuccess()) {
      return;
    }

    if (isKnownEmptyTagResponse(response, filterBody)) {
      log.atWarn().setMessage("Normalizing known TeamServer empty tag-filter response").log();
      response.setSuccess(true);
      return;
    }

    throw new IOException("Unsuccessful server response envelope");
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

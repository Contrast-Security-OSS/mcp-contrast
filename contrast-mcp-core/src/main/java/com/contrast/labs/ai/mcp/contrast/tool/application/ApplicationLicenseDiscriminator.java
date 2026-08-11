/*
 * Copyright 2026 Contrast Security
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
package com.contrast.labs.ai.mcp.contrast.tool.application;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrastsecurity.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Classifies an application after an application-scoped endpoint returns HTTP 403. */
@Service
@RequiredArgsConstructor
public class ApplicationLicenseDiscriminator {

  public static final String LICENSED_LEVEL = "Licensed";

  private final ContrastApiClient contrastApiClient;

  /**
   * Fetches the application through an endpoint that remains available for archived and unlicensed
   * applications, then classifies the cause of the original denial.
   *
   * @param appId application identifier from the denied request
   * @param originalForbidden original HTTP 403 to preserve when discrimination fails
   * @return the application's access-relevant state
   * @throws UnauthorizedException the original denial when the discriminator lookup fails
   */
  public ApplicationState discriminate(String appId, UnauthorizedException originalForbidden) {
    try {
      var application = contrastApiClient.getApplicationWithLicense(appId);
      if (application == null) {
        throw originalForbidden;
      }
      if (application.isArchived()) {
        return ApplicationState.ARCHIVED;
      }
      if (application.getLicense() == null
          || !LICENSED_LEVEL.equals(application.getLicense().getLevel())) {
        return ApplicationState.UNLICENSED;
      }
      return ApplicationState.LICENSED;
    } catch (Exception fetchFailure) {
      throw originalForbidden;
    }
  }

  public enum ApplicationState {
    ARCHIVED,
    UNLICENSED,
    LICENSED
  }
}

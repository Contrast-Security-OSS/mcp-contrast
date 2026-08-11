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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.ApplicationLicense;
import com.contrast.labs.ai.mcp.contrast.tool.application.ApplicationLicenseDiscriminator.ApplicationState;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationLicenseDiscriminatorTest {

  private static final String APP_ID = "app-123";

  private ContrastApiClient contrastApiClient;
  private ApplicationLicenseDiscriminator discriminator;
  private UnauthorizedException originalForbidden;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    discriminator = new ApplicationLicenseDiscriminator(contrastApiClient);
    originalForbidden = new UnauthorizedException("Forbidden", "GET", "/route", 403, "Forbidden");
  }

  @Test
  void discriminate_should_return_archived_when_application_is_archived() throws Exception {
    var application = application(true, "Unlicensed");
    when(contrastApiClient.getApplicationWithLicense(APP_ID)).thenReturn(application);

    var result = discriminator.discriminate(APP_ID, originalForbidden);

    assertThat(result).isEqualTo(ApplicationState.ARCHIVED);
  }

  @Test
  void discriminate_should_return_unlicensed_when_application_is_not_licensed() throws Exception {
    var application = application(false, "Unlicensed");
    when(contrastApiClient.getApplicationWithLicense(APP_ID)).thenReturn(application);

    var result = discriminator.discriminate(APP_ID, originalForbidden);

    assertThat(result).isEqualTo(ApplicationState.UNLICENSED);
  }

  @Test
  void discriminate_should_return_unlicensed_when_application_license_is_missing()
      throws Exception {
    var application = application(false, null);
    when(contrastApiClient.getApplicationWithLicense(APP_ID)).thenReturn(application);

    var result = discriminator.discriminate(APP_ID, originalForbidden);

    assertThat(result).isEqualTo(ApplicationState.UNLICENSED);
  }

  @Test
  void discriminate_should_return_licensed_when_application_is_visible_and_licensed()
      throws Exception {
    var application = application(false, ApplicationLicenseDiscriminator.LICENSED_LEVEL);
    when(contrastApiClient.getApplicationWithLicense(APP_ID)).thenReturn(application);

    var result = discriminator.discriminate(APP_ID, originalForbidden);

    assertThat(result).isEqualTo(ApplicationState.LICENSED);
  }

  @Test
  void discriminate_should_rethrow_original_forbidden_when_application_fetch_fails()
      throws Exception {
    when(contrastApiClient.getApplicationWithLicense(APP_ID))
        .thenThrow(new IOException("Application lookup failed"));

    assertThatThrownBy(() -> discriminator.discriminate(APP_ID, originalForbidden))
        .isSameAs(originalForbidden);
  }

  @Test
  void discriminate_should_rethrow_original_forbidden_when_application_fetch_returns_null()
      throws Exception {
    when(contrastApiClient.getApplicationWithLicense(APP_ID)).thenReturn(null);

    assertThatThrownBy(() -> discriminator.discriminate(APP_ID, originalForbidden))
        .isSameAs(originalForbidden);
  }

  private static Application application(boolean archived, String licenseLevel) {
    var application = new Application();
    application.setArchived(archived);
    if (licenseLevel != null) {
      var license = new ApplicationLicense();
      license.setLevel(licenseLevel);
      application.setLicense(license);
    }
    return application;
  }
}

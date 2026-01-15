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
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SDKExtensionDateSerializationTest {

  private ContrastSDK mockSdk;
  private SDKExtension sdkExtension;

  @BeforeEach
  void setUp() {
    mockSdk = mock();
    sdkExtension = new SDKExtension(mockSdk);
  }

  @Test
  void getTraces_should_serialize_dates_as_epoch_milliseconds() throws Exception {
    // Given: A filter body with date filters
    var filterBody = new ExtendedTraceFilterBody();
    filterBody.setStartDate(new Date(1672531200000L)); // Jan 1, 2023 UTC
    filterBody.setEndDate(new Date(1704067200000L)); // Jan 1, 2024 UTC

    // Mock the SDK response
    var emptyResponse = "{\"traces\":[],\"count\":0}";
    when(mockSdk.makeRequestWithBody(any(), anyString(), anyString(), any()))
        .thenReturn(new ByteArrayInputStream(emptyResponse.getBytes(StandardCharsets.UTF_8)));

    // When: getTraces is called
    sdkExtension.getTraces("org-123", "app-456", filterBody);

    // Then: The JSON body should have epoch milliseconds, not strings
    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockSdk)
        .makeRequestWithBody(
            eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), eq(MediaType.JSON));

    var jsonBody = bodyCaptor.getValue();
    assertThat(jsonBody).contains("\"startDate\":1672531200000");
    assertThat(jsonBody).contains("\"endDate\":1704067200000");
    assertThat(jsonBody).doesNotContain("Jan");
    assertThat(jsonBody).doesNotContain("2023");
  }
}

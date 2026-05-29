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
package com.contrast.labs.ai.mcp.contrast.tool.sast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.AnonymousProjectBuilder;
import com.contrast.labs.ai.mcp.contrast.client.SdkApiClient;
import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.config.SDKExtensionFactory;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.scan.Projects;
import com.contrastsecurity.sdk.scan.ScanManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetSastProjectLocalParityTest {

  private static final String TEST_LOCAL_ORG_ID = "local-org-id-should-not-serialize";
  private static final String TEST_PROJECT_NAME = "test-project";
  private static final String PROJECT_OWNER_ORG_ID = "project-owner-org";

  @Mock private ContrastSDKFactory sdkFactory;
  @Mock private SDKExtensionFactory sdkExtensionFactory;
  @Mock private ContrastSDK sdk;
  @Mock private ScanManager scanManager;
  @Mock private Projects projects;

  private GetSastProjectTool tool;

  @BeforeEach
  void setUp() {
    var sdkApiClient = new SdkApiClient(sdkFactory, sdkExtensionFactory);
    tool = new GetSastProjectTool(sdkApiClient);
    when(sdkFactory.getOrgId()).thenReturn(TEST_LOCAL_ORG_ID);
    when(sdkFactory.getSDK()).thenReturn(sdk);
    when(sdk.scan(TEST_LOCAL_ORG_ID)).thenReturn(scanManager);
    when(scanManager.projects()).thenReturn(projects);
  }

  @Test
  void getScanProject_should_preserve_local_stdio_response_through_sdk_api_client()
      throws Exception {
    var project =
        AnonymousProjectBuilder.validProject()
            .withId("project-123")
            .withName(TEST_PROJECT_NAME)
            .withOrganizationId(PROJECT_OWNER_ORG_ID)
            .withLanguage("Java")
            .withHigh(7)
            .withCompletedScans(2)
            .build();
    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.of(project));

    var result = tool.getScanProject(TEST_PROJECT_NAME);
    var responseText = result.toString();

    verify(sdk).scan(TEST_LOCAL_ORG_ID);
    verify(projects).findByName(TEST_PROJECT_NAME);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data().id()).isEqualTo("project-123");
    assertThat(result.data().name()).isEqualTo(TEST_PROJECT_NAME);
    assertThat(result.data().organizationId()).isEqualTo(PROJECT_OWNER_ORG_ID);
    assertThat(result.data().language()).isEqualTo("Java");
    assertThat(result.data().high()).isEqualTo(7);
    assertThat(result.data().completedScans()).isEqualTo(2);
    assertThat(responseText).contains(PROJECT_OWNER_ORG_ID).doesNotContain(TEST_LOCAL_ORG_ID);
  }
}

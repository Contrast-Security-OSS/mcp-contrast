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
package com.contrast.labs.ai.mcp.contrast.tool.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.config.SDKExtensionFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrastsecurity.sdk.ContrastSDK;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalSdkSingleToolTest {

  @Mock private ContrastSDKFactory sdkFactory;
  @Mock private SDKExtensionFactory sdkExtensionFactory;
  @InjectMocks private TestLocalSdkSingleTool tool;

  @Test
  void getContrastSDK_should_delegate_to_factory() {
    ContrastSDK expected = mock();
    when(sdkFactory.getSDK()).thenReturn(expected);

    assertThat(tool.getContrastSDK()).isSameAs(expected);
  }

  @Test
  void getOrgId_should_delegate_to_factory() {
    when(sdkFactory.getOrgId()).thenReturn("org-456");

    assertThat(tool.getOrgId()).isEqualTo("org-456");
  }

  @Test
  void getSDKExtension_should_delegate_to_factory() {
    SDKExtension expected = mock();
    when(sdkExtensionFactory.getSDKExtension()).thenReturn(expected);

    assertThat(tool.getSDKExtension()).isSameAs(expected);
  }

  static class TestLocalSdkSingleTool extends LocalSdkSingleTool<ToolParams, String> {

    @Override
    protected String doExecute(ToolParams params, NoticeCollector collector) {
      return "test";
    }
  }
}

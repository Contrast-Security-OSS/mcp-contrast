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
package com.contrast.labs.ai.mcp.contrast.tool.base;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.config.SDKExtensionFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrastsecurity.sdk.ContrastSDK;
import org.springframework.beans.factory.annotation.Autowired;

/** Temporary stdio-only bridge for tools that have not migrated to ContrastApiClient yet. */
public abstract class LocalSdkPaginatedTool<P extends ToolParams, R> extends PaginatedTool<P, R> {

  @Autowired protected ContrastSDKFactory sdkFactory;
  @Autowired protected SDKExtensionFactory sdkExtensionFactory;

  protected ContrastSDK getContrastSDK() {
    return sdkFactory.getSDK();
  }

  protected String getOrgId() {
    return sdkFactory.getOrgId();
  }

  protected SDKExtension getSDKExtension() {
    return sdkExtensionFactory.getSDKExtension();
  }
}

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
package com.contrast.labs.ai.mcp.contrast.config;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for creating and caching SDKExtension instances. Follows the same pattern as {@link
 * ContrastSDKFactory} for consistent singleton management via Spring DI.
 *
 * <p>SDKExtension is thread-safe to share as a singleton because:
 *
 * <ul>
 *   <li>All fields are final (immutable after construction)
 *   <li>Gson is documented as thread-safe
 *   <li>UrlBuilder is already a static singleton
 *   <li>ContrastSDK is already shared via ContrastSDKFactory
 *   <li>All methods are stateless: input -> API call -> parse -> return
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SDKExtensionFactory {

  private final ContrastSDKFactory contrastSDKFactory;
  private SDKExtension sdkExtensionInstance;

  /**
   * Returns a cached SDKExtension instance, creating it on first access.
   *
   * @return cached SDKExtension instance
   */
  public synchronized SDKExtension getSDKExtension() {
    if (sdkExtensionInstance == null) {
      sdkExtensionInstance = new SDKExtension(contrastSDKFactory.getSDK());
    }
    return sdkExtensionInstance;
  }
}

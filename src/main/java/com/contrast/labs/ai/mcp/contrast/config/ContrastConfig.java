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

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrastsecurity.sdk.ContrastSDK;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Centralized configuration for Contrast SDK. Eliminates duplicated @Value field declarations
 * across service classes. Caches a single SDK instance for reuse across all tool calls.
 */
@Component
@Getter
public class ContrastConfig {

  @Getter(AccessLevel.NONE)
  private ContrastSDK sdkInstance;

  @Value("${contrast.host-name:${CONTRAST_HOST_NAME:}}")
  private String hostName;

  @Value("${contrast.api-key:${CONTRAST_API_KEY:}}")
  private String apiKey;

  @Value("${contrast.service-key:${CONTRAST_SERVICE_KEY:}}")
  private String serviceKey;

  @Value("${contrast.username:${CONTRAST_USERNAME:}}")
  private String userName;

  @Value("${contrast.org-id:${CONTRAST_ORG_ID:}}")
  private String orgId;

  @Value("${http.proxy.host:${http_proxy_host:}}")
  private String httpProxyHost;

  @Value("${http.proxy.port:${http_proxy_port:}}")
  private String httpProxyPort;

  @Value("${contrast.api.protocol:https}")
  private String protocol;

  /**
   * Returns a cached ContrastSDK instance, creating it on first access. Thread-safe via
   * synchronized lazy initialization. The SDK is safe to share across threads as it maintains no
   * per-request state.
   *
   * @return cached ContrastSDK instance
   */
  public synchronized ContrastSDK getSDK() {
    if (sdkInstance == null) {
      sdkInstance =
          SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
    }
    return sdkInstance;
  }

  /**
   * Validates that all required Contrast credentials are configured. Called automatically by Spring
   * after dependency injection. Fails fast with clear error message if any required credentials are
   * missing.
   *
   * @throws IllegalStateException if any required credentials are missing or empty
   */
  @PostConstruct
  public void validateConfiguration() {
    List<String> missing = new ArrayList<>();

    if (!StringUtils.hasText(hostName)) {
      missing.add("CONTRAST_HOST_NAME");
    }
    if (!StringUtils.hasText(apiKey)) {
      missing.add("CONTRAST_API_KEY");
    }
    if (!StringUtils.hasText(serviceKey)) {
      missing.add("CONTRAST_SERVICE_KEY");
    }
    if (!StringUtils.hasText(userName)) {
      missing.add("CONTRAST_USERNAME");
    }
    if (!StringUtils.hasText(orgId)) {
      missing.add("CONTRAST_ORG_ID");
    }

    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "Missing required Contrast configuration. Set the following environment variables: "
              + String.join(", ", missing));
    }
  }
}

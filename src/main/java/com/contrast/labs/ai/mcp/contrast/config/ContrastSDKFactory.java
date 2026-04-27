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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Factory for creating and caching ContrastSDK instances. Separates SDK lifecycle management from
 * the immutable {@link ContrastProperties} configuration record.
 *
 * <p>The SDK instance is lazily initialized and cached for reuse across all tool calls. Thread-safe
 * via synchronized lazy initialization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContrastSDKFactory {

  private final ContrastProperties properties;
  private ContrastSDK sdkInstance;

  /**
   * Returns a cached ContrastSDK instance, creating it on first access.
   *
   * @return cached ContrastSDK instance
   */
  public synchronized ContrastSDK getSDK() {
    if (sdkInstance == null) {
      sdkInstance =
          SDKHelper.getSDK(
              properties.hostName(),
              properties.apiKey(),
              properties.serviceKey(),
              properties.userName(),
              properties.httpProxyHost(),
              properties.httpProxyPort(),
              properties.protocol());
    }
    return sdkInstance;
  }

  /**
   * Returns the organization ID from configuration.
   *
   * @return organization ID
   */
  public String getOrgId() {
    return properties.orgId();
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
    var missing = new ArrayList<String>();

    if (!StringUtils.hasText(properties.hostName())) {
      missing.add("CONTRAST_HOST_NAME");
    }
    if (!StringUtils.hasText(properties.apiKey())) {
      missing.add("CONTRAST_API_KEY");
    }
    if (!StringUtils.hasText(properties.serviceKey())) {
      missing.add("CONTRAST_SERVICE_KEY");
    }
    if (!StringUtils.hasText(properties.userName())) {
      missing.add("CONTRAST_USERNAME");
    }
    if (!StringUtils.hasText(properties.orgId())) {
      missing.add("CONTRAST_ORG_ID");
    }

    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "Missing required Contrast configuration. Set the following environment variables: "
              + String.join(", ", missing));
    }

    var protocol = properties.protocol();
    if (StringUtils.hasText(protocol)) {
      var normalizedProtocol = protocol.strip().toLowerCase();
      if (normalizedProtocol.contains("://")) {
        throw new IllegalStateException(
            "Invalid contrast.api.protocol value: '"
                + protocol
                + "'. Set to 'https', not 'https://'. The protocol separator is added"
                + " automatically.");
      }
      if (!"https".equals(normalizedProtocol)) {
        throw new IllegalStateException(
            "Insecure protocol configured: '"
                + protocol
                + "'. The MCP server requires HTTPS to protect API credentials. Set"
                + " contrast.api.protocol=https or remove the property to use the"
                + " default.");
      }
    }

    var hostName = properties.hostName();
    if (StringUtils.hasText(hostName)) {
      var normalizedHostName = hostName.strip().toLowerCase();
      if (normalizedHostName.contains("://") && !normalizedHostName.startsWith("https://")) {
        throw new IllegalStateException(
            "Insecure protocol in CONTRAST_HOST_NAME: '"
                + hostName
                + "'. Use 'https://' or provide the hostname without a scheme"
                + " to use HTTPS by default.");
      }
    }
  }
}

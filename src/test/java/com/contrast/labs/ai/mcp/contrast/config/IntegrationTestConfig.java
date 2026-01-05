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
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * Test configuration that provides a shared SDK instance across all integration tests.
 *
 * <p>This significantly reduces test setup time by avoiding redundant SDK initialization in each
 * test class.
 *
 * <p>The shared SDK instance is thread-safe and can be safely used by parallel test execution.
 */
@TestConfiguration
public class IntegrationTestConfig {

  private static final Logger logger = LoggerFactory.getLogger(IntegrationTestConfig.class);

  @Value("${contrast.host-name:${CONTRAST_HOST_NAME:}}")
  private String hostName;

  @Value("${contrast.api-key:${CONTRAST_API_KEY:}}")
  private String apiKey;

  @Value("${contrast.service-key:${CONTRAST_SERVICE_KEY:}}")
  private String serviceKey;

  @Value("${contrast.username:${CONTRAST_USERNAME:}}")
  private String userName;

  @Value("${http.proxy.host:${http_proxy_host:}}")
  private String httpProxyHost;

  @Value("${http.proxy.port:${http_proxy_port:}}")
  private String httpProxyPort;

  @Value("${contrast.api.protocol:https}")
  private String protocol;

  /**
   * Provides a shared, singleton SDK extension instance for all integration tests.
   *
   * <p>This bean is created once per test run and reused across all test classes, eliminating
   * redundant SDK initialization.
   *
   * @return Configured SDKExtension instance
   */
  @Bean
  @Scope("singleton")
  public SDKExtension sharedSDKExtension() {
    logger.info("Initializing shared SDK extension for integration tests");
    logger.info("  Host: {}", hostName);
    logger.info("  Username: {}", userName);

    var sdk =
        SDKHelper.getSDK(
            hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort, protocol);
    var sdkExtension = new SDKExtension(sdk);

    logger.info("✓ Shared SDK extension initialized successfully");
    return sdkExtension;
  }
}

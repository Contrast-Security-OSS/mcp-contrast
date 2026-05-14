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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Immutable configuration properties for Contrast SDK. Uses Spring Boot's {@link
 * ConfigurationProperties} with a Java record for type-safe binding.
 *
 * <p>Properties are bound from the "contrast" prefix in application.properties or environment
 * variables (e.g., CONTRAST_HOST_NAME maps to contrast.host-name).
 *
 * <p>Required fields (hostName, apiKey, serviceKey, userName, orgId) are validated at startup by
 * {@link ContrastSDKFactory#validateConfiguration()}.
 */
@ConfigurationProperties(prefix = "contrast")
public record ContrastProperties(
    String hostName,
    String apiKey,
    String serviceKey,
    String userName,
    String orgId,
    String httpProxyHost,
    String httpProxyPort,
    String protocol) {}

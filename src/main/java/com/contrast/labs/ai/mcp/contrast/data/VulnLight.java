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
package com.contrast.labs.ai.mcp.contrast.data;

import com.contrastsecurity.models.SessionMetadata;

import java.util.List;

/**
 * Lightweight vulnerability record for listing operations.
 * Contains essential vulnerability information including application correlation data.
 *
 * @param title Vulnerability title/description
 * @param type Vulnerability type/rule name (e.g., "sql-injection", "xss-reflected")
 * @param vulnID Unique vulnerability identifier (UUID)
 * @param severity Severity level (CRITICAL, HIGH, MEDIUM, LOW, NOTE)
 * @param appID Application UUID that owns this vulnerability
 * @param appName Application display name that owns this vulnerability
 * @param sessionMetadata Session metadata tags associated with this vulnerability
 * @param lastSeenAt ISO-8601 timestamp of last detection
 * @param status Current vulnerability status (Reported, Confirmed, Remediated, etc.)
 * @param firstSeenAt ISO-8601 timestamp of first detection
 * @param closedAt ISO-8601 timestamp when closed (null if open)
 * @param environments List of environments where vulnerability was seen (DEVELOPMENT, QA, PRODUCTION)
 * @param tags User-defined tags applied to this vulnerability
 */
public record VulnLight(
    String title,
    String type,
    String vulnID,
    String severity,
    String appID,
    String appName,
    List<SessionMetadata> sessionMetadata,
    String lastSeenAt,
    String status,
    String firstSeenAt,
    String closedAt,
    List<String> environments,
    List<String> tags
) {
}

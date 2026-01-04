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
package com.contrast.labs.ai.mcp.contrast.data.sast;

import com.contrastsecurity.sdk.scan.Project;
import java.time.Instant;
import java.util.List;

/**
 * Serializable representation of a Contrast Scan (SAST) project.
 *
 * <p>This record wraps the SDK {@link Project} interface to provide Jackson-compatible
 * serialization. The SDK interface uses method-style accessors (e.g., {@code id()}) which Jackson
 * cannot serialize by default.
 *
 * @param id Unique project identifier (UUID)
 * @param name Project name as displayed in Contrast UI
 * @param organizationId Organization UUID that owns this project
 * @param archived Whether the project has been archived
 * @param language Programming language (JAVA, JAVASCRIPT, PYTHON, etc.)
 * @param critical Count of critical severity vulnerabilities in last scan
 * @param high Count of high severity vulnerabilities in last scan
 * @param medium Count of medium severity vulnerabilities in last scan
 * @param low Count of low severity vulnerabilities in last scan
 * @param note Count of note severity vulnerabilities in last scan
 * @param lastScanId ID of most recent completed scan (null if no scans)
 * @param lastScanTime Timestamp of most recent completed scan (null if no scans)
 * @param completedScans Total count of successfully completed scans
 * @param includeNamespaceFilters Namespaces to include in scans
 * @param excludeNamespaceFilters Namespaces to exclude from scans
 */
public record ScanProject(
    String id,
    String name,
    String organizationId,
    boolean archived,
    String language,
    int critical,
    int high,
    int medium,
    int low,
    int note,
    String lastScanId,
    Instant lastScanTime,
    int completedScans,
    List<String> includeNamespaceFilters,
    List<String> excludeNamespaceFilters) {

  /**
   * Creates a ScanProject from an SDK Project interface.
   *
   * @param project SDK Project to convert
   * @return new ScanProject with all fields mapped
   * @throws NullPointerException if project is null
   */
  public static ScanProject from(Project project) {
    return new ScanProject(
        project.id(),
        project.name(),
        project.organizationId(),
        project.archived(),
        project.language(),
        project.critical(),
        project.high(),
        project.medium(),
        project.low(),
        project.note(),
        project.lastScanId(),
        project.lastScanTime(),
        project.completedScans(),
        project.includeNamespaceFilters() != null
            ? List.copyOf(project.includeNamespaceFilters())
            : List.of(),
        project.excludeNamespaceFilters() != null
            ? List.copyOf(project.excludeNamespaceFilters())
            : List.of());
  }
}

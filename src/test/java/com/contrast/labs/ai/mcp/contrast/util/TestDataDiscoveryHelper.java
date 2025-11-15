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
package com.contrast.labs.ai.mcp.contrast.util;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utility for discovering suitable test data in integration tests.
 *
 * <p>Provides reusable discovery patterns that leverage SDK caching for efficiency.
 */
public class TestDataDiscoveryHelper {

  private static final Logger logger = LoggerFactory.getLogger(TestDataDiscoveryHelper.class);

  /**
   * Finds the first available application in the organization.
   *
   * <p>This is the simplest discovery pattern - useful when any application will work.
   *
   * @param orgId Organization ID
   * @param sdkExtension SDK extension instance
   * @return Optional containing the first application, or empty if no applications exist
   * @throws IOException If an error occurs fetching applications
   */
  public static Optional<Application> findFirstApplication(String orgId, SDKExtension sdkExtension)
      throws IOException {
    logger.info("Finding first available application...");

    var appsResponse = sdkExtension.getApplications(orgId);
    var applications = appsResponse.getApplications();

    if (applications.isEmpty()) {
      logger.warn("No applications found in organization");
      return Optional.empty();
    }

    var app = applications.get(0);
    logger.info("✓ Found application: {} (ID: {})", app.getName(), app.getAppId());
    return Optional.of(app);
  }

  /**
   * Finds an application that has third-party libraries.
   *
   * <p>This discovery pattern is useful for SCA (Software Composition Analysis) tests that need to
   * test library-related functionality.
   *
   * @param orgId Organization ID
   * @param sdkExtension SDK extension instance
   * @param maxAppsToCheck Maximum number of applications to check (default: 50)
   * @return Optional containing an application with libraries, or empty if none found
   * @throws IOException If an error occurs during discovery
   */
  public static Optional<ApplicationWithLibraries> findApplicationWithLibraries(
      String orgId, SDKExtension sdkExtension, int maxAppsToCheck) throws IOException {
    logger.info("Finding application with libraries (checking up to {} apps)...", maxAppsToCheck);

    var appsResponse = sdkExtension.getApplications(orgId);
    var applications = appsResponse.getApplications();

    if (applications.isEmpty()) {
      logger.warn("No applications found in organization");
      return Optional.empty();
    }

    int appsChecked = 0;
    int actualMaxToCheck = Math.min(applications.size(), maxAppsToCheck);

    for (Application app : applications) {
      if (appsChecked >= actualMaxToCheck) {
        logger.info("Reached max apps to check ({}), stopping search", actualMaxToCheck);
        break;
      }
      appsChecked++;

      logger.debug(
          "Checking app {}/{}: {} (ID: {})",
          appsChecked,
          actualMaxToCheck,
          app.getName(),
          app.getAppId());

      try {
        var libraries = SDKHelper.getLibsForID(app.getAppId(), orgId, sdkExtension);
        if (libraries != null && !libraries.isEmpty()) {
          logger.info(
              "✓ Found application with {} library/libraries: {} (ID: {})",
              libraries.size(),
              app.getName(),
              app.getAppId());

          // Check for vulnerable libraries
          boolean hasVulnerableLibrary = false;
          String vulnerableCveId = null;

          for (LibraryExtended lib : libraries) {
            if (lib.getVulnerabilities() != null && !lib.getVulnerabilities().isEmpty()) {
              hasVulnerableLibrary = true;
              var firstVuln = lib.getVulnerabilities().get(0);
              if (firstVuln.getName() != null && firstVuln.getName().startsWith("CVE-")) {
                vulnerableCveId = firstVuln.getName();
                logger.info("  ✓ Has vulnerable library with CVE: {}", vulnerableCveId);
                break;
              }
            }
          }

          return Optional.of(
              new ApplicationWithLibraries(app, libraries, hasVulnerableLibrary, vulnerableCveId));
        }
      } catch (IOException e) {
        logger.warn("Error checking libraries for app {}: {}", app.getAppId(), e.getMessage());
        // Continue to next app
      }
    }

    logger.warn("No application with libraries found after checking {} apps", appsChecked);
    return Optional.empty();
  }

  /**
   * Finds an application with libraries, checking up to 50 applications.
   *
   * @param orgId Organization ID
   * @param sdkExtension SDK extension instance
   * @return Optional containing an application with libraries, or empty if none found
   * @throws IOException If an error occurs during discovery
   */
  public static Optional<ApplicationWithLibraries> findApplicationWithLibraries(
      String orgId, SDKExtension sdkExtension) throws IOException {
    return findApplicationWithLibraries(orgId, sdkExtension, 50);
  }

  /**
   * Container class for an application with its libraries.
   *
   * <p>Includes additional metadata about vulnerable libraries for SCA testing.
   */
  public static class ApplicationWithLibraries {
    private final Application application;
    private final List<LibraryExtended> libraries;
    private final boolean hasVulnerableLibrary;
    private final String vulnerableCveId;

    public ApplicationWithLibraries(
        Application application,
        List<LibraryExtended> libraries,
        boolean hasVulnerableLibrary,
        String vulnerableCveId) {
      this.application = application;
      this.libraries = libraries;
      this.hasVulnerableLibrary = hasVulnerableLibrary;
      this.vulnerableCveId = vulnerableCveId;
    }

    public Application getApplication() {
      return application;
    }

    public List<LibraryExtended> getLibraries() {
      return libraries;
    }

    public boolean hasVulnerableLibrary() {
      return hasVulnerableLibrary;
    }

    public String getVulnerableCveId() {
      return vulnerableCveId;
    }

    @Override
    public String toString() {
      return String.format(
          "ApplicationWithLibraries{appId='%s', appName='%s', libraryCount=%d, "
              + "hasVulnerableLibrary=%s, vulnerableCveId='%s'}",
          application.getAppId(),
          application.getName(),
          libraries.size(),
          hasVulnerableLibrary,
          vulnerableCveId);
    }
  }
}

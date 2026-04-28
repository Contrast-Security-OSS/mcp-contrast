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
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper utility for discovering suitable test data in integration tests.
 *
 * <p>Provides reusable discovery patterns that leverage SDK caching for efficiency.
 */
@Slf4j
public class TestDataDiscoveryHelper {

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
    log.info("Finding first available application...");

    var applications = IntegrationTestDataCache.getApplications(orgId, sdkExtension);

    if (applications.isEmpty()) {
      log.warn("No applications found in organization");
      return Optional.empty();
    }

    var app = applications.get(0);
    log.info("✓ Found application: {} (ID: {})", app.getName(), app.getAppId());
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
    log.info("Finding application with libraries (checking up to {} apps)...", maxAppsToCheck);

    var applications = IntegrationTestDataCache.getApplications(orgId, sdkExtension);

    if (applications.isEmpty()) {
      log.warn("No applications found in organization");
      return Optional.empty();
    }

    int appsChecked = 0;
    int actualMaxToCheck = Math.min(applications.size(), maxAppsToCheck);

    // Prefer an app whose libraries include a populated vulnerabilities array AND surface a
    // CVE-prefixed vulnerability name so SCA tests can exercise severity breakdowns and CVE
    // lookups. Fall back tiers: vulnerable-but-no-CVE-name, then any app with libraries.
    ApplicationWithLibraries firstVulnerableNoCveMatch = null;
    ApplicationWithLibraries firstAnyLibMatch = null;

    for (Application app : applications) {
      if (appsChecked >= actualMaxToCheck) {
        log.info("Reached max apps to check ({}), stopping search", actualMaxToCheck);
        break;
      }
      appsChecked++;

      log.debug(
          "Checking app {}/{}: {} (ID: {})",
          appsChecked,
          actualMaxToCheck,
          app.getName(),
          app.getAppId());

      try {
        var libraries = IntegrationTestDataCache.getLibraries(orgId, app.getAppId(), sdkExtension);
        if (libraries == null || libraries.isEmpty()) {
          continue;
        }

        boolean hasVulnerableLibrary = false;
        String vulnerableCveId = null;
        for (LibraryExtended lib : libraries) {
          if (lib.getVulnerabilities() == null || lib.getVulnerabilities().isEmpty()) {
            continue;
          }
          hasVulnerableLibrary = true;
          for (var vuln : lib.getVulnerabilities()) {
            if (vuln.getName() != null && vuln.getName().startsWith("CVE-")) {
              vulnerableCveId = vuln.getName();
              break;
            }
          }
          if (vulnerableCveId != null) {
            break;
          }
        }

        if (hasVulnerableLibrary && vulnerableCveId != null) {
          log.info(
              "✓ Found application with {} library/libraries (vulnerable, CVE={}): {} (ID: {})",
              libraries.size(),
              vulnerableCveId,
              app.getName(),
              app.getAppId());
          return Optional.of(new ApplicationWithLibraries(app, libraries, true, vulnerableCveId));
        }

        if (hasVulnerableLibrary) {
          if (firstVulnerableNoCveMatch == null) {
            log.debug(
                "Remembering vulnerable app without CVE-named vuln as fallback: {} (ID: {})",
                app.getName(),
                app.getAppId());
            firstVulnerableNoCveMatch = new ApplicationWithLibraries(app, libraries, true, null);
          }
          continue;
        }

        if (firstAnyLibMatch == null) {
          log.debug(
              "Remembering app as non-vulnerable fallback: {} (ID: {})",
              app.getName(),
              app.getAppId());
          firstAnyLibMatch = new ApplicationWithLibraries(app, libraries, false, null);
        }
      } catch (IOException e) {
        log.warn("Error checking libraries for app {}: {}", app.getAppId(), e.getMessage());
        // Continue to next app
      }
    }

    if (firstVulnerableNoCveMatch != null) {
      log.info(
          "✓ Falling back to vulnerable app without CVE-named vuln: {} (ID: {})",
          firstVulnerableNoCveMatch.getApplication().getName(),
          firstVulnerableNoCveMatch.getApplication().getAppId());
      return Optional.of(firstVulnerableNoCveMatch);
    }

    if (firstAnyLibMatch != null) {
      log.info(
          "✓ Falling back to non-vulnerable app with libraries: {} (ID: {})",
          firstAnyLibMatch.getApplication().getName(),
          firstAnyLibMatch.getApplication().getAppId());
      return Optional.of(firstAnyLibMatch);
    }

    log.warn("No application with libraries found after checking {} apps", appsChecked);
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
   * Finds an application that has Protect/ADR rules configured.
   *
   * @param orgId Organization ID
   * @param sdkExtension SDK extension instance
   * @param maxAppsToCheck Maximum number of applications to inspect
   * @return Optional containing an application with Protect rules metadata
   * @throws IOException If an error occurs during discovery
   */
  public static Optional<ApplicationWithProtectRules> findApplicationWithProtectRules(
      String orgId, SDKExtension sdkExtension, int maxAppsToCheck) throws IOException {
    log.info("Finding application with Protect rules (checking up to {} apps)...", maxAppsToCheck);

    var applications = IntegrationTestDataCache.getApplications(orgId, sdkExtension);
    if (applications.isEmpty()) {
      log.warn("No applications found in organization");
      return Optional.empty();
    }

    int appsChecked = 0;
    int actualMaxToCheck = Math.min(applications.size(), maxAppsToCheck);

    for (Application app : applications) {
      if (appsChecked >= actualMaxToCheck) {
        log.info("Reached max apps to check ({}), stopping search", actualMaxToCheck);
        break;
      }
      appsChecked++;

      log.debug(
          "Checking Protect config for app {}/{}: {} (ID: {})",
          appsChecked,
          actualMaxToCheck,
          app.getName(),
          app.getAppId());

      try {
        var protectData =
            IntegrationTestDataCache.getProtectConfig(orgId, app.getAppId(), sdkExtension);
        if (protectData.isPresent()
            && protectData.get().getRules() != null
            && !protectData.get().getRules().isEmpty()) {
          var config = protectData.get();
          log.info(
              "✓ Found Protect-enabled application with {} rule(s): {} (ID: {})",
              config.getRules().size(),
              app.getName(),
              app.getAppId());
          return Optional.of(new ApplicationWithProtectRules(app, config));
        }
      } catch (IOException e) {
        log.warn("Error retrieving Protect config for app {}: {}", app.getAppId(), e.getMessage());
      }
    }

    log.warn("No application with Protect rules found after checking {} apps", appsChecked);
    return Optional.empty();
  }

  /**
   * Finds an application with Protect rules, checking up to 50 applications.
   *
   * @param orgId Organization ID
   * @param sdkExtension SDK extension instance
   * @return Optional containing Protect-enabled application metadata
   * @throws IOException If an error occurs during discovery
   */
  public static Optional<ApplicationWithProtectRules> findApplicationWithProtectRules(
      String orgId, SDKExtension sdkExtension) throws IOException {
    return findApplicationWithProtectRules(orgId, sdkExtension, 50);
  }

  /**
   * Finds an application with route coverage data and optional session metadata.
   *
   * @param orgId Organization ID
   * @param sdkExtension SDK extension instance
   * @param maxAppsToCheck Maximum number of applications to inspect
   * @return Optional containing route coverage test data
   * @throws IOException If an error occurs during discovery
   */
  public static Optional<RouteCoverageTestData> findApplicationWithRouteCoverage(
      String orgId, SDKExtension sdkExtension, int maxAppsToCheck) throws IOException {
    log.info("Finding application with route coverage (checking up to {} apps)...", maxAppsToCheck);

    var applications = IntegrationTestDataCache.getApplications(orgId, sdkExtension);
    if (applications.isEmpty()) {
      log.warn("No applications found in organization");
      return Optional.empty();
    }

    int appsChecked = 0;
    int actualMaxToCheck = Math.min(applications.size(), maxAppsToCheck);
    RouteCoverageTestData fallback = null;

    for (Application app : applications) {
      if (appsChecked >= actualMaxToCheck) {
        log.info("Reached max apps to check ({}), stopping search", actualMaxToCheck);
        break;
      }
      appsChecked++;

      log.debug(
          "Checking route coverage for app {}/{}: {} (ID: {})",
          appsChecked,
          actualMaxToCheck,
          app.getName(),
          app.getAppId());

      Optional<RouteCoverageResponse> routeCoverageOptional;
      try {
        routeCoverageOptional =
            IntegrationTestDataCache.getRouteCoverage(orgId, app.getAppId(), sdkExtension);
      } catch (IOException e) {
        log.warn("Error retrieving route coverage for app {}: {}", app.getAppId(), e.getMessage());
        continue;
      }

      if (routeCoverageOptional.isEmpty()
          || routeCoverageOptional.get().getRoutes() == null
          || routeCoverageOptional.get().getRoutes().isEmpty()) {
        continue;
      }

      var routeCoverage = routeCoverageOptional.get();
      var candidate =
          new RouteCoverageTestData(
              app,
              List.copyOf(routeCoverage.getRoutes()),
              routeCoverage.getRoutes().size(),
              false,
              null,
              null);

      // Attempt to fetch session metadata for richer assertions
      try {
        var sessionMetadata =
            IntegrationTestDataCache.getLatestSessionMetadata(orgId, app.getAppId(), sdkExtension);
        if (sessionMetadata.isPresent()
            && sessionMetadata.get().getAgentSession() != null
            && sessionMetadata.get().getAgentSession().getMetadataSessions() != null
            && !sessionMetadata.get().getAgentSession().getMetadataSessions().isEmpty()) {
          var firstMetadata = sessionMetadata.get().getAgentSession().getMetadataSessions().get(0);
          var metadataField = firstMetadata.getMetadataField();
          if (metadataField != null
              && metadataField.getAgentLabel() != null
              && firstMetadata.getValue() != null) {
            candidate =
                candidate.withSessionMetadata(
                    metadataField.getAgentLabel(), firstMetadata.getValue());
            log.info(
                "✓ Found application with routes ({}) and session metadata {}={}",
                candidate.routeCount(),
                candidate.sessionMetadataName(),
                candidate.sessionMetadataValue());
            return Optional.of(candidate);
          }
        }
      } catch (IOException e) {
        log.warn(
            "Error retrieving session metadata for app {}: {}", app.getAppId(), e.getMessage());
      }

      if (fallback == null) {
        fallback = candidate;
      }
    }

    if (fallback != null) {
      log.info(
          "No application with session metadata found; using app {} with {} route(s)",
          fallback.application().getName(),
          fallback.routeCount());
      return Optional.of(fallback);
    }

    log.warn("No application with route coverage found after checking {} apps", appsChecked);
    return Optional.empty();
  }

  /**
   * Finds an application with route coverage, checking up to 50 applications.
   *
   * @param orgId Organization ID
   * @param sdkExtension SDK extension instance
   * @return Optional containing route coverage data
   * @throws IOException If an error occurs during discovery
   */
  public static Optional<RouteCoverageTestData> findApplicationWithRouteCoverage(
      String orgId, SDKExtension sdkExtension) throws IOException {
    return findApplicationWithRouteCoverage(orgId, sdkExtension, 50);
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

  /**
   * Container class for an application with Protect/ADR rules.
   *
   * <p>Includes the application and its Protect configuration metadata.
   */
  public static class ApplicationWithProtectRules {
    private final Application application;
    private final ProtectData protectData;

    public ApplicationWithProtectRules(Application application, ProtectData protectData) {
      this.application = application;
      this.protectData = protectData;
    }

    public Application getApplication() {
      return application;
    }

    public ProtectData getProtectData() {
      return protectData;
    }

    public int getRuleCount() {
      return protectData == null || protectData.getRules() == null
          ? 0
          : protectData.getRules().size();
    }

    @Override
    public String toString() {
      return String.format(
          "ApplicationWithProtectRules{appId='%s', appName='%s', ruleCount=%d}",
          application.getAppId(), application.getName(), getRuleCount());
    }
  }

  /**
   * Container class for route coverage test data.
   *
   * <p>Tracks discovered routes and optional session metadata fields for richer assertions.
   */
  public static class RouteCoverageTestData {
    private final Application application;
    private final List<Route> routes;
    private final int routeCount;
    private final boolean hasSessionMetadata;
    private final String sessionMetadataName;
    private final String sessionMetadataValue;

    public RouteCoverageTestData(
        Application application,
        List<Route> routes,
        int routeCount,
        boolean hasSessionMetadata,
        String sessionMetadataName,
        String sessionMetadataValue) {
      this.application = application;
      this.routes = routes;
      this.routeCount = routeCount;
      this.hasSessionMetadata = hasSessionMetadata;
      this.sessionMetadataName = sessionMetadataName;
      this.sessionMetadataValue = sessionMetadataValue;
    }

    public Application application() {
      return application;
    }

    public List<Route> routes() {
      return routes;
    }

    public int routeCount() {
      return routeCount;
    }

    public boolean hasSessionMetadata() {
      return hasSessionMetadata;
    }

    public String sessionMetadataName() {
      return sessionMetadataName;
    }

    public String sessionMetadataValue() {
      return sessionMetadataValue;
    }

    public RouteCoverageTestData withSessionMetadata(String metadataName, String metadataValue) {
      return new RouteCoverageTestData(
          application, routes, routeCount, true, metadataName, metadataValue);
    }

    @Override
    public String toString() {
      return String.format(
          "RouteCoverageTestData{appId='%s', appName='%s', routeCount=%d, "
              + "hasSessionMetadata=%s, sessionMetadataName='%s', sessionMetadataValue='%s'}",
          application.getAppId(),
          application.getName(),
          routeCount,
          hasSessionMetadata,
          sessionMetadataName,
          sessionMetadataValue);
    }
  }
}

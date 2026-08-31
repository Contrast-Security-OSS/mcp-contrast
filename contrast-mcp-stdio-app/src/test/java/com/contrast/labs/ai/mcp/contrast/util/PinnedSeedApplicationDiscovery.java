/*
 * Copyright 2026 Contrast Security
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
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryVulnerabilityExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Resolves and validates the optional pinned seed application used by library integration tests.
 */
@Slf4j
@UtilityClass
class PinnedSeedApplicationDiscovery {

  private static final String SEED_APP_ID_ENV = "CONTRAST_TEST_SEED_APP_ID";

  static String seedAppId() {
    return System.getenv(SEED_APP_ID_ENV);
  }

  static Optional<String> normalizeSeedAppId(String seedAppId) {
    if (!StringUtils.hasText(seedAppId)) {
      return Optional.empty();
    }
    return Optional.of(seedAppId.trim());
  }

  static Optional<TestDataDiscoveryHelper.ApplicationWithLibraries> find(
      String orgId, SDKExtension sdkExtension, String seedAppId) {
    var normalizedSeedAppId = normalizeSeedAppId(seedAppId);
    if (normalizedSeedAppId.isEmpty()) {
      return Optional.empty();
    }

    var appId = normalizedSeedAppId.get();
    try {
      var libraries = IntegrationTestDataCache.getLibraries(orgId, appId, sdkExtension);
      var cveId = findUsedCve(libraries);
      if (cveId.isPresent()) {
        return seededApplication(appId, libraries, cveId.get());
      }
      log.warn(
          "Pinned integration-test seed app {} does not contain an actively used library with a "
              + "CVE-named vulnerability; falling back to organization discovery",
          appId);
    } catch (IOException e) {
      log.warn(
          "Could not verify pinned integration-test seed app {}; falling back to organization "
              + "discovery: {}",
          appId,
          e.getMessage());
    }
    return Optional.empty();
  }

  private static Optional<String> findUsedCve(List<LibraryExtended> libraries) {
    return libraries.stream()
        .filter(library -> library.getClassesUsed() > 0)
        .map(LibraryExtended::getVulnerabilities)
        .filter(vulnerabilities -> vulnerabilities != null && !vulnerabilities.isEmpty())
        .flatMap(List::stream)
        .map(LibraryVulnerabilityExtended::getName)
        .filter(name -> name != null && name.startsWith("CVE-"))
        .findFirst();
  }

  private static Optional<TestDataDiscoveryHelper.ApplicationWithLibraries> seededApplication(
      String seedAppId, List<LibraryExtended> libraries, String cveId) {
    var application = new Application();
    application.setAppId(seedAppId);
    // The override supplies no display name, but downstream fixture diagnostics require one.
    application.setName(seedAppId);
    log.info("Using pinned integration-test seed app (CVE={}): ID={}", cveId, seedAppId);
    return Optional.of(
        new TestDataDiscoveryHelper.ApplicationWithLibraries(application, libraries, true, cveId));
  }
}

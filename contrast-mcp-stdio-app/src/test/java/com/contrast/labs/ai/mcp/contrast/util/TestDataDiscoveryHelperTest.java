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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibrariesExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryVulnerabilityExtended;
import com.contrastsecurity.http.LibraryFilterForm;
import com.contrastsecurity.http.LibraryFilterForm.LibraryExpandValues;
import com.contrastsecurity.http.LibraryFilterForm.LibraryQuickFilterType;
import com.contrastsecurity.models.Application;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TestDataDiscoveryHelperTest {

  private static final String ORG_ID = "org-1";
  private static final String UNUSED_APP_ID = "app-unused";
  private static final String USED_APP_ID = "app-used";
  private static final int PAGE_SIZE = 25;

  @Test
  void findApplicationWithLibraries_should_prefer_used_vulnerable_library_from_org_query()
      throws Exception {
    SDKExtension sdkExtension = mock();
    var unusedLibrary = orgLibrary(UNUSED_APP_ID, "Unused App", "CVE-2026-1000", 0);
    var usedLibrary = orgLibrary(USED_APP_ID, "Used App", "CVE-2026-2000", 7);
    var orgResponse = new LibrariesExtended();
    orgResponse.setLibraries(List.of(unusedLibrary, usedLibrary));
    orgResponse.setCount(2L);
    when(sdkExtension.getLibrariesWithFilter(any(), any())).thenReturn(orgResponse);

    var confirmedLibrary = vulnerableLibrary("CVE-2026-2000");
    var unusedConfirmable = List.of(vulnerableLibrary("CVE-2026-1000"));
    try (var cache = mockStatic(IntegrationTestDataCache.class)) {
      cache
          .when(() -> IntegrationTestDataCache.getLibraries(ORG_ID, USED_APP_ID, sdkExtension))
          .thenReturn(List.of(confirmedLibrary));
      cache
          .when(() -> IntegrationTestDataCache.getLibraries(ORG_ID, UNUSED_APP_ID, sdkExtension))
          .thenReturn(unusedConfirmable);

      var result =
          TestDataDiscoveryHelper.findApplicationWithLibraries(ORG_ID, sdkExtension).orElseThrow();

      assertThat(result.getApplication().getAppId()).isEqualTo(USED_APP_ID);
      assertThat(result.getApplication().getName()).isEqualTo("Used App");
      assertThat(result.getLibraries()).containsExactly(confirmedLibrary);
      assertThat(result.hasVulnerableLibrary()).isTrue();
      assertThat(result.getVulnerableCveId()).isEqualTo("CVE-2026-2000");
    }

    var filterCaptor = ArgumentCaptor.forClass(LibraryFilterForm.class);
    verify(sdkExtension).getLibrariesWithFilter(eq(ORG_ID), filterCaptor.capture());
    assertThat(filterCaptor.getValue().getLimit()).isEqualTo(PAGE_SIZE);
    assertThat(filterCaptor.getValue().getOffset()).isZero();
    assertThat(filterCaptor.getValue().getQuickFilter())
        .isEqualTo(LibraryQuickFilterType.VULNERABLE);
    assertThat(filterCaptor.getValue().getExpand())
        .isEqualTo(EnumSet.of(LibraryExpandValues.VULNS, LibraryExpandValues.APPS));
  }

  @Test
  void findApplicationWithLibraries_should_page_org_libraries_until_candidate_is_found()
      throws Exception {
    SDKExtension sdkExtension = mock();
    var firstPage = new LibrariesExtended();
    firstPage.setLibraries(
        IntStream.range(0, PAGE_SIZE)
            .mapToObj(ignored -> vulnerableLibrary("CVE-2026-1000"))
            .toList());
    firstPage.setCount((long) PAGE_SIZE + 1);
    var secondPage = new LibrariesExtended();
    secondPage.setLibraries(List.of(orgLibrary(USED_APP_ID, "Used App", "CVE-2026-2000", 7)));
    secondPage.setCount((long) PAGE_SIZE + 1);
    when(sdkExtension.getLibrariesWithFilter(any(), any())).thenReturn(firstPage, secondPage);

    var confirmedLibrary = vulnerableLibrary("CVE-2026-2000");
    try (var cache = mockStatic(IntegrationTestDataCache.class)) {
      cache
          .when(() -> IntegrationTestDataCache.getLibraries(ORG_ID, USED_APP_ID, sdkExtension))
          .thenReturn(List.of(confirmedLibrary));

      var result =
          TestDataDiscoveryHelper.findApplicationWithLibraries(ORG_ID, sdkExtension).orElseThrow();

      assertThat(result.getVulnerableCveId()).isEqualTo("CVE-2026-2000");
    }

    var filterCaptor = ArgumentCaptor.forClass(LibraryFilterForm.class);
    verify(sdkExtension, times(2)).getLibrariesWithFilter(eq(ORG_ID), filterCaptor.capture());
    assertThat(filterCaptor.getAllValues())
        .extracting(LibraryFilterForm::getOffset)
        .containsExactly(0, PAGE_SIZE);
  }

  @Test
  void findApplicationWithLibraries_should_skip_candidate_when_confirmation_fails()
      throws Exception {
    SDKExtension sdkExtension = mock();
    var failedLibrary = orgLibrary("app-fail", "Failed App", "CVE-2026-1000", 7);
    var successfulLibrary = orgLibrary("app-ok", "Successful App", "CVE-2026-2000", 7);
    var orgResponse = new LibrariesExtended();
    orgResponse.setLibraries(List.of(failedLibrary, successfulLibrary));
    orgResponse.setCount(2L);
    when(sdkExtension.getLibrariesWithFilter(any(), any())).thenReturn(orgResponse);

    var confirmedLibrary = vulnerableLibrary("CVE-2026-2000");
    try (var cache = mockStatic(IntegrationTestDataCache.class)) {
      cache
          .when(() -> IntegrationTestDataCache.getLibraries(ORG_ID, "app-fail", sdkExtension))
          .thenThrow(new IOException("confirmation failed"));
      cache
          .when(() -> IntegrationTestDataCache.getLibraries(ORG_ID, "app-ok", sdkExtension))
          .thenReturn(List.of(confirmedLibrary));

      var result =
          TestDataDiscoveryHelper.findApplicationWithLibraries(ORG_ID, sdkExtension).orElseThrow();

      assertThat(result.getApplication().getAppId()).isEqualTo("app-ok");
      assertThat(result.getApplication().getName()).isEqualTo("Successful App");
      assertThat(result.getLibraries()).containsExactly(confirmedLibrary);
      assertThat(result.getVulnerableCveId()).isEqualTo("CVE-2026-2000");
      cache.verify(() -> IntegrationTestDataCache.getLibraries(ORG_ID, "app-fail", sdkExtension));
      cache.verify(() -> IntegrationTestDataCache.getLibraries(ORG_ID, "app-ok", sdkExtension));
    }
  }

  private static LibraryExtended orgLibrary(
      String appId, String appName, String cve, int classesUsed) {
    Application app = mock();
    when(app.getId()).thenReturn(appId);
    when(app.getName()).thenReturn(appName);

    var library = vulnerableLibrary(cve);
    library.setApplications(List.of(app));
    library.setClassesUsed(classesUsed);
    return library;
  }

  private static LibraryExtended vulnerableLibrary(String cve) {
    LibraryVulnerabilityExtended vulnerability = mock();
    when(vulnerability.getName()).thenReturn(cve);
    var library = new LibraryExtended();
    library.setHash("hash-" + cve);
    library.setVulnerabilities(List.of(vulnerability));
    return library;
  }
}

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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LibraryExtended}, particularly the getTotalVulnerabilities() workaround for
 * TeamServer bug (TS-41988) where total_vulnerabilities returns 0 despite vulns array being
 * populated.
 */
class LibraryExtendedTest {

  @Test
  void getTotalVulnerabilities_should_return_array_size_when_vulnerabilities_populated() {
    var library = new LibraryExtended();
    library.setTotalVulnerabilities(0); // API returns wrong value

    var vulns = List.of(new LibraryVulnerabilityExtended(), new LibraryVulnerabilityExtended());
    library.setVulnerabilities(vulns);

    assertThat(library.getTotalVulnerabilities()).isEqualTo(2);
  }

  @Test
  void getTotalVulnerabilities_should_return_api_value_when_vulnerabilities_null() {
    var library = new LibraryExtended();
    library.setTotalVulnerabilities(5);
    library.setVulnerabilities(null);

    assertThat(library.getTotalVulnerabilities()).isEqualTo(5);
  }

  @Test
  void getTotalVulnerabilities_should_return_api_value_when_vulnerabilities_empty() {
    var library = new LibraryExtended();
    library.setTotalVulnerabilities(3);
    library.setVulnerabilities(new ArrayList<>());

    assertThat(library.getTotalVulnerabilities()).isEqualTo(3);
  }

  @Test
  void getTotalVulnerabilities_should_ignore_api_value_when_array_has_different_count() {
    // Simulates the bug: API says 0 but array has 4 CVEs
    var library = new LibraryExtended();
    library.setTotalVulnerabilities(0);

    var vulns =
        List.of(
            new LibraryVulnerabilityExtended(),
            new LibraryVulnerabilityExtended(),
            new LibraryVulnerabilityExtended(),
            new LibraryVulnerabilityExtended());
    library.setVulnerabilities(vulns);

    assertThat(library.getTotalVulnerabilities()).isEqualTo(4);
  }

  @Test
  void criticalVulnerabilities_should_be_settable_and_gettable() {
    var library = new LibraryExtended();
    library.setCriticalVulnerabilities(5);

    assertThat(library.getCriticalVulnerabilities()).isEqualTo(5);
  }

  // ---- helper ----

  private LibraryVulnerabilityExtended vuln(String severityCode) {
    var v = new LibraryVulnerabilityExtended();
    v.setSeverityCode(severityCode);
    return v;
  }

  // ---- getMediumVulnerabilities ----

  @Test
  void getMediumVulnerabilities_should_return_count_when_medium_vulns_present() {
    var library = new LibraryExtended();
    library.setVulnerabilities(List.of(vuln("MEDIUM"), vuln("MEDIUM"), vuln("HIGH")));

    assertThat(library.getMediumVulnerabilities()).isEqualTo(2);
  }

  @Test
  void getMediumVulnerabilities_should_return_0_when_vulnerabilities_null() {
    var library = new LibraryExtended();
    // vulnerabilities is null by default

    assertThat(library.getMediumVulnerabilities()).isEqualTo(0);
  }

  @Test
  void getMediumVulnerabilities_should_return_0_when_vulnerabilities_empty() {
    var library = new LibraryExtended();
    library.setVulnerabilities(new ArrayList<>());

    assertThat(library.getMediumVulnerabilities()).isEqualTo(0);
  }

  // ---- getLowVulnerabilities ----

  @Test
  void getLowVulnerabilities_should_return_count_when_low_vulns_present() {
    var library = new LibraryExtended();
    library.setVulnerabilities(List.of(vuln("LOW"), vuln("HIGH")));

    assertThat(library.getLowVulnerabilities()).isEqualTo(1);
  }

  @Test
  void getLowVulnerabilities_should_return_0_when_vulnerabilities_null() {
    var library = new LibraryExtended();

    assertThat(library.getLowVulnerabilities()).isEqualTo(0);
  }

  @Test
  void getLowVulnerabilities_should_return_0_when_vulnerabilities_empty() {
    var library = new LibraryExtended();
    library.setVulnerabilities(new ArrayList<>());

    assertThat(library.getLowVulnerabilities()).isEqualTo(0);
  }

  // ---- getNoteVulnerabilities ----

  @Test
  void getNoteVulnerabilities_should_return_count_when_note_vulns_present() {
    var library = new LibraryExtended();
    library.setVulnerabilities(List.of(vuln("NOTE"), vuln("NOTE"), vuln("NOTE")));

    assertThat(library.getNoteVulnerabilities()).isEqualTo(3);
  }

  @Test
  void getNoteVulnerabilities_should_return_0_when_vulnerabilities_null() {
    var library = new LibraryExtended();

    assertThat(library.getNoteVulnerabilities()).isEqualTo(0);
  }

  @Test
  void getNoteVulnerabilities_should_return_0_when_vulnerabilities_empty() {
    var library = new LibraryExtended();
    library.setVulnerabilities(new ArrayList<>());

    assertThat(library.getNoteVulnerabilities()).isEqualTo(0);
  }

  // ---- cross-severity isolation ----

  @Test
  void get_vulnerability_counts_should_return_only_matching_severity_from_mixed_array() {
    var library = new LibraryExtended();
    library.setVulnerabilities(List.of(vuln("MEDIUM"), vuln("MEDIUM"), vuln("LOW"), vuln("NOTE")));

    assertThat(library.getMediumVulnerabilities()).isEqualTo(2);
    assertThat(library.getLowVulnerabilities()).isEqualTo(1);
    assertThat(library.getNoteVulnerabilities()).isEqualTo(1);
  }

  @Test
  void getLowVulnerabilities_should_return_0_when_absent_from_non_empty_array() {
    var library = new LibraryExtended();
    library.setVulnerabilities(List.of(vuln("MEDIUM"), vuln("MEDIUM"), vuln("NOTE")));

    assertThat(library.getMediumVulnerabilities()).isEqualTo(2);
    assertThat(library.getLowVulnerabilities()).isEqualTo(0);
    assertThat(library.getNoteVulnerabilities()).isEqualTo(1);
  }

  // ---- case-insensitive matching ----

  @Test
  void get_vulnerability_counts_should_match_lowercase_severity_codes() {
    var library = new LibraryExtended();
    library.setVulnerabilities(List.of(vuln("medium"), vuln("low"), vuln("note")));

    assertThat(library.getMediumVulnerabilities()).isEqualTo(1);
    assertThat(library.getLowVulnerabilities()).isEqualTo(1);
    assertThat(library.getNoteVulnerabilities()).isEqualTo(1);
  }

  @Test
  void get_vulnerability_counts_should_match_mixed_case_severity_codes() {
    var library = new LibraryExtended();
    library.setVulnerabilities(List.of(vuln("Medium"), vuln("Low"), vuln("Note")));

    assertThat(library.getMediumVulnerabilities()).isEqualTo(1);
    assertThat(library.getLowVulnerabilities()).isEqualTo(1);
    assertThat(library.getNoteVulnerabilities()).isEqualTo(1);
  }

  // ---- null severityCode handling ----

  @Test
  void get_vulnerability_counts_should_return_0_when_vuln_has_null_severity_code() {
    var library = new LibraryExtended();
    library.setVulnerabilities(List.of(vuln(null), vuln("HIGH")));

    assertThat(library.getMediumVulnerabilities()).isEqualTo(0);
    assertThat(library.getLowVulnerabilities()).isEqualTo(0);
    assertThat(library.getNoteVulnerabilities()).isEqualTo(0);
  }
}

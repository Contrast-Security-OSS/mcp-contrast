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
}

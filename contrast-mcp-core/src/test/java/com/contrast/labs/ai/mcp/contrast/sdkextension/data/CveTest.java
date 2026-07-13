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

import com.contrastsecurity.sdk.internal.GsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CveTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void jackson_should_omit_unavailable_cve_fields() throws Exception {
    var cve = new Cve();
    cve.setName("CVE-2015-4000");
    var cvssv2 = new CvssV2();
    cvssv2.setSeverity("High");
    cve.setCvssv2(cvssv2);

    var json = objectMapper.writeValueAsString(cve);

    assertThat(json)
        .contains("\"name\":\"CVE-2015-4000\"", "\"severity\":\"High\"")
        .doesNotContain("\"score\"", "\"uuid\"", "\"references\"");
  }

  @Test
  void gson_should_preserve_legacy_flat_cve_fields_used_by_protect_rules() {
    var protectData =
        GsonFactory.create()
            .fromJson(
                """
                {
                  "success": true,
                  "rules": [{
                    "name": "Log4Shell",
                    "type": "CVE",
                    "cves": [{
                      "name": "CVE-2021-44228",
                      "uuid": "cve-2021-44228",
                      "score": 9.3,
                      "accessVector": "NETWORK",
                      "accessComplexity": "LOW",
                      "authentication": "NONE",
                      "confidentialityImpact": "COMPLETE",
                      "integrityImpact": "COMPLETE",
                      "availabilityImpact": "COMPLETE",
                      "references": [123]
                    }]
                  }]
                }
                """,
                ProtectData.class);

    assertThat(protectData.getRules())
        .singleElement()
        .satisfies(
            rule ->
                assertThat(rule.getCves())
                    .singleElement()
                    .satisfies(
                        cve -> {
                          assertThat(cve.getUuid()).isEqualTo("cve-2021-44228");
                          assertThat(cve.getScore()).isEqualTo(9.3);
                          assertThat(cve.getAccessVector()).isEqualTo("NETWORK");
                          assertThat(cve.getReferences()).containsExactly("123");
                        }));
  }
}

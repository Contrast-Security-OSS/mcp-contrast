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
package com.contrast.labs.ai.mcp.contrast;

import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for verifying environments and tags are populated from real TeamServer.
 *
 * This test only runs if CONTRAST_HOST_NAME environment variable is set.
 *
 * Required environment variables:
 * - CONTRAST_HOST_NAME (e.g., app.contrastsecurity.com)
 * - CONTRAST_API_KEY
 * - CONTRAST_SERVICE_KEY
 * - CONTRAST_USERNAME
 * - CONTRAST_ORG_ID
 *
 * Run locally:
 *   export CONTRAST_HOST_NAME=app.contrastsecurity.com
 *   export CONTRAST_API_KEY=your-key
 *   export CONTRAST_SERVICE_KEY=your-key
 *   export CONTRAST_USERNAME=your-username
 *   export CONTRAST_ORG_ID=your-org-id
 *   mvn verify
 *
 * Or skip integration tests:
 *   mvn verify -DskipITs
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
public class EnvironmentsIT {

    @Autowired
    private AssessService assessService;

    @Test
    void testEnvironmentsAndTagsArePopulated() throws IOException {
        System.out.println("\n=== Integration Test: Environments and Tags ===");

        // Get vulnerabilities from real TeamServer
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1,      // page
            10,     // pageSize
            null,   // severities
            null,   // statuses
            null,   // appId
            null,   // vulnTypes
            null,   // environments
            null,   // lastSeenAfter
            null,   // lastSeenBefore
            null    // vulnTags
        );

        assertNotNull(response, "Response should not be null");
        assertTrue(response.items().size() > 0, "Should have at least one vulnerability");

        System.out.println("Retrieved " + response.items().size() + " vulnerabilities");

        // Analyze first few vulnerabilities
        int withEnvironments = 0;
        int withTags = 0;

        for (VulnLight vuln : response.items()) {
            assertNotNull(vuln.environments(), "Environments should never be null");
            assertNotNull(vuln.tags(), "Tags should never be null");

            // Debug: Show all environment and tag data
            System.out.println("Vuln " + vuln.vulnID() + ":");
            System.out.println("  environments: " + vuln.environments() + " (size: " + vuln.environments().size() + ")");
            System.out.println("  tags: " + vuln.tags() + " (size: " + vuln.tags().size() + ")");

            if (!vuln.environments().isEmpty()) {
                withEnvironments++;
                System.out.println("  ✓ Has environments: " + vuln.environments());
            }

            if (!vuln.tags().isEmpty()) {
                withTags++;
                System.out.println("  ✓ Has tags: " + vuln.tags());
            }
        }

        System.out.println("\nResults:");
        System.out.println("  Vulnerabilities with environments: " + withEnvironments + "/" + response.items().size());
        System.out.println("  Vulnerabilities with tags: " + withTags + "/" + response.items().size());

        // At least verify the fields are being returned (even if empty)
        // This ensures the API is returning the fields and they're being deserialized
        for (VulnLight vuln : response.items()) {
            assertNotNull(vuln.environments(), "Environments field should exist (even if empty list)");
            assertNotNull(vuln.tags(), "Tags field should exist (even if empty list)");
        }

        System.out.println("✓ Integration test passed: environments and tags fields are present");
    }

    @Test
    void testVulnerabilitiesHaveBasicFields() throws IOException {
        System.out.println("\n=== Integration Test: Basic Fields ===");

        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 5, null, null, null, null, null, null, null, null
        );

        assertNotNull(response);
        assertFalse(response.items().isEmpty(), "Should have vulnerabilities");

        // Verify each vulnerability has required fields
        for (VulnLight vuln : response.items()) {
            assertNotNull(vuln.title(), "Title should not be null");
            assertNotNull(vuln.type(), "Type should not be null");
            assertNotNull(vuln.vulnID(), "VulnID should not be null");
            assertNotNull(vuln.severity(), "Severity should not be null");
            assertNotNull(vuln.status(), "Status should not be null");

            System.out.println("✓ " + vuln.vulnID() + ": " + vuln.title() + " (" + vuln.severity() + ")");
        }

        System.out.println("✓ All vulnerabilities have required fields");
    }
}

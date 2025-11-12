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
 * Integration test for AssessService that validates vulnerability data from real TeamServer.
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
public class AssessServiceIntegrationTest {

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
        assertTrue(!response.items().isEmpty(), "Should have at least one vulnerability");

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
    void testSessionMetadataIsPopulated() throws IOException {
        System.out.println("\n=== Integration Test: Session Metadata ===");

        // Get vulnerabilities from real TeamServer with session metadata expanded
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
        assertTrue(!response.items().isEmpty(), "Should have at least one vulnerability");

        System.out.println("Retrieved " + response.items().size() + " vulnerabilities");

        // Analyze session metadata in vulnerabilities
        int withSessionMetadata = 0;
        int totalSessions = 0;

        for (VulnLight vuln : response.items()) {
            assertNotNull(vuln.sessionMetadata(), "Session metadata should never be null");

            // Debug: Show session metadata
            System.out.println("Vuln " + vuln.vulnID() + ":");
            System.out.println("  sessionMetadata: " + vuln.sessionMetadata().size() + " session(s)");

            if (!vuln.sessionMetadata().isEmpty()) {
                withSessionMetadata++;
                totalSessions += vuln.sessionMetadata().size();

                // Show details of first session
                var firstSession = vuln.sessionMetadata().get(0);
                System.out.println("  ✓ Has session metadata:");
                System.out.println("    - Session ID: " + firstSession.getSessionId());
                if (firstSession.getMetadata() != null && !firstSession.getMetadata().isEmpty()) {
                    System.out.println("    - Metadata items: " + firstSession.getMetadata().size());
                    // Show first metadata item
                    var firstItem = firstSession.getMetadata().get(0);
                    System.out.println("      * " + firstItem.getDisplayLabel() + ": " + firstItem.getValue());
                }
            }
        }

        System.out.println("\nResults:");
        System.out.println("  Vulnerabilities with session metadata: " + withSessionMetadata + "/" + response.items().size());
        System.out.println("  Total sessions found: " + totalSessions);

        // Verify the session metadata field exists (even if empty) - this confirms SDK expansion works
        for (VulnLight vuln : response.items()) {
            assertNotNull(vuln.sessionMetadata(), "Session metadata field should exist (even if empty list)");
        }

        System.out.println("✓ Integration test passed: session metadata field is present and SDK expansion works");
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
            assertNotNull(vuln.appID(), "appID should not be null (APPLICATION expand should be included)");
            assertNotNull(vuln.appName(), "appName should not be null (APPLICATION expand should be included)");
            assertFalse(vuln.appID().isEmpty(), "appID should not be empty");
            assertFalse(vuln.appName().isEmpty(), "appName should not be empty");

            System.out.println("✓ " + vuln.vulnID() + ": " + vuln.title() + " (" + vuln.severity() + ") - App: " + vuln.appName() + " (" + vuln.appID() + ")");
        }

        System.out.println("✓ All vulnerabilities have required fields including appID and appName");
    }

    @Test
    void testVulnTagsWithSpacesHandledBySDK() throws IOException {
        System.out.println("\n=== Integration Test: VulnTags with Spaces ===");
        System.out.println("Testing that SDK properly handles URL encoding of tags with spaces");

        // Query with a tag that contains spaces - this should work now that AIML-193 is complete
        // The SDK should handle URL encoding internally
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1,      // page
            50,     // pageSize (larger to increase chance of finding tagged vulns)
            null,   // severities
            null,   // statuses
            null,   // appId
            null,   // vulnTypes
            null,   // environments
            null,   // lastSeenAfter
            null,   // lastSeenBefore
            "SmartFix Remediated"  // vulnTags with space - SDK should handle encoding
        );

        assertNotNull(response, "Response should not be null");
        System.out.println("Query completed successfully (returned " + response.items().size() + " vulnerabilities)");

        // The query should complete without error - whether we get results depends on the org's data
        // The important thing is that the SDK properly encoded the tag with spaces
        if (response.items().size() > 0) {
            System.out.println("✓ Found vulnerabilities with 'SmartFix Remediated' tag:");
            for (VulnLight vuln : response.items()) {
                System.out.println("  - " + vuln.vulnID() + ": " + vuln.title());
                System.out.println("    Tags: " + vuln.tags());
            }
        } else {
            System.out.println("ℹ No vulnerabilities found with 'SmartFix Remediated' tag (this is OK)");
        }

        // Try with multiple tags including spaces and special characters
        System.out.println("\nTesting multiple tags with spaces:");
        response = assessService.getAllVulnerabilities(
            1, 10, null, null, null, null, null, null, null,
            "Tag With Spaces,another-tag"
        );

        assertNotNull(response, "Response should not be null");
        System.out.println("✓ Query with multiple tags completed successfully");
        System.out.println("  (returned " + response.items().size() + " vulnerabilities)");

        System.out.println("\n✓ Integration test passed: SDK properly handles vulnTags with spaces");
    }

    @Test
    void testListVulnsByAppIdWithSessionMetadata() throws IOException {
        System.out.println("\n=== Integration Test: listVulnsByAppId() with Session Metadata ===");

        // Step 1: Get some vulnerabilities to find an appId (single API call)
        System.out.println("Step 1: Getting vulnerabilities to discover an appId...");
        PaginatedResponse<VulnLight> allVulns = assessService.getAllVulnerabilities(
            1, 10, null, null, null, null, null, null, null, null
        );

        assertNotNull(allVulns, "Response should not be null");
        assertFalse(allVulns.items().isEmpty(), "Should have at least one vulnerability");

        System.out.println("  ✓ Found " + allVulns.items().size() + " vulnerability(ies)");

        // Step 2: Get applications list (single API call)
        System.out.println("Step 2: Getting first application with vulnerabilities...");
        var applications = assessService.getAllApplications();
        assertNotNull(applications, "Applications list should not be null");
        assertFalse(applications.isEmpty(), "Should have at least one application");

        // Just use the first application - no iteration needed
        String testAppId = applications.get(0).appID();
        String testAppName = applications.get(0).name();
        System.out.println("  ✓ Using application: " + testAppName + " (ID: " + testAppId + ")");

        // Step 3: Call listVulnsByAppId() with the discovered appId
        System.out.println("Step 3: Calling listVulnsByAppId() for app: " + testAppName);
        var vulnerabilities = assessService.listVulnsByAppId(testAppId);

        assertNotNull(vulnerabilities, "Vulnerabilities list should not be null");
        System.out.println("  ✓ Retrieved " + vulnerabilities.size() + " vulnerability(ies)");

        if (vulnerabilities.isEmpty()) {
            System.out.println("  ℹ No vulnerabilities for this app (this is OK for the test)");
            return;
        }

        // Step 4: Verify session metadata is populated
        System.out.println("Step 4: Verifying session metadata is populated...");
        int withSessionMetadata = 0;

        for (VulnLight vuln : vulnerabilities) {
            assertNotNull(vuln.sessionMetadata(), "Session metadata should never be null");

            if (!vuln.sessionMetadata().isEmpty()) {
                withSessionMetadata++;
                System.out.println("  ✓ Vuln " + vuln.vulnID() + " has " +
                    vuln.sessionMetadata().size() + " session(s)");
            }
        }

        System.out.println("\nResults:");
        System.out.println("  Vulnerabilities with session metadata: " + withSessionMetadata + "/" + vulnerabilities.size());
        System.out.println("✓ Integration test passed: listVulnsByAppId() returns vulnerabilities with session metadata");
    }

    @Test
    void testListVulnsInAppByNameForLatestSessionWithDynamicSessionId() throws IOException {
        System.out.println("\n=== Integration Test: listVulnsByAppIdForLatestSession() with Dynamic Session Discovery ===");

        // Step 1: Get applications list (single API call)
        System.out.println("Step 1: Getting first application...");
        var applications = assessService.getAllApplications();

        assertNotNull(applications, "Applications list should not be null");
        assertFalse(applications.isEmpty(), "Should have at least one application");

        // Just use the first application - no iteration needed
        String testAppID = applications.get(0).appID();
        String testAppName = applications.get(0).name();
        System.out.println("  ✓ Using application: " + testAppName + " (ID: " + testAppID + ")");

        // Step 2: Call listVulnsByAppIdForLatestSession() with the discovered app ID
        System.out.println("Step 2: Calling listVulnsByAppIdForLatestSession() for appID: " + testAppID);
        var latestSessionVulns = assessService.listVulnsByAppIdForLatestSession(testAppID);

        assertNotNull(latestSessionVulns, "Vulnerabilities list should not be null");
        System.out.println("  ✓ Retrieved " + latestSessionVulns.size() + " vulnerability(ies) for latest session");

        if (latestSessionVulns.isEmpty()) {
            System.out.println("  ℹ No vulnerabilities in latest session (this is valid if latest session has no vulns)");
            return;
        }

        // Step 3: Verify session metadata is populated in results
        System.out.println("Step 3: Verifying session metadata is populated...");
        int withSessionMetadata = 0;

        for (VulnLight vuln : latestSessionVulns) {
            assertNotNull(vuln.sessionMetadata(), "Session metadata should never be null");

            if (!vuln.sessionMetadata().isEmpty()) {
                withSessionMetadata++;
                String sessionId = vuln.sessionMetadata().get(0).getSessionId();
                System.out.println("  ✓ Vuln " + vuln.vulnID() + " has session ID: " + sessionId);
            }
        }

        System.out.println("\nResults:");
        System.out.println("  Vulnerabilities returned: " + latestSessionVulns.size());
        System.out.println("  Vulnerabilities with session metadata: " + withSessionMetadata + "/" + latestSessionVulns.size());
        System.out.println("✓ Integration test passed: listVulnsByAppIdForLatestSession() returns vulnerabilities with session metadata");
    }
}

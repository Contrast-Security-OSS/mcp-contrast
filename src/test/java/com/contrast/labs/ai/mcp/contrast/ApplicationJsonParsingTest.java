package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for Application JSON parsing. Tests the deserialization of Application objects from
 * JSON, specifically focusing on the missingRequiredFields structure that caused parsing issues.
 */
public class ApplicationJsonParsingTest {

  private final Gson gson = new Gson();

  @Test
  @DisplayName("Test Application JSON parsing with missingRequiredFields as objects")
  public void testApplicationParsingWithMissingRequiredFieldsObjects() {
    // JSON that matches what TeamServer API returns
    String applicationJson =
        """
        {
            "app_id": "test-app-123",
            "name": "Test Application",
            "status": "online",
            "language": "JAVA",
            "last_seen": 1692467436000,
            "size": 12345,
            "missingRequiredFields": [
                {
                    "fieldId": "29",
                    "fieldType": "STRING",
                    "displayLabel": "Custom Name",
                    "agentLabel": "customName",
                    "required": true,
                    "unique": false,
                    "subfields": null,
                    "links": []
                },
                {
                    "fieldId": "30",
                    "fieldType": "SELECT",
                    "displayLabel": "Environment",
                    "agentLabel": "environment",
                    "required": true,
                    "unique": false,
                    "subfields": null,
                    "links": []
                }
            ],
            "validationErrorFields": [],
            "metadataEntities": [],
            "tags": [],
            "techs": []
        }
        """;

    // This should not throw an exception with the fix
    var application = gson.fromJson(applicationJson, Application.class);

    // Verify basic fields
    assertThat(application).as("Application should not be null").isNotNull();
    assertThat(application.getAppId()).as("App ID should match").isEqualTo("test-app-123");
    assertThat(application.getName()).as("App name should match").isEqualTo("Test Application");
    assertThat(application.getStatus()).as("Status should match").isEqualTo("online");

    // Verify missingRequiredFields parsing
    assertThat(application.getMissingRequiredFields())
        .as("Missing required fields should not be null")
        .isNotNull();
    assertThat(application.getMissingRequiredFields())
        .as("Should have 2 missing required fields")
        .hasSize(2);

    // Verify first missing required field
    var firstField = application.getMissingRequiredFields().get(0);
    assertThat(firstField.getFieldId()).as("First field ID should match").isEqualTo("29");
    assertThat(firstField.getFieldType()).as("First field type should match").isEqualTo("STRING");
    assertThat(firstField.getDisplayLabel())
        .as("First field display label should match")
        .isEqualTo("Custom Name");
    assertThat(firstField.getAgentLabel())
        .as("First field agent label should match")
        .isEqualTo("customName");
    assertThat(firstField.isRequired()).as("First field should be required").isTrue();
    assertThat(firstField.isUnique()).as("First field should not be unique").isFalse();

    // Verify second missing required field
    var secondField = application.getMissingRequiredFields().get(1);
    assertThat(secondField.getFieldId()).as("Second field ID should match").isEqualTo("30");
    assertThat(secondField.getFieldType()).as("Second field type should match").isEqualTo("SELECT");
    assertThat(secondField.getDisplayLabel())
        .as("Second field display label should match")
        .isEqualTo("Environment");
    assertThat(secondField.getAgentLabel())
        .as("Second field agent label should match")
        .isEqualTo("environment");
    assertThat(secondField.isRequired()).as("Second field should be required").isTrue();
    assertThat(secondField.isUnique()).as("Second field should not be unique").isFalse();
  }

  @Test
  @DisplayName("Test Application JSON parsing with empty missingRequiredFields")
  public void testApplicationParsingWithEmptyMissingRequiredFields() {
    String applicationJson =
        """
        {
            "app_id": "test-app-456",
            "name": "Test Application 2",
            "status": "offline",
            "language": "JAVASCRIPT",
            "last_seen": 1692467436000,
            "size": 54321,
            "missingRequiredFields": [],
            "validationErrorFields": [],
            "metadataEntities": [],
            "tags": [],
            "techs": []
        }
        """;

    var application = gson.fromJson(applicationJson, Application.class);

    assertThat(application).as("Application should not be null").isNotNull();
    assertThat(application.getAppId()).as("App ID should match").isEqualTo("test-app-456");
    assertThat(application.getMissingRequiredFields())
        .as("Missing required fields should not be null")
        .isNotNull();
    assertThat(application.getMissingRequiredFields())
        .as("Missing required fields should be empty")
        .isEmpty();
  }

  @Test
  @DisplayName("Test Application JSON parsing with null missingRequiredFields")
  public void testApplicationParsingWithNullMissingRequiredFields() {
    String applicationJson =
        """
        {
            "app_id": "test-app-789",
            "name": "Test Application 3",
            "status": "online",
            "language": "PYTHON",
            "last_seen": 1692467436000,
            "size": 98765,
            "validationErrorFields": [],
            "metadataEntities": [],
            "tags": [],
            "techs": []
        }
        """;

    // Should handle missing missingRequiredFields field gracefully
    var application = gson.fromJson(applicationJson, Application.class);

    assertThat(application).as("Application should not be null").isNotNull();
    assertThat(application.getAppId()).as("App ID should match").isEqualTo("test-app-789");
    // Field returns empty list when not present in JSON (never null collections pattern)
    assertThat(application.getMissingRequiredFields())
        .as("Missing required fields should be empty when not in JSON")
        .isEmpty();
  }
}

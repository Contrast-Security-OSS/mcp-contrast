package com.contrast.labs.ai.mcp.contrast;

import static org.junit.jupiter.api.Assertions.*;

import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.Application;
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
    assertNotNull(application, "Application should not be null");
    assertEquals("test-app-123", application.getAppId(), "App ID should match");
    assertEquals("Test Application", application.getName(), "App name should match");
    assertEquals("online", application.getStatus(), "Status should match");

    // Verify missingRequiredFields parsing
    assertNotNull(
        application.getMissingRequiredFields(), "Missing required fields should not be null");
    assertEquals(
        2, application.getMissingRequiredFields().size(), "Should have 2 missing required fields");

    // Verify first missing required field
    var firstField = application.getMissingRequiredFields().get(0);
    assertEquals("29", firstField.getFieldId(), "First field ID should match");
    assertEquals("STRING", firstField.getFieldType(), "First field type should match");
    assertEquals(
        "Custom Name", firstField.getDisplayLabel(), "First field display label should match");
    assertEquals("customName", firstField.getAgentLabel(), "First field agent label should match");
    assertTrue(firstField.isRequired(), "First field should be required");
    assertFalse(firstField.isUnique(), "First field should not be unique");

    // Verify second missing required field
    var secondField = application.getMissingRequiredFields().get(1);
    assertEquals("30", secondField.getFieldId(), "Second field ID should match");
    assertEquals("SELECT", secondField.getFieldType(), "Second field type should match");
    assertEquals(
        "Environment", secondField.getDisplayLabel(), "Second field display label should match");
    assertEquals(
        "environment", secondField.getAgentLabel(), "Second field agent label should match");
    assertTrue(secondField.isRequired(), "Second field should be required");
    assertFalse(secondField.isUnique(), "Second field should not be unique");
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

    assertNotNull(application, "Application should not be null");
    assertEquals("test-app-456", application.getAppId(), "App ID should match");
    assertNotNull(
        application.getMissingRequiredFields(), "Missing required fields should not be null");
    assertTrue(
        application.getMissingRequiredFields().isEmpty(),
        "Missing required fields should be empty");
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

    assertNotNull(application, "Application should not be null");
    assertEquals("test-app-789", application.getAppId(), "App ID should match");
    // Field should be null when not present in JSON
    assertNull(
        application.getMissingRequiredFields(),
        "Missing required fields should be null when not in JSON");
  }
}

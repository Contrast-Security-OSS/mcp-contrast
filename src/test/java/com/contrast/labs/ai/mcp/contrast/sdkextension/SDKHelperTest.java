package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

class SDKHelperTest {

  @Mock private Environment environment;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Inject mocked Environment into SDKHelper's static field using reflection
    var envField = SDKHelper.class.getDeclaredField("environment");
    envField.setAccessible(true);
    envField.set(null, environment);

    // Set up default property
    when(environment.getProperty("contrast.api.protocol", "https")).thenReturn("https");
  }

  @Test
  void testGetProtocolAndServer_WithNull() {
    assertNull(SDKHelper.getProtocolAndServer(null));
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocol() {
    var result = SDKHelper.getProtocolAndServer("http://example.com");
    assertEquals("http://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithHttpsProtocol() {
    var result = SDKHelper.getProtocolAndServer("https://example.com");
    assertEquals("https://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithoutProtocol() {
    var result = SDKHelper.getProtocolAndServer("example.com");
    assertEquals("https://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithCustomProtocol() {
    when(environment.getProperty("contrast.api.protocol", "https")).thenReturn("http");

    var result = SDKHelper.getProtocolAndServer("example.com");
    assertEquals("http://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithEmptyString() {
    // Empty string should return null (consistent with null input handling)
    var result = SDKHelper.getProtocolAndServer("");
    assertNull(result);
  }

  @Test
  void testGetProtocolAndServer_WithWhitespaceOnly() {
    // Whitespace-only string should return null (consistent with null input handling)
    var result = SDKHelper.getProtocolAndServer("   ");
    assertNull(result);
  }

  @Test
  void testGetProtocolAndServer_WithLeadingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("  example.com");
    assertEquals("https://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithTrailingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("example.com  ");
    assertEquals("https://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithLeadingAndTrailingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("  https://example.com  ");
    assertEquals("https://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithInvalidProtocol_Ftp() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              SDKHelper.getProtocolAndServer("ftp://example.com");
            });

    assertTrue(exception.getMessage().contains("Invalid protocol"));
    assertTrue(exception.getMessage().contains("ftp://example.com"));
  }

  @Test
  void testGetProtocolAndServer_WithInvalidProtocol_Custom() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              SDKHelper.getProtocolAndServer("custom://example.com");
            });

    assertTrue(exception.getMessage().contains("Invalid protocol"));
  }

  @Test
  void testGetProtocolAndServer_WithMalformedProtocol() {
    // "ht://example.com" contains "://" but doesn't start with http:// or https://
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              SDKHelper.getProtocolAndServer("ht://example.com");
            });

    assertTrue(exception.getMessage().contains("Invalid protocol"));
  }

  @Test
  void testGetSDK_WithHttpsUrl() {
    var hostWithProtocol = "https://custom.example.com";
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    // getSDK is a public static method, so we can call it directly
    try {
      var sdk = SDKHelper.getSDK(hostWithProtocol, "apiKey", "serviceKey", "username", null, null);

      assertNotNull(sdk);
      // The SDK was successfully created with the https URL.
      // Detailed URL validation would require accessing ContrastSDK's internal state,
      // which is beyond the scope of a unit test and better suited for integration tests.
    } catch (Exception e) {
      fail("Exception occurred: " + e.getMessage());
    }
  }

  @Test
  void testGetSDK_WithHostnameOnly() {
    var hostname = "example.contrastsecurity.com";
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    try {
      var sdk = SDKHelper.getSDK(hostname, "apiKey", "serviceKey", "username", null, null);

      assertNotNull(sdk);
      // The SDK should prepend https:// by default
    } catch (Exception e) {
      fail("Exception occurred: " + e.getMessage());
    }
  }

  @Test
  void testGetProtocolAndServer_WithTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("example.com/");
    assertEquals("https://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithProtocolAndTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("https://example.com/");
    assertEquals("https://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocolAndTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("http://example.com/");
    assertEquals("http://example.com", result);
  }

  @Test
  void testGetProtocolAndServer_WithMultipleTrailingSlashes() {
    // Note: Only one trailing slash is removed
    var result = SDKHelper.getProtocolAndServer("example.com//");
    assertEquals("https://example.com/", result);
  }
}

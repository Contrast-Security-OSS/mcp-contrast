package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    assertThat(SDKHelper.getProtocolAndServer(null)).isNull();
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocol() {
    var result = SDKHelper.getProtocolAndServer("http://example.com");
    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpsProtocol() {
    var result = SDKHelper.getProtocolAndServer("https://example.com");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithoutProtocol() {
    var result = SDKHelper.getProtocolAndServer("example.com");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithCustomProtocol() {
    when(environment.getProperty("contrast.api.protocol", "https")).thenReturn("http");

    var result = SDKHelper.getProtocolAndServer("example.com");
    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithEmptyString() {
    // Empty string should return null (consistent with null input handling)
    var result = SDKHelper.getProtocolAndServer("");
    assertThat(result).isNull();
  }

  @Test
  void testGetProtocolAndServer_WithWhitespaceOnly() {
    // Whitespace-only string should return null (consistent with null input handling)
    var result = SDKHelper.getProtocolAndServer("   ");
    assertThat(result).isNull();
  }

  @Test
  void testGetProtocolAndServer_WithLeadingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("  example.com");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithTrailingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("example.com  ");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithLeadingAndTrailingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("  https://example.com  ");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithInvalidProtocol_Ftp() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("ftp://example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid protocol")
        .hasMessageContaining("ftp://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithInvalidProtocol_Custom() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("custom://example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid protocol");
  }

  @Test
  void testGetProtocolAndServer_WithMalformedProtocol() {
    // "ht://example.com" contains "://" but doesn't start with http:// or https://
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("ht://example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid protocol");
  }

  @Test
  void testGetSDK_WithHttpsUrl() {
    var hostWithProtocol = "https://custom.example.com";
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    // getSDK is a public static method, so we can call it directly
    var sdk = SDKHelper.getSDK(hostWithProtocol, "apiKey", "serviceKey", "username", null, null);

    assertThat(sdk).isNotNull();
    // The SDK was successfully created with the https URL.
    // Detailed URL validation would require accessing ContrastSDK's internal state,
    // which is beyond the scope of a unit test and better suited for integration tests.
  }

  @Test
  void testGetSDK_WithHostnameOnly() {
    var hostname = "example.contrastsecurity.com";
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    var sdk = SDKHelper.getSDK(hostname, "apiKey", "serviceKey", "username", null, null);

    assertThat(sdk).isNotNull();
    // The SDK should prepend https:// by default
  }

  @Test
  void testGetProtocolAndServer_WithTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("example.com/");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithProtocolAndTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("https://example.com/");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocolAndTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("http://example.com/");
    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithMultipleTrailingSlashes() {
    // Note: Only one trailing slash is removed
    var result = SDKHelper.getProtocolAndServer("example.com//");
    assertThat(result).isEqualTo("https://example.com/");
  }
}

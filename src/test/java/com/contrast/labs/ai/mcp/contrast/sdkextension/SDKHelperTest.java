package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibrariesExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrastsecurity.http.LibraryFilterForm;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

class SDKHelperTest {

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_APP_ID = "test-app-456";

  @Mock private Environment environment;
  @Mock private SDKExtension extendedSDK;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Inject mocked Environment into SDKHelper's static field using reflection
    var envField = SDKHelper.class.getDeclaredField("environment");
    envField.setAccessible(true);
    envField.set(null, environment);
  }

  @Test
  void testGetProtocolAndServer_WithNull() {
    assertThat(SDKHelper.getProtocolAndServer(null, "https")).isNull();
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocol() {
    var result = SDKHelper.getProtocolAndServer("http://example.com", "https");
    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpsProtocol() {
    var result = SDKHelper.getProtocolAndServer("https://example.com", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithoutProtocol() {
    var result = SDKHelper.getProtocolAndServer("example.com", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocolConfig() {
    // When protocol config is "http", hostnames without protocol should use http
    var result = SDKHelper.getProtocolAndServer("example.com", "http");
    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithEmptyString() {
    // Empty string should return null (consistent with null input handling)
    var result = SDKHelper.getProtocolAndServer("", "https");
    assertThat(result).isNull();
  }

  @Test
  void testGetProtocolAndServer_WithWhitespaceOnly() {
    // Whitespace-only string should return null (consistent with null input handling)
    var result = SDKHelper.getProtocolAndServer("   ", "https");
    assertThat(result).isNull();
  }

  @Test
  void testGetProtocolAndServer_WithLeadingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("  example.com", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithTrailingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("example.com  ", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithLeadingAndTrailingWhitespace() {
    var result = SDKHelper.getProtocolAndServer("  https://example.com  ", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithInvalidProtocol_Ftp() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("ftp://example.com", "https"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid protocol")
        .hasMessageContaining("ftp://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithInvalidProtocol_Custom() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("custom://example.com", "https"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid protocol");
  }

  @Test
  void testGetProtocolAndServer_WithMalformedProtocol() {
    // "ht://example.com" contains "://" but doesn't start with http:// or https://
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("ht://example.com", "https"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid protocol");
  }

  @Test
  void testGetProtocolAndServer_WithNullProtocol_DefaultsToHttps() {
    // When protocol parameter is null, should default to https
    var result = SDKHelper.getProtocolAndServer("example.com", null);
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithEmptyProtocol_DefaultsToHttps() {
    // When protocol parameter is empty, should default to https
    var result = SDKHelper.getProtocolAndServer("example.com", "");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithWhitespaceProtocol_DefaultsToHttps() {
    // When protocol parameter is whitespace, should default to https
    var result = SDKHelper.getProtocolAndServer("example.com", "   ");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetSDK_WithHttpsUrl() {
    var hostWithProtocol = "https://custom.example.com";
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    // getSDK is a public static method, so we can call it directly
    var sdk =
        SDKHelper.getSDK(hostWithProtocol, "apiKey", "serviceKey", "username", null, null, "https");

    assertThat(sdk).isNotNull();
    // The SDK was successfully created with the https URL.
    // Detailed URL validation would require accessing ContrastSDK's internal state,
    // which is beyond the scope of a unit test and better suited for integration tests.
  }

  @Test
  void testGetSDK_WithHostnameOnly() {
    var hostname = "example.contrastsecurity.com";
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    var sdk = SDKHelper.getSDK(hostname, "apiKey", "serviceKey", "username", null, null, "https");

    assertThat(sdk).isNotNull();
    // The SDK should prepend https:// by default
  }

  @Test
  void testGetSDK_WithHttpProtocol() {
    var hostname = "example.contrastsecurity.com";
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    var sdk = SDKHelper.getSDK(hostname, "apiKey", "serviceKey", "username", null, null, "http");

    assertThat(sdk).isNotNull();
    // The SDK should prepend http:// when protocol is "http"
  }

  @Test
  void testGetProtocolAndServer_WithTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("example.com/", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithProtocolAndTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("https://example.com/", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocolAndTrailingSlash() {
    var result = SDKHelper.getProtocolAndServer("http://example.com/", "https");
    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithMultipleTrailingSlashes() {
    // Note: Only one trailing slash is removed
    var result = SDKHelper.getProtocolAndServer("example.com//", "https");
    assertThat(result).isEqualTo("https://example.com/");
  }

  @Test
  void getLibraryPage_should_return_single_page_with_count() throws IOException {
    var mockResponse = new LibrariesExtended();
    mockResponse.setLibraries(List.of(new LibraryExtended()));
    mockResponse.setCount(50L);

    when(extendedSDK.getLibrariesWithFilter(
            eq(TEST_ORG_ID), eq(TEST_APP_ID), any(LibraryFilterForm.class)))
        .thenReturn(mockResponse);

    var result = SDKHelper.getLibraryPage(TEST_APP_ID, TEST_ORG_ID, extendedSDK, 50, 0);

    assertThat(result.getLibraries()).hasSize(1);
    assertThat(result.getCount()).isEqualTo(50L);
  }
}

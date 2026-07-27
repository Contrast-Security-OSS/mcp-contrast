package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibrariesExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.ApplicationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sca.LibraryObservation;
import com.contrastsecurity.http.LibraryFilterForm;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

// SDKHelper keeps its Environment and both caches in static fields, so these tests share
// mutable state and cannot run concurrently with each other.
@Execution(ExecutionMode.SAME_THREAD)
class SDKHelperTest {

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_APP_ID = "test-app-456";
  private static final String OTHER_APP_ID = "test-app-789";
  private static final String TEST_LIBRARY_ID = "test-lib-1";
  private static final int API_MAX_PAGE_SIZE = 50;
  private static final int DEFAULT_OBSERVATION_PAGE_SIZE = 25;
  private static final int APP_FILTER_LIMIT = 100;

  @Mock private Environment environment;
  @Mock private SDKExtension extendedSDK;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Exercises the @Autowired setter rather than reflecting onto the static field.
    new SDKHelper().setEnvironment(environment);
    SDKHelper.clearAllCaches();
  }

  @Test
  void testGetProtocolAndServer_WithNull() {
    assertThat(SDKHelper.getProtocolAndServer(null, "https")).isNull();
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocol() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("http://example.com", "https"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insecure protocol")
        .hasMessageContaining("http://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocolUppercase() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("HTTP://example.com", "https"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insecure protocol")
        .hasMessageContaining("HTTP://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpsProtocol() {
    var result = SDKHelper.getProtocolAndServer("https://example.com", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpsProtocolUppercase() {
    var result = SDKHelper.getProtocolAndServer("HTTPS://example.com", "https");
    assertThat(result).isEqualTo("HTTPS://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithoutProtocol() {
    var result = SDKHelper.getProtocolAndServer("example.com", "https");
    assertThat(result).isEqualTo("https://example.com");
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocolConfig() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("example.com", "http"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insecure protocol")
        .hasMessageContaining("http");
  }

  @Test
  void testGetProtocolAndServer_WithHttpProtocolConfigUppercase() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("example.com", "HTTP"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insecure protocol")
        .hasMessageContaining("HTTP");
  }

  @Test
  void testGetProtocolAndServer_WithProtocolConfigWithScheme() {
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("example.com", "https://"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not 'https://'");
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

    assertThatThrownBy(
            () ->
                SDKHelper.getSDK(hostname, "apiKey", "serviceKey", "username", null, null, "http"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insecure protocol")
        .hasMessageContaining("http");
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
    assertThatThrownBy(() -> SDKHelper.getProtocolAndServer("http://example.com/", "https"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insecure protocol")
        .hasMessageContaining("http://example.com/");
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

  @Test
  void getLibraryPage_should_clamp_the_limit_to_the_api_maximum() throws IOException {
    var forms = capturePages(TEST_APP_ID, page(1));

    SDKHelper.getLibraryPage(TEST_APP_ID, TEST_ORG_ID, extendedSDK, 500, 0);

    assertThat(forms).singleElement().returns(API_MAX_PAGE_SIZE, LibraryFilterForm::getLimit);
  }

  @Test
  void getLibraryPage_should_pass_a_smaller_limit_through_unchanged() throws IOException {
    var forms = capturePages(TEST_APP_ID, page(1));

    SDKHelper.getLibraryPage(TEST_APP_ID, TEST_ORG_ID, extendedSDK, 10, 60);

    assertThat(forms)
        .singleElement()
        .returns(10, LibraryFilterForm::getLimit)
        .returns(60, LibraryFilterForm::getOffset);
  }

  @Test
  void getLibraryPage_should_request_vulnerability_expansion() throws IOException {
    var forms = capturePages(TEST_APP_ID, page(1));

    SDKHelper.getLibraryPage(TEST_APP_ID, TEST_ORG_ID, extendedSDK, 10, 0);

    assertThat(forms.getFirst().getExpand())
        .isEqualTo(EnumSet.of(LibraryFilterForm.LibrariesExpandValues.VULNS));
  }

  @Test
  void getLibraryPage_should_query_the_sdk_every_call_because_it_does_not_cache()
      throws IOException {
    capturePages(TEST_APP_ID, page(1), page(1));

    SDKHelper.getLibraryPage(TEST_APP_ID, TEST_ORG_ID, extendedSDK, 10, 0);
    SDKHelper.getLibraryPage(TEST_APP_ID, TEST_ORG_ID, extendedSDK, 10, 0);

    verify(extendedSDK, times(2))
        .getLibrariesWithFilter(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(LibraryFilterForm.class));
  }

  @Test
  void getLibsForID_should_return_every_library_across_all_pages() throws IOException {
    capturePages(TEST_APP_ID, page(API_MAX_PAGE_SIZE), page(API_MAX_PAGE_SIZE), page(3));

    var libs = SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    assertThat(libs).hasSize(2 * API_MAX_PAGE_SIZE + 3);
    assertThat(libs).extracting(LibraryExtended::getHash).doesNotHaveDuplicates();
  }

  @Test
  void getLibsForID_should_advance_the_offset_by_a_full_page_each_time() throws IOException {
    var forms =
        capturePages(TEST_APP_ID, page(API_MAX_PAGE_SIZE), page(API_MAX_PAGE_SIZE), page(3));

    SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    assertThat(forms)
        .extracting(LibraryFilterForm::getOffset)
        .containsExactly(0, API_MAX_PAGE_SIZE, 2 * API_MAX_PAGE_SIZE);
  }

  @Test
  void getLibsForID_should_stop_after_one_call_when_the_first_page_is_short() throws IOException {
    capturePages(TEST_APP_ID, page(API_MAX_PAGE_SIZE - 1));

    SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    verify(extendedSDK, times(1))
        .getLibrariesWithFilter(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(LibraryFilterForm.class));
  }

  @Test
  void getLibsForID_should_serve_the_second_request_from_cache_without_calling_the_sdk()
      throws IOException {
    capturePages(TEST_APP_ID, page(2));

    var first = SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);
    var second = SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    verify(extendedSDK, times(1))
        .getLibrariesWithFilter(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(LibraryFilterForm.class));
    assertThat(second).containsExactlyElementsOf(first);
  }

  @Test
  void getLibsForID_should_cache_each_application_separately() throws IOException {
    capturePages(TEST_APP_ID, page(1));
    capturePages(OTHER_APP_ID, page(2));

    var first = SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);
    var second = SDKHelper.getLibsForID(OTHER_APP_ID, TEST_ORG_ID, extendedSDK);

    assertThat(first).hasSize(1);
    assertThat(second).hasSize(2);
  }

  @Test
  void clearLibraryCache_should_force_the_next_request_back_to_the_sdk() throws IOException {
    capturePages(TEST_APP_ID, page(1), page(1));
    SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    SDKHelper.clearLibraryCache();
    SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    verify(extendedSDK, times(2))
        .getLibrariesWithFilter(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(LibraryFilterForm.class));
  }

  @Test
  void clearLibraryCache_should_return_the_number_of_applications_it_evicted() throws IOException {
    capturePages(TEST_APP_ID, page(1));
    capturePages(OTHER_APP_ID, page(1));
    SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);
    SDKHelper.getLibsForID(OTHER_APP_ID, TEST_ORG_ID, extendedSDK);

    assertThat(SDKHelper.clearLibraryCache()).isEqualTo(2);
    assertThat(SDKHelper.clearLibraryCache()).isZero();
  }

  @Test
  void getLibraryObservationsWithCache_should_serve_a_repeat_request_from_cache() throws Exception {
    var observations = List.of(observation("commons-io"));
    when(extendedSDK.getLibraryObservations(
            TEST_ORG_ID, TEST_APP_ID, TEST_LIBRARY_ID, DEFAULT_OBSERVATION_PAGE_SIZE))
        .thenReturn(observations);

    var first =
        SDKHelper.getLibraryObservationsWithCache(
            TEST_LIBRARY_ID, TEST_APP_ID, TEST_ORG_ID, extendedSDK);
    var second =
        SDKHelper.getLibraryObservationsWithCache(
            TEST_LIBRARY_ID, TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    verify(extendedSDK, times(1))
        .getLibraryObservations(anyString(), anyString(), anyString(), anyInt());
    assertThat(first).extracting(LibraryObservation::getName).containsExactly("commons-io");
    assertThat(second).containsExactlyElementsOf(first);
  }

  @Test
  void getLibraryObservationsWithCache_should_use_the_default_page_size_on_the_short_overload()
      throws Exception {
    when(extendedSDK.getLibraryObservations(
            TEST_ORG_ID, TEST_APP_ID, TEST_LIBRARY_ID, DEFAULT_OBSERVATION_PAGE_SIZE))
        .thenReturn(List.of(observation("commons-io")));

    SDKHelper.getLibraryObservationsWithCache(
        TEST_LIBRARY_ID, TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    verify(extendedSDK)
        .getLibraryObservations(
            TEST_ORG_ID, TEST_APP_ID, TEST_LIBRARY_ID, DEFAULT_OBSERVATION_PAGE_SIZE);
  }

  @Test
  void getLibraryObservationsWithCache_should_treat_a_different_library_as_a_separate_entry()
      throws Exception {
    when(extendedSDK.getLibraryObservations(anyString(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of(observation("commons-io")));

    SDKHelper.getLibraryObservationsWithCache(
        TEST_LIBRARY_ID, TEST_APP_ID, TEST_ORG_ID, extendedSDK);
    SDKHelper.getLibraryObservationsWithCache("other-lib", TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    verify(extendedSDK, times(2))
        .getLibraryObservations(anyString(), anyString(), anyString(), anyInt());
  }

  @Test
  void getLibraryObservationsWithCache_should_treat_a_different_application_as_a_separate_entry()
      throws Exception {
    when(extendedSDK.getLibraryObservations(anyString(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of(observation("commons-io")));

    SDKHelper.getLibraryObservationsWithCache(
        TEST_LIBRARY_ID, TEST_APP_ID, TEST_ORG_ID, extendedSDK);
    SDKHelper.getLibraryObservationsWithCache(
        TEST_LIBRARY_ID, OTHER_APP_ID, TEST_ORG_ID, extendedSDK);

    verify(extendedSDK, times(2))
        .getLibraryObservations(anyString(), anyString(), anyString(), anyInt());
  }

  @Test
  void clearLibraryObservationsCache_should_force_the_next_request_back_to_the_sdk()
      throws Exception {
    when(extendedSDK.getLibraryObservations(anyString(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of(observation("commons-io")));
    SDKHelper.getLibraryObservationsWithCache(
        TEST_LIBRARY_ID, TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    assertThat(SDKHelper.clearLibraryObservationsCache()).isEqualTo(1);
    SDKHelper.getLibraryObservationsWithCache(
        TEST_LIBRARY_ID, TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    verify(extendedSDK, times(2))
        .getLibraryObservations(anyString(), anyString(), anyString(), anyInt());
  }

  @Test
  void clearAllCaches_should_report_the_entries_removed_from_both_caches() throws Exception {
    capturePages(TEST_APP_ID, page(1));
    capturePages(OTHER_APP_ID, page(1));
    when(extendedSDK.getLibraryObservations(anyString(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of(observation("commons-io")));
    SDKHelper.getLibsForID(TEST_APP_ID, TEST_ORG_ID, extendedSDK);
    SDKHelper.getLibsForID(OTHER_APP_ID, TEST_ORG_ID, extendedSDK);
    SDKHelper.getLibraryObservationsWithCache(
        TEST_LIBRARY_ID, TEST_APP_ID, TEST_ORG_ID, extendedSDK);

    assertThat(SDKHelper.clearAllCaches()).isEqualTo(3);
    assertThat(SDKHelper.clearAllCaches()).isZero();
  }

  @Test
  void getApplicationByName_should_return_the_match_regardless_of_case() throws Exception {
    stubFilteredApplications(
        application("Other App"), application("WEBGOAT"), application("Another App"));

    var result = SDKHelper.getApplicationByName("webgoat", TEST_ORG_ID, extendedSDK);

    assertThat(result).map(Application::getName).contains("WEBGOAT");
  }

  @Test
  void getApplicationByName_should_pass_the_name_to_the_server_side_filter() throws Exception {
    stubFilteredApplications(application("WebGoat"));

    SDKHelper.getApplicationByName("WebGoat", TEST_ORG_ID, extendedSDK);

    verify(extendedSDK)
        .getApplicationsFiltered(TEST_ORG_ID, "WebGoat", null, null, APP_FILTER_LIMIT, 0);
  }

  @Test
  void getApplicationByName_should_return_empty_when_no_name_matches() throws Exception {
    stubFilteredApplications(application("WebGoat Staging"), application("WebGoat Prod"));

    assertThat(SDKHelper.getApplicationByName("WebGoat", TEST_ORG_ID, extendedSDK)).isEmpty();
  }

  @Test
  void getApplicationByName_should_return_empty_when_the_response_is_null() throws Exception {
    when(extendedSDK.getApplicationsFiltered(
            anyString(), anyString(), any(), any(), anyInt(), anyInt()))
        .thenReturn(null);

    assertThat(SDKHelper.getApplicationByName("WebGoat", TEST_ORG_ID, extendedSDK)).isEmpty();
  }

  @Test
  void getApplicationByName_should_return_empty_when_the_response_carries_no_application_list()
      throws Exception {
    var response = new ApplicationsResponse();
    response.setApplications(null);
    when(extendedSDK.getApplicationsFiltered(
            anyString(), anyString(), any(), any(), anyInt(), anyInt()))
        .thenReturn(response);

    assertThat(SDKHelper.getApplicationByName("WebGoat", TEST_ORG_ID, extendedSDK)).isEmpty();
  }

  @Test
  void getSDK_should_append_the_contrast_api_path_to_the_host() {
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    var sdk =
        SDKHelper.getSDK(
            "example.contrastsecurity.com", "apiKey", "serviceKey", "user", null, null, "https");

    assertThat(sdk.getRestApiURL()).isEqualTo("https://example.contrastsecurity.com/Contrast/api");
  }

  @Test
  void getSDK_should_keep_a_host_that_already_carries_the_https_scheme() {
    when(environment.getProperty("spring.ai.mcp.server.version", "unknown")).thenReturn("1.0.0");

    var sdk =
        SDKHelper.getSDK(
            "https://custom.example.com", "apiKey", "serviceKey", "user", null, null, "https");

    assertThat(sdk.getRestApiURL()).isEqualTo("https://custom.example.com/Contrast/api");
  }

  @Test
  void resolveProxyPort_should_parse_an_explicit_port() {
    assertThat(SDKHelper.resolveProxyPort("8080")).isEqualTo(8080);
  }

  @Test
  void resolveProxyPort_should_default_a_blank_port_to_http() {
    assertThat(SDKHelper.resolveProxyPort("  ")).isEqualTo(80);
    assertThat(SDKHelper.resolveProxyPort(null)).isEqualTo(80);
  }

  @Test
  void resolveProxyPort_should_reject_a_port_that_is_not_a_number() {
    assertThatThrownBy(() -> SDKHelper.resolveProxyPort("http"))
        .isInstanceOf(NumberFormatException.class);
  }

  /**
   * Stubs consecutive responses for one app id and returns the filter form state captured at each
   * call. SDKHelper reuses and mutates a single form across pages, so the offset has to be read
   * when the call happens rather than from a captor afterwards.
   */
  private List<LibraryFilterForm> capturePages(String appId, LibrariesExtended... pages)
      throws IOException {
    var seen = new ArrayList<LibraryFilterForm>();
    var remaining = new ArrayDeque<>(List.of(pages));
    when(extendedSDK.getLibrariesWithFilter(
            eq(TEST_ORG_ID), eq(appId), any(LibraryFilterForm.class)))
        .thenAnswer(
            invocation -> {
              LibraryFilterForm form = invocation.getArgument(2);
              var snapshot = new LibraryFilterForm();
              snapshot.setLimit(form.getLimit());
              snapshot.setOffset(form.getOffset());
              snapshot.setExpand(form.getExpand());
              seen.add(snapshot);
              return remaining.isEmpty() ? page(0) : remaining.poll();
            });
    return seen;
  }

  private static LibrariesExtended page(int size) {
    var libraries = new ArrayList<LibraryExtended>();
    for (int index = 0; index < size; index++) {
      var library = new LibraryExtended();
      library.setHash("hash-" + UUID.randomUUID());
      libraries.add(library);
    }
    var response = new LibrariesExtended();
    response.setLibraries(libraries);
    response.setCount((long) size);
    return response;
  }

  private static LibraryObservation observation(String name) {
    var observation = new LibraryObservation();
    observation.setName(name);
    return observation;
  }

  private static Application application(String name) {
    var application = new Application();
    application.setName(name);
    return application;
  }

  private void stubFilteredApplications(Application... applications) throws Exception {
    var response = new ApplicationsResponse();
    response.setApplications(List.of(applications));
    when(extendedSDK.getApplicationsFiltered(
            anyString(), anyString(), any(), any(), anyInt(), anyInt()))
        .thenReturn(response);
  }
}

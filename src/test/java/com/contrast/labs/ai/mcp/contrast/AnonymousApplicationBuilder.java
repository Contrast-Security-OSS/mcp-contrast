package com.contrast.labs.ai.mcp.contrast;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Metadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Builder for creating anonymous Application mocks with sensible defaults. Only override fields
 * that matter for your specific test.
 *
 * <p>Example usage:
 *
 * <pre>
 * Application app = AnonymousApplicationBuilder.validApp()
 *     .withName("MyApp")
 *     .withTag("Production")
 *     .build();
 * </pre>
 */
public class AnonymousApplicationBuilder {
  private final Application app;
  private String name = "TestApp-" + UUID.randomUUID().toString().substring(0, 8);
  private String status = "ACTIVE";
  private String appId = "app-" + UUID.randomUUID().toString().substring(0, 8);
  private Long lastSeen = System.currentTimeMillis();
  private String language = "Java";
  private List<String> tags = new ArrayList<>();
  private List<String> techs = new ArrayList<>();
  private List<Metadata> metadataEntities = new ArrayList<>();

  private AnonymousApplicationBuilder() {
    this.app = mock(Application.class);
  }

  /** Create a builder with valid defaults for all required fields. */
  public static AnonymousApplicationBuilder validApp() {
    return new AnonymousApplicationBuilder();
  }

  public AnonymousApplicationBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public AnonymousApplicationBuilder withStatus(String status) {
    this.status = status;
    return this;
  }

  public AnonymousApplicationBuilder withAppId(String appId) {
    this.appId = appId;
    return this;
  }

  public AnonymousApplicationBuilder withLastSeen(Long lastSeen) {
    this.lastSeen = lastSeen;
    return this;
  }

  public AnonymousApplicationBuilder withLanguage(String language) {
    this.language = language;
    return this;
  }

  public AnonymousApplicationBuilder withTag(String tag) {
    this.tags.add(tag);
    return this;
  }

  public AnonymousApplicationBuilder withTags(List<String> tags) {
    // Never null collections pattern - use empty list if null
    this.tags = tags != null ? tags : Collections.emptyList();
    return this;
  }

  public AnonymousApplicationBuilder withTech(String tech) {
    this.techs.add(tech);
    return this;
  }

  public AnonymousApplicationBuilder withTechs(List<String> techs) {
    // Never null collections pattern - use empty list if null
    this.techs = techs != null ? techs : Collections.emptyList();
    return this;
  }

  public AnonymousApplicationBuilder withMetadata(String name, String value) {
    Metadata metadata = mock(Metadata.class);
    lenient().when(metadata.getName()).thenReturn(name);
    lenient().when(metadata.getValue()).thenReturn(value);
    this.metadataEntities.add(metadata);
    return this;
  }

  public AnonymousApplicationBuilder withMetadataEntities(List<Metadata> metadataEntities) {
    // Never null collections pattern - use empty list if null
    this.metadataEntities = metadataEntities != null ? metadataEntities : Collections.emptyList();
    return this;
  }

  /**
   * Build the Application mock with all configured values. Uses lenient stubbing to avoid
   * UnnecessaryStubbingException for fields not accessed in specific tests.
   */
  public Application build() {
    lenient().when(app.getName()).thenReturn(name);
    lenient().when(app.getStatus()).thenReturn(status);
    lenient().when(app.getAppId()).thenReturn(appId);
    lenient().when(app.getLastSeen()).thenReturn(lastSeen);
    lenient().when(app.getLanguage()).thenReturn(language);
    lenient().when(app.getTags()).thenReturn(tags);
    lenient().when(app.getTechs()).thenReturn(techs);
    lenient().when(app.getMetadataEntities()).thenReturn(metadataEntities);
    return app;
  }
}

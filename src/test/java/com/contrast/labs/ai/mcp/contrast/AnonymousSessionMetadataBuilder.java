package com.contrast.labs.ai.mcp.contrast;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.contrastsecurity.models.MetadataItem;
import com.contrastsecurity.models.SessionMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for creating anonymous SessionMetadata mocks with sensible defaults. Only override fields
 * that matter for your specific test.
 *
 * <p>Example usage:
 *
 * <pre>
 * SessionMetadata session = AnonymousSessionMetadataBuilder.validSessionMetadata()
 *     .withSessionId("my-session-123")
 *     .withMetadataItem("ENVIRONMENT", "PRODUCTION")
 *     .build();
 * </pre>
 */
public class AnonymousSessionMetadataBuilder {
  private final SessionMetadata sessionMetadata;
  private String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
  private List<MetadataItem> metadata = new ArrayList<>();

  private AnonymousSessionMetadataBuilder() {
    this.sessionMetadata = mock(SessionMetadata.class);
  }

  /** Create a builder with valid defaults for all required fields. */
  public static AnonymousSessionMetadataBuilder validSessionMetadata() {
    return new AnonymousSessionMetadataBuilder();
  }

  public AnonymousSessionMetadataBuilder withSessionId(String sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public AnonymousSessionMetadataBuilder withMetadata(List<MetadataItem> metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Convenience method to add a single metadata item without creating it manually.
   *
   * @param displayLabel the display label for the metadata item
   * @param value the value for the metadata item
   */
  public AnonymousSessionMetadataBuilder withMetadataItem(String displayLabel, String value) {
    MetadataItem item =
        AnonymousMetadataItemBuilder.validMetadataItem()
            .withDisplayLabel(displayLabel)
            .withValue(value)
            .build();
    this.metadata.add(item);
    return this;
  }

  /**
   * Build the SessionMetadata mock with all configured values. Uses lenient stubbing to avoid
   * UnnecessaryStubbingException for fields not accessed in specific tests.
   */
  public SessionMetadata build() {
    lenient().when(sessionMetadata.getSessionId()).thenReturn(sessionId);
    lenient().when(sessionMetadata.getMetadata()).thenReturn(metadata);
    return sessionMetadata;
  }
}

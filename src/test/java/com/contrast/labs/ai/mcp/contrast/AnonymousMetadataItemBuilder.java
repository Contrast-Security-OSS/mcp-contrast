package com.contrast.labs.ai.mcp.contrast;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.contrastsecurity.models.MetadataItem;
import java.util.UUID;

/**
 * Builder for creating anonymous MetadataItem mocks with sensible defaults. Only override fields
 * that matter for your specific test.
 *
 * <p>Example usage:
 *
 * <pre>
 * MetadataItem item = AnonymousMetadataItemBuilder.validMetadataItem()
 *     .withDisplayLabel("ENVIRONMENT")
 *     .withValue("PRODUCTION")
 *     .build();
 * </pre>
 */
public class AnonymousMetadataItemBuilder {
  private final MetadataItem metadataItem;
  private String displayLabel = "Label-" + UUID.randomUUID().toString().substring(0, 8);
  private String agentLabel = "agentLabel-" + UUID.randomUUID().toString().substring(0, 8);
  private String value = "Value-" + UUID.randomUUID().toString().substring(0, 8);

  private AnonymousMetadataItemBuilder() {
    this.metadataItem = mock(MetadataItem.class);
  }

  /** Create a builder with valid defaults for all required fields. */
  public static AnonymousMetadataItemBuilder validMetadataItem() {
    return new AnonymousMetadataItemBuilder();
  }

  public AnonymousMetadataItemBuilder withDisplayLabel(String displayLabel) {
    this.displayLabel = displayLabel;
    return this;
  }

  public AnonymousMetadataItemBuilder withAgentLabel(String agentLabel) {
    this.agentLabel = agentLabel;
    return this;
  }

  public AnonymousMetadataItemBuilder withValue(String value) {
    this.value = value;
    return this;
  }

  /**
   * Build the MetadataItem mock with all configured values. Uses lenient stubbing to avoid
   * UnnecessaryStubbingException for fields not accessed in specific tests.
   */
  public MetadataItem build() {
    lenient().when(metadataItem.getDisplayLabel()).thenReturn(displayLabel);
    lenient().when(metadataItem.getAgentLabel()).thenReturn(agentLabel);
    lenient().when(metadataItem.getValue()).thenReturn(value);
    return metadataItem;
  }
}

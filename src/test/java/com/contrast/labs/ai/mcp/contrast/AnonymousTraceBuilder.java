package com.contrast.labs.ai.mcp.contrast;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.contrastsecurity.models.SessionMetadata;
import com.contrastsecurity.models.Trace;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for creating anonymous Trace mocks with sensible defaults. Only override fields that
 * matter for your specific test.
 *
 * <p>Example usage:
 *
 * <pre>
 * Trace trace = AnonymousTraceBuilder.validTrace()
 *     .withTitle("SQL Injection")
 *     .withSeverity("HIGH")
 *     .withSessionMetadata("session-123", "ENVIRONMENT", "PRODUCTION")
 *     .build();
 * </pre>
 */
public class AnonymousTraceBuilder {
  private final Trace trace;
  private String title = "Vulnerability-" + UUID.randomUUID().toString().substring(0, 8);
  private String rule = "rule-" + UUID.randomUUID().toString().substring(0, 8);
  private String uuid = "uuid-" + UUID.randomUUID().toString().substring(0, 8);
  private String severity = "MEDIUM";
  private String status = "REPORTED";
  private String hash = "hash-" + UUID.randomUUID().toString().substring(0, 8);
  private String language = "Java";
  private String category = "category-" + UUID.randomUUID().toString().substring(0, 8);
  private String likelihood = "MEDIUM";
  private String impact = "MEDIUM";
  private String confidence = "MEDIUM";
  private String subStatus = null;
  private Long firstTimeSeen = System.currentTimeMillis();
  private Long lastTimeSeen = System.currentTimeMillis();
  private Long closedTime = null;
  private Boolean visible = true;
  private List<SessionMetadata> sessionMetadata = new ArrayList<>();
  private List<String> tags = new ArrayList<>();
  private List<String> serverEnvironments = new ArrayList<>();

  private AnonymousTraceBuilder() {
    this.trace = mock(Trace.class);
  }

  /** Create a builder with valid defaults for all required fields. */
  public static AnonymousTraceBuilder validTrace() {
    return new AnonymousTraceBuilder();
  }

  public AnonymousTraceBuilder withTitle(String title) {
    this.title = title;
    return this;
  }

  public AnonymousTraceBuilder withRule(String rule) {
    this.rule = rule;
    return this;
  }

  public AnonymousTraceBuilder withUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public AnonymousTraceBuilder withSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public AnonymousTraceBuilder withStatus(String status) {
    this.status = status;
    return this;
  }

  public AnonymousTraceBuilder withHash(String hash) {
    this.hash = hash;
    return this;
  }

  public AnonymousTraceBuilder withLanguage(String language) {
    this.language = language;
    return this;
  }

  public AnonymousTraceBuilder withCategory(String category) {
    this.category = category;
    return this;
  }

  public AnonymousTraceBuilder withLikelihood(String likelihood) {
    this.likelihood = likelihood;
    return this;
  }

  public AnonymousTraceBuilder withImpact(String impact) {
    this.impact = impact;
    return this;
  }

  public AnonymousTraceBuilder withConfidence(String confidence) {
    this.confidence = confidence;
    return this;
  }

  public AnonymousTraceBuilder withSubStatus(String subStatus) {
    this.subStatus = subStatus;
    return this;
  }

  public AnonymousTraceBuilder withFirstTimeSeen(Long firstTimeSeen) {
    this.firstTimeSeen = firstTimeSeen;
    return this;
  }

  public AnonymousTraceBuilder withLastTimeSeen(Long lastTimeSeen) {
    this.lastTimeSeen = lastTimeSeen;
    return this;
  }

  public AnonymousTraceBuilder withClosedTime(Long closedTime) {
    this.closedTime = closedTime;
    return this;
  }

  public AnonymousTraceBuilder withVisible(Boolean visible) {
    this.visible = visible;
    return this;
  }

  public AnonymousTraceBuilder withSessionMetadataList(List<SessionMetadata> sessionMetadata) {
    this.sessionMetadata = sessionMetadata;
    return this;
  }

  /**
   * Convenience method to add a session with simple metadata.
   *
   * @param sessionId the session ID
   * @param metadataLabel the metadata label (e.g., "ENVIRONMENT")
   * @param metadataValue the metadata value (e.g., "PRODUCTION")
   */
  public AnonymousTraceBuilder withSessionMetadata(
      String sessionId, String metadataLabel, String metadataValue) {
    SessionMetadata session =
        AnonymousSessionMetadataBuilder.validSessionMetadata()
            .withSessionId(sessionId)
            .withMetadataItem(metadataLabel, metadataValue)
            .build();
    this.sessionMetadata.add(session);
    return this;
  }

  /**
   * Convenience method to add a session with just a session ID and no metadata items.
   *
   * @param sessionId the session ID
   */
  public AnonymousTraceBuilder withSessionMetadata(String sessionId) {
    SessionMetadata session =
        AnonymousSessionMetadataBuilder.validSessionMetadata().withSessionId(sessionId).build();
    this.sessionMetadata.add(session);
    return this;
  }

  public AnonymousTraceBuilder withTag(String tag) {
    this.tags.add(tag);
    return this;
  }

  public AnonymousTraceBuilder withTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public AnonymousTraceBuilder withServerEnvironment(String serverEnvironment) {
    this.serverEnvironments.add(serverEnvironment);
    return this;
  }

  public AnonymousTraceBuilder withServerEnvironments(List<String> serverEnvironments) {
    this.serverEnvironments = serverEnvironments;
    return this;
  }

  /**
   * Build the Trace mock with all configured values. Uses lenient stubbing to avoid
   * UnnecessaryStubbingException for fields not accessed in specific tests.
   */
  public Trace build() {
    lenient().when(trace.getTitle()).thenReturn(title);
    lenient().when(trace.getRule()).thenReturn(rule);
    lenient().when(trace.getUuid()).thenReturn(uuid);
    lenient().when(trace.getSeverity()).thenReturn(severity);
    lenient().when(trace.getStatus()).thenReturn(status);
    lenient().when(trace.getHash()).thenReturn(hash);
    lenient().when(trace.getLanguage()).thenReturn(language);
    lenient().when(trace.getCategory()).thenReturn(category);
    lenient().when(trace.getLikelihood()).thenReturn(likelihood);
    lenient().when(trace.getImpact()).thenReturn(impact);
    lenient().when(trace.getConfidence()).thenReturn(confidence);
    lenient().when(trace.getSubStatus()).thenReturn(subStatus);
    lenient().when(trace.getFirstTimeSeen()).thenReturn(firstTimeSeen);
    lenient().when(trace.getLastTimeSeen()).thenReturn(lastTimeSeen);
    lenient().when(trace.getClosedTime()).thenReturn(closedTime);
    lenient().when(trace.getVisible()).thenReturn(visible);
    lenient().when(trace.getSessionMetadata()).thenReturn(sessionMetadata);
    lenient().when(trace.getTags()).thenReturn(tags);
    lenient().when(trace.getServerEnvironments()).thenReturn(serverEnvironments);
    return trace;
  }
}

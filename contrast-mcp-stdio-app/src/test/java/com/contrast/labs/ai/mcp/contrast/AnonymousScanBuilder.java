/*
 * Copyright 2026 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.contrast.labs.ai.mcp.contrast;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.contrastsecurity.sdk.scan.Scan;
import com.contrastsecurity.sdk.scan.ScanStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Builder for creating anonymous Scan mocks with sensible defaults. Only override fields that
 * matter for your specific test.
 *
 * <p>Note: Scan is an interface, so this builder mocks the interface methods (not getters).
 *
 * <p>Example usage:
 *
 * <pre>
 * Scan scan = AnonymousScanBuilder.validScan()
 *     .withId("scan-123")
 *     .withStatus(ScanStatus.COMPLETED)
 *     .withSarif("{\"version\":\"2.1.0\"}")
 *     .build();
 * </pre>
 */
public class AnonymousScanBuilder {
  private final Scan scan;
  private String id = "scan-" + UUID.randomUUID().toString().substring(0, 8);
  private String projectId = "project-" + UUID.randomUUID().toString().substring(0, 8);
  private String organizationId = "org-" + UUID.randomUUID().toString().substring(0, 8);
  private ScanStatus status = ScanStatus.COMPLETED;
  private String errorMessage = null;
  private boolean isFinished = true;
  private InputStream sarif =
      new ByteArrayInputStream(
          "{\"version\":\"2.1.0\",\"runs\":[]}".getBytes(StandardCharsets.UTF_8));

  private AnonymousScanBuilder() {
    this.scan = mock();
  }

  /** Create a builder with valid defaults for all required fields. */
  public static AnonymousScanBuilder validScan() {
    return new AnonymousScanBuilder();
  }

  public AnonymousScanBuilder withId(String id) {
    this.id = id;
    return this;
  }

  public AnonymousScanBuilder withProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  public AnonymousScanBuilder withOrganizationId(String organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  public AnonymousScanBuilder withStatus(ScanStatus status) {
    this.status = status;
    return this;
  }

  public AnonymousScanBuilder withErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  public AnonymousScanBuilder withIsFinished(boolean isFinished) {
    this.isFinished = isFinished;
    return this;
  }

  public AnonymousScanBuilder withSarif(InputStream sarif) {
    this.sarif = sarif;
    return this;
  }

  /**
   * Convenience method to set SARIF content from a String.
   *
   * @param sarifContent the SARIF JSON as a string
   */
  public AnonymousScanBuilder withSarif(String sarifContent) {
    this.sarif = new ByteArrayInputStream(sarifContent.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  /**
   * Build the Scan mock with all configured values. Uses lenient stubbing to avoid
   * UnnecessaryStubbingException for fields not accessed in specific tests.
   */
  public Scan build() throws IOException {
    lenient().when(scan.id()).thenReturn(id);
    lenient().when(scan.projectId()).thenReturn(projectId);
    lenient().when(scan.organizationId()).thenReturn(organizationId);
    lenient().when(scan.status()).thenReturn(status);
    lenient().when(scan.errorMessage()).thenReturn(errorMessage);
    lenient().when(scan.isFinished()).thenReturn(isFinished);
    lenient().when(scan.sarif()).thenReturn(sarif);
    return scan;
  }
}

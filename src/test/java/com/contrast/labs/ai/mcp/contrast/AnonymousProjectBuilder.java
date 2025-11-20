package com.contrast.labs.ai.mcp.contrast;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.contrastsecurity.sdk.scan.Project;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Builder for creating anonymous Project mocks with sensible defaults. Only override fields that
 * matter for your specific test.
 *
 * <p>Note: Project is an interface, so this builder mocks the interface methods (not getters).
 *
 * <p>Example usage:
 *
 * <pre>
 * Project project = AnonymousProjectBuilder.validProject()
 *     .withName("MyProject")
 *     .withId("project-123")
 *     .build();
 * </pre>
 */
public class AnonymousProjectBuilder {
  private final Project project;
  private String id = "project-" + UUID.randomUUID().toString().substring(0, 8);
  private String name = "Project-" + UUID.randomUUID().toString().substring(0, 8);
  private String organizationId = "org-" + UUID.randomUUID().toString().substring(0, 8);
  private String language = "Java";
  private boolean archived = false;
  private int critical = 0;
  private int high = 0;
  private int medium = 0;
  private int low = 0;
  private int note = 0;
  private String lastScanId = "scan-" + UUID.randomUUID().toString().substring(0, 8);
  private Instant lastScanTime = Instant.now();
  private int completedScans = 1;
  private Collection<String> includeNamespaceFilters = new ArrayList<>();
  private Collection<String> excludeNamespaceFilters = new ArrayList<>();

  private AnonymousProjectBuilder() {
    this.project = mock(Project.class);
  }

  /** Create a builder with valid defaults for all required fields. */
  public static AnonymousProjectBuilder validProject() {
    return new AnonymousProjectBuilder();
  }

  public AnonymousProjectBuilder withId(String id) {
    this.id = id;
    return this;
  }

  public AnonymousProjectBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public AnonymousProjectBuilder withOrganizationId(String organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  public AnonymousProjectBuilder withLanguage(String language) {
    this.language = language;
    return this;
  }

  public AnonymousProjectBuilder withArchived(boolean archived) {
    this.archived = archived;
    return this;
  }

  public AnonymousProjectBuilder withCritical(int critical) {
    this.critical = critical;
    return this;
  }

  public AnonymousProjectBuilder withHigh(int high) {
    this.high = high;
    return this;
  }

  public AnonymousProjectBuilder withMedium(int medium) {
    this.medium = medium;
    return this;
  }

  public AnonymousProjectBuilder withLow(int low) {
    this.low = low;
    return this;
  }

  public AnonymousProjectBuilder withNote(int note) {
    this.note = note;
    return this;
  }

  public AnonymousProjectBuilder withLastScanId(String lastScanId) {
    this.lastScanId = lastScanId;
    return this;
  }

  public AnonymousProjectBuilder withLastScanTime(Instant lastScanTime) {
    this.lastScanTime = lastScanTime;
    return this;
  }

  public AnonymousProjectBuilder withCompletedScans(int completedScans) {
    this.completedScans = completedScans;
    return this;
  }

  public AnonymousProjectBuilder withIncludeNamespaceFilters(
      Collection<String> includeNamespaceFilters) {
    this.includeNamespaceFilters = includeNamespaceFilters;
    return this;
  }

  public AnonymousProjectBuilder withExcludeNamespaceFilters(
      Collection<String> excludeNamespaceFilters) {
    this.excludeNamespaceFilters = excludeNamespaceFilters;
    return this;
  }

  /**
   * Build the Project mock with all configured values. Uses lenient stubbing to avoid
   * UnnecessaryStubbingException for fields not accessed in specific tests.
   */
  public Project build() {
    lenient().when(project.id()).thenReturn(id);
    lenient().when(project.name()).thenReturn(name);
    lenient().when(project.organizationId()).thenReturn(organizationId);
    lenient().when(project.language()).thenReturn(language);
    lenient().when(project.archived()).thenReturn(archived);
    lenient().when(project.critical()).thenReturn(critical);
    lenient().when(project.high()).thenReturn(high);
    lenient().when(project.medium()).thenReturn(medium);
    lenient().when(project.low()).thenReturn(low);
    lenient().when(project.note()).thenReturn(note);
    lenient().when(project.lastScanId()).thenReturn(lastScanId);
    lenient().when(project.lastScanTime()).thenReturn(lastScanTime);
    lenient().when(project.completedScans()).thenReturn(completedScans);
    lenient().when(project.includeNamespaceFilters()).thenReturn(includeNamespaceFilters);
    lenient().when(project.excludeNamespaceFilters()).thenReturn(excludeNamespaceFilters);
    return project;
  }
}

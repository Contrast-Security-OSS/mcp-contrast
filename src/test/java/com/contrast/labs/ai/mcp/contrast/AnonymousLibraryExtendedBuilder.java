package com.contrast.labs.ai.mcp.contrast;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryVulnerabilityExtended;
import com.contrastsecurity.models.Application;
import com.contrastsecurity.models.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for creating anonymous LibraryExtended mocks with sensible defaults. Only override fields
 * that matter for your specific test.
 *
 * <p>Example usage:
 *
 * <pre>
 * LibraryExtended lib = AnonymousLibraryExtendedBuilder.validLibrary()
 *     .withFilename("log4j-core-2.14.1.jar")
 *     .withVersion("2.14.1")
 *     .withClassCount(100)
 *     .build();
 * </pre>
 */
public class AnonymousLibraryExtendedBuilder {
  private final LibraryExtended library;
  private String filename = "library-" + UUID.randomUUID().toString().substring(0, 8) + ".jar";
  private String version = "1.0." + UUID.randomUUID().toString().substring(0, 4);
  private String hash = "hash-" + UUID.randomUUID().toString().substring(0, 16);
  private String group = "com.example";
  private String grade = "B";
  private String manifest = "Manifest-Version: 1.0";
  private String fileVersion = "1.0.0";
  private String appId = "app-" + UUID.randomUUID().toString().substring(0, 8);
  private String appName = "TestApp";
  private String appContextPath = "/app";
  private String appLanguage = "Java";
  private String latestVersion = "1.1.0";
  private long libraryId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
  private int classCount = 100;
  private int classesUsed = 50;
  private long releaseDate = System.currentTimeMillis();
  private long latestReleaseDate = System.currentTimeMillis();
  private int totalVulnerabilities = 0;
  private int criticalVulnerabilities = 0;
  private int highVulnerabilities = 0;
  private boolean custom = false;
  private double libScore = 75.0;
  private int monthsOutdated = 0;
  private List<Application> applications = new ArrayList<>();
  private List<Server> servers = new ArrayList<>();
  private List<LibraryVulnerabilityExtended> vulnerabilities = new ArrayList<>();

  private AnonymousLibraryExtendedBuilder() {
    this.library = mock(LibraryExtended.class);
  }

  /** Create a builder with valid defaults for all required fields. */
  public static AnonymousLibraryExtendedBuilder validLibrary() {
    return new AnonymousLibraryExtendedBuilder();
  }

  public AnonymousLibraryExtendedBuilder withFilename(String filename) {
    this.filename = filename;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withVersion(String version) {
    this.version = version;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withHash(String hash) {
    this.hash = hash;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withGroup(String group) {
    this.group = group;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withGrade(String grade) {
    this.grade = grade;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withManifest(String manifest) {
    this.manifest = manifest;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withFileVersion(String fileVersion) {
    this.fileVersion = fileVersion;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withAppId(String appId) {
    this.appId = appId;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withAppName(String appName) {
    this.appName = appName;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withAppContextPath(String appContextPath) {
    this.appContextPath = appContextPath;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withAppLanguage(String appLanguage) {
    this.appLanguage = appLanguage;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withLibraryId(long libraryId) {
    this.libraryId = libraryId;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withClassCount(int classCount) {
    this.classCount = classCount;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withClassesUsed(int classesUsed) {
    this.classesUsed = classesUsed;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withReleaseDate(long releaseDate) {
    this.releaseDate = releaseDate;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withLatestReleaseDate(long latestReleaseDate) {
    this.latestReleaseDate = latestReleaseDate;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withTotalVulnerabilities(int totalVulnerabilities) {
    this.totalVulnerabilities = totalVulnerabilities;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withCriticalVulnerabilities(int criticalVulnerabilities) {
    this.criticalVulnerabilities = criticalVulnerabilities;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withHighVulnerabilities(int highVulnerabilities) {
    this.highVulnerabilities = highVulnerabilities;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withCustom(boolean custom) {
    this.custom = custom;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withLibScore(double libScore) {
    this.libScore = libScore;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withMonthsOutdated(int monthsOutdated) {
    this.monthsOutdated = monthsOutdated;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withApplications(List<Application> applications) {
    this.applications = applications;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withServers(List<Server> servers) {
    this.servers = servers;
    return this;
  }

  public AnonymousLibraryExtendedBuilder withVulnerabilities(
      List<LibraryVulnerabilityExtended> vulnerabilities) {
    this.vulnerabilities = vulnerabilities;
    return this;
  }

  /**
   * Build the LibraryExtended mock with all configured values. Uses lenient stubbing to avoid
   * UnnecessaryStubbingException for fields not accessed in specific tests.
   */
  public LibraryExtended build() {
    lenient().when(library.getFilename()).thenReturn(filename);
    lenient().when(library.getVersion()).thenReturn(version);
    lenient().when(library.getHash()).thenReturn(hash);
    lenient().when(library.getGroup()).thenReturn(group);
    lenient().when(library.getGrade()).thenReturn(grade);
    lenient().when(library.getManifest()).thenReturn(manifest);
    lenient().when(library.getFileVersion()).thenReturn(fileVersion);
    lenient().when(library.getAppId()).thenReturn(appId);
    lenient().when(library.getAppName()).thenReturn(appName);
    lenient().when(library.getAppContextPath()).thenReturn(appContextPath);
    lenient().when(library.getAppLanguage()).thenReturn(appLanguage);
    lenient().when(library.getLatestVersion()).thenReturn(latestVersion);
    lenient().when(library.getLibraryId()).thenReturn(libraryId);
    lenient().when(library.getClassCount()).thenReturn(classCount);
    lenient().when(library.getClassesUsed()).thenReturn(classesUsed);
    lenient().when(library.getReleaseDate()).thenReturn(releaseDate);
    lenient().when(library.getLatestReleaseDate()).thenReturn(latestReleaseDate);
    lenient().when(library.getTotalVulnerabilities()).thenReturn(totalVulnerabilities);
    lenient().when(library.getCriticalVulnerabilities()).thenReturn(criticalVulnerabilities);
    lenient().when(library.getHighVulnerabilities()).thenReturn(highVulnerabilities);
    lenient().when(library.isCustom()).thenReturn(custom);
    lenient().when(library.getLibScore()).thenReturn(libScore);
    lenient().when(library.getMonthsOutdated()).thenReturn(monthsOutdated);
    lenient().when(library.getApplications()).thenReturn(applications);
    lenient().when(library.getServers()).thenReturn(servers);
    lenient().when(library.getVulnerabilities()).thenReturn(vulnerabilities);
    return library;
  }
}

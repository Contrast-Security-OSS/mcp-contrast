package com.contrast.labs.build;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads PMD and CPD XML reports, compares findings against a text baseline, and writes or tightens
 * that baseline.
 *
 * <p>Free of Gradle types so it can be unit tested without a build.
 */
public final class PmdBaselineVerification {

  static final String PMD_PREFIX = "PMD";
  static final String CPD_PREFIX = "CPD";
  private static final String SEPARATOR = "|";

  private PmdBaselineVerification() {}

  /**
   * Parses PMD violation entries from an XML report.
   *
   * <p>Each entry is {@code PMD|relative/path|RuleName}. Multiple violations of the same rule in
   * one file share one entry.
   */
  public static Set<String> pmdFindings(File report, File rootDir) {
    if (!report.isFile() || report.length() == 0L) {
      return Set.of();
    }
    Document document = parse(report);
    NodeList files = document.getElementsByTagName("file");
    Set<String> findings = new LinkedHashSet<>();
    for (int fileIndex = 0; fileIndex < files.getLength(); fileIndex++) {
      Element fileElement = (Element) files.item(fileIndex);
      String path = relativePath(rootDir, new File(fileElement.getAttribute("name")));
      NodeList violations = fileElement.getElementsByTagName("violation");
      for (int violationIndex = 0; violationIndex < violations.getLength(); violationIndex++) {
        Element violation = (Element) violations.item(violationIndex);
        findings.add(PMD_PREFIX + SEPARATOR + path + SEPARATOR + violation.getAttribute("rule"));
      }
    }
    return findings;
  }

  /**
   * Parses CPD duplication entries from an XML report.
   *
   * <p>Each entry is {@code CPD|file1,file2,...} with the participating file paths sorted.
   */
  public static Set<String> cpdFindings(File report, File rootDir) {
    if (!report.isFile() || report.length() == 0L) {
      return Set.of();
    }
    Document document = parse(report);
    NodeList duplications = document.getElementsByTagName("duplication");
    Set<String> findings = new LinkedHashSet<>();
    for (int duplicationIndex = 0;
        duplicationIndex < duplications.getLength();
        duplicationIndex++) {
      Element duplication = (Element) duplications.item(duplicationIndex);
      NodeList files = duplication.getElementsByTagName("file");
      List<String> locations = new ArrayList<>();
      for (int fileIndex = 0; fileIndex < files.getLength(); fileIndex++) {
        Element fileElement = (Element) files.item(fileIndex);
        locations.add(relativePath(rootDir, new File(fileElement.getAttribute("path"))));
      }
      locations.sort(String::compareTo);
      findings.add(CPD_PREFIX + SEPARATOR + String.join(",", locations));
    }
    return findings;
  }

  /**
   * Collects all findings from a set of PMD and CPD report files.
   *
   * <p>Files named {@code cpd.xml} are treated as CPD reports, everything else as PMD.
   */
  public static Set<String> allFindings(Collection<File> reports, File rootDir) {
    Set<String> findings = new LinkedHashSet<>();
    for (File report : reports) {
      if ("cpd.xml".equals(report.getName())) {
        findings.addAll(cpdFindings(report, rootDir));
      } else {
        findings.addAll(pmdFindings(report, rootDir));
      }
    }
    return findings;
  }

  /** Reads the baseline file, skipping comments and blank lines. */
  public static Set<String> readBaseline(File baselineFile) {
    if (!baselineFile.isFile()) {
      return Set.of();
    }
    try {
      List<String> lines = Files.readAllLines(baselineFile.toPath());
      Set<String> entries = new LinkedHashSet<>();
      for (String line : lines) {
        String trimmed = line.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
          entries.add(trimmed);
        }
      }
      return entries;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read baseline " + baselineFile, e);
    }
  }

  /**
   * Returns findings that are not in the baseline, sorted for stable output.
   *
   * @return new findings not covered by the baseline, or empty if all pass
   */
  public static List<String> newFindings(Set<String> findings, Set<String> baseline) {
    return findings.stream().filter(f -> !baseline.contains(f)).sorted().toList();
  }

  /**
   * Returns baseline entries that no longer appear in the current findings.
   *
   * @return stale entries that can be removed, sorted for stable output
   */
  public static List<String> staleEntries(Set<String> findings, Set<String> baseline) {
    return baseline.stream().filter(b -> !findings.contains(b)).sorted().toList();
  }

  /**
   * Writes a baseline file from the current findings.
   *
   * @param moduleName used in the regeneration comment
   */
  public static void writeBaseline(File baselineFile, Set<String> findings, String moduleName) {
    Set<String> sorted = new TreeSet<>(findings);
    StringBuilder content = new StringBuilder();
    content.append("# PMD baseline for ").append(moduleName).append(".\n");
    content
        .append("# Regenerate with ./gradlew :")
        .append(moduleName)
        .append(":writePmdBaseline\n");
    content.append("# PMD entries are matched by file and rule, not line number.\n");
    content.append("# Multiple violations of the same PMD rule in one file share one entry.\n");
    content.append(
        "# CPD entries are matched by participating files, not line number or token count.\n");
    for (String entry : sorted) {
      content.append(entry).append("\n");
    }
    try {
      Files.createDirectories(baselineFile.toPath().getParent());
      Files.writeString(baselineFile.toPath(), content.toString());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write baseline " + baselineFile, e);
    }
  }

  /**
   * Tightens a baseline by removing entries that are no longer reported, and writes the result.
   *
   * @return the entries that were removed
   */
  public static List<String> tightenBaseline(
      File baselineFile, Set<String> findings, String moduleName) {
    Set<String> baseline = readBaseline(baselineFile);
    List<String> stale = staleEntries(findings, baseline);
    if (stale.isEmpty()) {
      return List.of();
    }
    Set<String> tightened = new LinkedHashSet<>(baseline);
    stale.forEach(tightened::remove);
    writeBaseline(baselineFile, tightened, moduleName);
    return stale;
  }

  private static Document parse(File reportFile) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setExpandEntityReferences(false);
      factory.setXIncludeAware(false);
      return factory.newDocumentBuilder().parse(reportFile);
    } catch (ParserConfigurationException | SAXException e) {
      throw new IllegalStateException("Failed to parse PMD report " + reportFile, e);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read PMD report " + reportFile, e);
    }
  }

  private static String relativePath(File baseDir, File file) {
    Path basePath = baseDir.toPath().toAbsolutePath().normalize();
    Path filePath = file.toPath().toAbsolutePath().normalize();
    List<String> segments = new ArrayList<>();
    basePath.relativize(filePath).forEach(segment -> segments.add(segment.toString()));
    return String.join("/", segments);
  }
}

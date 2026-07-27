package com.contrast.labs.build;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads per-file line coverage out of a JaCoCo XML report and reports which of a given set of
 * changed source files fall below a minimum ratio.
 *
 * <p>Deliberately free of Gradle types so the arithmetic and the XML parsing can be unit tested
 * without a build.
 */
public final class JacocoChangedFileCoverage {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final int RATIO_SCALE = 4;
  private static final int PERCENT_SCALE = 2;

  private JacocoChangedFileCoverage() {}

  /** Line coverage for a single source file, as counted by JaCoCo. */
  public record FileCoverage(File file, int missedLines, int coveredLines) {

    public int totalLines() {
      return missedLines + coveredLines;
    }

    /**
     * A file with no countable lines is treated as fully covered, so an interface or a constants
     * holder cannot fail the gate for having nothing to execute.
     */
    public BigDecimal coveredRatio() {
      if (totalLines() == 0) {
        return BigDecimal.ONE;
      }
      return BigDecimal.valueOf(coveredLines)
          .divide(BigDecimal.valueOf(totalLines()), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal coveredPercent() {
      return coveredRatio().multiply(ONE_HUNDRED).setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }
  }

  /**
   * Outcome of an analysis run.
   *
   * @param checkedFiles files the report had line data for
   * @param skippedFiles files absent from the report, so the gate could say nothing about them
   */
  public record Result(List<FileCoverage> checkedFiles, List<File> skippedFiles) {

    public List<FileCoverage> failuresBelow(BigDecimal minimumRatio) {
      return checkedFiles.stream().filter(it -> it.coveredRatio().compareTo(minimumRatio) < 0).toList();
    }
  }

  /**
   * Matches each changed file against the report by its path relative to {@code sourceRoot}, which
   * is how JaCoCo names source files (package directory plus file name).
   *
   * <p>A missing or empty report means nothing was measured, so every file is skipped rather than
   * failed. Callers that need a report to exist must assert that themselves.
   */
  public static Result analyze(File sourceRoot, List<File> changedFiles, File reportFile) {
    if (!reportFile.isFile() || reportFile.length() == 0L) {
      return new Result(List.of(), List.copyOf(changedFiles));
    }

    Map<String, LineCoverage> coverageBySourcePath = coverageBySourcePath(reportFile);
    List<FileCoverage> checkedFiles = new ArrayList<>();
    List<File> skippedFiles = new ArrayList<>();

    for (File file : changedFiles) {
      LineCoverage coverage = coverageBySourcePath.get(relativePath(sourceRoot, file));
      if (coverage == null || coverage.totalLines() == 0) {
        skippedFiles.add(file);
      } else {
        checkedFiles.add(new FileCoverage(file, coverage.missedLines(), coverage.coveredLines()));
      }
    }

    return new Result(List.copyOf(checkedFiles), List.copyOf(skippedFiles));
  }

  /** Returns a report of the files below the minimum, or an empty string when none are. */
  public static String failureMessage(File projectDir, Result result, BigDecimal minimumRatio) {
    List<FileCoverage> failures = result.failuresBelow(minimumRatio);
    if (failures.isEmpty()) {
      return "";
    }

    String minimumPercent =
        minimumRatio.multiply(ONE_HUNDRED).stripTrailingZeros().toPlainString();
    StringBuilder message = new StringBuilder();
    message.append("Changed-file JaCoCo line coverage is below ").append(minimumPercent).append("%:");
    failures.stream()
        .sorted(Comparator.comparing(it -> it.file().getAbsolutePath()))
        .forEach(
            it ->
                message
                    .append(System.lineSeparator())
                    .append("* ")
                    .append(relativePath(projectDir, it.file()))
                    .append(": ")
                    .append(it.coveredPercent())
                    .append("% (")
                    .append(it.coveredLines())
                    .append("/")
                    .append(it.totalLines())
                    .append(" covered lines)"));
    return message.toString();
  }

  private static Map<String, LineCoverage> coverageBySourcePath(File reportFile) {
    Element documentElement = parse(reportFile).getDocumentElement();
    NodeList packages = documentElement.getElementsByTagName("package");

    Map<String, LineCoverage> coverage = new LinkedHashMap<>();
    for (int packageIndex = 0; packageIndex < packages.getLength(); packageIndex++) {
      Element packageElement = (Element) packages.item(packageIndex);
      String packageName = packageElement.getAttribute("name");
      NodeList sourceFiles = packageElement.getElementsByTagName("sourcefile");
      for (int sourceIndex = 0; sourceIndex < sourceFiles.getLength(); sourceIndex++) {
        Element sourceFileElement = (Element) sourceFiles.item(sourceIndex);
        LineCoverage lineCoverage = lineCoverage(sourceFileElement);
        if (lineCoverage == null) {
          continue;
        }
        String sourcePath = sourcePath(packageName, sourceFileElement.getAttribute("name"));
        coverage.merge(sourcePath, lineCoverage, LineCoverage::plus);
      }
    }
    return coverage;
  }

  private static org.w3c.dom.Document parse(File reportFile) {
    // JaCoCo's XML declares a DOCTYPE, so the declaration has to be allowed. Every form of
    // external resolution and entity expansion stays off, matching coverageSummary in the
    // root build.gradle. The input is our own build output, not anything user supplied.
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
      throw new IllegalStateException("Failed to parse JaCoCo report " + reportFile, e);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read JaCoCo report " + reportFile, e);
    }
  }

  private static String sourcePath(String packageName, String sourceFileName) {
    if (packageName.isBlank()) {
      return sourceFileName;
    }
    return packageName.replace('.', '/') + "/" + sourceFileName;
  }

  private static LineCoverage lineCoverage(Element sourceFileElement) {
    NodeList counters = sourceFileElement.getElementsByTagName("counter");
    for (int counterIndex = 0; counterIndex < counters.getLength(); counterIndex++) {
      Element counter = (Element) counters.item(counterIndex);
      if ("LINE".equals(counter.getAttribute("type"))) {
        return new LineCoverage(
            Integer.parseInt(counter.getAttribute("missed")),
            Integer.parseInt(counter.getAttribute("covered")));
      }
    }
    return null;
  }

  private static String relativePath(File baseDir, File file) {
    Path relative = normalize(baseDir).relativize(normalize(file));
    List<String> segments = new ArrayList<>();
    relative.forEach(segment -> segments.add(segment.toString()));
    return String.join("/", segments);
  }

  private static Path normalize(File file) {
    return file.toPath().toAbsolutePath().normalize();
  }

  private record LineCoverage(int missedLines, int coveredLines) {

    int totalLines() {
      return missedLines + coveredLines;
    }

    LineCoverage plus(LineCoverage other) {
      return new LineCoverage(
          missedLines + other.missedLines, coveredLines + other.coveredLines);
    }
  }
}

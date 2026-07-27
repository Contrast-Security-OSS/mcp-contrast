package com.contrast.labs.build;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads per-file line coverage out of a JaCoCo XML report and reports which of a given set of
 * changed source files fall below a minimum ratio.
 *
 * <p>Free of Gradle types so it can be unit tested without a build.
 */
public final class JacocoChangedFileCoverage {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final int RATIO_SCALE = 4;
  private static final int PERCENT_SCALE = 2;
  private static final String CLASS_FILE_SUFFIX = ".class";
  private static final String SOURCE_FILE_SUFFIX = ".java";

  private JacocoChangedFileCoverage() {}

  /** Line coverage for a single source file, as counted by JaCoCo. */
  public record FileCoverage(File file, int missedLines, int coveredLines) {

    public FileCoverage {
      if (missedLines < 0 || coveredLines < 0) {
        throw new IllegalArgumentException("Line counts cannot be negative: " + file);
      }
      // analyze routes files with nothing countable to unmeasurableFiles, so there is no ratio
      // to invent here.
      if (missedLines + coveredLines == 0) {
        throw new IllegalArgumentException("No countable lines to measure: " + file);
      }
    }

    public int totalLines() {
      return missedLines + coveredLines;
    }

    public BigDecimal coveredRatio() {
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
   * @param checkedFiles files with countable line data, so the minimum applies
   * @param unmeasurableFiles nothing to measure, either no counted lines or excluded on purpose
   * @param unreportedFiles absent from the report and not excluded, so never measured
   */
  public record Result(
      List<FileCoverage> checkedFiles, List<File> unmeasurableFiles, List<File> unreportedFiles) {

    public List<FileCoverage> failuresBelow(BigDecimal minimumRatio) {
      return checkedFiles.stream()
          .filter(it -> it.coveredRatio().compareTo(minimumRatio) < 0)
          .toList();
    }
  }

  /**
   * Matches each changed file against the report by its path relative to {@code sourceRoot}, which
   * is how JaCoCo names source files.
   *
   * @param excludedSourcePaths source paths that are absent from the report by design, from {@link
   *     #excludedSourcePaths}
   * @throws IllegalStateException when the report is missing or empty, since nothing was measured
   *     and passing would be the fail-open behaviour AIML-501 set out to avoid
   */
  public static Result analyze(
      File sourceRoot, List<File> changedFiles, File reportFile, Set<String> excludedSourcePaths) {
    if (!reportFile.isFile() || reportFile.length() == 0L) {
      throw new IllegalStateException(
          "No JaCoCo report at "
              + reportFile
              + ", so changed-file coverage was never measured. Run jacocoTestReport first.");
    }

    Map<String, LineCoverage> coverageBySourcePath = coverageBySourcePath(reportFile);
    List<FileCoverage> checkedFiles = new ArrayList<>();
    List<File> unmeasurableFiles = new ArrayList<>();
    List<File> unreportedFiles = new ArrayList<>();

    for (File file : changedFiles) {
      String sourcePath = relativePath(sourceRoot, file);
      LineCoverage coverage = coverageBySourcePath.get(sourcePath);
      if (coverage == null) {
        // An excluded class is absent by design. Anything else was never measured.
        if (excludedSourcePaths.contains(sourcePath)) {
          unmeasurableFiles.add(file);
        } else {
          unreportedFiles.add(file);
        }
      } else if (coverage.totalLines() == 0) {
        // An interface, or a type JaCoCo filtered out entirely via Lombok's @Generated.
        unmeasurableFiles.add(file);
      } else {
        checkedFiles.add(new FileCoverage(file, coverage.missedLines(), coverage.coveredLines()));
      }
    }

    return new Result(
        List.copyOf(checkedFiles), List.copyOf(unmeasurableFiles), List.copyOf(unreportedFiles));
  }

  /**
   * Maps JaCoCo class-file exclusion patterns onto the source paths they hide, so a deliberate
   * exclusion is not mistaken for a file the report failed to mention.
   *
   * <p>Wildcard and inner-class patterns name no single source file and are ignored, since the
   * outer class entry already names it.
   */
  public static Set<String> excludedSourcePaths(Collection<String> classFilePatterns) {
    Set<String> sourcePaths = new LinkedHashSet<>();
    for (String pattern : classFilePatterns) {
      if (pattern.indexOf('*') >= 0
          || pattern.indexOf('$') >= 0
          || !pattern.endsWith(CLASS_FILE_SUFFIX)) {
        continue;
      }
      sourcePaths.add(
          pattern.substring(0, pattern.length() - CLASS_FILE_SUFFIX.length()) + SOURCE_FILE_SUFFIX);
    }
    return Set.copyOf(sourcePaths);
  }

  /** Returns a report of the files below the minimum, or an empty string when none are. */
  public static String failureMessage(File projectDir, Result result, BigDecimal minimumRatio) {
    List<FileCoverage> failures = result.failuresBelow(minimumRatio);
    if (failures.isEmpty()) {
      return "";
    }

    String minimumPercent = minimumRatio.multiply(ONE_HUNDRED).stripTrailingZeros().toPlainString();
    StringBuilder message = new StringBuilder();
    message
        .append("Changed-file JaCoCo line coverage is below ")
        .append(minimumPercent)
        .append("%:");
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

  /**
   * Returns a report of the changed files the JaCoCo report never mentioned, or an empty string
   * when there are none.
   */
  public static String unreportedMessage(File projectDir, Result result) {
    if (result.unreportedFiles().isEmpty()) {
      return "";
    }

    StringBuilder message = new StringBuilder();
    message.append(
        "Changed Java file(s) absent from the JaCoCo report, so their coverage could not be "
            + "measured. Either cover them, or exclude the class deliberately through "
            + "coverageExcludedClassFiles in the root build.gradle:");
    result.unreportedFiles().stream()
        .sorted(Comparator.comparing(File::getAbsolutePath))
        .forEach(
            file ->
                message
                    .append(System.lineSeparator())
                    .append("* ")
                    .append(relativePath(projectDir, file)));
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
        String sourcePath = sourcePath(packageName, sourceFileElement.getAttribute("name"));
        // Recorded as zero lines rather than omitted: present-with-no-counter is "nothing to
        // measure", absent is "never measured", and the gate treats them differently.
        LineCoverage lineCoverage = lineCoverage(sourceFileElement);
        coverage.merge(sourcePath, lineCoverage, LineCoverage::plus);
      }
    }
    return coverage;
  }

  private static Document parse(File reportFile) {
    // JaCoCo's XML declares a DOCTYPE, so the declaration has to be allowed. External
    // resolution and entity expansion stay off, matching coverageSummary in the root
    // build.gradle.
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
    return new LineCoverage(0, 0);
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
      return new LineCoverage(missedLines + other.missedLines, coveredLines + other.coveredLines);
    }
  }
}

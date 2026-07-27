package com.contrast.labs.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contrast.labs.build.JacocoChangedFileCoverage.FileCoverage;
import com.contrast.labs.build.JacocoChangedFileCoverage.Result;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

class JacocoChangedFileCoverageTest {

  private static final BigDecimal EIGHTY_FIVE_PERCENT = new BigDecimal("0.85");
  private static final String APPLICATION_CLASS_FILE =
      "com/contrast/labs/ai/mcp/contrast/McpContrastApplication.class";
  private static final String APPLICATION_INNER_CLASS_FILES =
      "com/contrast/labs/ai/mcp/contrast/McpContrastApplication$*.class";
  private static final String APPLICATION_SOURCE_PATH =
      "com/contrast/labs/ai/mcp/contrast/McpContrastApplication.java";

  @TempDir Path tempDir;

  @Test
  void analyze_should_report_line_ratio_when_report_has_the_changed_file() throws IOException {
    Path covered = sourceFile("com/example/Covered.java");
    File report = report(reportPackage("com/example", "Covered.java", 1, 9));

    Result result = analyze(report, covered);

    assertThat(result.unmeasurableFiles()).isEmpty();
    assertThat(result.unreportedFiles()).isEmpty();
    assertThat(result.checkedFiles())
        .singleElement()
        .satisfies(
            coverage -> {
              assertThat(coverage.file()).isEqualTo(covered.toFile());
              assertThat(coverage.missedLines()).isEqualTo(1);
              assertThat(coverage.coveredLines()).isEqualTo(9);
              assertThat(coverage.totalLines()).isEqualTo(10);
              assertThat(coverage.coveredPercent()).isEqualByComparingTo("90.00");
            });
  }

  @Test
  void analyze_should_sum_the_counters_when_the_report_lists_the_source_file_twice()
      throws IOException {
    Path split = sourceFile("com/example/Split.java");
    File report =
        report(
            reportPackage("com/example", "Split.java", 1, 4),
            reportPackage("com/example", "Split.java", 1, 4));

    Result result = analyze(report, split);

    assertThat(result.checkedFiles())
        .singleElement()
        .satisfies(
            coverage -> {
              assertThat(coverage.missedLines()).isEqualTo(2);
              assertThat(coverage.coveredLines()).isEqualTo(8);
              assertThat(coverage.coveredPercent()).isEqualByComparingTo("80.00");
            });
  }

  @Test
  void
      analyze_should_report_the_file_as_unreported_when_the_report_omits_it_and_it_is_not_excluded()
          throws IOException {
    Path missing = sourceFile("com/example/Missing.java");
    File report = report(reportPackage("com/example", "Other.java", 0, 5));

    Result result = analyze(report, missing);

    assertThat(result.checkedFiles()).isEmpty();
    assertThat(result.unmeasurableFiles()).isEmpty();
    assertThat(result.unreportedFiles()).containsExactly(missing.toFile());
  }

  @Test
  void analyze_should_report_the_file_as_unmeasurable_when_the_report_omits_an_excluded_file()
      throws IOException {
    Path excluded = sourceFile(APPLICATION_SOURCE_PATH);
    File report = report(reportPackage("com/example", "Other.java", 0, 5));

    Result result =
        JacocoChangedFileCoverage.analyze(
            sourceRoot(), List.of(excluded.toFile()), report, Set.of(APPLICATION_SOURCE_PATH));

    assertThat(result.checkedFiles()).isEmpty();
    assertThat(result.unreportedFiles()).isEmpty();
    assertThat(result.unmeasurableFiles()).containsExactly(excluded.toFile());
  }

  @Test
  void analyze_should_report_the_file_as_unmeasurable_when_it_has_no_countable_lines()
      throws IOException {
    Path marker = sourceFile("com/example/Marker.java");
    File report = report(reportPackage("com/example", "Marker.java", 0, 0));

    Result result = analyze(report, marker);

    assertThat(result.checkedFiles()).isEmpty();
    assertThat(result.unreportedFiles()).isEmpty();
    assertThat(result.unmeasurableFiles()).containsExactly(marker.toFile());
  }

  @Test
  void analyze_should_report_the_file_as_unmeasurable_when_the_report_has_no_line_counter()
      throws IOException {
    Path contract = sourceFile("com/example/Contract.java");
    File report = report(reportPackageWithoutLineCounter("com/example", "Contract.java"));

    Result result = analyze(report, contract);

    assertThat(result.checkedFiles()).isEmpty();
    assertThat(result.unreportedFiles()).isEmpty();
    assertThat(result.unmeasurableFiles()).containsExactly(contract.toFile());
  }

  @Test
  void analyze_should_match_the_file_when_the_package_is_the_default_package() throws IOException {
    Path root = sourceFile("Root.java");
    File report = report(reportPackage("", "Root.java", 3, 7));

    Result result = analyze(report, root);

    assertThat(result.unmeasurableFiles()).isEmpty();
    assertThat(result.unreportedFiles()).isEmpty();
    assertThat(result.checkedFiles())
        .singleElement()
        .satisfies(coverage -> assertThat(coverage.coveredPercent()).isEqualByComparingTo("70.00"));
  }

  @Test
  void analyze_should_throw_when_the_report_file_is_missing() throws IOException {
    Path changed = sourceFile("com/example/Covered.java");
    File absentReport = tempDir.resolve("absent.xml").toFile();

    assertThatThrownBy(
            () ->
                JacocoChangedFileCoverage.analyze(
                    sourceRoot(), List.of(changed.toFile()), absentReport, Set.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No JaCoCo report at " + absentReport)
        .hasMessageContaining("changed-file coverage was never measured")
        .hasMessageContaining("Run jacocoTestReport first.");
  }

  @Test
  void analyze_should_throw_when_the_report_file_is_empty() throws IOException {
    Path changed = sourceFile("com/example/Covered.java");
    File emptyReport = Files.createFile(tempDir.resolve("empty.xml")).toFile();

    assertThatThrownBy(
            () ->
                JacocoChangedFileCoverage.analyze(
                    sourceRoot(), List.of(changed.toFile()), emptyReport, Set.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No JaCoCo report at " + emptyReport);
  }

  @Test
  void analyze_should_throw_when_the_report_is_not_well_formed_xml() throws IOException {
    Path changed = sourceFile("com/example/Covered.java");
    Path malformed = tempDir.resolve("malformed.xml");
    Files.writeString(malformed, "<report><package name=\"com/example\">\n");

    assertThatThrownBy(
            () ->
                JacocoChangedFileCoverage.analyze(
                    sourceRoot(), List.of(changed.toFile()), malformed.toFile(), Set.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to parse JaCoCo report " + malformed)
        .hasCauseInstanceOf(SAXException.class);
  }

  @Test
  void failuresBelow_should_select_only_files_under_the_minimum() throws IOException {
    Path failing = sourceFile("com/example/Failing.java");
    Path passing = sourceFile("com/example/Passing.java");
    File report =
        report(
            reportPackage("com/example", "Failing.java", 5, 5),
            reportPackage("com/example", "Passing.java", 1, 19));

    Result result = analyze(report, failing, passing);

    assertThat(result.failuresBelow(EIGHTY_FIVE_PERCENT))
        .extracting(coverage -> coverage.file().getName())
        .containsExactly("Failing.java");
  }

  @Test
  void failuresBelow_should_pass_a_file_sitting_exactly_on_the_minimum() throws IOException {
    Path exact = sourceFile("com/example/Exact.java");
    File report = report(reportPackage("com/example", "Exact.java", 3, 17));

    Result result = analyze(report, exact);

    assertThat(result.checkedFiles())
        .singleElement()
        .satisfies(coverage -> assertThat(coverage.coveredPercent()).isEqualByComparingTo("85.00"));
    assertThat(result.failuresBelow(EIGHTY_FIVE_PERCENT)).isEmpty();
  }

  @Test
  void failureMessage_should_name_each_failing_file_with_its_percentage() throws IOException {
    Path failing = sourceFile("com/example/Failing.java");
    File report = report(reportPackage("com/example", "Failing.java", 6, 4));

    Result result = analyze(report, failing);

    String message =
        JacocoChangedFileCoverage.failureMessage(sourceRoot(), result, EIGHTY_FIVE_PERCENT);

    assertThat(message)
        .startsWith("Changed-file JaCoCo line coverage is below 85%:")
        .contains("com/example/Failing.java: 40.00% (4/10 covered lines)");
  }

  @Test
  void failureMessage_should_be_empty_when_every_file_meets_the_minimum() throws IOException {
    Path passing = sourceFile("com/example/Passing.java");
    File report = report(reportPackage("com/example", "Passing.java", 0, 10));

    Result result = analyze(report, passing);

    assertThat(JacocoChangedFileCoverage.failureMessage(sourceRoot(), result, EIGHTY_FIVE_PERCENT))
        .isEmpty();
  }

  @Test
  void unreportedMessage_should_name_each_file_the_report_never_mentioned() throws IOException {
    Path first = sourceFile("com/example/First.java");
    Path second = sourceFile("com/example/Second.java");
    File report = report(reportPackage("com/example", "Other.java", 0, 5));

    Result result = analyze(report, first, second);

    String message = JacocoChangedFileCoverage.unreportedMessage(sourceRoot(), result);

    assertThat(message)
        .startsWith("Changed Java file(s) absent from the JaCoCo report")
        .contains("coverageExcludedClassFiles in the root build.gradle")
        .contains("* com/example/First.java")
        .contains("* com/example/Second.java");
  }

  @Test
  void unreportedMessage_should_be_empty_when_the_report_mentioned_every_file() throws IOException {
    Path covered = sourceFile("com/example/Covered.java");
    File report = report(reportPackage("com/example", "Covered.java", 0, 10));

    Result result = analyze(report, covered);

    assertThat(JacocoChangedFileCoverage.unreportedMessage(sourceRoot(), result)).isEmpty();
  }

  @Test
  void excludedSourcePaths_should_map_the_class_file_and_ignore_its_inner_class_pattern() {
    Set<String> excluded =
        JacocoChangedFileCoverage.excludedSourcePaths(
            List.of(APPLICATION_CLASS_FILE, APPLICATION_INNER_CLASS_FILES));

    assertThat(excluded).containsExactly(APPLICATION_SOURCE_PATH);
  }

  @Test
  void excludedSourcePaths_should_ignore_wildcard_inner_class_and_non_class_file_patterns() {
    Set<String> excluded =
        JacocoChangedFileCoverage.excludedSourcePaths(
            List.of(
                "com/example/*.class",
                "com/example/Outer$Inner.class",
                "com/example/notes.txt",
                "com/example/Kept.class"));

    assertThat(excluded).containsExactly("com/example/Kept.java");
  }

  @Test
  void excludedSourcePaths_should_be_empty_when_no_patterns_are_given() {
    assertThat(JacocoChangedFileCoverage.excludedSourcePaths(List.of())).isEmpty();
  }

  @Test
  void fileCoverage_should_reject_a_zero_total() {
    File file = tempDir.resolve("Empty.java").toFile();

    assertThatThrownBy(() -> new FileCoverage(file, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No countable lines to measure")
        .hasMessageContaining("Empty.java");
  }

  @Test
  void fileCoverage_should_reject_a_negative_count() {
    File file = tempDir.resolve("Negative.java").toFile();

    assertThatThrownBy(() -> new FileCoverage(file, -1, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Line counts cannot be negative")
        .hasMessageContaining("Negative.java");
    assertThatThrownBy(() -> new FileCoverage(file, 5, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Line counts cannot be negative");
  }

  private Result analyze(File report, Path... changedFiles) {
    List<File> files = Arrays.stream(changedFiles).map(Path::toFile).toList();
    return JacocoChangedFileCoverage.analyze(sourceRoot(), files, report, Set.of());
  }

  private File sourceRoot() {
    return tempDir.resolve("src/main/java").toFile();
  }

  private Path sourceFile(String relativePath) throws IOException {
    Path file = tempDir.resolve("src/main/java").resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, "// generated by test\n");
    return file;
  }

  private static String reportPackage(
      String packageName, String fileName, int missedLines, int coveredLines) {
    return """
    <package name="%s">
      <sourcefile name="%s">
        <counter type="INSTRUCTION" missed="0" covered="1"/>
        <counter type="LINE" missed="%d" covered="%d"/>
      </sourcefile>
    </package>
    """
        .formatted(packageName, fileName, missedLines, coveredLines);
  }

  private static String reportPackageWithoutLineCounter(String packageName, String fileName) {
    return """
    <package name="%s">
      <sourcefile name="%s">
        <counter type="INSTRUCTION" missed="0" covered="1"/>
      </sourcefile>
    </package>
    """
        .formatted(packageName, fileName);
  }

  /** Carries the DOCTYPE JaCoCo emits, so the parser configuration is exercised not assumed. */
  private File report(String... packages) throws IOException {
    Path file = tempDir.resolve("jacocoTestReport.xml");
    Files.writeString(
        file,
        """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
        <report name="mcp-contrast">
        %s
        </report>
        """
            .formatted(String.join("\n", packages)));
    return file.toFile();
  }
}

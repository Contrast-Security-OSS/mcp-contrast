package com.contrast.labs.build;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.build.JacocoChangedFileCoverage.Result;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JacocoChangedFileCoverageTest {

  private static final BigDecimal EIGHTY_FIVE_PERCENT = new BigDecimal("0.85");

  @TempDir Path tempDir;

  @Test
  void analyze_should_report_line_ratio_when_report_has_the_changed_file() throws IOException {
    Path sourceRoot = sourceFile("com/example/Covered.java");
    File report =
        report(
            sourceFile("com/example", "Covered.java", 1, 9));

    Result result =
        JacocoChangedFileCoverage.analyze(
            sourceRoot(), List.of(sourceRoot.toFile()), report);

    assertThat(result.skippedFiles()).isEmpty();
    assertThat(result.checkedFiles())
        .singleElement()
        .satisfies(
            coverage -> {
              assertThat(coverage.missedLines()).isEqualTo(1);
              assertThat(coverage.coveredLines()).isEqualTo(9);
              assertThat(coverage.totalLines()).isEqualTo(10);
              assertThat(coverage.coveredPercent()).isEqualByComparingTo("90.00");
            });
  }

  @Test
  void analyze_should_skip_the_file_when_the_report_does_not_list_it() throws IOException {
    Path changed = sourceFile("com/example/Excluded.java");
    File report = report(sourceFile("com/example", "Other.java", 0, 5));

    Result result =
        JacocoChangedFileCoverage.analyze(sourceRoot(), List.of(changed.toFile()), report);

    assertThat(result.checkedFiles()).isEmpty();
    assertThat(result.skippedFiles()).containsExactly(changed.toFile());
  }

  @Test
  void analyze_should_skip_every_file_when_the_report_is_missing() throws IOException {
    Path changed = sourceFile("com/example/Covered.java");

    Result result =
        JacocoChangedFileCoverage.analyze(
            sourceRoot(), List.of(changed.toFile()), tempDir.resolve("absent.xml").toFile());

    assertThat(result.checkedFiles()).isEmpty();
    assertThat(result.skippedFiles()).containsExactly(changed.toFile());
  }

  @Test
  void analyze_should_skip_the_file_when_it_has_no_countable_lines() throws IOException {
    Path changed = sourceFile("com/example/Marker.java");
    File report = report(sourceFile("com/example", "Marker.java", 0, 0));

    Result result =
        JacocoChangedFileCoverage.analyze(sourceRoot(), List.of(changed.toFile()), report);

    assertThat(result.checkedFiles()).isEmpty();
    assertThat(result.skippedFiles()).containsExactly(changed.toFile());
  }

  @Test
  void analyze_should_match_the_file_when_the_package_is_the_default_package() throws IOException {
    Path changed = sourceFile("Root.java");
    File report = report(sourceFile("", "Root.java", 3, 7));

    Result result =
        JacocoChangedFileCoverage.analyze(sourceRoot(), List.of(changed.toFile()), report);

    assertThat(result.skippedFiles()).isEmpty();
    assertThat(result.checkedFiles())
        .singleElement()
        .satisfies(coverage -> assertThat(coverage.coveredPercent()).isEqualByComparingTo("70.00"));
  }

  @Test
  void failuresBelow_should_select_only_files_under_the_minimum() throws IOException {
    Path failing = sourceFile("com/example/Failing.java");
    Path passing = sourceFile("com/example/Passing.java");
    File report =
        report(
            sourceFile("com/example", "Failing.java", 5, 5),
            sourceFile("com/example", "Passing.java", 1, 19));

    Result result =
        JacocoChangedFileCoverage.analyze(
            sourceRoot(), List.of(failing.toFile(), passing.toFile()), report);

    assertThat(result.failuresBelow(EIGHTY_FIVE_PERCENT))
        .extracting(coverage -> coverage.file().getName())
        .containsExactly("Failing.java");
  }

  @Test
  void failuresBelow_should_pass_a_file_sitting_exactly_on_the_minimum() throws IOException {
    Path exact = sourceFile("com/example/Exact.java");
    File report = report(sourceFile("com/example", "Exact.java", 3, 17));

    Result result =
        JacocoChangedFileCoverage.analyze(sourceRoot(), List.of(exact.toFile()), report);

    assertThat(result.checkedFiles())
        .singleElement()
        .satisfies(coverage -> assertThat(coverage.coveredPercent()).isEqualByComparingTo("85.00"));
    assertThat(result.failuresBelow(EIGHTY_FIVE_PERCENT)).isEmpty();
  }

  @Test
  void failureMessage_should_name_each_failing_file_with_its_percentage() throws IOException {
    Path failing = sourceFile("com/example/Failing.java");
    File report = report(sourceFile("com/example", "Failing.java", 6, 4));

    Result result =
        JacocoChangedFileCoverage.analyze(sourceRoot(), List.of(failing.toFile()), report);

    String message =
        JacocoChangedFileCoverage.failureMessage(sourceRoot(), result, EIGHTY_FIVE_PERCENT);

    assertThat(message)
        .startsWith("Changed-file JaCoCo line coverage is below 85%:")
        .contains("com/example/Failing.java: 40.00% (4/10 covered lines)");
  }

  @Test
  void failureMessage_should_be_empty_when_every_file_meets_the_minimum() throws IOException {
    Path passing = sourceFile("com/example/Passing.java");
    File report = report(sourceFile("com/example", "Passing.java", 0, 10));

    Result result =
        JacocoChangedFileCoverage.analyze(sourceRoot(), List.of(passing.toFile()), report);

    assertThat(JacocoChangedFileCoverage.failureMessage(sourceRoot(), result, EIGHTY_FIVE_PERCENT))
        .isEmpty();
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

  private static String sourceFile(
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

  /**
   * JaCoCo emits a DOCTYPE, which trips XML parsers configured to reject the declaration outright.
   * The fixture carries one so the parser configuration is exercised rather than assumed.
   */
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

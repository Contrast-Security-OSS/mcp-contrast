package com.contrast.labs.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PmdBaselineVerificationTest {

  @TempDir Path tempDir;

  @Test
  void pmdFindings_should_extract_file_and_rule_from_report() throws IOException {
    File report =
        pmdReport(
            pmdFile(
                "/repo/src/main/java/com/example/Foo.java",
                violation("CyclomaticComplexity"),
                violation("ExcessiveParameterList")));

    Set<String> findings = PmdBaselineVerification.pmdFindings(report, new File("/repo"));

    assertThat(findings)
        .containsExactlyInAnyOrder(
            "PMD|src/main/java/com/example/Foo.java|CyclomaticComplexity",
            "PMD|src/main/java/com/example/Foo.java|ExcessiveParameterList");
  }

  @Test
  void pmdFindings_should_deduplicate_same_rule_in_same_file() throws IOException {
    File report =
        pmdReport(
            pmdFile(
                "/repo/src/main/java/com/example/Bar.java",
                violation("UnusedPrivateField"),
                violation("UnusedPrivateField")));

    Set<String> findings = PmdBaselineVerification.pmdFindings(report, new File("/repo"));

    assertThat(findings)
        .containsExactly("PMD|src/main/java/com/example/Bar.java|UnusedPrivateField");
  }

  @Test
  void pmdFindings_should_return_empty_for_missing_file() {
    File absent = tempDir.resolve("absent.xml").toFile();

    assertThat(PmdBaselineVerification.pmdFindings(absent, tempDir.toFile())).isEmpty();
  }

  @Test
  void pmdFindings_should_return_empty_for_empty_file() throws IOException {
    File empty = Files.createFile(tempDir.resolve("empty.xml")).toFile();

    assertThat(PmdBaselineVerification.pmdFindings(empty, tempDir.toFile())).isEmpty();
  }

  @Test
  void pmdFindings_should_return_empty_for_clean_report() throws IOException {
    File report = pmdReport();

    assertThat(PmdBaselineVerification.pmdFindings(report, tempDir.toFile())).isEmpty();
  }

  @Test
  void cpdFindings_should_extract_sorted_file_paths() throws IOException {
    File report =
        cpdReport(
            duplication(
                "/repo/src/main/java/com/example/B.java",
                "/repo/src/main/java/com/example/A.java"));

    Set<String> findings = PmdBaselineVerification.cpdFindings(report, new File("/repo"));

    assertThat(findings)
        .containsExactly("CPD|src/main/java/com/example/A.java,src/main/java/com/example/B.java");
  }

  @Test
  void cpdFindings_should_return_empty_for_missing_file() {
    File absent = tempDir.resolve("cpd-absent.xml").toFile();

    assertThat(PmdBaselineVerification.cpdFindings(absent, tempDir.toFile())).isEmpty();
  }

  @Test
  void allFindings_should_combine_pmd_and_cpd_reports() throws IOException {
    File pmd =
        pmdReport(
            pmdFile("/repo/src/main/java/com/example/Foo.java", violation("EmptyCatchBlock")));
    File cpd =
        cpdReportNamed(
            "cpd.xml",
            duplication(
                "/repo/src/main/java/com/example/A.java",
                "/repo/src/main/java/com/example/B.java"));

    Set<String> findings =
        PmdBaselineVerification.allFindings(List.of(pmd, cpd), new File("/repo"));

    assertThat(findings)
        .containsExactlyInAnyOrder(
            "PMD|src/main/java/com/example/Foo.java|EmptyCatchBlock",
            "CPD|src/main/java/com/example/A.java,src/main/java/com/example/B.java");
  }

  @Test
  void readBaseline_should_skip_comments_and_blank_lines() throws IOException {
    Path baseline = tempDir.resolve("baseline.txt");
    Files.writeString(
        baseline,
        """
        # Comment
        PMD|src/Foo.java|Rule1

        PMD|src/Bar.java|Rule2
        # Another comment
        """);

    Set<String> entries = PmdBaselineVerification.readBaseline(baseline.toFile());

    assertThat(entries).containsExactly("PMD|src/Foo.java|Rule1", "PMD|src/Bar.java|Rule2");
  }

  @Test
  void readBaseline_should_return_empty_for_missing_file() {
    File absent = tempDir.resolve("absent-baseline.txt").toFile();

    assertThat(PmdBaselineVerification.readBaseline(absent)).isEmpty();
  }

  @Test
  void newFindings_should_return_findings_not_in_baseline() {
    Set<String> findings = Set.of("PMD|A.java|Rule1", "PMD|B.java|Rule2");
    Set<String> baseline = Set.of("PMD|A.java|Rule1");

    List<String> result = PmdBaselineVerification.newFindings(findings, baseline);

    assertThat(result).containsExactly("PMD|B.java|Rule2");
  }

  @Test
  void newFindings_should_return_empty_when_all_findings_are_baselined() {
    Set<String> findings = Set.of("PMD|A.java|Rule1");
    Set<String> baseline = Set.of("PMD|A.java|Rule1", "PMD|B.java|Rule2");

    assertThat(PmdBaselineVerification.newFindings(findings, baseline)).isEmpty();
  }

  @Test
  void staleEntries_should_return_baseline_entries_no_longer_reported() {
    Set<String> findings = Set.of("PMD|A.java|Rule1");
    Set<String> baseline = Set.of("PMD|A.java|Rule1", "PMD|Gone.java|Rule2");

    List<String> stale = PmdBaselineVerification.staleEntries(findings, baseline);

    assertThat(stale).containsExactly("PMD|Gone.java|Rule2");
  }

  @Test
  void staleEntries_should_return_empty_when_baseline_matches_findings() {
    Set<String> findings = Set.of("PMD|A.java|Rule1");
    Set<String> baseline = Set.of("PMD|A.java|Rule1");

    assertThat(PmdBaselineVerification.staleEntries(findings, baseline)).isEmpty();
  }

  @Test
  void writeBaseline_should_produce_sorted_entries_with_header() throws IOException {
    File baselineFile = tempDir.resolve("out-baseline.txt").toFile();
    Set<String> findings =
        Set.of("PMD|src/B.java|Rule2", "CPD|src/A.java,src/C.java", "PMD|src/A.java|Rule1");

    PmdBaselineVerification.writeBaseline(baselineFile, findings, "my-module");

    String content = Files.readString(baselineFile.toPath());
    assertThat(content).startsWith("# PMD baseline for my-module.");
    assertThat(content).contains("./gradlew :my-module:writePmdBaseline");

    Set<String> roundTripped = PmdBaselineVerification.readBaseline(baselineFile);
    assertThat(roundTripped).containsExactlyInAnyOrderElementsOf(findings);
  }

  @Test
  void tightenBaseline_should_remove_stale_entries_and_return_them() throws IOException {
    File baselineFile = tempDir.resolve("tighten-baseline.txt").toFile();
    PmdBaselineVerification.writeBaseline(
        baselineFile, Set.of("PMD|A.java|Rule1", "PMD|Gone.java|Rule2"), "mod");

    Set<String> currentFindings = Set.of("PMD|A.java|Rule1");
    List<String> removed =
        PmdBaselineVerification.tightenBaseline(baselineFile, currentFindings, "mod");

    assertThat(removed).containsExactly("PMD|Gone.java|Rule2");
    assertThat(PmdBaselineVerification.readBaseline(baselineFile))
        .containsExactly("PMD|A.java|Rule1");
  }

  @Test
  void tightenBaseline_should_return_empty_when_nothing_to_remove() throws IOException {
    File baselineFile = tempDir.resolve("tight-baseline.txt").toFile();
    Set<String> findings = Set.of("PMD|A.java|Rule1");
    PmdBaselineVerification.writeBaseline(baselineFile, findings, "mod");

    List<String> removed = PmdBaselineVerification.tightenBaseline(baselineFile, findings, "mod");

    assertThat(removed).isEmpty();
  }

  // -- report builders --

  private File pmdReport(String... fileElements) throws IOException {
    return pmdReportNamed("pmd-report.xml", fileElements);
  }

  private File pmdReportNamed(String name, String... fileElements) throws IOException {
    Path file = tempDir.resolve(name);
    Files.writeString(
        file,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <pmd xmlns="http://pmd.sourceforge.net/report/2.0.0" version="7.26.0">
        %s
        </pmd>
        """
            .formatted(String.join("\n", fileElements)));
    return file.toFile();
  }

  private File cpdReport(String... duplications) throws IOException {
    return cpdReportNamed("cpd.xml", duplications);
  }

  private File cpdReportNamed(String name, String... duplications) throws IOException {
    Path file = tempDir.resolve(name);
    Files.writeString(
        file,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <pmd-cpd>
        %s
        </pmd-cpd>
        """
            .formatted(String.join("\n", duplications)));
    return file.toFile();
  }

  private static String pmdFile(String path, String... violations) {
    return """
    <file name="%s">
    %s
    </file>
    """
        .formatted(path, String.join("\n", violations));
  }

  private static String violation(String ruleName) {
    return """
    <violation beginline="1" endline="5" rule="%s" ruleset="test" priority="3">
      Test violation
    </violation>
    """
        .formatted(ruleName);
  }

  private static String duplication(String... paths) {
    StringBuilder sb = new StringBuilder();
    sb.append("<duplication lines=\"10\" tokens=\"80\">\n");
    for (String path : paths) {
      sb.append("  <file path=\"").append(path).append("\" line=\"1\"/>\n");
    }
    sb.append("  <codefragment><![CDATA[code]]></codefragment>\n");
    sb.append("</duplication>");
    return sb.toString();
  }
}

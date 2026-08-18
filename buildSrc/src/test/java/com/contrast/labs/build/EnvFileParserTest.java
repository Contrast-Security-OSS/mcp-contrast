package com.contrast.labs.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvFileParserTest {

  @TempDir Path tmp;

  @Test
  void parseLines_should_return_empty_map_when_no_lines() {
    assertThat(EnvFileParser.parseLines(List.of())).isEmpty();
  }

  @Test
  void parseLines_should_skip_comment_lines() {
    var result = EnvFileParser.parseLines(List.of("# this is a comment", "KEY=value"));
    assertThat(result).containsExactly(Map.entry("KEY", "value"));
  }

  @Test
  void parseLines_should_skip_blank_lines() {
    var result = EnvFileParser.parseLines(List.of("", "  ", "KEY=value"));
    assertThat(result).containsExactly(Map.entry("KEY", "value"));
  }

  @Test
  void parseLines_should_strip_export_prefix() {
    var result = EnvFileParser.parseLines(List.of("export FOO=bar"));
    assertThat(result).containsExactly(Map.entry("FOO", "bar"));
  }

  @Test
  void parseLines_should_strip_double_quotes_from_values() {
    var result = EnvFileParser.parseLines(List.of("KEY=\"quoted value\""));
    assertThat(result).containsExactly(Map.entry("KEY", "quoted value"));
  }

  @Test
  void parseLines_should_strip_single_quotes_from_values() {
    var result = EnvFileParser.parseLines(List.of("KEY='quoted value'"));
    assertThat(result).containsExactly(Map.entry("KEY", "quoted value"));
  }

  @Test
  void parseLines_should_not_strip_mismatched_quotes() {
    var result = EnvFileParser.parseLines(List.of("KEY=\"mismatched'"));
    assertThat(result).containsExactly(Map.entry("KEY", "\"mismatched'"));
  }

  @Test
  void parseLines_should_not_strip_single_char_quoted_value() {
    var result = EnvFileParser.parseLines(List.of("KEY=\""));
    assertThat(result).containsExactly(Map.entry("KEY", "\""));
  }

  @Test
  void parseLines_should_handle_empty_value() {
    var result = EnvFileParser.parseLines(List.of("KEY="));
    assertThat(result).containsExactly(Map.entry("KEY", ""));
  }

  @Test
  void parseLines_should_skip_lines_without_separator() {
    var result = EnvFileParser.parseLines(List.of("NOSEP", "KEY=value"));
    assertThat(result).containsExactly(Map.entry("KEY", "value"));
  }

  @Test
  void parseLines_should_skip_lines_with_leading_equals() {
    var result = EnvFileParser.parseLines(List.of("=value", "KEY=value"));
    assertThat(result).containsExactly(Map.entry("KEY", "value"));
  }

  @Test
  void parseLines_should_use_first_equals_as_separator() {
    var result = EnvFileParser.parseLines(List.of("KEY=val=ue"));
    assertThat(result).containsExactly(Map.entry("KEY", "val=ue"));
  }

  @Test
  void parseLines_should_trim_whitespace_around_key_and_value() {
    var result = EnvFileParser.parseLines(List.of("  KEY  =  value  "));
    assertThat(result).containsExactly(Map.entry("KEY", "value"));
  }

  @Test
  void parseLines_should_handle_export_with_quotes() {
    var result = EnvFileParser.parseLines(List.of("export KEY=\"hello world\""));
    assertThat(result).containsExactly(Map.entry("KEY", "hello world"));
  }

  @Test
  void parseLines_should_preserve_order() {
    var result = EnvFileParser.parseLines(List.of("A=1", "B=2", "C=3"));
    assertThat(result.keySet()).containsExactly("A", "B", "C");
  }

  @Test
  void parse_should_return_empty_map_when_file_does_not_exist() {
    assertThat(EnvFileParser.parse(new File(tmp.toFile(), "missing"))).isEmpty();
  }

  @Test
  void parse_should_read_file_contents() throws IOException {
    Path envFile = tmp.resolve("test.env");
    Files.writeString(envFile, "export HOST=example.com\nAPI_KEY='secret'\n");
    var result = EnvFileParser.parse(envFile.toFile());
    assertThat(result)
        .containsEntry("HOST", "example.com")
        .containsEntry("API_KEY", "secret")
        .hasSize(2);
  }

  @Test
  void parseLines_should_handle_template_format() {
    var result =
        EnvFileParser.parseLines(
            List.of(
                "# Integration Test Environment Variables",
                "# Copy this file and fill in your credentials",
                "",
                "export CONTRAST_HOST_NAME=app.contrastsecurity.com",
                "export CONTRAST_API_KEY=your-api-key-here",
                "export CONTRAST_SERVICE_KEY=your-service-key-here",
                "export CONTRAST_USERNAME=your-username-here",
                "export CONTRAST_ORG_ID=your-org-id-here",
                "",
                "# Note: .env.integration-test is in .gitignore"));
    assertThat(result)
        .hasSize(5)
        .containsEntry("CONTRAST_HOST_NAME", "app.contrastsecurity.com")
        .containsEntry("CONTRAST_API_KEY", "your-api-key-here")
        .containsEntry("CONTRAST_ORG_ID", "your-org-id-here");
  }
}

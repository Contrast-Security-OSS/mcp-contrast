package com.contrast.labs.build;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contrast.labs.build.ChangedJavaFiles.ChangedFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChangedJavaFilesTest {

  @TempDir Path repo;

  @BeforeEach
  void initRepository() throws Exception {
    git("init", "--initial-branch=main");
    git("config", "user.email", "build@example.com");
    git("config", "user.name", "Build");
    write("src/main/java/com/example/Existing.java", "class Existing {}");
    git("add", ".");
    git("commit", "-m", "initial");
  }

  @Test
  void forSourceSet_should_return_uncommitted_and_untracked_files_when_no_base_is_given()
      throws Exception {
    Path existing =
        write("src/main/java/com/example/Existing.java", "class Existing { void a() {} }");
    Path added = write("src/main/java/com/example/Added.java", "class Added {}");

    ChangedFiles changed = forMainSourceSet(null, null);

    assertThat(changed.present()).containsExactlyInAnyOrder(existing.toFile(), added.toFile());
    assertThat(changed.absentFromWorkingTree()).isEmpty();
    assertThat(changed.isEmpty()).isFalse();
  }

  @Test
  void forSourceSet_should_return_only_files_in_the_pushed_range_when_a_base_is_given()
      throws Exception {
    write("src/main/java/com/example/InBase.java", "class InBase {}");
    git("add", ".");
    git("commit", "-m", "base commit");
    String base = revParse("HEAD");

    Path inRange = write("src/main/java/com/example/InRange.java", "class InRange {}");
    git("add", ".");
    git("commit", "-m", "range commit");

    ChangedFiles changed = forMainSourceSet(base, "HEAD");

    assertThat(changed.present()).containsExactly(inRange.toFile());
    assertThat(changed.absentFromWorkingTree()).isEmpty();
  }

  @Test
  void forSourceSet_should_ignore_working_tree_changes_when_a_base_is_given() throws Exception {
    String base = revParse("HEAD");
    Path committed = write("src/main/java/com/example/Committed.java", "class Committed {}");
    git("add", ".");
    git("commit", "-m", "committed");
    write("src/main/java/com/example/Uncommitted.java", "class Uncommitted {}");

    ChangedFiles changed = forMainSourceSet(base, "HEAD");

    assertThat(changed.present()).containsExactly(committed.toFile());
  }

  @Test
  void forSourceSet_should_compare_against_head_when_the_head_ref_is_blank() throws Exception {
    String base = revParse("HEAD");
    Path added = write("src/main/java/com/example/Added.java", "class Added {}");
    git("add", ".");
    git("commit", "-m", "added");

    ChangedFiles changed = forMainSourceSet(base, "   ");

    assertThat(changed.present()).containsExactly(added.toFile());
  }

  @Test
  void forSourceSet_should_exclude_files_outside_the_source_set() throws Exception {
    write("src/test/java/com/example/ExistingTest.java", "class ExistingTest {}");
    Path added = write("src/main/java/com/example/Added.java", "class Added {}");

    ChangedFiles changed = forMainSourceSet(null, null);

    assertThat(changed.present()).containsExactly(added.toFile());
    assertThat(changed.absentFromWorkingTree()).isEmpty();
  }

  @Test
  void forSourceSet_should_exclude_non_java_files() throws Exception {
    write("src/main/java/com/example/notes.txt", "not java");
    Path added = write("src/main/java/com/example/Added.java", "class Added {}");

    ChangedFiles changed = forMainSourceSet(null, null);

    assertThat(changed.present()).containsExactly(added.toFile());
    assertThat(changed.absentFromWorkingTree()).isEmpty();
  }

  @Test
  void forSourceSet_should_return_nothing_when_a_file_was_added_and_deleted_within_the_range()
      throws Exception {
    write("src/main/java/com/example/Doomed.java", "class Doomed {}");
    git("add", ".");
    git("commit", "-m", "add doomed");
    String base = revParse("HEAD~1");
    git("rm", "src/main/java/com/example/Doomed.java");
    git("commit", "-m", "remove doomed");

    ChangedFiles changed = forMainSourceSet(base, "HEAD");

    assertThat(changed.present()).isEmpty();
    assertThat(changed.absentFromWorkingTree()).isEmpty();
    assertThat(changed.isEmpty()).isTrue();
  }

  @Test
  void forSourceSet_should_report_a_file_deleted_from_the_working_tree_as_absent()
      throws Exception {
    String base = revParse("HEAD");
    Path vanished = write("src/main/java/com/example/Vanished.java", "class Vanished {}");
    git("add", ".");
    git("commit", "-m", "add vanished");
    Files.delete(vanished);

    ChangedFiles changed = forMainSourceSet(base, "HEAD");

    assertThat(changed.present()).isEmpty();
    assertThat(changed.absentFromWorkingTree()).containsExactly(vanished.toFile());
    assertThat(changed.isEmpty()).isFalse();
  }

  @Test
  void forSourceSet_should_return_only_the_destination_path_when_a_file_was_renamed()
      throws Exception {
    write("src/main/java/com/example/Before.java", "class Before {}");
    git("add", ".");
    git("commit", "-m", "add before");
    String base = revParse("HEAD");
    git("mv", "src/main/java/com/example/Before.java", "src/main/java/com/example/After.java");
    git("commit", "-m", "rename before");

    ChangedFiles changed = forMainSourceSet(base, "HEAD");

    assertThat(changed.present())
        .containsExactly(repo.resolve("src/main/java/com/example/After.java").toFile());
    assertThat(changed.absentFromWorkingTree()).isEmpty();
  }

  @Test
  void forSourceSet_should_return_non_ascii_paths_when_no_base_is_given() throws Exception {
    // git C-quotes non-ASCII paths as "src/main/java/com/example/Na\303\257ve.java" unless
    // core.quotepath is off, and a quoted path fails the .java suffix test.
    Path tracked = write("src/main/java/com/example/Tracké.java", "class Tracke {}");
    git("add", ".");
    git("commit", "-m", "add tracked");
    write("src/main/java/com/example/Tracké.java", "class Tracke { void a() {} }");
    Path untracked = write("src/main/java/com/example/Naïve.java", "class Naive {}");

    ChangedFiles changed = forMainSourceSet(null, null);

    assertThat(changed.present()).containsExactlyInAnyOrder(tracked.toFile(), untracked.toFile());
    assertThat(changed.absentFromWorkingTree()).isEmpty();
  }

  @Test
  void forSourceSet_should_return_a_non_ascii_path_when_a_base_is_given() throws Exception {
    String base = revParse("HEAD");
    Path committed = write("src/main/java/com/example/Naïve.java", "class Naive {}");
    git("add", ".");
    git("commit", "-m", "add naive");

    ChangedFiles changed = forMainSourceSet(base, "HEAD");

    assertThat(changed.present()).containsExactly(committed.toFile());
    assertThat(changed.absentFromWorkingTree()).isEmpty();
  }

  @Test
  void forSourceSet_should_throw_when_the_base_ref_does_not_exist() {
    assertThatThrownBy(() -> forMainSourceSet("no-such-ref", "HEAD"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Git command failed with exit code 128")
        .hasMessageContaining("no-such-ref...HEAD")
        .hasMessageContaining("bad revision");
  }

  @Test
  void forSourceSet_should_return_empty_when_nothing_changed() {
    ChangedFiles changed = forMainSourceSet(null, null);

    assertThat(changed.present()).isEmpty();
    assertThat(changed.absentFromWorkingTree()).isEmpty();
    assertThat(changed.isEmpty()).isTrue();
  }

  private ChangedFiles forMainSourceSet(String base, String head) {
    return ChangedJavaFiles.forSourceSet(
        repo.toFile(), repo.resolve("src/main/java").toFile(), base, head);
  }

  private Path write(String relativePath, String content) throws IOException {
    Path file = repo.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content + "\n", UTF_8);
    return file;
  }

  private String revParse(String ref) throws Exception {
    return run("git", "-C", repo.toAbsolutePath().toString(), "rev-parse", ref).trim();
  }

  /** Signing and hooks are switched off, so a developer's global git config cannot fail a test. */
  private void git(String... args) throws Exception {
    String[] command = new String[args.length + 7];
    command[0] = "git";
    command[1] = "-c";
    command[2] = "commit.gpgsign=false";
    command[3] = "-c";
    command[4] = "core.hooksPath=/dev/null";
    command[5] = "-C";
    command[6] = repo.toAbsolutePath().toString();
    System.arraycopy(args, 0, command, 7, args.length);
    run(command);
  }

  private static String run(String... command) throws Exception {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), UTF_8);
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IllegalStateException(
          "Command failed (" + exitCode + "): " + String.join(" ", command) + "\n" + output);
    }
    return output;
  }
}

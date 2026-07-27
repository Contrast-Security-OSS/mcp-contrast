package com.contrast.labs.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    write("src/main/java/com/example/Existing.java", "class Existing { void a() {} }");
    write("src/main/java/com/example/Added.java", "class Added {}");

    List<File> changed = forMainSourceSet(null, null);

    assertThat(changed)
        .extracting(File::getName)
        .containsExactlyInAnyOrder("Existing.java", "Added.java");
  }

  @Test
  void forSourceSet_should_return_only_files_in_the_pushed_range_when_a_base_is_given()
      throws Exception {
    write("src/main/java/com/example/InBase.java", "class InBase {}");
    git("add", ".");
    git("commit", "-m", "base commit");
    String base = revParse("HEAD");

    write("src/main/java/com/example/InRange.java", "class InRange {}");
    git("add", ".");
    git("commit", "-m", "range commit");

    List<File> changed = forMainSourceSet(base, "HEAD");

    assertThat(changed).extracting(File::getName).containsExactly("InRange.java");
  }

  @Test
  void forSourceSet_should_ignore_working_tree_changes_when_a_base_is_given() throws Exception {
    String base = revParse("HEAD");
    write("src/main/java/com/example/Committed.java", "class Committed {}");
    git("add", ".");
    git("commit", "-m", "committed");
    write("src/main/java/com/example/Uncommitted.java", "class Uncommitted {}");

    List<File> changed = forMainSourceSet(base, "HEAD");

    assertThat(changed).extracting(File::getName).containsExactly("Committed.java");
  }

  @Test
  void forSourceSet_should_exclude_files_outside_the_source_set() throws Exception {
    write("src/test/java/com/example/ExistingTest.java", "class ExistingTest {}");
    write("src/main/java/com/example/Added.java", "class Added {}");

    List<File> changed = forMainSourceSet(null, null);

    assertThat(changed).extracting(File::getName).containsExactly("Added.java");
  }

  @Test
  void forSourceSet_should_exclude_non_java_files() throws Exception {
    write("src/main/java/com/example/notes.txt", "not java");
    write("src/main/java/com/example/Added.java", "class Added {}");

    List<File> changed = forMainSourceSet(null, null);

    assertThat(changed).extracting(File::getName).containsExactly("Added.java");
  }

  @Test
  void forSourceSet_should_exclude_a_deleted_file_that_no_longer_exists_on_disk() throws Exception {
    write("src/main/java/com/example/Doomed.java", "class Doomed {}");
    git("add", ".");
    git("commit", "-m", "add doomed");
    String base = revParse("HEAD~1");
    git("rm", "src/main/java/com/example/Doomed.java");
    git("commit", "-m", "remove doomed");

    List<File> changed = forMainSourceSet(base, "HEAD");

    assertThat(changed).isEmpty();
  }

  @Test
  void forSourceSet_should_return_empty_when_nothing_changed() {
    assertThat(forMainSourceSet(null, null)).isEmpty();
  }

  private List<File> forMainSourceSet(String base, String head) {
    return ChangedJavaFiles.forSourceSet(
        repo.toFile(), repo.resolve("src/main/java").toFile(), base, head);
  }

  private void write(String relativePath, String content) throws IOException {
    Path file = repo.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content + "\n");
  }

  private String revParse(String ref) throws Exception {
    return run("git", "-C", repo.toAbsolutePath().toString(), "rev-parse", ref).trim();
  }

  private void git(String... args) throws Exception {
    String[] command = new String[args.length + 3];
    command[0] = "git";
    command[1] = "-C";
    command[2] = repo.toAbsolutePath().toString();
    System.arraycopy(args, 0, command, 3, args.length);
    run(command);
  }

  private static String run(String... command) throws Exception {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes());
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IllegalStateException(
          "Command failed (" + exitCode + "): " + String.join(" ", command) + "\n" + output);
    }
    return output;
  }
}

package gitlab.clone;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabCloneCommandTest {

    public static final String PUBLIC_GROUP_NAME = "gitlab-clone-example";
    @TempDir
    File cloneDirectory;

    @Test
    public void run_publicGroup_withoutRecursion() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"-v", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertThat(baos.toString()).contains("Cloning group 'gitlab-clone-example'")
                                       .contains("Looking for group named: gitlab-clone-example")
                                       .contains("Searching for projects in group gitlab-clone-example")
                                       .contains("All done");

            assertThat(cloneDirectory).isDirectoryContaining("glob:**/gitlab-clone-example")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/a-project/README.md")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/sub-group-1/some-project/README.md")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/sub-group-2/sub-group-3/another-project/README.md");

            final Path submodulePath = Path.of(cloneDirectory.getAbsolutePath(), "gitlab-clone-example", "a-project", "some-project-sub-module");
            assertThat(submodulePath).isEmptyDirectory();
        }
    }

    @Test
    public void run_publicGroup_withRecursion() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"-v", "-r", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertThat(baos.toString()).contains("Cloning group 'gitlab-clone-example'")
                                       .contains("Looking for group named: gitlab-clone-example")
                                       .contains("Searching for projects in group gitlab-clone-example")
                                       .contains("All done");

            assertThat(cloneDirectory).isDirectoryContaining("glob:**/gitlab-clone-example")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/a-project/README.md")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/a-project/some-project-sub-module/README.md")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/sub-group-1/some-project/README.md")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/sub-group-2/sub-group-3/another-project/README.md");
        }
    }

    @Test
    @ClearEnvironmentVariable(key = "GITLAB_TOKEN")
    public void run_publicGroup_withRecursion_withoutGitlabToken() {
        assertThat(System.getenv("GITLAB_TOKEN")).isNull();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"--trace", "-r", "gitlab-clone-example", cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertThat(baos.toString()).contains("Cloning group 'gitlab-clone-example'")
                                       .contains("Looking for group named: gitlab-clone-example")
                                       .contains("Searching for projects in group gitlab-clone-example")
                                       .contains("All done")
                                       .doesNotContain("PRIVATE-TOKEN");

            assertThat(cloneDirectory).isDirectoryContaining("glob:**/gitlab-clone-example")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/a-project/README.md")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/a-project/some-project-sub-module/README.md")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/sub-group-1/some-project/README.md")
                                      .isDirectoryRecursivelyContaining("glob:**/gitlab-clone-example/sub-group-2/sub-group-3/another-project/README.md");
        }
    }
}

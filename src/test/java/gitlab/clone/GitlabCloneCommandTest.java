package gitlab.clone;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabCloneCommandTest {

    @TempDir
    File cloneDirectory;

    @Test
    public void testWithCommandLineOption() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"-v", "-p", cloneDirectory.toPath().toString(), "-g", "gitlab-clone-example", "-t", System.getenv("GITLAB_PRIVATE_TOKEN")};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            // gitlab-clone
            assertThat(baos.toString()).contains("Cloning group 'gitlab-clone-example'");
        }
    }
}

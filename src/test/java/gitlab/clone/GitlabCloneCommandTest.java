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
            String[] args = new String[]{"-v", "gitlab-clone-example", cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertThat(baos.toString()).contains("Cloning group 'gitlab-clone-example'")
                                       .contains("Looking for group named: gitlab-clone-example")
                                       .contains("Searching for projects in group gitlab-clone-example")
                                       .contains("All done");
        }
    }
}

package gitlab.clone;

import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabCloneCommandWithTokenTest extends GitlabCloneCommandBase {

    @TempDir
    File cloneDirectory;

    @BeforeAll
    static void beforeAll() throws JoranException {
        // Need to do this because configuration is static and loaded once per JVM,
        // if the full logs test runs before this one the full log configuration
        // remains active.
        LoggingConfiguration.loadLogsConfig();
    }

    @Test
    public void run_publicGroup_withoutRecursion_verbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"-v", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertLogsDebug(baos.toString(), PUBLIC_GROUP_NAME);
            assertCloneContentsPublicGroup(cloneDirectory, false);
            final Path submodulePath = Path.of(cloneDirectory.getAbsolutePath(), "gitlab-clone-example", "a-project", "some-project-sub-module");
            assertThat(submodulePath).isEmptyDirectory();
        }
    }

    @Test
    public void run_privateGroup_withoutRecursion_VeryVerbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"-x", PRIVATE_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertLogsTrace(baos.toString(), PRIVATE_GROUP_NAME);
            assertCloneContentsPrivateGroup(cloneDirectory);
        }
    }

    @Test
    public void run_publicGroup_withRecursion_veryVerbose() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(TEST_OUTPUT_ARRAY_SIZE);
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"-x", "-r", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertLogsTrace(baos.toString(), PUBLIC_GROUP_NAME);
            assertCloneContentsPublicGroup(cloneDirectory, true);
        }
    }
}

package gitlab.clone;

import java.io.ByteArrayOutputStream;
import java.io.File;

import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GitlabCloneCommandWithoutTokenTest extends GitlabCloneCommandBase {

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
    public void run_publicGroup_withRecursion_verbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext(NO_TOKEN_CONTEXT_PROPERTIES)) {
            String[] args = new String[]{"-v", "-r", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertLogsDebug(baos.toString(), PUBLIC_GROUP_NAME);
            assertCloneContentsPublicGroup(cloneDirectory, true);
        }
    }

    @Test
    public void run_privateGroup_withoutRecursion_veryVerbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext(NO_TOKEN_CONTEXT_PROPERTIES)) {
            String[] args = new String[]{"-x", PRIVATE_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            final AbstractStringAssert<?> testAssert = assertLogsTrace(baos.toString(), PRIVATE_GROUP_NAME);
            assertLogsTraceWhenGroupNotFound(testAssert, PRIVATE_GROUP_NAME);
            assertNotCloned(cloneDirectory);
        }
    }
}

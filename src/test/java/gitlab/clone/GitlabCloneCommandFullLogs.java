package gitlab.clone;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class GitlabCloneCommandFullLogs extends GitlabCloneCommandBase {

    @TempDir
    File cloneDirectory;

    @Test
    public void run_publicGroup_debug() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"--debug", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertDebugFullLogs(baos.toString(), PUBLIC_GROUP_NAME);
            assertCloneContentsPublicGroup(cloneDirectory, false);
        }
    }

    @Test
    public void run_privateGroup_trace() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"--trace", PRIVATE_GROUP_NAME, cloneDirectory.toPath().toString()};
            PicocliRunner.run(GitlabCloneCommand.class, ctx, args);

            assertTraceFullLogs(baos.toString(), PRIVATE_GROUP_NAME);
            assertCloneContentsPrivateGroup(cloneDirectory);
        }
    }
}

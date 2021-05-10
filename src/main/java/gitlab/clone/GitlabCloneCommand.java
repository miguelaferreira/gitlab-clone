package gitlab.clone;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.logging.LogLevel;
import io.micronaut.logging.LoggingSystem;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Slf4j
@Command(
        name = "gitlab-clone",
        headerHeading = "Usage:%n%n",
        synopsisHeading = "%n",
        header = {
                "Clone an entire GitLab group with all sub-groups and repositories.",
                "While cloning initialize project git sub-modules (may require two runs due to ordering of projects).",
                "When a project is already cloned, tries to initialize git sub-modules."
        },
        description = {
                "The GitLab URL and private token are read from the environment, using GITLAB_URL and GITLAB_TOKEN variables.",
                "GITLAB_URL defaults to 'https://gitlab.com'.",
                "The token in GITLAB_TOKEN needs 'read_api' scope for public groups and 'api' scope for private groups."
        },
        footer = {
                "%nCopyright(c) 2021 - Miguel Ferreira - GitHub/GitLab: @miguelaferreira"
        },
        descriptionHeading = "%nGitLab configuration:%n%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        mixinStandardHelpOptions = true,
        versionProvider = GitlabCloneCommand.AppVersionProvider.class,
        sortOptions = false,
        usageHelpAutoWidth = true
)
public class GitlabCloneCommand implements Runnable {

    @Option(
            order = 0,
            names = {"-v", "--verbose"},
            description = "Print out extra information about what the tool is doing."
    )
    private boolean verbose;

    @Option(
            order = 1,
            names = {"-x", "--very-verbose"},
            description = "Print out even more information about what the tool is doing."
    )
    private boolean veryVerbose;

    @Option(
            order = 2,
            names = {"--debug"},
            description = "Sets all loggers to DEBUG level."
    )
    private boolean debug;

    @Option(
            order = 3,
            names = {"--trace"},
            description = "Sets all loggers to TRACE level. WARNING: this setting will leak the GitLab token to the logs, use with caution."
    )
    private boolean trace;

    @CommandLine.Parameters(
            index = "0",
            paramLabel = "GROUP",
            description = "The GitLab group to clone."
    )
    private String gitlabGroupName;

    @CommandLine.Parameters(
            index = "1",
            paramLabel = "PATH",
            description = "The local path where to create the group clone.",
            defaultValue = ".",
            showDefaultValue = CommandLine.Help.Visibility.ON_DEMAND
    )
    private String localPath;

    @Inject
    GitlabService gitlabService;
    @Inject
    GitService gitService;
    @Inject
    LoggingSystem loggingSystem;

    public static void main(String[] args) {
        PicocliRunner.run(GitlabCloneCommand.class, args);
    }

    @Override
    public void run() {
        if (trace) {
            LoggerUtils.disableDefaultAppender();
            loggingSystem.setLogLevel("root", LogLevel.TRACE);
            loggingSystem.setLogLevel("gitlab.clone", LogLevel.TRACE);
            log.trace("All loggers set to 'TRACE'");
        } else if (debug) {
            LoggerUtils.disableDefaultAppender();
            loggingSystem.setLogLevel("root", LogLevel.DEBUG);
            loggingSystem.setLogLevel("gitlab.clone", LogLevel.DEBUG);
            log.debug("All loggers set to 'DEBUG'");
        } else if (veryVerbose) {
            LoggerUtils.disableFullAppender();
            loggingSystem.setLogLevel("gitlab.clone", LogLevel.TRACE);
            log.trace("Set 'gitlab.clone' logger to TRACE");
        } else if (verbose) {
            LoggerUtils.disableFullAppender();
            loggingSystem.setLogLevel("gitlab.clone", LogLevel.DEBUG);
            log.debug("Set 'gitlab.clone' logger to DEBUG");
        } else {
            LoggerUtils.disableFullAppender();
        }

        log.debug("gitlab-clone {}", String.join("", new AppVersionProvider().getVersion()));

        log.info("Cloning group '{}'", gitlabGroupName);
        final Flowable<Git> operations = gitlabService.searchGroups(gitlabGroupName, true)
                                                      .map(group -> gitlabService.getGitlabGroupProjects(group))
                                                      .flatMap(projects -> gitService.cloneProjects(projects, localPath, true));

        // have to consume all elements of iterable for the code to execute
        operations.blockingIterable()
                  .forEach(gitRepo -> log.trace("Done with: {}", gitRepo));

        log.info("All done");
    }

    static class AppVersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            final InputStream in = AppVersionProvider.class.getResourceAsStream("/VERSION");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String version = reader.lines().collect(Collectors.joining());
            return new String[]{
                    "v" + version
            };
        }
    }

}

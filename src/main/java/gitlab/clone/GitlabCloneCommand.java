package gitlab.clone;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.logging.LogLevel;
import io.micronaut.logging.LoggingSystem;
import io.reactivex.Flowable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
            names = {"-r", "--recurse-submodules"},
            description = "Initialize project submodules. If projects are already cloned try and initialize sub-modules anyway.",
            defaultValue = "false"
    )
    private boolean recurseSubmodules;

    @Option(
            order = 1,
            names = {"-v", "--verbose"},
            description = "Print out extra information about what the tool is doing."
    )
    private boolean verbose;

    @Option(
            order = 2,
            names = {"-x", "--very-verbose"},
            description = "Print out even more information about what the tool is doing."
    )
    private boolean veryVerbose;

    @Option(
            order = 3,
            names = {"--debug"},
            description = "Sets all loggers to DEBUG level."
    )
    private boolean debug;

    @Option(
            order = 4,
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
        try {
            if (trace) {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.TRACE, true);
                log.trace("Set all loggers to TRACE");
            } else if (debug) {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.DEBUG, true);
                log.debug("Set all loggers to DEBUG");
            } else if (veryVerbose) {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.TRACE, false);
                log.trace("Set application loggers to TRACE");
            } else if (verbose) {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.DEBUG, false);
                log.debug("Set application loggers to DEBUG");
            } else {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.INFO, false);
            }
        } catch (JoranException e) {
            System.out.println("ERROR: failed to configure loggers.");
        }

        log.debug("gitlab-clone {}", String.join("", new AppVersionProvider().getVersion()));

        log.info("Cloning group '{}'", gitlabGroupName);
        final Either<String, GitlabGroup> maybeGroup = gitlabService.findGroupByName(gitlabGroupName);
        if (maybeGroup.isLeft()) {
            log.info("Could not find group '{}': {}", gitlabGroupName, maybeGroup.getLeft());
            return;
        }

        final GitlabGroup group = maybeGroup.get();
        final Flowable<GitlabProject> groupProjects = gitlabService.getGitlabGroupProjects(group);
        final Flowable<Tuple2<GitlabProject, GitlabProject>> projectsZip = groupProjects.map(project -> Tuple.of(project, project));
        final Flowable<Tuple2<GitlabProject, Either<String, Git>>> clonedProjects = projectsZip.map(
                tuple1 -> tuple1.map2(
                        project -> recurseSubmodules ? gitService.cloneOrInitSubmodulesProject(project, localPath) : gitService.cloneProject(project, localPath))
        );

        clonedProjects.blockingIterable()
                      .forEach(tuple -> {
                          final GitlabProject project = tuple._1;
                          final Either<String, Git> gitRepoOrError = tuple._2;
                          if (gitRepoOrError.isLeft()) {
                              log.warn(gitRepoOrError.getLeft());
                          } else {
                              log.info("Project '{}' updated.", project.getNameWithNamespace());
                          }
                      });

        log.info("All done");
    }

    static class AppVersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            final InputStream in = AppVersionProvider.class.getResourceAsStream("/VERSION");
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String version = reader.lines().collect(Collectors.joining());
                return new String[]{"v" + version};
            } else {
                return new String[]{"No version"};
            }
        }
    }
}

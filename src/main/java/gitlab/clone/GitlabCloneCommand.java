package gitlab.clone;

import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.env.Environment;
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
                "The GitLab token is used for both querying the GitLab API and discover the group to clone and as the password for cloning using HTTPS.",
                "No token is needed for public groups and repositories."
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
            names = {"-c", "--clone-protocol"},
            description = "Chose the transport protocol used clone the project repositories. Valid values: ${COMPLETION-CANDIDATES}.",
            defaultValue = "SSH"
    )
    private GitlabCloneProtocol cloneProtocol;

    @Option(
            order = 2,
            names = {"-u", "--https-username"},
            description = "The username to authenticate with when the HTTPS clone protocol is selected. This option is required when cloning private groups, in which case the GitLab token will be used as the password.",
            arity = "0..1",
            interactive = true
    )
    private String httpsUsername;

    @Option(
            order = 10,
            names = {"-v", "--verbose"},
            description = "Print out extra information about what the tool is doing."
    )
    private boolean verbose;

    @Option(
            order = 11,
            names = {"-x", "--very-verbose"},
            description = "Print out even more information about what the tool is doing."
    )
    private boolean veryVerbose;

    @Option(
            order = 12,
            names = {"--debug"},
            description = "Sets all loggers to DEBUG level."
    )
    private boolean debug;

    @Option(
            order = 13,
            names = {"--trace"},
            description = "Sets all loggers to TRACE level. WARNING: this setting will leak the GitLab token or password to the logs, use with caution."
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
        final ApplicationContextBuilder builder = ApplicationContext.builder(GitlabCloneCommand.class, Environment.CLI);
        try (ApplicationContext context = builder.start()) {
            new CommandLine(GitlabCloneCommand.class, new MicronautFactory(context)).setCaseInsensitiveEnumValuesAllowed(true)
                                                                                    .execute(args);
        }
    }

    @Override
    public void run() {
        configureLogging();
        log.debug("gitlab-clone {}", String.join("", new AppVersionProvider().getVersion()));
        configureGitService();
        cloneGroup();
    }

    private void configureGitService() {
        gitService.setCloneProtocol(cloneProtocol);
        gitService.setHttpsUsername(httpsUsername);
    }

    private void cloneGroup() {
        log.info("Cloning group '{}'", gitlabGroupName);
        final Either<String, GitlabGroup> maybeGroup = gitlabService.findGroupByName(gitlabGroupName);
        if (maybeGroup.isLeft()) {
            log.info("Could not find group '{}': {}", gitlabGroupName, maybeGroup.getLeft());
            return;
        }

        final GitlabGroup group = maybeGroup.get();
        log.debug("Found group = {}", group);
        final Flowable<Tuple2<GitlabProject, Either<Throwable, Git>>> clonedProjects =
                gitlabService.getGitlabGroupProjects(group)
                             .map(project -> Tuple.of(project, project))
                             .map(tuple -> tuple.map2(
                                     project -> recurseSubmodules
                                             ? gitService.cloneOrInitSubmodulesProject(project, localPath)
                                             : gitService.cloneProject(project, localPath)
                                     )
                             );

        clonedProjects.blockingIterable()
                      .forEach(tuple -> {
                          final GitlabProject project = tuple._1;
                          final Either<Throwable, Git> gitRepoOrError = tuple._2;
                          if (gitRepoOrError.isLeft()) {
                              log.warn("Git operation failed", gitRepoOrError.getLeft());
                          } else {
                              log.info("Project '{}' updated.", project.getNameWithNamespace());
                          }
                      });

        log.info("All done");
    }

    private void configureLogging() {
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

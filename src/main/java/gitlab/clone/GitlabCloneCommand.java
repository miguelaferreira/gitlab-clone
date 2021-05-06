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

@Slf4j
@Command(
        name = "gitlab-clone",
        description = "A tool to clone an entire GitLab group with all sub-groups and repositories.",
        mixinStandardHelpOptions = true
)
public class GitlabCloneCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Print out extra information about what the tool is doing.")
    private boolean verbose;

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
    CloningService cloningService;
    @Inject
    LoggingSystem loggingSystem;

    public static void main(String[] args) {
        PicocliRunner.run(GitlabCloneCommand.class, args);
    }

    @Override
    public void run() {
        if (verbose) {
            loggingSystem.setLogLevel("gitlab.clone", LogLevel.DEBUG);
        }

        log.info("Cloning group '{}'", gitlabGroupName);
        final Flowable<Git> operations = gitlabService.searchGroups(gitlabGroupName, true)
                                                      .map(group -> gitlabService.getGitlabGroupProjects(group))
                                                      .flatMap(projects -> cloningService.cloneProjects(projects, localPath));

        // have to consume all elements of iterable for the code to execute
        operations.blockingIterable()
                  .forEach(gitRepo -> log.trace("Working on: {}", gitRepo));

        log.info("All done");
    }
}

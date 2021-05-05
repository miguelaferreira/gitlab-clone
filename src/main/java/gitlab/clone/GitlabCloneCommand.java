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

    @Option(names = {"-g", "--group"}, description = "The GitLab group.", required = true, paramLabel = "GROUP")
    private String gitlabGroupName;

    @Option(names = {"-t", "--token"}, description = "The GitLab private token.", required = true, paramLabel = "TOKEN")
    private String gitlabToken;

    @Option(names = {"-p", "--path"}, description = "The local path where to create the group clone.", paramLabel = "PATH", defaultValue = ".", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String localPath;

    @Inject
    GitlabService gitlabService;
    @Inject
    CloningService cloningService;
    @Inject
    LoggingSystem loggingSystem;


    public static void main(String[] args) throws Exception {
        PicocliRunner.run(GitlabCloneCommand.class, args);
    }

    @Override
    public void run() {
        if (verbose) {
            loggingSystem.setLogLevel("gitlab.clone", LogLevel.DEBUG);
        }

        log.info("Cloning group '{}'", gitlabGroupName);

        log.debug("Looking for group named: {}", gitlabGroupName);
        final Flowable<Git> operations = gitlabService.searchGroups(gitlabToken, gitlabGroupName, true)
                                                      .map(group -> gitlabService.getGitlabGroupProjects(gitlabToken, group))
                                                      .flatMap(projects -> cloningService.cloneProjects(projects, localPath));

        operations.blockingIterable()
                  .forEach(git -> log.info("Cloned project {}", git.getRepository().getDirectory().toString()));
    }
}

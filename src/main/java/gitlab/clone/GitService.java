package gitlab.clone;

import io.micronaut.context.annotation.Value;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Objects;

@Slf4j
@Singleton
public class GitService {

    private static final SshSessionFactory sshSessionFactory = new OverrideJschConfigSessionFactory();

    private GitlabCloneProtocol cloneProtocol = GitlabCloneProtocol.SSH;
    private String httpsUsername = "";
    @Value("${gitlab.token:}")
    private String httpsPassword = "";

    public void setCloneProtocol(GitlabCloneProtocol cloneProtocol) {
        this.cloneProtocol = cloneProtocol;
    }

    public void setHttpsUsername(String httpsUsername) {
        this.httpsUsername = httpsUsername;
    }

    protected void setHttpsPassword(String httpsPassword) {
        this.httpsPassword = httpsPassword;
    }

    public Either<String, Git> cloneOrInitSubmodulesProject(GitlabProject project, String cloneDirectory) {
        final String projectName = project.getNameWithNamespace();
        log.trace("Cloning or initializing submodules for project '{}' under directory '{}'", projectName, cloneDirectory);
        return tryCloneProject(project, cloneDirectory, projectName, true)
                .recoverWith(t -> Try.of(() -> initSubmodules(project, cloneDirectory)))
                .onSuccess(gitRepo -> log.trace("Initialized submodules of git repository at '{}'", getDirectory(gitRepo)))
                .onFailure(t -> logFailedSubmoduleInit(projectName, t))
                .toEither()
                .mapLeft(t -> String.format("Could not clone nor initialize project submodules for project '%s'.", projectName));
    }

    public Either<String, Git> cloneProject(final GitlabProject project, final String cloneDirectory) {
        final String projectName = project.getNameWithNamespace();
        log.trace("Cloning project '{}' under directory '{}'", projectName, cloneDirectory);
        return tryCloneProject(project, cloneDirectory, projectName, false)
                .toEither()
                .mapLeft(t -> String.format("Could not clone project '%s'.", projectName));
    }

    private Try<Git> tryCloneProject(final GitlabProject project, final String cloneDirectory, final String projectName, final boolean cloneSubmodules) {
        return Try.of(() -> cloneProject(project, cloneDirectory, cloneSubmodules))
                  .onSuccess(gitRepo -> log.trace("Cloned project '{}' to '{}'", projectName, getDirectory(gitRepo)))
                  .onFailure(t -> logFailedClone(projectName, t));
    }

    protected Git openRepository(GitlabProject project, String cloneDirectory) throws IOException {
        String pathToRepo = cloneDirectory + FileSystems.getDefault().getSeparator() + project.getPathWithNamespace();
        return Git.open(new File(pathToRepo));
    }

    private void logFailedClone(String projectName, Throwable throwable) {
        log.debug(String.format("Could not clone project '%s' because: %s", projectName, throwable.getMessage()), throwable);
    }

    private String getDirectory(Git gitRepo) {
        return gitRepo.getRepository().getDirectory().toString();
    }

    protected Git cloneProject(GitlabProject project, String cloneDirectory, boolean cloneSubmodules) throws GitAPIException {
        String pathToClone = cloneDirectory + FileSystems.getDefault().getSeparator() + project.getPathWithNamespace();

        final CloneCommand cloneCommand = Git.cloneRepository();
        switch (cloneProtocol) {
            case SSH -> {
                cloneCommand.setURI(project.getSshUrlToRepo());
                cloneCommand.setTransportConfigCallback(transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                });
            }
            case HTTPS -> {
                cloneCommand.setURI(project.getHttpUrlToRepo());
                final String username = Objects.requireNonNullElse(httpsUsername, "");
                final String password = Objects.requireNonNullElse(httpsPassword, "");
                if (!username.isBlank() && !password.isBlank()) {
                    cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(httpsUsername, httpsPassword));
                } else {
                    log.debug("Credentials for HTTPS remote not set, group to clone must be public.");
                }
            }
        }
        cloneCommand.setDirectory(new File(pathToClone));
        cloneCommand.setCloneSubmodules(cloneSubmodules);

        return cloneCommand.call();
    }

    protected Git initSubmodules(GitlabProject project, String cloneDirectory) throws IOException, GitAPIException {
        final Git repo = openRepository(project, cloneDirectory);
        repo.submoduleInit().call();
        repo.submoduleUpdate().call();
        return repo;
    }

    private void logFailedSubmoduleInit(String projectName, Throwable throwable) {
        log.debug(String.format("Could not initialize submodules for project '%s' because: %s", projectName, throwable.getMessage()), throwable);
    }
}

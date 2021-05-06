package gitlab.clone;

import com.jcraft.jsch.Session;
import io.reactivex.Flowable;
import io.vavr.Tuple;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;

@Slf4j
@Singleton
public class CloningService {

    private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host host, Session session) {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
        }
    };

    public Flowable<Git> cloneProjects(Flowable<GitlabProject> projects, String root) {
        log.debug("Cloning projects under directory '{}'", root);
        return projects.map(project -> Tuple.of(project, Try.of(() -> cloneProject(project, root))))
                       .map(tuple -> {
                           final GitlabProject project = tuple._1;
                           final Try<Git> cloneOperation = tuple._2;
                           final String projectName = project.getNameWithNamespace();
                           log.info("Project '{}'", projectName);
                           return cloneOperation.onSuccess(repo -> log.debug("Cloned project '{}' to '{}'", projectName, getDirectory(repo)))
                                                .onFailure(throwable -> logFailedClone(projectName, throwable))
                                                .recoverWith(throwable -> recoverCloneError(root, project, projectName, throwable))
                                                .onFailure(throwable -> logFailedSubModulesInit(projectName))
                                                .toOption();
                       }).filter(Option::isDefined).map(Option::get);
    }

    private void logFailedSubModulesInit(String projectName) {
        log.debug("Could not initialize submodules for project '{}'", projectName);
    }

    private void logFailedClone(String projectName, Throwable throwable) {
        log.debug("Not cloning project '{}' because: {}", projectName, throwable.getMessage());
    }

    private String getDirectory(Git repo) {
        return repo.getRepository().getDirectory().toString();
    }

    private Try<Git> recoverCloneError(String root, GitlabProject project, String projectName, Throwable throwable) {
        log.debug("Initializing submodules for project '{}'", projectName);
        return Try.of(() -> initSubmodules(project, root));
    }

    protected Git cloneProject(GitlabProject project, String root) throws GitAPIException {
        String pathToClone = root + FileSystems.getDefault().getSeparator() + project.getPathWithNamespace();

        final CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(project.getSshUrlToRepo());
        cloneCommand.setDirectory(new File(pathToClone));
        cloneCommand.setCloneSubmodules(true);
        cloneCommand.setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        });
        return cloneCommand.call();
    }

    protected Git initSubmodules(GitlabProject project, String root) throws GitAPIException, IOException {
        String pathToRepo = root + FileSystems.getDefault().getSeparator() + project.getPathWithNamespace();

        final Git repo = Git.open(new File(pathToRepo));
        repo.submoduleInit().call();
        return repo;
    }
}

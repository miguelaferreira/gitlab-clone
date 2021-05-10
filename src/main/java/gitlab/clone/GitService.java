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
public class GitService {

    public static final SshSessionFactory SSH_SESSION_FACTORY = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host host, Session session) {
            log.trace("ssh :: host = {}", host.getHostName());
            log.trace("ssh :: port = {}", host.getPort());
            log.trace("ssh :: preferred auth = {}", host.getPreferredAuthentications());
            log.trace("ssh :: strict host key checking = {}", host.getStrictHostKeyChecking());
            log.trace("ssh :: username = {}", session.getUserName());
        }
    };

    public Flowable<Git> cloneProjects(Flowable<GitlabProject> projects, String cloneDirectory, boolean cloneSubmodules) {
        log.debug("Cloning projects under directory '{}'", cloneDirectory);
        return projects.map(project -> Tuple.of(project, Try.of(() -> cloneProject(project, cloneDirectory, cloneSubmodules))))
                       .map(tuple -> {
                           final GitlabProject project = tuple._1;
                           final Try<Git> cloneOperation = tuple._2;
                           final String projectName = project.getNameWithNamespace();
                           log.info("Project '{}'", projectName);
                           return cloneOperation.onSuccess(repo -> log.debug("Cloned project '{}' to '{}'", projectName, getDirectory(repo)))
                                                .onFailure(throwable -> logFailedClone(projectName, throwable))
                                                .toOption();
                       }).filter(Option::isDefined).map(Option::get);
    }
    
    private void logFailedClone(String projectName, Throwable throwable) {
        log.debug("Not cloning project '{}' because: {}", projectName, throwable.getMessage());
    }

    private String getDirectory(Git repo) {
        return repo.getRepository().getDirectory().toString();
    }

    protected Git cloneProject(GitlabProject project, String cloneDirectory, boolean cloneSubmodules) throws GitAPIException {
        String pathToClone = cloneDirectory + FileSystems.getDefault().getSeparator() + project.getPathWithNamespace();

        final CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(project.getSshUrlToRepo());
        cloneCommand.setDirectory(new File(pathToClone));
        cloneCommand.setCloneSubmodules(cloneSubmodules);
        cloneCommand.setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(SSH_SESSION_FACTORY);
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

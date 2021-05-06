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
        return projects.map(project -> Tuple.of(project, Try.of(() -> cloneProject(project, root))))
                       .map(tuple -> {
                           final GitlabProject project = tuple._1;
                           final Try<Git> cloneOperation = tuple._2;
                           final String projectName = project.getNameWithNamespace();
                           if (cloneOperation.isSuccess()) {
                               final Git git = cloneOperation.get();
                               return Option.of(git);
                           } else {
                               final String reason = cloneOperation.getCause().getMessage();
                               log.warn("Not cloning project '{}' because: {}", projectName, reason);
                               return Option.<Git>none();
                           }
                       }).filter(Option::isDefined).map(Option::get);
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
}

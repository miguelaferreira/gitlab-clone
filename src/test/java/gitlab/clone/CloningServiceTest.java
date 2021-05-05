package gitlab.clone;

import io.reactivex.Flowable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class CloningServiceTest {

    @TempDir
    File cloneDirectory;

    @Test
    void cloneRepo() throws GitAPIException {
        final GitlabProject project = GitlabProject.builder()
                                                   .name("a-project")
                                                   .sshUrlToRepo("git@gitlab.com:gitlab-clone-example/a-project.git")
                                                   .nameWithNamespace("gitlab-clone-example / a-project")
                                                   .pathWithNamespace("gitlab-clone-example/a-project")
                                                   .build();

        final Git repo = new CloningService().cloneProject(project, cloneDirectory.toPath().toString());

        assertThat(repo.log().call()).isNotEmpty();
    }

    @Test
    void testCloneRepositories() {
        Flowable<GitlabProject> projects = Flowable.just(
                GitlabProject.builder()
                             .name("a-project")
                             .sshUrlToRepo("git@gitlab.com:gitlab-clone-example/a-project.git")
                             .nameWithNamespace("gitlab-clone-example / a-project")
                             .pathWithNamespace("gitlab-clone-example/a-project")
                             .build(),
                GitlabProject.builder()
                             .name("some-project")
                             .sshUrlToRepo("git@gitlab.com:gitlab-clone-example/sub-group-1/some-project.git")
                             .nameWithNamespace("gitlab-clone-example / sub-group-1 / some-project")
                             .pathWithNamespace("gitlab-clone-example/sub-group-1/some-project")
                             .build(),
                GitlabProject.builder()
                             .name("another-project")
                             .sshUrlToRepo("git@gitlab.com:gitlab-clone-example/sub-group-2/sub-group-3/another-project.git")
                             .nameWithNamespace("gitlab-clone-example / sub-group-2 / sub-group-3 / another-project")
                             .pathWithNamespace("gitlab-clone-example/sub-group-2/sub-group-3/another-project")
                             .build()
        );

        final Flowable<Git> gits = new CloningService().cloneProjects(projects, cloneDirectory.toPath().toString());

        assertThat(gits.blockingIterable()).hasSize(3);
    }
}

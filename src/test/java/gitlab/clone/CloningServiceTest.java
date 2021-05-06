package gitlab.clone;

import io.reactivex.Flowable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.submodule.SubmoduleStatusType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jgit.submodule.SubmoduleStatusType.INITIALIZED;

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
    void testCloneRepositories() throws GitAPIException {
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

        final List<Git> repos = StreamSupport.stream(gits.blockingIterable().spliterator(), false)
                                             .collect(Collectors.toList());
        assertThat(repos).hasSize(3);
        assertThat(repos.get(0).submoduleStatus().call())
                .containsKey("some-project-sub-module")
                .allSatisfy((key, value) ->
                        assertThat(value).extracting("type")
                                         .isInstanceOfSatisfying(SubmoduleStatusType.class, status ->
                                                 assertThat(status).isEqualTo(INITIALIZED)));
    }
}

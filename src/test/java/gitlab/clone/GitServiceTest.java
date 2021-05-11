package gitlab.clone;

import io.reactivex.Flowable;
import io.vavr.collection.Stream;
import io.vavr.control.Either;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.submodule.SubmoduleStatusType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jgit.submodule.SubmoduleStatusType.INITIALIZED;
import static org.eclipse.jgit.submodule.SubmoduleStatusType.UNINITIALIZED;

class GitServiceTest {

    @TempDir
    File cloneDirectory;
    private String cloneDirectoryPath;

    @BeforeEach
    void setUp() {
        cloneDirectoryPath = this.cloneDirectory.toPath().toString();
    }

    @Test
    void cloneRepo_withSubmodule() throws GitAPIException {
        final GitlabProject project = GitlabProject.builder()
                                                   .name("a-project")
                                                   .sshUrlToRepo("git@gitlab.com:gitlab-clone-example/a-project.git")
                                                   .nameWithNamespace("gitlab-clone-example / a-project")
                                                   .pathWithNamespace("gitlab-clone-example/a-project")
                                                   .build();

        final Git repo = new GitService().cloneProject(project, cloneDirectoryPath, true);

        assertThat(repo.log().call()).isNotEmpty();
        assertThat(repo.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                 .allSatisfy((key, value) -> submoduleIsInitialized(value));
    }

    @Test
    void cloneRepo_withoutSubmodule() throws GitAPIException {
        final GitlabProject project = GitlabProject.builder()
                                                   .name("a-project")
                                                   .sshUrlToRepo("git@gitlab.com:gitlab-clone-example/a-project.git")
                                                   .nameWithNamespace("gitlab-clone-example / a-project")
                                                   .pathWithNamespace("gitlab-clone-example/a-project")
                                                   .build();

        final Git repo = new GitService().cloneProject(project, cloneDirectory.toPath().toString(), false);

        assertThat(repo.log().call()).isNotEmpty();
        assertThat(repo.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                 .allSatisfy((key, value) -> submoduleIsUninitialized(value));
    }

    @Test
    void testCloneRepositories_freshClone_withSubmodules() throws GitAPIException {
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


        final Stream<Either<GitlabProject, Git>> result = flowableToStream(new GitService().cloneProjects(projects, cloneDirectoryPath, true));
        final List<Git> gits = result.filter(Either::isRight).map(Either::get).toJavaList();

        assertThat(gits).hasSize(3);
        assertThat(gits.get(0).submoduleStatus().call()).containsKey("some-project-sub-module")
                                                        .allSatisfy((key, value) -> submoduleIsInitialized(value));
    }

    @Test
    void testCloneRepositories_existingClone_withSubmodules() throws GitAPIException, IOException {
        final GitService gitService = new GitService();
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
        // create first clone with only one repo
        final GitlabProject firstProject = projects.blockingFirst();
        final Git existingClone = gitService.cloneProject(firstProject, cloneDirectoryPath, true);
        assertThat(existingClone).isNotNull();
        assertThat(existingClone.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                          .allSatisfy((key, value) -> submoduleIsInitialized(value));

        // clone entire group
        final Stream<Either<GitlabProject, Git>> result = flowableToStream(gitService.cloneProjects(projects, cloneDirectoryPath, true));
        final List<GitlabProject> notClonedProjects = result.filter(Either::isLeft).map(Either::getLeft).toJavaList();
        final List<Git> gits = result.filter(Either::isRight).map(Either::get).toJavaList();

        assertThat(notClonedProjects).hasSize(1);
        assertThat(gits).hasSize(2);
        assertThat(gitService.openRepository(firstProject, cloneDirectoryPath).submoduleStatus().call())
                .containsKey("some-project-sub-module")
                .allSatisfy((key, value) -> submoduleIsInitialized(value));
    }

    @Test
    void testCloneRepositories_existingClone_withoutSubmodules() throws GitAPIException, IOException {
        final GitService gitService = new GitService();
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
        // create first clone with only one repo
        final GitlabProject firstProject = projects.blockingFirst();
        final Git existingClone = gitService.cloneProject(firstProject, cloneDirectoryPath, false);
        assertThat(existingClone).isNotNull();
        assertThat(existingClone.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                          .allSatisfy((key, value) -> submoduleIsUninitialized(value));

        // clone entire group
        final Stream<Either<GitlabProject, Git>> result = flowableToStream(new GitService().cloneProjects(projects, cloneDirectoryPath, true));
        final List<GitlabProject> notClonedProjects = result.filter(Either::isLeft).map(Either::getLeft).toJavaList();
        final List<Git> gits = result.filter(Either::isRight).map(Either::get).toJavaList();

        assertThat(notClonedProjects).hasSize(1);
        assertThat(gits).hasSize(2);
        assertThat(gitService.openRepository(firstProject, cloneDirectoryPath).submoduleStatus().call())
                .containsKey("some-project-sub-module")
                .allSatisfy((key, value) -> submoduleIsUninitialized(value));
    }

    private Stream<Either<GitlabProject, Git>> flowableToStream(Flowable<Either<GitlabProject, Git>> gits) {
        return Stream.ofAll(gits.blockingIterable());
    }

    private void submoduleIsInitialized(org.eclipse.jgit.submodule.SubmoduleStatus value) {
        assertSubmoduleStatus(value, INITIALIZED);
    }

    private void submoduleIsUninitialized(org.eclipse.jgit.submodule.SubmoduleStatus value) {
        assertSubmoduleStatus(value, UNINITIALIZED);
    }

    private void assertSubmoduleStatus(org.eclipse.jgit.submodule.SubmoduleStatus value, SubmoduleStatusType initialized) {
        assertThat(value).extracting("type")
                         .isInstanceOfSatisfying(SubmoduleStatusType.class, status -> assertThat(status).isEqualTo(initialized));
    }
}

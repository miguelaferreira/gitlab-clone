package gitlab.clone;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import org.assertj.core.api.Assertions;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GitlabServiceTest {

    public static final String GITLAB_GROUP_NAME = "gitlab-clone-example";
    public static final String GITLAB_GROUP_ID = "11961707";

    @Inject
    private GitlabService service;

    @Test
    void findGroupByName() {
        final Either<String, GitlabGroup> maybeGroup = service.findGroupByName(GITLAB_GROUP_NAME);

        VavrAssertions.assertThat(maybeGroup).isRight();
        Assertions.assertThat(maybeGroup.get().getName()).isEqualTo(GITLAB_GROUP_NAME);
    }

    @Test
    void getDescendantGroups() {
        final Flowable<GitlabGroup> descendantGroups = service.getDescendantGroups(GITLAB_GROUP_ID);

        assertThat(descendantGroups.blockingIterable()).hasSize(3);
    }

    @Test
    void getSubGroupsRecursively() {
        final Flowable<GitlabGroup> descendantGroups = service.getSubGroupsRecursively(GITLAB_GROUP_ID);

        assertThat(descendantGroups.blockingIterable()).hasSize(3);
    }

    @Test
    void getGitlabGroupProjects() {
        final GitlabGroup group = service.findGroupByName(GITLAB_GROUP_NAME).get();
        final Flowable<GitlabProject> projects = service.getGitlabGroupProjects(group);

        assertThat(projects.blockingIterable()).hasSize(3);
    }
}

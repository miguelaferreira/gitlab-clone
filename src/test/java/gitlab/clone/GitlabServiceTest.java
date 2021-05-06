package gitlab.clone;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;


@MicronautTest
class GitlabServiceTest {

    public static final String GITLAB_GROUP = "gitlab-clone-example";

    @Inject
    private GitlabService service;

    @Test
    void searchGroups_nameOnly() {
        final Flowable<GitlabGroup> groups = service.searchGroups(GITLAB_GROUP, true);

        assertThat(groups.blockingIterable()).hasSize(1);
    }

    @Test
    void getGitlabGroupProjects() {
        final GitlabGroup group = service.searchGroups(GITLAB_GROUP, true).blockingFirst();
        final Flowable<GitlabProject> projects = service.getGitlabGroupProjects(group);

        assertThat(projects.blockingIterable()).hasSize(5);
    }
}

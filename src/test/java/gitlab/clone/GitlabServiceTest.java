package gitlab.clone;

import io.micronaut.context.annotation.Value;
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

    @Value("${gitlab.private.token}")
    private String gitlabPrivateToken;

    @Test
    void searchGroups_nameOnly() {
        final Flowable<GitlabGroup> groups = service.searchGroups(gitlabPrivateToken, GITLAB_GROUP, true);

        assertThat(groups.blockingIterable()).hasSize(1);
    }

    @Test
    void getGitlabGroupProjects() {
        final GitlabGroup group = service.searchGroups(gitlabPrivateToken, GITLAB_GROUP, true).blockingFirst();
        final Flowable<GitlabProject> projects = service.getGitlabGroupProjects(gitlabPrivateToken, group);

        assertThat(projects.blockingIterable()).hasSize(5);
    }
}

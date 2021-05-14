package gitlab.clone;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import org.assertj.core.api.Assertions;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;

@MicronautTest
class GitlabServiceTest {

    public static final String GITLAB_GROUP = "gitlab-clone-example";

    @Inject
    private GitlabService service;

    @Test
    void findGroupByName() {
        final Either<String, GitlabGroup> maybeGroup = service.findGroupByName(GITLAB_GROUP);

        VavrAssertions.assertThat(maybeGroup).isRight();
        Assertions.assertThat(maybeGroup.get().getName()).isEqualTo(GITLAB_GROUP);
    }

    @Test
    void getGitlabGroupProjects() {
        final GitlabGroup group = service.findGroupByName(GITLAB_GROUP).get();
        final Flowable<GitlabProject> projects = service.getGitlabGroupProjects(group);

        assertThat(projects.blockingIterable()).hasSize(3);
    }
}

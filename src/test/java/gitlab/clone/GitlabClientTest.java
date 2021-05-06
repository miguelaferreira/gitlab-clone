package gitlab.clone;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GitlabClientTest {

    @Inject
    private GitlabClient client;

    @Test
    void searchGroups() {
        final Flowable<GitlabGroup> groups = client.searchGroups("gitlab-clone-example", 1000);

        assertThat(groups.blockingIterable()).hasSize(4);
    }

    @Test
    void groupDescendants() {
        final Flowable<GitlabGroup> groups = client.groupDescendants("11961707", true, 100, 0);

        assertThat(groups.blockingIterable()).hasSize(3);
    }

    @Test
    void groupProjects() {
        final Flowable<GitlabProject> groups = client.groupProjects("11961707");

        assertThat(groups.blockingIterable()).hasSize(1);
    }
}

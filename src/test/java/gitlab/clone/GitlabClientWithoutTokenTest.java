package gitlab.clone;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ResultOfMethodCallIgnored")
@MicronautTest
@Property(name = "gitlab.token")
class GitlabClientWithoutTokenTest {

    public static final String PRIVATE_GROUP_ID = "12040044";
    public static final String PUBLIC_GROUP_ID = "11961707";
    public static final String PRIVATE_GROUP_NAME = "gitlab-clone-example-private";
    public static final String PUBLIC_GROUP_NAME = "gitlab-clone-example";

    @Inject
    private GitlabClient client;

    @Test
    void searchGroups_privateGroup() {
        final Flowable<GitlabGroup> groups = client.searchGroups(PRIVATE_GROUP_NAME, 10, true);

        assertThat(groups.blockingIterable()).hasSize(0);
    }

    @Test
    void searchGroups_publicGroup() {
        final Flowable<GitlabGroup> groups = client.searchGroups(PUBLIC_GROUP_NAME, 10, true);

        assertThat(groups.blockingIterable()).hasSize(4);
    }

    @Test
    void groupDescendants_privateGroup() {
        final Flowable<GitlabGroup> groups = client.groupDescendants(PRIVATE_GROUP_ID, true, 10, 0);

        final HttpClientResponseException e = assertThrows(HttpClientResponseException.class, groups::blockingFirst);

        assertThat(e).hasMessage("Not Found");
    }

    @Test
    void groupDescendants_publicGroup() {
        final Flowable<GitlabGroup> groups = client.groupDescendants(PUBLIC_GROUP_ID, true, 10, 0);

        assertThat(groups.blockingIterable()).hasSize(3);
    }

    @Test
    void groupProjects_publicGroup() {
        final Flowable<GitlabProject> groups = client.groupProjects(PUBLIC_GROUP_ID);

        assertThat(groups.blockingIterable()).hasSize(1);
    }

    @Test
    void groupProjects_privateGroup() {
        final Flowable<GitlabProject> groups = client.groupProjects(PRIVATE_GROUP_ID);

        final HttpClientException e = assertThrows(HttpClientException.class, groups::blockingFirst);

        assertThat(e).hasMessage("Not Found");
    }
}

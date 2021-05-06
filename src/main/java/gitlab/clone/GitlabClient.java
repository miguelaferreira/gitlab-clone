package gitlab.clone;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.reactivex.Flowable;

import static gitlab.clone.GitlabClient.H_PRIVATE_TOKEN;

@Client("${gitlab.url}/api/v4")
@Header(name = H_PRIVATE_TOKEN, value = "${GITLAB_TOKEN}")
public interface GitlabClient {
    String H_PRIVATE_TOKEN = "PRIVATE-TOKEN";

    @Get("/groups{?search,per_page}")
    Flowable<GitlabGroup> searchGroups(@QueryValue String search, @QueryValue(value = "per_page") int perPage);

    @Get("/groups/{id}/descendant_groups{?all_available,per_page,page}")
    Flowable<GitlabGroup> groupDescendants(
            @PathVariable String id,
            @QueryValue(value = "all_available") boolean allAvailable,
            @QueryValue(value = "per_page") int perPage,
            @QueryValue int page
    );

    @Get("/groups/{id}/projects")
    Flowable<GitlabProject> groupProjects(@PathVariable String id);
}

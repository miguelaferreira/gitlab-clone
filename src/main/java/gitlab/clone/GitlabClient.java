package gitlab.clone;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.reactivex.Flowable;

@Client("${gitlab.url}/api/v4")
public interface GitlabClient {
    String H_PRIVATE_TOKEN = "PRIVATE-TOKEN";

    @Get("/groups{?search,per_page}")
    Flowable<GitlabGroup> searchGroups(@Header(H_PRIVATE_TOKEN) String privateToken, @QueryValue String search, @QueryValue(value = "per_page") int perPage);

    @Get("/groups/{id}/descendant_groups{?all_available,per_page,page}")
    Flowable<GitlabGroup> groupDescendants(
            @Header(H_PRIVATE_TOKEN) String privateToken,
            @PathVariable String id,
            @QueryValue(value = "all_available") boolean allAvailable,
            @QueryValue(value = "per_page") int perPage,
            @QueryValue int page
    );

    @Get("/groups/{id}/projects")
    Flowable<GitlabProject> groupProjects(@Header(H_PRIVATE_TOKEN) String privateToken, @PathVariable String id);
}

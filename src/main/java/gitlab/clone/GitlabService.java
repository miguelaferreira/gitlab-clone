package gitlab.clone;

import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GitlabService {

    public static final int MAX_GROUPS_PER_PAGE = 100;
    public static final String RESPONSE_HEADER_NEXT_PAGE = "X-Next-Page";
    private final GitlabClient client;

    public GitlabService(GitlabClient client) {
        this.client = client;
    }

    public Either<String, GitlabGroup> findGroupByName(String groupName) {
        log.debug("Looking for group named: {}", groupName);
        Function<Integer, Flowable<HttpResponse<List<GitlabGroup>>>> apiCall = pageIndex -> client.searchGroups(groupName, true, MAX_GROUPS_PER_PAGE, pageIndex);
        final Flowable<GitlabGroup> results = paginatedApiCall(apiCall);
        return results.filter(gitlabGroup -> gitlabGroup.getName().equalsIgnoreCase(groupName))
                      .map(Either::<String, GitlabGroup>right).blockingFirst(Either.left("Group not found"));
    }

    public Flowable<GitlabProject> getGitlabGroupProjects(GitlabGroup group) {
        log.debug("Searching for projects in group '{}'", group.getFullPath());
        final String groupId = group.getId();
        Flowable<GitlabProject> projects = getGroupProjects(groupId);
        Flowable<GitlabGroup> subGroups = getSubGroups(groupId);

        return Flowable.concat(projects, subGroups.flatMap(subGroup -> getGroupProjects(subGroup.getId())));
    }

    private Flowable<GitlabGroup> getSubGroups(String groupId) {
        return paginatedApiCall(pageIndex -> client.groupDescendants(groupId, true, MAX_GROUPS_PER_PAGE, pageIndex));
    }

    private Flowable<GitlabProject> getGroupProjects(String groupId) {
        return paginatedApiCall(pageIndex -> client.groupProjects(groupId, true, MAX_GROUPS_PER_PAGE, pageIndex));
    }

    private <T> Flowable<T> paginatedApiCall(final Function<Integer, Flowable<HttpResponse<List<T>>>> apiCall) {
        log.trace("Invoking paginated API");
        final Flowable<HttpResponse<List<T>>> responses = paginatedApiCall(apiCall, 1);
        return responses.map(response -> Option.of(response.body()))
                        .filter(Option::isDefined)
                        .map(Option::get)
                        .flatMap(Flowable::fromIterable);
    }

    private <T> Flowable<HttpResponse<T>> paginatedApiCall(Function<Integer, Flowable<HttpResponse<T>>> apiCall, int pageIndex) {
        try {
            final Flowable<HttpResponse<T>> page = apiCall.apply(pageIndex);
            Flowable<HttpResponse<T>> nextPage = page.flatMap(response -> {
                final String nextPageHeader = Objects.requireNonNullElse(response.getHeaders()
                                                                                 .get(RESPONSE_HEADER_NEXT_PAGE), "0");
                int nextPageIndex;
                if (!nextPageHeader.isBlank() && (nextPageIndex = Integer.parseInt(nextPageHeader)) > 1) {
                    log.trace("Making recursive call to fetch (next) page {}", nextPageIndex);
                    return apiCall.apply(nextPageIndex);
                }
                log.trace("No more pages");
                return Flowable.empty();
            });
            return Flowable.concat(page, nextPage);
        } catch (HttpClientException e) {
            log.error("GitLab API call failed: {}", e.getMessage());
            return Flowable.empty();
        }
    }
}

package gitlab.clone;

import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GitlabService {

    public static final int MAX_GROUPS_PER_PAGE = 20;
    public static final String RESPONSE_HEADER_NEXT_PAGE = "X-Next-Page";
    public static final String GROUP_DESCENDANTS_VERSION = "13.5";
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

    protected Flowable<GitlabGroup> getSubGroups(String groupId) {
        log.trace("Retrieving sub-groups of '{}'", groupId);
        final Option<GitlabVersion> maybeVersion = getVersion();
        if (maybeVersion.isDefined()) {
            final GitlabVersion gitlabVersion = maybeVersion.get();
            if (gitlabVersion.isBefore(GROUP_DESCENDANTS_VERSION)) {
                log.trace("Retrieving sib-groups recursively because GitLab server version is '{}'", gitlabVersion.getVersion());
                return getSubGroupsRecursively(groupId);
            }
        } else {
            log.trace("Could not get GitLab server version, defaulting to retrieving sub-groups with descendant API.");
        }
        return getDescendantGroups(groupId);
    }

    private Option<GitlabVersion> getVersion() {
        try {
            return Option.of(client.version());
        } catch (HttpClientResponseException e) {
            final HttpStatus status = e.getStatus();
            if (status.equals(HttpStatus.UNAUTHORIZED)) {
                log.trace("Could not detect GitLab server version without a valid token.");
            } else {
                log.warn("Unexpected status {} checking GitLab version: {}, ", status.getCode(), status.getReason());
            }
        }
        return Option.<GitlabVersion>none();
    }

    protected Flowable<GitlabGroup> getDescendantGroups(String groupId) {
        return paginatedApiCall(pageIndex -> client.groupDescendants(groupId, true, MAX_GROUPS_PER_PAGE, pageIndex));
    }

    protected Flowable<GitlabGroup> getSubGroupsRecursively(String groupId) {
        final Flowable<GitlabGroup> subGroups = paginatedApiCall(pageIndex -> client.groupSubGroups(groupId, true, MAX_GROUPS_PER_PAGE, pageIndex));
        return Flowable.concat(subGroups, subGroups.flatMap(group -> getSubGroupsRecursively(group.getId())));
    }

    private Flowable<GitlabProject> getGroupProjects(String groupId) {
        log.trace("Retrieving group '{}' projects", groupId);
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

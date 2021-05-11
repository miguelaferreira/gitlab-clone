package gitlab.clone;

import io.micronaut.http.client.exceptions.HttpClientException;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Slf4j
@Singleton
public class GitlabService {

    public static final int MAX_GROUPS_PER_PAGE = 100;
    private final GitlabClient client;

    public GitlabService(GitlabClient client) {
        this.client = client;
    }

    public Flowable<GitlabGroup> searchGroups(String search, boolean onlyNameMatches) {
        try {
            final Flowable<GitlabGroup> gitlabGroupFlowable = client.searchGroups(search, MAX_GROUPS_PER_PAGE, true);
            if (onlyNameMatches) {
                log.debug("Looking for group named: {}", search);
                return gitlabGroupFlowable.filter(gitlabGroup -> gitlabGroup.getName().equalsIgnoreCase(search));
            } else {
                log.debug("Looking for group matching: {}", search);
                return gitlabGroupFlowable;
            }
        } catch (HttpClientException e) {
            log.error("GitLab API call failed: {}", e.getMessage());
            return Flowable.empty();
        }
    }

    public Flowable<GitlabProject> getGitlabGroupProjects(GitlabGroup group) {
        log.debug("Searching for projects in group {}", group.getFullPath());
        final String groupId = group.getId();
        Flowable<GitlabProject> projects = getGroupProjects(groupId);
        Flowable<GitlabGroup> subGroups = getSubGroups(groupId);

        return Flowable.concat(projects, subGroups.flatMap(subGroup -> getGroupProjects(subGroup.getId())));
    }

    private Flowable<GitlabGroup> getSubGroups(String groupId) {
        return getSubGroups(groupId, 0);
    }

    private Flowable<GitlabGroup> getSubGroups(String groupId, int page) {
        log.trace("Looking for subgroups at page {}", page);
        try {
            final Flowable<GitlabGroup> groups = client.groupDescendants(groupId, true, MAX_GROUPS_PER_PAGE, page);
            return Flowable.concat(groups, groups.isEmpty()
                                                 .toFlowable()
                                                 .flatMap(isEmpty -> isEmpty ? Flowable.empty() : getSubGroups(groupId, page + 1)));
        } catch (HttpClientException e) {
            log.error("GitLab API call failed: {}", e.getMessage());
            return Flowable.empty();
        }
    }

    private GitlabProject logProject(GitlabProject project) {
        log.trace("Found project {}", project.getPathWithNamespace());
        return project;
    }

    private Flowable<GitlabProject> getGroupProjects(String groupId) {
        try {
            return client.groupProjects(groupId).map(this::logProject);
        } catch (HttpClientException e) {
            log.error("GitLab API call failed: {}", e.getMessage());
            return Flowable.empty();
        }
    }
}

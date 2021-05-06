package gitlab.clone;

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

    public Flowable<GitlabGroup> searchGroups(String privateToken, String search, boolean onlyNameMatches) {
        final Flowable<GitlabGroup> gitlabGroupFlowable = client.searchGroups(privateToken, search, MAX_GROUPS_PER_PAGE);
        if (onlyNameMatches) {
            log.debug("Looking for group named: {}", search);
            return gitlabGroupFlowable.filter(gitlabGroup -> gitlabGroup.getName().equalsIgnoreCase(search));
        } else {
            log.debug("Looking for group matching: {}", search);
            return gitlabGroupFlowable;
        }
    }

    public Flowable<GitlabProject> getGitlabGroupProjects(String privateToken, GitlabGroup group) {
        log.debug("Searching for projects in group {}", group.getFullPath());
        final String groupId = group.getId();
        Flowable<GitlabProject> projects = getGroupProjects(privateToken, groupId);
        Flowable<GitlabGroup> subGroups = getSubGroups(privateToken, groupId);

        return Flowable.concat(projects, subGroups.flatMap(subGroup -> getGroupProjects(privateToken, subGroup.getId())));
    }

    private Flowable<GitlabGroup> getSubGroups(String privateToken, String groupId) {
        return getSubGroups(privateToken, groupId, 0);
    }

    private Flowable<GitlabGroup> getSubGroups(String privateToken, String groupId, int page) {
        log.trace("Looking for subgroups at page {}", page);
        final Flowable<GitlabGroup> groups = client.groupDescendants(privateToken, groupId, true, MAX_GROUPS_PER_PAGE, page);
        return Flowable.concat(groups, groups.isEmpty()
                                             .toFlowable()
                                             .flatMap(isEmpty -> isEmpty ? Flowable.empty() : getSubGroups(privateToken, groupId, page + 1)));
    }

    private GitlabProject logProject(GitlabProject project) {
        log.trace("Found project {}", project.getPathWithNamespace());
        return project;
    }

    private Flowable<GitlabProject> getGroupProjects(String privateToken, String groupId) {
        return client.groupProjects(privateToken, groupId).map(this::logProject);
    }
}

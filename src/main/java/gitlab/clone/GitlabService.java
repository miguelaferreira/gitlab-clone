package gitlab.clone;

import io.reactivex.Flowable;

import javax.inject.Singleton;

@Singleton
public class GitlabService {

    private final GitlabClient client;

    public GitlabService(GitlabClient client) {
        this.client = client;
    }

    public Flowable<GitlabGroup> searchGroups(String privateToken, String search, boolean onlyNameMatches) {
        final Flowable<GitlabGroup> gitlabGroupFlowable = client.searchGroups(privateToken, search, 1000);
        if (onlyNameMatches) {
            return gitlabGroupFlowable.filter(gitlabGroup -> gitlabGroup.getName().equalsIgnoreCase(search));
        } else {
            return gitlabGroupFlowable;
        }
    }

    public Flowable<GitlabProject> getGitlabGroupProjects(String privateToken, GitlabGroup group) {
        final String groupId = group.getId();
        Flowable<GitlabProject> projects = getGroupProjects(privateToken, groupId);
        Flowable<GitlabGroup> groups = getSubGroups(privateToken, groupId);

        return Flowable.concat(projects, groups.flatMap(subGroup -> getGitlabGroupProjects(privateToken, subGroup)));
    }

    private Flowable<GitlabGroup> getSubGroups(String privateToken, String groupId) {
        return client.groupDescendants(privateToken, groupId);
    }

    private Flowable<GitlabProject> getGroupProjects(String privateToken, String groupId) {
        return client.groupProjects(privateToken, groupId);
    }
}

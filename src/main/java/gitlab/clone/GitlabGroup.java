package gitlab.clone;

import com.fasterxml.jackson.annotation.JsonCreator;

public class GitlabGroup {
    private String id;
    private String name;
    private String path;

    @JsonCreator
    public GitlabGroup() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

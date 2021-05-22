package gitlab.clone;

import java.util.function.Predicate;

public enum GitlabGroupSearchMode {
    NAME, FULL_PATH;

    public String textualQualifier() {
        switch (this) {
            case NAME:
                return "named";
            case FULL_PATH:
                return "with full path";
        }
        throw new IllegalArgumentException("There is no qualifier defined for " + this);
    }

    public Predicate<GitlabGroup> groupPredicate(String search) {
        switch (this) {
            case NAME:
                return group -> group.getName().equalsIgnoreCase(search);
            case FULL_PATH:
                return group -> group.getFullPath().equalsIgnoreCase(search);
        }
        throw new IllegalArgumentException("There is no predicate defined for " + this);
    }
}

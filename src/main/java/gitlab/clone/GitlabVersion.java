package gitlab.clone;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.micronaut.core.annotation.Introspected;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Introspected
@AllArgsConstructor
@ToString
public class GitlabVersion {
    String version;
    String revision;

    @JsonCreator
    public GitlabVersion() {
    }
}

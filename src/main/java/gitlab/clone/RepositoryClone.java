package gitlab.clone;

import lombok.Builder;
import lombok.Value;

import java.net.URI;
import java.nio.file.Path;

@Value
@Builder(builderClassName = "Builder")
public class RepositoryClone {
    Path path;
    String name;
    URI remote;
}

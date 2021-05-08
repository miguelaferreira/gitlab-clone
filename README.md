[![Continuous Integration](https://github.com/miguelaferreira/gitlab-clone/actions/workflows/development.yml/badge.svg)](https://github.com/miguelaferreira/gitlab-clone/actions/workflows/development.yml)
[![Continuous Delivery](https://github.com/miguelaferreira/gitlab-clone/actions/workflows/create-release.yaml/badge.svg)](https://github.com/miguelaferreira/gitlab-clone/actions/workflows/create-release.yaml)

## GitLab Clone

GitLab offers the ability to create groups of repositories and then leverage those groups to manage multiple repositories at one.
Things like CI/CD, user membership can be defined at the group level and then inherited by all the underlying repositories.
Furthermore, it's also possible to create relationships between repositories simply by leveraging the group structure.
For example, one can include git sub-modules and reference them by their relative path.

It's handy, and sometimes needed, to clone the groups of repositories preserving the group structure.
That is what this tool does.

## Installing

The `gitlab-clone` tool is built for two operating systems Linux and macOS.
Each release on this repository provides binaries for these two operating systems.
To install the tool, either download the binary from the latest release, make it executable and place it on a reachable path;
or use `brew`.
```bash
brew install miguelaferreira/tools/git-clone
```

## Usage

Both the gitlab url (`GITLAB_URL`) and the private token (`GITLAB_TOKEN`) for accessing GitLab API are read from the environment.
The GitLab url defaults to [https://gitlab.com](https://gitlab.com) when not defined in the environment.
For cloning public groups the token needs `read_api` scope and for private groups it needs the `api` scope.
See GitLab's [Limiting scopes of a personal access token](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html#limiting-scopes-of-a-personal-access-token)
for more details on token scopes.

```bash
$ GITLAB_TOKEN="..." gitlab-clone -h
Usage: gitlab-clone [-hvVx] [--debug] [--trace] GROUP PATH
Clone an entire GitLab group with all sub-groups and repositories.
      GROUP            The GitLab group to clone.
      PATH             The local path where to create the group clone.
  -h, --help           Show this help message and exit.
  -V, --version        Print version information and exit.
  -v, --verbose        Print out extra information about what the tool is doing.
  -x, --very-verbose   Print out even more information about what the tool is doing.
      --debug          Sets all loggers to DEBUG level.
      --trace          Sets all loggers to TRACE level.
```

## Development

In order to properly build a native image some configuration needs to be generated from running the app as a jar.
That configuration is then included as a resource for the application, and the native image builder will load that to
properly create the single binary.
That can be done by running the app from jar while setting a JVM agent to collect the configuration.  
During the app run all functionality should be exercised.
```
./gradlew clean build
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar build/libs/gitlab-clone-*-all.jar ...
```

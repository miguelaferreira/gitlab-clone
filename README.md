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

```bash
$ GITLAB_TOKEN="..." gitlab-clone -h
Usage: gitlab-clone [-hvV] GROUP PATH
A tool to clone an entire GitLab group with all sub-groups and repositories.
      GROUP       The GitLab group to clone.
      PATH        The local path where to create the group clone.
  -h, --help      Show this help message and exit.
  -v, --verbose   Print out extra information about what the tool is doing.
  -V, --version   Print version information and exit.
```

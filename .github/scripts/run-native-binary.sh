#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

build/native-image/application -V
build/native-image/application -h

build/native-image/application gitlab-clone-example -x tool-test
[[ ! -f  tool-test/gitlab-clone-example/a-project/some-project-sub-module/README.md ]]

build/native-image/application gitlab-clone-example -x -r tool-test
[[ -f  tool-test/gitlab-clone-example/a-project/some-project-sub-module/README.md ]]

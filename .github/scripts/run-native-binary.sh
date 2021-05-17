#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

say() {
  what="$@"
  echo "==> ${what}"
}

say "Asking for tool version"
build/native-image/application -V

say "Asking for tool help"
build/native-image/application -h

local_path="ssh-no-submodules"
say "Asking to clone a group, via ssh, without submodules"
build/native-image/application gitlab-clone-example -x "${local_path}"
[[ ! -f "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]

say "Asking to clone the same group, via ssh, with submodules (effectively only initializing submodules)"
build/native-image/application gitlab-clone-example -x -r "${local_path}"
[[ -f  "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"git@"* ]]
cd -

local_path="ssh-with-submodules"
say "Asking to clone group, via ssh, with submodules"
build/native-image/application gitlab-clone-example -x -r "${local_path}"
[[ -f  "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"git@"* ]]
cd -

local_path="https-with-submodules"
say "Asking to clone group, via https, with submodules"
build/native-image/application gitlab-clone-example -x -r -c HTTPS -u devex-bot "${local_path}"
[[ -f  "${local_path}/gitlab-clone-example/a-project/some-project-sub-module/README.md" ]]
cd "${local_path}/gitlab-clone-example/a-project"
[[ "$(git remote -v | head -n 1)" == *"https://"* ]]
cd -

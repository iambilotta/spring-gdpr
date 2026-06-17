#!/usr/bin/env bash
# Install this repo's git hooks via the pre-commit framework (https://pre-commit.com):
# declarative config in .pre-commit-config.yaml, every tool version pinned, hook
# environments isolated. This script is the stable entry point (`make setup` calls it).
#
# Stages installed: pre-commit PLUS post-merge + post-rewrite. The post-* stages regenerate
# the WHOLE-TREE tracegate `_generated/` catalog after merge / pull / rebase / cherry-pick:
# operations that REPLAY existing commits and so never fire the pre-commit hook (ADR
# sw-scm-007). Without them, integrating several branches yields an HEAD whose `_generated`
# matches no single branch and was regenerated against the union by nobody, and the CI
# `tracegate` drift-gate then catches it late.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if ! command -v pre-commit >/dev/null 2>&1 && [ ! -x "$HOME/.local/bin/pre-commit" ]; then
  echo "pre-commit not found. Install it:" >&2
  echo "  pip3 install --user --break-system-packages pre-commit" >&2
  exit 1
fi

PRE_COMMIT="$(command -v pre-commit || echo "$HOME/.local/bin/pre-commit")"

# pre-commit refuses to `install` while core.hooksPath is set ("Cowardly refusing..."), even
# when it points at the default location. If a clone/worktree sets the override (some do, to
# share hooks across worktrees), drop it for the duration of the install (pre-commit writes
# into the common .git/hooks, which is where any sane override points anyway), then restore it.
COMMON_HOOKS="$(git rev-parse --path-format=absolute --git-common-dir)/hooks"
SAVED_HOOKS_PATH="$(git config --get core.hooksPath || true)"
if [ -n "$SAVED_HOOKS_PATH" ]; then
  git config --unset-all core.hooksPath
  restore_hooks_path() { git config core.hooksPath "$SAVED_HOOKS_PATH"; }
  trap restore_hooks_path EXIT
fi

"$PRE_COMMIT" install
"$PRE_COMMIT" install --hook-type post-merge --hook-type post-rewrite
echo "git hooks installed (pre-commit framework, config: .pre-commit-config.yaml)"
echo "  stages: pre-commit, post-merge, post-rewrite"

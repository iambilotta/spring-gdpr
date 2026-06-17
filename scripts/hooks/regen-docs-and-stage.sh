#!/usr/bin/env bash
# Regenerate every module's `_generated/` catalog and auto-stage the results.
# Idempotent: a clean tree regenerates byte-identical output, so `git add` is a no-op.
# Owned by .pre-commit-config.yaml; do not call directly (use `make requirements` for that).
#
# The generator is the OSS tool `tracegate` (github.com/iambilotta/tracegate), installed
# pinned by `make setup` (-> make tracegate-install). The pin is the single source of the
# version (Makefile TRACEGATE_REF, kept in lockstep with the CI `tracegate` job). Running an
# UNPINNED tracegate here regenerates a different catalog than CI gates against -> false drift,
# so the pinned install is load-bearing, not a nicety.
#
# Apps + labels are NOT passed on the command line: this repo registers them in tracegate.toml
# (deterministic labels across machines/CI, see that file). The zero-config `tracegate .` is the
# same invocation `make requirements` and the CI drift-gate (`tracegate . --check`) use.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

if ! command -v tracegate >/dev/null 2>&1; then
  echo "tracegate not found on PATH. Run 'make setup' (or 'make tracegate-install') once per clone." >&2
  exit 1
fi

# Regenerate every registered module's catalog into its committed `_generated/` dir.
tracegate . >/dev/null

# Stage every generated artifact (the .md docs AND requirements.json, the machine view the
# CI drift-gate also verifies) EXCEPT the gitignored ones. `coverage.md` is tracked here but is
# only refreshed when the JaCoCo CSV exists; tracegate leaves it untouched otherwise (soft-skip),
# so staging it is a no-op on the common path.
while IFS= read -r f; do
  [ -f "$f" ] || continue
  # A gitignored generated artifact would make `git add` exit non-zero under `set -e` and kill
  # the hook on every commit; skip it explicitly (none are ignored today, but keep it robust).
  git check-ignore -q "$f" && continue
  git add "$f"
done < <(find . -type d -name _generated -prune -exec find {} -type f \;)

# spring-gdpr — local regen of the living requirements catalog (tracegate) + git hooks.
#
# The Java build stays on Maven (`./mvnw verify`); this Makefile only drives the
# tracegate dogfood loop so contributors can regenerate / drift-check the
# committed `*/_generated/` catalog the same way CI does, and installs the git
# hooks that keep that catalog from drifting.
#
#   make setup               one-shot per clone: install pinned tracegate + git hooks
#   make requirements        regenerate every module's _generated/ catalog
#   make requirements-check  drift-gate: exit 2 if the catalog drifted from the code
#   make tracegate-install   install tracegate, pinned to the CI commit
#   make install-hooks       install the pre-commit framework hooks (commit + post-merge/post-rewrite)
#
# The catalog is GENERATED — never hand-edit a `_generated/*` file. To change a
# requirement, change the test (rename it, add a @spec javadoc) and rerun `make
# requirements`.
.DEFAULT_GOAL := help

# Pin tracegate to the exact commit CI uses, so local and CI agree byte-for-byte.
TRACEGATE_REF ?= 3bb94964f1e0502d2e68681611bf5ac335180f4b
TRACEGATE_PKG := git+https://github.com/iambilotta/tracegate@$(TRACEGATE_REF)
PIP ?= python3 -m pip
# PEP 668: a system Python refuses `pip install` without this; harmless on a venv.
PIP_FLAGS ?= --break-system-packages

.PHONY: help setup requirements requirements-check tracegate-install install-hooks

help:  ## show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'

setup: tracegate-install install-hooks  ## one-shot per clone: pinned tracegate + git hooks
	@echo "setup complete: tracegate pinned to $(TRACEGATE_REF), git hooks installed"

tracegate-install:  ## install tracegate, pinned to the CI commit
	$(PIP) install --quiet $(PIP_FLAGS) "$(TRACEGATE_PKG)"

install-hooks:  ## install pre-commit hooks (pre-commit + post-merge + post-rewrite, ADR sw-scm-007)
	scripts/install-git-hooks.sh

requirements:  ## regenerate the as-built requirements catalog (writes each module's _generated/)
	tracegate .

requirements-check:  ## drift-gate: fail (exit 2) if the committed catalog drifted from the code
	tracegate . --check

# Harness engineering route

The harness-engineering corpus is a read-only context bundle and set of playbooks
by Ryan Lopopolo, meant to be pointed at a target repository. Two project commands
run its playbooks against this repo just in time, so the corpus is never preloaded
into CLAUDE.md.

- `/harness-review` runs the repository-review playbook, a broad diagnostic of
  whether a coding agent can recover the real job, make a coherent change, prove
  the outcome, and operate safely here without a human relay.
- `/improve-harness` runs the improve-harness playbook, one bounded loop from
  baseline through the earliest failed handoff to the smallest owning
  intervention, native verification, and a fresh rerun.

Both commands read the corpus `AGENTS.md`, follow its routing to the named
playbook, and keep the corpus read-only. This repo is always the target.

## Locate the corpus

The corpus is pinned to commit `226c8d35fb6ea3ed55467753dba6dea2b5fd5778` for
reproducible runs. Any developer with git and network access resolves the same
commit, no pre-existing local layout required. Run this to resolve `CORPUS_DIR`.

```bash
CORPUS_COMMIT=226c8d35fb6ea3ed55467753dba6dea2b5fd5778
CORPUS_URL=https://github.com/lopopolo/harness-engineering.git
CACHE_DIR=$HOME/.cache/harness-engineering/$CORPUS_COMMIT

# Fast path, reuse a local checkout only when it already sits on the pin.
# Never checkout inside a developer's own clone, the corpus stays read-only.
if [ -n "$HARNESS_ENGINEERING_DIR" ] && \
   [ "$(git -C "$HARNESS_ENGINEERING_DIR" rev-parse HEAD 2>/dev/null)" = "$CORPUS_COMMIT" ]; then
  CORPUS_DIR=$HARNESS_ENGINEERING_DIR
else
  # Managed per-user cache, clone once and pin to the exact commit.
  if [ ! -e "$CACHE_DIR/AGENTS.md" ]; then
    git clone --quiet "$CORPUS_URL" "$CACHE_DIR"
  fi
  git -C "$CACHE_DIR" fetch --quiet origin "$CORPUS_COMMIT" 2>/dev/null || true
  git -C "$CACHE_DIR" checkout --quiet "$CORPUS_COMMIT"
  CORPUS_DIR=$CACHE_DIR
fi
echo "corpus $CORPUS_DIR at $(git -C "$CORPUS_DIR" rev-parse HEAD)"
```

Set `HARNESS_ENGINEERING_DIR` to an existing corpus checkout to skip the clone
when it already sits on the pin. To bump the pin, change `CORPUS_COMMIT` here.
This file owns the pin, so it lives in one place.

## Read-only and non-goals

Treat the corpus as read-only. The playbooks target this repo, so every change
lands here through the normal branching and PR workflow. Do not embed the corpus
in this repo, do not paste it into CLAUDE.md, and do not copy its file layouts,
version pins, or fixtures. Adapt its ideas to this repo without importing its
structure.

## Reach

These commands are scoped to this repo. Packaging the playbooks as a plugin in
`Contrast-Security-Inc/claude-marketplace`, so every Contrast repo gets them from
one source, is a possible follow-up tracked under AIML-1164.

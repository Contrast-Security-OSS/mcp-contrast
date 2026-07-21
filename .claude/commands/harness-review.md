---
description: Run the harness-engineering repository-review playbook against this repo, corpus kept read-only.
argument-hint: [optional focus, e.g. "delivery and authority"]
---

# /harness-review

Run Ryan Lopopolo's harness-engineering repository-review playbook against this
repository. It is a broad, read-only diagnostic. It asks whether a capable coding
agent can recover the real job, make a coherent change, prove the outcome, and
operate safely here without using a person as a relay.

An optional focus area may be passed as `$ARGUMENTS`.

## Steps

1. Locate the corpus by running the snippet in `docs/agents/harness.md`. That
   resolves `CORPUS_DIR` to a read-only checkout pinned to a known commit.

2. Read `$CORPUS_DIR/AGENTS.md`. Follow its Application routing and Working loop.
   It routes a broad assessment to the repository-review playbook.

3. Read `$CORPUS_DIR/playbooks/README.md`, then
   `$CORPUS_DIR/playbooks/repository-review.md`. Treat the playbook as a scaffold
   you can adapt, and record any omission with the local evidence that required it.

4. Run the playbook with this repo as the target. Hold one model and
   coding-agent configuration constant. Start from this repo's own instructions,
   `CLAUDE.md` and `docs/agents/`, before pulling any corpus thesis. Pull a corpus
   thesis only when local evidence leaves an owning decision unresolved. Keep the
   corpus read-only, and adapt its ideas without copying its file layouts, version
   pins, or fixtures.

5. Deliver findings ordered by consequence, following the playbook's Findings
   section. For each, name the invariant at risk, the concrete evidence and owning
   boundary, why existing proof missed it, and the smallest owning correction.

This command inspects only. It does not authorize changes to the repo. To act on
one finding through a bounded change-and-verify loop, run `/improve-harness`.

If `$ARGUMENTS` names a focus area, weight the review there while still answering
the playbook's outcome questions.

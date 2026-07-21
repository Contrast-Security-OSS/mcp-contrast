---
description: Run the harness-engineering improve-harness playbook for one bounded job in this repo.
argument-hint: [the job to improve, e.g. "add a new MCP tool"]
---

# /improve-harness

Run Ryan Lopopolo's harness-engineering improve-harness playbook against this
repository. It closes one bounded operational loop, from baseline through the
earliest failed handoff to the smallest owning intervention, native verification,
and a fresh rerun.

The job to improve is passed as `$ARGUMENTS`. If none is given, ask for one
bounded, representative job before starting.

## Steps

1. Locate the corpus by running the snippet in `docs/agents/harness.md`. That
   resolves `CORPUS_DIR` to a read-only checkout pinned to a known commit.

2. Read `$CORPUS_DIR/AGENTS.md`. Follow its Application routing and Working loop,
   and use its Context routing inside the gap-classification step.

3. Read `$CORPUS_DIR/playbooks/README.md`, then
   `$CORPUS_DIR/playbooks/improve-harness.md`. Treat the procedure as a scaffold
   you can adapt or reject, and record what changed and why.

4. Run the loop with this repo as the target. Record the job contract first, and
   establish scope and authority before changing anything. Change this repo only
   when the task and its authority contract grant that operation, otherwise stop
   after recording the evidence, proposed owner, intervention, proof plan, and
   expected effect.

5. Implement the smallest reversible change at the earliest owner, then verify
   both layers. Run this repo's native checks (`make check-test`, plus
   `make verify` when credentials allow), and exercise the user or operational
   journey that establishes the accepted outcome.

6. Record the compact result where this repo keeps architecture or history, a
   bead comment, an ADR under `docs/adr/`, or `CONTEXT.md`. Name the owner, the
   evidence, and the retirement condition.

Keep the corpus read-only, and adapt its ideas without copying its file layouts,
version pins, or fixtures. Follow this repo's branching and PR workflow in
CLAUDE.md for any change.

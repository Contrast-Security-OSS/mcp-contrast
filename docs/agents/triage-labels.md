# Triage Labels

The skills speak in terms of five canonical triage roles. This repo uses the canonical
strings verbatim for all new triage work.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

Apply these labels to Beads with `br label add <bead-id> -l <label>`.

## Existing repo labels (do not repurpose — awareness only)

CLAUDE.md already defines a separate human-review vocabulary. New triage work uses the
canonical labels above; these pre-existing labels keep their established meanings:

- `ready-for-human` — **note the clash**: in CLAUDE.md this means "a human should inspect/
  decide/run the work with agent assistance; autonomous agents should NOT pick it up."
  The canonical role above means "requires human implementation." Both keep agents from
  auto-grabbing the issue, so the string is shared, but read the surrounding context.
- `needs-human-review` — do not start work; ask a human to review first.
- `human-security-review` — security review required before agent work proceeds.
- `external-approval` — blocked on approval outside the agent workflow.
- `human-reviewed` — cleared marker after review; AI may proceed when no human-only label remains.
- `stacked-branch`, `pr-created`, `in-review` — branch/PR workflow state (see CLAUDE.md).

# Triage labels

The skills speak in terms of five canonical triage roles. **The canonical labels are the strings to apply.** When a skill applies a triage role to a bead, use the label in the middle column with `br label add <bead-id> -l <label>`.

`triaged` is not a state role. It only means an initial analysis pass has been recorded on the bead. If that analysis concludes a maintainer still needs to choose a direction, add `needs-decision`.

This repo also has an older bead label vocabulary. The right-hand column records which existing label carries related meaning so you can recognise it on older beads. When applying labels, use the canonical string unless you need to interoperate with older automation.

| Role | Canonical label to apply | Older bead label with related meaning |
| --- | --- | --- |
| Maintainer needs to deliberate or choose a direction | `needs-decision` | `needs-triage` |
| Waiting on reporter for info | `needs-info` | none |
| Fully specified, AFK-ready | `ready-for-agent` | no label, the bead simply has no human-only label and shows up in `br ready` |
| Requires human implementation | `ready-for-human` | `ready-for-human` (exact match, keep using it) |
| Will not be actioned | `wontfix` | none, beads are normally closed with `br close <id> --reason "..."` |

## Human-only labels that block agent work

These labels mean an autonomous agent must not pick the bead up until a human clears it.

- `needs-triage` — older label for `needs-decision`; do not start, ask a maintainer to decide first
- `needs-human-review` — do not start, ask a human to review first
- `human-security-review` — security review required before agent work proceeds
- `external-approval` — blocked on approval outside the agent workflow

`human-reviewed` is the cleared marker. An agent may proceed once no human-only label remains. After review, swap them.

```bash
br label remove <bead-id> -l needs-human-review
br label add <bead-id> -l human-reviewed
```

## Workflow labels (not triage)

`stacked-branch`, `pr-created`, and `in-review` track branch and PR state, see the `bead-workflow` skill. Do not repurpose them.

Edit this file if the vocabulary changes.
